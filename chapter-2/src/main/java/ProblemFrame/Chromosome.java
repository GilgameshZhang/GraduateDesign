package ProblemFrame;

import ProgramEntity.Job;
import ProgramEntity.Solution;
import ProgramEntity.Item;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

public class Chromosome implements Comparable<Chromosome> {
    public int[] gene_OS;
    public int[] gene_MS;

    public Random r;
    public double fitness;
    public List<Solution>[] printSolution;

    public Chromosome(Random r) {
        this.r = r;
    }

    public Chromosome(Job[] entries, Random r, ProgramEntity.Problem problem) {
        this.r = r;
        ArrayList<Integer> os = new ArrayList<>();
        // 批处理阶段不在染色体中，只包含打印工序和离散工序
        ArrayList<Integer> printOps = new ArrayList<>();
        ArrayList<Integer> discreteOps = new ArrayList<>();
        
        for (int i = 0; i < entries.length; i++) {
            printOps.add(entries[i].index);     // 每个工件的打印工序（工序0）
            // 批处理工序（工序1）不在染色体中，由装箱算法自动处理
            for (int j = 2; j < entries[i].opsNr; j++) {  // 离散工序（从工序2开始）
                discreteOps.add(entries[i].index);
            }
        }
        
        // 分段打乱
        Collections.shuffle(printOps, this.r);
        Collections.shuffle(discreteOps, this.r);
        
        // 合并：打印工序在前，离散工序在后
        os.addAll(printOps);
        os.addAll(discreteOps);

        ArrayList<Integer> ms = new ArrayList<>();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        // 打印工序段：按os的顺序为每个工件的打印工序选择机器
        for (int i = 0; i < printOps.size(); i++) {
            int jobNo = os.get(i);  // 从os中获取工件编号（已打乱）
            // 为该工件的打印工序选择机器
            int printMachineCount = entries[jobNo].opsMacNr[0];
            ms.add(r.nextInt(printMachineCount) + 1);  // 打印机1, 2, ...
        }
        
        // 批处理工序（工序1）不在染色体中，不需要机器选择基因
        
        // ✅ 离散工序段：按固定顺序（工件编号顺序）生成MS，与OS解耦
        // MS的离散段结构：工件0的所有离散工序，工件1的所有离散工序，...
        // 这个顺序是固定的，不随OS变化
        for (int jobNo = 0; jobNo < entries.length; jobNo++) {
            // 获取该工件的离散工序数量（从工序2开始）
            int discreteOpsCount = entries[jobNo].opsNr - 2;  // 减去打印和批处理
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;  // 实际工序编号
            int operIdx = operationToIndex[jobNo][operNo];
            
            // 收集可选的机器编号（1-based）
            ArrayList<Integer> availableMachines = new ArrayList<>();
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                if (proDesMatrix[operIdx][k] != 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                        availableMachines.add(k + 1);
                    }
                }
                
            if (!availableMachines.isEmpty()) {
                    // 随机选择一个机器的相对索引（1到availableMachines.size()）
                    int relativeIndex = 1 + r.nextInt(availableMachines.size());
                    ms.add(relativeIndex);
            } else {
                throw new RuntimeException("工件" + jobNo + "的工序" + operNo + "没有可用机器");
                }
            }
        }

        this.gene_OS = new int[os.size()];
        for (int i = 0; i < os.size(); i++) {
            this.gene_OS[i] = os.get(i);
        }
        this.gene_MS = new int[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            this.gene_MS[i] = ms.get(i);
        }

        this.fitness = 0;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    public Chromosome(int[] OS, int[] MS, Random r) {
        this.gene_OS = OS;
        this.gene_MS = MS;
        this.r = r;
        this.fitness = -1;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }
    
    /**
     * 带初始化策略的构造函数
     * @param entries 工件信息
     * @param r 随机数生成器
     * @param problem 问题实例
     * @param strategy 初始化策略
     */
    public Chromosome(Job[] entries, Random r, ProgramEntity.Problem problem, InitializationStrategy strategy) {
        this.r = r;
        ArrayList<Integer> os = new ArrayList<>();
        ArrayList<Integer> printOps = new ArrayList<>();
        ArrayList<Integer> discreteOps = new ArrayList<>();
        
        // 收集打印工序和离散工序
        for (int i = 0; i < entries.length; i++) {
            printOps.add(entries[i].index);
            for (int j = 2; j < entries[i].opsNr; j++) {
                discreteOps.add(entries[i].index);
            }
        }
        
        // 应用打印工序排序策略
        sortOperations(printOps, strategy.printSortStrategy, problem);
        
        // 应用离散工序排序策略
        sortOperations(discreteOps, strategy.discreteSortStrategy, problem);
        
        // 合并工序序列
        os.addAll(printOps);
        os.addAll(discreteOps);
        
        // 生成机器选择序列
        ArrayList<Integer> ms = new ArrayList<>();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        Machine[] machines = problem.getMachines();
        Item[] items = problem.getItems();
        
        // 打印工序的机器选择
        // 注意：不使用entries[jobNo].opsMacNr[0]的限制
        // 只要零件的长宽高符合打印机的长宽高，就可以分配
        int totalPrintMachineCount = problem.getPrintMachineCount();
        
        for (int i = 0; i < printOps.size(); i++) {
            int jobNo = os.get(i);
            Item item = items[jobNo];
            
            // 找出所有能容纳该零件的打印机
            ArrayList<Integer> suitableMachines = new ArrayList<>();
            for (int m = 0; m < totalPrintMachineCount; m++) {
                PrintMachine pm = (PrintMachine) machines[m];
                // 检查零件尺寸是否能放入打印机（考虑旋转）
                boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                if (fitsNormal || fitsRotated) {
                    suitableMachines.add(m + 1);  // 1-based
                }
            }
            
            if (suitableMachines.isEmpty()) {
                throw new RuntimeException("工件" + jobNo + "尺寸(" + 
                    String.format("%.2f x %.2f x %.2f", item.l, item.w, item.h) + 
                    ")无法放入任何打印机");
            }
            
            int selectedMachine;
            if (strategy.printMachineStrategy == InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL) {
                // 轮盘赌：基于打印机能力（printH和recoatingTime）
                selectedMachine = selectPrintMachineByRoulette(jobNo, suitableMachines, machines, items, r);
            } else if (strategy.printMachineStrategy == InitializationStrategy.MachineSelectionStrategy.LOAD_BALANCE) {
                // 负载均衡：平均分配（轮流分配，round-robin）
                selectedMachine = suitableMachines.get(i % suitableMachines.size());
            } else {
                // 随机选择
                selectedMachine = suitableMachines.get(r.nextInt(suitableMachines.size()));
            }
            ms.add(selectedMachine);
        }
        
        // ✅ 离散工序的机器选择：按固定顺序（工件编号顺序）生成MS，与OS解耦
        // MS的离散段结构：工件0的所有离散工序，工件1的所有离散工序，...
        // 这个顺序是固定的，不随OS变化
        int jobCount = entries.length;
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            // 获取该工件的离散工序数量（从工序2开始）
            int discreteOpsCount = entries[jobNo].opsNr - 2;  // 减去打印和批处理
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;  // 实际工序编号
            int operIdx = operationToIndex[jobNo][operNo];
            
            // 收集可选机器
            ArrayList<Integer> availableMachines = new ArrayList<>();
            ArrayList<Double> processingTimes = new ArrayList<>();
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                if (proDesMatrix[operIdx][k] != 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                    availableMachines.add(k + 1);
                    processingTimes.add(proDesMatrix[operIdx][k]);
                }
            }
            
            if (availableMachines.isEmpty()) {
                throw new RuntimeException("工件" + jobNo + "的工序" + operNo + "没有可用机器");
            }
            
                int selectedMachineIndex;  // 选中机器在availableMachines中的索引（0-based）
            if (strategy.discreteMachineStrategy == InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL) {
                // 轮盘赌：基于加工时长（时间越短，概率越大）
                    int selectedMachine = selectDiscreteMachineByRoulette(availableMachines, processingTimes, r);
                    selectedMachineIndex = availableMachines.indexOf(selectedMachine);
            } else if (strategy.discreteMachineStrategy == InitializationStrategy.MachineSelectionStrategy.SHORTEST_PROCESS_TIME) {
                // 选择加工时间最短的机器
                    int selectedMachine = selectShortestProcessTimeMachine(availableMachines, processingTimes);
                    selectedMachineIndex = availableMachines.indexOf(selectedMachine);
            } else {
                // 随机选择
                    selectedMachineIndex = r.nextInt(availableMachines.size());
            }
                
                // ✅ 存储相对索引（1-based）而不是绝对机器编号
                int relativeIndex = selectedMachineIndex + 1;
                ms.add(relativeIndex);
            }
        }
        
        // 转换为数组
        this.gene_OS = new int[os.size()];
        for (int i = 0; i < os.size(); i++) {
            this.gene_OS[i] = os.get(i);
        }
        this.gene_MS = new int[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            this.gene_MS[i] = ms.get(i);
        }
        
        this.fitness = 0;
        this.printSolution = null;
    }
    
    /**
     * 根据策略对工序进行排序
     */
    private void sortOperations(ArrayList<Integer> operations, 
                                InitializationStrategy.OperationSortStrategy strategy,
                                ProgramEntity.Problem problem) {
        switch (strategy) {
            case RANDOM:
                Collections.shuffle(operations, this.r);
                break;
                
            case AREA_DESCENDING:
                // 按面积降序排列
                operations.sort((j1, j2) -> {
                    Item item1 = problem.getItems()[j1];
                    Item item2 = problem.getItems()[j2];
                    double area1 = item1.l * item1.w;
                    double area2 = item2.l * item2.w;
                    return Double.compare(area2, area1);  // 降序
                });
                break;
                
            case HEIGHT_DESCENDING:
                // 按高度降序排列
                operations.sort((j1, j2) -> {
                    Item item1 = problem.getItems()[j1];
                    Item item2 = problem.getItems()[j2];
                    return Double.compare(item2.h, item1.h);  // 降序
                });
                break;
        }
    }
    
    /**
     * 轮盘赌选择打印机（基于打印能力）
     * 打印时间 = prepareTime + reCoatingTime * itemHeight / printH
     * 时间越短，打印能力越强，被选中概率越大
     */
    private int selectPrintMachineByRoulette(int jobNo, ArrayList<Integer> suitableMachines,
                                            Machine[] machines, Item[] items, Random r) {
        Item item = items[jobNo];
        double[] printTimes = new double[suitableMachines.size()];
        double totalInverseTime = 0.0;
        
        // 计算每台合适打印机的打印时间（越短越好）
        for (int i = 0; i < suitableMachines.size(); i++) {
            int machineIdx = suitableMachines.get(i) - 1;  // 转换为0-based索引
            PrintMachine pm = (PrintMachine) machines[machineIdx];
            double printTime = pm.prepareTime + pm.reCoatingTime * item.h / pm.printH;
            printTimes[i] = printTime;
            // 使用倒数作为适应度（时间越短，适应度越高）
            totalInverseTime += 1.0 / printTime;
        }
        
        // 轮盘赌选择
        double rand = r.nextDouble() * totalInverseTime;
        double sum = 0.0;
        for (int i = 0; i < suitableMachines.size(); i++) {
            sum += 1.0 / printTimes[i];
            if (sum >= rand) {
                return suitableMachines.get(i);  // 返回1-based机器编号
            }
        }
        
        return suitableMachines.get(suitableMachines.size() - 1);  // 默认返回最后一台
    }
    
    /**
     * 轮盘赌选择离散处理机器（基于加工时长）
     * 加工时间越短，被选中概率越大
     */
    private int selectDiscreteMachineByRoulette(ArrayList<Integer> availableMachines,
                                               ArrayList<Double> processingTimes,
                                               Random r) {
        double totalInverseTime = 0.0;
        
        // 计算适应度总和（使用倒数，时间越短适应度越高）
        for (double time : processingTimes) {
            totalInverseTime += 1.0 / time;
        }
        
        // 轮盘赌选择
        double rand = r.nextDouble() * totalInverseTime;
        double sum = 0.0;
        for (int i = 0; i < availableMachines.size(); i++) {
            sum += 1.0 / processingTimes.get(i);
            if (sum >= rand) {
                return availableMachines.get(i);
            }
        }
        
        return availableMachines.get(availableMachines.size() - 1);  // 默认返回最后一个
    }
    
    /**
     * 选择加工时间最短的机器（贪心策略）
     * 
     * @param availableMachines 可选机器列表
     * @param processingTimes 对应的加工时间列表
     * @return 加工时间最短的机器ID
     */
    private int selectShortestProcessTimeMachine(ArrayList<Integer> availableMachines,
                                                 ArrayList<Double> processingTimes) {
        if (availableMachines.isEmpty()) {
            throw new IllegalArgumentException("可选机器列表为空");
        }
        
        // 找到加工时间最短的机器
        int shortestIndex = 0;
        double shortestTime = processingTimes.get(0);
        
        for (int i = 1; i < availableMachines.size(); i++) {
            if (processingTimes.get(i) < shortestTime) {
                shortestTime = processingTimes.get(i);
                shortestIndex = i;
            }
        }
        
        return availableMachines.get(shortestIndex);
    }

    public Chromosome(Chromosome c) {
        this.gene_MS = new int[c.gene_MS.length];
        System.arraycopy(c.gene_MS, 0, this.gene_MS, 0, c.gene_MS.length);
        this.gene_OS = new int[c.gene_OS.length];
        System.arraycopy(c.gene_OS, 0, this.gene_OS, 0, c.gene_OS.length);
        this.r = c.r;
        this.fitness = c.fitness;
        
        // 深度复制printSolution（重要！确保best染色体的装箱结果被保留）
        if (c.printSolution != null) {
            this.printSolution = new List[c.printSolution.length];
            for (int i = 0; i < c.printSolution.length; i++) {
                if (c.printSolution[i] != null) {
                    this.printSolution[i] = new ArrayList<>(c.printSolution[i]);
                } else {
                    this.printSolution[i] = null;
                }
            }
        } else {
            this.printSolution = null;
        }
    }

    @Override
    public int compareTo(Chromosome o) {
        Chromosome s = (Chromosome) o;
        if (s.fitness > this.fitness) {
            return 1;
        } else if (this.fitness == s.fitness) {
            return 0;
        } else {
            return -1;
        }
    }
}
