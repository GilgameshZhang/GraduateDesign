package test;

import AlgorthmFrame.ga.GA;
import ProgramEntity.Input;
import ProgramEntity.Problem;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 批量运行所有测试算例
 * 对比不同算例的性能
 */
public class RunAllInstances {

    // 算例文件列表
    private static final String[] INSTANCE_FILES = {
        "test_instance_small_2m_5j.txt",
        "test_instance_medium_3m_10j.txt",
        "test_instance_large_4m_20j.txt",
        "test_instance_xlarge_5m_30j.txt",
        "test_instance_complex_3m_15j_5ops.txt",
        "test_instance_large_parts_2m_8j.txt",
        "test_instance_small_parts_3m_12j.txt",
        "test_instance_multi_ops_4m_25j_6ops.txt"
    };
    
    // 基础路径
    private static final String BASE_PATH = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\";
    
    // 每个算例运行次数
    private static final int RUN_TIMES = 5;

    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("        批量测试所有算例 - 遗传算法性能评估");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        List<TestResult> results = new ArrayList<>();
        
        // 对每个算例进行测试
        for (String fileName : INSTANCE_FILES) {
            System.out.println("\n【正在测试】" + fileName);
            System.out.println("─────────────────────────────────────────────────────────");
            
            TestResult result = runInstance(fileName);
            if (result != null) {
                results.add(result);
                result.printSummary();
            }
            
            System.out.println();
        }
        
        // 输出汇总结果
        printSummaryTable(results);
        
        // 导出结果到CSV
        exportResults(results, BASE_PATH + "algorithm_results.csv");
    }
    
    /**
     * 运行单个算例（多次运行取平均值）
     */
    private static TestResult runInstance(String fileName) {
        String filePath = BASE_PATH + fileName;
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.err.println("✗ 文件不存在：" + filePath);
            return null;
        }
        
        TestResult result = new TestResult(fileName);
        
        try {
            // 读取问题实例
            Input input = new Input(file);
            Problem problem = input.getProblemDesFromFile();
            
            result.machineCount = problem.getMachineCount();
            result.jobCount = problem.getJobCount();
            result.operationCount = problem.getTotalOperationCount();
            
            System.out.println("问题规模：" + result.jobCount + "个工件，" + 
                             result.operationCount + "道工序");
            
            // 多次运行
            double totalTime = 0;
            boolean allSuccess = true;
            
            for (int i = 0; i < RUN_TIMES; i++) {
                System.out.print("  第" + (i + 1) + "次运行...");
                
                try {
                    long startTime = System.currentTimeMillis();
                    GA ga = new GA(problem);
                    ga.solve();
                    long endTime = System.currentTimeMillis();
                    
                    double elapsedTime = (endTime - startTime) / 1000.0;
                    totalTime += elapsedTime;
                    
                    System.out.println(" 完成！用时：" + String.format("%.2f", elapsedTime) + "秒");
                    
                } catch (Exception e) {
                    System.out.println(" 失败！");
                    System.err.println("    错误：" + e.getMessage());
                    allSuccess = false;
                }
            }
            
            if (allSuccess) {
                result.avgTime = totalTime / RUN_TIMES;
                result.success = true;
            }
            
        } catch (Exception e) {
            System.err.println("✗ 解析失败：" + e.getMessage());
            result.success = false;
        }
        
        return result;
    }
    
    /**
     * 打印汇总表格
     */
    private static void printSummaryTable(List<TestResult> results) {
        System.out.println("\n\n═══════════════════════════════════════════════════════════");
        System.out.println("                      测试结果汇总");
        System.out.println("═══════════════════════════════════════════════════════════\n");
        
        System.out.println("┌────────────────────────────────┬──────┬──────┬──────┬────────────┬────────┐");
        System.out.println("│ 算例名称                       │机器数│工件数│工序数│平均时间(秒)│ 状态   │");
        System.out.println("├────────────────────────────────┼──────┼──────┼──────┼────────────┼────────┤");
        
        for (TestResult result : results) {
            String shortName = result.instanceName.replace("test_instance_", "")
                                                  .replace(".txt", "");
            if (shortName.length() > 30) {
                shortName = shortName.substring(0, 27) + "...";
            }
            
            String status = result.success ? "✓ 成功" : "✗ 失败";
            String timeStr = result.success ? String.format("%.2f", result.avgTime) : "N/A";
            
            System.out.printf("│ %-30s │  %2d  │  %2d  │ %4d │  %8s  │ %6s │%n",
                shortName,
                result.machineCount,
                result.jobCount,
                result.operationCount,
                timeStr,
                status);
        }
        
        System.out.println("└────────────────────────────────┴──────┴──────┴──────┴────────────┴────────┘");
        
        // 统计信息
        long successCount = results.stream().filter(r -> r.success).count();
        System.out.println("\n总计：" + results.size() + "个算例");
        System.out.println("成功：" + successCount + "个");
        System.out.println("失败：" + (results.size() - successCount) + "个");
        System.out.println("\n每个算例运行次数：" + RUN_TIMES + "次");
    }
    
    /**
     * 导出结果到CSV文件
     */
    private static void exportResults(List<TestResult> results, String outputPath) {
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath));
            
            // 写入表头
            writer.write("算例名称,机器数,工件数,工序数,平均时间(秒),状态\n");
            
            // 写入数据
            for (TestResult result : results) {
                writer.write(String.format("%s,%d,%d,%d,%.2f,%s\n",
                    result.instanceName,
                    result.machineCount,
                    result.jobCount,
                    result.operationCount,
                    result.avgTime,
                    result.success ? "成功" : "失败"));
            }
            
            writer.close();
            System.out.println("\n✓ 结果已导出到：" + outputPath);
            
        } catch (IOException e) {
            System.err.println("✗ 导出失败：" + e.getMessage());
        }
    }
    
    /**
     * 测试结果数据类
     */
    static class TestResult {
        String instanceName;
        int machineCount;
        int jobCount;
        int operationCount;
        double avgTime;
        boolean success;
        
        TestResult(String instanceName) {
            this.instanceName = instanceName;
            this.success = false;
            this.avgTime = 0;
        }
        
        void printSummary() {
            if (success) {
                System.out.println("✓ 测试成功");
                System.out.println("  平均计算时间：" + String.format("%.2f", avgTime) + " 秒");
            } else {
                System.out.println("✗ 测试失败");
            }
        }
    }
}

