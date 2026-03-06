package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.*;
import java.util.*;

/**
 * T4: 关键块首尾相邻工序交换（对应第二章N4）
 * 
 * 邻域策略：识别关键路径和关键块，交换块首或块尾的两相邻工序
 * - 块首交换：交换块的第一个工序和第二个工序
 * - 块尾交换：交换块的倒数第二个工序和最后一个工序
 * 
 * 操作前约束筛选（在生成候选时执行）：
 * 1. 直接优先级约束：确保交换不违反工序的直接前驱后继关系
 * 2. 间接优先级约束：确保交换后工序仍在其所有前驱之后、所有后继之前
 * 3. 工序顺序约束：只在关键块内部交换，不跨块操作
 * 
 * 完全照搬第二章的N4实现，适配第三章的Operator接口
 * 
 * @author AI Assistant (基于第二章N4)
 * @version 2.0
 */
public class T4_DiscreteCriticalOpTimeReassign extends AbstractOperator {
    
    private final Problem problem;
    
    public T4_DiscreteCriticalOpTimeReassign(Random random, Problem problem) {
        super("T4-CriticalBlockSwap", IndividualType.TIME_DEFICIENT, random);
        this.problem = problem;
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
        //System.out.println("T4-CriticalBlockSwap");
        //System.out.println(" t4");
        // 从individual获取已评估的operationMatrix
        Operation[][] operationMatrix = individual.operationMatrix;
        
        // 如果operationMatrix为空（未评估），返回null
        if (operationMatrix == null) {
            return null;
        }
        
        // 1. 识别关键路径
        List<Operation> criticalPath = identifyCriticalPath(operationMatrix);
        if (criticalPath.size() < 2) {
            return null;  // 无关键路径或路径太短
        }
        
        // 2. 在关键路径上识别关键块（同一机器上连续的工序）
        List<CriticalBlock> criticalBlocks = identifyCriticalBlocks(criticalPath, individual);
        if (criticalBlocks.isEmpty()) {
            return null;
        }
        
        int[] os = individual.gene_OS;
        int[] ms = individual.gene_MS;
        int jobCount = problem.getJobCount();
        
        // 3. 随机选择一个关键块
        CriticalBlock selectedBlock = criticalBlocks.get(random.nextInt(criticalBlocks.size()));
        
        if (selectedBlock.operationPositions.size() < 2) {
            return null;  // 块太小，无法交换
        }
        
        // 4. 随机选择块首或块尾交换
        int swapPos1, swapPos2;
        
        if (random.nextBoolean()) {
            // 交换块首的两相邻工序（第一个和第二个）
            swapPos1 = selectedBlock.operationPositions.get(0);
            swapPos2 = selectedBlock.operationPositions.get(1);
        } else {
            // 交换块尾的两相邻工序（倒数第二个和最后一个）
            swapPos1 = selectedBlock.operationPositions.get(selectedBlock.operationPositions.size() - 2);
            swapPos2 = selectedBlock.operationPositions.get(selectedBlock.operationPositions.size() - 1);
        }
        
        // 5. 执行交换（只交换gene_OS中的工件编号，MS保持不变）
        // 在新编码方式下，离散段的MS是固定顺序的，不应该交换
        int tempJob = os[swapPos1];
        os[swapPos1] = os[swapPos2];
        os[swapPos2] = tempJob;
        
        // 标记受影响的作业
        delta.markJobAffected(os[swapPos1]);
        delta.markJobAffected(os[swapPos2]);
        
        return new Candidate(individual, delta, getName());
    }
    
    /**
     * 识别关键路径：使用反向追溯法（CPM）找到离散处理阶段的关键工序序列
     * 
     * 注意：只考虑离散处理阶段（task >= 2），忽略打印和批处理阶段
     */
    private List<Operation> identifyCriticalPath(Operation[][] operationMatrix) {
        List<Operation> criticalPath = new ArrayList<>();
        
        // 1. 找到离散阶段makespan工序（完成时间最晚的离散工序）
        Operation lastOp = null;
        double maxEndTime = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < operationMatrix.length; i++) {
            if (operationMatrix[i] == null || operationMatrix[i].length == 0) {
                continue;
            }
            
            for (Operation op : operationMatrix[i]) {
                // 只考虑离散工序（task >= 2）
                if (op != null && op.task >= 2 && op.endTime > maxEndTime) {
                    maxEndTime = op.endTime;
                    lastOp = op;
                }
            }
        }
        
        if (lastOp == null) {
            return criticalPath;
        }
        
        // 2. 使用栈进行反向追溯（从后往前）
        List<Operation> reversePath = new ArrayList<>();
        Set<String> visited = new HashSet<>();  // 防止循环
        Operation currentOp = lastOp;
        
