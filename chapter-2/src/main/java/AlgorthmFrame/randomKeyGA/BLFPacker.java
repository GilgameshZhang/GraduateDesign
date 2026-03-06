package AlgorthmFrame.randomKeyGA;

import ProgramEntity.Item;
import ProgramEntity.PlaceItem;
import ProgramEntity.Solution;
import ProgramEntity.SkyLine;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static util.compareUtil.compareDouble;

/**
 * 改进的左下优先（BLF, Bottom-Left-First）嵌套启发式
 * 
 * 核心特点：
 * 1. 按染色体指定的零件序列依次嵌套
 * 2. 使用染色体指定的朝向和旋转角度
 * 3. 无法嵌套的零件转入下一批次
 * 4. 实现批次的动态生成
 * 
 * 基于天际线（Skyline）算法实现
 */
public class BLFPacker {
    
    // 构建腔室的长、宽、高
    private double L;
    private double W;
    private double H;
    
    /**
     * 构造函数
     * @param L 构建腔室长度
     * @param W 构建腔室宽度
     * @param H 构建腔室高度
     */
    public BLFPacker(double L, double W, double H) {
        this.L = L;
        this.W = W;
        this.H = H;
    }
    
    /**
     * 按指定顺序、朝向、旋转角度进行批次嵌套
     * 
     * @param items 所有零件数组
     * @param partSequence 零件嵌套顺序（索引数组）
     * @param orientations 每个零件的朝向（1-3）
     * @param rotations 每个零件的旋转角度（1-8）
     * @return 嵌套结果列表（每个批次一个Solution）
     */
    public List<Solution> packInBatches(Item[] items, int[] partSequence, 
                                        int[] orientations, int[] rotations) {
        List<Solution> batches = new ArrayList<>();
        boolean[] packed = new boolean[items.length];
        int packedCount = 0;
        
        // 循环生成批次，直到所有零件都被嵌套
        while (packedCount < items.length) {
            Solution batch = packSingleBatch(items, partSequence, orientations, rotations, packed);
            
            // 检测空批次：如果没有零件被放置，说明剩余零件无法放置，跳出循环
            if (batch.placeItemList.isEmpty()) {
                System.out.println("警告：BLFPacker无法放置剩余 " + (items.length - packedCount) + " 个零件");
                break;  // 跳出循环，避免无限添加空批次
            }
            
            batches.add(batch);
            
            // 统计已嵌套的零件数量
            packedCount = 0;
            for (boolean p : packed) {
                if (p) packedCount++;
            }
        }
        
        return batches;
    }
    
    /**
     * 嵌套单个批次（参考原版SkylinePacking的核心逻辑）
     *
     * @param items 所有零件数组
     * @param partSequence 零件嵌套顺序
     * @param orientations 零件朝向
     * @param rotations 零件旋转
     * @param packed 零件是否已嵌套的标记数组
     * @return 单个批次的嵌套结果
     */
    private Solution packSingleBatch(Item[] items, int[] partSequence,
                                     int[] orientations, int[] rotations, boolean[] packed) {
        // 已放置的零件列表
        List<PlaceItem> placedItems = new ArrayList<>();

        // 天际线优先队列
        PriorityQueue<SkyLine> skyLines = new PriorityQueue<>();
        skyLines.add(new SkyLine(0, 0, L));  // 初始天际线

        // 总面积和最大高度
        double totalArea = 0.0;
        double maxHeight = 0.0;
        
        // 已嵌套零件计数
        int packedCount = 0;
        for (boolean p : packed) {
            if (p) packedCount++;
        }
        
        // 核心逻辑：循环处理天际线，直到队列为空或所有零件都已放置
        while (!skyLines.isEmpty() && packedCount < items.length) {
            // 从队列中取出最下最左的天际线
            SkyLine currentSkyLine = skyLines.poll();
            
            // 尝试在当前天际线上放置剩余零件
            PlaceItem placedItem = tryPlaceOnCurrentSkyline(items, partSequence, orientations, rotations, 
                                                             packed, currentSkyLine);
            
            if (placedItem != null) {
                // 成功放置零件
                int idx = Integer.parseInt(placedItem.name);
                packed[idx] = true;
                packedCount++;

                placedItem.name = items[idx].name;
                placedItems.add(placedItem);
                
                // 获取零件索引


                
                totalArea += placedItem.l * placedItem.w;
                maxHeight = Math.max(maxHeight, placedItem.h);
                
                // 更新天际线
                updateSkylinesLikeOriginal(skyLines, currentSkyLine, placedItem);
            } else {
                // 当前天际线放不下任何零件，尝试合并天际线
                combineSkyLine(skyLines, currentSkyLine);
            }
        }
        
        // 计算利用率
        double utilizationRate = (L * W > 0) ? (totalArea / (L * W)) : 0.0;
        
        return new Solution(placedItems, maxHeight, totalArea, utilizationRate);
    }
    
