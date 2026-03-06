# 能耗计算模块 - 架构总结

## 📐 模块架构

```
chapter-3/
├── ProblemFrame/
│   ├── PowerParameters.java              # ✅ 能耗参数配置类
│   ├── EnergyCalculator.java             # ✅ 能耗计算引擎
│   ├── MOEvaluator.java                  # ✅ 多目标评价器（含能耗目标）
│   ├── MOIndividual.java                 # ✅ 多目标个体（含objectives[]）
│   ├── CaculateFitness.java              # ✅ 单目标评价（复用解码）
│   ├── Chromosome.java                   # ✅ 染色体（基础编码）
│   └── SkyLinePacking.java               # ✅ 装箱算法
│
├── AlgorithmFrame/nsgaii/
│   ├── NSGAIIOperations.java             # ✅ NSGA-II核心操作
│   ├── NSGAII.java                       # ✅ NSGA-II主算法
│   ├── EnergyNSGAIIExample.java          # ✅ 完整示例（NEW!）
│   └── QuickVerification.java            # ✅ 快速验证程序
│
└── docs/
    ├── ENERGY_USER_GUIDE.md              # ✅ 用户使用指南（NEW!）
    ├── README.md                         # ✅ 模块总览
    └── QUICK_START.md                    # ✅ 快速开始
```

---

## 🔄 数据流程

### 1. **单目标评价流程（复用现有代码）**

```
Chromosome (OS+MS)
    ↓
CaculateFitness.evaluate()
    ↓
├─→ SkylinePacking (打印阶段装箱)
├─→ Batch Scheduling (批处理调度)
└─→ Discrete Scheduling (离散工序调度)
    ↓
operationMatrix[][] (所有工序的时间信息)
    ↓
计算 Cmax = max(endTime)
```

### 2. **多目标评价流程（新增能耗计算）**

```
MOIndividual (OS+MS + objectives[])
    ↓
MOEvaluator.evaluate()
    ↓
├─→ 复用 CaculateFitness.evaluate() 得到 operationMatrix
│
├─→ 目标1: MaximumCompletionTime
│       └─→ objectives[0] = max(endTime)
│
├─→ 目标2: TotalEnergyConsumption
│       ↓
│   EnergyCalculator.calculateTotalEnergy()
│       ↓
│   按机器分组 + 排序工序
│       ↓
│   ├─→ 运行能耗: E_run = Σ(P_run × duration)
│   │
│   └─→ 空闲能耗: 遍历工序间隔
│           ↓
│       ┌───────────────────┐
│       │ gap < T_w + T_be? │
│       └───────────────────┘
│           │            │
│         YES           NO
│           ↓            ↓
│       待机         关机
│       P_idle×gap   E_switch
│       ↓
│   objectives[1] = E_total
│
└─→ 目标3+: TotalTardiness, WaitingTime, ... (可选)
    ↓
MOIndividual.objectives[] = [Cmax, Energy, ...]
```

### 3. **NSGA-II主循环流程**

```
初始化种群 (Population P0)
    ↓
for gen = 1 to MAX_GEN:
    ├─→ 评价种群
    │     └─→ MOEvaluator.evaluate() → objectives[]
    │
    ├─→ 遗传操作（选择、交叉、变异）
    │     └─→ 生成子代 Q
    │
    ├─→ 评价子代
    │     └─→ MOEvaluator.evaluate() → objectives[]
    │
    ├─→ 环境选择
    │     ├─→ R = P ∪ Q
    │     ├─→ fastNonDominatedSort(R) → fronts[]
    │     ├─→ assignCrowdingDistance(fronts)
    │     └─→ 选择前N个个体（rank优先 → crowding优先 → packingQ tie-breaker）
    │
    └─→ P_next = selected N individuals
    ↓
输出 Pareto前沿
```

---

## 🎯 核心算法伪代码

### 算法1: 能耗计算（含开关机策略）

