package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.*;
import java.util.*;

/**
 * T5: 关键工序机器重分配（对应第二章N5）
 * 
 * 邻域策略：识别所有关键路径上的离散处理工序，对可选机器根据加工时间进行轮盘赌重新选择
 * - 加工时间短的机器被选中概率高
 * - 只修改机器分配（gene_MS中的相对索引），不移动工序顺序（gene_OS）
 * 
 * 操作前约束筛选：
 * 1. 机器可行性约束：只考虑该工序的可选机器集合（proDesMatrix）
 * 2. 轮盘赌选择：加工时间短的机器权重大
 * 
 * 注意：由于离散工序的MS使用相对索引编码（固定顺序），此处直接修改MS中的相对索引
 * 
 * 完全照搬第二章的N5实现，适配第三章的Operator接口
 * 
 * @author AI Assistant (基于第二章N5)
 * @version 2.0
 */
public class T5_MachineReassignment extends AbstractOperator {
    
    private final Problem problem;
    
    public T5_MachineReassignment(Random random, Problem problem) {
        super("T5-MachineReassignment", IndividualType.TIME_DEFICIENT, random);
        this.problem = problem;
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("T5-MachineReassignment");
        // 从individual获取已评估的operationMatrix
        Operation[][] operationMatrix = individual.operationMatrix;
        
        // 如果operationMatrix为空（未评估），返回null
        if (operationMatrix == null) {
            return null;
        }
        
        // 1. 识别关键路径
        List<Operation> criticalPath = identifyCriticalPath(operationMatrix);
        if (criticalPath == null || criticalPath.isEmpty()) {
            return null;
        }
        
        int[] os = individual.gene_OS;
        int[] ms = individual.gene_MS;
        int jobCount = problem.getJobCount();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        // 2. 对关键路径上的每个离散工序，尝试重分配机器
        List<MachineReassignmentCandidate> candidates = new ArrayList<>();
        
        for (Operation op : criticalPath) {
            // 只处理离散工序
            if (op.task < 2) {
                continue;
            }
            
            int jobNo = op.jobNo;
            int currentMachine = op.machineNo;  // 0-based
            int taskNo = op.task;
            
            // 检查工序编号是否有效
            if (taskNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            // 获取该工序在proDesMatrix中的索引
            int operIdx = operationToIndex[jobNo][taskNo];
            
            // 计算该工序在gene_MS中的固定索引
            int msIndex = jobCount;
            for (int j = 0; j < jobNo; j++) {
                msIndex += (problem.getOperationCountArr()[j] - 2);
            }
            msIndex += (taskNo - 2);
            
            // 获取该工序的所有可选机器及其加工时间
            List<Integer> allAvailableMachines = new ArrayList<>();
            List<Double> allProcessingTimes = new ArrayList<>();
            
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                    allAvailableMachines.add(k);  // 0-based
                    allProcessingTimes.add(proDesMatrix[operIdx][k]);
                }
            }
            
            if (allAvailableMachines.isEmpty()) {
                continue;
            }
            
            // 排除当前机器，构建候选机器列表
            List<Integer> candidateMachinesIdx = new ArrayList<>();
            List<Double> candidateProcessingTimes = new ArrayList<>();
            
            for (int i = 0; i < allAvailableMachines.size(); i++) {
                if (allAvailableMachines.get(i) != currentMachine) {
                    candidateMachinesIdx.add(i);
                    candidateProcessingTimes.add(allProcessingTimes.get(i));
                }
            }
            
            if (candidateMachinesIdx.isEmpty()) {
                continue;
            }
            
            // 轮盘赌选择：根据加工时间选择新机器（时间短权重大）
            int selectedIdx = selectMachineIndexByRoulette(candidateProcessingTimes);
            int selectedMachineInAllList = candidateMachinesIdx.get(selectedIdx);
            int selectedRelativeIndex = selectedMachineInAllList + 1;  // 转换为1-based
            
            // 生成候选
            candidates.add(new MachineReassignmentCandidate(
                msIndex,
                ms[msIndex],
                selectedRelativeIndex,
                jobNo
            ));
        }
        
        if (candidates.isEmpty()) {
            return null;
        }
        
        // 随机选择一个候选执行
        MachineReassignmentCandidate selected = candidates.get(random.nextInt(candidates.size()));
        
        // 应用机器重分配
        individual.gene_MS[selected.msPosition] = selected.newRelativeIndex;
        
        // 标记受影响的作业
        delta.markJobAffected(selected.jobNo);
        
