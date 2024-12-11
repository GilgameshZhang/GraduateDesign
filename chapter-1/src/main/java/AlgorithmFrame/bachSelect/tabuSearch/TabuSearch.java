package AlgorithmFrame.bachSelect.tabuSearch;

import AlgorithmFrame.bachSelect.skyLine.SkyLinePacking;
import ProblemFrame.BatchResult;
import ProblemFrame.Item;
import ProblemFrame.Machine;
import ProblemFrame.Solution;

import java.util.*;

public class TabuSearch {

    // 迭代次数(提高这个值可以稳定地提高解质量，但是会增加求解时间)
    private int MAX_GEN;
    // 局部搜索的次数(这个值不要太大，太大的话搜索效率会降低)
    public int N;
    // 禁忌长度
    private int tabuSize;
    // 最小和最大禁忌表长度
    private int minTabuSize;
    private int maxTabuSize;
    // 禁忌表
    private int[][] tabuList;
    // 最佳的迭代次数
    public int bestT = -1;
    // 随机函数对象
    public Random random;
    // 当前禁忌表长度
    public int l = 0;
    // 用哪台机器
    Machine machine;
    // 边界的长
    private double L;
    // 边界的宽
    private double W;
    // 矩形数组
    Item[] items;
    // 是否可以旋转
    private boolean isRotateEnable;
    //当前最好解
    List<Solution> bestSolution;
    //开始时间
    List<Double> startTime = new ArrayList<>();
    //结束时间
    List<Double> endTime = new ArrayList<>();

    /**
     * @param MAX_GEN  迭代次数(提高这个值可以稳定地提高解质量，但是会增加求解时间)
     * @param N        局部搜索的次数(这个值不要太大，太大的话搜索效率会降低)
     * @param minTabuSize 最小禁忌长度
     * @param printItem  矩形集合
     * @param machine 加工机器
     * @param seed     随机数种子，如果传入null则不设置随机数种子，否则按照传入的种子进行设置，方便复现结果
     * @Description 构造函数
     */
    public TabuSearch(int MAX_GEN, int N, int minTabuSize, Machine machine, List<Item> printItem, Long seed, boolean isRotateEnable) {
        this.MAX_GEN = MAX_GEN;
        this.N = N;
        this.minTabuSize = minTabuSize;
        this.maxTabuSize = minTabuSize + printItem.size();
        this.L = machine.L;
        this.W = machine.W;
        this.machine = machine;
        this.isRotateEnable = isRotateEnable;
        if (printItem == null) {
            this.items = new Item[0];
        } else {
            this.items = printItem.toArray(new Item[0]);
        }
        this.random = seed == null ? new Random() : new Random(seed);
    }

