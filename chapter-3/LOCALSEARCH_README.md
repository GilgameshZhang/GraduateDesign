# NSGA-II 局部搜索集成说明

## 概述

本模块实现了基于分位排名的Memetic局部搜索，集成到NSGA-II多目标遗传算法中。

## 设计特点

### 1. 分位分类（Percentile-based Classification）

- 将精英个体分为两类：
  - **TIME_DEFICIENT (T)**: 时间不足型 - Cmax分位排名较高
  - **ENERGY_DEFICIENT (E)**: 能耗不足型 - Energy分位排名较高

- 分类依据：`delta = pc - pe`
  - `pc`: Cmax分位排名（0..1）
  - `pe`: Energy分位排名（0..1）
  - `delta >= 0` → TIME_DEFICIENT
  - `delta < 0` → ENERGY_DEFICIENT

### 2. 算子池

#### 时间向算子（降低Cmax）
- **T1-SimpleSwap**: 简单交换算子 - 随机交换打印工序段中的两个位置
- **T3-SimpleInsert**: 简单插入算子 - 随机选择一个离散工序插入到另一个位置

#### 能耗向算子（降低Energy）
- **E2-SimpleMachineReassign**: 简单换机算子 - 随机选择一个离散工序换到另一台机器

### 3. 接受准则

#### Pareto支配优先
- 如果邻域解支配当前解，则接受

#### ε-容忍（按类型）
- **T型（时间向）**：
  - 要求：`Cmax下降 >= 0.5%`
  - 允许：`Energy上升 <= 5%`

- **E型（能耗向）**：
  - 要求：`Energy下降 >= 1%`
  - 允许：`Cmax上升 <= 2%`

### 4. 自适应算子选择

- 基于滑动窗口（50次）的成功率统计
- 采样概率：`p(op) ∝ 0.1 + successRate(op)`

## 使用方法

### 基本用法

```java
// 1. 创建NSGA-II实例
NSGAII nsgaii = new NSGAII(problem, Arrays.asList(
    new MOEvaluator.MaximumCompletionTime(),
    new MOEvaluator.TotalEnergyConsumption(problem)
));

// 2. 配置基本参数
nsgaii.setPopulationSize(100);
nsgaii.setMaxGenerations(500);
nsgaii.setMaxRunTimeMinutes(5.0);

// 3. 启用局部搜索（每10代执行一次）
nsgaii.enableLocalSearch(true, 10);

// 4. 运行算法
nsgaii.solve();
```

### 高级配置

```java
// 启用局部搜索并自定义参数
nsgaii.enableLocalSearch(true, 5);  // 每5代执行一次

// 配置局部搜索参数
nsgaii.setLocalSearchParameters(
    15,      // L: 每个精英15次尝试
    0.15,    // eta: 选择15%精英
    0.08,    // epsE: T型允许能耗上升8%
    0.03,    // epsC: E型允许Cmax上升3%
    0.01,    // improvC: T型要求Cmax下降1%
    0.015    // improvE: E型要求能耗下降1.5%
);
```

## 核心组件

### 1. 数据结构

- **IndividualType**: 个体类型枚举（TIME_DEFICIENT / ENERGY_DEFICIENT）
- **Delta**: 快照类，用于增量解码和回滚
- **Candidate**: 候选解类，封装邻域解及其Delta
- **MachineEnergySnapshot**: 机器能耗快照

### 2. 算子系统

- **Operator**: 算子接口
- **AbstractOperator**: 算子抽象基类（提供成功率统计）
- **SimpleSwapOperator**: T1简单交换算子
- **SimpleInsertOperator**: T3简单插入算子
- **SimpleMachineReassignOperator**: E2简单换机算子

### 3. 核心引擎

- **PercentileClassifier**: 分位分类器
- **OperatorSelector**: 算子选择器（基于自适应成功率）
- **LocalSearchEngine**: 局部搜索引擎（主控制器）

### 4. 集成到NSGA-II

