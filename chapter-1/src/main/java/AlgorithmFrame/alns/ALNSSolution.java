package AlgorithmFrame.alns;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ALNS算法的解表示
 * 包含所有机器的批次序列
 */
public class ALNSSolution {
    // 每台机器的批次序列 machineId -> List<Batch>
    public Map<Integer, List<ALNSBatch>> machineBatches;
    // 每个作业的完工时间 jobId -> completionTime
    public Map<Integer, Double> jobCompletionTimes;
    // 目标值：最大完工时间 Cmax
    public double cmax;
    
    public ALNSSolution(int numMachines) {
        this.machineBatches = new HashMap<>();
        for (int i = 0; i < numMachines; i++) {
            machineBatches.put(i, new ArrayList<>());
        }
        this.jobCompletionTimes = new HashMap<>();
        this.cmax = 0.0;
    }
    
    /**
     * 添加批次到指定机器
     */
    public void addBatch(int machineId, ALNSBatch batch) {
        machineBatches.get(machineId).add(batch);
    }
    
    /**
     * 获取指定机器的批次列表
     */
    public List<ALNSBatch> getMachineBatches(int machineId) {
        return machineBatches.get(machineId);
    }
    
    /**
     * 深度复制解
     */
    public ALNSSolution copy() {
        ALNSSolution newSol = new ALNSSolution(machineBatches.size());
        for (Map.Entry<Integer, List<ALNSBatch>> entry : machineBatches.entrySet()) {
            List<ALNSBatch> newBatches = new ArrayList<>();
            for (ALNSBatch batch : entry.getValue()) {
                newBatches.add(batch.copy());
            }
            newSol.machineBatches.put(entry.getKey(), newBatches);
        }
        newSol.jobCompletionTimes = new HashMap<>(this.jobCompletionTimes);
        newSol.cmax = this.cmax;
        return newSol;
    }
    
    /**
     * 获取解中的所有作业ID
     */
    public List<Integer> getAllJobIds() {
        List<Integer> allJobs = new ArrayList<>();
        for (List<ALNSBatch> batches : machineBatches.values()) {
            for (ALNSBatch batch : batches) {
                allJobs.addAll(batch.jobs);
            }
        }
        return allJobs;
    }
    
    /**
     * 移除指定的作业
     */
    public void removeJob(int jobId) {
        for (List<ALNSBatch> batches : machineBatches.values()) {
            for (ALNSBatch batch : batches) {
                if (batch.jobs.contains(jobId)) {
                    batch.removeJob(jobId);
                }
            }
            // 移除空批次
            batches.removeIf(batch -> batch.jobs.isEmpty());
        }
        jobCompletionTimes.remove(jobId);
    }
}
