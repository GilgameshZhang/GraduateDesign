# 能耗计算模块使用说明

## 📚 概述

本模块实现了"绿色车间"能耗优化的完整功能，包括：
- ✅ 运行能耗计算：`E_run = Σ(P_run × duration)`
- ✅ 空闲能耗计算（智能开/关机策略）
- ✅ 多机器类型支持（打印机、批处理机、离散加工机）
- ✅ 可配置能耗参数
- ✅ 能耗统计和分析功能

---

## 🚀 快速开始

### 1. 基本用法（单个调度方案的能耗计算）

```java
// 1) 创建能耗计算器（启用开关机策略）
EnergyCalculator calculator = new EnergyCalculator(problem, true, false);

// 2) 计算总能耗
double totalEnergy = calculator.calculateTotalEnergy(operationMatrix, problem);

System.out.println("总能耗: " + totalEnergy + " kWh");
```

### 2. 多目标优化用法

```java
// 1) 定义多目标函数
List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
objectives.add(new MOEvaluator.MaximumCompletionTime());  // Cmax
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));  // 能耗

// 2) 创建评价器
MOEvaluator evaluator = new MOEvaluator();

// 3) 评价个体
MOIndividual individual = new MOIndividual(chromosome, 2);
Operation[][] operationMatrix = createOperationMatrix(problem);
evaluator.evaluate(individual, problem, operationMatrix, objectives);

// 4) 获取目标值
double cmax = individual.objectives[0];
double energy = individual.objectives[1];
```

### 3. 自定义能耗参数

```java
// 创建能耗目标函数
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 为特定机器设置自定义参数
PowerParameters customParams = new PowerParameters(
    6.0,    // P_run: 运行功率 (kW)
    0.4,    // P_idle: 待机功率 (kW)
    1.5,    // E_switch: 开关机能耗 (kWh)
    3.0,    // T_warmup: 预热时间
    true    // allowSwitch: 允许关机
);

// 应用到机器0（打印机1）
energyObj.setMachineParameters(0, customParams);
```

---

## 📐 核心类说明

### 1. `PowerParameters` - 能耗参数类

**字段说明**：
```java
public double P_run;        // 运行功率 (kW) - 设备工作时的功率
public double P_idle;       // 待机功率 (kW) - 设备空闲时的功率
public double E_switch;     // 开关机能耗 (kWh) - 一次关机+开机的总能耗代价
public double T_warmup;     // 预热时间 - 开机后达到工作状态的时间
public boolean allowSwitch; // 是否允许关机 - 某些设备不适合频繁开关机
```

**关键方法**：
```java
// 计算盈亏平衡时间
double T_be = params.getBreakEvenTime();  // T_be = E_switch / P_idle

// 判断是否应该关机
boolean shouldShutdown = params.shouldShutdown(gapDuration);
// 条件: allowSwitch=true && gapDuration >= T_warmup + T_be

// 计算空闲间隔能耗
double gapEnergy = params.calculateGapEnergy(gapDuration);
// 短间隔: P_idle × gap
// 长间隔: E_switch
```

**默认参数**（可通过静态方法创建）：
```java
// 打印机（允许关机）
PowerParameters printParams = PowerParameters.createPrintMachineDefault();
// P_run=5.0, P_idle=0.5, E_switch=2.0, T_warmup=5.0, allowSwitch=true

// 批处理机（不允许关机，需保持温度）
PowerParameters batchParams = PowerParameters.createBatchMachineDefault();
// P_run=3.0, P_idle=0.8, E_switch=5.0, T_warmup=20.0, allowSwitch=false

// 离散加工机（允许关机）
PowerParameters discreteParams = PowerParameters.createDiscreteMachineDefault();
// P_run=2.0, P_idle=0.3, E_switch=1.0, T_warmup=3.0, allowSwitch=true
```

---

### 2. `EnergyCalculator` - 能耗计算器

**构造函数**：
```java
// 1) 默认构造（启用开关机策略，无调试输出）
EnergyCalculator calculator = new EnergyCalculator(problem);

// 2) 完整构造（可配置策略和调试输出）
EnergyCalculator calculator = new EnergyCalculator(
    problem,              // 问题实例
    true,                 // enableSwitchingStrategy: 启用开关机策略
    false                 // debugOutput: 输出调试信息
);
```

