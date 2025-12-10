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
            int machineNo1 = entry.getKey();
            List<Integer> jobList = entry.getValue();
            List<Item> itemList = new ArrayList<>();
            for (int i = 0; i < jobList.size(); i++) {
                jobNo = jobList.get(i);
                // 注意：这里只是准备装箱数据，不增加operNoOfEachJob
                itemList.add(items[jobNo]);
            }
            PrintMachine printMachine = (PrintMachine) machines[machineNo1];
            List<Solution> solutions = new SkyLinePacking(printMachine.L, printMachine.W, items, true).packings();
            chromosome.printSolution[machineNo1].addAll(solutions);

            int count = 1;
            for (ProgramEntity.Solution solution : solutions) {
                // 计算最小的分批数
                Batch batch = new Batch(count++, solution);
                int size = machTimes[machineNo1].size();
                double start = machTimes[machineNo1].get(size - 1).start;
                double end = start + printMachine.prepareTime + printMachine.reCoatingTime * solution.maxG / printMachine.printH;
                solution.startTime = start;
                solution.endTime = end;
                //将批次加入批次队列
                pq.add(solution);
                // 更新机器时间段
                ArrayList<Time> t = new ArrayList<>();
                t.add(new Time(start, end, 1));
                t.add(new Time(end, Double.MAX_VALUE, 0));
                machTimes[machineNo].remove(size - 1);
                machTimes[machineNo].addAll(size - 1, t);
                //为批次内的每个工件进行打印工序时间赋值
                for (int i = 0; i < solution.placeItemList.size(); i++) {
                    PlaceItem placeItem = solution.placeItemList.get(i);
                    int jobNo1 = Integer.parseInt(placeItem.name);
                    int currentOperNo = operNoOfEachJob[jobNo1];// 获取当前工序号（打印工序）
                    operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                    operationMatrix[jobNo1][currentOperNo].machineNo = machineNo1;
                    operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                    operationMatrix[jobNo1][currentOperNo].startTime = start;
                    operationMatrix[jobNo1][currentOperNo].endTime = end;
                    operNoOfEachJob[jobNo1]++;// 打印工序完成，工序号+1
                }
            }
        }
        //按照规则安排这一批次的加工去下一个机器
        while (!pq.isEmpty()) {0
            Solution solution = pq.poll();
            //搜索当前完工时间最小的批处理机器，将该批次分配给他
            int minIndex = printMachineCount;
            for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
                int size = machTimes[i].size();
                if (machTimes[i].get(size - 1).start < machTimes[minIndex].get(machTimes[minIndex].size() - 1).start) {
                    minIndex = i;
                } else if (machTimes[i].get(size - 1).start == machTimes[minIndex].get(machTimes[minIndex].size() - 1).start) {
                    BathchMachine bathchMachine = (BathchMachine)machines[i];
                    BathchMachine bathchMachine1 = (BathchMachine)machines[minIndex];
                    if (bathchMachine.processingTime < bathchMachine1.processingTime) {
                        minIndex = i;
                    }
                }
            }
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
                operationMatrix[jobNo1][currentOperNo].jobNo = jobNo1;
                operationMatrix[jobNo1][currentOperNo].machineNo = minIndex;
                operationMatrix[jobNo1][currentOperNo].task = currentOperNo;
                operationMatrix[jobNo1][currentOperNo].startTime = start;
                operationMatrix[jobNo1][currentOperNo].endTime = end;
                operNoOfEachJob[jobNo1]++;// 批处理工序完成，工序号+1
            }
        }
        for (int i = jobCount * 2; i < chromosome.gene_OS.length; i++) {
            jobNo = chromosome.gene_OS[i];// 工件名
             int currentOperNo = operNoOfEachJob[jobNo];
    
    // 检查是否还有工序需要处理
    if (currentOperNo >= input.getOperationCountArr()[jobNo]) {
        continue; // 跳过已经处理完的工件
    }
    operNo = currentOperNo;
    
            //找到这道工序对应的机器编号以及加工时间
            System.out.println("Job: " + jobNo + ", OperNo: " + operNo + 
                   ", MaxOper: " + input.getOperationCountArr()[jobNo]);
if (operNo >= input.getOperationCountArr()[jobNo]) {
    System.out.println("ERROR: operNo exceeded!");
}
            machineNoAndTimeArr = getMachineNoAndTime(input, chromosome.gene_MS, jobNo, operNo, i);
            machineNo = (int)machineNoAndTimeArr[0];
            operationTime = machineNoAndTimeArr[1];

//			System.out.println("i=" + i + ",JobNo " + jobNo + ",OperNo " + operNo + ",machineNo " + machineNo
//					+ ",operationTime" + operationTime);

            operationMatrix[jobNo][operNo].aStartTime = operationMatrix[jobNo][operNo - 1].endTime;
            operationMatrix[jobNo][operNo].machineNo = machineNo;
            operationMatrix[jobNo][operNo].jobNo = jobNo;
            operationMatrix[jobNo][operNo].task = operNo;
            for (int j = 0; j < machTimes[machineNo].size(); j++) {
                double start = Math.max(operationMatrix[jobNo][operNo].aStartTime, machTimes[machineNo].get(j).start);
                double end = start + operationTime;
                // 对机器空闲的时间段，若可以加工，则加工，否则判断下一个空闲时间段
                if (machTimes[machineNo].get(j).type == 0 && end <= machTimes[machineNo].get(j).end) {
                    // 设置工序开始结束时间
                    operationMatrix[jobNo][operNo].startTime = start;
                    operationMatrix[jobNo][operNo].endTime = end;
                    // 更新机器时间段
                    ArrayList<Time> t = new ArrayList<>();
                    if (operationMatrix[jobNo][operNo].aStartTime > machTimes[machineNo].get(j).start) {
                        t.add(new Time(machTimes[machineNo].get(j).start, operationMatrix[jobNo][operNo].aStartTime, 0));
                        t.add(new Time(operationMatrix[jobNo][operNo].aStartTime, end, 1));
                    } else {
                        t.add(new Time(machTimes[machineNo].get(j).start, end, 1));
                    }
                    if (end < machTimes[machineNo].get(j).end) {
                        t.add(new Time(end, machTimes[machineNo].get(j).end, 0));
                    }
                    machTimes[machineNo].remove(j);
                    machTimes[machineNo].addAll(j, t);
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
