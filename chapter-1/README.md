# 第一章实验系统使用指南

## 🎉 欢迎使用第一章实验框架！

这是一个功能完整的实验框架，支持多算法对比、消融实验、多线程加速和自动可视化。

## 🚀 3分钟快速开始

### 第1步：快速测试（30秒）

**Windows用户**：双击运行
```
run_quick_test.bat
```

**或在IDE中运行**：
```java
// VisualizationTest.java
public static void main(String[] args) {
    quickTest();
}
```

**检查**：`chapter-1/src/main/output/test/` 目录是否生成了图表

---

### 第2步：多算法对比（约8分钟）

**Windows用户**：双击运行
```
run_multi_algorithm_comparison.bat
```

**或在IDE中运行**：
```java
// MultiAlgorithmComparison.java
public static void main(String[] args) {
    // 已配置好，直接运行
}
```

**输出**：4个算法（GA-TS、GA-GA、GA-ACO、ALNS）的完整对比

---

### 第3步：查看结果

打开输出目录：
```
chapter-1/src/main/output/multi_algorithm_comparison/Small-2M10J/
```

查看：
- `summary_statistics.csv` - 统计结果
- `comparison_report.txt` - 对比报告
- 各算法的可视化图表

---

## 📋 支持的实验类型

### 1️⃣ 多算法对比实验 ⭐推荐

**文件**：`MultiAlgorithmComparison.java`  
**启动**：`run_multi_algorithm_comparison.bat`

**包含算法**：
- GA-TS（您的算法）
- GA-GA
- GA-ACO
- ALNS

**特点**：
- ✅ 多线程并行
- ✅ 自动生成对比报告
- ✅ 完整可视化
- ✅ 统计分析

**耗时**：约7.5分钟（8核CPU）

---

### 2️⃣ 消融实验

**文件**：`AblationExperimentRunner.java`  
**启动**：`run_ablation_experiment.bat`

**测试配置**：
- V0-Full（完整算法）
- V1-NoHeuristic（无启发式初始化）
- V2-NoTabu（无禁忌搜索）
- V3-SimpleHeuristic（简化启发式）
- V4-Minimal（最小化版本）

**特点**：
- ✅ 多线程并行
- ✅ RPD分析
- ✅ 组件贡献度评估

**耗时**：约47分钟（8核CPU）

---

### 3️⃣ 单算法测试

**文件**：`ComparisonExperiment.java`

**用途**：深度测试单个算法

**特点**：
- ✅ 每次运行都生成可视化
- ✅ 详细的性能分析

**耗时**：15分钟（3次运行）

---

## ⏱️ 时间限制说明

### 核心改进

**所有算法现在使用5分钟时间限制作为终止条件**

```java
long timeLimitMs = 5 * 60 * 1000;  // 5分钟
```

### 为什么使用时间限制？

✅ **公平对比**：所有算法运行相同时间  
✅ **实际应用**：符合真实使用场景  
✅ **论文友好**：更有说服力  

### 输出示例

```
代数:10, Cmax:850.23, 进度:2.5% (7.5s/300.0s)
代数:20, Cmax:820.45, 进度:5.0% (15.0s/300.0s)
...
达到时间限制 300000ms，算法终止
总共完成 683 代，最优Cmax: 785.50
```

---

## 📊 可视化功能

### 自动生成的图表

#### 1. 迭代曲线图
- 显示算法收敛过程
- 横轴：迭代次数
- 纵轴：最优Cmax

#### 2. 批次分布图
- 蓝色柱：各机器批次数量
- 橙色线：各机器利用率

#### 3. 甘特图
- 横轴：时间
- 纵轴：机器
- 矩形：批次

#### 4. 批次布局图
- 每个批次的零件布局
- 红色↻：旋转标记

---

## 💻 系统要求

- **Java**: JDK 8 或更高
- **Maven**: 3.6+
- **CPU**: 建议4核以上（多线程加速）
- **内存**: 建议8GB以上
- **磁盘**: 预留2-5GB（用于可视化输出）

---

## 📚 详细文档

| 文档 | 说明 |
|------|------|
| 📘 [实验框架总览.md](实验框架总览.md) | 整体介绍和索引 |
| 📗 [可视化功能快速开始.md](可视化功能快速开始.md) | 可视化快速入门 |
| 📙 [多线程实验使用说明.md](多线程实验使用说明.md) | 多线程详细说明 |
| 📕 [时间限制说明.md](时间限制说明.md) | 时间限制详解 |
| 📓 [GA-TS对比实验说明.md](GA-TS对比实验说明.md) | GA-TS算法说明 |

---

## 🎓 论文使用流程

### Step 1: 算法对比实验

```bash
运行: run_multi_algorithm_comparison.bat
耗时: 约8分钟
输出: 4个算法的对比数据和图表
```

用于论文：
- 性能对比表格
- 迭代曲线图（算法收敛特性）
- 甘特图（调度方案展示）

---

### Step 2: 消融实验

```bash
运行: run_ablation_experiment.bat
耗时: 约47分钟
输出: 5个配置的对比分析
```

用于论文：
- 组件贡献度分析
- RPD表格
- 算法设计合理性验证

---

### Step 3: 案例分析

```bash
运行: ComparisonExperiment.java
选择: 最优运行的详细结果
```

用于论文：
- 具体调度方案展示
- 批次布局图
- 资源利用分析

---

## 🔧 常用操作

### 查看实验结果

```powershell
# 打开输出目录
explorer chapter-1\src\main\output\
```

### 清理旧结果

```powershell
# 删除旧的输出（释放空间）
rmdir /s /q chapter-1\src\main\output
```

### 修改时间限制

在相应的Java文件中：
```java
long timeLimitMs = 10 * 60 * 1000;  // 改为10分钟
```

### 关闭可视化（加快速度）

```java
runMultiAlgorithmComparison(..., false);  // 最后一个参数改为false
```

---

## ❓ 遇到问题？

### 编译错误

```bash
# 清理并重新编译
mvn clean compile test-compile
```

### 找不到类

```bash
# 确保在正确的目录
cd chapter-1
```

### 内存不足

```bash
# 增加JVM内存
set MAVEN_OPTS=-Xmx4g
```

### 图表中文乱码

修改代码中的字体：
```java
Font font = new Font("SimHei", Font.PLAIN, 12);  // 使用黑体
```

---

## 📞 技术支持

如有问题：
1. 查看对应的详细文档
2. 检查代码注释
3. 运行快速测试验证环境

---

## ✅ 使用检查清单

- [ ] JDK 8+ 已安装
- [ ] Maven 已配置
- [ ] 运行 `run_quick_test.bat` 成功
- [ ] 输出目录生成了图表
- [ ] 图表中文显示正常
- [ ] 运行多算法对比实验
- [ ] 查看并理解输出结果
- [ ] 运行消融实验（如需要）

---

**现在您可以开始实验了！建议从快速测试开始，然后逐步进行完整实验。**

**祝您的毕设顺利完成！🎓**
