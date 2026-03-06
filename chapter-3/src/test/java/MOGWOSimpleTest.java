//package AlgorithmFrame.mogwo;
//
//import ProblemFrame.*;
//import ProgramEntity.*;
//import java.io.File;
//import java.util.*;
//
///**
// * MOGWO 简单测试
// *
// * 用于快速验证算法实现是否正确
// *
// * @author AI Assistant
// * @version 1.0
// */
//public class SimpleTest {
//
//    public static void main(String[] args) {
//        System.out.println("╔" + "═".repeat(60) + "╗");
//        System.out.println("║" + " ".repeat(18) + "MOGWO 简单测试" + " ".repeat(24) + "║");
//        System.out.println("╚" + "═".repeat(60) + "╝\n");
//
//        // 测试1: 基本功能测试
//        System.out.println("【测试1】基本功能测试");
//        System.out.println("-".repeat(62));
//        testBasicFunctionality();
//
//        System.out.println("\n" + "═".repeat(62) + "\n");
//
//        // 测试2: 小规模算例测试
//        System.out.println("【测试2】小规模算例测试");
//        System.out.println("-".repeat(62));
//        testSmallInstance();
//
//        System.out.println("\n" + "═".repeat(62) + "\n");
//
//        // 测试3: 与NSGA-II快速对比
//        System.out.println("【测试3】与NSGA-II快速对比");
//        System.out.println("-".repeat(62));
//        quickComparison();
//
//        System.out.println("\n所有测试完成！");
//    }
//
//    /**
//     * 测试1: 基本功能测试
//     */
//    private static void testBasicFunctionality() {
//        try {
//            // 创建一个简单的测试问题
//            String problemFile = "src/main/resources/test_instance_small_2m_5j.txt";
//
//            File file = new File(problemFile);
//            if (!file.exists()) {
//                System.out.println("⚠️  测试文件不存在: " + problemFile);
//                System.out.println("   请确保测试文件存在后重试");
//                return;
//            }
//
//            // 加载问题
//            EnergyAwareInput input = new EnergyAwareInput(file);
//            Problem problem = input.getProblemDesFromFile();
//
//            // 定义目标
//            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
//                new MOEvaluator.MaximumCompletionTime(),
//                new MOEvaluator.TotalEnergyConsumption(problem)
//            );
//
//            // 创建MOGWO
//            MOGWO mogwo = new MOGWO(problem, objectives);
//            mogwo.setPopulationSize(20);    // 小种群
//            mogwo.setArchiveSize(20);
//            mogwo.setMaxGenerations(50);    // 少代数
//            mogwo.setSeed(12345L);
//
//            System.out.println("✓ 算法创建成功");
//
//            // 运行算法
//            System.out.println("✓ 开始运行算法（种群20，代数50）...\n");
//            List<MOIndividual> paretoFront = mogwo.solve();
//
//            // 检查结果
//            if (paretoFront == null || paretoFront.isEmpty()) {
//                System.out.println("✗ 测试失败：Pareto前沿为空");
//                return;
//            }
//
//            System.out.println("\n✓ 算法运行成功");
//            System.out.println("✓ Pareto前沿大小: " + paretoFront.size());
//
//            // 显示前3个解
//            System.out.println("\n前3个解:");
//            for (int i = 0; i < Math.min(3, paretoFront.size()); i++) {
//                MOIndividual ind = paretoFront.get(i);
//                System.out.printf("  解%d: Cmax=%.2f, Energy=%.2f\n",
//                    i + 1, ind.objectives[0], ind.objectives[1]);
//            }
//
//            System.out.println("\n✅ 基本功能测试通过");
//
//        } catch (Exception e) {
//            System.out.println("✗ 测试失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 测试2: 小规模算例测试
//     */
//    private static void testSmallInstance() {
//        try {
//            // 尝试使用能耗算例
//            String[] possibleFiles = {
//                "src/main/resources/instance/energy/J20P3B2D5_energy.txt",
//                "src/main/resources/instance/J20/J20P3B2D5_01.txt",
//                "src/main/resources/test_instance_medium_3m_10j.txt"
//            };
//
//            String problemFile = null;
//            for (String file : possibleFiles) {
//                if (new File(file).exists()) {
//                    problemFile = file;
//                    break;
//                }
//            }
//
//            if (problemFile == null) {
//                System.out.println("⚠️  未找到合适的测试算例");
//                System.out.println("   请确保以下文件之一存在：");
//                for (String file : possibleFiles) {
//                    System.out.println("   - " + file);
//                }
//                return;
//            }
//
//            System.out.println("使用算例: " + problemFile);
//
//            // 运行MOGWO
//            System.out.println("运行参数: 种群50，代数100");
//
//            File instanceFile = new File(problemFile);
//            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
//            Problem problem = input.getProblemDesFromFile();
//
//            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
//                new MOEvaluator.MaximumCompletionTime(),
//                new MOEvaluator.TotalEnergyConsumption(problem)
//            );
//
//            MOGWO mogwo = new MOGWO(problem, objectives);
//            mogwo.setPopulationSize(50);
//            mogwo.setArchiveSize(50);
//            mogwo.setMaxGenerations(100);
//            mogwo.setSeed(12345L);
//
//            long startTime = System.currentTimeMillis();
//            List<MOIndividual> paretoFront = mogwo.solve();
//            long endTime = System.currentTimeMillis();
//
//            double runTime = (endTime - startTime) / 1000.0;
//
//            System.out.println("\n结果统计:");
//            System.out.println("  - Pareto前沿大小: " + paretoFront.size());
//            System.out.println("  - 运行时间: " + String.format("%.2f", runTime) + " 秒");
//
//            // 找出极端解
//            double minCmax = Double.MAX_VALUE;
//            double minEnergy = Double.MAX_VALUE;
//
//            for (MOIndividual ind : paretoFront) {
//                minCmax = Math.min(minCmax, ind.objectives[0]);
//                minEnergy = Math.min(minEnergy, ind.objectives[1]);
//            }
//
//            System.out.println("  - 最小Cmax: " + String.format("%.2f", minCmax));
//            System.out.println("  - 最小Energy: " + String.format("%.2f", minEnergy));
//
//            System.out.println("\n✅ 小规模算例测试通过");
//
//        } catch (Exception e) {
//            System.out.println("✗ 测试失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 测试3: 与NSGA-II快速对比
//     */
//    private static void quickComparison() {
//        try {
//            // 查找测试文件
//            String problemFile = null;
//            String[] candidates = {
//                "src/main/resources/test_instance_medium_3m_10j.txt",
//                "src/main/resources/test_instance_small_2m_5j.txt"
//            };
//
//            for (String file : candidates) {
//                if (new File(file).exists()) {
//                    problemFile = file;
//                    break;
//                }
//            }
//
//            if (problemFile == null) {
//                System.out.println("⚠️  未找到测试文件，跳过对比测试");
//                return;
//            }
//
//            System.out.println("使用算例: " + problemFile);
//
//            // 加载问题
//            File instanceFile = new File(problemFile);
//            EnergyAwareInput input = new EnergyAwareInput(instanceFile);
//            Problem problem = input.getProblemDesFromFile();
//
//            List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
//                new MOEvaluator.MaximumCompletionTime(),
//                new MOEvaluator.TotalEnergyConsumption(problem)
//            );
//
//            // 统一参数
//            int popSize = 30;
//            int maxGen = 50;
//            long seed = 12345L;
//
//            System.out.println("运行参数: 种群" + popSize + "，代数" + maxGen);
//            System.out.println();
//
//            // 运行MOGWO
//            System.out.println("运行 MOGWO...");
//            MOGWO mogwo = new MOGWO(problem, objectives);
//            mogwo.setPopulationSize(popSize);
//            mogwo.setArchiveSize(popSize);
//            mogwo.setMaxGenerations(maxGen);
//            mogwo.setSeed(seed);
//
//            long startTime = System.currentTimeMillis();
//            List<MOIndividual> mogwoPF = mogwo.solve();
//            long mogwoTime = System.currentTimeMillis() - startTime;
//
//            // 运行NSGA-II
//            System.out.println("\n运行 NSGA-II...");
//            AlgorithmFrame.nsgaii.NSGAII nsgaii = new AlgorithmFrame.nsgaii.NSGAII(problem, objectives);
//            nsgaii.setPopulationSize(popSize);
//            nsgaii.setMaxGenerations(maxGen);
//            nsgaii.setSeed(seed);
//
//            startTime = System.currentTimeMillis();
//            List<MOIndividual> nsgaiiPF = nsgaii.solve();
//            long nsgaiiTime = System.currentTimeMillis() - startTime;
//
//            // 对比结果
//            System.out.println("\n" + "-".repeat(62));
//            System.out.println("对比结果:");
//            System.out.println("-".repeat(62));
//            System.out.printf("%-15s | %12s | %15s\n", "算法", "PF大小", "运行时间(s)");
//            System.out.println("-".repeat(62));
//            System.out.printf("%-15s | %12d | %15.2f\n", "MOGWO", mogwoPF.size(), mogwoTime / 1000.0);
//            System.out.printf("%-15s | %12d | %15.2f\n", "NSGA-II", nsgaiiPF.size(), nsgaiiTime / 1000.0);
//            System.out.println("-".repeat(62));
//
//            System.out.println("\n✅ 快速对比测试完成");
//
//        } catch (Exception e) {
//            System.out.println("✗ 测试失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
