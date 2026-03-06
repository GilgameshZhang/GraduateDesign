package ProblemFrame;

import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Operation;
import ProgramEntity.Problem;

import java.util.*;

/**
 * 能耗计算器 - 支持开/关机策略
 * 
 * 核心功能：
 * 1. 计算运行能耗：E_run = sum(P_run * duration)
 * 2. 计算空闲能耗：考虑开/关机策略
 *    - 短空闲：待机 E = P_idle * gap
 *    - 长空闲：关机 E = E_switch
 * 3. 支持按机器类型配置不同的能耗参数
 * 4. 自动处理时间单位转换（秒 -> 小时）
 * 
 * 时间单位说明：
 * - 调度系统使用"秒"作为时间单位
 * - 能耗计算使用"小时"作为时间单位（kWh = kW × 小时）
 * - 本类自动进行单位转换
 * 
 * @author AI Assistant
 * @version 2.0 (支持时间单位转换)
 */
public class EnergyCalculator {
    
    /** 时间单位转换：秒 -> 小时 */
    private static final double SECONDS_TO_HOURS = 1.0 / 3600.0;
    
    /** 机器能耗参数映射 (machineId -> PowerParameters) */
    private Map<Integer, PowerParameters> machineParams;
    
    /** 是否启用开关机策略（全局开关，用于消融实验） */
    private boolean enableSwitchingStrategy;
    
    /** 调试输出标志 */
    private boolean debugOutput;
    
    /**
     * 构造函数 - 使用默认参数
     */
    public EnergyCalculator(Problem problem) {
        this(problem, true, false);
    }
    
    /**
     * 构造函数 - 可配置
     */
    public EnergyCalculator(Problem problem, boolean enableSwitchingStrategy, boolean debugOutput) {
        this.enableSwitchingStrategy = enableSwitchingStrategy;
        this.debugOutput = debugOutput;
        this.machineParams = new HashMap<>();
        
        // 初始化默认参数
        initializeDefaultParameters(problem);
    }
    
    /**
     * 初始化能耗参数
     * 
     * 优先级：
     * 1. 从机器对象读取（machine.powerParams）
     * 2. 如果未设置，根据机器类型使用默认值：
     *    - 打印机：允许关机，P_run=5.0, P_idle=0.5
     *    - 批处理机：不允许关机（需保持温度），P_run=3.0, P_idle=0.8
     *    - 离散加工机：允许关机，P_run=2.0, P_idle=0.3
     */
    private void initializeDefaultParameters(Problem problem) {
        Machine[] machines = problem.getMachines();
        
        for (int i = 0; i < machines.length; i++) {
            PowerParameters params = null;
            
            // 优先从机器对象读取能耗参数
            if (machines[i].hasPowerParameters()) {
                params = machines[i].getPowerParameters();
                if (debugOutput) {
                    System.out.println("  ✓ 机器 " + (i+1) + ": 使用机器自带能耗参数");
                }
            } else {
                // 如果机器未设置，使用默认值
                if (machines[i] instanceof PrintMachine) {
                    params = PowerParameters.createPrintMachineDefault();
                } else if (machines[i] instanceof BathchMachine) {
                    params = PowerParameters.createBatchMachineDefault();
                } else {
                    // 离散加工机或其他类型
                    params = PowerParameters.createDiscreteMachineDefault();
                }
                
                if (debugOutput) {
                    System.out.println("  ⚠ 机器 " + (i+1) + ": 使用默认能耗参数");
                }
            }
            
            machineParams.put(i, params);
        }
    }
    
    /**
     * 设置指定机器的能耗参数
     */
    public void setMachineParameters(int machineId, PowerParameters params) {
        machineParams.put(machineId, params);
    }
    
    /**
     * 设置是否启用开关机策略
     */
    public void setEnableSwitchingStrategy(boolean enable) {
        this.enableSwitchingStrategy = enable;
    }
    
