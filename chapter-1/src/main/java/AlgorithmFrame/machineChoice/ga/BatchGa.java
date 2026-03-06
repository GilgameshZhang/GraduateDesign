package AlgorithmFrame.machineChoice.ga;

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

    // Counter for generations with the same best solution
    private int sameBestCount = 0;
    // Threshold for increasing mutation rate
    private final int SAME_BEST_THRESHOLD = 5;
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
    public int t = 0;
    //最佳迭代次数
    public int bestT = -1;
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
    // 自适应变异率
    private double initialMutationRate;
    private double tempMutationRate;
    
    // 初始化策略参数：启发式初始化的比例 (0.0-1.0)
    // 0.0 = 全部随机, 1.0 = 全部启发式, 0.5 = 50%启发式 + 50%随机
    public double heuristicInitRatio = 1.0; // 默认全部启发式（保持原有行为）

    public int decodeMaxGen;

    public int decodeTabuSize;

    public int decodeMaxN;
    
    // 时间限制（毫秒）
    public long timeLimitMs;



    public BatchGa(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, double mutationRate, double crossoverRate, Input input, boolean isRotateEnable, String method) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.initialMutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.isRotateEnable = isRotateEnable;
        this.machines = input.machineList.toArray(new Machine[0]);
        this.items = input.itemList.toArray(new Item[0]);
        this.random = new Random();
        this.method = method;
    }

    public BatchGa(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, double mutationRate, double crossoverRate, Input input, boolean isRotateEnable, String method, int decodeMaxGen, int decodeTabuSize, int decodeMaxN) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.initialMutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.isRotateEnable = isRotateEnable;
        this.machines = input.machineList.toArray(new Machine[0]);
        this.items = input.itemList.toArray(new Item[0]);
        this.random = new Random();
        this.method = method;
        this.decodeMaxGen = decodeMaxGen;
        this.decodeTabuSize = decodeTabuSize;
        this.decodeMaxN = decodeMaxN;
        this.timeLimitMs = 0; // 默认不使用时间限制
    }
    
    /**
     * 构造函数（带时间限制）
     */
    public BatchGa(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, 
                   double mutationRate, double crossoverRate, Input input, boolean isRotateEnable, 
                   String method, int decodeMaxGen, int decodeTabuSize, int decodeMaxN, long timeLimitMs) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.initialMutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.isRotateEnable = isRotateEnable;
        this.machines = input.machineList.toArray(new Machine[0]);
        this.items = input.itemList.toArray(new Item[0]);
        this.random = new Random();
        this.method = method;
        this.decodeMaxGen = decodeMaxGen;
        this.decodeTabuSize = decodeTabuSize;
        this.decodeMaxN = decodeMaxN;
        this.timeLimitMs = timeLimitMs;
    }
    
    /**
     * 构造函数（带时间限制和初始化比例）
     * @param heuristicInitRatio 启发式初始化的比例 (0.0-1.0)
     *                          0.0 = 全部随机初始化
     *                          1.0 = 全部启发式初始化  
     *                          0.5 = 50%启发式 + 50%随机
     */
    public BatchGa(int MAX_GEN, int popSize, double variationExchangeCount, int cloneNumOfBestIndividual, 
                   double mutationRate, double crossoverRate, Input input, boolean isRotateEnable, 
                   String method, int decodeMaxGen, int decodeTabuSize, int decodeMaxN, long timeLimitMs,
                   double heuristicInitRatio) {
        this.MAX_GEN = MAX_GEN;
        this.popSize = popSize;
        this.variationExchangeCount = variationExchangeCount;
        this.cloneNumOfBestIndividual = cloneNumOfBestIndividual;
        this.mutationRate = mutationRate;
        this.initialMutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.isRotateEnable = isRotateEnable;
        this.machines = input.machineList.toArray(new Machine[0]);
        this.items = input.itemList.toArray(new Item[0]);
        this.random = new Random();
        this.method = method;
        this.decodeMaxGen = decodeMaxGen;
        this.decodeTabuSize = decodeTabuSize;
        this.decodeMaxN = decodeMaxN;
        this.timeLimitMs = timeLimitMs;
        this.heuristicInitRatio = Math.max(0.0, Math.min(1.0, heuristicInitRatio)); // 确保在[0,1]范围内
    }

    /**
     * 算法总逻辑函数
     */
    public Result solve() {
        //初始化
        initVar();
        List<Double> itreatorList = new ArrayList<>();
        
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        
        //迭代次数小于设定次数时进行迭代
        // 如果设置了时间限制，则使用时间限制；否则使用迭代次数限制
        while (true) {
            // 检查时间限制
            if (timeLimitMs > 0) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime >= timeLimitMs) {
                    System.out.println("达到时间限制 " + timeLimitMs + "ms，算法终止");
                    System.out.println("总共完成 " + t + " 代，最优Cmax: " + bestGenome.cMax);
                    break;
                }
            }
            
            popSize = population.size();
            //进行进化操作
            evolution();
            //更新种群
            population = copyGenomeList(newPopulation);
            t++;
            
            // 每10代输出一次进度
            if (timeLimitMs > 0 && t % 10 == 0) {
                long elapsedTime = System.currentTimeMillis() - startTime;
                double progress = (elapsedTime * 100.0) / timeLimitMs;
                System.out.println(String.format("代数:%d, Cmax:%.2f, 进度:%.1f%% (%.1fs/%.1fs)", 
                    t, bestGenome.cMax, progress, elapsedTime/1000.0, timeLimitMs/1000.0));
            } else {
                System.out.println("当前代数:" + t + ":" + bestGenome.cMax);
            }
            
            itreatorList.add(bestGenome.cMax);
        }
        //返回最好的染色体
        return new Result(bestGenome.solutions, itreatorList);
    }

    /**
     * 初始化种群（支持混合初始化策略）
     * 根据 heuristicInitRatio 参数决定启发式初始化和随机初始化的比例
     */
    public void initVar() {
        this.itemNum = this.items.length;
        this.machineNum = this.machines.length;
        List<Integer> genomeArray1 = new ArrayList<>();
        for (int i = 0; i < itemNum; i++) {
            genomeArray1.add(i);
        }
        //统计每个机器的加工能力，并进行归一化处理
        double[] machinePrintAbility = new double[machineNum];
        double sum = 0.0;
        for (int i = 0; i < machineNum; i++) {
            machinePrintAbility[i] = 1 * machines[i].reCoatingTime / machines[i].printH;
            sum += machinePrintAbility[i];
        }
        for (int i = 0; i < machineNum; i++) {
            machinePrintAbility[i] /= sum;
        }
        this.population = new ArrayList<>();
        
        // 计算启发式初始化的个体数量
        int heuristicCount = (int) Math.ceil(popSize * heuristicInitRatio);
        int randomCount = popSize - heuristicCount;
        
        System.out.println(String.format("种群初始化策略: 启发式=%d个(%.0f%%), 随机=%d个(%.0f%%)", 
            heuristicCount, heuristicInitRatio * 100, randomCount, (1 - heuristicInitRatio) * 100));
        
        // 1. 生成启发式初始化的个体
        for (int i = 0; i < heuristicCount; i++) {
            Integer[] itemSequence = new Integer[itemNum];
            for (int j = 0; j < itemSequence.length; j++) {
                itemSequence[j] = genomeArray1.get(j);
            }
            //对零件进行排序从高到低（启发式规则）
            Arrays.sort(itemSequence, (o1, o2) -> (int) (items[o2].h - items[o1].h));
            int[] machineSequence = new int[itemNum];
            for (int j = 0; j < machineSequence.length; j++) {
                while (true) {
                    double r = random.nextDouble();
                    double sum1 = 0.0;
                    for (int k = 0; k < machineNum; k++) {
                        sum1 += machinePrintAbility[k];
                        if (compareDouble(r, sum1) != 1) {
                            machineSequence[j] = k;
                            break;
                        }
                    }
                    if (items[itemSequence[j]].h < machines[machineSequence[j]].H && items[itemSequence[j]].w < machines[machineSequence[j]].W && items[itemSequence[j]].l < machines[machineSequence[j]].L) {
                        break;
                    }
                }
            }
            // Convert Integer[] to int[]
            int[] itemSequenceInt = Arrays.stream(itemSequence).mapToInt(Integer::intValue).toArray();
            BatchGenome batchGenome = new BatchGenome(items, machines, isRotateEnable, machineSequence, itemSequenceInt, method);
            batchGenome.updateFitnessAndSolution();
            population.add(batchGenome);
        }
        
        // 2. 生成随机初始化的个体
        for (int i = 0; i < randomCount; i++) {
            //Collections.shuffle随机打乱原来的顺序生成初始单个染色体（随机初始化）
            Collections.shuffle(genomeArray1);
            int[] itemSequence = new int[itemNum];
            for (int j = 0; j < itemSequence.length; j++) {
                itemSequence[j] = genomeArray1.get(j);
            }
            int[] machineSequence = new int[itemNum];
            for (int j = 0; j < machineSequence.length; j++) {
                while (true) {
                    int machineIndex = random.nextInt(machineNum);
                    if (items[itemSequence[j]].h < machines[machineIndex].H && items[itemSequence[j]].w < machines[machineIndex].W && items[itemSequence[j]].l < machines[machineIndex].L) {
                        machineSequence[j] = machineIndex;
                        break;
                    }
                }
            }
            BatchGenome batchGenome = new BatchGenome(items, machines, isRotateEnable, machineSequence.clone(), itemSequence.clone(), method);
            //计算个体适应度值
            batchGenome.updateFitnessAndSolution();
            //将生成的染色体加入种群中
            population.add(batchGenome);
        }
        
        //找出最优初始解
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
        // 动态调整变异率
        double currentMutationRate = tempMutationRate;
        for (int i = 1; i < population.size(); i++) {
            if (compareDouble(random.nextDouble(), currentMutationRate) != 1) {
                varation(i);
            }
        }
//        for (int i = 0; i < population.size(); i++) {
//            if (compareDouble(random.nextDouble(), mutationRate) != 1) {
//                varation(i);
//            }
//        }
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
        population.set(0, tempBest);
        if (compareDouble(tempBest.fitness, bestGenome.fitness) == -1) {
            bestGenome = copyGenome(tempBest);
            bestT = t;
            sameBestCount = 0; // Reset counter
            tempMutationRate = initialMutationRate; // Reset mutation rate
            //System.out.println("当前代数:" + t + ":" + bestGenome.fitness);
        } else {
            sameBestCount++;
            if (sameBestCount >= SAME_BEST_THRESHOLD) {
                 tempMutationRate = Math.min(initialMutationRate + 0.05, 0.9); // Increase mutation rate
            }
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
        probabilitys[0] = -1;
        totalFitness = 0;
        for (int i = 1; i < population.size(); i++) {
            totalFitness += 1 / (population.get(i).fitness - 150000);
        }
        double rate = 0.0;
        for (int i = 1; i < population.size(); i++) {
            rate += ((1 / (population.get(i).fitness - 150000)) / totalFitness);
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
        int[] machineArray1 = copy1.genomeMachineArray;
        int[] itemArray1 = copy1.genomeItemArray;
        int[] machineArray2 = copy2.genomeMachineArray;
        int[] itemArray2 = copy2.genomeItemArray;
//        //找到交叉的位置
//        int crossIndex1 = random.nextInt(itemNum);
//        int crossIndex2 = random.nextInt(itemNum);
//        //保证index1一定比index2小
//        if (crossIndex2 < crossIndex1) {
//            int temp = 0;
//            temp = crossIndex2;
//            crossIndex2 = crossIndex1;
//            crossIndex1 = temp;
//        }
//        //获取相交的片段并交换
//        for (int i = crossIndex1; i <= crossIndex2; i++) {
//            int machineTemp = machineArray1[i];
//            machineArray1[i] = machineArray2[i];
//            machineArray2[i] = machineTemp;
//        }
        //采用POX交叉
        //将所有的零件号分成随机两个集合
        List<Integer> job1 = new ArrayList<>();
        List<Integer> job2 = new ArrayList<>();
        for (int i = 0; i < itemNum; i++) {
            if (random.nextBoolean()) {
                job1.add(i);
            } else {
                job2.add(i);
            }
        }
        //复制Parent1 包含在J1的工件到Children1，Parent2包含在J1的工件到Children2，保留它们的位置。
        int[] genomeArray1 = new int[itemNum];
        int[] genomeMachineArray1 = new int[itemNum];
        int[] genomeArray2 = new int[itemNum];
        int[] genomeMachineArray2 = new int[itemNum];
        Arrays.fill(genomeArray1, -1);
        Arrays.fill(genomeArray2, -1);
        for (int i = 0; i < job1.size(); i++) {
            for(int j = 0; j < itemArray1.length; j++) {
                if (itemArray1[j] == job1.get(i)) {
                    genomeArray1[j] = itemArray1[j];
                    genomeMachineArray1[j] = machineArray1[j];
                }
                if (itemArray2[j] == job1.get(i)) {
                    genomeArray2[j] = itemArray2[j];
                    genomeMachineArray2[j] = machineArray2[j];
                }
            }
        }
        //将Parent2中不包含在J1的工件按照顺序放入Children1，Parent1中不包含在J1的工件按照顺序放入Children2。
        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < itemNum; i++) {
            if (genomeArray1[i] == -1) {
                while (index1 < itemArray2.length && job1.contains(itemArray2[index1])) {
                    index1++;
                }
                if (index1 < itemArray2.length) {
                    genomeArray1[i] = itemArray2[index1];
                    index1++;
                }

            }
            if (genomeArray2[i] == -1) {
                while (index2 < itemArray1.length && job1.contains(itemArray1[index2])) {
                    index2++;
                }
                if (index2 < itemArray1.length) {
                    genomeArray2[i] = itemArray1[index2];
                    index2++;
                }
            }
        }
//        for (int i = 0; i < genomeArray1.length; i++) {
//            System.out.print(genomeArray1[i] + " ");
//        }
//        System.out.println(isValid(genomeArray1));
//        for (int i = 0; i < genomeArray2.length; i++) {
//            System.out.print(genomeArray2[i] + " ");
//        }
//        System.out.println(isValid(genomeArray2));
        //交叉完毕，将基因放回个体，再将个体放回种群，并更新他们的适应值和路径长度
        copy1.genomeMachineArray = machineArray1.clone();
        copy1.genomeItemArray = genomeArray1.clone();
        copy1.solutions.clear();
        copy1.updateFitnessAndSolution();
        newPopulation.add(copy1);
        copy2.genomeMachineArray = machineArray2.clone();
        copy2.genomeItemArray = genomeArray2.clone();
        copy2.solutions.clear();
        copy2.updateFitnessAndSolution();
        newPopulation.add(copy2);
//        newPopulation.get(k).setGenomeArray(genomeArray1);
//        newPopulation.get(k).updateFitnessAndSolution();
//        newPopulation.get(r).setGenomeArray(genomeArray2);
//        newPopulation.get(r).updateFitnessAndSolution();
    }

    /**
     * 判断染色体是否合法
     */
//    private boolean isValid(int[] genomeArray) {
//        //染色体为1-9的数字且不重复
//        Set<Integer> set = new HashSet<>();
//        for (int i = 0; i < genomeArray.length; i++) {
//            if (set.contains(genomeArray[i]) || genomeArray[i] < 0 || genomeArray[i] > 9) {
//                return false;
//            }
//            set.add(genomeArray[i]);
//        }
//        return true;
//    }

    /**
     * 变异操作
     * @param k 变异的基因idx
     */
    protected void varation(int k) {
        //小根堆
        PriorityQueue<BatchGenome> heap = new PriorityQueue<>(new Comparator<BatchGenome>() {
            @Override
            public int compare(BatchGenome o1, BatchGenome o2) {
                return compareDouble(o1.fitness, o2.fitness);
            }
        });
        BatchGenome genome = newPopulation.get(k);
        
        // 添加空值检查，防止NullPointerException
        if (genome.solutions == null || genome.solutions.isEmpty()) {
            return; // 如果solutions为空，直接返回
        }
        
        // 检查是否所有solutions都为空
        boolean allEmpty = true;
        for (int i = 0; i < genome.solutions.size(); i++) {
            if (genome.solutions.get(i) != null && 
                !genome.solutions.get(i).solutions.isEmpty()) {
                allEmpty = false;
                break;
            }
        }
        if (allEmpty) {
            return; // 如果所有solutions都为空，直接返回
        }
        
        int[] machineArray = genome.genomeMachineArray;
        int[] itemArray = genome.genomeItemArray;
        //找到最大和最小的机器号-1
        int maxIndex = -1;
        int minIndex = -1;
        double minEndTime = Double.MAX_VALUE;
        double maxEndTime = Double.MIN_VALUE;
        //找到最后一个批次利用率最小和第二小的机器
        int minRate = 0;
        int secRate = 1;
        double minUsage = Double.MAX_VALUE;
        double secUsage = Double.MAX_VALUE;
        // 首先检查genome.solutions是否为空
//        if (genome.solutions == null || genome.solutions.isEmpty()) {
//            // 处理空列表的情况，根据你的业务逻辑返回或抛出异常
//            throw new RuntimeException("genome.solutions为空");
//        }
//        // 确保minRate和secRate的初始值有效
//        if (genome.solutions.size() <= 1) {
//            // 处理解决方案不足的情况
//            return;
//        }
//
//        // 确保初始索引对应的解决方案不为空
//        if (genome.solutions.get(minRate).solutions.isEmpty() ||
//                genome.solutions.get(secRate).solutions.isEmpty()) {
//            // 处理初始解决方案为空的情况
//            return;
//        }
        //todo 找到最后一批次使用率最小的机器，這裏有bug
        for (int i = 0; i < genome.solutions.size(); i++) {
            BatchResult solution = genome.solutions.get(i);
            // 情况1：解决方案为空（利用率=0，最小）
            if (solution == null || solution.solutions.isEmpty()) {
                secUsage = minUsage;
                secRate = minRate;
                minUsage = 0;
                minRate = i;
                if (0 < minEndTime) {
                    minEndTime = 0;
                    minIndex = i;
                }
                continue;
            }

            // 情况2：解决方案非空，获取最后一批次使用率
            Solution lastBatch = solution.solutions.get(solution.solutions.size() - 1);
            double lastEndTime = solution.endTimes.get(solution.endTimes.size() - 1);
            double currentUsage = lastBatch.rate;

            // 更新最小和第二小的值
            if (currentUsage < minUsage) {
                secUsage = minUsage;
                secRate = minRate;
                minUsage = currentUsage;
                minRate = i;
            } else if (currentUsage < secUsage && currentUsage != minUsage) {
                secUsage = currentUsage;
                secRate = i;
            }
            // 更新最小endTime
            if (lastEndTime < minEndTime) {
                minEndTime = lastEndTime;
                minIndex = i;
            }

            // 更新最大endTime
            if (lastEndTime > maxEndTime) {
                maxEndTime = lastEndTime;
                maxIndex = i;
            }
        }

//        if (genome.solutions.get(i).solutions.isEmpty() || genome.solutions.get(i).endTimes.isEmpty()) {
//                secRate = minRate;
//                minRate = i;
//                minIndex = i;
//                //todo 不能簡單的break；
//                continue;
//            }
//            if (!genome.solutions.get(minRate).solutions.isEmpty() && genome.solutions.get(i).solutions.get(genome.solutions.get(i).solutions.size() - 1).rate < genome.solutions.get(minRate).solutions.get(genome.solutions.get(minRate).solutions.size() - 1).rate) {
//                secRate = minRate;
//                minRate = i;
//            } else if ((!genome.solutions.get(secRate).solutions.isEmpty() && genome.solutions.get(i).solutions.get(genome.solutions.get(i).solutions.size() - 1).rate < genome.solutions.get(secRate).solutions.get(genome.solutions.get(secRate).solutions.size() - 1).rate) &&
//                    (genome.solutions.get(minRate).solutions.isEmpty() || genome.solutions.get(i).solutions.get(genome.solutions.get(i).solutions.size() - 1).rate != genome.solutions.get(minRate).solutions.get(genome.solutions.get(minRate).solutions.size() - 1).rate)) {
//                secRate = i;
//            }
//            if (genome.solutions.get(maxIndex).endTimes.isEmpty() || genome.solutions.get(i).endTimes.get(genome.solutions.get(i).endTimes.size() - 1) > genome.solutions.get(maxIndex).endTimes.get(genome.solutions.get(maxIndex).endTimes.size() - 1)) {
//                maxIndex = i;
//            }
//            if (!genome.solutions.get(minIndex).endTimes.isEmpty() && genome.solutions.get(i).endTimes.get(genome.solutions.get(i).endTimes.size() - 1) < genome.solutions.get(minIndex).endTimes.get(genome.solutions.get(minIndex).endTimes.size() - 1)) {
//                minIndex = i;
//            }
//        }
        //零件号-序列顺序映射
        int[] itemIndex = new int[itemArray.length];
        for (int i = 0; i < itemArray.length; i++) {
            itemIndex[itemArray[i]] = i;
        }
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
//        for (int i = 0; i < machineArray.length; i++) {
//            machineMap.computeIfAbsent(machineArray[i], k1 -> new ArrayList<>()).add(itemArray[i]);
//        }
        for (int i = 0; i < machineArray.length; i++) {
            if (machineMap.containsKey(machineArray[i])) {
                machineMap.get(machineArray[i]).add(i);
            } else {
                List<Integer> list = new ArrayList<>();
                list.add(i);
                machineMap.put(machineArray[i], list);
            }
        }
        // 检查maxIndex和minIndex是否有效
        if (maxIndex == -1 || minIndex == -1) {
            return; // 如果无法确定maxIndex或minIndex，直接返回
        }
        
        // 检查maxIndex是否在有效范围内
        if (maxIndex >= machines.length || minIndex >= machines.length) {
            return; // 索引超出范围
        }
        
        // 检查machineMap中是否有对应的键
        if (!machineMap.containsKey(maxIndex)) {
            // maxIndex机器没有分配零件，尝试找一个有零件的机器
            for (Integer machineId : machineMap.keySet()) {
                if (!machineMap.get(machineId).isEmpty()) {
                    maxIndex = machineId;
                    break;
                }
            }
        }
        
        if (!machineMap.containsKey(minIndex)) {
            // minIndex机器没有分配零件，尝试找一个有零件的机器
            for (Integer machineId : machineMap.keySet()) {
                if (!machineMap.get(machineId).isEmpty()) {
                    minIndex = machineId;
                    break;
                }
            }
        }
        
        // 再次检查是否找到了有效的机器
        if (!machineMap.containsKey(maxIndex) || !machineMap.containsKey(minIndex)) {
            return; // 如果machineMap中没有对应的机器，直接返回
        }
        
        // 检查machineMap中的列表是否为空
        if (machineMap.get(maxIndex).isEmpty() || machineMap.get(minIndex).isEmpty()) {
            return; // 如果对应机器没有分配零件，直接返回
        }
        
        for (int i = 0; i < 2 * variationExchangeCount; i++) {
            BatchGenome genomeI = copyGenome(newPopulation.get(k));
            int[] machineArrayI = genome.genomeMachineArray;
            int[] itemArrayI = genome.genomeItemArray;
            double r = random.nextDouble();
            //百分之70的概率将加工时间最长的机器上的一个零件分给加工时间最短的机器，百分之三十的概率将加工时间最长的机器上的一个零件分给随机一个满足加工要求的机器
            if (r < 0.5) {
                while (true) {
                    int r1 = random.nextInt(machineMap.get(maxIndex).size());
                    if (items[machineMap.get(maxIndex).get(r1)].l > machines[minIndex].L || items[machineMap.get(maxIndex).get(r1)].w > machines[minIndex].W || items[machineMap.get(maxIndex).get(r1)].h > machines[minIndex].H) {
                        continue;
                    }
                    machineArrayI[itemIndex[machineMap.get(maxIndex).get(r1)]] = minIndex;
                    break;
                }
            } else if (r < 0.9) {
                //将加工时间最长的机器上的最后一个批次里最高的零件分配给加工时间最短的机器
                // 检查genomeI.solutions是否有效
                if (genomeI.solutions == null || genomeI.solutions.size() <= maxIndex || 
                    genomeI.solutions.get(maxIndex) == null || 
                    genomeI.solutions.get(maxIndex).solutions == null ||
                    genomeI.solutions.get(maxIndex).solutions.isEmpty()) {
                    continue; // 跳过这次迭代
                }
                
                List<PlaceItem> placeItemList = genomeI.solutions.get(maxIndex).solutions.get(genomeI.solutions.get(maxIndex).solutions.size() - 1).placeItemList;
                if (placeItemList == null || placeItemList.isEmpty()) {
                    continue; // 跳过这次迭代
                }
                
                PlaceItem maxIndex1 = placeItemList.get(0);
                for (int j = 0; j < placeItemList.size(); j++) {
                    if (placeItemList.get(j).h > maxIndex1.h) {
                        maxIndex1 = placeItemList.get(j);
                    }
                }
                int maxPrintNumbber = Integer.valueOf(maxIndex1.name) - 1;
                int maxIndex2 = itemIndex[maxPrintNumbber];
                while (true) {
                    if (items[maxPrintNumbber].l > machines[minIndex].L || items[maxPrintNumbber].w > machines[minIndex].W || items[maxPrintNumbber].h > machines[minIndex].H) {
                        continue;
                    }
                        machineArrayI[maxIndex2] = minIndex;
                        break;
                    }
                //machineArrayI[machineMap.get(maxIndex).get(machineMap.get(maxIndex).size() - 1)] = minIndex;
            } else {
                    // 检查minRate是否有效
                    if (!machineMap.containsKey(minRate) || machineMap.get(minRate).isEmpty()) {
                        continue; // 跳过这次迭代
                    }
                    int r1 = random.nextInt(machineMap.get(maxIndex).size());
                    if (items[machineMap.get(maxIndex).get(r1)].l > machines[minRate].L || items[machineMap.get(maxIndex).get(r1)].w > machines[minRate].W || items[machineMap.get(maxIndex).get(r1)].h > machines[minRate].H) {
                        while (true) {
                            int machineIndex = random.nextInt(machineNum);
                            if (items[machineMap.get(maxIndex).get(r1)].l > machines[machineIndex].L || items[machineMap.get(maxIndex).get(r1)].w > machines[machineIndex].W || items[machineMap.get(maxIndex).get(r1)].h > machines[machineIndex].H) {
                                continue;
                            }
                            machineArrayI[itemIndex[machineMap.get(maxIndex).get(r1)]] = machineIndex;
                            break;
                        }
                    } else {
                        if (maxIndex == minIndex) {
                            machineArrayI[itemIndex[machineMap.get(maxIndex).get(r1)]] = secRate;
                        } else {
                            machineArrayI[itemIndex[machineMap.get(maxIndex).get(r1)]] = minRate;
                        }
                    }
                }

//            int variationType = random.nextInt(3);
//            switch (variationType) {
//                case 0:
//                    //将加工时间最长的机器上的一个零件分给加工时间最短的机器
//                    while (true) {
//                        int r1 = random.nextInt(machineMap.get(maxIndex).size());
//                        if (items[itemArrayI[machineMap.get(maxIndex).get(r1)]].l > machines[minIndex].L || items[itemArrayI[machineMap.get(maxIndex).get(r1)]].w > machines[minIndex].W || items[itemArrayI[machineMap.get(maxIndex).get(r1)]].h > machines[minIndex].H) {
//                            continue;
//                        }
//                        machineArrayI[machineMap.get(maxIndex).get(r1)] = minIndex;
//                        break;
//                    }
//                    break;
//                case 1:
//                    //随机交换两个位置上的零件
//                    int r2 = random.nextInt(itemArray.length);
//                    //找到第一个不等于r2的位置
//                    int r3 = random.nextInt(itemArray.length);
//                    while (r3 == r2) {
//                        r3 = random.nextInt(itemArray.length);
//                    }
//                    int temp = itemArrayI[r2];
//                    int temp1 = machineArrayI[r2];
//                    itemArrayI[r2] = itemArrayI[r3];
//                    machineArrayI[r2] = machineArrayI[r3];
//                    itemArrayI[r3] = temp;
//                    machineArrayI[r3] = temp1;
//                    break;
//                case 2:
//                    //随机将一个机器上的零件加入到另一个机器上
//                    int r4 = random.nextInt(machineArray.length);
//                    int machineIndex = machineArrayI[r4];
//                    int r5 = random.nextInt(machines.length);
//                    int count = 0;
//                    while (count < 100 &&  r4 == r5 || items[itemArrayI[r4]].l > machines[r5].L || items[itemArrayI[r4]].w > machines[r5].W || items[itemArrayI[r4]].h > machines[r5].H) {
//                        r5 = random.nextInt(machines.length);
//                        count++;
//                    }
//                    machineArrayI[r4] = machineArrayI[r5];
//                    break;
//            }
            //将加工时间最长的机器上的一个零件分给加工时间最短的机器
//            while (true) {
//                int r1 = random.nextInt(machineMap.get(maxIndex).size());
//                if (items[itemArrayI[machineMap.get(maxIndex).get(r1)]].l > machines[minIndex].L || items[itemArrayI[machineMap.get(maxIndex).get(r1)]].w > machines[minIndex].W || items[itemArrayI[machineMap.get(maxIndex).get(r1)]].h > machines[minIndex].H) {
//                    continue;
//                }
//                machineArrayI[machineMap.get(maxIndex).get(r1)] = minIndex;
//                break;
//            }
//            //随机交换两个位置上的零件
//            int r2 = random.nextInt(itemArray.length);
//            //找到第一个不等于r2的位置
//            int r3 = random.nextInt(itemArray.length);
//            while (r3 == r2) {
//                r3 = random.nextInt(itemArray.length);
//            }
//            int temp = itemArrayI[r2];
//            int temp1 = machineArrayI[r2];
//            itemArrayI[r2] = itemArrayI[r3];
//            machineArrayI[r2] = machineArrayI[r3];
//            itemArrayI[r3] = temp;
//            machineArrayI[r3] = temp1;
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
//        System.out.println(isValid(itemArray));
            genomeI.genomeMachineArray = machineArrayI.clone();
            genomeI.genomeItemArray = itemArrayI.clone();
            genomeI.solutions.clear();
//        for (int i = 0; i < genome.genomeItemArray.length; i++) {
//            System.out.print(genome.genomeItemArray[i] + " ");
//        }
//        System.out.println(isValid(genome.genomeItemArray));
//        for (int i = 0; i < genome.genomeMachineArray.length; i++) {
//            System.out.print(genome.genomeMachineArray[i] + " ");
//        }
//        System.out.println();
            //更新基因的适应值和路径长短
            genomeI.updateFitnessAndSolution();
            heap.add(genomeI);
        }
        BatchGenome bestVaritiongenome = heap.poll();
        //将变异后的个体放回种群
        newPopulation.set(k, bestVaritiongenome);
    }



    /**
     * 复制染色体
     */
    public BatchGenome copyGenome(BatchGenome genome) {
        BatchGenome copy = new BatchGenome(genome.items, genome.machines, genome.isRotateEnable, genome.genomeMachineArray.clone(), genome.genomeItemArray, genome.method);
        copy.solutions = new ArrayList<>(genome.solutions);
        copy.fitness = genome.fitness;
        copy.cMax = genome.cMax;
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
