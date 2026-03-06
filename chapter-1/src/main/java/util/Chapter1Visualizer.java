package util;

import ProblemFrame.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * 第一章通用可视化工具
 * 支持生成迭代曲线图、批次分布图和甘特图
 */
public class Chapter1Visualizer {
    
    /**
     * 生成迭代曲线图
     * @param iterationHistory 迭代历史数据（每代的最优值）
     * @param outputPath 输出文件路径
     * @param title 图表标题
     */
    public static void generateIterationCurve(List<Double> iterationHistory, 
                                             String outputPath, String title) throws IOException {
        System.out.println("生成迭代曲线图...");
        
        if (iterationHistory == null || iterationHistory.isEmpty()) {
            System.out.println("迭代历史为空，跳过迭代曲线图生成");
            return;
        }
        
        // 创建数据集
        XYSeries series = new XYSeries("最优Cmax");
        for (int i = 0; i < iterationHistory.size(); i++) {
            series.add(i, iterationHistory.get(i));
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,                       // 标题
            "迭代次数（代数）",           // X轴标签
            "最优Cmax",                  // Y轴标签
            dataset,                     // 数据集
            PlotOrientation.VERTICAL,    // 垂直方向
            true,                        // 显示图例
            true,                        // 显示工具提示
            false                        // 不显示URL
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置线条样式
        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer = 
            new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, new Color(31, 119, 180)); // 蓝色
        renderer.setSeriesStroke(0, new BasicStroke(2.5f));
        renderer.setSeriesShapesVisible(0, false);  // 不显示数据点
        plot.setRenderer(renderer);
        
        // 设置Y轴范围（添加padding）
        double minCmax = iterationHistory.stream().min(Double::compare).orElse(0.0);
        double maxCmax = iterationHistory.stream().max(Double::compare).orElse(1.0);
        double padding = (maxCmax - minCmax) * 0.1;
        plot.getRangeAxis().setRange(Math.max(0, minCmax - padding), maxCmax + padding);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 确保输出目录存在
        new File(outputPath).getParentFile().mkdirs();
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("迭代曲线图已保存: " + outputPath);
    }
    
