package AlgorithmFrame.bachSelect.aco;
import ProblemFrame.BatchResult;
import ProblemFrame.Item;
import ProblemFrame.Machine;
import ProblemFrame.Solution;

import javax.sound.midi.Sequence;
import java.util.ArrayList;
import java.util.List;

public class ACO {

    // 蚂蚁数组
    public Ant[] ants;
    // 蚂蚁数量
    public int antNum;
    // 矩形数量
    public int itemNum;
    // 最大迭代数
    public int MAX_GEN;
    // 信息素矩阵
    public double[][] pheromone;
    // 最佳放置序列
    public List<Integer> bestSquence;
    // 最佳迭代数
    public int bestT;
    // 最优解
    public List<Solution> bestSolution;
    // 最优解的蚂蚁
    public Ant bestAnt;
    // 不同度矩形
    double[][] different;
    // 三个参数
    // 信息素重要程度
    private double alpha;
    // 启发式因子重要程度
    private double beta;
    // 信息素挥发速率
    private double rho;
    //用哪个机器加工
    Machine machine;
    // 边界的长 原W-现L
    private double L;
    // 边界的宽 原H-现W
    private double W;
    // 边界的高 原L-现H
    private double H;
    // 矩形数组
    Item[] items;
    // 是否可以旋转
    private boolean isRotateEnable;
    // 随机数种子
    Long seed;

    /**
     * @param antNum   蚂蚁数量
     * @param MAX_GEN  迭代次数(提高这个值可以稳定地提高解质量，但是会增加求解时间)
     * @param alpha    信息素重要程度
     * @param beta     启发式因子重要程度
     * @param rho      信息素挥发速率
     * @param machine 打印机对象
     * @param seed     随机数种子，如果传入null则不设置随机数种子，否则按照传入的种子进行设置，方便复现结果
     * @Description 构造函数
     */
    public ACO(int antNum, int MAX_GEN, double alpha, double beta, double rho, Machine machine, List<Item> printItem, Long seed, boolean isRotateEnable) {
        this.antNum = antNum;
        this.ants = new Ant[antNum];
        this.alpha = alpha;
        this.beta = beta;
        this.rho = rho;
        this.MAX_GEN = MAX_GEN;
        this.machine = machine;
        this.L = machine.L;
        this.W = machine.W;
        this.H = machine.H;
        this.isRotateEnable = isRotateEnable;
        if (printItem == null || printItem.isEmpty()) {
            items = null;
        } else {
            this.items = printItem.toArray(new Item[0]);
            this.itemNum = this.items.length;
        }
        this.seed = seed;
    }

    /**
     * @return 最佳装载结果对象Solution
     * @Description 蚁群算法主函数
     */
    public BatchResult solve() {
        if (items == null || items.length == 0) {
            return new BatchResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        } else if (items.length == 1) {
            Ant ant1 = new Ant(isRotateEnable, machine, items, seed);
            List<Integer> sequences = new ArrayList<>() ;
            sequences.add(0);
            ant1.setSequence(sequences);
            ant1.setAllowedItems(new ArrayList<>());
            ant1.setItems(items);
            ant1.evaluate();
            return new BatchResult(ant1.getLocalSolution(), ant1.getStartTime(), ant1.getEndTime(), ant1.getFitness());
        }
        // 进行初始化操作
        init();
        // 迭代MAX_GEN次
        for (int g = 0; g < MAX_GEN; g++) {
            // antNum只蚂蚁
            for (int i = 0; i < antNum; i++) {
                // i这只蚂蚁走itemNum步，构成一个完整的矩形放置顺序
                for (int j = 1; j < itemNum; j++) {
                    ants[i].selectNextSquare(pheromone);
                }
                // 查看这只蚂蚁装载利用率是否比当前最优解优秀
                ants[i].evaluate();
                if (bestSolution == null || compareDouble(ants[i].getFitness(), bestAnt.getFitness()) == -1) {
                    // 比当前优秀则拷贝优秀的放置顺序
                    bestSquence = new ArrayList<>(ants[i].getSequence());
                    bestAnt = ants[i];
                    bestT = g;
                    bestSolution = ants[i].getLocalSolution();
                    //System.out.println("蚂蚁 " + (i + 1) + " 找到更优解 , 当前迭代次数为: " + g + " , 该机器加工的最大完工时间为：" + bestAnt.getFitness());
                }
                // 更新这只蚂蚁的信息数变化矩阵，对称矩阵
                for (int j = 0; j < itemNum; j++) {
                    ants[i].getDelta()[ants[i].getSequence().get(j)][ants[i]
                            .getSequence().get(j + 1 >= itemNum ? 0 : j + 1)] = (1.0 / ants[i]
                            .getFitness());
                    ants[i].getDelta()[ants[i].getSequence().get(j + 1 >= itemNum ? 0 : j + 1)][ants[i]
                            .getSequence().get(j)] = (1.0 / ants[i]
                            .getFitness());
                }
            }
            // 更新信息素
            updatePheromone();
            // 重新初始化蚂蚁
            for (int i = 0; i < antNum; i++) {
                ants[i].initAnt(different, alpha, beta);
            }
        }
        BatchResult batchResult = new BatchResult(bestAnt.getLocalSolution(), bestAnt.getStartTime(), bestAnt.getEndTime(), bestAnt.getFitness());
        return batchResult;
        // 返回结果
    }

    /**
     * @Description 初始化操作
     */
    private void init() {
        //初始化不同度矩阵
        different = new double[itemNum][itemNum];
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                if (i == j) {
                    different[i][j] = 0.0;
                } else {
                    different[i][j] = getDifferent(items[i], items[j]);
                }
            }
        }
        //初始化信息素矩阵
        pheromone = new double[itemNum][itemNum];
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                // 初始化为0.1
                pheromone[i][j] = 0.1;
            }
        }
        // 放置蚂蚁
        for (int i = 0; i < antNum; i++) {
            ants[i] = new Ant(isRotateEnable, machine, items, seed);
            ants[i].initAnt(different, alpha, beta);
        }
    }



    /**
     * @Description 更新信息素
     */
    private void updatePheromone() {
        // 信息素挥发
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                pheromone[i][j] = pheromone[i][j] * (1 - rho);
            }
        }
        // 信息素更新
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                for (int k = 0; k < antNum; k++) {
                    pheromone[i][j] += ants[k].getDelta()[i][j];
                }
            }
        }
    }

    /**
     * @param a 矩形a
     * @param b 矩形b
     * @return 矩形a和b的不同度
     * @Description 计算矩形a对b的不同度
     */
    public double getDifferent(Item a, Item b) {
        double avgH = (a.h + b.h) / 2.0;
        double avgW = (a.w + b.w) / 2.0;
        double avgL = (a.l + b.l) / 2.0;
        double different = 0.2 * (Math.abs(a.w - b.w) / avgW + Math.abs(a.l - b.l) / avgL) + 0.8 * Math.abs(a.h - b.h) / avgH;
        return Math.max(0.0001, different);
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

}