    /**
     * 计算总能耗（含开/关机策略）
     * 
     * @param operationMatrix 排程结果（所有工序的时间信息）
     * @param problem 问题实例
     * @return 总能耗 (kWh)
     */
    public double calculateTotalEnergy(Operation[][] operationMatrix, Problem problem) {
        // 1. 按机器分组所有工序
        Map<Integer, List<Operation>> machineOperations = groupOperationsByMachine(
            operationMatrix, problem.getMachineCount());
        
        // 2. 对每台机器计算能耗
        double totalEnergy = 0.0;
        int machineCount = problem.getMachineCount();
        
        if (debugOutput) {
            System.out.println("\n========== 能耗计算详情 ==========");
            System.out.println("启用开关机策略: " + enableSwitchingStrategy);
        }
        
        for (int machineId = 0; machineId < machineCount; machineId++) {
            List<Operation> ops = machineOperations.get(machineId);
            
            if (ops == null || ops.isEmpty()) {
                continue;
            }
            
            // 按开始时间排序
            ops.sort(Comparator.comparingDouble(o -> o.startTime));
            
            // 计算该机器的能耗
            double machineEnergy = calculateMachineEnergy(machineId, ops);
            totalEnergy += machineEnergy;
            
            if (debugOutput) {
                System.out.println(String.format("\n机器 %d: %.2f kWh (%d个工序)", 
                    machineId + 1, machineEnergy, ops.size()));
            }
        }
        
        if (debugOutput) {
            System.out.println(String.format("\n总能耗: %.2f kWh", totalEnergy));
            System.out.println("==================================\n");
        }
        
        return totalEnergy;
    }
    
    /**
     * 计算单台机器的能耗
     * 
     * @param machineId 机器ID (0-based)
     * @param operations 该机器上的所有工序（已按startTime排序）
     * @return 该机器的总能耗 (kWh)
     */
    private double calculateMachineEnergy(int machineId, List<Operation> operations) {
        PowerParameters params = machineParams.get(machineId);
        if (params == null) {
            // 如果没有配置参数，使用默认值
            params = PowerParameters.createDiscreteMachineDefault();
        }
        
        double energy = 0.0;
        
        // 1. 计算运行能耗（需要去重批处理工序）
        double runEnergy = 0.0;
        List<Operation> uniqueOperations = removeBatchDuplicates(operations);
        
        for (Operation op : uniqueOperations) {
            double durationSeconds = op.endTime - op.startTime;  // 调度系统使用秒
            double durationHours = durationSeconds * SECONDS_TO_HOURS;  // 转换为小时
            runEnergy += params.P_run * durationHours;  // P_run单位是kW，所以结果是kWh
        }
        energy += runEnergy;
        
        if (debugOutput && operations.size() != uniqueOperations.size()) {
            System.out.println(String.format("  批处理去重: %d个工序 -> %d个批次", 
                operations.size(), uniqueOperations.size()));
        }
        
        // 2. 计算空闲间隔能耗（使用去重后的工序列表）
        double idleEnergy = 0.0;
        int shutdownCount = 0;
        int idleCount = 0;
        
        // 2.1 处理起始空闲段（从时间0到第一个工序开始）
        if (!uniqueOperations.isEmpty()) {
            Operation firstOp = uniqueOperations.get(0);
            double initialGapSeconds = firstOp.startTime - 0.0;  // 秒
            double initialGapHours = initialGapSeconds * SECONDS_TO_HOURS;  // 转换为小时
            
            if (initialGapHours > 0) {
                // 根据策略计算起始空闲能耗
                double gapEnergy = 0.0;
                if (enableSwitchingStrategy) {
                    // 使用开关机策略（shouldShutdown内部参数也是小时）
                    if (params.shouldShutdown(initialGapHours * 60)) {
                        gapEnergy = params.E_switch / 60;
                        shutdownCount++;
                    } else {
                        gapEnergy = params.P_idle * initialGapHours;  // kW × 小时 = kWh
                        idleCount++;
                    }
                } else {
                    // 强制待机（用于对比实验）
                    gapEnergy = params.P_idle * initialGapHours;
                    idleCount++;
                }
                
                idleEnergy += gapEnergy;
                
                if (debugOutput && initialGapSeconds > 360) {  // 大于6分钟才输出
                    System.out.println(String.format("  起始空闲: gap=%.2f秒(%.4f小时), energy=%.4f kWh %s", 
                        initialGapSeconds, initialGapHours, gapEnergy,
                        (enableSwitchingStrategy && params.shouldShutdown(initialGapHours)) ? "(关机)" : "(待机)"));
                }
            }
        }
        
        // 2.2 处理工序之间的空闲间隔
        for (int i = 0; i < uniqueOperations.size() - 1; i++) {
            Operation current = uniqueOperations.get(i);
            Operation next = uniqueOperations.get(i + 1);
            
            double gapSeconds = next.startTime - current.endTime;  // 秒
            double gapHours = gapSeconds * SECONDS_TO_HOURS;  // 转换为小时
            
            if (gapHours <= 0) {
                continue; // 无空闲，跳过
            }
            
            // 根据策略计算空闲能耗
            double gapEnergy = 0.0;
            if (enableSwitchingStrategy) {
                // 使用开关机策略
                if (params.shouldShutdown(gapHours * 60)) {
                    gapEnergy = params.E_switch / 60;
                    shutdownCount++;
                } else {
                    gapEnergy = params.P_idle * gapHours;  // kW × 小时 = kWh
                    idleCount++;
                }
            } else {
                // 强制待机（用于对比实验）
                gapEnergy = params.P_idle * gapHours;
                idleCount++;
            }
            
            idleEnergy += gapEnergy;
            
            if (debugOutput && gapSeconds > 360) {  // 大于6分钟才输出
                System.out.println(String.format("  空闲 %d: gap=%.2f秒(%.4f小时), energy=%.4f kWh %s", 
                    i + 1, gapSeconds, gapHours, gapEnergy,
                    (enableSwitchingStrategy && params.shouldShutdown(gapHours)) ? "(关机)" : "(待机)"));
            }
        }
        
        energy += idleEnergy;
        
        if (debugOutput) {
            System.out.println(String.format("  运行能耗: %.2f kWh", runEnergy));
            System.out.println(String.format("  空闲能耗: %.2f kWh (关机%d次, 待机%d次)", 
                idleEnergy, shutdownCount, idleCount));
        }
        
        return energy;
    }
    
