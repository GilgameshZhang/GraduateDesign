package util;

import AlgorithmFrame.machineChoice.ga.BatchGenome;
import ProblemFrame.*;
import util.ChartGenerator;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 实验结果输出工具类
 * 将实验结果输出到文件
 */
public class ExperimentResultWriter {
    private String outputFilePath;
    private String experimentFolder;
    private PrintWriter writer;
    
    /**
     * 创建实验结果写入器
     * @param experimentName 实验名称
     * @param runId 运行ID
     * @param instanceName 算例名称
     */
    public ExperimentResultWriter(String experimentName, int runId, String instanceName) throws IOException {
        String baseDir = "chapter-1/src/main/output/" + experimentName + "/";
        
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String folderName = instanceName + "_run" + runId + "_" + timestamp;
        this.experimentFolder = baseDir + folderName + "/";
        
        java.io.File experimentDir = new java.io.File(experimentFolder);
        if (!experimentDir.exists()) {
            experimentDir.mkdirs();
        }
        
        this.outputFilePath = experimentFolder + "实验报告.txt";
        this.writer = new PrintWriter(new FileWriter(outputFilePath), true);
    }
    
    /**
     * 写入实验基本信息
     */
    public void writeExperimentInfo(String experimentName, int runId, String instanceName,
                                    int maxGen, int popSize, double mutationRate, double crossoverRate) {
        writer.println("================================================================================");
        writer.println("                          实验结果报告");
        writer.println("================================================================================");
        writer.println("实验名称: " + experimentName);
        writer.println("算例名称: " + instanceName);
        writer.println("运行ID: " + runId);
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        writer.println("【参数配置】");
        writer.println("  最大代数: " + maxGen);
        writer.println("  种群规模: " + popSize);
        writer.println("  变异率: " + mutationRate);
        writer.println("  交叉率: " + crossoverRate);
        writer.println();
    }
    
    /**
     * 写入最终结果
     */
    public void writeFinalResults(double bestCmax, long runTime, int finalGeneration,
                                   List<Double> iterationHistory,
                                   BatchGenome bestGenome,
                                   Machine[] machines) {
        writer.println("================================================================================");
        writer.println("                          最终实验结果");
        writer.println("================================================================================");
        writer.println("最优Cmax: " + String.format("%.4f", bestCmax));
        writer.println("运行时间: " + runTime + " ms (" + String.format("%.2f", runTime/1000.0) + " 秒)");
        writer.println("终止代数: " + finalGeneration);
        writer.println();
        
        // 写入收敛曲线数据
        if (iterationHistory != null && !iterationHistory.isEmpty()) {
            writer.println("【收敛曲线数据】");
            writer.println("代数\tCmax");
            for (int i = 0; i < iterationHistory.size(); i++) {
                if (i % 10 == 0 || i == iterationHistory.size() - 1) {
                    writer.println((i + 1) + "\t" + String.format("%.4f", iterationHistory.get(i)));
                }
            }
            writer.println();
            
            // 生成收敛曲线图
            try {
                String chartPath = experimentFolder + "收敛曲线图.png";
                ChartGenerator.generateConvergenceCurve(
                    iterationHistory,
                    chartPath,
                    "算法收敛曲线"
                );
                writer.println("✓ 收敛曲线图已生成: 收敛曲线图.png");
            } catch (IOException e) {
                writer.println("⚠️ 收敛曲线图生成失败: " + e.getMessage());
            }
        }
        
        writer.println();
    }
    
    /**
     * 写入机器负载信息
     */
    public void writeMachineLoads(BatchGenome bestGenome) {
        if (bestGenome == null || bestGenome.solutions == null) {
            return;
        }
        
        writer.println("【机器负载统计】");
        for (int i = 0; i < bestGenome.solutions.size(); i++) {
            BatchResult solution = bestGenome.solutions.get(i);
            if (solution != null) {
                writer.println(String.format("  机器 %d: 批次数=%d, 完工时间=%.4f", 
                    i + 1, 
                    solution.solutions != null ? solution.solutions.size() : 0,
                    solution.fitness));
            }
        }
        writer.println();
    }
    
    /**
     * 写入任意文本行
     */
    public void writeLine(String text) {
        writer.println(text);
    }
    
    /**
     * 写入分隔线
     */
    public void writeSeparator() {
        writer.println("================================================================================");
    }
    
    /**
     * 获取实验文件夹路径
     */
    public String getExperimentFolder() {
        return experimentFolder;
    }
    
    /**
     * 关闭写入器
     */
    public void close() {
        if (writer != null) {
            writer.println();
            writer.println("================================================================================");
            writer.println("                         实验结果输出完成");
            writer.println("================================================================================");
            writer.close();
        }
    }
}
