package AlgorithmFrame.alns;

import AlgorithmFrame.alns.destroy.*;
import AlgorithmFrame.alns.localsearch.LocalSearch;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;
import AlgorithmFrame.alns.repair.*;

import java.util.*;

/**
 * ALNS-SL算法主类
 * 实现Adaptive Large Neighborhood Search with Skyline Packing
 */
public class ALNS {
    private ALNSJob[] jobs;
    private ALNSMachine[] machines;
    private ALNSParameters params;
    private ALNSEvaluator evaluator;
    private SkylinePackingAdapter packer;
    private LocalSearch localSearch;
    private Random random;
    
    // Destroy和Repair算子
    private List<DestroyOperator> destroyOperators;
    private List<RepairOperator> repairOperators;
    
    // 算子权重
    private Map<DestroyOperator, Double> destroyWeights;
    private Map<RepairOperator, Double> repairWeights;
    
    // 算子使用统计
    private Map<DestroyOperator, Integer> destroyUseCounts;
    private Map<RepairOperator, Integer> repairUseCounts;
    private Map<DestroyOperator, Double> destroyScores;
    private Map<RepairOperator, Double> repairScores;
    
    // 迭代历史记录（用于可视化）
    private List<Double> iterationHistory;
    
    public ALNS(ALNSJob[] jobs, ALNSMachine[] machines, ALNSParameters params, long seed) {
        this.jobs = jobs;
        this.machines = machines;
        this.params = params;
        this.random = new Random(seed);
        
        this.evaluator = new ALNSEvaluator(jobs, machines);
        this.packer = new SkylinePackingAdapter(true); // 允许旋转
        this.localSearch = new LocalSearch(jobs, machines, evaluator, packer);
        this.iterationHistory = new ArrayList<>();
        
        initializeOperators();
        initializeWeights();
    }
    
    /**
     * 初始化所有Destroy和Repair算子
     */
    private void initializeOperators() {
        destroyOperators = new ArrayList<>();
        destroyOperators.add(new RandomRemovalJob(random));
        destroyOperators.add(new ShawRemoval(random));
        destroyOperators.add(new WorstRemovalJob(evaluator));
        destroyOperators.add(new ProcessingTimeRemoval(random));
        destroyOperators.add(new IdleRemoval());
        destroyOperators.add(new RandomRemovalBatch(random));
        destroyOperators.add(new ProcessingTimeVarianceRemoval());
        destroyOperators.add(new ReleaseTimeVarianceRemoval());
        destroyOperators.add(new MinProcessingTimeVarianceRemoval());
        destroyOperators.add(new MaxCmaxContributionRemoval(evaluator));
        destroyOperators.add(new CriticalMachineRemoval());
        
        repairOperators = new ArrayList<>();
        repairOperators.add(new GreedyInsertionByMinProcessing());
        repairOperators.add(new GreedyInsertionByRelease());
        repairOperators.add(new SlackInsertion());
        repairOperators.add(new RegretBatchInsertion(params.gamma1, params.gamma2));
        repairOperators.add(new RegretInsertion());
        repairOperators.add(new ProcessingTimeInsertion());
        repairOperators.add(new RandomInsertion(random));
    }
    
    /**
     * 初始化算子权重
     */
    private void initializeWeights() {
        destroyWeights = new HashMap<>();
        repairWeights = new HashMap<>();
        destroyUseCounts = new HashMap<>();
        repairUseCounts = new HashMap<>();
        destroyScores = new HashMap<>();
        repairScores = new HashMap<>();
        
        for (DestroyOperator op : destroyOperators) {
            destroyWeights.put(op, params.initialWeight);
            destroyUseCounts.put(op, 0);
            destroyScores.put(op, 0.0);
        }
        
        for (RepairOperator op : repairOperators) {
            repairWeights.put(op, params.initialWeight);
            repairUseCounts.put(op, 0);
            repairScores.put(op, 0.0);
        }
    }
    
