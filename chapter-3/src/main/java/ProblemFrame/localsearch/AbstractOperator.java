package ProblemFrame.localsearch;

import java.util.Random;

/**
 * 算子抽象基类
 * 
 * 提供通用的成功率统计功能，简化具体算子的实现
 * 
 * @author AI Assistant
 * @version 1.0
 */
public abstract class AbstractOperator implements Operator {
    
    /** 算子名称 */
    protected String name;
    
    /** 支持的个体类型 */
    protected IndividualType supportedType;
    
    /** 随机数生成器 */
    protected Random random;
    
    /** 成功次数（滑动窗口） */
    private int successCount;
    
    /** 尝试次数（滑动窗口） */
    private int attemptCount;
    
    /** 滑动窗口大小 */
    private static final int WINDOW_SIZE = 50;
    
    /**
     * 构造函数
     * 
     * @param name 算子名称
     * @param supportedType 支持的个体类型
     * @param random 随机数生成器
     */
    public AbstractOperator(String name, IndividualType supportedType, Random random) {
        this.name = name;
        this.supportedType = supportedType;
        this.random = random;
        this.successCount = 0;
        this.attemptCount = 0;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public IndividualType getSupportedType() {
        return supportedType;
    }
    
    @Override
    public void onAccepted() {
        successCount++;
        attemptCount++;
        
        // 滑动窗口：超过限制时重置（保持比例）
        if (attemptCount > WINDOW_SIZE) {
            double ratio = (double) successCount / attemptCount;
            successCount = (int) (ratio * WINDOW_SIZE);
            attemptCount = WINDOW_SIZE;
        }
    }
    
    @Override
    public void onRejected() {
        attemptCount++;
        
        // 滑动窗口：超过限制时重置（保持比例）
        if (attemptCount > WINDOW_SIZE) {
            double ratio = (double) successCount / attemptCount;
            successCount = (int) (ratio * WINDOW_SIZE);
            attemptCount = WINDOW_SIZE;
        }
    }
    
    @Override
    public double getSuccessRate() {
        if (attemptCount == 0) {
            return 0.5; // 初始成功率50%
        }
        return (double) successCount / attemptCount;
    }
    
    @Override
    public void resetStatistics() {
        successCount = 0;
        attemptCount = 0;
    }
    
    /**
     * 获取成功次数
     */
    public int getSuccessCount() {
        return successCount;
    }
    
    /**
     * 获取尝试次数
     */
    public int getAttemptCount() {
        return attemptCount;
    }
    
    @Override
    public String getInfo() {
        return String.format("%s[%s] - 成功率: %.2f%% (%d/%d)", 
            name, 
            supportedType.getShortName(),
            getSuccessRate() * 100,
            successCount,
            attemptCount);
    }
}
