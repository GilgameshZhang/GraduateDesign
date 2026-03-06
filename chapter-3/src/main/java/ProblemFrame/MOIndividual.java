package ProblemFrame;

import ProblemFrame.localsearch.IndividualType;
import java.util.Arrays;
import java.util.Random;

/**
 * 多目标优化个体（Multi-Objective Individual）
 * 
 * 继承自单目标的Chromosome，增加NSGA-II所需的多目标相关字段
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOIndividual implements Comparable<MOIndividual> {
    
    // ==================== 基础染色体信息（来自单目标） ====================
    /** 工序序列基因 (OS) */
    public int[] gene_OS;
    
    /** 机器选择基因 (MS) */
    public int[] gene_MS;
    
    /** 随机数生成器 */
    public Random r;
    
    /** 打印阶段的解（每台打印机的批次列表） */
    public java.util.List<ProgramEntity.Solution>[] printSolution;
    
    /** 
     * 工序矩阵（Operation Matrix）
     * 用于存储解码后的调度信息，供局部搜索算子使用
     * operationMatrix[jobNo][operNo] = Operation对象
     */
    public ProgramEntity.Operation[][] operationMatrix;
    
    // ==================== 多目标优化相关字段 ====================
    /** 
     * 多目标值数组
     * objectives[0] = Cmax (makespan)
     * objectives[1] = 能耗 (可选)
     * objectives[2] = 拖期/等待时间 (可选)
     * ...
     */
    public double[] objectives;
    
    /** 
     * 非支配层号 (Pareto rank)
     * rank=1: 第一前沿（非支配解）
     * rank=2: 第二前沿
     * ...
     */
    public int rank;
    
    /** 
     * 拥挤距离 (Crowding Distance)
     * 用于维持种群多样性
     * 边界个体 = Double.POSITIVE_INFINITY
     */
    public double crowdingDistance;
    
    /** 
     * 装箱质量指标 (Packing Quality)
     * 
     * 不作为优化目标，仅用于tie-breaker（平局判别）
     * 
     * 计算方式（可配置）：
     * - LAST_BATCH_MIN: 所有打印机最后一批占用率的最小值
     * - LAST_BATCH_AVG: 所有打印机最后一批占用率的平均值
     * - SCORE_SUM: 装箱评分系统的总分
     * 
     * packingQ越大越好（表示装箱更紧凑）
     */
    public double packingQ;
    
    /** 
     * 批次总数
     * 可选作为二级tie-breaker（批次数少优先）
     */
    public int batchCount;
    
    // ==================== 局部搜索相关字段 ====================
    
    /**
     * Cmax分位排名（0..1，越大表示Cmax越差）
     * 用于局部搜索的个体分类
     */
    public double pc;
    
    /**
     * Energy分位排名（0..1，越大表示Energy越差）
     * 用于局部搜索的个体分类
     */
    public double pe;
    
    /**
     * 个体类型（基于分位排名分类）
     * TIME_DEFICIENT: 时间不足型
     * ENERGY_DEFICIENT: 能耗不足型
     */
    public IndividualType type;
    
    // ==================== 构造函数 ====================
    
    /**
     * 默认构造函数
     */
    public MOIndividual(Random r) {
        this.r = r;
        this.rank = Integer.MAX_VALUE;
        this.crowdingDistance = 0.0;
        this.packingQ = 0.0;
        this.batchCount = 0;
        this.pc = 0.5;
        this.pe = 0.5;
        this.type = null;
    }
    
    /**
     * 从单目标染色体构造多目标个体
     */
    public MOIndividual(Chromosome chromosome, int numObjectives) {
        this.gene_OS = Arrays.copyOf(chromosome.gene_OS, chromosome.gene_OS.length);
        this.gene_MS = Arrays.copyOf(chromosome.gene_MS, chromosome.gene_MS.length);
        this.r = chromosome.r;
        this.printSolution = chromosome.printSolution;
        
        // 初始化多目标相关字段
        this.objectives = new double[numObjectives];
        this.rank = Integer.MAX_VALUE;
        this.crowdingDistance = 0.0;
        this.packingQ = 0.0;
        this.batchCount = 0;
        this.pc = 0.5;
        this.pe = 0.5;
        this.type = null;
    }
    
    /**
     * 带初始化策略的构造函数（完全照搬第二章）
     */
    public MOIndividual(ProgramEntity.Job[] entries, Random r, ProgramEntity.Problem problem, 
                        InitializationStrategy strategy, int numObjectives) {
        this.r = new Random();
        java.util.ArrayList<Integer> os = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> printOps = new java.util.ArrayList<>();
        java.util.ArrayList<Integer> discreteOps = new java.util.ArrayList<>();
        
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
        java.util.ArrayList<Integer> ms = new java.util.ArrayList<>();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        ProgramEntity.Machine.Machine[] machines = problem.getMachines();
        ProgramEntity.Item[] items = problem.getItems();
        
        // 打印工序的机器选择
        int totalPrintMachineCount = problem.getPrintMachineCount();
        
        for (int i = 0; i < printOps.size(); i++) {
            int jobNo = os.get(i);
            ProgramEntity.Item item = items[jobNo];
            
            // 找出所有能容纳该零件的打印机
            java.util.ArrayList<Integer> suitableMachines = new java.util.ArrayList<>();
            for (int m = 0; m < totalPrintMachineCount; m++) {
                ProgramEntity.Machine.PrintMachine pm = (ProgramEntity.Machine.PrintMachine) machines[m];
                boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                if (fitsNormal || fitsRotated) {
                    suitableMachines.add(m + 1);  // 1-based
                }
            }
            
            if (suitableMachines.isEmpty()) {
                throw new RuntimeException("工件" + jobNo + "尺寸无法放入任何打印机");
            }
            
            int selectedMachine;
            if (strategy.printMachineStrategy == InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL) {
                selectedMachine = selectPrintMachineByRoulette(jobNo, suitableMachines, machines, items, r);
            } else if (strategy.printMachineStrategy == InitializationStrategy.MachineSelectionStrategy.LOAD_BALANCE) {
                selectedMachine = suitableMachines.get(i % suitableMachines.size());
            } else {
                selectedMachine = suitableMachines.get(r.nextInt(suitableMachines.size()));
            }
            ms.add(selectedMachine);
        }
        
        // 离散工序的机器选择：按固定顺序生成MS
        int jobCount = entries.length;
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = entries[jobNo].opsNr - 2;
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;
                int operIdx = operationToIndex[jobNo][operNo];
                
                java.util.ArrayList<Integer> availableMachines = new java.util.ArrayList<>();
                java.util.ArrayList<Double> processingTimes = new java.util.ArrayList<>();
                for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                    if (proDesMatrix[operIdx][k] != 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                        availableMachines.add(k + 1);
                        processingTimes.add(proDesMatrix[operIdx][k]);
                    }
                }
                
                if (availableMachines.isEmpty()) {
                    throw new RuntimeException("工件" + jobNo + "的工序" + operNo + "没有可用机器");
                }
                
                int selectedMachineIndex;
                if (strategy.discreteMachineStrategy == InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL) {
                    int selectedMachine = selectDiscreteMachineByRoulette(availableMachines, processingTimes, r);
                    selectedMachineIndex = availableMachines.indexOf(selectedMachine);
                } else if (strategy.discreteMachineStrategy == InitializationStrategy.MachineSelectionStrategy.SHORTEST_PROCESS_TIME) {
                    int selectedMachine = selectShortestProcessTimeMachine(availableMachines, processingTimes);
                    selectedMachineIndex = availableMachines.indexOf(selectedMachine);
                } else {
                    selectedMachineIndex = r.nextInt(availableMachines.size());
                }
                
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
        
        // 初始化多目标相关字段
        this.objectives = new double[numObjectives];
        this.rank = Integer.MAX_VALUE;
        this.crowdingDistance = 0.0;
        this.packingQ = 0.0;
        this.batchCount = 0;
        this.pc = 0.5;
        this.pe = 0.5;
        this.type = null;
        this.printSolution = null;
    }
    
    /**
     * 根据策略对工序进行排序
     */
    private void sortOperations(java.util.ArrayList<Integer> operations, 
                                InitializationStrategy.OperationSortStrategy strategy,
                                ProgramEntity.Problem problem) {
        switch (strategy) {
            case RANDOM:
                java.util.Collections.shuffle(operations, this.r);
                break;
                
            case AREA_DESCENDING:
                operations.sort((j1, j2) -> {
                    ProgramEntity.Item item1 = problem.getItems()[j1];
                    ProgramEntity.Item item2 = problem.getItems()[j2];
                    double area1 = item1.l * item1.w;
                    double area2 = item2.l * item2.w;
                    return Double.compare(area2, area1);
                });
                break;
                
            case HEIGHT_DESCENDING:
                operations.sort((j1, j2) -> {
                    ProgramEntity.Item item1 = problem.getItems()[j1];
                    ProgramEntity.Item item2 = problem.getItems()[j2];
                    return Double.compare(item2.h, item1.h);
                });
                break;
        }
    }
    
    /**
     * 轮盘赌选择打印机（基于打印能力）
     */
    private int selectPrintMachineByRoulette(int jobNo, java.util.ArrayList<Integer> suitableMachines,
                                            ProgramEntity.Machine.Machine[] machines, ProgramEntity.Item[] items, Random r) {
        double[] printTimes = new double[suitableMachines.size()];
        double totalInverseTime = 0.0;
        
        for (int i = 0; i < suitableMachines.size(); i++) {
            int machineIdx = suitableMachines.get(i) - 1;
            ProgramEntity.Machine.PrintMachine pm = (ProgramEntity.Machine.PrintMachine) machines[machineIdx];
            double printTime = pm.prepareTime + pm.reCoatingTime/ pm.printH;
            printTimes[i] = printTime;
            totalInverseTime += (1.0 / printTime) * 10000;
        }
        
        double rand = r.nextDouble() * totalInverseTime;
        double sum = 0.0;
        for (int i = 0; i < suitableMachines.size(); i++) {
            sum += 10000.0 / printTimes[i];
            if (sum >= rand) {
                return suitableMachines.get(i);
            }
        }
        
        return suitableMachines.get(suitableMachines.size() - 1);
    }
    
    /**
     * 轮盘赌选择离散处理机器（基于加工时长）
     */
    private int selectDiscreteMachineByRoulette(java.util.ArrayList<Integer> availableMachines,
                                               java.util.ArrayList<Double> processingTimes,
                                               Random r) {
        double totalInverseTime = 0.0;
        
        for (double time : processingTimes) {
            totalInverseTime += 1000.0 / time;
        }
        
        double rand = r.nextDouble() * totalInverseTime;
        double sum = 0.0;
        for (int i = 0; i < availableMachines.size(); i++) {
            sum += 1000.0 / processingTimes.get(i);
            if (sum >= rand) {
                return availableMachines.get(i);
            }
        }
        
        return availableMachines.get(availableMachines.size() - 1);
    }
    
    /**
     * 选择加工时间最短的机器（贪心策略）
     */
    private int selectShortestProcessTimeMachine(java.util.ArrayList<Integer> availableMachines,
                                                 java.util.ArrayList<Double> processingTimes) {
        if (availableMachines.isEmpty()) {
            throw new IllegalArgumentException("可选机器列表为空");
        }
        
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
    
    /**
     * 拷贝构造函数
     */
    public MOIndividual(MOIndividual other) {
        this.gene_OS = Arrays.copyOf(other.gene_OS, other.gene_OS.length);
        this.gene_MS = Arrays.copyOf(other.gene_MS, other.gene_MS.length);
        this.r = other.r;
        
        if (other.printSolution != null) {
            this.printSolution = Arrays.copyOf(other.printSolution, other.printSolution.length);
        }
        
        this.objectives = Arrays.copyOf(other.objectives, other.objectives.length);
        this.rank = other.rank;
        this.crowdingDistance = other.crowdingDistance;
        this.packingQ = other.packingQ;
        this.batchCount = other.batchCount;
        this.pc = other.pc;
        this.pe = other.pe;
        this.type = other.type;
        
        // ✅ 深拷贝 operationMatrix（关键！T4/T5需要）
        if (other.operationMatrix != null) {
            this.operationMatrix = new ProgramEntity.Operation[other.operationMatrix.length][];
            for (int i = 0; i < other.operationMatrix.length; i++) {
                if (other.operationMatrix[i] != null) {
                    this.operationMatrix[i] = Arrays.copyOf(other.operationMatrix[i], other.operationMatrix[i].length);
                }
            }
        } else {
            this.operationMatrix = null;
        }
    }
    
    // ==================== NSGA-II 比较方法 ====================
    
    /**
     * NSGA-II 的支配关系判断
     * 
     * @param other 另一个个体
     * @return true: 当前个体支配other；false: 不支配
     * 
     * 支配定义（最小化问题）：
     * A 支配 B <=> 所有目标 A_i <= B_i 且至少一个 A_i < B_i
     */
    public boolean dominates(MOIndividual other) {
        boolean atLeastOneBetter = false;
        
        for (int i = 0; i < objectives.length; i++) {
            if (this.objectives[i] > other.objectives[i]) {
                // 有一个目标更差，不支配
                return false;
            }
            if (this.objectives[i] < other.objectives[i]) {
                atLeastOneBetter = true;
            }
        }
        
        return atLeastOneBetter;
    }
    
    /**
     * NSGA-II 的个体比较（用于选择）
     * 
     * 比较顺序：
     * 1. rank 更小优先（前沿更靠前）
     * 2. rank 相同，crowdingDistance 更大优先（更稀疏区域）
     * 3. crowdingDistance 接近时，使用 tie-breaker
     * 
     * @param other 另一个个体
     * @return <0: this更优; >0: other更优; =0: 相同
     */
    @Override
    public int compareTo(MOIndividual other) {
        // 1. rank 更小优先
        if (this.rank != other.rank) {
            return Integer.compare(this.rank, other.rank);
        }
        
        // 2. crowdingDistance 更大优先
        // 注意：crowdingDistance的差值可能很小，使用阈值判断
        double cdDiff = this.crowdingDistance - other.crowdingDistance;
        final double DELTA = 1e-6;  // 阈值
        
        if (Math.abs(cdDiff) > DELTA) {
            // 拥挤距离有明显差异
            return -Double.compare(this.crowdingDistance, other.crowdingDistance);
        }
        
        // 3. tie-breaker: packingQ 更大优先（装箱质量更好）
//        if (Math.abs(this.packingQ - other.packingQ) > 1e-9) {
//            return -Double.compare(this.packingQ, other.packingQ);
//        }
        
        // 4. 次级 tie-breaker: batchCount 更少优先
//        if (this.batchCount != other.batchCount) {
//            return Integer.compare(this.batchCount, other.batchCount);
//        }
        
        // 5. 完全相同
        return 0;
    }
    
    /**
     * 专用于截断时的比较（更严格的tie-breaker）
     * 
     * @param other 另一个个体
     * @param delta 拥挤距离阈值
     * @return <0: this更优; >0: other更优; =0: 相同
     */
    public int compareForTruncation(MOIndividual other, double delta) {
        // 1. crowdingDistance 更大优先
        double cdDiff = this.crowdingDistance - other.crowdingDistance;
        
        if (Math.abs(cdDiff) > delta) {
            return -Double.compare(this.crowdingDistance, other.crowdingDistance);
        }
        
        // 2. tie-breaker: packingQ 更大优先
        if (Math.abs(this.packingQ - other.packingQ) > 1e-9) {
            return -Double.compare(this.packingQ, other.packingQ);
        }
        
        // 3. 次级 tie-breaker: batchCount 更少优先
        if (this.batchCount != other.batchCount) {
            return Integer.compare(this.batchCount, other.batchCount);
        }
        
        return 0;
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 获取目标值的字符串表示
     */
    public String objectivesToString() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < objectives.length; i++) {
            sb.append(String.format("%.2f", objectives[i]));
            if (i < objectives.length - 1) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }
    
    /**
     * 获取个体的详细信息
     */
    @Override
    public String toString() {
        return String.format("Individual[rank=%d, crowding=%.4f, objectives=%s, packingQ=%.4f, batches=%d]",
                rank, crowdingDistance, objectivesToString(), packingQ, batchCount);
    }
    
    /**
     * 转换为单目标染色体（用于复用现有评价函数）
     */
    public Chromosome toChromosome() {
        Chromosome c = new Chromosome(this.gene_OS, this.gene_MS, this.r);
        c.printSolution = this.printSolution;
        // 单目标fitness可以设置为第一个目标的倒数（如果需要）
        if (objectives != null && objectives.length > 0 && objectives[0] > 0) {
            c.fitness = 100000000.0 / objectives[0];
        }
        return c;
    }
    
    /**
     * 从单目标染色体更新多目标个体
     */
    public void updateFromChromosome(Chromosome c) {
        this.gene_OS = Arrays.copyOf(c.gene_OS, c.gene_OS.length);
        this.gene_MS = Arrays.copyOf(c.gene_MS, c.gene_MS.length);
        this.printSolution = c.printSolution;
    }
}

