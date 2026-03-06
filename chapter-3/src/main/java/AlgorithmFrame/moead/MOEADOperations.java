package AlgorithmFrame.moead;

import ProblemFrame.*;
import AlgorithmFrame.nsgaii.NSGAIIOperations;
import ProgramEntity.*;
import java.util.*;

/**
 * MOEA/D 核心算法组件
 * 
 * 包含：
 * - 权重向量生成
 * - 聚合函数（Tchebycheff、Weighted Sum、PBI）
 * - 邻域更新策略
 * - 交叉和变异操作（复用NSGA-II）
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOEADOperations {
    
    // 依赖
    private Problem problem;
    private Random random;
    private NSGAIIOperations nsgaiiOps;  // 复用NSGA-II的交叉变异算子
    
    /**
     * 聚合函数类型
     */
    public enum ScalarizingFunction {
        /** Tchebycheff方法 (推荐) */
        TCHEBYCHEFF,
        
        /** 加权和方法 */
        WEIGHTED_SUM,
        
        /** PBI (Penalty-based Boundary Intersection) */
        PBI
    }
    
    /**
     * 构造函数
     */
    public MOEADOperations(Problem problem, Random random) {
        this.problem = problem;
        this.random = random;
        this.nsgaiiOps = new NSGAIIOperations(problem, random);
    }
    
    // ==================== 聚合函数 ====================
    
    /**
     * 计算聚合函数值（标量化）
     * 
     * @param objectives 目标值数组
     * @param weights 权重向量
     * @param idealPoint 理想点
     * @param function 聚合函数类型
     * @return 标量化值（越小越好）
     */
    public double calculateScalarValue(double[] objectives, double[] weights, 
                                      double[] idealPoint, ScalarizingFunction function) {
        switch (function) {
            case TCHEBYCHEFF:
                return tchebycheff(objectives, weights, idealPoint);
            case WEIGHTED_SUM:
                return weightedSum(objectives, weights, idealPoint);
            case PBI:
                return pbi(objectives, weights, idealPoint, 5.0);  // theta=5.0是常用值
            default:
                return tchebycheff(objectives, weights, idealPoint);
        }
    }
    
    /**
     * Tchebycheff聚合函数（推荐）
     * 
     * g^te(x|λ,z*) = max_{i=1..m} { λ_i * |f_i(x) - z*_i| }
     * 
     * 优点：
     * - 可以找到凹、凸、混合的Pareto前沿
     * - 对权重分布不敏感
     * 
     * @param objectives 目标值数组 f(x)
     * @param weights 权重向量 λ
     * @param idealPoint 理想点 z*
     * @return Tchebycheff值（越小越好）
     */
    private double tchebycheff(double[] objectives, double[] weights, double[] idealPoint) {
        double maxValue = Double.NEGATIVE_INFINITY;
        
        for (int i = 0; i < objectives.length; i++) {
            // 避免权重为0导致的问题
            double weight = Math.max(weights[i], 1e-6);
            double value = weight * Math.abs(objectives[i] - idealPoint[i]);
            maxValue = Math.max(maxValue, value);
        }
        
        return maxValue;
    }
    
    /**
     * 加权和聚合函数
     * 
     * g^ws(x|λ,z*) = Σ λ_i * (f_i(x) - z*_i)
     * 
     * 优点：
     * - 计算简单
     * 
     * 缺点：
     * - 对于非凸Pareto前沿，无法找到凹陷部分的解
     * 
     * @param objectives 目标值数组 f(x)
     * @param weights 权重向量 λ
     * @param idealPoint 理想点 z*
     * @return 加权和值（越小越好）
     */
    private double weightedSum(double[] objectives, double[] weights, double[] idealPoint) {
        double sum = 0.0;
        
        for (int i = 0; i < objectives.length; i++) {
            sum += weights[i] * (objectives[i] - idealPoint[i]);
        }
        
        return sum;
    }
    
    /**
     * PBI (Penalty-based Boundary Intersection) 聚合函数
     * 
     * g^pbi(x|λ,z*,θ) = d1 + θ * d2
     * 
     * 其中：
     * - d1 = (f(x) - z*) · λ / ||λ||  (沿权重方向的距离)
     * - d2 = ||f(x) - z* - d1 * λ/||λ|| ||  (垂直于权重方向的距离)
     * - θ 是惩罚参数（通常为5.0）
     * 
     * 优点：
     * - 可以找到凹凸Pareto前沿
     * - 比Tchebycheff收敛性更好
     * 
     * @param objectives 目标值数组 f(x)
     * @param weights 权重向量 λ
     * @param idealPoint 理想点 z*
     * @param theta 惩罚参数
     * @return PBI值（越小越好）
     */
    private double pbi(double[] objectives, double[] weights, double[] idealPoint, double theta) {
        int m = objectives.length;
        
        // 计算 f(x) - z*
        double[] diff = new double[m];
        for (int i = 0; i < m; i++) {
            diff[i] = objectives[i] - idealPoint[i];
        }
        
        // 计算 ||λ||
        double normWeight = 0.0;
        for (double w : weights) {
            normWeight += w * w;
        }
        normWeight = Math.sqrt(normWeight);
        
        // 避免除零
        if (normWeight < 1e-10) {
            normWeight = 1.0;
        }
        
        // 计算 d1 = (f(x) - z*) · λ / ||λ||
        double d1 = 0.0;
        for (int i = 0; i < m; i++) {
            d1 += diff[i] * weights[i];
        }
        d1 /= normWeight;
        
        // 计算 d2 = ||f(x) - z* - d1 * λ/||λ|| ||
        double d2 = 0.0;
        for (int i = 0; i < m; i++) {
            double temp = diff[i] - d1 * weights[i] / normWeight;
            d2 += temp * temp;
        }
        d2 = Math.sqrt(d2);
        
        // g^pbi = d1 + θ * d2
        return d1 + theta * d2;
    }
    
    // ==================== 交叉和变异操作（复用NSGA-II） ====================
    
    /**
     * 交叉操作（复用NSGA-II的POX/JBX算子）
     */
    public void crossover(MOIndividual ind1, MOIndividual ind2) {
        nsgaiiOps.crossover(ind1, ind2);
    }
    
    /**
     * 变异操作（复用NSGA-II的多种变异算子）
     */
    public void mutation(MOIndividual individual, Operation[][] operationMatrix) {
        nsgaiiOps.mutation(individual, operationMatrix, false);
    }
    
    // ==================== 权重向量生成（工具方法） ====================
    
    /**
     * 生成均匀分布的权重向量（双目标）
     * 
     * @param N 权重向量数量
     * @return 权重向量数组 [N][2]
     */
    public static double[][] generateUniformWeights2D(int N) {
        double[][] weights = new double[N][2];
        
        for (int i = 0; i < N; i++) {
            double w1 = (double) i / (N - 1);
            weights[i][0] = w1;
            weights[i][1] = 1.0 - w1;
        }
        
        return weights;
    }
    
    /**
     * 生成Simplex-Lattice权重向量（三目标）
     * 
     * @param H 划分数（生成约 C(H+M-1, M-1) 个权重向量）
     * @return 权重向量列表
     */
    public static List<double[]> generateSimplexLatticeWeights3D(int H) {
        List<double[]> weights = new ArrayList<>();
        
        for (int i = 0; i <= H; i++) {
            for (int j = 0; j <= H - i; j++) {
                int k = H - i - j;
                double[] w = new double[3];
                w[0] = (double) i / H;
                w[1] = (double) j / H;
                w[2] = (double) k / H;
                weights.add(w);
            }
        }
        
        return weights;
    }
    
    /**
     * 生成随机归一化权重向量
     * 
     * @param N 权重向量数量
     * @param M 目标数量
     * @param random 随机数生成器
     * @return 权重向量数组 [N][M]
     */
    public static double[][] generateRandomWeights(int N, int M, Random random) {
        double[][] weights = new double[N][M];
        
        for (int i = 0; i < N; i++) {
            double sum = 0;
            for (int j = 0; j < M; j++) {
                weights[i][j] = random.nextDouble();
                sum += weights[i][j];
            }
            // 归一化
            for (int j = 0; j < M; j++) {
                weights[i][j] /= sum;
            }
        }
        
        return weights;
    }
    
    /**
     * 计算邻域结构（基于欧氏距离）
     * 
     * @param weights 权重向量数组 [N][M]
     * @param T 邻域大小
     * @return 邻域结构 [N][T]，每行包含该权重向量最近的T个邻居索引
     */
    public static int[][] computeNeighborhood(double[][] weights, int T) {
        int N = weights.length;
        int[][] neighborhoods = new int[N][T];
        
        for (int i = 0; i < N; i++) {
            // 计算与其他所有权重向量的距离
            double[] distances = new double[N];
            for (int j = 0; j < N; j++) {
                distances[j] = euclideanDistance(weights[i], weights[j]);
            }
            
            // 找到最近的T个权重向量（包括自己）
            Integer[] indices = new Integer[N];
            for (int j = 0; j < N; j++) {
                indices[j] = j;
            }
            
            // 按距离排序
            Arrays.sort(indices, Comparator.comparingDouble(idx -> distances[idx]));
            
            // 选择最近的T个
            for (int j = 0; j < T; j++) {
                neighborhoods[i][j] = indices[j];
            }
        }
        
        return neighborhoods;
    }
    
    /**
     * 计算欧氏距离
     */
    private static double euclideanDistance(double[] v1, double[] v2) {
        double sum = 0;
        for (int i = 0; i < v1.length; i++) {
            double diff = v1[i] - v2[i];
            sum += diff * diff;
        }
        return Math.sqrt(sum);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 打印权重向量
     */
    public static void printWeights(double[][] weights) {
        System.out.println("权重向量 (共" + weights.length + "个):");
        for (int i = 0; i < Math.min(10, weights.length); i++) {
            System.out.printf("  [%3d] ", i);
            for (double w : weights[i]) {
                System.out.printf("%.4f ", w);
            }
            System.out.println();
        }
        if (weights.length > 10) {
            System.out.println("  ...");
        }
    }
    
    /**
     * 打印邻域结构
     */
    public static void printNeighborhoods(int[][] neighborhoods) {
        System.out.println("邻域结构 (共" + neighborhoods.length + "个子问题):");
        for (int i = 0; i < Math.min(5, neighborhoods.length); i++) {
            System.out.printf("  子问题[%3d]的邻域: ", i);
            for (int j = 0; j < Math.min(10, neighborhoods[i].length); j++) {
                System.out.printf("%d ", neighborhoods[i][j]);
            }
            if (neighborhoods[i].length > 10) {
                System.out.print("...");
            }
            System.out.println();
        }
        if (neighborhoods.length > 5) {
            System.out.println("  ...");
        }
    }
}
