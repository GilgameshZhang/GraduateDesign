package ProblemFrame;

import ProgramEntity.*;
import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Solution;

import java.util.*;

public class CaculateFitness {
    /**
     * @return int[2] machineNoAndTimeArr machine index and time cost
     */
    //todo 修改寻找对应加工时间的方法
   public static double[] getMachineNoAndTime(Problem input, int MS[], int jobNo, int operationNo, int i) {
    double[][] proDesMatrix = input.getProDesMatrix();
    int operationToIndex[][] = input.getOperationToIndex();
    int tempCount = 0;
    int totaloperNo = operationToIndex[jobNo][operationNo];// 工序编号
    double machineTimeArr[] = proDesMatrix[totaloperNo];// 工序在备选机器上的加工时间
    double[] machineNoAndTimeArr = new double[2];
    int machineNo = MS[i];// 工序对应的机器编号（备选机器，1-based）
    machineNoAndTimeArr[0] = machineNo;
    // 修正：用机器编号-1作为索引（转为0-based）
    machineNoAndTimeArr[1] = proDesMatrix[totaloperNo][machineNo - 1];
    return machineNoAndTimeArr;
}

    /**
     * @param operationMatrix the operation description of the scheduling problem
     */
    public static void initOperationMatrix(Operation[][] operationMatrix) {
        int i = 0, j = 0;
        for (i = 0; i < operationMatrix.length; i++) {
            for (j = 0; j < operationMatrix[i].length; j++)
                operationMatrix[i][j].initOperation();
        }
    }

    public class Time {
        double start;
        double end;
        int type;// 0为工作,1为空闲。

        Time(double s, double e, int t) {
            this.start = s;
            this.end = e;
            this.type = t;
        }
    }