    /**
     * 运行ALNS算法
     */
    public ALNSSolution solve() {
        long startTime = System.currentTimeMillis();
        
        // 生成初始解
        ALNSSolution currentSolution = generateInitialSolution();
        if (currentSolution == null) {
            System.out.println("无法生成初始解！");
            return null;
        }
        
        evaluator.evaluate(currentSolution);
        ALNSSolution bestSolution = currentSolution.copy();
        
        // 记录初始解
        iterationHistory.add(currentSolution.cmax);
        
        System.out.println("初始解 Cmax: " + currentSolution.cmax);
        
        // ALNS主循环
        double temperature = params.T0;
        int iteration = 0;
        int iterationsWithoutImprovement = 0;
        
        while ((System.currentTimeMillis() - startTime) < params.timeLimitMs) {
            
            iteration++;
            
            // 轮盘赌选择destroy和repair算子
            DestroyOperator destroyOp = rouletteSelect(destroyOperators, destroyWeights);
            RepairOperator repairOp = rouletteSelect(repairOperators, repairWeights);
            
            // 计算移除作业数量
            int numToRemove = Math.max(1, (int) Math.ceil(params.theta * jobs.length));
            
            // Destroy
            ALNSSolution partialSolution = currentSolution.copy();
            List<Integer> removedJobs = destroyOp.destroy(partialSolution, jobs, machines, numToRemove);
            
            if (removedJobs.isEmpty()) {
                continue;
            }
            
            // Repair
            boolean repaired = repairOp.repair(partialSolution, removedJobs, jobs, machines, evaluator, packer);
            
            if (!repaired) {
                if (iteration % 100 == 0) {
                    System.err.println("迭代 " + iteration + ": Repair失败，" + destroyOp.getName() + 
                                     " + " + repairOp.getName() + "，移除了 " + removedJobs.size() + " 个作业");
                }
                continue;
            }
            
            // 验证Repair后作业数量
            int jobsAfterRepair = partialSolution.getAllJobIds().size();
            if (jobsAfterRepair != jobs.length) {
                System.err.println("严重错误：Repair后作业数不匹配！期望=" + jobs.length + 
                                 ", 实际=" + jobsAfterRepair + 
                                 ", Destroy=" + destroyOp.getName() + 
                                 ", Repair=" + repairOp.getName());
                continue;
            }
            
            evaluator.evaluate(partialSolution);
            
            // Local Search
            ALNSSolution newSolution = localSearch.search(partialSolution);
            
            // 验证LocalSearch后作业数量
            int jobsAfterLS = newSolution.getAllJobIds().size();
            if (jobsAfterLS != jobs.length) {
                System.err.println("严重错误：LocalSearch后作业数不匹配！期望=" + jobs.length + 
                                 ", 实际=" + jobsAfterLS);
                continue;
            }
            
            // 最终验证：在接受解之前再次检查作业数量
            int finalJobCount = newSolution.getAllJobIds().size();
            if (finalJobCount != jobs.length) {
                System.err.println("严重错误：最终解作业数不匹配！期望=" + jobs.length + 
                                 ", 实际=" + finalJobCount + "，拒绝接受该解");
                continue;
            }
            
            // 接受准则（Simulated Annealing）
            boolean accept = false;
            int reward = params.sigma3; // 默认拒绝奖励
            
            if (newSolution.cmax < currentSolution.cmax) {
                accept = true;
                reward = params.sigma2;
                
                if (newSolution.cmax < bestSolution.cmax) {
                    // 再次验证最优解
                    int bestJobCount = newSolution.getAllJobIds().size();
                    if (bestJobCount == jobs.length) {
                        bestSolution = newSolution.copy();
                        reward = params.sigma1;
                        iterationsWithoutImprovement = 0;
                        System.out.println("迭代 " + iteration + ": 新最优解 Cmax = " + bestSolution.cmax);
                    } else {
                        System.err.println("警告：最优解候选作业数不匹配，不更新bestSolution");
                        iterationsWithoutImprovement++;
                    }
                } else {
                    iterationsWithoutImprovement++;
                }
            } else {
                // SA接受准则
                double delta = newSolution.cmax - currentSolution.cmax;
                double probability = Math.exp(-delta / temperature);
                if (random.nextDouble() < probability) {
                    accept = true;
                    reward = params.sigma2;
                }
                iterationsWithoutImprovement++;
            }
            
            if (accept) {
                currentSolution = newSolution;
            }
            
            // 记录当前最优解
            iterationHistory.add(bestSolution.cmax);
            
            // 更新算子分数
            updateOperatorScore(destroyOp, repairOp, reward);
            
            // 降温
            temperature *= params.beta;
            
            // 定期更新权重
            if (iteration % params.weightUpdateInterval == 0) {
                updateWeights();
                
                if (iteration % (params.weightUpdateInterval * 10) == 0) {
                    System.out.println("迭代 " + iteration + 
                                     ": 当前Cmax = " + currentSolution.cmax + 
                                     ", 最优Cmax = " + bestSolution.cmax +
                                     ", 温度 = " + String.format("%.4f", temperature));
                }
            }
        }
        
        long endTime = System.currentTimeMillis();
        System.out.println("ALNS完成！总迭代: " + iteration + 
                         ", 运行时间: " + (endTime - startTime) + "ms" +
                         ", 最优Cmax: " + bestSolution.cmax);
        
        // 检查解的完整性
        int totalJobsInSolution = 0;
        for (int machineId = 0; machineId < machines.length; machineId++) {
            List<ALNSBatch> batches = bestSolution.getMachineBatches(machineId);
            for (ALNSBatch batch : batches) {
                totalJobsInSolution += batch.jobs.size();
            }
        }
        System.out.println("原始作业数: " + jobs.length + ", 解中作业数: " + totalJobsInSolution);
        
        if (totalJobsInSolution != jobs.length) {
            System.err.println("警告：解中的作业数与原始作业数不匹配！");
            // 找出缺失的作业
            List<Integer> allJobsInSolution = bestSolution.getAllJobIds();
            for (int i = 0; i < jobs.length; i++) {
                if (!allJobsInSolution.contains(i)) {
                    System.err.println("  缺失作业ID: " + i + " (name=" + jobs[i].name + ")");
                }
            }
        }
        
        return bestSolution;
    }
    
