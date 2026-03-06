package AlgorthmFrame.randomKeyGA;

import AlgorthmFrame.randomKeyGA.BLFPacker;
import AlgorthmFrame.randomKeyGA.RandomKeyChromosome;
import ProgramEntity.*;
import ProgramEntity.Machine.*;
import ProgramEntity.Solution;

import java.util.*;

/**
 * 随机密钥遗传算法的适应度评估器
 * 
 * 功能：
 * 1. 根据染色体解码零件序列、朝向、旋转、机器分配
 * 2. 使用BLF嵌套启发式进行零件嵌套
 * 3. 计算批次的打印时间
 * 4. 处理批处理工序（支撑去除）的排队
 * 5. 处理离散工序的排队
 * 6. 计算总makespan
 * 7. 构建Operation矩阵（用于甘特图）
 */
public class RandomKeyEvaluator {
    
    private Problem problem;
    private BLFPacker[] packers;  // 每台打印机对应一个packer
    
    // Fitness缩放系数（与原GA保持一致）
    private static final double FITNESS_SCALE = 100000000.0;
    
    /**
     * 构造函数
     * @param problem 问题实例
     */
    public RandomKeyEvaluator(Problem problem) {
        this.problem = problem;
        
        // 为每台打印机创建BLFPacker
        int printMachineCount = problem.getPrintMachineCount();
        packers = new BLFPacker[printMachineCount];
        
        Machine[] machines = problem.getMachines();
        for (int i = 0; i < printMachineCount; i++) {
            PrintMachine pm = (PrintMachine) machines[i];
            packers[i] = new BLFPacker(pm.L, pm.W, pm.H);
        }
    }
    
    /**
     * 评估染色体，计算makespan和fitness
     * 
     * @param chromosome 待评估的染色体
     * @return makespan值
     */
    public double evaluate(RandomKeyChromosome chromosome) {
        int jobCount = problem.getJobCount();
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        Item[] items = problem.getItems();
        Machine[] machines = problem.getMachines();
        
        // 初始化打印批次存储
        chromosome.printSolution = new List[problem.getMachineCount()];
        for (int i = 0; i < chromosome.printSolution.length; i++) {
            chromosome.printSolution[i] = new ArrayList<>();
        }
        
        // 初始化Operation矩阵（用于甘特图）
        int[] operationCountArr = problem.getOperationCountArr();
        chromosome.operationMatrix = new Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            chromosome.operationMatrix[i] = new Operation[operationCountArr[i]];
            for (int j = 0; j < operationCountArr[i]; j++) {
                chromosome.operationMatrix[i][j] = new Operation();
            }
        }
        
        // 1. 解码染色体：获取零件嵌套顺序
        int[] partSequence = chromosome.decodePartSequence();
        
        // 2. 按机器分配对零件进行分组
        Map<Integer, List<Integer>> machinePartGroups = new HashMap<>();
        for (int i = 0; i < jobCount; i++) {
            int machineNo = chromosome.getMachineAssignment(i);
            if (!machinePartGroups.containsKey(machineNo)) {
                machinePartGroups.put(machineNo, new ArrayList<>());
            }
            machinePartGroups.get(machineNo).add(i);
        }
        
        // 3. 为每台打印机生成嵌套批次
        // 记录每台打印机的批次列表和完工时间
        Map<Integer, List<Solution>> machineBatches = new HashMap<>();
        Map<Integer, Double> machineEndTimes = new HashMap<>();
        
        for (int machineNo = 1; machineNo <= printMachineCount; machineNo++) {
            machineEndTimes.put(machineNo, 0.0);
            machineBatches.put(machineNo, new ArrayList<>());
        }
        
        // 按优先队列处理批次（先完工的批次先进入后处理）
        PriorityQueue<BatchInfo> batchQueue = new PriorityQueue<>(
            Comparator.comparingDouble(b -> b.endTime)
        );
        
