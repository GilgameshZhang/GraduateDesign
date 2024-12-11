package AlgorithmFrame.bachSelect.ga;

import AlgorithmFrame.machineChoice.ga.BatchGenome;
import ProblemFrame.*;

import java.util.*;

import static util.compareUtil.compareDouble;

public class Ga {
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
    public Genome bestGenome;
    //放置所有的种群基因信息
    public List<Genome> population = new ArrayList<>();
    //新一代种群的基因信息
    public List<Genome> newPopulation = new ArrayList<>();
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
    Machine machine;
    //矩形数组
    public Item[] items;
    //是否可以旋转
    public boolean isRotateEnable;

    public Ga(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, double mutationRate, double crossoverRate, Machine machine, List<Item> items, boolean isRotateEnable) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.machine = machine;
        this.random = new Random();
        if (items == null || items.size() == 0) {
            this.items = null;
        } else {
            this.items = items.toArray(new Item[0]);
        }
        this.isRotateEnable = isRotateEnable;
    }

    /**
     * 算法总逻辑函数
     */
    public BatchResult solve() {
        if (items == null || items.length == 0) {
            return new BatchResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        }
        //初始化
        initVar();
        if (items.length == 1) {
            return new BatchResult(population.get(0).batchPlan, population.get(0).startTime, population.get(0).endTime, population.get(0).fitness);
        }
        //迭代次数小于设定次数时进行迭代
        while (t < MAX_GEN) {
            popSize = population.size();
            //进行进化操作
            evolution();
            //更新种群
            population = copyGenomeList(newPopulation);
            t++;
            //System.out.println("当前代数:" + t + ":" + bestGenome.fitness);
        }
        //返回最好的染色体
        return new BatchResult(bestGenome.batchPlan, bestGenome.startTime, bestGenome.endTime, bestGenome.fitness);
    }

    /**
     * 初始化种群
     */
    public void initVar() {
        this.itemNum = this.items.length;
        this.population = new ArrayList<>();
        //初始化种群信息
        List<Integer> sequence = new ArrayList<>();
        for (int i = 1; i <= itemNum; i++) {
            sequence.add(i);
        }
        for (int i = 0; i < popSize; i++) {
            //Collections.shuffle随机打乱原来的顺序生成初始单个染色体
            Collections.shuffle(sequence);
            int[] initSequence = new int[itemNum];
            for (int j = 0; j < sequence.size(); j++) {
                initSequence[j] = sequence.get(j);
            }
            //生成染色体
            Genome genome = new Genome(items, isRotateEnable, initSequence.clone(), machine);
            //计算个体适应度值
            genome.updateFitnessAndSolution();
            //将生成的染色体加入种群中
            population.add(genome);
        }
        bestGenome = copyGenome(population.get(0));
        for (int i = 1; i < popSize; i++) {
            Genome genome = population.get(i);
            if (bestGenome.fitness > genome.fitness) {
                bestGenome = copyGenome(genome);
            }
        }
        //System.out.println("初始解为：" + bestGenome.fitness);
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
//                System.out.println("——————交叉————————");
                cross(parent.get(0), parent.get(1));
            } else {
                newPopulation.add(population.get(parent.get(0)));
                newPopulation.add(population.get(parent.get(1)));
            }
        }
        for (int i = 0; i < population.size(); i++) {
            if (compareDouble(random.nextDouble(), mutationRate) != 1) {
//                System.out.println("——————变异————————");
                varation(i);
            }
        }
    }

    /**
     * 挑选种群中适应度最大的个体基因，并复制n份，直接加入下一代种群
     */
    private void updateProbabilityAndTotalFitness() {
        newPopulation = new ArrayList<>();
        Genome tempBest = population.get(0);
        for (int i = 1; i < population.size(); i++) {
            if (population.get(i).fitness < tempBest.fitness) {
                tempBest = population.get(i);
            }
        }
        if (compareDouble(tempBest.fitness, bestGenome.fitness) == -1) {
            bestGenome = copyGenome(tempBest);
            bestT = t;
//            System.out.println("当前代数:" + t + ":" + bestGenome.fitness);
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
        for (Genome genome : population) {
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
        Genome copy1 = copyGenome(population.get(k));
        Genome copy2 = copyGenome(population.get(r));
        int[] genomeArray1 = population.get(k).genomeArray;
        int[] genomeArray2 = population.get(r).genomeArray;
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
            int temp = genomeArray1[i];
            genomeArray1[i] = genomeArray2[i];
            genomeArray2[i] = temp;
        }
        //找到重复的基因并去重
        HashSet<Integer> set = new HashSet<>();
        //map<index, value>
        HashMap<Integer, Integer> repeatMap = new HashMap<>();
        for (int i = 0; i < genomeArray1.length; i++) {
            //set.add方法当set中没有重复元素，成功添加返回true，当有重复元素时,添加失败并返回flase
            if (!set.add(genomeArray1[i])) {
                //将重复基因的序号和内容存进Map中
                repeatMap.put(i, genomeArray1[i]);
            }
        }
        set.clear();
        for (int i = 0; i <genomeArray2.length; i++) {
            if (!set.add(genomeArray2[i])) {
                //交换重复的基因
                for (int key : repeatMap.keySet()) {
                    genomeArray1[key] = genomeArray2[i];
                    genomeArray2[i] = repeatMap.get(key);
                    repeatMap.remove(key);
                    break;
                }
            }
        }
        //交叉完毕，将基因放回个体，再将个体放回种群，并更新他们的适应值和路径长度
        copy1.genomeArray = genomeArray1.clone();
        copy1.batchPlan.clear();
        copy1.startTime.clear();
        copy1.endTime.clear();
        copy1.updateFitnessAndSolution();
        newPopulation.add(copy1);
        copy2.genomeArray = genomeArray2.clone();
        copy2.batchPlan.clear();
        copy2.startTime.clear();
        copy2.endTime.clear();
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
        Genome genome = newPopulation.get(k);
        int[] genomeArray = genome.genomeArray;
        for (int i = 0; i < variationExchangeCount; i++) {
            int r1 = random.nextInt(itemNum);
            int r2 = random.nextInt(itemNum);
            while (r1 == r2) {
                r2 = random.nextInt(itemNum);
            }
            //交换
            int temp = genomeArray[r1];
            genomeArray[r1] = genomeArray[r2];
            genomeArray[r2] = temp;
        }
        //将变异后的基因序列放回个体
        genome.genomeArray = genomeArray.clone();
        genome.batchPlan.clear();
        genome.startTime.clear();
        genome.endTime.clear();
        //更新基因的适应值和路径长短
        genome.updateFitnessAndSolution();
        //将变异后的个体放回种群
        newPopulation.set(k, genome);
    }



    /**
     * 复制染色体
     */
    public Genome copyGenome(Genome genome) {
        Genome copy = new Genome(genome.items, genome.isRotateEnable, genome.genomeArray.clone(), genome.machine, new ArrayList<>(genome.batchPlan), new ArrayList<>(genome.startTime), new ArrayList<>(genome.endTime));
        copy.fitness = genome.fitness;
        return copy;
    }

    /**
     * 根据传入的模板基因集合，进行拷贝，返回拷贝好的基因集合
     * @param genomeList 模板基因集合
     * @return 返回拷贝好的模板基因集合
     */
    private List<Genome> copyGenomeList(List<Genome> genomeList) {
        List<Genome> copyList = new ArrayList<>();
        for (Genome genome : genomeList) {
            copyList.add(copyGenome(genome));
        }
        return copyList;
    }
}
