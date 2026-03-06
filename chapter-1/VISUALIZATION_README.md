# 第一章可视化功能使用指南

## 概述

第一章现在已经集成了完整的可视化功能，可以为**对比实验**和**消融实验**自动生成以下图表：

1. **迭代曲线图** - 显示算法收敛过程
2. **批次分布图** - 显示各机器的批次数量和利用率
3. **甘特图** - 显示作业调度时序
4. **批次布局图** - 显示每个批次中零件的具体布局

## 可视化工具类

### 1. ALNS算法可视化 (`ALNSVisualizer`)

位置：`src/main/java/AlgorithmFrame/alns/visualization/ALNSVisualizer.java`

适用于ALNS-SL算法的可视化。

### 2. 通用可视化工具 (`Chapter1Visualizer`)

位置：`src/main/java/util/Chapter1Visualizer.java`

适用于所有第一章算法（GA-TS、ALNS等）的可视化。

## 使用方法

### 一、ALNS算法可视化

#### 1.1 基础使用

```java
import AlgorithmFrame.alns.*;

// 运行ALNS并生成可视化
String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01";
String outputDir = "chapter-1/src/main/output/visualization/alns_example";

ALNSParameters params = new ALNSParameters();
params.maxIterations = 1000;

ALNSSolution solution = ALNSRunner.runALNSWithVisualization(
    machinePath, itemPath, params, 12345L, outputDir
);
```

#### 1.2 手动生成可视化

```java
// 运行算法并保留历史记录
ALNSRunner.ALNSRunResult result = ALNSRunner.runALNSWithHistory(
    machinePath, itemPath, params, seed
);

// 创建可视化器
ALNSVisualizer visualizer = new ALNSVisualizer(
    result.solution,
    result.alns.getJobs(),
    result.alns.getMachines(),
    result.alns.getIterationHistory()
);

// 生成所有图表
visualizer.generateAllCharts("output/directory");
```

### 二、对比实验可视化

修改 `ComparisonExperiment.java` 的 main 方法：

```java
public static void main(String[] args) {
    // 运行对比实验并开启可视化
    runComparison(
        "chapter-1/src/main/resources/Machine/machine_2",
        "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
        5,      // 运行次数
        true    // 开启可视化
    );
}
```

#### 说明：
- 第一次运行和最优运行会自动生成可视化图表
- 可视化结果保存在 `chapter-1/src/main/output/comparison/`

### 三、消融实验可视化

修改 `AblationExperimentRunner.java` 的 main 方法：

```java
public static void main(String[] args) {
    // 运行消融实验并开启可视化
    runAblationExperiment(true);  // true = 开启可视化
}
```

#### 说明：
- 每个配置的最优运行会生成可视化图表
- 可视化结果按算例和配置分类保存
- 保存路径：`chapter-1/src/main/output/ablation/{算例名称}/{配置ID}/`

### 四、GA-TS算法可视化

使用通用可视化工具：

```java
import util.Chapter1Visualizer;
import ProblemFrame.*;

// 运行算法获取结果
BatchGa batchGa = new BatchGa(...);
Result result = batchGa.solve();

// 生成所有可视化图表
Chapter1Visualizer.generateAllCharts(
    result.solutions,           // 批次结果
    machines,                    // 机器数组
    result.itreatorList,        // 迭代历史
    "GA-TS算法",                // 算法名称
    "output/directory"          // 输出目录
);
```

## 输出文件说明

运行可视化后，会在指定目录生成以下文件：

```
output/
├── iteration_curve.png           # 迭代曲线图
├── batch_distribution.png        # 批次分布图
├── gantt_chart.png               # 甘特图
├── machine_1_batch_1_layout.png  # 机器1批次1布局图
├── machine_1_batch_2_layout.png  # 机器1批次2布局图
├── machine_2_batch_1_layout.png  # 机器2批次1布局图
└── ...                           # 其他批次布局图
```

