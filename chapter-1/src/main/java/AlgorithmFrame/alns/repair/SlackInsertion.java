package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * SI - Slack Insertion (改为紧迫度)
 * 原论文slack依赖交期，此处改为紧迫度：s_j = r_j + min_k p_{j,k}
 * 越小越紧迫，越先插入
 */
public class SlackInsertion implements RepairOperator {
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        // 按紧迫度升序排序（越紧迫越先插）
        List<Integer> sortedJobs = new ArrayList<>(removedJobs);
        sortedJobs.sort(Comparator.comparingDouble(jobId -> 
            jobs[jobId].releaseTime + jobs[jobId].getMinProcessingTime()
        ));
        
        // 逐个插入
        for (int jobId : sortedJobs) {
            if (!insertJobMinimizingCmax(solution, jobId, jobs, machines, evaluator, packer)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean insertJobMinimizingCmax(ALNSSolution solution, int jobId, ALNSJob[] jobs,
                                           ALNSMachine[] machines, ALNSEvaluator evaluator, 
                                           SkylinePackingAdapter packer) {
        double currentCmax = evaluator.evaluate(solution);
        
        InsertionPosition bestPosition = null;
        double minCmaxIncrease = Double.MAX_VALUE;
        
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            // 尝试插入到现有批次
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ALNSBatch batch = batches.get(batchIdx);
                List<Integer> testJobs = new ArrayList<>(batch.jobs);
                testJobs.add(jobId);
                
                ALNSBatch newBatch = packer.packJobsIntoBatch(testJobs, machines[machineId], jobs);
                if (newBatch != null) {
                    ALNSSolution tempSol = solution.copy();
                    tempSol.getMachineBatches(machineId).set(batchIdx, newBatch);
                    double newCmax = evaluator.evaluate(tempSol);
                    double cmaxIncrease = newCmax - currentCmax;
                    
                    if (cmaxIncrease < minCmaxIncrease) {
                        minCmaxIncrease = cmaxIncrease;
                        bestPosition = new InsertionPosition(machineId, batchIdx, newBatch);
                    }
                }
            }
            
            // 尝试创建新批次
            List<Integer> newBatchJobs = new ArrayList<>();
            newBatchJobs.add(jobId);
            ALNSBatch newBatch = packer.packJobsIntoBatch(newBatchJobs, machines[machineId], jobs);
            if (newBatch != null) {
                ALNSSolution tempSol = solution.copy();
                tempSol.addBatch(machineId, newBatch);
                double newCmax = evaluator.evaluate(tempSol);
                double cmaxIncrease = newCmax - currentCmax;
                
                if (cmaxIncrease < minCmaxIncrease) {
                    minCmaxIncrease = cmaxIncrease;
                    bestPosition = new InsertionPosition(machineId, -1, newBatch);
                }
            }
        }
        
        if (bestPosition != null) {
            if (bestPosition.batchIndex == -1) {
                solution.addBatch(bestPosition.machineId, bestPosition.batch);
            } else {
                solution.getMachineBatches(bestPosition.machineId).set(bestPosition.batchIndex, bestPosition.batch);
            }
            return true;
        }
        
        return false;
    }
    
    private static class InsertionPosition {
        int machineId;
        int batchIndex;
        ALNSBatch batch;
        
        InsertionPosition(int machineId, int batchIndex, ALNSBatch batch) {
            this.machineId = machineId;
            this.batchIndex = batchIndex;
            this.batch = batch;
        }
    }
    
    @Override
    public String getName() {
        return "SI";
    }
}