    /**
     * @return 最佳装载结果对象Solution
     * @Description 禁忌搜索主函数
     */
    public BatchResult solve() {
        if (items.length == 0) {
            return new BatchResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        }
        // 初始化禁忌表
        tabuList = new int[tabuSize][items.length];
        // 初始解的构造讲矩形按照高度降序排列
        Arrays.sort(items, (o1, o2) -> {
            // 由于是降序，所以要加个负号
            return -compareDouble(o1.h, o2.h);
        });
        // 获取初始解 [ 03, 1, 2, 3,... ,n ]
        int[] sequence = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            sequence[i] = i;
        }
        // 初始解就是当前最优解
        bestSolution = evaluate(sequence);
        if (items.length == 1) {
            return new BatchResult(bestSolution, startTime, endTime, calFitness(bestSolution));
        }
        int[] bestSequence = sequence.clone();
        //System.out.println("初始解：" + calFitness(bestSolution));
        // 开始迭代，停止条件为达到指定迭代次数
        for (int t = 0; t < MAX_GEN; t++) {
            // 当前领域搜索次数
            int n = 0;
            List<Solution> LocalSolution = null;
            int[] LocalSequence = null;
            while (n <= N) {
                // 两两交换，得到当前序列的邻居序列
                int[] tempSequence = generateNewSequence(sequence);
                // 判断其是否在禁忌表中
                if (!isInTabuList(tempSequence)) {
                    List<Solution> tempSolution = evaluate(tempSequence);
                    if (LocalSolution == null || compareDouble(calFitness(tempSolution), calFitness(LocalSolution)) == -1) {
                        // 如果临时解优于本次领域搜索的最优解
                        // 那么就将临时解替换本次领域搜索的最优解
                        LocalSequence = tempSequence.clone();
                        LocalSolution = tempSolution;
                    }
                }
                n++;
            }
            // 如果局部搜索到了其他序列，则进行下面的流程
            if (LocalSequence != null) {
                if (compareDouble(calFitness(LocalSolution), calFitness(bestSolution)) == -1) {
                    // 如果本次搜索的最优解优于全局最优解
                    // 那么领域最优解替换全局最优解
                    bestT = t;
                    bestSequence = LocalSequence.clone();
                    bestSolution = evaluate(bestSequence);
                    //System.out.println("找到更优解: t = " + t + " , " + calFitness(bestSolution));
                }
                sequence = LocalSequence.clone();
                // 加入禁忌表
                enterTabuList(sequence);
            }
        }
        //System.out.println("-------------------------------------------------------------------------------------------------------------------------------");
        // 返回结果
        return new BatchResult(bestSolution, startTime, endTime, calFitness(bestSolution));
    }

    // 调整禁忌表长度的方法
    private void adjustTabuListLength() {
        int newTabuSize = minTabuSize + random.nextInt(maxTabuSize - minTabuSize + 1);
        if (newTabuSize != tabuList.length) {
            int[][] newTabuList = new int[newTabuSize][items.length];
            for (int i = 0; i < Math.min(tabuList.length, newTabuSize); i++) {
                newTabuList[i] = tabuList[i];
            }
            tabuList = newTabuList;
        }
    }

    //计算适应度值
    public double calFitness(List<Solution> solutions) {
        startTime.clear();
        endTime.clear();
        for (Solution solution : solutions) {
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
        return endTime.get(endTime.size() - 1);
    }

    /**
     * @param sequence 序列
     * @return void
     * @Description 将序列加入禁忌表
     */
    public void enterTabuList(int[] sequence) {
        adjustTabuListLength();
        if (l < tabuSize) {
            // 如果当前禁忌表还有空位，则直接加入即可
            tabuList[l] = sequence.clone();
            l++;
        } else {
            // 如果禁忌表已经满了，则移除第一个进表的路径，添加新的路径到禁忌表末尾
            // 后面的禁忌序列全部向前移动一位，覆盖掉当前第一个禁忌序列
            for (int i = 0; i < tabuList.length - 1; i++) {
                tabuList[i] = tabuList[i + 1].clone();
            }
            // 将sequence加入到禁忌队列的最后
            tabuList[tabuList.length - 1] = sequence.clone();
        }
    }

    /**
     * @param sequence 序列
     * @return 如序列是否存在于禁忌表中则返回true，反之返回false
     * @Description 判断序列是否存在于禁忌表中
     */
    public boolean isInTabuList(int[] sequence) {
//        int count = 0;
//        for (int[] ints : tabuList) {
//            for (int j = 0; j < ints.length; j++) {
//                if (sequence[j] != ints[j]) {
//                    count++;
//                    break;
//                }
//            }
//        }
//        return count != tabuList.length;
        for (int[] tabu : tabuList) {
            if (Arrays.equals(tabu, sequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param sequence 旧序列
     * @return 新序列
     * @Description 两两互换，根据旧序列生成新序列
     */
    public int[] generateNewSequence(int[] sequence) {
        //将 sequence 复制到 tempSequence
        int[] tempSequence = sequence.clone();
        // 获取两个不同的随机索引
        int r1 = random.nextInt(items.length);
        int r2 = random.nextInt(items.length);
        //System.out.println("r1 = " + r1 + " , r2 = " + r2 + ", items.length = "+ items.length);
        if (items.length == 1) {
            return tempSequence;
        }
        while (r1 == r2) {
            r2 = random.nextInt(items.length);
        }
        // 交换
        int temp = tempSequence[r1];
        tempSequence[r1] = tempSequence[r2];
        tempSequence[r2] = temp;
        return tempSequence;
    }

    /**
     * @param sequence 序列
     * @return 装载结果对象Solution
     * @Description 评价序列的函数：传入一个序列，以该顺序的矩形集合传入天际线算法进行装载
     */
    public List<Solution> evaluate(int[] sequence) {
        Item[] items = new Item[this.items.length];
        for (int i = 0; i < sequence.length; i++) {
            items[i] = this.items[sequence[i]];
        }
        return new SkyLinePacking(L, W, items, isRotateEnable).packings();
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

