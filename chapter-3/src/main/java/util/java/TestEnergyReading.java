package util.java;

import ProgramEntity.EnergyAwareInput;
import ProgramEntity.Problem;
import ProgramEntity.Machine.Machine;
import ProblemFrame.PowerParameters;

import java.io.File;

/**
 * 测试能耗参数读取功能
 * 
 * 验证：
 * 1. Input类是否正确读取并存储能耗参数字符串
 * 2. EnergyAwareInput是否正确解析能耗参数
 * 3. Machine对象是否正确获得PowerParameters
 */
public class TestEnergyReading {
    
    public static void main(String[] args) {
        System.out.println("========== 测试能耗参数读取 ==========\n");
        
        // 测试算例路径
        String testFile = "chapter-3/src/main/resources/instance/test_J3P2B1D2_01.txt";
        File file = new File(testFile);
        
        if (!file.exists()) {
            System.err.println("❌ 测试文件不存在: " + testFile);
            return;
        }
        
        try {
            System.out.println("【步骤1】使用EnergyAwareInput读取算例");
            EnergyAwareInput input = new EnergyAwareInput(file);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("\n【步骤2】验证基本信息");
            System.out.println("  ✓ 工件数: " + problem.getJobCount());
            System.out.println("  ✓ 机器总数: " + problem.getMachineCount());
            System.out.println("  ✓ 打印机数: " + problem.getPrintMachineCount());
            System.out.println("  ✓ 批处理机数: " + problem.getBatchMachineCount());
            System.out.println("  ✓ 离散机数: " + 
                (problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()));
            
            System.out.println("\n【步骤3】检查能耗参数字符串是否被读取");
            System.out.println("  打印机能耗参数: " + 
                (problem.getPrinterEnergyParams() != null ? "已读取" : "未读取"));
            System.out.println("  批处理机能耗参数: " + 
                (problem.getBatchEnergyParams() != null ? "已读取" : "未读取"));
            System.out.println("  离散机能耗参数: " + 
                (problem.getDiscreteEnergyParams() != null ? "已读取" : "未读取"));
            System.out.println("  问题包含能耗参数: " + problem.hasEnergyParams());
            
            if (problem.hasEnergyParams()) {
                System.out.println("\n  原始能耗参数内容:");
                System.out.println("  - 打印机: " + problem.getPrinterEnergyParams());
                System.out.println("  - 批处理机: " + problem.getBatchEnergyParams());
                System.out.println("  - 离散机: " + problem.getDiscreteEnergyParams());
            }
            
            System.out.println("\n【步骤4】检查EnergyAwareInput是否成功解析");
            System.out.println("  EnergyAwareInput包含能耗参数: " + input.hasEnergyParams());
            
            System.out.println("\n【步骤5】检查Machine对象的PowerParameters");
            Machine[] machines = problem.getMachines();
            int machinesWithPower = 0;
            
            for (int i = 0; i < machines.length; i++) {
                if (machines[i].hasPowerParameters()) {
                    machinesWithPower++;
                    PowerParameters params = machines[i].getPowerParameters();
                    
                    String machineType = "未知";
                    if (machines[i] instanceof ProgramEntity.Machine.PrintMachine) {
                        machineType = "打印机";
                    } else if (machines[i] instanceof ProgramEntity.Machine.BathchMachine) {
                        machineType = "批处理机";
                    } else if (machines[i] instanceof ProgramEntity.Machine.DiscreteProcessingMachine) {
                        machineType = "离散机";
                    }
                    
                    System.out.println(String.format("  ✓ 机器%d (%s): P_run=%.2fkW, P_idle=%.2fkW, E_switch=%.2fkWh, T_warmup=%.0fs, 允许关机=%s",
                        i + 1,
                        machineType,
                        params.P_run,
                        params.P_idle,
                        params.E_switch,
                        params.T_warmup
                    ));
                }
            }
            
            System.out.println("\n【步骤6】统计结果");
            System.out.println("  总机器数: " + machines.length);
            System.out.println("  配置了能耗参数的机器数: " + machinesWithPower);
            
            if (machinesWithPower == machines.length) {
                System.out.println("\n✅ 测试通过！所有机器都已正确配置能耗参数");
            } else {
                System.out.println("\n⚠️ 警告：有 " + (machines.length - machinesWithPower) + 
                    " 台机器未配置能耗参数");
            }
            
            // 额外验证：计算盈亏平衡时间
            System.out.println("\n【步骤7】计算盈亏平衡时间");
            for (int i = 0; i < Math.min(3, machines.length); i++) {
                PowerParameters params = machines[i].getPowerParameters();
                if (params != null) {
                    double breakEvenTime = params.getBreakEvenTime();
                    System.out.println(String.format("  机器%d盈亏平衡时间: %.2f秒 (%.2f分钟)",
                        i + 1, breakEvenTime, breakEvenTime / 60.0));
                }
            }
            
        } catch (Exception e) {
            System.err.println("\n❌ 测试失败: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println("\n========== 测试完成 ==========");
    }
}
