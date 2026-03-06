package util;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 图表生成工具类（第一章专用）- 参考第二章画风
 */
public class ChartGenerator {
    
    /**
     * 生成收敛曲线图（迭代图）- 完全匹配第二章风格
     */
    public static void generateConvergenceCurve(List<Double> cmaxHistory, 
                                               String outputPath, 
                                               String title) throws IOException {
        // 创建数据集
        XYSeries series = new XYSeries("最优Cmax");
        for (int i = 0; i < cmaxHistory.size(); i++) {
            series.add(i, cmaxHistory.get(i));
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        dataset.addSeries(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,                          // 标题
            "迭代次数（代数）",              // X轴标签
            "最优Cmax",                     // Y轴标签
            dataset,                        // 数据集
            PlotOrientation.VERTICAL,       // 垂直方向
            true,                           // 显示图例
            true,                           // 显示工具提示
            false                           // 不显示URL
        );
        
        // 获取绘图区域
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置线条样式 - 完全匹配第二章
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesPaint(0, Color.BLUE);              // 蓝色线条
        renderer.setSeriesStroke(0, new BasicStroke(2.0f)); // 2.0粗细
        renderer.setSeriesShapesVisible(0, false);          // 不显示数据点
        plot.setRenderer(renderer);
        
        // 设置X轴范围
        plot.getDomainAxis().setRange(-1, cmaxHistory.size());
        
        // 设置Y轴范围（10% padding）
        double minCmax = cmaxHistory.stream().min(Double::compare).orElse(0.0);
        double maxCmax = cmaxHistory.stream().max(Double::compare).orElse(1.0);
        double padding = Math.max((maxCmax - minCmax) * 0.1, 500.0) ;
        plot.getRangeAxis().setRange(minCmax - padding, maxCmax + padding);
        
        // 设置中文字体 - 完全匹配第二章
        setChineseFont(chart);
        
        // 保存图表 - 高分辨率1920x1080
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1920, 1080);
    }
    
    /**
     * 生成多条收敛曲线对比图 - 完全匹配第二章风格
     */
    public static void generateMultipleConvergenceCurves(
            java.util.Map<String, List<Double>> historyMap,
            String outputPath, 
            String title) throws IOException {
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        double minCmax = Double.MAX_VALUE;
        double maxCmax = Double.MIN_VALUE;
        
        // 为每组实验创建一个数据系列
        for (java.util.Map.Entry<String, List<Double>> entry : historyMap.entrySet()) {
            String experimentName = entry.getKey();
            List<Double> history = entry.getValue();
            
            XYSeries series = new XYSeries(experimentName);
            for (int i = 0; i < history.size(); i++) {
                double value = history.get(i);
                series.add(i, value);  // 从0开始，与单曲线保持一致
                minCmax = Math.min(minCmax, value);
                maxCmax = Math.max(maxCmax, value);
            }
            dataset.addSeries(series);
        }
        
        // 创建图表
        JFreeChart chart = ChartFactory.createXYLineChart(
            title,
            "迭代次数（代数）",          // X轴标签
            "最优Cmax",                  // Y轴标签
            dataset,
            PlotOrientation.VERTICAL,
            true,                        // 显示图例
            true,                        // 显示工具提示
            false                        // 不显示URL
        );
        
        // 获取绘图区域
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置线条样式 - 完全匹配第二章
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(2.0f)); // 2.0粗细
            renderer.setSeriesShapesVisible(i, false);          // 不显示数据点
        }
        plot.setRenderer(renderer);
        
        // 设置Y轴范围（10% padding）
        double range = maxCmax - minCmax;
        if (range < 0.001) {
            double minRange = Math.max(1.0, minCmax * 0.005);
            plot.getRangeAxis().setRange(minCmax - minRange, maxCmax + minRange);
        } else {
            double padding = range * 0.1;  // 10% padding，与单曲线保持一致
            plot.getRangeAxis().setRange(minCmax - padding, maxCmax + padding);
        }
        
        // 设置中文字体 - 完全匹配第二章
        setChineseFont(chart);
        
        // 保存图表 - 高分辨率1920x1080
        File outputFile = new File(outputPath);
        ChartUtils.saveChartAsPNG(outputFile, chart, 1920, 1080);
    }
    
    /**
     * 设置中文字体 - 完全匹配第二章
     */
    private static void setChineseFont(JFreeChart chart) {
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
        
        // 设置图例字体（如果有）
        if (chart.getLegend() != null) {
            chart.getLegend().setItemFont(new Font("宋体", Font.PLAIN, 12));
        }
    }
}
