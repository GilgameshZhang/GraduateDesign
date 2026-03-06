package AlgorithmFrame.machineChoice.ga;

import AlgorithmFrame.bachSelect.aco.ACO;
import AlgorithmFrame.bachSelect.ga.Ga;
import AlgorithmFrame.bachSelect.tabuSearch.TabuSearch;
import ProblemFrame.*;

import java.util.*;

public class BatchGenome {
    //边界数组
    public Item[] items;
    //机器数组(input里输入)
    public Machine[] machines;
    //是否可以旋转
    public boolean isRotateEnable;
    //适应度函数值（装在利用率）
    public double fitness;
    //最大完工时间
    public double cMax;
    //序列对应的装载结果列表
    public List<BatchResult> solutions;
    //加工工件序列
    public int[] genomeItemArray;
    //加工工件机器序列
    public int[] genomeMachineArray;
    //机器对应的加工工件序列
    Map<Integer, List<Item>> machineGenomeMap;
    //用什么算法解决问题
    public String method;

    //
    public int maxGen;

    public int populationSize;

    public int tabuSize;
    /**
     *
     * @param items 矩形集合
     * @param isRotateEnable 是否可以旋转
     */
    public BatchGenome(Item[] items, Machine[] machines,  boolean isRotateEnable, int[] genomeMachineArray, int[] genomeItemArray, String method) {
        this.items = items;
        this.machines = machines;
        this.isRotateEnable = isRotateEnable;
        this.genomeMachineArray = genomeMachineArray;
        this.genomeItemArray = genomeItemArray;
        this.method = method;
        machineGenomeMap = new HashMap<>();
        solutions = new ArrayList<>();
    }

    public BatchGenome(Item[] items, Machine[] machines,  boolean isRotateEnable, int[] genomeMachineArray, int[] genomeItemArray, String method, int maxGen, int populationSize, int tabuSize) {
        this.items = items;
        this.machines = machines;
        this.isRotateEnable = isRotateEnable;
        this.genomeMachineArray = genomeMachineArray;
        this.genomeItemArray = genomeItemArray;
        this.method = method;
        machineGenomeMap = new HashMap<>();
        solutions = new ArrayList<>();
        this.maxGen = maxGen;
        this.populationSize = populationSize;
        this.tabuSize = tabuSize;
    }

    /**
     * 获取适应度值和路径长度
     */
    public void updateFitnessAndSolution() {
        this.decode();
        double Cmax = 0.0;
        double wasteAverage = 0.0;
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
                    // 检查是否使用禁忌搜索（支持消融实验）
                    boolean useTabuSearch = Boolean.parseBoolean(
                        System.getProperty("ablation.useTabuSearch", "true")
                    );
                    
                    if (useTabuSearch) {
                        // 使用禁忌搜索
                        TabuSearch tabuSearch = new TabuSearch(maxGen, populationSize, tabuSize, machine, machineGenomeMap.get(i), null, true);
                        BatchResult batchResultTb = tabuSearch.solve();
                        updateGenome(batchResultTb, i);
                        solutions.add(batchResultTb);
                    } else {
                        // 不使用禁忌搜索（消融实验）- 使用最小参数
                        TabuSearch simpleSearch = new TabuSearch(1, 1, 1, machine, machineGenomeMap.get(i), null, true);
                        BatchResult batchResultTb = simpleSearch.solve();
                        updateGenome(batchResultTb, i);
                        solutions.add(batchResultTb);
                    }
                    break;
            }
            double m = !solutions.get(i).endTimes.isEmpty() ? solutions.get(i).endTimes.get(solutions.get(i).endTimes.size() - 1) : 0;
            Cmax = Math.max(Cmax, m);
            wasteAverage += solutions.get(i).fitness - m;
        }
//        for (int i = 0; i < this.genomeItemArray.length; i++) {
//            System.out.print(this.genomeItemArray[i] + " ");
//        }
//        System.out.println("主动解码后" + this.isValide());
        cMax = Cmax;
        fitness = Cmax + wasteAverage / machines.length;
    }

    public void decode() {
//        System.out.println("主动解码前" + this.isValide());
        //将Machine数组对应的Item放入对应机器中
        for (int i = 0; i < genomeMachineArray.length; i++) {
            if (machineGenomeMap.get(genomeMachineArray[i]) == null) {
                List<Item> printItem = new ArrayList<>();
                printItem.add(items[genomeItemArray[i]]);
                machineGenomeMap.put(genomeMachineArray[i], printItem);
            } else {
                machineGenomeMap.get(genomeMachineArray[i]).add(items[genomeItemArray[i]]);
            }
        }
    }

//    public boolean isValide() {
//        //染色体为1-9的数字且不重复
//        Set<Integer> set = new HashSet<>();
//        for (int i = 0; i < this.genomeItemArray.length; i++) {
//            if (set.contains(this.genomeItemArray[i]) || this.genomeItemArray[i] < 0 || this.genomeItemArray[i] > 9) {
//                return false;
//            }
//            set.add(this.genomeItemArray[i]);
//        }
//        return true;
//    }

    public void updateGenome(BatchResult batchResult, int machineIndex) {
        int[] genomeItemArray = this.genomeItemArray;
        //batchResult.solutions.size()为该机器的分批数量
        int index = 0;
        for (int i = 0; i < batchResult.solutions.size(); i++) {
            //遍历每一批次中的零件
            for (int j = 0; j < batchResult.solutions.get(i).placeItemList.size(); j++) {
                PlaceItem item = batchResult.solutions.get(i).placeItemList.get(j);
                while (index < this.genomeMachineArray.length) {
                    if (this.genomeMachineArray[index] == machineIndex) {
                        genomeItemArray[index] = Integer.parseInt(item.name) - 1;
                        index++;
                        break;
                    }
                    index++;
                }
            }
        }
    }
}
