package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * PTR - Processing Time Removal
 * 移除处理时间不合适的作业（促使作业迁移到更合适的机器）
 */
public class ProcessingTimeRemoval implements DestroyOperator {
    private Random random;
    
    public ProcessingTimeRemoval(Random random) {
        this.random = random;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<JobMachinePair> candidates = new ArrayList<>();
        
        // 找出所有作业及其当前所在机器
        for (int machineId = 0; machineId < machines.length; machineId++) {
            for (ALNSBatch batch : solution.getMachineBatches(machineId)) {
                for (int jobId : batch.jobs) {
                    ALNSJob job = jobs[jobId];
                    double currentProcTime = job.getProcessingTime(machineId);
                    double minProcTime = job.getMinProcessingTime();
                    
                    // 如果当前机器不是最优机器，则该作业是候选
                    double inefficiency = currentProcTime - minProcTime;
                    if (inefficiency > 0.01) {
                        candidates.add(new JobMachinePair(jobId, machineId, inefficiency));
                    }
                }
            }
        }
        
        // 按低效率排序（降序）
        candidates.sort((a, b) -> Double.compare(b.inefficiency, a.inefficiency));
        
        List<Integer> removedJobs = new ArrayList<>();
        for (int i = 0; i < Math.min(numToRemove, candidates.size()); i++) {
            removedJobs.add(candidates.get(i).jobId);
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    private static class JobMachinePair {
        int jobId;
        int machineId;
        double inefficiency;
        
        JobMachinePair(int jobId, int machineId, double inefficiency) {
            this.jobId = jobId;
            this.machineId = machineId;
            this.inefficiency = inefficiency;
        }
    }
    
    @Override
    public String getName() {
        return "PTR";
    }
}
