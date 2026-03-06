import AlgorithmFrame.alns.ALNSBatch;
import AlgorithmFrame.alns.ALNSParameters;
import AlgorithmFrame.alns.ALNSRunner;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.List;

/**
 * ALNS算法快速测试类
 * 用于验证算法实现的正确性
 */
public class QuickTestALNS {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("ALNS-SL算法快速测试");
        System.out.println("========================================\n");
        
        // 测试1: 最小规模算例
        test1SmallInstance();
        
        // 测试2: 自定义参数
        test2CustomParameters();
        
        System.out.println("\n========================================");
        System.out.println("所有测试完成！");
        System.out.println("========================================");
    }
    
    /**
     * 测试1: 使用默认参数运行小规模算例
     */
    private static void test1SmallInstance() {
        System.out.println("【测试1】小规模算例 - 默认参数");
        System.out.println("----------------------------------------");
        
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        
        try {
            ALNSSolution solution = ALNSRunner.runALNSWithDefaultParams(machinePath, itemPath);
            
            if (solution != null) {
                System.out.println("✅ 测试1通过");
                System.out.println("   最优Cmax: " + solution.cmax);
                System.out.println("   批次总数: " + countTotalBatches(solution));
            } else {
                System.out.println("❌ 测试1失败：无法生成解");
            }
        } catch (Exception e) {
            System.out.println("❌ 测试1异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 测试2: 使用自定义参数
     */
    private static void test2CustomParameters() {
        System.out.println("【测试2】自定义参数测试");
        System.out.println("----------------------------------------");
        
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        
        try {
            // 使用较少的迭代次数进行快速测试
            ALNSParameters params = new ALNSParameters();
            params.maxIterations = 500;
            params.timeLimitMs = 30000; // 30秒
            params.theta = 0.2; // 增加破坏程度
            
            System.out.println("参数设置:");
            System.out.println("  最大迭代次数: " + params.maxIterations);
            System.out.println("  时间限制: " + params.timeLimitMs + "ms");
            System.out.println("  移除比例: " + params.theta);
            
            ALNSSolution solution = ALNSRunner.runALNS(machinePath, itemPath, params, 54321L);
            
            if (solution != null) {
                System.out.println("✅ 测试2通过");
                System.out.println("   最优Cmax: " + solution.cmax);
                
                // 输出每台机器的详细信息
                printDetailedSolution(solution);
            } else {
                System.out.println("❌ 测试2失败：无法生成解");
            }
        } catch (Exception e) {
            System.out.println("❌ 测试2异常: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * 统计总批次数
     */
    private static int countTotalBatches(ALNSSolution solution) {
        int total = 0;
        for (int machineId = 0; machineId < solution.machineBatches.size(); machineId++) {
            total += solution.getMachineBatches(machineId).size();
        }
        return total;
    }
    
    /**
     * 打印详细解信息
     */
    private static void printDetailedSolution(ALNSSolution solution) {
        System.out.println("\n   详细解信息:");
        
        for (int machineId = 0; machineId < solution.machineBatches.size(); machineId++) {
            List<ALNSBatch> batches = solution.getMachineBatches(machineId);
            
            if (batches.isEmpty()) {
                System.out.println("   机器" + machineId + ": (空闲)");
            } else {
                System.out.println("   机器" + machineId + " (" + batches.size() + "个批次):");
                
                for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                    ALNSBatch batch = batches.get(batchIdx);
                    System.out.println(String.format(
                        "     批次%d: %d个作业, [%.1f, %.1f], 时长=%.1f",
                        batchIdx,
                        batch.jobs.size(),
                        batch.startTime,
                        batch.endTime,
                        batch.processingTime
                    ));
                }
            }
        }
    }
}
