package AlgorithmFrame.alns.localsearch;

import AlgorithmFrame.alns.*;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 局部搜索
 * 包含三个邻域：Relocation, Swap, Split
 */
public class LocalSearch {
    private ALNSJob[] jobs;
    private ALNSMachine[] machines;
    private ALNSEvaluator evaluator;
    private SkylinePackingAdapter packer;
    
    public LocalSearch(ALNSJob[] jobs, ALNSMachine[] machines, 
                      ALNSEvaluator evaluator, SkylinePackingAdapter packer) {
        this.jobs = jobs;
        this.machines = machines;
        this.evaluator = evaluator;
        this.packer = packer;
    }
    
    /**
     * 执行局部搜索（first-improvement）
     */
    public ALNSSolution search(ALNSSolution solution) {
        boolean improved = true;
        ALNSSolution bestSolution = solution.copy();
        int expectedJobCount = jobs.length;
        
        while (improved) {
            improved = false;
            
            // 尝试Relocation
            ALNSSolution relocatedSol = relocation(bestSolution);
            if (relocatedSol != null && relocatedSol.cmax < bestSolution.cmax) {
                // 验证作业数量
                if (relocatedSol.getAllJobIds().size() == expectedJobCount) {
                    bestSolution = relocatedSol;
                    improved = true;
                    continue;
                } else {
                    System.err.println("LocalSearch-Relocation: 作业数不匹配，丢弃该解");
                }
            }
            
            // 尝试Swap
            ALNSSolution swappedSol = swap(bestSolution);
            if (swappedSol != null && swappedSol.cmax < bestSolution.cmax) {
                // 验证作业数量
                if (swappedSol.getAllJobIds().size() == expectedJobCount) {
                    bestSolution = swappedSol;
                    improved = true;
                    continue;
                } else {
                    System.err.println("LocalSearch-Swap: 作业数不匹配，丢弃该解");
                }
            }
            
            // 尝试Split
            ALNSSolution splitSol = split(bestSolution);
            if (splitSol != null && splitSol.cmax < bestSolution.cmax) {
                // 验证作业数量
                if (splitSol.getAllJobIds().size() == expectedJobCount) {
                    bestSolution = splitSol;
                    improved = true;
                } else {
                    System.err.println("LocalSearch-Split: 作业数不匹配，丢弃该解");
                }
            }
        }
        
        return bestSolution;
    }
    
    /**
     * Relocation：将一个作业从当前批次移动到另一个批次或新批次
     */
    private ALNSSolution relocation(ALNSSolution solution) {
        double currentCmax = solution.cmax;
        
        // 枚举所有作业
        for (int sourceMachineId = 0; sourceMachineId < machines.length; sourceMachineId++) {
            List<ALNSBatch> sourceBatches = solution.getMachineBatches(sourceMachineId);
            
            for (int sourceBatchIdx = 0; sourceBatchIdx < sourceBatches.size(); sourceBatchIdx++) {
                ALNSBatch sourceBatch = sourceBatches.get(sourceBatchIdx);
                
                for (int jobId : new ArrayList<>(sourceBatch.jobs)) {
                    // 尝试移动到其他机器的其他批次
                    for (int targetMachineId = 0; targetMachineId < machines.length; targetMachineId++) {
                        List<ALNSBatch> targetBatches = solution.getMachineBatches(targetMachineId);
                        
                        for (int targetBatchIdx = 0; targetBatchIdx < targetBatches.size(); targetBatchIdx++) {
                            // 跳过同一批次
                            if (sourceMachineId == targetMachineId && sourceBatchIdx == targetBatchIdx) {
                                continue;
                            }
                            
                            ALNSSolution newSol = tryRelocate(solution, jobId, 
                                sourceMachineId, sourceBatchIdx, 
                                targetMachineId, targetBatchIdx);
                            
                            if (newSol != null && newSol.cmax < currentCmax) {
                                return newSol;
                            }
                        }
                        
                        // 尝试创建新批次
                        ALNSSolution newSol = tryRelocateToNewBatch(solution, jobId,
                            sourceMachineId, sourceBatchIdx, targetMachineId);
                        
                        if (newSol != null && newSol.cmax < currentCmax) {
                            return newSol;
                        }
                    }
                }
            }
        }
        
        return null;
    }
    
