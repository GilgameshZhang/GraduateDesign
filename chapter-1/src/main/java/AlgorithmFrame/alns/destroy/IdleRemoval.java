package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * IR - Idle Removal
 * 移除最长空闲段相关的作业
 */
public class IdleRemoval implements DestroyOperator {
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<IdleSegment> idleSegments = new ArrayList<>();
        
        // 找出所有机器上的空闲段
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            for (int i = 0; i < batches.size() - 1; i++) {
                ALNSBatch currentBatch = batches.get(i);
                ALNSBatch nextBatch = batches.get(i + 1);
                
                double idleTime = nextBatch.startTime - currentBatch.endTime;
                if (idleTime > 0.01) {
                    idleSegments.add(new IdleSegment(machineId, i, i + 1, idleTime));
                }
            }
        }
        
        // 按空闲时间降序排序
        idleSegments.sort((a, b) -> Double.compare(b.idleTime, a.idleTime));
        
        List<Integer> removedJobs = new ArrayList<>();
        
        // 从最大空闲段相关的批次中移除作业
        for (IdleSegment segment : idleSegments) {
            if (removedJobs.size() >= numToRemove) {
                break;
            }
            
            List<ALNSBatch> batches = solution.getMachineBatches(segment.machineId);
            if (segment.batchIndex1 < batches.size()) {
                ALNSBatch batch = batches.get(segment.batchIndex1);
                for (int jobId : new ArrayList<>(batch.jobs)) {
                    if (removedJobs.size() < numToRemove) {
                        removedJobs.add(jobId);
                    }
                }
            }
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    private static class IdleSegment {
        int machineId;
        int batchIndex1;
        int batchIndex2;
        double idleTime;
        
        IdleSegment(int machineId, int batchIndex1, int batchIndex2, double idleTime) {
            this.machineId = machineId;
            this.batchIndex1 = batchIndex1;
            this.batchIndex2 = batchIndex2;
            this.idleTime = idleTime;
        }
    }
    
    @Override
    public String getName() {
        return "IR";
    }
}
