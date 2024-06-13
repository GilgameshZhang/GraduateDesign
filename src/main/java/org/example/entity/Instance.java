package org.example.entity;

import java.util.List;

/**
 * 实例对象
 * 整个问题的信息（边界尺寸，矩形集合等）
 */
public class Instance {
    //边界的宽
    private double W;
    //边界的高
    private double H;
    //矩形列表
    private List<Item> itemList;
    //是否允许矩形旋转
    private  boolean isRotateEnable;

    public double getW() {
        return W;
    }

    public void setW(double w) {
        W = w;
    }

    public double getH() {
        return H;
    }

    public void setH(double h) {
        H = h;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public boolean isRotateEnable() {
        return isRotateEnable;
    }

    public void setRotateEnable(boolean rotateEnable) {
        isRotateEnable = rotateEnable;
    }

    public Instance() {
    }

    public Instance(double w, double h, List<Item> itemList, boolean isRotateEnable) {
        W = w;
        H = h;
        this.itemList = itemList;
        this.isRotateEnable = isRotateEnable;
    }
}