    /**
     * 生成批次分布图（适用于GA-TS算法）
     * @param batchResults 批次结果列表
     * @param machines 机器数组
     * @param outputPath 输出文件路径
     */
    public static void generateBatchDistribution(List<BatchResult> batchResults, 
                                                Machine[] machines, 
                                                String outputPath) throws IOException {
        System.out.println("生成批次分布图...");
        
        if (batchResults == null || batchResults.isEmpty()) {
            System.out.println("批次结果为空，跳过批次分布图生成");
            return;
        }
        
        // 统计每台机器的批次数量
        Map<Integer, Integer> machineBatchCount = new HashMap<>();
        Map<Integer, Double> machineUtilization = new HashMap<>();
        
        double maxTime = 0.0;
        for (BatchResult br : batchResults) {
            for (Solution sol : br.solutions) {
                maxTime = Math.max(maxTime, sol.endTime);
            }
        }
        
        for (int i = 0; i < batchResults.size(); i++) {
            BatchResult br = batchResults.get(i);
            int batchCount = br.solutions.size();
            machineBatchCount.put(i, batchCount);
            
            // 计算利用率
            double busyTime = 0.0;
            for (Solution sol : br.solutions) {
                busyTime += (sol.endTime - sol.startTime);
            }
            double utilization = maxTime > 0 ? (busyTime / maxTime) * 100 : 0.0;
            machineUtilization.put(i, utilization);
        }
        
        // 创建数据集 - 批次数量
        XYSeries batchSeries = new XYSeries("批次数量");
        XYSeries utilizationSeries = new XYSeries("利用率(%)");
        
        for (int i = 0; i < machines.length; i++) {
            batchSeries.add(i, machineBatchCount.getOrDefault(i, 0));
            utilizationSeries.add(i, machineUtilization.getOrDefault(i, 0.0));
        }
        
        XYSeriesCollection dataset1 = new XYSeriesCollection();
        dataset1.addSeries(batchSeries);
        
        XYSeriesCollection dataset2 = new XYSeriesCollection();
        dataset2.addSeries(utilizationSeries);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYBarChart(
            "各机器批次分布与利用率",      // 标题
            "机器编号",                     // X轴标签
            false,                          // X轴使用数值型
            "批次数量",                     // Y轴标签
            dataset1,                       // 数据集
            PlotOrientation.VERTICAL,       // 垂直方向
            true,                           // 显示图例
            true,                           // 显示工具提示
            false                           // 不显示URL
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置第二个Y轴（利用率）
        org.jfree.chart.axis.NumberAxis axis2 = new org.jfree.chart.axis.NumberAxis("利用率(%)");
        plot.setRangeAxis(1, axis2);
        plot.setDataset(1, dataset2);
        plot.mapDatasetToRangeAxis(1, 1);
        
        // 设置渲染器
        org.jfree.chart.renderer.xy.XYBarRenderer renderer1 = 
            new org.jfree.chart.renderer.xy.XYBarRenderer();
        renderer1.setSeriesPaint(0, new Color(31, 119, 180)); // 蓝色条
        plot.setRenderer(0, renderer1);
        
        org.jfree.chart.renderer.xy.XYLineAndShapeRenderer renderer2 = 
            new org.jfree.chart.renderer.xy.XYLineAndShapeRenderer();
        renderer2.setSeriesPaint(0, new Color(255, 127, 14)); // 橙色线
        renderer2.setSeriesStroke(0, new BasicStroke(2.5f));
        plot.setRenderer(1, renderer2);
        
        // 设置中文字体
        setChineseFont(chart);
        axis2.setLabelFont(new Font("微软雅黑", Font.PLAIN, 14));
        
        // 确保输出目录存在
        new File(outputPath).getParentFile().mkdirs();
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("批次分布图已保存: " + outputPath);
    }
    
    /**
     * 生成甘特图（适用于GA-TS算法）
     * @param batchResults 批次结果列表
     * @param machines 机器数组
     * @param outputPath 输出文件路径
     */
    public static void generateGanttChart(List<BatchResult> batchResults, 
                                         Machine[] machines, 
                                         String outputPath) throws IOException {
        System.out.println("生成甘特图...");
        
        if (batchResults == null || batchResults.isEmpty()) {
            System.out.println("批次结果为空，跳过甘特图生成");
            return;
        }
        
        // 创建空的散点图
        JFreeChart chart = ChartFactory.createScatterPlot(
            "调度甘特图",                 // 标题
            "时间",                       // X轴标签
            "机器",                       // Y轴标签
            new XYSeriesCollection(),     // 空数据集
            PlotOrientation.VERTICAL,     // 垂直方向
            false,                        // 不显示图例
            true,                         // 显示工具提示
            false                         // 不显示URL
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 计算时间范围
        double minTime = 0.0;
        double maxTime = 0.0;
        
        for (BatchResult br : batchResults) {
            for (Solution sol : br.solutions) {
                maxTime = Math.max(maxTime, sol.endTime);
            }
        }
        
        // 为每台机器绘制批次
        for (int machineId = 0; machineId < batchResults.size(); machineId++) {
            BatchResult br = batchResults.get(machineId);
            List<Solution> solutions = br.solutions;
            
            // 按开始时间排序
            solutions.sort(Comparator.comparingDouble(s -> s.startTime));
            
            int batchNumber = 1;
            for (Solution sol : solutions) {
                drawBatchRectangle(plot, sol, machineId, batchNumber);
                batchNumber++;
            }
        }
        
        // 设置Y轴范围（机器轴）
        plot.getRangeAxis().setRange(-0.5, machines.length - 0.5);
        plot.getRangeAxis().setTickLabelsVisible(false);
        
        // 设置X轴范围（时间轴）
        double timePadding = maxTime * 0.15;
        plot.getDomainAxis().setRange(minTime - timePadding, maxTime + timePadding);
        
        // 添加机器标签
        double labelXPosition = minTime - maxTime * 0.12;
        for (int i = 0; i < machines.length; i++) {
            XYTextAnnotation label = new XYTextAnnotation(
                "机器" + (i + 1),
                labelXPosition,
                i
            );
            label.setPaint(Color.BLACK);
            label.setFont(new Font("微软雅黑", Font.BOLD, 12));
            plot.addAnnotation(label);
        }
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 确保输出目录存在
        new File(outputPath).getParentFile().mkdirs();
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("甘特图已保存: " + outputPath);
    }
    
    /**
     * 绘制批次矩形
     */
    private static void drawBatchRectangle(XYPlot plot, Solution solution, 
                                          int machineId, int batchNumber) {
        double x = solution.startTime;
        double y = machineId - 0.3;
        double width = solution.endTime - solution.startTime;
        double height = 0.6;
        
        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
        
        // 获取颜色（基于批次编号）
        Color color = getColorForBatch(batchNumber - 1);
        
        // 创建矩形注释
        XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
            rect, new BasicStroke(1.0f), Color.BLACK, color
        );
        
        plot.addAnnotation(rectAnnotation);
        
        // 添加批次标签
        String label = "B" + (machineId + 1) + "-" + batchNumber;
        XYTextAnnotation textLabel = new XYTextAnnotation(
            label,
            x + width * 0.1,
            y + height / 2
        );
        textLabel.setPaint(Color.BLACK);
        textLabel.setFont(new Font("微软雅黑", Font.BOLD, 10));
        plot.addAnnotation(textLabel);
        
        // 添加零件信息
        if (width > 50 && solution.placeItemList != null) {
            XYTextAnnotation itemCount = new XYTextAnnotation(
                "(" + solution.placeItemList.size() + "零件)",
                x + width * 0.5,
                y + height / 2
            );
            itemCount.setPaint(Color.DARK_GRAY);
            itemCount.setFont(new Font("微软雅黑", Font.PLAIN, 8));
            plot.addAnnotation(itemCount);
        }
    }
    
    /**
     * 生成每个批次的布局图
     * @param batchResults 批次结果列表
     * @param machines 机器数组
     * @param outputDir 输出目录
     */
    public static void generateBatchLayoutCharts(List<BatchResult> batchResults, 
                                                 Machine[] machines, 
                                                 String outputDir) throws IOException {
        System.out.println("生成批次布局图...");
        
        if (batchResults == null || batchResults.isEmpty()) {
            System.out.println("批次结果为空，跳过批次布局图生成");
            return;
        }
        
        for (int machineId = 0; machineId < batchResults.size(); machineId++) {
            BatchResult br = batchResults.get(machineId);
            List<Solution> solutions = br.solutions;
            
            for (int batchIdx = 0; batchIdx < solutions.size(); batchIdx++) {
                String outputPath = String.format(
                    "%s/machine_%d_batch_%d_layout.png",
                    outputDir, machineId + 1, batchIdx + 1
                );
                
                generateSingleBatchLayout(
                    machineId, batchIdx, solutions.get(batchIdx), 
                    machines[machineId], outputPath
                );
            }
        }
        
        System.out.println("批次布局图已生成");
    }
    
    /**
     * 生成单个批次的布局图
     */
    private static void generateSingleBatchLayout(int machineId, int batchIdx, 
                                                 Solution solution, Machine machine, 
                                                 String outputPath) throws IOException {
        // 创建散点图
        JFreeChart chart = ChartFactory.createScatterPlot(
            String.format("机器%d - 批次%d 零件布局", machineId + 1, batchIdx + 1),
            "X 坐标",
            "Y 坐标",
            new XYSeriesCollection()
        );
        
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置坐标轴范围
        double margin = 20.0;
        plot.getDomainAxis().setRange(-margin, machine.L + margin);
        plot.getRangeAxis().setRange(-margin, machine.W + margin);
        
        // 绘制机器边界
        drawMachineBoundary(plot, machine);
        
        // 绘制每个零件
        if (solution.placeItemList != null) {
            for (int i = 0; i < solution.placeItemList.size(); i++) {
                PlaceItem item = solution.placeItemList.get(i);
                drawItemRectangle(plot, item, i);
            }
        }
        
        // 添加批次信息
        XYTextAnnotation batchInfo = new XYTextAnnotation(
            String.format("批次%d: %.1f-%.1f (%d零件, 利用率:%.1f%%)",
                batchIdx + 1, solution.startTime, solution.endTime, 
                solution.placeItemList != null ? solution.placeItemList.size() : 0,
                solution.rate * 100),
            machine.L * 0.05, machine.W * 0.95
        );
        batchInfo.setFont(new Font("微软雅黑", Font.PLAIN, 12));
        plot.addAnnotation(batchInfo);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 确保输出目录存在
        new File(outputPath).getParentFile().mkdirs();
        
        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 800);
    }
    
