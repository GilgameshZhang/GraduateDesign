package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProgramEntity.Problem;
import java.util.Random;

/**
 * 简单换机算子（示例实现）
 * 
 * E2简化版：随机选择一个离散工序，换到另一台可选机器
 * 目标：通过选择能耗更低的机器来降低总能耗
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class SimpleMachineReassignOperator extends AbstractOperator {
    
    public SimpleMachineReassignOperator(Random random) {
        super("E2-SimpleMachineReassign", IndividualType.ENERGY_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        int jobCount = problem.getJobCount();
        
        // 1. 创建Delta
        Delta delta = new Delta();
        
        // 2. 检查是否有离散工序
        int discreteStart = jobCount;
        int discreteLength = individual.gene_MS.length - discreteStart;
        
        if (discreteLength <= 0) {
            return null;  // 没有离散工序
        }
        
        // 3. 随机选择一个离散工序位置
        int pos = discreteStart + random.nextInt(discreteLength);
        
        // 4. 获取当前机器选择（相对索引）
        int currentMachine = individual.gene_MS[pos];
        
        // 5. 尝试换到另一台机器（简化：随机选择1-3之间的相对索引）
        // 注意：实际应该查询该工序的可选机器集合
        int maxMachines = 3;  // 假设最多3台可选机器
        int newMachine = 1 + random.nextInt(maxMachines);
        
        // 确保换到不同的机器
        if (newMachine == currentMachine && maxMachines > 1) {
            newMachine = (newMachine % maxMachines) + 1;
        }
        
        if (newMachine == currentMachine) {
            return null;  // 无法换机
        }
        
        // 6. 修改MS基因
        individual.gene_MS[pos] = newMachine;
        
        // 7. 标记受影响的工件（通过OS获取）
        int affectedJob = individual.gene_OS[pos];
        delta.markJobAffected(affectedJob);
        
        // 8. 返回候选解
        return new Candidate(individual, delta, getName());
    }
}
