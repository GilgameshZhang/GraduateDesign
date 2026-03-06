package util.java;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

/**
 * 第三章算例生成器 - 含能耗参数
 * 
 * 生成适用于MOGWO多目标优化算法的测试算例
 * 优化目标: 最小化makespan和能耗
 * 
 * 相比第二章的改进:
 * 1. 新增机器能耗参数配置
 * 2. 支持自定义能耗参数范围
 * 3. 提供典型场景模板 (节能型/标准型/重载型)
 * 
 * @author AI Assistant
 * @version 2.0
 */
public class EnergyAwareInstanceGenerator {
    
    private Random random;
    
    /** 能耗参数配置模板（已简化为统一配置） */
    public enum EnergyProfile {
        /** 统一配置 - 所有机器使用相同的随机范围 */
        UNIFIED
    }
    
    /**
     * 构造函数
     */
    public EnergyAwareInstanceGenerator(long seed) {
        this.random = new Random(seed);
    }
    
    /**
     * 生成单个算例文件
     * 
     * @param jobCount 工件数
     * @param printMachineCount 打印机数
     * @param batchMachineCount 批处理机数
     * @param discreteMachineCount 离散加工机数 (通常为0)
     * @param outputPath 输出文件路径
     * @param printerProfile 打印机能耗配置
     * @param batchProfile 批处理机能耗配置
     */
    public void generateInstance(
            int jobCount,
            int printMachineCount,
            int batchMachineCount,
            int discreteMachineCount,
            String outputPath,
            EnergyProfile printerProfile,
            EnergyProfile batchProfile) {
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputPath))) {
            
            System.out.println("\n========== 生成算例 ==========");
            System.out.println("文件: " + outputPath);
            System.out.println("配置: J" + jobCount + "P" + printMachineCount + 
                             "B" + batchMachineCount + "D" + discreteMachineCount);
            
            int totalMachines = printMachineCount + batchMachineCount + discreteMachineCount;
            
            // 第1行: 问题规模
            writer.write(totalMachines + " " + jobCount + "\n");
            
            // 第2行: 打印机配置
            writePrintMachineConfig(writer, printMachineCount);
            
            // 第3行: 批处理机配置
            writeBatchMachineConfig(writer, batchMachineCount);
            
            // 第4行: 打印机能耗参数 *** 新增 ***
            writePrintMachineEnergyParams(writer, printMachineCount, printerProfile);
            
            // 第5行: 批处理机能耗参数 *** 新增 ***
            writeBatchMachineEnergyParams(writer, batchMachineCount, batchProfile);
            
            // 第6行: 离散加工机能耗参数 *** 新增 ***
            writeDiscreteMachineEnergyParams(writer, discreteMachineCount);
            
            // 第7行起: 工件信息
            writeJobsInfo(writer, jobCount, printMachineCount, batchMachineCount, discreteMachineCount);
            
            System.out.println("✓ 算例生成完成");
            System.out.println("==============================\n");
            
        } catch (IOException e) {
            System.err.println("生成算例失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 写入打印机配置
     */
    private void writePrintMachineConfig(BufferedWriter writer, int count) throws IOException {
        writer.write(count + "");
        
        for (int i = 0; i < count; i++) {
            int id = i + 1;
            // 随机生成打印平台尺寸 (800-1200范围)
            double L = 800 + random.nextInt(400);
            double W = 400 + random.nextInt(200);
            double H = 500 + random.nextInt(200);
            // 打印速度 (0.08-0.12 mm/s)
            double printSpeed = Math.round((random.nextDouble() * 0.095 + 0.025) * 1000.0) / 1000.0;
            // 记录时间 (10-20秒)
            double recordTime = 10 + random.nextInt(11);
            // 准备时间 (300-600秒)
            double prepareTime = 300 + random.nextInt(301);
            
            writer.write(String.format(" %d %.0f %.0f %.0f %.3f %.0f %.0f",
                id, L, W, H, printSpeed, prepareTime, recordTime));
        }
        writer.write("\n");
    }
    
    /**
     * 写入批处理机配置
     */
    private void writeBatchMachineConfig(BufferedWriter writer, int count) throws IOException {
        writer.write(count + "");
        
        for (int i = 0; i < count; i++) {
            int id = i + 1;
            // 批处理容量 (800-1200)
            double capacity = 800 + random.nextInt(401);
            
            writer.write(String.format(" %d %.0f", id, capacity));
        }
        writer.write("\n");
    }
    
    /**
     * 写入打印机能耗参数 *** 核心新功能 ***
     */
    private void writePrintMachineEnergyParams(BufferedWriter writer, int count, 
                                               EnergyProfile profile) throws IOException {
        writer.write(count + "");
        
        for (int i = 0; i < count; i++) {
            int id = i + 1;
            PowerParams params = getPowerParams(profile, "Printer", i);
            
            writer.write(String.format(" %d %.2f %.2f %.2f %.0f %d",
                id, params.P_run, params.P_idle, params.E_switch, 
                params.T_warmup, params.allowSwitch ? 1 : 0));
        }
        writer.write("\n");
    }
    
    /**
     * 写入批处理机能耗参数 *** 核心新功能 ***
     */
    private void writeBatchMachineEnergyParams(BufferedWriter writer, int count, 
                                               EnergyProfile profile) throws IOException {
        writer.write(count + "");
        
        for (int i = 0; i < count; i++) {
            int id = i + 1;
            PowerParams params = getPowerParams(profile, "Batch", i);
            
            writer.write(String.format(" %d %.2f %.2f %.2f %.0f %d",
                id, params.P_run, params.P_idle, params.E_switch, 
                params.T_warmup, params.allowSwitch ? 1 : 0));
        }
        writer.write("\n");
    }
    
    /**
     * 写入离散加工机能耗参数
     */
    private void writeDiscreteMachineEnergyParams(BufferedWriter writer, int count) throws IOException {
        writer.write(count + "");
        
        // 如果有离散加工机，输出其能耗参数
        for (int i = 0; i < count; i++) {
            int id = i + 1;
            PowerParams params = getPowerParams(EnergyProfile.UNIFIED, "Discrete", i);
            
            writer.write(String.format(" %d %.2f %.2f %.2f %.0f %d",
                id, params.P_run, params.P_idle, params.E_switch, 
                params.T_warmup, params.allowSwitch ? 1 : 0));
        }
        writer.write("\n");
    }
    
    /**
     * 写入工件信息（使用混合分布生成尺寸）
     */
    private void writeJobsInfo(BufferedWriter writer, int jobCount, int printCount, 
                               int batchCount, int discreteCount) throws IOException {
        
        int totalMachines = printCount + batchCount + discreteCount;
        
        for (int jobId = 0; jobId < jobCount; jobId++) {
            // 工序数 (2-10)
            int opsCount = 2 + random.nextInt(9);
            
            // 工件尺寸 - 使用混合分布策略
            double L, W, H;
            
            // 随机数决定尺寸类别
            double sizeCategory = random.nextDouble();
            
            if (sizeCategory < 0.40) {
                // 小件（40%）: L∈[50,220], W∈[50,160]
                L = 50 + random.nextInt(171);   // 50-220
                W = 50 + random.nextInt(111);   // 50-160
            } else if (sizeCategory < 0.80) {
                // 中件（40%）: L∈[120,480], W∈[80,260]
                L = 120 + random.nextInt(361);  // 120-480
                W = 80 + random.nextInt(181);   // 80-260
            } else {
                // 大件（20%）: L∈[250,760], W∈[120,360]
                L = 250 + random.nextInt(511);  // 250-760
                W = 120 + random.nextInt(241);  // 120-360
            }
            
            // 高度混合分布：80%矮件，20%高件
            double heightCategory = random.nextDouble();
            if (heightCategory < 0.8) {
                // 矮件（80%）: H=10-50
                H = 10 + random.nextInt(90);   // 10-100
            } else {
                // 高件（20%）: H=300-480
                H = 100 + random.nextInt(100);  // 100-200
            }
            
            writer.write(String.format("%d %.0f %.0f %.0f %d",
                opsCount, L, W, H, opsCount));
            
            // 为每道工序生成备选机器和加工时间
            for (int opId = 0; opId < opsCount; opId++) {
                // 根据工序类型确定可选机器范围
                int machineStart, machineEnd, machineTypeCount;
                
                if (opId == 0) {
                    // 第1道工序：打印工序，只能选择打印机 (0 to printCount-1)
                    machineStart = 0;
                    machineEnd = printCount;
                    machineTypeCount = printCount;
                } else if (opId == 1) {
                    // 第2道工序：批处理工序，只能选择批处理机 (printCount to printCount+batchCount-1)
                    machineStart = printCount;
                    machineEnd = printCount + batchCount;
                    machineTypeCount = batchCount;
                } else {
                    // 第3道及以后工序：离散加工工序，只能选择离散加工机 (printCount+batchCount to totalMachines-1)
                    machineStart = printCount + batchCount;
                    machineEnd = totalMachines;
                    machineTypeCount = discreteCount;
                }
                
                // 备选机器数 (1到该类型机器总数，最多3台)
                int maxCandidates = Math.min(3, machineTypeCount);
                int candidateCount = 1 + random.nextInt(maxCandidates);
                writer.write(" " + candidateCount);
                
                // 从该类型机器中随机选择candidateCount台
                boolean[] selected = new boolean[machineTypeCount];
                int selectedCount = 0;
                
                while (selectedCount < candidateCount) {
                    int relativeId = random.nextInt(machineTypeCount);
                    if (!selected[relativeId]) {
                        selected[relativeId] = true;
                        selectedCount++;
                        
                        int actualMachineId = machineStart + relativeId;
                        
                        // 根据工序类型生成不同的加工时间
                        int processingTime;
                        if (opId == 0) {
                            // 打印工序：时间较长 (1200-3600秒, 20-60分钟)
                            processingTime = 0;
                        } else if (opId == 1) {
                            // 批处理工序：时间中等 (900-1800秒, 15-30分钟)
                            processingTime = 0;
                        } else {
                            // 离散加工工序：时间较短 (300-900秒, 5-15分钟)
                            processingTime = 900 + random.nextInt(901);
                        }
                        
                        writer.write(String.format(" %d %d", actualMachineId + 1, processingTime));
                    }
                }
            }
            
            writer.write("\n");
        }
    }
    
    /**
     * 生成统一的能耗参数（所有机器使用相同的随机范围）
     * 
     * @param profile 能耗配置类型（已简化，只有UNIFIED）
     * @param machineType 机器类型（保留参数但不影响结果）
     * @param index 机器索引（用于随机种子）
     * @return 能耗参数对象
     */
    private PowerParams getPowerParams(EnergyProfile profile, String machineType, int index) {
        PowerParams params = new PowerParams();
        
        // 统一配置 - 所有机器都允许开关机，使用相同的参数范围
        params.P_run = 3.0 + random.nextDouble() * 2.0;       // 3-5 kW (加工功率)
        params.P_idle = 1.0 + random.nextDouble() * 1.0;      // 1-2 kW (空载功率)
        params.E_switch = 5.0 + random.nextDouble() * 5.0;    // 5-10 kWh (开关机能耗)
        params.T_warmup = 60 + random.nextInt(241);           // 60-300秒 (预热时间)
        params.allowSwitch = true;                            // 所有机器都允许开关机
        
        return params;
    }
    
    /**
     * 能耗参数数据类
     */
    private static class PowerParams {
        double P_run;        // 运行功率 (kW)
        double P_idle;       // 待机功率 (kW)
        double E_switch;     // 开关机能耗 (kWh)
        double T_warmup;     // 预热时间 (秒)
        boolean allowSwitch; // 是否允许关机
    }
    
    /**
     * 批量生成标准测试集
     */
    public static void generateStandardTestSet(String outputDir) {
        System.out.println("========== 生成标准测试集 ==========\n");
        
        // 确保输出目录存在
        new File(outputDir).mkdirs();
        
        // 小规模: J10 系列 (2打印+1批处理+2离散)
        System.out.println("--- 生成小规模算例 (J10) ---");
        for (int i = 1; i <= 5; i++) {
            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(1000 + i);
            String filename = String.format("%s/J10P2B1D2_%02d.txt", outputDir, i);
            gen.generateInstance(10, 2, 1, 2, filename, 
                EnergyProfile.UNIFIED, EnergyProfile.UNIFIED);
        }
        
        // 中规模: J20 系列 (3打印+2批处理+5离散)
        System.out.println("\n--- 生成中规模算例 (J20) ---");
        for (int i = 1; i <= 5; i++) {
            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(2000 + i);
            String filename = String.format("%s/J20P3B2D5_%02d.txt", outputDir, i);
            gen.generateInstance(20, 3, 2, 5, filename,
                EnergyProfile.UNIFIED, EnergyProfile.UNIFIED);
        }
        
        // 大规模: J50 系列 (4打印+3批处理+8离散)
        System.out.println("\n--- 生成大规模算例 (J50) ---");
        for (int i = 1; i <= 5; i++) {
            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(5000 + i);
            String filename = String.format("%s/J50P4B3D8_%02d.txt", outputDir, i);
            gen.generateInstance(50, 4, 2, 8, filename,
                EnergyProfile.UNIFIED, EnergyProfile.UNIFIED);
        }
        
        // 超大规模: J100 系列 (5打印+4批处理+10离散)
        System.out.println("\n--- 生成超大规模算例 (J100) ---");
        for (int i = 1; i <= 5; i++) {
            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(10000 + i);
            String filename = String.format("%s/J100P5B4D10_%02d.txt", outputDir, i);
            gen.generateInstance(100, 5, 2, 10, filename,
                EnergyProfile.UNIFIED, EnergyProfile.UNIFIED);
        }
        
        System.out.println("\n========== 测试集生成完成 ==========");
        System.out.println("总计: 20个算例");
        System.out.println("位置: " + outputDir);
    }
    
    /**
     * 生成能耗对比实验算例（已简化，使用统一配置）
     */
    public static void generateEnergyComparisonSet(String outputDir) {
        System.out.println("========== 生成对比实验算例 ==========\n");
        
        String compDir = outputDir + "/energy_comparison";
        new File(compDir).mkdirs();
        
        // 固定问题规模: J20P3B2D5，生成多个随机种子的算例
        int[] seeds = {3001, 3002, 3003, 3004, 3005};
        
        for (int seedIdx = 0; seedIdx < seeds.length; seedIdx++) {
            EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(seeds[seedIdx]);
            String filename = String.format("%s/J20P3B2D5_%02d.txt", compDir, seedIdx + 1);
            
            gen.generateInstance(20, 3, 2, 5, filename,
                EnergyProfile.UNIFIED, EnergyProfile.UNIFIED);
        }
        
        System.out.println("\n========== 对比实验算例生成完成 ==========");
        System.out.println("总计: " + seeds.length + "个算例");
        System.out.println("位置: " + compDir);
    }
    
    /**
     * 主函数 - 测试和批量生成
     */
    public static void main(String[] args) {
        System.out.println("第三章算例生成器 v2.0 - 统一能耗参数配置\n");
        System.out.println("能耗参数配置:");
        System.out.println("  - 加工功率: 3-5 kW (随机)");
        System.out.println("  - 空载功率: 1-2 kW (随机)");
        System.out.println("  - 开关机能耗: 5-10 kWh (随机)");
        System.out.println("  - 预热时间: 60-300秒 (随机)");
        System.out.println("  - 所有机器都允许开关机\n");
        
        // 方案1: 生成标准测试集 (推荐)
        String outputDir = "chapter-3/src/main/resources/instance";
        generateStandardTestSet(outputDir);
        
        // 方案2: 生成能耗对比实验算例
        // generateEnergyComparisonSet(outputDir);
        
        // 方案3: 生成单个自定义算例
        // EnergyAwareInstanceGenerator gen = new EnergyAwareInstanceGenerator(12345);
        // gen.generateInstance(
        //     30,                          // 30个工件
        //     3,                           // 3台打印机
        //     2,                           // 2台批处理机
        //     5,                           // 5台离散机
        //     outputDir + "/custom_J30P3B2D5_01.txt",
        //     EnergyProfile.UNIFIED,       // 统一配置
        //     EnergyProfile.UNIFIED        // 统一配置
        // );
        
        System.out.println("\n========== 所有任务完成 ==========");
        System.out.println("提示: 使用 EnergyAwareInput 读取生成的算例");
        System.out.println("提示: 查看 算例格式说明.txt 了解详细格式");
    }
}