    /**
     * 计算一条染色体（一个可行的调度）所耗费的最大时间
     *
     * @param input the time and order information of the problem
     * @return the fitness of a sheduling
     */
    public double evaluate(Chromosome chromosome, Problem input, Operation[][] operationMatrix) {
        int jobCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        int batchMachineCount = input.getBatchMachineCount();
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int machineCount = input.getMachineCount();
        initOperationMatrix(operationMatrix);
        
        // 初始化printSolution数组（如果还未初始化）
        if (chromosome.printSolution == null) {
            chromosome.printSolution = new List[machineCount];
            for (int i = 0; i < machineCount; i++) {
                chromosome.printSolution[i] = new ArrayList<>();
            }
        }
        int[] operNoOfEachJob = new int[jobCount];// 当前处理到工件的工序No
        Arrays.fill(operNoOfEachJob, 0);
        ArrayList<Time> machTimes[] = new ArrayList[machineCount];// 机器的时间段
        for (int i = 0; i < machineCount; i++) {
            machTimes[i] = new ArrayList<>();
            machTimes[i].add(new Time(0.0, Double.MAX_VALUE, 0));
        }
        
        // ===== 修复非法的染色体（在评估前确保染色体有效）=====
        
        // 1. 修复打印阶段的OS：确保每个工件恰好出现一次
        boolean[] jobAppeared = new boolean[jobCount];
        Arrays.fill(jobAppeared, false);
        ArrayList<Integer> missingJobs = new ArrayList<>();
        ArrayList<Integer> duplicatePositions = new ArrayList<>();
        
        for (int i = 0; i < jobCount; i++) {
            int job = chromosome.gene_OS[i];
            if (job < 0 || job >= jobCount) {
                // 非法工件号
                duplicatePositions.add(i);
            } else if (jobAppeared[job]) {
                // 重复的工件
                duplicatePositions.add(i);
            } else {
                jobAppeared[job] = true;
            }
        }
        
        // 找出缺失的工件
        for (int j = 0; j < jobCount; j++) {
            if (!jobAppeared[j]) {
                missingJobs.add(j);
            }
        }
        
        // 用缺失的工件填充重复位置
        int missingIdx = 0;
        for (int pos : duplicatePositions) {
            if (missingIdx < missingJobs.size()) {
                chromosome.gene_OS[pos] = missingJobs.get(missingIdx++);
            }
        }
        
        // 2. 修复打印阶段的MS：必须在[1, printMachineCount]范围内
        for (int i = 0; i < jobCount; i++) {
            if (chromosome.gene_MS[i] < 1 || chromosome.gene_MS[i] > printMachineCount) {
                chromosome.gene_MS[i] = 1 + (i % printMachineCount); // 轮流分配到各打印机
            }
        }
        
        // 3. 离散阶段的MS必须在[printMachineCount+batchMachineCount+1, machineCount]范围内
        int discreteMachineStart = printMachineCount + batchMachineCount + 1;
        for (int i = jobCount; i < chromosome.gene_MS.length; i++) {
            if (chromosome.gene_MS[i] < discreteMachineStart || chromosome.gene_MS[i] > machineCount) {
                chromosome.gene_MS[i] = discreteMachineStart; // 使用第一台离散机器
            }
        }
        // ===== 修复完成 =====
        
        int jobNo = 0;
        int operNo = 0;
        double operationTime = 0;
        int machineNo = 0;
        double machineNoAndTimeArr[] = new double[2];
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        //计算第一道工序的加工时间,统计机器分配情况
        for (int i = 0; i < jobCount; i++) {
            jobNo = chromosome.gene_OS[i];// 工件名
            // 注意：这里只是统计，不增加operNoOfEachJob
            machineNo = chromosome.gene_MS[i];// 机器编号
            if (!machineMap.containsKey(machineNo)) {
                machineMap.put(machineNo, new ArrayList<>());
            }
            machineMap.get(machineNo).add(jobNo);
        }
        PriorityQueue<Solution> pq = new PriorityQueue<>(new Comparator<Solution>() {
            @Override
            public int compare(Solution o1, Solution o2) {
                if (o1.endTime > o2.endTime) {
                    return 1;
                } else if (o1.endTime < o2.endTime) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        //遍历map调用SkyLinePacking
        for (Map.Entry<Integer, List<Integer>> entry : machineMap.entrySet()) {
            int machineNo1 = entry.getKey(); // 1-based机器号（已在上面修复，此处必定合法）
            int machIdx1 = machineNo1 - 1;   // 0-based数组索引
            List<Integer> jobList = entry.getValue();
            // 获取分配给当前打印机的工件列表
            Item[] itemsForMachine = new Item[jobList.size()];
            for (int i = 0; i < jobList.size(); i++) {
                jobNo = jobList.get(i);
                itemsForMachine[i] = items[jobNo];
            }
            PrintMachine printMachine = (PrintMachine) machines[machIdx1];
            // 修正：只打包分配给当前打印机的工件，而不是所有工件
            List<Solution> solutions = new SkyLinePacking(printMachine.L, printMachine.W, itemsForMachine, true).packings();
            chromosome.printSolution[machIdx1].addAll(solutions);

            int count = 1;
            for (ProgramEntity.Solution solution : solutions) {
                // 计算最小的分批数
                Batch batch = new Batch(count++, solution);
                int size = machTimes[machIdx1].size();
                double start = machTimes[machIdx1].get(size - 1).start;
                double end = start + printMachine.prepareTime + printMachine.reCoatingTime * solution.maxG / printMachine.printH;
                solution.startTime = start;
                solution.endTime = end;
                //将批次加入批次队列
                pq.add(solution);
                // 更新机器时间段
                ArrayList<Time> t = new ArrayList<>();
                t.add(new Time(start, end, 1));
                t.add(new Time(end, Double.MAX_VALUE, 0));
                machTimes[machIdx1].remove(size - 1);
                machTimes[machIdx1].addAll(size - 1, t);
                //为批次内的每个工件进行打印工序时间赋值
                for (int i = 0; i < solution.placeItemList.size(); i++) {
                    PlaceItem placeItem = solution.placeItemList.get(i);
                    int jobNo1 = Integer.parseInt(placeItem.name);
                    int currentOperNo = operNoOfEachJob[jobNo1];// 获取当前工序号（打印工序）
                    operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                    operationMatrix[jobNo1][currentOperNo].machineNo = machineNo1; // 存储1-based机器号
                    operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                    operationMatrix[jobNo1][currentOperNo].startTime = start;
                    operationMatrix[jobNo1][currentOperNo].endTime = end;
                    operNoOfEachJob[jobNo1]++;// 打印工序完成，工序号+1
                }
            }
        }
        //按照规则安排这一批次的加工去下一个机器
        while (!pq.isEmpty()) {
            Solution solution = pq.poll();
            //搜索能最早开始加工的批处理机器，将该批次分配给他
            //批处理开始时间 = max(机器空闲时间, 批次打印完成时间)
            //如果开始时间相同，优先选择加工时间短的机器
            int minIndex = printMachineCount;
            double minStartTime = Double.MAX_VALUE;
            double minProcessingTime = Double.MAX_VALUE;
            
            for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
                int size = machTimes[i].size();
                double machineAvailTime = machTimes[i].get(size - 1).start;
                // 批处理开始时间 = max(机器可用时间, 批次打印完成时间)
                double startTime = Math.max(machineAvailTime, solution.endTime);
                BathchMachine bm = (BathchMachine)machines[i];
                
                if (startTime < minStartTime) {
                    // 开始时间更早，选择这台机器
                    minIndex = i;
                    minStartTime = startTime;
                    minProcessingTime = bm.processingTime;
                } else if (startTime == minStartTime && bm.processingTime < minProcessingTime) {
                    // 开始时间相同，选择加工时间更短的机器
                    minIndex = i;
                    minProcessingTime = bm.processingTime;
                }
            }
            
            int size = machTimes[minIndex].size();
            BathchMachine bathchMachine = (BathchMachine)machines[minIndex];
            double start = minStartTime;
            double end = start + bathchMachine.processingTime;
            // 更新机器时间段
            ArrayList<Time> t = new ArrayList<>();
            t.add(new Time(start, end, 1));
            t.add(new Time(end, Double.MAX_VALUE, 0));
            machTimes[minIndex].remove(size - 1);  // 修正：应该用minIndex
            machTimes[minIndex].addAll(size - 1, t);  // 修正：应该用minIndex
            //为批次内的每个工件进行批处理工序时间赋值
            for (int i = 0; i < solution.placeItemList.size(); i++) {
                PlaceItem placeItem = solution.placeItemList.get(i);
                int jobNo1 = Integer.parseInt(placeItem.name);
                int currentOperNo = operNoOfEachJob[jobNo1];// 获取当前工序号（批处理工序）
                operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                operationMatrix[jobNo1][currentOperNo].machineNo = minIndex + 1; // 存储1-based机器号
                operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                operationMatrix[jobNo1][currentOperNo].startTime = start;
                operationMatrix[jobNo1][currentOperNo].endTime = end;
                operNoOfEachJob[jobNo1]++;// 批处理工序完成，工序号+1
            }
        }
        // ==================== 离散工序阶段（半主动式解码/前插方式）====================
        // 染色体结构：[打印工序(jobCount个)] + [离散工序]
        // 批处理工序不在染色体中体现，由上面的优先队列自动安排
        // 
        // 半主动式解码策略：
        // 1. 从前往后遍历机器的所有时间段
        // 2. 对于每个空闲时间段，检查是否能安排当前工序
        // 3. 如果找到合适的空闲时间段（前插），立即安排；否则继续检查下一个空闲时间段
        // 4. 这样可以充分利用机器的空闲时间，减少总完工时间
        
        for (int i = jobCount; i < chromosome.gene_OS.length; i++) {
            jobNo = chromosome.gene_OS[i];// 工件名
            int currentOperNo = operNoOfEachJob[jobNo];
    
            // 检查是否还有工序需要处理
            if (currentOperNo >= input.getOperationCountArr()[jobNo]) {
                continue; // 跳过已经处理完的工件
            }
            operNo = currentOperNo;
    
            // 找到这道工序对应的机器编号以及加工时间
            machineNoAndTimeArr = getMachineNoAndTime(input, chromosome.gene_MS, jobNo, operNo, i);
            machineNo = (int)machineNoAndTimeArr[0];
            operationTime = machineNoAndTimeArr[1];

            // 工序的最早可开始时间 = 前道工序的完成时间（工艺约束）
            double earliestStartTime = operationMatrix[jobNo][operNo - 1].endTime;
            operationMatrix[jobNo][operNo].aStartTime = earliestStartTime;
            operationMatrix[jobNo][operNo].machineNo = machineNo;
            operationMatrix[jobNo][operNo].jobNo = jobNo;
            operationMatrix[jobNo][operNo].task = operNo;
            
            // machineNo是1-based机器号，machTimes是0-based数组，需要-1
            int machIdx = machineNo - 1;
            
            // ===== 半主动式解码：从前往后遍历所有时间段，找到第一个可用的空闲时间段 =====
            for (int j = 0; j < machTimes[machIdx].size(); j++) {
                Time timeSlot = machTimes[machIdx].get(j);
                
                // 只考虑空闲时间段（type == 0）
                if (timeSlot.type != 0) {
                    continue;
                }
                
                // 计算在这个空闲时间段内，工序的开始时间
                // 开始时间 = max(工序最早可开始时间, 空闲时间段开始时间)
                double start = Math.max(earliestStartTime, timeSlot.start);
                double end = start + operationTime;
                
                // 检查是否能在这个空闲时间段内完成工序（前插条件）
                // 条件：工序结束时间 <= 空闲时间段结束时间
                if (end <= timeSlot.end) {
                    // ===== 找到合适的空闲时间段，安排工序 =====
                    operationMatrix[jobNo][operNo].startTime = start;
                    operationMatrix[jobNo][operNo].endTime = end;
                    
                    // 更新机器时间段（分割空闲时间段）
                    ArrayList<Time> newTimeSlots = new ArrayList<>();
                    
                    // 如果工序开始时间晚于空闲时间段开始时间，前面保留空闲时间段
                    // （供后续工序可能的前插使用）
                    if (start > timeSlot.start) {
                        newTimeSlots.add(new Time(timeSlot.start, start, 0)); // 空闲
                    }
                    
                    // 添加工序占用的时间段
                    newTimeSlots.add(new Time(start, end, 1)); // 忙碌
                    
                    // 如果工序结束时间早于空闲时间段结束时间，后面保留空闲时间段
                    if (end < timeSlot.end) {
                        newTimeSlots.add(new Time(end, timeSlot.end, 0)); // 空闲
                    }
                    
                    // 替换原时间段
                    machTimes[machIdx].remove(j);
                    machTimes[machIdx].addAll(j, newTimeSlots);
                    
                    break; // 找到位置，退出循环
                }
                // 如果这个空闲时间段不够，继续检查下一个空闲时间段
            }
            operNoOfEachJob[jobNo]++;
        }

        double longestTime = 0.0;
        for (int i = 0; i < machineCount; i++)
            longestTime = Math.max(machTimes[i].get(machTimes[i].size() - 1).start, longestTime);

        return longestTime;
    }

/**
 * 计算一条染色体（一个可行的调度）所耗费的最大时间
 *
 * @param input  the time and order information of the problem
 * @return the fitness of a sheduling
 */
//        public static int evaluate1 (Chromosome chromosome, Problem input, Operation[][]operationMatrix){
//            int jobCount = input.getJobCount();
//            int machineCount = input.getMachineCount();
//            initOperationMatrix(operationMatrix);
//
//            int span = -1;
//            int[] operNoOfEachJob = new int[jobCount];// 当前处理到工件的工序No
//            Arrays.fill(operNoOfEachJob, 0);
//
//            int[] machFreeTime = new int[machineCount];// 机器最早空闲时间
//            Arrays.fill(machFreeTime, 0);
//
//            int jobNo = 0;
//            int operNo = 0;
//            int operationTime = 0;
//            int machineNo = 0;
//            int machineNoAndTimeArr[] = new int[2];
//
//            for (int i = 0; i < chromosome.gene_OS.length; i++) {
//                jobNo = chromosome.gene_OS[i];// 工件名
//                operNo = operNoOfEachJob[jobNo]++;// 当前工件操作所在的工序数
//
//                machineNoAndTimeArr = getMachineNoAndTime(input, chromosome.gene_MS, jobNo, operNo);
//                machineNo = machineNoAndTimeArr[0];
//                operationTime = machineNoAndTimeArr[1];
//
////			System.out.println("i=" + i + ",JobNo " + jobNo + ",OperNo " + operNo + ",machineNo " + machineNo
////					+ ",operationTime" + operationTime);
//
//                if (operNo == 0) {
//                    // 如果是第一个，开始时间
//                    operationMatrix[jobNo][operNo].jobNo = jobNo;
//                    operationMatrix[jobNo][operNo].machineNo = machineNo;
//                    operationMatrix[jobNo][operNo].task = operNo;
//                    operationMatrix[jobNo][operNo].startTime = machFreeTime[machineNo];
//                    operationMatrix[jobNo][operNo].endTime = operationMatrix[jobNo][operNo].startTime + operationTime;
//                } else {
//                    operationMatrix[jobNo][operNo].jobNo = jobNo;
//                    operationMatrix[jobNo][operNo].machineNo = machineNo;
//                    operationMatrix[jobNo][operNo].task = operNo;
//                    operationMatrix[jobNo][operNo].startTime = Math.max(operationMatrix[jobNo][operNo - 1].endTime,
//                            machFreeTime[machineNo]);
//                    operationMatrix[jobNo][operNo].endTime = operationMatrix[jobNo][operNo].startTime + operationTime;
//                }
//
//                machFreeTime[machineNo] = operationMatrix[jobNo][operNo].endTime;
//                if (operationMatrix[jobNo][operNo].endTime > span) {
//                    span = operationMatrix[jobNo][operNo].endTime;
//                }
//            }
//
//            return span;
//        }
}
