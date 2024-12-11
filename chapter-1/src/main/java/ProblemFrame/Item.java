package ProblemFrame;

import java.util.ArrayList;
import java.util.List;

public class Item {
    //名字
    public String name;
    //长
    public double l;
    //宽
    public double w;
    //高
    public double h;

    public Item(String name, double l, double w, double h) {
        this.name = name;
        this.l = l;
        this.w = w;
        this.h = h;
    }

    //复制单个Item
    public static Item copy(Item item) {
        return new Item(item.name, item.l, item.w, item.h);
    }

    //复制Item数组
    public static Item[] copy(Item[] items) {
        Item[] newItems = new Item[items.length];
        for (int i = 0; i < items.length; i++) {
            newItems[i] = copy(items[i]);
        }
        return newItems;
    }

    //复制Item列表
    public static List<Item> copy(List<Item> items) {
        List<Item> newItems = new ArrayList<>();
        for (Item item : items) {
            newItems.add(item);
        }
        return newItems;
    }
}
