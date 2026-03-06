package AlgorithmFrame.spea2;

import ProblemFrame.*;
import AlgorithmFrame.nsgaii.NSGAIIOperations;
import ProgramEntity.*;
import java.util.*;

/**
 * SPEA2 核心算法组件
 * 
 * 包含：
 * 1. 强度值计算 (Strength Calculation)
 * 2. 适应度分配 (Fitness Assignment)
 * 3. k近邻密度估计 (k-th Nearest Neighbor Density Estimation)
 * 4. 环境选择（存档截断） (Environmental Selection / Archive Truncation)
 * 5. 二元锦标赛选择 (Binary Tournament Selection)
 * 6. 交叉和变异操作（复用NSGAII的）
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SPEA2Operations {
    
    private NSGAIIOperations nsgaiiOperations;  // 复用NSGAII的交叉变异算子
    
    /**
     * 构造函数
     */
    public SPEA2Operations(Problem problem, Random random) {
        this.nsgaiiOperations = new NSGAIIOperations(problem, random);
    }
    
    // ==================== SPEA2核心算法 ====================
    
    /**
     * 计算强度值 (Strength)
     * 
     * S(i) = |{j | j ∈ P_t + P̄_t ∧ i ≻ j}|
     * 
     * 强度值 = 被该个体支配的个体数量
     * 
     * @param population 种群（P_t + P̄_t）
     * @return 强度值数组
     */
    private static int[] calculateStrength(List<MOIndividual> population) {
        int n = population.size();
        int[] strength = new int[n];
        
        for (int i = 0; i < n; i++) {
            MOIndividual pi = population.get(i);
            int count = 0;
            
            for (int j = 0; j < n; j++) {
                if (i != j && pi.dominates(population.get(j))) {
                    count++;
                }
            }
            
            strength[i] = count;
        }
        
        return strength;
    }
    
    /**
     * 计算原始适应度 (Raw Fitness)
     * 
     * R(i) = Σ S(j)  其中 j ≻ i
     * 
     * 原始适应度 = 所有支配该个体的个体的强度值之和
     * 
     * @param population 种群
     * @param strength 强度值数组
     * @return 原始适应度数组
     */
    private static double[] calculateRawFitness(List<MOIndividual> population, int[] strength) {
        int n = population.size();
        double[] rawFitness = new double[n];
        
        for (int i = 0; i < n; i++) {
            MOIndividual pi = population.get(i);
            double sum = 0.0;
            
            for (int j = 0; j < n; j++) {
                if (i != j && population.get(j).dominates(pi)) {
                    sum += strength[j];
                }
            }
            
            rawFitness[i] = sum;
        }
        
        return rawFitness;
    }
    
    /**
     * 计算k近邻密度 (k-th Nearest Neighbor Density)
     * 
     * D(i) = 1 / (σ_i^k + 2)
     * 
     * 其中 σ_i^k 是第i个个体到第k近邻的欧氏距离
     * 
     * @param population 种群
     * @param k k近邻参数
     * @return 密度值数组
     */
    private static double[] calculateDensity(List<MOIndividual> population, int k) {
        int n = population.size();
        double[] density = new double[n];
        
        // 确保k不超过种群大小-1
        k = Math.min(k, n - 1);
        if (k < 1) k = 1;
        
        for (int i = 0; i < n; i++) {
            MOIndividual pi = population.get(i);
            
            // 计算到所有其他个体的距离
            List<Double> distances = new ArrayList<>();
            
            for (int j = 0; j < n; j++) {
                if (i != j) {
                    double dist = euclideanDistance(pi, population.get(j));
                    distances.add(dist);
                }
            }
            
            // 排序找到第k近邻
            Collections.sort(distances);
            
            // σ_i^k = 第k近邻的距离
            double sigma_k = distances.get(k - 1);
            
            // D(i) = 1 / (σ_i^k + 2)
            density[i] = 1.0 / (sigma_k + 2.0);
        }
        
        return density;
    }
    
    /**
     * 计算两个个体在目标空间的欧氏距离
     */
    private static double euclideanDistance(MOIndividual a, MOIndividual b) {
        double sum = 0.0;
        for (int i = 0; i < a.objectives.length; i++) {
            double diff = a.objectives[i] - b.objectives[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    /**
     * 适应度分配 (Fitness Assignment)
     * 
     * F(i) = R(i) + D(i)
     * 
     * 最终适应度 = 原始适应度 + 密度值
     * 
     * 注意：适应度越小越好（最小化问题）
     * - 非支配解的 R(i) = 0
     * - D(i) 用于在非支配解之间进行区分
     * 
     * @param population 种群（P_t + P̄_t）
     * @param k k近邻参数
     */
    public static void assignFitness(List<MOIndividual> population, int k) {
        int n = population.size();
        
        if (n == 0) return;
        
        // 1. 计算强度值
        int[] strength = calculateStrength(population);
        
        // 2. 计算原始适应度
        double[] rawFitness = calculateRawFitness(population, strength);
        
        // 3. 计算密度值
        double[] density = calculateDensity(population, k);
        
        // 4. 计算最终适应度
        for (int i = 0; i < n; i++) {
            MOIndividual ind = population.get(i);
            
            // F(i) = R(i) + D(i)
            // 注意：这里将适应度存储在crowdingDistance字段中（复用）
            // 实际上SPEA2不使用crowdingDistance，而是使用自己的fitness
            ind.crowdingDistance = rawFitness[i] + density[i];
            
            // 也可以用rank字段存储原始适应度（用于调试）
            ind.rank = (int) rawFitness[i];
        }
    }
    
    /**
     * 环境选择（存档更新）
     * 
     * 从 P_t ∪ P̄_t 中选择 N̄ 个个体作为新的存档 P̄_{t+1}
     * 
     * 规则：
     * 1. 如果非支配解数量 < N̄：全部加入，用支配解填充
     * 2. 如果非支配解数量 = N̄：全部加入
     * 3. 如果非支配解数量 > N̄：使用截断算子
     * 
     * @param population 合并后的种群（P_t + P̄_t）
     * @param archiveSize 存档大小 N̄
     * @param k k近邻参数
     * @return 新的存档
     */
    public static List<MOIndividual> environmentalSelection(
            List<MOIndividual> population,
            int archiveSize,
            int k) {
        
        if (population.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 1. 分离非支配解和支配解
        List<MOIndividual> nonDominated = new ArrayList<>();
        List<MOIndividual> dominated = new ArrayList<>();
        
        for (MOIndividual ind : population) {
            // R(i) = 0 表示非支配解
            if (ind.rank == 0) {
                nonDominated.add(ind);
            } else {
                dominated.add(ind);
            }
        }
        
        List<MOIndividual> archive = new ArrayList<>();
        
        // 2. 情况1：非支配解数量 < N̄
        if (nonDominated.size() < archiveSize) {
            // 全部非支配解加入存档
            archive.addAll(nonDominated);
            
            // 按适应度排序支配解
            dominated.sort(Comparator.comparingDouble(ind -> ind.crowdingDistance));
            
            // 用支配解填充剩余位置
            int remaining = archiveSize - nonDominated.size();
            for (int i = 0; i < remaining && i < dominated.size(); i++) {
                archive.add(dominated.get(i));
            }
        }
        // 3. 情况2：非支配解数量 = N̄
        else if (nonDominated.size() == archiveSize) {
            archive.addAll(nonDominated);
        }
        // 4. 情况3：非支配解数量 > N̄ （需要截断）
        else {
            archive = truncateArchive(nonDominated, archiveSize);
        }
        
        return archive;
    }
    
    /**
     * 存档截断算子 (Archive Truncation)
     * 
     * 当非支配解数量 > N̄ 时，迭代移除拥挤区域的个体
     * 
     * 迭代移除规则：
     * 1. 计算所有个体到其他个体的距离
     * 2. 找到最小距离最小的个体（最拥挤的个体）
     * 3. 移除该个体
     * 4. 重复直到剩余 N̄ 个个体
     * 
     * @param nonDominated 非支配解集合
     * @param targetSize 目标大小 N̄
     * @return 截断后的存档
     */
    private static List<MOIndividual> truncateArchive(
            List<MOIndividual> nonDominated,
            int targetSize) {
        
        List<MOIndividual> archive = new ArrayList<>(nonDominated);
        
        while (archive.size() > targetSize) {
            int n = archive.size();
            
            // 计算距离矩阵
            double[][] distances = new double[n][n];
            for (int i = 0; i < n; i++) {
                for (int j = i + 1; j < n; j++) {
                    double dist = euclideanDistance(archive.get(i), archive.get(j));
                    distances[i][j] = dist;
                    distances[j][i] = dist;
                }
            }
            
            // 找到最拥挤的个体（最小距离最小的个体）
            int mostCrowded = -1;
            double minDistance = Double.POSITIVE_INFINITY;
            
            for (int i = 0; i < n; i++) {
                // 找到该个体到其他个体的最小距离
                double minDist = Double.POSITIVE_INFINITY;
                for (int j = 0; j < n; j++) {
                    if (i != j) {
                        minDist = Math.min(minDist, distances[i][j]);
                    }
                }
                
                // 如果这个最小距离更小，更新最拥挤个体
                if (minDist < minDistance) {
                    minDistance = minDist;
                    mostCrowded = i;
                } else if (minDist == minDistance && mostCrowded != -1) {
                    // 如果最小距离相同，比较第二小的距离
                    double secondMin_i = getSecondMinDistance(distances[i], n);
                    double secondMin_mostCrowded = getSecondMinDistance(distances[mostCrowded], n);
                    
                    if (secondMin_i < secondMin_mostCrowded) {
                        mostCrowded = i;
                    }
                }
            }
            
            // 移除最拥挤的个体
            if (mostCrowded != -1) {
                archive.remove(mostCrowded);
            } else {
                // 安全检查：如果找不到，随机移除一个
                archive.remove(0);
            }
        }
        
        return archive;
    }
    
    /**
     * 获取距离数组中第二小的距离
     */
    private static double getSecondMinDistance(double[] distances, int n) {
        double min = Double.POSITIVE_INFINITY;
        double secondMin = Double.POSITIVE_INFINITY;
        
        for (int i = 0; i < n; i++) {
            if (distances[i] > 0) {  // 排除自己（距离为0）
                if (distances[i] < min) {
                    secondMin = min;
                    min = distances[i];
                } else if (distances[i] < secondMin) {
                    secondMin = distances[i];
                }
            }
        }
        
        return secondMin;
    }
    
    /**
     * 二元锦标赛选择（SPEA2版本）
     * 
     * 从存档中选择 populationSize 个个体作为父代
     * 
     * 选择规则：
     * - 随机选择两个个体
     * - 适应度更小的获胜
     * 
     * @param archive 存档
     * @param count 选择数量
     * @param random 随机数生成器
     * @return 选中的个体列表
     */
    public static List<MOIndividual> binaryTournamentSelection(
            List<MOIndividual> archive,
            int count,
            Random random) {
        
        List<MOIndividual> selected = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            // 随机选择两个个体
            int idx1 = random.nextInt(archive.size());
            int idx2 = random.nextInt(archive.size());
            while (idx2 == idx1 && archive.size() > 1) {
                idx2 = random.nextInt(archive.size());
            }
            
            MOIndividual ind1 = archive.get(idx1);
            MOIndividual ind2 = archive.get(idx2);
            
            // 适应度更小的获胜
            if (ind1.crowdingDistance < ind2.crowdingDistance) {
                selected.add(new MOIndividual(ind1));
            } else if (ind2.crowdingDistance < ind1.crowdingDistance) {
                selected.add(new MOIndividual(ind2));
            } else {
                // 适应度相同，随机选择
                selected.add(new MOIndividual(random.nextBoolean() ? ind1 : ind2));
            }
        }
        
        return selected;
    }
    
    /**
     * 提取非支配解
     * 
     * @param population 种群
     * @return 非支配解集合
     */
    public static List<MOIndividual> extractNonDominatedSolutions(List<MOIndividual> population) {
        List<MOIndividual> nonDominated = new ArrayList<>();
        
        for (MOIndividual ind : population) {
            boolean isDominated = false;
            
            for (MOIndividual other : population) {
                if (ind != other && other.dominates(ind)) {
                    isDominated = true;
                    break;
                }
            }
            
            if (!isDominated) {
                nonDominated.add(ind);
            }
        }
        
        return nonDominated;
    }
    
    /**
     * 计算超体积指标（Hypervolume）- 2目标版本
     * 
     * 复用NSGAII的实现
     */
    public static double calculateHypervolume2D(List<MOIndividual> front, double[] referencePoint) {
        return AlgorithmFrame.nsgaii.NSGAIIOperations.calculateHypervolume2D(front, referencePoint);
    }
    
    // ==================== 交叉和变异操作（复用NSGAII） ====================
    
    /**
     * 交叉操作
     */
    public void crossover(MOIndividual ind1, MOIndividual ind2) {
        nsgaiiOperations.crossover(ind1, ind2);
    }
    
    /**
     * 变异操作
     */
    public void mutation(MOIndividual individual, Operation[][] operationMatrix) {
        nsgaiiOperations.mutation(individual, operationMatrix, false);
    }
}
