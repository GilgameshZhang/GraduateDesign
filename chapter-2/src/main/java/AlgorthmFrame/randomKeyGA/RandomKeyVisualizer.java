package AlgorthmFrame.randomKeyGA;

import AlgorthmFrame.randomKeyGA.RandomKeyGA.RandomKeySolution;
import AlgorthmFrame.randomKeyGA.RandomKeyChromosome;
import ProgramEntity.Problem;
import ProgramEntity.Solution;
import ProgramEntity.PlaceItem;
import ProgramEntity.Operation;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Machine.Machine;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * 随机密钥GA可视化工具
 * 生成甘特图、打印批次布局图和详细报告
 */
public class RandomKeyVisualizer {
    
    private final RandomKeySolution solution;
    private final Problem problem;
    
    public RandomKeyVisualizer(RandomKeySolution solution, Problem problem) {
        this.solution = solution;
        this.problem = problem;
    }
    
    /**
     * 生成所有可视化内容
     */
    public void generateAllVisualizations(String outputDir) throws IOException {
        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        // 生成甘特图
        generateGanttChart(outputDir + "/gantt_chart.png");
        
        // 生成迭代曲线
        generateIterationCurve(outputDir + "/iteration_curve.png");
        
        // 生成打印批次布局图
        generatePrinterLayouts(outputDir);
        
        // 生成详细报告
        //generateDetailedReport(outputDir + "/detailed_report.txt");
        
        System.out.println("✓ 可视化文件已生成到: " + outputDir);
    }
    
    /**
     * 生成甘特图 - 横轴时间，纵轴机器号
     */
    private void generateGanttChart(String outputPath) throws IOException {
        RandomKeyChromosome chromosome = solution.chromosome;
        
        if (chromosome.operationMatrix == null) {
            System.out.println("无工序数据，跳过甘特图生成");
            return;
        }
        
        // 设置中文字体
        Font titleFont = new Font("Microsoft YaHei", Font.BOLD, 16);
        Font labelFont = new Font("Microsoft YaHei", Font.PLAIN, 12);
        Font tickFont = new Font("Microsoft YaHei", Font.PLAIN, 10);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createScatterPlot(
            "随机密钥GA - 调度甘特图",
            "时间",
            "机器",
            new XYSeriesCollection()
        );
        
        // 设置图表字体
        chart.getTitle().setFont(titleFont);
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置坐标轴字体
        plot.getDomainAxis().setLabelFont(labelFont);
        plot.getDomainAxis().setTickLabelFont(tickFont);
        plot.getRangeAxis().setLabelFont(labelFont);
        plot.getRangeAxis().setTickLabelFont(tickFont);
        
        // 设置Y轴范围（机器编号从0开始）
        // 参考原版ScheduleVisualizer的设置
        int totalMachines = problem.getMachineCount();
        plot.getRangeAxis().setRange(-0.5, totalMachines - 0.5);
        
        // 为每个零件分配颜色
        int jobCount = problem.getJobCount();
        Color[] colors = generateColors(jobCount);
        
        // 统计工序数量（用于调试）
        int printOpCount = 0, batchOpCount = 0, discreteOpCount = 0;
        
        // 遍历所有工序，绘制矩形
        for (int jobNo = 0; jobNo < chromosome.operationMatrix.length; jobNo++) {
            Operation[] operations = chromosome.operationMatrix[jobNo];
            
            for (Operation op : operations) {
                if (op != null && op.startTime >= 0 && op.endTime > op.startTime) {
                    // 统计工序类型
                    int printMachineCount = problem.getPrintMachineCount();
                    int batchMachineCount = problem.getBatchMachineCount();
                    
                    if (op.machineNo < printMachineCount) {
                        printOpCount++;
                    } else if (op.machineNo < printMachineCount + batchMachineCount) {
                        batchOpCount++;
                    } else {
                        discreteOpCount++;
                    }
                    
                    // 绘制工序矩形（参考原版ScheduleVisualizer的绘制方式）
                    double x = op.startTime;
                    double y = op.machineNo - 0.3;  // 机器位置（居中）
                    double width = op.endTime - op.startTime;
                    double height = 0.6;  // 固定高度
                    
                    Rectangle2D rect = new Rectangle2D.Double(x, y, width, height);
                    
                    XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
                        rect,
                        new BasicStroke(1.5f),
                        Color.BLACK,
                        colors[jobNo % colors.length]
                    );
                    plot.addAnnotation(rectAnnotation);
                    
                    // 添加工序标签（如果矩形足够大）
                    if (width > 5) {  // 只在矩形足够大时添加标签
                        String label = "J" + jobNo + "T" + op.task;
                        XYTextAnnotation textAnnotation = new XYTextAnnotation(
                            label,
                            x + width / 2,
                            y + height / 2
                        );
                        textAnnotation.setFont(new Font("Arial", Font.PLAIN, 9));
                        textAnnotation.setTextAnchor(TextAnchor.CENTER);
                        plot.addAnnotation(textAnnotation);
                    }
                }
            }
        }
        
