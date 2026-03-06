package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * GIR - Greedy Insertion by Release time
 * 按释放期升序贪婪插入，评价改为最小ΔCmax
 */
public class GreedyInsertionByRelease implements RepairOperator {
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        // 按释放期升序排序
        List<Integer> sortedJobs = new ArrayList<>(removedJobs);
        sortedJobs.sort(Comparator.comparingDouble(jobId -> jobs[jobId].releaseTime));
        
        // 逐个插入
        for (int jobId : sortedJobs) {
            if (!insertJobMinimizingCmax(solution, jobId, jobs, machines, evaluator, packer)) {
                return false; // 插入失败
            }
        }
        
        return true;
    }
    
    /**
     * 将作业插入到使Cmax增量最小的位置
     */
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
                
                // 尝试装箱
                ALNSBatch newBatch = packer.packJobsIntoBatch(testJobs, machines[machineId], jobs);
                if (newBatch != null) {
                    // 评估Cmax变化
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
        
        // 执行最佳插入
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
        int batchIndex; // -1表示新建批次
        ALNSBatch batch;
        
        InsertionPosition(int machineId, int batchIndex, ALNSBatch batch) {
            this.machineId = machineId;
            this.batchIndex = batchIndex;
            this.batch = batch;
        }
    }
    
    @Override
    public String getName() {
        return "GIR";
    }
}
