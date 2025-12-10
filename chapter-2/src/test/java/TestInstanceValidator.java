package test;

import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Item;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Machine.BathchMachine;

import java.io.File;

/**
 * 测试算例验证工具
 * 用于检查算例文件的格式正确性和数据合理性
 */
public class TestInstanceValidator {

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

        System.out.println("==========================================");
        System.out.println("测试算例验证工具");
        System.out.println("==========================================\n");

        int passCount = 0;
        int failCount = 0;

        for (String fileName : instanceFiles) {
            System.out.println("正在验证: " + fileName);
            System.out.println("------------------------------------------");
            
            try {
                // 构建文件路径
                String resourcePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\" + fileName;
                File file = new File(resourcePath);
                
                if (!file.exists()) {
                    System.out.println("❌ 文件不存在: " + resourcePath);
                    System.out.println();
                    failCount++;
                    continue;
                }

                // 读取并解析算例
                Input input = new Input(file);
                Problem problem = input.getProblemDesFromFile();

                // 执行验证
                boolean isValid = validateProblem(problem, fileName);

                if (isValid) {
                    System.out.println("✓ 验证通过");
                    passCount++;
                } else {
                    System.out.println("✗ 验证失败");
                    failCount++;
                }

            } catch (Exception e) {
                System.out.println("❌ 解析错误: " + e.getMessage());
                e.printStackTrace();
                failCount++;
            }

            System.out.println();
        }