        // 添加机器标签（左侧，参考原版ScheduleVisualizer）
        // 计算时间范围
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;
        for (int jobNo = 0; jobNo < chromosome.operationMatrix.length; jobNo++) {
            Operation[] operations = chromosome.operationMatrix[jobNo];
            for (Operation op : operations) {
                if (op != null && op.startTime >= 0 && op.endTime > op.startTime) {
                    minTime = Math.min(minTime, op.startTime);
                    maxTime = Math.max(maxTime, op.endTime);
                }
            }
        }
        
        // 设置X轴范围，留出空间显示机器标签
        if (maxTime > minTime) {
            double timePadding = (maxTime - minTime) * 0.15;
            plot.getDomainAxis().setRange(minTime - timePadding, maxTime + timePadding);
        }
        
        // 在Y轴左侧添加机器标签
        double labelXPosition = minTime - (maxTime - minTime) * 0.12;
        for (int i = 0; i < totalMachines; i++) {
            String machineLabel = getMachineLabel(i);
            XYTextAnnotation machineAnnotation = new XYTextAnnotation(
                machineLabel,
                labelXPosition,
                i + 0.5
            );
            machineAnnotation.setFont(labelFont);
            machineAnnotation.setPaint(Color.BLACK);
            plot.addAnnotation(machineAnnotation);
        }
        
