package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.Problem;
import java.util.Random;

/**
 * 简单插入算子（示例实现）
 * 
 * T3简化版：随机选择一个离散工序，插入到另一个位置
 * 目标：通过调整离散工序顺序来优化关键路径
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SimpleInsertOperator extends AbstractOperator {
    
    public SimpleInsertOperator(Random random) {
        super("T3-SimpleInsert", IndividualType.TIME_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        int jobCount = problem.getJobCount();
        
        // 1. 创建Delta
        Delta delta = new Delta();
        
        // 2. 检查是否有离散工序
        int discreteStart = jobCount;
        int discreteLength = individual.gene_MS.length - discreteStart;
        
        if (discreteLength < 2) {
            return null;  // 离散工序太少，无法插入
        }
        
        // 3. 随机选择源位置和目标位置
        int srcPos = discreteStart + random.nextInt(discreteLength);
        int dstPos = discreteStart + random.nextInt(discreteLength);
        
        // 确保srcPos != dstPos
        while (dstPos == srcPos && discreteLength > 1) {
            dstPos = discreteStart + random.nextInt(discreteLength);
        }
        
        if (srcPos == dstPos) {
            return null;  // 无法插入
        }
        
        // 4. 执行插入操作（移除srcPos的元素，插入到dstPos）
        int jobToMove = individual.gene_OS[srcPos];
        int machineToMove = individual.gene_MS[srcPos];
        
        // 移除srcPos
        if (srcPos < dstPos) {
            // 向右移动元素
            for (int i = srcPos; i < dstPos; i++) {
                individual.gene_OS[i] = individual.gene_OS[i + 1];
                individual.gene_MS[i] = individual.gene_MS[i + 1];
            }
            individual.gene_OS[dstPos] = jobToMove;
            individual.gene_MS[dstPos] = machineToMove;
        } else {
            // 向左移动元素
            for (int i = srcPos; i > dstPos; i--) {
                individual.gene_OS[i] = individual.gene_OS[i - 1];
                individual.gene_MS[i] = individual.gene_MS[i - 1];
            }
            individual.gene_OS[dstPos] = jobToMove;
            individual.gene_MS[dstPos] = machineToMove;
        }
        
        // 5. 标记受影响的工件
        delta.markJobAffected(jobToMove);
        
        // 6. 返回候选解
        return new Candidate(individual, delta, getName());
    }
}
