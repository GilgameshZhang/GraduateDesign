//package util.java;
//
//import ProgramEntity.EnergyAwareInput;
//import ProgramEntity.Problem;
//import ProblemFrame.EnergyCalculator;
//import ProblemFrame.PowerParameters;
//
//import java.io.File;
//
///**
// * 第三章算例工具综合示例
// *
// * 演示:
// * 1. 生成含能耗参数的算例
// * 2. 读取算例
// * 3. 显示能耗参数
// * 4. 模拟能耗计算
// *
// * @author AI Assistant
// * @version 1.0
// */
//public class Chapter3InstanceExample {
//
//    /**
//     * 示例1: 生成和读取算例
//     */
//    public static void example1_GenerateAndRead() {
//        System.out.println("\n========== 示例1: 生成和读取算例 ==========\n");
//
//        // 1. 生成测试算例
//        String instancePath = "chapter-3/src/main/resources/instance/example_J10P2B1D0_01.txt";
//
//        System.out.println("步骤1: 生成算例");
//        EnergyAwareInstanceGenerator generator = new EnergyAwareInstanceGenerator(12345);
//        generator.generateInstance(
//            10,                                    // 10个工件
//            2,                                     // 2台打印机
//            1,                                     // 1台批处理机
//            0,                                     // 0台离散机
//            instancePath,
//            EnergyAwareInstanceGenerator.EnergyProfile.STANDARD,
//            EnergyAwareInstanceGenerator.EnergyProfile.STANDARD
//        );
//
//        // 2. 读取算例
//        System.out.println("\n步骤2: 读取算例");
//        File instanceFile = new File(instancePath);
//        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
//        Problem problem = input.getProblemDesFromFile();
//
//        // 3. 显示基本信息
//        System.out.println("\n步骤3: 显示算例信息");
//        System.out.println("  工件数: " + problem.getJobCount());
//        System.out.println("  机器数: " + problem.getMachineCount());
//        System.out.println("  打印机数: " + problem.getPrintMachineCount());
//        System.out.println("  批处理机数: " + problem.getBatchMachineCount());
//        System.out.println("  包含能耗参数: " + input.hasEnergyParams());
//
//        // 4. 显示能耗参数
//        if (input.hasEnergyParams()) {
//            input.printEnergyParamsSummary();
//        }
//    }
//
//    /**
//     * 示例2: 不同能耗配置对比
//     */
//    public static void example2_CompareEnergyProfiles() {
//        System.out.println("\n========== 示例2: 不同能耗配置对比 ==========\n");
//
//        String outputDir = "chapter-3/src/main/resources/instance/comparison/";
//        new File(outputDir).mkdirs();
//
//        // 固定问题规模，使用不同能耗配置
//        EnergyAwareInstanceGenerator.EnergyProfile[] profiles = {
//            EnergyAwareInstanceGenerator.EnergyProfile.ECO,
//            EnergyAwareInstanceGenerator.EnergyProfile.STANDARD,
//            EnergyAwareInstanceGenerator.EnergyProfile.HEAVY_DUTY,
//            EnergyAwareInstanceGenerator.EnergyProfile.TEMPERATURE_SENSITIVE
//        };
//
//        String[] profileNames = {"ECO", "STANDARD", "HEAVY", "TEMP"};
//
//        for (int i = 0; i < profiles.length; i++) {
//            String filename = outputDir + "J10P2B1D0_" + profileNames[i] + ".txt";
//
//            System.out.println("生成 " + profileNames[i] + " 配置算例...");
//
//            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(99999);
//            gen.generateInstance(10, 2, 1, 0, filename, profiles[i], profiles[i]);
//
//            // 读取并显示关键参数
//            EnergyAwareInput input = new EnergyAwareInput(new File(filename));
//            Problem problem = input.getProblemDesFromFile();
//
//            PowerParameters p0 = input.getMachineEnergyParams(0);
//            if (p0 != null) {
//                System.out.println(String.format("  机器0: P_run=%.2f, P_idle=%.2f, E_switch=%.2f, T_be=%.2f小时",
//                    p0.P_run, p0.P_idle, p0.E_switch, p0.getBreakEvenTime()));
//            }
//            System.out.println();
//        }
//    }
//
//    /**
//     * 示例3: 模拟能耗计算
//     */
//    public static void example3_SimulateEnergyCalculation() {
//        System.out.println("\n========== 示例3: 模拟能耗计算 ==========\n");
//
//        // 生成测试算例
//        String instancePath = "chapter-3/src/main/resources/instance/energy_test_J5P2B1D0_01.txt";
//
//        EnergyAwareInstanceGenerator generator = new EnergyAwareInstanceGenerator(55555);
//        generator.generateInstance(5, 2, 1, 0, instancePath,
//            EnergyAwareInstanceGenerator.EnergyProfile.STANDARD,
//            EnergyAwareInstanceGenerator.EnergyProfile.STANDARD);
//
//        // 读取算例
//        EnergyAwareInput input = new EnergyAwareInput(new File(instancePath));
//        Problem problem = input.getProblemDesFromFile();
//
//        System.out.println("算例: " + problem.getJobCount() + "工件, " +
//                          problem.getMachineCount() + "机器");
//
//        // 创建能耗计算器 (启用调试输出)
//        EnergyCalculator energyCalc = new EnergyCalculator(problem, true, true);
//
//        // 创建一个简单的模拟调度方案
//        // 注意: 这里只是示例，实际应该从调度算法获取
//        System.out.println("\n创建模拟调度方案...");
//        ProgramEntity.Operation[][] schedule = createMockSchedule(problem);
//
//        // 计算能耗
//        System.out.println("\n计算能耗...");
//        double totalEnergy = energyCalc.calculateTotalEnergy(schedule, problem);
//
//        // 获取详细统计
//        EnergyCalculator.EnergyStatistics stats = energyCalc.getStatistics(schedule, problem);
//
//        System.out.println("\n========== 能耗统计结果 ==========");
//        System.out.println("总能耗: " + String.format("%.2f kWh", stats.totalEnergy));
//        System.out.println("运行能耗: " + String.format("%.2f kWh (%.1f%%)",
//            stats.runEnergy, 100.0 * stats.runEnergy / stats.totalEnergy));
//        System.out.println("空闲能耗: " + String.format("%.2f kWh (%.1f%%)",
//            stats.idleEnergy, 100.0 * stats.idleEnergy / stats.totalEnergy));
//        System.out.println("开关机次数: " + stats.shutdownCount);
//        System.out.println("待机次数: " + stats.idleCount);
//        System.out.println("==================================");
//    }
//
//    /**
//     * 创建模拟调度方案 (仅用于演示)
//     */
//    private static ProgramEntity.Operation[][] createMockSchedule(Problem problem) {
//        int jobCount = problem.getJobCount();
//        int maxOps = problem.getMaxOperationCount();
//
//        ProgramEntity.Operation[][] schedule = new ProgramEntity.Operation[jobCount][maxOps];
//
//        double currentTime = 0.0;
//
//        for (int i = 0; i < jobCount; i++) {
//            int opsCount = problem.getOperationCountArr()[i];
//
//            for (int j = 0; j < opsCount; j++) {
//                ProgramEntity.Operation op = new ProgramEntity.Operation();
//                op.jobNo = i;
//                op.task = j;
//                op.machineNo = j % problem.getMachineCount();  // 简单轮询分配机器
//                op.startTime = currentTime;
//                op.endTime = currentTime + 1000;  // 假设每个工序1000秒
//
//                schedule[i][j] = op;
//                currentTime += 1200;  // 留200秒空闲
//            }
//        }
//
//        return schedule;
//    }
//
//    /**
//     * 示例4: 向后兼容性测试
//     */
//    public static void example4_BackwardCompatibility() {
//        System.out.println("\n========== 示例4: 向后兼容性测试 ==========\n");
//
//        System.out.println("测试读取第二章格式算例（不含能耗参数）...\n");
//
//        // 假设存在第二章的算例
//        File chapter2Instance = new File("chapter-2/src/main/resources/instance/J20/J20P3B2D5_01.txt");
//
//        if (chapter2Instance.exists()) {
//            EnergyAwareInput input = new EnergyAwareInput(chapter2Instance);
//            Problem problem = input.getProblemDesFromFile();
//
//            System.out.println("✓ 成功读取第二章算例");
//            System.out.println("  工件数: " + problem.getJobCount());
//            System.out.println("  机器数: " + problem.getMachineCount());
//            System.out.println("  包含能耗参数: " + input.hasEnergyParams());
//
//            if (!input.hasEnergyParams()) {
//                System.out.println("  → 自动使用默认能耗参数");
//
//                // 显示默认参数
//                PowerParameters p0 = input.getMachineEnergyParams(0);
//                if (p0 != null) {
//                    System.out.println("\n默认能耗参数示例 (机器0):");
//                    System.out.println("  " + p0.toString());
//                }
//            }
//        } else {
//            System.out.println("⚠ 未找到第二章算例文件，跳过测试");
//            System.out.println("提示: 可以使用 add_energy_params.py 工具转换第二章算例");
//        }
//    }
//
//    /**
//     * 主函数 - 运行所有示例
//     */
//    public static void main(String[] args) {
//        System.out.println("╔═══════════════════════════════════════════════════════════╗");
//        System.out.println("║     第三章算例工具综合示例 - NSGAII多目标优化             ║");
//        System.out.println("╚═══════════════════════════════════════════════════════════╝");
//
//        try {
//            // 运行所有示例
//            example1_GenerateAndRead();
//
//            example2_CompareEnergyProfiles();
//
//            example3_SimulateEnergyCalculation();
//
//            example4_BackwardCompatibility();
//
//            System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
//            System.out.println("║                    所有示例运行完成！                      ║");
//            System.out.println("╚═══════════════════════════════════════════════════════════╝");
//
//            System.out.println("\n后续步骤:");
//            System.out.println("  1. 查看生成的算例文件");
//            System.out.println("  2. 在NSGAII算法中集成EnergyAwareInput和EnergyCalculator");
//            System.out.println("  3. 运行对比实验，验证能耗优化效果");
//            System.out.println("\n相关文档:");
//            System.out.println("  - 算例格式说明.txt");
//            System.out.println("  - 算例工具使用说明.md");
//
//        } catch (Exception e) {
//            System.err.println("\n错误: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
