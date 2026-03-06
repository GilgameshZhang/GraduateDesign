package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * PTI - Processing Time Insertion
 * 优先插入到处理时间最短的机器，但最终位置按最小ΔCmax决定
 */
public class ProcessingTimeInsertion implements RepairOperator {
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        // 对每个作业，找到其最优机器（处理时间最短）并插入
        for (int jobId : removedJobs) {
            if (!insertJobPreferShortestProcessing(solution, jobId, jobs, machines, evaluator, packer)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean insertJobPreferShortestProcessing(ALNSSolution solution, int jobId, ALNSJob[] jobs,
                                                     ALNSMachine[] machines, ALNSEvaluator evaluator, 
                                                     SkylinePackingAdapter packer) {
        double currentCmax = evaluator.evaluate(solution);
        
        // 找到处理时间最短的机器
        int bestMachine = 0;
        double minProcessingTime = jobs[jobId].getProcessingTime(0);
        for (int machineId = 1; machineId < machines.length; machineId++) {
            double procTime = jobs[jobId].getProcessingTime(machineId);
            if (procTime < minProcessingTime) {
                minProcessingTime = procTime;
                bestMachine = machineId;
            }
        }
        
        // 在所有机器上寻找最佳插入位置，但优先考虑处理时间短的机器
        InsertionPosition bestPosition = null;
        double minCmaxIncrease = Double.MAX_VALUE;
        
        // 首先尝试最优机器
        List<Integer> machineOrder = new ArrayList<>();
        machineOrder.add(bestMachine);
        for (int machineId = 0; machineId < machines.length; machineId++) {
            if (machineId != bestMachine) {
                machineOrder.add(machineId);
            }
        }
        
        for (int machineId : machineOrder) {
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
        return "PTI";
    }
}
