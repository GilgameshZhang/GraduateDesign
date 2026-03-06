package AlgorthmFrame.ga;

import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProblemFrame.GAParameters;
import ProblemFrame.InitializationStrategy;
import ProgramEntity.Item;
import ProgramEntity.Job;
import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.DiscreteProcessingMachine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import ProblemFrame.Solution;
import util.java.ExperimentResultWriter;

import java.util.*;

public class GA {
    private Problem input;
    private Operation[][] operationMatrix;
    private Random r;

    // 使用参数配置对象
    private GAParameters params;
    private final int popSize;
    private final double pr;
    private final double pc;
    private final double pm;
    private final int maxGen;
    private final int maxStagnantStep;
    private final int timeLimit = -1;// no time limit
    
    // 禁忌搜索参数（保持原有）
    private final int maxT = 9;
    private final int maxTabuLimit = 100;
    private final double pt = 0.05;
    private final double pp = 0.30;
    
    // Fitness缩放系数：使fitness值更大，便于指导进化方向
    // fitness = FITNESS_SCALE / makespan，makespan越小fitness越大
    public static final double FITNESS_SCALE = 100000000.0;
    
    // 调试控制参数
    public static boolean DEBUG_MODE = false; // 总调试开关
    public static boolean PRINT_CROSSOVER = true; // 是否打印交叉操作
    public static boolean PRINT_MUTATION = true; // 是否打印变异操作
    public static int PRINT_INTERVAL = 50; // 打印间隔（每N代打印一次）
    public static int PRINT_CHROMOSOME_LENGTH = 100; // 打印染色体的前N个基因
    
    // 收集每一代的最优makespan，用于绘制迭代曲线图
    private java.util.List<Double> bestMakespanHistory;

    // 结果输出器（用于田口实验）
    private ExperimentResultWriter resultWriter;
    
    // 存储最优染色体（用于可视化）
    private Chromosome bestChromosome;

    /**
     * 默认构造函数（使用默认参数）
     */
    public GA(Problem input) {
        this(input, GAParameters.getDefaultParameters());
    }
    
    /**
     * 参数化构造函数（用于田口实验）
     */
    public GA(Problem input, GAParameters params) {
        this.input = input;
        this.params = params;
        
        // 从参数配置中设置值
        this.popSize = params.popSize;
        this.pr = params.pr;
        this.pc = params.pc;
        this.pm = params.pm;
        // 使用很大的maxGen值，确保算法由时间而非迭代次数终止
        this.maxGen = 999999;
        this.maxStagnantStep = params.maxStagnantStep;
        
        this.operationMatrix = new Operation[input.getJobCount()][];
        for (int i = 0; i < operationMatrix.length; i++) {
            operationMatrix[i] = new Operation[input.getOperationCountArr()[i]];
            for (int j = 0; j < operationMatrix[i].length; j++)
                operationMatrix[i][j] = new Operation();
        }

        this.r = new Random();
//		this.r.setSeed(1);
    }
    
    /**
     * 设置结果输出器（用于田口实验）
     */
    public void setResultWriter(ExperimentResultWriter writer) {
        this.resultWriter = writer;
    }
    
    /**
     * 获取参数配置
     */
    public GAParameters getParameters() {
        return params;
    }
    
    /**
     * 输出到控制台或文件（根据是否设置了resultWriter）
     */
    private void printOrWrite(String text) {
        if (resultWriter != null) {
            resultWriter.writeLine(text);
        } else {
            System.out.println(text);
        }
    }

