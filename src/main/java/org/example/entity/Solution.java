package org.example.entity;

import java.util.List;

/**
 * 装箱结果类（包括已经放置矩形列表，以防止矩形总面积， 装载利用率信息）
 */
public class Solution {
    //已放置矩形
    private List<PlaceItem> placeItemList;
    //放置总面积
    private double totalS;
    //利用率
    private double rate;

    public Solution() {
    }

    public Solution(List<PlaceItem> placeItemList, double totalS, double rate) {
        this.placeItemList = placeItemList;
        this.totalS = totalS;
        this.rate = rate;
    }

    public List<PlaceItem> getPlaceItemList() {
        return placeItemList;
    }

    public void setPlaceItemList(List<PlaceItem> placeItemList) {
        this.placeItemList = placeItemList;
    }

    public double getTotalS() {
        return totalS;
    }

    public void setTotalS(double totalS) {
        this.totalS = totalS;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}