        // 对每台打印机进行嵌套
        for (Map.Entry<Integer, List<Integer>> entry : machinePartGroups.entrySet()) {
            int machineNo = entry.getKey();
            List<Integer> partIndices = entry.getValue();
            
            if (partIndices.isEmpty()) {
                continue;
            }
            
            // 构建该机器的零件序列（按染色体指定的全局顺序）
            List<Integer> machinePartSequence = new ArrayList<>();
            for (int idx : partSequence) {
                if (partIndices.contains(idx)) {
                    machinePartSequence.add(idx);
                }
            }
            
            // 提取零件数组和配置
            Item[] machineParts = new Item[machinePartSequence.size()];
            int[] orientations = new int[machinePartSequence.size()];
            int[] rotations = new int[machinePartSequence.size()];
            
            // 创建Item name到全局索引的映射（因为BLFPacker会将PlaceItem.name设置为Item.name）
            Map<String, Integer> itemNameToGlobalIndex = new HashMap<>();
            
            for (int i = 0; i < machinePartSequence.size(); i++) {
                int partIdx = machinePartSequence.get(i);
                machineParts[i] = items[partIdx];
                orientations[i] = chromosome.getOrientation(partIdx);
                rotations[i] = chromosome.partRotations[partIdx];
                
                // 建立Item name到全局索引的映射
                itemNameToGlobalIndex.put(items[partIdx].name, partIdx);
            }
            
            // 使用BLF进行嵌套
            int machineIndex = machineNo - 1;
            int[] localSequence = new int[machinePartSequence.size()];
            for (int i = 0; i < machinePartSequence.size(); i++) {
                localSequence[i] = i;  // 局部顺序已经按全局顺序排好了
            }
            
            List<Solution> batches = packers[machineIndex].packInBatches(
                machineParts, localSequence, orientations, rotations
            );
            
            // 保存打印批次信息到染色体（用于可视化）
            chromosome.printSolution[machineIndex] = new ArrayList<>(batches);
            
            // 计算每个批次的打印时间
            PrintMachine pm = (PrintMachine) machines[machineIndex];
            double currentTime = 0.0;
            
            for (Solution batch : batches) {
                double printTime = pm.prepareTime + pm.reCoatingTime * batch.maxG / pm.printH;
                batch.startTime = currentTime;
                batch.endTime = currentTime + printTime;
                
                // 记录批次中每个零件的打印工序（工序0）
                for (PlaceItem placeItem : batch.placeItemList) {
                    // 通过Item name获取全局索引
                    Integer partIdx = itemNameToGlobalIndex.get(placeItem.name);
                    
                    if (partIdx != null) {
                        // 记录打印工序
                        Operation printOp = chromosome.operationMatrix[partIdx][0];
                        printOp.jobNo = partIdx;
                        printOp.task = 0;  // 打印工序
                        printOp.machineNo = machineIndex;
                        printOp.startTime = batch.startTime;
                        printOp.endTime = batch.endTime;
                        printOp.id = "J" + partIdx + "T0";
                    }
                }
                
                // 加入批次队列
                // 需要传递Item name到全局索引的映射
                batchQueue.add(new BatchInfo(machineNo, batch, itemNameToGlobalIndex));
                
                currentTime = batch.endTime;
            }
            
            machineEndTimes.put(machineNo, currentTime);
            machineBatches.put(machineNo, batches);
        }
        
        // 4. 处理批处理工序（支撑去除、检测等）
        // 使用队列模拟FCFS（先到先服务）
        double[] batchMachineAvailableTime = new double[batchMachineCount];
        Arrays.fill(batchMachineAvailableTime, 0.0);
        
        Map<Integer, Double> partBatchEndTime = new HashMap<>();  // 记录每个零件批处理结束时间
        
