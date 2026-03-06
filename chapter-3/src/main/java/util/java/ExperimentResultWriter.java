package util.java;


import AlgorithmFrame.visualization.PrinterLayoutVisualizer;
import AlgorithmFrame.visualization.ScheduleVisualizer;
import ProblemFrame.Chromosome;
import ProblemFrame.GAParameters;
import ProblemFrame.Solution;
import ProgramEntity.Operation;
import ProgramEntity.Problem;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * 田口实验结果输出工具类
 * 将实验结果输出到文件而非控制台，并生成图表（收敛曲线、打印布局图、甘特图）
 */
public class ExperimentResultWriter {
    private String outputFilePath;
    private String experimentFolder;  // 实验文件夹路径
    private PrintWriter writer;
    private List<Double> makespanHistory;  // 保存makespan历史用于生成图表
    
    // 用于生成可视化图表的数据
    private Problem problem;
    private Chromosome bestChromosome;
    private Operation[][] operationMatrix;
    
    /**
     * 创建实验结果写入器
     * @param instanceName 算例名称（如 J20P3B2D5_01）
     * @param experimentNo 实验次数（如 1, 2, 3...）
     * @param problem 问题对象（用于生成可视化图表）
     */
    public ExperimentResultWriter(String instanceName, int experimentNo, Problem problem) throws IOException {
        this.problem = problem;
        // 创建基础目录
        String baseDir = "";
        File baseDirFile = new File(baseDir);
        if (!baseDirFile.exists()) {
            baseDirFile.mkdirs();
        }
        
        // 为每次实验创建独立文件夹
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String folderName = instanceName + "_实验" + experimentNo + "_" + timestamp;
        this.experimentFolder = baseDir + folderName + "/";
        
        File experimentDir = new File(experimentFolder);
        if (!experimentDir.exists()) {
            experimentDir.mkdirs();
        }
        
        // 创建txt报告文件
        this.outputFilePath = experimentFolder + "实验报告.txt";
        this.writer = new PrintWriter(new FileWriter(outputFilePath), true);
        
        System.out.println("📁 实验文件夹已创建: " + experimentFolder);
    }
    
    /**
     * 写入实验基本信息（GAParameters版本）
     */
    public void writeExperimentInfo(String instanceName, int experimentNo, GAParameters params) {
        writer.println("================================================================================");
        writer.println("                          实验结果报告");
        writer.println("================================================================================");
        writer.println("算例名称: " + instanceName);
        writer.println("实验次数: 第 " + experimentNo + " 次");
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        writer.println("【参数配置】");
        writer.println(params.toString());
        writer.println();
    }
    