**核心方法**：
```java
// 1. 计算总能耗
double totalEnergy = calculator.calculateTotalEnergy(operationMatrix, problem);

// 2. 获取详细统计信息
EnergyCalculator.EnergyStatistics stats = 
    calculator.getStatistics(operationMatrix, problem);

System.out.println("运行能耗: " + stats.runEnergy + " kWh");
System.out.println("空闲能耗: " + stats.idleEnergy + " kWh");
System.out.println("关机次数: " + stats.shutdownCount);
System.out.println("待机次数: " + stats.idleCount);

// 3. 设置机器参数
calculator.setMachineParameters(machineId, powerParams);

// 4. 启用/禁用开关机策略
calculator.setEnableSwitchingStrategy(false);  // 强制待机（对比实验）
```

**能耗计算逻辑**：

1. **按机器分组**：将所有工序按机器分组并排序
2. **计算运行能耗**：
   ```
   E_run = Σ(P_run × (endTime - startTime))
   ```
3. **计算空闲能耗**：遍历相邻工序的间隔
   - 短间隔（gap < T_warmup + T_be）：待机
     ```
     E_idle = P_idle × gap
     ```
   - 长间隔（gap >= T_warmup + T_be）：关机
     ```
     E_idle = E_switch
     ```
4. **累加总能耗**：`E_total = Σ(E_run + E_idle)`

---

### 3. `MOEvaluator.TotalEnergyConsumption` - 能耗目标函数

**构造函数**：
```java
// 1) 默认构造（启用开关机策略）
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem);

// 2) 完整构造
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(
        problem,              // 问题实例
        true,                 // enableSwitchingStrategy: 启用开关机策略
        false                 // debugOutput: 输出调试信息
    );
```

**使用方法**：
```java
// 1. 创建能耗目标
MOEvaluator.TotalEnergyConsumption energyObj = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 2. 添加到目标列表
List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
objectives.add(new MOEvaluator.MaximumCompletionTime());  // Cmax
objectives.add(energyObj);                                 // Energy

// 3. 评价个体
MOEvaluator evaluator = new MOEvaluator();
evaluator.evaluate(individual, problem, operationMatrix, objectives);

// 4. 获取能耗统计（可选）
EnergyCalculator.EnergyStatistics stats = 
    energyObj.getStatistics(chromosome, problem, operationMatrix);
```

---

## 🎯 完整使用示例

### 示例1：基本能耗计算

```java
import ProblemFrame.*;
import ProgramEntity.*;

public class BasicEnergyExample {
    public static void main(String[] args) {
        // 1. 加载问题
        Problem problem = new Input(new File("instance.txt")).getProblemDesFromFile();
        
        // 2. 创建解决方案（假设已有调度结果）
        Operation[][] operationMatrix = getScheduleResult();
        
        // 3. 计算能耗
        EnergyCalculator calculator = new EnergyCalculator(problem, true, true);
        double energy = calculator.calculateTotalEnergy(operationMatrix, problem);
        
        System.out.println("总能耗: " + energy + " kWh");
        
        // 4. 查看详细统计
        EnergyCalculator.EnergyStatistics stats = 
            calculator.getStatistics(operationMatrix, problem);
        System.out.println("运行能耗: " + stats.runEnergy + " kWh");
        System.out.println("空闲能耗: " + stats.idleEnergy + " kWh");
        System.out.println("关机次数: " + stats.shutdownCount);
    }
}
```

### 示例2：能耗策略对比

```java
public class EnergyStrategyComparison {
    public static void main(String[] args) {
        Problem problem = loadProblem();
        Operation[][] schedule = getSchedule();
        
        // 策略1: 智能开关机
        EnergyCalculator calcWithSwitching = 
            new EnergyCalculator(problem, true, false);
        double energyWithSwitching = 
            calcWithSwitching.calculateTotalEnergy(schedule, problem);
        
        // 策略2: 纯待机
        EnergyCalculator calcIdleOnly = 
            new EnergyCalculator(problem, false, false);
        double energyIdleOnly = 
            calcIdleOnly.calculateTotalEnergy(schedule, problem);
        
        // 对比
        double savings = energyIdleOnly - energyWithSwitching;
        double savingsPercent = (savings / energyIdleOnly) * 100.0;
        
        System.out.println("开关机策略能耗: " + energyWithSwitching + " kWh");
        System.out.println("纯待机能耗: " + energyIdleOnly + " kWh");
        System.out.println("节能效果: " + savings + " kWh (" + savingsPercent + "%)");
    }
}
```

### 示例3：NSGA-II多目标优化