    private ALNSSolution tryRelocate(ALNSSolution solution, int jobId,
                                     int sourceMachineId, int sourceBatchIdx,
                                     int targetMachineId, int targetBatchIdx) {
        ALNSSolution newSol = solution.copy();
        
        // 获取原始批次的作业列表
        ALNSBatch originalSourceBatch = newSol.getMachineBatches(sourceMachineId).get(sourceBatchIdx);
        ALNSBatch originalTargetBatch = newSol.getMachineBatches(targetMachineId).get(targetBatchIdx);
        
        // 构建新的作业列表
        List<Integer> newSourceJobs = new ArrayList<>();
        for (int jid : originalSourceBatch.jobs) {
            if (jid != jobId) {
                newSourceJobs.add(jid);
            }
        }
        
        List<Integer> newTargetJobs = new ArrayList<>(originalTargetBatch.jobs);
        newTargetJobs.add(jobId);
        
        // 先处理目标批次（添加作业）
        ALNSBatch newTargetBatch = packer.packJobsIntoBatch(newTargetJobs, machines[targetMachineId], jobs);
        if (newTargetBatch == null) {
            return null;
        }
        
        // 再处理源批次（移除作业）
        if (!newSourceJobs.isEmpty()) {
            ALNSBatch newSourceBatch = packer.packJobsIntoBatch(newSourceJobs, machines[sourceMachineId], jobs);
            if (newSourceBatch == null) {
                return null;
            }
            newSol.getMachineBatches(sourceMachineId).set(sourceBatchIdx, newSourceBatch);
            newSol.getMachineBatches(targetMachineId).set(targetBatchIdx, newTargetBatch);
        } else {
            // 源批次变空，需要删除
            // 注意：如果在同一机器且目标索引大于源索引，删除源批次后目标索引会-1
            if (sourceMachineId == targetMachineId && targetBatchIdx > sourceBatchIdx) {
                newSol.getMachineBatches(targetMachineId).set(targetBatchIdx, newTargetBatch);
                newSol.getMachineBatches(sourceMachineId).remove(sourceBatchIdx);
            } else {
                newSol.getMachineBatches(sourceMachineId).remove(sourceBatchIdx);
                // 重新获取正确的目标索引
                int adjustedTargetIdx = targetBatchIdx;
                if (sourceMachineId == targetMachineId && sourceBatchIdx < targetBatchIdx) {
                    adjustedTargetIdx = targetBatchIdx - 1;
                }
                newSol.getMachineBatches(targetMachineId).set(adjustedTargetIdx, newTargetBatch);
            }
        }
        
        evaluator.evaluate(newSol);
        return newSol;
    }
    
    private ALNSSolution tryRelocateToNewBatch(ALNSSolution solution, int jobId,
                                               int sourceMachineId, int sourceBatchIdx,
                                               int targetMachineId) {
        ALNSSolution newSol = solution.copy();
        
        // 从源批次移除
        ALNSBatch sourceBatch = newSol.getMachineBatches(sourceMachineId).get(sourceBatchIdx);
        sourceBatch.removeJob(jobId);
        
        // 创建新批次
        List<Integer> newBatchJobs = new ArrayList<>();
        newBatchJobs.add(jobId);
        ALNSBatch newBatch = packer.packJobsIntoBatch(newBatchJobs, machines[targetMachineId], jobs);
        if (newBatch == null) {
            return null;
        }
        
        newSol.addBatch(targetMachineId, newBatch);
        
        // 处理源批次
        if (!sourceBatch.jobs.isEmpty()) {
            ALNSBatch newSourceBatch = packer.packJobsIntoBatch(sourceBatch.jobs, machines[sourceMachineId], jobs);
            if (newSourceBatch == null) {
                return null;
            }
            newSol.getMachineBatches(sourceMachineId).set(sourceBatchIdx, newSourceBatch);
        } else {
            newSol.getMachineBatches(sourceMachineId).remove(sourceBatchIdx);
        }
        
        evaluator.evaluate(newSol);
        return newSol;
    }
    