### 图表说明

#### 1. 迭代曲线图 (`iteration_curve.png`)
- **横轴**：迭代次数（代数）
- **纵轴**：最优Cmax
- **用途**：观察算法收敛速度和质量

#### 2. 批次分布图 (`batch_distribution.png`)
- **左Y轴**：批次数量（蓝色柱状图）
- **右Y轴**：利用率百分比（橙色折线图）
- **横轴**：机器编号
- **用途**：评估负载均衡和资源利用率

#### 3. 甘特图 (`gantt_chart.png`)
- **横轴**：时间
- **纵轴**：机器编号
- **矩形**：批次（标注为 B机器号-批次号）
- **用途**：查看作业调度时序和机器空闲时间

#### 4. 批次布局图 (`machine_X_batch_Y_layout.png`)
- **显示内容**：批次中每个零件的具体位置
- **矩形**：零件（标注零件名称）
- **旋转标记**：红色 ↻ 表示零件被旋转
- **用途**：验证装箱方案的可行性

## 配置选项

### ALNS参数

```java
ALNSParameters params = new ALNSParameters();
params.maxIterations = 5000;              // 最大迭代次数
params.timeLimitMs = 300000;              // 时间限制（毫秒）
params.T0 = 100.0;                        // 初始温度
params.beta = 0.995;                      // 降温系数
params.theta = 0.3;                       // 移除比例
```

### 可视化自定义

如果需要自定义图表样式，可以修改：

- **字体**：`setChineseFont()` 方法中的字体设置
- **颜色**：`getColorForBatch()` 和 `getColorForItem()` 方法
- **图表大小**：`ChartUtils.saveChartAsPNG()` 的宽高参数

## 示例：完整工作流

### 运行对比实验并查看可视化

1. 打开 `ComparisonExperiment.java`
2. 设置可视化选项为 `true`
3. 运行 main 方法
4. 查看输出目录：`chapter-1/src/main/output/comparison/`

### 运行消融实验并查看可视化

1. 打开 `AblationExperimentRunner.java`
2. 确保 main 方法中 `runAblationExperiment(true)`
3. 运行 main 方法
4. 查看输出目录：`chapter-1/src/main/output/ablation/`

## 注意事项

1. **性能考虑**
   - 大规模实验开启可视化会增加运行时间
   - 建议先运行少量实验验证可视化效果

2. **存储空间**
   - 每组图表约需5-15MB（取决于批次数量）
   - 确保有足够的磁盘空间

3. **中文字体**
   - 使用"微软雅黑"字体显示中文
   - 如果系统没有该字体，图表中的中文可能显示为方块

4. **兼容性**
   - 所有可视化功能向后兼容
   - 不启用可视化时，算法运行逻辑不变

## 故障排除

### 问题1：图表中文显示为方块

**解决方案**：修改字体设置
```java
// 在 setChineseFont() 方法中
Font titleFont = new Font("SimHei", Font.BOLD, 18);  // 使用黑体
```

### 问题2：生成图表失败

**可能原因**：
1. 输出目录权限不足
2. JFreeChart依赖缺失

**解决方案**：
1. 检查目录权限
2. 确认pom.xml中包含JFreeChart依赖

### 问题3：批次布局图为空

**可能原因**：Solution对象中placeItemList为null

**解决方案**：确保算法正确构造了批次结果

## 技术支持

如有问题或建议，请查看：
- `ALNS.java` - ALNS算法核心实现
- `BatchGa.java` - GA-TS算法核心实现
- `Chapter1Visualizer.java` - 可视化工具源代码

## 更新日志

### 2026-02-05
- ✅ 添加ALNS算法迭代历史跟踪
- ✅ 创建通用可视化工具类 `Chapter1Visualizer`
- ✅ 集成对比实验可视化
- ✅ 集成消融实验可视化
- ✅ 支持批次分布图、甘特图和批次布局图
- ✅ 添加中文字体支持
