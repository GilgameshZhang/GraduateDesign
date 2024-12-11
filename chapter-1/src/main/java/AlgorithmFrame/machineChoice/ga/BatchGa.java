package AlgorithmFrame.machineChoice.ga;

import AlgorithmFrame.bachSelect.ga.Genome;
import ProblemFrame.*;

import java.util.*;

import static util.compareUtil.compareDouble;

public class BatchGa {
    //最大迭代次数
    public int MAX_GEN;
    //种群数量信息
    public int popSize;
    //变异兑换次数
    public double variationExchangeCount;
    //复制最优解的次数（选择种群的最优个体，然后复制几次，将最优个体复制多个，存到新的集合中）
    public int cloneNumOfBestIndividual;
    //基因蠕变概率
    public double mutationRate;
    //基因交叉概率
    public double crossoverRate;
    //种群总的适应度数值
    public double totalFitness;
    //最好的适应度对应的染色体
    public BatchGenome bestGenome;
    //放置所有的种群基因信息
    public List<BatchGenome> population = new ArrayList<>();
    //新一代种群的基因信息
    public List<BatchGenome> newPopulation = new ArrayList<>();
    //遗传的代数（）第几代
    private int t = 0;
    //最佳迭代次数
    private int bestT = -1;
    //随机函数对象
    public Random random;
    //各个个体的累积概率
    double[] probabilitys;
    //矩形数量
    int itemNum;
    //打印机器的数量
    int machineNum;
    //矩形数组
    public Item[] items;
    //机器数组
    public Machine[] machines;
    //是否可以旋转
    public boolean isRotateEnable;
    //选择分批的方法
    public String method;

