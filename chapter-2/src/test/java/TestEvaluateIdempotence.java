//package test;
//
//import AlgorthmFrame.ga.GA;
//import ProblemFrame.CaculateFitness;
//import ProblemFrame.Chromosome;
//import ProgramEntity.Operation;
//import ProgramEntity.Problem;
//
//import java.util.Random;
//
///**
// * 测试evaluate方法的幂等性
// * 验证：相同的染色体多次evaluate应该得到相同的makespan
// */
//public class TestEvaluateIdempotence {
//
//    public static void main(String[] args) {
//        System.out.println("========================================");
//        System.out.println("      evaluate幂等性测试");
//        System.out.println("========================================");
//        System.out.println();
//
//        // 测试多个算例
//        String[] instanceFiles = {
//            "src/main/resources/instance/J20P3B2D5_01.txt",
//
//           //        };
//
//        int passCount = 0;
//        int failCount = 0;
//
//        for (String instanceFile : instanceFiles) {
//            System.out.println("====================================");
//            System.out.println("测试算例: " + instanceFile);
//            System.out.println("====================================");
//
//            try {
//                if (testInstance(instanceFile)) {
//                    passCount++;
//                    System.out.println("✓ 测试通过");
//                } else {
//                    failCount++;
//                    System.out.println("✗ 测试失败");
//                }
//            } catch (Exception e) {
//                failCount++;
//                System.out.println("✗ 测试出错: " + e.getMessage());
//                e.printStackTrace();
//            }
//            System.out.println();
//        }
//
//        System.out.println("========================================");
//        System.out.println("      测试总结");
//        System.out.println("========================================");
//        System.out.println("通过: " + passCount);
//        System.out.println("失败: " + failCount);
//        System.out.println("总计: " + (passCount + failCount));
//        System.out.println();
//
//        if (failCount == 0) {
//            System.out.println("🎉 所有测试通过！evaluate是幂等的。");
//        } else {
//            System.out.println("⚠️ 有测试失败！evaluate可能不是幂等的。");
//        }
//    }
//
//    /**
//     * 测试单个算例
//     */
//    private static boolean testInstance(String instanceFile) throws Exception {
//        // 1. 读取问题实例
//        //Problem problem = new Problem(instanceFile);
//
//        // 2. 创建CaculateFitness实例
//        CaculateFitness caculateFitness = new CaculateFitness();
//
//        // 3. 创建随机染色体（使用固定种子确保可重复）
//        Random random = new Random(12345);
//        //Chromosome chromosome = new Chromosome(problem.getJobs(), random, problem);
//
//        System.out.println("工件数: " + problem.getJobCount());
//        System.out.println("染色体长度: " + chromosome.gene_OS.length);
//
//        // 4. 多次evaluate并记录结果
//        int evaluateCount = 5;
//        double[] makespans = new double[evaluateCount];
//
//        System.out.println("\n开始测试evaluate幂等性（共" + evaluateCount + "次）...");
//
//        for (int i = 0; i < evaluateCount; i++) {
//            // 创建独立的operationMatrix
//            Operation[][] operationMatrix = createOperationMatrix(problem);
//
//            // 保存染色体基因的副本（确保evaluate不会修改）
//            int[] originalOS = chromosome.gene_OS.clone();
//            int[] originalMS = chromosome.gene_MS.clone();
//
//            // 执行evaluate
//            makespans[i] = caculateFitness.evaluate(chromosome, problem, operationMatrix);
//
//            // 验证染色体基因没有被修改
//            boolean osModified = false;
//            boolean msModified = false;
//            for (int j = 0; j < originalOS.length; j++) {
//                if (originalOS[j] != chromosome.gene_OS[j]) {
//                    osModified = true;
//                    break;
//                }
//                if (originalMS[j] != chromosome.gene_MS[j]) {
//                    msModified = true;
//                    break;
//                }
//            }
//
//            if (osModified || msModified) {
//                System.out.println("⚠️ 警告：第" + (i+1) + "次evaluate修改了染色体基因！");
//                System.out.println("   gene_OS被修改: " + osModified);
//                System.out.println("   gene_MS被修改: " + msModified);
//            }
//
//            System.out.printf("  第%d次: makespan = %.2f%n", i+1, makespans[i]);
//        }
//
//        // 5. 检查所有结果是否相同
//        System.out.println("\n结果分析:");
//        boolean allSame = true;
//        double firstMakespan = makespans[0];
//        double maxDiff = 0.0;
//
//        for (int i = 1; i < evaluateCount; i++) {
//            double diff = Math.abs(makespans[i] - firstMakespan);
//            if (diff > 0.01) {  // 允许微小的浮点误差
//                allSame = false;
//                System.out.printf("  第%d次与第1次差异: %.2f (%.4f%%)%n",
//                    i+1, diff, (diff / firstMakespan) * 100);
//            }
//            maxDiff = Math.max(maxDiff, diff);
//        }
//
//        if (allSame) {
//            System.out.println("  ✓ 所有evaluate结果完全一致");
//            System.out.printf("  一致的makespan: %.2f%n", firstMakespan);
//        } else {
//            System.out.println("  ✗ evaluate结果不一致！");
//            System.out.printf("  最大差异: %.2f (%.4f%%)%n", maxDiff, (maxDiff / firstMakespan) * 100);
//            System.out.println("\n详细结果:");
//            for (int i = 0; i < evaluateCount; i++) {
//                System.out.printf("    第%d次: %.2f%n", i+1, makespans[i]);
//            }
//        }
//
//        return allSame;
//    }
//
//    /**
//     * 创建operationMatrix
//     */
//    private static Operation[][] createOperationMatrix(Problem problem) {
//        Operation[][] matrix = new Operation[problem.getJobCount()][];
//        for (int i = 0; i < matrix.length; i++) {
//            matrix[i] = new Operation[problem.getOperationCountArr()[i]];
//            for (int j = 0; j < matrix[i].length; j++) {
//                matrix[i][j] = new Operation();
//            }
//        }
//        return matrix;
//    }
//}
//

