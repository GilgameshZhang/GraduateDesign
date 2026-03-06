# 算例生成器使用指南 - 能耗参数版

## 📚 概述

`EnergyAwareInstanceGenerator` 是增强版的算例生成器，支持生成包含机器能耗参数的算例文件，用于绿色车间调度研究。

### 核心特性

✅ **能耗参数生成**：为每台机器自动生成真实的能耗参数  
✅ **向后兼容**：可选择生成带/不带能耗参数的算例  
✅ **多机器类型**：打印机、批处理机、离散加工机分别设置  
✅ **真实参数范围**：基于实际3D打印车间的典型值  
✅ **批量生成**：支持生成多个场景的算例集  

---

## 🎯 算例格式对比

### 原有格式（chapter-2兼容）

```
10 20                          ← 总机器数 工件数
3 1 800 500 560 0.073 451 17 ... ← 打印机配置
2 1 1050 2 840                 ← 批处理机配置
4 209 236 57 4 1 1 0 ...       ← 工件数据
...
```

### 新格式（含能耗参数，chapter-3）

```
10 20                          ← 总机器数 工件数
3 1 800 500 560 0.073 451 17 ... ← 打印机配置
2 1 1050 2 840                 ← 批处理机配置
3 1 5.23 0.65 2.14 6.78 1 ...  ← [新增] 打印机能耗参数
2 1 8.45 1.87 18.32 35.12 0 ... ← [新增] 批处理机能耗参数
5 1 3.45 0.34 1.23 2.67 1 ...  ← [新增] 离散加工机能耗参数
4 209 236 57 4 1 1 0 ...       ← 工件数据
...
```

### 能耗参数格式说明

每行格式：`机器数 [machineId P_run P_idle E_switch T_warmup allowSwitch] ...`

字段说明：
- `machineId`: 机器编号（1-based）
- `P_run`: 运行功率 (kW)
- `P_idle`: 待机功率 (kW)
- `E_switch`: 开关机能耗 (kWh)
- `T_warmup`: 预热时间（单位与调度时间一致）
- `allowSwitch`: 是否允许关机（1=允许, 0=不允许）

---

## 🚀 快速开始

### 1. 生成单个算例（含能耗参数）

```java
import util.java.EnergyAwareInstanceGenerator;

public class Example1 {
    public static void main(String[] args) throws Exception {
        // 创建生成器（seed=42, 包含能耗参数=true）
        EnergyAwareInstanceGenerator generator = 
            new EnergyAwareInstanceGenerator(42, true);
        
        // 生成算例: 20工件, 3打印机, 2批处理机, 5离散机
        generator.generateInstance(20, 3, 2, 5, 
            "chapter-3/src/main/resources/instance/my_instance.txt");
        
        System.out.println("✅ 算例生成完成！");
    }
}
```

### 2. 批量生成算例

```java
// 生成10个中等规模算例
EnergyAwareInstanceGenerator generator = 
    new EnergyAwareInstanceGenerator(42, true);

generator.generateMultipleInstances(
    20,                    // jobCount
    3,                     // printerCount
    2,                     // batchCount
    5,                     // discreteCount
    10,                    // instanceCount
    "chapter-3/src/main/resources/instance/medium"  // outputDir
);

// 输出文件: J20P3B2D5_01_energy.txt, J20P3B2D5_02_energy.txt, ...
```

### 3. 生成预定义场景

```java
EnergyAwareInstanceGenerator generator = 
    new EnergyAwareInstanceGenerator(42, true);

generator.generateScenarioInstances(
    "chapter-3/src/main/resources/instance"
);

// 自动生成4种规模的算例集:
// - small:   J10P2B1D3  (5个算例)
// - medium:  J20P3B2D5  (10个算例)
// - large:   J50P4B3D10 (10个算例)
// - xlarge:  J100P5B4D15 (5个算例)
```

---

## 📊 能耗参数说明

### 打印机能耗参数

| 参数 | 范围 | 说明 |
|------|------|------|
| P_run | 4.0 - 8.0 kW | 运行功率（加热+电机） |
| P_idle | 0.3 - 0.8 kW | 待机功率（保持温度） |
| E_switch | 1.0 - 3.0 kWh | 开关机能耗 |
| T_warmup | 3.0 - 10.0 | 预热时间（分钟） |
| allowSwitch | 允许 | 适合长间隔关机 |

**盈亏平衡时间**：T_be = E_switch / P_idle ≈ 2-10  
**节能潜力**：中等（适合长空闲间隔关机）

### 批处理机能耗参数

| 参数 | 范围 | 说明 |
|------|------|------|
| P_run | 5.0 - 15.0 kW | 运行功率（热处理炉） |
| P_idle | 1.0 - 3.0 kW | 待机功率（保温） |
| E_switch | 10.0 - 30.0 kWh | 开关机能耗很高 |
| T_warmup | 20.0 - 60.0 | 预热时间很长 |
| allowSwitch | 不允许 | 需长时间保温 |

