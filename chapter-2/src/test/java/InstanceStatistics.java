package test;

import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Item;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Machine.BathchMachine;

import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 算例统计分析工具
 * 生成所有测试算例的统计信息
 */
public class InstanceStatistics {

    public static void main(String[] args) {
        // 测试所有算例文件
        String[] instanceFiles = {
            "test_instance_small_2m_5j.txt",
            "test_instance_medium_3m_10j.txt",
            "test_instance_large_4m_20j.txt",
            "test_instance_xlarge_5m_30j.txt",
            "test_instance_complex_3m_15j_5ops.txt",
            "test_instance_large_parts_2m_8j.txt",
            "test_instance_small_parts_3m_12j.txt",
            "test_instance_multi_ops_4m_25j_6ops.txt"
        };

        List<InstanceInfo> instanceInfos = new ArrayList<>();

        System.out.println("==========================================");
        System.out.println("测试算例统计分析");
        System.out.println("==========================================\n");

        for (String fileName : instanceFiles) {
            try {
                String resourcePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\" + fileName;
                File file = new File(resourcePath);
                
                if (!file.exists()) {
                    continue;
                }

                Input input = new Input(file);
                Problem problem = input.getProblemDesFromFile();
                
                InstanceInfo info = analyzeInstance(problem, fileName);
                instanceInfos.add(info);
                
                printInstanceInfo(info);

            } catch (Exception e) {
                System.out.println("分析失败: " + fileName);
                e.printStackTrace();
            }
        }

        // 生成汇总表格
        System.out.println("\n==========================================");
        System.out.println("算例汇总表");
        System.out.println("==========================================\n");
        generateSummaryTable(instanceInfos);

        // 生成CSV文件
        try {
            exportToCSV(instanceInfos, "src/main/resources/instance_statistics.csv");
            System.out.println("\n✓ 统计数据已导出到: instance_statistics.csv");
        } catch (IOException e) {
            System.out.println("\n✗ CSV导出失败: " + e.getMessage());
        }
    }

    /**
     * 分析单个算例实例
     */
    private static InstanceInfo analyzeInstance(Problem problem, String fileName) {
        InstanceInfo info = new InstanceInfo();
        info.fileName = fileName;
        info.machineCount = problem.getMachineCount();
        info.jobCount = problem.getJobCount();
        info.printMachineCount = problem.getPrintMachineCount();
        info.batchMachineCount = problem.getBatchMachineCount();
        info.totalOperationCount = problem.getTotalOperationCount();
        info.maxOperationCount = problem.getMaxOperationCount();

        // 零件尺寸统计
        Item[] items = problem.getItems();
        double totalVolume = 0;
        double minSize = Double.MAX_VALUE;
        double maxSize = 0;
        
        for (Item item : items) {
            double volume = item.l * item.w * item.h;
            totalVolume += volume;
            double size = Math.max(item.l, Math.max(item.w, item.h));
            minSize = Math.min(minSize, size);
            maxSize = Math.max(maxSize, size);
        }
        
        info.avgItemVolume = totalVolume / items.length / 1000; // 转换为cm³
        info.minItemSize = minSize;
        info.maxItemSize = maxSize;

        // 打印机容量统计
        double totalPrintCapacity = 0;
        for (int i = 0; i < problem.getPrintMachineCount(); i++) {
            PrintMachine pm = (PrintMachine) problem.getMachines()[i];
            totalPrintCapacity += pm.L * pm.W;
        }
        info.avgPrintCapacity = totalPrintCapacity / problem.getPrintMachineCount() / 100; // cm²

        // 批处理时间统计
        double totalBatchTime = 0;
        for (int i = problem.getPrintMachineCount(); 
             i < problem.getPrintMachineCount() + problem.getBatchMachineCount(); 
             i++) {
            BathchMachine bm = (BathchMachine) problem.getMachines()[i];
            totalBatchTime += bm.processingTime;
        }
        info.avgBatchTime = totalBatchTime / problem.getBatchMachineCount();

        // 加工时间统计
        double[][] proDesMatrix = problem.getProDesMatrix();
        double totalProcessTime = 0;
        int processCount = 0;
        
        for (double[] row : proDesMatrix) {
            for (double time : row) {
                if (time > 0) {
                    totalProcessTime += time;
                    processCount++;
                }
            }
        }
        info.avgProcessTime = processCount > 0 ? totalProcessTime / processCount : 0;

        return info;
    }