```python
function calculateTotalEnergy(operationMatrix, problem, params[]):
    E_total = 0
    
    # 遍历每台机器
    for machineId in range(machineCount):
        ops = filter_and_sort(operationMatrix, machineId)
        
        if ops.isEmpty():
            continue
        
        # 运行能耗
        E_run = 0
        for op in ops:
            E_run += params[machineId].P_run * (op.endTime - op.startTime)
        
        # 空闲能耗
        E_idle = 0
        for i in range(len(ops) - 1):
            gap = ops[i+1].startTime - ops[i].endTime
            
            if gap <= 0:
                continue
            
            # 关机判定
            T_be = params[machineId].E_switch / params[machineId].P_idle
            T_threshold = params[machineId].T_warmup + T_be
            
            if params[machineId].allowSwitch and gap >= T_threshold:
                # 关机策略
                E_idle += params[machineId].E_switch
            else:
                # 待机策略
                E_idle += params[machineId].P_idle * gap
        
        E_total += E_run + E_idle
    
    return E_total
```

### 算法2: NSGA-II环境选择（含tie-breaker）

```python
function environmentalSelection(parent, offspring, N):
    # 1. 合并种群
    R = parent ∪ offspring
    
    # 2. 非支配排序
    fronts = fastNonDominatedSort(R)
    
    # 3. 依次添加前沿
    nextPop = []
    i = 0
    
    while |nextPop| + |fronts[i]| <= N:
        assignCrowdingDistance(fronts[i])
        nextPop += fronts[i]
        i += 1
    
    # 4. 截断最后一个前沿
    if |nextPop| < N:
        remainingSlots = N - |nextPop|
        assignCrowdingDistance(fronts[i])
        
        # 排序（拥挤距离 + tie-breaker）
        fronts[i].sort(key=lambda ind: (
            -ind.crowdingDistance,  # 降序（大的优先）
            -ind.packingQ,          # tie-breaker 1
            ind.batchCount          # tie-breaker 2
        ))
        
        nextPop += fronts[i][:remainingSlots]
    
    return nextPop
```

### 算法3: 拥挤距离计算（标准NSGA-II）

```python
function assignCrowdingDistance(front):
    n = len(front)
    m = num_objectives
    
    # 初始化
    for ind in front:
        ind.crowdingDistance = 0
    
    if n <= 2:
        # 边界情况：所有个体设为无穷大
        for ind in front:
            ind.crowdingDistance = INF
        return
    
    # 对每个目标
    for obj_idx in range(m):
        # 按该目标排序
        front.sort(key=lambda ind: ind.objectives[obj_idx])
        
        # 边界个体设为无穷大
        front[0].crowdingDistance = INF
        front[n-1].crowdingDistance = INF
        
        # 计算归一化因子
        f_min = front[0].objectives[obj_idx]
        f_max = front[n-1].objectives[obj_idx]
        
        if abs(f_max - f_min) < 1e-9:
            continue  # 该目标无差异，跳过
        
        # 累加中间个体的拥挤距离
        for i in range(1, n-1):
            distance = (front[i+1].objectives[obj_idx] - 
                       front[i-1].objectives[obj_idx]) / (f_max - f_min)
            front[i].crowdingDistance += distance
```

---

## 📊 类关系图

