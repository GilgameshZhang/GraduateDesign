package ProblemFrame.localsearch;

/**
 * 机器能耗快照
 * 
 * 用于记录单台机器的能耗状态，支持回滚
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MachineEnergySnapshot {
    
    /** 机器编号 */
    public int machineNo;
    
    /** 加工能耗 */
    public double processingEnergy;
    
    /** 空闲能耗（待机或关机） */
    public double idleEnergy;
    
    /** 启动能耗 */
    public double startupEnergy;
    
    /** 总能耗 */
    public double totalEnergy;
    
    /** 启动次数 */
    public int startupCount;
    
    /** 总空闲时间 */
    public double totalIdleTime;
    
    /** 总加工时间 */
    public double totalProcessingTime;
    
    /**
     * 构造函数
     */
    public MachineEnergySnapshot(int machineNo) {
        this.machineNo = machineNo;
        this.processingEnergy = 0.0;
        this.idleEnergy = 0.0;
        this.startupEnergy = 0.0;
        this.totalEnergy = 0.0;
        this.startupCount = 0;
        this.totalIdleTime = 0.0;
        this.totalProcessingTime = 0.0;
    }
    
    /**
     * 拷贝构造函数
     */
    public MachineEnergySnapshot(MachineEnergySnapshot other) {
        this.machineNo = other.machineNo;
        this.processingEnergy = other.processingEnergy;
        this.idleEnergy = other.idleEnergy;
        this.startupEnergy = other.startupEnergy;
        this.totalEnergy = other.totalEnergy;
        this.startupCount = other.startupCount;
        this.totalIdleTime = other.totalIdleTime;
        this.totalProcessingTime = other.totalProcessingTime;
    }
    
    /**
     * 计算总能耗
     */
    public void updateTotalEnergy() {
        this.totalEnergy = processingEnergy + idleEnergy + startupEnergy;
    }
    
    @Override
    public String toString() {
        return String.format("Machine[%d]: E_total=%.2f (proc=%.2f, idle=%.2f, start=%.2f), starts=%d",
            machineNo, totalEnergy, processingEnergy, idleEnergy, startupEnergy, startupCount);
    }
}