    /**
     * 打印单个算例信息
     */
    private static void printInstanceInfo(InstanceInfo info) {
        System.out.println("算例: " + info.fileName);
        System.out.println("  机器配置: " + info.printMachineCount + "台打印机 + " + 
                          info.batchMachineCount + "台批处理机");
        System.out.println("  工件数量: " + info.jobCount);
        System.out.println("  总工序数: " + info.totalOperationCount);
        System.out.println("  最大工序数/工件: " + info.maxOperationCount);
        System.out.println("  零件尺寸范围: " + String.format("%.1f - %.1f mm", 
                          info.minItemSize, info.maxItemSize));
        System.out.println("  平均零件体积: " + String.format("%.2f cm³", info.avgItemVolume));
        System.out.println("  平均打印平台: " + String.format("%.0f cm²", info.avgPrintCapacity));
        System.out.println("  平均批处理时间: " + String.format("%.1f min", info.avgBatchTime));
        System.out.println("  平均加工时间: " + String.format("%.1f min", info.avgProcessTime));
        System.out.println();
    }

    /**
     * 生成汇总表格
     */
    private static void generateSummaryTable(List<InstanceInfo> infos) {
        System.out.println("┌─────────────────────────────┬──────┬──────┬──────┬──────┬──────┬──────┐");
        System.out.println("│ 算例名称                    │打印机│批处理│工件数│总工序│平均尺│难度值│");
        System.out.println("├─────────────────────────────┼──────┼──────┼──────┼──────┼──────┼──────┤");
        
        for (InstanceInfo info : infos) {
            String shortName = info.fileName.replace("test_instance_", "")
                                           .replace(".txt", "");
            if (shortName.length() > 28) {
                shortName = shortName.substring(0, 25) + "...";
            }
            
            // 计算难度值 (综合考虑规模和复杂度)
            int difficulty = calculateDifficulty(info);
            String difficultyStr = getDifficultyStars(difficulty);
            
            System.out.printf("│ %-27s │  %2d  │  %2d  │  %2d  │ %4d │ %4.0f │ %4s │%n",
                shortName, 
                info.printMachineCount,
                info.batchMachineCount,
                info.jobCount,
                info.totalOperationCount,
                (info.minItemSize + info.maxItemSize) / 2,
                difficultyStr);
        }
        
        System.out.println("└─────────────────────────────┴──────┴──────┴──────┴──────┴──────┴──────┘");
    }

    /**
     * 计算难度值
     */
    private static int calculateDifficulty(InstanceInfo info) {
        // 综合考虑多个因素
        double score = 0;
        
        // 工件数量影响 (0-30分)
        score += Math.min(info.jobCount, 30);
        
        // 机器数量影响 (0-20分)
        score += (info.printMachineCount + info.batchMachineCount) * 2;
        
        // 工序复杂度影响 (0-30分)
        score += info.maxOperationCount * 5;
        
        // 总工序数影响 (0-20分)
        score += Math.min(info.totalOperationCount / 5, 20);
        
        return (int) score;
    }

    /**
     * 获取难度星级
     */
    private static String getDifficultyStars(int difficulty) {
        if (difficulty < 40) return "★☆☆☆";
        if (difficulty < 60) return "★★☆☆";
        if (difficulty < 80) return "★★★☆";
        if (difficulty < 100) return "★★★★";
        return "★★★★";
    }

    /**
     * 导出为CSV文件
     */
    private static void exportToCSV(List<InstanceInfo> infos, String filePath) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        
        // 写入表头
        writer.write("算例名称,打印机数,批处理机数,工件数,总工序数,最大工序数," +
                    "最小零件尺寸,最大零件尺寸,平均零件体积,平均打印平台,平均批处理时间,平均加工时间\n");
        
        // 写入数据
        for (InstanceInfo info : infos) {
            writer.write(String.format("%s,%d,%d,%d,%d,%d,%.1f,%.1f,%.2f,%.0f,%.1f,%.1f\n",
                info.fileName,
                info.printMachineCount,
                info.batchMachineCount,
                info.jobCount,
                info.totalOperationCount,
                info.maxOperationCount,
                info.minItemSize,
                info.maxItemSize,
                info.avgItemVolume,
                info.avgPrintCapacity,
                info.avgBatchTime,
                info.avgProcessTime));
        }
        
        writer.close();
    }

    /**
     * 算例信息数据类
     */
    static class InstanceInfo {
        String fileName;
        int machineCount;
        int jobCount;
        int printMachineCount;
        int batchMachineCount;
        int totalOperationCount;
        int maxOperationCount;
        double minItemSize;
        double maxItemSize;
        double avgItemVolume;
        double avgPrintCapacity;
        double avgBatchTime;
        double avgProcessTime;
    }
}

