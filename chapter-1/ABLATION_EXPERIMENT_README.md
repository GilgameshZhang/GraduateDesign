# 第一章GA-TS算法消融实验说明

## 实验目的

验证第一章GA-TS算法中三个关键组件对求解性能的贡献：
1. **初始化策略**（启发式初始化 vs 全随机初始化）
2. **禁忌搜索**（完整TabuSearch vs 不用搜索）
3. **Skyline启发式规则**（12级复杂规则 vs 简化规则）

## 实验配置

共5个消融配置：

| 配置ID | 配置名称 | 初始化 | 禁忌搜索 | Skyline规则 | 说明 |
|--------|---------|--------|---------|------------|------|
| V0-Full | 完整算法 | ✓ 启发式+随机 | ✓ 完整TS | ✓ 复杂规则(0-12分) | 基线 |
| V1-NoHeuristic | 无启发式初始化 | ✗ 全随机 | ✓ 完整TS | ✓ 复杂规则 | 测试初始化 |
| V2-NoTabu | 无禁忌搜索 | ✓ 启发式+随机 | ✗ 不用搜索 | ✓ 复杂规则 | 测试禁忌搜索 |
| V3-SimpleHeuristic | 简化启发式 | ✓ 启发式+随机 | ✓ 完整TS | ✗ 简化规则(能放=1) | 测试启发式规则 |
| V4-Minimal | 最小化版本 | ✗ 全随机 | ✗ 不用搜索 | ✗ 简化规则 | 所有组件移除 |

## 测试算例

- **Small-2M10J**: 2机器10作业
- **Medium-3M30J**: 3机器30作业
- **Large-4M50J**: 4机器50作业

每个配置在每个算例上运行10次，总计：**5配置 × 3算例 × 10次 = 150次运行**

## 算法参数

```
GA参数:
- MAX_GEN = 500
- popSize = 100
- crossoverRate = 0.8
- mutationRate = 0.2
- cloneNumOfBestIndividual = 2

TS参数:
- decodeMaxGen = 100
- decodeTabuSize = 10
- decodeMaxN = 30
```

## 如何运行

### 方法1：直接运行主类

```bash
cd chapter-1
javac -cp src/main/java src/test/java/AblationExperimentRunner.java
java -cp "src/main/java:src/test/java" AblationExperimentRunner
```

### 方法2：使用IDE运行

1. 打开 `chapter-1/src/test/java/AblationExperimentRunner.java`
2. 运行 `main` 方法

### 方法3：使用Maven（如果配置了）

```bash
cd chapter-1
mvn test -Dtest=AblationExperimentRunner
```

## 输出文件

实验结果将保存在 `chapter-1/src/main/output/ablation/` 目录下：

1. **ablation_detailed_results.csv** - 详细结果（每次运行的具体数据）
   - 列：ConfigID, ConfigName, Instance, RunID, Cmax, AvgUtilization, TimeMs

2. **ablation_summary.csv** - 汇总统计
   - 列：ConfigID, ConfigName, Instance, AvgCmax, MinCmax, MaxCmax, StdDev, AvgUtilization, AvgTimeMs, RPD(%)

3. **ablation_report.txt** - 文字报告（易读的实验报告）

## 评价指标

- **Cmax**: 最大完工时间（越小越好）
- **平均利用率**: 材料利用率（越大越好）
- **标准差**: 解的稳定性（越小越稳定）
- **RPD (相对性能损失)**: `RPD = (当前配置平均Cmax - V0-Full平均Cmax) / V0-Full平均Cmax × 100%`
  - RPD越大，说明移除该组件后性能下降越明显，组件贡献越大

## 技术实现

### 新增文件

1. **AblationExperimentRunner.java** - 消融实验主程序
2. **BatchGaAblation.java** - 支持消融实验的BatchGa（可控制初始化策略）
3. **BatchGenomeAblation.java** - 支持消融实验的BatchGenome（可控制是否使用TabuSearch）
4. **SkyLinePackingAblation.java** - 支持消融实验的SkyLinePacking（可控制评分规则）

### 配置机制

通过Java系统属性传递配置：
- `ablation.useHeuristicInit` - 是否使用启发式初始化
- `ablation.useTabuSearch` - 是否使用禁忌搜索
- `ablation.useComplexScore` - 是否使用复杂评分规则

## 预期运行时间

根据算例规模和机器性能：
- 小规模算例：约5-10分钟/配置
- 中等规模算例：约15-30分钟/配置
- 大规模算例：约30-60分钟/配置

**总运行时间预计：2-5小时**

## 注意事项

1. 确保有足够的磁盘空间存储结果文件
2. 运行过程中会在控制台输出进度信息
3. 如果中断，可以修改代码跳过已完成的配置
4. 建议在性能较好的机器上运行

## 结果分析

完成后，查看 `ablation_report.txt` 文件，重点关注：
1. 各配置的RPD值 - 判断组件重要性
2. 平均Cmax和标准差 - 判断性能和稳定性
3. 不同规模算例的趋势 - 判断组件在不同规模下的作用

## 故障排除

### 问题1：编译错误
- 检查是否所有依赖的类都在classpath中
- 确保util包和ProblemFrame包可访问

### 问题2：内存不足
- 增加JVM堆内存：`java -Xmx4g -cp ...`

### 问题3：结果文件无法写入
- 检查输出目录权限
- 手动创建 `chapter-1/src/main/output/ablation/` 目录

## 联系方式

如有问题，请联系开发者。
