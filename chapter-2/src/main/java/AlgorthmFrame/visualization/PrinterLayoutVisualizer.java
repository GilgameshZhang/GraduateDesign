package AlgorthmFrame.visualization;

import ProblemFrame.Solution;
import ProgramEntity.PlaceItem;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Machine.Machine;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYShapeAnnotation;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.ui.TextAnchor;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * 打印机布局可视化器
 * 专门用于可视化打印批次中的零件布局
 */
public class PrinterLayoutVisualizer {

    private final ProblemFrame.Chromosome chromosome;
    private final Machine[] machines;

    public PrinterLayoutVisualizer(ProblemFrame.Chromosome chromosome, Machine[] machines) {
        this.chromosome = chromosome;
        this.machines = machines;
    }

    /**
     * 生成打印机布局图表 - 为每个批次生成单独的图表
     */
    public void generatePrinterLayoutChart(String baseOutputPath) throws IOException {
        System.out.println("生成打印批次布局图...");

        int numPrinters = 0;
        for (Machine machine : machines) {
            if (machine instanceof PrintMachine) {
                numPrinters++;
            }
        }

        if (numPrinters == 0 || chromosome.printSolution == null) {
            System.out.println("没有打印机或打印批次数据，跳过布局图生成");
            return;
        }

        // 为每个打印机的每个批次生成单独的图表
        for (int printerIdx = 0; printerIdx < numPrinters; printerIdx++) {
            if (chromosome.printSolution[printerIdx] != null &&
                !chromosome.printSolution[printerIdx].isEmpty()) {

                List<ProgramEntity.Solution> batches = chromosome.printSolution[printerIdx];
                for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                    String outputPath = baseOutputPath.replace(".png",
                        "_printer" + (printerIdx + 1) + "_batch" + (batchIdx + 1) + ".png");

                    generateSingleBatchLayout(
                        printerIdx, batchIdx, batches.get(batchIdx), outputPath
                    );
                }
            }
        }

        System.out.println("打印批次布局图已保存");
    }

    /**
     * 生成单个批次的布局图 - 显示该批次中所有零件的矩形布局
     */
    private void generateSingleBatchLayout(int printerIdx, int batchIdx,
                                         ProgramEntity.Solution batch, String outputPath) throws IOException {
        PrintMachine printer = (PrintMachine) machines[printerIdx];

        // 创建自定义图表 - 直接绘制矩形
        JFreeChart chart = ChartFactory.createScatterPlot(
            String.format("打印机%d - 批次%d 零件布局", printerIdx + 1, batchIdx + 1),
            "X 坐标",
            "Y 坐标",
            new XYSeriesCollection() // 空的dataset，因为我们要直接绘制
        );

        // 获取绘图区域
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setDomainGridlinePaint(Color.LIGHT_GRAY);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // 设置坐标轴范围（稍微大于打印平台尺寸）
        double margin = 20.0;
        plot.getDomainAxis().setRange(-margin, printer.L + margin);
        plot.getRangeAxis().setRange(-margin, printer.W + margin);

        // 直接在图表上绘制零件矩形
        for (int i = 0; i < batch.placeItemList.size(); i++) {
            PlaceItem item = batch.placeItemList.get(i);
            drawPartRectangle(plot, item, i);
        }

        // 添加批次信息
        String batchInfo = String.format("批次%d: %.1f-%.1f (%d零件)",
            batchIdx + 1, batch.startTime, batch.endTime, batch.placeItemList.size());

        plot.addAnnotation(new org.jfree.chart.annotations.XYTextAnnotation(
            batchInfo, printer.L * 0.1, printer.W * 0.95
        ));

        // 添加打印平台边界
        drawPlatformBoundary(plot, printer);

        // 设置中文字体
        setChineseFont(chart);

        // 保存图表
        ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1000, 800);
        System.out.println("批次布局图已保存: " + outputPath);
    }

    /**
     * 绘制单个零件的矩形
     */
    private void drawPartRectangle(XYPlot plot, PlaceItem item, int itemIndex) {
        // 严格按照零件的长宽属性进行绘图，不考虑旋转
        double width = item.l;   // 长作为宽度
        double height = item.w;  // 宽作为高度

        // 创建矩形
        Rectangle2D.Double rect = new Rectangle2D.Double(
            item.x, item.y, width, height
        );

        // 获取颜色
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
        // label.setTextAnchor(TextAnchor.CENTER); // 暂时注释
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
     * 绘制打印平台边界
     */
    private void drawPlatformBoundary(XYPlot plot, PrintMachine printer) {
        // 绘制平台边框
        Rectangle2D.Double platformRect = new Rectangle2D.Double(
            0, 0, printer.L, printer.W
        );

        XYShapeAnnotation platformAnnotation = new XYShapeAnnotation(
            platformRect, new BasicStroke(2.0f), Color.BLACK, null
        );
        plot.addAnnotation(platformAnnotation);

        // 添加平台尺寸标注
        XYTextAnnotation sizeLabel = new XYTextAnnotation(
            String.format("平台尺寸: %.0fx%.0f", printer.L, printer.W),
            printer.L * 0.5, printer.W + 10
        );
        sizeLabel.setPaint(Color.BLACK);
        sizeLabel.setFont(new Font("宋体", Font.PLAIN, 12));
        // sizeLabel.setTextAnchor(TextAnchor.CENTER); // 暂时注释
        plot.addAnnotation(sizeLabel);
    }

    /**
     * 为零件获取颜色
     */
    private Color getColorForPart(int partIndex) {
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
     * 为批次获取颜色（保留原方法以防其他地方使用）
     */
    private Color getColorForBatch(int batchIndex) {
        Color[] colors = {
            Color.RED, Color.BLUE, Color.GREEN, Color.ORANGE,
            Color.MAGENTA, Color.CYAN, Color.PINK, Color.YELLOW
        };
        return colors[batchIndex % colors.length];
    }


    /**
     * 设置中文字体
     */
    private void setChineseFont(JFreeChart chart) {
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
}
