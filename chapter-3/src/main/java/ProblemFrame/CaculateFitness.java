package ProblemFrame;


import ProgramEntity.Item;
import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.PlaceItem;
import ProgramEntity.Problem;

import java.util.*;

public class CaculateFitness {
    
    /**
     * @return int[2] machineNoAndTimeArr machine index and time cost
     */
    //修改寻找对应加工时间的方法
   public static double[] getMachineNoAndTime(Problem input, int MS[], int jobNo, int operationNo, int msIndex) {
    double[][] proDesMatrix = input.getProDesMatrix();
    int operationToIndex[][] = input.getOperationToIndex();
    int totaloperNo = operationToIndex[jobNo][operationNo];// 工序编号
    double machineTimeArr[] = proDesMatrix[totaloperNo];// 工序在备选机器上的加工时间
    double[] machineNoAndTimeArr = new double[2];
    
    // 获取加工时间
    double operationTime = 0;
    int machineNo = 0;
    
    if (operationNo == 0) {
        // 打印工序：MS[msIndex]存储实际的机器编号（1-based）
        machineNo = MS[msIndex];
        operationTime = 0;  // 时间由装箱算法决定
    } else if (operationNo == 1) {
        // 批处理工序：时间由批处理机器决定（不应该在这里被调用）
        System.out.println("⚠️ 警告：批处理工序不应该调用getMachineNoAndTime！");
        System.out.println("  jobNo=" + jobNo + ", operationNo=" + operationNo);
        machineNo = MS[msIndex];
        operationTime = 0;
    } else {
        // ✅ 离散工序：从MS的固定位置读取相对索引
        // msIndex已经由调用者根据工件编号和工序编号计算好了
        int relativeIndex = MS[msIndex];  // 相对索引：1表示第1个可选机器，2表示第2个...
        
        // 找到所有可用机器
        List<Integer> availableMachines = new ArrayList<>();
        for (int k = 0; k < machineTimeArr.length; k++) {
            if (machineTimeArr[k] > 0 && machineTimeArr[k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);  // 存储实际机器编号（1-based）
            }
        }
        
        // 验证相对索引是否有效
        if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
            System.out.println("❌ 错误：相对索引超出范围！");
            System.out.println("  工件J" + jobNo + ", 工序" + operationNo + " (离散工序" + (operationNo-1) + ")");
            System.out.println("  MS索引: " + msIndex + ", 相对索引: " + relativeIndex + " (应该在1到" + availableMachines.size() + "之间)");
            System.out.print("  该工序的可用机器: ");
            for (int k = 0; k < availableMachines.size(); k++) {
                System.out.print("M" + availableMachines.get(k) + " ");
            }
            System.out.println();
            
            // 修复：使用第一个可选机器
            relativeIndex = 1;
        }
        
        // 根据相对索引获取实际机器编号
        machineNo = availableMachines.get(relativeIndex - 1);  // relativeIndex是1-based，转为0-based索引
        
        // 从proDesMatrix中获取加工时间
        operationTime = machineTimeArr[machineNo - 1];  // machineNo是1-based，数组索引是0-based
        
        // 调试：如果加工时间为0，输出详细信息
        if (operationTime == 0 || operationTime == Double.MAX_VALUE) {
            System.out.println("❌ 错误：离散工序的加工时间无效！");
            System.out.println("  工件J" + jobNo + ", 工序" + operationNo + " (离散工序" + (operationNo-1) + ")");
            System.out.println("  MS索引: " + msIndex + ", 相对索引: " + relativeIndex + ", 实际机器: M" + machineNo);
            System.out.println("  加工时间: " + operationTime);
        }
    }
    
