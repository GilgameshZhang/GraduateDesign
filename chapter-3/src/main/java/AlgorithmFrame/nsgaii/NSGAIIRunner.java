package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.Machine;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * NSGA-II主程序 - 完整的多目标优化运行器
 * 
 * 功能：
 * 1. 从文件加载算例（支持能耗参数）
 * 2. 运行NSGA-II多目标优化
 * 3. 输出Pareto前沿和详细结果
 * 4. 保存结果到文件
 * 5. 能耗策略对比分析
 * 
 * 使用方法：
 * 1. 在main方法中指定算例文件路径
 * 2. 配置算法参数
 * 3. 运行程序
 * 
 * @author AI Assistant
 * @version 2.0
 */
public class NSGAIIRunner {
    
    // ==================== 配置参数 ====================
    
    /** 算法参数 */
    private static final int POPULATION_SIZE = 100;        // 种群规模
    private static final int MAX_GENERATIONS = 200;        // 最大代数
    private static final double CROSSOVER_RATE = 0.9;      // 交叉率
    private static final double MUTATION_RATE = 0.3;       // 变异率
    private static final double MAX_RUN_TIME = 1;       // 最大运行时间（分钟）
    
    /** 输出配置 */
    private static final boolean SAVE_RESULTS = true;      // 是否保存结果
    private static final boolean SHOW_PROGRESS = true;     // 是否显示进度
    private static final int DISPLAY_TOP_N = 20;           // 显示前N个解
    
    /** 随机种子（可选，0表示使用随机种子） */
    private static final long RANDOM_SEED = 0;
    
    // ==================== 主程序 ====================
    
