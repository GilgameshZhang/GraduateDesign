package AlgorithmFrame.machineChoice.ga;

import AlgorithmFrame.bachSelect.aco.ACO;
import AlgorithmFrame.bachSelect.ga.Ga;
import AlgorithmFrame.bachSelect.tabuSearch.TabuSearch;
import ProblemFrame.BatchResult;
import ProblemFrame.Item;
import ProblemFrame.Machine;
import ProblemFrame.Solution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BatchGenome {
    //边界数组
    public Item[] items;
    //机器数组(input里输入)
    public Machine[] machines;
    //是否可以旋转
    public boolean isRotateEnable;
    //适应度函数值（装在利用率）
    public double fitness;
    //序列对应的装载结果列表
    public List<BatchResult> solutions;
    //加工工件机器序列
    public int[] genomeMachineArray;
    //机器对应的加工工件序列
    Map<Integer, List<Item>> machineGenomeMap;
    //用什么算法解决问题
    public String method;
    /**
     *
     * @param items 矩形集合
     * @param isRotateEnable 是否可以旋转
     */
    public BatchGenome(Item[] items, Machine[] machines,  boolean isRotateEnable, int[] genomeMachineArray, String method) {
        this.items = items;
        this.machines = machines;
        this.isRotateEnable = isRotateEnable;
        this.genomeMachineArray = genomeMachineArray;
        this.method = method;
        machineGenomeMap = new HashMap<>();
        solutions = new ArrayList<>();
    }

    /**
     * 获取适应度值和路径长度
     */
    public void updateFitnessAndSolution() {
        this.decode();
        double Cmax = 0.0;
        for (int i = 0; i < machines.length; i++) {
            Machine machine = machines[i];
            switch (method) {
                case "GA":
                    Ga ga = new Ga(500, 10, 5, 4,
                            0.98, 0.9, machine, machineGenomeMap.get(i), true);
                    BatchResult batchResult = ga.solve();
                    solutions.add(batchResult);
                    break;
                case "ACO":
                    ACO aco = new ACO(10, 500, 1, 1, 0.5, machine, machineGenomeMap.get(i),null, true);
                    BatchResult batchResultAco = aco.solve();
                    solutions.add(batchResultAco);
                    break;
                case "TabuSearch":
                    TabuSearch tabuSearch = new TabuSearch(500, 10, 10, machine, machineGenomeMap.get(i), null, true);
                    BatchResult batchResultTb = tabuSearch.solve();
                    solutions.add(batchResultTb);
                    break;
            }
            double m = !solutions.get(i).endTimes.isEmpty() ? solutions.get(i).endTimes.get(solutions.get(i).endTimes.size() - 1) : 0;
            Cmax = Math.max(Cmax, m);
        }
        fitness = Cmax;
    }

    public void decode() {
        //将Machine数组对应的Item放入对应机器中
        for (int i = 0; i < genomeMachineArray.length; i++) {
            if (machineGenomeMap.get(genomeMachineArray[i]) == null) {
                List<Item> printItem = new ArrayList<>();
                printItem.add(items[i]);
                machineGenomeMap.put(genomeMachineArray[i], printItem);
            } else {
                machineGenomeMap.get(genomeMachineArray[i]).add(items[i]);
            }
        }
    }
}
