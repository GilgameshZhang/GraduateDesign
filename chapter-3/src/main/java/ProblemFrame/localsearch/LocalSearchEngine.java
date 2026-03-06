package ProblemFrame.localsearch;

import ProblemFrame.*;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import java.util.*;

/**
 * 局部搜索引擎
 * 
 * 负责：
 * 1. 对精英个体执行局部搜索
 * 2. 调用算子生成邻域解
 * 3. 增量解码和评价
 * 4. 接受准则判断
 * 5. 回滚机制
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class LocalSearchEngine {
    
    // ==================== 依赖组件 ====================
    
    /** 问题实例 */
    private Problem problem;
    
    /** 算子选择器 */
    private OperatorSelector selector;
    
    /** 分位分类器 */
    private PercentileClassifier classifier;
    
    /** 评价器 */
    private MOEvaluator evaluator;
    
    /** 目标函数列表 */
    private List<MOEvaluator.ObjectiveFunction> objectives;
    
    /** 工序矩阵（用于解码） */
    private Operation[][] operationMatrix;
    
    // ==================== 参数配置 ====================
    
    /** 每个精英的局部搜索次数 */
    private int L = 10;
    
    /** 精英选择比例（eta） */
    private double eta = 0.10;
    
    /** T型允许的Energy上涨比例 */
    private double epsE = 0.05;
    
    /** E型允许的Cmax上涨比例 */
    private double epsC = 0.02;
    
    /** T型要求的Cmax最小下降比例 */
    private double improvC = 0;
    
    /** E型要求的Energy最小下降比例 */
    private double improvE = 0;
    
    // ==================== 统计信息 ====================
    
    private int totalAttempts;
    private int totalAccepted;
    private int paretoAccepted;
    private int epsilonAccepted;
    
    /**
     * 构造函数
     */
    public LocalSearchEngine(Problem problem, 
                            OperatorSelector selector,
                            PercentileClassifier classifier,
                            MOEvaluator evaluator,
                            List<MOEvaluator.ObjectiveFunction> objectives) {
        this.problem = problem;
        this.selector = selector;
        this.classifier = classifier;
        this.evaluator = evaluator;
        this.objectives = objectives;
        
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
        
        // 重置统计
        resetStatistics();
    }
    
    // ==================== 参数设置 ====================
    
    public void setL(int L) { this.L = L; }
    public void setEta(double eta) { this.eta = eta; }
    public void setEpsE(double epsE) { this.epsE = epsE; }
    public void setEpsC(double epsC) { this.epsC = epsC; }
    public void setImprovC(double improvC) { this.improvC = improvC; }
    public void setImprovE(double improvE) { this.improvE = improvE; }
    
    // ==================== 主要方法 ====================
    
    /**
     * 选择精英集合（F1 ∪ F2的前eta%）
     * 
     * @param population 当前种群
     * @return 精英集合
     */
    public List<MOIndividual> selectElites(List<MOIndividual> population) {
        // 1. 非支配排序
        List<List<MOIndividual>> fronts = AlgorithmFrame.nsgaii.NSGAIIOperations.fastNonDominatedSort(population);
        
        if (fronts.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 2. 合并F1和F2
        List<MOIndividual> candidates = new ArrayList<>();
        if (fronts.size() >= 1) {
            candidates.addAll(fronts.get(0));
        }
        if (fronts.size() >= 2) {
            candidates.addAll(fronts.get(1));
        }
        
        if (candidates.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 3. 计算拥挤距离（用于选择多样性好的个体）
        AlgorithmFrame.nsgaii.NSGAIIOperations.assignCrowdingDistance(candidates);
        
        // 4. 按拥挤距离降序排序（选择更稀疏区域的个体）
        candidates.sort((a, b) -> -Double.compare(a.crowdingDistance, b.crowdingDistance));
        
        // 5. 选择前eta%
        int eliteCount = Math.max(1, (int) (candidates.size() * eta));
        return new ArrayList<>(candidates.subList(0, Math.min(eliteCount, candidates.size())));
    }
    
    /**
     * 对精英集合执行局部搜索
     * 
     * @param elites 精英个体列表
     * @return 改进后的个体列表（包含原精英和改进解）
     */
    public List<MOIndividual> searchElites(List<MOIndividual> elites) {
        if (elites.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 分位分类
        classifier.classify(elites);
        
        //System.out.println("  局部搜索: " + classifier.getClassificationSummary(elites));
        
        // 2. 对每个精英执行L次局部搜索
        List<MOIndividual> improvedSet = new ArrayList<>();
        
        for (MOIndividual elite : elites) {
            MOIndividual improved = improve(elite, L);
            improvedSet.add(improved);
        }
        
        return improvedSet;
    }
    
    /**
     * 改进单个个体
     * 
     * @param individual 待改进的个体
     * @param maxAttempts 最大尝试次数
     * @return 改进后的个体（如果没有改进则返回原个体）
     */
    public MOIndividual improve(MOIndividual individual, int maxAttempts) {
        MOIndividual current = new MOIndividual(individual);
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 1. 根据个体类型选择算子
            Operator operator = selector.select(current.type);
            if (operator == null) {
                break;  // 没有可用算子
            }
            
            // 2. 创建邻域解的副本（避免修改current）
            MOIndividual neighbor = new MOIndividual(current);
            
            // 3. 尝试应用算子
            Candidate candidate = operator.tryApply(neighbor, problem);
            if (candidate == null || !candidate.isValid()) {
                operator.onRejected();
                continue;  // 算子无法应用
            }
            //System.out.println("使用"+operator.getName());
            // 4. 增量解码（这里简化为全量解码，完整版需要实现增量解码）
            evaluateIndividual(candidate.individual);
            
            totalAttempts++;
            
            // 5. 接受准则判断
            if (accept(current, candidate.individual)) {
                current = candidate.individual;
                operator.onAccepted();
                totalAccepted++;
                //System.out.println("  尝试: " + candidate.operatorName + " " + candidate.getAffectedSummary());
            } else {
                operator.onRejected();
                // 不接受，继续使用current
            }
        }
        
        return current;
    }
    
    /**
     * 评价个体（全量解码）
     */
    private void evaluateIndividual(MOIndividual individual) {
        // 初始化工序矩阵
        CaculateFitness.initOperationMatrix(operationMatrix);
        
        // 评价个体
        evaluator.evaluate(individual, problem, operationMatrix, objectives);
        
        // ✅ 保存operationMatrix副本到个体（供T4/T5使用）
        individual.operationMatrix = copyOperationMatrix(operationMatrix);
    }
    
    /**
     * 深拷贝operationMatrix
     */
    private ProgramEntity.Operation[][] copyOperationMatrix(ProgramEntity.Operation[][] source) {
        if (source == null) {
            return null;
        }
        
        ProgramEntity.Operation[][] copy = new ProgramEntity.Operation[source.length][];
        for (int i = 0; i < source.length; i++) {
            if (source[i] != null) {
                copy[i] = Arrays.copyOf(source[i], source[i].length);
            }
        }
        return copy;
    }
    
    /**
     * 接受准则
     * 
     * 1. Pareto支配优先
     * 2. ε-容忍（按类型）
     * 
     * @param old 当前解
     * @param neu 邻域解
     * @return true表示接受neu
     */
    private boolean accept(MOIndividual old, MOIndividual neu) {
        // 1. Pareto支配检查
        if (neu.dominates(old)) {
            paretoAccepted++;
            return true;
        }
        
        // 2. ε-容忍（按类型）
        if (old.type == IndividualType.TIME_DEFICIENT) {
            // T型：要求Cmax有明确下降，允许Energy小幅上升
            boolean cmaxImproved = neu.objectives[0] < old.objectives[0];
            boolean energyTolerable = neu.objectives[1] <= old.objectives[1] * (1 + epsE);
            
            if (cmaxImproved && energyTolerable) {
                epsilonAccepted++;
                return true;
            }
        } else if (old.type == IndividualType.ENERGY_DEFICIENT) {
            // E型：要求Energy有明确下降，允许Cmax小幅上升
            boolean energyImproved = neu.objectives[1] < old.objectives[1];
            boolean cmaxTolerable = neu.objectives[0] <= old.objectives[0] * (1 + epsC);
            
            if (energyImproved && cmaxTolerable) {
                epsilonAccepted++;
                return true;
            }
        }
        
        return false;
    }
    
    // ==================== 统计方法 ====================
    
    public void resetStatistics() {
        totalAttempts = 0;
        totalAccepted = 0;
        paretoAccepted = 0;
        epsilonAccepted = 0;
    }
    
    public String getStatistics() {
        double acceptRate = totalAttempts > 0 ? 100.0 * totalAccepted / totalAttempts : 0.0;
        return String.format("局部搜索统计: 尝试=%d, 接受=%d (%.1f%%), Pareto=%d, ε-容忍=%d",
            totalAttempts, totalAccepted, acceptRate, paretoAccepted, epsilonAccepted);
    }
}
