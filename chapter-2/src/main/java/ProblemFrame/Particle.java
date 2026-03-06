package ProblemFrame;

import ProgramEntity.Job;
import ProgramEntity.Problem;
import ProgramEntity.Solution;

import java.util.*;

/**
 * PSO粒子 - 表示一个调度解
 * 
 * 编码方式支持两种模式：
 * 
 * 模式1：离散编码（当前实现）
 * - gene_OS: 工序序列（Operation Sequence）
 * - gene_MS: 机器分配序列（Machine Selection）
 * - velocity: 交换操作和机器变更
 * 
 * 模式2：连续编码（基于优先级）
 * - position_OS: 连续优先级向量 [0, 1] → 通过排序映射为离散序列
 * - position_MS: 连续机器优先级 [0, 1] → 通过规则映射为机器编号
 * - velocity_continuous: 连续速度向量
 * 
 * PSO特有属性：
 * - pBest: 个体历史最优位置
 * - pBestFitness: 个体历史最优适应度
 */
public class Particle {
    // ========== 离散编码（原方案） ==========
    // 位置（当前解 - 离散）
    public int[] gene_OS;           // 工序序列
    public int[] gene_MS;           // 机器分配
    public double fitness;           // 当前适应度
    public List<Solution>[] printSolution;   // 打印阶段的解（缓存）
    
    // 速度（变化方向 - 离散）
    public List<SwapOperation> velocity_OS;    // 工序序列的速度
    public List<MachineChange> velocity_MS;    // 机器分配的速度
    
    // 个体最优（离散）
    public int[] pBest_OS;          // 个体历史最优工序序列
    public int[] pBest_MS;          // 个体历史最优机器分配
    public double pBestFitness;     // 个体历史最优适应度
    
    // ========== 连续编码（新方案 - 两阶段分离） ==========
    // 打印阶段：OS和MS对应
    public double[] position_OS_print_continuous;       // 打印段OS连续向量 [0,1]^jobCount
    public double[] position_MS_print_continuous;       // 打印段MS连续向量 [0,1]^jobCount
    public double[] velocity_OS_print_continuous;       // 打印段OS速度
    public double[] velocity_MS_print_continuous;       // 打印段MS速度
    
    // 离散处理阶段：OS可变，MS固定结构
    public double[] position_OS_discrete_continuous;    // 离散段OS连续向量 [0,1]^discreteOpsCount
    public double[] position_MS_discrete_continuous;    // 离散段MS连续向量 [0,1]^discreteOpsCount（固定结构）
    public double[] velocity_OS_discrete_continuous;    // 离散段OS速度
    public double[] velocity_MS_discrete_continuous;    // 离散段MS速度
    
    // 个体最优（连续）- 分阶段
    public double[] pBest_OS_print_continuous;
    public double[] pBest_MS_print_continuous;
    public double[] pBest_OS_discrete_continuous;
    public double[] pBest_MS_discrete_continuous;
    
    // 编码模式标志
    public boolean useContinuousEncoding = false;  // true=连续编码, false=离散编码
    
    private Random r;
    
    /**
     * 交换操作 - 用于表示工序序列的速度
     */
    public static class SwapOperation {
        public int pos1;
        public int pos2;
        
        public SwapOperation(int pos1, int pos2) {
            this.pos1 = pos1;
            this.pos2 = pos2;
        }
    }
    
    /**
     * 机器变更 - 用于表示机器分配的速度
     */
    public static class MachineChange {
        public int position;      // 位置
        public int newMachine;    // 新机器编号
        
        public MachineChange(int position, int newMachine) {
            this.position = position;
            this.newMachine = newMachine;
        }
    }
    
    /**
     * 从染色体创建粒子（支持连续/离散两种模式）
     * 
     * @param useContinuous true=使用连续编码, false=使用离散编码
     * @param problem 问题实例（连续编码需要，用于获取jobCount等信息）
     */
    public Particle(Chromosome chromosome, Random r, boolean useContinuous, Problem problem) {
        this.r = r;
        this.useContinuousEncoding = useContinuous;
        
        // 离散编码（总是需要，用于适应度评估）
        this.gene_OS = chromosome.gene_OS.clone();
        this.gene_MS = chromosome.gene_MS.clone();
        this.fitness = chromosome.fitness;
        this.printSolution = chromosome.printSolution;
        
        if (useContinuous) {
            // ========== 连续编码模式（两阶段分离） ==========
            int jobCount = problem.getJobCount();
            int totalLength = chromosome.gene_OS.length;
            int discreteLength = totalLength - jobCount;
            
            // 打印段：从离散编码生成连续位置
            this.position_OS_print_continuous = new double[jobCount];
            this.position_MS_print_continuous = new double[jobCount];
            for (int i = 0; i < jobCount; i++) {
                this.position_OS_print_continuous[i] = (i + r.nextDouble()) / jobCount;
                this.position_MS_print_continuous[i] = (i + r.nextDouble()) / jobCount;
            }
            
            // 离散段：从离散编码生成连续位置
            this.position_OS_discrete_continuous = new double[discreteLength];
            this.position_MS_discrete_continuous = new double[discreteLength];
            for (int i = 0; i < discreteLength; i++) {
                this.position_OS_discrete_continuous[i] = (i + r.nextDouble()) / discreteLength;
                this.position_MS_discrete_continuous[i] = r.nextDouble();  // [0,1]均匀分布
            }
            
            // 初始化连续速度为0
            this.velocity_OS_print_continuous = new double[jobCount];
            this.velocity_MS_print_continuous = new double[jobCount];
            this.velocity_OS_discrete_continuous = new double[discreteLength];
            this.velocity_MS_discrete_continuous = new double[discreteLength];
            
            // 初始化个体最优（连续）
            this.pBest_OS_print_continuous = this.position_OS_print_continuous.clone();
            this.pBest_MS_print_continuous = this.position_MS_print_continuous.clone();
            this.pBest_OS_discrete_continuous = this.position_OS_discrete_continuous.clone();
            this.pBest_MS_discrete_continuous = this.position_MS_discrete_continuous.clone();
        } else {
            // ========== 离散编码模式 ==========
            // 初始化离散速度为空
            this.velocity_OS = new ArrayList<>();
            this.velocity_MS = new ArrayList<>();
            
            // 初始化个体最优（离散）
            this.pBest_OS = chromosome.gene_OS.clone();
            this.pBest_MS = chromosome.gene_MS.clone();
        }
        
        this.pBestFitness = chromosome.fitness;
    }
    
