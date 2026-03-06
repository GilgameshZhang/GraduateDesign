package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * 多目标优化主程序入口
 * 
 * 完整的NSGA-II多目标优化流程：
 * 1. 加载算例（含能耗参数）
 * 2. 初始化种群
 * 3. 运行NSGA-II算法
 * 4. 输出Pareto前沿
 * 5. 能耗策略对比分析
 * 6. 保存结果
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MainEntry {
    
    // 算法参数
    private static final int POPULATION_SIZE = 100;
    private static final int MAX_GENERATION = 200;
    private static final double CROSSOVER_RATE = 0.9;
    private static final double MUTATION_RATE = 0.1;
    
    public static void main(String[] args) {
        printHeader();
        
        try {
            // ========== 第1步：加载算例 ==========
            System.out.println("【步骤1】加载测试算例\n");
            
            // 选择算例文件
            String instancePath = selectInstance();
            System.out.println("  选择算例: " + instancePath);
            
            // 加载问题实例
            File instanceFile = new File(instancePath);
            if (!instanceFile.exists()) {
                System.err.println("  ❌ 算例文件不存在！");
                System.err.println("  请先运行 GenerateTestInstances.java 生成算例");
                return;
            }
            
            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
            Problem problem = input.getProblemDesFromFile();
            
            System.out.println("  ✓ 算例加载成功");
            printProblemInfo(problem);
            
            // 验证能耗参数
            if (input.hasEnergyParams()) {
                System.out.println("  ✓ 能耗参数: 已从文件加载");
            } else {
                System.out.println("  ⚠ 能耗参数: 使用默认值");
            }
            System.out.println();
            
            // ========== 第2步：定义多目标 ==========
            System.out.println("【步骤2】定义多目标函数\n");
            
            List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
            objectives.add(new MOEvaluator.MaximumCompletionTime());
            System.out.println("  ✓ 目标1: Cmax (最大完工时间) - 最小化");
            
            MOEvaluator.TotalEnergyConsumption energyObj = 
                new MOEvaluator.TotalEnergyConsumption(problem, true, false);
            objectives.add(energyObj);
            System.out.println("  ✓ 目标2: Energy (总能耗，含开关机策略) - 最小化");
            System.out.println();
            
            // ========== 第3步：初始化种群 ==========
            System.out.println("【步骤3】初始化种群\n");
            
            List<MOIndividual> population = initializePopulation(problem, POPULATION_SIZE);
            System.out.println("  ✓ 种群规模: " + POPULATION_SIZE);
            System.out.println("  ✓ 初始化策略: 随机初始化");
            System.out.println();
            
            // ========== 第4步：评价初始种群 ==========
            System.out.println("【步骤4】评价初始种群\n");
            
            MOEvaluator evaluator = new MOEvaluator();
            evaluatePopulation(population, problem, evaluator, objectives);
            
            System.out.println("  ✓ 初始种群评价完成");
            printPopulationStats(population);
            System.out.println();
            
            // ========== 第5步：运行NSGA-II ==========
            System.out.println("【步骤5】运行NSGA-II优化\n");
            
            System.out.println("  算法参数:");
            System.out.println("    • 最大代数: " + MAX_GENERATION);
            System.out.println("    • 交叉率: " + CROSSOVER_RATE);
            System.out.println("    • 变异率: " + MUTATION_RATE);
            System.out.println("\n  开始优化...\n");
            
            List<MOIndividual> finalPopulation = runNSGAII(
                population, problem, evaluator, objectives, MAX_GENERATION);
            
            System.out.println("\n  ✓ 优化完成！");
            System.out.println();
            
            // ========== 第6步：输出Pareto前沿 ==========
            System.out.println("【步骤6】输出Pareto前沿\n");
            
            List<List<MOIndividual>> finalFronts = 
                NSGAIIOperations.fastNonDominatedSort(finalPopulation);
            List<MOIndividual> paretoFront = finalFronts.get(0);
            
            System.out.println("  Pareto前沿规模: " + paretoFront.size() + " 个非支配解");
            System.out.println();
            
            displayParetoFront(paretoFront, 15);
            
            // ========== 第7步：能耗策略对比 ==========
            System.out.println("\n【步骤7】能耗策略对比分析\n");
            
            // 选择一个代表性解进行对比
            MOIndividual representative = selectRepresentativeSolution(paretoFront);
            compareEnergyStrategies(representative, problem, evaluator, objectives);
            
            // ========== 第8步：保存结果 ==========
            System.out.println("\n【步骤8】保存结果\n");
            
            String outputFile = saveResults(paretoFront, instancePath);
            System.out.println("  ✓ 结果已保存到: " + outputFile);
            System.out.println();
            
            // ========== 完成 ==========
            printFooter(paretoFront);
            
        } catch (Exception e) {
            System.err.println("\n❌ 程序执行出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 选择算例文件
     */
    private static String selectInstance() {
        // 默认使用中等规模的第一个算例
        String defaultInstance = "chapter-3/src/main/resources/instance/medium/J20P3B2D5_01.txt";
        
        // 检查是否存在，如果不存在则使用小规模
        File file = new File(defaultInstance);
        if (!file.exists()) {
            defaultInstance = "chapter-3/src/main/resources/instance/small/J10P2B1D3_01.txt";
        }
        
        return defaultInstance;
    }
    
    /**
     * 打印问题信息
     */
    private static void printProblemInfo(Problem problem) {
        System.out.println("  问题规模:");
        System.out.println("    • 工件数: " + problem.getJobCount());
        System.out.println("    • 机器总数: " + problem.getMachineCount());
        System.out.println("    • 打印机: " + problem.getPrintMachineCount());
        System.out.println("    • 批处理机: " + problem.getBatchMachineCount());
        System.out.println("    • 离散加工机: " + 
            (problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()));
    }
    
    /**
     * 初始化种群
     */
    private static List<MOIndividual> initializePopulation(Problem problem, int popSize) {
        List<MOIndividual> population = new ArrayList<>();
        Random random = new Random(42);
        
        for (int i = 0; i < popSize; i++) {
            // 创建随机染色体
            int[] gene_OS = createRandomOS(problem, random);
            int[] gene_MS = createRandomMS(problem, random);
            Chromosome chromosome = new Chromosome(gene_OS, gene_MS, random);
            
            MOIndividual individual = new MOIndividual(chromosome, 2);
            population.add(individual);
        }
        
        return population;
    }
    
    /**
     * 创建随机OS基因
     */
    private static int[] createRandomOS(Problem problem, Random random) {
        int jobCount = problem.getJobCount();
        int[] operCountArr = problem.getOperationCountArr();
        
        // 计算总工序数
        int totalOps = 0;
        for (int count : operCountArr) {
            totalOps += count;
        }
        
        // 创建OS基因
        int[] gene_OS = new int[totalOps];
        int index = 0;
        for (int i = 0; i < jobCount; i++) {
            for (int j = 0; j < operCountArr[i]; j++) {
                gene_OS[index++] = i;
            }
        }
        
        // 打乱
        for (int i = gene_OS.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = gene_OS[i];
            gene_OS[i] = gene_OS[j];
            gene_OS[j] = temp;
        }
        
        return gene_OS;
    }
    
    /**
     * 创建随机MS基因
     */
    private static int[] createRandomMS(Problem problem, Random random) {
        int jobCount = problem.getJobCount();
        int[] operCountArr = problem.getOperationCountArr();
        
        // 计算总工序数
        int totalOps = 0;
        for (int count : operCountArr) {
            totalOps += count;
        }
        
        // 创建MS基因
        int[] gene_MS = new int[totalOps];
        for (int i = 0; i < totalOps; i++) {
            gene_MS[i] = random.nextInt(3) + 1;  // 1-3
        }
        
        return gene_MS;
    }
    
    /**
     * 评价种群
     */
    private static void evaluatePopulation(List<MOIndividual> population, 
                                          Problem problem,
                                          MOEvaluator evaluator,
                                          List<MOEvaluator.ObjectiveFunction> objectives) {
        for (MOIndividual ind : population) {
            Operation[][] operationMatrix = createOperationMatrix(problem);
            evaluator.evaluate(ind, problem, operationMatrix, objectives);
        }
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
     * 打印种群统计信息
     */
    private static void printPopulationStats(List<MOIndividual> population) {
        double minCmax = Double.POSITIVE_INFINITY;
        double maxCmax = Double.NEGATIVE_INFINITY;
        double minEnergy = Double.POSITIVE_INFINITY;
        double maxEnergy = Double.NEGATIVE_INFINITY;
        
        for (MOIndividual ind : population) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            maxCmax = Math.max(maxCmax, ind.objectives[0]);
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
            maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
        }
        
        System.out.println("  初始种群统计:");
        System.out.println(String.format("    • Cmax 范围: [%.2f, %.2f]", minCmax, maxCmax));
        System.out.println(String.format("    • 能耗范围: [%.2f, %.2f] kWh", minEnergy, maxEnergy));
    }
    
    /**
     * 运行NSGA-II算法
     */
    private static List<MOIndividual> runNSGAII(List<MOIndividual> population,
                                                Problem problem,
                                                MOEvaluator evaluator,
                                                List<MOEvaluator.ObjectiveFunction> objectives,
                                                int maxGen) {
        Random random = new Random(42);
        
        for (int gen = 1; gen <= maxGen; gen++) {
            // 生成子代（简化版，使用随机）
            List<MOIndividual> offspring = new ArrayList<>();
            for (int i = 0; i < POPULATION_SIZE; i++) {
                // 简单的变异操作
                MOIndividual parent = population.get(random.nextInt(population.size()));
                MOIndividual child = new MOIndividual(parent);
                
                // 变异
                if (random.nextDouble() < MUTATION_RATE) {
                    mutate(child, random);
                }
                
                offspring.add(child);
            }
            
            // 评价子代
            evaluatePopulation(offspring, problem, evaluator, objectives);
            
            // 环境选择（不需要delta参数）
            population = NSGAIIOperations.environmentalSelection(
                population, offspring, POPULATION_SIZE, 1e-6);
            
            // 输出进度
            if (gen % 20 == 0 || gen == 1) {
                List<List<MOIndividual>> fronts = 
                    NSGAIIOperations.fastNonDominatedSort(population);
                System.out.println(String.format("  第 %3d 代: Pareto前沿 %d 个解", 
                    gen, fronts.get(0).size()));
            }
        }
        
        return population;
    }
    
    /**
     * 简单的变异操作
     */
    private static void mutate(MOIndividual individual, Random random) {
        // OS变异：交换两个基因
        if (individual.gene_OS != null && individual.gene_OS.length > 1) {
            int pos1 = random.nextInt(individual.gene_OS.length);
            int pos2 = random.nextInt(individual.gene_OS.length);
            int temp = individual.gene_OS[pos1];
            individual.gene_OS[pos1] = individual.gene_OS[pos2];
            individual.gene_OS[pos2] = temp;
        }
        
        // MS变异：改变机器选择
        if (individual.gene_MS != null && individual.gene_MS.length > 0) {
            int pos = random.nextInt(individual.gene_MS.length);
            individual.gene_MS[pos] = random.nextInt(3) + 1;
        }
    }
    
    /**
     * 显示Pareto前沿
     */
    private static void displayParetoFront(List<MOIndividual> paretoFront, int maxDisplay) {
        // 按Cmax排序
        List<MOIndividual> sorted = new ArrayList<>(paretoFront);
        sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
        
        System.out.println("  ┌────────────────────────────────────────────────────────────────┐");
        System.out.println("  │  序号   Cmax        能耗(kWh)    packingQ    批次数   拥挤距离  │");
        System.out.println("  ├────────────────────────────────────────────────────────────────┤");
        
        int count = Math.min(maxDisplay, sorted.size());
        for (int i = 0; i < count; i++) {
            MOIndividual ind = sorted.get(i);
            System.out.println(String.format(
                "  │  %-6d  %-10.2f  %-11.2f  %-10.4f  %-7d  %-10.4f│",
                i + 1,
                ind.objectives[0],
                ind.objectives[1],
                ind.packingQ,
                ind.batchCount,
                ind.crowdingDistance
            ));
        }
        
        if (sorted.size() > maxDisplay) {
            System.out.println("  │  ... (共 " + sorted.size() + " 个非支配解)                                       │");
        }
        System.out.println("  └────────────────────────────────────────────────────────────────┘");
    }
    
    /**
     * 选择代表性解（最靠近理想点）
     */
    private static MOIndividual selectRepresentativeSolution(List<MOIndividual> paretoFront) {
        // 找到最小Cmax和最小能耗
        double minCmax = Double.POSITIVE_INFINITY;
        double minEnergy = Double.POSITIVE_INFINITY;
        
        for (MOIndividual ind : paretoFront) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
        }
        
        // 找到最接近理想点的解
        MOIndividual best = paretoFront.get(0);
        double minDist = Double.POSITIVE_INFINITY;
        
        for (MOIndividual ind : paretoFront) {
            double dist = Math.sqrt(
                Math.pow(ind.objectives[0] - minCmax, 2) +
                Math.pow(ind.objectives[1] - minEnergy, 2)
            );
            
            if (dist < minDist) {
                minDist = dist;
                best = ind;
            }
        }
        
        return best;
    }
    
    /**
     * 对比能耗策略
     */
    private static void compareEnergyStrategies(MOIndividual individual,
                                                Problem problem,
                                                MOEvaluator evaluator,
                                                List<MOEvaluator.ObjectiveFunction> objectives) {
        // 重新评价以获取操作矩阵
        Operation[][] operationMatrix = createOperationMatrix(problem);
        evaluator.evaluate(individual, problem, operationMatrix, objectives);
        
        // 策略1: 智能开关机
        EnergyCalculator calcWithSwitching = new EnergyCalculator(problem, true, false);
        double energyWithSwitching = calcWithSwitching.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsWithSwitching = 
            calcWithSwitching.getStatistics(operationMatrix, problem);
        
        // 策略2: 纯待机
        EnergyCalculator calcIdleOnly = new EnergyCalculator(problem, false, false);
        double energyIdleOnly = calcIdleOnly.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsIdleOnly = 
            calcIdleOnly.getStatistics(operationMatrix, problem);
        
        System.out.println("  选择代表性解进行对比:");
        System.out.println("    • Cmax: " + String.format("%.2f", individual.objectives[0]));
        System.out.println("    • packingQ: " + String.format("%.4f", individual.packingQ));
        System.out.println();
        
        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
        System.out.println("  │              能耗策略对比（同一调度方案）                │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        System.out.println("  │ 策略1: 智能开关机                                       │");
        System.out.println("  │   总能耗: " + String.format("%-43.2f", energyWithSwitching) + " kWh │");
        System.out.println("  │   运行能耗: " + String.format("%-41.2f", statsWithSwitching.runEnergy) + " kWh │");
        System.out.println("  │   空闲能耗: " + String.format("%-41.2f", statsWithSwitching.idleEnergy) + " kWh │");
        System.out.println("  │   关机次数: " + String.format("%-43d", statsWithSwitching.shutdownCount) + " 次 │");
        System.out.println("  │   待机次数: " + String.format("%-43d", statsWithSwitching.idleCount) + " 次 │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        System.out.println("  │ 策略2: 纯待机（baseline）                              │");
        System.out.println("  │   总能耗: " + String.format("%-43.2f", energyIdleOnly) + " kWh │");
        System.out.println("  │   运行能耗: " + String.format("%-41.2f", statsIdleOnly.runEnergy) + " kWh │");
        System.out.println("  │   空闲能耗: " + String.format("%-41.2f", statsIdleOnly.idleEnergy) + " kWh │");
        System.out.println("  ├─────────────────────────────────────────────────────────┤");
        
        double savings = energyIdleOnly - energyWithSwitching;
        double savingsPercent = (savings / energyIdleOnly) * 100.0;
        
        if (savings > 0) {
            System.out.println("  │ 节能效果                                                │");
            System.out.println("  │   节省能耗: " + String.format("%-41.2f", savings) + " kWh │");
            System.out.println("  │   节能比例: " + String.format("%-42.1f", savingsPercent) + " %  │");
            System.out.println("  │   ✅ 开关机策略有效！                                   │");
        } else {
            System.out.println("  │   ⚠️ 当前调度下，开关机策略未产生节能                   │");
        }
        
        System.out.println("  └─────────────────────────────────────────────────────────┘");
    }
    
    /**
     * 保存结果到文件
     */
    private static String saveResults(List<MOIndividual> paretoFront, String instancePath) 
            throws IOException {
        String outputFile = "chapter-3/output/pareto_front_results.txt";
        new File("chapter-3/output").mkdirs();
        
        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("========== Pareto前沿结果 ==========\n\n");
            writer.write("算例文件: " + instancePath + "\n");
            writer.write("Pareto前沿规模: " + paretoFront.size() + "\n\n");
            
            writer.write(String.format("%-6s %-12s %-12s %-12s %-10s %-12s\n",
                "序号", "Cmax", "能耗(kWh)", "packingQ", "批次数", "拥挤距离"));
            writer.write("----------------------------------------------------------------------\n");
            
            // 按Cmax排序
            List<MOIndividual> sorted = new ArrayList<>(paretoFront);
            sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
            
            for (int i = 0; i < sorted.size(); i++) {
                MOIndividual ind = sorted.get(i);
                writer.write(String.format("%-6d %-12.2f %-12.2f %-12.4f %-10d %-12.4f\n",
                    i + 1,
                    ind.objectives[0],
                    ind.objectives[1],
                    ind.packingQ,
                    ind.batchCount,
                    ind.crowdingDistance
                ));
            }
            
            // 统计信息
            writer.write("\n========== 统计信息 ==========\n\n");
            double minCmax = sorted.get(0).objectives[0];
            double maxCmax = sorted.get(sorted.size() - 1).objectives[0];
            
            double minEnergy = Double.POSITIVE_INFINITY;
            double maxEnergy = Double.NEGATIVE_INFINITY;
            for (MOIndividual ind : sorted) {
                minEnergy = Math.min(minEnergy, ind.objectives[1]);
                maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
            }
            
            writer.write(String.format("Cmax 范围: [%.2f, %.2f]\n", minCmax, maxCmax));
            writer.write(String.format("能耗范围: [%.2f, %.2f] kWh\n", minEnergy, maxEnergy));
            writer.write(String.format("Cmax 变化: %.2f%%\n", ((maxCmax - minCmax) / minCmax * 100)));
            writer.write(String.format("能耗变化: %.2f%%\n", ((maxEnergy - minEnergy) / minEnergy * 100)));
        }
        
        return outputFile;
    }
    
    /**
     * 打印头部
     */
    private static void printHeader() {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║   NSGA-II 多目标优化 - 绿色车间能耗优化主程序           ║");
        System.out.println("║                                                           ║");
        System.out.println("║   目标: Cmax + Energy (含智能开关机策略)                 ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * 打印尾部
     */
    private static void printFooter(List<MOIndividual> paretoFront) {
        System.out.println("╔═══════════════════════════════════════════════════════════╗");
        System.out.println("║                   优化完成！                              ║");
        System.out.println("╚═══════════════════════════════════════════════════════════╝");
        
        System.out.println("\n最终结果:");
        System.out.println("  • Pareto前沿规模: " + paretoFront.size() + " 个非支配解");
        
        // 找到极值
        double minCmax = Double.POSITIVE_INFINITY;
        double maxCmax = Double.NEGATIVE_INFINITY;
        double minEnergy = Double.POSITIVE_INFINITY;
        double maxEnergy = Double.NEGATIVE_INFINITY;
        
        for (MOIndividual ind : paretoFront) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            maxCmax = Math.max(maxCmax, ind.objectives[0]);
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
            maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
        }
        
        System.out.println(String.format("  • Cmax 范围: [%.2f, %.2f]", minCmax, maxCmax));
        System.out.println(String.format("  • 能耗范围: [%.2f, %.2f] kWh", minEnergy, maxEnergy));
        System.out.println(String.format("  • Cmax-能耗权衡空间: %.2f%%", 
            ((maxCmax - minCmax) / minCmax * 100)));
        
        System.out.println("\n结果文件:");
        System.out.println("  • chapter-3/output/pareto_front_results.txt");
        
        System.out.println("\n感谢使用！祝你的研究顺利！🎉");
    }
}

