package AlgorithmFrame.bachSelect.bottomLeft;

import ProblemFrame.Item;
import ProblemFrame.PlaceItem;
import ProblemFrame.Solution;

import java.util.ArrayList;
import java.util.List;

import static util.compareUtil.compareDouble;

/**
 * 最左最下规则（Bottom-Left Rule）装箱算法
 * 核心思想：每个物品先尽可能向下移动，再尽可能向左移动
 */
public class BottomLeftPacking {
    
    // 容器宽度
    private double W;
    // 容器高度
    private double H;
    // 待放置的物品数组
    private Item[] items;
    // 是否允许旋转
    private boolean isRotateEnable;
    
    public BottomLeftPacking() {
    }
    
    public BottomLeftPacking(double w, double h, Item[] items, boolean isRotateEnable) {
        this.W = w;
        this.H = h;
        this.items = items;
        this.isRotateEnable = isRotateEnable;
    }
    
    /**
     * 单盘装箱
     * @return 装箱方案
     */
    public Solution packing() {
        // 存放已放置的物品
        List<PlaceItem> placeItemList = new ArrayList<>();
        // 总面积
        double totalS = 0.0;
        // 总竖直高度
        double totalG = 0.0;
        
        // 按照给定顺序依次放置物品
        for (Item item : items) {
            // 尝试不旋转和旋转两种情况，选择最优位置
            PlaceItem bestPlace = findBestPosition(item, placeItemList);
            
            if (bestPlace != null) {
                placeItemList.add(bestPlace);
                totalS += (item.l * item.w);
                totalG += item.h;
            }
        }
        
        // 计算放置零件数值高度方差
        double avgG = placeItemList.size() > 0 ? totalG / placeItemList.size() : 0;
        double sG = 0.0;
        for (PlaceItem placeItem : placeItemList) {
            sG += Math.pow((placeItem.h - avgG), 2) / placeItemList.size();
        }
        
        return new Solution(placeItemList, sG, totalS, totalS / (W * H));
    }
    
    /**
     * 多盘装箱（分批）
     * @return 多个装箱方案
     */
    public List<Solution> packings() {
        List<Solution> solutions = new ArrayList<>();
        boolean[] used = new boolean[items.length];
        int counter = 0;
        
        while (counter < items.length) {
            List<PlaceItem> placeItemList = new ArrayList<>();
            double totalS = 0.0;
            double maxG = 0.0;
            
            // 对每个未使用的物品尝试放置
            for (int i = 0; i < items.length; i++) {
                if (!used[i]) {
                    PlaceItem bestPlace = findBestPosition(items[i], placeItemList);
                    
                    if (bestPlace != null) {
                        placeItemList.add(bestPlace);
                        used[i] = true;
                        counter++;
                        totalS += (items[i].l * items[i].w);
                        if (compareDouble(items[i].h, maxG) == 1) {
                            maxG = items[i].h;
                        }
                    }
                }
            }
            
            if (!placeItemList.isEmpty()) {
                solutions.add(new Solution(placeItemList, maxG, totalS, totalS / (W * H)));
            } else {
                // 如果本轮没有放置任何物品，说明剩余物品都太大，强制放入新盘
                break;
            }
        }
        
        return solutions;
    }
    
    /**
     * 为物品寻找最佳放置位置（最左最下）
     * @param item 待放置物品
     * @param placedItems 已放置物品列表
     * @return 最佳放置位置，如果无法放置则返回null
     */
    private PlaceItem findBestPosition(Item item, List<PlaceItem> placedItems) {
        PlaceItem bestPlace = null;
        double bestY = Double.MAX_VALUE;
        double bestX = Double.MAX_VALUE;
        
        // 尝试不旋转
        PlaceItem place1 = findPositionForSize(item.l, item.w, item, false, placedItems);
        if (place1 != null && (compareDouble(place1.y, bestY) == -1 || 
            (compareDouble(place1.y, bestY) == 0 && compareDouble(place1.x, bestX) == -1))) {
            bestPlace = place1;
            bestY = place1.y;
            bestX = place1.x;
        }
        
        // 如果允许旋转，尝试旋转90度
        if (isRotateEnable && compareDouble(item.l, item.w) != 0) {
            PlaceItem place2 = findPositionForSize(item.w, item.l, item, true, placedItems);
            if (place2 != null && (compareDouble(place2.y, bestY) == -1 || 
                (compareDouble(place2.y, bestY) == 0 && compareDouble(place2.x, bestX) == -1))) {
                bestPlace = place2;
            }
        }
        
        return bestPlace;
    }
    
