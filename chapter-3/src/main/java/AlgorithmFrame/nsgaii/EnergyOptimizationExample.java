package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.Input;
import ProgramEntity.Operation;
import ProgramEntity.Problem;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 能耗优化示例
 * 
 * 演示如何使用NSGA-II进行多目标优化，包含开/关机策略的能耗目标
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class EnergyOptimizationExample {
    
    public static void main(String[] args) {
        System.out.println("========== NSGA-II 能耗优化示例 ==========\n");
        
        // 1. 加载问题实例
        String instancePath = "../chapter-2/src/main/resources/instance/J20/J20P3B2D5_01.txt";
        File instanceFile = new File(instancePath);
        
        if (!instanceFile.exists()) {
            System.err.println("错误：找不到算例文件: " + instancePath);
            System.err.println("请确保路径正确，或修改为你的算例路径");
            return;
        }
        
        Input input = new Input(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        System.out.println("【问题规模】");
        System.out.println("  工件数: " + problem.getJobCount());
        System.out.println("  机器数: " + problem.getMachineCount());
        System.out.println("  打印机数: " + problem.getPrintMachineCount());
        System.out.println("  批处理机数: " + problem.getBatchMachineCount());
        System.out.println();
        
        // 2. 定义多目标函数
        System.out.println("【目标函数设置】");
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        
        // 目标1: Makespan (Cmax)
        MOEvaluator.MaximumCompletionTime cmaxObj = new MOEvaluator.MaximumCompletionTime();
        objectives.add(cmaxObj);
        System.out.println("  目标1: " + cmaxObj.getName() + " (最小化)");
        
        // 目标2: 总能耗（含开/关机策略）
        MOEvaluator.TotalEnergyConsumption energyObj = 
            new MOEvaluator.TotalEnergyConsumption(problem, true, false);
        objectives.add(energyObj);
        System.out.println("  目标2: " + energyObj.getName() + " (最小化)");
        System.out.println();
        
        // 3. 演示能耗参数配置
        System.out.println("【能耗参数配置】");
        demonstratePowerParameters(problem);
        System.out.println();
        
        // 4. 对比开关机策略 vs 纯待机策略
        System.out.println("【策略对比】");
        compareSwitchingStrategies(problem);
        System.out.println();
        
        // 5. NSGA-II 算法配置
        System.out.println("【NSGA-II 配置】");
        GAParameters params = GAParameters.getDefaultParameters();
        params.popSize = 100;
        params.maxGen = 50;
        System.out.println("  种群规模: " + params.popSize);
        System.out.println("  最大代数: " + params.maxGen);
        System.out.println("  交叉概率: " + params.pc);
        System.out.println("  变异概率: " + params.pm);
        System.out.println();
        
        System.out.println("========== 示例完成 ==========");
        System.out.println("\n提示：要运行完整的NSGA-II算法，请：");
        System.out.println("  1. 确保已从chapter-2复制: Chromosome.java, CaculateFitness.java, SkyLinePacking.java");
        System.out.println("  2. 运行 AlgorithmFrame.nsgaii.NSGAII 主类");
        System.out.println("  3. 分析输出的Pareto前沿结果");
    }
    
    /**
     * 演示能耗参数配置
     */
    private static void demonstratePowerParameters(Problem problem) {
        System.out.println("默认能耗参数:");
        
        // 打印机参数
        PowerParameters printParams = PowerParameters.createPrintMachineDefault();
        System.out.println("\n  打印机:");
        System.out.println("    " + printParams);
        System.out.println("    运行功率: " + printParams.P_run + " kW");
        System.out.println("    待机功率: " + printParams.P_idle + " kW");
        System.out.println("    开关机能耗: " + printParams.E_switch + " kWh");
        System.out.println("    预热时间: " + printParams.T_warmup);
        System.out.println("    允许关机: " + printParams.allowSwitch);
        System.out.println("    盈亏平衡时间: " + printParams.getBreakEvenTime());
        System.out.println("    关机条件: gap >= " + 
            (printParams.T_warmup + printParams.getBreakEvenTime()));
        
        // 批处理机参数
        PowerParameters batchParams = PowerParameters.createBatchMachineDefault();
        System.out.println("\n  批处理机:");
        System.out.println("    " + batchParams);
        System.out.println("    允许关机: " + batchParams.allowSwitch + " (需保持温度)");
        
        // 离散加工机参数
        PowerParameters discreteParams = PowerParameters.createDiscreteMachineDefault();
        System.out.println("\n  离散加工机:");
        System.out.println("    " + discreteParams);
        System.out.println("    关机条件: gap >= " + 
            (discreteParams.T_warmup + discreteParams.getBreakEvenTime()));
    }
    
    /**
     * 对比开关机策略与纯待机策略
     */
    private static void compareSwitchingStrategies(Problem problem) {
        // 创建模拟调度
        Operation[][] mockSchedule = createMockSchedule(problem);
        
        // 策略1: 启用开关机
        EnergyCalculator calcWithSwitching = new EnergyCalculator(problem, true, false);
        double energyWithSwitching = calcWithSwitching.calculateTotalEnergy(mockSchedule, problem);
        EnergyCalculator.EnergyStatistics statsWithSwitching = 
            calcWithSwitching.getStatistics(mockSchedule, problem);
        
        // 策略2: 纯待机（不开关机）
        EnergyCalculator calcIdleOnly = new EnergyCalculator(problem, false, false);
        double energyIdleOnly = calcIdleOnly.calculateTotalEnergy(mockSchedule, problem);
        
        // 输出对比
        System.out.println("模拟调度能耗对比:");
        System.out.println("  策略1（开关机）: " + String.format("%.2f", energyWithSwitching) + " kWh");
        System.out.println("    - 关机次数: " + statsWithSwitching.shutdownCount);
        System.out.println("  策略2（纯待机）: " + String.format("%.2f", energyIdleOnly) + " kWh");
        
        double savings = energyIdleOnly - energyWithSwitching;
        if (savings > 0) {
            double savingsPercent = 100.0 * savings / energyIdleOnly;
            System.out.println("  节能效果: " + String.format("%.2f", savings) + " kWh (" + 
                String.format("%.1f%%", savingsPercent) + ")");
        } else {
            System.out.println("  注意: 当前调度下开关机策略未产生节能效果");
            System.out.println("  可能原因: 空闲间隔太短或开关机代价太高");
        }
    }
    
    /**
     * 创建模拟调度（用于演示）
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
                schedule[i][j].machineNo = j % problem.getMachineCount(); // 简单分配
                schedule[i][j].startTime = time;
                schedule[i][j].endTime = time + 10.0; // 假设每个工序10个时间单位
                
                time += 10.0;
                
                // 模拟空闲间隔
                if ((i + j) % 3 == 0) {
                    time += 15.0; // 长间隔，可能触发关机
                } else {
                    time += 2.0;  // 短间隔，待机
                }
            }
        }
        
        return schedule;
    }
}

