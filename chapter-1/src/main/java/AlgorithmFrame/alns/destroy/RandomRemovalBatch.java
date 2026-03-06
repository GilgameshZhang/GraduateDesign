package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * RRB - Random Removal Batch
 * 随机移除一整批
 */
public class RandomRemovalBatch implements DestroyOperator {
    private Random random;
    
    public RandomRemovalBatch(Random random) {
        this.random = random;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<BatchInfo> allBatches = new ArrayList<>();
        
        // 收集所有批次
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                allBatches.add(new BatchInfo(machineId, batchIdx, batches.get(batchIdx)));
            }
        }
        
        if (allBatches.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 随机选择一个批次
        BatchInfo selectedBatch = allBatches.get(random.nextInt(allBatches.size()));
        List<Integer> removedJobs = new ArrayList<>(selectedBatch.batch.jobs);
        
        // 从解中移除这些作业
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    private static class BatchInfo {
        int machineId;
        int batchIndex;
        ALNSBatch batch;
        
        BatchInfo(int machineId, int batchIndex, ALNSBatch batch) {
            this.machineId = machineId;
            this.batchIndex = batchIndex;
            this.batch = batch;
        }
    }
    
    @Override
    public String getName() {
        return "RRB";
    }
}
