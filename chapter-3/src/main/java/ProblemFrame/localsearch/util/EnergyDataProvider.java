package ProblemFrame.localsearch.util;

import ProblemFrame.MOIndividual;
import ProgramEntity.*;
import ProgramEntity.Machine.*;
import java.util.*;

/**
 * 能耗数据提供器
 * 
 * 为局部搜索算子提供真实的能耗数据：
 * - 机器功率参数
 * - 打印机idle能耗估算
 * - 机器利用率统计
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class EnergyDataProvider {
    
    /** 默认功率参数 (kW) */
    private static final double DEFAULT_PRINT_POWER = 5.0;
    private static final double DEFAULT_BATCH_POWER = 3.0;
    private static final double DEFAULT_DISCRETE_POWER = 2.0;
    
    /** idle功率比例（idle功率 = 运行功率 * IDLE_RATIO） */
    private static final double IDLE_POWER_RATIO = 0.2;
    
    /** 时间单位转换：秒 -> 小时 */
    private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;
    
    private final Problem problem;
    private final Map<Integer, Double> machinePowerMap;
    
    public EnergyDataProvider(Problem problem) {
        this.problem = problem;
        this.machinePowerMap = new HashMap<>();
        initializeMachinePower();
    }
    
    /**
     * 初始化机器功率映射
     */
    private void initializeMachinePower() {
        Machine[] machines = problem.getMachines();
        
        for (int i = 0; i < machines.length; i++) {
            double power;
            
            if (machines[i] instanceof PrintMachine) {
                power = DEFAULT_PRINT_POWER;
            } else if (machines[i] instanceof BathchMachine) {
                power = DEFAULT_BATCH_POWER;
            } else {
                power = DEFAULT_DISCRETE_POWER;
            }
            
            machinePowerMap.put(i, power);
        }
    }
    
    /**
     * 获取机器的加工功率
     */
    public double getMachinePower(int machineId) {
        return machinePowerMap.getOrDefault(machineId, DEFAULT_DISCRETE_POWER);
    }
    
    /**
     * 获取机器的idle功率
     */
    public double getMachineIdlePower(int machineId) {
        return getMachinePower(machineId) * IDLE_POWER_RATIO;
    }
    
    /**
     * 获取所有机器功率映射
     */
    public Map<Integer, Double> getMachinePowerMap() {
        return new HashMap<>(machinePowerMap);
    }
    
    /**
     * 计算打印机的能耗统计信息
     */
    public PrinterEnergyStats calculatePrinterEnergyStats(
            MOIndividual individual, int printerNo) {
        
        if (individual.printSolution == null || 
            printerNo < 0 || 
            printerNo >= individual.printSolution.length) {
            return null;
        }
        
        List<Solution> batchList = individual.printSolution[printerNo];
        if (batchList == null || batchList.isEmpty()) {
            return null;
        }
        
        Machine machine = problem.getMachines()[printerNo];
        if (!(machine instanceof PrintMachine)) {
            return null;
        }
        
        PrintMachine printer = (PrintMachine) machine;
        
        // 计算批次数、总运行时间、空闲时间
        int batchCount = batchList.size();
        double totalRunTime = 0.0;
        double totalIdleTime = 0.0;
        double totalUtilization = 0.0;
        
        double lastEndTime = 0.0;
        for (Solution batch : batchList) {
            // 批次运行时间
            double batchDuration = batch.endTime - batch.startTime;
            totalRunTime += batchDuration;
            
            // 批次间的空闲时间
            if (lastEndTime > 0) {
                double idleGap = batch.startTime - lastEndTime;
                if (idleGap > 0) {
                    totalIdleTime += idleGap;
                }
            }
            
            lastEndTime = batch.endTime;
            
            // 面积利用率
            totalUtilization += batch.rate;
        }
        
        double avgUtilization = totalUtilization / batchCount;
        
        // 计算能耗
        double procPower = getMachinePower(printerNo);
        double idlePower = getMachineIdlePower(printerNo);
        
        double runEnergy = procPower * totalRunTime * SECONDS_TO_HOURS;
        double idleEnergy = idlePower * totalIdleTime * SECONDS_TO_HOURS;
        double totalEnergy = runEnergy + idleEnergy;
        
        return new PrinterEnergyStats(
            printerNo,
            batchCount,
            totalRunTime,
            totalIdleTime,
            avgUtilization,
            runEnergy,
            idleEnergy,
            totalEnergy,
            lastEndTime
        );
    }
    
    /**
     * 计算所有打印机的能耗统计
     */
    public List<PrinterEnergyStats> calculateAllPrinterStats(MOIndividual individual) {
        List<PrinterEnergyStats> statsList = new ArrayList<>();
        
        if (individual.printSolution == null) {
            return statsList;
        }
        
        for (int i = 0; i < individual.printSolution.length; i++) {
            PrinterEnergyStats stats = calculatePrinterEnergyStats(individual, i);
            if (stats != null) {
                statsList.add(stats);
            }
        }
        
        return statsList;
    }
    
    /**
     * 计算工序的能耗
     */
    public double calculateOperationEnergy(int operIdx, int machineId, double processingTime) {
        double power = getMachinePower(machineId);
        return power * processingTime * SECONDS_TO_HOURS;
    }
    
    /**
     * 获取可选机器的能耗列表
     */
    public List<MachineEnergyOption> getAlternativeMachineEnergies(
            int operIdx, double[][] proDesMatrix) {
        
        List<MachineEnergyOption> options = new ArrayList<>();
        
        if (operIdx < 0 || operIdx >= proDesMatrix.length) {
            return options;
        }
        
        for (int m = 0; m < proDesMatrix[operIdx].length; m++) {
            double procTime = proDesMatrix[operIdx][m];
            
            if (procTime > 0 && procTime != Double.MAX_VALUE) {
                double power = getMachinePower(m);
                double energy = power * procTime * SECONDS_TO_HOURS;
                
                options.add(new MachineEnergyOption(m, procTime, power, energy));
            }
        }
        
        // 按能耗升序排序
        options.sort(Comparator.comparingDouble(o -> o.energy));
        
        return options;
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 打印机能耗统计
     */
    public static class PrinterEnergyStats {
        public final int printerNo;
        public final int batchCount;
        public final double totalRunTime;
        public final double totalIdleTime;
        public final double avgUtilization;
        public final double runEnergy;
        public final double idleEnergy;
        public final double totalEnergy;
        public final double makespan;
        
        public PrinterEnergyStats(int printerNo, int batchCount, double totalRunTime,
                                 double totalIdleTime, double avgUtilization,
                                 double runEnergy, double idleEnergy, double totalEnergy,
                                 double makespan) {
            this.printerNo = printerNo;
            this.batchCount = batchCount;
            this.totalRunTime = totalRunTime;
            this.totalIdleTime = totalIdleTime;
            this.avgUtilization = avgUtilization;
            this.runEnergy = runEnergy;
            this.idleEnergy = idleEnergy;
            this.totalEnergy = totalEnergy;
            this.makespan = makespan;
        }
        
        @Override
        public String toString() {
            return String.format(
                "Printer[%d] batches=%d, util=%.2f%%, idleE=%.2f kWh, totalE=%.2f kWh",
                printerNo, batchCount, avgUtilization * 100, idleEnergy, totalEnergy
            );
        }
    }
    
    /**
     * 机器能耗选项
     */
    public static class MachineEnergyOption {
        public final int machineId;
        public final double processingTime;
        public final double power;
        public final double energy;
        
        public MachineEnergyOption(int machineId, double processingTime, 
                                  double power, double energy) {
            this.machineId = machineId;
            this.processingTime = processingTime;
            this.power = power;
            this.energy = energy;
        }
        
        @Override
        public String toString() {
            return String.format("Machine[%d] time=%.2f, power=%.2f kW, energy=%.4f kWh",
                machineId, processingTime, power, energy);
        }
    }
}
