package test;

import ProgramEntity.Input;
import ProgramEntity.Problem;
import ProgramEntity.Item;
import ProgramEntity.Machine.PrintMachine;

import java.io.File;

/**
 * 简单的算例读取和显示示例
 * 演示如何使用生成的测试算例
 */
public class SimpleInstanceReader {

    public static void main(String[] args) {
        // 选择一个测试算例
        String instanceFile = "test_instance_small_2m_5j.txt";
        
        System.out.println("==========================================");
        System.out.println("算例读取示例");
        System.out.println("==========================================\n");
        
        try {
            // 读取算例文件
            String resourcePath = "chapter-2/src/main/resources/" + instanceFile;
            File file = new File(resourcePath);
            
            if (!file.exists()) {
                System.out.println("错误: 文件不存在 - " + resourcePath);
                return;
            }
            
            System.out.println("正在读取: " + instanceFile);
            System.out.println("------------------------------------------\n");
            
            // 解析算例
            Input input = new Input(file);
            Problem problem = input.getProblemDesFromFile();
            
            // 显示问题信息
            displayProblemInfo(problem);
            
            // 显示如何访问数据
            System.out.println("\n==========================================");
            System.out.println("数据访问示例");
            System.out.println("==========================================\n");
            demonstrateDataAccess(problem);
            
        } catch (Exception e) {
            System.out.println("错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 显示问题基本信息
     */
    private static void displayProblemInfo(Problem problem) {
        System.out.println("【问题规模】");
        System.out.println("  总机器数: " + problem.getMachineCount());
        System.out.println("  工件数: " + problem.getJobCount());
        System.out.println("  打印机数: " + problem.getPrintMachineCount());
        System.out.println("  批处理机数: " + problem.getBatchMachineCount());
        System.out.println("  总工序数: " + problem.getTotalOperationCount());
        
        System.out.println("\n【打印机配置】");
        for (int i = 0; i < problem.getPrintMachineCount(); i++) {
            PrintMachine pm = (PrintMachine) problem.getMachines()[i];
            System.out.printf("  打印机%d: %dx%dx%d mm, 层高%.2fmm, 准备%.0fmin, 换层%.1fmin/层\n",
                i + 1, (int)pm.L, (int)pm.W, (int)pm.H, 
                pm.printH, pm.prepareTime, pm.reCoatingTime);
        }
        
        System.out.println("\n【零件信息】");
        Item[] items = problem.getItems();
        for (int i = 0; i < Math.min(items.length, 5); i++) {
            Item item = items[i];
            System.out.printf("  零件%d: %.0fx%.0fx%.0f mm\n",
                i, item.l, item.w, item.h);
        }
        if (items.length > 5) {
            System.out.println("  ... (共" + items.length + "个零件)");
        }
        
        System.out.println("\n【工序信息】");
        int[] operationCountArr = problem.getOperationCountArr();
        System.out.print("  各工件工序数: ");
        for (int i = 0; i < Math.min(operationCountArr.length, 10); i++) {
            System.out.print(operationCountArr[i] + " ");
        }
        if (operationCountArr.length > 10) {
            System.out.print("...");
        }
        System.out.println();
    }
    
    /**
     * 演示如何访问各种数据
     */
    private static void demonstrateDataAccess(Problem problem) {
        System.out.println("// 获取工件数");
        System.out.println("int jobCount = problem.getJobCount();");
        System.out.println(">>> " + problem.getJobCount());
        
        System.out.println("\n// 获取某个工件的尺寸");
        System.out.println("Item item = problem.getItems()[0];");
        System.out.println("double length = item.l, width = item.w, height = item.h;");
        Item item0 = problem.getItems()[0];
        System.out.printf(">>> L=%.1f, W=%.1f, H=%.1f\n", item0.l, item0.w, item0.h);
        
        System.out.println("\n// 获取某个工件的工序数");
        System.out.println("int opsCount = problem.getOperationCountArr()[0];");
        System.out.println(">>> " + problem.getOperationCountArr()[0]);
        
        System.out.println("\n// 获取某个打印机的参数");
        System.out.println("PrintMachine pm = (PrintMachine)problem.getMachines()[0];");
        System.out.println("double platformL = pm.L, platformW = pm.W;");
        PrintMachine pm = (PrintMachine)problem.getMachines()[0];
        System.out.printf(">>> 平台尺寸: %.0fx%.0f mm\n", pm.L, pm.W);
        
        System.out.println("\n// 获取工序在机器上的加工时间");
        System.out.println("double[][] timeMatrix = problem.getProDesMatrix();");
        System.out.println("double time = timeMatrix[operationIndex][machineIndex];");
        double[][] matrix = problem.getProDesMatrix();
        if (matrix.length > 0 && matrix[0].length > 0) {
            System.out.printf(">>> 工序0在机器0的时间: %.1f min\n", matrix[0][0]);
        }
        
        System.out.println("\n// 获取工件i的第j道工序对应的全局工序索引");
        System.out.println("int globalOpIndex = problem.getOperationToIndex()[i][j];");
        int[][] opToIndex = problem.getOperationToIndex();
        if (opToIndex.length > 0 && opToIndex[0].length > 0) {
            System.out.printf(">>> 工件0的工序0对应全局索引: %d\n", opToIndex[0][0]);
        }
    }
}