    /**
     * 直接从随机连续向量初始化粒子（纯PSO初始化，不依赖Chromosome）
     * 
     * 初始化流程：
     * 1. 随机初始化连续位置向量（Random Key）
     * 2. 通过解码得到离散染色体（gene_OS, gene_MS）
     * 3. 初始化速度为0
     * 4. 初始化pbest
     * 
     * @param problem 问题实例
     * @param r 随机数生成器
     */
    public Particle(Problem problem, Random r) {
        this.r = r;
        this.useContinuousEncoding = true;  // 使用连续编码
        
        int jobCount = problem.getJobCount();
        int totalLength = 0;
        
        // 计算gene_OS的总长度：打印工序数 + 离散工序数
        int[] operationCountArr = problem.getOperationCountArr();
        for (int i = 0; i < jobCount; i++) {
            totalLength += (operationCountArr[i] - 1);  // 减去批处理工序
        }
        
        int discreteLength = totalLength - jobCount;
        
        // ========== 初始化连续位置向量（Random Key） ==========
        
        // 打印段：随机初始化到扩展边界[-4, 4]
        this.position_OS_print_continuous = new double[jobCount];
        this.position_MS_print_continuous = new double[jobCount];
        for (int i = 0; i < jobCount; i++) {
            // Random Key for OS: 随机值，用于排序
            this.position_OS_print_continuous[i] = -4 + r.nextDouble() * 8;  // [-4, 4]
            // Random Key for MS: 随机值，用于机器选择
            this.position_MS_print_continuous[i] = -6 + r.nextDouble() * 12;  // [-6, 6]
        }
        
        // 离散段：随机初始化到扩展边界[-4, 4]
        this.position_OS_discrete_continuous = new double[discreteLength];
        this.position_MS_discrete_continuous = new double[discreteLength];
        for (int i = 0; i < discreteLength; i++) {
            this.position_OS_discrete_continuous[i] = -4 + r.nextDouble() * 8;  // [-4, 4]
            this.position_MS_discrete_continuous[i] = -6 + r.nextDouble() * 12;  // [-6, 6]
        }
        
        // ========== 初始化离散染色体（用于解码） ==========
        
        // 先创建初始的gene_OS（按工件顺序）
        this.gene_OS = new int[totalLength];
        int pos = 0;
        // 打印段
        for (int i = 0; i < jobCount; i++) {
            gene_OS[pos++] = i;
        }
        // 离散段
        for (int i = 0; i < jobCount; i++) {
            int discreteOpsCount = operationCountArr[i] - 2;  // 减去打印和批处理
            for (int j = 0; j < discreteOpsCount; j++) {
                gene_OS[pos++] = i;
            }
        }
        
        // 初始化gene_MS（临时值，会被解码覆盖）
        this.gene_MS = new int[totalLength];
        Arrays.fill(gene_MS, 1);
        
        // ========== 通过解码得到实际的gene_OS和gene_MS ==========
        continuousToDiscrete_OS(problem);
        continuousToDiscrete_MS(problem);
        
        // ========== 初始化速度为0 ==========
        this.velocity_OS_print_continuous = new double[jobCount];
        this.velocity_MS_print_continuous = new double[jobCount];
        this.velocity_OS_discrete_continuous = new double[discreteLength];
        this.velocity_MS_discrete_continuous = new double[discreteLength];
        
        // ========== 初始化个体最优（连续） ==========
        this.pBest_OS_print_continuous = this.position_OS_print_continuous.clone();
        this.pBest_MS_print_continuous = this.position_MS_print_continuous.clone();
        this.pBest_OS_discrete_continuous = this.position_OS_discrete_continuous.clone();
        this.pBest_MS_discrete_continuous = this.position_MS_discrete_continuous.clone();
        
        // 初始化适应度（需要后续evaluate）
        this.fitness = 0;
        this.pBestFitness = 0;
        this.printSolution = null;
    }
    
    /**
     * 兼容旧接口：从染色体创建粒子（需要Problem用于连续编码）
     */
    public Particle(Chromosome chromosome, Random r, boolean useContinuous) {
        this(chromosome, r, useContinuous, null);
    }
    
    /**
     * 兼容旧接口：从染色体创建粒子（默认离散编码）
     */
    public Particle(Chromosome chromosome, Random r) {
        this(chromosome, r, false, null);  // 默认使用离散编码
    }
    
