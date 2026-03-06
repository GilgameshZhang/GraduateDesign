package ProblemFrame;

/**
 * 设备能耗参数
 * 
 * 用于"绿色车间"能耗优化，支持开/关机策略
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class PowerParameters {
    
    /** 运行功率 (kW) - 设备加工/处理期间的功率 */
    public double P_run;
    
    /** 待机功率 (kW) - 设备开机但空闲时的功率 */
    public double P_idle;
    
    /** 开关机能耗代价 (kWh) - 一次"关机+再开机"的总能耗 */
    public double E_switch;
    
    /** 预热时间 (时间单位) - 开机/预热所需时间，影响时间轴 */
    public double T_warmup;
    
    /** 是否允许关机策略 - 某些设备可能不允许频繁开关机 */
    public boolean allowSwitch;
    
    /**
     * 默认构造函数 - 创建不允许关机的配置
     */
    public PowerParameters() {
        this.P_run = 1.0;
        this.P_idle = 0.1;
        this.E_switch = 0.5;
        this.T_warmup = 0.0;
        this.allowSwitch = false;
    }
    
    /**
     * 完整参数构造函数
     */
    public PowerParameters(double P_run, double P_idle, double E_switch, 
                          double T_warmup, boolean allowSwitch) {
        this.P_run = P_run;
        this.P_idle = P_idle;
        this.E_switch = E_switch;
        this.T_warmup = T_warmup;
        this.allowSwitch = allowSwitch;
    }
    
    /**
     * 计算盈亏平衡时间 T_breakEven
     * 
     * 定义：当空闲时间 > T_breakEven + T_warmup 时，关机比待机更节能
     * 
     * T_breakEven = E_switch / P_idle
     * 
     * @return 盈亏平衡时间；若 P_idle=0 返回无穷大
     */
    public double getBreakEvenTime() {
        if (P_idle <= 1e-9) {
            return Double.POSITIVE_INFINITY;
        }
        return E_switch / P_idle;
    }
    
    /**
     * 判断给定空闲间隔是否应该关机
     * 
     * 关机条件：
     * 1. allowSwitch = true
     * 2. gapDuration >= T_warmup + T_breakEven
     * 
     * @param gapDuration 空闲间隔时长
     * @return true: 应该关机；false: 应该待机
     */
    public boolean shouldShutdown(double gapDuration) {
        if (!allowSwitch) {
            return false;
        }
        
        if (gapDuration < T_warmup) {
            // 空闲时间不足以完成预热，必须待机
            return false;
        }
        
        double T_be = getBreakEvenTime();
        return gapDuration >= (T_warmup + T_be);
    }
    
    /**
     * 计算空闲间隔的能耗
     * 
     * @param gapDuration 空闲间隔时长
     * @return 能耗 (kWh)
     */
    public double calculateGapEnergy(double gapDuration) {
        if (gapDuration <= 0) {
            return 0.0;
        }
        
        if (shouldShutdown(gapDuration)) {
            // 关机策略：E_switch + 可能的预热期待机能耗
            // 简化模型：E_switch 已包含开关机总能耗，预热期不再额外计算
            return E_switch;
        } else {
            // 待机策略：P_idle * gapDuration
            return P_idle * gapDuration;
        }
    }
    
    /**
     * 创建打印机默认参数（允许关机）
     */
    public static PowerParameters createPrintMachineDefault() {
        return new PowerParameters(
            5.0,   // P_run: 打印时功率较高
            0.5,   // P_idle: 待机功率
            2.0,   // E_switch: 开关机代价
            5.0,   // T_warmup: 预热时间
            true   // allowSwitch: 允许关机
        );
    }
    
    /**
     * 创建批处理机默认参数（不允许关机，因为需要保持温度）
     */
    public static PowerParameters createBatchMachineDefault() {
        return new PowerParameters(
            3.0,   // P_run: 批处理时功率
            0.8,   // P_idle: 待机功率较高（保持温度）
            5.0,   // E_switch: 开关机代价很高
            20.0,  // T_warmup: 预热时间很长
            false  // allowSwitch: 不允许关机
        );
    }
    
    /**
     * 创建离散加工机默认参数（允许关机）
     */
    public static PowerParameters createDiscreteMachineDefault() {
        return new PowerParameters(
            2.0,   // P_run: 离散加工功率
            0.3,   // P_idle: 待机功率
            1.0,   // E_switch: 开关机代价
            3.0,   // T_warmup: 预热时间
            true   // allowSwitch: 允许关机
        );
    }
    
    @Override
    public String toString() {
        return String.format("PowerParams[P_run=%.2f, P_idle=%.2f, E_switch=%.2f, " +
                           "T_warmup=%.2f, allowSwitch=%b, T_be=%.2f]",
                P_run, P_idle, E_switch, T_warmup, allowSwitch, getBreakEvenTime());
    }
}