    /**
     * 绘制机器边界
     */
    private static void drawMachineBoundary(XYPlot plot, Machine machine) {
        Rectangle2D.Double boundary = new Rectangle2D.Double(
            0, 0, machine.L, machine.W
        );
        
        XYShapeAnnotation annotation = new XYShapeAnnotation(
            boundary, new BasicStroke(2.0f), Color.BLACK, null
        );
        plot.addAnnotation(annotation);
        
        // 添加尺寸标注
        XYTextAnnotation sizeLabel = new XYTextAnnotation(
            String.format("平台尺寸: %.0fx%.0f", machine.L, machine.W),
            machine.L * 0.5, machine.W + machine.W * 0.03
        );
        sizeLabel.setPaint(Color.BLACK);
        sizeLabel.setFont(new Font("微软雅黑", Font.BOLD, 12));
        plot.addAnnotation(sizeLabel);
    }
    
    /**
     * 绘制零件矩形
     */
    private static void drawItemRectangle(XYPlot plot, PlaceItem item, int index) {
        double width = item.l;
        double height = item.w;
        
        Rectangle2D.Double rect = new Rectangle2D.Double(item.x, item.y, width, height);
        Color color = getColorForItem(index);
        
        XYShapeAnnotation annotation = new XYShapeAnnotation(
            rect, new BasicStroke(1.0f), Color.BLACK, color
        );
        plot.addAnnotation(annotation);
        
        // 添加零件编号
        XYTextAnnotation label = new XYTextAnnotation(
            item.name,
            item.x + width / 2,
            item.y + height / 2
        );
        label.setPaint(Color.BLACK);
        label.setFont(new Font("微软雅黑", Font.BOLD, 9));
        plot.addAnnotation(label);
        
        // 如果旋转，添加标记
        if (item.isRotate) {
            XYTextAnnotation rotateMark = new XYTextAnnotation(
                "↻",
                item.x + width * 0.85,
                item.y + height * 0.85
            );
            rotateMark.setPaint(Color.RED);
            rotateMark.setFont(new Font("微软雅黑", Font.BOLD, 12));
            plot.addAnnotation(rotateMark);
        }
    }
    
