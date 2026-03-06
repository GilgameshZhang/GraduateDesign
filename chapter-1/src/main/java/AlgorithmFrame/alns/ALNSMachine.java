package AlgorithmFrame.alns;

/**
 * ALNS算法中的机器表示
 */
public class ALNSMachine {
    // 机器ID
    public int id;
    // 机器名称
    public String name;
    // 平台宽度
    public double width;
    // 平台高度（对应2D装箱的高度）
    public double height;
    // 打印层高
    public double printHeight;
    // 准备时间
    public double prepareTime;
    // 换层时间
    public double reCoatingTime;
    
    public ALNSMachine(int id, String name, double width, double height, double printHeight, 
                      double prepareTime, double reCoatingTime) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.height = height;
        this.printHeight = printHeight;
        this.prepareTime = prepareTime;
        this.reCoatingTime = reCoatingTime;
    }
}