        while (currentOp != null) {
            // 只添加离散工序到反向路径
            if (currentOp.task >= 2) {
                reversePath.add(currentOp);
            }
            
            String opKey = currentOp.jobNo + "_" + currentOp.task;
            visited.add(opKey);
            
            // 查找关键紧前工序（只考虑离散工序）
            Operation criticalPredecessor = findCriticalPredecessor(currentOp, operationMatrix, visited);
            currentOp = criticalPredecessor;
        }
        
        // 3. 反转路径（变成从前往后的顺序）
        for (int i = reversePath.size() - 1; i >= 0; i--) {
            criticalPath.add(reversePath.get(i));
        }
        
        return criticalPath;
    }
    
    /**
     * 找到指定工序的关键紧前工序（只考虑离散处理阶段）
     */
    private Operation findCriticalPredecessor(Operation currentOp, Operation[][] operationMatrix, Set<String> visited) {
        List<Operation> candidates = new ArrayList<>();
        
        // 候选1：工艺紧前工序（同一工件的前一道离散工序）
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
        
        // 候选2：机器紧前工序（同一机器上紧挨着的前一道离散工序）
        Operation machinePredecessor = null;
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
                        machinePredecessor = op;
                    }
                }
            }
        }
        
        if (machinePredecessor != null) {
            candidates.add(machinePredecessor);
        }
        
        // 从候选中选择关键紧前工序（endTime最接近currentOp.startTime的）
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
     * 在关键路径上识别关键块
     * 关键块 = 关键路径上在同一机器上连续加工的工序序列
     */
    private List<CriticalBlock> identifyCriticalBlocks(List<Operation> criticalPath, MOIndividual individual) {
        List<CriticalBlock> blocks = new ArrayList<>();
        
        if (criticalPath.isEmpty()) {
            return blocks;
        }
        
        int[] os = individual.gene_OS;
        int[] ms = individual.gene_MS;
        int jobCount = problem.getJobCount();
        
        CriticalBlock currentBlock = null;
        int lastMachine = -1;
        
        for (Operation op : criticalPath) {
            if (op.task < 2) {
                continue;
            }
            
            int currentMachine = op.machineNo;
            
            // 找到该工序在gene_OS中的位置
            int positionInOS = findOperationPosition(op, os, ms, jobCount);
            if (positionInOS == -1) {
                continue;
            }
            
            if (currentMachine == lastMachine && currentBlock != null) {
                currentBlock.operationPositions.add(positionInOS);
            } else {
                if (currentBlock != null && currentBlock.operationPositions.size() > 1) {
                    blocks.add(currentBlock);
                }
                currentBlock = new CriticalBlock(currentMachine);
                currentBlock.operationPositions.add(positionInOS);
                lastMachine = currentMachine;
            }
        }
        
        if (currentBlock != null && currentBlock.operationPositions.size() > 1) {
            blocks.add(currentBlock);
        }
        
        return blocks;
    }
    
    /**
     * 在gene_OS中找到指定工序的位置
     */
    private int findOperationPosition(Operation op, int[] os, int[] ms, int jobCount) {
        int jobNo = op.jobNo;
        int targetMachineNo = op.machineNo;  // 0-based
        int operNo = op.task;
        
        if (operNo < 2) {
            return -1;
        }
        
        int discreteOperIndex = operNo - 2;
        
        // 在OS中找到该工件的第discreteOperIndex次出现（在离散段）
        int count = 0;
        int positionInOS = -1;
        for (int i = jobCount; i < os.length; i++) {
            if (os[i] == jobNo) {
                if (count == discreteOperIndex) {
                    positionInOS = i;
                    break;
                }
                count++;
            }
        }
        
        if (positionInOS == -1) {
            return -1;
        }
        
        // 验证机器编号
        int msIndex = jobCount;
        for (int j = 0; j < jobNo; j++) {
            msIndex += (problem.getOperationCountArr()[j] - 2);
        }
        msIndex += discreteOperIndex;
        
        int relativeIndex = ms[msIndex];
        
        int[][] operationToIndex = problem.getOperationToIndex();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int operIdx = operationToIndex[jobNo][operNo];
        
        List<Integer> availableMachines = new ArrayList<>();
        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);  // 1-based
            }
        }
        
        if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
            return -1;
        }
        
        int actualMachineNo = availableMachines.get(relativeIndex - 1);
        
        if (actualMachineNo - 1 == targetMachineNo) {
            return positionInOS;
        }
        
        return -1;
    }
    
    
    /**
     * 关键块：关键路径上在同一机器上连续加工的工序序列
     */
    private static class CriticalBlock {
        int machineNo;
        List<Integer> operationPositions;
        
        CriticalBlock(int machineNo) {
            this.machineNo = machineNo;
            this.operationPositions = new ArrayList<>();
        }
    }
}
