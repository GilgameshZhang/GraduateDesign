package AlgorithmFrame.visualization;

import ProblemFrame.Solution;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 调度结果可视化器
 * 使用JFreeChart生成甘特图和打印机布局图
 */
public class ScheduleVisualizer {

    private final Solution solution;
    private final Problem problem;
    private final Operation[][] operationMatrix;
    private final ProblemFrame.Chromosome chromosome;
    private final List<Double> bestMakespanHistory;

    public ScheduleVisualizer(Solution solution, ProblemFrame.Chromosome chromosome, Problem problem, Operation[][] operationMatrix, List<Double> bestMakespanHistory) {
        this.solution = solution;
        this.chromosome = chromosome;
        this.problem = problem;
        this.operationMatrix = operationMatrix;
        this.bestMakespanHistory = bestMakespanHistory;
    }

    /**
     * 生成所有可视化图表
     */
    public void generateAllCharts(String outputDir) throws IOException {
        // 确保输出目录存在
        File dir = new File(outputDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        // 生成甘特图
        generateGanttChart(outputDir + "/gantt_chart.png");

        // 生成打印机布局图
        PrinterLayoutVisualizer layoutVisualizer =
            new PrinterLayoutVisualizer(chromosome, problem.getMachines());
        layoutVisualizer.generatePrinterLayoutChart(outputDir + "/printer_layout.png");
        
        // 生成迭代曲线图
        if (bestMakespanHistory != null && !bestMakespanHistory.isEmpty()) {
            generateIterationCurve(outputDir + "/iteration_curve.png");
        }

        // 生成汇总报告
        generateSummaryReport(outputDir + "/summary_report.json");

        System.out.println("可视化图表已生成到: " + outputDir);
    }

    /**
     * 生成甘特图 - 横轴时间，纵轴机器号
     */
    public void generateGanttChart(String outputPath) throws IOException {
        System.out.println("生成甘特图...");

        // 创建自定义XY图表
        JFreeChart chart = ChartFactory.createScatterPlot(
            "调度甘特图",     // 标题
            "时间",           // X轴标签
            "机器",           // Y轴标签
            new XYSeriesCollection(), // 空的dataset，因为我们要直接绘制
            PlotOrientation.VERTICAL, // 垂直方向
            false,           // 不显示图例
            true,            // 显示工具提示
            false            // 不显示URL
        );

        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        int machineCount = problem.getMachineCount();
        String[] machineNames = getMachineTypeNames();

        // 计算时间范围
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;

        // 收集所有操作并绘制矩形
        // 为每台机器维护独立的批次计数器
        Map<Integer, Integer> machineBatchCounter = new HashMap<>();
        
        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            // 收集该机器上的所有工序
            List<Operation> machineOperations = new ArrayList<>();
            for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
                for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                    Operation op = operationMatrix[jobIdx][opIdx];
                    if (op != null && op.machineNo == machineIdx) {
                        machineOperations.add(op);
                        minTime = Math.min(minTime, op.startTime);
                        maxTime = Math.max(maxTime, op.endTime);
                    }
                }
            }

            // 按开始时间排序，如果开始时间相同则按结束时间排序
            machineOperations.sort((a, b) -> {
                int cmp = Double.compare(a.startTime, b.startTime);
                if (cmp == 0) {
                    return Double.compare(a.endTime, b.endTime);
                }
                return cmp;
            });

            // 为该机器初始化批次计数器（从1开始）
            int batchCounter = 1;
            double lastStartTime = -1;
            double lastEndTime = -1;
            
