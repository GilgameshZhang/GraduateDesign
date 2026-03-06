package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 完整的NSGA-II多目标优化示例（真实能耗计算）
 * 
 * 目标函数：
 * 1. Cmax（最大完工时间）- 最小化
 * 2. Energy（总能耗，含开/关机策略）- 最小化
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class EnergyNSGAIIExample {
    
    public static void main(String[] args) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║     NSGA-II 多目标优化 - 绿色车间能耗优化示例            ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
        
        try {
            // ========== 第1步：加载问题实例 ==========
            System.out.println("【步骤1】加载问题实例");
            //Problem problem = createTestProblem();  // 使用内置测试问题
            // 如果要使用文件，取消下面的注释：
             String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J10\\J10P2B1D2_01.txt";
             Problem problem = new Input(new File(instancePath)).getProblemDesFromFile();
            
            System.out.println("  ✓ 工件数: " + problem.getJobCount());
            System.out.println("  ✓ 机器数: " + problem.getMachineCount());
            System.out.println("  ✓ 打印机: " + problem.getPrintMachineCount());
            System.out.println("  ✓ 批处理机: " + problem.getBatchMachineCount() + "\n");
            
            // ========== 第2步：配置能耗参数 ==========
            System.out.println("【步骤2】配置机器能耗参数");
            configureEnergyParameters(problem);
            
            // ========== 第3步：定义多目标函数 ==========
            System.out.println("\n【步骤3】定义多目标函数");
            List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
            
            // 目标1：Cmax（最大完工时间）
            objectives.add(new MOEvaluator.MaximumCompletionTime());
            System.out.println("  ✓ 目标1: " + objectives.get(0).getName() + " (最小化)");
            
            // 目标2：能耗（开关机策略）
            MOEvaluator.TotalEnergyConsumption energyObj = 
                new MOEvaluator.TotalEnergyConsumption(problem, true, false);
            objectives.add(energyObj);
            System.out.println("  ✓ 目标2: " + energyObj.getName() + " (最小化)");
            
            // ========== 第4步：初始化种群 ==========
            System.out.println("\n【步骤4】初始化种群");
            int popSize = 200;
            Random random = new Random(42);
            
            List<MOIndividual> population = initializePopulation(problem, popSize, random);
            System.out.println("  ✓ 种群规模: " + popSize);
            
            // ========== 第5步：评价初始种群 ==========
            System.out.println("\n【步骤5】评价初始种群");
            MOEvaluator evaluator = new MOEvaluator();
            
            for (MOIndividual ind : population) {
                Operation[][] operationMatrix = createOperationMatrix(problem);
                evaluator.evaluate(ind, problem, operationMatrix, objectives);
            }
            System.out.println("  ✓ 初始种群评价完成");
            
            // ========== 第6步：非支配排序 ==========
            System.out.println("\n【步骤6】非支配排序");
            List<List<MOIndividual>> fronts = NSGAIIOperations.fastNonDominatedSort(population);
            System.out.println("  ✓ Pareto前沿数: " + fronts.size());
            System.out.println("  ✓ 第一前沿个体数: " + fronts.get(0).size());
            
            // ========== 第7步：计算拥挤距离 ==========
            System.out.println("\n【步骤7】计算拥挤距离");
            for (List<MOIndividual> front : fronts) {
                NSGAIIOperations.assignCrowdingDistance(front);
            }
            System.out.println("  ✓ 拥挤距离计算完成");
            
            // ========== 第8步：展示Pareto前沿 ==========
            System.out.println("\n【步骤8】Pareto最优解集（前10个）");
            displayParetoFront(fronts.get(0), 10);
            
            // ========== 第9步：能耗分析 ==========
            System.out.println("\n【步骤9】能耗策略对比分析");
            analyzeEnergyStrategies(problem, fronts.get(0).get(0), evaluator, objectives);
            
            // ========== 第10步：保存结果（可选） ==========
            System.out.println("\n【步骤10】优化结果统计");
            printStatistics(fronts.get(0));
            
            System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
            System.out.println("║                 NSGA-II 优化完成！                        ║");
            System.out.println("╚═══════════════════════════════════════════════════════════╝");
            
        } catch (Exception e) {
            System.err.println("❌ 错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 配置机器能耗参数
     */
    private static void configureEnergyParameters(Problem problem) {
        Machine[] machines = problem.getMachines();
        
        for (int i = 0; i < machines.length; i++) {
            PowerParameters params;
            
            if (machines[i] instanceof PrintMachine) {
                // 打印机：允许关机
                params = new PowerParameters(
                    5.0,   // P_run (kW)
                    0.5,   // P_idle (kW)
                    2.0,   // E_switch (kWh)
                    5.0,   // T_warmup
                    true   // allowSwitch
                );
                System.out.println("  ✓ 打印机 " + (i+1) + ": " + params);
                
            } else if (machines[i] instanceof BathchMachine) {
                // 批处理机：不允许关机（保持温度）
                params = new PowerParameters(
                    3.0,   // P_run (kW)
                    0.8,   // P_idle (kW)
                    5.0,   // E_switch (kWh)
                    20.0,  // T_warmup
                    false  // allowSwitch
                );
                System.out.println("  ✓ 批处理机 " + (i+1) + ": " + params);
                
            } else {
                // 离散加工机：允许关机
                params = new PowerParameters(
                    2.0,   // P_run (kW)
                    0.3,   // P_idle (kW)
                    1.0,   // E_switch (kWh)
                    3.0,   // T_warmup
                    true   // allowSwitch
                );
                System.out.println("  ✓ 离散加工机 " + (i+1) + ": " + params);
            }
        }
    }
    
    /**
     * 初始化种群
     */
    private static List<MOIndividual> initializePopulation(Problem problem, int popSize, Random random) {
        List<MOIndividual> population = new ArrayList<>();
        
        // 获取工件信息
        Job[] jobs = problem.getJobs();
        
        for (int i = 0; i < popSize; i++) {
            // 生成随机染色体
            Chromosome chromosome = new Chromosome(jobs, random, problem);
            
            // 转换为多目标个体
            MOIndividual individual = new MOIndividual(chromosome, 2);  // 2个目标
            population.add(individual);
        }
        
        return population;
    }
    
    /**
     * 创建工序矩阵
     */
    private static Operation[][] createOperationMatrix(Problem problem) {
        int jobCount = problem.getJobCount();
        int[] operCountArr = problem.getOperationCountArr();
        
        Operation[][] operationMatrix = new Operation[jobCount][];
        for (int i = 0; i < jobCount; i++) {
            operationMatrix[i] = new Operation[operCountArr[i]];
            for (int j = 0; j < operCountArr[i]; j++) {
                operationMatrix[i][j] = new Operation();
            }
        }
        
        return operationMatrix;
    }
    
    /**
     * 显示Pareto前沿
     */
    private static void displayParetoFront(List<MOIndividual> paretoFront, int maxDisplay) {
        System.out.println("\n  排序: Cmax (↑) 优先");
        System.out.println("  " + repeatString("─", 70));
        System.out.println(String.format("  %-6s %-15s %-15s %-12s %-8s", 
            "序号", "Cmax", "能耗(kWh)", "packingQ", "批次数"));
        System.out.println("  " + repeatString("─", 70));
        
        // 按Cmax排序（从小到大）
        List<MOIndividual> sorted = new ArrayList<>(paretoFront);
        sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
        
        int count = Math.min(maxDisplay, sorted.size());
        for (int i = 0; i < count; i++) {
            MOIndividual ind = sorted.get(i);
            System.out.println(String.format("  %-6d %-15.2f %-15.2f %-12.4f %-8d",
                i + 1,
                ind.objectives[0],  // Cmax
                ind.objectives[1],  // Energy
                ind.packingQ,
                ind.batchCount
            ));
        }
        
        if (sorted.size() > maxDisplay) {
            System.out.println("  ... (共 " + sorted.size() + " 个非支配解)");
        }
        System.out.println("  " + repeatString("─", 70));
    }
    
    /**
     * 辅助方法：重复字符串（兼容Java 8）
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 能耗策略对比分析
     */
    private static void analyzeEnergyStrategies(Problem problem, 
                                                MOIndividual individual,
                                                MOEvaluator evaluator,
                                                List<MOEvaluator.ObjectiveFunction> objectives) {
        
        // 重新评价个体以获取操作矩阵
        Operation[][] operationMatrix = createOperationMatrix(problem);
        evaluator.evaluate(individual, problem, operationMatrix, objectives);
        
        // 策略1: 启用开关机策略
        EnergyCalculator calcWithSwitching = new EnergyCalculator(problem, true, false);
        double energyWithSwitching = calcWithSwitching.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsWithSwitching = 
            calcWithSwitching.getStatistics(operationMatrix, problem);
        
        // 策略2: 纯待机（关闭开关机策略）
        EnergyCalculator calcIdleOnly = new EnergyCalculator(problem, false, false);
        double energyIdleOnly = calcIdleOnly.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsIdleOnly = 
            calcIdleOnly.getStatistics(operationMatrix, problem);
        
        System.out.println("\n  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │              能耗策略对比（同一调度方案）                │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        
        // 策略1
        System.out.println("  │ 策略1: 智能开关机                                       │");
        System.out.println("  │   总能耗: " + String.format("%-45.2f", energyWithSwitching) + " kWh │");
        System.out.println("  │   运行能耗: " + String.format("%-43.2f", statsWithSwitching.runEnergy) + " kWh │");
        System.out.println("  │   空闲能耗: " + String.format("%-43.2f", statsWithSwitching.idleEnergy) + " kWh │");
        System.out.println("  │   关机次数: " + String.format("%-45d", statsWithSwitching.shutdownCount) + " 次 │");
        System.out.println("  │   待机次数: " + String.format("%-45d", statsWithSwitching.idleCount) + " 次 │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        
        // 策略2
        System.out.println("  │ 策略2: 纯待机（baseline）                              │");
        System.out.println("  │   总能耗: " + String.format("%-45.2f", energyIdleOnly) + " kWh │");
        System.out.println("  │   运行能耗: " + String.format("%-43.2f", statsIdleOnly.runEnergy) + " kWh │");
        System.out.println("  │   空闲能耗: " + String.format("%-43.2f", statsIdleOnly.idleEnergy) + " kWh │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        
        // 节能效果
        double savings = energyIdleOnly - energyWithSwitching;
        double savingsPercent = (savings / energyIdleOnly) * 100.0;
        
        if (savings > 0) {
            System.out.println("  │ 节能效果                                                │");
            System.out.println("  │   节省能耗: " + String.format("%-43.2f", savings) + " kWh │");
            System.out.println("  │   节能比例: " + String.format("%-44.1f", savingsPercent) + " %  │");
            System.out.println("  │   ✅ 开关机策略有效！                                   │");
        } else {
            System.out.println("  │   ⚠️ 当前调度下，开关机策略未产生节能                   │");
            System.out.println("  │   （可能是空闲间隔太短，不适合关机）                    │");
        }
        
        System.out.println("  └─────────────────────────────────────────────────────────┘");
    }
    
    /**
     * 打印统计信息
     */
    private static void printStatistics(List<MOIndividual> paretoFront) {
        // Cmax范围
        double minCmax = Double.POSITIVE_INFINITY;
        double maxCmax = Double.NEGATIVE_INFINITY;
        
        // 能耗范围
        double minEnergy = Double.POSITIVE_INFINITY;
        double maxEnergy = Double.NEGATIVE_INFINITY;
        
        for (MOIndividual ind : paretoFront) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            maxCmax = Math.max(maxCmax, ind.objectives[0]);
            
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
            maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
        }
        
        System.out.println("  Pareto前沿规模: " + paretoFront.size() + " 个非支配解");
        System.out.println("  Cmax 范围: [" + String.format("%.2f", minCmax) + ", " + 
                         String.format("%.2f", maxCmax) + "]");
        System.out.println("  能耗范围: [" + String.format("%.2f", minEnergy) + ", " + 
                         String.format("%.2f", maxEnergy) + "] kWh");
        System.out.println("  能耗变化: " + String.format("%.2f%%", 
                         ((maxEnergy - minEnergy) / minEnergy * 100)));
    }
    
    /**
     * 创建测试问题（简化版）
     */
    private static Problem createTestProblem() {
        Problem problem = new Problem();
        
        // 基本参数
        int jobCount = 10;
        int printMachineCount = 2;
        int batchMachineCount = 1;
        int discreteMachineCount = 3;
        int totalMachineCount = printMachineCount + batchMachineCount + discreteMachineCount;
        
        problem.setJobCount(jobCount);
        problem.setMachineCount(totalMachineCount);
        problem.setPrintMachineCount(printMachineCount);
        problem.setBatchMachineCount(batchMachineCount);
        
        // 创建机器
        Machine[] machines = new Machine[totalMachineCount];
        machines[0] = new PrintMachine("P1", 200, 200, 100, 0.1, 5.0, 0.5);
        machines[1] = new PrintMachine("P2", 200, 200, 100, 0.1, 5.0, 0.5);
        machines[2] = new BathchMachine("B1", 10.0);
        machines[3] = new DiscreteProcessingMachine("D1", new ArrayList<>(), new ArrayList<>());
        machines[4] = new DiscreteProcessingMachine("D2", new ArrayList<>(), new ArrayList<>());
        machines[5] = new DiscreteProcessingMachine("D3", new ArrayList<>(), new ArrayList<>());
        problem.setMachines(machines);
        
        // 工序数
        int[] operationCountArr = new int[jobCount];
        for (int i = 0; i < jobCount; i++) {
            operationCountArr[i] = 3;  // 打印 -> 批处理 -> 离散
        }
        problem.setOperationCountArr(operationCountArr);
        
        // 创建零件（Items）
        Item[] items = new Item[jobCount];
        for (int i = 0; i < jobCount; i++) {
            items[i] = new Item(
                "Item" + i,
                50 + (i % 5) * 10,  // l
                50 + (i % 5) * 10,  // w
                10                   // h
            );
        }
        problem.setItems(items);
        
        // 工序时间矩阵（简化）
        int totalOperCount = jobCount * 3;
        double[][] proDesMatrix = new double[totalOperCount][totalMachineCount];
        
        for (int i = 0; i < totalOperCount; i++) {
            for (int j = 0; j < totalMachineCount; j++) {
                proDesMatrix[i][j] = Double.MAX_VALUE;
            }
        }
        
        // 设置打印工序（工序0）
        for (int i = 0; i < jobCount; i++) {
            int opIndex = i * 3;
            proDesMatrix[opIndex][0] = 0;  // P1
            proDesMatrix[opIndex][1] = 0;  // P2
        }
        
        // 设置批处理工序（工序1）
        for (int i = 0; i < jobCount; i++) {
            int opIndex = i * 3 + 1;
            proDesMatrix[opIndex][2] = 10.0;  // B1
        }
        
        // 设置离散工序（工序2）
        for (int i = 0; i < jobCount; i++) {
            int opIndex = i * 3 + 2;
            proDesMatrix[opIndex][3] = 5.0 + (i % 3);   // D1
            proDesMatrix[opIndex][4] = 6.0 + (i % 3);   // D2
            proDesMatrix[opIndex][5] = 7.0 + (i % 3);   // D3
        }
        
        problem.setProDesMatrix(proDesMatrix);
        
        // 工序索引映射
        int[][] operationToIndex = new int[jobCount][3];
        for (int i = 0; i < jobCount; i++) {
            for (int j = 0; j < 3; j++) {
                operationToIndex[i][j] = i * 3 + j;
            }
        }
        problem.setOperationToIndex(operationToIndex);
        
        return problem;
    }
}


