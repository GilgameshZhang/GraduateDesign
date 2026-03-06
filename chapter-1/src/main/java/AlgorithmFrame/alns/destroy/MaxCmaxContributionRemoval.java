package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSEvaluator;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * MTR - Max Tardiness Removal (改为最大Cmax贡献批次移除)
 * 移除对Cmax贡献最大的批次
 */
public class MaxCmaxContributionRemoval implements DestroyOperator {
    private ALNSEvaluator evaluator;
    
    public MaxCmaxContributionRemoval(ALNSEvaluator evaluator) {
        this.evaluator = evaluator;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        double currentCmax = solution.cmax;
        
        // 找出在关键路径上的机器（完工时间等于Cmax的机器）
        int criticalMachine = -1;
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            if (!batches.isEmpty()) {
                ALNSBatch lastBatch = batches.get(batches.size() - 1);
                if (Math.abs(lastBatch.endTime - currentCmax) < 0.01) {
                    criticalMachine = machineId;
                    break;
                }
            }
        }
        
        if (criticalMachine == -1) {
            return new ArrayList<>();
        }
        
        // 评估关键机器上每个批次的移除效果
        List<ALNSBatch> criticalBatches = solution.getMachineBatches(criticalMachine);
        BatchRemovalEffect bestEffect = null;
        
        for (int batchIdx = 0; batchIdx < criticalBatches.size(); batchIdx++) {
            ALNSBatch batch = criticalBatches.get(batchIdx);
            
            // 创建临时解移除该批次
            ALNSSolution tempSol = solution.copy();
            for (int jobId : batch.jobs) {
                tempSol.removeJob(jobId);
            }
            
            double newCmax = evaluator.evaluate(tempSol);
            double cmaxReduction = currentCmax - newCmax;
            
            if (bestEffect == null || cmaxReduction > bestEffect.cmaxReduction) {
                bestEffect = new BatchRemovalEffect(batch, cmaxReduction);
            }
        }
        
        List<Integer> removedJobs = new ArrayList<>();
        if (bestEffect != null) {
            removedJobs.addAll(bestEffect.batch.jobs);
            
            // 从解中移除
            for (int jobId : removedJobs) {
                solution.removeJob(jobId);
            }
        }
        
        return removedJobs;
    }
    
    private static class BatchRemovalEffect {
        ALNSBatch batch;
        double cmaxReduction;
        
        BatchRemovalEffect(ALNSBatch batch, double cmaxReduction) {
            this.batch = batch;
            this.cmaxReduction = cmaxReduction;
        }
    }
    
    @Override
    public String getName() {
        return "MTR";
    }
}