            // 为每个工序绘制矩形
            for (Operation op : machineOperations) {
                // 只有打印机和批处理机才使用批次号，离散处理机传0
                if (isPrintMachine(op.machineNo) || isBatchMachine(op.machineNo)) {
                    // 判断是否是新批次：如果开始时间或结束时间与上一个工序不同，说明是新批次
                    // 允许微小的浮点误差（0.01）
                    if (lastStartTime >= 0 && 
                        (Math.abs(op.startTime - lastStartTime) > 0.01 || 
                         Math.abs(op.endTime - lastEndTime) > 0.01)) {
                        batchCounter++; // 新批次，批次号递增
                    }
                    
                    drawOperationRectangle(plot, op, machineIdx, batchCounter);
                    
                    // 更新上一个工序的时间
                    lastStartTime = op.startTime;
                    lastEndTime = op.endTime;
                } else {
                    // 离散处理机不使用批次号
                    drawOperationRectangle(plot, op, machineIdx, 0);
                }
            }
        }

        // 设置Y轴范围（机器轴）
        plot.getRangeAxis().setRange(-0.5, machineCount - 0.5);
        plot.getRangeAxis().setTickLabelsVisible(false); // 隐藏默认刻度标签

        // 设置X轴范围（时间轴），需要给左侧留出更多空间用于显示机器标签
        if (maxTime > minTime) {
            double timePadding = (maxTime - minTime) * 0.15;  // 增加padding到15%
            plot.getDomainAxis().setRange(minTime - timePadding, maxTime + timePadding);
        }

        // 添加机器标签在Y轴上（在设置X轴范围之后，这样可以使用新的范围）
        double labelXPosition = minTime - (maxTime - minTime) * 0.12;  // 增加偏移到12%
        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            XYTextAnnotation machineLabel = new XYTextAnnotation(
                machineNames[machineIdx],
                labelXPosition,  // X位置（靠近Y轴左侧，但不被挡住）
                machineIdx  // Y位置（机器位置）
            );
            machineLabel.setPaint(Color.BLACK);
            machineLabel.setFont(new Font("宋体", Font.BOLD, 12));
            plot.addAnnotation(machineLabel);
        }

        // 设置中文字体
        setChineseFont(chart);

        // 保存图表（提高分辨率到1920x1080）
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("甘特图已保存: " + outputPath);
    }
    
    /**
     * 生成迭代曲线图 - 显示每一代的最优makespan
     */
    private void generateIterationCurve(String outputPath) throws IOException {
        System.out.println("生成迭代曲线图...");
        
        // 创建数据集
        org.jfree.data.xy.XYSeries series = new org.jfree.data.xy.XYSeries("最优Makespan");
        for (int i = 0; i < bestMakespanHistory.size(); i++) {
            series.add(i, bestMakespanHistory.get(i));
        }
        
        org.jfree.data.xy.XYSeriesCollection dataset = new org.jfree.data.xy.XYSeriesCollection();
        dataset.addSeries(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            "遗传算法迭代曲线图",      // 标题
            "迭代次数（代数）",        // X轴标签
            "最优Makespan",           // Y轴标签
            dataset,                  // 数据集
            PlotOrientation.VERTICAL, // 垂直方向
            true,                     // 显示图例
            true,                     // 显示工具提示
            false                     // 不显示URL
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置线条样式
        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer = 
            new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        renderer.setSeriesShapesVisible(0, false);  // 不显示数据点，只显示线条
        plot.setRenderer(renderer);
        
        // 设置X轴范围
        plot.getDomainAxis().setRange(-1, bestMakespanHistory.size());
        
        // 设置Y轴范围（添加一点padding）
        double minMakespan = bestMakespanHistory.stream().min(Double::compare).orElse(0.0);
        double maxMakespan = bestMakespanHistory.stream().max(Double::compare).orElse(1.0);
        double padding = (maxMakespan - minMakespan) * 0.1;
        plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 保存图表（1920x1080高分辨率）
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("迭代曲线图已保存: " + outputPath);
    }

    /**
     * 绘制单个操作的矩形
     */
    private void drawOperationRectangle(XYPlot plot, Operation op, int machineIdx, int batchNumber) {
        // 计算矩形位置和大小 - 横轴时间，纵轴机器
        double x = op.startTime;      // 开始时间
        double y = machineIdx - 0.3;  // 机器位置（稍微下移以居中）
        double width = op.endTime - op.startTime; // 持续时间
        double height = 0.6;          // 固定高度

        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);

        // 获取颜色（基于工件）
        Color color = getColorForJob(op.jobNo);

        // 创建XYShapeAnnotation来绘制矩形
        XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
            rect, new BasicStroke(1.0f), Color.BLACK, color
        );

        // 设置标签
        String label;
        if (isPrintMachine(op.machineNo) || isBatchMachine(op.machineNo)) {
            // 打印机和批处理机：B机器号-批次号（用'-'分隔）
            int machineNumber = getMachineNumber(op.machineNo);
            label = "B" + machineNumber + "-" + batchNumber;
        } else {
            // 离散机：工件号
            label = "J" + op.jobNo;
        }

        rectAnnotation.setToolTipText(String.format("%s: %.1f-%.1f (%.1f)",
            label, op.startTime, op.endTime, width));

        plot.addAnnotation(rectAnnotation);

        // 添加操作标签（靠近坐标轴侧，即左侧）
        XYTextAnnotation textLabel = new XYTextAnnotation(
            label,
            x + width * 0.1,  // 靠近左侧（坐标轴侧）
            y + height / 2    // 垂直居中
        );
        textLabel.setPaint(Color.BLACK);
        textLabel.setFont(new Font("宋体", Font.BOLD, 10));
        // textLabel.setTextAnchor(TextAnchor.CENTER); // 暂时注释，可能版本不兼容
        plot.addAnnotation(textLabel);
    }

    /**
     * 判断是否为打印机
     */
    private boolean isPrintMachine(int machineNo) {
        return machineNo < problem.getPrintMachineCount();
    }

    /**
     * 判断是否为批处理机器
     */
    private boolean isBatchMachine(int machineNo) {
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        return machineNo >= printMachineCount && machineNo < printMachineCount + batchMachineCount;
    }

    /**
     * 获取机器编号（从1开始）
     */
    private int getMachineNumber(int machineNo) {
        if (isPrintMachine(machineNo)) {
            return machineNo + 1; // 打印机编号从1开始
        } else if (isBatchMachine(machineNo)) {
            return (machineNo - problem.getPrintMachineCount()) + 1; // 批处理机编号从1开始
        } else {
            return machineNo + 1; // 其他机器
        }
    }

    /**
     * 获取批处理机器的索引（从0开始）
     */
    private int getBatchMachineIndex(int machineNo) {
        return machineNo - problem.getPrintMachineCount();
    }


    /**
     * 生成汇总报告
     */
    private void generateSummaryReport(String outputPath) throws IOException {
        System.out.println("生成汇总报告...");

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode report = mapper.createObjectNode();

        // 基本信息
        report.put("makespan", solution.cost);
        report.put("job_count", problem.getJobCount());
        report.put("machine_count", problem.getMachineCount());
        report.put("printer_count", problem.getPrintMachineCount());
        report.put("batch_machine_count", problem.getBatchMachineCount());
        report.put("discrete_machine_count",
            problem.getMachineCount() - problem.getPrintMachineCount() - problem.getBatchMachineCount());

        // 机器利用率
        ArrayNode machineUtilization = report.putArray("machine_utilization");
        for (int i = 0; i < problem.getMachineCount(); i++) {
            ObjectNode machine = machineUtilization.addObject();
            machine.put("machine_id", i + 1);
            machine.put("name", getMachineTypeNames()[i]);

            // 计算该机器上的工序时间
            double busyTime = 0.0;
            for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
                for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                    Operation op = operationMatrix[jobIdx][opIdx];
                    if (op != null && op.machineNo == i) {
                        busyTime += (op.endTime - op.startTime);
                    }
                }
            }

            double utilization = solution.cost > 0 ? busyTime / solution.cost : 0.0;
            machine.put("utilization", utilization);
            machine.put("busy_time", busyTime);
            machine.put("total_time", solution.cost);
        }

        // 工件完工时间
        ArrayNode jobCompletion = report.putArray("job_completion_times");
        for (int jobIdx = 0; jobIdx < operationMatrix.length; jobIdx++) {
            ObjectNode job = jobCompletion.addObject();
            job.put("job_id", jobIdx);

            double completionTime = 0.0;
            for (int opIdx = 0; opIdx < operationMatrix[jobIdx].length; opIdx++) {
                Operation op = operationMatrix[jobIdx][opIdx];
                if (op != null && op.endTime > completionTime) {
                    completionTime = op.endTime;
                }
            }
            job.put("completion_time", completionTime);
        }

        // 保存JSON文件
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File(outputPath), report);
        System.out.println("汇总报告已保存: " + outputPath);
    }

    /**
     * 获取机器类型名称数组
     */
    private String[] getMachineTypeNames() {
        int machineCount = problem.getMachineCount();
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();

        String[] names = new String[machineCount];
        for (int i = 0; i < printMachineCount; i++) {
            names[i] = "打印机" + (i + 1);
        }
        for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
            names[i] = "批处理机" + (i - printMachineCount + 1);
        }
        for (int i = printMachineCount + batchMachineCount; i < machineCount; i++) {
            names[i] = "离散处理机" + (i - printMachineCount - batchMachineCount + 1);
        }
        return names;
    }

    /**
     * 获取工序类型名称
     */
    private String getOperationTypeName(int task) {
        if (task == 0) return "打印工序";
        else if (task == 1) return "批处理工序";
        else return "离散工序" + (task - 1);
    }

    /**
     * 为工件获取颜色
     */
    private Color getColorForJob(int jobIndex) {
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.CYAN, Color.PINK, Color.YELLOW, Color.GRAY, new Color(128, 0, 128),
            new Color(0, 128, 128), new Color(128, 128, 0), new Color(255, 128, 0),
            new Color(128, 255, 0), new Color(0, 255, 128), new Color(0, 128, 255)
        };
        return colors[jobIndex % colors.length];
    }

    /**
     * 设置中文字体
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