    /**
     * 生成初始解
     * 按r_j升序 + p*_j降序排序，贪婪插入使Cmax最小
     */
    private ALNSSolution generateInitialSolution() {
        ALNSSolution solution = new ALNSSolution(machines.length);
        
        // 作业排序：优先按释放期升序，其次按最小处理时间降序
        List<Integer> jobOrder = new ArrayList<>();
        for (int i = 0; i < jobs.length; i++) {
            jobOrder.add(i);
        }
        
        jobOrder.sort((j1, j2) -> {
            int releaseCmp = Double.compare(jobs[j1].releaseTime, jobs[j2].releaseTime);
            if (releaseCmp != 0) return releaseCmp;
            return Double.compare(jobs[j2].getMinProcessingTime(), jobs[j1].getMinProcessingTime());
        });
        
        // 贪婪插入
        for (int jobId : jobOrder) {
            double minCmaxIncrease = Double.MAX_VALUE;
            int bestMachine = -1;
            int bestBatch = -1;
            ALNSBatch bestNewBatch = null;
            
            // 枚举所有可能的插入位置
            for (int machineId = 0; machineId < machines.length; machineId++) {
                List<ALNSBatch> batches = solution.getMachineBatches(machineId);
                
                // 尝试插入到现有批次
                for (int batchIdx = 0; batchIdx < batches.size(); batchIdx++) {
                    List<Integer> testJobs = new ArrayList<>(batches.get(batchIdx).jobs);
                    testJobs.add(jobId);
                    
                    ALNSBatch newBatch = packer.packJobsIntoBatch(testJobs, machines[machineId], jobs);
                    if (newBatch != null) {
                        ALNSSolution tempSol = solution.copy();
                        tempSol.getMachineBatches(machineId).set(batchIdx, newBatch);
                        double newCmax = evaluator.evaluate(tempSol);
                        double cmaxIncrease = newCmax - solution.cmax;
                        
                        if (cmaxIncrease < minCmaxIncrease) {
                            minCmaxIncrease = cmaxIncrease;
                            bestMachine = machineId;
                            bestBatch = batchIdx;
                            bestNewBatch = newBatch;
                        }
                    }
                }
                
                // 尝试创建新批次
                List<Integer> newBatchJobs = new ArrayList<>();
                newBatchJobs.add(jobId);
                ALNSBatch newBatch = packer.packJobsIntoBatch(newBatchJobs, machines[machineId], jobs);
                if (newBatch != null) {
                    ALNSSolution tempSol = solution.copy();
                    tempSol.addBatch(machineId, newBatch);
                    double newCmax = evaluator.evaluate(tempSol);
                    double cmaxIncrease = newCmax - solution.cmax;
                    
                    if (cmaxIncrease < minCmaxIncrease) {
                        minCmaxIncrease = cmaxIncrease;
                        bestMachine = machineId;
                        bestBatch = -1;
                        bestNewBatch = newBatch;
                    }
                }
            }
            
            // 执行最佳插入
            if (bestMachine == -1) {
                System.err.println("警告：无法插入作业 " + jobId + " (name=" + jobs[jobId].name + 
                                 ", width=" + jobs[jobId].width + ", height=" + jobs[jobId].height + ")");
                System.err.println("  已插入作业数: " + (jobOrder.indexOf(jobId)) + " / " + jobs.length);
                
                // 打印机器平台尺寸
                for (int m = 0; m < machines.length; m++) {
                    System.err.println("  机器" + m + "平台: " + machines[m].width + " x " + machines[m].height);
                }
                return null;
            }
            
            if (bestBatch == -1) {
                solution.addBatch(bestMachine, bestNewBatch);
            } else {
                solution.getMachineBatches(bestMachine).set(bestBatch, bestNewBatch);
            }
            
            evaluator.evaluate(solution);
        }
        
        System.out.println("初始解生成成功，共插入 " + jobs.length + " 个作业");
        return solution;
    }
    
