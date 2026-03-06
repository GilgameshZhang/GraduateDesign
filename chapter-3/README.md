# NSGA-II 多目标遗传算法 - Chapter 3

## 📚 概述

本模块实现了基于 NSGA-II 的多目标遗传算法，用于求解增材制造车间调度的多目标优化问题。

### 核心特性

✅ **完整的 NSGA-II 实现**
- 快速非支配排序 (Fast Non-dominated Sort)
- 拥挤距离计算 (Crowding Distance Assignment)
- 环境选择 (Environmental Selection)
- 精英保留策略

✅ **标准 tie-breaker 机制**
- 拥挤距离接近时使用 packingQ（装箱质量）
- 次级 tie-breaker：批次数量

✅ **多目标支持**
- Cmax (最大完工时间)
- 能耗 (Total Energy Consumption)
- 拖期时间 (Total Tardiness)
- 批处理等待时间 (Batch Waiting Time)
- 可扩展自定义目标

✅ **与 chapter-2 兼容**
- 保持相同的编码/解码逻辑
- 复用天际线装箱算法
- 不修改原有模块代码

---

## 🏗️ 架构设计

### 模块结构

```
chapter-3/
├── src/main/java/
│   ├── AlgorithmFrame/nsgaii/
│   │   ├── NSGAII.java                  # 主算法类
│   │   └── NSGAIIOperations.java        # 核心操作（排序、拥挤距离等）
│   ├── ProblemFrame/
│   │   ├── MOIndividual.java            # 多目标个体类
│   │   └── MOEvaluator.java             # 多目标评价函数
│   └── ProgramEntity/                   # 从chapter-2复制的实体类
├── src/test/java/
│   └── NSGAIITest.java                  # 单元测试
└── pom.xml
```

### 核心类说明

#### 1. MOIndividual（多目标个体）

继承自单目标染色体，增加多目标相关字段：

```java
public class MOIndividual {
    // 基础编码（与chapter-2相同）
    int[] gene_OS;              // 工序序列
    int[] gene_MS;              // 机器选择
    
    // 多目标字段
    double[] objectives;        // 目标值数组
    int rank;                   // 非支配层号
    double crowdingDistance;    // 拥挤距离
    
    // tie-breaker 字段
    double packingQ;            // 装箱质量（不作为目标）
    int batchCount;             // 批次总数
}
```

#### 2. NSGAIIOperations（核心操作）

- `fastNonDominatedSort()`: O(MN²) 快速非支配排序
- `assignCrowdingDistance()`: O(MN log N) 拥挤距离计算
- `environmentalSelection()`: 环境选择（带截断）
- `tournamentSelection()`: 锦标赛选择

#### 3. MOEvaluator（多目标评价）

- 复用 chapter-2 的 `CaculateFitness.evaluate()`
- 扩展计算 packingQ（最后一批占用率）
- 支持多种预定义目标函数

#### 4. NSGAII（主算法）

标准的 NSGA-II 主循环：
1. 初始化种群
2. 非支配排序 + 拥挤距离
3. 锦标赛选择
4. 交叉变异生成子代
5. 环境选择（P ∪ Q → P'）
6. 迭代直到终止条件

---

## 🚀 快速开始

### 1. 基本使用（双目标：Cmax + 能耗）

```java
// 读取问题实例（复用chapter-2的Input类）
Input input = new Input(new File("算例路径.txt"));
Problem problem = input.getProblemDesFromFile();

// 创建NSGA-II算法（默认双目标）
NSGAII nsgaii = new NSGAII(problem);

// 设置参数（可选）
nsgaii.setPopulationSize(100);
nsgaii.setCrossoverRate(0.9);
nsgaii.setMutationRate(0.1);
nsgaii.setMaxGenerations(500);
nsgaii.setMaxRunTimeMinutes(5.0);

// 运行算法
List<MOIndividual> paretoFront = nsgaii.solve();

// 输出结果
System.out.println("找到 " + paretoFront.size() + " 个Pareto最优解");
for (MOIndividual ind : paretoFront) {
    System.out.println(ind);
}
```

### 2. 自定义目标函数

```java
// 定义多个目标（最小化）
List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),      // Cmax
    new MOEvaluator.TotalEnergyConsumption(),     // 能耗
    new MOEvaluator.TotalBatchWaitingTime()       // 等待时间
);

// 创建NSGA-II实例
NSGAII nsgaii = new NSGAII(problem, objectives);

// 运行
List<MOIndividual> paretoFront = nsgaii.solve();
```

### 3. 配置 packingQ 计算模式

```java
NSGAII nsgaii = new NSGAII(problem);

// 设置packingQ模式
nsgaii.setPackingQMode(MOEvaluator.PackingQMode.LAST_BATCH_MIN);  // 最小值
// 或
nsgaii.setPackingQMode(MOEvaluator.PackingQMode.LAST_BATCH_AVG);  // 平均值

List<MOIndividual> paretoFront = nsgaii.solve();
```