**盈亏平衡时间**：T_be = E_switch / P_idle ≈ 10-30  
**节能潜力**：低（通常不关机）

### 离散加工机能耗参数

| 参数 | 范围 | 说明 |
|------|------|------|
| P_run | 2.0 - 6.0 kW | 运行功率（CNC） |
| P_idle | 0.2 - 0.5 kW | 待机功率（低） |
| E_switch | 0.5 - 2.0 kWh | 开关机能耗较低 |
| T_warmup | 1.0 - 5.0 | 预热时间短 |
| allowSwitch | 允许 | 快速启动 |

**盈亏平衡时间**：T_be = E_switch / P_idle ≈ 2-10  
**节能潜力**：高（适合频繁开关机）

---

## 📂 算例读取

### 使用 EnergyAwareInput 读取算例

```java
import ProgramEntity.EnergyAwareInput;
import ProgramEntity.Problem;
import ProblemFrame.PowerParameters;
import java.io.File;

public class Example2 {
    public static void main(String[] args) {
        // 1. 创建输入对象
        File instanceFile = new File(
            "chapter-3/src/main/resources/instance/my_instance.txt");
        EnergyAwareInput input = new EnergyAwareInput(instanceFile);
        
        // 2. 读取问题（自动检测是否包含能耗参数）
        Problem problem = input.getProblemDesFromFile();
        
        // 3. 检查是否读取到能耗参数
        if (input.hasEnergyParams()) {
            System.out.println("✅ 算例包含能耗参数");
            
            // 4. 获取机器能耗参数
            for (int i = 0; i < problem.getMachineCount(); i++) {
                PowerParameters params = input.getMachineEnergyParams(i);
                System.out.println("机器 " + (i+1) + ": " + params);
            }
        } else {
            System.out.println("⚠️ 使用默认能耗参数");
        }
        
        // 5. 打印能耗参数摘要
        input.printEnergyParamsSummary();
    }
}
```

### 集成到 MOEvaluator

```java
// 1. 读取算例（含能耗参数）
EnergyAwareInput input = new EnergyAwareInput(new File("instance.txt"));
Problem problem = input.getProblemDesFromFile();

// 2. 创建能耗目标函数
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 3. 如果算例包含能耗参数，应用到能耗计算器
if (input.hasEnergyParams()) {
    Map<Integer, PowerParameters> energyParams = input.getAllEnergyParams();
    
    for (Map.Entry<Integer, PowerParameters> entry : energyParams.entrySet()) {
        energyObj.setMachineParameters(entry.getKey(), entry.getValue());
    }
    
    System.out.println("✅ 已应用算例中的能耗参数");
}

// 4. 使用能耗目标进行优化
List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
objectives.add(new MOEvaluator.MaximumCompletionTime());
objectives.add(energyObj);

// ... 运行NSGA-II ...
```

---

## 🔧 高级用法

### 1. 自定义能耗参数范围

修改 `EnergyAwareInstanceGenerator` 中的生成方法：

```java
private String generatePrinterEnergyParams(int count) {
    StringBuilder line = new StringBuilder(String.valueOf(count));
    
    for (int i = 0; i < count; i++) {
        // 自定义参数范围
        double P_run = 6.0 + random.nextDouble() * 2.0;    // 6-8 kW
        double P_idle = 0.4 + random.nextDouble() * 0.2;   // 0.4-0.6 kW
        double E_switch = 1.5 + random.nextDouble() * 1.0; // 1.5-2.5 kWh
        double T_warmup = 5.0 + random.nextDouble() * 3.0; // 5-8
        int allowSwitch = 1;
        
        line.append(String.format(" %d %.2f %.2f %.2f %.2f %d",
            i + 1, P_run, P_idle, E_switch, T_warmup, allowSwitch));
    }
    
    return line.toString();
}
```

### 2. 生成对比实验算例

```java
// 生成3组算例，测试不同能耗参数的影响

// 场景A: 低开关机代价（适合关机）
EnergyAwareInstanceGenerator genA = 
    new EnergyAwareInstanceGenerator(42, true);
// 修改 E_switch 范围为 0.5-1.0
genA.generateMultipleInstances(20, 3, 2, 5, 10, "instance/low_switch_cost");

// 场景B: 高开关机代价（不适合关机）
EnergyAwareInstanceGenerator genB = 
    new EnergyAwareInstanceGenerator(43, true);
// 修改 E_switch 范围为 5.0-10.0
genB.generateMultipleInstances(20, 3, 2, 5, 10, "instance/high_switch_cost");

// 场景C: 高待机功率（关机更有利）
EnergyAwareInstanceGenerator genC = 
    new EnergyAwareInstanceGenerator(44, true);
// 修改 P_idle 范围为 1.5-3.0
genC.generateMultipleInstances(20, 3, 2, 5, 10, "instance/high_idle_power");
```

