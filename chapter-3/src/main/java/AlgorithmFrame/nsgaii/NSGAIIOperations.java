package AlgorithmFrame.nsgaii;

import ProblemFrame.*;
import ProgramEntity.*;
import ProgramEntity.Machine.*;
import java.util.*;

/**
 * NSGA-II 核心算法组件
 * 
 * 包含：
 * - 快速非支配排序 (Fast Non-dominated Sort)
 * - 拥挤距离计算 (Crowding Distance Assignment)
 * - 环境选择 (Environmental Selection)
 * - 交叉操作 (Crossover Operations) - 来自chapter-2
 * - 变异操作 (Mutation Operations) - 来自chapter-2
 * 
 * @author AI Assistant
 * @version 2.0 (整合chapter-2交叉变异算子)
 */
public class NSGAIIOperations {
    
    // 依赖
    private Problem problem;
    private Random random;
    private CaculateFitness fitnessCalculator;
    
    /**
     * 构造函数
     */
    public NSGAIIOperations(Problem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.fitnessCalculator = new CaculateFitness();
    }
    
    /**
     * 快速非支配排序
     * 
     * 将种群按非支配关系分层：
     * - F1: 非支配解集（Pareto前沿）
     * - F2: 被F1支配的解集
     * - F3: 被F1和F2支配的解集
     * ...
     * 
     * 时间复杂度: O(MN^2)，其中M是目标数，N是种群大小
     * 
     * @param population 待排序的种群
     * @return 分层结果，fronts[0]是第一前沿，fronts[1]是第二前沿，...
     */
    public static List<List<MOIndividual>> fastNonDominatedSort(List<MOIndividual> population) {
        int n = population.size();
        
        // 每个个体被多少个个体支配
        int[] dominationCount = new int[n];
        
        // 每个个体支配哪些个体
        @SuppressWarnings("unchecked")
        List<Integer>[] dominatedSolutions = new ArrayList[n];
        for (int i = 0; i < n; i++) {
            dominatedSolutions[i] = new ArrayList<>();
        }
        
        // 第一前沿（非支配解）
        List<Integer> firstFront = new ArrayList<>();
        
        // 计算支配关系
        for (int i = 0; i < n; i++) {
            MOIndividual p = population.get(i);
            
            for (int j = 0; j < n; j++) {
                if (i == j) continue;
                
                MOIndividual q = population.get(j);
                
                if (p.dominates(q)) {
                    // p 支配 q
                    dominatedSolutions[i].add(j);
                } else if (q.dominates(p)) {
                    // q 支配 p
                    dominationCount[i]++;
                }
            }
            
            // 如果p不被任何个体支配，加入第一前沿
            if (dominationCount[i] == 0) {
                p.rank = 1;
                firstFront.add(i);
            }
        }
        
        // 构建所有前沿
        List<List<MOIndividual>> fronts = new ArrayList<>();
        List<Integer> currentFront = firstFront;
        int currentRank = 1;
        
        while (!currentFront.isEmpty()) {
            List<MOIndividual> frontIndividuals = new ArrayList<>();
            List<Integer> nextFront = new ArrayList<>();
            
            for (int i : currentFront) {
                MOIndividual p = population.get(i);
                frontIndividuals.add(p);
                
                // 对p支配的每个个体q
                for (int j : dominatedSolutions[i]) {
                    dominationCount[j]--;
                    
                    // 如果q不再被任何个体支配，加入下一前沿
                    if (dominationCount[j] == 0) {
                        population.get(j).rank = currentRank + 1;
                        nextFront.add(j);
                    }
                }
            }
            
            fronts.add(frontIndividuals);
            currentFront = nextFront;
            currentRank++;
        }
        
        return fronts;
    }
    
