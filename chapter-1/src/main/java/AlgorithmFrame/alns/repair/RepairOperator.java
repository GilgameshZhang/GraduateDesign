package AlgorithmFrame.alns.repair;

import AlgorithmFrame.alns.ALNSEvaluator;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;
import AlgorithmFrame.alns.packing.SkylinePackingAdapter;

import java.util.List;

/**
 * Repair算子接口
 */
public interface RepairOperator {
    /**
     * 将移除的作业重新插入解中
     * @param solution 当前解
     * @param removedJobs 被移除的作业ID列表
     * @param jobs 所有作业
     * @param machines 所有机器
     * @param evaluator 评价器
     * @param packer 装箱适配器
     * @return 是否成功插入所有作业
     */
    boolean repair(ALNSSolution solution, List<Integer> removedJobs, ALNSJob[] jobs, 
                  ALNSMachine[] machines, ALNSEvaluator evaluator, SkylinePackingAdapter packer);
    
    /**
     * 获取算子名称
     */
    String getName();
}