    /**
     * 为给定尺寸寻找BL位置
     * @param l 长度
     * @param w 宽度
     * @param item 原始物品
     * @param isRotate 是否旋转
     * @param placedItems 已放置物品列表
     * @return 放置位置
     */
    private PlaceItem findPositionForSize(double l, double w, Item item, 
                                          boolean isRotate, List<PlaceItem> placedItems) {
        // 生成候选点列表
        List<Point> candidatePoints = generateCandidatePoints(placedItems);
        
        PlaceItem bestPlace = null;
        double bestY = Double.MAX_VALUE;
        double bestX = Double.MAX_VALUE;
        
        // 对每个候选点应用BL规则
        for (Point point : candidatePoints) {
            // 应用BL规则：从该点出发，先下后左
            Point blPosition = applyBottomLeftRule(point.x, point.y, l, w, placedItems);
            
            if (blPosition != null) {
                // 检查是否在容器范围内
                if (compareDouble(blPosition.x + l, W) <= 0 && 
                    compareDouble(blPosition.y + w, H) <= 0) {
                    // 选择最下最左的位置
                    if (compareDouble(blPosition.y, bestY) == -1 || 
                        (compareDouble(blPosition.y, bestY) == 0 && compareDouble(blPosition.x, bestX) == -1)) {
                        bestY = blPosition.y;
                        bestX = blPosition.x;
                        bestPlace = new PlaceItem(item.name, blPosition.x, blPosition.y, 
                                                 l, w, item.h, isRotate);
                    }
                }
            }
        }
        
        return bestPlace;
    }
    
    /**
     * 应用最左最下规则
     * @param startX 起始X坐标
     * @param startY 起始Y坐标
     * @param l 物品长度
     * @param w 物品宽度
     * @param placedItems 已放置物品列表
     * @return 最终位置
     */
    private Point applyBottomLeftRule(double startX, double startY, double l, double w, 
                                      List<PlaceItem> placedItems) {
        double x = startX;
        double y = startY;
        
        boolean moved = true;
        int maxIterations = 1000; // 防止无限循环
        int iterations = 0;
        
        while (moved && iterations < maxIterations) {
            moved = false;
            iterations++;
            
            // 尝试向下移动
            double newY = moveDown(x, y, l, w, placedItems);
            if (compareDouble(newY, y) == -1) {
                y = newY;
                moved = true;
                continue; // 向下移动后，重新开始循环
            }
            
            // 尝试向左移动
            double newX = moveLeft(x, y, l, w, placedItems);
            if (compareDouble(newX, x) == -1) {
                x = newX;
                moved = true;
            }
        }
        
        // 最终检查是否与已放置物品重叠
        if (!isOverlapping(x, y, l, w, placedItems)) {
            return new Point(x, y);
        }
        
        return null; // 如果重叠，返回null
    }
    
    /**
     * 检查指定位置的物品是否与已放置物品重叠
     */
    private boolean isOverlapping(double x, double y, double l, double w, List<PlaceItem> placedItems) {
        for (PlaceItem placed : placedItems) {
            // 两个矩形不重叠的条件：
            // 1. 当前物品在已放置物品的右边：x >= placed.x + placed.l
            // 2. 当前物品在已放置物品的左边：x + l <= placed.x
            // 3. 当前物品在已放置物品的上边：y >= placed.y + placed.w
            // 4. 当前物品在已放置物品的下边：y + w <= placed.y
            
            boolean notOverlap = compareDouble(x, placed.x + placed.l) >= 0 ||  // 在右边
                                compareDouble(x + l, placed.x) <= 0 ||          // 在左边
                                compareDouble(y, placed.y + placed.w) >= 0 ||  // 在上边
                                compareDouble(y + w, placed.y) <= 0;           // 在下边
            
            if (!notOverlap) {
                return true; // 发现重叠
            }
        }
        return false; // 没有重叠
    }
    