    /**
     * 按机器分组工序
     * 
     * @param operationMatrix 工序矩阵
     * @param machineCount 机器总数
     * @return Map<machineId, List<Operation>>
     */
    private Map<Integer, List<Operation>> groupOperationsByMachine(
            Operation[][] operationMatrix, int machineCount) {
        
        Map<Integer, List<Operation>> machineOps = new HashMap<>();
        
        // 初始化每台机器的工序列表
        for (int i = 0; i < machineCount; i++) {
            machineOps.put(i, new ArrayList<>());
        }
        
        // 收集所有工序并分组
        for (int i = 0; i < operationMatrix.length; i++) {
            for (int j = 0; j < operationMatrix[i].length; j++) {
                Operation op = operationMatrix[i][j];
                
                if (op != null && op.machineNo >= 0 && op.machineNo < machineCount) {
                    machineOps.get(op.machineNo).add(op);
                }
            }
        }
        
        return machineOps;
    }
    
    /**
     * 去除批处理设备的重复工序
     * 
     * 对于打印机和批处理机，同一批次的多个零件会有相同的startTime和endTime
     * 需要去重，避免重复计算能耗
     * 
     * @param operations 原始工序列表
     * @return 去重后的工序列表
     */
    private List<Operation> removeBatchDuplicates(List<Operation> operations) {
        if (operations == null || operations.isEmpty()) {
            return operations;
        }
        
        List<Operation> unique = new ArrayList<>();
        
        for (Operation op : operations) {
            boolean isDuplicate = false;
            
            // 检查是否与已有的工序重复（同时间段）
            for (Operation existingOp : unique) {
                // 如果startTime和endTime都相同，认为是同一批次
                // 使用小的误差容忍度（0.001）来处理浮点数比较
                if (Math.abs(op.startTime - existingOp.startTime) < 0.001 &&
                    Math.abs(op.endTime - existingOp.endTime) < 0.001) {
                    isDuplicate = true;
                    break;
                }
            }
            
            if (!isDuplicate) {
                unique.add(op);
            }
        }
        
        return unique;
    }
    
