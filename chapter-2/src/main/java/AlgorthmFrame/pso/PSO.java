package AlgorthmFrame.pso;

import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProblemFrame.InitializationStrategy;
import ProblemFrame.Particle;
import ProblemFrame.PSOParameters;
import ProblemFrame.Solution;
import ProblemFrame.GAParameters;  // 引入GA参数
import AlgorthmFrame.ga.ChromosomeOperation;  // 引入染色体操作类
import ProgramEntity.Item;
import ProgramEntity.Job;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import util.java.ExperimentResultWriter;

import java.util.*;

/**
 * 粒子群算法 (PSO) - 用于3D打印与离散加工混合流水车间调度
 * 
 * 算法流程：
 * 1. 初始化粒子群（使用启发式策略）
 * 2. 评估适应度
 * 3. 更新个体最优和全局最优
 * 4. 更新速度和位置
 * 5. 重复2-4直到终止条件
 * 
 * PSO参数：
 * - w: 惯性权重（inertia weight）
 * - c1: 个体学习因子（cognitive coefficient）
 * - c2: 社会学习因子（social coefficient）
 */
public class PSO {
    // 问题实例
    private Problem input;
    private Operation[][] operationMatrix;
    private Random r;
    
    // PSO参数
    private int swarmSize;          // 粒子群大小
    private double w;                // 惯性权重（当前值）
    private double w_max = 0.9;      // 最大惯性权重
    private double w_min = 0.4;      // 最小惯性权重
    private double c1;               // 个体学习因子
    private double c2;               // 社会学习因子
    private int maxStagnantStep;    // 最大停滞步数
    private double maxRunTime;      // 最大运行时间（分钟）
    
    // 迭代控制
    private int currentIteration;    // 当前迭代次数
    private int maxIterations;       // 最大迭代次数
    
    // 全局最优
    private Particle gBest;         // 全局最优粒子
    
    // 结果输出
    private ExperimentResultWriter resultWriter;
    
    // 适应度计算
    private CaculateFitness caculateFitness;
    
    // 染色体操作类（用于N1-N5局部搜索）
    private ChromosomeOperation chromOps;
    private GAParameters gaParams;  // GA参数配置
    
    // 历史记录
    private List<Double> bestMakespanHistory;
    
    // FITNESS常量
    public static final double FITNESS_SCALE = 1000000.0;
    
    /**
     * 默认构造函数
     */
    public PSO(Problem input) {
        this.input = input;
        this.r = new Random();
        this.caculateFitness = new CaculateFitness();
        this.operationMatrix = createOperationMatrix();
        
        // 初始化染色体操作类和GA参数（用于N1-N5局部搜索）
        this.gaParams = GAParameters.getDefaultParameters();
        this.chromOps = new ChromosomeOperation(r,input , caculateFitness, gaParams);
        
        // 默认PSO参数
        this.swarmSize = 200;
        this.w = 0.7;              // 惯性权重
        this.c1 = 1.5;             // 个体学习因子
        this.c2 = 1.5;             // 社会学习因子
        this.maxStagnantStep = 30000;
        this.maxRunTime = 10.0;     // 3分钟
        
        this.bestMakespanHistory = new ArrayList<>();
    }
    
    /**
     * 参数化构造函数
     */
    public PSO(Problem input, PSOParameters params) {
        this.input = input;
        this.r = new Random();
        this.caculateFitness = new CaculateFitness();
        this.operationMatrix = createOperationMatrix();
        
        // 初始化染色体操作类和GA参数（用于N1-N5局部搜索）
        this.gaParams = GAParameters.getDefaultParameters();
        this.chromOps = new ChromosomeOperation(r,input , caculateFitness, gaParams);
        
        this.swarmSize = params.swarmSize;
        this.w = params.w;
        this.c1 = params.c1;
        this.c2 = params.c2;
        this.maxStagnantStep = params.maxStagnantStep;
        this.maxRunTime = params.maxRunTime;
        
        this.bestMakespanHistory = new ArrayList<>();
    }
    
    /**
     * 设置结果输出器
     */
    public void setResultWriter(ExperimentResultWriter writer) {
        this.resultWriter = writer;
    }
    
