package ProblemFrame;

import ProgramEntity.Job;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.PlaceItem;
import ProgramEntity.Problem;

import java.util.ArrayList;
import java.util.List;

/**
 * 多目标评价函数
 * 
 * 扩展自单目标评价，增加：
 * - 多目标计算（Cmax、能耗、拖期等）
 * - packingQ 计算（最后一批占用率）
 * - 批次数统计
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class MOEvaluator {
    
    /**
     * packingQ 计算方式枚举
     */
    public enum PackingQMode {
        /** 所有打印机最后一批占用率的最小值 */
        LAST_BATCH_MIN,
        
        /** 所有打印机最后一批占用率的平均值 */
        LAST_BATCH_AVG,
        
        /** 装箱评分系统的总分（如果实现了12级评分） */
        SCORE_SUM
    }
    
    /** packingQ 计算模式（默认为最小值） */
    private PackingQMode packingQMode = PackingQMode.LAST_BATCH_MIN;
    
    /**
     * 设置 packingQ 计算模式
     */
    public void setPackingQMode(PackingQMode mode) {
        this.packingQMode = mode;
    }
    
    /**
     * 评价多目标个体
     * 
     * 复用单目标的 CaculateFitness.evaluate() 进行解码和makespan计算
     * 然后扩展计算其他目标和 packingQ
     * 
     * @param individual 待评价的个体
     * @param problem 问题实例
     * @param operationMatrix 工序矩阵
     * @param objectiveFunctions 要计算的目标函数列表
     */
    public void evaluate(MOIndividual individual, 
                        Problem problem,
                        Operation[][] operationMatrix,
                        List<ObjectiveFunction> objectiveFunctions) {
        
        // 1. 转换为单目标染色体，复用现有解码逻辑
        Chromosome chromosome = individual.toChromosome();
        
        // 2. 调用单目标评价函数（会进行解码、装箱、调度）
        // 注意：需要传入operationMatrix的引用，因为evaluate会修改它
        CaculateFitness fitness = new CaculateFitness();
        double makespan = fitness.evaluate(chromosome, problem, operationMatrix);
        
        // 3. 更新个体的基因和解（已解码）
        individual.updateFromChromosome(chromosome);
        
        // 4. 初始化objectives数组（如果未初始化）
        if (individual.objectives == null || individual.objectives.length != objectiveFunctions.size()) {
            individual.objectives = new double[objectiveFunctions.size()];
        }
        
        // 5. 计算各个目标
        for (int i = 0; i < objectiveFunctions.size(); i++) {
            ObjectiveFunction objFunc = objectiveFunctions.get(i);
            individual.objectives[i] = objFunc.calculate(chromosome, problem, operationMatrix);
        }
        
        // 6. 计算 packingQ（装箱质量指标）
        individual.packingQ = calculatePackingQ(chromosome, problem);
        
        // 7. 统计批次总数
        individual.batchCount = countTotalBatches(chromosome);
    }
    
    /**
     * 计算 packingQ（装箱质量指标）
     * 
     * 根据配置的模式计算：
     * - LAST_BATCH_MIN: 最后一批占用率的最小值
     * - LAST_BATCH_AVG: 最后一批占用率的平均值
     * 
     * @param chromosome 已评价的染色体（包含printSolution）
     * @param problem 问题实例
     * @return packingQ值（0.0-1.0，越大越好）
     */
    private double calculatePackingQ(Chromosome chromosome, Problem problem) {
        if (chromosome.printSolution == null || chromosome.printSolution.length == 0) {
            return 0.0;
        }
        
        Machine[] machines = problem.getMachines();
        int printMachineCount = 0;
        
        // 统计打印机数量
        for (Machine m : machines) {
            if (m instanceof PrintMachine) {
                printMachineCount++;
            }
        }
        
        if (printMachineCount == 0) {
            return 0.0;
        }
        
        double[] lastBatchRates = new double[printMachineCount];
        int validCount = 0;
        
        // 计算每台打印机最后一批的占用率
        for (int i = 0; i < printMachineCount; i++) {
            List<ProgramEntity.Solution> machineSolution = chromosome.printSolution[i];
            
            if (machineSolution != null && !machineSolution.isEmpty()) {
                // 获取最后一批
                ProgramEntity.Solution lastBatch = machineSolution.get(machineSolution.size() - 1);
                
                // 占用率 = 已用面积 / 平台面积
                // 注意：lastBatch.rate 可能已经计算好了
                if (lastBatch.rate > 0) {
                    lastBatchRates[validCount++] = lastBatch.rate;
                } else {
                    // 如果rate未计算，手动计算
                    PrintMachine pm = (PrintMachine) machines[i];
                    double plateArea = pm.L * pm.W;
                    double usedArea = calculateUsedArea(lastBatch);
                    lastBatchRates[validCount++] = usedArea / plateArea;
                }
            }
        }
        
        if (validCount == 0) {
            return 0.0;
        }
        
        // 根据模式计算 packingQ
        switch (packingQMode) {
            case LAST_BATCH_MIN:
                // 返回最小占用率
                double minRate = Double.POSITIVE_INFINITY;
                for (int i = 0; i < validCount; i++) {
                    minRate = Math.min(minRate, lastBatchRates[i]);
                }
                return minRate;
                
            case LAST_BATCH_AVG:
                // 返回平均占用率
                double sum = 0.0;
                for (int i = 0; i < validCount; i++) {
                    sum += lastBatchRates[i];
                }
                return sum / validCount;
                
            case SCORE_SUM:
                // TODO: 如果实现了12级评分系统，这里返回总评分
                // 目前先返回平均占用率
                double avgScore = 0.0;
                for (int i = 0; i < validCount; i++) {
                    avgScore += lastBatchRates[i];
                }
                return avgScore / validCount;
                
            default:
                return 0.0;
        }
    }
    
    /**
     * 计算批次的实际使用面积
     */
    private double calculateUsedArea(ProgramEntity.Solution batch) {
        if (batch.placeItemList == null || batch.placeItemList.isEmpty()) {
            return 0.0;
        }
        
        double usedArea = 0.0;
        for (PlaceItem item : batch.placeItemList) {
            usedArea += item.l * item.w;
        }
        
        return usedArea;
    }
    
    /**
     * 统计总批次数
     */
    private int countTotalBatches(Chromosome chromosome) {
        if (chromosome.printSolution == null) {
            return 0;
        }
        
        int totalBatches = 0;
        for (List<ProgramEntity.Solution> machineSolution : chromosome.printSolution) {
            if (machineSolution != null) {
                totalBatches += machineSolution.size();
            }
        }
        
        return totalBatches;
    }
    
    /**
     * 目标函数接口
     */
    public interface ObjectiveFunction {
        /**
         * 计算目标值（最小化）
         */
        double calculate(Chromosome chromosome, Problem problem, Operation[][] operationMatrix);
        
        /**
         * 获取目标名称
         */
        String getName();
    }
    
    /**
     * 预定义目标函数：Makespan (Cmax)
     */
    public static class MaximumCompletionTime implements ObjectiveFunction {
        @Override
        public double calculate(Chromosome chromosome, Problem problem, Operation[][] operationMatrix) {
            // 找到所有工序的最大完工时间
            double maxTime = 0.0;
            
            for (int i = 0; i < operationMatrix.length; i++) {
                for (int j = 0; j < operationMatrix[i].length; j++) {
                    if (operationMatrix[i][j].endTime > maxTime) {
                        maxTime = operationMatrix[i][j].endTime;
                    }
                }
            }
            
            return maxTime;
        }
        
        @Override
        public String getName() {
            return "Cmax";
        }
    }
    
    /**
     * 预定义目标函数：总能耗（含开/关机策略）
     * 
     * 能耗模型（绿色车间）：
     * 1. 运行能耗：E_run = sum(P_run_k * duration_k)
     * 2. 空闲能耗（考虑开关机策略）：
     *    - 短空闲间隔：待机 E_idle = P_idle * gap
     *    - 长空闲间隔：关机 E_idle = E_switch
     * 
     * 开关机判定条件：
     * - gap >= T_warmup + T_breakEven
     * - 其中 T_breakEven = E_switch / P_idle
     * 
     * @author AI Assistant
     * @version 2.0 (支持开关机策略)
     */
    public static class TotalEnergyConsumption implements ObjectiveFunction {
        /** 能耗计算器 */
        private EnergyCalculator calculator;
        
        /** 是否启用开关机策略（可配置，用于对比实验） */
        private boolean enableSwitchingStrategy;
        
        /** 是否输出调试信息 */
        private boolean debugOutput;
        
        /**
         * 默认构造函数 - 启用开关机策略
         */
        public TotalEnergyConsumption(Problem problem) {
            this(problem, true, false);
        }
        
        /**
         * 可配置构造函数
         * 
         * @param problem 问题实例
         * @param enableSwitchingStrategy 是否启用开关机策略
         * @param debugOutput 是否输出调试信息
         */
        public TotalEnergyConsumption(Problem problem, boolean enableSwitchingStrategy, 
                                     boolean debugOutput) {
            this.enableSwitchingStrategy = enableSwitchingStrategy;
            this.debugOutput = debugOutput;
            this.calculator = new EnergyCalculator(problem, enableSwitchingStrategy, debugOutput);
        }
        
        /**
         * 设置指定机器的能耗参数（可选）
         * 
         * @param machineId 机器ID (0-based)
         * @param params 能耗参数
         */
        public void setMachineParameters(int machineId, PowerParameters params) {
            calculator.setMachineParameters(machineId, params);
        }
        
        @Override
        public double calculate(Chromosome chromosome, Problem problem, Operation[][] operationMatrix) {
            // 使用能耗计算器计算总能耗（含开/关机策略）
            return calculator.calculateTotalEnergy(operationMatrix, problem);
        }
        
        /**
         * 获取能耗统计信息（用于分析）
         */
        public EnergyCalculator.EnergyStatistics getStatistics(
                Chromosome chromosome, Problem problem, Operation[][] operationMatrix) {
            return calculator.getStatistics(operationMatrix, problem);
        }
        
        @Override
        public String getName() {
            return enableSwitchingStrategy ? "Energy(开关机)" : "Energy(待机)";
        }
    }
    
    /**
     * 预定义目标函数：总拖期时间
     * 
     * 假设每个工件有交货期 due date（存储在 Job.dueDate）
     * 拖期 = max(0, 实际完工时间 - 交货期)
     * 总拖期 = sum(所有工件的拖期)
     */
    public static class TotalTardiness implements ObjectiveFunction {
        @Override
        public double calculate(Chromosome chromosome, Problem problem, Operation[][] operationMatrix) {
            double totalTardiness = 0.0;
            Job[] jobs = problem.getJobs();
            
            for (int i = 0; i < operationMatrix.length; i++) {
                // 获取工件的最后一道工序的完工时间
                int lastOpIndex = operationMatrix[i].length - 1;
                double completionTime = operationMatrix[i][lastOpIndex].endTime;
                
                // 如果Job类有dueDate字段，计算拖期
                // 注意：需要确认Job类是否有dueDate字段
                // 这里假设有一个默认的交货期（例如：所有工件交货期=makespan的80%）
                // 实际使用时需要根据Job类的实际字段修改
                
                // 临时方案：假设交货期为一个固定值或比例
                double dueDate = 1000.0; // 默认交货期（需要根据实际情况调整）
                
                double tardiness = Math.max(0, completionTime - dueDate);
                totalTardiness += tardiness;
            }
            
            return totalTardiness;
        }
        
        @Override
        public String getName() {
            return "Tardiness";
        }
    }
    
    /**
     * 预定义目标函数：批处理等待时间总和
     * 
     * 批处理等待时间 = 打印完成时间 到 批处理开始时间 的间隔
     */
    public static class TotalBatchWaitingTime implements ObjectiveFunction {
        @Override
        public double calculate(Chromosome chromosome, Problem problem, Operation[][] operationMatrix) {
            double totalWaitingTime = 0.0;
            
            for (int i = 0; i < operationMatrix.length; i++) {
                if (operationMatrix[i].length >= 2) {
                    Operation printOp = operationMatrix[i][0];      // 打印工序
                    Operation batchOp = operationMatrix[i][1];      // 批处理工序
                    
                    double waitingTime = batchOp.startTime - printOp.endTime;
                    totalWaitingTime += Math.max(0, waitingTime);
                }
            }
            
            return totalWaitingTime;
        }
        
        @Override
        public String getName() {
            return "WaitTime";
        }
    }
}

