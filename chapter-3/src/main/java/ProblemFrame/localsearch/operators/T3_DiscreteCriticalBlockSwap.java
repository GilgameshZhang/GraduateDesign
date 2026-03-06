package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.*;
import java.util.*;

/**
 * T3: 相邻批次交换（对应第二章N3）
 * 
 * 邻域策略：在同一台打印机上，交换相邻两个批次的执行顺序
 * 策略：在打印完工时间最长的机器上进行批次交换
 * 
 * 完全照搬第二章的N3实现，适配第三章的Operator接口
 * 
 * @author AI Assistant (基于第二章N3)
 * @version 2.0
 */
public class T3_DiscreteCriticalBlockSwap extends AbstractOperator {
    
    private final Problem problem;
    
    public T3_DiscreteCriticalBlockSwap(Random random, Problem problem) {
        super("T3-AdjacentBatchSwap", IndividualType.TIME_DEFICIENT, random);
        this.problem = problem;
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("T3-AdjacentBatchSwap");
        //System.out.println("T3-AdjacentBatchSwap");
        if (individual.printSolution == null) {
            return null;
        }
        
        List<Solution>[] batchSolution = individual.printSolution;
        int[] ms = individual.gene_MS;
        int[] os = individual.gene_OS;
        int printCount = problem.getJobCount();
        
        // 找到打印完工时间最长的机器
        int maxMachineIdx = -1;
        double maxTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < batchSolution.length; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) {
                continue;
            }
            
            double endTime = machineSolution.get(machineSolution.size() - 1).endTime;
            if (endTime > maxTime) {
                maxTime = endTime;
                maxMachineIdx = i;
            }
        }
        
        if (maxMachineIdx == -1) {
            return null;
        }
        
        // 遍历该打印机的批次
        List<Solution> machineBatches = batchSolution[maxMachineIdx];
        
        if (machineBatches == null || machineBatches.size() < 2) {
            return null;  // 少于2个批次，无法交换
        }
        
        // 随机选择一对相邻批次
        int batchIdx = random.nextInt(machineBatches.size() - 1);
        
        Solution batch1 = machineBatches.get(batchIdx);
        Solution batch2 = machineBatches.get(batchIdx + 1);
        
        // 收集批次1的零件在gene_OS中的位置
        List<Integer> batch1Positions = new ArrayList<>();
        for (PlaceItem item : batch1.placeItemList) {
            int jobNo = Integer.parseInt(item.name);
            // 在打印段找到该零件的位置
            for (int i = 0; i < printCount; i++) {
                if (os[i] == jobNo && ms[i] == (maxMachineIdx + 1)) {
                    batch1Positions.add(i);
                    break;
                }
            }
        }
        
        // 收集批次2的零件在gene_OS中的位置
        List<Integer> batch2Positions = new ArrayList<>();
        for (PlaceItem item : batch2.placeItemList) {
            int jobNo = Integer.parseInt(item.name);
            // 在打印段找到该零件的位置
            for (int i = 0; i < printCount; i++) {
                if (os[i] == jobNo && ms[i] == (maxMachineIdx + 1)) {
                    // 确保不重复添加（一个零件只能在一个批次中）
                    if (!batch1Positions.contains(i)) {
                        batch2Positions.add(i);
                        break;
                    }
                }
            }
        }
        
        if (batch1Positions.isEmpty() || batch2Positions.isEmpty()) {
            return null;
        }
        
        // 保存原始状态
        int[] originalOS = os.clone();
        int[] originalMS = ms.clone();
        
        // 执行批次交换
        // 提取批次1和批次2的零件和机器信息
        List<Integer> batch1Jobs = new ArrayList<>();
        List<Integer> batch1Machines = new ArrayList<>();
        for (int pos : batch1Positions) {
            batch1Jobs.add(os[pos]);
            batch1Machines.add(ms[pos]);
        }
        
        List<Integer> batch2Jobs = new ArrayList<>();
        List<Integer> batch2Machines = new ArrayList<>();
        for (int pos : batch2Positions) {
            batch2Jobs.add(os[pos]);
            batch2Machines.add(ms[pos]);
        }
        
        // 合并所有位置并排序（确保按原顺序处理）
        List<Integer> allPositions = new ArrayList<>();
        allPositions.addAll(batch1Positions);
        allPositions.addAll(batch2Positions);
        Collections.sort(allPositions);
        
        // 重新排列：先放batch2，再放batch1（实现交换）
        int writeIdx = 0;
        for (int pos : allPositions) {
            if (writeIdx < batch2Jobs.size()) {
                os[pos] = batch2Jobs.get(writeIdx);
                ms[pos] = batch2Machines.get(writeIdx);
            } else {
                int batch1Idx = writeIdx - batch2Jobs.size();
                os[pos] = batch1Jobs.get(batch1Idx);
                ms[pos] = batch1Machines.get(batch1Idx);
            }
            writeIdx++;
        }
        
        individual.printSolution = null;  // 清空装箱结果
        
        // 标记受影响的打印机和作业
        delta.markPrinterAffected(maxMachineIdx);
        for (int jobId : batch1Jobs) {
            delta.markJobAffected(jobId);
        }
        for (int jobId : batch2Jobs) {
            delta.markJobAffected(jobId);
        }
        
        return new Candidate(individual, delta, getName());
    }
}
