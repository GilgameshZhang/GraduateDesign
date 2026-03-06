package ProgramEntity.Machine;

import ProblemFrame.PowerParameters;

/**
 * 机器基类
 * 
 * 支持能耗参数配置（用于绿色车间调度）
 */
public class Machine {
    //机器名称
    public String name;
    
    //机器能耗（简化版，向后兼容）
    public Double eneragyCost;
    
    /**
     * 能耗参数（完整版，用于能耗优化）
     * 
     * 包含：
     * - P_run: 运行功率 (kW)
     * - P_idle: 待机功率 (kW)
     * - E_switch: 开关机能耗 (kWh)
     * - T_warmup: 预热时间
     * - allowSwitch: 是否允许关机
     * 
     * 如果为null，EnergyCalculator会使用默认值
     */
    public PowerParameters powerParams;
    
    public Machine() {
    }
    
    public Machine(String name) {
        this.name = name;
    }
    
    /**
     * 设置能耗参数
     */
    public void setPowerParameters(PowerParameters params) {
        this.powerParams = params;
    }
    
    /**
     * 获取能耗参数
     * 如果未设置，返回null（会使用默认值）
     */
    public PowerParameters getPowerParameters() {
        return this.powerParams;
    }
    
    /**
     * 检查是否已配置能耗参数
     */
    public boolean hasPowerParameters() {
        return this.powerParams != null;
    }
}

