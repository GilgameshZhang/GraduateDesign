package AlgorthmFrame.randomKeyGA;

import ProblemFrame.GAParameters;
import ProgramEntity.Problem;
import ProgramEntity.Solution;
import util.java.ExperimentResultWriter;

import java.util.*;

/**
 * 基于随机密钥编码的遗传算法
 * 
 * 核心特点：
 * 1. 多子串随机密钥编码（零件序列、朝向、旋转、机器分配）
 * 2. 参数化均匀交叉（PUX）
 * 3. 精英保留策略（10%最优个体直接进入下一代）
 * 4. 改进的BLF嵌套启发式
 * 5. 批处理和离散工序的队列调度
 * 
 * 对比算法说明：
 * 该算法作为对比算法，与主算法（混合GA）进行性能对比
 */
public class RandomKeyGA {
    
    private Problem problem;
    private Random random;
    private GAParameters params;
    
    // 算法参数
    private int populationSize;
    private double crossoverRate;
    private double mutationRate;
    private int maxGenerations;
    private int maxStagnantGenerations;
    private double eliteRatio;  // 精英保留比例（默认10%）
    
    // 核心组件
    private RandomKeyEvaluator evaluator;
    private RandomKeyOperations operations;
    
    // 结果记录
    private List<Double> bestMakespanHistory;
    private RandomKeyChromosome bestChromosome;
    private ExperimentResultWriter resultWriter;
    
    /**
     * 构造函数
     * @param problem 问题实例
     * @param params GA参数配置
     */
    public RandomKeyGA(Problem problem, GAParameters params) {
        this.problem = problem;
        this.params = params;
        this.random = new Random();
        
        // 从params中提取参数
        this.populationSize = params.popSize;
        this.crossoverRate = params.pc;
        this.mutationRate = params.pm;
        this.maxGenerations = 999999;  // 使用很大的值，由时间限制终止
        this.maxStagnantGenerations = params.maxStagnantStep;
        this.eliteRatio = 0.1;  // 10%精英保留
        
        // 初始化核心组件
        this.evaluator = new RandomKeyEvaluator(problem);
        this.operations = new RandomKeyOperations(random);
        this.bestMakespanHistory = new ArrayList<>();
    }
    
    /**
     * 设置结果输出器
     */
    public void setResultWriter(ExperimentResultWriter writer) {
        this.resultWriter = writer;
    }
    