    /**
     * 拥挤距离计算
     * 
     * 对一个前沿内的个体计算拥挤距离：
     * - 边界个体（每个目标的最小/最大值）设为无穷大
     * - 中间个体累加各目标的归一化距离
     * - 特殊情况：前沿大小<=2时，所有个体设为无穷大
     * 
     * 时间复杂度: O(MN log N)，其中M是目标数，N是前沿大小
     * 
     * @param front 同一前沿内的个体列表
     */
    public static void assignCrowdingDistance(List<MOIndividual> front) {
        int size = front.size();
        
        // 特殊情况：前沿大小<=2
        if (size <= 2) {
            for (MOIndividual ind : front) {
                ind.crowdingDistance = Double.POSITIVE_INFINITY;
            }
            return;
        }
        
        int numObjectives = front.get(0).objectives.length;
        
        // 初始化拥挤距离
        for (MOIndividual ind : front) {
            ind.crowdingDistance = 0.0;
        }
        
        // 对每个目标计算拥挤距离
        for (int m = 0; m < numObjectives; m++) {
            final int objIndex = m;
            
            // 按第m个目标升序排序
            List<MOIndividual> sortedFront = new ArrayList<>(front);
            sortedFront.sort(Comparator.comparingDouble(ind -> ind.objectives[objIndex]));
            
            // 获取该目标的范围
            double minObj = sortedFront.get(0).objectives[objIndex];
            double maxObj = sortedFront.get(size - 1).objectives[objIndex];
            double range = maxObj - minObj;
            
            // 边界个体设为无穷大
            sortedFront.get(0).crowdingDistance = Double.POSITIVE_INFINITY;
            sortedFront.get(size - 1).crowdingDistance = Double.POSITIVE_INFINITY;
            
            // 如果该目标所有个体值相同，跳过
            if (range < 1e-10) {
                continue;
            }
            
            // 计算中间个体的拥挤距离
            for (int i = 1; i < size - 1; i++) {
                MOIndividual current = sortedFront.get(i);
                
                // 跳过已经是边界的个体
                if (Double.isInfinite(current.crowdingDistance)) {
                    continue;
                }
                
                double prevObj = sortedFront.get(i - 1).objectives[objIndex];
                double nextObj = sortedFront.get(i + 1).objectives[objIndex];
                
                // 累加归一化距离
                current.crowdingDistance += (nextObj - prevObj) / range;
            }
        }
    }
    
    /**
     * 环境选择（带截断）
     * 
     * 合并父代和子代，选择最优的N个个体作为下一代：
     * 1. 合并父代和子代 R = P ∪ Q
     * 2. 快速非支配排序得到前沿
     * 3. 依次加入前沿直到某个前沿放不下（截断前沿）
     * 4. 对截断前沿计算拥挤距离并按拥挤距离降序选择
     * 5. 拥挤距离接近时使用tie-breaker (packingQ)
     * 
     * @param parent 父代种群
     * @param offspring 子代种群
     * @param targetSize 目标种群大小N
     * @param delta 拥挤距离阈值（用于tie-breaker）
     * @return 下一代种群（恰好N个个体）
     */
    public static List<MOIndividual> environmentalSelection(
            List<MOIndividual> parent,
            List<MOIndividual> offspring,
            int targetSize,
            double delta) {
        
        // 1. 合并父代和子代
        List<MOIndividual> combined = new ArrayList<>();
        combined.addAll(parent);
        combined.addAll(offspring);
        
        // 2. 快速非支配排序
        List<List<MOIndividual>> fronts = fastNonDominatedSort(combined);
        
        // 3. 构建下一代种群
        List<MOIndividual> nextPopulation = new ArrayList<>();
        int frontIndex = 0;
        
        // 依次加入完整的前沿
        while (frontIndex < fronts.size() && 
               nextPopulation.size() + fronts.get(frontIndex).size() <= targetSize) {
            List<MOIndividual> currentFront = fronts.get(frontIndex);
            
            // 计算该前沿的拥挤距离
            assignCrowdingDistance(currentFront);
            
            nextPopulation.addAll(currentFront);
            frontIndex++;
        }
        
        // 4. 处理截断前沿
        if (nextPopulation.size() < targetSize && frontIndex < fronts.size()) {
            List<MOIndividual> lastFront = fronts.get(frontIndex);
            
            // 计算截断前沿的拥挤距离
            assignCrowdingDistance(lastFront);
            
            // 按拥挤距离降序排序（带tie-breaker）
            lastFront.sort((a, b) -> a.compareForTruncation(b, delta));
            
            // 填充剩余位置
            int remaining = targetSize - nextPopulation.size();
            for (int i = 0; i < remaining && i < lastFront.size(); i++) {
                nextPopulation.add(lastFront.get(i));
            }
        }
        
        return nextPopulation;
    }
    
    /**
     * 锦标赛选择（NSGA-II版本）
     * 
     * 随机选择tournamentSize个个体，选出最优的一个
     * 比较规则：
     * 1. rank 更小优先
     * 2. rank 相同，crowdingDistance 更大优先
     * 3. crowdingDistance 接近时，使用tie-breaker
     * 
     * @param population 种群
     * @param tournamentSize 锦标赛大小（通常为2或3）
     * @param random 随机数生成器
     * @return 选中的个体
     */
    public static MOIndividual tournamentSelection(
            List<MOIndividual> population,
            int tournamentSize,
            Random random) {
        
        MOIndividual best = null;
        
        for (int i = 0; i < tournamentSize; i++) {
            int index = random.nextInt(population.size());
            MOIndividual candidate = population.get(index);
            
            if (best == null || candidate.compareTo(best) < 0) {
                best = candidate;
            }
        }
        
        return best;
    }
    