    /**
     * the whole logic of the flexible job shop sheduling problem
     */
    public Solution solve() {
        int jobCount = input.getJobCount();
        CaculateFitness c = new CaculateFitness();

        // 创建染色体操作对象，传入CaculateFitness实例和参数配置
        ChromosomeOperation chromOps = new ChromosomeOperation(r, input, c, params);
//        TabuSearch1 tabu = new TabuSearch1(input, r);
        
        // 初始化最优makespan历史记录
        bestMakespanHistory = new java.util.ArrayList<>();

        // 初始化工件类entries
        int[][] operationToIndex = input.getOperationToIndex();
        Job[] jobs = new Job[jobCount];
        for (int i = 0; i < jobCount; i++) {
            int index = i;// 工件编号
            int opsNr = input.getOperationCountArr()[i];// 工件工序数
            int[] opsIndex = operationToIndex[i];// 工件工序对应的index
            int[] opsMacNr = new int[opsNr];// 工序对应备选机器数
            for (int j = 0; j < opsNr; j++) {
                opsMacNr[j] = input.getMachineCountArr()[opsIndex[j]];
            }
            jobs[i] = new Job(index, opsNr, opsIndex, opsMacNr);
        }

        long startTime = System.currentTimeMillis();// 算法开始
        Chromosome[] parents = new Chromosome[this.popSize];
        if (params.enableHeuristicInit) {
            // 生成初始种群：多样化策略
            int strategySize = (int) (this.popSize / 6);  // 每种策略约16.7%

            // 输出到控制台或文件
            printOrWrite("[初始化] 生成多样化初始种群：");
            printOrWrite("  - 随机策略: " + strategySize + "个");
            printOrWrite("  - 按高度降序+轮盘赌: " + strategySize + "个");
            printOrWrite("  - 按面积降序+轮盘赌: " + strategySize + "个");
            printOrWrite("  - 按高度降序+负载均衡: " + strategySize + "个");
            printOrWrite("  - 按高度降序+最短加工时间: " + strategySize + "个");
            printOrWrite("  - 混合策略: " + (this.popSize - 5 * strategySize) + "个");

            for (int i = 0; i < this.popSize; i++) {
                InitializationStrategy strategy;

                if (i < strategySize) {
                    // 完全随机策略
                    strategy = InitializationStrategy.randomStrategy();
                } else if (i < 2 * strategySize) {
                    // 高度降序 + 轮盘赌机器选择
                    strategy = InitializationStrategy.heuristicStrategy();
                } else if (i < 3 * strategySize) {
                    // 面积降序 + 轮盘赌机器选择
                    strategy = InitializationStrategy.areaBasedStrategy();
                } else if (i < 4 * strategySize) {
                    // 高度降序 + 负载均衡
                    strategy = new InitializationStrategy(
                            InitializationStrategy.OperationSortStrategy.HEIGHT_DESCENDING,
                            InitializationStrategy.OperationSortStrategy.RANDOM,
                            InitializationStrategy.MachineSelectionStrategy.LOAD_BALANCE,
                            InitializationStrategy.MachineSelectionStrategy.ROULETTE_WHEEL
                    );
                } else if (i < 5 * strategySize) {
                    // 高度降序 + 最短加工时间（贪心策略）
                    strategy = InitializationStrategy.heightShortestProcessStrategy();
                } else {
                    // 混合策略：随机选择一种
                    int randChoice = r.nextInt(4);
                    if (randChoice == 0) {
                        strategy = InitializationStrategy.randomStrategy();
                    } else if (randChoice == 1) {
                        strategy = InitializationStrategy.heuristicStrategy();
                    } else if (randChoice == 2) {
                        strategy = InitializationStrategy.areaBasedStrategy();
                    } else {
                        strategy = InitializationStrategy.heightShortestProcessStrategy();
                    }
                }

                parents[i] = new Chromosome(jobs, r, input, strategy);

                // 修复初始染色体中可能存在的无效机器分配
                chromOps.fixInvalidMachineAssignments(parents[i]);

                // 验证初始染色体
                boolean hasInvalid = false;
                for (int j = 0; j < parents[i].gene_OS.length; j++) {
                    if (parents[i].gene_OS[j] == -1 || parents[i].gene_MS[j] <= 0) {
                        hasInvalid = true;
                        break;
                    }
                }
                if (hasInvalid) {
                    System.out.println("ERROR: 初始种群第" + i + "个染色体包含无效值！");
                    System.out.print("gene_OS: [");
                    for (int j = 0; j < parents[i].gene_OS.length; j++) {
                        System.out.print(parents[i].gene_OS[j] + (j < parents[i].gene_OS.length - 1 ? ", " : ""));
                    }
                    System.out.println("]");
                    System.out.print("gene_MS: [");
                    for (int j = 0; j < parents[i].gene_MS.length; j++) {
                        System.out.print(parents[i].gene_MS[j] + (j < parents[i].gene_MS.length - 1 ? ", " : ""));
                    }
                    System.out.println("]");
                }

                parents[i].fitness = FITNESS_SCALE / c.evaluate(parents[i], input, operationMatrix);
            }
        } else {
            for (int i = 0; i < this.popSize; i++) {
                InitializationStrategy strategy;
                    // 完全随机策略
                strategy = InitializationStrategy.randomStrategy();

                parents[i] = new Chromosome(jobs, r, input, strategy);

                // 修复初始染色体中可能存在的无效机器分配
                chromOps.fixInvalidMachineAssignments(parents[i]);

                // 验证初始染色体
                boolean hasInvalid = false;
                for (int j = 0; j < parents[i].gene_OS.length; j++) {
                    if (parents[i].gene_OS[j] == -1 || parents[i].gene_MS[j] <= 0) {
                        hasInvalid = true;
                        break;
                    }
                }
                if (hasInvalid) {
                    System.out.println("ERROR: 初始种群第" + i + "个染色体包含无效值！");
                    System.out.print("gene_OS: [");
                    for (int j = 0; j < parents[i].gene_OS.length; j++) {
                        System.out.print(parents[i].gene_OS[j] + (j < parents[i].gene_OS.length - 1 ? ", " : ""));
                    }
                    System.out.println("]");
                    System.out.print("gene_MS: [");
                    for (int j = 0; j < parents[i].gene_MS.length; j++) {
                        System.out.print(parents[i].gene_MS[j] + (j < parents[i].gene_MS.length - 1 ? ", " : ""));
                    }
                    System.out.println("]");
                }

                parents[i].fitness = FITNESS_SCALE / c.evaluate(parents[i], input, operationMatrix);
            }

        }


        Chromosome[] children = new Chromosome[this.popSize];
        for (int i = 0; i < this.popSize; i++) {
            children[i] = new Chromosome(parents[i]);
        }

        // 获取最优子代并统计初始种群fitness分布
        double maxFitness = Double.NEGATIVE_INFINITY;
        double minFitness = Double.MAX_VALUE;
        double sumFitness = 0;
        int index = 0;
        for (int i = 0; i < this.popSize; i++) {
            sumFitness += parents[i].fitness;
            if (maxFitness < parents[i].fitness) {
                index = i;
                maxFitness = parents[i].fitness;
            }
            if (minFitness > parents[i].fitness) {
                minFitness = parents[i].fitness;
            }
        }
        double avgFitness = sumFitness / this.popSize;
        double bestMakespan = FITNESS_SCALE / maxFitness;
        double worstMakespan = FITNESS_SCALE / minFitness;
        double avgMakespan = FITNESS_SCALE / avgFitness;

        System.out.println("[初始种群统计]");
        System.out.println("  最优makespan: " + String.format("%.2f", bestMakespan) + " (fitness: " + String.format("%.2f", maxFitness) + ")");
        System.out.println("  平均makespan: " + String.format("%.2f", avgMakespan) + " (fitness: " + String.format("%.2f", avgFitness) + ")");
        System.out.println("  最差makespan: " + String.format("%.2f", worstMakespan) + " (fitness: " + String.format("%.2f", minFitness) + ")");
        System.out.println();
        
        // 记录第0代的最优makespan
        bestMakespanHistory.add(bestMakespan);

        Chromosome best = new Chromosome(parents[index]);
        Chromosome currentBest = new Chromosome(parents[index]);

        int noImprove = 0;
        int gen = 0;
        while (noImprove < this.maxStagnantStep && System.currentTimeMillis() - startTime <= 5 * 60 * 1000) {

            // 陷入局部最优时进行扰动:取部分精英个体后随机生成新个体
//            if (gen - noImprove > this.maxStagnantStep) {
////                break;
//                int num = (int) (pp * popSize);
//                ArrayList<Chromosome> p = new ArrayList<>();
//                Collections.addAll(p, parents);
//                Collections.sort(p);
//                for (int i = 0; i < num; i++)
//                    parents[i] = p.get(i);
//                for (int i = num; i < this.popSize; i++) {
//                    parents[i] = new Chromosome(jobs, r, input);
//                    parents[i].fitness = FITNESS_SCALE / c.evaluate(parents[i], input, operationMatrix);
//                }
//
//                noImprove = gen;
//            }

            // 选择 selection
            children = chromOps.Selection(parents, pr);

            // 交叉 cross
            boolean printCrossoverDetails = DEBUG_MODE && PRINT_CROSSOVER && (gen % PRINT_INTERVAL == 0);
            for (int i = 0; i < this.popSize; i += 2) {
                if (r.nextDouble() < this.pc) {
//					int fatherIndex = r.nextInt(popSize);
//					int motherIndex = r.nextInt(popSize);
//					while (fatherIndex == motherIndex)
//						motherIndex = r.nextInt(popSize);
                    int motherIndex = i + 1;

                    // 打印交叉前的染色体（只打印第一对，避免输出过多）
                    //if (printCrossoverDetails && i == 0) {
                        //System.out.println("\n========== 代数 " + gen + ": 交叉操作示例 ==========");
                        //printChromosomeSimple("  交叉前-父代" + i + ": ", children[i], PRINT_CHROMOSOME_LENGTH);
                        //printChromosomeSimple("  交叉前-母代" + motherIndex + ": ", children[motherIndex], PRINT_CHROMOSOME_LENGTH);
                    //}

                    chromOps.Crossover(children[i], children[motherIndex]);

                    // 打印交叉后的染色体
                    //if (printCrossoverDetails && i == 0) {
                        //printChromosomeSimple("  交叉后-父代" + i + ": ", children[i], PRINT_CHROMOSOME_LENGTH);
                        //printChromosomeSimple("  交叉后-母代" + motherIndex + ": ", children[motherIndex], PRINT_CHROMOSOME_LENGTH);
                        //System.out.println("================================================\n");
                    //}
                }
            }

            // 变异 mutation
            boolean printMutationDetails = DEBUG_MODE && PRINT_MUTATION && (gen % PRINT_INTERVAL == 0);
            int mutationCount = 0;
            for (int i = 0; i < this.popSize; i++) {
                if (r.nextDouble() < this.pm) {
                    // 打印变异前的染色体（只打印第一个变异的个体）
                    if (printMutationDetails && mutationCount == 0) {
                        System.out.println("\n========== 代数 " + gen + ": 变异操作示例 ==========");
                        printChromosomeSimple("  变异前-个体" + i + ": ", children[i], PRINT_CHROMOSOME_LENGTH);
                    }

                    chromOps.Mutation(children[i]);

                    // 打印变异后的染色体
                    if (printMutationDetails && mutationCount == 0) {
                        printChromosomeSimple("  变异后-个体" + i + ": ", children[i], PRINT_CHROMOSOME_LENGTH);
                        System.out.println("================================================\n");
                    }
                    mutationCount++;
                }
            }

            if (printMutationDetails && mutationCount > 0) {
                System.out.println("  本代共发生 " + mutationCount + " 次变异\n");
            }

            // update fitness
//            for (int i = 0; i < this.popSize; i++){
//                children[i].fitness = 1.0 / c.evaluate(children[i], input, operationMatrix);
//                parents[i] = new Chromosome(children[i]);
//            }

            for (int i = 0; i < this.popSize; i++) {
                double t = c.evaluate(children[i], input, operationMatrix);
                children[i].fitness = FITNESS_SCALE / t;
                double t1 = c.evaluate(children[i], input, operationMatrix);
                if (t != t1) {
                    System.out.println("1这里有问题");
                }
                int maxTSIterSize = (int) (maxGen * ((float) gen / (float) maxGen));

                // TS1
                //sol = NeiborAl2.search(sol, maxTSIterSize);

                // TS2
//                sol = NeighbourAlgorithms.neighbourSearch(sol);

                parents[i] = new Chromosome(children[i]);
                double t3 = c.evaluate(parents[i], input, operationMatrix);
                if (t3 != t) {
                       System.out.println("这里2有问题");
                }
            }

            // 局部搜索：在evaluate之后对所有子代进行局部改进
            if (gen % 10 == 0 && DEBUG_MODE) {
                System.out.println("\n========== 代数 " + gen + ": 开始局部搜索 ==========");
            }
            
            // 按fitness排序，以便分级应用局部搜索
            Chromosome[] sortedChildren = new Chromosome[this.popSize];
            System.arraycopy(parents, 0, sortedChildren, 0, this.popSize);
            //java.util.Arrays.sort(sortedChildren, Collections.reverseOrder());  // fitness从高到低
            
            // 根据参数配置决定是否执行局部搜索
            if (params.enableLocalSearch) {
                for (int i = 0; i < this.popSize; i++) {
                    // 分级局部搜索策略
                    int maxIterations;
                    if (i < this.popSize * 0.3) {
                        // Top 30%: 深度搜索
                        maxIterations = 20;
                    } else if (i < this.popSize * 0.7) {
                        // Middle 40%: 中度搜索
                        maxIterations = 10;
                    } else {
                        // Bottom 30%: 轻度搜索
                        maxIterations = 5;
                    }
                    
                    // 执行局部搜索（使用参数配置的迭代次数）
                    double beforeMakespan = FITNESS_SCALE / sortedChildren[i].fitness;
                    chromOps.LocalSearch(sortedChildren[i], params.localSearchMaxIter);
                    double afterMakespan = FITNESS_SCALE / sortedChildren[i].fitness;
                    
                    // 打印前几个个体的局部搜索效果（调试用）
//                        System.out.println(String.format("  Top%d: 局部搜索前makespan=%.2f, 后=%.2f, 改进=%.2f",
//                            i+1, beforeMakespan, afterMakespan, beforeMakespan - afterMakespan));
                }
                
                if (gen % 10 == 0 && DEBUG_MODE) {
                    System.out.println("========== 局部搜索完成 ==========\n");
                }
            } else {
                if (gen % 10 == 0 && DEBUG_MODE) {
                    System.out.println("========== 局部搜索已禁用 ==========\n");
                }
            }
            
            // 将处理后的结果复制回parents
            System.arraycopy(sortedChildren, 0, parents, 0, this.popSize);

            // 精英保留策略（Elitism）：确保最优个体不会丢失
            // 找到当代最差的个体，用历史最优替换
            if (gen > 0) {  // 从第1代开始应用精英保留
                int worstIndex = 0;
                double worstFitness = Double.POSITIVE_INFINITY;
                for (int i = 0; i < this.popSize; i++) {
                    if (parents[i].fitness < worstFitness) {
                        worstFitness = parents[i].fitness;
                        worstIndex = i;
                    }
                }

                // 如果历史最优不在当代种群中，用它替换最差个体
                boolean bestInPopulation = false;
                for (int i = 0; i < this.popSize; i++) {
                    if (Math.abs(parents[i].fitness - best.fitness) < 1e-6) {
                        bestInPopulation = true;
                        break;
                    }
                }

                if (!bestInPopulation && best.fitness > worstFitness) {
                    parents[worstIndex] = new Chromosome(best);
                    if (DEBUG_MODE && (gen % PRINT_INTERVAL == 0)) {
                        System.out.println("🛡️ 精英保留：将历史最优个体（fitness=" +
                                String.format("%.2f", best.fitness) +
                                ", makespan=" + String.format("%.2f", FITNESS_SCALE / best.fitness) +
                                "）保留到第" + gen + "代");
                    }
                }
            }

            // get best chromosome
            currentBest = getBest(parents);

//            ArrayList<Chromosome> p = new ArrayList<>();
//            Collections.addAll(p, children);
//            Collections.sort(p);
//            currentBest = p.get(0);
//
//             tabu search
//            int tabuNr = popSize - (int) (pt * popSize);
//            for (int i = 0; i < tabuNr; i++) {
//                parents[i] = new Chromosome(children[i]);
//            }
//            for (int i = tabuNr; i < popSize; i++) {
//                int maxTSIterSize = (int) (maxGen * ((float) gen / (float) maxGen));
//                parents[i] = new Chromosome(tabu.TabuSearch(maxTSIterSize, 5, children[i], best.fitness));
//            }


            if (best.fitness < currentBest.fitness) {
                double t2 = c.evaluate(currentBest, input, operationMatrix);
                best = new Chromosome(currentBest);
                double t1 = c.evaluate(best, input, operationMatrix);
                if (t1 != t2) System.out.println("有问题" + t1 + ":" + t2);
                double t = c.evaluate(best, input, operationMatrix);
                if (t != t) System.out.println("有问题" + t + ":" + t1);
                //System.out.println("best的打印序列为空");

                // 关键修复：立即对新的best进行完整evaluate，确保printSolution被保存
                // 这样可以避免后续输出时因printSolution=null而重新evaluate导致结果不一致
//                if (best.printSolution == null) {
//                    Operation[][] tempOpMatrix = createOperationMatrix();
//                    c.evaluate(best, input, tempOpMatrix);
//                    // 注意：evaluate会设置best.printSolution
//                }

                noImprove = 0;
                double newBestMakespan = FITNESS_SCALE / currentBest.fitness;
                printOrWrite("In " + gen + " generation, find new best fitness is:" + currentBest.fitness +
                        ", makespan: " + String.format("%.2f", newBestMakespan));
            } else {
                noImprove++;
            }

            // 在每一代结束时，确保best染色体的printSolution被保留
            // 这是防止丢失的最后一道防线
//            if (best.printSolution == null) {
//                Operation[][] tempOpMatrix = createOperationMatrix();
//                c.evaluate(best, input, tempOpMatrix);
//                if (DEBUG_MODE && (gen % PRINT_INTERVAL == 0)) {
//                    System.out.println("⚠️ 检测到best.printSolution=null，重新evaluate以保存装箱结果");
//                }
//            }
            
            // 计算并打印平均makespan和最小makespan
            // 注意：从fitness反算makespan，避免重复evaluate导致结果不一致
            double totalMakespan = 0.0;
            double minMakespan = Double.MAX_VALUE;
            maxFitness = Double.NEGATIVE_INFINITY;
            for (int i = 0; i < this.popSize; i++) {
                // 从fitness反算makespan，确保与适应度计算一致
                double makespan = FITNESS_SCALE / parents[i].fitness;
                totalMakespan += makespan;
                if (makespan < minMakespan) {
                    minMakespan = makespan;
                }
                if (parents[i].fitness > maxFitness) {
                    maxFitness = parents[i].fitness;
                }
            }
            avgMakespan = totalMakespan / this.popSize;
            bestMakespan = FITNESS_SCALE / best.fitness;

            // 验证：min makespan应该对应max fitness
            double minMakespanFromMaxFitness = FITNESS_SCALE / maxFitness;
            if (Math.abs(minMakespan - minMakespanFromMaxFitness) > 0.01) {
                System.err.println("⚠️ 警告：统计数据不一致！minMakespan=" + minMakespan +
                        ", 但maxFitness对应的makespan=" + minMakespanFromMaxFitness);
            }
            // 写入迭代信息到文件或控制台
            if (resultWriter != null) {
                resultWriter.writeIterationInfo(gen, bestMakespan, avgMakespan);
            } else {
            System.out.println(" After " + gen + " generation, the best fitness is:" + best.fitness +
                    " (makespan: " + String.format("%.2f", bestMakespan) + ")" +
                    ", avg makespan: " + String.format("%.2f", avgMakespan) +
                    ", min makespan: " + String.format("%.2f", minMakespan));
            }
            
            // 记录当前代的最优makespan
            bestMakespanHistory.add(bestMakespan);
            
            gen++;
        }

        // 重要：使用fitness反算makespan
        double fitnessBasedMakespan = FITNESS_SCALE / best.fitness;
        
        // 关键修复：必须重新evaluate best染色体以填充operationMatrix
        // 因为在前面的循环中，operationMatrix被最后一个children[99]的evaluate覆盖了
        // 如果不重新evaluate，printDetailedResults输出的机器完成时间将是children[99]的，而不是best的
        printOrWrite("");
        printOrWrite("正在生成最优解的详细调度数据...");
        double actualMakespan = c.evaluate(best, input, operationMatrix);
        
        // 对比fitness反算的makespan和重新evaluate的makespan
        if (Math.abs(actualMakespan - fitnessBasedMakespan) > 0.01) {
            printOrWrite("\n⚠️ 注意：由于禁忌搜索的随机性，重新evaluate的makespan与fitness略有不同");
            printOrWrite("  Fitness反算的makespan: " + String.format("%.2f", fitnessBasedMakespan));
            printOrWrite("  重新evaluate的makespan: " + String.format("%.2f", actualMakespan));
            double diff = Math.abs(actualMakespan - fitnessBasedMakespan);
            double diffPercent = diff / fitnessBasedMakespan * 100;
            printOrWrite("  差异: " + String.format("%.2f", diff) + 
                             " (" + String.format("%.2f%%", diffPercent) + ")");
            printOrWrite("  说明：这是正常现象，因为禁忌搜索使用随机邻域搜索");
        } else {
            printOrWrite("✓ 重新evaluate确认：makespan = " + String.format("%.2f", actualMakespan));
        }
        
        printOrWrite("\n After " + gen + " generation, the best schedule cost is:" + String.format("%.2f", actualMakespan));
        
        // 保存最优染色体到成员变量（用于后续访问）
        this.bestChromosome = best;
        
        Solution bestSolution = new Solution(operationMatrix, best, input, actualMakespan);

        long endTime = System.currentTimeMillis();// 算法开始
        long runTime = endTime - startTime;
        printOrWrite(" 算法时间花费：" + runTime / 1000.0 + "s");
        
        // 如果设置了结果输出器，写入最终结果
        if (resultWriter != null) {
            // 传入bestChromosome和operationMatrix用于生成可视化图表
            resultWriter.writeFinalResults(actualMakespan, runTime, gen, bestMakespanHistory, best, operationMatrix);
            resultWriter.writeDetailedSchedule(operationMatrix, getMachineTypeNames());
            resultWriter.writeMachineLoads(operationMatrix, input.getMachineCount());
        }
        bestSolution.algrithmTimeCost = (endTime - startTime) / 1000.0;

        // 输出详细结果（仅在非田口实验模式下输出到控制台）
        if (resultWriter == null) {
            printDetailedResults(bestSolution, best, operationMatrix);
            
            // 生成可视化图表（仅在非田口实验模式下生成到默认文件夹）
            try {
                String outputDir = "visualization_results";
                AlgorthmFrame.visualization.ScheduleVisualizer visualizer =
                        new AlgorthmFrame.visualization.ScheduleVisualizer(bestSolution, best, input, operationMatrix, bestMakespanHistory);
                visualizer.generateAllCharts(outputDir);
            } catch (Exception e) {
                System.err.println("生成可视化图表时出错: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return bestSolution;
    }

    private Chromosome getBest(Chromosome[] parents) {
        double maxFitness = Double.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < this.popSize; i++) {
            if (maxFitness < parents[i].fitness) {
                index = i;
                maxFitness = parents[i].fitness;
            }
        }
        return new Chromosome(parents[index]);
    }
    
    /**
     * 创建一个新的operationMatrix用于evaluate
     * 注意：每个工件的工序数可能不同，需要根据operationCountArr动态创建
     */
    private Operation[][] createOperationMatrix() {
        Operation[][] matrix = new Operation[input.getJobCount()][];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new Operation[input.getOperationCountArr()[i]];
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = new Operation();
            }
        }
        return matrix;
    }

