package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import java.util.*;

/**
 * T2: 基于面积占用率的跨机移动（对应第二章N2）
 * 
 * 邻域策略：将最后一批中高度最高的零件转移到最后一批中面积利用率最小的机器
 * 
 * 完全照搬第二章的N2实现，适配第三章的Operator接口
 * 
 * @author AI Assistant (基于第二章N2)
 * @version 2.0
 */
public class T2_CriticalBatchAreaTransfer extends AbstractOperator {
    
    private final Problem problem;
    
    public T2_CriticalBatchAreaTransfer(Random random, Problem problem) {
        super("T2-AreaBasedMove", IndividualType.TIME_DEFICIENT, random);
        this.problem = problem;
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("T2-AreaBasedMove");
        //System.out.println("T2");
        if (individual.printSolution == null) {
            return null;
        }
        
        List<Solution>[] batchSolution = individual.printSolution;
        int[] ms = individual.gene_MS;
        int[] os = individual.gene_OS;
        Item[] items = problem.getItems();
        Machine[] machines = problem.getMachines();
        int printCount = problem.getJobCount();
        int printMachineCount = problem.getPrintMachineCount();
        
        // 步骤1：在所有打印机的最后一批中，找到高度最高的零件
        int highestJobIdx = -1;
        int highestJobMachineIdx = -1;
        double maxHeight = Double.NEGATIVE_INFINITY;
        
        // ✅ 只遍历打印机范围
        int searchLimit = Math.min(printMachineCount, batchSolution.length);
        
        for (int i = 0; i < searchLimit; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) continue;
            
            Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
            
            // 遍历该批次中的所有零件
            for (PlaceItem placeItem : lastBatch.placeItemList) {
                int jobNo = Integer.parseInt(placeItem.name);
                Item item = items[jobNo];
                
                if (item.h > maxHeight) {
                    maxHeight = item.h;
                    highestJobMachineIdx = i;
                    
                    // 在gene_OS中找到该零件的位置
                    for (int j = 0; j < printCount; j++) {
                        if (os[j] == jobNo && ms[j] == (i + 1)) {
                            highestJobIdx = j;
                            break;
                        }
                    }
                }
            }
        }
        
        if (highestJobIdx == -1) {
            return null;
        }
        
        int jobNo = os[highestJobIdx];
        Item item = items[jobNo];
        
        // 步骤2：找到最后一批中面积利用率最小的机器（只遍历打印机）
        int minRateMachineIdx = -1;
        double minRate = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < printMachineCount; i++) {  // ✅ 只遍历打印机
            if (i == highestJobMachineIdx) continue;  // 跳过源机器
            
            // ✅ 安全地获取打印机
            if (i >= machines.length || !(machines[i] instanceof PrintMachine)) {
                continue;
            }
            
            PrintMachine targetMachine = (PrintMachine) machines[i];
            List<Solution> machineSolution = (i < batchSolution.length) ? batchSolution[i] : null;
            
            if (machineSolution == null || machineSolution.isEmpty()) {
                // 空机器，利用率为0（最小）
                boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
                boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
                
                if (fitsNormal || fitsRotated) {
                    minRate = 0.0;
                    minRateMachineIdx = i;
                }
                continue;
            }
            
            Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
            double rate = lastBatch.rate;
            
            // 检查该机器能否容纳要移动的零件
            boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
            boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
            
            if ((fitsNormal || fitsRotated) && rate < minRate) {
                minRate = rate;
                minRateMachineIdx = i;
            }
        }
        
        // 如果找到了合适的目标机器，创建移动
        if (minRateMachineIdx != -1) {
            // 标记受影响的作业和打印机
            delta.markJobAffected(jobNo);
            delta.markPrinterAffected(highestJobMachineIdx);
            delta.markPrinterAffected(minRateMachineIdx);
            
            // 应用移动：修改染色体的机器分配
            individual.gene_MS[highestJobIdx] = minRateMachineIdx + 1;
            individual.printSolution = null;  // 清空装箱结果
            
            return new Candidate(individual, delta, getName());
        }
        
        return null;
    }
}