    /**
     * 创建操作矩阵
     * 注意：每个工件的工序数可能不同，需要根据operationCountArr动态创建
     * 并且必须初始化每个Operation对象
     */
    private Operation[][] createOperationMatrix() {
        int jobCount = input.getJobCount();
        Operation[][] matrix = new Operation[jobCount][];
        for (int i = 0; i < matrix.length; i++) {
            matrix[i] = new Operation[input.getOperationCountArr()[i]];
            for (int j = 0; j < matrix[i].length; j++) {
                matrix[i][j] = new Operation();  // 关键：必须初始化每个Operation对象
            }
        }
        return matrix;
    }
    
    /**
     * 输出到控制台或文件
     */
    private void printOrWrite(String message) {
        if (resultWriter != null) {
            resultWriter.writeIterationInfo(message);
        } else {
            System.out.println(message);
        }
    }
    
    /**
     * PSO主算法
     */
    public Solution solve() {
        long startTime = System.currentTimeMillis();
        
        // 初始化迭代控制变量
        currentIteration = 0;
        maxIterations = maxStagnantStep * 10;  // 估算最大迭代次数
        
        printOrWrite("================================================================================");
        printOrWrite("                        PSO算法开始运行 (连续编码模式)");
        printOrWrite("================================================================================");
        printOrWrite("粒子群大小: " + swarmSize);
        printOrWrite("惯性权重 w: " + w_max + " → " + w_min + " (动态递减)");
        printOrWrite("个体学习因子 c1: " + c1);
        printOrWrite("社会学习因子 c2: " + c2);
        printOrWrite("最大停滞步数: " + maxStagnantStep);
        printOrWrite("最大运行时间: " + maxRunTime + " 分钟");
        printOrWrite("================================================================================\n");
        
        // 1. 初始化粒子群
        printOrWrite("[初始化] 生成初始粒子群（连续编码）...");
        Particle[] swarm = initializeSwarm();
        
        // 2. 评估初始适应度
        for (Particle particle : swarm) {
            evaluateParticle(particle);
            particle.updatePBest();
        }
        
        // 3. 初始化全局最优
        gBest = findBestParticle(swarm);
        double initialMakespan = FITNESS_SCALE / gBest.fitness;
        printOrWrite(String.format("[初始化] 初始最优 Makespan: %.2f\n", initialMakespan));
        
        bestMakespanHistory.add(initialMakespan);
        
        // 4. 主循环
        int noImprove = 0;
        double previousBestMakespan = initialMakespan;
        
        printOrWrite("开始迭代优化...\n");
        
        while (noImprove < maxStagnantStep && 
               System.currentTimeMillis() - startTime <= maxRunTime * 60 * 1000) {
            
            currentIteration++;
            
            // 更新所有粒子
            for (Particle particle : swarm) {
                // 更新速度（连续PSO公式）
                updateVelocity(particle);
                
                // 更新位置（连续→离散映射）
                updatePosition(particle);
                
                // 评估新位置
                evaluateParticle(particle);
                
                // 更新个体最优
                particle.updatePBest();
            }
            
            // 更新全局最优
            Particle currentBest = findBestParticle(swarm);
            if (currentBest.fitness > gBest.fitness) {
                gBest = new Particle(currentBest);
                noImprove = 0;
            } else {
                noImprove++;
            }
            
            // ========== 使用N1-N5邻域搜索策略 ==========
            // 策略1：每次迭代对gbest进行轻量级N1-N5搜索（5次迭代）
//            if (gBest.localSearchWithN1N5(input, caculateFitness, operationMatrix, chromOps, 5)) {
//                evaluateParticle(gBest);
//            }
//
//            // 策略2：每5代对所有粒子的pbest进行中等强度N1-N5搜索（10次迭代）
//            if (currentIteration % 5 == 0) {
//                int improvedCount = 0;
//                for (Particle p : swarm) {
//                    // 临时将pbest赋值给粒子进行搜索
//                    int[] origOS = p.gene_OS.clone();
//                    int[] origMS = p.gene_MS.clone();
//
//                    if (!p.useContinuousEncoding) {
//                        p.gene_OS = p.pBest_OS.clone();
//                        p.gene_MS = p.pBest_MS.clone();
//                    }
//
//                    if (p.localSearchWithN1N5(input, caculateFitness, operationMatrix, chromOps, 10)) {
//                        // 更新pbest
//                        p.pBest_OS = p.gene_OS.clone();
//                        p.pBest_MS = p.gene_MS.clone();
//                        double makespan = 1000000.0 / p.fitness;
//                        p.pBestFitness = p.fitness;
//                        improvedCount++;
//                    }
//
//                    // 恢复原始位置
//                    p.gene_OS = origOS;
//                    p.gene_MS = origMS;
//                }
//                if (improvedCount > 0) {
//                    printOrWrite(String.format("  [N1-N5搜索] %d个pbest得到改进", improvedCount));
//                }
//            }
//
//            // 策略3：每20代对前20%粒子进行深度N1-N5搜索（20次迭代）
//            if (currentIteration % 20 == 0) {
//                Particle[] sortedSwarm = swarm.clone();
//                java.util.Arrays.sort(sortedSwarm, (p1, p2) -> Double.compare(p2.fitness, p1.fitness));
//                int deepSearchCount = Math.max(1, (int)(swarm.length * 0.2));
//                int improvedCount = 0;
//
//                for (int i = 0; i < deepSearchCount; i++) {
//                    if (sortedSwarm[i].localSearchWithN1N5(input, caculateFitness, operationMatrix, chromOps, 20)) {
//                        evaluateParticle(sortedSwarm[i]);
//                        improvedCount++;
//                    }
//                }
//                if (improvedCount > 0) {
//                    printOrWrite(String.format("  [深度N1-N5搜索] 对前%d个粒子深度搜索，%d个得到改进", deepSearchCount, improvedCount));
//                }
//            }
            // ====================================
            
            // 记录历史
            double currentMakespan = FITNESS_SCALE / gBest.fitness;
            bestMakespanHistory.add(currentMakespan);
            
            // 每10代输出一次
            if (currentIteration % 10 == 0) {
                double improvement = ((previousBestMakespan - currentMakespan) / previousBestMakespan) * 100.0;
                printOrWrite(String.format("代数 %4d | 最优 Makespan: %.2f | 改进: %+.2f%% | 停滞: %d", 
                    currentIteration, currentMakespan, improvement, noImprove));
                previousBestMakespan = currentMakespan;
            }
        }
        
        long endTime = System.currentTimeMillis();
        double totalTime = (endTime - startTime) / 1000.0;
        
        printOrWrite("\n================================================================================");
        printOrWrite("                        PSO算法运行完成");
        printOrWrite("================================================================================");
        printOrWrite(String.format("总迭代次数: %d", currentIteration));
        printOrWrite(String.format("总运行时间: %.2f 秒", totalTime));
        printOrWrite(String.format("最优 Makespan: %.2f", FITNESS_SCALE / gBest.fitness));
        printOrWrite("================================================================================\n");
        
        // 输出最终结果
        if (resultWriter != null) {
            resultWriter.writeFinalResults(
                FITNESS_SCALE / gBest.fitness,
                endTime - startTime,
                currentIteration,
                bestMakespanHistory,
                gBest.toChromosome(),
                operationMatrix
            );
        }
        
        // 构建Solution对象
        Solution solution = new Solution();
        solution.cost = FITNESS_SCALE / gBest.fitness;
        return solution;
    }
    
