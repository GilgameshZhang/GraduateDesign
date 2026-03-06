# MOEA/D 算法实现说明文档

## 目录
1. [算法简介](#算法简介)
2. [算法原理](#算法原理)
3. [使用方法](#使用方法)
4. [参数说明](#参数说明)
5. [示例代码](#示例代码)
6. [与NSGA-II对比](#与nsga-ii对比)
7. [文件说明](#文件说明)

---

## 算法简介

**MOEA/D** (Multi-Objective Evolutionary Algorithm based on Decomposition，基于分解的多目标进化算法) 是由张青富等人在2007年提出的经典多目标优化算法。

### 核心思想
- 将多目标优化问题**分解**为多个单目标子问题
- 每个子问题使用不同的**权重向量**
- 相邻子问题通过**邻域协作**共享信息
- 使用**聚合函数**将多目标转换为单目标

### 主要优势
1. ✅ **计算效率高**：比NSGA-II更快（无需非支配排序）
2. ✅ **分布性好**：权重向量保证解的均匀分布
3. ✅ **可扩展性强**：适合3目标及以上的问题
4. ✅ **参数少**：核心参数仅3个（T, δ, nr）

---

## 算法原理

### 1. 问题分解

将多目标问题分解为 **N** 个单目标子问题，每个子问题对应一个权重向量 **λ**：

```
子问题 i: minimize g(x | λᵢ, z*)
```

其中：
- `x`: 候选解
- `λᵢ`: 第i个权重向量
- `z*`: 理想点（每个目标的最优值）
- `g()`: 聚合函数

### 2. 聚合函数

#### (1) Tchebycheff方法（推荐）
```
g^te(x|λ,z*) = max_{m=1..M} { λₘ * |fₘ(x) - z*ₘ| }
```

**优点**：可以找到凹凸混合的Pareto前沿

#### (2) 加权和方法
```
g^ws(x|λ,z*) = Σ λₘ * (fₘ(x) - z*ₘ)
```

**缺点**：无法找到非凸Pareto前沿的凹陷部分

#### (3) PBI方法
```
g^pbi(x|λ,z*) = d₁ + θ * d₂
```

**说明**：
- `d₁`: 沿权重方向的距离
- `d₂`: 垂直于权重方向的距离
- `θ`: 惩罚参数（通常为5.0）

### 3. 邻域结构

基于权重向量的**欧氏距离**计算邻域：

```
每个子问题的邻域 = 距离最近的 T 个子问题
```

### 4. 算法流程

```
1. 初始化：
   - 生成 N 个均匀分布的权重向量
   - 计算邻域结构（每个子问题的 T 个邻居）
   - 初始化种群和理想点 z*

2. 主循环（每代）：
   For each 子问题 i = 1 to N:
     a) 选择父代（从邻域或全局）
     b) 交叉和变异生成子代
     c) 评价子代并更新 z*
     d) 更新邻域解（最多替换 nr 个）

3. 输出：提取Pareto前沿
```

---

## 使用方法

### 快速开始

```java
// 1. 加载问题
Problem problem = new Problem("path/to/problem.txt");

// 2. 定义目标函数
List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),      // Cmax
    new MOEvaluator.TotalEnergyConsumption(problem)  // 能耗
);

// 3. 创建MOEA/D算法
MOEAD moead = new MOEAD(problem, objectives);
moead.setPopulationSize(100);
moead.setMaxGenerations(500);
moead.setSeed(12345L);

// 4. 运行算法
List<MOIndividual> paretoFront = moead.solve();

// 5. 输出结果
for (MOIndividual ind : paretoFront) {
    System.out.println(ind.objectivesToString());
}
```

### 使用Runner类

```java
// 方法1：快速运行
MOEADRunner.quickRun(
    "path/to/problem.txt",  // 问题文件
    "output/",              // 输出目录
    100,                    // 种群大小
    500,                    // 最大代数
    12345L                  // 随机种子
);

// 方法2：完整配置
MOEADRunner.MOEADConfig config = new MOEADRunner.MOEADConfig();
config.populationSize = 100;
config.maxGenerations = 500;
config.neighborhoodSize = 20;
config.scalarizingFunction = MOEADOperations.ScalarizingFunction.TCHEBYCHEFF;
config.enableLocalSearch = true;

MOEADRunner.fullRun("path/to/problem.txt", "output/", config);
```

---

## 参数说明

### 基础参数

| 参数 | 默认值 | 说明 |
|-----|--------|------|
| `populationSize` | 100 | 种群大小（子问题数量N） |
| `crossoverRate` | 0.9 | 交叉率 |
| `mutationRate` | 0.1 | 变异率 |
| `maxGenerations` | 500 | 最大进化代数 |
| `maxRunTimeMinutes` | 5.0 | 最大运行时间（分钟） |

### MOEA/D特有参数

| 参数 | 符号 | 默认值 | 推荐范围 | 说明 |
|-----|------|--------|----------|------|
| `neighborhoodSize` | T | 20 | [10, 30] | 邻域大小（影响局部搜索范围） |
| `neighborhoodSelectionProb` | δ | 0.9 | [0.8, 0.95] | 从邻域选择的概率 |
| `maxReplacementSize` | nr | 2 | [1, 3] | 每次最多替换的解数量 |
| `scalarizingFunction` | - | TCHEBYCHEFF | - | 聚合函数类型 |

#### 参数调优建议

**邻域大小 T**：
- 小规模问题（N<50）：`T = 10`
- 中等规模（50≤N≤200）：`T = 20`
- 大规模问题（N>200）：`T = 30`

**邻域选择概率 δ**：
- 希望更快收敛：`δ = 0.9 ~ 0.95`
- 希望更多样化：`δ = 0.7 ~ 0.8`

**最大替换数 nr**：
- 更激进更新：`nr = 3`
- 保守更新（推荐）：`nr = 2`
- 非常保守：`nr = 1`

### 聚合函数选择

```java
// 方法1：Tchebycheff（推荐，适用于所有问题）
moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.TCHEBYCHEFF);

// 方法2：加权和（仅适用于凸Pareto前沿）
moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.WEIGHTED_SUM);

// 方法3：PBI（收敛性好，但参数敏感）
moead.setScalarizingFunction(MOEADOperations.ScalarizingFunction.PBI);
```

### 局部搜索配置

```java
// 启用局部搜索
moead.enableLocalSearch(true, 10);  // 每10代执行一次

// 配置局部搜索参数
moead.setLocalSearchParameters(
    10,      // L: 每个精英的尝试次数
    0.10,    // eta: 精英比例（10%）
    0.05,    // epsE: E型个体能耗松弛度
    0.02,    // epsC: T型个体Cmax松弛度
    0.005,   // improvC: T型要求Cmax改进幅度
    0.01     // improvE: E型要求能耗改进幅度
);
```

---

## 示例代码

### 示例1：基本使用

```java
import AlgorithmFrame.moead.*;
import ProblemFrame.*;
import ProgramEntity.*;
import java.util.*;

public class Example1_Basic {
    public static void main(String[] args) {
        // 加载问题
        Problem problem = new Problem("data/test_10jobs.txt");
        
        // 定义双目标
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        );
        
        // 创建并配置MOEA/D
        MOEAD moead = new MOEAD(problem, objectives);
        moead.setPopulationSize(50);
        moead.setMaxGenerations(100);
        moead.setSeed(12345L);
        
        // 运行算法
        List<MOIndividual> pareto = moead.solve();
        
        // 输出结果
        System.out.println("找到 " + pareto.size() + " 个Pareto最优解");
    }
}
```

### 示例2：对比不同聚合函数

```java
public class Example2_CompareFunctions {
    public static void main(String[] args) {
        String problemFile = "data/test_10jobs.txt";
        
        // 对比三种聚合函数
        MOEADRunner.compareScalarizingFunctions(
            problemFile,
            "output/comparison/",  // 输出基础目录
            100,                   // 种群大小
            500,                   // 最大代数
            12345L                 // 随机种子
        );
        
        // 结果将分别保存到：
        // - output/comparison/TCHEBYCHEFF/
        // - output/comparison/WEIGHTED_SUM/
        // - output/comparison/PBI/
    }
}
```

### 示例3：MOEA/D vs NSGA-II

```java
public class Example3_Comparison {
    public static void main(String[] args) {
        // 运行对比实验
        ComparisonExperiment.runComparison(
            "data/test_20jobs.txt",  // 问题文件
            100,                     // 种群大小
            500,                     // 最大代数
            12345L                   // 随机种子
        );
        
        // 输出：
        // - Pareto前沿大小
        // - 运行时间
        // - 目标值范围
        // - 超体积指标
    }
}
```

### 示例4：带局部搜索

```java
public class Example4_LocalSearch {
    public static void main(String[] args) {
        Problem problem = new Problem("data/test_30jobs.txt");
        
        List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        );
        
        MOEAD moead = new MOEAD(problem, objectives);
        moead.setPopulationSize(100);
        moead.setMaxGenerations(500);
        
        // 启用局部搜索（每10代执行一次）
        moead.enableLocalSearch(true, 10);
        
        // 运行
        List<MOIndividual> pareto = moead.solve();
        
        System.out.println("Pareto前沿大小: " + pareto.size());
    }
}
```

---

## 与NSGA-II对比

### 算法特点对比

| 特性 | MOEA/D | NSGA-II |
|-----|--------|---------|
| **核心机制** | 分解+聚合 | 非支配排序+拥挤距离 |
| **时间复杂度** | O(NM) | O(MN²) |
| **空间复杂度** | O(N²) (邻域) | O(N) |
| **分布性** | ⭐⭐⭐⭐⭐ 极好（权重保证） | ⭐⭐⭐⭐ 好（拥挤距离） |
| **收敛性** | ⭐⭐⭐⭐ 好 | ⭐⭐⭐⭐⭐ 很好 |
| **多目标扩展** | ⭐⭐⭐⭐⭐ 优秀（M≥3） | ⭐⭐⭐ 一般（M≥4困难） |
| **参数敏感性** | ⭐⭐⭐ 中等（T, δ, nr） | ⭐⭐ 低 |

### 性能对比（典型场景）

**双目标问题（M=2）**：
- MOEA/D：✅ 快1.5-2倍，分布更均匀
- NSGA-II：✅ 收敛性稍好

**三目标问题（M=3）**：
- MOEA/D：✅✅ 明显优势，快2-3倍
- NSGA-II：⚠️ 性能下降明显

**四目标及以上（M≥4）**：
- MOEA/D：✅✅✅ 强烈推荐
- NSGA-II：❌ 不推荐（效率低下）

### 适用场景建议

**选择 MOEA/D**：
- ✅ 目标数 M ≥ 3
- ✅ 需要计算效率
- ✅ 需要解的均匀分布
- ✅ Pareto前沿形状已知（可调整聚合函数）

**选择 NSGA-II**：
- ✅ 双目标问题（M=2）
- ✅ 强调收敛性
- ✅ Pareto前沿形状未知
- ✅ 参数调优能力有限

**折中方案**：
- 两者都运行，取并集（推荐用于论文对比）
- 使用MOEA/D-DRA（动态资源分配，更先进）

---

## 文件说明

### 核心文件

```
AlgorithmFrame/moead/
├── MOEAD.java                    # MOEA/D主算法类
├── MOEADOperations.java          # 算子操作（聚合函数、邻域）
├── MOEADRunner.java              # 运行器（便捷接口）
├── QuickStart.java               # 快速测试入口
├── ComparisonExperiment.java     # 对比实验类
└── README.md                     # 本文档
```

### 类说明

#### 1. `MOEAD.java`
主算法类，包含：
- 权重向量生成
- 邻域结构初始化
- 理想点更新
- 子问题优化
- Pareto前沿提取

**主要方法**：
```java
List<MOIndividual> solve()                     // 执行算法
void setNeighborhoodSize(int T)               // 设置邻域大小
void setScalarizingFunction(...)              // 设置聚合函数
void enableLocalSearch(boolean, int)          // 启用局部搜索
```

#### 2. `MOEADOperations.java`
算子操作类，包含：
- 三种聚合函数实现
- 权重向量生成工具
- 邻域计算工具
- 交叉变异算子（复用NSGA-II）

**主要方法**：
```java
double calculateScalarValue(...)              // 计算聚合函数值
double tchebycheff(...)                       // Tchebycheff函数
double weightedSum(...)                       // 加权和函数
double pbi(...)                               // PBI函数
```

#### 3. `MOEADRunner.java`
运行器类，提供便捷接口：
```java
void quickRun(...)                            // 快速运行
void fullRun(..., MOEADConfig)                // 完整配置运行
void compareScalarizingFunctions(...)         // 对比聚合函数
```

#### 4. `QuickStart.java`
快速测试入口，用于验证算法功能。

#### 5. `ComparisonExperiment.java`
对比实验类，用于：
- MOEA/D vs NSGA-II 单次对比
- 批量对比（多随机种子）
- 超体积指标计算

---

## 运行测试

### 方法1：使用QuickStart

```bash
# 进入项目根目录
cd chapter-3

# 编译
javac -d bin src/main/java/AlgorithmFrame/moead/*.java

# 运行快速测试
java -cp bin AlgorithmFrame.moead.QuickStart
```

### 方法2：使用IDE
1. 在IDEA/Eclipse中打开项目
2. 找到 `QuickStart.java`
3. 右键 → Run 'QuickStart.main()'

### 方法3：使用Maven（如果配置了）

```bash
mvn exec:java -Dexec.mainClass="AlgorithmFrame.moead.QuickStart"
```

---

## 输出结果

### 控制台输出

```
========================================
      MOEA/D 多目标进化算法 (基于分解)
========================================
问题: 10个工件
目标数: 2
目标函数: Cmax Energy(开关机) 
种群大小: 100
交叉率: 0.9
变异率: 0.1
邻域大小: 20
聚合函数: TCHEBYCHEFF
最大代数: 500
最大时间: 5.0 分钟
========================================

初始化权重向量...
生成权重向量数量: 100
计算邻域结构...
初始化种群...
[初始化] 生成多样化初始种群：
  - 随机策略: 20个
  - 按高度降序+轮盘赌: 20个
  - 按面积降序+轮盘赌: 20个
  - 按高度降序+负载均衡: 20个
  - 按高度降序+最短加工时间: 20个
  - 混合策略: 0个
评价初始种群...
初始理想点: [1234.56, 5678.90]

开始进化...

代数    1 | Pareto前沿大小:  15 | 耗时: 1.23秒 | 示例解: [1200.00, 5500.00]
代数   10 | Pareto前沿大小:  22 | 耗时: 5.67秒 | 示例解: [1150.00, 5300.00]
...
代数  500 | Pareto前沿大小:  35 | 耗时: 120.45秒 | 示例解: [1100.00, 5000.00]

========================================
           算法运行完成
========================================
总代数: 500
总耗时: 120.45 秒
Pareto前沿大小: 35
========================================

Pareto最优解集:
----------------------------------------
解  1: Cmax=1100.00 Energy=5500.00 | packingQ=0.8500 | batches=5
解  2: Cmax=1120.00 Energy=5300.00 | packingQ=0.8600 | batches=5
解  3: Cmax=1150.00 Energy=5100.00 | packingQ=0.8700 | batches=4
...
解 10: Cmax=1300.00 Energy=4800.00 | packingQ=0.9000 | batches=4
... (共35个Pareto解)
========================================
```

---

## 常见问题

### Q1: 如何选择聚合函数？

**A:** 
- **默认推荐**：`TCHEBYCHEFF`（适用于所有问题）
- **凸Pareto前沿**：`WEIGHTED_SUM` 或 `TCHEBYCHEFF`
- **非凸Pareto前沿**：`TCHEBYCHEFF` 或 `PBI`
- **不确定**：先用 `TCHEBYCHEFF`

### Q2: 邻域大小T如何设置？

**A:** 经验公式：`T = 0.1 * N` 到 `0.2 * N`
- N=50: T=10
- N=100: T=20
- N=200: T=30

### Q3: MOEA/D比NSGA-II慢？

**A:** 可能原因：
1. 权重向量生成开销（初始化一次）
2. 邻域选择逻辑（可优化）
3. 聚合函数计算（Tchebycheff很快）

**解决方案**：
- 使用更小的邻域（T=10-15）
- 减少 `maxReplacementSize`（nr=1）

### Q4: Pareto前沿分布不均？

**A:** 
1. 检查权重向量生成是否均匀
2. 尝试增大邻域大小T
3. 尝试不同的聚合函数（PBI）

### Q5: 如何保存结果？

**A:** 使用 `NSGAIIResultExporter`（已集成）：
```java
NSGAIIResultExporter.exportResults(
    moead.getParetoFront(),
    moead.getParetoSizeHistory(),
    moead.getHypervolomeHistory(),
    "output/",
    "MOEAD"
);
```

生成文件：
- `MOEAD_pareto_front.csv` - Pareto前沿数据
- `MOEAD_statistics.txt` - 统计信息
- `MOEAD_convergence.png` - 收敛曲线（如果有绘图库）

---

## 参考文献

1. **Zhang, Q., & Li, H. (2007).** MOEA/D: A multiobjective evolutionary algorithm based on decomposition. *IEEE Transactions on Evolutionary Computation*, 11(6), 712-731.

2. **Li, H., & Zhang, Q. (2009).** Multiobjective optimization problems with complicated Pareto sets, MOEA/D and NSGA-II. *IEEE Transactions on Evolutionary Computation*, 13(2), 284-302.

3. **Zhang, Q., Liu, W., & Li, H. (2009).** The performance of a new version of MOEA/D on CEC09 unconstrained MOP test instances. *IEEE Congress on Evolutionary Computation*, 203-208.

---

## 更新日志

### v1.0 (2026-01-19)
- ✅ 实现MOEA/D核心算法
- ✅ 支持三种聚合函数（Tchebycheff、Weighted Sum、PBI）
- ✅ 集成局部搜索（复用NSGA-II）
- ✅ 提供便捷的Runner接口
- ✅ 实现与NSGA-II的对比实验
- ✅ 完整的文档和示例

---

## 联系方式

如有问题或建议，请联系：
- 作者：AI Assistant
- 项目：毕业设计 - 第三章对比算法

---

**祝您使用愉快！🎉**
