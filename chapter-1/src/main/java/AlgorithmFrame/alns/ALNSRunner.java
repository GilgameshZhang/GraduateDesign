package AlgorithmFrame.alns;

import AlgorithmFrame.alns.visualization.ALNSVisualizer;
import ProblemFrame.Input;
import util.ReadDataUtil;

/**
 * ALNS算法运行器
 * 用于运行ALNS-SL算法的简单接口
 */
public class ALNSRunner {
    
    /**
     * 运行结果包装类
     */
    public static class ALNSRunResult {
        public ALNSSolution solution;
        public ALNS alns;
        
        public ALNSRunResult(ALNSSolution solution, ALNS alns) {
            this.solution = solution;
            this.alns = alns;
        }
    }
    
    /**
     * 运行ALNS算法（返回包含ALNS实例的结果）
     * @param machinePath 机器数据文件路径
     * @param itemPath 作业数据文件路径
     * @param params ALNS参数
     * @param seed 随机种子
     * @return 运行结果（包含解和ALNS实例）
     */
    public static ALNSRunResult runALNSWithHistory(String machinePath, String itemPath, 
                                                    ALNSParameters params, long seed) {
        try {
            // 读取数据
            ReadDataUtil readDataUtil = new ReadDataUtil();
            String[] pathList = {itemPath, machinePath};
            Input input = readDataUtil.getInput(pathList);
            
            // 转换数据格式
            ALNSAdapter.ALNSData alnsData = ALNSAdapter.convertInput(input);
            
            System.out.println("======== ALNS-SL算法开始 ========");
            System.out.println("作业数量: " + alnsData.jobs.length);
            System.out.println("机器数量: " + alnsData.machines.length);
            System.out.println("参数: T0=" + params.T0 + ", beta=" + params.beta + 
                             ", theta=" + params.theta);
            System.out.println("================================");
            
            // 创建并运行ALNS
            ALNS alns = new ALNS(alnsData.jobs, alnsData.machines, params, seed);
            ALNSSolution solution = alns.solve();
            
            if (solution != null) {
                System.out.println("\n======== ALNS-SL算法完成 ========");
                System.out.println("最优Cmax: " + solution.cmax);
                System.out.println("================================\n");
                
                // 将解转换回原始格式（可选，用于可视化）
                ALNSAdapter.convertSolutionToOriginalFormat(solution, input);
                
                return new ALNSRunResult(solution, alns);
            }
            
            return null;
        } catch (Exception e) {
            System.err.println("ALNS运行出错: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * 运行ALNS算法
     * @param machinePath 机器数据文件路径
     * @param itemPath 作业数据文件路径
     * @param params ALNS参数
     * @param seed 随机种子
     * @return 最优解
     */
    public static ALNSSolution runALNS(String machinePath, String itemPath, 
                                       ALNSParameters params, long seed) {
        ALNSRunResult result = runALNSWithHistory(machinePath, itemPath, params, seed);
        return result != null ? result.solution : null;
    }
    
    /**
     * 使用默认参数运行
     */
    public static ALNSSolution runALNSWithDefaultParams(String machinePath, String itemPath) {
        ALNSParameters params = new ALNSParameters();
        return runALNS(machinePath, itemPath, params, System.currentTimeMillis());
    }
    
    /**
     * 运行ALNS并生成可视化图表
     * @param machinePath 机器数据文件路径
     * @param itemPath 作业数据文件路径
     * @param params ALNS参数
     * @param seed 随机种子
     * @param outputDir 可视化输出目录
     * @return 最优解
     */
    public static ALNSSolution runALNSWithVisualization(String machinePath, String itemPath, 
                                                        ALNSParameters params, long seed, 
                                                        String outputDir) {
        ALNSRunResult result = runALNSWithHistory(machinePath, itemPath, params, seed);
        
        if (result != null && result.solution != null) {
            try {
                System.out.println("\n开始生成可视化图表...");
                ALNSVisualizer visualizer = new ALNSVisualizer(
                    result.solution,
                    result.alns.getJobs(),
                    result.alns.getMachines(),
                    result.alns.getIterationHistory()
                );
                visualizer.generateAllCharts(outputDir);
                System.out.println("可视化图表生成完成！\n");
            } catch (Exception e) {
                System.err.println("生成可视化图表时出错: " + e.getMessage());
                e.printStackTrace();
            }
            
            return result.solution;
        }
        
        return null;
    }
    
    /**
     * 主函数示例
     */
    public static void main(String[] args) {
        // 示例：运行ALNS算法并生成可视化
        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
        
        ALNSParameters params = new ALNSParameters();
        params.maxIterations = 1000;
        params.timeLimitMs = 60000; // 1分钟
        
        // 运行并生成可视化
        String outputDir = "chapter-1/src/main/output/visualization/example";
        ALNSSolution solution = runALNSWithVisualization(
            machinePath, itemPath, params, 12345L, outputDir
        );
        
        if (solution != null) {
            // 输出详细信息
            System.out.println("\n详细结果:");
            for (int machineId = 0; machineId < solution.machineBatches.size(); machineId++) {
                System.out.println("机器 " + machineId + ":");
                for (int batchIdx = 0; batchIdx < solution.getMachineBatches(machineId).size(); batchIdx++) {
                    ALNSBatch batch = solution.getMachineBatches(machineId).get(batchIdx);
                    System.out.println("  批次 " + batchIdx + 
                                     ": 作业数=" + batch.jobs.size() +
                                     ", 开始=" + String.format("%.2f", batch.startTime) +
                                     ", 结束=" + String.format("%.2f", batch.endTime));
                }
            }
        }
    }
}
