package AlgorithmFrame.alns;

import ProblemFrame.Input;
import ProblemFrame.Item;
import ProblemFrame.Machine;

import java.util.ArrayList;
import java.util.List;

/**
 * ALNS算法适配器
 * 将第一章原始数据结构转换为ALNS算法需要的数据结构
 */
public class ALNSAdapter {
    
    /**
     * 将Input转换为ALNS所需的数据结构
     */
    public static ALNSData convertInput(Input input) {
        // 转换作业
        ALNSJob[] alnsJobs = convertJobs(input.itemList, input.machineList);
        
        // 转换机器
        ALNSMachine[] alnsMachines = convertMachines(input.machineList);
        
        return new ALNSData(alnsJobs, alnsMachines);
    }
    
    /**
     * 转换作业列表
     */
    private static ALNSJob[] convertJobs(List<Item> items, List<Machine> machines) {
        ALNSJob[] alnsJobs = new ALNSJob[items.size()];
        
        for (int i = 0; i < items.size(); i++) {
            Item item = items.get(i);
            
            // 处理时间数组（对于3D打印，处理时间 = 准备时间 + (高度/打印层高) * 换层时间）
            double[] processingTimes = new double[machines.size()];
            for (int j = 0; j < machines.size(); j++) {
                Machine machine = machines.get(j);
                // 简化计算：处理时间 = 准备时间 + 层数 * 换层时间
                double layers = Math.ceil(item.h / machine.printH);
                processingTimes[j] = machine.prepareTime + layers * machine.reCoatingTime;
            }
            
            // 释放期默认为0（如果没有提供）
            double releaseTime = 0.0;
            
            alnsJobs[i] = new ALNSJob(
                i, 
                item.name, 
                item.l,    // width
                item.w,    // height  
                item.h,    // depth (3D高度)
                releaseTime,
                processingTimes
            );
        }
        
        return alnsJobs;
    }
    
    /**
     * 转换机器列表
     */
    private static ALNSMachine[] convertMachines(List<Machine> machines) {
        ALNSMachine[] alnsMachines = new ALNSMachine[machines.size()];
        
        for (int i = 0; i < machines.size(); i++) {
            Machine machine = machines.get(i);
            alnsMachines[i] = new ALNSMachine(
                i,
                machine.name,
                machine.L,
                machine.W,
                machine.printH,
                machine.prepareTime,
                machine.reCoatingTime
            );
        }
        
        return alnsMachines;
    }
    
    /**
     * 将ALNS解转换回原始格式（用于可视化和输出）
     */
    public static void convertSolutionToOriginalFormat(ALNSSolution solution, Input input) {
        // 清空原有的分配结果
        for (Machine machine : input.machineList) {
            machine.batchPlan = new ArrayList<>();
            machine.startTime = new ArrayList<>();
            machine.endTime = new ArrayList<>();
            machine.printItem = new ArrayList<>();
        }
        
        int totalJobsConverted = 0;
        
        // 转换ALNS解
        for (int machineId = 0; machineId < input.machineList.size(); machineId++) {
            Machine machine = input.machineList.get(machineId);
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            for (ALNSBatch batch : batches) {
                // 创建Solution对象（装箱结果）
                ProblemFrame.Solution batchSolution = new ProblemFrame.Solution();
                batchSolution.placeItemList = new ArrayList<>();
                
                double totalArea = 0.0;
                for (int jobId : batch.jobs) {
                    if (jobId < 0 || jobId >= input.itemList.size()) {
                        System.err.println("错误：jobId超出范围: " + jobId + 
                                         " (itemList.size=" + input.itemList.size() + ")");
                        continue;
                    }
                    
                    ALNSBatch.Placement placement = batch.placements.get(jobId);
                    if (placement == null) {
                        System.err.println("错误：找不到作业" + jobId + "的装箱信息");
                        continue;
                    }
                    
                    Item originalItem = input.itemList.get(jobId);
                    
                    ProblemFrame.PlaceItem placeItem = new ProblemFrame.PlaceItem(
                        originalItem.name,
                        placement.x1,
                        placement.y1,
                        placement.rotated ? originalItem.w : originalItem.l,
                        placement.rotated ? originalItem.l : originalItem.w,
                        originalItem.h,
                        placement.rotated
                    );
                    
                    batchSolution.placeItemList.add(placeItem);
                    totalArea += originalItem.l * originalItem.w;
                    totalJobsConverted++;
                }
                
                batchSolution.totalS = totalArea;
                batchSolution.rate = totalArea / (machine.L * machine.W);
                
                machine.batchPlan.add(batchSolution);
                machine.startTime.add(batch.startTime);
                machine.endTime.add(batch.endTime);
            }
        }
        
        System.out.println("转换完成：原始作业数=" + input.itemList.size() + 
                         ", 转换后作业数=" + totalJobsConverted);
        
        if (totalJobsConverted != input.itemList.size()) {
            System.err.println("警告：转换后作业数量不匹配！");
        }
    }
    
    /**
     * ALNS数据包装类
     */
    public static class ALNSData {
        public ALNSJob[] jobs;
        public ALNSMachine[] machines;
        
        public ALNSData(ALNSJob[] jobs, ALNSMachine[] machines) {
            this.jobs = jobs;
            this.machines = machines;
        }
    }
}
