package AlgorithmFrame.mogwo;

import ProblemFrame.*;
import ProblemFrame.localsearch.*;
import ProblemFrame.localsearch.operators.T1_CriticalBatchFrontInsert;
import ProblemFrame.localsearch.operators.T2_CriticalBatchAreaTransfer;
import ProblemFrame.localsearch.operators.T3_DiscreteCriticalBlockSwap;
import ProblemFrame.localsearch.operators.T4_DiscreteCriticalOpTimeReassign;
import ProblemFrame.localsearch.operators.T5_MachineReassignment;
import ProblemFrame.localsearch.operators.E1_PrintConsolidationMove;
import ProblemFrame.localsearch.operators.E2_DiscreteEnergyOptimalReassign;
import ProgramEntity.*;
import java.util.*;

/**
 * MOGWO - 多目标灰狼优化算法
 * Multi-Objective Grey Wolf Optimizer
 * 
 * 基于灰狼捕猎行为的多目标优化算法
 * 
 * 算法特点：
 * 1. 社会层级机制：Alpha、Beta、Delta领导狼群
 * 2. 外部存档：维护非支配解集
 * 3. 网格机制：保持存档多样性
 * 4. 自适应系数：平衡探索与开发
 * 
 * 参考文献：
 * Mirjalili, S., et al. (2016). Multi-objective grey wolf optimizer: 
 * A novel algorithm for multi-criterion optimization. 
 * Expert Systems with Applications, 47, 106-119.
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOGWO {
    
    // ==================== 基础配置 ====================
    private final Problem problem;
    private final Operation[][] operationMatrix;
    private Random random;
    private MOGWOOperations operations;  // 算子操作类
    
    // ==================== 算法参数 ====================
    private int populationSize = 100;           // 灰狼种群大小
    private int archiveSize = 100;              // 外部存档大小
    private int maxGenerations = 500;           // 最大迭代次数
    private double maxRunTimeMinutes = 5.0;     // 最大运行时间（分钟）
    private int gridDivisions = 10;             // 网格划分数
    private final double alpha_decay = 0.0;           // Alpha衰减参数（用于线性衰减）
    private final double beta_constant = 1.0;         // Beta常数（用于领导者选择）
    
    // ==================== 多目标配置 ====================
    private final List<MOEvaluator.ObjectiveFunction> objectives;
    private final MOEvaluator evaluator;
    
    // ==================== 局部搜索配置 ====================
    private boolean enableLocalSearch = false;
    private LocalSearchEngine localSearchEngine;
    private OperatorSelector operatorSelector;
    private PercentileClassifier classifier;
    private int localSearchInterval = 10;
    
    // ==================== 运行时数据 ====================
    private List<MOIndividual> population;      // 灰狼种群
    private List<MOIndividual> archive;         // 外部存档（非支配解）
    private List<MOIndividual> paretoFront;     // 最终Pareto前沿
    private int currentGeneration;
    private long startTime;
    
    // ==================== 统计数据 ====================
    private List<Integer> archiveSizeHistory;
    private List<Double> hypervolumeHistory;
    
    // ==================== 构造函数 ====================
    
    /**
     * 自定义目标函数构造函数
     */
    public MOGWO(Problem problem, List<MOEvaluator.ObjectiveFunction> objectives) {
        this.problem = problem;
        this.objectives = objectives;
        this.random = new Random();
        
        // 初始化工序矩阵
        int jobCount = problem.getJobCount();
        int[] opsCount = problem.getOperationCountArr();
        this.operationMatrix = new Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new Operation[opsCount[i]];
            for (int j = 0; j < opsCount[i]; j++) {
                operationMatrix[i][j] = new Operation();
            }
        }
        
        // 初始化评价器
        this.evaluator = new MOEvaluator();
        
        // 初始化算子操作类
        this.operations = new MOGWOOperations(problem, random);
        
        // 初始化统计列表
        this.archiveSizeHistory = new ArrayList<>();
        this.hypervolumeHistory = new ArrayList<>();
    }
    
    // ==================== 参数设置 ====================
    
    public void setPopulationSize(int size) {
        this.populationSize = size;
    }
    
    public void setArchiveSize(int size) {
        this.archiveSize = size;
    }
    
    public void setMaxGenerations(int gen) {
        this.maxGenerations = gen;
    }
    
    public void setMaxRunTimeMinutes(double minutes) {
        this.maxRunTimeMinutes = minutes;
    }
    
    public void setGridDivisions(int divisions) {
        this.gridDivisions = divisions;
    }
    
    public void setPackingQMode(MOEvaluator.PackingQMode mode) {
        this.evaluator.setPackingQMode(mode);
    }
    
    public void setSeed(long seed) {
        this.random = new Random(seed);
        this.operations = new MOGWOOperations(problem, random);
    }
    
    /**
     * 启用局部搜索
     * 
     * @param enable 是否启用
     * @param interval 局部搜索间隔（每隔多少代执行一次）
     */
    public void enableLocalSearch(boolean enable, int interval) {
        this.enableLocalSearch = enable;
        this.localSearchInterval = interval;
        
        if (enable) {
            initializeLocalSearch();
        }
    }
    
    /**
     * 设置局部搜索参数
     */
    public void setLocalSearchParameters(int L, double eta, double epsE, double epsC,
                                         double improvC, double improvE) {
        if (localSearchEngine != null) {
            localSearchEngine.setL(L);
            localSearchEngine.setEta(eta);
            localSearchEngine.setEpsE(epsE);
            localSearchEngine.setEpsC(epsC);
            localSearchEngine.setImprovC(improvC);
            localSearchEngine.setImprovE(improvE);
        }
    }
    
    /**
     * 初始化局部搜索组件
     */
    private void initializeLocalSearch() {
        // 1. 创建分位分类器
        classifier = new PercentileClassifier(0.05);
        
        // 2. 创建算子选择器并注册算子
        operatorSelector = new OperatorSelector(random);
        
        // 注册时间向算子（降低Cmax）
        operatorSelector.registerTimeOperator(new T1_CriticalBatchFrontInsert(random, problem));
        operatorSelector.registerTimeOperator(new T2_CriticalBatchAreaTransfer(random, problem));
        operatorSelector.registerTimeOperator(new T3_DiscreteCriticalBlockSwap(random, problem));
        operatorSelector.registerTimeOperator(new T4_DiscreteCriticalOpTimeReassign(random, problem));
        operatorSelector.registerTimeOperator(new T5_MachineReassignment(random, problem));
        
        // 注册能耗向算子（降低Energy）
        operatorSelector.registerEnergyOperator(new E1_PrintConsolidationMove(random));
        operatorSelector.registerEnergyOperator(new E2_DiscreteEnergyOptimalReassign(random));
        
        // 3. 创建局部搜索引擎
        localSearchEngine = new LocalSearchEngine(
            problem, 
            operatorSelector, 
            classifier, 
            evaluator, 
            objectives
        );
        
        // 4. 配置局部搜索参数（默认值）
        localSearchEngine.setL(10);           // 每个精英10次尝试
        localSearchEngine.setEta(0.10);       // 选择10%精英
        localSearchEngine.setEpsE(0.05);      // T型允许能耗上升5%
        localSearchEngine.setEpsC(0.02);      // E型允许Cmax上升2%
        localSearchEngine.setImprovC(0.005);  // T型要求Cmax下降0.5%
        localSearchEngine.setImprovE(0.01);   // E型要求能耗下降1%
    }
    
    // ==================== 核心算法 ====================
    
    /**
     * 求解多目标优化问题
     * 
     * @return Pareto前沿解集
     */
    public List<MOIndividual> solve() {
        startTime = System.currentTimeMillis();
        
        System.out.println("========================================");
        System.out.println("      MOGWO - 多目标灰狼优化算法");
        System.out.println("========================================");
        System.out.println("种群大小: " + populationSize);
        System.out.println("存档大小: " + archiveSize);
        System.out.println("最大迭代次数: " + maxGenerations);
        System.out.println("目标数量: " + objectives.size());
        System.out.println("局部搜索: " + (enableLocalSearch ? "启用" : "禁用"));
        System.out.println("========================================\n");
        
        // 1. 初始化种群
        System.out.println("初始化灰狼种群...");
        initializePopulation();
        
        // 2. 评价初始种群
        System.out.println("评价初始种群...");
        evaluatePopulation(population);
        
        // 3. 初始化外部存档
        System.out.println("初始化外部存档...");
        initializeArchive();
        
        System.out.println("开始迭代优化...\n");
        
        // 4. 主循环
        for (currentGeneration = 1; currentGeneration <= maxGenerations; currentGeneration++) {
            
            // 检查运行时间
            if (isTimeoutReached()) {
                System.out.println("\n⏱️  达到最大运行时间，停止优化");
                break;
            }
            
            // 计算自适应参数 a (从2线性递减到0)
            double a = 2.0 - 2.0 * currentGeneration / (double) maxGenerations;
            
            // 从存档中选择Alpha、Beta、Delta领导者
            MOIndividual alpha = selectLeader();
            MOIndividual beta = selectLeader();
            MOIndividual delta = selectLeader();
            
            // 更新每个灰狼的位置
            for (int i = 0; i < populationSize; i++) {
                MOIndividual wolf = population.get(i);
                
                // 基于三个领导者更新位置
                MOIndividual newWolf = updatePosition(wolf, alpha, beta, delta, a);
                
                // 边界检查与修复
                operations.repair(newWolf);
                
                // 评价新解
                evaluator.evaluate(newWolf, problem, operationMatrix, objectives);
                
                // 贪婪选择：保留更优的解
                if (isBetterOrEqual(newWolf, wolf)) {
                    population.set(i, newWolf);
                }
            }
            
            // 更新外部存档
            updateArchive();
            
            // 执行局部搜索（可选）
            if (enableLocalSearch && currentGeneration % localSearchInterval == 0) {
                applyLocalSearch();
            }
            
            // 记录统计信息
            recordStatistics();
            
            // 输出进度
            if (currentGeneration % 50 == 0 || currentGeneration == 1) {
                printProgress();
            }
        }
        
        // 5. 提取最终Pareto前沿
        paretoFront = new ArrayList<>(archive);
        
        System.out.println("\n========================================");
        System.out.println("优化完成！");
        System.out.println("最终Pareto前沿大小: " + paretoFront.size());
        System.out.println("总迭代次数: " + currentGeneration);
        System.out.println("总耗时: " + getElapsedTimeSeconds() + " 秒");
        System.out.println("========================================\n");
        
        return paretoFront;
    }
    
    /**
     * 初始化灰狼种群
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        Job[] entries = problem.getJobs();
        
        // 使用多样化的初始化策略
        InitializationStrategy[] strategies = {
            InitializationStrategy.randomStrategy(),
        };
        
        int strategyCount = strategies.length;
        int individualsPerStrategy = populationSize / strategyCount;
        
        for (int s = 0; s < strategyCount; s++) {
            InitializationStrategy strategy = strategies[s];
            int count = (s == strategyCount - 1) ? 
                (populationSize - s * individualsPerStrategy) : individualsPerStrategy;
            
            for (int i = 0; i < count; i++) {
                MOIndividual ind = new MOIndividual(entries, random, problem, strategy, objectives.size());
                population.add(ind);
            }
        }
    }
    
    /**
     * 评价种群
     */
    private void evaluatePopulation(List<MOIndividual> pop) {
        for (MOIndividual ind : pop) {
            evaluator.evaluate(ind, problem, operationMatrix, objectives);
        }
    }
    
    /**
     * 初始化外部存档
     */
    private void initializeArchive() {
        archive = new ArrayList<>();
        
        // 将所有非支配解加入存档
        for (MOIndividual ind : population) {
            addToArchive(ind);
        }
        
        // 如果存档超过容量，进行截断
        if (archive.size() > archiveSize) {
            truncateArchive();
        }
    }
    
    /**
     * 从存档中选择一个领导者（使用网格选择机制）
     */
    private MOIndividual selectLeader() {
        if (archive.isEmpty()) {
            // 如果存档为空，随机选择一个种群个体
            return population.get(random.nextInt(populationSize));
        }
        
        // 使用轮盘赌选择（偏向选择较少占用的网格）
        List<Integer> gridCounts = computeGridCounts();
        
        // 计算选择概率（网格个体数越少，被选中概率越高）
        double[] selectionProbs = new double[archive.size()];
        double totalInverse = 0.0;
        
        for (int i = 0; i < archive.size(); i++) {
            int gridIndex = getGridIndex(archive.get(i));
            int count = gridCounts.get(gridIndex);
            selectionProbs[i] = 1.0 / count;
            totalInverse += selectionProbs[i];
        }
        
        // 轮盘赌选择
        double rand = random.nextDouble() * totalInverse;
        double sum = 0.0;
        
        for (int i = 0; i < archive.size(); i++) {
            sum += selectionProbs[i];
            if (sum >= rand) {
                return new MOIndividual(archive.get(i));
            }
        }
        
        return new MOIndividual(archive.get(archive.size() - 1));
    }
    
    /**
     * 根据三个领导者更新灰狼位置
     * 
     * @param wolf 当前灰狼
     * @param alpha Alpha领导者
     * @param beta Beta领导者
     * @param delta Delta领导者
     * @param a 自适应参数
     * @return 新位置的灰狼
     */
    private MOIndividual updatePosition(MOIndividual wolf, MOIndividual alpha, 
                                       MOIndividual beta, MOIndividual delta, double a) {
        
        // 计算三个位置更新向量
        MOIndividual X1 = moveTowards(wolf, alpha, a);
        MOIndividual X2 = moveTowards(wolf, beta, a);
        MOIndividual X3 = moveTowards(wolf, delta, a);
        
        // 加权组合三个位置
        MOIndividual newWolf = combinePositions(X1, X2, X3);
        
        return newWolf;
    }
    
    /**
     * 向领导者方向移动（两段式编码）
     * 
     * @param wolf 当前灰狼
     * @param leader 领导者
     * @param a 自适应参数
     * @return 移动后的位置
     */
    private MOIndividual moveTowards(MOIndividual wolf, MOIndividual leader, double a) {
        // 计算系数向量
        double r1 = random.nextDouble();
        double r2 = random.nextDouble();
        
        double A = 2.0 * a * r1 - a;  // [-a, a]
        double C = 2.0 * r2;          // [0, 2]
        
        // 创建新个体（深拷贝）
        MOIndividual newWolf = new MOIndividual(wolf);
        
        int jobCount = problem.getJobCount();
        
        // 计算倾向系数
        double leaderBias = (Math.abs(A) + C) / (2.0 * a + 2.0);  // 归一化到[0,1]
        leaderBias = Math.min(1.0, Math.max(0.0, leaderBias));
        
        // ==================== 第一段：打印工序（前jobCount个）====================
        // 打印工序的OS和MS需要同步处理
        
        // 随机决定哪些工件来自领导者
        Set<Integer> leaderJobs = new HashSet<>();
        for (int i = 0; i < jobCount; i++) {
            if (random.nextDouble() < leaderBias) {
                leaderJobs.add(i);  // 工件i来自领导者
            }
        }
        
        // 重建打印段OS和MS（保持同步）
        int[] printOS = new int[jobCount];
        int[] printMS = new int[jobCount];
        Arrays.fill(printOS, -1);
        Arrays.fill(printMS, -1);
        
        // 先放置来自leader的工件（保持leader中的顺序）
        int fillIndex = 0;
        for (int i = 0; i < jobCount; i++) {
            int job = leader.gene_OS[i];
            if (leaderJobs.contains(job)) {
                printOS[fillIndex] = job;
                printMS[fillIndex] = leader.gene_MS[i];
                fillIndex++;
            }
        }
        
        // 再放置来自wolf的工件（保持wolf中的顺序）
        for (int i = 0; i < jobCount; i++) {
            int job = wolf.gene_OS[i];
            if (!leaderJobs.contains(job)) {
                printOS[fillIndex] = job;
                printMS[fillIndex] = wolf.gene_MS[i];
                fillIndex++;
            }
        }
        
        // 复制到newWolf
        System.arraycopy(printOS, 0, newWolf.gene_OS, 0, jobCount);
        System.arraycopy(printMS, 0, newWolf.gene_MS, 0, jobCount);
        
        // ==================== 第二段：离散工序（jobCount之后）====================
        // 离散工序只对OS进行混合，MS保持原结构
        
        int discreteLength = wolf.gene_OS.length - jobCount;
        
        // 对离散段OS进行混合
        for (int i = 0; i < discreteLength; i++) {
            int idx = jobCount + i;
            if (random.nextDouble() < leaderBias) {
                // 使用领导者的离散工序序列
                newWolf.gene_OS[idx] = leader.gene_OS[idx];
            } else {
                // 使用当前灰狼的离散工序序列
                newWolf.gene_OS[idx] = wolf.gene_OS[idx];
            }
        }
        
        // 离散段MS保持原结构（不变）
        System.arraycopy(wolf.gene_MS, jobCount, newWolf.gene_MS, jobCount, discreteLength);
        
        return newWolf;
    }
    
    /**
     * 组合三个位置（两段式编码）
     */
    private MOIndividual combinePositions(MOIndividual X1, MOIndividual X2, MOIndividual X3) {
        int jobCount = problem.getJobCount();
        
        // 从三个位置中随机选择一个作为基础
        MOIndividual base;
        double rand = random.nextDouble();
        if (rand < 0.33) {
            base = X1;
        } else if (rand < 0.67) {
            base = X2;
        } else {
            base = X3;
        }
        
        // 创建新个体（基于选中的base）
        MOIndividual newWolf = new MOIndividual(base);
        
        // ==================== 第一段：打印工序 ====================
        // 打印段：从三个位置中投票选择
        
        // 为每个工件投票
        Map<Integer, List<Integer>> jobVotes = new HashMap<>();
        for (int job = 0; job < jobCount; job++) {
            jobVotes.put(job, new ArrayList<>());
        }
        
        // 收集每个工件在三个位置中的索引
        for (int i = 0; i < jobCount; i++) {
            jobVotes.get(X1.gene_OS[i]).add(i);  // X1中job在位置i
            jobVotes.get(X2.gene_OS[i]).add(i);  // X2中job在位置i
            jobVotes.get(X3.gene_OS[i]).add(i);  // X3中job在位置i
        }
        
        // 计算每个工件的平均位置（投票结果）
        Map<Integer, Double> avgPositions = new HashMap<>();
        for (int job = 0; job < jobCount; job++) {
            List<Integer> positions = jobVotes.get(job);
            double avg = positions.stream().mapToInt(Integer::intValue).average().orElse(job);
            avgPositions.put(job, avg);
        }
        
        // 按平均位置排序工件
        List<Integer> sortedJobs = new ArrayList<>(avgPositions.keySet());
        sortedJobs.sort(Comparator.comparingDouble(avgPositions::get));
        
        // 构建打印段OS
        for (int i = 0; i < jobCount; i++) {
            newWolf.gene_OS[i] = sortedJobs.get(i);
        }
        
        // 打印段MS：选择base的机器分配（已通过深拷贝继承）
        // 或者随机从三个位置选择
        for (int i = 0; i < jobCount; i++) {
            int job = newWolf.gene_OS[i];
            
            // 找出这个job在X1,X2,X3中的MS分配
            int ms1 = -1, ms2 = -1, ms3 = -1;
            for (int j = 0; j < jobCount; j++) {
                if (X1.gene_OS[j] == job) ms1 = X1.gene_MS[j];
                if (X2.gene_OS[j] == job) ms2 = X2.gene_MS[j];
                if (X3.gene_OS[j] == job) ms3 = X3.gene_MS[j];
            }
            
            // 随机选择一个MS分配
            double r = random.nextDouble();
            if (r < 0.33 && ms1 != -1) {
                newWolf.gene_MS[i] = ms1;
            } else if (r < 0.67 && ms2 != -1) {
                newWolf.gene_MS[i] = ms2;
            } else if (ms3 != -1) {
                newWolf.gene_MS[i] = ms3;
            }
        }
        
        // ==================== 第二段：离散工序 ====================
        // 离散段OS：投票机制（简化）
        for (int i = jobCount; i < newWolf.gene_OS.length; i++) {
            double r = random.nextDouble();
            if (r < 0.33) {
                newWolf.gene_OS[i] = X1.gene_OS[i];
            } else if (r < 0.67) {
                newWolf.gene_OS[i] = X2.gene_OS[i];
            } else {
                newWolf.gene_OS[i] = X3.gene_OS[i];
            }
        }
        
        // 离散段MS：继承base的MS（已通过深拷贝）
        
        return newWolf;
    }
    
    /**
     * 更新外部存档
     */
    private void updateArchive() {
        // 将所有种群个体尝试加入存档
        for (MOIndividual ind : population) {
            addToArchive(ind);
        }
        
        // 如果存档超过容量，进行截断
        if (archive.size() > archiveSize) {
            truncateArchive();
        }
    }
    
    /**
     * 将个体加入存档（维护非支配性）
     */
    private void addToArchive(MOIndividual newInd) {
        boolean isDominated = false;
        List<MOIndividual> toRemove = new ArrayList<>();
        
        // 检查新个体是否被存档中的解支配
        for (MOIndividual archiveInd : archive) {
            if (archiveInd.dominates(newInd)) {
                isDominated = true;
                break;
            }
            if (newInd.dominates(archiveInd)) {
                toRemove.add(archiveInd);
            }
        }
        
        // 如果新个体未被支配，加入存档
        if (!isDominated) {
            archive.removeAll(toRemove);
            archive.add(new MOIndividual(newInd));
        }
    }
    
    /**
     * 截断存档（使用网格机制）
     */
    private void truncateArchive() {
        while (archive.size() > archiveSize) {
            // 计算每个网格中的个体数
            List<Integer> gridCounts = computeGridCounts();
            
            // 找到最拥挤的网格
            int maxCount = Collections.max(gridCounts);
            int mostCrowdedGrid = gridCounts.indexOf(maxCount);
            
            // 从最拥挤的网格中删除一个个体
            for (int i = 0; i < archive.size(); i++) {
                if (getGridIndex(archive.get(i)) == mostCrowdedGrid) {
                    archive.remove(i);
                    break;
                }
            }
        }
    }
    
    /**
     * 计算每个网格中的个体数
     */
    private List<Integer> computeGridCounts() {
        int totalGrids = (int) Math.pow(gridDivisions, objectives.size());
        List<Integer> counts = new ArrayList<>(Collections.nCopies(totalGrids, 0));
        
        for (MOIndividual ind : archive) {
            int gridIndex = getGridIndex(ind);
            counts.set(gridIndex, counts.get(gridIndex) + 1);
        }
        
        return counts;
    }
    
    /**
     * 获取个体所在的网格索引
     */
    private int getGridIndex(MOIndividual ind) {
        // 计算每个目标的归一化位置
        double[] minObjectives = new double[objectives.size()];
        double[] maxObjectives = new double[objectives.size()];
        
        // 初始化
        Arrays.fill(minObjectives, Double.MAX_VALUE);
        Arrays.fill(maxObjectives, Double.MIN_VALUE);
        
        // 计算最小值和最大值
        for (MOIndividual archiveInd : archive) {
            for (int i = 0; i < objectives.size(); i++) {
                minObjectives[i] = Math.min(minObjectives[i], archiveInd.objectives[i]);
                maxObjectives[i] = Math.max(maxObjectives[i], archiveInd.objectives[i]);
            }
        }
        
        // 计算网格索引
        int gridIndex = 0;
        int multiplier = 1;
        
        for (int i = 0; i < objectives.size(); i++) {
            double range = maxObjectives[i] - minObjectives[i];
            int division;
            
            if (range < 1e-10) {
                division = 0;
            } else {
                double normalized = (ind.objectives[i] - minObjectives[i]) / range;
                division = (int) (normalized * gridDivisions);
                division = Math.min(division, gridDivisions - 1);
            }
            
            gridIndex += division * multiplier;
            multiplier *= gridDivisions;
        }
        
        return gridIndex;
    }
    
    /**
     * 判断个体A是否优于或等于个体B
     */
    private boolean isBetterOrEqual(MOIndividual a, MOIndividual b) {
        // A支配B，或者A与B互不支配但A具有更好的tie-breaker
        if (a.dominates(b)) {
            return true;
        }
        if (b.dominates(a)) {
            return false;
        }
        
        // 互不支配时，比较装箱质量
        return a.packingQ >= b.packingQ;
    }
    
    /**
     * 应用局部搜索
     */
    private void applyLocalSearch() {
        System.out.println("  [LS] 执行局部搜索...");
        
        // 1. 从存档中选择精英（前eta%）
        List<MOIndividual> elites = localSearchEngine.selectElites(archive);
        
        if (elites.isEmpty()) {
            System.out.println("  [LS] 没有精英个体，跳过局部搜索");
            return;
        }
        
        System.out.println("  [LS] 选择精英数量: " + elites.size());
        
        // 2. 对精英执行局部搜索
        List<MOIndividual> improvedSolutions = localSearchEngine.searchElites(elites);
        
        // 3. 将改进的解加入存档
        int addedCount = 0;
        for (MOIndividual improved : improvedSolutions) {
            if (improved != null) {
                addToArchive(improved);
                addedCount++;
            }
        }
        
        // 4. 截断存档
        if (archive.size() > archiveSize) {
            truncateArchive();
        }
        
        System.out.println("  [LS] 局部搜索完成，改进解数量: " + addedCount);
    }
    
    /**
     * 记录统计信息
     */
    private void recordStatistics() {
        archiveSizeHistory.add(archive.size());
        
        // 计算超体积（可选）
        // double hv = computeHypervolume(archive);
        // hypervolumeHistory.add(hv);
    }
    
    /**
     * 输出进度
     */
    private void printProgress() {
        double elapsed = getElapsedTimeSeconds();
        System.out.printf("代数: %4d | 存档大小: %4d | 耗时: %.2fs\n",
            currentGeneration, archive.size(), elapsed);
    }
    
    /**
     * 检查是否超时
     */
    private boolean isTimeoutReached() {
        return getElapsedTimeSeconds() >= maxRunTimeMinutes * 60.0;
    }
    
    /**
     * 获取已用时间（秒）
     */
    private double getElapsedTimeSeconds() {
        return (System.currentTimeMillis() - startTime) / 1000.0;
    }
    
    // ==================== Getter方法 ====================
    
    public List<MOIndividual> getPopulation() {
        return population;
    }
    
    public List<MOIndividual> getArchive() {
        return archive;
    }
    
    public List<MOIndividual> getParetoFront() {
        return paretoFront;
    }
    
    public List<Integer> getArchiveSizeHistory() {
        return archiveSizeHistory;
    }
    
    public List<Double> getHypervolumeHistory() {
        return hypervolumeHistory;
    }
    
    public int getCurrentGeneration() {
        return currentGeneration;
    }
}
