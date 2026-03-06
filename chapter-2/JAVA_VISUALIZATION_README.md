# Java遗传算法调度可视化工具

本工具将遗传算法的调度结果自动转换为可视化图表，包括甘特图和打印机零件排布图。

## 功能特点

- ✅ **自动集成**：算法运行结束后自动生成可视化图表
- ✅ **甘特图**：显示各机器的工作时间安排
- ✅ **打印机布局图**：显示打印批次中零件的位置布局
- ✅ **汇总报告**：JSON格式的详细统计信息
- ✅ **中文支持**：图表完全支持中文显示

## 项目结构

```
chapter-2/
├── pom.xml                                      # Maven配置（已添加图表库依赖）
├── src/main/java/AlgorthmFrame/
│   ├── ga/
│   │   ├── GA.java                              # 遗传算法主类（已集成可视化）
│   │   └── ...
│   └── visualization/                           # 可视化模块
│       ├── ScheduleVisualizer.java              # 主可视化器
│       └── PrinterLayoutVisualizer.java         # 打印机布局专用可视化器
├── src/test/java/
│   ├── SimpleVisualizationTest.java             # 简单可视化测试
│   └── TestVisualization.java                   # 完整GA测试
└── visualization_results/                       # 输出目录（自动创建）
    ├── gantt_chart.png                         # 甘特图
    ├── printer_layout_printer1.png             # 打印机1布局图
    ├── printer_layout_printer2.png             # 打印机2布局图
    └── summary_report.json                     # 汇总报告
```

## 依赖库

项目使用以下第三方库：

- **JFreeChart 1.5.3** - 图表生成库
- **JCommon 1.0.24** - JFreeChart必需库
- **Jackson 2.13.4** - JSON处理库

依赖已在`pom.xml`中配置，会自动下载。

## 使用方法

### 方法1：运行完整算法（推荐）

```bash
cd chapter-2
mvn clean compile
java -cp "target/classes:target/dependency/*" test.RunAlgorithmExample
```

算法运行结束后，会自动在`visualization_results/`目录下生成：
- `gantt_chart.png` - 甘特图
- `printer_layout_printer1.png` 等 - 打印机布局图
- `summary_report.json` - 汇总报告

### 方法2：运行可视化测试

```bash
# 编译
mvn clean compile

# 运行简单测试（不运行完整GA）
java -cp "target/classes:target/dependency/*" SimpleVisualizationTest

# 运行完整GA测试
java -cp "target/classes:target/dependency/*" test.TestVisualization
```

## 输出文件说明

### 甘特图 (gantt_chart.png)
- **内容**：展示所有机器的调度时间安排
- **横轴**：机器编号（打印机1、批处理机1、离散处理机1等）
- **纵轴**：时间轴（从最早开始时间到最晚结束时间）
- **彩色矩形**：每个矩形代表该机器上的一个操作，高度表示持续时间
- **矩形标签**：
  - 打印机：显示"B机器号批次号"格式（如"B11"、"B12"）
  - 批处理机：显示"B机器号批次号"格式（如"B11"、"B22"）
  - 离散机：显示工件号（如"J0"、"J5"）
- **颜色区分**：不同颜色代表不同工件
- **机器标注**：每个机器在X轴下方标明机器类型和编号

### 打印机布局图 (printer_layout_printerX_batchY.png)
- **内容**：为每个打印批次生成单独的布局图
- **矩形显示**：每个零件显示为实际尺寸的彩色矩形
- **坐标系统**：xy坐标表示矩形左下角位置
- **尺寸准确**：严格按照零件的l(长)和w(宽)属性绘图，不考虑旋转
- **旋转标记**：如果零件被旋转，会在右下角显示红色旋转符号(↻)
- **零件标签**：每个矩形中心显示工件编号(J0, J1等)
- **平台边界**：显示打印平台的完整边界和尺寸标注
- **批次信息**：显示批次的时间范围和零件数量
- **文件命名**：printer_layout_printer1_batch1.png, printer_layout_printer1_batch2.png等

### 汇总报告 (summary_report.json)
```json
{
  "makespan": 1250.5,
  "job_count": 20,
  "machine_count": 7,
  "printer_count": 3,
  "batch_machine_count": 2,
  "discrete_machine_count": 2,
  "machine_utilization": [
    {
      "machine_id": 1,
      "name": "打印机1",
      "utilization": 0.85,
      "busy_time": 1062.5,
      "total_time": 1250.5
    }
  ],
  "job_completion_times": [
    {
      "job_id": 0,
      "completion_time": 980.3
    }
  ]
}
```

