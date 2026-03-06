package AlgorithmFrame.alns;

import java.util.List;

/**
 * ALNS解的评价函数
 * 计算最大完工时间Cmax
 */
public class ALNSEvaluator {
    private ALNSJob[] jobs;
    private ALNSMachine[] machines;
    
    public ALNSEvaluator(ALNSJob[] jobs, ALNSMachine[] machines) {
        this.jobs = jobs;
        this.machines = machines;
    }
    
    /**
     * 评价解并计算Cmax
     */
    public double evaluate(ALNSSolution solution) {
        double maxCmax = 0.0;
        solution.jobCompletionTimes.clear();
        
        // 遍历每台机器
        for (int machineId = 0; machineId < machines.length; machineId++) {
            double currentTime = 0.0;
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            // 遍历机器上的每个批次
            for (ALNSBatch batch : batches) {
                // 计算批次释放期（批内所有作业的最大释放期）
                double batchReleaseTime = 0.0;
                for (int jobId : batch.jobs) {
                    if (jobs[jobId].releaseTime > batchReleaseTime) {
                        batchReleaseTime = jobs[jobId].releaseTime;
                    }
                }
                
                // 计算批次处理时间（批内所有作业在该机器上的最大处理时间）
                double batchProcessingTime = 0.0;
                for (int jobId : batch.jobs) {
                    double jobProcTime = jobs[jobId].getProcessingTime(machineId);
                    if (jobProcTime > batchProcessingTime) {
                        batchProcessingTime = jobProcTime;
                    }
                }
                
                // 批次开始时间 = max(当前时间, 批次释放期)
                double batchStartTime = Math.max(currentTime, batchReleaseTime);
                // 批次完工时间
                double batchEndTime = batchStartTime + batchProcessingTime;
                
                // 更新批次信息
                batch.releaseTime = batchReleaseTime;
                batch.processingTime = batchProcessingTime;
                batch.startTime = batchStartTime;
                batch.endTime = batchEndTime;
                
                // 记录批次中每个作业的完工时间
                for (int jobId : batch.jobs) {
                    solution.jobCompletionTimes.put(jobId, batchEndTime);
                }
                
                // 更新当前时间
                currentTime = batchEndTime;
            }
            
            // 更新最大完工时间
            if (currentTime > maxCmax) {
                maxCmax = currentTime;
            }
        }
        
        solution.cmax = maxCmax;
        return maxCmax;
    }
    
    /**
     * 快速评估插入一个作业到指定批次后的Cmax变化
     */
    public double estimateCmaxAfterInsertion(ALNSSolution solution, int jobId, int machineId, int batchIndex) {
        // 创建临时解进行评估
        ALNSSolution tempSol = solution.copy();
        
        // 如果是新建批次
        if (batchIndex == -1 || batchIndex >= tempSol.getMachineBatches(machineId).size()) {
            ALNSBatch newBatch = new ALNSBatch();
            newBatch.addJob(jobId, null); // 装箱信息在实际操作时确定
            tempSol.addBatch(machineId, newBatch);
        } else {
            // 插入到现有批次
            tempSol.getMachineBatches(machineId).get(batchIndex).addJob(jobId, null);
        }
        
        return evaluate(tempSol);
    }
}
