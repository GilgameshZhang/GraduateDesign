package AlgorithmFrame.nsgaii;

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
 * NSGA-II 多目标遗传算法
 * 
 * 基于chapter-2的单目标GA扩展为多目标版本
 * 保持相同的编码解码逻辑，替换选择和环境选择机制
 * 
 * @version 2.0 - 集成局部搜索
 * @author AI Assistant
 */
public class NSGAII {
    
    // ==================== 基础配置 ====================
    private Problem problem;
    private Operation[][] operationMatrix;
    private Random random;
    private NSGAIIOperations operations;  // 算子操作类
    
    // ==================== 算法参数 ====================
    private int populationSize = 100;           // 种群大小
    private double crossoverRate = 0.9;         // 交叉率
    private double mutationRate = 0.1;          // 变异率
    private int maxGenerations = 500;           // 最大代数
    private double maxRunTimeMinutes = 5.0;     // 最大运行时间（分钟）
    private int tournamentSize = 2;             // 锦标赛大小
    private double crowdingDelta = 1e-6;        // 拥挤距离阈值（tie-breaker用）
    
    // ==================== 多目标配置 ====================
    private List<MOEvaluator.ObjectiveFunction> objectives;
    private MOEvaluator evaluator;
    
    // ==================== 局部搜索配置 ====================
    private boolean enableLocalSearch = false;      // 是否启用局部搜索
    private LocalSearchEngine localSearchEngine;    // 局部搜索引擎
    private OperatorSelector operatorSelector;      // 算子选择器
    private PercentileClassifier classifier;        // 分位分类器
    private int localSearchInterval = 1;           // 局部搜索间隔（代数）

    private boolean useStrategy = false;
    
    // ==================== 运行时数据 ====================
    private List<MOIndividual> population;
    private List<MOIndividual> paretoFront;
    private int currentGeneration;
    private long startTime;
    
    // ==================== 统计数据 ====================
    private List<Integer> paretoSizeHistory;    // 每代Pareto前沿大小
    private List<Double> hypervolomeHistory;     // 每代超体积值
    
    // ==================== 构造函数 ====================
    
    /**
     * 默认构造函数（双目标：Cmax + 能耗）
     */
//    public NSGAII(Problem problem) {
//        this(problem, Arrays.asList(
//            new MOEvaluator.MaximumCompletionTime(),
//            new MOEvaluator.TotalEnergyConsumption()
//        ));
//    }
    
    /**
     * 自定义目标函数构造函数
     */
    public NSGAII(Problem problem, List<MOEvaluator.ObjectiveFunction> objectives) {
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
        
        // 初始化算子操作类（包含交叉和变异）
        this.operations = new NSGAIIOperations(problem, random);
        
        // 初始化统计列表
        this.paretoSizeHistory = new ArrayList<>();
        this.hypervolomeHistory = new ArrayList<>();
    }
    
    // ==================== 参数设置 ====================
    
    public void setPopulationSize(int size) {
        this.populationSize = size;
    }
    
    public void setCrossoverRate(double rate) {
        this.crossoverRate = rate;
    }
    
    public void setMutationRate(double rate) {
        this.mutationRate = rate;
    }
    
    public void setMaxGenerations(int gen) {
        this.maxGenerations = gen;
    }
    
    public void setMaxRunTimeMinutes(double minutes) {
        this.maxRunTimeMinutes = minutes;
    }
    
    public void setTournamentSize(int size) {
        this.tournamentSize = size;
    }
    
    public void setCrowdingDelta(double delta) {
        this.crowdingDelta = delta;
    }
    
    public void setPackingQMode(MOEvaluator.PackingQMode mode) {
        this.evaluator.setPackingQMode(mode);
    }
    
    public void setSeed(long seed) {
        this.random = new Random();
    }

    public void setUseStrategy(boolean useStrategy) {
        this.useStrategy = useStrategy;
    }
    
    /**
     * 启用局部搜索
     * 
     * @param enable 是否启用
     * @param interval 局部搜索间隔（每隔多少代执行一次局部搜索）
     */
    public void enableLocalSearch(boolean enable, int interval) {
        this.enableLocalSearch = enable;
        this.localSearchInterval = interval;
        
        if (enable) {
            initializeLocalSearch();
        }
    }
    
    /**
     * 启用局部搜索（默认每10代执行一次）
     */
    public void enableLocalSearch(boolean enable) {
        enableLocalSearch(enable, 10);
    }
    