---

## 🧪 运行单元测试

```bash
cd chapter-3
mvn test
```

测试内容：
- ✅ 支配关系判断
- ✅ 快速非支配排序
- ✅ 拥挤距离计算（边界=INF）
- ✅ 环境选择（恰好N个）
- ✅ tie-breaker机制
- ✅ 锦标赛选择

---

## 📊 输出结果

### 运行时输出

```
========================================
      NSGA-II 多目标遗传算法
========================================
问题: 20个工件
目标数: 2
目标函数: Cmax Energy 
种群大小: 100
交叉率: 0.9
变异率: 0.1
最大代数: 500
最大时间: 5.0 分钟
========================================

初始化种群...
评价初始种群...
初始种群Pareto前沿大小: 8

开始进化...

代数    1 | Pareto前沿大小:   8 | 耗时: 2.34秒 | 示例解: [850.23, 12345.67]
代数   10 | Pareto前沿大小:  12 | 耗时: 23.45秒 | 示例解: [820.15, 11890.23]
...
```

### Pareto前沿输出

```
Pareto最优解集:
----------------------------------------
解  1: Cmax=820.15 Energy=11890.23 | packingQ=0.7854 | batches=12
解  2: Cmax=835.42 Energy=11234.56 | packingQ=0.8123 | batches=11
解  3: Cmax=850.78 Energy=10890.12 | packingQ=0.8456 | batches=10
...
```

---

## 🔧 关键算法细节

### 1. 支配关系（最小化）

A 支配 B ⟺ ∀i: A[i] ≤ B[i] ∧ ∃j: A[j] < B[j]

```java
public boolean dominates(MOIndividual other) {
    boolean atLeastOneBetter = false;
    for (int i = 0; i < objectives.length; i++) {
        if (this.objectives[i] > other.objectives[i]) {
            return false;  // 有一个目标更差
        }
        if (this.objectives[i] < other.objectives[i]) {
            atLeastOneBetter = true;
        }
    }
    return atLeastOneBetter;
}
```

### 2. 拥挤距离计算

对前沿 F 中的每个个体 i：

```
crowding[i] = Σ_m (f_m[i+1] - f_m[i-1]) / (f_m^max - f_m^min)
```

边界个体：`crowding = +∞`
前沿大小 ≤ 2：所有个体 `crowding = +∞`

### 3. 截断时的 tie-breaker

当拥挤距离接近（|cd_a - cd_b| ≤ δ）时：

```java
public int compareForTruncation(MOIndividual other, double delta) {
    // 1. 拥挤距离差异 > delta，按拥挤距离排序
    if (|this.crowding - other.crowding| > delta) {
        return -compare(this.crowding, other.crowding);
    }
    
    // 2. tie-breaker: packingQ 更大优先
    if (|this.packingQ - other.packingQ| > 1e-9) {
        return -compare(this.packingQ, other.packingQ);
    }
    
    // 3. 次级 tie-breaker: batchCount 更少优先
    return compare(this.batchCount, other.batchCount);
}
```

### 4. packingQ 计算

**模式1：LAST_BATCH_MIN**（默认）

```
packingQ = min_i (usedArea_i / plateArea_i)
```

**模式2：LAST_BATCH_AVG**

```
packingQ = avg_i (usedArea_i / plateArea_i)
```

其中 i 遍历所有打印机的最后一批。

---

## 📈 性能与复杂度

| 操作 | 时间复杂度 | 说明 |
|------|----------|------|
| 非支配排序 | O(MN²) | M=目标数, N=种群大小 |
| 拥挤距离 | O(MN log N) | 每个目标需要排序 |
| 环境选择 | O(MN²) | 包含排序和截断 |
| 锦标赛选择 | O(1) | 常数时间 |

**单代总复杂度**：O(MN²)

**典型运行时间**（N=100, M=2, 20工件）：
- 单代评价：~0.5秒
- 500代总耗时：~4-5分钟

---

## 🎯 预定义目标函数

### 1. MaximumCompletionTime (Cmax)

最大完工时间（makespan）

```java
new MOEvaluator.MaximumCompletionTime()
```

### 2. TotalEnergyConsumption

总能耗（简化模型）

```java
// 默认功率系数
new MOEvaluator.TotalEnergyConsumption()

// 自定义功率系数
new MOEvaluator.TotalEnergyConsumption(
    printPower=2.0,    // 打印功率
    batchPower=1.5,    // 批处理功率
    discretePower=1.0  // 离散加工功率
)
```

### 3. TotalTardiness

总拖期时间

```java
new MOEvaluator.TotalTardiness()
```

### 4. TotalBatchWaitingTime

批处理等待时间总和

```java
new MOEvaluator.TotalBatchWaitingTime()
```

---

## 🔌 扩展：自定义目标函数

实现 `MOEvaluator.ObjectiveFunction` 接口：