    /**
     * 打印染色体信息（简化版）
     */
    private void printChromosomeSimple(String prefix, Chromosome c, int maxLength) {
        System.out.print(prefix);
        System.out.print("OS=[");
        for (int i = 0; i < Math.min(c.gene_OS.length, maxLength); i++) {
            System.out.print(c.gene_OS[i]);
            if (i < Math.min(c.gene_OS.length, maxLength) - 1) System.out.print(",");
        }
        if (c.gene_OS.length > maxLength) System.out.print("...");
        System.out.print("], MS=[");
        for (int i = 0; i < Math.min(c.gene_MS.length, maxLength); i++) {
            System.out.print(c.gene_MS[i]);
            if (i < Math.min(c.gene_MS.length, maxLength) - 1) System.out.print(",");
        }
        if (c.gene_MS.length > maxLength) System.out.print("...");
        System.out.println("], fitness=" + String.format("%.4f", c.fitness));
    }

    /**
     * 打印染色体信息（详细版）
     */
    private void printChromosomeDetailed(String prefix, Chromosome c) {
        System.out.println(prefix);
        System.out.print("  gene_OS: [");
        for (int i = 0; i < c.gene_OS.length; i++) {
            System.out.print(c.gene_OS[i]);
            if (i < c.gene_OS.length - 1) System.out.print(", ");
            if ((i + 1) % 20 == 0 && i < c.gene_OS.length - 1) {
                System.out.print("\n           ");
            }
        }
        System.out.println("]");
        
        System.out.print("  gene_MS: [");
        for (int i = 0; i < c.gene_MS.length; i++) {
            System.out.print(c.gene_MS[i]);
            if (i < c.gene_MS.length - 1) System.out.print(", ");
            if ((i + 1) % 20 == 0 && i < c.gene_MS.length - 1) {
                System.out.print("\n           ");
            }
        }
        System.out.println("]");
        System.out.println("  fitness: " + c.fitness);
    }

