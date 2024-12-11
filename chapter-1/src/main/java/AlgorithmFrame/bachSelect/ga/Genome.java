package AlgorithmFrame.bachSelect.ga;

import AlgorithmFrame.bachSelect.skyLine.SkyLinePacking;
import ProblemFrame.Item;
import ProblemFrame.Machine;
import ProblemFrame.Solution;

import java.util.ArrayList;
import java.util.List;

public class Genome {
    //边界数组
    public Item[] items;
    //机器
    Machine machine;
    List<Solution> batchPlan = new ArrayList<>();
    List<Double> startTime = new ArrayList<>();
    List<Double> endTime = new ArrayList<>();
    //是否可以旋转
    public boolean isRotateEnable = true;
    //基因序列
    public int[] genomeArray;
    //适应度函数值（装在利用率）
    public double fitness;

    public Genome(Item[] items,  boolean isRotateEnable, int[] genomeArray, Machine machine) {
        this.items = items;
        this.isRotateEnable = isRotateEnable;
        this.genomeArray = genomeArray;
        this.machine = machine;
    }

    /**
     *
     * @param items 矩形集合
     * @param isRotateEnable 是否可以旋转
     * @param genomeArray 基因序列
     */
    public Genome(Item[] items,  boolean isRotateEnable, int[] genomeArray, Machine machine, List<Solution> batchPlan, List<Double> startTime, List<Double> endTime) {
        this.items = items;
        this.isRotateEnable = isRotateEnable;
        this.genomeArray = genomeArray;
        this.machine = machine;
        this.batchPlan = batchPlan;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    /**
     * 获取适应度值和路径长度
     */
    public void updateFitnessAndSolution() {
        //解码
        List<Item> sequence = decode();
        batchPlan = new SkyLinePacking(machine.L, machine.W, sequence.toArray(new Item[0]), isRotateEnable).packings();
        for (Solution solution : batchPlan) {
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

    public List<Item> decode() {
        List<Item> sequence = new ArrayList<>();
        for (int i = 0; i < genomeArray.length; i++) {
                sequence.add(items[genomeArray[i] - 1]);
        }
        return sequence;
    }
}
