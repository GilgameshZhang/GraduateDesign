package util.java;

import java.io.*;

/**
 * 验证工序机器分配逻辑
 * 
 * 检查生成的算例中：
 * - 第1道工序是否只包含打印机
 * - 第2道工序是否只包含批处理机
 * - 第3道及以后工序是否只包含离散加工机
 */
public class VerifyOperationMachineAssignment {
    
    public static void main(String[] args) {
        System.out.println("========== 验证工序机器分配 ==========\n");
        
        // 先生成一个测试算例
        System.out.println("【步骤1】生成测试算例");
        String testFile = "chapter-3/src/main/resources/instance/verify_test.txt";
        
        EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(12345);
        gen.generateInstance(5, 2, 1, 2, testFile, 
            EnergyAwareInstanceGenerator.EnergyProfile.UNIFIED,
            EnergyAwareInstanceGenerator.EnergyProfile.UNIFIED);
        
        System.out.println("✅ 测试算例已生成: " + testFile);
        
        // 验证算例
        System.out.println("\n【步骤2】验证工序机器分配");
        try {
            verifyInstance(testFile, 2, 1, 2);
        } catch (Exception e) {
            System.err.println("❌ 验证失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========== 验证完成 ==========");
    }
    
    private static void verifyInstance(String filepath, int printCount, 
                                       int batchCount, int discreteCount) throws IOException {
        
        BufferedReader reader = new BufferedReader(new FileReader(filepath));
        
        // 跳过前6行（机器配置和能耗参数）
        for (int i = 0; i < 6; i++) {
            reader.readLine();
        }
        
        // 定义机器范围
        int printStart = 1, printEnd = printCount;
        int batchStart = printCount + 1, batchEnd = printCount + batchCount;
        int discreteStart = printCount + batchCount + 1;
        int discreteEnd = printCount + batchCount + discreteCount;
        
        System.out.println("机器范围定义:");
        System.out.println("  打印机: " + printStart + "-" + printEnd);
        System.out.println("  批处理机: " + batchStart + "-" + batchEnd);
        System.out.println("  离散加工机: " + discreteStart + "-" + discreteEnd);
        System.out.println();
        
        boolean allCorrect = true;
        int jobId = 0;
        String line;
        
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            jobId++;
            System.out.println("--- 工件 " + jobId + " ---");
            
            String[] tokens = line.split("\\s+");
            int opsCount = Integer.parseInt(tokens[0]);
            
            // 跳过工件尺寸 (L, W, H) 和重复的工序数
            int index = 5;
            
            for (int opId = 0; opId < opsCount; opId++) {
                int candidateCount = Integer.parseInt(tokens[index++]);
                
                System.out.print("  工序 " + (opId + 1) + " (");
                if (opId == 0) {
                    System.out.print("打印");
                } else if (opId == 1) {
                    System.out.print("批处理");
                } else {
                    System.out.print("离散加工");
                }
                System.out.print("): 候选机器 [");
                
                boolean opCorrect = true;
                
                for (int c = 0; c < candidateCount; c++) {
                    int machineId = Integer.parseInt(tokens[index++]);
                    int processingTime = Integer.parseInt(tokens[index++]);
                    
                    if (c > 0) System.out.print(", ");
                    System.out.print("M" + machineId);
                    
                    // 检查机器类型是否正确
                    boolean isCorrect = false;
                    String expectedType = "";
                    
                    if (opId == 0) {
                        // 第1道工序应该是打印机
                        isCorrect = (machineId >= printStart && machineId <= printEnd);
                        expectedType = "打印机";
                    } else if (opId == 1) {
                        // 第2道工序应该是批处理机
                        isCorrect = (machineId >= batchStart && machineId <= batchEnd);
                        expectedType = "批处理机";
                    } else {
                        // 第3道及以后应该是离散加工机
                        isCorrect = (machineId >= discreteStart && machineId <= discreteEnd);
                        expectedType = "离散加工机";
                    }
                    
                    if (!isCorrect) {
                        System.out.print("❌");
                        opCorrect = false;
                        allCorrect = false;
                    }
                }
                
                System.out.print("]");
                if (opCorrect) {
                    System.out.println(" ✅");
                } else {
                    System.out.println(" ❌ 错误!");
                }
            }
        }
        
        reader.close();
        
        System.out.println("\n【验证结果】");
        if (allCorrect) {
            System.out.println("✅ 所有工序的机器分配都正确!");
        } else {
            System.out.println("❌ 发现机器分配错误，请检查!");
        }
    }
}
