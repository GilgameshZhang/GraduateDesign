package ProblemFrame;

/**
 * 遗传算法参数配置类
 * 用于田口实验的参数配置化
 */
public class GAParameters {
    // 种群参数
    public int popSize;              // 种群规模
    public double pr;                // 复制概率
    public double pc;                // 交叉概率
    public double pm;                // 变异概率
    
    // 终止参数（注：主要终止条件由时间控制）
    public int maxStagnantStep;      // 最大停滞步数（早停阈值）
    public double maxRunTime;        // 最大运行时间（分钟）
    
    // 局部搜索参数
    public int localSearchMaxIter;   // 局部搜索最大迭代次数
    public int localSearchNoImprove; // 局部搜索无改进停止阈值
    
    // 邻域算子候选数量
    public int n1n2Candidates;       // N1/N2候选数量（打印段跨机移动）
    public int n3Candidates;         // N3候选数量（批次交换）
    public int n4Candidates;         // N4候选数量（关键块插入）
    public int n5Candidates;         // N5候选数量（机器重分配）
    
    // 初始化策略比例
    public double heuristicRatio;    // 启发式策略比例（0-1）
    
    // 消融实验开关
    public boolean enableLocalSearch;   // 是否启用局部搜索
    public boolean enableHeuristicInit; // 是否启用启发式初始化
    
    // 邻域策略独立开关（细粒度消融实验）
    public boolean enableN1;  // N1: 基于打印时间的跨机移动
    public boolean enableN2;  // N2: 基于面积占用率的跨机移动
    public boolean enableN3;  // N3: 相邻批次交换
    public boolean enableN4;  // N4: 关键块首尾相邻工序交换
    public boolean enableN5;  // N5: 关键工序机器重分配
    
    /**
     * 默认参数配置（当前论文中使用的参数）
     * 注意：终止条件由时间控制，不再使用maxGen
     */
    public static GAParameters getDefaultParameters() {
        GAParameters params = new GAParameters();
        params.popSize = 200;
        params.pr = 0.1;
        params.pc = 0.90;
        params.pm = 0.3;
        params.maxStagnantStep = 30000;  // 300代无改进终止
        params.maxRunTime = 3.0;  // 3分钟运行时间限制
        params.localSearchMaxIter = 10;
        params.localSearchNoImprove = 3;
        params.n1n2Candidates = 10;
        params.n3Candidates = 5;
        params.n4Candidates = 8;
        params.n5Candidates = 6;
        params.heuristicRatio = 0.80;
        params.enableLocalSearch = true;
        params.enableHeuristicInit = true;
        // 默认启用所有邻域策略
        params.enableN1 = true;
        params.enableN2 = true;
        params.enableN3 = true;
        params.enableN4 = true;
        params.enableN5 = true;
        return params;
    }
    
    /**
     * 创建自定义参数配置（用于田口实验）
     */
    public static GAParameters createCustomParameters(
            int popSize, double pc, double pm, int localSearchMaxIter) {
        GAParameters params = getDefaultParameters();
        params.popSize = popSize;
        params.pc = pc;
        params.pm = pm;
        params.localSearchMaxIter = localSearchMaxIter;
        return params;
    }
    
    /**
     * 创建消融实验参数配置
     * @param enableLocalSearch 是否启用局部搜索
     * @param enableHeuristicInit 是否启用启发式初始化
     */
    public static GAParameters createAblationParameters(
            boolean enableLocalSearch, boolean enableHeuristicInit) {
        GAParameters params = getDefaultParameters();
        params.enableLocalSearch = enableLocalSearch;
        params.enableHeuristicInit = enableHeuristicInit;
        
        // 如果关闭启发式初始化，将比例设为0
        if (!enableHeuristicInit) {
            params.heuristicRatio = 0.0;
        }
        
        return params;
    }
    
    /**
     * 创建邻域消融实验参数配置（细粒度消融）
     * @param enableN1 是否启用N1（基于打印时间的跨机移动）
     * @param enableN2 是否启用N2（基于面积占用率的跨机移动）
     * @param enableN3 是否启用N3（相邻批次交换）
     * @param enableN4 是否启用N4（关键块首尾相邻工序交换）
     * @param enableN5 是否启用N5（关键工序机器重分配）
     */
    public static GAParameters createNeighborhoodAblationParameters(
            boolean enableN1, boolean enableN2, boolean enableN3, 
            boolean enableN4, boolean enableN5) {
        GAParameters params = getDefaultParameters();
        params.enableLocalSearch = true;  // 必须启用局部搜索
        params.enableN1 = enableN1;
        params.enableN2 = enableN2;
        params.enableN3 = enableN3;
        params.enableN4 = enableN4;
        params.enableN5 = enableN5;
        return params;
    }
    
    @Override
    public String toString() {
        return String.format(
            "GAParameters{\n" +
            "  popSize=%d, pr=%.2f, pc=%.2f, pm=%.2f\n" +
            "  maxStagnantStep=%d (早停阈值), maxRunTime=%.1f分钟\n" +
            "  localSearch: %s (maxIter=%d, noImprove=%d)\n" +
            "  heuristicInit: %s (ratio=%.2f)\n" +
            "  candidates: N1N2=%d, N3=%d, N4=%d, N5=%d\n" +
            "  neighborhoods: N1=%s, N2=%s, N3=%s, N4=%s, N5=%s\n" +
            "}",
            popSize, pr, pc, pm,
            maxStagnantStep, maxRunTime,
            enableLocalSearch ? "启用" : "关闭", localSearchMaxIter, localSearchNoImprove,
            enableHeuristicInit ? "启用" : "关闭", heuristicRatio,
            n1n2Candidates, n3Candidates, n4Candidates, n5Candidates,
            enableN1 ? "ON" : "OFF", enableN2 ? "ON" : "OFF", 
            enableN3 ? "ON" : "OFF", enableN4 ? "ON" : "OFF", enableN5 ? "ON" : "OFF"
        );
    }
}