        return new Candidate(individual, delta, getName());
    }
    
    /**
     * 轮盘赌选择机器（基于加工时间，时间短权重大）
     */
    private int selectMachineIndexByRoulette(List<Double> processingTimes) {
        if (processingTimes.size() == 1) {
            return 0;
        }
        
        // 计算权重：使用倒数，使得加工时间短的机器权重大
        List<Double> weights = new ArrayList<>();
        double maxTime = Double.NEGATIVE_INFINITY;
        for (double time : processingTimes) {
            if (time > maxTime) {
                maxTime = time;
            }
        }
        
        // 权重 = (maxTime - time + 1)，使得时间短的机器权重大
        double totalWeight = 0;
        for (double time : processingTimes) {
            double weight = (maxTime - time + 1);
            weights.add(weight);
            totalWeight += weight;
        }
        
        // 轮盘赌选择
        double randomValue = random.nextDouble() * totalWeight;
        double cumulativeWeight = 0;
        
        for (int i = 0; i < weights.size(); i++) {
            cumulativeWeight += weights.get(i);
            if (randomValue <= cumulativeWeight) {
                return i;
            }
        }
        
        return weights.size() - 1;
    }
    
    /**
     * 识别关键路径（简化版本：返回空列表）
     * 注意：完整实现需要从operationMatrix回溯
     */
    private List<Operation> identifyCriticalPath(Operation[][] operationMatrix) {
        List<Operation> criticalPath = new ArrayList<>();
        
        // 1. 找到离散阶段makespan工序
        Operation lastOp = null;
        double maxEndTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < operationMatrix.length; i++) {
            if (operationMatrix[i] == null || operationMatrix[i].length == 0) {
                continue;
            }
            
            for (Operation op : operationMatrix[i]) {
                if (op != null && op.task >= 2 && op.endTime > maxEndTime) {
                    maxEndTime = op.endTime;
                    lastOp = op;
                }
            }
        }
        
        if (lastOp == null) {
            return criticalPath;
        }
        
        // 2. 反向追溯
        List<Operation> reversePath = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        Operation currentOp = lastOp;
        
        while (currentOp != null) {
            if (currentOp.task >= 2) {
                reversePath.add(currentOp);
            }
            
            String opKey = currentOp.jobNo + "_" + currentOp.task;
            visited.add(opKey);
            
            Operation criticalPred = findCriticalPredecessor(currentOp, operationMatrix, visited);
            currentOp = criticalPred;
        }
        
        // 3. 反转路径
        for (int i = reversePath.size() - 1; i >= 0; i--) {
            criticalPath.add(reversePath.get(i));
        }
        
        return criticalPath;
    }
    
    /**
     * 找到关键紧前工序
     */
    private Operation findCriticalPredecessor(Operation currentOp, Operation[][] operationMatrix, Set<String> visited) {
        List<Operation> candidates = new ArrayList<>();
        
        // 工艺紧前
        if (currentOp.task > 2) {
            Operation[] jobOps = operationMatrix[currentOp.jobNo];
            if (jobOps != null) {
                for (int i = 0; i < jobOps.length; i++) {
                    if (jobOps[i] != null && jobOps[i].task == currentOp.task && i > 0) {
                        for (int j = i - 1; j >= 0; j--) {
                            Operation prevOp = jobOps[j];
                            if (prevOp != null && prevOp.task >= 2) {
                                String key = prevOp.jobNo + "_" + prevOp.task;
                                if (!visited.contains(key)) {
                                    candidates.add(prevOp);
                                }
                                break;
                            }
                        }
                        break;
                    }
                }
            }
        }
        
        // 机器紧前
        Operation machinePred = null;
        double maxEndTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < operationMatrix.length; i++) {
            if (operationMatrix[i] == null) continue;
            
            for (Operation op : operationMatrix[i]) {
                if (op == null || op.task < 2) continue;
                
                String key = op.jobNo + "_" + op.task;
                if (visited.contains(key)) continue;
                
                if (op.machineNo == currentOp.machineNo && op.endTime <= currentOp.startTime) {
                    if (op.endTime > maxEndTime) {
                        maxEndTime = op.endTime;
                        machinePred = op;
                    }
                }
            }
        }
        
        if (machinePred != null) {
            candidates.add(machinePred);
        }
        
        // 选择关键紧前工序
        Operation criticalPred = null;
        double minGap = Double.POSITIVE_INFINITY;
        final double EPSILON = 1e-6;
        
        for (Operation candidate : candidates) {
            double gap = currentOp.startTime - candidate.endTime;
            if (gap < minGap && gap >= -EPSILON) {
                minGap = gap;
                criticalPred = candidate;
            }
        }
        
        return criticalPred;
    }
    
    
    /**
     * 机器重分配候选
     */
    private static class MachineReassignmentCandidate {
        int msPosition;
        int oldRelativeIndex;
        int newRelativeIndex;
        int jobNo;
        
        MachineReassignmentCandidate(int msPosition, int oldRelativeIndex, int newRelativeIndex, int jobNo) {
            this.msPosition = msPosition;
            this.oldRelativeIndex = oldRelativeIndex;
            this.newRelativeIndex = newRelativeIndex;
            this.jobNo = jobNo;
        }
    }
}