```
┌─────────────────────────────────────────────────────────┐
│                    Problem (问题实例)                    │
│  - machines[]                                           │
│  - jobs[]                                               │
│  - proDesMatrix[][]                                     │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 提供问题数据
                        ↓
┌─────────────────────────────────────────────────────────┐
│                PowerParameters (能耗参数)                │
│  + P_run, P_idle, E_switch, T_warmup, allowSwitch      │
│  + getBreakEvenTime()                                   │
│  + shouldShutdown(gap)                                  │
│  + calculateGapEnergy(gap)                              │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 配置参数
                        ↓
┌─────────────────────────────────────────────────────────┐
│           EnergyCalculator (能耗计算引擎)                │
│  - machineParams: Map<machineId, PowerParameters>      │
│  - enableSwitchingStrategy: boolean                     │
│  + calculateTotalEnergy(operationMatrix, problem)       │
│  + getStatistics(...)                                   │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 嵌入到
                        ↓
┌─────────────────────────────────────────────────────────┐
│      MOEvaluator.TotalEnergyConsumption (目标函数)       │
│  - calculator: EnergyCalculator                         │
│  + calculate(chromosome, problem, operationMatrix)      │
│  + setMachineParameters(machineId, params)              │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 作为目标
                        ↓
┌─────────────────────────────────────────────────────────┐
│              MOEvaluator (多目标评价器)                  │
│  + evaluate(individual, problem, opMatrix, objectives)  │
│  - 内置目标: Cmax, Energy, Tardiness, WaitTime         │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 评价
                        ↓
┌─────────────────────────────────────────────────────────┐
│            MOIndividual (多目标个体)                     │
│  - gene_OS[], gene_MS[] (染色体编码)                   │
│  - objectives[] (目标值数组)                           │
│  - rank, crowdingDistance (NSGA-II字段)                │
│  - packingQ, batchCount (tie-breaker)                   │
│  + dominates(other)                                     │
│  + compareTo(other)                                     │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 种群
                        ↓
┌─────────────────────────────────────────────────────────┐
│          NSGAIIOperations (NSGA-II操作)                 │
│  + fastNonDominatedSort(population)                     │
│  + assignCrowdingDistance(front)                        │
│  + environmentalSelection(parent, offspring, N)         │
│  + tournamentSelect(population)                         │
└─────────────────────────────────────────────────────────┘
                        │
                        │ 使用
                        ↓
┌─────────────────────────────────────────────────────────┐
│                 NSGAII (主算法)                         │
│  + solve(problem, objectives, params)                   │
│  - 主循环: 评价→遗传操作→环境选择                        │
└─────────────────────────────────────────────────────────┘
```

---

## ✅ 功能清单

### 核心功能
- [x] 能耗参数配置（`PowerParameters`）
- [x] 运行能耗计算（`E_run = Σ(P_run × duration)`）
- [x] 空闲能耗计算（待机/关机策略）
- [x] 开关机判定逻辑（`gap >= T_warmup + T_breakEven`）
- [x] 多机器类型支持（打印机、批处理机、离散加工机）
- [x] 能耗统计功能（`EnergyStatistics`）

### NSGA-II功能
- [x] 多目标个体（`MOIndividual`）
- [x] 支配关系判断（`dominates()`）
- [x] 快速非支配排序（`fastNonDominatedSort()`）
- [x] 拥挤距离计算（`assignCrowdingDistance()`）
- [x] 环境选择（`environmentalSelection()`）
- [x] 锦标赛选择（`tournamentSelect()`）
- [x] Tie-breaker机制（`packingQ`, `batchCount`）

### 目标函数
- [x] Cmax（最大完工时间）
- [x] 能耗（含开关机策略）
- [x] 拖期时间（Tardiness）
- [x] 批处理等待时间（WaitingTime）

### 示例和文档
- [x] 快速验证程序（`QuickVerification.java`）
- [x] 完整示例（`EnergyNSGAIIExample.java`）
- [x] 用户使用指南（`ENERGY_USER_GUIDE.md`）
- [x] 模块总览（`README.md`）
- [x] 快速开始（`QUICK_START.md`）

---

## 🚀 如何使用

### 步骤1：导入必要的类

```java
import ProblemFrame.*;
import ProgramEntity.*;
import AlgorithmFrame.nsgaii.*;
```

### 步骤2：加载问题实例

```java
// 方法1: 从文件加载
Problem problem = new Input(new File("instance.txt")).getProblemDesFromFile();

// 方法2: 使用内置测试问题
Problem problem = EnergyNSGAIIExample.createTestProblem();
```

### 步骤3：定义多目标函数

```java
List<MOEvaluator.ObjectiveFunction> objectives = new ArrayList<>();
objectives.add(new MOEvaluator.MaximumCompletionTime());  // Cmax
objectives.add(new MOEvaluator.TotalEnergyConsumption(problem, true, false));  // Energy
```

### 步骤4：初始化种群

```java
List<MOIndividual> population = new ArrayList<>();
InitializationStrategy initStrategy = new InitializationStrategy();
Random random = new Random(42);

for (int i = 0; i < 100; i++) {
    Chromosome chromosome = initStrategy.randomInitialization(problem, random);
    MOIndividual individual = new MOIndividual(chromosome, 2);
    population.add(individual);
}
```

