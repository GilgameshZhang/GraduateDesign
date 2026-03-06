package AlgorthmFrame.tabuSearch;

import ProblemFrame.SkyLinePacking;
import ProgramEntity.Item;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Solution;

import java.util.*;

/**
 * 打印机零件排布禁忌搜索
 * 用于优化单台打印机上的零件排列顺序，以减少分批数和打印时间
 */
public class PrintTabuSearch {

    // 迭代次数
    private int maxIterations;
    // 局部搜索的次数
    private int neighborSearchCount;
    // 最小和最大禁忌表长度
    private int minTabuSize;
    private int maxTabuSize;
    // 禁忌表
    private int[][] tabuList;
    // 当前禁忌表长度
    private int currentTabuLength = 0;
    // 随机函数对象
    private Random random;
    // 打印机
    private PrintMachine printMachine;
    // 打印机的长和宽
    private double L;
    private double W;
    // 零件数组
    private Item[] items;
    // 是否可以旋转
    private boolean isRotateEnable;
    // 当前最好解
    private List<Solution> bestSolution;
    // 最佳序列（优化后的零件排列顺序）
    private int[] bestSequence;
    // 最佳迭代次数
    private int bestIteration = -1;

    /**
     * 构造函数
     * @param maxIterations 最大迭代次数
     * @param neighborSearchCount 每次迭代的邻域搜索次数
     * @param minTabuSize 最小禁忌表长度
     * @param printMachine 打印机
     * @param itemList 零件列表（该列表的顺序将作为禁忌搜索的初始解）
     * @param random 随机数生成器
     * @param isRotateEnable 是否允许旋转
     */
    public PrintTabuSearch(int maxIterations, int neighborSearchCount, int minTabuSize,
                           PrintMachine printMachine, List<Item> itemList, 
                           Random random, boolean isRotateEnable) {
        this.maxIterations = maxIterations;
        this.neighborSearchCount = neighborSearchCount;
        this.minTabuSize = minTabuSize;
        this.printMachine = printMachine;
        this.L = printMachine.L;
        this.W = printMachine.W;
        this.isRotateEnable = isRotateEnable;
        this.random = random;
        
        // 将itemList转换为数组，保持原始顺序
        // 这个顺序将作为禁忌搜索的初始解
        if (itemList == null || itemList.isEmpty()) {
            this.items = new Item[0];
        } else {
            this.items = itemList.toArray(new Item[0]);
        }
        
        if (itemList != null && !itemList.isEmpty()) {
            this.maxTabuSize = minTabuSize + itemList.size();
            this.tabuList = new int[minTabuSize][itemList.size()];
        }
    }

    /**
     * 禁忌搜索主函数
     * @return 最佳装载结果
     */
    public List<Solution> solve() {
        if (items.length == 0) {
            return new ArrayList<>();
        }
        
        // 如果只有一个零件，直接装箱返回
        if (items.length == 1) {
            return evaluate(new int[]{0});
        }
        
        // 初始解就按照给定的顺序进行
        int[] initialSequence = getInitialSequence();
        bestSolution = evaluate(initialSequence);
        this.bestSequence = initialSequence.clone();  // 保存到成员变量
        int[] currentSequence = initialSequence.clone();
        
        // 开始迭代
        for (int iter = 0; iter < maxIterations; iter++) {
            // 当前迭代的局部最优解
            List<Solution> localBestSolution = null;
            int[] localBestSequence = null;
            double localBestFitness = Double.MAX_VALUE;
            
            // 邻域搜索
            for (int n = 0; n < neighborSearchCount; n++) {
                // 生成邻域解
                int[] neighborSequence = generateNeighbor(currentSequence);
                
                // 判断是否在禁忌表中
                if (!isTabu(neighborSequence)) {
                    List<Solution> neighborSolution = evaluate(neighborSequence);
                    double neighborFitness = calculateFitness(neighborSolution);
                    
                    // 如果邻域解优于局部最优解，更新局部最优
                    if (neighborFitness < localBestFitness) {
                        localBestSequence = neighborSequence.clone();
                        localBestSolution = neighborSolution;
                        localBestFitness = neighborFitness;
                    }
                }
            }
            
            // 如果找到了局部最优解
            if (localBestSequence != null) {
                double currentBestFitness = calculateFitness(bestSolution);
                
                // 如果局部最优解优于全局最优解，更新全局最优
                if (localBestFitness < currentBestFitness) {
                    this.bestSequence = localBestSequence.clone();
                    bestSolution = localBestSolution;
                    bestIteration = iter;
                }
                
                // 更新当前解为局部最优解
                currentSequence = localBestSequence.clone();
                
                // 将局部最优解加入禁忌表
                addToTabuList(localBestSequence);
            }
        }
        
        return bestSolution;
    }
    
    /**
     * 获取最优序列（优化后的零件排列顺序）
     * @return 最优序列的索引数组，表示优化后的零件排列顺序
     */
    public int[] getBestSequence() {
        return bestSequence;
    }

