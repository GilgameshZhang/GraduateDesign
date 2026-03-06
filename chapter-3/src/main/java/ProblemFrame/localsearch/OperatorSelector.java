package ProblemFrame.localsearch;

import java.util.*;

/**
 * 算子选择器
 * 
 * 基于自适应成功率进行轮盘赌选择
 * 采样概率：p(op) ∝ 0.1 + sr(op)
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class OperatorSelector {
    
    /** 时间向算子池 */
    private List<Operator> timeOperators;
    
    /** 能耗向算子池 */
    private List<Operator> energyOperators;
    
    /** 随机数生成器 */
    private Random random;
    
    /** 基础概率（保证每个算子都有最小概率被选中） */
    private static final double BASE_PROBABILITY = 0.1;
    
    /**
     * 构造函数
     * 
     * @param random 随机数生成器
     */
    public OperatorSelector(Random random) {
        this.random = random;
        this.timeOperators = new ArrayList<>();
        this.energyOperators = new ArrayList<>();
    }
    
    /**
     * 注册时间向算子
     */
    public void registerTimeOperator(Operator operator) {
        if (operator.getSupportedType() == IndividualType.TIME_DEFICIENT) {
            timeOperators.add(operator);
        } else {
            throw new IllegalArgumentException("算子类型不匹配：期望TIME_DEFICIENT");
        }
    }
    
    /**
     * 注册能耗向算子
     */
    public void registerEnergyOperator(Operator operator) {
        if (operator.getSupportedType() == IndividualType.ENERGY_DEFICIENT) {
            energyOperators.add(operator);
        } else {
            throw new IllegalArgumentException("算子类型不匹配：期望ENERGY_DEFICIENT");
        }
    }
    
    /**
     * 根据个体类型选择算子
     * 
     * @param type 个体类型
     * @return 选中的算子，如果没有可用算子则返回null
     */
    public Operator select(IndividualType type) {
        Random r = new Random();
        double rand1 = r.nextDouble();
        List<Operator> pool;
        if (rand1 < 0.8) {
            pool = (type == IndividualType.TIME_DEFICIENT)
                    ? timeOperators
                    : energyOperators;
        } else {
            pool = (type == IndividualType.TIME_DEFICIENT)
                    ? energyOperators
                    : timeOperators;
        }
        if (pool.isEmpty()) {
            return null;
        }
        
        if (pool.size() == 1) {
            return pool.get(0);
        }
        
        // 计算每个算子的权重
        double[] weights = new double[pool.size()];
        double totalWeight = 0.0;
        
        for (int i = 0; i < pool.size(); i++) {
            Operator op = pool.get(i);
            // 权重 = BASE_PROBABILITY + successRate
            weights[i] = BASE_PROBABILITY + op.getSuccessRate();
            totalWeight += weights[i];
        }
        
        // 轮盘赌选择
        double rand = random.nextDouble() * totalWeight;
        double sum = 0.0;
        
        for (int i = 0; i < pool.size(); i++) {
            sum += weights[i];
            if (sum >= rand) {
                return pool.get(i);
            }
        }
        
        // 默认返回最后一个
        return pool.get(pool.size() - 1);
    }
    
    /**
     * 获取所有算子的统计信息
     */
    public String getStatistics() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== 算子统计 ===\n");
        
        sb.append("时间向算子:\n");
        for (Operator op : timeOperators) {
            sb.append("  ").append(op.getInfo()).append("\n");
        }
        
        sb.append("能耗向算子:\n");
        for (Operator op : energyOperators) {
            sb.append("  ").append(op.getInfo()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 重置所有算子的统计信息
     */
    public void resetAllStatistics() {
        for (Operator op : timeOperators) {
            op.resetStatistics();
        }
        for (Operator op : energyOperators) {
            op.resetStatistics();
        }
    }
    
    /**
     * 获取时间向算子数量
     */
    public int getTimeOperatorCount() {
        return timeOperators.size();
    }
    
    /**
     * 获取能耗向算子数量
     */
    public int getEnergyOperatorCount() {
        return energyOperators.size();
    }
}
