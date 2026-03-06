package AlgorithmFrame.alns;

/**
 * ALNS算法中的作业表示
 * 包含作业的尺寸、释放期、处理时间等信息
 */
public class ALNSJob {
    // 作业ID
    public int id;
    // 作业名称
    public String name;
    // 长度
    public double width;
    // 宽度
    public double height;
    // 高度（3D打印中的竖直方向）
    public double depth;
    // 释放期
    public double releaseTime;
    // 每台机器上的处理时间
    public double[] processingTime;
    
    public ALNSJob(int id, String name, double width, double height, double depth, double releaseTime, double[] processingTime) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.depth = depth;
        this.releaseTime = releaseTime;
        this.processingTime = processingTime;
    }
    
    /**
     * 获取在指定机器上的处理时间
     */
    public double getProcessingTime(int machineId) {
        return processingTime[machineId];
    }
    
    /**
     * 获取最小处理时间（用于某些启发式规则）
     */
    public double getMinProcessingTime() {
        double min = Double.MAX_VALUE;
        for (double pt : processingTime) {
            if (pt < min) min = pt;
        }
        return min;
    }
    
    /**
     * 复制作业
     */
    public ALNSJob copy() {
        return new ALNSJob(id, name, width, height, depth, releaseTime, processingTime.clone());
    }
}
