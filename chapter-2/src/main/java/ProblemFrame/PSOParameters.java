package ProblemFrame;

/**
 * PSO算法参数配置类
 * 
 * 集中管理PSO的所有参数，便于进行参数调优和对比实验
 */
public class PSOParameters {
    // 粒子群参数
    public int swarmSize;            // 粒子群大小
    public double w;                 // 惯性权重 (inertia weight)
    public double c1;                // 个体学习因子 (cognitive coefficient)
    public double c2;                // 社会学习因子 (social coefficient)
    
    // 终止参数
    public int maxStagnantStep;      // 最大停滞步数
    public double maxRunTime;        // 最大运行时间（分钟）
    
    /**
     * 默认参数配置
     */
    public static PSOParameters getDefaultParameters() {
        PSOParameters params = new PSOParameters();
        params.swarmSize = 200;
        params.w = 0.7;              // 惯性权重（会自适应递减）
        params.c1 = 1.7;             // 个体学习因子
        params.c2 = 1.7;             // 社会学习因子
        params.maxStagnantStep = 30000;  // 300代无改进终止
        params.maxRunTime = 10.0;     // 3分钟运行时间限制
        return params;
    }
    
    /**
     * 自定义参数配置
     */
    public static PSOParameters createCustomParameters(int swarmSize, double w, double c1, double c2) {
        PSOParameters params = new PSOParameters();
        params.swarmSize = swarmSize;
        params.w = w;
        params.c1 = c1;
        params.c2 = c2;
        params.maxStagnantStep = 30000;
        params.maxRunTime = 3.0;
        return params;
    }
    
    @Override
    public String toString() {
        return String.format(
            "PSOParameters{\n" +
            "  swarmSize=%d, w=%.2f, c1=%.2f, c2=%.2f\n" +
            "  maxStagnantStep=%d (早停阈值), maxRunTime=%.1f分钟\n" +
            "}",
            swarmSize, w, c1, c2,
            maxStagnantStep, maxRunTime
        );
    }
}
