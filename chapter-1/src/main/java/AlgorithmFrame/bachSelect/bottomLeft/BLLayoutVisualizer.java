package AlgorithmFrame.bachSelect.bottomLeft;

import ProblemFrame.PlaceItem;
import ProblemFrame.Solution;
import ProblemFrame.Machine;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * BL算法平面排布图可视化器
 * 用于生成装箱结果的二维平面图
 */
public class BLLayoutVisualizer {
    
    private final List<Solution> solutions;
    private final Machine machine;
    
    public BLLayoutVisualizer(List<Solution> solutions, Machine machine) {
        this.solutions = solutions;
        this.machine = machine;
    }
    
    /**
     * 生成所有托盘的布局图
     * @param baseOutputPath 基础输出路径（不含扩展名）
     */
    public void generateAllLayouts(String baseOutputPath) throws IOException {
        System.out.println("开始生成平面排布图...");
        
        if (solutions == null || solutions.isEmpty()) {
            System.out.println("没有装箱方案，跳过布局图生成");
            return;
        }
        
        // 为每个托盘生成单独的图表
        for (int i = 0; i < solutions.size(); i++) {
            String outputPath = baseOutputPath + "_托盘" + (i + 1) + ".png";
            generateSingleLayout(i, solutions.get(i), outputPath);
        }
        
        System.out.println("✓ 所有平面排布图已生成完成");
    }
    
    /**
     * 生成单个托盘的布局图
     */
    private void generateSingleLayout(int trayIdx, Solution solution, String outputPath) 
            throws IOException {
        
        // 创建散点图
        JFreeChart chart = ChartFactory.createScatterPlot(
            String.format("托盘%d - BL算法装箱布局 (利用率: %.2f%%)", 
                         trayIdx + 1, solution.rate * 100),
            "X 坐标 (mm)",
            "Y 坐标 (mm)",
            new XYSeriesCollection() // 空的dataset，我们直接绘制矩形
        );
        
        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);
        
        // 设置坐标轴范围（稍微大于容器尺寸）
        double margin = machine.L * 0.05; // 5%的边距
        plot.getDomainAxis().setRange(-margin, machine.L + margin);
        plot.getRangeAxis().setRange(-margin, machine.W + margin);
        
        // 绘制容器边界
        drawContainerBoundary(plot);
        
        // 绘制每个物品
        for (int i = 0; i < solution.placeItemList.size(); i++) {
            PlaceItem item = solution.placeItemList.get(i);
            drawItemRectangle(plot, item, i);
        }
        
        // 添加统计信息
        addStatisticsInfo(plot, solution, trayIdx);
        
        // 设置中文字体
        setChineseFont(chart);
        