    machineNoAndTimeArr[0] = machineNo;  // 实际机器编号（1-based）
    machineNoAndTimeArr[1] = operationTime;  // 加工时间
    return machineNoAndTimeArr;
}

    /**
     * @param operationMatrix the operation description of the scheduling problem
     */
    public static void initOperationMatrix(Operation[][] operationMatrix) {
        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++)
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
        // 每次evaluate都重新初始化printSolution，避免重复累积
        chromosome.printSolution = new List[machineCount];
        for (int i = 0; i < machineCount; i++) {
            chromosome.printSolution[i] = new ArrayList<>();
        }
        int[] operNoOfEachJob = new int[jobCount];// 当前处理到工件的工序No
        Arrays.fill(operNoOfEachJob, 0);
        ArrayList<Time> machTimes[] = new ArrayList[machineCount];// 机器的时间段
        for (int i = 0; i < machineCount; i++) {
            machTimes[i] = new ArrayList<>();
            machTimes[i].add(new Time(0.0, Double.MAX_VALUE, 0));
        }
        int jobNo = 0;
        int operNo = 0;
        double operationTime = 0;
        int machineNo = 0;
        double machineNoAndTimeArr[] = new double[2];
        
        // 调试：检查染色体是否包含-1
        boolean hasInvalidGene = false;
        for (int i = 0; i < chromosome.gene_OS.length; i++) {
            if (chromosome.gene_OS[i] == -1 || chromosome.gene_MS[i] == -1) {
                hasInvalidGene = true;
                break;
            }
        }
        if (hasInvalidGene) {
            System.out.println("\n===== 发现无效染色体（含-1） =====");
            System.out.print("gene_OS: [");
            for (int i = 0; i < chromosome.gene_OS.length; i++) {
                System.out.print(chromosome.gene_OS[i]);
                if (i < chromosome.gene_OS.length - 1) System.out.print(", ");
            }
            System.out.println("]");
            System.out.print("gene_MS: [");
            for (int i = 0; i < chromosome.gene_MS.length; i++) {
                System.out.print(chromosome.gene_MS[i]);
                if (i < chromosome.gene_MS.length - 1) System.out.print(", ");
            }
            System.out.println("]");
            System.out.println("染色体长度: " + chromosome.gene_OS.length + ", 工件数: " + jobCount);
            System.out.println("===================================\n");
        }

        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        //计算第一道工序的加工时间,统计机器分配情况
        for (int i = 0; i < jobCount; i++) {
            jobNo = chromosome.gene_OS[i];// 工件名
            // 注意：这里只是统计，不增加operNoOfEachJob
            machineNo = chromosome.gene_MS[i];// 机器编号

            // 检查是否为无效值
            if (jobNo < 0 || jobNo >= jobCount) {
                System.out.println("ERROR: 打印工序段位置 " + i + " 的工件编号无效: " + jobNo + " (应该在0到" + (jobCount-1) + "之间)");
                throw new RuntimeException("染色体包含无效的工件编号: " + jobNo + " at position " + i);
            }
            if (machineNo < 1 || machineNo > printMachineCount) {
                System.out.println("ERROR: 打印工序段位置 " + i + " 的机器编号无效: " + machineNo + " (应该在1到" + printMachineCount + "之间)");
                throw new RuntimeException("染色体包含无效的机器编号: " + machineNo + " at position " + i);
            }

            if (!machineMap.containsKey(machineNo)) {
                machineMap.put(machineNo, new ArrayList<>());
            }
            machineMap.get(machineNo).add(jobNo);
        }
        PriorityQueue<ProgramEntity.Solution> pq = new PriorityQueue<>(new Comparator<ProgramEntity.Solution>() {
            @Override
            public int compare(ProgramEntity.Solution o1, ProgramEntity.Solution o2) {
                if (o1.endTime > o2.endTime) {
                    return 1;
                } else if (o1.endTime < o2.endTime) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        //遍历map调用SkyLinePacking或禁忌搜索
        for (Map.Entry<Integer, List<Integer>> entry : machineMap.entrySet()) {
            int machineNo1 = entry.getKey();
            List<Integer> jobList = entry.getValue();

            // 按照jobList的顺序构建itemList
            // jobList的顺序来自于染色体的gene_OS编码，反映了零件的排列顺序
            // 这个顺序将作为禁忌搜索的初始解
            List<Item> itemList = new ArrayList<>();
            for (int i = 0; i < jobList.size(); i++) {
                jobNo = jobList.get(i);
                // 注意：这里只是准备装箱数据，不增加operNoOfEachJob
                itemList.add(items[jobNo]);
            }
            // machineNo1是"第几台可选机器"（1-based），转换为0-based机器索引
            int machineIndex = machineNo1 - 1;
            PrintMachine printMachine = (PrintMachine) machines[machineIndex];

            // 使用天际线装箱算法
            List<ProgramEntity.Solution> solutions = new SkyLinePacking(printMachine.L, printMachine.W,
                                              itemList.toArray(new Item[0]), true).packings();
            chromosome.printSolution[machineIndex].addAll(solutions);
            int count = 1;
            for (ProgramEntity.Solution solution : solutions) {
                // 计算最小的分批数
                Batch batch = new Batch(count++, solution);
                int size = machTimes[machineIndex].size();
                double start = machTimes[machineIndex].get(size - 1).start;
                double end = start + printMachine.prepareTime + printMachine.reCoatingTime * solution.maxG / printMachine.printH;
                solution.startTime = start;
                solution.endTime = end;
                //将批次加入批次队列
                pq.add(solution);
                // 更新机器时间段
                ArrayList<Time> t = new ArrayList<>();
                t.add(new Time(start, end, 1));
                t.add(new Time(end, Double.MAX_VALUE, 0));
                machTimes[machineIndex].remove(size - 1);
                machTimes[machineIndex].addAll(size - 1, t);
                //为批次内的每个工件进行打印工序时间赋值
                for (int i = 0; i < solution.placeItemList.size(); i++) {
                    PlaceItem placeItem = solution.placeItemList.get(i);
                    int jobNo1 = Integer.parseInt(placeItem.name);
                    int currentOperNo = operNoOfEachJob[jobNo1];// 获取当前工序号（打印工序）
                    operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                    operationMatrix[jobNo1][currentOperNo].machineNo = machineIndex;  // 存储0-based机器索引
                    operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                    operationMatrix[jobNo1][currentOperNo].startTime = start;
                    operationMatrix[jobNo1][currentOperNo].endTime = end;
                    operNoOfEachJob[jobNo1]++;// 打印工序完成，工序号+1
                }
            }
        }
        //按照规则安排这一批次的加工去下一个机器
        while (!pq.isEmpty()) {
            ProgramEntity.Solution solution = pq.poll();
            
            //查询当前可用的批处理机（能立即开始加工），选择加工时间最短的
            int selectedIndex = -1;
            double minProcessingTime = Double.MAX_VALUE;
            
            for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
                int size = machTimes[i].size();
                double machineAvailableTime = machTimes[i].get(size - 1).start;  // 机器可用时间
                
                // 检查机器是否当前可用（机器可用时间 <= 批次打印完成时间）
                if (machineAvailableTime <= solution.endTime) {
                    BathchMachine bathchMachine = (BathchMachine)machines[i];
                    
                    // 在可用机器中选择加工时间最短的
                    if (bathchMachine.processingTime < minProcessingTime) {
                        minProcessingTime = bathchMachine.processingTime;
                        selectedIndex = i;
                    }
                }
            }
            
            // 如果没有当前可用的机器，选择最早可用且加工时间最短的
            if (selectedIndex == -1) {
                double earliestAvailableTime = Double.MAX_VALUE;
                
                for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
                    int size = machTimes[i].size();
                    double machineAvailableTime = machTimes[i].get(size - 1).start;
                    
                    if (machineAvailableTime < earliestAvailableTime) {
                        earliestAvailableTime = machineAvailableTime;
                        BathchMachine bathchMachine = (BathchMachine)machines[i];
                        minProcessingTime = bathchMachine.processingTime;
                        selectedIndex = i;
                    } else if (machineAvailableTime == earliestAvailableTime) {
                        BathchMachine bathchMachine = (BathchMachine)machines[i];
                        if (bathchMachine.processingTime < minProcessingTime) {
                            minProcessingTime = bathchMachine.processingTime;
                            selectedIndex = i;
                        }
                    }
                }
            }
            
            int minIndex = selectedIndex;
            int size = machTimes[minIndex].size();
            BathchMachine bathchMachine = (BathchMachine)machines[minIndex];
            double start = Math.max(machTimes[minIndex].get(size - 1).start, solution.endTime);
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
                if (currentOperNo == 5) {
                    System.out.println(1);
                    break;
                }
                operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                operationMatrix[jobNo1][currentOperNo].machineNo = minIndex;
                operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                operationMatrix[jobNo1][currentOperNo].startTime = start;
                operationMatrix[jobNo1][currentOperNo].endTime = end;
                operNoOfEachJob[jobNo1]++;// 批处理工序完成，工序号+1
            }
        }
        // 批处理阶段不在染色体中，离散工序从jobCount开始
        for (int i = jobCount; i < chromosome.gene_OS.length; i++) {
            jobNo = chromosome.gene_OS[i];// 工件名
             int currentOperNo = operNoOfEachJob[jobNo];
    
    // 检查是否还有工序需要处理
    if (currentOperNo >= input.getOperationCountArr()[jobNo]) {
        continue; // 跳过已经处理完的工件
    }
    operNo = currentOperNo;
    
            // 计算MS的固定位置
            // MS离散段结构：工件0的所有离散工序，工件1的所有离散工序，...
            int msIndex = jobCount;  // 从离散段起点开始
            
            // 累加前面所有工件的离散工序数量
            for (int j = 0; j < jobNo; j++) {
                int discreteOpsCount = input.getOperationCountArr()[j] - 2;  // 减去打印和批处理
                msIndex += discreteOpsCount;
            }
            
            // 加上当前工件内的偏移（operNo - 2，因为离散工序从工序2开始）
            msIndex += (operNo - 2);
            
            // 检查msIndex是否有效
            if (msIndex >= chromosome.gene_MS.length) {
                System.out.println("ERROR: msIndex=" + msIndex + " 超出gene_MS长度 " + chromosome.gene_MS.length);
                System.out.println("  jobNo=" + jobNo + ", operNo=" + operNo);
                throw new RuntimeException("gene_MS索引越界");
            }
            
            int msValue = chromosome.gene_MS[msIndex];
            if (msValue <= 0) {
                System.out.println("ERROR: gene_MS[" + msIndex + "] = " + msValue + " (应该>=1)");
                System.out.println("  jobNo=" + jobNo + ", operNo=" + operNo);
                System.out.print("  完整gene_MS: [");
                for (int j = 0; j < chromosome.gene_MS.length; j++) {
                    System.out.print(chromosome.gene_MS[j] + (j < chromosome.gene_MS.length - 1 ? ", " : ""));
                }
                System.out.println("]");
                throw new RuntimeException("gene_MS包含无效的机器编号: " + msValue);
            }
            
            // 从MS的固定位置读取机器信息
            machineNoAndTimeArr = getMachineNoAndTime(input, chromosome.gene_MS, jobNo, operNo, msIndex);
            machineNo = (int)machineNoAndTimeArr[0];  // 从gene_MS读取的是1-based
            operationTime = machineNoAndTimeArr[1];

//			System.out.println("i=" + i + ",JobNo " + jobNo + ",OperNo " + operNo + ",machineNo " + machineNo
//					+ ",operationTime" + operationTime);

            operationMatrix[jobNo][operNo].aStartTime = operationMatrix[jobNo][operNo - 1].endTime;
            operationMatrix[jobNo][operNo].machineNo = machineNo - 1;  // 转换为0-based存储
            operationMatrix[jobNo][operNo].jobNo = jobNo;
            operationMatrix[jobNo][operNo].task = operNo;
            // machineNo是1-based，访问machTimes数组需要-1转为0-based索引
            for (int j = 0; j < machTimes[machineNo - 1].size(); j++) {
                double start = Math.max(operationMatrix[jobNo][operNo].aStartTime, machTimes[machineNo - 1].get(j).start);
                double end = start + operationTime;
                // 对机器空闲的时间段，若可以加工，则加工，否则判断下一个空闲时间段
                if (machTimes[machineNo - 1].get(j).type == 0 && end <= machTimes[machineNo - 1].get(j).end) {
                    // 设置工序开始结束时间
                    operationMatrix[jobNo][operNo].startTime = start;
                    operationMatrix[jobNo][operNo].endTime = end;
                    // 更新机器时间段
                    ArrayList<Time> t = new ArrayList<>();
                    if (operationMatrix[jobNo][operNo].aStartTime > machTimes[machineNo - 1].get(j).start) {
                        t.add(new Time(machTimes[machineNo - 1].get(j).start, operationMatrix[jobNo][operNo].aStartTime, 0));
                        t.add(new Time(operationMatrix[jobNo][operNo].aStartTime, end, 1));
                    } else {
                        t.add(new Time(machTimes[machineNo - 1].get(j).start, end, 1));
                    }
                    if (end < machTimes[machineNo - 1].get(j).end) {
                        t.add(new Time(end, machTimes[machineNo - 1].get(j).end, 0));
                    }
                    machTimes[machineNo - 1].remove(j);
                    machTimes[machineNo - 1].addAll(j, t);
//					System.out.println("startTime "+operationMatrix[jobNo][operNo].startTime+
//							",endTime "+operationMatrix[jobNo][operNo].endTime);

                    break;
                }
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