    /**
     * 尝试向下移动到最低可行位置
     * 找到所有在水平方向有重叠的已放置物品中，最高的那个的顶部位置
     */
    private double moveDown(double x, double y, double l, double w, List<PlaceItem> placedItems) {
        double maxY = 0.0; // 最低可以到达容器底部
        
        // 检查与每个已放置物品的碰撞
        for (PlaceItem placed : placedItems) {
            // 检查水平方向是否有重叠
            // 重叠条件：当前物品的右边界 > 已放置物品的左边界 且 当前物品的左边界 < 已放置物品的右边界
            boolean horizontalOverlap = compareDouble(x + l, placed.x) > 0 && 
                                       compareDouble(x, placed.x + placed.l) < 0;
            
            if (horizontalOverlap) {
                // 该已放置物品会阻挡当前物品向下移动
                // 更新最高的阻挡位置（该已放置物品的顶部）
                if (compareDouble(placed.y + placed.w, maxY) == 1) {
                    maxY = placed.y + placed.w;
                }
            }
        }
        
        return maxY;
    }
    
    /**
     * 尝试向左移动到最左可行位置
     * 找到所有在垂直方向有重叠的已放置物品中，最靠右的那个的右边界位置
     */
    private double moveLeft(double x, double y, double l, double w, List<PlaceItem> placedItems) {
        double maxX = 0.0; // 最左可以到达容器左边界
        
        // 检查与每个已放置物品的碰撞
        for (PlaceItem placed : placedItems) {
            // 检查垂直方向是否有重叠
            // 重叠条件：当前物品的上边界 > 已放置物品的下边界 且 当前物品的下边界 < 已放置物品的上边界
            boolean verticalOverlap = compareDouble(y + w, placed.y) > 0 && 
                                     compareDouble(y, placed.y + placed.w) < 0;
            
            if (verticalOverlap) {
                // 该已放置物品会阻挡当前物品向左移动
                // 更新最靠右的阻挡位置（该已放置物品的右边界）
                if (compareDouble(placed.x + placed.l, maxX) == 1) {
                    maxX = placed.x + placed.l;
                }
            }
        }
        
        return maxX;
    }
    
    /**
     * 生成候选放置点
     * 候选点包括：
     * 1. 原点(0, 0)
     * 2. 每个已放置物品的四个角点
     * 3. 容器的四个角
     */
    private List<Point> generateCandidatePoints(List<PlaceItem> placedItems) {
        List<Point> points = new ArrayList<>();
        
        // 总是包含原点（左下角）
        points.add(new Point(0, 0));
        
        // 添加容器的其他角点（虽然通常只有左下角有用）
        points.add(new Point(0, 0));  // 左下
        points.add(new Point(W, 0));  // 右下（通常用不到）
        
        // 添加已放置物品产生的关键点
        for (PlaceItem placed : placedItems) {
            // 四个角点：
            // 右下角 - 最重要的候选点
            points.add(new Point(placed.x + placed.l, placed.y));
            // 左上角 - 第二重要的候选点
            points.add(new Point(placed.x, placed.y + placed.w));
            // 右上角
            points.add(new Point(placed.x + placed.l, placed.y + placed.w));
            // 左下角（通常不需要，因为已被占据）
            // points.add(new Point(placed.x, placed.y));
        }
        
        return points;
    }
    
    /**
     * 内部类：表示一个二维点
     */
    private static class Point {
        double x;
        double y;
        
        Point(double x, double y) {
            this.x = x;
            this.y = y;
        }
    }
}