        // 保存图表
        File outputFile = new File(outputPath);
        // 确保父目录存在
        outputFile.getParentFile().mkdirs();
        ChartUtils.saveChartAsPNG(outputFile, chart, 1200, 1000);
        System.out.println("  ✓ 托盘" + (trayIdx + 1) + " 布局图已保存: " + outputPath);
    }
    
    /**
     * 绘制容器边界
     */
    private void drawContainerBoundary(XYPlot plot) {
        // 绘制容器边框
        Rectangle2D.Double containerRect = new Rectangle2D.Double(
            0, 0, machine.L, machine.W
        );
        
        XYShapeAnnotation containerAnnotation = new XYShapeAnnotation(
            containerRect, 
            new BasicStroke(3.0f), // 粗边框
            Color.BLACK, 
            null
        );
        plot.addAnnotation(containerAnnotation);
        
        // 添加容器尺寸标注
        XYTextAnnotation sizeLabel = new XYTextAnnotation(
            String.format("容器尺寸: %.0f × %.0f mm", machine.L, machine.W),
            machine.L * 0.5, 
            machine.W + machine.W * 0.03
        );
        sizeLabel.setPaint(Color.BLACK);
        sizeLabel.setFont(new Font("微软雅黑", Font.BOLD, 14));
        plot.addAnnotation(sizeLabel);
    }
    
    /**
     * 绘制单个物品的矩形
     */
    private void drawItemRectangle(XYPlot plot, PlaceItem item, int itemIndex) {
        // 物品尺寸
        double width = item.l;   // 长作为宽度
        double height = item.w;  // 宽作为高度
        
        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(
            item.x, item.y, width, height
        );
        
        // 获取颜色
        Color fillColor = getColorForItem(itemIndex);
        Color borderColor = fillColor.darker();
        
        // 创建XYShapeAnnotation来绘制矩形
        XYShapeAnnotation rectAnnotation = new XYShapeAnnotation(
            rect, 
            new BasicStroke(1.5f), // 矩形边框
            borderColor,           // 边框颜色
            fillColor              // 填充颜色
        );
        plot.addAnnotation(rectAnnotation);
        
        // 添加物品名称标签（居中）
        XYTextAnnotation nameLabel = new XYTextAnnotation(
            item.name,
            item.x + width / 2,
            item.y + height / 2
        );
        nameLabel.setPaint(Color.BLACK);
        nameLabel.setFont(new Font("微软雅黑", Font.BOLD, 11));
        plot.addAnnotation(nameLabel);
        
        // 如果物品被旋转，添加旋转标记
        if (item.isRotate) {
            XYTextAnnotation rotateMark = new XYTextAnnotation(
                "↻",
                item.x + width * 0.85,
                item.y + height * 0.85
            );
            rotateMark.setPaint(Color.RED);
            rotateMark.setFont(new Font("宋体", Font.BOLD, 14));
            plot.addAnnotation(rotateMark);
        }
        
        // 添加尺寸标注（小字）
        XYTextAnnotation sizeLabel = new XYTextAnnotation(
            String.format("%.0f×%.0f", width, height),
            item.x + width / 2,
            item.y + height * 0.2
        );
        sizeLabel.setPaint(Color.GRAY);
        sizeLabel.setFont(new Font("宋体", Font.PLAIN, 9));
        plot.addAnnotation(sizeLabel);
    }
    
    /**
     * 添加统计信息
     */
    private void addStatisticsInfo(XYPlot plot, Solution solution, int trayIdx) {
        double infoX = machine.L * 0.02;
        double infoY = machine.W * 0.96;
        double lineHeight = machine.W * 0.04;
        
        // 托盘编号
        addInfoText(plot, String.format("【托盘 %d】", trayIdx + 1), 
                   infoX, infoY, Color.BLACK, Font.BOLD, 14);
        
        // 物品数量
        addInfoText(plot, String.format("物品数量: %d", solution.placeItemList.size()), 
                   infoX, infoY - lineHeight, Color.BLUE, Font.PLAIN, 12);
        
        // 利用率
        addInfoText(plot, String.format("利用率: %.2f%%", solution.rate * 100), 
                   infoX, infoY - lineHeight * 2, Color.BLUE, Font.PLAIN, 12);
        
        // 总面积
        addInfoText(plot, String.format("使用面积: %.0f mm²", solution.totalS), 
                   infoX, infoY - lineHeight * 3, Color.BLUE, Font.PLAIN, 12);
        
        // 最大高度
        addInfoText(plot, String.format("最大高度: %.2f mm", solution.maxG), 
                   infoX, infoY - lineHeight * 4, Color.BLUE, Font.PLAIN, 12);
    }
    
    /**
     * 添加信息文本
     */
    private void addInfoText(XYPlot plot, String text, double x, double y, 
                           Color color, int fontStyle, int fontSize) {
        XYTextAnnotation annotation = new XYTextAnnotation(text, x, y);
        annotation.setPaint(color);
        annotation.setFont(new Font("微软雅黑", fontStyle, fontSize));
        plot.addAnnotation(annotation);
    }
    
    /**
     * 为物品获取颜色
     */
    private Color getColorForItem(int itemIndex) {
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
            new Color(220, 180, 180), // 浅玫瑰
            new Color(180, 180, 220), // 浅蓝紫
            new Color(200, 220, 200), // 浅灰绿
        };
        return colors[itemIndex % colors.length];
    }
    
    /**
     * 设置中文字体
     */
    private void setChineseFont(JFreeChart chart) {
        // 设置标题字体
        Font titleFont = new Font("微软雅黑", Font.BOLD, 18);
        chart.getTitle().setFont(titleFont);
        
        // 设置轴标签字体
        Font labelFont = new Font("微软雅黑", Font.PLAIN, 14);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.getDomainAxis().setLabelFont(labelFont);
        plot.getRangeAxis().setLabelFont(labelFont);
        
        // 设置刻度字体
        Font tickFont = new Font("宋体", Font.PLAIN, 12);
        plot.getDomainAxis().setTickLabelFont(tickFont);
        plot.getRangeAxis().setTickLabelFont(tickFont);
    }
}

