package AlgorithmFrame.alns.packing;

import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.bachSelect.skyLine.SkyLinePacking;
import ProblemFrame.Item;
import ProblemFrame.PlaceItem;
import ProblemFrame.Solution;

import java.util.ArrayList;
import java.util.List;

/**
 * Skyline装箱算法适配器
 * 用于ALNS算法中的装箱操作
 */
public class SkylinePackingAdapter {
    private boolean rotationEnabled;
    
    public SkylinePackingAdapter(boolean rotationEnabled) {
        this.rotationEnabled = rotationEnabled;
    }
    
    /**
     * 尝试将一组作业装入指定机器的一个批次
     * @param jobs 要装箱的作业列表
     * @param machine 目标机器
     * @param alnsJobs 所有作业的映射
     * @return 装箱后的批次，如果无法装箱则返回null
     */
    public ALNSBatch packJobsIntoBatch(List<Integer> jobs, ALNSMachine machine, ALNSJob[] alnsJobs) {
        if (jobs.isEmpty()) {
            return null;
        }
        
        // 转换为Item数组
        Item[] items = new Item[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) {
            ALNSJob job = alnsJobs[jobs.get(i)];
            items[i] = new Item(job.name, job.width, job.height, job.depth);
        }
        
        // 调用Skyline装箱
        SkyLinePacking packing = new SkyLinePacking(machine.width, machine.height, items, rotationEnabled);
        Solution solution = packing.packing();
        
        // 检查是否所有作业都成功装箱
        if (solution.placeItemList.size() != jobs.size()) {
            return null;
        }
        
        // 创建ALNS批次
        ALNSBatch batch = new ALNSBatch();
        for (int i = 0; i < jobs.size(); i++) {
            PlaceItem placeItem = solution.placeItemList.get(i);
            ALNSBatch.Placement placement = new ALNSBatch.Placement(
                placeItem.x, 
                placeItem.y,
                placeItem.x + placeItem.l,
                placeItem.y + placeItem.w,
                placeItem.isRotate
            );
            batch.addJob(jobs.get(i), placement);
        }
        
        return batch;
    }
    
    /**
     * 尝试将一个作业添加到现有批次中
     * @param batch 现有批次
     * @param jobId 要添加的作业ID
     * @param machine 目标机器
     * @param alnsJobs 所有作业映射
     * @return 是否成功添加
     */
    public boolean tryAddJobToBatch(ALNSBatch batch, int jobId, ALNSMachine machine, ALNSJob[] alnsJobs) {
        List<Integer> allJobs = new ArrayList<>(batch.jobs);
        allJobs.add(jobId);
        
        ALNSBatch newBatch = packJobsIntoBatch(allJobs, machine, alnsJobs);
        if (newBatch != null) {
            // 更新批次
            batch.jobs.clear();
            batch.jobs.addAll(newBatch.jobs);
            batch.placements.clear();
            batch.placements.putAll(newBatch.placements);
            return true;
        }
        return false;
    }
    
    /**
     * 使用RandomLS尝试多次装箱
     * @param jobs 要装箱的作业
     * @param machine 目标机器
     * @param alnsJobs 所有作业映射
     * @param maxAttempts 最大尝试次数
     * @return 装箱结果
     */
    public ALNSBatch packWithRandomLS(List<Integer> jobs, ALNSMachine machine, ALNSJob[] alnsJobs, int maxAttempts) {
        ALNSBatch bestBatch = null;
        
        for (int attempt = 0; attempt < maxAttempts; attempt++) {
            // 随机打乱作业顺序
            List<Integer> shuffledJobs = new ArrayList<>(jobs);
            java.util.Collections.shuffle(shuffledJobs);
            
            ALNSBatch batch = packJobsIntoBatch(shuffledJobs, machine, alnsJobs);
            if (batch != null) {
                return batch; // 找到可行解即返回
            }
        }
        
        return bestBatch;
    }
}
