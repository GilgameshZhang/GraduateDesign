package util.java;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.RectangleInsets;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 图表生成工具类
 * 用于生成田口实验的各种图表
 */
public class ChartGenerator {
    
    /**
     * 生成收敛曲线图
     * 
     * @param makespanHistory 每代的最优makespan历史
     * @param outputPath 输出文件路径
     * @param title 图表标题
     * @throws IOException IO异常
     */
    public static void generateConvergenceCurve(List<Double> makespanHistory, 
                                               String outputPath, 
                                               String title) throws IOException {
        // 创建数据集
        XYSeries series = new XYSeries("最优Makespan");
        for (int i = 0; i < makespanHistory.size(); i++) {
            series.add(i + 1, makespanHistory.get(i));
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,                      // 标题
            "迭代次数 (Generation)",    // X轴标签
            "Makespan",                 // Y轴标签
            dataset,                    // 数据集
            PlotOrientation.VERTICAL,   // 方向
            true,                       // 显示图例
            true,                       // 显示工具提示
            false                       // 不生成URL
        );
        
        // 自定义图表样式
        customizeChart(chart);
        
        // 根据数据范围自动调整Y轴
        XYPlot plot = chart.getXYPlot();
        double minMakespan = makespanHistory.stream().min(Double::compare).orElse(0.0);
        double maxMakespan = makespanHistory.stream().max(Double::compare).orElse(1.0);
        
        // 计算padding（5%的范围，更紧凑）
        double range = maxMakespan - minMakespan;
        
        // 如果所有值相同（range为0），设置一个最小范围以避免绘图错误
        if (range < 0.001) {
            // 设置为makespan值的±0.5%作为显示范围
            double minRange = Math.max(1.0, minMakespan * 0.005);
            minMakespan = minMakespan - minRange;
            maxMakespan = maxMakespan + minRange;
        } else {
            double padding = range * 0.05;
            minMakespan = minMakespan - padding;
            maxMakespan = maxMakespan + padding;
        }
        
        // 设置Y轴范围
        plot.getRangeAxis().setRange(minMakespan, maxMakespan);
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 800, 600);
    }
    
    /**
     * 生成多条收敛曲线对比图（用于对比不同参数配置）
     * 
     * @param historyMap 多组实验的makespan历史 (实验名称 -> makespan历史)
     * @param outputPath 输出文件路径
     * @param title 图表标题
     * @throws IOException IO异常
     */
    public static void generateMultipleConvergenceCurves(
            java.util.Map<String, List<Double>> historyMap,
            String outputPath, 
            String title) throws IOException {
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        
        // 为每组实验创建一个数据系列，同时计算全局最小最大值
        for (java.util.Map.Entry<String, List<Double>> entry : historyMap.entrySet()) {
            String experimentName = entry.getKey();
            List<Double> history = entry.getValue();
            
            XYSeries series = new XYSeries(experimentName);
            for (int i = 0; i < history.size(); i++) {
                double value = history.get(i);
                series.add(i + 1, value);
                minMakespan = Math.min(minMakespan, value);
                maxMakespan = Math.max(maxMakespan, value);
            }
            dataset.addSeries(series);
        }
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            "迭代次数 (Generation)",
            "Makespan",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // 自定义图表样式
        customizeChart(chart);
        
        // 根据数据范围自动调整Y轴
        XYPlot plot = chart.getXYPlot();
        double range = maxMakespan - minMakespan;
        
        // 如果所有值相同（range为0），设置一个最小范围
        if (range < 0.001) {
            double minRange = Math.max(1.0, minMakespan * 0.005);
            plot.getRangeAxis().setRange(minMakespan - minRange, maxMakespan + minRange);
        } else {
            double padding = range * 0.05;
            plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
        }
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1000, 600);
    }
    
    /**
     * 生成参数配置对比柱状图
     * 
     * @param experimentNames 实验名称数组
     * @param makespans 对应的makespan值数组
     * @param outputPath 输出文件路径
     * @param title 图表标题
     * @throws IOException IO异常
     */
    public static void generateParameterComparisonChart(
            String[] experimentNames,
            double[] makespans,
            String outputPath,
            String title) throws IOException {
        
        org.jfree.data.category.DefaultCategoryDataset dataset = 
            new org.jfree.data.category.DefaultCategoryDataset();
        
        // 找出最大最小值
        double minMakespan = Double.MAX_VALUE;
        double maxMakespan = Double.MIN_VALUE;
        for (int i = 0; i < experimentNames.length; i++) {
            dataset.addValue(makespans[i], "Makespan", experimentNames[i]);
            minMakespan = Math.min(minMakespan, makespans[i]);
            maxMakespan = Math.max(maxMakespan, makespans[i]);
        }
        
        // 创建柱状图
        JFreeChart chart = ChartFactory.createBarChart(
            title,
            "实验编号",
            "平均Makespan",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // 自定义图表样式
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("微软雅黑", Font.BOLD, 18));
        
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 根据数据范围自动调整Y轴
        double range = maxMakespan - minMakespan;
        
        // 如果所有值相同（range为0），设置一个最小范围
        if (range < 0.001) {
            double minRange = Math.max(1.0, minMakespan * 0.005);
            plot.getRangeAxis().setRange(minMakespan - minRange, maxMakespan + minRange);
        } else {
            double padding = range * 0.05;
            plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
        }
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1000, 600);
    }
    
    /**
     * 生成S/N比主效应图（田口方法）
     * 
     * @param factorNames 因子名称
     * @param levelValues 每个因子的各水平S/N比值
     * @param outputPath 输出文件路径
     * @param title 图表标题
     * @throws IOException IO异常
     */
    public static void generateMainEffectsPlot(
            String[] factorNames,
            double[][] levelValues,  // [因子数][水平数]
            String outputPath,
            String title) throws IOException {
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        // 为每个因子创建一条曲线
        for (int i = 0; i < factorNames.length; i++) {
            XYSeries series = new XYSeries(factorNames[i]);
            for (int j = 0; j < levelValues[i].length; j++) {
                series.add(j + 1, levelValues[i][j]);
            }
            dataset.addSeries(series);
        }
        
        // 创建折线图
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            "水平 (Level)",
            "S/N 比 (dB)",
            dataset,
            PlotOrientation.VERTICAL,
            true,
            true,
            false
        );
        
        // 自定义图表样式
        customizeChart(chart);
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1000, 600);
    }
    
    /**
     * 自定义图表样式
     */
    private static void customizeChart(JFreeChart chart) {
        // 设置背景色
        chart.setBackgroundPaint(Color.WHITE);
        
        // 设置标题字体
        chart.getTitle().setFont(new Font("微软雅黑", Font.BOLD, 18));
        
        // 获取绘图区
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        plot.setOutlineStroke(new BasicStroke(1.0f));
        
        // 设置轴标签字体
        plot.getDomainAxis().setLabelFont(new Font("微软雅黑", Font.PLAIN, 14));
        plot.getRangeAxis().setLabelFont(new Font("微软雅黑", Font.PLAIN, 14));
        plot.getDomainAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        plot.getRangeAxis().setTickLabelFont(new Font("Arial", Font.PLAIN, 12));
        
        // 设置图例字体
        chart.getLegend().setItemFont(new Font("微软雅黑", Font.PLAIN, 12));
        
        // 设置渲染器 - 只显示线条，不显示点
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setDefaultShapesVisible(false);  // 不显示点
        renderer.setDefaultLinesVisible(true);     // 显示线条
        renderer.setDefaultStroke(new BasicStroke(2.0f));
        plot.setRenderer(renderer);
        
        // 设置边距
        plot.setInsets(new RectangleInsets(10, 10, 10, 10));
    }
    
    /**
     * 生成箱线图（用于显示多次实验的统计分布）
     * 
     * @param experimentData 实验数据 (实验名称 -> makespan值列表)
     * @param outputPath 输出文件路径
     * @param title 图表标题
     * @throws IOException IO异常
     */
    public static void generateBoxPlot(
            java.util.Map<String, List<Double>> experimentData,
            String outputPath,
            String title) throws IOException {
        
        org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset dataset = 
            new org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset();
        
        for (java.util.Map.Entry<String, List<Double>> entry : experimentData.entrySet()) {
            dataset.add(entry.getValue(), "Makespan", entry.getKey());
        }
        
        // 创建箱线图
        JFreeChart chart = ChartFactory.createBoxAndWhiskerChart(
            title,
            "实验配置",
            "Makespan",
            dataset,
            true
        );
        
        // 自定义样式
        chart.setBackgroundPaint(Color.WHITE);
        chart.getTitle().setFont(new Font("微软雅黑", Font.BOLD, 18));
        
        org.jfree.chart.plot.CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1000, 600);
    }
}
