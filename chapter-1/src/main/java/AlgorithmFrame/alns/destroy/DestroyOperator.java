package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.List;

/**
 * Destroy算子接口
 */
public interface DestroyOperator {
    /**
     * 从解中移除指定数量的作业
     * @param solution 当前解
     * @param jobs 所有作业
     * @param machines 所有机器
     * @param numToRemove 要移除的作业数量
     * @return 被移除的作业ID列表
     */
    List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove);
    
    /**
     * 获取算子名称
     */
    String getName();
}
