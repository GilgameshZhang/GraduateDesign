package AlgorithmFrame.machineChoice.ga;

import ProblemFrame.Input;

/**
 * 支持消融实验的BatchGa（简化版本）
 * BatchGenome 已经支持禁忌搜索和评分策略的消融（通过系统属性控制）
 * 本类主要支持动态设置启发式初始化比例
 */
public class BatchGaAblation extends BatchGa {
    
    /**
     * 构造函数（带时间限制和初始化比例）
     * @param heuristicInitRatio 启发式初始化的比例 (0.0-1.0)
     *                          0.0 = 全部随机初始化
     *                          0.5 = 50%启发式 + 50%随机
     *                          1.0 = 全部启发式初始化
     */
    public BatchGaAblation(int MAX_GEN, int popSize, double variationExchangeCount, 
                          int cloneNumOfBestIndividual, double mutationRate, 
                          double crossoverRate, Input input, boolean isRotateEnable, 
                          String method, int decodeMaxGen, int decodeTabuSize, int decodeMaxN,
                          long timeLimitMs, double heuristicInitRatio) {
        super(MAX_GEN, popSize, variationExchangeCount, cloneNumOfBestIndividual, 
              mutationRate, crossoverRate, input, isRotateEnable, method, 
              decodeMaxGen, decodeTabuSize, decodeMaxN, timeLimitMs, heuristicInitRatio);
    }
    
    /**
     * 获取算法名称标识
     */
    public String getAlgorithmName() {
        return "GA-TS-Ablation";
    }
}
