import AlgorithmFrame.alns.*;
import ProblemFrame.Input;
import lombok.var;
import util.ReadDataUtil;

/**
 * ALNS算法诊断程序
 * 用于诊断作业丢失和尺寸错误的问题
 */
public class DiagnoseALNS {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ALNS算法诊断程序");
        System.out.println("========================================\n");
        
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        
        try {
            // 步骤1: 读取数据
            System.out.println("【步骤1】读取原始数据");
            System.out.println("----------------------------------------");
            ReadDataUtil readDataUtil = new ReadDataUtil();
            String[] pathList = {itemPath, machinePath};
            Input input = readDataUtil.getInput(pathList);
            
            System.out.println("原始数据：");
            System.out.println("  机器数量: " + input.machineList.size());
            System.out.println("  作业数量: " + input.itemList.size());
            
            for (int i = 0; i < input.itemList.size(); i++) {
                var item = input.itemList.get(i);
                System.out.println("  作业" + i + ": name=" + item.name + 
                                 ", l=" + item.l + ", w=" + item.w + ", h=" + item.h);
            }
            
            for (int i = 0; i < input.machineList.size(); i++) {
                var machine = input.machineList.get(i);
                System.out.println("  机器" + i + ": L=" + machine.L + ", W=" + machine.W + 
                                 ", H=" + machine.H);
            }
            
            // 步骤2: 转换数据格式
            System.out.println("\n【步骤2】转换数据格式");
            System.out.println("----------------------------------------");
            ALNSAdapter.ALNSData alnsData = ALNSAdapter.convertInput(input);
            
            System.out.println("ALNS数据：");
            System.out.println("  机器数量: " + alnsData.machines.length);
            System.out.println("  作业数量: " + alnsData.jobs.length);
            
            for (int i = 0; i < alnsData.jobs.length; i++) {
                ALNSJob job = alnsData.jobs[i];
                System.out.println("  ALNSJob" + i + ": id=" + job.id + ", name=" + job.name + 
                                 ", width=" + job.width + ", height=" + job.height + 
                                 ", depth=" + job.depth);
            }
            
            for (int i = 0; i < alnsData.machines.length; i++) {
                ALNSMachine machine = alnsData.machines[i];
                System.out.println("  ALNSMachine" + i + ": width=" + machine.width + 
                                 ", height=" + machine.height);
            }
            
            // 步骤3: 测试初始解生成
            System.out.println("\n【步骤3】测试初始解生成");
            System.out.println("----------------------------------------");
            
            ALNSParameters params = new ALNSParameters();
            params.maxIterations = 10; // 只运行10次迭代
            
            ALNS alns = new ALNS(alnsData.jobs, alnsData.machines, params, 12345L);
            ALNSSolution solution = alns.solve();
            
            if (solution != null) {
                System.out.println("\n【步骤4】检查解的完整性");
                System.out.println("----------------------------------------");
                
                int totalJobs = 0;
                for (int machineId = 0; machineId < alnsData.machines.length; machineId++) {
                    var batches = solution.getMachineBatches(machineId);
                    System.out.println("机器" + machineId + ": " + batches.size() + "个批次");
                    
                    for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                        ALNSBatch batch = batches.get(batchIdx);
                        System.out.println("  批次" + batchIdx + ": " + batch.jobs.size() + "个作业");
                        
                        for (int jobId : batch.jobs) {
                            System.out.println("    作业ID=" + jobId + ", name=" + 
                                             alnsData.jobs[jobId].name);
                            totalJobs++;
                        }
                    }
                }
                
                System.out.println("\n总结:");
                System.out.println("  原始作业数: " + alnsData.jobs.length);
                System.out.println("  解中作业数: " + totalJobs);
                System.out.println("  匹配状态: " + (totalJobs == alnsData.jobs.length ? "✓ 正确" : "✗ 错误"));
                
                // 步骤5: 测试转换回原始格式
                System.out.println("\n【步骤5】测试转换回原始格式");
                System.out.println("----------------------------------------");
                
                ALNSAdapter.convertSolutionToOriginalFormat(solution, input);
                
                int convertedJobs = 0;
                for (var machine : input.machineList) {
                    for (var batchSolution : machine.batchPlan) {
                        convertedJobs += batchSolution.placeItemList.size();
                    }
                }
                
                System.out.println("转换后作业数: " + convertedJobs);
                System.out.println("匹配状态: " + (convertedJobs == input.itemList.size() ? "✓ 正确" : "✗ 错误"));
                
                // 检查作业尺寸
                System.out.println("\n【步骤6】检查作业尺寸");
                System.out.println("----------------------------------------");
                
                for (int machineId = 0; machineId < input.machineList.size(); machineId++) {
                    var machine = input.machineList.get(machineId);
                    System.out.println("机器" + machineId + ":");
                    
                    for (int batchIdx = 0; batchIdx < machine.batchPlan.size(); batchIdx++) {
                        var batchSolution = machine.batchPlan.get(batchIdx);
                        System.out.println("  批次" + batchIdx + ":");
                        
                        for (var placeItem : batchSolution.placeItemList) {
                            System.out.println("    " + placeItem.name + 
                                             ": l=" + placeItem.l + ", w=" + placeItem.w + 
                                             ", h=" + placeItem.h + 
                                             ", rotated=" + placeItem.isRotate);
                        }
                    }
                }
                
            } else {
                System.err.println("错误：无法生成解！");
            }
            
        } catch (Exception e) {
            System.err.println("诊断出错: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========================================");
        System.out.println("诊断完成");
        System.out.println("========================================");
    }
}
