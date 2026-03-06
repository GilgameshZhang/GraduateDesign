package ProblemFrame.localsearch;

import ProblemFrame.MOIndividual;
import ProgramEntity.Problem;

/**
 * 局部搜索算子接口
 * 
 * 所有局部搜索算子需要实现此接口
 * 
 * 设计原则：
 * - tryApply() 只做结构修改并记录Delta，不做全量解码
 * - 解码由LocalSearchEngine统一调用
 * - 支持自适应成功率统计
 * 
 * @author AI Assistant
 * @version 1.0
 */
public interface Operator {
    
    /**
     * 获取算子名称
     */
    String getName();
    
    /**
     * 获取算子支持的个体类型
     * 
     * @return TIME_DEFICIENT 或 ENERGY_DEFICIENT
     */
    IndividualType getSupportedType();
    
    /**
     * 尝试应用算子到个体
     * 
     * 注意：此方法只做结构修改并记录Delta，不做全量解码
     * 
     * @param individual 待改进的个体（会被修改）
     * @param problem 问题实例
     * @return 候选解（包含修改后的个体和Delta），如果无法应用则返回null
     */
    Candidate tryApply(MOIndividual individual, Problem problem);
    
    /**
     * 当算子产生的邻域解被接受时调用（用于自适应统计）
     */
    default void onAccepted() {
        // 默认空实现
    }
    
    /**
     * 当算子产生的邻域解被拒绝时调用（用于自适应统计）
     */
    default void onRejected() {
        // 默认空实现
    }
    
    /**
     * 获取算子的成功率（用于自适应选择）
     * 
     * @return 成功率，范围[0, 1]
     */
    default double getSuccessRate() {
        return 0.5; // 默认返回50%
    }
    
    /**
     * 重置算子的统计信息
     */
    default void resetStatistics() {
        // 默认空实现
    }
    
    /**
     * 获取算子的详细信息（用于调试）
     */
    default String getInfo() {
        return String.format("%s[%s] - 成功率: %.2f%%", 
            getName(), 
            getSupportedType().getShortName(),
            getSuccessRate() * 100);
    }
}