### 3. 验证能耗参数合理性

```java
// 读取生成的算例并验证
EnergyAwareInput input = new EnergyAwareInput(new File("instance.txt"));
Problem problem = input.getProblemDesFromFile();

if (input.hasEnergyParams()) {
    System.out.println("\n========== 能耗参数验证 ==========");
    
    for (int i = 0; i < problem.getMachineCount(); i++) {
        PowerParameters p = input.getMachineEnergyParams(i);
        
        // 计算盈亏平衡时间
        double T_be = p.getBreakEvenTime();
        
        // 计算关机阈值
        double threshold = p.T_warmup + T_be;
        
        System.out.println(String.format(
            "机器 %d: T_be=%.2f, 关机阈值=%.2f, allowSwitch=%b",
            i + 1, T_be, threshold, p.allowSwitch));
        
        // 验证合理性
        if (T_be < 0 || T_be > 100) {
            System.err.println("  ⚠️ 盈亏平衡时间异常！");
        }
        
        if (p.allowSwitch && threshold < 1.0) {
            System.out.println("  ✓ 适合频繁关机");
        } else if (p.allowSwitch && threshold > 20.0) {
            System.out.println("  ⚠️ 关机阈值较高，关机机会较少");
        }
    }
}
```

---

## 📈 实验建议

### 1. 基准实验

```java
// 生成基准算例集（标准能耗参数）
EnergyAwareInstanceGenerator gen = 
    new EnergyAwareInstanceGenerator(42, true);

gen.generateMultipleInstances(20, 3, 2, 5, 30, "instance/benchmark");
```

### 2. 参数敏感性分析

分别测试以下参数的影响：
- **E_switch**: 开关机能耗 (低/中/高)
- **P_idle**: 待机功率 (低/中/高)
- **T_warmup**: 预热时间 (短/中/长)
- **allowSwitch**: 关机策略 (允许/不允许)

### 3. 机器类型影响分析

```java
// 场景1: 全部允许关机
// 修改批处理机参数 allowSwitch = 1

// 场景2: 打印机不允许关机
// 修改打印机参数 allowSwitch = 0

// 场景3: 全部不允许关机（baseline）
// 所有机器 allowSwitch = 0
```

---

## 🎓 论文写作建议

### 算例描述部分

```
本文生成了多组算例用于实验验证，规模从小到大分为4类：

1. 小规模: J10P2B1D3 (10工件, 2打印机, 1批处理机, 3离散机)
2. 中等规模: J20P3B2D5 (20工件, 3打印机, 2批处理机, 5离散机)
3. 大规模: J50P4B3D10 (50工件, 4打印机, 3批处理机, 10离散机)
4. 超大规模: J100P5B4D15 (100工件, 5打印机, 4批处理机, 15离散机)

每种规模生成5-10个算例实例，每个实例采用不同的随机种子。
机器能耗参数基于实际3D打印车间的典型值设定：

- 打印机: P_run=4-8kW, P_idle=0.3-0.8kW, E_switch=1-3kWh
- 批处理机: P_run=5-15kW, P_idle=1-3kW, E_switch=10-30kWh
- 离散加工机: P_run=2-6kW, P_idle=0.2-0.5kW, E_switch=0.5-2kWh
```

### 实验设计部分

```
为验证智能开关机策略的有效性，本文设计了以下对比实验：

实验组1（With Switching）: 启用开关机策略，允许机器根据空闲时长
                           自动决定待机或关机。

实验组2（Idle Only）: 禁用开关机策略，机器在空闲期间始终保持待机
                       状态（baseline）。

两组实验使用相同的算例集和相同的NSGA-II参数，仅在能耗计算时采用
不同的策略。通过对比Pareto前沿的能耗范围和超体积（Hypervolume），
评估开关机策略的节能效果。
```

---

## 📞 技术支持

如有问题，请查看：
1. `EnergyAwareInstanceGenerator.java` 源代码
2. `EnergyAwareInput.java` 源代码
3. `ENERGY_USER_GUIDE.md` 能耗计算详细文档

**快速测试**：
```bash
# 在IDE中运行
EnergyAwareInstanceGenerator.main()
EnergyAwareInput.main()
```

---

## ✅ 总结

✅ **已实现功能**：
- 能耗参数自动生成
- 算例格式向后兼容
- 批量生成和场景生成
- 能耗参数读取和验证

✅ **使用流程**：
1. 运行 `EnergyAwareInstanceGenerator` 生成算例
2. 使用 `EnergyAwareInput` 读取算例
3. 集成到 `MOEvaluator` 进行多目标优化

✅ **实验建议**：
- 基准实验（标准参数）
- 参数敏感性分析
- 对比实验（有/无开关机策略）

**祝你的实验顺利！** 🎉


