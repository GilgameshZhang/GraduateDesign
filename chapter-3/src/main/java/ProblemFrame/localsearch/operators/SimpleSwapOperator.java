package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.Problem;
import java.util.Random;

/**
 * 简单交换算子（示例实现）
 * 
 * T1简化版：随机交换打印工序段中的两个位置
 * 目标：通过调整打印顺序来优化关键路径
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SimpleSwapOperator extends AbstractOperator {
    
    public SimpleSwapOperator(Random random) {
        super("T1-SimpleSwap", IndividualType.TIME_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        int jobCount = problem.getJobCount();
        
        // 1. 创建Delta
        Delta delta = new Delta();
        
        // 2. 检查打印工序段长度
        if (individual.gene_OS.length < 2 || jobCount < 2) {
            return null;  // 无法交换
        }
        
        // 3. 在打印工序段（前jobCount个位置）随机选择两个不同的位置
        int pos1 = random.nextInt(jobCount);
        int pos2 = random.nextInt(jobCount);
        
        // 确保pos1 != pos2
        while (pos2 == pos1 && jobCount > 1) {
            pos2 = random.nextInt(jobCount);
        }
        
        if (pos1 == pos2) {
            return null;  // 无法交换
        }
        
        // 4. 交换OS基因
        int temp = individual.gene_OS[pos1];
        individual.gene_OS[pos1] = individual.gene_OS[pos2];
        individual.gene_OS[pos2] = temp;
        
        // 5. 标记受影响的工件
        delta.markJobAffected(individual.gene_OS[pos1]);
        delta.markJobAffected(individual.gene_OS[pos2]);
        
        // 6. 返回候选解
        return new Candidate(individual, delta, getName());
    }
}