        while (!batchQueue.isEmpty()) {
            BatchInfo batchInfo = batchQueue.poll();
            
            // 选择最早可用的批处理机器
            int selectedBatchMachine = 0;
            double minAvailableTime = batchMachineAvailableTime[0];
            for (int i = 1; i < batchMachineCount; i++) {
                if (batchMachineAvailableTime[i] < minAvailableTime) {
                    minAvailableTime = batchMachineAvailableTime[i];
                    selectedBatchMachine = i;
                }
            }
            
            // 批次到达时间 = 打印结束时间
            double arrivalTime = batchInfo.endTime;
            double startTime = Math.max(arrivalTime, batchMachineAvailableTime[selectedBatchMachine]);
            
            // 批处理时间
            BathchMachine bm = (BathchMachine) machines[printMachineCount + selectedBatchMachine];
            double batchProcessTime = bm.processingTime;
            double endTime = startTime + batchProcessTime;
            
            // 更新机器可用时间
            batchMachineAvailableTime[selectedBatchMachine] = endTime;
            
            // 记录批次中每个零件的批处理结束时间和工序信息（工序1）
            for (int partIdx : batchInfo.partIndices) {
                partBatchEndTime.put(partIdx, endTime);
                
                // 记录批处理工序
                if (partIdx < chromosome.operationMatrix.length && 
                    chromosome.operationMatrix[partIdx].length > 1) {
                    Operation batchOp = chromosome.operationMatrix[partIdx][1];
                    batchOp.jobNo = partIdx;
                    batchOp.task = 1;  // 批处理工序
                    batchOp.machineNo = printMachineCount + selectedBatchMachine;
                    batchOp.startTime = startTime;
                    batchOp.endTime = endTime;
                    batchOp.id = "J" + partIdx + "T1";
                }
            }
        }
        
        // 5. 处理离散工序（使用FCFS队列调度）
        int[][] operationToIndex = problem.getOperationToIndex();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int totalMachineCount = problem.getMachineCount();
        
        // 离散处理机器的可用时间（从打印机和批处理机器之后开始）
        int discreteMachineCount = totalMachineCount - printMachineCount - batchMachineCount;
        double[] discreteMachineAvailableTime = new double[discreteMachineCount];
        Arrays.fill(discreteMachineAvailableTime, 0.0);
        
        // 创建离散工序队列（按批处理完成时间排序，FCFS）
        PriorityQueue<DiscreteOperInfo> discreteQueue = new PriorityQueue<>(
            Comparator.comparingDouble(o -> o.arrivalTime)
        );
        
        // 收集所有离散工序
        for (int partIdx = 0; partIdx < jobCount; partIdx++) {
            int operCount = problem.getOperationCountArr()[partIdx];
            double prevEndTime = partBatchEndTime.getOrDefault(partIdx, 0.0);
            
            if (operCount > 2) {
                // 有离散工序（工序0=打印，工序1=批处理，工序2+为离散）
                for (int op = 2; op < operCount; op++) {
                    int opIndex = operationToIndex[partIdx][op];
                    
                    // 创建离散工序信息（不预先分配机器）
                    DiscreteOperInfo operInfo = new DiscreteOperInfo(
                        partIdx, op, -1, 0.0, prevEndTime
                    );
                    operInfo.opIndex = opIndex;  // 保存工序索引用于查找可用机器
                    discreteQueue.add(operInfo);
                }
            }
        }
        
        // 使用FCFS策略处理离散工序队列
        Map<Integer, Double> partCompletionTime = new HashMap<>();
        for (int i = 0; i < jobCount; i++) {
            partCompletionTime.put(i, partBatchEndTime.getOrDefault(i, 0.0));
        }
        
