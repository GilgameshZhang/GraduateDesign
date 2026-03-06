package util.java;

import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.DiscreteProcessingMachine;

import java.io.File;

/**
 * 测试Input类读取功能
 * 
 * 验证:
 * 1. 是否正确跳过能耗参数行
 * 2. 是否正确创建离散加工机对象
 * 3. 机器数组大小是否正确
 */
public class TestInputReading {
    
    public static void main(String[] args) {
        System.out.println("========== 测试Input类读取功能 ==========\n");
        
        // 测试文件
        String testFile = "chapter-3/src/main/resources/instance/test_J3P2B1D2_01.txt";
        File file = new File(testFile);
        
        if (!file.exists()) {
            System.err.println("错误: 测试文件不存在 - " + testFile);
            return;
        }
        
        System.out.println("读取文件: " + testFile);
        System.out.println("预期配置: 5台机器(2打印+1批处理+2离散), 3个工件\n");
        
        try {
            // 使用Input类读取
            Input input = new Input(file);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("\n========== 读取结果 ==========");
            
            // 1. 验证基本信息
            System.out.println("【基本信息】");
            System.out.println("  总机器数: " + problem.getMachineCount());
            System.out.println("  工件数: " + problem.getJobCount());
            System.out.println("  打印机数: " + problem.getPrintMachineCount());
            System.out.println("  批处理机数: " + problem.getBatchMachineCount());
            System.out.println("  离散机数: " + (problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()));
            
            // 2. 验证机器数组
            Machine[] machines = problem.getMachines();
            System.out.println("\n【机器数组验证】");
            System.out.println("  机器数组长度: " + machines.length);
            
            int printCount = 0, batchCount = 0, discreteCount = 0;
            for (int i = 0; i < machines.length; i++) {
                if (machines[i] instanceof PrintMachine) {
                    printCount++;
                    System.out.println("  机器" + (i+1) + ": 打印机 (name=" + machines[i].name + ")");
                } else if (machines[i] instanceof BathchMachine) {
                    batchCount++;
                    System.out.println("  机器" + (i+1) + ": 批处理机 (name=" + machines[i].name + ")");
                } else if (machines[i] instanceof DiscreteProcessingMachine) {
                    discreteCount++;
                    System.out.println("  机器" + (i+1) + ": 离散加工机 (name=" + machines[i].name + ")");
                } else {
                    System.out.println("  机器" + (i+1) + ": 未知类型");
                }
            }
            
            System.out.println("\n  统计: 打印机" + printCount + "台, 批处理机" + batchCount + "台, 离散机" + discreteCount + "台");
            
            // 3. 验证工件信息
            System.out.println("\n【工件信息】");
            System.out.println("  总工序数: " + problem.getTotalOperationCount());
            System.out.println("  最大工序数: " + problem.getMaxOperationCount());
            
            // 4. 验证proDesMatrix
            double[][] proDesMatrix = problem.getProDesMatrix();
            System.out.println("\n【加工时间矩阵】");
            System.out.println("  矩阵维度: " + proDesMatrix.length + " × " + proDesMatrix[0].length);
            System.out.println("  矩阵列数(机器数): " + proDesMatrix[0].length);
            
            // 显示第一道工序的加工时间
            System.out.println("\n  第1道工序的加工时间:");
            for (int m = 0; m < proDesMatrix[0].length; m++) {
                if (proDesMatrix[0][m] > 0) {
                    System.out.println("    机器" + (m+1) + ": " + proDesMatrix[0][m] + "秒");
                }
            }
            
            // 5. 验证结果
            System.out.println("\n========== 验证结果 ==========");
            boolean success = true;
            
            if (problem.getMachineCount() != 5) {
                System.out.println("❌ 总机器数错误: 期望5, 实际" + problem.getMachineCount());
                success = false;
            }
            
            if (printCount != 2) {
                System.out.println("❌ 打印机数错误: 期望2, 实际" + printCount);
                success = false;
            }
            
            if (batchCount != 1) {
                System.out.println("❌ 批处理机数错误: 期望1, 实际" + batchCount);
                success = false;
            }
            
            if (discreteCount != 2) {
                System.out.println("❌ 离散机数错误: 期望2, 实际" + discreteCount);
                success = false;
            }
            
            if (problem.getJobCount() != 3) {
                System.out.println("❌ 工件数错误: 期望3, 实际" + problem.getJobCount());
                success = false;
            }
            int machineNum = problem.getMachineCount();
            if (machines.length != machineNum) {
                System.out.println("❌ 机器数组长度错误: 期望" + machineNum + ", 实际" + machines.length);
                success = false;
            }
            
            if (proDesMatrix[0].length != machineNum) {
                System.out.println("❌ 加工时间矩阵列数错误: 期望" + machineNum + ", 实际" + proDesMatrix[0].length);
                success = false;
            }
            
            if (success) {
                System.out.println("✅ 所有验证通过！Input类读取功能正常！");
            } else {
                System.out.println("❌ 验证失败，请检查Input类实现");
            }
            
            System.out.println("==============================\n");
            
        } catch (Exception e) {
            System.err.println("\n❌ 读取失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
