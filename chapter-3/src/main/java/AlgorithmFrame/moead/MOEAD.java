package AlgorithmFrame.moead;

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
 * MOEA/D 多目标进化算法（基于分解）
 * 
 * Multi-Objective Evolutionary Algorithm based on Decomposition
 * 
 * 核心思想：
 * 1. 使用权重向量将多目标问题分解为多个单目标子问题
 * 2. 每个子问题优化一个聚合函数（Tchebycheff、加权和或PBI）
 * 3. 相邻子问题共享信息（邻域合作）
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOEAD {
    
    // ==================== 基础配置 ====================
    private Problem problem;
    private Operation[][] operationMatrix;
    private Random random;
    private MOEADOperations operations;  // 算子操作类
    
    // ==================== 算法参数 ====================
    private int populationSize = 100;           // 种群大小（子问题数量）
    private double crossoverRate = 0.9;         // 交叉率
    private double mutationRate = 0.1;          // 变异率
    private int maxGenerations = 500;           // 最大代数
    private double maxRunTimeMinutes = 5.0;     // 最大运行时间（分钟）
    private int neighborhoodSize = 20;          // 邻域大小（T）
    private double neighborhoodSelectionProb = 0.9;  // 从邻域选择的概率（delta）
    private int maxReplacementSize = 2;         // 最大替换数量（nr）
    
    // ==================== 多目标配置 ====================
    private List<MOEvaluator.ObjectiveFunction> objectives;
    private MOEvaluator evaluator;
    private MOEADOperations.ScalarizingFunction scalarizingFunction;
    
    // ==================== 局部搜索配置 ====================
    private boolean enableLocalSearch = false;      // 是否启用局部搜索
    private LocalSearchEngine localSearchEngine;    // 局部搜索引擎
    private OperatorSelector operatorSelector;      // 算子选择器
    private PercentileClassifier classifier;        // 分位分类器
    private int localSearchInterval = 10;           // 局部搜索间隔（代数）
    
    // ==================== 运行时数据 ====================
    private List<MOIndividual> population;          // 当前种群（每个子问题的当前解）
    private double[][] weightVectors;               // 权重向量矩阵 [N][M]
    private int[][] neighborhoods;                  // 邻域结构 [N][T]
    private double[] idealPoint;                    // 理想点 z* (最优值)
    private double[] nadirPoint;                    // nadir点 (最差值，用于归一化)
    private List<MOIndividual> paretoFront;         // Pareto前沿
    private int currentGeneration;
    private long startTime;
    
    // ==================== 统计数据 ====================
    private List<Integer> paretoSizeHistory;    // 每代Pareto前沿大小
    private List<Double> hypervolomeHistory;     // 每代超体积值
    
    // ==================== 构造函数 ====================
    
    /**
     * 自定义目标函数构造函数
     */
    public MOEAD(Problem problem, List<MOEvaluator.ObjectiveFunction> objectives) {
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
        this.operations = new MOEADOperations(problem, random);
        
        // 默认使用Tchebycheff聚合函数
        this.scalarizingFunction = MOEADOperations.ScalarizingFunction.TCHEBYCHEFF;
        
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
    
    public void setNeighborhoodSize(int size) {
        this.neighborhoodSize = size;
    }
    
    public void setNeighborhoodSelectionProb(double prob) {
        this.neighborhoodSelectionProb = prob;
    }
    
    public void setMaxReplacementSize(int size) {
        this.maxReplacementSize = size;
    }
    
    public void setScalarizingFunction(MOEADOperations.ScalarizingFunction func) {
        this.scalarizingFunction = func;
    }
    
    public void setPackingQMode(MOEvaluator.PackingQMode mode) {
        this.evaluator.setPackingQMode(mode);
    }
    
    public void setSeed(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * 启用局部搜索
     */
    public void enableLocalSearch(boolean enable, int interval) {
        this.enableLocalSearch = enable;
        this.localSearchInterval = interval;
        
        if (enable) {
            initializeLocalSearch();
        }
    }
    
    public void enableLocalSearch(boolean enable) {
        enableLocalSearch(enable, 10);
    }
    
    /**
     * 初始化局部搜索组件
     */
    private void initializeLocalSearch() {
        classifier = new PercentileClassifier(0.05);
        operatorSelector = new OperatorSelector(random);
        
        // 注册时间向算子
        operatorSelector.registerTimeOperator(new T1_CriticalBatchFrontInsert(random, problem));
        operatorSelector.registerTimeOperator(new T2_CriticalBatchAreaTransfer(random, problem));
        operatorSelector.registerTimeOperator(new T3_DiscreteCriticalBlockSwap(random, problem));
        operatorSelector.registerTimeOperator(new T4_DiscreteCriticalOpTimeReassign(random, problem));
        operatorSelector.registerTimeOperator(new T5_MachineReassignment(random, problem));
        
        // 注册能耗向算子
        operatorSelector.registerEnergyOperator(new E1_PrintConsolidationMove(random));
        operatorSelector.registerEnergyOperator(new E2_DiscreteEnergyOptimalReassign(random));
        
        // 创建局部搜索引擎
        localSearchEngine = new LocalSearchEngine(
            problem, 
            operatorSelector, 
            classifier, 
            evaluator, 
            objectives
        );
        
        // 配置局部搜索参数
        localSearchEngine.setL(10);
        localSearchEngine.setEta(0.10);
        localSearchEngine.setEpsE(0.05);
        localSearchEngine.setEpsC(0.02);
        localSearchEngine.setImprovC(0.005);
        localSearchEngine.setImprovE(0.01);
    }
    
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
     * 执行MOEA/D算法
     */
    public List<MOIndividual> solve() {
        System.out.println("========================================");
        System.out.println("      MOEA/D 多目标进化算法 (基于分解)");
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
        System.out.println("邻域大小: " + neighborhoodSize);
        System.out.println("聚合函数: " + scalarizingFunction);
        System.out.println("最大代数: " + maxGenerations);
        System.out.println("最大时间: " + maxRunTimeMinutes + " 分钟");
        System.out.println("========================================\n");
        
        startTime = System.currentTimeMillis();
        
        // 1. 初始化权重向量和邻域结构
        System.out.println("初始化权重向量...");
        initializeWeightVectors();
        
        System.out.println("计算邻域结构...");
        initializeNeighborhoods();
        
        // 2. 初始化种群
        System.out.println("初始化种群...");
        initializePopulation();
        
        // 3. 评价初始种群并初始化理想点和nadir点
        System.out.println("评价初始种群...");
        evaluatePopulation(population);
        initializeReferencePoints();
        
        System.out.println("初始理想点: " + Arrays.toString(idealPoint));
        System.out.println("\n开始进化...\n");
        
        currentGeneration = 1;
        
        // 4. 主循环
        while (!isTimeUp()) {
            
            // 5. 对每个子问题进行优化
            for (int i = 0; i < populationSize; i++) {
                
                // 5.1 选择邻域类型（邻域或全局）
                List<Integer> matingPool;
                if (random.nextDouble() < neighborhoodSelectionProb) {
                    // 从邻域中选择
                    //将int[]改为Integer[]
                    matingPool = new ArrayList<>();
                    for (int j = 0; j < neighborhoodSize; j++) {
                        matingPool.add(neighborhoods[i][j]);
                    };
                } else {
                    // 从全局选择
                    matingPool = new ArrayList<>();
                    for (int j = 0; j < populationSize; j++) {
                        matingPool.add(j);
                    }
                }
                
                // 5.2 从交配池中选择父代
                int parent1 = matingPool.get(random.nextInt(matingPool.size()));
                int parent2 = matingPool.get(random.nextInt(matingPool.size()));
                while (parent2 == parent1 && matingPool.size() > 1) {
                    parent2 = matingPool.get(random.nextInt(matingPool.size()));
                }
                
                // 5.3 生成子代
                MOIndividual offspring = generateOffspring(
                    population.get(parent1), 
                    population.get(parent2)
                );
                
                // 5.4 评价子代
                evaluateIndividual(offspring);
                
                // 5.5 更新理想点和nadir点
                updateReferencePoints(offspring);
                
                // 5.6 更新邻域解
                updateNeighboringSolutions(offspring, i);
            }
            
            // 6. 局部搜索（可选）
            if (enableLocalSearch && currentGeneration % localSearchInterval == 0) {
                applyLocalSearch();
            }
            
            // 7. 记录统计数据
            recordStatistics();
            
            // 8. 输出进度
            if (currentGeneration % 1 == 0 || currentGeneration == 1) {
                printProgress();
            }
            
            currentGeneration++;
        }
        
        // 9. 提取最终Pareto前沿
        paretoFront = extractParetoFront();
        
        // 10. 输出最终结果
        printFinalResults();
        
        return paretoFront;
    }
    
    /**
     * 初始化权重向量
     * 使用均匀设计或Simplex-Lattice设计
     */
    private void initializeWeightVectors() {
        int numObjectives = objectives.size();
        
        if (numObjectives == 2) {
            // 双目标：均匀分布
            weightVectors = new double[populationSize][numObjectives];
            for (int i = 0; i < populationSize; i++) {
                double w1 = (double) i / (populationSize - 1);
                weightVectors[i][0] = w1;
                weightVectors[i][1] = 1.0 - w1;
            }
        } else if (numObjectives == 3) {
            // 三目标：使用Simplex-Lattice设计
            List<double[]> weights = new ArrayList<>();
            int H = (int) Math.sqrt(2 * populationSize);  // 划分数
            
            for (int i = 0; i <= H; i++) {
                for (int j = 0; j <= H - i; j++) {
                    int k = H - i - j;
                    double[] w = new double[3];
                    w[0] = (double) i / H;
                    w[1] = (double) j / H;
                    w[2] = (double) k / H;
                    weights.add(w);
                }
            }
            
            // 如果生成的权重数量超过populationSize，随机采样
            if (weights.size() > populationSize) {
                Collections.shuffle(weights, random);
                weights = weights.subList(0, populationSize);
            } else if (weights.size() < populationSize) {
                // 如果不足，随机生成额外的权重
                while (weights.size() < populationSize) {
                    double[] w = new double[3];
                    double sum = 0;
                    for (int i = 0; i < 3; i++) {
                        w[i] = random.nextDouble();
                        sum += w[i];
                    }
                    for (int i = 0; i < 3; i++) {
                        w[i] /= sum;
                    }
                    weights.add(w);
                }
            }
            
            weightVectors = weights.toArray(new double[0][]);
        } else {
            // 多目标：随机生成归一化权重
            weightVectors = new double[populationSize][numObjectives];
            for (int i = 0; i < populationSize; i++) {
                double sum = 0;
                for (int j = 0; j < numObjectives; j++) {
                    weightVectors[i][j] = random.nextDouble();
                    sum += weightVectors[i][j];
                }
                // 归一化
                for (int j = 0; j < numObjectives; j++) {
                    weightVectors[i][j] /= sum;
                }
            }
        }
        
        System.out.println("生成权重向量数量: " + weightVectors.length);
    }
    
    /**
     * 初始化邻域结构
     * 基于权重向量的欧氏距离
     */
    private void initializeNeighborhoods() {
        neighborhoods = new int[populationSize][neighborhoodSize];
        
        for (int i = 0; i < populationSize; i++) {
            // 计算与其他所有权重向量的距离
            double[] distances = new double[populationSize];
            for (int j = 0; j < populationSize; j++) {
                distances[j] = euclideanDistance(weightVectors[i], weightVectors[j]);
            }
            
            // 找到最近的T个权重向量（包括自己）
            Integer[] indices = new Integer[populationSize];
            for (int j = 0; j < populationSize; j++) {
                indices[j] = j;
            }
            
            // 按距离排序
            Arrays.sort(indices, Comparator.comparingDouble(idx -> distances[idx]));
            
            // 选择最近的T个
            for (int j = 0; j < neighborhoodSize; j++) {
                neighborhoods[i][j] = indices[j];
            }
        }
    }
    
    /**
     * 计算欧氏距离
     */
    private double euclideanDistance(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 初始化种群（完全照搬NSGA-II的多样化初始化策略）
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        Job[] jobs = problem.getJobs();
        int strategySize = populationSize / 5;
        for (int i = 0; i < populationSize; i++) {
            InitializationStrategy strategy;
            strategy = InitializationStrategy.randomStrategy();
            MOIndividual individual = new MOIndividual(jobs, random, problem, strategy, objectives.size());
            population.add(individual);
        }
    }
    
    /**
     * 评价种群
     */
    private void evaluatePopulation(List<MOIndividual> pop) {
        for (MOIndividual individual : pop) {
            evaluateIndividual(individual);
        }
    }
    
    /**
     * 评价单个个体
     */
    private void evaluateIndividual(MOIndividual individual) {
        CaculateFitness.initOperationMatrix(operationMatrix);
        evaluator.evaluate(individual, problem, operationMatrix, objectives);
        individual.operationMatrix = copyOperationMatrix(operationMatrix);
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
     * 初始化理想点和nadir点
     */
    private void initializeReferencePoints() {
        int numObjectives = objectives.size();
        idealPoint = new double[numObjectives];
        nadirPoint = new double[numObjectives];
        
        Arrays.fill(idealPoint, Double.POSITIVE_INFINITY);
        Arrays.fill(nadirPoint, Double.NEGATIVE_INFINITY);
        
        for (MOIndividual ind : population) {
            for (int i = 0; i < numObjectives; i++) {
                idealPoint[i] = Math.min(idealPoint[i], ind.objectives[i]);
                nadirPoint[i] = Math.max(nadirPoint[i], ind.objectives[i]);
            }
        }
    }
    
    /**
     * 更新理想点和nadir点
     */
    private void updateReferencePoints(MOIndividual individual) {
        for (int i = 0; i < objectives.size(); i++) {
            idealPoint[i] = Math.min(idealPoint[i], individual.objectives[i]);
            nadirPoint[i] = Math.max(nadirPoint[i], individual.objectives[i]);
        }
    }
    
    /**
     * 生成子代
     */
    private MOIndividual generateOffspring(MOIndividual parent1, MOIndividual parent2) {
        MOIndividual child1 = new MOIndividual(parent1);
        MOIndividual child2 = new MOIndividual(parent2);
        
        // 交叉
        if (random.nextDouble() < crossoverRate) {
            operations.crossover(child1, child2);
        }
        
        // 变异
        if (random.nextDouble() < mutationRate) {
            operations.mutation(child1, operationMatrix);
        }
        
        // 返回其中一个子代
        return random.nextBoolean() ? child1 : child2;
    }
    
    /**
     * 更新邻域解
     */
    private void updateNeighboringSolutions(MOIndividual offspring, int subproblemIndex) {
        // 随机打乱邻域顺序
        List<Integer> neighborIndices = new ArrayList<>();
        for (int idx : neighborhoods[subproblemIndex]) {
            neighborIndices.add(idx);
        }
        Collections.shuffle(neighborIndices, random);
        
        int replacementCount = 0;
        
        for (int idx : neighborIndices) {
            if (replacementCount >= maxReplacementSize) {
                break;
            }
            
            // 计算当前解和子代的聚合函数值
            double currentValue = operations.calculateScalarValue(
                population.get(idx).objectives,
                weightVectors[idx],
                idealPoint,
                scalarizingFunction
            );
            
            double offspringValue = operations.calculateScalarValue(
                offspring.objectives,
                weightVectors[idx],
                idealPoint,
                scalarizingFunction
            );
            
            // 如果子代更优，替换（注意：聚合函数是最小化）
            if (offspringValue < currentValue) {
                population.set(idx, new MOIndividual(offspring));
                replacementCount++;
            }
        }
    }
    
    /**
     * 执行局部搜索
     */
    private void applyLocalSearch() {
        List<MOIndividual> elites = localSearchEngine.selectElites(population);
        
        if (elites.isEmpty()) {
            return;
        }
        
        List<MOIndividual> improvedElites = localSearchEngine.searchElites(elites);
        
        // 尝试将改进后的个体替换到对应的子问题
        for (MOIndividual improved : improvedElites) {
            // 找到最适合该解的子问题（聚合函数值最小）
            int bestSubproblem = -1;
            double bestValue = Double.POSITIVE_INFINITY;
            
            for (int i = 0; i < populationSize; i++) {
                double value = operations.calculateScalarValue(
                    improved.objectives,
                    weightVectors[i],
                    idealPoint,
                    scalarizingFunction
                );
                
                if (value < bestValue) {
                    bestValue = value;
                    bestSubproblem = i;
                }
            }
            
            // 如果找到更优的子问题，更新
            if (bestSubproblem >= 0) {
                double currentValue = operations.calculateScalarValue(
                    population.get(bestSubproblem).objectives,
                    weightVectors[bestSubproblem],
                    idealPoint,
                    scalarizingFunction
                );
                
                if (bestValue < currentValue) {
                    population.set(bestSubproblem, improved);
                }
            }
        }
    }
    
    /**
     * 提取Pareto前沿
     */
    private List<MOIndividual> extractParetoFront() {
        List<MOIndividual> front = new ArrayList<>();
        
        for (MOIndividual ind : population) {
            boolean isDominated = false;
            
            // 检查是否被其他个体支配
            for (MOIndividual other : population) {
                if (other.dominates(ind)) {
                    isDominated = true;
                    break;
                }
            }
            
            if (!isDominated) {
                front.add(new MOIndividual(ind));
            }
        }
        
        return front;
    }
    
    /**
     * 记录统计数据
     */
    private void recordStatistics() {
        List<MOIndividual> currentPareto = extractParetoFront();
        paretoSizeHistory.add(currentPareto.size());
        
        // 计算超体积（仅2目标）
        if (objectives.size() == 2 && !currentPareto.isEmpty()) {
            double[] referencePoint = new double[2];
            referencePoint[0] = nadirPoint[0] * 1.1;
            referencePoint[1] = nadirPoint[1] * 1.1;
            double hv = calculateHypervolume2D(currentPareto, referencePoint);
            hypervolomeHistory.add(hv);
        }
    }
    
    /**
     * 计算2D超体积
     */
    private double calculateHypervolume2D(List<MOIndividual> front, double[] referencePoint) {
        if (front.isEmpty()) {
            return 0.0;
        }
        
        List<MOIndividual> sorted = new ArrayList<>(front);
        sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        double hypervolume = 0.0;
        double prevY = referencePoint[1];
        
        for (MOIndividual ind : sorted) {
            double x = referencePoint[0] - ind.objectives[0];
            double y = prevY - ind.objectives[1];
            
            if (x > 0 && y > 0) {
                hypervolume += x * y;
                prevY = ind.objectives[1];
            }
        }
        
        return hypervolume;
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
        List<MOIndividual> currentPareto = extractParetoFront();
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.printf("代数 %4d | Pareto前沿大小: %3d | 耗时: %.2f秒",
            currentGeneration, currentPareto.size(), elapsed / 1000.0);
        
        if (!currentPareto.isEmpty()) {
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
    
    public double[] getIdealPoint() {
        return idealPoint;
    }
    
    public double[] getNadirPoint() {
        return nadirPoint;
    }
}