        while (!discreteQueue.isEmpty()) {
            DiscreteOperInfo operInfo = discreteQueue.poll();
            
            // 工序到达时间 = 前一工序完成时间
            double arrivalTime = partCompletionTime.get(operInfo.partIndex);
            
            // 搜索该工序的所有可用机器，选择最早能开始的
            int opIndex = operInfo.opIndex;
            int bestMachine = -1;
            double bestStartTime = Double.MAX_VALUE;
            double bestProcessTime = 0.0;
            
            for (int m = 0; m < proDesMatrix[opIndex].length; m++) {
                double processTime = proDesMatrix[opIndex][m];
                
                // 检查该机器是否可用（有效的处理时间）
                if (processTime > 0 && processTime < Double.MAX_VALUE) {
                    // 计算该机器的索引（离散机器从printMachineCount+batchMachineCount开始）
                    int globalMachineIdx = m;
                    
                    // 如果是离散处理机器
                    if (globalMachineIdx >= printMachineCount + batchMachineCount) {
                        int discreteMachineIdx = globalMachineIdx - (printMachineCount + batchMachineCount);
                        
                        if (discreteMachineIdx >= 0 && discreteMachineIdx < discreteMachineCount) {
                            // 计算在该机器上的最早开始时间
                            double machineAvailable = discreteMachineAvailableTime[discreteMachineIdx];
                            double startTime = Math.max(arrivalTime, machineAvailable);
                            
                            // 选择最早能开始的机器
                            if (startTime < bestStartTime) {
                                bestStartTime = startTime;
                                bestMachine = discreteMachineIdx;
                                bestProcessTime = processTime;
                            }
                        }
                    }
                }
            }
            
            // 如果找到了可用机器，分配工序
            if (bestMachine >= 0 && bestMachine < discreteMachineCount) {
                double endTime = bestStartTime + bestProcessTime;
                
                // 更新机器可用时间
                discreteMachineAvailableTime[bestMachine] = endTime;
                
                // 更新零件完工时间
                partCompletionTime.put(operInfo.partIndex, endTime);
                
                // 将该零件的后续工序的到达时间也更新（如果有多个离散工序）
                // 后续工序会在下一轮处理时读取更新后的partCompletionTime
                
                // 记录离散工序到Operation矩阵
                int partIdx = operInfo.partIndex;
                int operIdx = operInfo.operIndex;
                int actualMachineNo = printMachineCount + batchMachineCount + bestMachine;
                
                if (partIdx < chromosome.operationMatrix.length && 
                    operIdx < chromosome.operationMatrix[partIdx].length) {
                    Operation discreteOp = chromosome.operationMatrix[partIdx][operIdx];
                    discreteOp.jobNo = partIdx;
                    discreteOp.task = operIdx;  // 工序编号
                    discreteOp.machineNo = actualMachineNo;
                    discreteOp.startTime = bestStartTime;
                    discreteOp.endTime = endTime;
                    discreteOp.id = "J" + partIdx + "T" + operIdx;
                }
            }
        }
        
        // 计算最大makespan
        double maxMakespan = 0.0;
        for (double completionTime : partCompletionTime.values()) {
            maxMakespan = Math.max(maxMakespan, completionTime);
        }
        
        // 6. 设置染色体的makespan和fitness
        chromosome.setMakespan(maxMakespan);
        chromosome.setFitness(FITNESS_SCALE / maxMakespan);
        
        return maxMakespan;
    }
    
    /**
     * 批次信息（用于优先队列）
     */
    private static class BatchInfo {
        int machineNo;
        Solution batch;
        List<Integer> partIndices;  // 该批次包含的零件全局索引
        double endTime;
        
        public BatchInfo(int machineNo, Solution batch, Map<String, Integer> itemNameToGlobalIndex) {
            this.machineNo = machineNo;
            this.batch = batch;
            this.partIndices = new ArrayList<>();
            
            // 从batch中提取零件的全局索引（通过Item name映射）
            for (PlaceItem pi : batch.placeItemList) {
                Integer globalIdx = itemNameToGlobalIndex.get(pi.name);
                if (globalIdx != null) {
                    this.partIndices.add(globalIdx);
                }
            }
            
            this.endTime = batch.endTime;
        }
    }
    
    /**
     * 离散工序信息（用于FCFS队列）
     */
    private static class DiscreteOperInfo {
        int partIndex;       // 零件索引
        int operIndex;       // 工序索引（在零件中的工序编号）
        int opIndex;         // 工序在operationToIndex中的索引（用于查找可用机器）
        int machineIndex;    // 机器索引（已弃用，改为动态选择）
        double processTime;  // 处理时间（已弃用，改为动态选择）
        double arrivalTime;  // 到达时间（前一工序完成时间）
        
        public DiscreteOperInfo(int partIndex, int operIndex, int machineIndex, 
                                double processTime, double arrivalTime) {
            this.partIndex = partIndex;
            this.operIndex = operIndex;
            this.machineIndex = machineIndex;
            this.processTime = processTime;
            this.arrivalTime = arrivalTime;
        }
    }
}