    /**
     * 将连续位置向量映射为离散OS序列（两阶段分离）
     * 
     * Random Key方法 + Sigmoid归一化：
     * - 打印段：sigmoid归一化 → 排序 → gene_OS[0:jobCount]
     * - 离散段：sigmoid归一化 → 排序 → gene_OS[jobCount:end]
     * 
     * ⚠️ 虽然排序不要求[0,1]范围，但归一化可以提高更新有效性：
     *    扩展边界[-4,4]中的小变化，归一化后在[0,1]中可能改变排序结果
     */
    public void continuousToDiscrete_OS(Problem problem) {
        int jobCount = problem.getJobCount();
        int totalLength = gene_OS.length;
        
        //===== 打印段：Sigmoid归一化 + Random Key排序 =====
        class IndexPriority {
            int index;
            double priority;
            IndexPriority(int i, double p) { index = i; priority = p; }
        }
        
        // 打印段排序（使用归一化后的值）
        List<IndexPriority> printPairs = new ArrayList<>();
        int[] originalPrintOS = new int[jobCount];
        for (int i = 0; i < jobCount; i++) {
            originalPrintOS[i] = gene_OS[i];
            // ⚠️ 使用sigmoid归一化，将扩展边界的值映射到(0,1)，提高更新有效性
            double normalizedValue = sigmoid(position_OS_print_continuous[i]);
            printPairs.add(new IndexPriority(i, normalizedValue));
        }
        printPairs.sort((a, b) -> Double.compare(a.priority, b.priority));
        
        // 应用排序到gene_OS打印段
        for (int i = 0; i < jobCount; i++) {
            gene_OS[i] = originalPrintOS[printPairs.get(i).index];
        }
        
        //===== 离散段：Sigmoid归一化 + Random Key排序 =====
        int discreteLength = totalLength - jobCount;
        List<IndexPriority> discretePairs = new ArrayList<>();
        int[] originalDiscreteOS = new int[discreteLength];
        for (int i = 0; i < discreteLength; i++) {
            originalDiscreteOS[i] = gene_OS[jobCount + i];
            // ⚠️ 使用sigmoid归一化，将扩展边界的值映射到(0,1)，提高更新有效性
            double normalizedValue = sigmoid(position_OS_discrete_continuous[i]);
            discretePairs.add(new IndexPriority(i, normalizedValue));
        }
        discretePairs.sort((a, b) -> Double.compare(a.priority, b.priority));
        
        // 应用排序到gene_OS离散段
        for (int i = 0; i < discreteLength; i++) {
            gene_OS[jobCount + i] = originalDiscreteOS[discretePairs.get(i).index];
        }
    }
    
    /**
     * 将连续位置向量映射为离散MS序列（两阶段分离）
     * 
     * ⚠️ 关键：打印段和离散段的MS编码方式完全不同！
     * 
     * 打印段：position_MS_print[i] → gene_MS[i]（绝对打印机编号）
     *         与gene_OS[i]对应，随OS变化
     * 
     * 离散段：position_MS_discrete[固定位置] → gene_MS[固定位置]（相对机器索引）
     *         固定结构：工件0的所有离散工序，工件1的所有离散工序，...
     *         不随OS变化！
     */
    public void continuousToDiscrete_MS(ProgramEntity.Problem problem) {
        int jobCount = problem.getJobCount();
        int printMachineCount = problem.getPrintMachineCount();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        int[] operationCountArr = problem.getOperationCountArr();
        ProgramEntity.Item[] items = problem.getItems();
        ProgramEntity.Machine.Machine[] machines = problem.getMachines();
        
        //===== 打印段：与OS对应，映射到绝对打印机编号 =====
        for (int i = 0; i < jobCount; i++) {
            int jobNo = gene_OS[i];  // ✅ 从OS读取工件编号
            double continuousValue = position_MS_print_continuous[i];
            
            // ⚠️ 归一化到[0, 1]范围（使用sigmoid函数，处理任意范围的值）
            double normalizedValue = sigmoid(continuousValue);
            
            // 找到所有可以容纳该零件的打印机
            List<Integer> availablePrinters = new ArrayList<>();
            double l = items[jobNo].l;
            double w = items[jobNo].w;
            
            for (int m = 0; m < printMachineCount; m++) {
                ProgramEntity.Machine.PrintMachine pm = 
                    (ProgramEntity.Machine.PrintMachine) machines[m];
                if (l <= pm.L && w <= pm.W) {
                    availablePrinters.add(m + 1);  // 绝对机器编号（1-based）
                }
            }
            
            // 使用归一化后的连续值选择打印机（绝对编号）
            if (!availablePrinters.isEmpty()) {
                int idx = (int) (normalizedValue * availablePrinters.size());
                idx = Math.max(0, Math.min(idx, availablePrinters.size() - 1));  // 确保索引有效
                gene_MS[i] = availablePrinters.get(idx);  // 存储绝对打印机编号
            } else {
                gene_MS[i] = 1;  // 默认第一台
            }
        }
        
        //===== 离散段：固定结构，按工件顺序，映射到相对机器索引 =====
        int msIndex = jobCount;  // MS离散段起点
        
        // ⚠️ 关键：按工件编号顺序遍历（不从OS读取！）
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = operationCountArr[jobNo] - 2;  // 减去打印和批处理
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;  // 实际工序编号（工序2开始）
                int operIdx = operationToIndex[jobNo][operNo];
                
                // 获取连续值（注意：从position_MS_discrete_continuous读取）
                int discreteIndex = msIndex - jobCount;
                double continuousValue = position_MS_discrete_continuous[discreteIndex];
                
                // ⚠️ 归一化到[0, 1]范围（使用sigmoid函数）
                double normalizedValue = sigmoid(continuousValue);
                
                // 找到该工序的所有可用机器
                List<Integer> availableMachines = new ArrayList<>();
                for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
                    if (proDesMatrix[operIdx][m] > 0 && 
                        proDesMatrix[operIdx][m] != Double.MAX_VALUE) {
                        availableMachines.add(m + 1);  // 绝对机器编号
                    }
                }
                