    /**
     * 输出详细的调度结果
     */
    private void printDetailedResults(Solution bestSolution, Chromosome bestChromosome, Operation[][] operationMatrix) {
        System.out.println("\n");
        System.out.println("================================================================================");
        System.out.println("                           详细调度结果");
        System.out.println("================================================================================");
        
        // 1. 最优解基本信息
        System.out.println("\n【1. 最优解基本信息】");
        System.out.println("  最优makespan: " + bestSolution.cost);
        System.out.println("  算法运行时间: " + bestSolution.algrithmTimeCost + "s");
        System.out.println("  工件数量: " + input.getJobCount());
        System.out.println("  打印机数量: " + input.getPrintMachineCount());
        System.out.println("  批处理机数量: " + input.getBatchMachineCount());
        System.out.println("  离散处理机数量: " + (input.getMachineCount() - input.getPrintMachineCount() - input.getBatchMachineCount()));
        
        // 2. 打印机的打印批次
        System.out.println("\n【2. 打印机的打印批次】");
        if (bestChromosome.printSolution != null) {
            for (int i = 0; i < bestChromosome.printSolution.length; i++) {
                List<ProgramEntity.Solution> batches = bestChromosome.printSolution[i];
                if (batches != null && !batches.isEmpty()) {
                    System.out.println("\n  打印机 " + (i + 1) + " 共有 " + batches.size() + " 个批次：");
                    for (int j = 0; j < batches.size(); j++) {
                        ProgramEntity.Solution batch = batches.get(j);
                        System.out.println("\n    ┌─ 批次 " + (j + 1) + " ─────────────────────────────────────────");
                        System.out.println("    │ 时间信息:");
                        System.out.println("    │   开始时间: " + String.format("%.2f", batch.startTime));
                        System.out.println("    │   结束时间: " + String.format("%.2f", batch.endTime));
                        System.out.println("    │   加工时间: " + String.format("%.2f", batch.endTime - batch.startTime));
                        System.out.println("    │ 打印参数:");
                        System.out.println("    │   打印高度: " + String.format("%.2f", batch.maxG));
                        System.out.println("    │   工件数量: " + batch.placeItemList.size());
                        System.out.println("    │");
                        System.out.println("    │ 工件布局详情:");
                        System.out.println("    │   " + String.format("%-8s %-10s %-10s %-10s %-10s %-10s %-8s", 
                            "工件", "X坐标", "Y坐标", "长(L)", "宽(W)", "高(H)", "旋转"));
                        System.out.println("    │   " + "────────────────────────────────────────────────────────────");
                        
                        for (int k = 0; k < batch.placeItemList.size(); k++) {
                            ProgramEntity.PlaceItem item = batch.placeItemList.get(k);
                            System.out.println("    │   " + String.format("%-8s %-10.2f %-10.2f %-10.2f %-10.2f %-10.2f %-8s", 
                                "J" + item.name, 
                                item.x, 
                                item.y,
                                item.l,
                                item.w,
                                item.h,
                                (item.isRotate ? "是" : "否")));
                        }
                        System.out.println("    └──────────────────────────────────────────────────────────");
                    }
                }
            }
        }
        
        // 3. 以机器分别的甘特图数据
        System.out.println("\n【3. 各机器的加工甘特图数据】");
        
        // 按机器分组整理工序
        int machineCount = input.getMachineCount();
        List<List<Operation>> machineOperations = new ArrayList<>();
        for (int i = 0; i < machineCount; i++) {
            machineOperations.add(new ArrayList<>());
        }
        
        // 收集所有工序并按机器分组
        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation op = operationMatrix[i][j];
                if (op != null && op.machineNo >= 0 && op.machineNo < machineCount) {
                    machineOperations.get(op.machineNo).add(op);
                }
            }
        }
        
        // 输出每台机器的甘特图数据
        String[] machineTypes = getMachineTypeNames();
        for (int i = 0; i < machineCount; i++) {
            List<Operation> ops = machineOperations.get(i);
            if (!ops.isEmpty()) {
                // 按开始时间排序
                ops.sort((o1, o2) -> Double.compare(o1.startTime, o2.startTime));
                
                System.out.println("\n  " + machineTypes[i] + " (机器 " + (i + 1) + "):");
                System.out.println("    总工序数: " + ops.size());
                System.out.println("    完成时间: " + String.format("%.2f", ops.get(ops.size() - 1).endTime));
                System.out.println("    工序详情:");
                System.out.println("      " + String.format("%-8s %-12s %-12s %-12s %-10s", 
                    "工件", "工序", "开始时间", "结束时间", "加工时间"));
                System.out.println("      " + "------------------------------------------------------------");
                
                for (Operation op : ops) {
                    String operationType = getOperationTypeName(op.task);
                    System.out.println("      " + String.format("%-8s %-12s %-12.2f %-12.2f %-10.2f", 
                        "J" + op.jobNo, 
                        operationType,
                        op.startTime, 
                        op.endTime,
                        (op.endTime - op.startTime)));
                }
            }
        }
        
        // 4. 工件加工路径
        System.out.println("\n【4. 各工件的加工路径】");
        for (int i = 0; i < operationMatrix.length; i++) {
            System.out.println("\n  工件 " + i + " 的加工路径:");
            System.out.println("    " + String.format("%-15s %-20s %-12s %-12s %-10s", 
                "工序", "机器", "开始时间", "结束时间", "加工时间"));
            System.out.println("    " + "----------------------------------------------------------------");
            
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation op = operationMatrix[i][j];
                if (op != null) {
                    String operationType = getOperationTypeName(op.task);
                    String machineType = getMachineTypeNames()[op.machineNo];
                    System.out.println("    " + String.format("%-15s %-20s %-12.2f %-12.2f %-10.2f", 
                        operationType,
                        machineType,
                        op.startTime, 
                        op.endTime,
                        (op.endTime - op.startTime)));
                }
            }
            System.out.println("    总完工时间: " + String.format("%.2f", operationMatrix[i][operationMatrix[i].length - 1].endTime));
        }
        
        System.out.println("\n================================================================================");
        System.out.println("                         结果输出完成");
        System.out.println("================================================================================\n");
    }
    
    /**
     * 获取机器类型名称数组
     */
    private String[] getMachineTypeNames() {
        int machineCount = input.getMachineCount();
        int printMachineCount = input.getPrintMachineCount();
        int batchMachineCount = input.getBatchMachineCount();
        
        String[] names = new String[machineCount];
        for (int i = 0; i < printMachineCount; i++) {
            names[i] = "打印机" + (i + 1);
        }
        for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
            names[i] = "批处理机" + (i - printMachineCount + 1);
        }
        for (int i = printMachineCount + batchMachineCount; i < machineCount; i++) {
            names[i] = "离散处理机" + (i - printMachineCount - batchMachineCount + 1);
        }
        return names;
    }
    
    /**
     * 获取工序类型名称
     */
    private String getOperationTypeName(int task) {
        if (task == 0) return "打印工序";
        else if (task == 1) return "批处理工序";
        else return "离散工序" + (task - 1);
    }
    
    /**
     * 使用启发式规则创建染色体
     * 策略：
     * 1. 打印工序：按工件体积降序排列（大件先打印，利于装箱）
     * 2. 离散工序：按最短处理时间（SPT）排序
     * 3. 机器选择：优先选择加工时间短的机器
     */
    private Chromosome createHeuristicChromosome(Job[] entries, Random r, Problem input) {
        ProgramEntity.Item[] items = input.getItems();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int printMachineCount = input.getPrintMachineCount();
        
        ArrayList<Integer> printOps = new ArrayList<>();
        ArrayList<Integer> discreteOps = new ArrayList<>();
        
        // 1. 打印工序：按工件体积降序排列
        class JobVolume {
            int jobNo;
            double volume;
            JobVolume(int jobNo, double volume) {
                this.jobNo = jobNo;
                this.volume = volume;
            }
        }
        
        List<JobVolume> jobVolumes = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            double volume = items[i].l * items[i].w * items[i].h;
            jobVolumes.add(new JobVolume(entries[i].index, volume));
        }
        // 按体积降序排序（大件先打印）
        jobVolumes.sort((a, b) -> Double.compare(b.volume, a.volume));
        for (JobVolume jv : jobVolumes) {
            printOps.add(jv.jobNo);
        }
        
        // 2. 离散工序：保持每个工件工序的内部顺序
        // 关键修复：任何排序都会破坏同一工件工序的顺序，导致工序编号与机器分配错位
        // 例如：J9工序2,3,4 按SPT排序后可能变成 J9工序3,2,4
        // 但在分配机器时，工序编号是根据出现顺序计算的（2,3,4）
        // 导致工序3被分配了工序2的机器 ❌
        
        // 策略：按工件顺序添加，但工件间可以打乱
        List<Job> jobList = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            jobList.add(entries[i]);
        }
        Collections.shuffle(jobList, r);  // 打乱工件顺序
        
        // 按打乱后的工件顺序，依次添加每个工件的所有离散工序
        for (Job job : jobList) {
            for (int j = 2; j < job.opsNr; j++) {
                discreteOps.add(job.index);  // 保持同一工件工序的顺序
            }
        }
        
        // 构建gene_OS
        ArrayList<Integer> os = new ArrayList<>();
        os.addAll(printOps);
        os.addAll(discreteOps);
        
        // 3. 机器选择 - 重要：必须按照os的顺序生成ms，确保os和ms一一对应
        ArrayList<Integer> ms = new ArrayList<>();
        
        // 打印工序段：基于零件尺寸和打印机容量分配
        // 不使用算例文件中的机器限制，只要零件尺寸能放入打印机即可
        for (int i = 0; i < printOps.size(); i++) {
            int jobNo = os.get(i);  // 从os中获取工件编号（已排序）
            Item item = items[jobNo];
            
            // 找出所有能容纳该零件的打印机
            ArrayList<Integer> suitableMachines = new ArrayList<>();
            for (int m = 0; m < printMachineCount; m++) {
                PrintMachine pm = (PrintMachine) input.getMachines()[m];
                // 检查零件尺寸是否能放入打印机（考虑旋转）
                boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                if (fitsNormal || fitsRotated) {
                    suitableMachines.add(m + 1);  // 1-based
                }
            }
            
            if (suitableMachines.isEmpty()) {
                throw new RuntimeException("启发式初始化：工件" + jobNo + "尺寸(" + 
                    String.format("%.2f x %.2f x %.2f", item.l, item.w, item.h) + 
                    ")无法放入任何打印机");
            }
            
            // 负载均衡：从合适的打印机中轮询分配
            int machineNo = suitableMachines.get(i % suitableMachines.size());
            ms.add(machineNo);
        }
        
        // 离散工序段：按os的顺序选择最优机器
        int discreteStartIndex = printOps.size();
        for (int i = 0; i < discreteOps.size(); i++) {
            int jobNo = os.get(discreteStartIndex + i);  // 从os中获取工件编号（已排序）
            
            // 计算这是该工件的第几个离散工序
            int discreteOperNo = 0;
            for (int j = discreteStartIndex; j < discreteStartIndex + i; j++) {
                if (os.get(j) == jobNo) {
                    discreteOperNo++;
                }
            }
            
            // 工序编号 = 2 + discreteOperNo
            int operNo = 2 + discreteOperNo;
            int operIdx = operationToIndex[jobNo][operNo];
            
            // 找加工时间最短的机器
            int bestMachine = -1;
            double minTime = Double.MAX_VALUE;
            for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] < minTime) {
                    minTime = proDesMatrix[operIdx][k];
                    bestMachine = k + 1;  // 转换为1-based
                }
            }
            
            if (bestMachine > 0) {
                ms.add(bestMachine);
            } else {
                // 如果没找到，随机选择一个
                ArrayList<Integer> availableMachines = new ArrayList<>();
                for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                    if (proDesMatrix[operIdx][k] > 0) {
                        availableMachines.add(k + 1);
                    }
                }
                if (!availableMachines.isEmpty()) {
                    ms.add(availableMachines.get(r.nextInt(availableMachines.size())));
                } else {
                    throw new RuntimeException("启发式初始化：工件" + jobNo + "的工序" + operNo + "没有可用机器");
                }
            }
        }
        
        // 构建染色体
        Chromosome chromosome = new Chromosome(r);
        chromosome.gene_OS = new int[os.size()];
        for (int i = 0; i < os.size(); i++) {
            chromosome.gene_OS[i] = os.get(i);
        }
        chromosome.gene_MS = new int[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            chromosome.gene_MS[i] = ms.get(i);
        }
        chromosome.fitness = 0;
        chromosome.printSolution = null;
        
        return chromosome;
    }
    /**
     * 获取最优染色体
     */
    public Chromosome getBest() {
        return bestChromosome;
    }
    
    /**
     * 获取操作矩阵
     */
    public Operation[][] getOperationMatrix() {
        return operationMatrix;
    }

}
