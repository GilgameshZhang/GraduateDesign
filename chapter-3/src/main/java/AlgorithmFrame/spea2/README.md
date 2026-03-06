# SPEA2 多目标进化算法

## 简介

**SPEA2 (Strength Pareto Evolutionary Algorithm 2)** 是Zitzler等人在2001年提出的经典多目标进化算法，是SPEA算法的改进版本。

### 主要特点

1. **改进的适应度分配机制**
   - 基于强度值（Strength）的适应度计算
   - 原始适应度 = 所有支配该个体的个体的强度值之和
   - 最终适应度 = 原始适应度 + 密度值

2. **k近邻密度估计**
   - 使用第k近邻距离来估计个体周围的密度
   - 密度值 D(i) = 1 / (σᵢᵏ + 2)
   - 其中 σᵢᵏ 是第i个个体到第k近邻的欧氏距离

3. **外部存档机制**
   - 维护一个固定大小的外部存档来保存非支配解
   - 当存档满时，通过截断操作移除密度最大区域的个体
   - 当存档未满时，从种群中添加最优的支配解

4. **改进的存档截断方法**
   - 迭代移除最拥挤的个体（最小距离最小的个体）
   - 确保Pareto前沿的均匀分布

### 与NSGA-II的区别

| 特性 | SPEA2 | NSGA-II |
|-----|-------|---------|
| 选择机制 | 外部存档 + 二元锦标赛 | 拥挤距离锦标赛 |
| 适应度分配 | 强度值 + 密度 | 非支配层级 + 拥挤距离 |
| 多样性保持 | k近邻密度估计 | 拥挤距离 |
| 精英保留 | 存档机制 | 环境选择 |
| 计算复杂度 | O(MN²log N) | O(MN²) |

## 文件结构

```
spea2/
├── SPEA2.java                      # SPEA2核心算法
├── SPEA2Operations.java            # SPEA2算法组件
├── SPEA2Runner.java                # 便捷运行器
├── SPEA2ComparisonExperiment.java  # 对比实验
├── QuickStart.java                 # 快速开始示例
└── README.md                       # 本文档
```

## 快速开始

### 1. 最简单的使用

```java
// 加载问题
File instanceFile = new File("path/to/problem.txt");
EnergyAwareInput input = new EnergyAwareInput(instanceFile);
Problem problem = input.getProblemDesFromFile();

// 定义优化目标
List<MOEvaluator.ObjectiveFunction> objectives = Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),
    new MOEvaluator.TotalEnergyConsumption(problem)
);

// 创建并运行SPEA2
SPEA2 spea2 = new SPEA2(problem, objectives);
spea2.setPopulationSize(50);
spea2.setMaxGenerations(100);
List<MOIndividual> paretoFront = spea2.solve();
```

### 2. 使用Runner快速运行

```java
SPEA2Runner.quickRun(
    "path/to/problem.txt",
    "output/results",
    50,      // 种群大小
    100,     // 最大代数
    12345L   // 随机种子
);
```

### 3. 启用局部搜索

```java
SPEA2 spea2 = new SPEA2(problem, objectives);
spea2.setPopulationSize(100);
spea2.setMaxGenerations(200);

// 启用局部搜索
spea2.enableLocalSearch(true, 10);  // 每10代执行一次
spea2.setLocalSearchParameters(
    10,     // L: 每个精英尝试次数
    0.10,   // eta: 选择10%精英
    0.05,   // epsE: T型允许能耗上升5%
    0.02,   // epsC: E型允许Cmax上升2%
    0.005,  // improvC: T型要求Cmax下降0.5%
    0.01    // improvE: E型要求能耗下降1%
);

List<MOIndividual> paretoFront = spea2.solve();
```

## 参数说明

### 基础参数

| 参数 | 类型 | 默认值 | 说明 |
|-----|------|--------|------|
| populationSize | int | 100 | 种群大小 N |
| archiveSize | int | 100 | 存档大小 N̄（通常等于种群大小）|
| crossoverRate | double | 0.9 | 交叉率 |
| mutationRate | double | 0.1 | 变异率 |
| maxGenerations | int | 500 | 最大代数 |
| maxRunTimeMinutes | double | 5.0 | 最大运行时间（分钟）|
| kNearest | int | √(N+N̄) | k近邻参数（推荐值：√(N+N̄)）|
| tournamentSize | int | 2 | 锦标赛大小 |
| seed | long | - | 随机种子 |

### 局部搜索参数

| 参数 | 说明 |
|-----|------|
| enableLocalSearch | 是否启用局部搜索 |
| localSearchInterval | 局部搜索间隔（每隔多少代执行一次）|
| L | 每个精英个体的尝试次数 |
| eta | 精英比例（0-1之间）|
| epsE | T型算子允许的能耗上升比例 |
| epsC | E型算子允许的Cmax上升比例 |
| improvC | T型算子要求的Cmax下降比例 |
| improvE | E型算子要求的能耗下降比例 |

## 算法流程