    /**
     * 生成所有可视化图表
     * @param batchResults 批次结果
     * @param machines 机器数组
     * @param iterationHistory 迭代历史
     * @param algorithmName 算法名称
     * @param outputDir 输出目录
     */
    public static void generateAllCharts(List<BatchResult> batchResults, 
                                        Machine[] machines,
                                        List<Double> iterationHistory,
                                        String algorithmName,
                                        String outputDir) throws IOException {
        System.out.println("\n开始生成可视化图表...");
        
        // 确保输出目录存在
        new File(outputDir).mkdirs();
        
        // 将BatchResult中的时间信息复制到Solution对象中（用于可视化）
        if (batchResults != null) {
            for (BatchResult br : batchResults) {
                if (br.solutions != null && br.startTimes != null && br.endTimes != null) {
                    for (int i = 0; i < br.solutions.size() && i < br.startTimes.size() && i < br.endTimes.size(); i++) {
                        Solution sol = br.solutions.get(i);
                        sol.startTime = br.startTimes.get(i);
                        sol.endTime = br.endTimes.get(i);
                    }
                }
            }
        }
        
        // 1. 生成迭代曲线图
        if (iterationHistory != null && !iterationHistory.isEmpty()) {
            generateIterationCurve(
                iterationHistory, 
                outputDir + "/iteration_curve.png",
                algorithmName + " 算法迭代曲线图"
            );
        }
        
        // 2. 生成批次分布图
        if (batchResults != null && !batchResults.isEmpty()) {
            generateBatchDistribution(
                batchResults, 
                machines, 
                outputDir + "/batch_distribution.png"
            );
        }
        
        // 3. 生成甘特图
        if (batchResults != null && !batchResults.isEmpty()) {
            generateGanttChart(
                batchResults, 
                machines, 
                outputDir + "/gantt_chart.png"
            );
        }
        
        // 4. 生成批次布局图
        if (batchResults != null && !batchResults.isEmpty()) {
            generateBatchLayoutCharts(
                batchResults, 
                machines, 
                outputDir
            );
        }
        
        System.out.println("所有可视化图表已生成到: " + outputDir + "\n");
    }
    
