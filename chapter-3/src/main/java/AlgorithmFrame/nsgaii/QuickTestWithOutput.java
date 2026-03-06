//package AlgorithmFrame.nsgaii;
//
//import ProblemFrame.MOIndividual;
//import ProgramEntity.EnergyAwareInput;
//import ProgramEntity.Problem;
//
//import java.io.File;
//import java.util.List;
//
///**
// * 快速测试NSGA-II输出功能
// *
// * 使用小规模算例快速验证所有输出功能是否正常工作
// *
// * @author AI Assistant
// * @version 1.0
// */
//public class QuickTestWithOutput {
//
//    public static void main(String[] args) {
//        try {
//            System.out.println("╔═══════════════════════════════════════════════════════════════╗");
//            System.out.println("║         NSGA-II 输出功能快速测试                              ║");
//            System.out.println("╚═══════════════════════════════════════════════════════════════╝\n");
//
//            // 1. 加载算例（使用小规模算例以加快测试）
//            String instancePath = "instances/Mk01.fjs";  // 修改为你的算例路径
//            System.out.println("正在加载算例: " + instancePath);
//
//            Problem problem = new Problem();
//            EnergyAwareInput input = new EnergyAwareInput(new File(instancePath));
//            input.read(problem);
//
//            System.out.println("✓ 算例加载成功");
//            System.out.println("  工件数: " + problem.getJobCount());
//            System.out.println("  机器数: " + problem.getMachineCount());
//            System.out.println();
//
//            // 2. 配置NSGA-II（使用较小的参数以加快测试）
//            System.out.println("正在配置NSGA-II...");
//            int popSize = 20;           // 小种群
//            int maxGen = 50;            // 少代数
//            double crossoverRate = 0.9;
//            double mutationRate = 0.1;
//
//            System.out.println("  种群规模: " + popSize);
//            System.out.println("  最大代数: " + maxGen);
//            System.out.println();
//
//            // 3. 运行NSGA-II
//            System.out.println("正在运行NSGA-II...");
//            NSGAII nsgaii = new NSGAII(problem, popSize, maxGen, crossoverRate, mutationRate, 10.0);
//            List<MOIndividual> paretoFront = nsgaii.run();
//
//            System.out.println("✓ NSGA-II运行完成");
//            System.out.println("  Pareto前沿解数: " + paretoFront.size());
//            System.out.println();
//
//            // 4. 测试输出功能
//            System.out.println("正在测试输出功能...");
//            System.out.println("═══════════════════════════════════════════════════════════════\n");
//
//            // 创建输出目录
//            String outputDir = "chapter-3/output/quick_test";
//
//            // 使用NSGAIIResultExporter导出所有结果
//            ProgramEntity.Operation[][] operationMatrix = createOperationMatrix(problem);
//            NSGAIIResultExporter exporter = new NSGAIIResultExporter(problem, operationMatrix);
//            exporter.exportAllResults(paretoFront, outputDir);
//
//            // 5. 完成
//            System.out.println("\n╔═══════════════════════════════════════════════════════════════╗");
//            System.out.println("║         测试完成！                                            ║");
//            System.out.println("╚═══════════════════════════════════════════════════════════════╝");
//            System.out.println("\n请检查输出目录: " + outputDir);
//            System.out.println("\n输出文件包括:");
//            System.out.println("  1. pareto_front.png - Pareto前沿图");
//            System.out.println("  2. pareto_front_data.txt - Pareto前沿数据");
//            System.out.println("  3. best_cmax_gantt.png - 甘特图");
//            System.out.println("  4. best_cmax_printer_layout.png - 打印批次排布图");
//            System.out.println("  5. best_cmax_schedule_records.txt - 详细调度记录");
//
//        } catch (Exception e) {
//            System.err.println("\n❌ 测试失败:");
//            e.printStackTrace();
//
//            System.err.println("\n可能的原因:");
//            System.err.println("  1. 算例文件路径不正确");
//            System.err.println("  2. 缺少JFreeChart依赖");
//            System.err.println("  3. 输出目录无写入权限");
//        }
//    }
//
//    /**
//     * 创建工序矩阵
//     */
//    private static ProgramEntity.Operation[][] createOperationMatrix(Problem problem) {
//        int jobCount = problem.getJobCount();
//        int[] operCountArr = problem.getOperationCountArr();
//
//        ProgramEntity.Operation[][] operationMatrix = new ProgramEntity.Operation[jobCount][];
//        for (int i = 0; i < jobCount; i++) {
//            operationMatrix[i] = new ProgramEntity.Operation[operCountArr[i]];
//            for (int j = 0; j < operCountArr[i]; j++) {
//                operationMatrix[i][j] = new ProgramEntity.Operation();
//            }
//        }
//
//        return operationMatrix;
//    }
//}
//
