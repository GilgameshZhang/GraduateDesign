import AlgorithmFrame.alns.*;
import AlgorithmFrame.machineChoice.ga.BatchGa;
import ProblemFrame.Input;
import ProblemFrame.Result;
import util.Chapter1Visualizer;
import util.ReadDataUtil;

/**
 * 可视化功能测试类
 * 用于快速测试和验证可视化功能
 */
public class VisualizationTest {
    
    /**
     * 测试ALNS算法可视化
     */
    public static void testALNSVisualization() {
        System.out.println("========================================");
        System.out.println("测试ALNS算法可视化功能");
        System.out.println("========================================\n");
        
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        String outputDir = "chapter-1/src/main/output/test/alns_visualization";
        
        ALNSParameters params = new ALNSParameters();
        params.maxIterations = 10000;  // 设置很大，实际由时间控制
        params.timeLimitMs = 1 * 60 * 1000;  // 1分钟（测试用）
        
        System.out.println("运行ALNS算法（时间限制：1分钟用于快速测试）...");
        ALNSSolution solution = ALNSRunner.runALNSWithVisualization(
            machinePath, itemPath, params, 12345L, outputDir
        );
        
        if (solution != null) {
            System.out.println("\n✓ ALNS算法运行成功");
            System.out.println("✓ 可视化图表已生成到: " + outputDir);
            System.out.println("✓ 最优Cmax: " + solution.cmax);
        } else {
            System.err.println("✗ ALNS算法运行失败");
        }
    }
    
    /**
     * 测试GA-TS算法可视化
     */
    public static void testGATSVisualization() {
        System.out.println("\n========================================");
        System.out.println("测试GA-TS算法可视化功能");
        System.out.println("========================================\n");
        
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        String outputDir = "chapter-1/src/main/output/test/gats_visualization";
        
        try {
            System.out.println("读取数据...");
            Input input = ReadDataUtil.readData(machinePath, itemPath, true);
            
            System.out.println("运行GA-TS算法（时间限制：1分钟用于快速测试）...");
            long timeLimitMs = 1 * 60 * 1000;  // 1分钟（测试用）
            
            BatchGa batchGa = new BatchGa(
                10000,  // MAX_GEN (设置很大，实际由时间控制)
                50,     // popSize (减少以加快测试)
                5,      // variationExchangeCount
                2,      // cloneNumOfBestIndividual
                0.2,    // mutationRate
                0.8,    // crossoverRate
                input,
                true,   // isRotateEnable
                "TabuSearch",
                100,    // decodeMaxGen
                10,     // decodeTabuSize
                30,     // decodeMaxN
                timeLimitMs  // 时间限制：1分钟
            );
            
            Result result = batchGa.solve();
            
            System.out.println("\n生成可视化图表...");
            Chapter1Visualizer.generateAllCharts(
                result.getSolutionList(),
                input.machineList.toArray(new ProblemFrame.Machine[0]),
                result.getIreatorList(),
                "GA-TS算法测试",
                outputDir
            );
            
            System.out.println("\n✓ GA-TS算法运行成功");
            System.out.println("✓ 可视化图表已生成到: " + outputDir);
            System.out.println("✓ 最优Cmax: " + batchGa.bestGenome.cMax);
            
        } catch (Exception e) {
            System.err.println("✗ GA-TS算法运行失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 测试单独生成迭代曲线图
     */
    public static void testIterationCurve() {
        System.out.println("\n========================================");
        System.out.println("测试迭代曲线图生成");
        System.out.println("========================================\n");
        
        // 模拟迭代历史数据
        java.util.List<Double> iterationHistory = new java.util.ArrayList<>();
        double cmax = 1000.0;
        for (int i = 0; i < 100; i++) {
            cmax = cmax * 0.99 + Math.random() * 5;  // 模拟收敛过程
            iterationHistory.add(cmax);
        }
        
        String outputPath = "chapter-1/src/main/output/test/iteration_curve_test.png";
        
        try {
            Chapter1Visualizer.generateIterationCurve(
                iterationHistory,
                outputPath,
                "迭代曲线测试"
            );
            
            System.out.println("✓ 迭代曲线图已生成: " + outputPath);
            
        } catch (Exception e) {
            System.err.println("✗ 生成迭代曲线图失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 快速测试（只测试迭代曲线）
     */
    public static void quickTest() {
        System.out.println("========================================");
        System.out.println("快速可视化测试");
        System.out.println("========================================\n");
        
        testIterationCurve();
        
        System.out.println("\n========================================");
        System.out.println("快速测试完成！");
        System.out.println("如需完整测试，请运行 fullTest()");
        System.out.println("========================================");
    }
    
    /**
     * 完整测试（测试所有可视化功能）
     */
    public static void fullTest() {
        System.out.println("========================================");
        System.out.println("完整可视化测试");
        System.out.println("========================================\n");
        
        // 测试迭代曲线
        testIterationCurve();
        
        // 测试ALNS可视化
        testALNSVisualization();
        
        // 测试GA-TS可视化
        testGATSVisualization();
        
        System.out.println("\n========================================");
        System.out.println("完整测试完成！");
        System.out.println("请检查输出目录：");
        System.out.println("  - chapter-1/src/main/output/test/");
        System.out.println("========================================");
    }
    
    /**
     * 主函数
     */
    public static void main(String[] args) {
        // 根据需要选择：
        
        // 1. 快速测试（只测试迭代曲线，不运行算法）
        quickTest();
        
        // 2. 完整测试（测试所有可视化功能，需要较长时间）
        // fullTest();
        
        // 3. 单独测试
        // testIterationCurve();
        // testALNSVisualization();
        // testGATSVisualization();
    }
}