    /**
     * 批量锦标赛选择
     * 
     * @param population 种群
     * @param count 选择个体数量
     * @param tournamentSize 锦标赛大小
     * @param random 随机数生成器
     * @return 选中的个体列表
     */
    public static List<MOIndividual> tournamentSelectionBatch(
            List<MOIndividual> population,
            int count,
            int tournamentSize,
            Random random) {
        
        List<MOIndividual> selected = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            selected.add(tournamentSelection(population, tournamentSize, random));
        }
        
        return selected;
    }
    
    /**
     * 获取Pareto前沿（第一前沿）
     * 
     * @param population 种群
     * @return Pareto前沿的个体列表
     */
    public static List<MOIndividual> getParetoFront(List<MOIndividual> population) {
        List<List<MOIndividual>> fronts = fastNonDominatedSort(population);
        return fronts.isEmpty() ? new ArrayList<>() : fronts.get(0);
    }
    
    /**
     * 计算超体积指标（Hypervolume）
     * 
     * 注意：这是一个简化版本，仅适用于2目标
     * 对于多目标(M>2)，建议使用专门的超体积计算库
     * 
     * @param front Pareto前沿
     * @param referencePoint 参考点（通常为nadir点）
     * @return 超体积值
     */
    public static double calculateHypervolume2D(List<MOIndividual> front, double[] referencePoint) {
        if (front.isEmpty() || front.get(0).objectives.length != 2) {
            return 0.0;
        }
        
        // 按第一个目标排序
        List<MOIndividual> sorted = new ArrayList<>(front);
        sorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        double hypervolume = 0.0;
        double prevY = referencePoint[1];
        //
        for (MOIndividual ind : sorted) {
            double x = referencePoint[0] - ind.objectives[0];
            double y = prevY - ind.objectives[1];
            
            if (x > 0 && y > 0) {
                hypervolume += x * y;
                prevY = ind.objectives[1];
            }
        }
        
        return hypervolume;
    }
    
    // ==================== 交叉和变异操作（来自chapter-2） ====================
    
    /**
     * 交叉操作（主函数）
     * 
     * 对两个个体的染色体进行交叉操作：
     * - 打印段（前jobCount个）：OS和MS同步进行POX或JBX交叉
     * - 离散段（jobCount之后）：只对OS进行POX或JBX交叉，MS保持不变
     * 
     * @param ind1 个体1
     * @param ind2 个体2
     */
    public void crossover(MOIndividual ind1, MOIndividual ind2) {
        // 清空装箱结果
        ind1.printSolution = null;
        ind2.printSolution = null;
        
        int jobCount = problem.getJobCount();
        
        // 随机选择交叉类型
        double crossoverType = random.nextDouble();
        if (crossoverType < 0.5) {
            // 50% 使用POX交叉
            operSeqCrossoverPOX_PrintSync(ind1.gene_OS, ind1.gene_MS, ind2.gene_OS, ind2.gene_MS);
            operSeqCrossoverPOX_DiscreteOnly(ind1.gene_OS, ind2.gene_OS, jobCount);
        } else {
            // 50% 使用JBX交叉
            operSeqCrossoverJBX_PrintSync(ind1.gene_OS, ind1.gene_MS, ind2.gene_OS, ind2.gene_MS);
            operSeqCrossoverJBX_DiscreteOnly(ind1.gene_OS, ind2.gene_OS, jobCount);
        }
        
        // 修复可能出现的无效机器分配
        fixInvalidMachineAssignments(ind1);
        fixInvalidMachineAssignments(ind2);
    }
    
    /**
     * 变异操作（主函数）
     * 
     * 对个体的染色体进行变异操作：
     * - 生成多个变异候选（3-5个）
     * - 每个候选随机选择一种变异类型
     * - 选择最优的候选
     * 
     * @param individual 个体
     * @param operationMatrix 工序矩阵（用于评估fitness）
     */
    public void mutation(MOIndividual individual, Operation[][] operationMatrix, Boolean flag) {
        individual.printSolution = null;
        int numCandidates;
        // 生成多个变异候选（3-5个）
        if (flag) {
            numCandidates = 5 + random.nextInt(3);
        } else {
            numCandidates = 1;
        }
        // 初始化最优候选
        MOIndividual bestCandidate = null;
        double bestFitness = Double.MAX_VALUE;
        
        // 生成并评估多个变异候选
        for (int i = 0; i < numCandidates; i++) {
            // 复制个体
            MOIndividual candidate = new MOIndividual(individual);
            
            // 随机选择变异类型：
            // 0=打印段OS交换, 1=离散段OS交换, 2=MS机器重分配, 3=同机器打印件优化
            int mutationType = random.nextInt(4);
            
            try {
                switch (mutationType) {
                    case 0:
                        mutatePrintSequenceSwap(candidate);
                        break;
                    case 1:
                        mutateDiscreteSequenceSwap(candidate);
                        break;
                    case 2:
                        mutateMachineAssignment(candidate);
                        break;
                    case 3:
                        mutatePrintSameMachineOptimization(candidate);
                        break;
                }
                
                // 修复可能的无效机器分配
                fixInvalidMachineAssignments(candidate);
                
                // 评估候选的fitness（转换为Chromosome评估）
                Chromosome tempChromosome = candidate.toChromosome();
                double candidateFitness = fitnessCalculator.evaluate(tempChromosome, problem, operationMatrix);
                
                // 更新最优候选
                if (candidateFitness < bestFitness) {
                    bestFitness = candidateFitness;
                    bestCandidate = candidate;
                }
            } catch (Exception e) {
                // 如果变异失败，跳过这个候选
                 System.err.println("⚠️ 变异候选" + i + "生成失败: " + e.getMessage());
            }
        }
        
        // 选择最优的候选
        if (bestCandidate != null) {
            individual.gene_OS = bestCandidate.gene_OS;
            individual.gene_MS = bestCandidate.gene_MS;
            individual.printSolution = null;
        }
    }
    
    // ==================== 具体的交叉算子 ====================
    
    /**
     * 打印段同步POX交叉（OS和MS同步）
     */
    private void operSeqCrossoverPOX_PrintSync(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = problem.getJobCount();
        
        // 随机分配工件集
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp, random);
        int len1 = random.nextInt(jobCount) + 1;
        List<Integer> jobSet1 = temp.subList(0, len1);
        
        // 备份打印段
        int[] p1 = Arrays.copyOf(o1, jobCount);
        int[] p2 = Arrays.copyOf(o2, jobCount);
        int[] pm1 = Arrays.copyOf(m1, jobCount);
        int[] pm2 = Arrays.copyOf(m2, jobCount);
        
        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);
        
        // 从p1中选jobSet1的工件放入o1
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
                m1[i] = pm1[i];
                p1[i] = -1;
                pm1[i] = -1;
            }
            if (jobSet1.contains(p2[i])) {
                o2[i] = p2[i];
                m2[i] = pm2[i];
                p2[i] = -1;
                pm2[i] = -1;
            }
        }
        
        // 填充剩余位置
        int index1 = 0, index2 = 0;
        for (int i = 0; i < jobCount; i++) {
            if (o2[i] == -1) {
                while (p1[index1] == -1) index1++;
                o2[i] = p1[index1];
                m2[i] = pm1[index1];
                index1++;
            }
            if (o1[i] == -1) {
                while (p2[index2] == -1) index2++;
                o1[i] = p2[index2];
                m1[i] = pm2[index2];
                index2++;
            }
        }
    }
    
    /**
     * 打印段同步JBX交叉（OS和MS同步）
     */
    private void operSeqCrossoverJBX_PrintSync(int[] o1, int[] m1, int[] o2, int[] m2) {
        int jobCount = problem.getJobCount();

        if (jobCount < 2) {
            return;  // 工件数太少，无法交叉
        }

        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + random.nextInt(jobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);

        // ⚠️ 调试输出
//        System.out.println("  [JBX打印段] jobSet1=" + jobSet1 + ", jobCount=" + jobCount);
//        System.out.println("    交叉前-父代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉前-父代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));

        // 备份打印段
        int[] p1 = new int[jobCount];
        int[] p2 = new int[jobCount];
        int[] pm1 = new int[jobCount];
        int[] pm2 = new int[jobCount];
        System.arraycopy(o1, 0, p1, 0, jobCount);
        System.arraycopy(o2, 0, p2, 0, jobCount);
        System.arraycopy(m1, 0, pm1, 0, jobCount);
        System.arraycopy(m2, 0, pm2, 0, jobCount);

        // 清空打印段
        Arrays.fill(o1, 0, jobCount, -1);
        Arrays.fill(o2, 0, jobCount, -1);
        Arrays.fill(m1, 0, jobCount, -1);
        Arrays.fill(m2, 0, jobCount, -1);

        // JBX: 保留jobSet1的工件在原位置，其他位置用另一个父代的非jobSet1工件填充

        // 步骤1：保留jobSet1的工件在原位置
        for (int i = 0; i < jobCount; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
                m1[i] = pm1[i];
            }
            if (jobSet1.contains(p2[i])) {
                o2[i] = p2[i];
                m2[i] = pm2[i];
            }
        }

        // 步骤2：收集非jobSet1的工件（用于填充）
        List<Integer> nonJobSet1_p1 = new ArrayList<>();  // 父代1中非jobSet1的工件
        List<Integer> nonJobSet1_p1_machines = new ArrayList<>();
        List<Integer> nonJobSet1_p2 = new ArrayList<>();  // 父代2中非jobSet1的工件
        List<Integer> nonJobSet1_p2_machines = new ArrayList<>();

        for (int i = 0; i < jobCount; i++) {
            if (!jobSet1.contains(p1[i])) {
                nonJobSet1_p1.add(p1[i]);
                nonJobSet1_p1_machines.add(pm1[i]);
            }
            if (!jobSet1.contains(p2[i])) {
                nonJobSet1_p2.add(p2[i]);
                nonJobSet1_p2_machines.add(pm2[i]);
            }
        }

        // 步骤3：填充空位
        int index1 = 0;  // 用于填充o1的索引（从nonJobSet1_p2取）
        int index2 = 0;  // 用于填充o2的索引（从nonJobSet1_p1取）

        for (int i = 0; i < jobCount; i++) {
            // 填充o1：用父代2的非jobSet1工件
            if (o1[i] == -1) {
                if (index2 < nonJobSet1_p2.size()) {
                    o1[i] = nonJobSet1_p2.get(index2);
                    m1[i] = nonJobSet1_p2_machines.get(index2);
                    index2++;
                }
            }
            // 填充o2：用父代1的非jobSet1工件
            if (o2[i] == -1) {
                if (index1 < nonJobSet1_p1.size()) {
                    o2[i] = nonJobSet1_p1.get(index1);
                    m2[i] = nonJobSet1_p1_machines.get(index1);
                    index1++;
                }
            }
        }

        // ⚠️ 调试输出
