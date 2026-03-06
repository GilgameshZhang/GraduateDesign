package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProblemFrame.localsearch.util.EnergyDataProvider;
import ProblemFrame.localsearch.util.EnergyDataProvider.PrinterEnergyStats;
import ProblemFrame.localsearch.util.SkylinePackingHelper;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import java.util.*;

/**
 * E1: 打印任务集中化算子（降idle/start）
 * 
 * 目标：减少打印机idle能耗或启动次数；尽量集中到少数机器上运行
 * 
 * 邻域动作：从idle能耗高/利用率低的打印机迁移少量零件到已高负载的打印机现有批次（不新增批次优先）
 * 
 * 设计文档位置：7.2 ENERGY_DEFICIENT算子池 - E1
 * 
 * 完整版本：使用真实的能耗数据和Skyline装箱
 * 
 * @author AI Assistant
 * @version 2.0
 */
public class E1_PrintConsolidationMove extends AbstractOperator {
    
    /** 低利用率阈值（利用率低于此值的打印机将被考虑） */
    private static final double LOW_UTILIZATION_THRESHOLD = 0.5;
    
    /** 选择源打印机的比例（选择idle能耗最高的前20%） */
    private static final double SOURCE_SELECTION_RATIO = 0.20;
    
    /** 最小空余面积比例 */
    private static final double MIN_FREE_SPACE_RATIO = 0.10;
    
