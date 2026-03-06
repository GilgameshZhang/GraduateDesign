package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * SR - Shaw Removal
 * 基于相似度移除相关作业
 * 原论文中使用交期差异，此处改为释放期差异和处理时间差异
 */
public class ShawRemoval implements DestroyOperator {
    private Random random;
    
    public ShawRemoval(Random random) {
        this.random = random;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<Integer> allJobs = solution.getAllJobIds();
        if (allJobs.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<Integer> removedJobs = new ArrayList<>();
        
        // 随机选择一个种子作业
        int seedJob = allJobs.get(random.nextInt(allJobs.size()));
        removedJobs.add(seedJob);
        allJobs.remove(Integer.valueOf(seedJob));
        
        // 基于相似度添加其他作业
        while (removedJobs.size() < numToRemove && !allJobs.isEmpty()) {
            int mostSimilar = -1;
            double minDistance = Double.MAX_VALUE;
            
            for (int jobId : allJobs) {
                // 计算与已移除作业的平均相似度
                double avgDistance = 0.0;
                for (int removedJobId : removedJobs) {
                    avgDistance += calculateDistance(jobs[jobId], jobs[removedJobId]);
                }
                avgDistance /= removedJobs.size();
                
                if (avgDistance < minDistance) {
                    minDistance = avgDistance;
                    mostSimilar = jobId;
                }
            }
            
            if (mostSimilar != -1) {
                removedJobs.add(mostSimilar);
                allJobs.remove(Integer.valueOf(mostSimilar));
            } else {
                break;
            }
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    /**
     * 计算两个作业之间的距离（相似度的逆）
     * 基于释放期差异和最小处理时间差异
     */
    private double calculateDistance(ALNSJob job1, ALNSJob job2) {
        double releaseDiff = Math.abs(job1.releaseTime - job2.releaseTime);
        double procTimeDiff = Math.abs(job1.getMinProcessingTime() - job2.getMinProcessingTime());
        
        // 归一化并组合（简单加权）
        return releaseDiff + procTimeDiff * 0.5;
    }
    
    @Override
    public String getName() {
        return "SR";
    }
}
