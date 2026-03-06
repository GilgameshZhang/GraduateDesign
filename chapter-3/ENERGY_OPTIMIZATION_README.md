# 能耗优化模块 - 开/关机策略实现

## 📋 概述

本模块在NSGA-II多目标优化框架的基础上，实现了**"绿色车间"能耗目标**，支持**开/关机策略**以最小化总能耗。

## 🎯 核心功能

### 1. 多目标优化
- **objectives[0]**: Cmax（最大完工时间，最小化）
- **objectives[1]**: E_total（总能耗，最小化）
- 可扩展：拖期、等待时间等

### 2. 开/关机策略能耗模型

对每台设备 k（打印机、批处理机、离散处理机）定义参数：

| 参数 | 符号 | 含义 | 默认值（打印机） |
|------|------|------|------------------|
| 运行功率 | `P_run_k` | 加工/处理期间的功率 (kW) | 5.0 |
| 待机功率 | `P_idle_k` | 开机但空闲时的功率 (kW) | 0.5 |
| 开关机能耗 | `E_switch_k` | 一次"关机+再开机"的总能耗 (kWh) | 2.0 |
| 预热时间 | `T_warmup_k` | 开机/预热所需时间 | 5.0 |
| 允许关机 | `allowSwitch_k` | 是否允许关机策略 | true |

### 3. 能耗计算公式

**运行能耗**：
```
E_run = sum(P_run_k * (endTime - startTime))
```

**空闲间隔能耗**（对相邻任务之间的gap）：
```
gap = startTime_next - endTime_current

if gap <= 0:
    E_gap = 0

elif !allowSwitch_k:
    E_gap = P_idle_k * gap  # 强制待机

elif gap < T_warmup_k:
    E_gap = P_idle_k * gap  # 预热时间不足，待机

else:
    T_breakEven = E_switch_k / P_idle_k
    
    if gap >= T_warmup_k + T_breakEven:
        E_gap = E_switch_k  # 关机
    else:
        E_gap = P_idle_k * gap  # 待机
```

**总能耗**：
```
E_total = sum(E_run_k + sum(E_gap_k))
```

### 4. 关机判定条件

设备k在空闲间隔gap时应该关机，当且仅当：

```
gap >= T_warmup_k + T_breakEven_k
```

其中盈亏平衡时间：
```
T_breakEven = E_switch / P_idle
```

## 📂 新增文件

### 核心类

1. **`PowerParameters.java`** - 能耗参数类
   - 定义设备的能耗参数
   - 提供开关机判定逻辑
   - 预设默认参数（打印机/批处理机/离散机）

2. **`EnergyCalculator.java`** - 能耗计算器
   - 计算运行能耗和空闲能耗
   - 实现开/关机策略逻辑
   - 支持调试输出和统计分析

3. **`MOEvaluator.java`** - 多目标评价器（已更新）
   - 新增 `TotalEnergyConsumption` 目标函数（v2.0）
   - 集成能耗计算器
   - 支持启用/禁用开关机策略

### 测试和示例

4. **`EnergyCalculatorTest.java`** - 单元测试
   - 验证盈亏平衡时间计算
   - 验证开关机判定逻辑
   - 验证能耗计算正确性

5. **`EnergyOptimizationExample.java`** - 使用示例
   - 演示如何配置能耗参数
   - 对比开关机策略 vs 纯待机策略
   - 展示NSGA-II集成方法

## 🚀 使用方法

### 方法1：使用预定义目标函数

```java
// 1. 加载问题实例
Problem problem = ...;

// 2. 定义多目标函数
List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();

// 目标1: Makespan
objectives.add(new MOEvaluator.MaximumCompletionTime());

// 目标2: 总能耗（启用开关机策略）
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));

// 3. 创建评价器
MOEvaluator evaluator = new MOEvaluator();

// 4. 评价个体
evaluator.evaluate(individual, problem, operationMatrix, objectives);
```

### 方法2：自定义能耗参数

```java
// 创建能耗目标函数
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 自定义打印机参数
PowerParameters customParams = new PowerParameters(
    6.0,    // P_run: 提高运行功率
    0.4,    // P_idle: 降低待机功率
    1.5,    // E_switch: 降低开关机代价
    3.0,    // T_warmup: 缩短预热时间
    true    // allowSwitch: 允许关机
);

// 应用到特定机器（例如第0台打印机）
energyObj.setMachineParameters(0, customParams);
```

### 方法3：对比实验（开关机 vs 纯待机）

```java
// 策略1: 启用开关机
MOEvaluator.TotalEnergyConsumption energyWithSwitching = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 策略2: 纯待机（消融实验）
MOEvaluator.TotalEnergyConsumption energyIdleOnly = 
    new MOEvaluator.TotalEnergyConsumption(problem, false, false);

// 分别计算并对比能耗
double E1 = energyWithSwitching.calculate(chromosome, problem, operationMatrix);
double E2 = energyIdleOnly.calculate(chromosome, problem, operationMatrix);

double savings = E2 - E1;
System.out.println("节能: " + savings + " kWh");
```