```
NSGA-II主流程：
1. 初始化种群
2. 评价种群
3. while 未达到终止条件:
   a. 生成子代（交叉+变异）
   b. 评价子代
   c. 环境选择（P ∪ Q → P'）
   d. [每N代] 局部搜索精英
   e. 记录统计
4. 返回Pareto前沿
```

## 默认参数

| 参数 | 默认值 | 说明 |
|------|--------|------|
| L | 10 | 每个精英的局部搜索次数 |
| eta | 0.10 | 精英选择比例（F1∪F2的前10%） |
| epsE | 0.05 | T型允许Energy上升比例（5%） |
| epsC | 0.02 | E型允许Cmax上升比例（2%） |
| improvC | 0.005 | T型要求Cmax下降比例（0.5%） |
| improvE | 0.01 | E型要求Energy下降比例（1%） |
| tau | 0.05 | 分位差值平局阈值 |
| interval | 10 | 局部搜索间隔（代数） |

## 测试

运行测试类验证功能：

```bash
# 在chapter-3目录下
mvn test -Dtest=LocalSearchNSGAIITest
```

或直接运行：

```bash
cd chapter-3
javac -cp "target/classes:target/test-classes" src/test/java/LocalSearchNSGAIITest.java
java -cp "target/classes:target/test-classes" LocalSearchNSGAIITest
```

## 输出示例

```
========================================
      NSGA-II 多目标遗传算法
========================================
问题: 20个工件
目标数: 2
目标函数: Cmax Energy(开关机) 
种群大小: 100
交叉率: 0.9
变异率: 0.1
最大代数: 500
最大时间: 5.0 分钟
========================================

...

代数   50 | Pareto前沿大小:  15 | 耗时: 12.34秒
  局部搜索: 精英分类: T=8 (53.3%), E=7 (46.7%)

...

========================================
           算法运行完成
========================================
总代数: 500
总耗时: 123.45 秒
Pareto前沿大小: 25
----------------------------------------
局部搜索统计: 尝试=500, 接受=125 (25.0%), Pareto=80, ε-容忍=45
=== 算子统计 ===
时间向算子:
  T1-SimpleSwap[T] - 成功率: 28.50% (57/200)
  T3-SimpleInsert[T] - 成功率: 22.00% (44/200)
能耗向算子:
  E2-SimpleMachineReassign[E] - 成功率: 24.00% (24/100)
========================================
```

## 扩展建议

### 1. 实现完整的算子集合

根据设计文档实现：
- **T2**: CriticalBatchAreaTransfer（关键批次面积导向跨机迁移）
- **T4**: DiscreteCriticalOpTimeReassign（离散关键工序最短时间换机）
- **E1**: PrintConsolidationMove（打印任务集中化）
- **E3**: PrintStartCountReductionReorder（减少启动次数的批次重排）

### 2. 增量解码

当前使用全量解码，可以实现增量解码以提高效率：
- 只重算受影响的打印机、后处理机、离散机器
- 只更新受影响工件的时间
- 局部更新能耗统计

### 3. 关键路径识别

实现关键路径回溯功能，用于：
- T1: 识别关键打印批次
- T2: 识别关键批次中的零件
- T3: 识别关键离散工序块
- T4: 识别关键离散工序

### 4. 能耗统计缓存

为Delta快照添加能耗统计缓存，支持快速回滚。

## 注意事项

1. **初始化顺序**：必须在调用`solve()`之前调用`enableLocalSearch()`
2. **性能影响**：启用局部搜索会增加约20-50%的运行时间
3. **参数调优**：建议根据具体问题调整epsilon和improvement参数
4. **算子扩展**：可以通过实现`Operator`接口添加自定义算子

## 参考文献

基于设计文档：
- `em-nsgaii_local_search_design_java.md`
- Memetic NSGA-II with Percentile-based Classification

## 版本历史

- v1.0 (2026-01-15): 初始版本
  - 实现基础框架
  - 实现3个简单算子（T1, T3, E2）
  - 集成到NSGA-II主流程
  - 添加测试和文档