    /**
     * 初始化局部搜索组件
     */
    private void initializeLocalSearch() {
        // 1. 创建分位分类器
        classifier = new PercentileClassifier(0.05);
        
        // 2. 创建算子选择器并注册算子
        operatorSelector = new OperatorSelector(random);
        
        // 注册时间向算子（降低Cmax）- 完全照搬第二章N1-N5
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
        
        // 4. 配置局部搜索参数
        localSearchEngine.setL(20);           // 每个精英10次尝试
        localSearchEngine.setEta(1.0);       // 选择10%精英
        localSearchEngine.setEpsE(0);      // T型允许能耗上升5%
        localSearchEngine.setEpsC(0);      // E型允许Cmax上升2%
        localSearchEngine.setImprovC(0);  // T型要求Cmax下降0.5%
        localSearchEngine.setImprovE(0);   // E型要求能耗下降1%
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
    
    // ==================== 主算法 ====================
    
    /**
     * 执行NSGA-II算法
     * 
     * @return Pareto最优解集
     */
    public List<MOIndividual> solve() {
        System.out.println("========================================");
        System.out.println("      NSGA-II 多目标遗传算法");
        System.out.println("========================================");
        System.out.println("问题: " + problem.getJobCount() + "个工件");
        System.out.println("目标数: " + objectives.size());
        System.out.print("目标函数: ");
        for (MOEvaluator.ObjectiveFunction obj : objectives) {
            System.out.print(obj.getName() + " ");
        }
        System.out.println();
        System.out.println("种群大小: " + populationSize);
        System.out.println("交叉率: " + crossoverRate);
        System.out.println("变异率: " + mutationRate);
        System.out.println("最大代数: " + maxGenerations);
        System.out.println("最大时间: " + maxRunTimeMinutes + " 分钟");
        System.out.println("========================================\n");
        
        startTime = System.currentTimeMillis();
        
        // 1. 初始化种群
        System.out.println("初始化种群...");
        initializePopulation();
        
        // 2. 评价初始种群
        System.out.println("评价初始种群...");
        evaluatePopulation(population);
        
        // 3. 非支配排序和拥挤距离计算
        List<List<MOIndividual>> fronts = NSGAIIOperations.fastNonDominatedSort(population);
        for (List<MOIndividual> front : fronts) {
            NSGAIIOperations.assignCrowdingDistance(front);
        }
        
        System.out.println("初始种群Pareto前沿大小: " + fronts.get(0).size());
        System.out.println("\n开始进化...\n");
        currentGeneration = 1;
        // 4. 主循环
        while (!isTimeUp()) {
            
//            // 检查时间限制
//            if (isTimeUp()) {
//                System.out.println("\n达到时间限制，停止进化");
//                break;
//            }
            
            // 5. 生成子代
            List<MOIndividual> offspring = generateOffspring();
            
            // 6. 评价子代
            evaluatePopulation(offspring);
            
            // 7. 环境选择（合并父代和子代，选择最优N个）
            population = NSGAIIOperations.environmentalSelection(
                population, offspring, populationSize, crowdingDelta
            );
            
            // 7.5. 局部搜索（可选）
            if (enableLocalSearch && currentGeneration % localSearchInterval == 0) {
                applyLocalSearch();
            }
            
            // 8. 记录统计数据
            recordStatistics();
            
            // 9. 输出进度
//            if (currentGeneration % 1 == 0 || currentGeneration == 1) {
//                printProgress();
//            }
            currentGeneration++;
        }
        
        // 10. 提取最终Pareto前沿
        paretoFront = NSGAIIOperations.getParetoFront(population);
        
        // 11. 输出最终结果
        printFinalResults();
        
        return paretoFront;
    }
    
    /**
     * 初始化种群（完全照搬第二章的多样化初始化策略）
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        Job[] jobs = problem.getJobs();
        
        // 多样化初始化策略
        Random random = new Random();  // 每种策略占20%
        
        System.out.println("[初始化] 生成多样化初始种群：");
        for (int i = 0; i < populationSize; i++) {
            InitializationStrategy strategy;
            double r = random.nextDouble();
            if (useStrategy) {
                if (r < 0.3) {
                    // 完全随机策略
                    strategy = InitializationStrategy.randomStrategy();
                } else if (r < 0.5) {
                    // 高度降序 + 轮盘赌机器选择
                    strategy = InitializationStrategy.heuristicStrategy();
                } else if (r < 0.6) {
                    // 面积降序 + 轮盘赌机器选择
                    strategy = InitializationStrategy.areaBasedStrategy();
                } else if (r < 0.7) {
                    // 高度降序 + 负载均衡
                    strategy = new InitializationStrategy(
                            InitializationStrategy.OperationSortStrategy.HEIGHT_DESCENDING,
                            InitializationStrategy.OperationSortStrategy.RANDOM,
                            InitializationStrategy.MachineSelectionStrategy.LOAD_BALANCE,
                            InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL
                    );
                } else if (r < 0.9) {
                    // 高度降序 + 最短加工时间（贪心策略）
                    strategy = InitializationStrategy.heightShortestProcessStrategy();
                } else {
                    // 混合策略：随机选择一种
                    int randChoice = random.nextInt(4);
                    if (randChoice == 0) {
                        strategy = InitializationStrategy.randomStrategy();
                    } else if (randChoice == 1) {
                        strategy = InitializationStrategy.heuristicStrategy();
                    } else if (randChoice == 2) {
                        strategy = InitializationStrategy.areaBasedStrategy();
                    } else {
                        strategy = InitializationStrategy.heightShortestProcessStrategy();
                    }
                }
            } else {
                strategy = InitializationStrategy.randomStrategy();
            }
            // 使用策略创建个体
            MOIndividual individual = new MOIndividual(jobs, random, problem, strategy, objectives.size());
            population.add(individual);
        }
    }
    
    /**
     * 评价种群
     */
    private void evaluatePopulation(List<MOIndividual> pop) {
        for (MOIndividual individual : pop) {
            // 初始化工序矩阵
            CaculateFitness.initOperationMatrix(operationMatrix);
            
            // 评价个体
            evaluator.evaluate(individual, problem, operationMatrix, objectives);
            
            // 保存operationMatrix副本到个体（供局部搜索使用）
            individual.operationMatrix = copyOperationMatrix(operationMatrix);
        }
    }
    
    /**
     * 复制operationMatrix
     */
    private Operation[][] copyOperationMatrix(Operation[][] source) {
        Operation[][] copy = new Operation[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = new Operation[source[i].length];
            for (int j = 0; j < source[i].length; j++) {
                copy[i][j] = new Operation();
                // 复制关键字段
                copy[i][j].jobNo = source[i][j].jobNo;
                copy[i][j].task = source[i][j].task;
                copy[i][j].machineNo = source[i][j].machineNo;
                copy[i][j].startTime = source[i][j].startTime;
                copy[i][j].endTime = source[i][j].endTime;
                copy[i][j].span = source[i][j].span;
            }
        }
        return copy;
    }
    
    /**
     * 生成子代种群
     */
    private List<MOIndividual> generateOffspring() {
        List<MOIndividual> offspring = new ArrayList<>();
        
        while (offspring.size() < populationSize) {
            // 锦标赛选择两个父代
            MOIndividual parent1 = NSGAIIOperations.tournamentSelection(population, tournamentSize, random);
            MOIndividual parent2 = NSGAIIOperations.tournamentSelection(population, tournamentSize, random);
            
            MOIndividual child1, child2;
            
            // 交叉（使用chapter-2的POX/JBX算子）
            if (random.nextDouble() < crossoverRate) {
                // 复制父代创建子代
                child1 = new MOIndividual(parent1);
                child2 = new MOIndividual(parent2);
                // 执行交叉
                operations.crossover(child1, child2);
            } else {
                child1 = new MOIndividual(parent1);
                child2 = new MOIndividual(parent2);
            }
            
            // 变异（使用chapter-2的多种变异算子）
            if (random.nextDouble() < mutationRate) {
                operations.mutation(child1, operationMatrix, useStrategy);
            }
            if (random.nextDouble() < mutationRate) {
                operations.mutation(child2, operationMatrix, useStrategy);
            }
            
            offspring.add(child1);
            if (offspring.size() < populationSize) {
                offspring.add(child2);
            }
        }
        
        return offspring;
    }
    
    // 注意：交叉和变异操作已移至NSGAIIOperations类
    // operations.crossover() - 使用chapter-2的POX/JBX算子
    // operations.mutation() - 使用chapter-2的多种变异算子
    
    /**
     * 执行局部搜索
     */
    private void applyLocalSearch() {
        // 1. 选择精英个体
        List<MOIndividual> elites = localSearchEngine.selectElites(population);
        
        if (elites.isEmpty()) {
            return;
        }
        
        // 2. 执行局部搜索
        List<MOIndividual> improvedElites = localSearchEngine.searchElites(elites);
        
        // 3. 将改进后的个体加入种群
        List<MOIndividual> combined = new ArrayList<>(population);
        combined.addAll(improvedElites);
        
        // 4. 环境选择（保持种群大小）
        population = NSGAIIOperations.environmentalSelection(
            combined, new ArrayList<>(), populationSize, crowdingDelta
        );
    }
    
    /**
     * 记录统计数据
     */
    private void recordStatistics() {
        List<MOIndividual> currentPareto = NSGAIIOperations.getParetoFront(population);
        paretoSizeHistory.add(currentPareto.size());
        
        // 计算超体积（仅2目标）
        if (objectives.size() == 2 && !currentPareto.isEmpty()) {
            // 使用nadir点作为参考点
            double[] referencePoint = calculateNadirPoint(currentPareto);
            double hv = NSGAIIOperations.calculateHypervolume2D(currentPareto, referencePoint);
            hypervolomeHistory.add(hv);
        }
    }
    
    /**
     * 计算nadir点（每个目标的最大值）
     */
    private double[] calculateNadirPoint(List<MOIndividual> front) {
        double[] nadir = new double[objectives.size()];
        Arrays.fill(nadir, Double.NEGATIVE_INFINITY);
        
        for (MOIndividual ind : front) {
            for (int i = 0; i < objectives.size(); i++) {
                nadir[i] = Math.max(nadir[i], ind.objectives[i]);
            }
        }
        
        // 增加10%的边界
        for (int i = 0; i < nadir.length; i++) {
            nadir[i] *= 1.1;
        }
        
        return nadir;
    }
    
    /**
     * 检查是否超时
     */
    private boolean isTimeUp() {
        long elapsed = System.currentTimeMillis() - startTime;
        return elapsed >= maxRunTimeMinutes * 60 * 1000;
    }
    
    /**
     * 打印进度
     */
    private void printProgress() {
        List<MOIndividual> currentPareto = NSGAIIOperations.getParetoFront(population);
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.printf("代数 %4d | Pareto前沿大小: %3d | 耗时: %.2f秒",
            currentGeneration, currentPareto.size(), elapsed / 1000.0);
        
        if (!currentPareto.isEmpty()) {
            // 显示第一个Pareto解的目标值
            System.out.print(" | 示例解: ");
            System.out.print(currentPareto.get(0).objectivesToString());
        }
        
        System.out.println();
    }
    
    /**
     * 打印最终结果
     */
    private void printFinalResults() {
        long totalTime = System.currentTimeMillis() - startTime;
        
        System.out.println("\n========================================");
        System.out.println("           算法运行完成");
        System.out.println("========================================");
        System.out.println("总代数: " + currentGeneration);
        System.out.println("总耗时: " + (totalTime / 1000.0) + " 秒");
        System.out.println("Pareto前沿大小: " + paretoFront.size());
        
        // 输出局部搜索统计
        if (enableLocalSearch && localSearchEngine != null) {
            System.out.println("----------------------------------------");
            System.out.println(localSearchEngine.getStatistics());
            System.out.println(operatorSelector.getStatistics());
        }
        
        System.out.println("========================================");
        
        System.out.println("\nPareto最优解集:");
        System.out.println("----------------------------------------");
        
        // 按第一个目标排序显示
        List<MOIndividual> sortedFront = new ArrayList<>(paretoFront);
        sortedFront.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        for (int i = 0; i < Math.min(10, sortedFront.size()); i++) {
            MOIndividual ind = sortedFront.get(i);
            System.out.printf("解 %2d: ", i + 1);
            for (int j = 0; j < objectives.size(); j++) {
                System.out.printf("%s=%.2f ", objectives.get(j).getName(), ind.objectives[j]);
            }
            System.out.printf("| packingQ=%.4f | batches=%d\n", 
                ind.packingQ, ind.batchCount);
        }
        
        if (sortedFront.size() > 10) {
            System.out.println("... (共" + sortedFront.size() + "个Pareto解)");
        }
        
        System.out.println("========================================\n");
    }
    
    // ==================== Getter方法 ====================
    
    public List<MOIndividual> getPopulation() {
        return population;
    }
    
    public List<MOIndividual> getParetoFront() {
        return paretoFront;
    }
    
    public List<Integer> getParetoSizeHistory() {
        return paretoSizeHistory;
    }
    
    public List<Double> getHypervolomeHistory() {
        return hypervolomeHistory;
    }
}

