package ProblemFrame.localsearch;

import ProblemFrame.MOIndividual;

/**
 * 候选解类
 * 
 * 封装算子产生的邻域解及其Delta快照
 * 用于增量解码和回滚
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class Candidate {
    
    /** 修改后的个体 */
    public MOIndividual individual;
    
    /** Delta快照（用于增量解码和回滚） */
    public Delta delta;
    
    /** 产生此候选解的算子名称（用于调试和统计） */
    public String operatorName;
    
    /**
     * 构造函数
     * 
     * @param individual 修改后的个体
     * @param delta Delta快照
     * @param operatorName 算子名称
     */
    public Candidate(MOIndividual individual, Delta delta, String operatorName) {
        this.individual = individual;
        this.delta = delta;
        this.operatorName = operatorName;
    }
    
    /**
     * 检查候选解是否有效
     */
    public boolean isValid() {
        return individual != null && delta != null;
    }
    
    /**
     * 获取受影响实体的概要信息
     */
    public String getAffectedSummary() {
        return delta != null ? delta.getAffectedSummary() : "无Delta";
    }
    
    @Override
    public String toString() {
        return String.format("Candidate[%s] - %s", 
            operatorName,
            getAffectedSummary());
    }
}