    /**
     * 获取初始解
     * 直接使用传入的itemList的顺序作为初始解
     * 即初始序列为[0,1,2,3,...,n-1]，表示按照items数组（即itemList的原始顺序）进行装箱
     */
    private int[] getInitialSequence() {
        // 创建索引数组，直接按照items数组的顺序（即传入的itemList的顺序）
        int[] sequence = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            sequence[i] = i;  // 索引i对应items[i]，保持itemList的原始顺序
        }
        
        return sequence;
    }

    /**
     * 生成邻域解
     * 使用三种邻域结构：Swap、2-opt、Insert
     */
    private int[] generateNeighbor(int[] sequence) {
        int[] neighbor = sequence.clone();
        
        // 随机选择两个不同的位置
        int pos1 = random.nextInt(items.length);
        int pos2 = random.nextInt(items.length);
        while (pos1 == pos2 && items.length > 1) {
            pos2 = random.nextInt(items.length);
        }
        
        // 随机选择一种邻域结构
        int method = random.nextInt(3);
        
        switch (method) {
            case 0:
                // Swap: 交换两个位置的元素
                int temp = neighbor[pos1];
                neighbor[pos1] = neighbor[pos2];
                neighbor[pos2] = temp;
                break;
                
            case 1:
                // 2-opt: 反转两个位置之间的子序列
                if (pos1 > pos2) {
                    int t = pos1;
                    pos1 = pos2;
                    pos2 = t;
                }
                while (pos1 < pos2) {
                    int tempRev = neighbor[pos1];
                    neighbor[pos1] = neighbor[pos2];
                    neighbor[pos2] = tempRev;
                    pos1++;
                    pos2--;
                }
                break;
                
            case 2:
                // Insert: 将一个元素插入到另一个位置
                int element = neighbor[pos1];
                if (pos1 < pos2) {
                    System.arraycopy(neighbor, pos1 + 1, neighbor, pos1, pos2 - pos1);
                    neighbor[pos2] = element;
                } else {
                    System.arraycopy(neighbor, pos2, neighbor, pos2 + 1, pos1 - pos2);
                    neighbor[pos2] = element;
                }
                break;
        }
        
        return neighbor;
    }

    /**
     * 判断序列是否在禁忌表中
     */
    private boolean isTabu(int[] sequence) {
        for (int i = 0; i < currentTabuLength; i++) {
            if (Arrays.equals(tabuList[i], sequence)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将序列加入禁忌表
     */
    private void addToTabuList(int[] sequence) {
        // 动态调整禁忌表长度
        adjustTabuLength();
        
        if (currentTabuLength < tabuList.length) {
            // 禁忌表还有空位，直接加入
            tabuList[currentTabuLength] = sequence.clone();
            currentTabuLength++;
        } else {
            // 禁忌表已满，采用先进先出策略
            for (int i = 0; i < tabuList.length - 1; i++) {
                tabuList[i] = tabuList[i + 1];
            }
            tabuList[tabuList.length - 1] = sequence.clone();
        }
    }

    /**
     * 动态调整禁忌表长度
     */
    private void adjustTabuLength() {
        int newTabuSize = minTabuSize + random.nextInt(maxTabuSize - minTabuSize + 1);
        if (newTabuSize != tabuList.length) {
            int[][] newTabuList = new int[newTabuSize][items.length];
            int copyLength = Math.min(currentTabuLength, newTabuSize);
            
            if (tabuList.length <= newTabuSize) {
                // 禁忌表变大，复制所有现有元素
                for (int i = 0; i < copyLength; i++) {
                    newTabuList[i] = tabuList[i];
                }
            } else {
                // 禁忌表变小，只保留最新的元素
                int startIdx = currentTabuLength - copyLength;
                for (int i = 0; i < copyLength; i++) {
                    newTabuList[i] = tabuList[startIdx + i];
                }
            }
            
            tabuList = newTabuList;
            currentTabuLength = Math.min(currentTabuLength, newTabuSize);
        }
    }

    /**
     * 评估序列（调用装箱算法）
     */
    private List<Solution> evaluate(int[] sequence) {
        Item[] orderedItems = new Item[items.length];
        for (int i = 0; i < sequence.length; i++) {
            orderedItems[i] = items[sequence[i]];
        }
        return new SkyLinePacking(L, W, orderedItems, isRotateEnable).packings();
    }

    /**
     * 计算适应度值
     * 目标：最小化该打印机器的完工时间（makespan）
     * 完工时间 = 所有批次的打印时间总和
     */
    private double calculateFitness(List<Solution> solutions) {
        if (solutions == null || solutions.isEmpty()) {
            return Double.MAX_VALUE;
        }
        
        // 计算该打印机器的完工时间（所有批次顺序执行）
        double makespan = 0.0;
        
        for (Solution solution : solutions) {
            // 打印时间 = 准备时间 + 层高相关的打印时间
            double printTime = printMachine.prepareTime + 
                             printMachine.reCoatingTime * solution.maxG / printMachine.printH;
            makespan += printTime;
        }
        
        // 适应度 = 完工时间（越小越好）
        return makespan;
    }

    /**
     * 获取最佳迭代次数
     */
    public int getBestIteration() {
        return bestIteration;
    }
}

