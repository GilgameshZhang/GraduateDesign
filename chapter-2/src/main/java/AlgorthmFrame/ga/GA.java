package AlgorthmFrame.ga;

import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProgramEntity.Job;
import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.DiscreteProcessingMachine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import ProblemFrame.Solution;

import java.util.*;

public class GA {
    private Problem input;
    private Operation[][] operationMatrix;
    private Random r;

    private final int popSize = 50;// population size 400
    private final double pr = 0.10;// Reproduction probability
    private final double pc = 0.80;// Crossover probability
    private final double pm = 0.10;// Mutation probability

    private final int maxT = 9;// tabu list length
    private final int maxTabuLimit = 100;// maxTSIterSize = maxTabuLimit * (Gen / maxGen)
    private final double pt = 0.05;// tabu probability

    private final double pp = 0.30;// perturbation probability

    private final int maxGen = 200;// iterator for 200 time for each loop
    private final int maxStagnantStep = 30;// max iterator no improve
    private final int timeLimit = -1;// no time limit
    
    // 调试控制参数
    public static boolean DEBUG_MODE = false; // 总调试开关
    public static boolean PRINT_CROSSOVER = true; // 是否打印交叉操作
    public static boolean PRINT_MUTATION = true; // 是否打印变异操作
    public static int PRINT_INTERVAL = 50; // 打印间隔（每N代打印一次）
    public static int PRINT_CHROMOSOME_LENGTH = 10; // 打印染色体的前N个基因

    public GA(Problem input) {
        this.input = input;
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
     * the whole logic of the flexible job shop sheduling problem
     */
    public Solution solve() {
        int jobCount = input.getJobCount();
        CaculateFitness c = new CaculateFitness();
        ChromosomeOperation chromOps = new ChromosomeOperation(r, input);
//        TabuSearch1 tabu = new TabuSearch1(input, r);

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

        // 随机生成初始种群
        Chromosome[] parents = new Chromosome[this.popSize];// 染色体
        for (int i = 0; i < this.popSize; i++) {
            parents[i] = new Chromosome(jobs, r);
            parents[i].fitness = 1.0 / c.evaluate(parents[i], input, operationMatrix);
        }

        Chromosome[] children = new Chromosome[this.popSize];
        for (int i = 0; i < this.popSize; i++) {
            children[i] = new Chromosome(parents[i]);
        }

        // 获取最优子代
        double maxFitness = Double.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < this.popSize; i++) {
            if (maxFitness < parents[i].fitness) {
                index = i;
                maxFitness = parents[i].fitness;
            }
        }
        Chromosome best = new Chromosome(parents[index]);
        Chromosome currentBest = new Chromosome(parents[index]);

        int noImprove = 0;
        int gen = 0;
        while (gen < this.maxGen) {

            // 陷入局部最优时进行扰动:取部分精英个体后随机生成新个体
            if (gen - noImprove > this.maxStagnantStep) {
//                break;

                int num = (int) (pp * popSize);
                ArrayList<Chromosome> p = new ArrayList<>();
                Collections.addAll(p, parents);
                Collections.sort(p);
                for (int i = 0; i < num; i++)
                    parents[i] = p.get(i);
                for (int i = num; i < this.popSize; i++) {
                    parents[i] = new Chromosome(jobs, r);
                    parents[i].fitness = 1.0 / c.evaluate(parents[i], input, operationMatrix);
                }

                noImprove = gen;
            }

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
                    int fatherIndex = i;
                    int motherIndex = i + 1;
                    
                    // 打印交叉前的染色体（只打印第一对，避免输出过多）
                    if (printCrossoverDetails && i == 0) {
                        System.out.println("\n========== 代数 " + gen + ": 交叉操作示例 ==========");
                        printChromosomeSimple("  交叉前-父代" + fatherIndex + ": ", children[fatherIndex], PRINT_CHROMOSOME_LENGTH);
                        printChromosomeSimple("  交叉前-母代" + motherIndex + ": ", children[motherIndex], PRINT_CHROMOSOME_LENGTH);
                    }
                    
                    chromOps.Crossover(children[fatherIndex], children[motherIndex]);
                    
                    // 打印交叉后的染色体
                    if (printCrossoverDetails && i == 0) {
                        printChromosomeSimple("  交叉后-父代" + fatherIndex + ": ", children[fatherIndex], PRINT_CHROMOSOME_LENGTH);
                        printChromosomeSimple("  交叉后-母代" + motherIndex + ": ", children[motherIndex], PRINT_CHROMOSOME_LENGTH);
                        System.out.println("================================================\n");
                    }
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
                children[i].fitness = 1.0 / c.evaluate(children[i], input, operationMatrix);
                int maxTSIterSize = (int) (maxGen * ((float) gen / (float) maxGen));
                Solution sol = new Solution(operationMatrix, children[i], input, 1.0 / children[i].fitness);

                // TS1
                //sol = NeiborAl2.search(sol, maxTSIterSize);

                // TS2
//                sol = NeighbourAlgorithms.neighbourSearch(sol);

                parents[i] = sol.toChromosome();
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
                best = new Chromosome(currentBest);
                noImprove = gen;
                System.out.println("In " + gen + " generation, find new best fitness is:" + currentBest.fitness);
            }
            System.out.println(" After " + gen + " generation, the best fitness is:" + best.fitness);

            gen++;
        }
        Solution bestSolution = new Solution(operationMatrix, best, input, c.evaluate(best, input, operationMatrix));
        System.out.println();
        System.out.println(" After " + gen + " generation, the best schedule cost is:" + bestSolution.cost);

        long endTime = System.currentTimeMillis();// 算法开始
        System.out.println(" 算法时间花费：" + (endTime - startTime) / 1000.0 + "s");
        bestSolution.algrithmTimeCost = (endTime - startTime) / 1000.0;

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

}
