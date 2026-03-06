package ProgramEntity;

import ProblemFrame.PowerParameters;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 增强版输入类 - 支持读取能耗参数
 * 
 * 兼容两种算例格式：
 * 1. 原有格式（不含能耗参数）- 使用默认能耗参数
 * 2. 新格式（含能耗参数）- 从文件读取能耗参数
 * 
 * @author AI Assistant
 * @version 2.0 (支持能耗参数)
 */
public class EnergyAwareInput extends Input {
    
    /** 机器能耗参数映射 (machineId -> PowerParameters) */
    private Map<Integer, PowerParameters> machineEnergyParams;
    
    /** 是否包含能耗参数（自动检测） */
    private boolean hasEnergyParams;
    
    /**
     * 构造函数
     */
    public EnergyAwareInput(File inputFile) {
        super(inputFile);
        this.machineEnergyParams = new HashMap<>();
        this.hasEnergyParams = false;
    }
    
    /**
     * 从文件读取问题描述（增强版）
     * 
     * 自动检测算例格式：
     * - 如果文件包含能耗参数，则读取并设置到机器对象
     * - 如果不包含，则使用默认参数
     */
    @Override
    public Problem getProblemDesFromFile() {
        // 1. 先调用父类方法，读取基本信息（包括能耗参数字符串）
        Problem problem = super.getProblemDesFromFile();
        
        // 2. 检查Problem对象中是否有能耗参数
        if (problem.hasEnergyParams()) {
            System.out.println("✅ 检测到能耗参数，开始解析...");
            try {
                // 从Problem对象中解析能耗参数
                parseEnergyParametersFromProblem(problem);
                // 将能耗参数设置到机器对象
                applyEnergyParametersToMachines(problem);
                hasEnergyParams = true;
                System.out.println("✅ 成功解析并应用能耗参数");
            } catch (Exception e) {
                System.err.println("⚠️ 解析能耗参数失败: " + e.getMessage());
                hasEnergyParams = false;
                // 使用默认参数
                initializeDefaultEnergyParams(problem);
                applyEnergyParametersToMachines(problem);
            }
        } else {
            System.out.println("⚠️ 未找到能耗参数，使用默认值");
            hasEnergyParams = false;
            // 使用默认参数
            initializeDefaultEnergyParams(problem);
            // 设置到机器对象
            applyEnergyParametersToMachines(problem);
        }
        
        return problem;
    }
    
    /**
     * 从Problem对象中解析能耗参数
     */
    private void parseEnergyParametersFromProblem(Problem problem) {
        int printMachineCount = problem.getPrintMachineCount();
        int batchMachineCount = problem.getBatchMachineCount();
        int totalMachineCount = problem.getMachineCount();
        int discreteMachineCount = totalMachineCount - printMachineCount - batchMachineCount;
        
        // 解析打印机能耗参数
        String printerParams = problem.getPrinterEnergyParams();
        if (printerParams != null && !printerParams.isEmpty()) {
            String[] tokens = printerParams.split("\\s+");
            parseAndStoreMachineTypeEnergy(tokens, 0, printMachineCount);
        }
        
        // 解析批处理机能耗参数
        String batchParams = problem.getBatchEnergyParams();
        if (batchParams != null && !batchParams.isEmpty()) {
            String[] tokens = batchParams.split("\\s+");
            parseAndStoreMachineTypeEnergy(tokens, printMachineCount, batchMachineCount);
        }
        
        // 解析离散加工机能耗参数
        String discreteParams = problem.getDiscreteEnergyParams();
        if (discreteParams != null && !discreteParams.isEmpty()) {
            String[] tokens = discreteParams.split("\\s+");
            parseAndStoreMachineTypeEnergy(tokens, printMachineCount + batchMachineCount, discreteMachineCount);
        }
    }
    
    /**
     * 解析某一类机器的能耗参数并存储
     * 
     * @param tokens 参数数组
     * @param startMachineId 起始机器ID (0-based)
     * @param expectedCount 预期机器数量
     */
    private void parseAndStoreMachineTypeEnergy(String[] tokens, int startMachineId, int expectedCount) {
        // 格式: count [machineId P_run P_idle E_switch T_warmup allowSwitch] ...
        // 每台机器6个参数
        
        if (tokens.length < 1) {
            System.err.println("警告: 能耗参数格式错误");
            return;
        }
        
        int count = Integer.parseInt(tokens[0]);
        if (count != expectedCount) {
            System.err.println("警告: 能耗参数机器数量不匹配 (期望:" + expectedCount + ", 实际:" + count + ")");
        }
        
        int expectedTokens = 1 + count * 6;
        if (tokens.length < expectedTokens) {
            System.err.println("警告: 能耗参数数量不足，使用默认值");
            return;
        }
        
        for (int i = 0; i < count; i++) {
            int baseIndex = 1 + i * 6;
            
            try {
                int machineId = Integer.parseInt(tokens[baseIndex]);  // 1-based
                double P_run = Double.parseDouble(tokens[baseIndex + 1]);
                double P_idle = Double.parseDouble(tokens[baseIndex + 2]);
                double E_switch = Double.parseDouble(tokens[baseIndex + 3]);
                double T_warmup = Double.parseDouble(tokens[baseIndex + 4]);
                boolean allowSwitch = Integer.parseInt(tokens[baseIndex + 5]) == 1;
                
                PowerParameters params = new PowerParameters(
                    P_run, P_idle, E_switch, T_warmup, allowSwitch);
                
                // 存储（使用0-based索引）
                machineEnergyParams.put(startMachineId + i, params);
                
                if (debugOutput()) {
                    System.out.println(String.format("  解析机器 %d: P_run=%.2fkW P_idle=%.2fkW E_switch=%.2fkWh", 
                        startMachineId + i + 1, P_run, P_idle, E_switch));
                }
                
            } catch (Exception e) {
                System.err.println("警告: 解析机器" + (startMachineId + i + 1) + 
                    "的能耗参数失败: " + e.getMessage());
            }
        }
    }
    
