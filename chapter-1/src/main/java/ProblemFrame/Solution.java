package ProblemFrame;

import java.util.List;

/**
 * 装箱结果类（包括已经放置矩形列表，以防止矩形总面积， 装载利用率信息）
 */
public class Solution {
    //已放置矩形
    public List<PlaceItem> placeItemList;
    //放置总面积
    public double totalS;
    //利用率
    public double rate;
    //已放置零件最大竖直高度和其他零件高度差之和
    public double maxG;
    //批次开始时间（用于可视化）
    public double startTime;
    //批次结束时间（用于可视化）
    public double endTime;

    public Solution() {
    }

    public Solution(List<PlaceItem> placeItemList, double maxG, double totalS, double rate) {
        this.placeItemList = placeItemList;
        this.maxG = maxG;
        this.totalS = totalS;
        this.rate = rate;
        this.startTime = 0.0;
        this.endTime = 0.0;
    }
    
    public Solution(List<PlaceItem> placeItemList, double maxG, double totalS, double rate, 
                   double startTime, double endTime) {
        this.placeItemList = placeItemList;
        this.maxG = maxG;
        this.totalS = totalS;
        this.rate = rate;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
