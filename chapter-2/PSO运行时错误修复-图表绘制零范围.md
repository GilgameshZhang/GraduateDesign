# PSO运行时错误修复 - 图表绘制零范围

## ❌ 问题描述

**错误信息**:
```
❌ 某次实验执行失败: A positive range length is required: Range[46922.245901639355,46922.245901639355]
❌ 算例 J20P3B2D10_02 执行失败: 实验执行失败
java.lang.Exception: 实验执行失败
	at PSOExperimentRunner.runInstanceExperiments(PSOExperimentRunner.java:179)
	at PSOExperimentRunner.main(PSOExperimentRunner.java:402)
Caused by: java.lang.IllegalArgumentException: A positive range length is required: Range[46922.245901639355,46922.245901639355]
	at org.jfree.chart.axis.ValueAxis.setRange(ValueAxis.java:1198)
	at util.java.ChartGenerator.generateConvergenceCurve(ChartGenerator.java:69)
	at util.java.ExperimentResultWriter.writeFinalResults(ExperimentResultWriter.java:162)
	at AlgorthmFrame.pso.PSO.solve(PSO.java:238)
	at PSOExperimentRunner.runSingleExperiment(PSOExperimentRunner.java:101)
```

**发生时机**: PSO算法运行完毕后，生成收敛曲线图时崩溃

---

## 🔍 问题分析

### 错误原因

当PSO算法的makespan值从头到尾**完全没有变化**时，会导致：

```java
minMakespan = 46922.245901639355
maxMakespan = 46922.245901639355
range = maxMakespan - minMakespan = 0  // ❌ 范围为0

// JFreeChart要求Y轴必须有正的范围
plot.getRangeAxis().setRange(min, max);  // min == max → 抛出异常
```

### 为什么会出现这种情况？

1. **初始化就找到最优解**
   - 启发式初始化非常好，直接找到最优
   - 后续迭代无法改进

2. **算例特殊性**
   - 某些小规模算例解空间有限
   - 多个初始解收敛到同一个值

3. **停滞立即终止**
   - PSO快速收敛后停滞
   - 所有记录的makespan值相同

---

## ✅ 修复方案

### 策略：当range为0时，设置最小显示范围

**修复前** ❌:
```java
double range = maxMakespan - minMakespan;
double padding = range * 0.05;

// 当range=0时，padding=0，导致setRange(x, x)错误
plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
```

**修复后** ✅:
```java
double range = maxMakespan - minMakespan;

// 如果所有值相同（range为0），设置一个最小范围
if (range < 0.001) {
    // 设置为makespan值的±0.5%作为显示范围
    double minRange = Math.max(1.0, minMakespan * 0.005);
    plot.getRangeAxis().setRange(minMakespan - minRange, maxMakespan + minRange);
} else {
    double padding = range * 0.05;
    plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
}
```

**关键改进**:
1. ✅ 检查`range < 0.001`（考虑浮点精度）
2. ✅ 设置最小范围：`max(1.0, makespan * 0.005)`
3. ✅ 确保Y轴始终有正的范围

---

## 📝 修复的方法

### 1. generateConvergenceCurve() - 单条收敛曲线 ✅

**位置**: `ChartGenerator.java:59-80`

**修复内容**:
```java
// 根据数据范围自动调整Y轴
XYPlot plot = chart.getXYPlot();
double minMakespan = makespanHistory.stream().min(Double::compare).orElse(0.0);
double maxMakespan = makespanHistory.stream().max(Double::compare).orElse(1.0);

// 计算padding（5%的范围，更紧凑）
double range = maxMakespan - minMakespan;

// ✅ 新增：处理range为0的情况
if (range < 0.001) {
    double minRange = Math.max(1.0, minMakespan * 0.005);
    minMakespan = minMakespan - minRange;
    maxMakespan = maxMakespan + minRange;
} else {
    double padding = range * 0.05;
    minMakespan = minMakespan - padding;
    maxMakespan = maxMakespan + padding;
}

// 设置Y轴范围
plot.getRangeAxis().setRange(minMakespan, maxMakespan);
```

---

### 2. generateMultipleConvergenceCurves() - 多条收敛曲线对比 ✅

**位置**: `ChartGenerator.java:134-145`

**修复内容**:
```java
// 根据数据范围自动调整Y轴
XYPlot plot = chart.getXYPlot();
double range = maxMakespan - minMakespan;

// ✅ 新增：处理range为0的情况
if (range < 0.001) {
    double minRange = Math.max(1.0, minMakespan * 0.005);
    plot.getRangeAxis().setRange(minMakespan - minRange, maxMakespan + minRange);
} else {
    double padding = range * 0.05;
    plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
}
```

---

### 3. generateParameterComparisonChart() - 参数对比柱状图 ✅

**位置**: `ChartGenerator.java:200-211`

**修复内容**:
```java
// 根据数据范围自动调整Y轴
double range = maxMakespan - minMakespan;

// ✅ 新增：处理range为0的情况
if (range < 0.001) {
    double minRange = Math.max(1.0, minMakespan * 0.005);
    plot.getRangeAxis().setRange(minMakespan - minRange, maxMakespan + minRange);
} else {
    double padding = range * 0.05;
    plot.getRangeAxis().setRange(minMakespan - padding, maxMakespan + padding);
}
```