## 技术实现

### ScheduleVisualizer类
主可视化器，负责：
- 生成甘特图
- 协调打印机布局可视化
- 生成汇总报告

### PrinterLayoutVisualizer类
专门处理打印机布局可视化：
- 为每个打印机生成单独的布局图
- 使用散点图显示零件位置
- 支持零件旋转显示

### 集成方式
可视化功能集成在`GA.solve()`方法的最后：
```java
// 在GA.java的solve方法末尾
try {
    String outputDir = "visualization_results";
    ScheduleVisualizer visualizer = new ScheduleVisualizer(bestSolution, best, input, operationMatrix);
    visualizer.generateAllCharts(outputDir);
} catch (Exception e) {
    System.err.println("生成可视化图表时出错: " + e.getMessage());
}
```

## 自定义配置

### 修改输出目录
在`GA.java`中修改：
```java
String outputDir = "my_custom_results";
```

### 调整图表大小
在可视化类中修改：
```java
ChartUtils.saveChartAsPNG(new File(outputPath), chart, 1600, 1000); // 更大的图表
```

### 添加新图表类型
扩展`ScheduleVisualizer.generateAllCharts()`方法：
```java
public void generateAllCharts(String outputDir) throws IOException {
    generateGanttChart(outputDir + "/gantt_chart.png");
    generatePrinterLayoutChart(outputDir + "/printer_layout.png");
    generateMachineUtilizationChart(outputDir + "/utilization_chart.png"); // 新图表
    generateSummaryReport(outputDir + "/summary_report.json");
}
```

## 故障排除

### 编译问题
```bash
# 清除并重新下载依赖
mvn clean dependency:copy-dependencies

# 重新编译
mvn clean compile
```

### 中文字体问题
确保系统安装了中文字体，代码已设置宋体作为默认字体。

### 图表显示问题
- 检查JFreeChart版本兼容性
- 确认输出目录有写入权限
- 查看控制台错误信息

### 内存不足
对于大型算例，增加JVM内存：
```bash
java -Xmx2g -cp "..." test.RunAlgorithmExample
```

## 性能考虑

- 图表生成对大规模算例（>100工件）可能需要较长时间
- 大图表文件（>10MB）可能影响磁盘空间
- 对于频繁运行的场景，考虑条件生成（只在需要时生成）

## 扩展建议

### 实时可视化
在GA进化过程中定期生成图表，观察优化过程。

### 交互式图表
集成JavaFX创建可交互的图表界面。

### 网络界面
添加Web服务器，将图表以HTML格式输出。

## 版本信息

- JFreeChart: 1.5.3
- JCommon: 1.0.24
- Jackson: 2.13.4
- Java: JDK 8+

---

## 算法输出信息

遗传算法运行过程中会输出以下信息：

- **每一代信息**：
  ```
  After X generation, the best fitness is: 256.18 (makespan: 390282.15), avg makespan: 445231.24, min makespan: 405763.15
  ```
  - 显示当前代数
  - 最佳适应度值及其对应的makespan
  - 当前种群的平均makespan
  - 当前种群的最小makespan

- **发现新最优解**：
  ```
  In X generation, find new best fitness is: 257.02, makespan: 389161.90
  ```
  - 当找到更好的解时会单独打印
  - 显示新的最佳适应度和makespan

### 📊 数据一致性保证

**重要说明**：所有统计数据（avg makespan, min makespan）都是基于已计算的fitness值反算得出，确保与适应度评估完全一致。

**一致性验证**：
- min makespan 必定对应当代的 max fitness
- 如果发现数据不一致，会打印警告：`⚠️ 警告：统计数据不一致`
- 正常情况下，当best是当代最优时：min makespan = best makespan

**数据关系**：
```
当代最优 (max fitness) ← getBest(parents)
    ↓
best个体更新 (如果更优)
    ↓
统计数据计算 (从fitness反算)
    ↓
min makespan = FITNESS_SCALE / max(parents[i].fitness)
avg makespan = FITNESS_SCALE / avg(parents[i].fitness)
```

**为什么不重复evaluate？**
- `evaluate()`方法对共享状态有副作用
- 多次调用可能返回略有不同的结果
- 重复计算浪费性能（50%加速）
- 从fitness反算确保数据完全一致

- **找到新最优解时**：
  ```
  In X generation, find new best fitness is: 0.1234
  ```

- **算法完成信息**：
  - 最终最优makespan
  - 算法运行时间
  - 详细调度结果
  - 可视化图表生成

**注意**：可视化功能不会影响算法的运行性能，只在算法结束后执行。