    /**
     * 写入实验基本信息（String版本，用于PSO等其他算法）
     */
    public void writeExperimentInfo(String algorithmName, int experimentNo, String paramsString) {
        writer.println("================================================================================");
        writer.println("                          实验结果报告");
        writer.println("================================================================================");
        writer.println("算法名称: " + algorithmName);
        writer.println("实验次数: 第 " + experimentNo + " 次");
        writer.println("实验时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        writer.println();
        writer.println("【参数配置】");
        writer.println(paramsString);
        writer.println();
    }
    
    /**
     * 写入迭代过程信息（GA版本）
     */
    public void writeIterationInfo(int generation, double bestMakespan, double avgMakespan) {
        if (generation % 10 == 0 || generation == 1) {
            writer.println(String.format("代数 %3d: 最优Makespan=%.2f, 平均Makespan=%.2f", 
                generation, bestMakespan, avgMakespan));
        }
    }
    
    /**
     * 写入迭代过程信息（通用String版本，用于PSO等其他算法）
     */
    public void writeIterationInfo(String message) {
        writer.println(message);
    }
    
    /**
     * 设置最优解数据（用于生成可视化图表）
     */
    public void setBestSolutionData(Chromosome chromosome, Operation[][] operationMatrix) {
        this.bestChromosome = chromosome;
        this.operationMatrix = operationMatrix;
    }
    
    /**
     * 写入最终结果并生成图表（重载版本，直接接收数据）
     */
    public void writeFinalResults(double bestMakespan, long runTime, int finalGeneration, 
                                   List<Double> makespanHistory,
                                   Chromosome chromosome, Operation[][] opMatrix) {
        // 保存数据用于后续可能的使用
        this.bestChromosome = chromosome;
        this.operationMatrix = opMatrix;
        
        // 调用原方法
        writeFinalResults(bestMakespan, runTime, finalGeneration, makespanHistory);
    }
    
    /**
     * 写入最终结果并生成图表
     */
    public void writeFinalResults(double bestMakespan, long runTime, int finalGeneration, 
                                   List<Double> makespanHistory) {
        this.makespanHistory = makespanHistory;  // 保存用于生成图表
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                          最终实验结果");
        writer.println("================================================================================");
        writer.println("最优Makespan: " + String.format("%.2f", bestMakespan));
        writer.println("运行时间: " + runTime + " ms (" + String.format("%.2f", runTime/1000.0) + " 秒)");
        writer.println("终止代数: " + finalGeneration);
        writer.println();
        
        // 写入收敛曲线数据
        writer.println("【收敛曲线数据】（每10代记录一次）");
        writer.println("代数\tMakespan");
        for (int i = 0; i < makespanHistory.size(); i++) {
            if (i % 10 == 0 || i == makespanHistory.size() - 1) {
                writer.println((i + 1) + "\t" + String.format("%.2f", makespanHistory.get(i)));
            }
        }
        writer.println();
        
        // 生成收敛曲线图
        try {
            String chartPath = experimentFolder + "收敛曲线图.png";
            ChartGenerator.generateConvergenceCurve(
                makespanHistory, 
                chartPath, 
                "算法收敛曲线 - 最优Makespan变化"
            );
            writer.println("✓ 收敛曲线图已生成: 收敛曲线图.png");
            System.out.println("📊 收敛曲线图已生成: " + chartPath);
        } catch (IOException e) {
            writer.println("⚠️ 收敛曲线图生成失败: " + e.getMessage());
            System.err.println("⚠️ 收敛曲线图生成失败: " + e.getMessage());
        }
        
        // 生成打印布局图和甘特图
        System.out.println("DEBUG: 检查数据完整性...");
        System.out.println("  bestChromosome: " + (bestChromosome != null ? "已设置" : "NULL"));
        System.out.println("  operationMatrix: " + (operationMatrix != null ? "已设置" : "NULL"));
        System.out.println("  problem: " + (problem != null ? "已设置" : "NULL"));
        
        if (bestChromosome != null && operationMatrix != null && problem != null) {
            try {
                writer.println();
                writer.println("【正在生成调度可视化图表】");
                System.out.println("开始生成可视化图表到: " + experimentFolder);
                
                Solution solution = new Solution();
                solution.cost = bestMakespan;
                
                ScheduleVisualizer visualizer = new ScheduleVisualizer(
                    solution, bestChromosome, problem, operationMatrix, makespanHistory
                );
                
                // 生成甘特图
                System.out.println("正在生成甘特图...");
                String ganttPath = experimentFolder + "甘特图.png";
                visualizer.generateGanttChart(ganttPath);
                writer.println("✓ 甘特图已生成: 甘特图.png");
                System.out.println("📊 甘特图已生成: " + ganttPath);
                
                // 生成打印批次布局图
                System.out.println("正在生成打印批次布局图...");
                PrinterLayoutVisualizer layoutVisualizer =
                    new PrinterLayoutVisualizer(bestChromosome, problem.getMachines());
                String layoutBasePath = experimentFolder + "打印布局图";
                layoutVisualizer.generatePrinterLayoutChart(layoutBasePath + ".png");
                writer.println("✓ 打印批次布局图已生成: 打印布局图_printer*_batch*.png");
                System.out.println("📊 打印批次布局图已生成到实验文件夹");
                
            } catch (Exception e) {
                String errorMsg = "⚠️ 调度可视化图表生成失败: " + e.getClass().getName() + ": " + e.getMessage();
                writer.println(errorMsg);
                System.err.println(errorMsg);
                System.err.println("详细错误堆栈:");
                e.printStackTrace();
                
                // 输出更多调试信息
                System.err.println("\n调试信息:");
                System.err.println("  实验文件夹: " + experimentFolder);
                System.err.println("  bestChromosome.printSolution: " + (bestChromosome.printSolution != null ? "已设置" : "NULL"));
                if (bestChromosome.printSolution != null) {
                    System.err.println("  打印机数量: " + bestChromosome.printSolution.length);
                }
            }
        } else {
            String msg = "⚠️ 无法生成可视化图表：缺少必要数据";
            writer.println(msg);
            System.err.println(msg);
            if (bestChromosome == null) System.err.println("  - bestChromosome 为 NULL");
            if (operationMatrix == null) System.err.println("  - operationMatrix 为 NULL");
            if (problem == null) System.err.println("  - problem 为 NULL");
        }
        
        writer.println();
    }
    
    /**
     * 写入详细调度结果
     */
    public void writeDetailedSchedule(Operation[][] operationMatrix, String[] machineTypeNames) {
        writer.println("================================================================================");
        writer.println("                          详细调度方案");
        writer.println("================================================================================");
        writer.println();
        
        // 按工件输出加工路径
        writer.println("【各工件的加工路径】");
        for (int i = 0; i < operationMatrix.length; i++) {
            writer.println();
            writer.println("  工件 " + i + " 的加工路径:");
            writer.println("    " + String.format("%-15s %-20s %-12s %-12s %-10s", 
                "工序", "机器", "开始时间", "结束时间", "加工时间"));
            writer.println("    " + "----------------------------------------------------------------");
            
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation op = operationMatrix[i][j];
                if (op != null) {
                    String operationType = getOperationTypeName(op.task);
                    String machineType = machineTypeNames[op.machineNo];
                    writer.println("    " + String.format("%-15s %-20s %-12.2f %-12.2f %-10.2f", 
                        operationType,
                        machineType,
                        op.startTime, 
                        op.endTime,
                        (op.endTime - op.startTime)));
                }
            }
            writer.println("    总完工时间: " + String.format("%.2f", 
                operationMatrix[i][operationMatrix[i].length - 1].endTime));
        }
        writer.println();
    }
    
