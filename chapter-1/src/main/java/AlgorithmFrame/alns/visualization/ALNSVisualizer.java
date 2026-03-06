package AlgorithmFrame.alns.visualization;

import AlgorithmFrame.alns.*;
import util.ChartGenerator;
import java.io.IOException;
import java.util.List;

/**
 * ALNS算法可视化工具
 */
public class ALNSVisualizer {
    
    private ALNSSolution solution;
    private ALNSJob[] jobs;
    private ALNSMachine[] machines;
    private List<Double> iterationHistory;
    
    /**
     * 构造函数
     */
    public ALNSVisualizer(ALNSSolution solution, ALNSJob[] jobs, 
                         ALNSMachine[] machines, List<Double> iterationHistory) {
        this.solution = solution;
        this.jobs = jobs;
        this.machines = machines;
        this.iterationHistory = iterationHistory;
    }
    
    /**
     * 生成收敛曲线图
     */
    public void generateConvergenceCurve(String outputFolder) throws IOException {
        if (iterationHistory == null || iterationHistory.isEmpty()) {
            System.out.println("⚠️ 无收敛历史数据");
            return;
        }
        
        String chartPath = outputFolder + "收敛曲线图.png";
        ChartGenerator.generateConvergenceCurve(
            iterationHistory,
            chartPath,
            "ALNS算法收敛曲线"
        );
        System.out.println("✓ ALNS收敛曲线图已生成: " + chartPath);
    }
    
    /**
     * 生成所有可视化图表
     */
    public void generateAllCharts(String outputFolder) throws IOException {
        generateConvergenceCurve(outputFolder);
    }
}
