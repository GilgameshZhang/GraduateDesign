# 🚀 从这里开始 - 对比实验框架

## ⚡ 3分钟快速开始

### 1️⃣ 运行实验（1分钟）

**Windows用户**：
```bash
双击运行: run_comparison.bat
```

**其他用户**：
```bash
cd chapter-3
mvn test -Dtest=AlgorithmComparisonExperimentRunner
```

---

### 2️⃣ 选择模式（10秒）

```
请选择运行模式:
1. 单个算例对比     ← 推荐首次运行（30-45分钟）✨
2. J20批量对比      (2-3小时)
3. J50批量对比      (4-6小时)
4. J100批量对比     (8-12小时)
5. 全部算例对比     (15-20小时)

请输入选择 (1-5): 1
```

---

### 3️⃣ 查看结果（1分钟）

```bash
# 结果保存在
cd output/comparison_experiment/算例名称_时间戳/

# 查看对比报告
cat ComparisonReport.txt

# 查看某个算法的结果
cd NSGAII/run_1/
ls  # 看到7个输出文件
```

---

### 4️⃣ 画箱线图（1分钟）

```bash
pip install numpy matplotlib seaborn scipy pandas
python visualize_comparison.py
```

✅ **完成！** 现在你有了完整的对比实验结果！

---

## 📚 下一步阅读

根据你的需求选择：

| 你想做什么 | 推荐阅读 | 时间 |
|-----------|---------|------|
| 🏃 快速上手 | `对比实验快速指南.md` | 5分钟 |
| 📊 画箱线图 | `快速使用_画箱线图.md` | 5分钟 |
| 🔬 消融实验 | `消融实验快速指南.md` | 10分钟 |
| 📖 详细教程 | `算法对比实验使用说明.md` | 30分钟 |
| 🎓 论文写作 | `对比实验_最终版本说明.md` | 15分钟 |
| 📁 输出说明 | `对比实验_完整输出说明.md` | 15分钟 |

---

## 🎯 核心功能一览

### 支持的算法（5种）
1. **NSGA-II** - 改进版（局部搜索）⭐
2. **NSGA-II-Basic** - 基准版（无局部搜索）
3. **MOEA/D** - 基于分解
4. **MOGWO** - 灰狼优化
5. **SPEA2** - 强度Pareto

### 评价指标（3种）
1. **HV** - 收敛性+多样性
2. **IGD** - 与真实前沿的距离
3. **C-metric** - 支配关系

### 输出内容（每次运行7个文件）
1. ParetoFront.txt - 坐标数据
2. pareto_front.png - 前沿图
3. pareto_front_data.txt - 详细数据
4. best_cmax_gantt.png - 甘特图
5. best_cmax_printer_layout.png - 排布图
6. best_cmax_schedule_records.txt - 调度记录
7. Statistics.txt - 统计摘要

---

## 💡 实验建议

### 首次运行
👉 **选择模式1**（单个算例）
- 时间：30-45分钟
- 用途：验证程序、熟悉输出

### 正式实验
👉 **选择模式5**（全部算例）
- 时间：15-20小时
- 用途：论文数据、完整对比

### 快速测试
👉 **修改配置后选择模式1**
- `REPEAT_TIMES = 3`
- 时间：10-15分钟
- 用途：测试修改是否正确

---

## 📊 输出位置

```
chapter-3/output/comparison_experiment/
└── J20P3B2D5_01_20260129_143025/
    ├── NSGAII/run_1/ ... run_10/
    ├── NSGAII-Basic/run_1/ ... run_10/
    ├── MOEAD/run_1/ ... run_10/
    ├── MOGWO/run_1/ ... run_10/
    ├── SPEA2/run_1/ ... run_10/
    ├── ComparisonReport.txt       ← 论文数据
    ├── MetricsData.csv            ← 画箱线图
    └── ...
```

---

## 🎨 论文写作快速参考

### 数据从哪里来？

| 需要的数据 | 文件来源 |
|-----------|---------|
| HV/IGD平均值±标准差 | `ComparisonReport.txt` |
| 箱线图数据 | `MetricsData.csv` |
| Pareto前沿画图 | `算法名/AlgorithmParetoFront.txt` |
| 甘特图 | `算法名/run_X/best_cmax_gantt.png` |
| 打印排布图 | `算法名/run_X/best_cmax_printer_layout.png` |
| 统计检验 | `statistical_test_report.txt` |