    public static void main(String[] args) {
        try {
            // 打印欢迎信息
            printWelcome();
            
            // 第1步：获取算例文件路径
            String instancePath = getInstancePath(args);
            System.out.println("【算例文件】" + instancePath + "\n");
            
            // 第2步：加载算例
            System.out.println("════════════════════════════════════════");
            System.out.println("  第1步：加载算例");
            System.out.println("════════════════════════════════════════\n");
            
            Problem problem = loadProblem(instancePath);
            printProblemInfo(problem);
            
            // 第3步：配置多目标函数
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  第2步：配置多目标函数");
            System.out.println("════════════════════════════════════════\n");
            
            List<MOEvaluator.ObjectiveFunction> objectives = configureObjectives(problem);
            
            // 第4步：运行NSGA-II
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  第3步：运行NSGA-II优化");
            System.out.println("════════════════════════════════════════\n");
            
            printAlgorithmConfig();
            List<MOIndividual> paretoFront = runOptimization(problem, objectives);
            
            // 第5步：结果分析
            System.out.println("\n════════════════════════════════════════");
            System.out.println("  第4步：结果分析");
            System.out.println("════════════════════════════════════════\n");
            
            analyzeResults(paretoFront, problem, objectives);
            
            // 第6步：保存结果
            if (SAVE_RESULTS) {
                System.out.println("\n════════════════════════════════════════");
                System.out.println("  第5步：保存结果");
                System.out.println("════════════════════════════════════════\n");
                
                String outputPath = saveResults(paretoFront, instancePath, problem, objectives);
                System.out.println("✅ 结果已保存到: " + outputPath);
            }
            
            // 完成
            printCompletion(paretoFront);
            
        } catch (Exception e) {
            System.err.println("\n❌ 程序执行出错:");
            System.err.println("   " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== 核心功能方法 ====================
    
    /**
     * 获取算例文件路径
     */
    private static String getInstancePath(String[] args) {
        // 优先使用命令行参数
        if (args.length > 0) {
            return args[0];
        }
        
        // 否则使用默认路径或交互式选择
        Scanner scanner = new Scanner(System.in);
        
        // 显示可用的算例
        System.out.println("请选择算例文件:");
        System.out.println("1. 使用默认算例 (chapter-2 J20P3B2D5_01.txt)");
        System.out.println("2. 输入自定义路径");
        System.out.print("\n请选择 (1/2): ");
        
        String choice = scanner.nextLine().trim();
        
        if (choice.equals("1")) {
            return "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-3\\src\\main\\resources\\instance\\J100P5B4D10_02.txt";
        } else {
            System.out.print("请输入算例文件路径: ");
            return scanner.nextLine().trim();
        }
    }
    
    /**
     * 加载问题实例
     */
    private static Problem loadProblem(String instancePath) throws Exception {
        File instanceFile = new File(instancePath);
        
        if (!instanceFile.exists()) {
            throw new FileNotFoundException("算例文件不存在: " + instancePath);
        }
        
        System.out.println("正在加载算例...");
        
        // 使用EnergyAwareInput加载（自动处理能耗参数）
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        Problem problem = input.getProblemDesFromFile();
        
        System.out.println("✅ 算例加载成功");
        
        // 检查能耗参数
        if (input.hasEnergyParams()) {
            System.out.println("✅ 能耗参数: 从文件加载");
        } else {
            System.out.println("⚠️  能耗参数: 使用默认值");
        }
        
        return problem;
    }
    
    /**
     * 配置多目标函数
     */
    private static List<MOEvaluator.ObjectiveFunction> configureObjectives(Problem problem) {
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        
        // 目标1: 最大完工时间 (Cmax)
        objectives.add(new MOEvaluator.MaximumCompletionTime());
        System.out.println("✅ 目标1: Cmax (最大完工时间) - 最小化");
        
        // 目标2: 总能耗（含开关机策略）
        MOEvaluator.TotalEnergyConsumption energyObj = 
            new MOEvaluator.TotalEnergyConsumption(problem, true, false);
        objectives.add(energyObj);
        System.out.println("✅ 目标2: Energy (总能耗，智能开关机) - 最小化");
        
        return objectives;
    }
    
    /**
     * 运行NSGA-II优化
     */
    private static List<MOIndividual> runOptimization(Problem problem, 
                                                      List<MOEvaluator.ObjectiveFunction> objectives) {
        // 创建NSGA-II实例
        NSGAII nsgaii = new NSGAII(problem, objectives);
        
        // 设置参数
        nsgaii.setPopulationSize(POPULATION_SIZE);
        nsgaii.setMaxGenerations(MAX_GENERATIONS);
        nsgaii.setCrossoverRate(CROSSOVER_RATE);
        nsgaii.setMutationRate(MUTATION_RATE);
        nsgaii.setMaxRunTimeMinutes(MAX_RUN_TIME);
        nsgaii.enableLocalSearch(true);
        
        // 设置随机种子（如果指定）
        if (RANDOM_SEED > 0) {
            nsgaii.setSeed(RANDOM_SEED);
            System.out.println("随机种子: " + RANDOM_SEED);
        }
        
        System.out.println("\n开始优化...\n");
        
        // 运行算法
        long startTime = System.currentTimeMillis();
        List<MOIndividual> paretoFront = nsgaii.solve();
        long endTime = System.currentTimeMillis();
        
        double runTime = (endTime - startTime) / 1000.0;
        System.out.println("\n✅ 优化完成!");
        System.out.println("   运行时间: " + String.format("%.2f", runTime) + " 秒");
        
        return paretoFront;
    }
    
    /**
     * 分析结果
     */
    private static void analyzeResults(List<MOIndividual> paretoFront, 
                                       Problem problem,
                                       List<MOEvaluator.ObjectiveFunction> objectives) {
        if (paretoFront.isEmpty()) {
            System.out.println("⚠️  未找到Pareto最优解");
            return;
        }
        
        System.out.println("Pareto前沿规模: " + paretoFront.size() + " 个非支配解\n");
        
        // 1. 显示Pareto前沿
        displayParetoFront(paretoFront, DISPLAY_TOP_N);
        
        // 2. 统计信息
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  统计信息");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        printStatistics(paretoFront);
        
        // 3. 能耗策略对比
        System.out.println("\n━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  能耗策略对比");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
        
        MOIndividual bestCmax = findBestByObjective(paretoFront, 0);
        compareEnergyStrategies(bestCmax, problem, objectives);
    }
    
    /**
     * 显示Pareto前沿
     */
    private static void displayParetoFront(List<MOIndividual> paretoFront, int maxDisplay) {
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("  Pareto前沿（按Cmax排序，显示前" + maxDisplay + "个）");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        
        // 按Cmax排序
        List<MOIndividual> sorted = new ArrayList<>(paretoFront);
        sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
        
        // 表头
        System.out.println(String.format("\n%-6s %-12s %-14s %-12s %-10s %-12s",
            "序号", "Cmax", "能耗(kWh)", "PackingQ", "批次数", "拥挤距离"));
        System.out.println(repeatString("─", 72));
        
        // 显示解
        int count = Math.min(maxDisplay, sorted.size());
        for (int i = 0; i < count; i++) {
            MOIndividual ind = sorted.get(i);
            String crowding = ind.crowdingDistance == Double.POSITIVE_INFINITY ? 
                "INF" : String.format("%.4f", ind.crowdingDistance);
            
            System.out.println(String.format("%-6d %-12.2f %-14.2f %-12.4f %-10d %-12s",
                i + 1,
                ind.objectives[0],       // Cmax
                ind.objectives[1],       // Energy
                ind.packingQ,
                ind.batchCount,
                crowding
            ));
        }
        
        if (sorted.size() > maxDisplay) {
            System.out.println("\n... 还有 " + (sorted.size() - maxDisplay) + " 个解未显示");
        }
        
        System.out.println(repeatString("─", 72));
    }
    
    /**
     * 打印统计信息
     */
    private static void printStatistics(List<MOIndividual> paretoFront) {
        // Cmax统计
        double minCmax = Double.POSITIVE_INFINITY;
        double maxCmax = Double.NEGATIVE_INFINITY;
        
        // 能耗统计
        double minEnergy = Double.POSITIVE_INFINITY;
        double maxEnergy = Double.NEGATIVE_INFINITY;
        
        // PackingQ统计
        double sumPackingQ = 0.0;
        int sumBatches = 0;
        
        for (MOIndividual ind : paretoFront) {
            minCmax = Math.min(minCmax, ind.objectives[0]);
            maxCmax = Math.max(maxCmax, ind.objectives[0]);
            
            minEnergy = Math.min(minEnergy, ind.objectives[1]);
            maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
            
            sumPackingQ += ind.packingQ;
            sumBatches += ind.batchCount;
        }
        
        double avgPackingQ = sumPackingQ / paretoFront.size();
        double avgBatches = (double) sumBatches / paretoFront.size();
        
        System.out.println("Cmax范围:   [" + String.format("%.2f", minCmax) + 
                         ", " + String.format("%.2f", maxCmax) + "]");
        System.out.println("能耗范围:   [" + String.format("%.2f", minEnergy) + 
                         ", " + String.format("%.2f", maxEnergy) + "] kWh");
        System.out.println("PackingQ均值: " + String.format("%.4f", avgPackingQ));
        System.out.println("批次数均值:   " + String.format("%.2f", avgBatches));
        
        double cmaxRange = ((maxCmax - minCmax) / minCmax) * 100.0;
        double energyRange = ((maxEnergy - minEnergy) / minEnergy) * 100.0;
        
        System.out.println("\nCmax变化幅度:  " + String.format("%.2f%%", cmaxRange));
        System.out.println("能耗变化幅度:  " + String.format("%.2f%%", energyRange));
    }
    
    /**
     * 能耗策略对比
     */
    private static void compareEnergyStrategies(MOIndividual individual, 
                                                Problem problem,
                                                List<MOEvaluator.ObjectiveFunction> objectives) {
        // 创建工序矩阵并评价
        Operation[][] operationMatrix = createOperationMatrix(problem);
        MOEvaluator evaluator = new MOEvaluator();
        evaluator.evaluate(individual, problem, operationMatrix, objectives);
        
        // 策略1: 智能开关机
        EnergyCalculator calcWithSwitching = new EnergyCalculator(problem, true, false);
        double energyWithSwitch = calcWithSwitching.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsWithSwitch = 
            calcWithSwitching.getStatistics(operationMatrix, problem);
        
        // 策略2: 纯待机
        EnergyCalculator calcIdleOnly = new EnergyCalculator(problem, false, false);
        double energyIdle = calcIdleOnly.calculateTotalEnergy(operationMatrix, problem);
        EnergyCalculator.EnergyStatistics statsIdle = 
            calcIdleOnly.getStatistics(operationMatrix, problem);
        
        // 显示对比
        System.out.println("选择解: Cmax = " + String.format("%.2f", individual.objectives[0]));
        System.out.println();
        
        System.out.println("策略1 - 智能开关机:");
        System.out.println("  总能耗:     " + String.format("%.2f kWh", energyWithSwitch));
        System.out.println("  运行能耗:   " + String.format("%.2f kWh", statsWithSwitch.runEnergy));
        System.out.println("  待机能耗:   " + String.format("%.2f kWh", statsWithSwitch.idleEnergy));
        //System.out.println("  开关机能耗: " + String.format("%.2f kWh", statsWithSwitch.switchEnergy));
        System.out.println("  关机次数:   " + statsWithSwitch.shutdownCount + " 次");
        
        System.out.println("\n策略2 - 纯待机（基准）:");
        System.out.println("  总能耗:     " + String.format("%.2f kWh", energyIdle));
        System.out.println("  运行能耗:   " + String.format("%.2f kWh", statsIdle.runEnergy));
        System.out.println("  待机能耗:   " + String.format("%.2f kWh", statsIdle.idleEnergy));
        
        double savings = energyIdle - energyWithSwitch;
        double savingsPercent = (savings / energyIdle) * 100.0;
        
        System.out.println("\n节能效果:");
        if (savings > 0) {
            System.out.println("  ✅ 节省能耗: " + String.format("%.2f kWh", savings));
            System.out.println("  ✅ 节能比例: " + String.format("%.2f%%", savingsPercent));
        } else {
            System.out.println("  ⚠️  当前调度下开关机策略未节能");
            System.out.println("  原因: 可能空闲时间过短");
        }
    }
    
    /**
     * 保存结果到文件
     */
    private static String saveResults(List<MOIndividual> paretoFront,
                                      String instancePath,
                                      Problem problem,
                                      List<MOEvaluator.ObjectiveFunction> objectives) throws IOException {
        // 创建输出目录（带时间戳）
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String timestamp = sdf.format(new Date());
        String outputDir = "chapter-3/output/nsgaii_" + timestamp;
        new File(outputDir).mkdirs();
        
        String outputFile = outputDir + "/NSGAII_result.txt";
        
        try (PrintWriter writer = new PrintWriter(new FileWriter(outputFile))) {
            // 写入头部信息
            writer.println("╔═══════════════════════════════════════════════════════════════╗");
            writer.println("║           NSGA-II 多目标优化结果                              ║");
            writer.println("╚═══════════════════════════════════════════════════════════════╝");
            writer.println();
            writer.println("生成时间: " + new Date());
            writer.println("算例文件: " + instancePath);
            writer.println();
            
            // 问题信息
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  问题信息");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("工件数:       " + problem.getJobCount());
            writer.println("机器总数:     " + problem.getMachineCount());
            writer.println("打印机数:     " + problem.getPrintMachineCount());
            writer.println("批处理机数:   " + problem.getBatchMachineCount());
            writer.println();
            
            // 算法参数
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  算法参数");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("种群规模:     " + POPULATION_SIZE);
            writer.println("最大代数:     " + MAX_GENERATIONS);
            writer.println("交叉率:       " + CROSSOVER_RATE);
            writer.println("变异率:       " + MUTATION_RATE);
            writer.println("最大运行时间: " + MAX_RUN_TIME + " 分钟");
            writer.println();
            
            // Pareto前沿
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  Pareto前沿（共 " + paretoFront.size() + " 个解）");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            // 按Cmax排序
            List<MOIndividual> sorted = new ArrayList<>(paretoFront);
            sorted.sort((a, b) -> Double.compare(a.objectives[0], b.objectives[0]));
            
            writer.println(String.format("%-6s %-12s %-14s %-12s %-10s %-12s",
                "序号", "Cmax", "能耗(kWh)", "PackingQ", "批次数", "拥挤距离"));
            writer.println(repeatString("─", 72));
            
            for (int i = 0; i < sorted.size(); i++) {
                MOIndividual ind = sorted.get(i);
                String crowding = ind.crowdingDistance == Double.POSITIVE_INFINITY ? 
                    "INF" : String.format("%.4f", ind.crowdingDistance);
                
                writer.println(String.format("%-6d %-12.2f %-14.2f %-12.4f %-10d %-12s",
                    i + 1,
                    ind.objectives[0],
                    ind.objectives[1],
                    ind.packingQ,
                    ind.batchCount,
                    crowding
                ));
            }
            
            // 统计信息
            writer.println();
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println("  统计信息");
            writer.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            writer.println();
            
            double minCmax = sorted.get(0).objectives[0];
            double maxCmax = sorted.get(sorted.size() - 1).objectives[0];
            
            double minEnergy = Double.POSITIVE_INFINITY;
            double maxEnergy = Double.NEGATIVE_INFINITY;
            for (MOIndividual ind : sorted) {
                minEnergy = Math.min(minEnergy, ind.objectives[1]);
                maxEnergy = Math.max(maxEnergy, ind.objectives[1]);
            }
            
            writer.println("Cmax范围:      [" + String.format("%.2f", minCmax) + 
                         ", " + String.format("%.2f", maxCmax) + "]");
            writer.println("能耗范围:      [" + String.format("%.2f", minEnergy) + 
                         ", " + String.format("%.2f", maxEnergy) + "] kWh");
            
            double cmaxRange = ((maxCmax - minCmax) / minCmax) * 100.0;
            double energyRange = ((maxEnergy - minEnergy) / minEnergy) * 100.0;
            
            writer.println("Cmax变化幅度:  " + String.format("%.2f%%", cmaxRange));
            writer.println("能耗变化幅度:  " + String.format("%.2f%%", energyRange));
            
            writer.println();
            writer.println("═══════════════════════════════════════════════════════════════");
        }
        
        // 使用NSGAIIResultExporter导出完整结果
        System.out.println("\n正在生成可视化图表和详细记录...");
        Operation[][] operationMatrix = createOperationMatrix(problem);
        NSGAIIResultExporter exporter = new NSGAIIResultExporter(problem, operationMatrix);
        exporter.exportAllResults(paretoFront, outputDir);
        
        return outputDir;
    }
    
    // ==================== 辅助方法 ====================
    
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
     * 找到某个目标最优的解
     */
    private static MOIndividual findBestByObjective(List<MOIndividual> paretoFront, int objIndex) {
        return paretoFront.stream()
            .min((a, b) -> Double.compare(a.objectives[objIndex], b.objectives[objIndex]))
            .orElse(paretoFront.get(0));
    }
    
    /**
     * 重复字符串
     */
    private static String repeatString(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }
    
    /**
     * 打印欢迎信息
     */
    private static void printWelcome() {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                               ║");
        System.out.println("║         NSGA-II 多目标遗传算法 - 绿色车间调度优化            ║");
        System.out.println("║                                                               ║");
        System.out.println("║  目标1: Cmax (最大完工时间) - 最小化                         ║");
        System.out.println("║  目标2: Energy (总能耗，智能开关机) - 最小化                 ║");
        System.out.println("║                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
    }
    
    /**
     * 打印问题信息
     */
    private static void printProblemInfo(Problem problem) {
        System.out.println("工件数:       " + problem.getJobCount());
        System.out.println("机器总数:     " + problem.getMachineCount());
        System.out.println("  打印机:     " + problem.getPrintMachineCount());
        System.out.println("  批处理机:   " + problem.getBatchMachineCount());
        System.out.println("  离散加工机: " + 
            (problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount()));
    }
    
    /**
     * 打印算法配置
     */
    private static void printAlgorithmConfig() {
        System.out.println("种群规模:     " + POPULATION_SIZE);
        System.out.println("最大代数:     " + MAX_GENERATIONS);
        System.out.println("交叉率:       " + CROSSOVER_RATE);
        System.out.println("变异率:       " + MUTATION_RATE);
        System.out.println("最大运行时间: " + MAX_RUN_TIME + " 分钟");
    }
    
    /**
     * 打印完成信息
     */
    private static void printCompletion(List<MOIndividual> paretoFront) {
        System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
        System.out.println("║                                                               ║");
        System.out.println("║                      优化完成！                               ║");
        System.out.println("║                                                               ║");
        System.out.println("║  Pareto前沿规模: " + String.format("%-44d", paretoFront.size()) + "║");
        System.out.println("║                                                               ║");
        System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
    }
}