        // 保存图表
        // 设置中文字体（参考原版）
        setChineseFont(chart);
        
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("  ✓ 甘特图已保存: " + outputPath);
        System.out.println("    - 打印工序: " + printOpCount + " 个");
        System.out.println("    - 批处理工序: " + batchOpCount + " 个");
        System.out.println("    - 离散工序: " + discreteOpCount + " 个");
    }
    
    /**
     * 生成颜色数组
     */
    private Color[] generateColors(int count) {
        Color[] colors = new Color[Math.max(count, 10)];
        colors[0] = new Color(255, 200, 200);
        colors[1] = new Color(200, 255, 200);
        colors[2] = new Color(200, 200, 255);
        colors[3] = new Color(255, 255, 200);
        colors[4] = new Color(255, 200, 255);
        colors[5] = new Color(200, 255, 255);
        colors[6] = new Color(255, 220, 180);
        colors[7] = new Color(180, 255, 220);
        colors[8] = new Color(220, 180, 255);
        colors[9] = new Color(255, 180, 220);
        
        // 如果零件数超过10，生成更多颜色
        for (int i = 10; i < colors.length; i++) {
            colors[i] = new Color(
                150 + (i * 23) % 105,
                150 + (i * 47) % 105,
                150 + (i * 71) % 105
            );
        }
        
        return colors;
    }
    
    /**
     * 获取机器标签
     */
    private String getMachineLabel(int machineIndex) {
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        
        if (machineIndex < printMachineCount) {
            return "打印机" + (machineIndex + 1);
        } else if (machineIndex < printMachineCount + batchMachineCount) {
            return "批处理" + (machineIndex - printMachineCount + 1);
        } else {
            return "离散" + (machineIndex - printMachineCount - batchMachineCount + 1);
        }
    }
    
    /**
     * 生成迭代曲线图
     */
    private void generateIterationCurve(String outputPath) throws IOException {
        List<Double> history = solution.makespanHistory;
        
        if (history == null || history.isEmpty()) {
            System.out.println("无迭代历史数据，跳过迭代曲线生成");
            return;
        }
        
        // 创建数据集
        XYSeries series = new XYSeries("最优Makespan");
        for (int i = 0; i < history.size(); i++) {
            series.add(i, history.get(i));
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            "随机密钥GA - 迭代曲线",
            "迭代次数",
            "最优Makespan",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // 设置样式
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置中文字体（参考原版）
        setChineseFont(chart);
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("  ✓ 迭代曲线已保存: " + outputPath);
    }
    
    /**
     * 生成打印批次布局图
     */
    private void generatePrinterLayouts(String outputDir) throws IOException {
        RandomKeyChromosome chromosome = solution.chromosome;
        
        if (chromosome.printSolution == null) {
            System.out.println("无打印批次数据，跳过布局图生成");
            return;
        }
        
        Machine[] machines = problem.getMachines();
        int printMachineCount = problem.getPrintMachineCount();
        
        // 为每台打印机的每个批次生成布局图
        for (int printerIdx = 0; printerIdx < printMachineCount; printerIdx++) {
            if (chromosome.printSolution[printerIdx] != null &&
                !chromosome.printSolution[printerIdx].isEmpty()) {
                
                List<Solution> batches = chromosome.printSolution[printerIdx];
                PrintMachine printer = (PrintMachine) machines[printerIdx];
                
                for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                    String outputPath = String.format("%s/printer%d_batch%d.png",
                        outputDir, printerIdx + 1, batchIdx + 1);
                    
                    generateSingleBatchLayout(
                        printer, printerIdx + 1, batchIdx + 1,
                        batches.get(batchIdx), outputPath
                    );
                }
            }
        }
    }
    
    /**
     * 生成单个批次的布局图
     */
    private void generateSingleBatchLayout(PrintMachine printer, int printerNo,
                                          int batchNo, Solution batch, String outputPath) throws IOException {
        // 创建图表
        JFreeChart chart = ChartFactory.createScatterPlot(
            String.format("打印机%d - 批次%d 零件布局", printerNo, batchNo),
            "X 坐标 (mm)",
            "Y 坐标 (mm)",
            new XYSeriesCollection()
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置坐标轴范围
        double margin = 20;
        plot.getDomainAxis().setRange(-margin, printer.L + margin);
        plot.getRangeAxis().setRange(-margin, printer.W + margin);
        
        // 绘制打印平台边界
        Rectangle2D platformBorder = new Rectangle2D.Double(0, 0, printer.L, printer.W);
        XYShapeAnnotation platformAnnotation = new XYShapeAnnotation(
            platformBorder,
            new BasicStroke(2.0f),
            Color.BLACK
        );
        plot.addAnnotation(platformAnnotation);
        
        // 绘制每个零件
        Color[] colors = {
            new Color(255, 200, 200), new Color(200, 255, 200),
            new Color(200, 200, 255), new Color(255, 255, 200),
            new Color(255, 200, 255), new Color(200, 255, 255)
        };
        
        int colorIndex = 0;
        for (PlaceItem item : batch.placeItemList) {
            // 绘制零件矩形
            Rectangle2D rect = new Rectangle2D.Double(item.x, item.y, item.l, item.w);
            Color partColor = colors[colorIndex % colors.length];
            
            XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
                rect,
                new BasicStroke(1.5f),
                Color.DARK_GRAY,
                partColor
            );
            plot.addAnnotation(rectAnnotation);
            
            // 添加零件标签
            XYTextAnnotation textAnnotation = new XYTextAnnotation(
                "P" + item.name,
                item.x + item.l / 2,
                item.y + item.w / 2
            );
            textAnnotation.setFont(new Font("SansSerif", Font.BOLD, 10));
            textAnnotation.setTextAnchor(TextAnchor.CENTER);
            plot.addAnnotation(textAnnotation);
            
            colorIndex++;
        }
        
        // 添加批次信息
        String info = String.format("零件数: %d | 利用率: %.1f%% | 高度: %.2f",
            batch.placeItemList.size(), batch.rate * 100, batch.maxG);
        XYTextAnnotation infoAnnotation = new XYTextAnnotation(
            info,
            printer.L / 2,
            printer.W + margin / 2
        );
        infoAnnotation.setFont(new Font("宋体", Font.PLAIN, 12));
        infoAnnotation.setTextAnchor(TextAnchor.TOP_CENTER);
        plot.addAnnotation(infoAnnotation);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 800);
        System.out.println(String.format("  ✓ 打印机%d批次%d布局图已保存", printerNo, batchNo));
    }
    
    /**
     * 生成详细报告
     */
    private void generateDetailedReport(String outputPath) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(outputPath));
        RandomKeyChromosome chromosome = solution.chromosome;
        
        writer.println("================================================================================");
        writer.println("                    随机密钥GA - 详细实验报告");
        writer.println("================================================================================");
        writer.println();
        
        // 1. 问题信息
        writer.println("【问题信息】");
        writer.println("零件数量: " + problem.getJobCount());
        writer.println("打印机数量: " + problem.getPrintMachineCount());
        writer.println("批处理机器数量: " + problem.getBatchMachineCount());
        writer.println();
        
        // 2. 求解结果
        writer.println("【求解结果】");
        writer.println("最优Makespan: " + String.format("%.2f", solution.cost));
        writer.println("总迭代次数: " + solution.makespanHistory.size());
        writer.println();
        
        // 3. 染色体编码
        writer.println("【最优染色体编码】");
        writer.println();
        
        int[] partSequence = chromosome.decodePartSequence();
        writer.println("零件加工顺序:");
        writer.print("  ");
        for (int i = 0; i < Math.min(20, partSequence.length); i++) {
            writer.print("P" + partSequence[i] + " ");
        }
        if (partSequence.length > 20) {
            writer.print("...");
        }
        writer.println();
        writer.println();
        
        writer.println("零件朝向配置:");
        writer.print("  ");
        for (int i = 0; i < Math.min(20, chromosome.partOrientations.length); i++) {
            writer.print("P" + i + ":O" + chromosome.partOrientations[i] + " ");
        }
        if (chromosome.partOrientations.length > 20) {
            writer.print("...");
        }
        writer.println();
        writer.println();
        
        writer.println("零件旋转配置:");
        writer.print("  ");
        for (int i = 0; i < Math.min(20, chromosome.partRotations.length); i++) {
            double angle = (chromosome.partRotations[i] - 1) * 45.0;
            writer.print("P" + i + ":" + (int)angle + "° ");
        }
        if (chromosome.partRotations.length > 20) {
            writer.print("...");
        }
        writer.println();
        writer.println();
        
        writer.println("零件机器分配:");
        writer.print("  ");
        for (int i = 0; i < Math.min(20, chromosome.machineAssignments.length); i++) {
            writer.print("P" + i + ":M" + chromosome.machineAssignments[i] + " ");
        }
        if (chromosome.machineAssignments.length > 20) {
            writer.print("...");
        }
        writer.println();
        writer.println();
        
        // 4. 打印批次详情
        writer.println("【打印批次详情】");
        writer.println();
        
        if (chromosome.printSolution != null) {
            int totalBatches = 0;
            int totalParts = 0;
            double totalUtilization = 0.0;
            
            for (int i = 0; i < problem.getPrintMachineCount(); i++) {
                if (chromosome.printSolution[i] != null && !chromosome.printSolution[i].isEmpty()) {
                    List<Solution> batches = chromosome.printSolution[i];
                    writer.println(String.format("打印机%d - 共%d个批次:", i + 1, batches.size()));
                    
                    for (int j = 0; j < batches.size(); j++) {
                        Solution batch = batches.get(j);
                        writer.println(String.format("  批次%d: 零件数=%d, 利用率=%.1f%%, 高度=%.2f, 时间=%.2f~%.2f",
                            j + 1,
                            batch.placeItemList.size(),
                            batch.rate * 100,
                            batch.maxG,
                            batch.startTime,
                            batch.endTime
                        ));
                        
                        totalParts += batch.placeItemList.size();
                        totalUtilization += batch.rate;
                    }
                    
                    totalBatches += batches.size();
                    writer.println();
                }
            }
            
            writer.println("汇总统计:");
            writer.println("  总批次数: " + totalBatches);
            writer.println("  总零件数: " + totalParts);
            if (totalBatches > 0) {
                writer.println(String.format("  平均利用率: %.1f%%", (totalUtilization / totalBatches) * 100));
            }
            writer.println();
        }
        
        // 5. 机器运行时间统计
        writer.println("【机器运行时间统计】");
        writer.println();
        
        if (chromosome.operationMatrix != null) {
            int totalMachines = problem.getMachineCount();
            int printMachineCount = problem.getPrintMachineCount();
            int batchMachineCount = problem.getBatchMachineCount();
            
            // 统计每台机器的运行时间和加工的工件详情
            double[] machineWorkTime = new double[totalMachines];
            double[] machineEndTime = new double[totalMachines];
            int[] machineOpCount = new int[totalMachines];
            List<List<String>> machineJobDetails = new ArrayList<>();  // 每台机器加工的工件详情（号、开始、结束）
            
            for (int i = 0; i < totalMachines; i++) {
                machineJobDetails.add(new ArrayList<>());
            }
            
            for (Operation[] operations : chromosome.operationMatrix) {
                for (Operation op : operations) {
                    if (op != null && op.startTime >= 0 && op.endTime > op.startTime) {
                        int machineIdx = op.machineNo;
                        if (machineIdx >= 0 && machineIdx < totalMachines) {
                            machineWorkTime[machineIdx] += (op.endTime - op.startTime);
                            machineEndTime[machineIdx] = Math.max(machineEndTime[machineIdx], op.endTime);
                            machineOpCount[machineIdx]++;
                            
                            // 记录工件详细信息：工件号[开始时间-结束时间]
                            String jobDetail = String.format("J%d[%.2f-%.2f]", op.jobNo, op.startTime, op.endTime);
                            machineJobDetails.get(machineIdx).add(jobDetail);
                        }
                    }
                }
            }
            
            // 输出打印机统计
            writer.println("打印机:");
            double totalPrintTime = 0.0;
            for (int i = 0; i < printMachineCount; i++) {
                if (machineOpCount[i] > 0) {
                    double utilization = machineEndTime[i] > 0 ? (machineWorkTime[i] / machineEndTime[i]) * 100 : 0;
                    writer.println(String.format("  打印机%d: 工序数=%d, 工作时间=%.2f, 完成时间=%.2f, 利用率=%.1f%%",
                        i + 1, machineOpCount[i], machineWorkTime[i], machineEndTime[i], utilization));
                    
                    // 输出该机器加工的所有工件（每个工序都显示）
                    if (!machineJobDetails.get(i).isEmpty()) {
                        writer.println("    加工工件详情:");
                        for (String jobDetail : machineJobDetails.get(i)) {
                            writer.println("      " + jobDetail);
                        }
                    }
                    
                    totalPrintTime += machineWorkTime[i];
                } else {
                    writer.println(String.format("  打印机%d: 未使用", i + 1));
                }
            }
            writer.println(String.format("  打印机总工作时间: %.2f", totalPrintTime));
            writer.println();
            
            // 输出批处理机器统计
            writer.println("批处理机器:");
            double totalBatchTime = 0.0;
            for (int i = 0; i < batchMachineCount; i++) {
                int machineIdx = printMachineCount + i;
                if (machineOpCount[machineIdx] > 0) {
                    double utilization = machineEndTime[machineIdx] > 0 ? (machineWorkTime[machineIdx] / machineEndTime[machineIdx]) * 100 : 0;
                    writer.println(String.format("  批处理机%d: 工序数=%d, 工作时间=%.2f, 完成时间=%.2f, 利用率=%.1f%%",
                        i + 1, machineOpCount[machineIdx], machineWorkTime[machineIdx], machineEndTime[machineIdx], utilization));
                    
                    // 输出该机器加工的所有工件（每个工序都显示）
                    if (!machineJobDetails.get(machineIdx).isEmpty()) {
                        writer.println("    加工工件详情:");
                        for (String jobDetail : machineJobDetails.get(machineIdx)) {
                            writer.println("      " + jobDetail);
                        }
                    }
                    
                    totalBatchTime += machineWorkTime[machineIdx];
                } else {
                    writer.println(String.format("  批处理机%d: 未使用", i + 1));
                }
            }
            writer.println(String.format("  批处理机总工作时间: %.2f", totalBatchTime));
            writer.println();
            
            // 输出离散处理机器统计
            int discreteMachineCount = totalMachines - printMachineCount - batchMachineCount;
            writer.println("离散处理机器:");
            double totalDiscreteTime = 0.0;
            for (int i = 0; i < discreteMachineCount; i++) {
                int machineIdx = printMachineCount + batchMachineCount + i;
                if (machineIdx < totalMachines && machineOpCount[machineIdx] > 0) {
                    double utilization = machineEndTime[machineIdx] > 0 ? (machineWorkTime[machineIdx] / machineEndTime[machineIdx]) * 100 : 0;
                    writer.println(String.format("  离散机%d: 工序数=%d, 工作时间=%.2f, 完成时间=%.2f, 利用率=%.1f%%",
                        i + 1, machineOpCount[machineIdx], machineWorkTime[machineIdx], machineEndTime[machineIdx], utilization));
                    
                    // 输出该机器加工的所有工件（每个工序都显示）
                    if (!machineJobDetails.get(machineIdx).isEmpty()) {
                        writer.println("    加工工件详情:");
                        for (String jobDetail : machineJobDetails.get(machineIdx)) {
                            writer.println("      " + jobDetail);
                        }
                    }
                    
                    totalDiscreteTime += machineWorkTime[machineIdx];
                } else if (machineIdx < totalMachines) {
                    writer.println(String.format("  离散机%d: 未使用", i + 1));
                }
            }
            writer.println(String.format("  离散机总工作时间: %.2f", totalDiscreteTime));
            writer.println();
            
            // 输出汇总
            writer.println("全局统计:");
            writer.println(String.format("  所有机器总工作时间: %.2f", totalPrintTime + totalBatchTime + totalDiscreteTime));
            writer.println(String.format("  最终完成时间(Makespan): %.2f", solution.cost));
            double overallUtilization = solution.cost > 0 ? ((totalPrintTime + totalBatchTime + totalDiscreteTime) / (solution.cost * totalMachines)) * 100 : 0;
            writer.println(String.format("  整体机器利用率: %.1f%%", overallUtilization));
            writer.println();
        }
        
        // 6. 迭代历史
        writer.println("【迭代历史（前10代和后10代）】");
        writer.println();
        
        List<Double> history = solution.makespanHistory;
        for (int i = 0; i < Math.min(10, history.size()); i++) {
            writer.println(String.format("  Gen %3d: %.2f", i, history.get(i)));
        }
        
        if (history.size() > 20) {
            writer.println("  ...");
            for (int i = Math.max(10, history.size() - 10); i < history.size(); i++) {
                writer.println(String.format("  Gen %3d: %.2f", i, history.get(i)));
            }
        } else if (history.size() > 10) {
            for (int i = 10; i < history.size(); i++) {
                writer.println(String.format("  Gen %3d: %.2f", i, history.get(i)));
            }
        }
        
        writer.println();
        writer.println("================================================================================");
        writer.println("                              报告生成完成");
        writer.println("================================================================================");
        
        writer.close();
        System.out.println("  ✓ 详细报告已保存: " + outputPath);
    }
    
    /**
     * 设置中文字体（参考原版ScheduleVisualizer）
     */
    private void setChineseFont(JFreeChart chart) {
        // 设置标题字体
        Font titleFont = new Font("宋体", Font.BOLD, 18);
        chart.getTitle().setFont(titleFont);
        
        // 设置轴标签字体
        Font labelFont = new Font("宋体", Font.PLAIN, 14);
        Font tickFont = new Font("宋体", Font.PLAIN, 12);
        
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.getDomainAxis().setLabelFont(labelFont);
            plot.getRangeAxis().setLabelFont(labelFont);
            plot.getDomainAxis().setTickLabelFont(tickFont);
            plot.getRangeAxis().setTickLabelFont(tickFont);
        }
    }
}