    /**
     * 初始化粒子群（直接从随机连续向量初始化 - Random Key方法）
     * 
     * 初始化流程：
     * 1. 直接随机初始化连续位置向量（Random Key）
     * 2. 通过解码得到离散染色体
     * 3. 不依赖于离散初始化策略（更纯粹的连续PSO）
     */
    private Particle[] initializeSwarm() {
        Particle[] swarm = new Particle[swarmSize];
        
        printOrWrite("  使用Random Key方法初始化连续位置向量...");
        
        // 直接从随机连续向量初始化每个粒子
        for (int i = 0; i < swarmSize; i++) {
            swarm[i] = new Particle(input, r);  // 新的构造函数：直接Random Key初始化
        }
        
        printOrWrite("  粒子群初始化完成！");
        
        return swarm;
    }
    
    /**
     * 评估粒子适应度
     */
    private void evaluateParticle(Particle particle) {
        double makespan = caculateFitness.evaluate(particle.toChromosome(), input, operationMatrix);
        particle.fitness = FITNESS_SCALE / makespan;
        particle.printSolution = particle.toChromosome().printSolution;
    }
    
    /**
     * 找到粒子群中的最优粒子
     */
    private Particle findBestParticle(Particle[] swarm) {
        Particle best = swarm[0];
        for (Particle p : swarm) {
            if (p.fitness > best.fitness) {
                best = p;
            }
        }
        return new Particle(best);
    }
    
