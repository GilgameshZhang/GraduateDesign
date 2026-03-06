package ProblemFrame.localsearch;

import ProblemFrame.MOIndividual;
import java.util.*;

/**
 * 分位分类器
 * 
 * 基于Cmax和Energy的分位排名将精英个体分为两类：
 * - TIME_DEFICIENT (T): Cmax分位排名较高
 * - ENERGY_DEFICIENT (E): Energy分位排名较高
 * 
 * 分类逻辑：
 * 1. 计算每个个体的pc（Cmax分位）和pe（Energy分位）
 * 2. 比较delta = pc - pe
 * 3. delta >= 0 → TIME_DEFICIENT，否则 → ENERGY_DEFICIENT
 * 4. 当abs(delta) < tau时，使用pc >= 0.5作为打破平局的规则
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class PercentileClassifier {
    
    /** 平局阈值（当|pc - pe| < tau时使用打破平局规则） */
    private double tau;
    
    /**
     * 构造函数
     * 
     * @param tau 平局阈值（推荐0.05）
     */
    public PercentileClassifier(double tau) {
        this.tau = tau;
    }
    
    /**
     * 默认构造函数（tau = 0.05）
     */
    public PercentileClassifier() {
        this(0.05);
    }
    
    /**
     * 为精英集合中的个体分类
     * 
     * @param elites 精英个体列表（通常为F1 ∪ F2的前η%）
     */
    public void classify(List<MOIndividual> elites) {
        if (elites == null || elites.isEmpty()) {
            return;
        }
        
        int size = elites.size();
        
        // 特殊情况：只有一个个体
        if (size == 1) {
            MOIndividual ind = elites.get(0);
            ind.pc = 0.5;
            ind.pe = 0.5;
            // 默认分为TIME_DEFICIENT
            ind.type = IndividualType.TIME_DEFICIENT;
            return;
        }
        
        // 1. 计算分位排名
        calculatePercentileRanks(elites);
        
        // 2. 为每个个体分类
        for (MOIndividual ind : elites) {
            ind.type = determineType(ind.pc, ind.pe);
        }
    }
    
    /**
     * 计算分位排名
     * 
     * pc(x) = rank_c / (|S|-1)，范围[0, 1]，越大表示Cmax越差
     * pe(x) = rank_e / (|S|-1)，范围[0, 1]，越大表示Energy越差
     */
    private void calculatePercentileRanks(List<MOIndividual> elites) {
        int size = elites.size();
        
        // 创建副本用于排序（避免修改原列表顺序）
        List<MOIndividual> cmaxSorted = new ArrayList<>(elites);
        List<MOIndividual> energySorted = new ArrayList<>(elites);
        
        // 按Cmax排序（升序，最小的rank=0）
        cmaxSorted.sort(Comparator.comparingDouble(ind -> ind.objectives[0]));
        
        // 按Energy排序（升序，最小的rank=0）
        energySorted.sort(Comparator.comparingDouble(ind -> ind.objectives[1]));
        
        // 为每个个体分配Cmax排名
        for (int i = 0; i < size; i++) {
            MOIndividual ind = cmaxSorted.get(i);
            ind.pc = (double) i / (size - 1);
        }
        
        // 为每个个体分配Energy排名
        for (int i = 0; i < size; i++) {
            MOIndividual ind = energySorted.get(i);
            ind.pe = (double) i / (size - 1);
        }
    }
    
    /**
     * 根据pc和pe确定个体类型
     * 
     * @param pc Cmax分位（0..1）
     * @param pe Energy分位（0..1）
     * @return 个体类型
     */
    private IndividualType determineType(double pc, double pe) {
        double delta = pc - pe;
        
        // 如果差值明显（超过阈值）
        if (Math.abs(delta) >= tau) {
            // delta >= 0 表示Cmax分位更高（时间更差）
            return delta >= 0 ? IndividualType.TIME_DEFICIENT 
                              : IndividualType.ENERGY_DEFICIENT;
        }
        
        // 差值很小，使用打破平局规则
        // pc >= 0.5表示Cmax在后半部分（较差），分为TIME_DEFICIENT
        return pc >= 0.5 ? IndividualType.TIME_DEFICIENT 
                         : IndividualType.ENERGY_DEFICIENT;
    }
    
    /**
     * 获取分类统计信息
     */
    public String getClassificationSummary(List<MOIndividual> elites) {
        if (elites == null || elites.isEmpty()) {
            return "无精英个体";
        }
        
        int timeCount = 0;
        int energyCount = 0;
        
        for (MOIndividual ind : elites) {
            if (ind.type == IndividualType.TIME_DEFICIENT) {
                timeCount++;
            } else if (ind.type == IndividualType.ENERGY_DEFICIENT) {
                energyCount++;
            }
        }
        
        return String.format("精英分类: T=%d (%.1f%%), E=%d (%.1f%%)",
            timeCount, 100.0 * timeCount / elites.size(),
            energyCount, 100.0 * energyCount / elites.size());
    }
    
    /**
     * 设置平局阈值
     */
    public void setTau(double tau) {
        this.tau = tau;
    }
    
    /**
     * 获取平局阈值
     */
    public double getTau() {
        return tau;
    }
}