//        System.out.println("    交叉后-子代1打印段OS: " + Arrays.toString(Arrays.copyOfRange(o1, 0, jobCount)));
//        System.out.println("    交叉后-子代2打印段OS: " + Arrays.toString(Arrays.copyOfRange(o2, 0, jobCount)));
    }
    
    /**
     * 离散段POX交叉（只交叉OS）
     */
    private void operSeqCrossoverPOX_DiscreteOnly(int[] o1, int[] o2, int jobCount) {
        int totalLen = o1.length;
        int discreteLen = totalLen - jobCount;
        
        if (discreteLen <= 1) return;
        
        // 随机选择工件集
        ArrayList<Integer> allJobs = new ArrayList<>();
        for (int i = 0; i < problem.getJobCount(); i++) {
            allJobs.add(i);
        }
        Collections.shuffle(allJobs, random);
        int setSize = 1 + random.nextInt(problem.getJobCount());
        Set<Integer> jobSet = new HashSet<>(allJobs.subList(0, setSize));
        
        // 备份离散段
        int[] p1 = new int[discreteLen];
        int[] p2 = new int[discreteLen];
        System.arraycopy(o1, jobCount, p1, 0, discreteLen);
        System.arraycopy(o2, jobCount, p2, 0, discreteLen);
        
        // 清空离散段
        Arrays.fill(o1, jobCount, totalLen, -1);
        Arrays.fill(o2, jobCount, totalLen, -1);
        
        // 从p1中选jobSet的工件放入o1
        for (int i = 0; i < discreteLen; i++) {
            if (jobSet.contains(p1[i])) {
                o1[jobCount + i] = p1[i];
                p1[i] = -1;
            }
            if (jobSet.contains(p2[i])) {
                o2[jobCount + i] = p2[i];
                p2[i] = -1;
            }
        }
        
        // 填充剩余位置
        int index1 = 0, index2 = 0;
        for (int i = 0; i < discreteLen; i++) {
            if (o2[jobCount + i] == -1) {
                while (p1[index1] == -1) index1++;
                o2[jobCount + i] = p1[index1];
                index1++;
            }
            if (o1[jobCount + i] == -1) {
                while (p2[index2] == -1) index2++;
                o1[jobCount + i] = p2[index2];
                index2++;
            }
        }
    }
    
    /**
     * 离散段JBX交叉（只交叉OS）
     */
    private void operSeqCrossoverJBX_DiscreteOnly(int[] o1, int[] o2, int jobCount) {
        int totalLen = o1.length;
        int discreteLen = totalLen - jobCount;

        if (discreteLen <= 0) {
            return;
        }

        int totalJobCount = problem.getJobCount();
        if (totalJobCount < 2) {
            return;  // 工件数太少，无法交叉
        }

        // 随机分配工件集（至少1个，最多jobCount-1个，确保两个父代都有贡献）
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < totalJobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = 1 + random.nextInt(totalJobCount - 1);  // [1, jobCount-1]
        List<Integer> jobSet1 = temp.subList(0, len1);

        // 备份离散段
        int[] p1 = new int[discreteLen];
        int[] p2 = new int[discreteLen];
        System.arraycopy(o1, jobCount, p1, 0, discreteLen);
        System.arraycopy(o2, jobCount, p2, 0, discreteLen);

        // 清空离散段
        Arrays.fill(o1, jobCount, totalLen, -1);
        Arrays.fill(o2, jobCount, totalLen, -1);

        // JBX: 保留jobSet1的工件在原位置，其他位置用另一个父代的非jobSet1工件填充

        // 步骤1：保留jobSet1的工件在原位置
        for (int i = 0; i < discreteLen; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[jobCount + i] = p1[i];
            }
            if (jobSet1.contains(p2[i])) {
                o2[jobCount + i] = p2[i];
            }
        }

        // 步骤2：收集非jobSet1的工件（用于填充）
        List<Integer> nonJobSet1_p1 = new ArrayList<>();  // 父代1中非jobSet1的工件
        List<Integer> nonJobSet1_p2 = new ArrayList<>();  // 父代2中非jobSet1的工件

        for (int i = 0; i < discreteLen; i++) {
            if (!jobSet1.contains(p1[i])) {
                nonJobSet1_p1.add(p1[i]);
            }
            if (!jobSet1.contains(p2[i])) {
                nonJobSet1_p2.add(p2[i]);
            }
        }

        // 步骤3：填充空位
        int index1 = 0;  // 用于填充o1的索引（从nonJobSet1_p2取）
        int index2 = 0;  // 用于填充o2的索引（从nonJobSet1_p1取）

        for (int i = 0; i < discreteLen; i++) {
            // 填充o1：用父代2的非jobSet1工件
            if (o1[jobCount + i] == -1) {
                if (index2 < nonJobSet1_p2.size()) {
                    o1[jobCount + i] = nonJobSet1_p2.get(index2);
                    index2++;
                }
            }
            // 填充o2：用父代1的非jobSet1工件
            if (o2[jobCount + i] == -1) {
                if (index1 < nonJobSet1_p1.size()) {
                    o2[jobCount + i] = nonJobSet1_p1.get(index1);
                    index1++;
                }
            }
        }
    }
    
    // ==================== 具体的变异算子 ====================
    
    /**
     * 打印段OS交换变异：随机选择两个位置，同时交换OS和MS
     */
    private void mutatePrintSequenceSwap(MOIndividual individual) {
        int jobCount = problem.getJobCount();
        
        if (jobCount < 2) return;
        
        // 随机选择两个不同的位置
        int pos1 = random.nextInt(jobCount);
        int pos2 = random.nextInt(jobCount);
        while (pos2 == pos1) {
            pos2 = random.nextInt(jobCount);
        }
        
        // 同时交换OS和MS
        int tempOS = individual.gene_OS[pos1];
        individual.gene_OS[pos1] = individual.gene_OS[pos2];
        individual.gene_OS[pos2] = tempOS;
        
        int tempMS = individual.gene_MS[pos1];
        individual.gene_MS[pos1] = individual.gene_MS[pos2];
        individual.gene_MS[pos2] = tempMS;
    }
    
    /**
     * 离散段OS交换变异：随机选择两个位置，只交换OS
     */
    private void mutateDiscreteSequenceSwap(MOIndividual individual) {
        int jobCount = problem.getJobCount();
        int totalLen = individual.gene_OS.length;
        int discreteLen = totalLen - jobCount;
        
        if (discreteLen < 2) return;
        
        // 在离散段随机选择两个不同的位置
        int pos1 = jobCount + random.nextInt(discreteLen);
        int pos2 = jobCount + random.nextInt(discreteLen);
        while (pos2 == pos1) {
            pos2 = jobCount + random.nextInt(discreteLen);
        }
        
        // 只交换OS
        int tempOS = individual.gene_OS[pos1];
        individual.gene_OS[pos1] = individual.gene_OS[pos2];
        individual.gene_OS[pos2] = tempOS;
    }
    
    /**
     * MS机器重分配变异：随机选择几个位置重新分配机器
     */
    private void mutateMachineAssignment(MOIndividual individual) {
        int jobCount = problem.getJobCount();
        int totalLen = individual.gene_MS.length;
        
        // 随机选择变异点数（1-3个）
        int numMutations = 1 + random.nextInt(3);
        
        for (int i = 0; i < numMutations; i++) {
            int pos = random.nextInt(totalLen);
            
            if (pos < jobCount) {
                // 打印段：重新分配打印机
                int jobNo = individual.gene_OS[pos];
                Item item = problem.getItems()[jobNo];
                Machine[] machines = problem.getMachines();
                int printMachineCount = problem.getPrintMachineCount();
                
                // 找到能容纳该零件的打印机
                List<Integer> suitablePrinters = new ArrayList<>();
                for (int m = 0; m < printMachineCount; m++) {
                    PrintMachine pm = (PrintMachine) machines[m];
                    boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                    boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                    if (fitsNormal || fitsRotated) {
                        suitablePrinters.add(m + 1);
                    }
                }
                
                if (!suitablePrinters.isEmpty()) {
                    individual.gene_MS[pos] = suitablePrinters.get(random.nextInt(suitablePrinters.size()));
                }
            } else {
                // 离散段：重新分配相对索引
                // 需要找到对应的工序
                int discretePos = pos - jobCount;
                int[] operationCountArr = problem.getOperationCountArr();
                
                // 找到该位置对应的工件和工序
                int currentPos = 0;
                for (int jobNo = 0; jobNo < jobCount; jobNo++) {
                    int discreteOpsCount = operationCountArr[jobNo] - 2;
                    if (discretePos < currentPos + discreteOpsCount) {
                        int localOperNo = discretePos - currentPos;
                        int operNo = 2 + localOperNo;
                        
                        // 获取可用机器数量
                        int[][] operationToIndex = problem.getOperationToIndex();
                        int operIdx = operationToIndex[jobNo][operNo];
                        double[][] proDesMatrix = problem.getProDesMatrix();
                        
                        int availableCount = 0;
                        for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                            if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                                availableCount++;
                            }
                        }
                        
                        if (availableCount > 0) {
                            individual.gene_MS[pos] = 1 + random.nextInt(availableCount);
                        }
                        break;
                    }
                    currentPos += discreteOpsCount;
                }
            }
        }
    }
    
    /**
     * 同机器打印件优化变异：针对同一台打印机上的打印件进行局部优化
     */
    private void mutatePrintSameMachineOptimization(MOIndividual individual) {
        int jobCount = problem.getJobCount();
        
        if (jobCount < 2) return;
        
        // 随机选择一台打印机
        int targetMachine = 1 + random.nextInt(problem.getPrintMachineCount());
        
        // 找到该打印机上的所有工件位置
        List<Integer> positions = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            if (individual.gene_MS[i] == targetMachine) {
                positions.add(i);
            }
        }
        
        if (positions.size() < 2) {
            // 该打印机上工件太少，使用普通变异
            mutatePrintSequenceSwap(individual);
            return;
        }
        
        // 随机选择一种操作
        int opType = random.nextInt(3);
        
        if (opType == 0) {
            // 交换两个工件
            int idx1 = random.nextInt(positions.size());
            int idx2 = random.nextInt(positions.size());
            while (idx2 == idx1) idx2 = random.nextInt(positions.size());
            
            int pos1 = positions.get(idx1);
            int pos2 = positions.get(idx2);
            
            int tempOS = individual.gene_OS[pos1];
            individual.gene_OS[pos1] = individual.gene_OS[pos2];
            individual.gene_OS[pos2] = tempOS;
            
            int tempMS = individual.gene_MS[pos1];
            individual.gene_MS[pos1] = individual.gene_MS[pos2];
            individual.gene_MS[pos2] = tempMS;
        } else if (opType == 1 && positions.size() >= 3) {
            // 2-opt: 反转一段子序列
            int idx1 = random.nextInt(positions.size());
            int idx2 = random.nextInt(positions.size());
            if (idx1 > idx2) {
                int temp = idx1;
                idx1 = idx2;
                idx2 = temp;
            }
            
            while (idx1 < idx2) {
                int pos1 = positions.get(idx1);
                int pos2 = positions.get(idx2);
                
                int tempOS = individual.gene_OS[pos1];
                individual.gene_OS[pos1] = individual.gene_OS[pos2];
                individual.gene_OS[pos2] = tempOS;
                
                int tempMS = individual.gene_MS[pos1];
                individual.gene_MS[pos1] = individual.gene_MS[pos2];
                individual.gene_MS[pos2] = tempMS;
                
                idx1++;
                idx2--;
            }
        } else {
            // 插入: 移除一个工件，插入到另一位置
            int removeIdx = random.nextInt(positions.size());
            int insertIdx = random.nextInt(positions.size());
            
            if (removeIdx != insertIdx) {
                int removePos = positions.get(removeIdx);
                int insertPos = positions.get(insertIdx);
                
                int tempOS = individual.gene_OS[removePos];
                int tempMS = individual.gene_MS[removePos];
                
                if (removePos < insertPos) {
                    // 向右移动
                    System.arraycopy(individual.gene_OS, removePos + 1, individual.gene_OS, removePos, insertPos - removePos);
                    System.arraycopy(individual.gene_MS, removePos + 1, individual.gene_MS, removePos, insertPos - removePos);
                    individual.gene_OS[insertPos] = tempOS;
                    individual.gene_MS[insertPos] = tempMS;
                } else {
                    // 向左移动
                    System.arraycopy(individual.gene_OS, insertPos, individual.gene_OS, insertPos + 1, removePos - insertPos);
                    System.arraycopy(individual.gene_MS, insertPos, individual.gene_MS, insertPos + 1, removePos - insertPos);
                    individual.gene_OS[insertPos] = tempOS;
                    individual.gene_MS[insertPos] = tempMS;
                }
            }
        }
    }
    
    // ==================== 修复方法 ====================
    
    /**
     * 修复染色体中无效的机器分配
     */
    private void fixInvalidMachineAssignments(MOIndividual individual) {
        int[] os = individual.gene_OS;
        int[] ms = individual.gene_MS;
        int jobCount = problem.getJobCount();
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        int[] operationCountArr = problem.getOperationCountArr();
        
        // 检查离散工序段
        int msIndex = jobCount;
        
        for (int jobNo = 0; jobNo < jobCount; jobNo++) {
            int discreteOpsCount = operationCountArr[jobNo] - 2;
            
            for (int localOperNo = 0; localOperNo < discreteOpsCount; localOperNo++) {
                int operNo = 2 + localOperNo;
                int operIdx = operationToIndex[jobNo][operNo];
                
                // 找到所有可用机器
                List<Integer> availableMachines = new ArrayList<>();
                for (int k = 0; k < proDesMatrix[operIdx].length; k++) {
                    if (proDesMatrix[operIdx][k] > 0 && proDesMatrix[operIdx][k] != Double.MAX_VALUE) {
                        availableMachines.add(k + 1);
                    }
                }
                
                if (availableMachines.isEmpty()) {
                    msIndex++;
                    continue;
                }
                
                // 检查当前相对索引是否有效
                int currentRelativeIndex = ms[msIndex];
                
                if (currentRelativeIndex < 1 || currentRelativeIndex > availableMachines.size()) {
                    int newRelativeIndex = 1 + random.nextInt(availableMachines.size());
                    ms[msIndex] = newRelativeIndex;
                }
                
                msIndex++;
            }
        }
        
        // 检查打印工序段
        int printMachineCount = problem.getPrintMachineCount();
        Item[] items = problem.getItems();
        Machine[] machines = problem.getMachines();
        
        for (int i = 0; i < jobCount; i++) {
            int jobNo = os[i];
            
            if (jobNo < 0 || jobNo >= jobCount) {
                jobNo = random.nextInt(jobCount);
                os[i] = jobNo;
            }
            
            int machineNo = ms[i];
            
            if (machineNo < 1 || machineNo > printMachineCount) {
                Item item = items[jobNo];
                List<Integer> suitablePrinters = new ArrayList<>();
                for (int m = 0; m < printMachineCount; m++) {
                    PrintMachine pm = (PrintMachine) machines[m];
                    boolean fitsNormal = (item.l <= pm.L && item.w <= pm.W && item.h <= pm.H);
                    boolean fitsRotated = (item.w <= pm.L && item.l <= pm.W && item.h <= pm.H);
                    if (fitsNormal || fitsRotated) {
                        suitablePrinters.add(m + 1);
                    }
                }
                
                if (!suitablePrinters.isEmpty()) {
                    ms[i] = suitablePrinters.get(random.nextInt(suitablePrinters.size()));
                } else {
                    ms[i] = 1;  // 默认第一台打印机
                }
            }
        }
    }
}

