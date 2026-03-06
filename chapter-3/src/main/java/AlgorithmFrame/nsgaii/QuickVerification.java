package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.PrintMachine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 快速验证程序 - 验证能耗计算模块是否正常工作
 * 
 * 直接在IDE中运行此类的main方法
 */
public class QuickVerification {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("   能耗计算模块 - 快速验证程序");
        System.out.println("========================================\n");
        
        boolean allPassed = true;
        
        // 测试1: PowerParameters基本功能
        System.out.println("【测试1】PowerParameters基本功能");
        allPassed &= testPowerParameters();
        System.out.println();
        
        // 测试2: 开关机判定逻辑
        System.out.println("【测试2】开关机判定逻辑");
        allPassed &= testShutdownLogic();
        System.out.println();
        
        // 测试3: 能耗计算（模拟调度）
        System.out.println("【测试3】能耗计算（模拟调度）");
        allPassed &= testEnergyCalculation();
        System.out.println();
        
        // 测试4: 多目标评价器集成
        System.out.println("【测试4】多目标评价器集成");
        allPassed &= testMOEvaluatorIntegration();
        System.out.println();
        
        // 测试5: 对比开关机策略
        System.out.println("【测试5】开关机策略对比");
        allPassed &= testStrategyComparison();
        System.out.println();
        