    /**
     * 在当前天际线上尝试放置剩余零件（按指定顺序）
     * 
     * @return 成功放置的PlaceItem，失败返回null
     */
    private PlaceItem tryPlaceOnCurrentSkyline(Item[] items, int[] partSequence,
                                               int[] orientations, int[] rotations,
                                               boolean[] packed, SkyLine currentSkyLine) {
        // 按染色体指定的顺序尝试嵌套零件
        for (int idx : partSequence) {
            // 跳过已嵌套的零件
            if (packed[idx]) {
                continue;
            }
            
            // 获取零件及其朝向、旋转
            Item item = items[idx];
            int orientation = orientations[idx];
            int rotation = rotations[idx];
            
            // 根据朝向和旋转计算零件的实际尺寸
            double[] dimensions = calculateDimensions(item, orientation, rotation);
            double itemL = dimensions[0];  // 长（X方向）
            double itemW = dimensions[1];  // 宽（Y方向）
            
            // 检查是否能放置在当前天际线上
            if (canPlace(currentSkyLine, itemL, itemW)) {
                // 创建放置对象（靠左放置）
                PlaceItem placedItem = new PlaceItem(
                    String.valueOf(idx),
                    currentSkyLine.x,      // X坐标：天际线左端
                    currentSkyLine.y,      // Y坐标：天际线高度
                    itemL,
                    itemW,
                    item.h,                // 恢复高度信息
                    false
                );
                
                return placedItem;
            }
        }
        
        // 当前天际线放不下任何零件
        return null;
    }
    
    /**
     * 合并天际线（当天际线放不下任何零件时调用）
     * 
     * 逻辑：将当前天际线"上移"到与相邻天际线同高，并合并长度
     */
    private void combineSkyLine(PriorityQueue<SkyLine> skyLines, SkyLine currentSkyLine) {
        boolean merged = false;
        
        for (SkyLine line : skyLines) {
            // 只与不高于当前天际线的天际线合并
            if (compareDouble(currentSkyLine.y, line.y) != 1) {
                // 情况1：头尾相连（line在currentSkyLine左侧）
                if (compareDouble(currentSkyLine.x, line.x + line.len) == 0) {
                    skyLines.remove(line);
                    merged = true;
                    // 合并：更新currentSkyLine的位置和长度
                    currentSkyLine.x = line.x;
                    currentSkyLine.y = line.y;
                    currentSkyLine.len = line.len + currentSkyLine.len;
                    break;
                }
                
                // 情况2：尾头相连（line在currentSkyLine右侧）
                if (compareDouble(currentSkyLine.x + currentSkyLine.len, line.x) == 0) {
                    skyLines.remove(line);
                    merged = true;
                    // 合并：更新currentSkyLine的高度和长度
                    currentSkyLine.y = line.y;
                    currentSkyLine.len = line.len + currentSkyLine.len;
                    break;
                }
            }
        }
        
        // 如果有合并，将合并后的天际线重新加入队列
        if (merged) {
            skyLines.add(currentSkyLine);
        }
        // 如果没有合并，天际线被丢弃（不再使用）
    }
    
    /**
     * 更新天际线（参考原始SkylinePacking的实现）
     * 
     * 关键逻辑：
     * 1. 移除旧天际线
     * 2. 添加零件顶部的新天际线：(x, y + w, l)
     * 3. 添加零件右侧剩余的天际线：(x + l, y, remainingLen)
     */
    private void updateSkylinesLikeOriginal(PriorityQueue<SkyLine> skyLines, SkyLine oldSkyLine, PlaceItem placedItem) {
        // 移除旧天际线（从队列中删除）
        skyLines.remove(oldSkyLine);
        
        // 添加零件顶部的新天际线
        addSkyLineInQueue(skyLines, placedItem.x, placedItem.y + placedItem.w, placedItem.l);
        
        // 添加零件右侧剩余的天际线
        double remainingLength = oldSkyLine.len - placedItem.l;
        addSkyLineInQueue(skyLines, placedItem.x + placedItem.l, oldSkyLine.y, remainingLength);
    }
    
    /**
     * 将指定属性的天际线加入到天际线队列中（length > 0才能加入）
     * 
     * @param skyLines 天际线队列
     * @param x 新天际线的x坐标
     * @param y 新天际线的y坐标
     * @param length 天际线长度
     */
    private void addSkyLineInQueue(PriorityQueue<SkyLine> skyLines, double x, double y, double length) {
        // 新天际线长度大于0才加入
        if (compareDouble(length, 0.0) == 1) {
            skyLines.add(new SkyLine(x, y, length));
        }
    }
    
    /**
     * 检查零件是否能放置在指定天际线上
     */
    private boolean canPlace(SkyLine skyLine, double itemL, double itemW) {
        // 检查长度
        if (compareDouble(skyLine.len, itemL) == -1) {
            return false;
        }
        
        // 检查宽度
        if (compareDouble(skyLine.y + itemW, W) == 1) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 根据朝向和旋转角度计算零件的实际尺寸
     * 
     * @param item 原始零件
     * @param orientation 朝向（1-3）
     * @param rotation 旋转（1-8，对应0°, 45°, 90°, ..., 315°）
     * @return 尺寸数组 [长L, 宽W, 高H]
     */
    private double[] calculateDimensions(Item item, int orientation, int rotation) {
        double dimL = item.l;
        double dimW = item.w;
        
        // 1. 应用朝向变换（简化处理：只考虑L×W平面的2D变换）
//        double dimL, dimW;
//        switch (orientation) {
//            case 1:
//                dimL = l;
//                dimW = w;
//                break;
//            case 2:
//                dimL = w;
//                dimW = l;
//                break;
//            case 3:
//                dimL = l;
//                dimW = w;
//                break;
//            default:
//                dimL = w;
//                dimW = l;
//        }
        
        // 2. 应用旋转变换（只考虑90°的倍数）
        double finalL, finalW;
        if (rotation == 3 || rotation == 7) {  // 90° 或 270°
            finalL = dimW;
            finalW = dimL;
        } else {
            finalL = dimL;
            finalW = dimW;
        }
        
        return new double[]{finalL, finalW};
    }
    
    /**
     * 获取构建腔室尺寸
     */
    public double getL() {
        return L;
    }
    
    public double getW() {
        return W;
    }
    
    public double getH() {
        return H;
    }
}
