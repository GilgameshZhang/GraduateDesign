package AlgorithmFrame.bachSelect.aco;

import AlgorithmFrame.bachSelect.skyLine.SkyLinePacking;
import ProblemFrame.Item;
import ProblemFrame.Machine;
import ProblemFrame.Solution;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static java.lang.Double.NaN;

/**
 */
@Data
public class Ant {
    // 矩形集合
    private Item[] items;
    // 用哪个机器加工
    public Machine machine;
    // 适应度值
    private double fitness;
    // 已经放置的矩形的索引
    private List<Integer> sequence;
    // 还没放置的矩形索引
    private List<Integer> allowedItems;
    // 信息素变化矩阵
    private double[][] delta;
    // 矩形不同度矩阵
    private double[][] different;
    // 信息素重要程度
    private double alpha;
    // 启发式因子重要程度
    private double beta;
    // 矩形数量
    private int itemNum;
    // 第一个放置的矩形
    private int firstSquare;
    // 当前放置的矩形
    private int currentSquare;
    // 随机数对象
    private Random random;
    // 外矩形的长宽搞
    double W, L, H;
    // 是否允许旋转
    private boolean isRotateEnable;
    // 本地搜索的解
    private List<Solution> localSolution;
    // 开始时间
    private List<Double> startTime;
    // 结束时间
    private List<Double> endTime;


    //构造函数
    public Ant(boolean isRotateEnable, Machine machine, Item[] items, Long seed) {
        this.itemNum = items.length;
        this.items = items;
        this.machine = machine;
        this.L = machine.L;
        this.H = machine.H;
        this.W = machine.W;
        this.isRotateEnable = isRotateEnable;
        localSolution = new ArrayList<>();
        startTime = new ArrayList<>();
        endTime = new ArrayList<>();
        this.random = seed == null ? new Random() : new Random(seed);
    }

    //初始化
    public void initAnt(double[][] different, double a, double b) {
        alpha = a;
        beta = b;
        this.different = different;
        // 初始允许搜索的矩形集合
        allowedItems = new ArrayList<>();
        // 初始禁忌表
        sequence = new ArrayList<>();
        // 初始信息数变化矩阵为0
        delta = new double[itemNum][itemNum];
        // 设置起始矩形(随机选取第一个矩形)
        firstSquare = random.nextInt(itemNum);

        for (int i = 0; i < itemNum; i++) {
            if (i != firstSquare) {
                allowedItems.add(i);
            }
        }
        // 将第一个放置的矩形添加至禁忌表
        sequence.add(firstSquare);
        // 第一个矩形即为当前放置的矩形
        currentSquare = firstSquare;
    }

    //选择下一个矩形
    public void selectNextSquare(double[][] pheromone) {
        double[] p = new double[itemNum];
        double sum = 0d;

        // --------------- 思路1:直接将距离看为一个常数1 --------------------
        // 计算分母部分
//        for (Integer i : allowedItems) {
//            sum += Math.pow(pheromone[currentSquare][i], alpha)
//                    * Math.pow(1.03, beta);
//        }
//        // 计算概率矩阵
//        for (int i : allowedItems) {
//            p[i] = (Math.pow(pheromone[currentSquare][i], alpha) * Math
//                    .pow(1.03, beta)) / sum;
//        }

        // --------------- 思路2:采用矩形的不同度代替距离 --------------------
        // 计算分母部分
        for (Integer i : allowedItems) {
            sum += Math.pow(pheromone[currentSquare][i], alpha)
                    * Math.pow(1.0 / different[currentSquare][i], beta);
        }
        // 计算概率矩阵
        for (int i : allowedItems) {
            p[i] = (Math.pow(pheromone[currentSquare][i], alpha) * Math
                    .pow(1.0 / different[currentSquare][i], beta)) / sum;
        }
        // 轮盘赌选择下一个矩形
        double sleectP = random.nextDouble();
        int selectSquare = -1;
        double sum1 = 0d;
        for (int i = 0; i < itemNum; i++) {
            sum1 += p[i];
            if(Double.isNaN(sum1)) {
                System.out.println("sum1 = " + sum1);
            }
            if (compareDouble(sum1, sleectP) != -1) {
                selectSquare = i;
                break;
            }
        }
        // 从允许选择的矩形中去除select 矩形
        for (Integer i : allowedItems) {
            if (i == selectSquare) {
                allowedItems.remove(i);
                break;
            }
        }
        // 在禁忌表中添加select矩形
        sequence.add(selectSquare);
        currentSquare = selectSquare;
    }

    // 根据顺序进行装箱,并返回装载的矩形总面积
    public void evaluate() {
        // 根据顺序进行装箱
        Item[] items = new Item[this.items.length];
        for (int i = 0; i < sequence.size(); i++) {
            items[i] = this.items[sequence.get(i)];
        }
        localSolution.clear();
        localSolution = new ArrayList<>(new SkyLinePacking(L, W, items, isRotateEnable).packings());
        startTime.clear();
        endTime.clear();
        // 计算适应度值
            for (Solution solution : localSolution) {
                if (startTime.isEmpty()) {
                    startTime.add(machine.prepareTime);
                } else {
                    double startTime1 = endTime.get(endTime.size() - 1) + machine.prepareTime;
                    startTime.add(startTime1);
                }
                double printTime = machine.reCoatingTime * solution.maxG / machine.printH;
                endTime.add(startTime.get(startTime.size() - 1) + printTime);
            }
            //最大完工时间为适应度值
            fitness = endTime.get(endTime.size() - 1);
        }

    /**
     * @param d1 双精度浮点型变量1
     * @param d2 双精度浮点型变量2
     * @return 返回0代表两个数相等，返回1代表前者大于后者，返回-1代表前者小于后者，
     * @Description 判断两个双精度浮点型变量的大小关系
     */
    private int compareDouble(double d1, double d2) {
        // 定义一个误差范围，如果两个数相差小于这个误差，则认为他们是相等的 1e-06 = 03.000001
        double error = 1e-06;
        if (Math.abs(d1 - d2) < error) {
            return 0;
        } else if (d1 < d2) {
            return -1;
        } else if (d1 > d2) {
            return 1;
        } else {
            throw new RuntimeException("d1 = " + d1 + " , d2 = " + d2);
        }
    }

    public boolean isRotateEnable() {
        return isRotateEnable;
    }

    public void setRotateEnable(boolean rotateEnable) {
        isRotateEnable = rotateEnable;
    }

}