    /**
     * 轮盘赌选择算子
     */
    private <T> T rouletteSelect(List<T> operators, Map<T, Double> weights) {
        double totalWeight = 0.0;
        for (T op : operators) {
            totalWeight += weights.get(op);
        }
        
        double rand = random.nextDouble() * totalWeight;
        double cumulative = 0.0;
        
        for (T op : operators) {
            cumulative += weights.get(op);
            if (cumulative >= rand) {
                return op;
            }
        }
        
        return operators.get(operators.size() - 1);
    }
    
    /**
     * 更新算子分数
     */
    private void updateOperatorScore(DestroyOperator destroyOp, RepairOperator repairOp, int reward) {
        destroyUseCounts.put(destroyOp, destroyUseCounts.get(destroyOp) + 1);
        repairUseCounts.put(repairOp, repairUseCounts.get(repairOp) + 1);
        destroyScores.put(destroyOp, destroyScores.get(destroyOp) + reward);
        repairScores.put(repairOp, repairScores.get(repairOp) + reward);
    }
    
    /**
     * 更新算子权重
     */
    private void updateWeights() {
        // 更新destroy算子权重
        for (DestroyOperator op : destroyOperators) {
            int useCount = destroyUseCounts.get(op);
            double score = destroyScores.get(op);
            double oldWeight = destroyWeights.get(op);
            
            if (useCount > 0) {
                double avgScore = score / useCount;
                double newWeight = oldWeight * (1 - params.r) + params.r * avgScore;
                destroyWeights.put(op, Math.max(0.01, newWeight)); // 保证最小权重
            }
            
            // 重置统计
            destroyUseCounts.put(op, 0);
            destroyScores.put(op, 0.0);
        }
        
        // 更新repair算子权重
        for (RepairOperator op : repairOperators) {
            int useCount = repairUseCounts.get(op);
            double score = repairScores.get(op);
            double oldWeight = repairWeights.get(op);
            
            if (useCount > 0) {
                double avgScore = score / useCount;
                double newWeight = oldWeight * (1 - params.r) + params.r * avgScore;
                repairWeights.put(op, Math.max(0.01, newWeight));
            }
            
            // 重置统计
            repairUseCounts.put(op, 0);
            repairScores.put(op, 0.0);
        }
    }
    
    /**
     * 获取最佳解
     */
    public ALNSSolution getBestSolution() {
        return null; // 在solve()方法中直接返回
    }
    
    /**
     * 获取迭代历史（用于可视化）
     */
    public List<Double> getIterationHistory() {
        return new ArrayList<>(iterationHistory);
    }
    
    /**
     * 获取作业数组（用于可视化）
     */
    public ALNSJob[] getJobs() {
        return jobs;
    }
    
    /**
     * 获取机器数组（用于可视化）
     */
    public ALNSMachine[] getMachines() {
        return machines;
    }
}