```java
public class CustomObjective implements MOEvaluator.ObjectiveFunction {
    @Override
    public double calculate(Chromosome chromosome, 
                          Problem problem, 
                          Operation[][] operationMatrix) {
        // 你的目标计算逻辑
        double objectiveValue = 0.0;
        
        // 例如：计算总流程时间
        for (int i = 0; i < operationMatrix.length; i++) {
            int lastOp = operationMatrix[i].length - 1;
            objectiveValue += operationMatrix[i][lastOp].endTime;
        }
        
        return objectiveValue;
    }
    
    @Override
    public String getName() {
        return "CustomObj";
    }
}
```

使用：

```java
List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),
    new CustomObjective()
);

NSGAII nsgaii = new NSGAII(problem, objectives);
```

---

## 📊 结果可视化（TODO）

可以扩展实现：
- Pareto前沿散点图
- 超体积指标演化曲线
- 并行坐标图（Parallel Coordinates）

---

## ⚙️ 参数调优建议

| 参数 | 推荐值 | 说明 |
|------|--------|------|
| populationSize | 100-200 | 目标数越多，种群应越大 |
| crossoverRate | 0.8-0.95 | 高交叉率有利于探索 |
| mutationRate | 0.05-0.15 | 适度变异保持多样性 |
| tournamentSize | 2-3 | 2或3通常效果最好 |
| crowdingDelta | 1e-6 ~ 1e-4 | 根据目标值尺度调整 |

---

## 🐛 调试与验证

### 运行单元测试

```bash
mvn test -Dtest=NSGAIITest
```

### 手动验证检查点

✅ **拥挤距离边界检查**

```java
List<MOIndividual> front = ...;
NSGAIIOperations.assignCrowdingDistance(front);

// 验证：前沿大小<=2时，所有个体crowding=INF
if (front.size() <= 2) {
    for (MOIndividual ind : front) {
        assert Double.isInfinite(ind.crowdingDistance);
    }
}
```

✅ **环境选择大小检查**

```java
List<MOIndividual> nextPop = NSGAIIOperations.environmentalSelection(
    parent, offspring, N, delta
);
assert nextPop.size() == N;  // 必须恰好N个
```

✅ **tie-breaker验证**

```java
// 创建两个拥挤距离接近的个体
MOIndividual ind1 = ...; // crowding=0.5, packingQ=0.8
MOIndividual ind2 = ...; // crowding=0.5, packingQ=0.7

int cmp = ind1.compareForTruncation(ind2, 1e-6);
assert cmp < 0;  // packingQ更大的ind1应该更优
```

---

## 📚 参考文献

1. **Deb, K., et al. (2002)**  
   "A fast and elitist multiobjective genetic algorithm: NSGA-II"  
   IEEE Transactions on Evolutionary Computation, 6(2), 182-197.

2. **Zhang, Q., & Li, H. (2007)**  
   "MOEA/D: A multiobjective evolutionary algorithm based on decomposition"  
   IEEE Transactions on Evolutionary Computation, 11(6), 712-731.

---

## 🔗 与其他章节的关系

- **chapter-1**: 基础框架（不依赖）
- **chapter-2**: 单目标GA，提供编码/解码逻辑（**需要复制部分类**）
- **chapter-3**: 本模块，多目标扩展（**独立模块**）

---

## ✅ 实现状态

| 功能 | 状态 | 说明 |
|------|------|------|
| MOIndividual类 | ✅ 完成 | 包含所有多目标字段 |
| 快速非支配排序 | ✅ 完成 | O(MN²) 算法 |
| 拥挤距离计算 | ✅ 完成 | 边界=INF，N<=2全INF |
| 环境选择 | ✅ 完成 | 带tie-breaker截断 |
| 锦标赛选择 | ✅ 完成 | NSGA-II比较规则 |
| packingQ计算 | ✅ 完成 | 支持MIN/AVG模式 |
| 多目标评价 | ✅ 完成 | 4个预定义目标 |
| 主算法NSGAII | ✅ 完成 | 完整主循环 |
| 单元测试 | ✅ 完成 | 6个测试用例 |
| 文档 | ✅ 完成 | 完整README |

---

## 🚀 下一步工作（可选扩展）

1. **可视化模块**
   - Pareto前沿散点图
   - 超体积演化曲线
   - 并行坐标图

2. **性能优化**
   - 并行评价（多线程）
   - 缓存机制（避免重复评价）

3. **高级特性**
   - 自适应参数调整
   - 约束处理机制
   - 偏好引导搜索

4. **与chapter-2完整集成**
   - 复制所有必要的ProgramEntity类
   - 集成POX/LOX交叉算子
   - 集成局部搜索策略

---

## 📞 联系方式

如有问题或建议，请查看单元测试代码或提交Issue。

**版本**: v1.0  
**创建日期**: 2025-12-29  
**作者**: AI Assistant

