package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import java.util.*;

/**
 * T1: 基于打印时间的跨机移动（对应第二章N1）
 * 
 * 邻域策略：将加工时间最长机器上最高的零件分配给加工时间最短的打印机
 * 
 * 完全照搬第二章的N1实现，适配第三章的Operator接口
 * 
 * @author AI Assistant (基于第二章N1)
 * @version 2.0
 */
public class T1_CriticalBatchFrontInsert extends AbstractOperator {
    
    private final Problem problem;
    
    public T1_CriticalBatchFrontInsert(Random random, Problem problem) {
        super("T1-PrintTimeBasedMove", IndividualType.TIME_DEFICIENT, random);
        this.problem = problem;
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("T1-PrintTimeBasedMove");
        // 必须先evaluate才有printSolution
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
        
        // 步骤1：找到打印完工时间（加工时间）最长的机器
        int maxTimeMachineIdx = -1;
        double maxTime = Double.NEGATIVE_INFINITY;
        
        // ✅ 只遍历打印机范围
        int searchLimit = Math.min(printMachineCount, batchSolution.length);
        
        for (int i = 0; i < searchLimit; i++) {
            List<Solution> machineSolution = batchSolution[i];
            if (machineSolution == null || machineSolution.isEmpty()) {
                continue;
            }
            
            double endTime = machineSolution.get(machineSolution.size() - 1).endTime;
            if (endTime > maxTime) {
                maxTime = endTime;
                maxTimeMachineIdx = i;
            }
        }
        
        if (maxTimeMachineIdx == -1) {
            return null;
        }
        
        int maxTimeMachineNo = maxTimeMachineIdx + 1;  // 1-based
        
        // 步骤2：在该机器上找到最高的零件
        int highestJobIdx = -1;
        double maxHeight = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < printCount; i++) {
            if (ms[i] == maxTimeMachineNo) {
                int jobNo = os[i];
                Item item = items[jobNo];
                
                if (item.h > maxHeight) {
                    maxHeight = item.h;
                    highestJobIdx = i;
                }
            }
        }
        
        if (highestJobIdx == -1) {
            return null;
        }
        
        int jobNo = os[highestJobIdx];
        Item item = items[jobNo];
        
        // 步骤3：找到加工时间（完工时间）最短的打印机
        int minTimeMachineIdx = -1;
        double minTime = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < printMachineCount; i++) {  // ✅ 只遍历打印机
            if (i == maxTimeMachineIdx) continue;  // 跳过源机器
            
//            // ✅ 安全地获取打印机
//            if (i >= machines.length || !(machines[i] instanceof PrintMachine)) {
//                continue;
//            }
            
            PrintMachine targetMachine = (PrintMachine) machines[i];
            boolean fitsNormal = (item.l <= targetMachine.L && item.w <= targetMachine.W && item.h <= targetMachine.H);
            boolean fitsRotated = (item.w <= targetMachine.L && item.l <= targetMachine.W && item.h <= targetMachine.H);
            
            if (fitsNormal || fitsRotated) {
                // 检查该机器的打印完工时间
                List<Solution> machineSolution = (i < batchSolution.length) ? batchSolution[i] : null;
                double endTime = 0.0;
                if (machineSolution != null && !machineSolution.isEmpty()) {
                    endTime = machineSolution.get(machineSolution.size() - 1).endTime;
                }
                
                if (endTime < minTime) {
                    minTime = endTime;
                    minTimeMachineIdx = i;
                }
            }
        }
        
        // 如果找到了合适的目标机器
        if (minTimeMachineIdx != -1) {
            // 记录原机器分配
            int fromMachine = maxTimeMachineNo;
            int toMachine = minTimeMachineIdx + 1;
            
            // 标记受影响的作业和打印机
            delta.markJobAffected(jobNo);
            delta.markPrinterAffected(maxTimeMachineIdx);
            delta.markPrinterAffected(minTimeMachineIdx);
            
            // 应用移动：修改染色体的机器分配
            individual.gene_MS[highestJobIdx] = toMachine;
            individual.printSolution = null;  // 清空装箱结果
            
            return new Candidate(individual, delta, getName());
        }
        
        return null;
    }
}