```java
public class MultiObjectiveOptimization {
    public static void main(String[] args) {
        // 1. 加载问题
        Problem problem = loadProblem();
        
        // 2. 定义多目标
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        objectives.add(new MOEvaluator.MaximumCompletionTime());
        objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));
        
        // 3. 初始化种群
        List<MOIndividual> population = initializePopulation(problem, 100);
        
        // 4. 评价种群
        MOEvaluator evaluator = new MOEvaluator();
        for (MOIndividual ind : population) {
            Operation[][] opMatrix = createOperationMatrix(problem);
            evaluator.evaluate(ind, problem, opMatrix, objectives);
        }
        
        // 5. NSGA-II主循环
        for (int gen = 0; gen < 200; gen++) {
            // 选择、交叉、变异
            List<MOIndividual> offspring = geneticOperations(population);
            
            // 评价子代
            for (MOIndividual ind : offspring) {
                Operation[][] opMatrix = createOperationMatrix(problem);
                evaluator.evaluate(ind, problem, opMatrix, objectives);
            }
            
            // 环境选择
            population = NSGAIIOperations.environmentalSelection(
                population, offspring, 100);
            
            // 输出进度
            if (gen % 20 == 0) {
                List<List<MOIndividual>> fronts = 
                    NSGAIIOperations.fastNonDominatedSort(population);
                System.out.println("第" + gen + "代: Pareto前沿 = " + 
                    fronts.get(0).size() + " 个解");
            }
        }
        
        // 6. 输出最终Pareto前沿
        List<List<MOIndividual>> finalFronts = 
            NSGAIIOperations.fastNonDominatedSort(population);
        System.out.println("\n最终Pareto前沿:");
        for (MOIndividual ind : finalFronts.get(0)) {
            System.out.println(String.format("  Cmax=%.2f, Energy=%.2f kWh",
                ind.objectives[0], ind.objectives[1]));
        }
    }
}
```

### 示例4：自定义机器能耗参数

```java
public class CustomEnergyParameters {
    public static void main(String[] args) {
        Problem problem = loadProblem();
        
        // 创建能耗目标函数
        MOEvaluator.TotalEnergyConsumption energyObj = 
            new MOEvaluator.TotalEnergyConsumption(problem, true, false);
        
        // 自定义打印机参数（高功率打印机）
        PowerParameters highPowerPrinter = new PowerParameters(
            8.0,    // 高运行功率
            0.6,    // 较高待机功率
            3.0,    // 较高开关机代价
            8.0,    // 较长预热时间
            true    // 允许关机
        );
        
        // 应用到机器0和机器1（两台打印机）
        energyObj.setMachineParameters(0, highPowerPrinter);
        energyObj.setMachineParameters(1, highPowerPrinter);
        
        // 自定义离散加工机参数（快速启动机器）
        PowerParameters fastStartMachine = new PowerParameters(
            2.5,    // 中等运行功率
            0.2,    // 低待机功率
            0.5,    // 低开关机代价
            1.0,    // 快速预热
            true    // 允许关机
        );
        
        // 应用到离散加工机
        for (int i = 3; i < problem.getMachineCount(); i++) {
            energyObj.setMachineParameters(i, fastStartMachine);
        }
        
        // 使用自定义参数进行优化
        List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
        objectives.add(new MOEvaluator.MaximumCompletionTime());
        objectives.add(energyObj);
        
        // ... 运行NSGA-II ...
    }
}
```

---

## 📊 运行完整示例

### 方法1：运行内置测试

在IDE中直接运行：
```
AlgorithmFrame.nsgaii.EnergyNSGAIIExample
```

**预期输出**：
```
╔═══════════════════════════════════════════════════════════╗
║     NSGA-II 多目标优化 - 绿色车间能耗优化示例            ║
╚═══════════════════════════════════════════════════════════╝

【步骤1】加载问题实例
  ✓ 工件数: 10
  ✓ 机器数: 6
  ✓ 打印机: 2
  ✓ 批处理机: 1

【步骤2】配置机器能耗参数
  ✓ 打印机 1: PowerParams[P_run=5.00, P_idle=0.50, ...]
  ✓ 打印机 2: PowerParams[P_run=5.00, P_idle=0.50, ...]
  ...

【步骤8】Pareto最优解集（前10个）
  ──────────────────────────────────────────────────────────
  序号   Cmax            能耗(kWh)       packingQ     批次数
  ──────────────────────────────────────────────────────────
  1      245.50          156.23          0.7845       12
  2      248.30          152.17          0.8012       11
  ...

【步骤9】能耗策略对比分析
  ┌─────────────────────────────────────────────────────────┐
  │              能耗策略对比（同一调度方案）                │
  ├─────────────────────────────────────────────────────────┤
  │ 策略1: 智能开关机                                       │
  │   总能耗: 152.17                                   kWh │
  │   关机次数: 15                                      次 │
  ├─────────────────────────────────────────────────────────┤
  │ 策略2: 纯待机（baseline）                              │
  │   总能耗: 168.45                                   kWh │
  ├─────────────────────────────────────────────────────────┤
  │ 节能效果                                                │
  │   节省能耗: 16.28                                  kWh │
  │   节能比例: 9.7                                     %  │
  │   ✅ 开关机策略有效！                                   │
  └─────────────────────────────────────────────────────────┘
```

