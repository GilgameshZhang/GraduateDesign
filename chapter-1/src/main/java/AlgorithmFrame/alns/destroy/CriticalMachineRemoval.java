package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * TR - Tardiness Removal (改为关键机器移除)
 * 移除完工时间最大的机器（关键机器）上的所有作业
 * 触发大规模重排以跳出局部最优
 */
public class CriticalMachineRemoval implements DestroyOperator {
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        double maxCompletionTime = -1.0;
        int criticalMachine = -1;
        
        // 找出完工时间最大的机器
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            if (!batches.isEmpty()) {
                ALNSBatch lastBatch = batches.get(batches.size() - 1);
                if (lastBatch.endTime > maxCompletionTime) {
                    maxCompletionTime = lastBatch.endTime;
                    criticalMachine = machineId;
                }
            }
        }
        
        if (criticalMachine == -1) {
            return new ArrayList<>();
        }
        
        // 收集关键机器上的所有作业
        List<Integer> removedJobs = new ArrayList<>();
        for (ALNSBatch batch : solution.getMachineBatches(criticalMachine)) {
            removedJobs.addAll(batch.jobs);
        }
        
        // 限制移除数量
        if (removedJobs.size() > numToRemove) {
            removedJobs = removedJobs.subList(0, numToRemove);
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    @Override
    public String getName() {
        return "TR";
    }
}
