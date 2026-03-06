package ProblemFrame.localsearch.util;

import ProblemFrame.MOIndividual;
import ProgramEntity.*;
import java.util.*;

/**
 * 关键路径识别器
 * 
 * 功能：从完成的调度方案中回溯关键路径
 * - 识别关键打印机和关键批次
 * - 识别关键离散工序
 * - 识别关键机器
 * 
 * 参考：第二章的关键路径回溯方法
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class CriticalPathIdentifier {
    
    /**
     * 识别关键打印机和关键批次
     * 策略：从Cmax回溯，找到最后完成的工件，追溯其打印批次
     */
    public static CriticalPrintInfo identifyCriticalPrinter(MOIndividual individual, Problem problem) {
        if (individual.printSolution == null || individual.printSolution.length == 0) {
            return null;
        }
        
        // 方法1：找到完成时间最晚的打印机（简化但有效）
        double maxEndTime = -1;
        int criticalPrinter = -1;
        int criticalBatchIndex = -1;
        
        for (int i = 0; i < individual.printSolution.length; i++) {
            if (individual.printSolution[i] == null || individual.printSolution[i].isEmpty()) {
                continue;
            }
            
            // 遍历该打印机的所有批次
            for (int j = 0; j < individual.printSolution[i].size(); j++) {
                Solution batch = individual.printSolution[i].get(j);
                if (batch.endTime > maxEndTime) {
                    maxEndTime = batch.endTime;
                    criticalPrinter = i;
                    criticalBatchIndex = j;
                }
            }
        }
        
        if (criticalPrinter < 0) {
            return null;
        }
        
        Solution criticalBatch = individual.printSolution[criticalPrinter].get(criticalBatchIndex);
        
        return new CriticalPrintInfo(
            criticalPrinter, 
            criticalBatchIndex, 
            criticalBatch,
            maxEndTime
        );
    }
    
    /**
     * 识别指定打印机上的关键批次
     * 策略：该打印机的最后一个批次
     */
    public static CriticalPrintInfo identifyCriticalBatchOnPrinter(
            MOIndividual individual, int printerNo) {
        
        if (individual.printSolution == null || 
            printerNo < 0 || 
            printerNo >= individual.printSolution.length) {
            return null;
        }
        
        List<Solution> batchList = individual.printSolution[printerNo];
        if (batchList == null || batchList.isEmpty()) {
            return null;
        }
        
        // 最后一个批次作为关键批次
        int criticalBatchIndex = batchList.size() - 1;
        Solution criticalBatch = batchList.get(criticalBatchIndex);
        
        return new CriticalPrintInfo(
            printerNo,
            criticalBatchIndex,
            criticalBatch,
            criticalBatch.endTime
        );
    }
    
    /**
     * 识别离散阶段的关键工序
     * 策略：在染色体的离散段（jobCount之后）中，找到加工时间最长的工序
     * 
     * 完整实现应该：从schedule回溯，找到关键路径上的离散工序
     */
    public static List<CriticalOperationInfo> identifyCriticalDiscreteOperations(
            MOIndividual individual, Problem problem, int topK) {
        
        List<CriticalOperationInfo> criticalOps = new ArrayList<>();
        
        int jobCount = problem.getJobCount();
        int discreteStart = jobCount;
        int discreteLength = individual.gene_MS.length - discreteStart;
        
        if (discreteLength <= 0) {
            return criticalOps;
        }
        
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        // 收集所有离散工序及其加工时间
        List<OpTimeInfo> allOps = new ArrayList<>();
        
        for (int i = discreteStart; i < individual.gene_MS.length; i++) {
            int jobNo = individual.gene_OS[i];
            int operNo = calculateOperationNumber(individual, i, jobCount);
            
            if (operNo < 0 || jobNo >= operationToIndex.length || 
                operNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            int operIdx = operationToIndex[jobNo][operNo];
            if (operIdx >= proDesMatrix.length) {
                continue;
            }
            
            // 获取可选机器
            List<Integer> availableMachines = new ArrayList<>();
            for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
                if (proDesMatrix[operIdx][m] > 0 && proDesMatrix[operIdx][m] != Double.MAX_VALUE) {
                    availableMachines.add(m);
                }
            }
            
            if (availableMachines.isEmpty()) {
                continue;
            }
            
            // 获取当前机器和加工时间
            int relativeIndex = individual.gene_MS[i];
            if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
                continue;
            }
            
            int machineId = availableMachines.get(relativeIndex - 1);
            double procTime = proDesMatrix[operIdx][machineId];
            
            allOps.add(new OpTimeInfo(i, jobNo, operNo, machineId, procTime));
        }
        
        // 按加工时间降序排序
        allOps.sort((a, b) -> -Double.compare(a.processingTime, b.processingTime));
        
        // 返回前topK个
        int count = Math.min(topK, allOps.size());
        for (int i = 0; i < count; i++) {
            OpTimeInfo op = allOps.get(i);
            criticalOps.add(new CriticalOperationInfo(
                op.position,
                op.jobNo,
                op.operNo,
                op.machineId,
                op.processingTime
            ));
        }
        
        return criticalOps;
    }
    
    /**
     * 识别高能耗的离散工序
     * 按 P_proc * p 排序
     */
    public static List<HighEnergyOperationInfo> identifyHighEnergyOperations(
            MOIndividual individual, Problem problem, Map<Integer, Double> machinePowerMap, int topK) {
        
        List<HighEnergyOperationInfo> highEnergyOps = new ArrayList<>();
        
        int jobCount = problem.getJobCount();
        int discreteStart = jobCount;
        int discreteLength = individual.gene_MS.length - discreteStart;
        
        if (discreteLength <= 0) {
            return highEnergyOps;
        }
        
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        // 收集所有离散工序及其能耗
        List<OpEnergyInfo> allOps = new ArrayList<>();
        
        for (int i = discreteStart; i < individual.gene_MS.length; i++) {
            int jobNo = individual.gene_OS[i];
            int operNo = calculateOperationNumber(individual, i, jobCount);
            
            if (operNo < 0 || jobNo >= operationToIndex.length || 
                operNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            int operIdx = operationToIndex[jobNo][operNo];
            if (operIdx >= proDesMatrix.length) {
                continue;
            }
            
            // 获取可选机器
            List<Integer> availableMachines = new ArrayList<>();
            for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
                if (proDesMatrix[operIdx][m] > 0 && proDesMatrix[operIdx][m] != Double.MAX_VALUE) {
                    availableMachines.add(m);
                }
            }
            
            if (availableMachines.isEmpty()) {
                continue;
            }
            
            // 获取当前机器和加工时间
            int relativeIndex = individual.gene_MS[i];
            if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
                continue;
            }
            
            int machineId = availableMachines.get(relativeIndex - 1);
            double procTime = proDesMatrix[operIdx][machineId];
            
            // 获取机器功率
            double power = machinePowerMap.getOrDefault(machineId, 100.0);
            double energy = power * procTime / 3600.0;  // 转换为kWh
            
            allOps.add(new OpEnergyInfo(i, jobNo, operNo, machineId, procTime, energy, availableMachines));
        }
        
        // 按能耗降序排序
        allOps.sort((a, b) -> -Double.compare(a.energy, b.energy));
        
        // 返回前topK个
        int count = Math.min(topK, allOps.size());
        for (int i = 0; i < count; i++) {
            OpEnergyInfo op = allOps.get(i);
            highEnergyOps.add(new HighEnergyOperationInfo(
                op.position,
                op.jobNo,
                op.operNo,
                op.machineId,
                op.processingTime,
                op.energy,
                op.availableMachines
            ));
        }
        
        return highEnergyOps;
    }
    
    /**
     * 计算工序编号
     * 根据染色体位置计算该位置对应的工序编号
     */
    private static int calculateOperationNumber(MOIndividual individual, int pos, int jobCount) {
        if (pos < jobCount) {
            return 0;  // 打印工序
        }
        
        // 离散工序段
        int jobNo = individual.gene_OS[pos];
        
        // 统计该工件在前面出现的次数
        int occurrences = 0;
        for (int i = jobCount; i < pos; i++) {
            if (individual.gene_OS[i] == jobNo) {
                occurrences++;
            }
        }
        
        // 工序编号 = 2（打印和批处理） + occurrences
        return 2 + occurrences;
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 关键打印信息
     */
    public static class CriticalPrintInfo {
        public final int printerNo;          // 打印机编号
        public final int batchIndex;         // 批次索引
        public final Solution batch;         // 批次对象
        public final double completionTime;  // 完成时间
        
        public CriticalPrintInfo(int printerNo, int batchIndex, Solution batch, double completionTime) {
            this.printerNo = printerNo;
            this.batchIndex = batchIndex;
            this.batch = batch;
            this.completionTime = completionTime;
        }
    }
    
    /**
     * 关键工序信息
     */
    public static class CriticalOperationInfo {
        public final int position;           // 染色体位置
        public final int jobNo;              // 工件编号
        public final int operNo;             // 工序编号
        public final int machineId;          // 当前机器ID
        public final double processingTime;  // 加工时间
        
        public CriticalOperationInfo(int position, int jobNo, int operNo, int machineId, double processingTime) {
            this.position = position;
            this.jobNo = jobNo;
            this.operNo = operNo;
            this.machineId = machineId;
            this.processingTime = processingTime;
        }
    }
    
    /**
     * 高能耗工序信息
     */
    public static class HighEnergyOperationInfo {
        public final int position;
        public final int jobNo;
        public final int operNo;
        public final int machineId;
        public final double processingTime;
        public final double energy;
        public final List<Integer> availableMachines;
        
        public HighEnergyOperationInfo(int position, int jobNo, int operNo, int machineId,
                                      double processingTime, double energy, List<Integer> availableMachines) {
            this.position = position;
            this.jobNo = jobNo;
            this.operNo = operNo;
            this.machineId = machineId;
            this.processingTime = processingTime;
            this.energy = energy;
            this.availableMachines = availableMachines;
        }
    }
    
    // 内部辅助类
    private static class OpTimeInfo {
        int position;
        int jobNo;
        int operNo;
        int machineId;
        double processingTime;
        
        OpTimeInfo(int position, int jobNo, int operNo, int machineId, double processingTime) {
            this.position = position;
            this.jobNo = jobNo;
            this.operNo = operNo;
            this.machineId = machineId;
            this.processingTime = processingTime;
        }
    }
    
    private static class OpEnergyInfo {
        int position;
        int jobNo;
        int operNo;
        int machineId;
        double processingTime;
        double energy;
        List<Integer> availableMachines;
        
        OpEnergyInfo(int position, int jobNo, int operNo, int machineId,
                    double processingTime, double energy, List<Integer> availableMachines) {
            this.position = position;
            this.jobNo = jobNo;
            this.operNo = operNo;
            this.machineId = machineId;
            this.processingTime = processingTime;
            this.energy = energy;
            this.availableMachines = new ArrayList<>(availableMachines);
        }
    }
}