```
1. 初始化种群 P₀（大小为N）
2. 创建空存档 P̄₀
3. 合并 P₀ + P̄₀，计算适应度
4. 环境选择得到新存档 P̄₁（大小为N̄）

对于 t = 1 到 最大代数：
    5. 从存档 P̄ₜ 中二元锦标赛选择父代
    6. 交叉和变异生成子代 Pₜ
    7. 评价子代
    8. 合并 Pₜ + P̄ₜ
    9. 计算适应度（强度值 + 密度）
    10. 环境选择得到新存档 P̄ₜ₊₁
    11. [可选] 局部搜索
    
返回：存档中的非支配解
```

## 对比实验

### 单次对比

```java
SPEA2ComparisonExperiment.runComparison(
    "path/to/problem.txt",
    50,      // 种群大小
    100,     // 最大代数
    12345L   // 随机种子
);
```

### 批量对比（多次运行取平均）

```java
SPEA2ComparisonExperiment.runMultipleComparisons(
    "path/to/problem.txt",
    50,      // 种群大小
    100,     // 最大代数
    5        // 运行次数
);
```

### 评价指标

对比实验会计算以下指标：

1. **Pareto前沿大小**：Pareto最优解的数量
2. **运行时间**：算法总耗时
3. **超体积（Hypervolume）**：Pareto前沿在目标空间的覆盖范围
4. **C指标（覆盖度）**：C(A,B) 表示B中被A支配或等于的解的比例

## 完整配置示例

```java
// 创建配置
SPEA2Runner.SPEA2Config config = new SPEA2Runner.SPEA2Config();

// 基础参数
config.populationSize = 100;
config.archiveSize = 100;
config.maxGenerations = 200;
config.crossoverRate = 0.9;
config.mutationRate = 0.1;
config.maxRunTimeMinutes = 10.0;
config.seed = 12345L;
config.kNearest = 14;  // √(100+100) ≈ 14

// 优化目标
config.optimizeCmax = true;
config.optimizeEnergy = true;
config.optimizeTardiness = false;

// 局部搜索
config.enableLocalSearch = true;
config.localSearchInterval = 10;
config.localSearchL = 10;
config.localSearchEta = 0.10;

// 运行
SPEA2Runner.fullRun(problemFile, outputDir, config);
```

## 算法细节

### 1. 强度值计算

```
S(i) = |{j | j ∈ Pₜ + P̄ₜ ∧ i ≻ j}|
```
强度值 = 被该个体支配的个体数量

### 2. 原始适应度计算

```
R(i) = Σ S(j)  其中 j ≻ i
```
原始适应度 = 所有支配该个体的个体的强度值之和

对于非支配解，R(i) = 0

### 3. 密度计算

```
D(i) = 1 / (σᵢᵏ + 2)
```
其中 σᵢᵏ 是第i个个体到第k近邻的欧氏距离

### 4. 最终适应度

```
F(i) = R(i) + D(i)
```

适应度越小越好（最小化问题）

### 5. 环境选择

从 Pₜ ∪ P̄ₜ 中选择 N̄ 个个体：

- **如果非支配解数量 < N̄**：全部加入，用支配解填充
- **如果非支配解数量 = N̄**：全部加入
- **如果非支配解数量 > N̄**：使用截断算子

### 6. 存档截断

迭代移除最拥挤的个体：
1. 计算所有个体间的距离
2. 找到最小距离最小的个体（最拥挤）
3. 移除该个体
4. 重复直到剩余 N̄ 个个体

## 性能优化建议

### 1. 参数设置

- **小规模问题**（工件数 < 20）：
  - populationSize = 50
  - archiveSize = 50
  - maxGenerations = 100
  - kNearest = 10

- **中等规模问题**（工件数 20-50）：
  - populationSize = 100
  - archiveSize = 100
  - maxGenerations = 200
  - kNearest = 14

- **大规模问题**（工件数 > 50）：
  - populationSize = 200
  - archiveSize = 200
  - maxGenerations = 300-500
  - kNearest = 20

### 2. k近邻参数

推荐设置为：`k = √(populationSize + archiveSize)`

- 过小：密度估计不准确
- 过大：计算开销增加，多样性可能降低

### 3. 存档大小

- 通常设置为：`archiveSize = populationSize`
- 如果需要更多Pareto解：`archiveSize = 1.5 × populationSize`
- 如果计算资源有限：`archiveSize = 0.5 × populationSize`

## 输出结果

算法会输出：

1. **Pareto最优解集**：存储在返回的 `List<MOIndividual>` 中
2. **控制台输出**：每10代输出进度信息
3. **CSV文件**（如果使用Runner）：
   - `SPEA2_pareto_front.csv`：Pareto前沿数据

CSV格式：
```csv
Solution,Cmax,Energy,PackingQ,BatchCount
1,120.50,4500.00,0.8750,5
2,115.30,4800.00,0.8650,6
...
```

## 注意事项

1. **计算复杂度**：SPEA2的计算复杂度高于NSGA-II，尤其是密度计算和存档截断部分
2. **参数敏感性**：k近邻参数对算法性能有较大影响，建议根据问题规模调整
3. **存档大小**：存档大小直接影响Pareto前沿的质量和计算时间
4. **随机种子**：为了可重现的结果，建议设置固定的随机种子

## 参考文献

Zitzler, E., Laumanns, M., & Thiele, L. (2001). SPEA2: Improving the strength Pareto evolutionary algorithm. TIK-report, 103.

## 联系方式

如有问题，请联系开发团队。