                // 使用归一化后的连续值选择机器（相对索引）
                if (!availableMachines.isEmpty()) {
                    // normalizedValue ∈ [0, 1]，映射到 [1, size]
                    int relativeIndex = (int) (normalizedValue * availableMachines.size()) + 1;
                    //relativeIndex = Math.max(, Math.min(relativeIndex, availableMachines.size()));  // 确保在[1, size]范围内
                    gene_MS[msIndex] = relativeIndex;  // ⚠️ 存储相对索引（1-based）
                } else {
                    gene_MS[msIndex] = 1;  // 默认第一个
                }
                
                msIndex++;
            }
        }
    }
    
    /**
     * Sigmoid函数：将任意实数映射到(0, 1)范围
     * 用于处理扩展边界后的连续位置向量
     * 
     * @param x 输入值（可以是任意实数）
     * @return 映射后的值，范围在(0, 1)
     */
    private double sigmoid(double x) {
        return 1.0 / (1.0 + Math.exp(-x));
    }
    
    /**
     * 获取指定位置实际使用的机器编号（用于调试和分析）
     * 
     * @param pos gene_MS中的位置
     * @param problem 问题实例
     * @return 实际机器的绝对编号
     */
    public int getActualMachineNumber(int pos, ProgramEntity.Problem problem) {
        int jobCount = problem.getJobCount();
        
        // 打印段：gene_MS直接存储绝对机器编号
        if (pos < jobCount) {
            return gene_MS[pos];
        }
        
        // 离散段：需要解码相对索引
        int relativeIndex = gene_MS[pos];
        
        // 找到该位置对应的工序
        int[][] operationToIndex = problem.getOperationToIndex();
        int[] operationCountArr = problem.getOperationCountArr();
        double[][] proDesMatrix = problem.getProDesMatrix();
        
        int currentPos = jobCount;
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = operationCountArr[jobNo] - 2;
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                if (currentPos == pos) {
                    int operNo = 2 + localOperNo;
                    int operIdx = operationToIndex[jobNo][operNo];
                    
                    // 获取可用机器列表
                    List<Integer> availableMachines = new ArrayList<>();
                    for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
                        if (proDesMatrix[operIdx][m] > 0 && 
                            proDesMatrix[operIdx][m] != Double.MAX_VALUE) {
                            availableMachines.add(m + 1);
                        }
                    }
                    
                    // 返回相对索引对应的绝对机器编号
                    if (relativeIndex >= 1 && relativeIndex <= availableMachines.size()) {
                        return availableMachines.get(relativeIndex - 1);
                    }
                }
                currentPos++;
            }
        }
        
        return -1;  // 未找到
    }
    
    /**
     * 打印机器映射详情（用于调试）
     */
    public void printMachineMapping(ProgramEntity.Problem problem) {
        int jobCount = problem.getJobCount();
        
        System.out.println("\n========== 机器映射详情 ==========");
        
        // 打印段
        System.out.println("\n【打印段】:");
        for (int i = 0; i < jobCount; i++) {
            int jobNo = gene_OS[i];
            double continuousValue = position_MS_print_continuous[i];
            double normalizedValue = sigmoid(continuousValue);
            int machineNo = gene_MS[i];
            
            System.out.printf("  位置%d (工件%d): continuous=%.3f → sigmoid=%.3f → 打印机%d\n",
                i, jobNo, continuousValue, normalizedValue, machineNo);
        }
        
        // 离散段
        System.out.println("\n【离散段】:");
        for (int i = jobCount; i < gene_MS.length; i++) {
            int discreteIndex = i - jobCount;
            double continuousValue = position_MS_discrete_continuous[discreteIndex];
            double normalizedValue = sigmoid(continuousValue);
            int relativeIndex = gene_MS[i];
            int actualMachine = getActualMachineNumber(i, problem);
            
            System.out.printf("  位置%d: continuous=%.3f → sigmoid=%.3f → 相对索引%d → 机器%d\n",
                i, continuousValue, normalizedValue, relativeIndex, actualMachine);
        }
        
        System.out.println("================================\n");
    }
    /**
     * 复制构造函数
     */
    public Particle(Particle p) {
        this.r = p.r;
        this.useContinuousEncoding = p.useContinuousEncoding;
        
        // 复制离散编码（总是需要）
        this.gene_OS = p.gene_OS.clone();
        this.gene_MS = p.gene_MS.clone();
        this.fitness = p.fitness;
        this.printSolution = p.printSolution;
        
        if (useContinuousEncoding) {
            // 复制连续编码（两阶段分离）
            this.position_OS_print_continuous = p.position_OS_print_continuous != null ? p.position_OS_print_continuous.clone() : null;
            this.position_MS_print_continuous = p.position_MS_print_continuous != null ? p.position_MS_print_continuous.clone() : null;
            this.velocity_OS_print_continuous = p.velocity_OS_print_continuous != null ? p.velocity_OS_print_continuous.clone() : null;
            this.velocity_MS_print_continuous = p.velocity_MS_print_continuous != null ? p.velocity_MS_print_continuous.clone() : null;
            
            this.position_OS_discrete_continuous = p.position_OS_discrete_continuous != null ? p.position_OS_discrete_continuous.clone() : null;
            this.position_MS_discrete_continuous = p.position_MS_discrete_continuous != null ? p.position_MS_discrete_continuous.clone() : null;
            this.velocity_OS_discrete_continuous = p.velocity_OS_discrete_continuous != null ? p.velocity_OS_discrete_continuous.clone() : null;
            this.velocity_MS_discrete_continuous = p.velocity_MS_discrete_continuous != null ? p.velocity_MS_discrete_continuous.clone() : null;
            
            this.pBest_OS_print_continuous = p.pBest_OS_print_continuous != null ? p.pBest_OS_print_continuous.clone() : null;
            this.pBest_MS_print_continuous = p.pBest_MS_print_continuous != null ? p.pBest_MS_print_continuous.clone() : null;
            this.pBest_OS_discrete_continuous = p.pBest_OS_discrete_continuous != null ? p.pBest_OS_discrete_continuous.clone() : null;
            this.pBest_MS_discrete_continuous = p.pBest_MS_discrete_continuous != null ? p.pBest_MS_discrete_continuous.clone() : null;
        } else {
            // 复制离散编码
            this.velocity_OS = p.velocity_OS != null ? new ArrayList<>(p.velocity_OS) : new ArrayList<>();
            this.velocity_MS = p.velocity_MS != null ? new ArrayList<>(p.velocity_MS) : new ArrayList<>();
            this.pBest_OS = p.pBest_OS != null ? p.pBest_OS.clone() : null;
            this.pBest_MS = p.pBest_MS != null ? p.pBest_MS.clone() : null;
        }
        
        this.pBestFitness = p.pBestFitness;
    }
    
    /**
     * 更新个体最优
     */
    public void updatePBest() {
        if (this.fitness > this.pBestFitness) {
            if (useContinuousEncoding) {
                // 更新连续编码的个体最优（两阶段分离）
                this.pBest_OS_print_continuous = this.position_OS_print_continuous.clone();
                this.pBest_MS_print_continuous = this.position_MS_print_continuous.clone();
                this.pBest_OS_discrete_continuous = this.position_OS_discrete_continuous.clone();
                this.pBest_MS_discrete_continuous = this.position_MS_discrete_continuous.clone();
            } else {
                // 更新离散编码的个体最优
                this.pBest_OS = this.gene_OS.clone();
                this.pBest_MS = this.gene_MS.clone();
            }
            this.pBestFitness = this.fitness;
        }
    }
    
    /**
     * 转换为染色体（用于适应度评估）
     */
    public Chromosome toChromosome() {
        Chromosome c = new Chromosome(r);
        c.gene_OS = this.gene_OS.clone();
        c.gene_MS = this.gene_MS.clone();
        c.fitness = this.fitness;
        c.printSolution = this.printSolution;
        return c;
    }
    
    // ========== 邻域改进方法 ==========
    
    /**
     * 使用GA的局部搜索策略对粒子进行邻域改进
     * 包含 N1、N2、N3、N4、N5 五个邻域动作
     * 
     * @param problem 问题实例
     * @param caculateFitness 适应度计算器
     * @param operationMatrix 工序矩阵
     * @param chromOps 染色体操作类（包含N1-N5的实现）
     * @param maxIterations 最大迭代次数
     * @return 是否找到更优解
     */
    public boolean localSearchWithN1N5(Problem problem, CaculateFitness caculateFitness,
                                       ProgramEntity.Operation[][] operationMatrix,
                                       AlgorthmFrame.ga.ChromosomeOperation chromOps,
                                       int maxIterations) {
        // 保存原始基因
        int[] originalOS = gene_OS.clone();
        int[] originalMS = gene_MS.clone();
        double originalFitness = fitness;
        
        try {
            // 转换为染色体进行局部搜索
            Chromosome c = toChromosome();
            double beforeMakespan = caculateFitness.evaluate(c, problem, operationMatrix);
            c.fitness = 1000000.0 / beforeMakespan;  // FITNESS_SCALE
            
            // 使用GA的局部搜索方法
            chromOps.LocalSearch(c, maxIterations);
            
            double afterMakespan = 1000000.0 / c.fitness;  // FITNESS_SCALE
            
            // 如果找到更优解，更新粒子
            if (afterMakespan < beforeMakespan) {
                this.gene_OS = c.gene_OS.clone();
                this.gene_MS = c.gene_MS.clone();
                this.fitness = c.fitness;
                this.printSolution = c.printSolution;
                return true;
            } else {
                // 未改进，恢复原始解
                this.gene_OS = originalOS;
                this.gene_MS = originalMS;
                this.fitness = originalFitness;
                return false;
            }
            
        } catch (Exception e) {
            // 发生错误时恢复原始解
            this.gene_OS = originalOS;
            this.gene_MS = originalMS;
            this.fitness = originalFitness;
            System.err.println("localSearchWithN1N5发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * 对当前粒子进行OS邻域改进（swap/insert操作）
     * 重点改进同机器或关键路径相关的工序
     * 
     * @param problem 问题实例
     * @param caculateFitness 适应度计算器
     * @param operationMatrix 工序矩阵
     * @param improveProb 执行改进的概率
     * @param maxAttempts 最大尝试次数
     * @return 是否找到更优解
     */
    public boolean localSearchOS(Problem problem, CaculateFitness caculateFitness, 
                                  ProgramEntity.Operation[][] operationMatrix, 
                                  double improveProb, int maxAttempts) {
        if (r.nextDouble() > improveProb) {
            return false;
        }
        
        // 保存原始基因
        int[] originalOS = gene_OS.clone();
        int[] originalMS = gene_MS.clone();
        
        boolean improved = false;
        int[] bestOS = gene_OS.clone();
        int[] bestMS = gene_MS.clone();
        double bestMakespan = Double.MAX_VALUE;
        
        try {
            // 计算当前makespan
            double currentMakespan = caculateFitness.evaluate(toChromosome(), problem, operationMatrix);
            bestMakespan = currentMakespan;
            
            // 获取关键路径相关工序
            Set<Integer> criticalOperations = getCriticalPathOperations(problem, caculateFitness, operationMatrix);
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int[] newOS = bestOS.clone();  // 从当前最优解开始
                
                // 随机选择操作类型：swap或insert
                if (r.nextDouble() < 0.5) {
                    // Swap操作：交换两个位置
                    int pos1 = selectPositionForImprovement(newOS, criticalOperations, problem);
                    int pos2 = selectPositionForImprovement(newOS, criticalOperations, problem);
                    
                    if (pos1 >= 0 && pos1 < newOS.length && pos2 >= 0 && pos2 < newOS.length && pos1 != pos2) {
                        // 交换
                        int temp = newOS[pos1];
                        newOS[pos1] = newOS[pos2];
                        newOS[pos2] = temp;
                    } else {
                        continue;  // 跳过无效的操作
                    }
                } else {
                    // Insert操作：将一个位置的工序插入到另一个位置
                    int fromPos = selectPositionForImprovement(newOS, criticalOperations, problem);
                    int toPos = selectPositionForImprovement(newOS, criticalOperations, problem);
                    
                    if (fromPos >= 0 && fromPos < newOS.length && toPos >= 0 && toPos < newOS.length && fromPos != toPos) {
                        int job = newOS[fromPos];
                        // 移除fromPos位置
                        if (fromPos < toPos) {
                            System.arraycopy(newOS, fromPos + 1, newOS, fromPos, toPos - fromPos);
                            newOS[toPos] = job;
                        } else {
                            System.arraycopy(newOS, toPos, newOS, toPos + 1, fromPos - toPos);
                            newOS[toPos] = job;
                        }
                    } else {
                        continue;  // 跳过无效的操作
                    }
                }
                
                // 评估新解
                gene_OS = newOS;
                Chromosome tempChrom = toChromosome();
                double newMakespan = caculateFitness.evaluate(tempChrom, problem, operationMatrix);
                
                // 如果找到更优解（makespan更小），保存
                if (newMakespan < bestMakespan) {
                    bestOS = newOS.clone();
                    bestMS = gene_MS.clone();
                    bestMakespan = newMakespan;
                    improved = true;
                }
            }
            
            // 应用最优解
            if (improved) {
                this.gene_OS = bestOS;
                this.gene_MS = bestMS;
                
                // 重新计算适应度和printSolution（使用PSO的FITNESS_SCALE）
                Chromosome c = toChromosome();
                double makespan = caculateFitness.evaluate(c, problem, operationMatrix);
                this.fitness = 1000000.0 / makespan;  // FITNESS_SCALE = 1000000.0
                this.printSolution = c.printSolution;
            } else {
                // 恢复原始解
                this.gene_OS = originalOS;
                this.gene_MS = originalMS;
            }
            
        } catch (Exception e) {
            // 发生错误时恢复原始解
            this.gene_OS = originalOS;
            this.gene_MS = originalMS;
            System.err.println("localSearchOS发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        return improved;
    }
    
    /**
     * 对当前粒子进行MA邻域改进（机器分配优化）
     * 对关键工序尝试换机器，看是否缩短关键路径
     * 
     * @param problem 问题实例
     * @param caculateFitness 适应度计算器
     * @param operationMatrix 工序矩阵
     * @param improveProb 执行改进的概率
     * @param maxAttempts 最大尝试次数
     * @return 是否找到更优解
     */
    public boolean localSearchMA(Problem problem, CaculateFitness caculateFitness,
                                  ProgramEntity.Operation[][] operationMatrix,
                                  double improveProb, int maxAttempts) {
        if (r.nextDouble() > improveProb) {
            return false;
        }
        
        // 保存原始基因
        int[] originalOS = gene_OS.clone();
        int[] originalMS = gene_MS.clone();
        
        boolean improved = false;
        int[] bestMS = gene_MS.clone();
        double bestMakespan = Double.MAX_VALUE;
        
        try {
            // 计算当前makespan
            double currentMakespan = caculateFitness.evaluate(toChromosome(), problem, operationMatrix);
            bestMakespan = currentMakespan;
            
            // 获取关键路径相关工序
            Set<Integer> criticalOperations = getCriticalPathOperations(problem, caculateFitness, operationMatrix);
            
            for (int attempt = 0; attempt < maxAttempts; attempt++) {
                int[] newMS = bestMS.clone();  // 从当前最优解开始
                
                // 选择一个关键工序位置进行机器变更
                int pos = selectCriticalPositionForMachineChange(criticalOperations, problem);
                
                if (pos >= 0 && pos < newMS.length) {
                    // 获取该位置对应的工序信息
                    int currentMachine = newMS[pos];
                    
                    // 获取可用机器列表
                    List<Integer> availableMachines = getAvailableMachines(pos, problem);
                    
                    // 尝试不同的机器
                    if (availableMachines.size() > 1) {
                        // 随机选择一个不同的机器
                        int newMachine = currentMachine;
                        int tryCount = 0;
                        while (newMachine == currentMachine && tryCount < availableMachines.size()) {
                            newMachine = availableMachines.get(r.nextInt(availableMachines.size()));
                            tryCount++;
                        }
                        
                        if (newMachine != currentMachine) {
                            newMS[pos] = newMachine;
                            
                            // 评估新解
                            gene_MS = newMS;
                            Chromosome tempChrom = toChromosome();
                            double newMakespan = caculateFitness.evaluate(tempChrom, problem, operationMatrix);
                            
                            // 如果找到更优解（makespan更小），保存
                            if (newMakespan < bestMakespan) {
                                bestMS = newMS.clone();
                                bestMakespan = newMakespan;
                                improved = true;
                            }
                        }
                    }
                }
            }
            
            // 应用最优解
            if (improved) {
                this.gene_MS = bestMS;
                
                // 重新计算适应度和printSolution（使用PSO的FITNESS_SCALE）
                Chromosome c = toChromosome();
                double makespan = caculateFitness.evaluate(c, problem, operationMatrix);
                this.fitness = 1000000.0 / makespan;  // FITNESS_SCALE = 1000000.0
                this.printSolution = c.printSolution;
            } else {
                // 恢复原始解
                this.gene_MS = originalMS;
            }
            
        } catch (Exception e) {
            // 发生错误时恢复原始解
            this.gene_OS = originalOS;
            this.gene_MS = originalMS;
            System.err.println("localSearchMA发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
        
        return improved;
    }
    
    /**
     * 获取关键路径上的工序位置集合
     * 简化版：基于完工时间识别关键工序
     */
    private Set<Integer> getCriticalPathOperations(Problem problem, 
                                                    CaculateFitness caculateFitness,
                                                    ProgramEntity.Operation[][] operationMatrix) {
        Set<Integer> criticalOps = new HashSet<>();
        
        try {
            // 如果没有printSolution，先计算
            if (printSolution == null || printSolution.length == 0) {
                Chromosome c = toChromosome();
                caculateFitness.evaluate(c, problem, operationMatrix);
                this.printSolution = c.printSolution;
            }
            
            // 找到最大完工时间（makespan）
            double maxCompletionTime = 0;
            if (printSolution != null) {
                for (int stage = 0; stage < printSolution.length; stage++) {
                    if (printSolution[stage] != null) {
                        for (Solution sol : printSolution[stage]) {
                            if (sol != null && sol.endTime > maxCompletionTime) {
                                maxCompletionTime = sol.endTime;
                            }
                        }
                    }
                }
            }
            
            // 将接近最大完工时间的工序视为关键工序（容差：5%）
            if (maxCompletionTime > 0) {
                double threshold = maxCompletionTime * 0.95;
                int position = 0;
                for (int stage = 0; stage < printSolution.length; stage++) {
                    if (printSolution[stage] != null) {
                        for (Solution sol : printSolution[stage]) {
                            if (sol != null && sol.endTime >= threshold && position < gene_OS.length) {
                                criticalOps.add(position);
                            }
                            position++;
                        }
                    }
                }
            }
            
            // 如果没有找到关键工序，随机添加一些位置
            if (criticalOps.isEmpty() && gene_OS.length > 0) {
                int count = Math.min(5, gene_OS.length);
                for (int i = 0; i < count; i++) {
                    criticalOps.add(r.nextInt(gene_OS.length));
                }
            }
            
        } catch (Exception e) {
            System.err.println("getCriticalPathOperations发生错误: " + e.getMessage());
            // 如果出错，返回一些随机位置
            if (gene_OS != null && gene_OS.length > 0) {
                int count = Math.min(5, gene_OS.length);
                for (int i = 0; i < count; i++) {
                    criticalOps.add(r.nextInt(gene_OS.length));
                }
            }
        }
        
        return criticalOps;
    }
    
    /**
     * 智能选择需要改进的位置
     * 优先选择关键路径上的工序或同机器的工序
     */
    private int selectPositionForImprovement(int[] os, Set<Integer> criticalOps, Problem problem) {
        if (os == null || os.length == 0) {
            return 0;
        }
        
        // 70%概率选择关键工序
        if (criticalOps != null && !criticalOps.isEmpty() && r.nextDouble() < 0.7) {
            List<Integer> criticalList = new ArrayList<>(criticalOps);
            // 过滤掉无效的位置
            criticalList.removeIf(pos -> pos < 0 || pos >= os.length);
            if (!criticalList.isEmpty()) {
                return criticalList.get(r.nextInt(criticalList.size()));
            }
        }
        
        // 否则随机选择
        return r.nextInt(os.length);
    }
    
    /**
     * 选择关键工序位置进行机器变更
     */
    private int selectCriticalPositionForMachineChange(Set<Integer> criticalOps, Problem problem) {
        if (gene_MS == null || gene_MS.length == 0) {
            return -1;
        }
        
        if (criticalOps != null && !criticalOps.isEmpty() && r.nextDouble() < 0.8) {
            // 80%概率选择关键工序
            List<Integer> criticalList = new ArrayList<>(criticalOps);
            // 过滤掉无效的位置
            criticalList.removeIf(pos -> pos < 0 || pos >= gene_MS.length);
            if (!criticalList.isEmpty()) {
                return criticalList.get(r.nextInt(criticalList.size()));
            }
        }
        
        // 否则随机选择（跳过打印阶段，从jobCount开始）
        int jobCount = problem.getJobCount();
        if (gene_MS.length > jobCount) {
            return jobCount + r.nextInt(gene_MS.length - jobCount);
        }
        
        // 如果没有离散工序，返回随机位置
        if (gene_MS.length > 0) {
            return r.nextInt(gene_MS.length);
        }
        
        return -1;
    }
    
    /**
     * 获取指定位置可用的机器列表
     */
    private List<Integer> getAvailableMachines(int pos, Problem problem) {
        List<Integer> machines = new ArrayList<>();
        int jobCount = problem.getJobCount();
        
        // 打印阶段
        if (pos < jobCount) {
            int jobNo = gene_OS[pos];
            ProgramEntity.Item[] items = problem.getItems();
            ProgramEntity.Machine.Machine[] allMachines = problem.getMachines();
            int printMachineCount = problem.getPrintMachineCount();
            
            double l = items[jobNo].l;
            double w = items[jobNo].w;
            
            for (int m = 0; m < printMachineCount; m++) {
                ProgramEntity.Machine.PrintMachine pm = 
                    (ProgramEntity.Machine.PrintMachine) allMachines[m];
                if (l <= pm.L && w <= pm.W) {
                    machines.add(m + 1);
                }
            }
        } else {
            // 离散处理阶段
            double[][] proDesMatrix = problem.getProDesMatrix();
            int[][] operationToIndex = problem.getOperationToIndex();
            int[] operationCountArr = problem.getOperationCountArr();
            
            // 找到该位置对应的工序
            int currentPos = jobCount;
            for (int jobNo = 0; jobNo < jobCount; jobNo++) {
                int discreteOpsCount = operationCountArr[jobNo] - 2;
                for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                    if (currentPos == pos) {
                        int operNo = 2 + localOperNo;
                        int operIdx = operationToIndex[jobNo][operNo];
                        
                        // 获取可用机器
                        for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
                            if (proDesMatrix[operIdx][m] > 0 && 
                                proDesMatrix[operIdx][m] != Double.MAX_VALUE) {
                                machines.add(m + 1);
                            }
                        }
                        return machines;
                    }
                    currentPos++;
                }
            }
        }
        
        return machines;
    }
    
    /**
     * 对pbest进行邻域改进（综合OS和MA）
     * 
     * @param problem 问题实例
     * @param caculateFitness 适应度计算器
     * @param operationMatrix 工序矩阵
     * @param improveProb 执行改进的概率
     * @param maxAttemptsOS OS改进最大尝试次数
     * @param maxAttemptsMA MA改进最大尝试次数
     * @return 是否找到更优解
     */
    public boolean improvePBest(Problem problem, CaculateFitness caculateFitness,
                                 ProgramEntity.Operation[][] operationMatrix,
                                 double improveProb, int maxAttemptsOS, int maxAttemptsMA) {
        // 保存当前状态
        int[] origOS = gene_OS.clone();
        int[] origMS = gene_MS.clone();
        double origFitness = fitness;
        
        try {
            // 将pbest复制到当前位置
            if (useContinuousEncoding) {
                // 连续编码模式：从pbest连续向量解码
                if (pBest_OS_print_continuous != null) {
                    position_OS_print_continuous = pBest_OS_print_continuous.clone();
                    position_MS_print_continuous = pBest_MS_print_continuous.clone();
                    position_OS_discrete_continuous = pBest_OS_discrete_continuous.clone();
                    position_MS_discrete_continuous = pBest_MS_discrete_continuous.clone();
                    
                    continuousToDiscrete_OS(problem);
                    continuousToDiscrete_MS(problem);
                }
            } else {
                // 离散编码模式：直接复制pbest
                if (pBest_OS != null && pBest_MS != null) {
                    gene_OS = pBest_OS.clone();
                    gene_MS = pBest_MS.clone();
                }
            }
            
            // 重新计算适应度
            Chromosome c = toChromosome();
            double makespan = caculateFitness.evaluate(c, problem, operationMatrix);
            fitness = 1000000.0 / makespan;  // FITNESS_SCALE = 1000000.0
            printSolution = c.printSolution;
            
            boolean improved = false;
            
            // 先尝试OS改进
            if (localSearchOS(problem, caculateFitness, operationMatrix, improveProb, maxAttemptsOS)) {
                improved = true;
            }
            
            // 再尝试MA改进
            if (localSearchMA(problem, caculateFitness, operationMatrix, improveProb, maxAttemptsMA)) {
                improved = true;
            }
            
            // 如果改进成功，更新pbest
            if (improved && fitness > pBestFitness) {
                if (useContinuousEncoding) {
                    if (position_OS_print_continuous != null) {
                        pBest_OS_print_continuous = position_OS_print_continuous.clone();
                        pBest_MS_print_continuous = position_MS_print_continuous.clone();
                        pBest_OS_discrete_continuous = position_OS_discrete_continuous.clone();
                        pBest_MS_discrete_continuous = position_MS_discrete_continuous.clone();
                    }
                } else {
                    pBest_OS = gene_OS.clone();
                    pBest_MS = gene_MS.clone();
                }
                pBestFitness = fitness;
            }
            
            // 恢复原始状态
            gene_OS = origOS;
            gene_MS = origMS;
            fitness = origFitness;
            
            // 重新计算printSolution
            c = toChromosome();
            caculateFitness.evaluate(c, problem, operationMatrix);
            printSolution = c.printSolution;
            
            return improved;
            
        } catch (Exception e) {
            // 发生错误时恢复原始解
            gene_OS = origOS;
            gene_MS = origMS;
            fitness = origFitness;
            System.err.println("improvePBest发生错误: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