    /**
     * Swap：交换两个作业的位置
     */
    private ALNSSolution swap(ALNSSolution solution) {
        double currentCmax = solution.cmax;
        
        List<JobLocation> allJobs = new ArrayList<>();
        
        // 收集所有作业位置
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                for (int jobId : batches.get(batchIdx).jobs) {
                    allJobs.add(new JobLocation(jobId, machineId, batchIdx));
                }
            }
        }
        
        // 尝试交换
        for (int i = 0; i < allJobs.size(); i++) {
            for (int j = i + 1; j < allJobs.size(); j++) {
                ALNSSolution newSol = trySwap(solution, allJobs.get(i), allJobs.get(j));
                if (newSol != null && newSol.cmax < currentCmax) {
                    return newSol;
                }
            }
        }
        
        return null;
    }
    
    private ALNSSolution trySwap(ALNSSolution solution, JobLocation loc1, JobLocation loc2) {
        ALNSSolution newSol = solution.copy();
        
        // 特殊情况：如果两个作业在同一个批次，直接返回null（不需要交换）
        if (loc1.machineId == loc2.machineId && loc1.batchIdx == loc2.batchIdx) {
            return null;
        }
        
        // 获取批次（注意：必须用原始批次的jobs列表，而不是修改后的）
        ALNSBatch originalBatch1 = newSol.getMachineBatches(loc1.machineId).get(loc1.batchIdx);
        ALNSBatch originalBatch2 = newSol.getMachineBatches(loc2.machineId).get(loc2.batchIdx);
        
        // 构建新的作业列表（交换作业）
        List<Integer> newJobs1 = new ArrayList<>();
        for (int jobId : originalBatch1.jobs) {
            if (jobId == loc1.jobId) {
                newJobs1.add(loc2.jobId);  // 用job2替换job1
            } else {
                newJobs1.add(jobId);
            }
        }
        
        List<Integer> newJobs2 = new ArrayList<>();
        for (int jobId : originalBatch2.jobs) {
            if (jobId == loc2.jobId) {
                newJobs2.add(loc1.jobId);  // 用job1替换job2
            } else {
                newJobs2.add(jobId);
            }
        }
        
        // 重新装箱
        ALNSBatch newBatch1 = packer.packJobsIntoBatch(newJobs1, machines[loc1.machineId], jobs);
        ALNSBatch newBatch2 = packer.packJobsIntoBatch(newJobs2, machines[loc2.machineId], jobs);
        
        if (newBatch1 == null || newBatch2 == null) {
            return null;
        }
        
        // 更新批次
        newSol.getMachineBatches(loc1.machineId).set(loc1.batchIdx, newBatch1);
        newSol.getMachineBatches(loc2.machineId).set(loc2.batchIdx, newBatch2);
        
        evaluator.evaluate(newSol);
        return newSol;
    }
    
    /**
     * Split：将一个批次分割成两个批次
     */
    private ALNSSolution split(ALNSSolution solution) {
        double currentCmax = solution.cmax;
        
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                ALNSBatch batch = batches.get(batchIdx);
                
                if (batch.jobs.size() >= 2) {
                    ALNSSolution newSol = trySplit(solution, machineId, batchIdx);
                    if (newSol != null && newSol.cmax < currentCmax) {
                        return newSol;
                    }
                }
            }
        }
        
        return null;
    }
    
    private ALNSSolution trySplit(ALNSSolution solution, int machineId, int batchIdx) {
        ALNSSolution newSol = solution.copy();
        ALNSBatch batch = newSol.getMachineBatches(machineId).get(batchIdx);
        
        if (batch.jobs.size() < 2) {
            return null;
        }
        
        // 简单地将批次分成两半
        int splitPoint = batch.jobs.size() / 2;
        List<Integer> jobs1 = new ArrayList<>(batch.jobs.subList(0, splitPoint));
        List<Integer> jobs2 = new ArrayList<>(batch.jobs.subList(splitPoint, batch.jobs.size()));
        
        ALNSBatch newBatch1 = packer.packJobsIntoBatch(jobs1, machines[machineId], jobs);
        ALNSBatch newBatch2 = packer.packJobsIntoBatch(jobs2, machines[machineId], jobs);
        
        if (newBatch1 == null || newBatch2 == null) {
            return null;
        }
        
        newSol.getMachineBatches(machineId).set(batchIdx, newBatch1);
        newSol.getMachineBatches(machineId).add(batchIdx + 1, newBatch2);
        
        evaluator.evaluate(newSol);
        return newSol;
    }
    
    private static class JobLocation {
        int jobId;
        int machineId;
        int batchIdx;
        
        JobLocation(int jobId, int machineId, int batchIdx) {
            this.jobId = jobId;
            this.machineId = machineId;
            this.batchIdx = batchIdx;
        }
    }
}
