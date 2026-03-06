package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * RI - Regret Insertion (改为基于Cmax增量的regret)
 * regret = u2 - u1 (次优Cmax增量 - 最优Cmax增量)
 * regret最大的作业优先插入
 */
public class RegretInsertion implements RepairOperator {
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        List<Integer> remainingJobs = new ArrayList<>(removedJobs);
        
        while (!remainingJobs.isEmpty()) {
            int bestJob = -1;
            InsertionPosition bestPosition = null;
            double maxRegret = -Double.MAX_VALUE;
            
            // 对每个剩余作业，计算regret
            for (int jobId : remainingJobs) {
                RegretEvaluation regretEval = calculateRegret(
                    solution, jobId, jobs, machines, evaluator, packer
                );
                
                if (regretEval != null && regretEval.regret > maxRegret) {
                    maxRegret = regretEval.regret;
                    bestJob = jobId;
                    bestPosition = regretEval.bestPosition;
                }
            }
            
            if (bestJob == -1 || bestPosition == null) {
                return false;
            }
            
            // 执行插入
            if (bestPosition.batchIndex == -1) {
                solution.addBatch(bestPosition.machineId, bestPosition.batch);
            } else {
                solution.getMachineBatches(bestPosition.machineId).set(bestPosition.batchIndex, bestPosition.batch);
            }
            
            remainingJobs.remove(Integer.valueOf(bestJob));
            evaluator.evaluate(solution);
        }
        
        return true;
    }
    
    private RegretEvaluation calculateRegret(ALNSSolution solution, int jobId, ALNSJob[] jobs,
                                             ALNSMachine[] machines, ALNSEvaluator evaluator, 
                                             SkylinePackingAdapter packer) {
        double currentCmax = solution.cmax;
        List<InsertionOption> options = new ArrayList<>();
        
        // 枚举所有可行的插入位置
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
                    
                    options.add(new InsertionOption(
                        new InsertionPosition(machineId, batchIdx, newBatch),
                        cmaxIncrease
                    ));
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
                
                options.add(new InsertionOption(
                    new InsertionPosition(machineId, -1, newBatch),
                    cmaxIncrease
                ));
            }
        }
        
        if (options.size() < 1) {
            return null;
        }
        
        // 按Cmax增量排序
        options.sort((a, b) -> Double.compare(a.cmaxIncrease, b.cmaxIncrease));
        
        double u1 = options.get(0).cmaxIncrease;
        double u2 = (options.size() > 1) ? options.get(1).cmaxIncrease : u1;
        double regret = u2 - u1;
        
        return new RegretEvaluation(options.get(0).position, regret);
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
    
    private static class InsertionOption {
        InsertionPosition position;
        double cmaxIncrease;
        
        InsertionOption(InsertionPosition position, double cmaxIncrease) {
            this.position = position;
            this.cmaxIncrease = cmaxIncrease;
        }
    }
    
    private static class RegretEvaluation {
        InsertionPosition bestPosition;
        double regret;
        
        RegretEvaluation(InsertionPosition bestPosition, double regret) {
            this.bestPosition = bestPosition;
            this.regret = regret;
        }
    }
    
    @Override
    public String getName() {
        return "RI";
    }
}