    public BatchGa(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, double mutationRate, double crossoverRate, Input input, boolean isRotateEnable, String method) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.isRotateEnable = isRotateEnable;
        this.machines = input.machineList.toArray(new Machine[0]);
        this.items = input.itemList.toArray(new Item[0]);
        this.random = new Random();
        this.method = method;
    }

    /**
     * 算法总逻辑函数
     */
    public List<BatchResult> solve() {
        //初始化
        initVar();
        //迭代次数小于设定次数时进行迭代
        while (t < MAX_GEN) {
            popSize = population.size();
            //进行进化操作
            evolution();
            //更新种群
            population = copyGenomeList(newPopulation);
            t++;
            System.out.println("当前代数:" + t + ":" + bestGenome.fitness);
        }
        return bestGenome.solutions;
    }

    /**
     * 初始化种群
     */
    public void initVar() {
        this.itemNum = this.items.length;
        this.machineNum = this.machines.length;
        this.population = new ArrayList<>();
        for (int i = 0; i < popSize; i++) {
            //Collections.shuffle随机打乱原来的顺序生成初始单个染色体
            int[] machineSequence = new int[itemNum];
            for (int j = 0; j < machineSequence.length; j++) {
                while (true) {
                    int machineIndex = random.nextInt(machineNum);
                    if (items[j].h < machines[machineIndex].H && items[j].w < machines[machineIndex].W && items[j].l < machines[machineIndex].L) {
                        machineSequence[j] = machineIndex;
                        break;
                    }
                }
            }
            BatchGenome batchGenome = new BatchGenome(items, machines, isRotateEnable, machineSequence.clone(), method);
            //计算个体适应度值
            batchGenome.updateFitnessAndSolution();
            //将生成的染色体加入种群中
            population.add(batchGenome);
        }
        bestGenome = copyGenome(population.get(0));
        for (int i = 1; i < popSize; i++) {
            BatchGenome genome = population.get(i);
            if (bestGenome.fitness > genome.fitness) {
                bestGenome = copyGenome(genome);
            }
        }
        System.out.println("初始解为：" + bestGenome.fitness);
    }

    /**
     * 进化步骤
     */
    public void evolution() {
        //更新累积概率和总适应度值
        updateProbabilityAndTotalFitness();
        //挑选该代种群适应度最高的个体
        selectBestGenomeAndJoinNext();
        //轮盘赌选择策略对剩下的个体进行交叉变异
        //选两个父代，如果小于交叉概率则进行交叉，否则直接复制加入子代
        while (newPopulation.size() < population.size()) {
            int parentNum = 0;
            List<Integer> parent = new ArrayList<>();
            while (parentNum < 2) {
                double r = random.nextDouble();
                for (int j = 0; j < probabilitys.length; j++) {
                    if (compareDouble(r, probabilitys[j]) != 1) {
                        parent.add(j);
                        //newPopulation.add(population.get(j));
                        parentNum++;
                        break;
                    }
                }
            }
            if (compareDouble(random.nextDouble(), crossoverRate) != 1) {
//                int r = random.nextInt(popSize);
//                int k = random.nextInt(popSize);
//                while (r == k) {
//                    r = random.nextInt(popSize);
//                }
                cross(parent.get(0), parent.get(1));
            } else {
                newPopulation.add(population.get(parent.get(0)));
                newPopulation.add(population.get(parent.get(1)));
            }
        }
        for (int i = 0; i < population.size(); i++) {
            if (compareDouble(random.nextDouble(), mutationRate) != 1) {
                varation(i);
            }
        }
    }

    /**
     * 挑选种群中适应度最大的个体基因，并复制n份，直接加入下一代种群
     */
    private void updateProbabilityAndTotalFitness() {
        newPopulation = new ArrayList<>();
        BatchGenome tempBest = population.get(0);
        for (int i = 1; i < population.size(); i++) {
            if (population.get(i).fitness < tempBest.fitness) {
                tempBest = population.get(i);
            }
        }
        if (compareDouble(tempBest.fitness, bestGenome.fitness) == -1) {
            bestGenome = copyGenome(tempBest);
            bestT = t;
            //System.out.println("当前代数:" + t + ":" + bestGenome.fitness);
        }
        //xySeries.add(t,bestGenome.fitness);
        for (int i = 0; i < cloneNumOfBestIndividual; i++) {
            newPopulation.add(copyGenome(tempBest));
        }
    }

    /**
     * 更新个体累计概率和种群适应度值
     */
    private void selectBestGenomeAndJoinNext() {
        probabilitys = new double[population.size()];
        totalFitness = 0;
        for (BatchGenome genome : population) {
            totalFitness += 1 / genome.fitness;
        }
        double rate = 0.0;
        for (int i = 0; i < population.size(); i++) {
            rate += ((1 / population.get(i).fitness) / totalFitness);
            probabilitys[i] = rate;
        }
    }

    /**
     * 对两个基因进行单点交叉并进行去重操作
     * @param k 基因1的索引
     * @param r 基因2的索引
     */
    private void cross(int k, int r) {
        //获取相交的两个基因
        BatchGenome copy1 = copyGenome(population.get(k));
        BatchGenome copy2 = copyGenome(population.get(r));
        int[] machineArray1 = population.get(k).genomeMachineArray;
        int[] machineArray2 = population.get(r).genomeMachineArray;
        //找到交叉的位置
        int crossIndex1 = random.nextInt(itemNum);
        int crossIndex2 = random.nextInt(itemNum);
        //保证index1一定比index2小
        if (crossIndex2 < crossIndex1) {
            int temp = 0;
            temp = crossIndex2;
            crossIndex2 = crossIndex1;
            crossIndex1 = temp;
        }
        //获取相交的片段并交换
        for (int i = crossIndex1; i <= crossIndex2; i++) {
            int machineTemp = machineArray1[i];
            machineArray1[i] = machineArray2[i];
            machineArray2[i] = machineTemp;
        }
        //交叉完毕，将基因放回个体，再将个体放回种群，并更新他们的适应值和路径长度
        copy1.genomeMachineArray = machineArray1.clone();
        copy1.solutions.clear();
        copy1.updateFitnessAndSolution();
        newPopulation.add(copy1);
        copy2.genomeMachineArray = machineArray2.clone();
        copy2.solutions.clear();
        copy2.updateFitnessAndSolution();
        newPopulation.add(copy2);
//        newPopulation.get(k).setGenomeArray(genomeArray1);
//        newPopulation.get(k).updateFitnessAndSolution();
//        newPopulation.get(r).setGenomeArray(genomeArray2);
//        newPopulation.get(r).updateFitnessAndSolution();
    }

    /**
     * 变异操作
     * @param k 变异的基因idx
     */
    private void varation(int k) {
        BatchGenome genome = newPopulation.get(k);
        int[] machineArray = genome.genomeMachineArray;
        //找到最大和最小的机器号-1
        int maxIndex = 0;
        int minIndex = 0;
        for (int i = 1; i < genome.solutions.size(); i++) {
            if (genome.solutions.get(i).endTimes.get(genome.solutions.get(i).endTimes.size() - 1) > genome.solutions.get(maxIndex).endTimes.get(genome.solutions.get(maxIndex).endTimes.size() - 1)) {
                maxIndex = i;
            }
            if (genome.solutions.get(i).endTimes.get(genome.solutions.get(i).endTimes.size() - 1) < genome.solutions.get(minIndex).endTimes.get(genome.solutions.get(minIndex).endTimes.size() - 1)) {
                minIndex = i;
            }
        }
        //机器号-零件号映射
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        for (int i = 0; i < machineArray.length; i++) {
            if (machineMap.containsKey(machineArray[i])) {
                machineMap.get(machineArray[i]).add(i);
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(i);
                machineMap.put(machineArray[i], list);
            }
        }
        //将加工时间最长的机器上的一个零件分给加工时间最短的机器
        int r1 = random.nextInt(machineMap.get(maxIndex).size());
        machineArray[machineMap.get(maxIndex).get(r1)] = minIndex + 1;
//        int r1 = random.nextInt(machineArray.length);
//        int machinetemp = machineArray[r1];
//        int count = 0;
//        while (true) {
//            int machineIndex = random.nextInt(machineNum);
//            if (machineIndex != machinetemp && items[r1].h < machines[machineIndex].H && items[r1].w < machines[machineIndex].W && items[r1].l < machines[machineIndex].L) {
//                machineArray[r1] = machineIndex;
//                break;
//            }
//            count++;
//            if (count > 100) {
//                break;
//            }
//        }
        //将变异后的基因序列放回个体
        genome.genomeMachineArray = machineArray.clone();
        genome.solutions.clear();
        //更新基因的适应值和路径长短
        genome.updateFitnessAndSolution();
        //将变异后的个体放回种群
        newPopulation.set(k, genome);
    }



    /**
     * 复制染色体
     */
    public BatchGenome copyGenome(BatchGenome genome) {
        BatchGenome copy = new BatchGenome(genome.items, genome.machines, genome.isRotateEnable, genome.genomeMachineArray.clone(), genome.method);
        copy.solutions = new ArrayList<>(genome.solutions);
        copy.fitness = genome.fitness;
        return copy;
    }

    /**
     * 根据传入的模板基因集合，进行拷贝，返回拷贝好的基因集合
     * @param genomeList 模板基因集合
     * @return 返回拷贝好的模板基因集合
     */
    private List<BatchGenome> copyGenomeList(List<BatchGenome> genomeList) {
        List<BatchGenome> copyList = new ArrayList<>();
        for (BatchGenome genome : genomeList) {
            copyList.add(copyGenome(genome));
        }
        return copyList;
    }

}