    /**
     * 求解算法主流程
     * @return 最优解
     */
    public RandomKeySolution solve() {
        long startTime = System.currentTimeMillis();
        int jobCount = problem.getJobCount();
        int printMachineCount = problem.getPrintMachineCount();
        
        printOrWrite("========== 随机密钥遗传算法开始 ==========");
        printOrWrite("问题规模: " + jobCount + "个零件, " + printMachineCount + "台打印机");
        printOrWrite("种群大小: " + populationSize);
        printOrWrite("交叉率: " + crossoverRate);
        printOrWrite("变异率: " + mutationRate);
        printOrWrite("精英保留比例: " + (eliteRatio * 100) + "%");
        printOrWrite("终止条件: " + params.maxRunTime + "分钟 或 " + maxStagnantGenerations + "代无改进");
        printOrWrite("==========================================\n");
        
        // 1. 初始化种群
        printOrWrite("正在初始化种群...");
        RandomKeyChromosome[] population = initializePopulation(jobCount, printMachineCount);
        
        // 评估初始种群
        for (RandomKeyChromosome chromosome : population) {
            evaluator.evaluate(chromosome);
        }
        
        // 排序并记录最优解
        Arrays.sort(population);
        bestChromosome = new RandomKeyChromosome(population[0]);
        bestMakespanHistory.add(bestChromosome.makespan);
        
        printOrWrite("初始种群最优解: Makespan = " + String.format("%.2f", bestChromosome.makespan));
        printOrWrite("初始种群平均解: Makespan = " + String.format("%.2f", getAverageMakespan(population)));
        printOrWrite("");
        
        // 2. 进化循环
        int generation = 0;
        int stagnantCount = 0;
        double previousBest = bestChromosome.makespan;
        
        while (generation < maxGenerations && stagnantCount < maxStagnantGenerations) {
            // 检查时间限制
            long currentTime = System.currentTimeMillis();
            double elapsedMinutes = (currentTime - startTime) / (1000.0 * 60.0);
            if (elapsedMinutes >= params.maxRunTime) {
                printOrWrite("\n达到时间限制(" + params.maxRunTime + "分钟)，算法终止。");
                break;
            }
            
            generation++;
            
            // 2.1 选择精英
            int eliteSize = (int) (populationSize * eliteRatio);
            RandomKeyChromosome[] elites = operations.selectElites(population, eliteSize);
            
            // 2.2 生成新种群
            RandomKeyChromosome[] newPopulation = new RandomKeyChromosome[populationSize];
            
            // 精英直接进入下一代
            for (int i = 0; i < eliteSize; i++) {
                newPopulation[i] = new RandomKeyChromosome(elites[i]);
            }
            
            // 2.3 通过交叉和变异生成剩余个体
            for (int i = eliteSize; i < populationSize; i += 2) {
                // 锦标赛选择父代
                RandomKeyChromosome parent1 = operations.tournamentSelection(population, 3);
                RandomKeyChromosome parent2 = operations.tournamentSelection(population, 3);
                
                RandomKeyChromosome offspring1, offspring2;
                
                // 交叉
                if (random.nextDouble() < crossoverRate) {
                    RandomKeyChromosome[] offsprings = operations.crossover(parent1, parent2);
                    offspring1 = offsprings[0];
                    offspring2 = offsprings[1];
                } else {
                    offspring1 = new RandomKeyChromosome(parent1);
                    offspring2 = new RandomKeyChromosome(parent2);
                }
                
                // 变异
                operations.mutate(offspring1, mutationRate);
                operations.mutate(offspring2, mutationRate);
                
                // 评估适应度
                evaluator.evaluate(offspring1);
                evaluator.evaluate(offspring2);
                
                // 加入新种群
                newPopulation[i] = offspring1;
                if (i + 1 < populationSize) {
                    newPopulation[i + 1] = offspring2;
                }
            }
            
            // 2.4 更新种群
            population = newPopulation;
            Arrays.sort(population);
            
            // 2.5 更新最优解
            if (population[0].makespan < bestChromosome.makespan) {
                bestChromosome = new RandomKeyChromosome(population[0]);
                stagnantCount = 0;
            } else {
                stagnantCount++;
            }
            
            bestMakespanHistory.add(bestChromosome.makespan);
            
            // 2.6 输出进度
            if (generation % 50 == 0 || generation == 1) {
                double avgMakespan = getAverageMakespan(population);
                double improvement = previousBest - bestChromosome.makespan;
                
                printOrWrite(String.format("Gen %4d | 最优: %.2f | 平均: %.2f | 改进: %.2f | 停滞: %d/%d | 耗时: %.1f分钟",
                    generation, bestChromosome.makespan, avgMakespan, improvement,
                    stagnantCount, maxStagnantGenerations, elapsedMinutes));
                
                previousBest = bestChromosome.makespan;
            }
        }
        
        // 3. 输出最终结果
        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000.0;
        double totalMinutes = totalTime / 60.0;
        
        // 判断终止原因
        String terminationReason;
        if (totalMinutes >= params.maxRunTime) {
            terminationReason = "达到时间限制(" + params.maxRunTime + "分钟)";
        } else if (stagnantCount >= maxStagnantGenerations) {
            terminationReason = "达到停滞代数限制(" + maxStagnantGenerations + "代无改进)";
        } else {
            terminationReason = "达到最大迭代次数";
        }
        
        printOrWrite("\n========== 随机密钥遗传算法结束 ==========");
        printOrWrite("终止原因: " + terminationReason);
        printOrWrite("总迭代次数: " + generation);
        printOrWrite("最优Makespan: " + String.format("%.2f", bestChromosome.makespan));
        printOrWrite("总耗时: " + String.format("%.2f", totalTime) + "秒 (" + String.format("%.2f", totalMinutes) + "分钟)");
        printOrWrite("==========================================\n");
        
        // 输出最优染色体信息
        if (resultWriter != null) {
            bestChromosome.printChromosome();
        }
        
        // 返回解
        return new RandomKeySolution(bestChromosome, bestMakespanHistory);
    }
    
    /**
     * 初始化种群
     */
    private RandomKeyChromosome[] initializePopulation(int partCount, int machineCount) {
        RandomKeyChromosome[] population = new RandomKeyChromosome[populationSize];
        
        for (int i = 0; i < populationSize; i++) {
            population[i] = new RandomKeyChromosome(partCount, machineCount, random);
        }
        
        return population;
    }
    
    /**
     * 计算种群平均makespan
     */
    private double getAverageMakespan(RandomKeyChromosome[] population) {
        double sum = 0.0;
        for (RandomKeyChromosome chromosome : population) {
            sum += chromosome.makespan;
        }
        return sum / population.length;
    }
    
    /**
     * 输出到控制台或文件
     */
    private void printOrWrite(String text) {
        if (resultWriter != null) {
            resultWriter.writeLine(text);
        } else {
            System.out.println(text);
        }
    }
    
    /**
     * 获取最优makespan历史
     */
    public List<Double> getBestMakespanHistory() {
        return bestMakespanHistory;
    }
    
    /**
     * 获取最优染色体
     */
    public RandomKeyChromosome getBestChromosome() {
        return bestChromosome;
    }
    
    /**
     * 解对象（包含染色体和历史记录）
     */
    public static class RandomKeySolution {
        public RandomKeyChromosome chromosome;
        public List<Double> makespanHistory;
        public double cost;  // 为了与原GA的Solution接口兼容
        
        public RandomKeySolution(RandomKeyChromosome chromosome, List<Double> makespanHistory) {
            this.chromosome = chromosome;
            this.makespanHistory = makespanHistory;
            this.cost = chromosome.makespan;
        }
    }
}

