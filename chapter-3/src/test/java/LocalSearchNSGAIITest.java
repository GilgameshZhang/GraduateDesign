import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOEvaluator;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import java.io.File;
import java.util.Arrays;

/**
 * 集成局部搜索的NSGA-II测试
 * 
 * 测试NSGA-II + Memetic Local Search的功能
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class LocalSearchNSGAIITest {
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("  NSGA-II + 局部搜索集成测试");
        System.out.println("========================================\n");
        
        // 1. 读取算例
        String instancePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-2\\src\\main\\resources\\instance\\J100\\J100P4B2D10_05.txt";
        System.out.println("读取算例: " + instancePath);
        
        Problem problem = null;
        try {
            // 使用Input类读取算例（第三章的标准方式）
            problem = new Input(new File(instancePath)).getProblemDesFromFile();
            System.out.println("算例加载成功");
            System.out.println("工件数: " + problem.getJobCount());
            System.out.println("机器数: " + problem.getMachineCount());
        } catch (Exception e) {
            System.err.println("读取算例失败: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
        System.out.println("\n========================================");
        System.out.println("测试1: 不启用局部搜索");
        System.out.println("========================================");

        // 2. 测试不启用局部搜索
//        NSGAII nsgaii1 = new NSGAII(problem, Arrays.asList(
//            new MOEvaluator.MaximumCompletionTime(),
//            new MOEvaluator.TotalEnergyConsumption(problem)
//        ));
//
//        nsgaii1.setPopulationSize(50);
//        nsgaii1.setMaxGenerations(50);
//        nsgaii1.setMaxRunTimeMinutes(2.0);
//        nsgaii1.setSeed(12345);
//
//        long start1 = System.currentTimeMillis();
//        nsgaii1.solve();
//        long time1 = System.currentTimeMillis() - start1;
//
//        System.out.println("不含局部搜索耗时: " + (time1 / 1000.0) + " 秒");
//        System.out.println("Pareto前沿大小: " + nsgaii1.getParetoFront().size());
//
        System.out.println("\n========================================");
        System.out.println("测试2: 启用局部搜索");
        System.out.println("========================================");
        
        // 3. 测试启用局部搜索
        NSGAII nsgaii2 = new NSGAII(problem, Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        ));
        
        nsgaii2.setPopulationSize(50);
        nsgaii2.setMaxGenerations(50);
        nsgaii2.setMaxRunTimeMinutes(2.0);
        nsgaii2.setSeed(12345);
        
        // 启用局部搜索（每5代执行一次）
        nsgaii2.enableLocalSearch(true, 1);
        
        // 配置局部搜索参数
        nsgaii2.setLocalSearchParameters(
            20,      // L: 每个精英10次尝试
            0.20,    // eta: 选择10%精英
            1,    // epsE: T型允许能耗上升5%
            1,    // epsC: E型允许Cmax上升2%
            0,   // improvC: T型要求Cmax下降0.5%
            0     // improvE: E型要求能耗下降1%
        );
        
        long start2 = System.currentTimeMillis();
        nsgaii2.solve();
        long time2 = System.currentTimeMillis() - start2;
        
        System.out.println("含局部搜索耗时: " + (time2 / 1000.0) + " 秒");
        System.out.println("Pareto前沿大小: " + nsgaii2.getParetoFront().size());
        
        System.out.println("\n========================================");
        System.out.println("对比结果");
        System.out.println("========================================");
//        System.out.println("不含局部搜索:");
//        System.out.println("  - Pareto前沿大小: " + nsgaii1.getParetoFront().size());
//        System.out.println("  - 耗时: " + (time1 / 1000.0) + " 秒");

        System.out.println("\n含局部搜索:");
        System.out.println("  - Pareto前沿大小: " + nsgaii2.getParetoFront().size());
        System.out.println("  - 耗时: " + (time2 / 1000.0) + " 秒");
        //System.out.println("  - 额外耗时: " + ((time2 - time1) / 1000.0) + " 秒");
        
        System.out.println("\n测试完成！");
    }
}