        // 输出总结
        System.out.println("==========================================");
        System.out.println("验证总结");
        System.out.println("==========================================");
        System.out.println("通过: " + passCount + " 个");
        System.out.println("失败: " + failCount + " 个");
        System.out.println("总计: " + (passCount + failCount) + " 个");
        System.out.println("==========================================");
    }

    /**
     * 验证问题实例的合法性
     */
    private static boolean validateProblem(Problem problem, String fileName) {
        boolean isValid = true;

        // 1. 基本信息验证
        System.out.println("1. 基本信息:");
        System.out.println("   机器总数: " + problem.getMachineCount());
        System.out.println("   工件总数: " + problem.getJobCount());
        System.out.println("   打印机数: " + problem.getPrintMachineCount());
        System.out.println("   批处理机数: " + problem.getBatchMachineCount());
        System.out.println("   总工序数: " + problem.getTotalOperationCount());
        System.out.println("   最大工序数: " + problem.getMaxOperationCount());

        // 验证机器数量一致性
        int expectedMachineCount = problem.getPrintMachineCount() + problem.getBatchMachineCount();
        if (problem.getMachineCount() != problem.getMachines().length) {
            System.out.println("   ⚠ 警告: 机器数量与实际机器数组长度不一致");
            //isValid = false;
        }

        // 2. 打印机信息验证
        System.out.println("\n2. 打印机信息:");
        Machine[] machines = problem.getMachines();
        for (int i = 0; i < problem.getPrintMachineCount(); i++) {
            if (machines[i] instanceof PrintMachine) {
                PrintMachine pm = (PrintMachine) machines[i];
                System.out.println("   打印机" + (i + 1) + ": " + 
                    pm.L + "x" + pm.W + "x" + pm.H + "mm, " +
                    "层高:" + pm.printH + "mm, " +
                    "准备:" + pm.prepareTime + "min, " +
                    "换层:" + pm.reCoatingTime + "min/层");
                
                // 验证打印机参数合理性
                if (pm.L <= 0 || pm.W <= 0 || pm.H <= 0) {
                    System.out.println("   ⚠ 错误: 打印机尺寸必须为正数");
                    isValid = false;
                }
                if (pm.printH <= 0 || pm.printH > 1) {
                    System.out.println("   ⚠ 警告: 层高异常 (通常为0.1-0.5mm)");
                }
            } else {
                System.out.println("   ⚠ 错误: 机器类型不匹配");
                isValid = false;
            }
        }

        // 3. 批处理机信息验证
        System.out.println("\n3. 批处理机信息:");
        for (int i = problem.getPrintMachineCount(); 
             i < problem.getPrintMachineCount() + problem.getBatchMachineCount(); 
             i++) {
            if (machines[i] instanceof BathchMachine) {
                BathchMachine bm = (BathchMachine) machines[i];
                System.out.println("   批处理机" + (i - problem.getPrintMachineCount() + 1) + 
                    ": 处理时间 " + bm.processingTime + "min");
                
                if (bm.processingTime <= 0) {
                    System.out.println("   ⚠ 错误: 处理时间必须为正数");
                    isValid = false;
                }
            } else {
                System.out.println("   ⚠ 错误: 机器类型不匹配");
                isValid = false;
            }
        }

        // 4. 零件信息验证
        System.out.println("\n4. 零件信息:");
        Item[] items = problem.getItems();
        double minL = Double.MAX_VALUE, maxL = 0;
        double minW = Double.MAX_VALUE, maxW = 0;
        double minH = Double.MAX_VALUE, maxH = 0;
        
        for (int i = 0; i < items.length; i++) {
            Item item = items[i];
            minL = Math.min(minL, item.l);
            maxL = Math.max(maxL, item.l);
            minW = Math.min(minW, item.w);
            maxW = Math.max(maxW, item.w);
            minH = Math.min(minH, item.h);
            maxH = Math.max(maxH, item.h);

            // 验证零件尺寸不超过所有打印机
            boolean fitsAnyMachine = false;
            for (int j = 0; j < problem.getPrintMachineCount(); j++) {
                PrintMachine pm = (PrintMachine) machines[j];
                if ((item.l <= pm.L && item.w <= pm.W || item.w <= pm.L && item.l <= pm.W) 
                    && item.h <= pm.H) {
                    fitsAnyMachine = true;
                    break;
                }
            }

            if (!fitsAnyMachine) {
                System.out.println("   ⚠ 错误: 零件" + i + " (" + item.l + "x" + item.w + "x" + item.h + 
                    ") 无法在任何打印机上打印");
                isValid = false;
            }
        }

        System.out.println("   零件数量: " + items.length);
        System.out.println("   长度范围: " + String.format("%.1f - %.1f mm", minL, maxL));
        System.out.println("   宽度范围: " + String.format("%.1f - %.1f mm", minW, maxW));
        System.out.println("   高度范围: " + String.format("%.1f - %.1f mm", minH, maxH));

        // 5. 工序信息验证
        System.out.println("\n5. 工序信息:");
        int[] operationCountArr = problem.getOperationCountArr();
        int totalOps = 0;
        int minOps = Integer.MAX_VALUE, maxOps = 0;
        
        for (int opCount : operationCountArr) {
            totalOps += opCount;
            minOps = Math.min(minOps, opCount);
            maxOps = Math.max(maxOps, opCount);
        }

        System.out.println("   工件工序数范围: " + minOps + " - " + maxOps);
        System.out.println("   总工序数: " + totalOps);
        
        if (totalOps != problem.getTotalOperationCount()) {
            System.out.println("   ⚠ 警告: 总工序数不匹配");
        }

        // 6. 加工时间矩阵验证
        System.out.println("\n6. 加工时间矩阵:");
        double[][] proDesMatrix = problem.getProDesMatrix();
        System.out.println("   矩阵行数: " + proDesMatrix.length);
        System.out.println("   矩阵列数: " + (proDesMatrix.length > 0 ? proDesMatrix[0].length : 0));
        
        if (proDesMatrix.length != totalOps) {
            System.out.println("   ⚠ 警告: 矩阵行数与总工序数不匹配");
        }

        // 统计加工时间
        double minTime = Double.MAX_VALUE, maxTime = 0;
        int zeroCount = 0;
        for (double[] row : proDesMatrix) {
            for (double time : row) {
                if (time > 0) {
                    minTime = Math.min(minTime, time);
                    maxTime = Math.max(maxTime, time);
                } else if (time == 0) {
                    zeroCount++;
                }
            }
        }

        System.out.println("   加工时间范围: " + String.format("%.1f - %.1f min", minTime, maxTime));
        System.out.println("   零值数量: " + zeroCount + " (打印工序时间为0是正常的)");

        return isValid;
    }
}