### 步骤5：运行NSGA-II

```java
MOEvaluator evaluator = new MOEvaluator();

for (int gen = 0; gen < 200; gen++) {
    // 评价种群
    for (MOIndividual ind : population) {
        Operation[][] opMatrix = createOperationMatrix(problem);
        evaluator.evaluate(ind, problem, opMatrix, objectives);
    }
    
    // 遗传操作（选择、交叉、变异）
    List<MOIndividual> offspring = geneticOperations(population);
    
    // 环境选择
    population = NSGAIIOperations.environmentalSelection(population, offspring, 100);
}
```

### 步骤6：获取Pareto前沿

```java
List<List<MOIndividual>> fronts = NSGAIIOperations.fastNonDominatedSort(population);
List<MOIndividual> paretoFront = fronts.get(0);

for (MOIndividual ind : paretoFront) {
    System.out.println(String.format("Cmax=%.2f, Energy=%.2f kWh",
        ind.objectives[0], ind.objectives[1]));
}
```

---

## 📈 实验建议

### 1. 消融实验（Ablation Study）

**对比开关机策略的有效性**：
```java
// 实验组1: 启用开关机策略
MOEvaluator.TotalEnergyConsumption objWithSwitching = 
    new MOEvaluator.TotalEnergyConsumption(problem, true, false);

// 实验组2: 禁用开关机策略（baseline）
MOEvaluator.TotalEnergyConsumption objIdleOnly = 
    new MOEvaluator.TotalEnergyConsumption(problem, false, false);

// 分别运行NSGA-II，对比Pareto前沿
```

### 2. 参数敏感性分析

**测试不同能耗参数的影响**：
```java
// 场景1: 低开关机代价
PowerParameters lowSwitchCost = new PowerParameters(5.0, 0.5, 0.5, 3.0, true);

// 场景2: 高开关机代价
PowerParameters highSwitchCost = new PowerParameters(5.0, 0.5, 5.0, 10.0, true);

// 场景3: 高待机功率
PowerParameters highIdlePower = new PowerParameters(5.0, 2.0, 2.0, 5.0, true);
```

### 3. 多目标权衡分析（Trade-off Analysis）

```java
// 计算Pareto前沿的超体积（Hypervolume）
double hv = NSGAIIOperations.calculateHypervolume(paretoFront, referencePoint);

// 分析Cmax和能耗的权衡关系
for (MOIndividual ind : paretoFront) {
    double cmaxDeviation = ind.objectives[0] - minCmax;
    double energySavings = maxEnergy - ind.objectives[1];
    System.out.println(String.format("Cmax增加: %.2f, 能耗节省: %.2f", 
        cmaxDeviation, energySavings));
}
```

---

## 🎓 理论依据

### 1. 能耗模型

**来源**：Che, A., Wu, X., Peng, J., & Yan, P. (2022). "Energy-efficient bi-objective single-machine scheduling with power-down mechanism." *Computers & Operations Research*, 85, 172-183.

**模型**：
```
E_total = E_run + E_idle
E_run = Σ(P_run_k × t_k)
E_idle = Σ(E_gap_i)

E_gap_i = {
    P_idle × gap_i,     if gap_i < T_warmup + T_be
    E_switch,           if gap_i >= T_warmup + T_be
}

T_be = E_switch / P_idle
```

### 2. NSGA-II算法

**来源**：Deb, K., Pratap, A., Agarwal, S., & Meyarivan, T. A. M. T. (2002). "A fast and elitist multiobjective genetic algorithm: NSGA-II." *IEEE Transactions on Evolutionary Computation*, 6(2), 182-197.

**核心思想**：
- 快速非支配排序（O(MN²)）
- 拥挤距离计算（保持多样性）
- 精英保留策略

---

## 📞 技术支持

- **文档**：参见 `ENERGY_USER_GUIDE.md`
- **示例**：运行 `EnergyNSGAIIExample.java`
- **测试**：运行 `QuickVerification.java`

**祝你的研究顺利！** 🎉


