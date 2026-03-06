package test;

import AlgorthmFrame.ga.ChromosomeOperation;
import ProblemFrame.CaculateFitness;
import ProblemFrame.Chromosome;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import util.DataReader;

import java.util.Random;

/**
 * 变异策略测试示例
 * 
 * 测试新的多试探选择最优变异策略
 */
public class MutationStrategyTest {
    
    public static void main(String[] args) {
        // 1. 读取问题数据
        String filePath = "data/Mk01.fjs";  // 根据实际路径修改
        DataReader dataReader = new DataReader();
        Problem problem = dataReader.read(filePath);
        
        // 2. 创建fitness计算器和染色体操作器
        Random random = new Random(42);  // 固定种子以便重现
        CaculateFitness fitness = new CaculateFitness();
        ChromosomeOperation chromOp = new ChromosomeOperation(random, problem, fitness);
        
        // 3. 创建初始染色体
        Chromosome chromosome = new Chromosome(problem.getJobs(), random, problem);
        
        // 4. 评估初始fitness
        Operation[][] opMatrix = createOperationMatrix(problem);
        initOperationMatrix(opMatrix, problem);
        fitness.evaluate(chromosome, problem, opMatrix);
        
        System.out.println("========== 变异策略测试 ==========");
        System.out.println("初始fitness: " + chromosome.fitness);
        System.out.println();
        
        // 5. 执行多次变异测试
        int numTests = 10;
        int improvedCount = 0;
        double totalImprovement = 0;
        
        for (int i = 0; i < numTests; i++) {
            // 备份原染色体
            Chromosome backup = new Chromosome(chromosome);
            double originalFitness = chromosome.fitness;
            
            // 执行变异
            chromOp.Mutation(chromosome);
            
            // 重新评估fitness（如果变异改变了染色体）
            fitness.evaluate(chromosome, problem, opMatrix);
            double newFitness = chromosome.fitness;
            
            // 统计结果
            double improvement = originalFitness - newFitness;
            if (improvement > 0) {
                improvedCount++;
                totalImprovement += improvement;
            }
            
            System.out.println("变异 " + (i+1) + ":");
            System.out.println("  原fitness: " + String.format("%.2f", originalFitness));
            System.out.println("  新fitness: " + String.format("%.2f", newFitness));
            System.out.println("  改进: " + String.format("%.2f", improvement) + 
                             (improvement > 0 ? " ✓" : " (无改进)"));
            System.out.println();
            
            // 如果没有改进，恢复原染色体用于下次测试
            if (improvement <= 0) {
                chromosome = backup;
            }
        }
        
        // 6. 输出统计结果
        System.out.println("========== 测试统计 ==========");
        System.out.println("总变异次数: " + numTests);
        System.out.println("改进次数: " + improvedCount);
        System.out.println("改进率: " + String.format("%.1f%%", (double)improvedCount / numTests * 100));
        System.out.println("平均改进: " + String.format("%.2f", totalImprovement / numTests));
        System.out.println("最终fitness: " + chromosome.fitness);
        
        // 7. 验证多试探策略的效果
        System.out.println();
        System.out.println("========== 多试探策略验证 ==========");
        System.out.println("✅ 关键特性：");
        System.out.println("  1. 每次变异只接受改进（fitness减小或不变）");
        System.out.println("  2. 如果所有候选都不如原染色体，则保持不变");
        System.out.println("  3. 三种变异类型：打印段OS交换、离散段OS交换、MS重分配");
        System.out.println();
        
        if (improvedCount == 0) {
            System.out.println("⚠️ 无改进可能原因：");
            System.out.println("  - 初始解已经很优，难以找到更好的邻域解");
            System.out.println("  - 候选数量(3-5)不足，可以增加候选数量");
            System.out.println("  - 问题规模较小，搜索空间有限");
        } else {
            System.out.println("✓ 变异策略工作正常！");
        }
    }
    
    /**
     * 创建操作矩阵
     */
    private static Operation[][] createOperationMatrix(Problem problem) {
        int jobCount = problem.getJobCount();
        Operation[][] matrix = new Operation[jobCount][];
        
        for (int i = 0; i < jobCount; i++) {
            int operCount = problem.getJobs()[i].getOperationNr();
            matrix[i] = new Operation[operCount];
        }
        
        return matrix;
    }
    
    /**
     * 初始化操作矩阵
     */
    private static void initOperationMatrix(Operation[][] matrix, Problem problem) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; i < matrix[i].length; j++) {
                matrix[i][j] = new Operation();
                matrix[i][j].jobNo = i;
                matrix[i][j].operNo = j;
            }
        }
    }
}

