package AlgorithmFrame.nsgaii;

import ProblemFrame.MOIndividual;
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
 * Pareto前沿可视化工具
 * 
 * 生成Pareto前沿图：
 * - 横轴：能耗 (kWh)
 * - 纵轴：Cmax (最大完工时间)
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class ParetoFrontVisualizer {
    
    /**
     * 生成Pareto前沿图并保存
     * 
     * @param paretoFront Pareto前沿解集
     * @param outputPath 输出文件路径（PNG格式）
     * @param width 图片宽度
     * @param height 图片高度
     */
    public static void generateParetoFrontChart(List<MOIndividual> paretoFront,
                                                 String outputPath,
                                                 int width,
                                                 int height) throws IOException {
        if (paretoFront == null || paretoFront.isEmpty()) {
            System.err.println("⚠️ Pareto前沿为空，无法生成图表");
            return;
        }
        
        // 创建数据集
        XYSeries series = new XYSeries("Pareto前沿");
        
        for (MOIndividual ind : paretoFront) {
            double energy = ind.objectives[1];  // 能耗（横轴）
            double cmax = ind.objectives[0];    // Cmax（纵轴）
            series.add(energy, cmax);
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection(series);
        
        // 创建图表
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Pareto前沿图",           // 标题
            "能耗 (kWh)",            // X轴标签
            "Cmax (最大完工时间)",   // Y轴标签
            dataset,                 // 数据集
            PlotOrientation.VERTICAL,
            true,                    // 显示图例
            true,                    // 工具提示
            false                    // URLs
        );
        
        // 自定义图表样式
        customizeChart(chart);
        
        // 保存为PNG文件
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ChartUtils.saveChartAsPNG(outputFile, chart, width, height);
        
        System.out.println("✅ Pareto前沿图已保存: " + outputPath);
    }
    
    /**
     * 自定义图表样式
     */
    private static void customizeChart(JFreeChart chart) {
        XYPlot plot = chart.getXYPlot();
        
        // 设置背景
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置渲染器
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        renderer.setSeriesLinesVisible(0, true);   // 显示连线
        renderer.setSeriesShapesVisible(0, true);  // 显示点
        renderer.setSeriesPaint(0, new Color(0, 102, 204));  // 蓝色
        renderer.setSeriesStroke(0, new BasicStroke(2.0f));
        
        // 设置点的形状和大小
        renderer.setSeriesShape(0, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        
        plot.setRenderer(renderer);
        
        // 设置字体
        chart.getTitle().setFont(new Font("微软雅黑", Font.BOLD, 18));
        plot.getDomainAxis().setLabelFont(new Font("微软雅黑", Font.PLAIN, 14));
        plot.getRangeAxis().setLabelFont(new Font("微软雅黑", Font.PLAIN, 14));
        chart.getLegend().setItemFont(new Font("微软雅黑", Font.PLAIN, 12));
    }
    
    /**
     * 生成多条曲线的Pareto前沿对比图
     */
    public static void generateMultiParetoFrontChart(List<List<MOIndividual>> paretoFronts,
                                                     List<String> labels,
                                                     String outputPath,
                                                     int width,
                                                     int height) throws IOException {
        if (paretoFronts == null || paretoFronts.isEmpty()) {
            System.err.println("⚠️ 没有Pareto前沿数据");
            return;
        }
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        
        for (int i = 0; i < paretoFronts.size(); i++) {
            String label = (labels != null && i < labels.size()) ? labels.get(i) : "前沿" + (i + 1);
            XYSeries series = new XYSeries(label);
            
            for (MOIndividual ind : paretoFronts.get(i)) {
                double energy = ind.objectives[1];
                double cmax = ind.objectives[0];
                series.add(energy, cmax);
            }
            
            dataset.addSeries(series);
        }
        
        // 创建图表
        JFreeChart chart = ChartFactory.createScatterPlot(
            "Pareto前沿对比",
            "能耗 (kWh)",
            "Cmax (最大完工时间)",
            dataset,
            PlotOrientation.VERTICAL,
            true, true, false
        );
        
        // 自定义样式
        XYPlot plot = chart.getXYPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        Color[] colors = {
            new Color(0, 102, 204),   // 蓝色
            new Color(204, 0, 0),     // 红色
            new Color(0, 153, 0),     // 绿色
            new Color(153, 0, 153)    // 紫色
        };
        
        for (int i = 0; i < dataset.getSeriesCount(); i++) {
            renderer.setSeriesLinesVisible(i, true);
            renderer.setSeriesShapesVisible(i, true);
            renderer.setSeriesPaint(i, colors[i % colors.length]);
            renderer.setSeriesStroke(i, new BasicStroke(2.0f));
            renderer.setSeriesShape(i, new java.awt.geom.Ellipse2D.Double(-4, -4, 8, 8));
        }
        
        plot.setRenderer(renderer);
        
        // 保存
        File outputFile = new File(outputPath);
        outputFile.getParentFile().mkdirs();
        ChartUtils.saveChartAsPNG(outputFile, chart, width, height);
        
        System.out.println("✅ Pareto前沿对比图已保存: " + outputPath);
    }
}