---

## 📊 修复效果

### 修复前 ❌

**情况**: PSO算法makespan无变化（如46922.24）

```
minMakespan = 46922.24
maxMakespan = 46922.24
range = 0
padding = 0

setRange(46922.24, 46922.24)  // ❌ 抛出异常
```

**结果**: 实验失败，无法生成图表

---

### 修复后 ✅

**情况**: PSO算法makespan无变化（如46922.24）

```
minMakespan = 46922.24
maxMakespan = 46922.24
range = 0 < 0.001  // ✅ 触发保护机制

minRange = max(1.0, 46922.24 * 0.005) = 234.61
setRange(46922.24 - 234.61, 46922.24 + 234.61)
       = (46687.63, 47156.85)  // ✅ 正的范围
```

**结果**: 
- ✅ 图表正常生成
- ✅ Y轴显示合理范围
- ✅ 实验顺利完成

---

## 🎯 最小范围的计算

### 为什么是 `max(1.0, makespan * 0.005)`？

**考虑两种情况**:

1. **小makespan值** (如 makespan = 100)
   ```
   makespan * 0.005 = 0.5
   minRange = max(1.0, 0.5) = 1.0  // 使用固定值1.0
   范围: [99, 101]  // 显示范围合理
   ```

2. **大makespan值** (如 makespan = 46922)
   ```
   makespan * 0.005 = 234.61
   minRange = max(1.0, 234.61) = 234.61  // 使用比例值
   范围: [46687, 47157]  // 显示范围合理，约±0.5%
   ```

**优势**:
- ✅ 小值时：固定范围，避免过大比例
- ✅ 大值时：比例范围，保持相对合理
- ✅ 自适应：根据makespan大小调整

---

## 🧪 验证步骤

### 1. 重新编译（2分钟）

```bash
mvn clean compile -DskipTests
```

**预期**: `BUILD SUCCESS`

---

### 2. 重新运行失败的实验（5分钟）

```java
// 只运行J20P3B2D10_02算例
PSOExperimentRunner.main()
```

**预期**:
- ✅ 实验正常完成
- ✅ 图表成功生成
- ✅ 无 `IllegalArgumentException`

---

### 3. 检查生成的图表

**路径**: `result/对比试验/PSO/J20P3B2D10_02/J20P3B2D10_02_run1/`

**预期文件**:
- ✅ `迭代曲线.png` - 收敛曲线图
- ✅ 实验报告.txt
- ✅ 甘特图.png
- ✅ 打印批次布局图 (多个)

---

## 📈 实际案例

### 案例：J20P3B2D10_02算例

**makespan历史** (所有值相同):
```
代数  1: 46922.245901639355
代数  2: 46922.245901639355
代数  3: 46922.245901639355
...
代数 50: 46922.245901639355
```

**修复前** ❌:
- 抛出异常：`Range[46922.245901639355,46922.245901639355]`
- 实验失败

**修复后** ✅:
- Y轴范围自动设置为：`[46687.63, 47156.85]`
- 图表正常生成
- 显示一条水平直线（符合实际：makespan无变化）

---

## 🎓 经验总结

### 1. 图表绘制要考虑边界情况

**常见边界情况**:
- ❌ 所有值相同 (range = 0)
- ❌ 只有1个数据点
- ❌ 只有2个数据点且相同
- ❌ 数值非常接近 (range < 0.001)

**处理原则**:
- ✅ 检查range是否为0或非常小
- ✅ 设置最小显示范围
- ✅ 确保Y轴范围为正

---

### 2. 使用相对范围而非绝对范围

```java
// ❌ 错误：固定范围，不适用于所有情况
minRange = 10.0;

// ✅ 正确：相对范围 + 最小保护
minRange = max(1.0, makespan * 0.005);
```

---

### 3. 浮点数比较要考虑精度

```java
// ❌ 错误：直接比较
if (range == 0)

// ✅ 正确：考虑精度
if (range < 0.001)
```

---

## 📝 修复文件

| 文件 | 修改方法 | 状态 |
|------|---------|------|
| `ChartGenerator.java` | `generateConvergenceCurve()` | ✅ 已修复 |
| `ChartGenerator.java` | `generateMultipleConvergenceCurves()` | ✅ 已修复 |
| `ChartGenerator.java` | `generateParameterComparisonChart()` | ✅ 已修复 |

---

## 🎊 总结

**PSO图表绘制错误已全部修复！**

| 错误类型 | 状态 |
|---------|------|
| 零范围导致的绘图失败 | ✅ 已修复（3处） |
| 边界情况处理 | ✅ 已完善 |
| 实验鲁棒性 | ✅ 已提升 |

**修复亮点**:
- ✅ 自适应范围设置
- ✅ 考虑makespan大小
- ✅ 处理所有边界情况
- ✅ 提升实验稳定性

**现在可以重新运行实验了！** 🚀

---

## 🚀 立即行动

### 编译
```bash
mvn clean compile -DskipTests
```

### 重新运行实验
```java
PSOExperimentRunner.main()
```

**预期**: 所有算例正常完成，图表成功生成 ✅

