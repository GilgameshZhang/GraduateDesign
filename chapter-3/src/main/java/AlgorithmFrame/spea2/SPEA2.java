package AlgorithmFrame.spea2;

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
 * SPEA2 多目标进化算法
 * Strength Pareto Evolutionary Algorithm 2 (Zitzler et al., 2001)
 * 
 * 主要特点：
 * 1. 改进的适应度分配机制（基于支配强度）
 * 2. 最近邻密度估计技术
 * 3. 外部存档截断方法
 * 4. 更好的多样性保持能力
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SPEA2 {
    
    // ==================== 基础配置 ====================
    private Problem problem;
    private Operation[][] operationMatrix;
    private Random random;
    private SPEA2Operations operations;  // 算子操作类
    
    // ==================== 算法参数 ====================
    private int populationSize = 100;           // 种群大小 N
    private int archiveSize = 100;              // 存档大小 N̄ (通常等于种群大小)
    private double crossoverRate = 0.9;         // 交叉率
    private double mutationRate = 0.1;          // 变异率
    private int maxGenerations = 500;           // 最大代数
    private double maxRunTimeMinutes = 5.0;     // 最大运行时间（分钟）
    private int tournamentSize = 2;             // 二元锦标赛大小
    private int kNearest = 1;                   // k近邻参数（通常设为√(N+N̄)）
    
    // ==================== 多目标配置 ====================
    private List<MOEvaluator.ObjectiveFunction> objectives;
    private MOEvaluator evaluator;
    
    // ==================== 局部搜索配置 ====================
    private boolean enableLocalSearch = false;
    private LocalSearchEngine localSearchEngine;
    private OperatorSelector operatorSelector;
    private PercentileClassifier classifier;
    private int localSearchInterval = 10;
    
    // ==================== 运行时数据 ====================
    private List<MOIndividual> population;      // 当前种群 P_t
    private List<MOIndividual> archive;         // 外部存档 P̄_t
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
    public SPEA2(Problem problem, List<MOEvaluator.ObjectiveFunction> objectives) {
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
        this.operations = new SPEA2Operations(problem, random);
        
        // 初始化统计列表
        this.archiveSizeHistory = new ArrayList<>();
        this.hypervolumeHistory = new ArrayList<>();
        
        // 设置k近邻参数：推荐值为√(N+N̄)
        this.kNearest = (int) Math.sqrt(populationSize + archiveSize);
    }
    
    // ==================== 参数设置 ====================
    
    public void setPopulationSize(int size) {
        this.populationSize = size;
        this.archiveSize = size;  // 默认存档大小等于种群大小
        this.kNearest = (int) Math.sqrt(populationSize + archiveSize);
    }
    
    public void setArchiveSize(int size) {
        this.archiveSize = size;
        this.kNearest = (int) Math.sqrt(populationSize + archiveSize);
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
    
    public void setKNearest(int k) {
        this.kNearest = k;
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
        
        operatorSelector.registerTimeOperator(new T1_CriticalBatchFrontInsert(random, problem));
        operatorSelector.registerTimeOperator(new T2_CriticalBatchAreaTransfer(random, problem));
        operatorSelector.registerTimeOperator(new T3_DiscreteCriticalBlockSwap(random, problem));
        operatorSelector.registerTimeOperator(new T4_DiscreteCriticalOpTimeReassign(random, problem));
        operatorSelector.registerTimeOperator(new T5_MachineReassignment(random, problem));
        
        operatorSelector.registerEnergyOperator(new E1_PrintConsolidationMove(random));
        operatorSelector.registerEnergyOperator(new E2_DiscreteEnergyOptimalReassign(random));
        
        localSearchEngine = new LocalSearchEngine(
            problem, operatorSelector, classifier, evaluator, objectives
        );
        
        localSearchEngine.setL(10);
        localSearchEngine.setEta(0.10);
        localSearchEngine.setEpsE(0.05);
        localSearchEngine.setEpsC(0.02);
        localSearchEngine.setImprovC(0.005);
        localSearchEngine.setImprovE(0.01);
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
     * 执行SPEA2算法
     * 
     * @return Pareto最优解集
     */
    public List<MOIndividual> solve() {
        System.out.println("========================================");
        System.out.println("      SPEA2 多目标进化算法");
        System.out.println("========================================");
        System.out.println("问题: " + problem.getJobCount() + "个工件");
        System.out.println("目标数: " + objectives.size());
        System.out.print("目标函数: ");
        for (MOEvaluator.ObjectiveFunction obj : objectives) {
            System.out.print(obj.getName() + " ");
        }
        System.out.println();
        System.out.println("种群大小: " + populationSize);
        System.out.println("存档大小: " + archiveSize);
        System.out.println("k近邻: " + kNearest);
        System.out.println("交叉率: " + crossoverRate);
        System.out.println("变异率: " + mutationRate);
        System.out.println("最大代数: " + maxGenerations);
        System.out.println("最大时间: " + maxRunTimeMinutes + " 分钟");
        System.out.println("========================================\n");
        
        startTime = System.currentTimeMillis();
        
        // 1. 初始化种群 P_0
        System.out.println("初始化种群...");
        initializePopulation();
        
        // 2. 评价初始种群
        System.out.println("评价初始种群...");
        evaluatePopulation(population);
        
        // 3. 创建初始存档 P̄_0 = ∅
        archive = new ArrayList<>();
        
        // 4. 计算初始适应度并更新存档
        List<MOIndividual> combined = new ArrayList<>(population);
        SPEA2Operations.assignFitness(combined, kNearest);
        archive = SPEA2Operations.environmentalSelection(combined, archiveSize, kNearest);
        
        System.out.println("初始存档大小: " + archive.size());
        System.out.println("\n开始进化...\n");
        
        currentGeneration = 1;
        
        // 5. 主循环
        while (!isTimeUp()) {
            
            // 6. 检查终止条件
            if (currentGeneration > maxGenerations) {
                System.out.println("\n达到最大代数，停止进化");
                break;
            }
            
            // 7. 从存档中选择父代（二元锦标赛）
            List<MOIndividual> matingPool = SPEA2Operations.binaryTournamentSelection(
                archive, populationSize, random
            );
            
            // 8. 生成子代
            population = generateOffspring(matingPool);
            
            // 9. 评价子代
            evaluatePopulation(population);
            
            // 10. 合并种群和存档
            combined = new ArrayList<>(population);
            combined.addAll(archive);
            
            // 11. 计算适应度并更新存档
            SPEA2Operations.assignFitness(combined, kNearest);
            archive = SPEA2Operations.environmentalSelection(combined, archiveSize, kNearest);
            
            // 12. 局部搜索（可选）
            if (enableLocalSearch && currentGeneration % localSearchInterval == 0) {
                applyLocalSearch();
            }
            
            // 13. 记录统计数据
            recordStatistics();
            
            // 14. 输出进度
            if (currentGeneration % 10 == 0 || currentGeneration == 1) {
                printProgress();
            }
            
            currentGeneration++;
        }
        
        // 15. 提取最终Pareto前沿（存档中的非支配解）
        paretoFront = SPEA2Operations.extractNonDominatedSolutions(archive);
        
        // 16. 输出最终结果
        printFinalResults();
        
        return paretoFront;
    }
    
    /**
     * 初始化种群（多样化初始化策略）
     */
    private void initializePopulation() {
        population = new ArrayList<>();
        Job[] jobs = problem.getJobs();
        for (int i = 0; i < populationSize; i++) {
            InitializationStrategy strategy = InitializationStrategy.randomStrategy();
            MOIndividual individual = new MOIndividual(jobs, random, problem, strategy, objectives.size());
            population.add(individual);
        }
    }
    
    /**
     * 评价种群
     */
    private void evaluatePopulation(List<MOIndividual> pop) {
        for (MOIndividual individual : pop) {
            CaculateFitness.initOperationMatrix(operationMatrix);
            evaluator.evaluate(individual, problem, operationMatrix, objectives);
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
    private List<MOIndividual> generateOffspring(List<MOIndividual> matingPool) {
        List<MOIndividual> offspring = new ArrayList<>();
        
        while (offspring.size() < populationSize) {
            // 随机选择两个父代
            int idx1 = random.nextInt(matingPool.size());
            int idx2 = random.nextInt(matingPool.size());
            while (idx2 == idx1) {
                idx2 = random.nextInt(matingPool.size());
            }
            
            MOIndividual parent1 = matingPool.get(idx1);
            MOIndividual parent2 = matingPool.get(idx2);
            
            MOIndividual child1, child2;
            
            // 交叉
            if (random.nextDouble() < crossoverRate) {
                child1 = new MOIndividual(parent1);
                child2 = new MOIndividual(parent2);
                operations.crossover(child1, child2);
            } else {
                child1 = new MOIndividual(parent1);
                child2 = new MOIndividual(parent2);
            }
            
            // 变异
            if (random.nextDouble() < mutationRate) {
                operations.mutation(child1, operationMatrix);
            }
            if (random.nextDouble() < mutationRate) {
                operations.mutation(child2, operationMatrix);
            }
            
            offspring.add(child1);
            if (offspring.size() < populationSize) {
                offspring.add(child2);
            }
        }
        
        return offspring;
    }
    
    /**
     * 执行局部搜索
     */
    private void applyLocalSearch() {
        List<MOIndividual> elites = localSearchEngine.selectElites(archive);
        
        if (elites.isEmpty()) {
            return;
        }
        
        List<MOIndividual> improvedElites = localSearchEngine.searchElites(elites);
        
        // 将改进后的个体加入存档
        List<MOIndividual> combined = new ArrayList<>(archive);
        combined.addAll(improvedElites);
        
        // 重新计算适应度并更新存档
        SPEA2Operations.assignFitness(combined, kNearest);
        archive = SPEA2Operations.environmentalSelection(combined, archiveSize, kNearest);
    }
    
    /**
     * 记录统计数据
     */
    private void recordStatistics() {
        archiveSizeHistory.add(archive.size());
        
        // 计算超体积（仅2目标）
        if (objectives.size() == 2 && !archive.isEmpty()) {
            double[] referencePoint = calculateNadirPoint(archive);
            double hv = SPEA2Operations.calculateHypervolume2D(archive, referencePoint);
            hypervolumeHistory.add(hv);
        }
    }
    
    /**
     * 计算nadir点
     */
    private double[] calculateNadirPoint(List<MOIndividual> front) {
        double[] nadir = new double[objectives.size()];
        Arrays.fill(nadir, Double.NEGATIVE_INFINITY);
        
        for (MOIndividual ind : front) {
            for (int i = 0; i < objectives.size(); i++) {
                nadir[i] = Math.max(nadir[i], ind.objectives[i]);
            }
        }
        
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
        long elapsed = System.currentTimeMillis() - startTime;
        
        System.out.printf("代数 %4d | 存档大小: %3d | 耗时: %.2f秒",
            currentGeneration, archive.size(), elapsed / 1000.0);
        
        if (!archive.isEmpty()) {
            // 显示存档中第一个解的目标值
            System.out.print(" | 示例解: ");
            System.out.print(archive.get(0).objectivesToString());
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
        System.out.println("存档大小: " + archive.size());
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
}
