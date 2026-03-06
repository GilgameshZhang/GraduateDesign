# 第一章实验系统 - 完整输出总览

## 🎉 系统概述

第一章实验系统现已完全升级，所有实验类型**每次运行都自动生成**完整的实验报告和可视化图表，**完全匹配第二章的画风**。

## 📋 实验类型

### 1. 消融实验 (Ablation Experiment)

**文件**：`AblationExperimentRunner.java`

**目的**：验证算法各组件的贡献度

**配置**：5个配置 × 3个算例 × 10次运行 = 150次

**输出位置**：`chapter-1/src/main/output/ablation/`

**详细说明**：查看 `消融实验输出说明.md`

### 2. 单算法对比实验 (Single Algorithm Comparison)

**文件**：`ComparisonExperiment.java`

**目的**：测试GA-TS算法性能

**配置**：1个算法 × 多个算例 × 3-10次运行

**输出位置**：`chapter-1/src/main/output/ablation/`

**详细说明**：查看 `对比实验输出说明.md`

### 3. 多算法对比实验 (Multi-Algorithm Comparison)

**文件**：`MultiAlgorithmComparison.java`

**目的**：对比多个算法的性能

**配置**：4个算法 × 多个算例 × 3-10次运行

**算法**：GA-TS, GA-GA, GA-ACO, ALNS

**输出位置**：`chapter-1/src/main/output/ablation/`

**详细说明**：查看 `对比实验输出说明.md`

## 📊 统一输出格式

### 每次运行自动生成

所有实验类型，**每次运行**都生成：

```
实验文件夹_时间戳/
├── 实验报告.txt                    # 10-20 KB
├── 收敛曲线图.png                  # 1920×1080 高清
├── 甘特图.png                      # 1920×1080 高清
├── 批次布局图_machine1_batch1.png  # 1000×800
├── 批次布局图_machine1_batch2.png  # 1000×800
├── 批次布局图_machine2_batch1.png  # 1000×800
└── ... (更多批次图)
```

### 汇总文件

每个实验类型还生成汇总文件：

- `detailed_results.csv` - 所有运行的详细数据
- `statistics.csv` - 统计汇总
- `summary_report.txt` - 文字报告

## 🎨 图表风格

### 完全匹配第二章

| 特性 | 第二章 | 第一章 | 状态 |
|-----|-------|-------|------|
| 收敛曲线分辨率 | 1920×1080 | 1920×1080 | ✅ |
| 甘特图分辨率 | 1920×1080 | 1920×1080 | ✅ |
| 布局图分辨率 | 1000×800 | 1000×800 | ✅ |
| 字体系统 | 宋体 | 宋体 | ✅ |
| 线条颜色 | 蓝色 | 蓝色 | ✅ |
| 线条粗细 | 2.0f | 2.0f | ✅ |
| 甘特图颜色 | 16色鲜艳系 | 16色鲜艳系 | ✅ |
| 布局图颜色 | 10色浅色系 | 10色浅色系 | ✅ |

**详细说明**：查看 `图表风格说明.md`

## 🔧 核心工具类

### 1. ChartGenerator.java

**功能**：生成收敛曲线图

**特点**：
- 1920×1080高分辨率
- 蓝色线条，无数据点
- 宋体字体
- 10% Y轴padding

**代码位置**：`util/ChartGenerator.java`

### 2. BatchVisualization.java

**功能**：生成甘特图和批次布局图

**特点**：
- 甘特图：横轴时间，纵轴机器，1920×1080
- 布局图：每批次独立，1000×800
- 完全匹配第二章风格

**代码位置**：`util/BatchVisualization.java`

### 3. ExperimentResultWriter.java

**功能**：管理实验报告和图表生成

**特点**：
- 自动创建实验文件夹
- 写入详细报告
- 调用可视化工具
- 统一输出格式

**代码位置**：`util/ExperimentResultWriter.java`

## 📁 完整目录结构

```
chapter-1/
├── src/
│   ├── main/java/
│   │   ├── AlgorithmFrame/
│   │   │   └── machineChoice/ga/
│   │   │       ├── BatchGa.java               # ✅ 已修复
│   │   │       ├── BatchGaAblation.java       # ✅ 消融实验
│   │   │       └── BatchGenomeAblation.java   # ✅ 消融实验
│   │   └── util/
│   │       ├── ChartGenerator.java            # ✅ 图表生成
│   │       ├── BatchVisualization.java        # ✅ 批次可视化
│   │       └── ExperimentResultWriter.java    # ✅ 报告输出
│   ├── test/java/
│   │   ├── AblationExperimentRunner.java      # ✅ 消融实验
│   │   ├── QuickAblationTest.java             # ✅ 快速测试
│   │   ├── ComparisonExperiment.java          # ✅ 单算法对比
│   │   └── MultiAlgorithmComparison.java      # ✅ 多算法对比
│   └── main/output/
│       └── ablation/                          # 所有实验输出到这里
│           ├── V0-Full_*_run*_*/              # 消融实验输出
│           ├── GA-TS_*_run*_*/                # 对比实验输出
│           ├── ablation_*.csv                 # 消融汇总
│           └── multi_algorithm_*/             # 多算法汇总
├── 消融实验输出说明.md                         # 消融实验文档
├── 对比实验输出说明.md                         # 对比实验文档
├── 图表风格说明.md                            # 图表风格文档
├── 完整输出功能说明.md                        # 功能详解
├── README_消融实验完整版.md                   # 消融实验总览
└── README_实验输出总览.md                     # 本文档
```

