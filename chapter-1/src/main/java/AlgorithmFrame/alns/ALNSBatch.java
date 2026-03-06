package AlgorithmFrame.alns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ALNS算法中的批次表示
 * 一个批次包含多个作业及其在打印平台上的放置坐标
 */
public class ALNSBatch {
    // 批次中的作业列表
    public List<Integer> jobs;
    // 每个作业的放置信息 jobId -> Placement
    public Map<Integer, Placement> placements;
    // 批次释放期（批内所有作业的最大释放期）
    public double releaseTime;
    // 批次处理时间（取决于机器）
    public double processingTime;
    // 批次开始时间
    public double startTime;
    // 批次完工时间
    public double endTime;
    
    public ALNSBatch() {
        this.jobs = new ArrayList<>();
        this.placements = new HashMap<>();
        this.releaseTime = 0.0;
        this.processingTime = 0.0;
        this.startTime = 0.0;
        this.endTime = 0.0;
    }
    
    /**
     * 添加作业到批次
     */
    public void addJob(int jobId, Placement placement) {
        jobs.add(jobId);
        placements.put(jobId, placement);
    }
    
    /**
     * 移除作业
     */
    public void removeJob(int jobId) {
        jobs.remove(Integer.valueOf(jobId));
        placements.remove(jobId);
    }
    
    /**
     * 复制批次
     */
    public ALNSBatch copy() {
        ALNSBatch newBatch = new ALNSBatch();
        newBatch.jobs = new ArrayList<>(this.jobs);
        newBatch.placements = new HashMap<>();
        for (Map.Entry<Integer, Placement> entry : this.placements.entrySet()) {
            newBatch.placements.put(entry.getKey(), entry.getValue().copy());
        }
        newBatch.releaseTime = this.releaseTime;
        newBatch.processingTime = this.processingTime;
        newBatch.startTime = this.startTime;
        newBatch.endTime = this.endTime;
        return newBatch;
    }
    
    /**
     * 作业的放置信息
     */
    public static class Placement {
        public double x1, y1, x2, y2;
        public boolean rotated;
        
        public Placement(double x1, double y1, double x2, double y2, boolean rotated) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.rotated = rotated;
        }
        
        public Placement copy() {
            return new Placement(x1, y1, x2, y2, rotated);
        }
    }
}