### 论文必需的对比

✅ **1. 消融实验**（证明改进有效）
- NSGA-II vs NSGA-II-Basic
- 量化局部搜索的贡献

✅ **2. 算法对比**（证明竞争力）
- NSGA-II vs MOEA/D, MOGWO, SPEA2
- 证明NSGA-II最优

✅ **3. 统计检验**（证明显著性）
- Wilcoxon检验
- p < 0.01或p < 0.05

---

## 🔧 常见操作

### 查看实验进度

```bash
# 实验运行中，查看已完成的运行
ls output/comparison_experiment/*/NSGAII/
```

### 快速查看某个算法的结果

```bash
# 查看NSGA-II第1次运行的统计
cat output/.../NSGAII/run_1/Statistics.txt

# 查看Pareto前沿图
open output/.../NSGAII/run_1/pareto_front.png
```

### 对比两个算法的调度方案

```bash
# 同时打开两个甘特图
open NSGAII/run_1/best_cmax_gantt.png
open MOEAD/run_1/best_cmax_gantt.png
```

---

## ❓ 遇到问题？

### 编译失败
```bash
mvn clean install
```

### 内存不足
```bash
export MAVEN_OPTS="-Xmx8g"
```

### 运行太慢
- 减少 `REPEAT_TIMES = 5`
- 降低 `MAX_RUN_TIME = 3.0`
- 选择模式1（单个算例）

### 图片不显示
- 检查是否有错误信息
- 查看对应的`.txt`文件
- 图片生成失败不影响数据输出

---

## 📚 完整文档列表

### 🏃 快速入门（推荐先读）
1. `START_HERE.md` ← 本文件，从这里开始
2. `对比实验快速指南.md` - 3步完成实验
3. `快速使用_画箱线图.md` - 画图教程
4. `消融实验快速指南.md` - 消融实验

### 📖 详细教程
5. `算法对比实验使用说明.md` - 完整操作手册
6. `对比实验_完整输出说明.md` - 输出文件详解
7. `对比实验_指标数据文件说明.md` - 数据文件说明

### 🔧 功能说明
8. `对比实验_Pareto前沿去重说明.md` - 去重功能
9. `对比实验_NSGAII-Basic说明.md` - 基准算法

### 📝 版本更新
10. `对比实验_v1.1更新说明.md` - v1.1更新
11. `对比实验_v1.2更新说明.md` - v1.2更新
12. `对比实验_v1.3更新说明.md` - v1.3更新

### 📦 项目管理
13. `对比实验README.md` - 项目概述
14. `对比实验完成清单.md` - 功能清单
15. `对比实验交付清单.md` - 交付说明
16. `对比实验_新增文件清单.md` - 文件列表
17. `对比实验_最终版本说明.md` - 最终版本

---

## 🎓 成功案例流程

### 学生A - 快速验证（1天）

```
1. 运行模式1（单个J20算例）              30分钟
2. 查看ComparisonReport.txt              5分钟
3. 运行visualize_comparison.py           5分钟
4. 查看生成的图表                        10分钟
5. 理解实验结果                          30分钟
─────────────────────────────────────────────
总计                                     1.5小时
```

### 学生B - 完整实验（3天）

```
第1天：
- 运行模式5（全部算例）                  15小时
- 过夜运行，第二天早上查看结果

第2天：
- 查看所有对比报告                       2小时
- 生成所有图表                           2小时
- 数据分析和统计检验                     3小时

第3天：
- 撰写论文实验部分                       4小时
- 整理图表和表格                         2小时
- 检查和完善                             2小时
─────────────────────────────────────────────
总计                                     30小时
```

---

## ✨ 最后提醒

### 运行前
- [ ] 确认Java和Maven已安装
- [ ] 确认算例文件存在
- [ ] 预留足够的磁盘空间（~500MB）

### 运行中
- [ ] 不要关闭窗口
- [ ] 可以查看进度输出
- [ ] 如果使用模式5，建议过夜运行

### 运行后
- [ ] 检查输出文件是否完整
- [ ] 查看ComparisonReport.txt
- [ ] 运行可视化脚本
- [ ] 开始论文写作

---

**准备好了吗？立即开始吧！** 🚀

```bash
run_comparison.bat
```

**祝你实验顺利！** 🎉📊✨

---

*创建时间：2026-01-29*  
*版本：v1.3 Final*  
*作者：AI Assistant*
