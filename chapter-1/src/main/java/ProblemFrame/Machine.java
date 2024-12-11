package ProblemFrame;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D打印机
 */
public class Machine {
    //机器名字
    public String name;
    //加工能力-长
    public double L;
    //加工能力-宽
    public double W;
    //加工能力-高
    public double H;
    //加工批次规划结果
    public List<Solution> batchPlan;
    //准备时间
    public double prepareTime;
    //换层时间
    public double reCoatingTime;
    //各批次加工开始时间
    public List<Double> startTime;
    //各批次加工结束时间
    public List<Double> endTime;
    //加工零件初始化顺序列表
    public List<Item> printItem;
    //打印层高
    public double printH;


    public Machine() {
    }
    //3D打印机初始化
    public Machine(String name, double l, double w, double h, double printH, double prepareTime, double reCoatingTime) {
        this.name = name;
        L = l;
        W = w;
        H = h;
        this.printItem = new ArrayList<>();
        this.startTime = new ArrayList<>();
        this.endTime = new ArrayList<>();
        this.batchPlan = new ArrayList<>();
        this.printH = printH;
        this.prepareTime = prepareTime;
        this.reCoatingTime = reCoatingTime;
    }
}
