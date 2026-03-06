package ProblemFrame.localsearch.util;

import ProblemFrame.SkyLinePacking;
import ProgramEntity.*;
import ProgramEntity.Machine.PrintMachine;
import java.util.*;

/**
 * Skyline装箱辅助类
 * 
 * 用于局部搜索算子中的真实装箱尝试
 * - 检查零件是否可以插入现有批次
 * - 尝试将零件插入批次
 * - 处理旋转和放置逻辑
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SkylinePackingHelper {
    
    /**
     * 尝试将零件插入到批次中（真实Skyline装箱）
     * 
     * @param batch 目标批次
     * @param item 要插入的零件
     * @param printer 打印机参数
     * @param isRotateEnable 是否允许旋转
     * @return true: 可以插入; false: 无法插入
     */
    public static boolean canInsertToBatch(Solution batch, PlaceItem item, 
                                          PrintMachine printer, boolean isRotateEnable) {
        if (batch == null || item == null || printer == null) {
            return false;
        }
        
        // 1. 快速检查：尺寸是否超限
        if (!canFitInPrinter(item, printer, isRotateEnable)) {
            return false;
        }
        
        // 2. 快速检查：面积是否超限
        double plateArea = printer.L * printer.W;
        double usedArea = batch.totalS;
        double itemArea = item.l * item.w;
        
        if (usedArea + itemArea > plateArea * 1.01) {  // 允许1%误差
            return false;
        }
        
        // 3. 使用Skyline装箱进行真实检查
        return tryPackWithSkyline(batch, item, printer, isRotateEnable);
    }
    
    /**
     * 实际执行Skyline装箱尝试
     * 
     * 将批次中的所有零件 + 新零件重新装箱，检查是否可行
     */
    private static boolean tryPackWithSkyline(Solution batch, PlaceItem newItem,
                                             PrintMachine printer, boolean isRotateEnable) {
        // 创建零件数组（包含批次中的所有零件 + 新零件）
        List<PlaceItem> allItems = new ArrayList<>(batch.placeItemList);
        allItems.add(newItem);
        
        // 转换为Item数组
        Item[] items = new Item[allItems.size()];
        for (int i = 0; i < allItems.size(); i++) {
            PlaceItem pi = allItems.get(i);
            items[i] = new Item(pi.name, pi.l, pi.w, pi.h);
        }
        
        // 使用SkylinePacking进行装箱
        SkyLinePacking packing = new SkyLinePacking(
            printer.L, 
            printer.W, 
            items, 
            isRotateEnable
        );
        
        Solution result = packing.packing();
        
        // 检查是否所有零件都成功装入
        // 成功条件：放置数量 = 总数量 且 最大高度不超限
        boolean success = (result.placeItemList.size() == items.length) && 
                         (result.maxG <= printer.H);
        
        return success;
    }
    
    /**
     * 检查零件尺寸是否在打印机范围内
     */
    public static boolean canFitInPrinter(PlaceItem item, PrintMachine printer, boolean isRotateEnable) {
        if (item == null || printer == null) {
            return false;
        }
        
        // 检查高度
        if (item.h > printer.H) {
            return false;
        }
        
        // 检查平面尺寸（考虑旋转）
        boolean fitsNormal = (item.l <= printer.L && item.w <= printer.W);
        boolean fitsRotated = isRotateEnable && (item.w <= printer.L && item.l <= printer.W);
        
        return fitsNormal || fitsRotated;
    }
    
    /**
     * 尝试将多个零件插入同一批次
     * 
     * @param batch 目标批次
     * @param items 要插入的零件列表
     * @param printer 打印机参数
     * @param isRotateEnable 是否允许旋转
     * @return 成功插入的零件数量
     */
    public static int tryInsertMultipleItems(Solution batch, List<PlaceItem> items,
                                            PrintMachine printer, boolean isRotateEnable) {
        int successCount = 0;
        
        for (PlaceItem item : items) {
            if (canInsertToBatch(batch, item, printer, isRotateEnable)) {
                // 成功插入，更新批次信息
                batch.placeItemList.add(item);
                batch.totalS += item.l * item.w;
                batch.maxG = Math.max(batch.maxG, item.h);
                batch.rate = batch.totalS / (printer.L * printer.W);
                successCount++;
            } else {
                // 一旦有零件无法插入，停止尝试
                break;
            }
        }
        
        return successCount;
    }
    
    /**
     * 获取批次的空余空间信息
     */
    public static BatchSpaceInfo getBatchSpaceInfo(Solution batch, PrintMachine printer) {
        if (batch == null || printer == null) {
            return null;
        }
        
        double plateArea = printer.L * printer.W;
        double usedArea = batch.totalS;
        double freeArea = plateArea - usedArea;
        double freeRatio = freeArea / plateArea;
        double usedHeight = batch.maxG;
        double freeHeight = printer.H - usedHeight;
        
        return new BatchSpaceInfo(
            freeArea, 
            freeRatio, 
            usedHeight, 
            freeHeight,
            batch.placeItemList.size()
        );
    }
    
    /**
     * 选择最适合插入的批次
     * 策略：空余面积最大且高度容许的批次
     */
    public static int selectBestBatchForInsertion(List<Solution> batchList, PlaceItem item,
                                                  PrintMachine printer, boolean isRotateEnable) {
        int bestBatch = -1;
        double maxFreeArea = -1;
        
        for (int i = 0; i < batchList.size(); i++) {
            Solution batch = batchList.get(i);
            
            // 检查高度
            if (batch.maxG + item.h > printer.H) {
                continue;
            }
            
            // 检查是否可以插入
            if (!canInsertToBatch(batch, item, printer, isRotateEnable)) {
                continue;
            }
            
            // 计算空余面积
            double plateArea = printer.L * printer.W;
            double freeArea = plateArea - batch.totalS;
            
            if (freeArea > maxFreeArea) {
                maxFreeArea = freeArea;
                bestBatch = i;
            }
        }
        
        return bestBatch;
    }
    
    /**
     * 批次空间信息
     */
    public static class BatchSpaceInfo {
        public final double freeArea;      // 空余面积
        public final double freeRatio;     // 空余比例
        public final double usedHeight;    // 已用高度
        public final double freeHeight;    // 剩余高度
        public final int itemCount;        // 零件数量
        
        public BatchSpaceInfo(double freeArea, double freeRatio, double usedHeight,
                            double freeHeight, int itemCount) {
            this.freeArea = freeArea;
            this.freeRatio = freeRatio;
            this.usedHeight = usedHeight;
            this.freeHeight = freeHeight;
            this.itemCount = itemCount;
        }
        
        @Override
        public String toString() {
            return String.format("BatchSpace[freeArea=%.2f, freeRatio=%.2f%%, usedH=%.2f, items=%d]",
                freeArea, freeRatio * 100, usedHeight, itemCount);
        }
    }
}
