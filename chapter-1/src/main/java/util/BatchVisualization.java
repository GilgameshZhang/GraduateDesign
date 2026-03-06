package util;

import ProblemFrame.*;
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
import java.util.*;
import java.util.List;

/**
 * 第一章批次可视化工具 - 完全匹配第二章画风
 * 生成甘特图和批次布局图
 */
public class BatchVisualization {
    
    /**
     * 生成甘特图 - 完全匹配第二章风格（横轴时间，纵轴机器）
     */
    public static void generateGanttChart(List<BatchResult> solutions, 
                                         String outputPath) throws IOException {
        System.out.println("生成甘特图...");
        
        // 创建自定义XY图表（与第二章一致）
        JFreeChart chart = ChartFactory.createScatterPlot(
            "批次调度甘特图",     // 标题
            "时间",              // X轴标签
            "机器",              // Y轴标签
            new XYSeriesCollection(), // 空的dataset
            PlotOrientation.VERTICAL,
            false,              // 不显示图例
            true,               // 显示工具提示
            false               // 不显示URL
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        int machineCount = solutions.size();
        
        // 计算时间范围
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;
        
        // 为每台机器绘制批次矩形
        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            BatchResult batchResult = solutions.get(machineIdx);
            
            if (batchResult.solutions != null && !batchResult.solutions.isEmpty()) {
                for (int batchIdx = 0; batchIdx < batchResult.solutions.size(); batchIdx++) {
                    double startTime = batchResult.startTimes.get(batchIdx);
                    double endTime = batchResult.endTimes.get(batchIdx);
                    
                    minTime = Math.min(minTime, startTime);
                    maxTime = Math.max(maxTime, endTime);
                    
                    // 绘制批次矩形
                    drawBatchRectangle(plot, machineIdx, batchIdx + 1, startTime, endTime);
                }
            }
        }
        
        // 设置Y轴范围（机器轴）
        plot.getRangeAxis().setRange(-0.5, machineCount - 0.5);
        plot.getRangeAxis().setTickLabelsVisible(false);
        
        // 设置X轴范围（时间轴），留出空间显示机器标签
        if (maxTime > minTime) {
            double timePadding = (maxTime - minTime) * 0.15;
            plot.getDomainAxis().setRange(minTime - timePadding, maxTime + timePadding);
        } else {
            plot.getDomainAxis().setRange(-10, 10);
        }
        
        // 添加机器标签（与第二章一致）
        double labelXPosition = minTime - (maxTime - minTime) * 0.12;
        for (int machineIdx = 0; machineIdx < machineCount; machineIdx++) {
            XYTextAnnotation machineLabel = new XYTextAnnotation(
                "机器 " + (machineIdx + 1),
                labelXPosition,
                machineIdx
            );
            machineLabel.setPaint(Color.BLACK);
            machineLabel.setFont(new Font("宋体", Font.BOLD, 12));
            plot.addAnnotation(machineLabel);
        }
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 保存图表 - 高分辨率1920x1080
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1920, 1080);
        System.out.println("甘特图已保存: " + outputPath);
    }
    
    /**
     * 绘制单个批次的矩形
     */
    private static void drawBatchRectangle(XYPlot plot, int machineIdx, int batchNo, 
                                          double startTime, double endTime) {
        // 计算矩形位置和大小 - 横轴时间，纵轴机器
        double x = startTime;
        double y = machineIdx - 0.3;  // 机器位置（稍微下移以居中）
        double width = endTime - startTime;
        double height = 0.6;  // 固定高度
        
        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(x, y, width, height);
        
        // 获取颜色（与第二章颜色方案一致）
        Color color = getColorForBatch(machineIdx);
        
        // 创建XYShapeAnnotation来绘制矩形
        XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
            rect, new BasicStroke(1.0f), Color.BLACK, color
        );
        rectAnnotation.setToolTipText(String.format("B%d-%d: %.1f-%.1f (%.1f)",
            machineIdx + 1, batchNo, startTime, endTime, width));
        
        plot.addAnnotation(rectAnnotation);
        
        // 添加批次标签（靠近左侧）
        String label = "B" + (machineIdx + 1) + "-" + batchNo;
        XYTextAnnotation textLabel = new XYTextAnnotation(
            label,
            x + width * 0.1,  // 靠近左侧
            y + height / 2    // 垂直居中
        );
        textLabel.setPaint(Color.BLACK);
        textLabel.setFont(new Font("宋体", Font.BOLD, 10));
        plot.addAnnotation(textLabel);
    }
    
    /**
     * 为批次获取颜色（使用第二章的颜色方案）
     */
    private static Color getColorForBatch(int machineIndex) {
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE, Color.MAGENTA,
            Color.CYAN, Color.PINK, Color.YELLOW, Color.GRAY, new Color(128, 0, 128),
            new Color(0, 128, 128), new Color(128, 128, 0), new Color(255, 128, 0),
            new Color(128, 255, 0), new Color(0, 255, 128), new Color(0, 128, 255)
        };
        return colors[machineIndex % colors.length];
    }
    
    /**
     * 生成批次布局图（矩形装箱可视化）- 完全匹配第二章风格
     */
    public static void generateBatchLayoutChart(BatchResult batchResult, 
                                               int machineId,
                                               Machine machine,
                                               String baseOutputPath) throws IOException {
        if (batchResult.solutions == null || batchResult.solutions.isEmpty()) {
            System.out.println("机器 " + (machineId + 1) + " 没有批次数据，跳过布局图生成");
            return;
        }
        
        System.out.println("生成机器 " + (machineId + 1) + " 的批次布局图...");
        
        // 为每个批次生成单独的图表（与第二章一致）
        for (int batchIdx = 0; batchIdx < batchResult.solutions.size(); batchIdx++) {
            // 生成完整的文件路径：basePath + _machine{N}_batch{M}.png
            String batchOutputPath = baseOutputPath + "_machine" + (machineId + 1) + 
                                    "_batch" + (batchIdx + 1) + ".png";
            
            generateSingleBatchLayout(
                machineId, batchIdx, batchResult.solutions.get(batchIdx),
                batchResult.startTimes.get(batchIdx),
                batchResult.endTimes.get(batchIdx),
                machine, batchOutputPath
            );
        }
    }
    
    /**
     * 生成单个批次的布局图 - 完全匹配第二章风格
     */
    private static void generateSingleBatchLayout(int machineId, int batchIdx,
                                                 Solution batch, double startTime, double endTime,
                                                 Machine machine, String outputPath) throws IOException {
        // 创建自定义图表 - 直接绘制矩形（与第二章一致）
        JFreeChart chart = ChartFactory.createScatterPlot(
            String.format("机器%d - 批次%d 零件布局", machineId + 1, batchIdx + 1),
            "X 坐标",
            "Y 坐标",
            new XYSeriesCollection()
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置坐标轴范围（稍微大于打印平台尺寸）
        double margin = 20.0;
        plot.getDomainAxis().setRange(-margin, machine.L + margin);
        plot.getRangeAxis().setRange(-margin, machine.W + margin);
        
        // 绘制零件矩形
        for (int i = 0; i < batch.placeItemList.size(); i++) {
            PlaceItem item = batch.placeItemList.get(i);
            drawPartRectangle(plot, item, i);
        }
        
        // 添加批次信息
        String batchInfo = String.format("批次%d: %.1f-%.1f (%d零件, 利用率%.1f%%)",
            batchIdx + 1, startTime, endTime, batch.placeItemList.size(), batch.rate * 100);
        
        XYTextAnnotation infoAnnotation = new XYTextAnnotation(
            batchInfo, machine.L * 0.1, machine.W * 0.95
        );
        infoAnnotation.setFont(new Font("宋体", Font.PLAIN, 12));
        plot.addAnnotation(infoAnnotation);
        
        // 添加打印平台边界
        drawPlatformBoundary(plot, machine);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 保存图表 - 1000x800分辨率（与第二章一致）
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 800);
        System.out.println("批次布局图已保存: " + outputPath);
    }
    
    /**
     * 绘制单个零件的矩形 - 完全匹配第二章风格
     */
    private static void drawPartRectangle(XYPlot plot, PlaceItem item, int itemIndex) {
        // 严格按照零件的长宽属性进行绘图
        double width = item.l;
        double height = item.w;
        
        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(
            item.x, item.y, width, height
        );
        
        // 获取颜色（使用第二章的浅色系颜色）
        Color color = getColorForPart(itemIndex);
        
        // 创建XYShapeAnnotation来绘制矩形
        XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
            rect, null, null, color
        );
        rectAnnotation.setToolTipText(String.format("J%s (%.0fx%.0f)",
            item.name, width, height));
        
        plot.addAnnotation(rectAnnotation);
        
        // 添加零件标签
        XYTextAnnotation label = new XYTextAnnotation(
            "J" + item.name,
            item.x + width / 2,
            item.y + height / 2
        );
        label.setPaint(Color.BLACK);
        label.setFont(new Font("宋体", Font.BOLD, 10));
        plot.addAnnotation(label);
        
        // 添加旋转标记（如果零件被旋转）
        if (item.isRotate) {
            XYTextAnnotation rotateMark = new XYTextAnnotation(
                "↻",
                item.x + width - 5,
                item.y + height - 5
            );
            rotateMark.setPaint(Color.RED);
            rotateMark.setFont(new Font("宋体", Font.BOLD, 12));
            plot.addAnnotation(rotateMark);
        }
    }
    
    /**
     * 绘制打印平台边界 - 完全匹配第二章风格
     */
    private static void drawPlatformBoundary(XYPlot plot, Machine machine) {
        // 绘制平台边框
        Rectangle2D.Double platformRect = new Rectangle2D.Double(
            0, 0, machine.L, machine.W
        );
        
        XYShapeAnnotation platformAnnotation = new XYShapeAnnotation(
            platformRect, new BasicStroke(2.0f), Color.BLACK, null
        );
        plot.addAnnotation(platformAnnotation);
        
        // 添加平台尺寸标注
        XYTextAnnotation sizeLabel = new XYTextAnnotation(
            String.format("平台尺寸: %.0fx%.0f", machine.L, machine.W),
            machine.L * 0.5, machine.W + 10
        );
        sizeLabel.setPaint(Color.BLACK);
        sizeLabel.setFont(new Font("宋体", Font.PLAIN, 12));
        plot.addAnnotation(sizeLabel);
    }
    
    /**
     * 为零件获取颜色 - 使用第二章的浅色系颜色方案
     */
    private static Color getColorForPart(int partIndex) {
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
        return colors[partIndex % colors.length];
    }
    
    /**
     * 设置中文字体 - 完全匹配第二章
     */
    private static void setChineseFont(JFreeChart chart) {
        // 设置标题字体
        Font titleFont = new Font("宋体", Font.BOLD, 16);
        chart.getTitle().setFont(titleFont);
        
        // 设置轴标签字体
        Font labelFont = new Font("宋体", Font.PLAIN, 14);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setLabelFont(labelFont);
        plot.getRangeAxis().setLabelFont(labelFont);
        
        // 设置刻度字体
        Font tickFont = new Font("宋体", Font.PLAIN, 12);
        plot.getDomainAxis().setTickLabelFont(tickFont);
        plot.getRangeAxis().setTickLabelFont(tickFont);
    }
    
    /**
     * 生成所有机器的批次布局图 - 为每个批次生成独立图表
     */
    public static void generateAllBatchLayouts(List<BatchResult> solutions, 
                                              Machine[] machines,
                                              String basePath) throws IOException {
        System.out.println("开始生成所有机器的批次布局图...");
        
        for (int i = 0; i < solutions.size(); i++) {
            generateBatchLayoutChart(solutions.get(i), i, machines[i], basePath);
        }
        
        System.out.println("所有批次布局图生成完成");
    }
}