### 方法2：使用真实算例

修改 `EnergyNSGAIIExample.java` 第31行：
```java
// 注释掉测试问题
// Problem problem = createTestProblem();

// 使用真实算例文件
String instancePath = "../chapter-2/src/main/resources/instance/J20/J20P3B2D5_01.txt";
Problem problem = new Input(new File(instancePath)).getProblemDesFromFile();
```

---

## ⚙️ 参数调优建议

### 1. 打印机参数

**3D打印机（FDM）**：
```java
new PowerParameters(
    4.0-6.0,   // P_run: 打印时功率 (kW)
    0.3-0.7,   // P_idle: 待机功率 (kW)
    1.5-3.0,   // E_switch: 开关机能耗 (kWh)
    3.0-10.0,  // T_warmup: 预热时间（分钟）
    true       // 适合关机
)
```

**工业级打印机（SLS/SLM）**：
```java
new PowerParameters(
    10.0-20.0, // P_run: 高功率激光
    2.0-5.0,   // P_idle: 高待机功率（保温）
    10.0-20.0, // E_switch: 高开关机代价
    30.0-60.0, // T_warmup: 长预热时间
    false      // 不适合频繁关机
)
```

### 2. 批处理机参数

**热处理炉**：
```java
new PowerParameters(
    3.0-8.0,   // P_run: 加热时功率
    1.0-3.0,   // P_idle: 保温功率
    10.0-30.0, // E_switch: 高开关机代价
    20.0-120.0,// T_warmup: 很长预热时间
    false      // 通常不关机
)
```

### 3. 离散加工机参数

**CNC铣床**：
```java
new PowerParameters(
    2.0-5.0,   // P_run: 加工功率
    0.2-0.5,   // P_idle: 低待机功率
    0.5-2.0,   // E_switch: 中等开关机代价
    1.0-5.0,   // T_warmup: 较短预热时间
    true       // 适合关机
)
```

---

## 🔍 常见问题（FAQ）

### Q1: 能耗为什么是0？
**A**: 检查 `operationMatrix` 是否正确填充了 `startTime` 和 `endTime`。如果工序未分配，`machineNo` 会是 `-1`，该工序会被跳过。

### Q2: 为什么没有触发关机？
**A**: 关机条件是 `gap >= T_warmup + T_breakEven`。检查：
- 空闲间隔是否足够长
- `allowSwitch` 是否为 `true`
- `T_breakEven = E_switch / P_idle` 是否合理

### Q3: 如何禁用开关机策略进行对比？
**A**: 
```java
// 方法1: 构造时禁用
EnergyCalculator calc = new EnergyCalculator(problem, false, false);

// 方法2: 运行时禁用
calc.setEnableSwitchingStrategy(false);
```

### Q4: 如何输出调试信息？
**A**:
```java
// 启用调试输出（第三个参数）
EnergyCalculator calc = new EnergyCalculator(problem, true, true);
```

### Q5: `PackingQ` 是什么？
**A**: `packingQ` 是装箱质量指标（最后一批占用率），用于在拥挤距离相近时作为tie-breaker，**不是优化目标**。

---

## 📈 性能优化建议

1. **预计算参数**：在算法主循环前计算好所有机器的 `T_breakEven`
2. **避免重复排序**：`EnergyCalculator` 已自动排序，无需外部排序
3. **批量评价**：如果评价多个个体，复用 `EnergyCalculator` 实例
4. **调试输出**：生产环境关闭 `debugOutput`

---

## 📝 引用和致谢

如果使用本模块进行研究，建议引用：

> Zhang H.L. (2026). "Multi-objective Optimization for Green 3D Printing Workshops with Machine On/Off Strategy". 
> Master's Thesis, [Your University].

**关键算法**：
- NSGA-II: Deb et al. (2002)
- 能耗模型: 参考绿色车间调度文献（Che et al., 2022）
- Skyline Packing: Liu & Teng (1999)

---

## 📞 技术支持

如有问题，请：
1. 查看本文档的 FAQ 部分
2. 运行 `QuickVerification.java` 进行自检
3. 查看 `EnergyNSGAIIExample.java` 的完整示例

**祝你的多目标优化研究顺利！** 🎉


