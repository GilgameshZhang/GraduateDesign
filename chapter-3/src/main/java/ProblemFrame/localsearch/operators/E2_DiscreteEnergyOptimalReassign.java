package ProblemFrame.localsearch.operators;

import ProblemFrame.MOIndividual;
import ProblemFrame.localsearch.*;
import ProblemFrame.localsearch.util.CriticalPathIdentifier;
import ProblemFrame.localsearch.util.CriticalPathIdentifier.HighEnergyOperationInfo;
import ProblemFrame.localsearch.util.EnergyDataProvider;
import ProblemFrame.localsearch.util.EnergyDataProvider.MachineEnergyOption;
import ProgramEntity.*;
import java.util.*;

/**
 * E2: 离散工序能耗最优换机算子
 * 
 * 目标：在可选机器中选 P_proc * p 最小的机器
 * 
 * 邻域动作：对高能耗工序执行换机
 * 
 * 设计文档位置：7.2 ENERGY_DEFICIENT算子池 - E2
 * 
 * 完整版本：使用真实的能耗数据和关键路径识别
 * 
 * @author AI Assistant
 * @version 2.0
 */
public class E2_DiscreteEnergyOptimalReassign extends AbstractOperator {
    
    /** 每次换机的工序数量范围 */
    private static final int MIN_OPS_TO_REASSIGN = 1;
    private static final int MAX_OPS_TO_REASSIGN = 5;
    
    /** 默认加工功率（如果机器没有设置功率参数） */
    private static final double DEFAULT_PROC_POWER = 100.0;  // kW
    
    public E2_DiscreteEnergyOptimalReassign(Random random) {
        super("E2-DiscreteEnergyOptimalReassign", IndividualType.ENERGY_DEFICIENT, random);
    }
    
    @Override
    public Candidate tryApply(MOIndividual individual, Problem problem) {
        // 创建Delta快照
        Delta delta = new Delta();
       // System.out.println("E2-DiscreteEnergyOptimalReassign");
        
        int jobCount = problem.getJobCount();
        
        // 步骤1: 检查离散工序段
        int discreteStart = jobCount;
        int discreteLength = individual.gene_MS.length - discreteStart;
        
        if (discreteLength < 1) {
            return null;
        }
        
        // 步骤2: 使用真实能耗数据按当前贡献 P_proc(m)*p(op,m) 排序选前K个高能耗工序
        int K = MIN_OPS_TO_REASSIGN + random.nextInt(
            Math.min(MAX_OPS_TO_REASSIGN - MIN_OPS_TO_REASSIGN + 1, discreteLength)
        );
        
        EnergyDataProvider energyProvider = new EnergyDataProvider(problem);
        Map<Integer, Double> machinePowerMap = energyProvider.getMachinePowerMap();
        
        List<HighEnergyOperationInfo> highEnergyOps = CriticalPathIdentifier.identifyHighEnergyOperations(
            individual, problem, machinePowerMap, K);
        
        if (highEnergyOps.isEmpty()) {
            return null;
        }
        
        // 步骤3: 使用真实能耗数据对每个高能耗工序找 m* = argmin P_proc(m)*p(op,m)
        boolean anyChange = false;
        double[][] proDesMatrix = problem.getProDesMatrix();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        for (HighEnergyOperationInfo highEnergyOp : highEnergyOps) {
            int jobNo = highEnergyOp.jobNo;
            int operNo = highEnergyOp.operNo;
            
            if (operNo < 0 || jobNo >= operationToIndex.length || 
                operNo >= operationToIndex[jobNo].length) {
                continue;
            }
            
            int operIdx = operationToIndex[jobNo][operNo];
            if (operIdx >= proDesMatrix.length) {
                continue;
            }
            
            // ✅ 正确计算MS的固定位置（离散段MS是固定顺序存储的）
            int msIndex = jobCount;
            for (int j = 0; j < jobNo; j++) {
                msIndex += (problem.getOperationCountArr()[j] - 2);
            }
            msIndex += (operNo - 2);
            
            // 使用能耗数据提供器获取可选机器的能耗列表（已按能耗升序排序）
            List<MachineEnergyOption> energyOptions = energyProvider.getAlternativeMachineEnergies(
                operIdx, proDesMatrix);
            
            if (energyOptions.isEmpty()) {
                continue;
            }
            
            // 第一个选项就是能耗最低的机器
            MachineEnergyOption bestOption = energyOptions.get(0);
            
            // 获取当前机器
            int currentMachineId = highEnergyOp.machineId;
            
            // 如果找到更好的机器，执行换机
            if (bestOption.machineId != currentMachineId &&
                bestOption.energy < highEnergyOp.energy * 0.95) {  // 至少降低5%

                // 更新机器选择
                int newRelativeIndex = getRelativeIndex(highEnergyOp.availableMachines, bestOption.machineId);
                if (newRelativeIndex > 0) {
//                    System.out.println("E2修改: 工件J" + jobNo + "工序" + operNo +
//                        ", MS[" + msIndex + "]=" + newRelativeIndex +
//                        " (可选机器数=" + highEnergyOp.availableMachines.size() + ")");
                    individual.gene_MS[msIndex] = newRelativeIndex;
                    delta.markJobAffected(jobNo);
                    anyChange = true;
                }
            }
        }
        
        if (!anyChange) {
            return null;
        }
        
        return new Candidate(individual, delta, getName());
    }
    
    /**
     * 计算工序编号
     */
    private int calculateOperationNumber(MOIndividual individual, int pos, int jobCount) {
        if (pos < jobCount) {
            return 0;
        }
        
        int jobNo = individual.gene_OS[pos];
        int occurrences = 0;
        for (int i = jobCount; i < pos; i++) {
            if (individual.gene_OS[i] == jobNo) {
                occurrences++;
            }
        }
        
        return 2 + occurrences;
    }
    
    /**
     * 从相对索引获取机器ID
     */
//    private int getMachineIdFromRelativeIndex(List<Integer> availableMachines, int relativeIndex) {
//        if (relativeIndex < 1 || relativeIndex > availableMachines.size()) {
//            return availableMachines.isEmpty() ? -1 : availableMachines.get(0).machineId;
//        }
//        return availableMachines.get(relativeIndex - 1).machineId;
//    }
    
    /**
     * 获取相对索引
     */
    private int getRelativeIndex(List<Integer> availableMachines, int machineId) {
        for (int i = 0; i < availableMachines.size(); i++) {
            if (availableMachines.get(i) == machineId) {
                return i + 1;
            }
        }
        return -1;
    }
}
