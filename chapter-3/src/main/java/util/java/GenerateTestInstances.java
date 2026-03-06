//package util.java;
//
//import java.io.File;
//
///**
// * 批量生成测试算例
// *
// * 生成多个不同规模的算例，存放在 resources 目录下
// */
//public class GenerateTestInstances {
//
//    public static void main(String[] args) {
//        System.out.println("╔═══════════════════════════════════════════════════════════╗");
//        System.out.println("║           批量生成测试算例（含能耗参数）                  ║");
//        System.out.println("╚═══════════════════════════════════════════════════════════╝\n");
//
//        try {
//            // 创建输出目录
//            String baseDir = "chapter-3/src/main/resources/instance";
//            createDirectories(baseDir);
//
//            // 生成不同规模的算例
//            EnergyAwareInstanceGenerator generator =
//                new EnergyAwareInstanceGenerator(42, true);
//
//            // 小规模测试算例（快速验证）
//            System.out.println("【1】生成小规模测试算例");
//            generateSmallInstances(generator, baseDir);
//
//            // 中等规模算例（标准实验）
//            System.out.println("\n【2】生成中等规模算例");
//            generateMediumInstances(generator, baseDir);
//
//            // 大规模算例（性能测试）
//            System.out.println("\n【3】生成大规模算例");
//            generateLargeInstances(generator, baseDir);
//
//            System.out.println("\n╔═══════════════════════════════════════════════════════════╗");
//            System.out.println("║                  算例生成完成！                           ║");
//            System.out.println("╚═══════════════════════════════════════════════════════════╝");
//
//            System.out.println("\n生成的算例：");
//            System.out.println("  • 小规模: " + baseDir + "/small/");
//            System.out.println("  • 中等规模: " + baseDir + "/medium/");
//            System.out.println("  • 大规模: " + baseDir + "/large/");
//
//            System.out.println("\n下一步：运行 MainEntry.java 进行多目标优化");
//
//        } catch (Exception e) {
//            System.err.println("❌ 生成算例失败: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * 生成小规模测试算例
//     */
//    private static void generateSmallInstances(EnergyAwareInstanceGenerator generator,
//                                               String baseDir) throws Exception {
//        String outputDir = baseDir + "/small";
//
//        // J10P2B1D3: 10工件, 2打印机, 1批处理机, 3离散机
//        System.out.println("  生成 J10P2B1D3 算例...");
//        for (int i = 1; i <= 3; i++) {
//            EnergyAwareInstanceGenerator gen =
//                new EnergyAwareInstanceGenerator(42 + i * 1000);
//            String filename = String.format("%s/J10P2B1D3_%02d.txt", outputDir, i);
//            gen.generateInstance(10, 2, 1, 3, filename);
//        }
//        System.out.println("  ✓ 生成 3 个小规模算例");
//    }
//
//    /**
//     * 生成中等规模算例
//     */
//    private static void generateMediumInstances(EnergyAwareInstanceGenerator generator,
//                                                String baseDir) throws Exception {
//        String outputDir = baseDir + "/medium";
//
//        // J20P3B2D5: 20工件, 3打印机, 2批处理机, 5离散机
//        System.out.println("  生成 J20P3B2D5 算例...");
//        for (int i = 1; i <= 5; i++) {
//            EnergyAwareInstanceGenerator gen =
//                new EnergyAwareInstanceGenerator(42 + i * 1000, true);
//            String filename = String.format("%s/J20P3B2D5_%02d.txt", outputDir, i);
//            gen.generateInstance(20, 3, 2, 5, filename);
//        }
//        System.out.println("  ✓ 生成 5 个中等规模算例");
//    }
//
//    /**
//     * 生成大规模算例
//     */
//    private static void generateLargeInstances(EnergyAwareInstanceGenerator generator,
//                                               String baseDir) throws Exception {
//        String outputDir = baseDir + "/large";
//
//        // J30P4B2D8: 30工件, 4打印机, 2批处理机, 8离散机
//        System.out.println("  生成 J30P4B2D8 算例...");
//        for (int i = 1; i <= 3; i++) {
//            EnergyAwareInstanceGenerator gen =
//                new EnergyAwareInstanceGenerator(42 + i * 1000, true);
//            String filename = String.format("%s/J30P4B2D8_%02d.txt", outputDir, i);
//            gen.generateInstance(30, 4, 2, 8, filename);
//        }
//        System.out.println("  ✓ 生成 3 个大规模算例");
//    }
//
//    /**
//     * 创建目录结构
//     */
//    private static void createDirectories(String baseDir) {
//        new File(baseDir + "/small").mkdirs();
//        new File(baseDir + "/medium").mkdirs();
//        new File(baseDir + "/large").mkdirs();
//        System.out.println("✓ 创建目录结构完成\n");
//    }
//}