    /**
     * 更新速度（连续PSO标准公式 - 两阶段分离）
     * 
     * v = w*v + c1*r1*(pBest - x) + c2*r2*(gBest - x)
     * 
     * 在连续空间[0,1]^n中进行速度更新，打印段和离散段分别处理
     */
    private void updateVelocity(Particle particle) {
        // 动态惯性权重（随迭代次数递减）
        double w_current = w_max - (w_max - w_min) * currentIteration / maxIterations;
        
        // 速度限制
        double v_max = 1.4;
        
        //===== 打印段速度更新 =====
        int printLength = particle.position_OS_print_continuous.length;
        
        // OS打印段
        for (int i = 0; i < printLength; i++) {
            double r1 = r.nextDouble();
            double r2 = r.nextDouble();
            
            particle.velocity_OS_print_continuous[i] = 
                w_current * particle.velocity_OS_print_continuous[i] +
                c1 * r1 * (particle.pBest_OS_print_continuous[i] - particle.position_OS_print_continuous[i]) +
                c2 * r2 * (gBest.pBest_OS_print_continuous[i] - particle.position_OS_print_continuous[i]);
            
            // 速度限制
            particle.velocity_OS_print_continuous[i] = Math.max(-v_max * 0.5,
                Math.min(v_max * 0.5, particle.velocity_OS_print_continuous[i]));
        }
        
        // MS打印段
        for (int i = 0; i < printLength; i++) {
            double r1 = r.nextDouble();
            double r2 = r.nextDouble();
            
            particle.velocity_MS_print_continuous[i] = 
                w_current * particle.velocity_MS_print_continuous[i] +
                c1 * r1 * (particle.pBest_MS_print_continuous[i] - particle.position_MS_print_continuous[i]) +
                c2 * r2 * (gBest.pBest_MS_print_continuous[i] - particle.position_MS_print_continuous[i]);
            
            // 速度限制
            particle.velocity_MS_print_continuous[i] = Math.max(-v_max, 
                Math.min(v_max, particle.velocity_MS_print_continuous[i]));
        }
        
        //===== 离散段速度更新 =====
        int discreteLength = particle.position_OS_discrete_continuous.length;
        
        // OS离散段
        for (int i = 0; i < discreteLength; i++) {
            double r1 = r.nextDouble();
            double r2 = r.nextDouble();
            
            particle.velocity_OS_discrete_continuous[i] = 
                w_current * particle.velocity_OS_discrete_continuous[i] +
                c1 * r1 * (particle.pBest_OS_discrete_continuous[i] - particle.position_OS_discrete_continuous[i]) +
                c2 * r2 * (gBest.pBest_OS_discrete_continuous[i] - particle.position_OS_discrete_continuous[i]);
            
            // 速度限制
            particle.velocity_OS_discrete_continuous[i] = Math.max(-v_max * 0.5,
                Math.min(v_max * 0.5, particle.velocity_OS_discrete_continuous[i]));
        }
        
        // MS离散段
        for (int i = 0; i < discreteLength; i++) {
            double r1 = r.nextDouble();
            double r2 = r.nextDouble();
            
            particle.velocity_MS_discrete_continuous[i] = 
                w_current * particle.velocity_MS_discrete_continuous[i] +
                c1 * r1 * (particle.pBest_MS_discrete_continuous[i] - particle.position_MS_discrete_continuous[i]) +
                c2 * r2 * (gBest.pBest_MS_discrete_continuous[i] - particle.position_MS_discrete_continuous[i]);
            
            // 速度限制
            particle.velocity_MS_discrete_continuous[i] = Math.max(-v_max, 
                Math.min(v_max, particle.velocity_MS_discrete_continuous[i]));
        }
    }
    