    /**
     * 获取批次颜色
     */
    private static Color getColorForBatch(int batchIndex) {
        Color[] colors = {
            new Color(255, 200, 200), // 浅红
            new Color(200, 255, 200), // 浅绿
            new Color(200, 200, 255), // 浅蓝
            new Color(255, 255, 200), // 浅黄
            new Color(255, 200, 255), // 浅粉
            new Color(200, 255, 255), // 浅青
            new Color(255, 220, 180), // 浅橙
            new Color(220, 180, 255), // 浅紫
            new Color(180, 255, 220), // 浅绿青
            new Color(255, 180, 220), // 浅粉红
        };
        return colors[batchIndex % colors.length];
    }
    
    /**
     * 获取零件颜色
     */
    private static Color getColorForItem(int itemIndex) {
        Color[] colors = {
            new Color(255, 200, 200), // 浅红
            new Color(200, 255, 200), // 浅绿
            new Color(200, 200, 255), // 浅蓝
            new Color(255, 255, 200), // 浅黄
            new Color(255, 200, 255), // 浅粉
            new Color(200, 255, 255), // 浅青
            new Color(255, 220, 180), // 浅橙
            new Color(220, 180, 255), // 浅紫
            new Color(180, 255, 220), // 浅绿青
            new Color(255, 180, 220), // 浅粉红
            new Color(220, 220, 180), // 浅黄绿
            new Color(180, 220, 220), // 浅青灰
        };
        return colors[itemIndex % colors.length];
    }
    
    /**
     * 设置中文字体
     */
    private static void setChineseFont(JFreeChart chart) {
        Font titleFont = new Font("微软雅黑", Font.BOLD, 18);
        chart.getTitle().setFont(titleFont);
        
        Font labelFont = new Font("微软雅黑", Font.PLAIN, 14);
        Font tickFont = new Font("微软雅黑", Font.PLAIN, 12);
        
        if (chart.getPlot() instanceof XYPlot) {
            XYPlot plot = (XYPlot) chart.getPlot();
            plot.getDomainAxis().setLabelFont(labelFont);
            plot.getRangeAxis().setLabelFont(labelFont);
            plot.getDomainAxis().setTickLabelFont(tickFont);
            plot.getRangeAxis().setTickLabelFont(tickFont);
        }
        
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("微软雅黑", Font.PLAIN, 12));
        }
    }
}
