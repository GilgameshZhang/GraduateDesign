package ProblemFrame;

/**
 * 遗传算法参数配置类
 */
public class GAParameters {
    // 种群参数
    public int popSize;              // 种群规模
    public double pr;                // 复制概率
    public double pc;                // 交叉概率
    public double pm;                // 变异概率
    public int maxGen;               // 最大迭代代数
    
    // 终止参数
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
     * 默认参数配置
     */
    public static GAParameters getDefaultParameters() {
        GAParameters params = new GAParameters();
        params.popSize = 200;
        params.pr = 0.1;
        params.pc = 0.90;
        params.pm = 0.3;
        params.maxGen = 500;
        params.maxStagnantStep = 30000;
        params.maxRunTime = 3.0;
        params.localSearchMaxIter = 10;
        params.localSearchNoImprove = 3;
        params.n1n2Candidates = 10;
        params.n3Candidates = 5;
        params.n4Candidates = 8;
        params.n5Candidates = 6;
        params.heuristicRatio = 0.80;
        params.enableLocalSearch = true;
        params.enableHeuristicInit = true;
        params.enableN1 = true;
        params.enableN2 = true;
        params.enableN3 = true;
        params.enableN4 = true;
        params.enableN5 = true;
        return params;
    }
}