    /**
     * 更新粒子位置 - 连续PSO标准更新（两阶段分离）
     * 
     * x = x + v
     * 
     * 然后通过映射机制将连续位置转换为离散调度解
     * 打印段和离散段分别处理
     */
    private void updatePosition(Particle particle) {
        //===== 打印段位置更新 =====
        int printLength = particle.position_OS_print_continuous.length;
        
        for (int i = 0; i < printLength; i++) {
            // x = x + v
            particle.position_OS_print_continuous[i] += particle.velocity_OS_print_continuous[i];
            particle.position_MS_print_continuous[i] += particle.velocity_MS_print_continuous[i];
            
            // 边界处理：截断法（Clamping）
            particle.position_OS_print_continuous[i] = Math.max(-4, Math.min(4.0, particle.position_OS_print_continuous[i]));
            particle.position_MS_print_continuous[i] = Math.max(-6, Math.min(6.0, particle.position_MS_print_continuous[i]));
        }
        
        //===== 离散段位置更新 =====
        int discreteLength = particle.position_OS_discrete_continuous.length;
        
        for (int i = 0; i < discreteLength; i++) {
            // x = x + v
            particle.position_OS_discrete_continuous[i] += particle.velocity_OS_discrete_continuous[i];
            particle.position_MS_discrete_continuous[i] += particle.velocity_MS_discrete_continuous[i];
            
            // 边界处理：截断法（Clamping）
            particle.position_OS_discrete_continuous[i] = Math.max(-4, Math.min(4, particle.position_OS_discrete_continuous[i]));
            particle.position_MS_discrete_continuous[i] = Math.max(-6, Math.min(6, particle.position_MS_discrete_continuous[i]));
        }
        
        //===== 映射到离散空间（Random Key方法 + 规则映射） =====
        particle.continuousToDiscrete_OS(input);
        particle.continuousToDiscrete_MS(input);
        
        // 清空缓存
        particle.printSolution = null;
    }

    
    /**
     * 打印段序列局部扰动（小范围交换）
     */
    private void mutatePrintSequenceLocal(Particle particle) {
        int jobCount = input.getJobCount();
        if (jobCount <= 1) return;
        
        // 随机选择一个位置，与其邻近位置交换
        int pos = r.nextInt(jobCount);
        int neighbor = pos + (r.nextBoolean() ? 1 : -1);
        
        if (neighbor >= 0 && neighbor < jobCount) {
            // 交换OS
            int tempOS = particle.gene_OS[pos];
            particle.gene_OS[pos] = particle.gene_OS[neighbor];
            particle.gene_OS[neighbor] = tempOS;
            
            // 交换MS
            int tempMS = particle.gene_MS[pos];
            particle.gene_MS[pos] = particle.gene_MS[neighbor];
            particle.gene_MS[neighbor] = tempMS;
        }
    }
    