## ✅ 测试验证

运行单元测试：

```bash
cd chapter-3
mvn test -Dtest=EnergyCalculatorTest
```

**测试覆盖**：
- ✓ 盈亏平衡时间计算
- ✓ 开关机判定逻辑（短/中/长间隔）
- ✓ 不允许关机的设备处理
- ✓ 空闲间隔能耗计算
- ✓ 能耗节省对比
- ✓ 边界情况（P_idle=0等）

## 📊 输出示例

### 个体信息
```
Individual[
  rank=1, 
  crowding=1.2345, 
  objectives=[150.50, 320.75], 
  packingQ=0.8234, 
  batches=15
]
```

- `objectives[0] = 150.50`：Makespan
- `objectives[1] = 320.75`：总能耗（kWh）
- `packingQ = 0.8234`：装箱质量（tie-breaker）
- `batches = 15`：批次数

### 能耗计算详情（调试输出）
```
========== 能耗计算详情 ==========
启用开关机策略: true

机器 1: 45.20 kWh (12个工序)
  运行能耗: 42.50 kWh
  空闲能耗: 2.70 kWh (关机2次, 待机5次)
  空闲 1: gap=3.50, energy=1.7500 (待机)
  空闲 2: gap=15.00, energy=2.0000 (关机)
  ...

机器 2: 38.60 kWh (10个工序)
  ...

总能耗: 320.75 kWh
==================================
```

## ⚙️ 配置说明

### 默认参数（可根据实际调整）

| 设备类型 | P_run | P_idle | E_switch | T_warmup | allowSwitch | T_breakEven |
|----------|-------|--------|----------|----------|-------------|-------------|
| 打印机 | 5.0 | 0.5 | 2.0 | 5.0 | true | 4.0 |
| 批处理机 | 3.0 | 0.8 | 5.0 | 20.0 | **false** | 6.25 |
| 离散机 | 2.0 | 0.3 | 1.0 | 3.0 | true | 3.33 |

**注意**：
- 批处理机默认**不允许关机**（需保持温度）
- 参数可通过 `setMachineParameters()` 修改

## 🔧 集成到NSGA-II

### 修改点（最小侵入）

1. **MOIndividual** (已完成)
   - 增加 `double[] objectives` 字段
   - 增加 `rank`, `crowdingDistance`, `packingQ`, `batchCount`

2. **MOEvaluator** (已完成)
   - 更新 `TotalEnergyConsumption` 目标函数
   - 集成 `EnergyCalculator`

3. **无需修改**：
   - ✓ 编码/解码逻辑（Chromosome）
   - ✓ 排程逻辑（CaculateFitness）
   - ✓ Skyline装箱（SkyLinePacking）

## 🎓 使用场景

### 场景1：纯能耗优化
```java
// 只关注能耗，忽略makespan
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem));
```

### 场景2：Makespan-能耗双目标优化
```java
// 同时优化makespan和能耗（Pareto前沿）
objectives.add(new MOEvaluator.MaximumCompletionTime());
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem));
```

### 场景3：三目标优化
```java
// Makespan + 能耗 + 拖期
objectives.add(new MOEvaluator.MaximumCompletionTime());
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem));
objectives.add(new MOEvaluator.TotalTardiness());
```

## 📝 注意事项

### 1. 参数校准
实际使用时，请根据设备实测数据校准：
- `P_run`, `P_idle`：查阅设备说明书或实测
- `E_switch`：测量一次完整开关机的能耗
- `T_warmup`：测量开机到ready状态的时间

### 2. 时间单位
确保所有时间单位一致：
- 排程时间：通常为分钟或小时
- 能耗单位：kWh（千瓦时）
- 功率单位：kW（千瓦）

### 3. 边界情况
- 若`P_idle=0`，`T_breakEven=+∞`，永远不关机
- 若`E_switch=0`，`T_breakEven=0`，总是关机（不现实）
- 若`allowSwitch=false`，强制待机（如批处理机）

## 🔬 实验建议

### 消融实验
1. 开关机策略 vs 纯待机策略
2. 不同`E_switch`值对能耗的影响
3. 不同`allowSwitch`配置的影响

### 参数敏感性分析
- 改变`P_idle`，观察关机频率变化
- 改变`E_switch`，观察节能效果
- 改变`T_warmup`，观察关机条件

## 📚 参考文献

1. 绿色车间调度相关论文（请补充）
2. 开关机策略能耗模型（请补充）
3. NSGA-II算法原理（Deb et al., 2002）

## 🐛 故障排除

### 问题1：总能耗为0
- **原因**：`operationMatrix`未正确填充
- **解决**：确保调用`CaculateFitness.evaluate()`

### 问题2：关机次数为0（预期应关机）
- **原因**：空闲间隔太短或参数设置不当
- **解决**：检查`T_warmup + T_breakEven`是否合理

### 问题3：编译错误
- **原因**：缺少`Chromosome.java`, `CaculateFitness.java`
- **解决**：从`chapter-2`复制这些文件

## 📬 联系方式

如有问题或建议，请联系项目维护者。

