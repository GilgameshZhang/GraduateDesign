package ProblemFrame;

/**
 * 染色体初始化策略
 */
public class InitializationStrategy {
    
    /**
     * 工序排序策略（用于打印工序）
     */
    public enum OperationSortStrategy {
        RANDOM,              // 随机排序
        AREA_DESCENDING,     // 按面积降序
        HEIGHT_DESCENDING    // 按高度降序
    }
    
    /**
     * 机器选择策略
     */
    public enum MachineSelectionStrategy {
        RANDOM,              // 随机选择
        ROULETTE_WHEEL,      // 轮盘赌选择（基于机器能力）
        LOAD_BALANCE,        // 负载均衡（平均分配）
        SHORTEST_PROCESS_TIME // 选择加工时间最短的机器（适用于离散处理）
    }
    
    // 策略配置
    public OperationSortStrategy printSortStrategy;
    public OperationSortStrategy discreteSortStrategy;
    public MachineSelectionStrategy printMachineStrategy;
    public MachineSelectionStrategy discreteMachineStrategy;
    
    /**
     * 默认策略：全部随机
     */
    public InitializationStrategy() {
        this.printSortStrategy = OperationSortStrategy.RANDOM;
        this.discreteSortStrategy = OperationSortStrategy.RANDOM;
        this.printMachineStrategy = MachineSelectionStrategy.RANDOM;
        this.discreteMachineStrategy = MachineSelectionStrategy.RANDOM;
    }
    
    /**
     * 自定义策略
     */
    public InitializationStrategy(OperationSortStrategy printSort,
                                 OperationSortStrategy discreteSort,
                                 MachineSelectionStrategy printMachine,
                                 MachineSelectionStrategy discreteMachine) {
        this.printSortStrategy = printSort;
        this.discreteSortStrategy = discreteSort;
        this.printMachineStrategy = printMachine;
        this.discreteMachineStrategy = discreteMachine;
    }
    
    /**
     * 启发式策略：按高度降序+轮盘赌选择
     */
    public static InitializationStrategy heuristicStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.HEIGHT_DESCENDING,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.ROULETTE_WHEEL,
            MachineSelectionStrategy.ROULETTE_WHEEL
        );
    }
    
    /**
     * 面积优先策略：按面积降序+轮盘赌选择
     */
    public static InitializationStrategy areaBasedStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.AREA_DESCENDING,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.ROULETTE_WHEEL,
            MachineSelectionStrategy.ROULETTE_WHEEL
        );
    }
    
    /**
     * 完全随机策略
     */
    public static InitializationStrategy randomStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.RANDOM,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.RANDOM,
            MachineSelectionStrategy.RANDOM
        );
    }
    
    /**
     * 负载均衡策略：打印机平均分配+离散机器轮盘赌
     */
    public static InitializationStrategy loadBalanceStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.RANDOM,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.LOAD_BALANCE,  // 打印机负载均衡
            MachineSelectionStrategy.ROULETTE_WHEEL  // 离散机器轮盘赌
        );
    }
    
    /**
     * 高度优先+负载均衡策略：按高度降序+打印机平均分配+离散机器轮盘赌
     */
    public static InitializationStrategy heightBalanceStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.HEIGHT_DESCENDING,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.LOAD_BALANCE,  // 打印机负载均衡
            MachineSelectionStrategy.ROULETTE_WHEEL  // 离散机器轮盘赌
        );
    }
    
    /**
     * 面积优先+负载均衡策略：按面积降序+打印机平均分配+离散机器轮盘赌
     */
    public static InitializationStrategy areaBalanceStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.AREA_DESCENDING,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.LOAD_BALANCE,  // 打印机负载均衡
            MachineSelectionStrategy.ROULETTE_WHEEL  // 离散机器轮盘赌
        );
    }
    
    /**
     * 最短加工时间策略：随机工序+最短加工时间机器选择
     */
    public static InitializationStrategy shortestProcessTimeStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.RANDOM,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.RANDOM,  // 打印机随机选择
            MachineSelectionStrategy.SHORTEST_PROCESS_TIME  // 离散机器选择加工时间最短的
        );
    }
    
    /**
     * 高度优先+最短加工时间策略：按高度降序+最短加工时间机器选择
     */
    public static InitializationStrategy heightShortestProcessStrategy() {
        return new InitializationStrategy(
            OperationSortStrategy.HEIGHT_DESCENDING,
            OperationSortStrategy.RANDOM,
            MachineSelectionStrategy.LOAD_BALANCE,  // 打印机负载均衡
            MachineSelectionStrategy.SHORTEST_PROCESS_TIME  // 离散机器选择加工时间最短的
        );
    }
}

