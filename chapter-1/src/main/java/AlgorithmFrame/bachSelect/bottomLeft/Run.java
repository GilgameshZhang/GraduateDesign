package AlgorithmFrame.bachSelect.bottomLeft;

import ProblemFrame.*;
import util.ReadDataUtil;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Run {
    public static void main(String[] args) throws IOException {
        // ============ 配置参数 ============
        // 数据路径
        String itemPath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\PrintItem\\printItem_10\\printItem_10_01";
        String machinePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\Machine\\machine_2";
        
        // ============ 读取数据 ============
        String[] pathList = new String[2];
        pathList[0] = itemPath;
        pathList[1] = machinePath;
        Input input = new ReadDataUtil().getInput(pathList);
        
        System.out.println("========================================");
        System.out.println("最左最下规则（BL）装箱算法测试");
        System.out.println("========================================");
        System.out.println("物品数量: " + input.itemList.size());
        System.out.println("机器数量: " + input.machineList.size());
        System.out.println("机器尺寸: " + input.machineList.get(0).L + " x " + input.machineList.get(0).W);
        System.out.println("是否允许旋转: " + input.isRotateEnable);
        System.out.println("========================================\n");
        
        // ============ 测试不同排序策略 ============
        testStrategy("原始顺序", input, SortStrategy.ORIGINAL);
        testStrategy("面积降序", input, SortStrategy.AREA_DESC);
        testStrategy("长边降序", input, SortStrategy.LENGTH_DESC);
        testStrategy("周长降序", input, SortStrategy.PERIMETER_DESC);
        testStrategy("宽度降序", input, SortStrategy.WIDTH_DESC);
        
        System.out.println("\n========================================");
        System.out.println("测试完成！");
        System.out.println("========================================");
    }
    
    /**
     * 测试某种排序策略
     */
    private static void testStrategy(String strategyName, Input input, SortStrategy strategy) throws IOException {
        System.out.println("\n【" + strategyName + "】");
        
        // 准备物品数组
        Item[] items = input.itemList.toArray(new Item[0]);
        
        // 应用排序策略
        sortItems(items, strategy);
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        // 创建BL算法实例（使用第一个机器）
        Machine machine = input.machineList.get(0);
        BottomLeftPacking blPacking = new BottomLeftPacking(
            machine.L, 
            machine.W, 
            items, 
            input.isRotateEnable
        );
        
        // 执行装箱
        List<Solution> solutions = blPacking.packings();
        
        // 计算总用时
        long elapsedTime = System.currentTimeMillis() - startTime;
        
        // ============ 输出结果 ============
        System.out.println("求解用时: " + elapsedTime / 1000.0 + " s");
        System.out.println("使用托盘数: " + solutions.size());
        
        double totalRate = 0.0;
        int totalItems = 0;
        
        for (int i = 0; i < solutions.size(); i++) {
            Solution solution = solutions.get(i);
            System.out.println("  托盘" + (i + 1) + ": 物品数=" + solution.placeItemList.size() 
                             + ", 利用率=" + String.format("%.4f", solution.rate)
                             + ", 最高零件=" + String.format("%.2f", solution.maxG));
            totalRate += solution.rate;
            totalItems += solution.placeItemList.size();
        }
        
        double avgRate = solutions.size() > 0 ? totalRate / solutions.size() : 0;
        System.out.println("平均利用率: " + String.format("%.4f", avgRate));
        System.out.println("放置物品总数: " + totalItems + " / " + items.length);
        
        // ============ 保存可视化数据 ============
        saveVisualizationData(strategyName, solutions);
        
        // ============ 生成平面排布图 ============
        try {
            BLLayoutVisualizer visualizer = new BLLayoutVisualizer(solutions, machine);
            String layoutPath = "visualization_results/BL_" + strategyName.replace(" ", "_");
            visualizer.generateAllLayouts(layoutPath);
        } catch (Exception e) {
            System.err.println("⚠️ 生成平面排布图失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 排序物品
     */
    private static void sortItems(Item[] items, SortStrategy strategy) {
        switch (strategy) {
            case AREA_DESC:
                // 按面积降序
                Arrays.sort(items, (o1, o2) -> -Double.compare(o1.l * o1.w, o2.l * o2.w));
                break;
            case LENGTH_DESC:
                // 按长边降序
                Arrays.sort(items, (o1, o2) -> -Double.compare(Math.max(o1.l, o1.w), Math.max(o2.l, o2.w)));
                break;
            case WIDTH_DESC:
                // 按宽度降序
                Arrays.sort(items, (o1, o2) -> -Double.compare(o1.w, o2.w));
                break;
            case PERIMETER_DESC:
                // 按周长降序
                Arrays.sort(items, (o1, o2) -> -Double.compare(o1.l + o1.w, o2.l + o2.w));
                break;
            case ORIGINAL:
            default:
                // 保持原始顺序
                break;
        }
    }
    
    /**
     * 保存可视化数据
     */
    private static void saveVisualizationData(String strategyName, List<Solution> solutions) throws IOException {
        String filename = "visualization_results/BL_" + strategyName.replace(" ", "_") + ".txt";
        FileWriter writer = new FileWriter(filename);
        
        writer.write("策略: " + strategyName + "\n");
        writer.write("托盘数: " + solutions.size() + "\n\n");
        
        for (int i = 0; i < solutions.size(); i++) {
            Solution solution = solutions.get(i);
            writer.write("========== 托盘 " + (i + 1) + " ==========\n");
            writer.write("物品数: " + solution.placeItemList.size() + "\n");
            writer.write("利用率: " + solution.rate + "\n");
            writer.write("最高零件: " + solution.maxG + "\n\n");
            
            for (PlaceItem item : solution.placeItemList) {
                writer.write(String.format("%s: x=%.2f, y=%.2f, l=%.2f, w=%.2f, h=%.2f, rotate=%b\n",
                    item.name, item.x, item.y, item.l, item.w, item.h, item.isRotate));
            }
            writer.write("\n");
        }
        
        writer.close();
    }
    
    /**
     * 排序策略枚举
     */
    private enum SortStrategy {
        ORIGINAL,       // 原始顺序
        AREA_DESC,      // 面积降序
        LENGTH_DESC,    // 长边降序
        WIDTH_DESC,     // 宽度降序
        PERIMETER_DESC  // 周长降序
    }
}

