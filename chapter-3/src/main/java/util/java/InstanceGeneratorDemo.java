//package util.java;
//
//import ProblemFrame.PowerParameters;
//import ProgramEntity.EnergyAwareInput;
//import ProgramEntity.Problem;
//
//import java.io.File;
//
///**
// * 算例生成器快速测试和演示程序
// *
// * 功能：
// * 1. 生成测试算例（含能耗参数）
// * 2. 读取并验证算例
// * 3. 输出能耗参数统计
// *
// * @author AI Assistant
// */
//public class InstanceGeneratorDemo {
//
//    public static void main(String[] args) {
//        System.out.println("╔═══════════════════════════════════════════════════════════╗");
//        System.out.println("║       算例生成器 - 能耗参数版 - 演示程序                 ║");
//        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
//
//        try {
//            // ========== 第1步：生成测试算例 ==========
//            System.out.println("【步骤1】生成测试算例\n");
//
//            String outputDir = "chapter-3/src/main/resources/instance/demo";
//
//            // 创建输出目录
//            File dir = new File(outputDir);
//            if (!dir.exists()) {
//                dir.mkdirs();
//                System.out.println("  ✓ 创建输出目录: " + outputDir);
//            }
//
//            // 生成算例
//            EnergyAwareInstanceGenerator generator =
//                new EnergyAwareInstanceGenerator(42, true);
//
//            String instanceFile = outputDir + "/demo_instance.txt";
//            generator.generateInstance(10, 2, 1, 3, instanceFile);
//
//            System.out.println("  ✓ 算例规模: J10P2B1D3 (10工件, 2打印机, 1批处理机, 3离散机)");
//            System.out.println("  ✓ 包含能耗参数: 是");
//
//            // ========== 第2步：读取算例 ==========
//            System.out.println("\n【步骤2】读取算例并验证\n");
//
//            EnergyAwareInput input = new EnergyAwareInput(new File(instanceFile));
//            Problem problem = input.getProblemDesFromFile();
//
//            System.out.println("  ✓ 算例加载成功");
//            System.out.println("  ✓ 工件数: " + problem.getJobCount());
//            System.out.println("  ✓ 机器数: " + problem.getMachineCount());
//            System.out.println("  ✓ 打印机数: " + problem.getPrintMachineCount());
//            System.out.println("  ✓ 批处理机数: " + problem.getBatchMachineCount());
//            System.out.println("  ✓ 包含能耗参数: " + input.hasEnergyParams());
//
//            // ========== 第3步：能耗参数分析 ==========
//            System.out.println("\n【步骤3】能耗参数分析\n");
//
//            if (input.hasEnergyParams()) {
//                analyzeEnergyParameters(problem, input);
//            }
//
//            // ========== 第4步：打印详细信息 ==========
//            System.out.println("\n【步骤4】详细能耗参数\n");
//            input.printEnergyParamsSummary();
//
//            // ========== 第5步：生成对比算例 ==========
//            System.out.println("【步骤5】生成对比算例（有/无能耗参数）\n");
//
//            // 不含能耗参数的算例
//            EnergyAwareInstanceGenerator generatorNoEnergy =
//                new EnergyAwareInstanceGenerator(42, false);
//            String noEnergyFile = outputDir + "/demo_no_energy.txt";
//            generatorNoEnergy.generateInstance(10, 2, 1, 3, noEnergyFile);
//
//            // 读取并对比
//            EnergyAwareInput inputNoEnergy = new EnergyAwareInput(new File(noEnergyFile));
//            Problem problemNoEnergy = inputNoEnergy.getProblemDesFromFile();
//
//            System.out.println("  对比结果:");
//            System.out.println("  ┌─────────────────────────────────────────────┐");
//            System.out.println("  │ 特性               │ 含能耗   │ 不含能耗    │");
//            System.out.println("  ├─────────────────────────────────────────────┤");
//            System.out.println(String.format("  │ 文件大小           │ %-8s │ %-11s │",
//                getFileSize(new File(instanceFile)),
//                getFileSize(new File(noEnergyFile))));
//            System.out.println(String.format("  │ 包含能耗参数       │ %-8s │ %-11s │",
//                input.hasEnergyParams() ? "是" : "否",
//                inputNoEnergy.hasEnergyParams() ? "是" : "否"));
//            System.out.println(String.format("  │ 参数来源           │ %-8s │ %-11s │",
//                "文件读取", "默认值"));
//            System.out.println("  └─────────────────────────────────────────────┘");
//
//            // ========== 第6步：节能潜力估算 ==========
//            System.out.println("\n【步骤6】节能潜力估算\n");
//            estimateEnergySavingPotential(problem, input);
//
//            // ========== 完成 ==========
//            System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
//            System.out.println("║                   演示完成！                              ║");
//            System.out.println("╚═══════════════════════════════════════════════════════════╝");
//
//            System.out.println("\n生成的文件:");
//            System.out.println("  • " + instanceFile + " (含能耗参数)");
//            System.out.println("  • " + noEnergyFile + " (不含能耗参数)");
//
//            System.out.println("\n下一步:");
//            System.out.println("  1. 查看生成的算例文件");
//            System.out.println("  2. 运行 EnergyNSGAIIExample 进行多目标优化");
//            System.out.println("  3. 使用这些算例进行实验");
//
//        } catch (Exception e) {
//            System.err.println("❌ 错误: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 分析能耗参数
//     */
//    private static void analyzeEnergyParameters(Problem problem, EnergyAwareInput input) {
//        System.out.println("  ┌─────────────────────────────────────────────────────────┐");
//        System.out.println("  │ 机器类型     │ 数量 │ P_run范围 │ 盈亏平衡时间        │");
//        System.out.println("  ├─────────────────────────────────────────────────────────┤");
//
//        // 打印机
//        int printerCount = problem.getPrintMachineCount();
//        double[] printerBreakEven = new double[printerCount];
//        for (int i = 0; i < printerCount; i++) {
//            PowerParameters p = input.getMachineEnergyParams(i);
//            if (p != null) {
//                printerBreakEven[i] = p.getBreakEvenTime();
//            }
//        }
//        double avgPrinterBE = average(printerBreakEven);
//        System.out.println(String.format("  │ 打印机       │ %-4d │ 4-8 kW    │ %.2f (平均)       │",
//            printerCount, avgPrinterBE));
//
//        // 批处理机
//        int batchCount = problem.getBatchMachineCount();
//        double[] batchBreakEven = new double[batchCount];
//        for (int i = 0; i < batchCount; i++) {
//            PowerParameters p = input.getMachineEnergyParams(printerCount + i);
//            if (p != null) {
//                batchBreakEven[i] = p.getBreakEvenTime();
//            }
//        }
//        double avgBatchBE = average(batchBreakEven);
//        System.out.println(String.format("  │ 批处理机     │ %-4d │ 5-15 kW   │ %.2f (不允许关机) │",
//            batchCount, avgBatchBE));
//
//        // 离散加工机
//        int discreteCount = problem.getMachineCount() - printerCount - batchCount;
//        double[] discreteBreakEven = new double[discreteCount];
//        for (int i = 0; i < discreteCount; i++) {
//            PowerParameters p = input.getMachineEnergyParams(printerCount + batchCount + i);
//            if (p != null) {
//                discreteBreakEven[i] = p.getBreakEvenTime();
//            }
//        }
//        double avgDiscreteBE = average(discreteBreakEven);
//        System.out.println(String.format("  │ 离散加工机   │ %-4d │ 2-6 kW    │ %.2f (平均)       │",
//            discreteCount, avgDiscreteBE));
//
//        System.out.println("  └─────────────────────────────────────────────────────────┘");
//
//        System.out.println("\n  说明:");
//        System.out.println("    • 盈亏平衡时间 = E_switch / P_idle");
//        System.out.println("    • 关机条件: 空闲时间 >= T_warmup + T_breakEven");
//        System.out.println("    • 打印机和离散机适合长间隔关机");
//        System.out.println("    • 批处理机通常保持运行（保温需求）");
//    }
//
//    /**
//     * 估算节能潜力
//     */
//    private static void estimateEnergySavingPotential(Problem problem, EnergyAwareInput input) {
//        System.out.println("  假设调度场景: 平均空闲间隔 = 15 时间单位\n");
//
//        int printerCount = problem.getPrintMachineCount();
//        int batchCount = problem.getBatchMachineCount();
//        int discreteCount = problem.getMachineCount() - printerCount - batchCount;
//
//        double totalSavings = 0.0;
//        int shutdownOpportunities = 0;
//
//        // 打印机节能
//        for (int i = 0; i < printerCount; i++) {
//            PowerParameters p = input.getMachineEnergyParams(i);
//            if (p != null && p.allowSwitch) {
//                double gap = 15.0;
//                if (p.shouldShutdown(gap)) {
//                    double idleEnergy = p.P_idle * gap;
//                    double switchEnergy = p.E_switch;
//                    double savings = idleEnergy - switchEnergy;
//                    if (savings > 0) {
//                        totalSavings += savings;
//                        shutdownOpportunities++;
//                    }
//                }
//            }
//        }
//
//        // 离散加工机节能
//        for (int i = 0; i < discreteCount; i++) {
//            PowerParameters p = input.getMachineEnergyParams(printerCount + batchCount + i);
//            if (p != null && p.allowSwitch) {
//                double gap = 15.0;
//                if (p.shouldShutdown(gap)) {
//                    double idleEnergy = p.P_idle * gap;
//                    double switchEnergy = p.E_switch;
//                    double savings = idleEnergy - switchEnergy;
//                    if (savings > 0) {
//                        totalSavings += savings;
//                        shutdownOpportunities++;
//                    }
//                }
//            }
//        }
//
//        System.out.println("  ┌─────────────────────────────────────────────┐");
//        System.out.println("  │ 指标                     │ 值              │");
//        System.out.println("  ├─────────────────────────────────────────────┤");
//        System.out.println(String.format("  │ 适合关机的机器数         │ %-16d│",
//            shutdownOpportunities));
//        System.out.println(String.format("  │ 单次关机节能（总计）     │ %.2f kWh        │",
//            totalSavings));
//        System.out.println(String.format("  │ 平均单机单次节能         │ %.2f kWh        │",
//            shutdownOpportunities > 0 ? totalSavings / shutdownOpportunities : 0));
//        System.out.println("  └─────────────────────────────────────────────┘");
//
//        System.out.println("\n  结论:");
//        if (shutdownOpportunities > 0) {
//            System.out.println("    ✅ 存在明显的节能机会");
//            System.out.println("    ✅ 建议启用智能开关机策略");
//            double estimatedSavingsPercent = (totalSavings / (totalSavings + 50.0)) * 100;
//            System.out.println(String.format("    ✅ 预计节能比例: %.1f%% (假设基准能耗50kWh)",
//                estimatedSavingsPercent));
//        } else {
//            System.out.println("    ⚠️ 当前参数下节能空间有限");
//            System.out.println("    ⚠️ 可考虑调整能耗参数或空闲间隔");
//        }
//    }
//
//    /**
//     * 获取文件大小
//     */
//    private static String getFileSize(File file) {
//        if (!file.exists()) {
//            return "N/A";
//        }
//        long bytes = file.length();
//        if (bytes < 1024) {
//            return bytes + "B";
//        } else if (bytes < 1024 * 1024) {
//            return String.format("%.1fKB", bytes / 1024.0);
//        } else {
//            return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
//        }
//    }
//
//    /**
//     * 计算平均值
//     */
//    private static double average(double[] values) {
//        if (values == null || values.length == 0) {
//            return 0.0;
//        }
//        double sum = 0.0;
//        for (double v : values) {
//            sum += v;
//        }
//        return sum / values.length;
//    }
//}
//
//