## 🚀 快速开始

### 1. 消融实验（推荐先运行）

```bash
cd chapter-1
run_ablation_experiment.bat
# 选择 [1] 快速测试（5-10分钟）
```

验证系统正常后：

```bash
run_ablation_experiment.bat
# 选择 [2] 完整实验（2-5小时）
```

### 2. 单算法对比

```java
// 在 ComparisonExperiment.java 中
public static void main(String[] args) {
    runComparison(
        "chapter-1/src/main/resources/Machine/machine_2",
        "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
        3,      // 运行3次
        true    // 开启可视化
    );
}
```

### 3. 多算法对比

```java
// 在 MultiAlgorithmComparison.java 中
public static void main(String[] args) {
    runMultiAlgorithmComparison(
        "chapter-1/src/main/resources/Machine/machine_2",
        "chapter-1/src/main/resources/PrintItem/printItem_10/printItem_10_01",
        "Small-2M10J",
        3,      // 每算法运行3次
        true    // 开启可视化
    );
}
```

## 📈 输出数据统计

### 消融实验

- **运行次数**：150次（5配置×3算例×10次）
- **总时长**：约2-5小时
- **输出文件**：约150个文件夹 + 3个汇总文件
- **磁盘空间**：约150-300 MB

### 单算法对比

- **运行次数**：可配置（通常3-10次）
- **总时长**：约15分钟-1小时
- **输出文件**：N个文件夹 + 1个汇总文件
- **磁盘空间**：约3-30 MB

### 多算法对比

- **运行次数**：4算法×N次（通常每算法3-10次）
- **总时长**：约1-4小时（多线程并行）
- **输出文件**：4N个文件夹 + 3个汇总文件
- **磁盘空间**：约12-120 MB

## 📊 论文使用指南

### 消融实验章节

**使用文件**：
- `ablation_summary.csv` - 制作对比表格
- 选择性展示收敛曲线图
- 最优配置的甘特图和布局图

**推荐图表**：
- 表X.X：消融实验结果汇总
- 图X.X：组件贡献度对比图
- 图X.X：代表性配置收敛曲线

### 算法对比章节

**使用文件**：
- `statistics.csv` - 制作算法对比表
- 多算法收敛曲线对比图
- 最优算法的甘特图和布局图

**推荐图表**：
- 表X.X：算法性能对比表
- 图X.X：多算法收敛曲线对比
- 图X.X：最优算法调度方案

### 案例分析章节

**使用文件**：
- 选择一个代表性运行的完整报告
- 展示批次分配详情
- 展示装箱布局图

**推荐内容**：
- 详细的批次分配表格
- 机器负载统计
- 批次装箱可视化

## 🎯 与第二章对比

| 项目 | 第二章 | 第一章 | 对比 |
|-----|-------|-------|------|
| 实验类型 | 消融+对比 | 消融+对比 | ✅ 一致 |
| 输出格式 | 报告+图表 | 报告+图表 | ✅ 一致 |
| 图表风格 | 第二章标准 | 第二章标准 | ✅ 一致 |
| 自动化程度 | 每次运行 | 每次运行 | ✅ 一致 |
| 图表质量 | 高清PNG | 高清PNG | ✅ 一致 |
| 字体系统 | 宋体 | 宋体 | ✅ 一致 |
| 颜色方案 | 统一配色 | 统一配色 | ✅ 一致 |

**结论**：第一章和第二章的实验输出**完全一致**！✅

## 📝 相关文档

### 快速参考

1. **消融实验输出说明.md** - 消融实验的详细输出说明
2. **对比实验输出说明.md** - 对比实验的详细输出说明
3. **图表风格说明.md** - 图表风格的详细说明
4. **完整输出功能说明.md** - 输出功能的详细介绍

### 技术文档

1. **ABLATION_EXPERIMENT_README.md** - 消融实验技术文档
2. **消融实验使用指南.md** - 消融实验快速上手
3. **编译错误修复说明.md** - 编译问题解决
4. **NullPointerException修复说明.md** - 运行错误解决

### 总览文档

1. **README_消融实验完整版.md** - 消融实验总览
2. **README_实验输出总览.md** - 本文档

## ✅ 检查清单

运行实验前，确认：

- [ ] JDK 8或以上已安装
- [ ] Maven依赖已安装（JFreeChart等）
- [ ] 有足够的磁盘空间（至少500MB）
- [ ] 有足够的运行时间（快速测试约10分钟，完整实验约2-5小时）

运行实验后，确认：

- [ ] 每个文件夹包含实验报告.txt
- [ ] 每个文件夹包含收敛曲线图.png
- [ ] 每个文件夹包含甘特图.png
- [ ] 每个文件夹包含批次布局图_*.png
- [ ] 生成了汇总CSV文件
- [ ] 所有图片可以正常打开
- [ ] 图表风格与第二章一致

## 🎉 总结

第一章实验系统已完全升级：

✅ **三种实验类型**：消融实验、单算法对比、多算法对比  
✅ **每次运行自动生成**：报告 + 收敛曲线 + 甘特图 + 批次布局  
✅ **完全匹配第二章**：分辨率、字体、颜色、布局  
✅ **论文就绪**：所有输出可直接用于毕业论文  
✅ **易于使用**：一键运行，自动输出  

可以放心用于毕业论文的实验章节！🎓

---

**版本**: v2.0（完整输出总览）  
**更新日期**: 2026-02-05  
**作者**: AI Assistant  
**状态**: ✅ 生产就绪
