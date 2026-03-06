package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * DTVR - Due Time Variance Removal (改为最小处理时间方差)
 * 由于没有交期，改为使用作业最小处理时间的方差
 */
public class MinProcessingTimeVarianceRemoval implements DestroyOperator {
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<BatchVariance> batchVariances = new ArrayList<>();
        
        // 计算每个批次的最小处理时间方差
        for (int machineId = 0; machineId < machines.length; machineId++) {
            for (ALNSBatch batch : solution.getMachineBatches(machineId)) {
                if (batch.jobs.isEmpty()) continue;
                
                double variance = calculateMinProcessingTimeVariance(batch, jobs);
                batchVariances.add(new BatchVariance(machineId, batch, variance));
            }
        }
        
        // 按方差降序排序
        batchVariances.sort((a, b) -> Double.compare(b.variance, a.variance));
        
        List<Integer> removedJobs = new ArrayList<>();
        
        // 从方差最大的批次中移除作业
        for (BatchVariance bv : batchVariances) {
            if (removedJobs.size() >= numToRemove) {
                break;
            }
            
            for (int jobId : bv.batch.jobs) {
                if (removedJobs.size() < numToRemove) {
                    removedJobs.add(jobId);
                } else {
                    break;
                }
            }
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    private double calculateMinProcessingTimeVariance(ALNSBatch batch, ALNSJob[] jobs) {
        if (batch.jobs.size() <= 1) return 0.0;
        
        double sum = 0.0;
        for (int jobId : batch.jobs) {
            sum += jobs[jobId].getMinProcessingTime();
        }
        double mean = sum / batch.jobs.size();
        
        double variance = 0.0;
        for (int jobId : batch.jobs) {
            double diff = jobs[jobId].getMinProcessingTime() - mean;
            variance += diff * diff;
        }
        variance /= batch.jobs.size();
        
        return variance;
    }
    
    private static class BatchVariance {
        int machineId;
        ALNSBatch batch;
        double variance;
        
        BatchVariance(int machineId, ALNSBatch batch, double variance) {
            this.machineId = machineId;
            this.batch = batch;
            this.variance = variance;
        }
    }
    
    @Override
    public String getName() {
        return "DTVR";
    }
}
