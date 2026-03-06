package AlgorithmFrame.alns;

/**
 * ALNS算法参数
 * 采用论文给出的ALNS-SL参数作为基线
 */
public class ALNSParameters {
    // 初始温度
    public double T0 = 14.0;
    
    // 温度衰减系数
    public double beta = 0.7378;
    
    // 移除比例（移除作业数 = theta * n）
    public double theta = 0.1243;
    
    // RBI算子权重参数
    public double gamma1 = 0.3442;
    public double gamma2 = 0.2679;
    
    // 轮盘赌权重更新参数
    public double r = 0.0412;
    
    // 奖励分数
    public int sigma1 = 33;  // 找到新的全局最优解
    public int sigma2 = 24;  // 接受但不是最优
    public int sigma3 = 13;  // 拒绝
    
    // 终止条件
    public int maxIterations = 10000;
    public long timeLimitMs = 300000; // 5分钟
    public int maxIterationsWithoutImprovement = 1000;
    
    // 权重更新频率
    public int weightUpdateInterval = 50;
    
    // 初始算子权重
    public double initialWeight = 1.0;
    
    public ALNSParameters() {
    }
    
    /**
     * 使用自定义参数
     */
    public ALNSParameters(double T0, double beta, double theta, double gamma1, double gamma2,
                         double r, int sigma1, int sigma2, int sigma3) {
        this.T0 = T0;
        this.beta = beta;
        this.theta = theta;
        this.gamma1 = gamma1;
        this.gamma2 = gamma2;
        this.r = r;
        this.sigma1 = sigma1;
        this.sigma2 = sigma2;
        this.sigma3 = sigma3;
    }
}