    public E1_PrintConsolidationMove(Random random) {
        super("E1-PrintConsolidationMove", IndividualType.ENERGY_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("E1-PrintConsolidationMove");
        // 步骤1: 检查printSolution
        if (individual.printSolution == null || individual.printSolution.length < 2) {
            return null;  // 至少需要2台打印机
        }
        
        // 步骤2: 使用真实的能耗数据提供器计算每台打印机的idle能耗和利用率
        EnergyDataProvider energyProvider = new EnergyDataProvider(problem);
        List<PrinterEnergyStats> printerStats = energyProvider.calculateAllPrinterStats(individual);
        if (printerStats.isEmpty()) {
            return null;
        }
        
        // 步骤3: 选择源打印机Pa（idle能耗最大或利用率最低的前20%）
        List<PrinterEnergyStats> sourceCandidates = selectSourcePrinters(printerStats);
        if (sourceCandidates.isEmpty()) {
            return null;
        }
        
        PrinterEnergyStats sourcePrinter = sourceCandidates.get(0);  // 选择第一个
        
        // 步骤4: 在Pa中选可迁移零件（小件、低高度优先）
        TransferableItem transferItem = selectTransferableItem(
            individual.printSolution[sourcePrinter.printerNo], problem);
        if (transferItem == null) {
            return null;
        }
        
        // 步骤5: 选目标打印机Pb：已高负载且最后批次空余面积大
        PrinterEnergyStats targetPrinter = selectTargetPrinter(
            printerStats, sourcePrinter.printerNo, transferItem.item, problem);
        if (targetPrinter == null) {
            return null;
        }
        
        // 步骤6: 使用真实Skyline装箱试插到Pb的现有批次
        int targetBatchIndex = tryInsertIntoBatchesWithSkyline(
            individual.printSolution[targetPrinter.printerNo],
            transferItem.item,
            (PrintMachine) problem.getMachines()[targetPrinter.printerNo]
        );
        
        if (targetBatchIndex < 0) {
            return null;  // 插入失败
        }
        
        // 步骤7: 记录Pa、Pb局部快照
        delta.snapshotPrinterBatches(sourcePrinter.printerNo, 
            individual.printSolution[sourcePrinter.printerNo]);
        delta.snapshotPrinterBatches(targetPrinter.printerNo, 
            individual.printSolution[targetPrinter.printerNo]);
        delta.markPrinterAffected(sourcePrinter.printerNo);
        delta.markPrinterAffected(targetPrinter.printerNo);
        
        // 步骤8: 执行迁移 - 修改染色体的gene_MS
        int jobNo = parseJobNumber(transferItem.item.name);
        if (jobNo < 0) {
            return null;  // 无法解析工件编号
        }
        
        // 在gene_OS中找到该工件的位置（打印段：0 ~ jobCount-1）
        int jobCount = problem.getJobCount();
        int jobPositionInOS = -1;
        for (int i = 0; i < jobCount; i++) {
            if (individual.gene_OS[i] == jobNo) {
                jobPositionInOS = i;
                break;
            }
        }
        
        if (jobPositionInOS == -1) {
            return null;  // 未找到工件位置
        }
        
        // 修改gene_MS：从源打印机改为目标打印机（1-based）
        individual.gene_MS[jobPositionInOS] = targetPrinter.printerNo + 1;
        
        // 清空printSolution，下次评估会重新解码
        individual.printSolution = null;
        
        // 步骤9: 标记affectedJobs
        delta.markJobAffected(jobNo);
        
        return new Candidate(individual, delta, getName());
    }
    
    /**
     * 选择源打印机
     * 策略：选择idle能耗最大或利用率最低的前20%
     */
    private List<PrinterEnergyStats> selectSourcePrinters(List<PrinterEnergyStats> allPrinters) {
        // 过滤：只考虑利用率低于阈值的打印机
        List<PrinterEnergyStats> lowUtilPrinters = new ArrayList<>();
        for (PrinterEnergyStats stats : allPrinters) {
            if (stats.avgUtilization < LOW_UTILIZATION_THRESHOLD) {
                lowUtilPrinters.add(stats);
            }
        }
        
        if (lowUtilPrinters.isEmpty()) {
            lowUtilPrinters = new ArrayList<>(allPrinters);
        }
        
        // 按idle能耗降序排序（使用真实的idle能耗数据）
        lowUtilPrinters.sort((a, b) -> -Double.compare(a.idleEnergy, b.idleEnergy));
        
        // 返回前20%
        int count = Math.max(1, (int) (lowUtilPrinters.size() * SOURCE_SELECTION_RATIO));
        return lowUtilPrinters.subList(0, Math.min(count, lowUtilPrinters.size()));
    }
    
    /**
     * 从批次中选择可迁移零件
     * 策略：小件、低高度优先
     */
    private TransferableItem selectTransferableItem(List<Solution> batchList, Problem problem) {
        if (batchList == null || batchList.isEmpty()) {
            return null;
        }
        
        // 遍历所有批次，找到可迁移的零件
        for (int batchIdx = 0; batchIdx < batchList.size(); batchIdx++) {
            Solution batch = batchList.get(batchIdx);
            if (batch.placeItemList == null || batch.placeItemList.size() <= 1) {
                continue;  // 批次只有一个零件，不能迁移
            }
            
            // 计算零件评分
            List<ItemScore> scores = new ArrayList<>();
            for (int itemIdx = 0; itemIdx < batch.placeItemList.size(); itemIdx++) {
                PlaceItem item = batch.placeItemList.get(itemIdx);
                double area = item.l * item.w;
                double height = item.h;
                double score = 1.0 / (area * height + 1.0);  // 小件、低高度评分高
                scores.add(new ItemScore(batchIdx, itemIdx, item, score));
            }
            
            if (!scores.isEmpty()) {
                // 按评分降序排序
                scores.sort((a, b) -> -Double.compare(a.score, b.score));
                ItemScore best = scores.get(0);
                return new TransferableItem(best.batchIndex, best.itemIndex, best.item);
            }
        }
        
        return null;
    }
    
    /**
     * 选择目标打印机
     * 策略：已高负载且最后批次空余面积大
     */
    private PrinterEnergyStats selectTargetPrinter(List<PrinterEnergyStats> allPrinters,
                                                  int sourcePrinter, PlaceItem item, Problem problem) {
        Machine[] machines = problem.getMachines();
        List<PrinterEnergyStats> candidates = new ArrayList<>();
        
        for (PrinterEnergyStats stats : allPrinters) {
            if (stats.printerNo == sourcePrinter) {
                continue;  // 跳过源打印机
            }
            
            // 只考虑高负载的打印机（利用率 >= 阈值）
            if (stats.avgUtilization < LOW_UTILIZATION_THRESHOLD) {
                continue;
            }
            
            // 检查是否能容纳零件
            if (!(machines[stats.printerNo] instanceof PrintMachine)) {
                continue;
            }
            
            PrintMachine pm = (PrintMachine) machines[stats.printerNo];
            if (!SkylinePackingHelper.canFitInPrinter(item, pm, true)) {
                continue;
            }
            
            candidates.add(stats);
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 按利用率降序排序（选择最高负载的）
        candidates.sort((a, b) -> -Double.compare(a.avgUtilization, b.avgUtilization));
        
        return candidates.get(0);
    }
    
    /**
     * 使用真实Skyline装箱尝试插入到批次
     */
    private int tryInsertIntoBatchesWithSkyline(List<Solution> batchList, PlaceItem item, PrintMachine printer) {
        // 尝试从最后一个批次开始插入
        for (int i = batchList.size() - 1; i >= 0; i--) {
            Solution batch = batchList.get(i);
            
            if (SkylinePackingHelper.canInsertToBatch(batch, item, printer, true)) {
                batch.placeItemList.add(item);
                batch.totalS += item.l * item.w;
                batch.maxG = Math.max(batch.maxG, item.h);
                batch.rate = batch.totalS / (printer.L * printer.W);
                return i;
            }
        }
        
        return -1;
    }
    
    private int parseJobNumber(String itemName) {
        if (itemName == null || itemName.isEmpty()) {
            return -1;
        }
        
        try {
            if (itemName.startsWith("J") || itemName.startsWith("j")) {
                return Integer.parseInt(itemName.substring(1));
            } else {
                return Integer.parseInt(itemName);
            }
        } catch (NumberFormatException e) {
            return -1;
        }
    }
    
    // ==================== 内部类 ====================
    
    private static class TransferableItem {
        int batchIndex;
        int itemIndex;
        PlaceItem item;
        
        TransferableItem(int batchIndex, int itemIndex, PlaceItem item) {
            this.batchIndex = batchIndex;
            this.itemIndex = itemIndex;
            this.item = item;
        }
    }
    
    private static class ItemScore {
        int batchIndex;
        int itemIndex;
        PlaceItem item;
        double score;
        
        ItemScore(int batchIndex, int itemIndex, PlaceItem item, double score) {
            this.batchIndex = batchIndex;
            this.itemIndex = itemIndex;
            this.item = item;
            this.score = score;
        }
    }
}
