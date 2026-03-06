package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * RBI - Regret Batch Insertion (改为基于Cmax的三段式评分)
 * score = γ1*part1 + γ2*part2 + (1-γ1-γ2)*part3
 * part1: 批次开始时间推迟
 * part2: 批次处理时间增加
 * part3: 全局Cmax增量
 */
public class RegretBatchInsertion implements RepairOperator {
    private double gamma1;
    private double gamma2;
    
    public RegretBatchInsertion(double gamma1, double gamma2) {
        this.gamma1 = gamma1;
        this.gamma2 = gamma2;
    }
    
    @Override
    public boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs,
                         ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        List<Integer> remainingJobs = new ArrayList<>(removedJobs);
        
        while (!remainingJobs.isEmpty()) {
            int bestJob = -1;
            InsertionPosition bestPosition = null;
            double minScore = Double.MAX_VALUE;
            
            // 对每个剩余作业，找到最佳插入位置
            for (int jobId : remainingJobs) {
                InsertionEvaluation eval = findBestInsertionWithScore(
                    solution, jobId, jobs, machines, evaluator, packer
                );
                
                if (eval != null && eval.score < minScore) {
                    minScore = eval.score;
                    bestJob = jobId;
                    bestPosition = eval.position;
                }
            }
            
            if (bestJob == -1 || bestPosition == null) {
                return false; // 无法插入
            }
            
            // 执行插入
            if (bestPosition.batchIndex == -1) {
                solution.addBatch(bestPosition.machineId, bestPosition.batch);
            } else {
                solution.getMachineBatches(bestPosition.machineId).set(bestPosition.batchIndex, bestPosition.batch);
            }
            
            remainingJobs.remove(Integer.valueOf(bestJob));
            evaluator.evaluate(solution); // 更新解状态
        }
        
        return true;
    }
    
    private InsertionEvaluation findBestInsertionWithScore(ALNSSolution solution, int jobId, 
                                                          ALNSJob[] jobs, ALNSMachine[] machines,
                                                          ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        double currentCmax = solution.cmax;
        InsertionPosition bestPosition = null;
        double minScore = Double.MAX_VALUE;
        
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            // 尝试插入到现有批次
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ALNSBatch oldBatch = batches.get(batchIdx);
                List<Integer> testJobs = new ArrayList<>(oldBatch.jobs);
                testJobs.add(jobId);
                
                ALNSBatch newBatch = packer.packJobsIntoBatch(testJobs, machines[machineId], jobs);
                if (newBatch != null) {
                    // 创建临时解评估
                    ALNSSolution tempSol = solution.copy();
                    tempSol.getMachineBatches(machineId).set(batchIdx, newBatch);
                    evaluator.evaluate(tempSol);
                    
                    ALNSBatch evaluatedBatch = tempSol.getMachineBatches(machineId).get(batchIdx);
                    
                    // 计算三段式得分
                    double part1 = Math.max(0, evaluatedBatch.startTime - oldBatch.startTime);
                    double part2 = Math.max(0, evaluatedBatch.processingTime - oldBatch.processingTime);
                    double part3 = Math.max(0, tempSol.cmax - currentCmax);
                    
                    double score = gamma1 * part1 + gamma2 * part2 + (1 - gamma1 - gamma2) * part3;
                    
                    if (score < minScore) {
                        minScore = score;
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
                
                double part1 = jobs[jobId].releaseTime;
                double part2 = jobs[jobId].getProcessingTime(machineId);
                double part3 = Math.max(0, newCmax - currentCmax);
                
                double score = gamma1 * part1 + gamma2 * part2 + (1 - gamma1 - gamma2) * part3;
                
                if (score < minScore) {
                    minScore = score;
                    bestPosition = new InsertionPosition(machineId, -1, newBatch);
                }
            }
        }
        
        if (bestPosition != null) {
            return new InsertionEvaluation(bestPosition, minScore);
        }
        return null;
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
    
    private static class InsertionEvaluation {
        InsertionPosition position;
        double score;
        
        InsertionEvaluation(InsertionPosition position, double score) {
            this.position = position;
            this.score = score;
        }
    }
    
    @Override
    public String getName() {
        return "RBI";
    }
}