    /**
     * 初始化默认能耗参数（当文件不包含能耗参数时）
     */
    private void initializeDefaultEnergyParams(Problem problem) {
        ProgramEntity.Machine.Machine[] machines = problem.getMachines();
        
        for (int i = 0; i < machines.length; i++) {
            PowerParameters params;
            
            if (machines[i] instanceof ProgramEntity.Machine.PrintMachine) {
                params = PowerParameters.createPrintMachineDefault();
            } else if (machines[i] instanceof ProgramEntity.Machine.BathchMachine) {
                params = PowerParameters.createBatchMachineDefault();
            } else {
                params = PowerParameters.createDiscreteMachineDefault();
            }
            
            machineEnergyParams.put(i, params);
        }
    }
    
    /**
     * 将能耗参数应用到机器对象
     * 
     * 这样机器对象就直接携带能耗信息，EnergyCalculator可以直接读取
     */
    private void applyEnergyParametersToMachines(Problem problem) {
        ProgramEntity.Machine.Machine[] machines = problem.getMachines();
        
        for (Map.Entry<Integer, PowerParameters> entry : machineEnergyParams.entrySet()) {
            int machineId = entry.getKey();
            PowerParameters params = entry.getValue();
            
            if (machineId >= 0 && machineId < machines.length) {
                machines[machineId].setPowerParameters(params);
            }
        }
        
        if (debugOutput()) {
            System.out.println("✅ 已将能耗参数设置到机器对象");
        }
    }
    
    /**
     * 调试输出开关（可配置）
     */
    private boolean debugOutput() {
        return false;  // 默认关闭，需要时可改为true
    }
    
    /**
     * 获取机器能耗参数
     * 
     * @param machineId 机器ID (0-based)
     * @return 能耗参数，如果未找到则返回null
     */
    public PowerParameters getMachineEnergyParams(int machineId) {
        return machineEnergyParams.get(machineId);
    }
    
    /**
     * 获取所有机器的能耗参数
     */
    public Map<Integer, PowerParameters> getAllEnergyParams() {
        return new HashMap<>(machineEnergyParams);
    }
    
    /**
     * 是否包含能耗参数
     */
    public boolean hasEnergyParams() {
        return hasEnergyParams;
    }
    
    /**
     * 打印能耗参数摘要
     */
    public void printEnergyParamsSummary() {
        System.out.println("\n========== 机器能耗参数 ==========");
        
        for (Map.Entry<Integer, PowerParameters> entry : machineEnergyParams.entrySet()) {
            int machineId = entry.getKey();
            PowerParameters params = entry.getValue();
            
            System.out.println(String.format("机器 %d: %s", 
                machineId + 1, params.toString()));
        }
        
        System.out.println("==================================\n");
    }
    
    /**
     * 测试示例
     */
    public static void main(String[] args) {
        try {
            System.out.println("========== 测试能耗参数读取 ==========\n");
            
            // 测试1: 读取含能耗参数的算例
            System.out.println("【测试1】读取含能耗参数的算例");
            File energyFile = new File("chapter-3/src/main/resources/instance/test_energy.txt");
            
            if (energyFile.exists()) {
                EnergyAwareInput input1 = new EnergyAwareInput(energyFile);
                Problem problem1 = input1.getProblemDesFromFile();
                
                System.out.println("  ✓ 问题加载成功");
                System.out.println("  ✓ 工件数: " + problem1.getJobCount());
                System.out.println("  ✓ 机器数: " + problem1.getMachineCount());
                System.out.println("  ✓ 包含能耗参数: " + input1.hasEnergyParams());
                
                // 打印能耗参数
                input1.printEnergyParamsSummary();
                
                // 测试获取单个机器的参数
                PowerParameters p0 = input1.getMachineEnergyParams(0);
                if (p0 != null) {
                    System.out.println("机器0的盈亏平衡时间: " + 
                        String.format("%.2f", p0.getBreakEvenTime()));
                }
            } else {
                System.out.println("  ⚠️ 测试文件不存在，请先运行 EnergyAwareInstanceGenerator");
            }
            
            // 测试2: 读取不含能耗参数的算例（向后兼容）
            System.out.println("\n【测试2】读取不含能耗参数的算例（向后兼容）");
            File regularFile = new File("chapter-2/src/main/resources/instance/J20/J20P3B2D5_01.txt");
            
            if (regularFile.exists()) {
                EnergyAwareInput input2 = new EnergyAwareInput(regularFile);
                Problem problem2 = input2.getProblemDesFromFile();
                
                System.out.println("  ✓ 问题加载成功（使用默认能耗参数）");
                System.out.println("  ✓ 工件数: " + problem2.getJobCount());
                System.out.println("  ✓ 机器数: " + problem2.getMachineCount());
                System.out.println("  ✓ 包含能耗参数: " + input2.hasEnergyParams());
                
                // 打印默认能耗参数
                System.out.println("\n默认能耗参数（前3台机器）:");
                for (int i = 0; i < Math.min(3, problem2.getMachineCount()); i++) {
                    PowerParameters p = input2.getMachineEnergyParams(i);
                    if (p != null) {
                        System.out.println("  机器 " + (i+1) + ": " + p.toString());
                    }
                }
            } else {
                System.out.println("  ⚠️ 测试文件不存在");
            }
            
            System.out.println("\n========== 测试完成 ==========");
            
        } catch (Exception e) {
            System.err.println("测试出错: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

