package ProblemFrame.localsearch;

/**
 * 个体分类类型枚举（基于分位排名）
 * 
 * 根据在精英集合中的Cmax和Energy分位排名，将个体分为两类：
 * - TIME_DEFICIENT: 时间不足型（Cmax分位高）→ 使用时间向算子
 * - ENERGY_DEFICIENT: 能耗不足型（Energy分位高）→ 使用能耗向算子
 * 
 * @author AI Assistant
 * @version 1.0
 */
public enum IndividualType {
    /**
     * 时间不足型：Cmax分位排名较高，需要降低完工时间
     * 对应算子：T1-T4（关键路径优化、批次调整、离散工序优化）
     */
    TIME_DEFICIENT,
    
    /**
     * 能耗不足型：Energy分位排名较高，需要降低能耗
     * 对应算子：E1-E3（打印集中化、能耗最优换机、启动次数优化）
     */
    ENERGY_DEFICIENT;
    
    /**
     * 获取类型的简称
     */
    public String getShortName() {
        switch (this) {
            case TIME_DEFICIENT:
                return "T";
            case ENERGY_DEFICIENT:
                return "E";
            default:
                return "?";
        }
    }
    
    /**
     * 获取类型的描述
     */
    public String getDescription() {
        switch (this) {
            case TIME_DEFICIENT:
                return "时间不足型（优化Cmax）";
            case ENERGY_DEFICIENT:
                return "能耗不足型（优化Energy）";
            default:
                return "未知类型";
        }
    }
}