    /**
     * 获取能耗统计信息（用于分析）
     */
    public EnergyStatistics getStatistics(Operation[][] operationMatrix, Problem problem) {
        Map<Integer, List<Operation>> machineOperations = groupOperationsByMachine(
            operationMatrix, problem.getMachineCount());
        
        EnergyStatistics stats = new EnergyStatistics();
        stats.totalEnergy = calculateTotalEnergy(operationMatrix, problem);
        stats.machineCount = problem.getMachineCount();
        stats.runEnergy = 0.0;
        stats.idleEnergy = 0.0;
        stats.shutdownCount = 0;
        stats.idleCount = 0;
        
        // 统计每台机器的能耗和开关机次数
        for (int machineId = 0; machineId < problem.getMachineCount(); machineId++) {
            List<Operation> ops = machineOperations.get(machineId);
            
            if (ops == null || ops.isEmpty()) {
                continue;
            }
            
            ops.sort((o1, o2) -> Double.compare(o1.startTime, o2.startTime));
            
            PowerParameters params = machineParams.get(machineId);
            if (params == null) {
                params = PowerParameters.createDiscreteMachineDefault();
            }
            
            // 去重批处理工序
            List<Operation> uniqueOps = removeBatchDuplicates(ops);
            
            // 计算运行能耗（使用去重后的列表，需要单位转换）
            for (Operation op : uniqueOps) {
                double durationHours = (op.endTime - op.startTime) * SECONDS_TO_HOURS;
                stats.runEnergy += params.P_run * durationHours;
            }
            
            // 统计开关机次数和空闲能耗（使用去重后的列表，需要单位转换）
            
            // 1. 处理起始空闲段（从时间0到第一个工序开始）
            if (!uniqueOps.isEmpty()) {
                Operation firstOp = uniqueOps.get(0);
                double initialGapHours = firstOp.startTime * SECONDS_TO_HOURS;
                
                if (initialGapHours > 0) {
                    if (enableSwitchingStrategy && params.shouldShutdown(initialGapHours)) {
                        stats.shutdownCount++;
                        stats.idleEnergy += params.E_switch;
                    } else {
                        stats.idleCount++;
                        stats.idleEnergy += params.P_idle * initialGapHours;
                    }
                }
            }
            
            // 2. 处理工序之间的空闲间隔
            for (int i = 0; i < uniqueOps.size() - 1; i++) {
                double gapSeconds = uniqueOps.get(i + 1).startTime - uniqueOps.get(i).endTime;
                double gapHours = gapSeconds * SECONDS_TO_HOURS;
                if (gapHours > 0) {
                    if (enableSwitchingStrategy && params.shouldShutdown(gapHours)) {
                        stats.shutdownCount++;
                        stats.idleEnergy += params.E_switch;
                    } else {
                        stats.idleCount++;
                        stats.idleEnergy += params.P_idle * gapHours;
                    }
                }
            }
        }
        
        return stats;
    }
    
    /**
     * 能耗统计信息类
     */
    public static class EnergyStatistics {
        public double totalEnergy;      // 总能耗
        public double runEnergy;        // 运行能耗
        public double idleEnergy;       // 空闲能耗
        public int machineCount;        // 机器总数
        public int shutdownCount;       // 总关机次数
        public int idleCount;           // 总待机次数
        
        @Override
        public String toString() {
            return String.format("EnergyStatistics[total=%.2f kWh, run=%.2f, idle=%.2f, machines=%d, shutdowns=%d, idles=%d]",
                totalEnergy, runEnergy, idleEnergy, machineCount, shutdownCount, idleCount);
        }
    }
}