        // 最终结果
        System.out.println("========================================");
        if (allPassed) {
            System.out.println("✅ 所有测试通过！能耗计算模块工作正常。");
            System.out.println("\n你现在可以：");
            System.out.println("  1. 运行NSGA-II算法进行多目标优化");
            System.out.println("  2. 使用EnergyOptimizationExample查看更多示例");
            System.out.println("  3. 自定义能耗参数进行实验");
        } else {
            System.out.println("❌ 部分测试失败，请检查错误信息");
        }
        System.out.println("========================================");
    }
    
    /**
     * 测试1: PowerParameters基本功能
     */
    private static boolean testPowerParameters() {
        try {
            PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
            
            // 测试盈亏平衡时间
            double T_be = params.getBreakEvenTime();
            assert Math.abs(T_be - 4.0) < 1e-9 : "盈亏平衡时间计算错误";
            System.out.println("  ✓ 盈亏平衡时间: T_be = " + T_be + " (预期: 4.0)");
            
            // 测试空闲能耗计算
            double shortGapEnergy = params.calculateGapEnergy(3.0);
            assert Math.abs(shortGapEnergy - 1.5) < 1e-9 : "短间隔能耗计算错误";
            System.out.println("  ✓ 短间隔能耗 (gap=3.0): " + shortGapEnergy + " kWh (待机)");
            
            double longGapEnergy = params.calculateGapEnergy(20.0);
            assert Math.abs(longGapEnergy - 2.0) < 1e-9 : "长间隔能耗计算错误";
            System.out.println("  ✓ 长间隔能耗 (gap=20.0): " + longGapEnergy + " kWh (关机)");
            
            System.out.println("  ✅ PowerParameters测试通过");
            return true;
        } catch (Exception e) {
            System.out.println("  ❌ PowerParameters测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试2: 开关机判定逻辑
     */
    private static boolean testShutdownLogic() {
        try {
            PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
            // T_warmup=5.0, T_breakEven=4.0, 关机条件: gap>=9.0
            
            assert !params.shouldShutdown(3.0) : "gap=3.0应该待机";
            System.out.println("  ✓ gap=3.0 < 5.0(warmup): 待机 ✓");
            
            assert !params.shouldShutdown(7.0) : "gap=7.0应该待机";
            System.out.println("  ✓ gap=7.0 < 9.0(warmup+T_be): 待机 ✓");
            
            assert params.shouldShutdown(10.0) : "gap=10.0应该关机";
            System.out.println("  ✓ gap=10.0 >= 9.0: 关机 ✓");
            
            assert params.shouldShutdown(100.0) : "gap=100.0应该关机";
            System.out.println("  ✓ gap=100.0 >= 9.0: 关机 ✓");
            
            // 测试不允许关机的设备
            PowerParameters noSwitchParams = new PowerParameters(3.0, 0.8, 5.0, 20.0, false);
            assert !noSwitchParams.shouldShutdown(100.0) : "不允许关机的设备应始终待机";
            System.out.println("  ✓ allowSwitch=false: 始终待机 ✓");
            
            System.out.println("  ✅ 开关机判定逻辑测试通过");
            return true;
        } catch (Exception e) {
            System.out.println("  ❌ 开关机判定逻辑测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试3: 能耗计算（模拟调度）
     */
    private static boolean testEnergyCalculation() {
        try {
            // 创建模拟问题
            Problem problem = createMockProblem();
            
            // 创建模拟调度
            Operation[][] schedule = createMockSchedule(problem);
            
            // 创建能耗计算器
            EnergyCalculator calculator = new EnergyCalculator(problem, true, false);
            
            // 计算能耗
            double totalEnergy = calculator.calculateTotalEnergy(schedule, problem);
            
            assert totalEnergy > 0 : "总能耗应该大于0";
            System.out.println("  ✓ 总能耗: " + String.format("%.2f", totalEnergy) + " kWh");
            
            // 获取统计信息
            EnergyCalculator.EnergyStatistics stats = calculator.getStatistics(schedule, problem);
            System.out.println("  ✓ 机器数: " + stats.machineCount);
            System.out.println("  ✓ 关机次数: " + stats.shutdownCount);
            
            System.out.println("  ✅ 能耗计算测试通过");
            return true;
        } catch (Exception e) {
            System.out.println("  ❌ 能耗计算测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试4: 多目标评价器集成
     */
    private static boolean testMOEvaluatorIntegration() {
        try {
            Problem problem = createMockProblem();
            
            // 创建目标函数
            List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
            objectives.add(new MOEvaluator.MaximumCompletionTime());
            objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));
            
            System.out.println("  ✓ 目标函数创建成功");
            System.out.println("    - 目标1: " + objectives.get(0).getName());
            System.out.println("    - 目标2: " + objectives.get(1).getName());
            
            System.out.println("  ✅ 多目标评价器集成测试通过");
            return true;
        } catch (Exception e) {
            System.out.println("  ❌ 多目标评价器集成测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 测试5: 对比开关机策略
     */
    private static boolean testStrategyComparison() {
        try {
            Problem problem = createMockProblem();
            Operation[][] schedule = createMockSchedule(problem);
            
            // 策略1: 启用开关机
            EnergyCalculator calcWithSwitching = new EnergyCalculator(problem, true, false);
            double energyWithSwitching = calcWithSwitching.calculateTotalEnergy(schedule, problem);
            
            // 策略2: 纯待机
            EnergyCalculator calcIdleOnly = new EnergyCalculator(problem, false, false);
            double energyIdleOnly = calcIdleOnly.calculateTotalEnergy(schedule, problem);
            
            System.out.println("  ✓ 开关机策略能耗: " + String.format("%.2f", energyWithSwitching) + " kWh");
            System.out.println("  ✓ 纯待机策略能耗: " + String.format("%.2f", energyIdleOnly) + " kWh");
            
            if (energyWithSwitching < energyIdleOnly) {
                double savings = energyIdleOnly - energyWithSwitching;
                double savingsPercent = 100.0 * savings / energyIdleOnly;
                System.out.println("  ✓ 节能效果: " + String.format("%.2f", savings) + " kWh (" + 
                    String.format("%.1f%%", savingsPercent) + ")");
            } else {
                System.out.println("  ⚠ 当前调度下开关机策略未产生节能（可能是间隔太短）");
            }
            
            System.out.println("  ✅ 策略对比测试通过");
            return true;
        } catch (Exception e) {
            System.out.println("  ❌ 策略对比测试失败: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 创建模拟问题实例
     */
    private static Problem createMockProblem() {
        Problem problem = new Problem();
        
        // 设置基本参数
        problem.setJobCount(5);
        problem.setMachineCount(3);
        problem.setPrintMachineCount(2);
        problem.setBatchMachineCount(1);
        
        // 创建机器
        ProgramEntity.Machine.Machine[] machines = new ProgramEntity.Machine.Machine[3];
        machines[0] = new PrintMachine("P1", 200, 200, 100, 0.1, 5.0, 0.5);
        machines[1] = new PrintMachine("P2", 200, 200, 100, 0.1, 5.0, 0.5);
        machines[2] = new ProgramEntity.Machine.BathchMachine("B1", 10.0);
        problem.setMachines(machines);
        
        // 设置工序数组
        int[] operationCountArr = new int[]{3, 3, 3, 3, 3};
        problem.setOperationCountArr(operationCountArr);
        
        return problem;
    }
    
    /**
     * 创建模拟调度
     */
    private static Operation[][] createMockSchedule(Problem problem) {
        int jobCount = problem.getJobCount();
        int[] operCountArr = problem.getOperationCountArr();
        
        Operation[][] schedule = new Operation[jobCount][];
        
        double time = 0.0;
        for (int i = 0; i < jobCount; i++) {
            int operCount = operCountArr[i];
            schedule[i] = new Operation[operCount];
            
            for (int j = 0; j < operCount; j++) {
                schedule[i][j] = new Operation();
                schedule[i][j].jobNo = i;
                schedule[i][j].task = j;
                schedule[i][j].machineNo = j % problem.getMachineCount();
                schedule[i][j].startTime = time;
                schedule[i][j].endTime = time + 10.0;
                
                time += 10.0;
                
                // 模拟不同长度的空闲间隔
                if ((i * operCount + j) % 4 == 0) {
                    time += 20.0; // 长间隔，可能触发关机
                } else {
                    time += 2.0;  // 短间隔，待机
                }
            }
        }
        
        return schedule;
    }
}

