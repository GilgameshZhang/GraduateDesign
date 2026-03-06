package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GID - Greedy Insertion (改为按最小处理时间降序)
 * 原论文按交期排序，此处改为按p*_j=min_k p_{j,k}降序排序
 */
public class GreedyInsertionByMinProcessing implements RepairOperator {
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        // 按最小处理时间降序排序（长作业优先）
        List<Integer> sortedJobs = new ArrayList<>(removedJobs);
        sortedJobs.sort((j1, j2) -> Double.compare(
            jobs[j2].getMinProcessingTime(), 
            jobs[j1].getMinProcessingTime()
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
        
        // 枚举所有可能的插入位置
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
        return "GID";
    }
}
