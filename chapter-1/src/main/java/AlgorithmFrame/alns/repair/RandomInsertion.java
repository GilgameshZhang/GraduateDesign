package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * RIJ - Random Insertion Job
 * 随机插入作业（用于扰动）
 */
public class RandomInsertion implements RepairOperator {
    private Random random;
    
    public RandomInsertion(Random random) {
        this.random = random;
    }
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        // 随机打乱作业顺序
        List<Integer> shuffledJobs = new ArrayList<>(removedJobs);
        Collections.shuffle(shuffledJobs, random);
        
        // 逐个随机插入
        for (int jobId : shuffledJobs) {
            if (!insertJobRandomly(solution, jobId, jobs, machines, evaluator, packer)) {
                return false;
            }
        }
        
        return true;
    }
    
    private boolean insertJobRandomly(ALNSSolution solution, int jobId, ALNSJob[] jobs,
                                     ALNSMachine[] machines, ALNSEvaluator evaluator, 
                                     SkylinePackingAdapter packer) {
        List<InsertionPosition> feasiblePositions = new ArrayList<>();
        
        // 收集所有可行的插入位置
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            // 尝试插入到现有批次
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ALNSBatch batch = batches.get(batchIdx);
                List<Integer> testJobs = new ArrayList<>(batch.jobs);
                testJobs.add(jobId);
                
                ALNSBatch newBatch = packer.packJobsIntoBatch(testJobs, machines[machineId], jobs);
                if (newBatch != null) {
                    feasiblePositions.add(new InsertionPosition(machineId, batchIdx, newBatch));
                }
            }
            
            // 尝试创建新批次
            List<Integer> newBatchJobs = new ArrayList<>();
            newBatchJobs.add(jobId);
            ALNSBatch newBatch = packer.packJobsIntoBatch(newBatchJobs, machines[machineId], jobs);
            if (newBatch != null) {
                feasiblePositions.add(new InsertionPosition(machineId, -1, newBatch));
            }
        }
        
        if (feasiblePositions.isEmpty()) {
            return false;
        }
        
        // 随机选择一个可行位置
        InsertionPosition selectedPosition = feasiblePositions.get(random.nextInt(feasiblePositions.size()));
        
        if (selectedPosition.batchIndex == -1) {
            solution.addBatch(selectedPosition.machineId, selectedPosition.batch);
        } else {
            solution.getMachineBatches(selectedPosition.machineId).set(selectedPosition.batchIndex, selectedPosition.batch);
        }
        
        return true;
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
        return "RIJ";
    }
}