    /**
     * 离散段序列局部扰动（小范围交换）
     */
    private void mutateDiscreteSequenceLocal(Particle particle) {
        int jobCount = input.getJobCount();
        int totalLength = particle.gene_OS.length;
        
        if (totalLength <= jobCount + 1) return;
        
        // 随机选择离散段的一个位置，与其邻近位置交换
        int pos = jobCount + r.nextInt(totalLength - jobCount);
        int neighbor = pos + (r.nextBoolean() ? 1 : -1);
        
        // 确保neighbor在离散段范围内
        if (neighbor >= jobCount && neighbor < totalLength) {
            // 交换OS
            int tempOS = particle.gene_OS[pos];
            particle.gene_OS[pos] = particle.gene_OS[neighbor];
            particle.gene_OS[neighbor] = tempOS;
            
            // 不交换MS，因为MS与工序绑定
            // 但需要验证交换后的MS是否仍然有效
            repairDiscreteMachineAssignment(particle, Math.min(pos, neighbor), Math.max(pos, neighbor));
        }
    }
    
    
    /**
     * 修复离散段的机器分配（确保符合工序顺序约束）
     * 
     * 在交换或变异后调用，确保每个工序的机器分配仍然有效
     */
    private void repairDiscreteMachineAssignment(Particle particle, int start, int end) {
        int jobCount = input.getJobCount();
        int[] operationCountArr = input.getOperationCountArr();
        int[][] operationToIndex = input.getOperationToIndex();
        double[][] proDesMatrix = input.getProDesMatrix();
        
        // 统计每个工件在离散段中已出现的次数
        int[] jobOccurrence = new int[jobCount];
        
        for (int i = jobCount; i <= end; i++) {
            int jobNo = particle.gene_OS[i];
            jobOccurrence[jobNo]++;
            
            // 计算工序编号（打印=0，批处理=1，离散加工从2开始）
            int operNo = jobOccurrence[jobNo] + 1;  // +1 因为0是打印，1是批处理
            
            // 检查工序编号是否有效
            if (operNo < operationToIndex[jobNo].length) {
                int operIdx = operationToIndex[jobNo][operNo];
                int currentMachine = particle.gene_MS[i];
                
                // 检查当前机器是否有效
                if (currentMachine < 1 || currentMachine > proDesMatrix[operIdx].length ||
                    proDesMatrix[operIdx][currentMachine - 1] == 0 ||
                    proDesMatrix[operIdx][currentMachine - 1] == Double.MAX_VALUE) {
                    
                    // 当前机器无效，重新选择一个有效机器
                    for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                        if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                            particle.gene_MS[i] = k + 1;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 变异策略3: 打印段机器重分配
     * 选择一个零件，重新分配到另一台可用的打印机
     */
    private void mutatePrintMachine(Particle particle) {
        int jobCount = input.getJobCount();
        int printMachineCount = input.getPrintMachineCount();
        
        if (jobCount == 0 || printMachineCount <= 1) return;
        
        // 随机选择一个打印工件
        int pos = r.nextInt(jobCount);
        int jobNo = particle.gene_OS[pos];
        
        // 获取该零件的尺寸
        Item[] items = input.getItems();
        double l = items[jobNo].l;
        double w = items[jobNo].w;
        
        // 找到所有可以容纳该零件的打印机
        List<Integer> availablePrinters = new ArrayList<>();
        Machine[] machines = input.getMachines();
        
        // 只考虑打印机（前printMachineCount台）
        for (int i = 0; i < printMachineCount; i++) {
            PrintMachine pm = (PrintMachine) machines[i];
            if (l <= pm.L && w <= pm.W) {
                availablePrinters.add(i + 1);  // 机器编号从1开始
            }
        }
        
        // 如果有多台可用打印机，随机选择一台不同于当前的
        if (availablePrinters.size() > 1) {
            int currentMachine = particle.gene_MS[pos];
            availablePrinters.removeIf(m -> m == currentMachine);
            
            if (!availablePrinters.isEmpty()) {
                particle.gene_MS[pos] = availablePrinters.get(r.nextInt(availablePrinters.size()));
            }
        }
    }
    
    /**
     * 变异策略4: 离散段机器重分配
     * 选择一个离散工序，重新分配到其候选机器集中的另一台
     */
    private void mutateDiscreteMachine(Particle particle) {
        int jobCount = input.getJobCount();
        int totalLength = particle.gene_OS.length;
        
        if (totalLength <= jobCount) return;
        
        // 随机选择一个离散工序位置
        int pos = jobCount + r.nextInt(totalLength - jobCount);
        int jobNo = particle.gene_OS[pos];
        
        // 计算该工序的编号
        int[] operationCountArr = input.getOperationCountArr();
        int[][] operationToIndex = input.getOperationToIndex();
        double[][] proDesMatrix = input.getProDesMatrix();
        
        int jobOccurrence = 0;
        for (int i = jobCount; i <= pos; i++) {
            if (particle.gene_OS[i] == jobNo) {
                jobOccurrence++;
            }
        }
        
        int operNo = jobOccurrence + 1;  // +1 因为0是打印，1是批处理
        
        // 检查工序编号是否有效
        if (operNo >= operationToIndex[jobNo].length) return;
        
        int operIdx = operationToIndex[jobNo][operNo];
        
        // 找到所有可用机器
        List<Integer> availableMachines = new ArrayList<>();
        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                availableMachines.add(k + 1);
            }
        }
        
        // 如果有多台可用机器，随机选择一台不同于当前的
        if (availableMachines.size() > 1) {
            int currentMachine = particle.gene_MS[pos];
            availableMachines.removeIf(m -> m == currentMachine);
            
            if (!availableMachines.isEmpty()) {
                particle.gene_MS[pos] = availableMachines.get(r.nextInt(availableMachines.size()));
            }
        }
    }
}