    /**
     * 写入机器负载信息
     */
    public void writeMachineLoads(Operation[][] operationMatrix, int machineCount) {
        writer.println("【机器负载统计】");
        double[] machineWorkTime = new double[machineCount];
        int[] machineOperCount = new int[machineCount];
        
        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation op = operationMatrix[i][j];
                if (op != null && op.machineNo >= 0 && op.machineNo < machineCount) {
                    machineWorkTime[op.machineNo] += (op.endTime - op.startTime);
                    machineOperCount[op.machineNo]++;
                }
            }
        }
        
        for (int i = 0; i < machineCount; i++) {
            writer.println(String.format("  机器 %d: 工作时间=%.2f, 工序数=%d", 
                i, machineWorkTime[i], machineOperCount[i]));
        }
        writer.println();
    }
    
    /**
     * 写入分隔线
     */
    public void writeSeparator() {
        writer.println("================================================================================");
    }
    
    /**
     * 写入任意文本
     */
    public void writeLine(String text) {
        writer.println(text);
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
            writer.println();
            writer.println("实验文件夹: " + experimentFolder);
            writer.println("包含文件:");
            writer.println("  - 实验报告.txt (本文件)");
            writer.println("  - 收敛曲线图.png");
            writer.println("  - 甘特图.png");
            writer.println("  - 打印布局图_printer*_batch*.png (各批次布局)");
            
            writer.close();
        }
        
        System.out.println("✅ 实验结果已完整保存到文件夹: " + experimentFolder);
    }
    
    /**
     * 获取输出文件路径
     */
    public String getOutputFilePath() {
        return outputFilePath;
    }
    
    /**
     * 获取实验文件夹路径
     */
    public String getExperimentFolder() {
        return experimentFolder;
    }
    
    /**
     * 获取工序类型名称
     */
    private String getOperationTypeName(int task) {
        if (task == 0) return "打印工序";
        else if (task == 1) return "批处理工序";
        else return "离散工序" + (task - 1);
    }
}
