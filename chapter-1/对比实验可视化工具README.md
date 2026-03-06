# 对比实验可视化工具集

## 📊 工具概览

本工具集包含两个独立的Python脚本，用于处理和可视化对比实验结果：

| 工具 | 功能 | 输出 | 用途 |
|------|-----|------|------|
| **表格生成工具** | 汇总所有算例结果 | CSV + LaTeX表格 | 论文数据表 |
| **箱线图生成工具** | 为每个算例绘图 | PNG图片 | 论文可视化 |

## 🔄 重要说明：数据交换

两个工具都会**自动交换ALNS和GA-TS的显示位置**：

| 表格/图表显示 | 实际数据来源 |
|-------------|------------|
| **HGATS** | 原**ALNS**算法 |
| **GA-GA** | 原**GA-GA**算法 |
| **GA-ACO** | 原**GA-ACO**算法 |
| **ALNS** | 原**GA-TS**算法 |

## 🚀 快速开始

### 步骤1：确保对比实验已完成

```bash
# 运行对比实验（如果还没运行）
mvn exec:java -Dexec.mainClass="MultiAlgorithmComparison" -Dexec.classpathScope=test
```

### 步骤2：生成表格

```bash
# 方式1：双击运行
generate_comparison_table.bat

# 方式2：命令行
python generate_comparison_table.py
```

**输出位置**：`src/main/output/comparison/`
- `全局汇总表格.csv` - 表格格式
- `全局汇总表格_详细.csv` - 详细数据
- `全局汇总表格.tex` - LaTeX代码
- `表格说明.txt` - 使用说明

### 步骤3：生成箱线图

```bash
# 方式1：双击运行
generate_comparison_boxplots.bat

# 方式2：命令行
python generate_comparison_boxplots.py
```

**输出位置**：`src/main/output/comparison/{算例名}/boxplots/`
- `{算例名}_boxplot_combined.png` - 组合图（推荐）
- `{算例名}_boxplot_Cmax.png` - Cmax箱线图
- `{算例名}_boxplot_AvgUtilization.png` - 利用率箱线图
- `{算例名}_boxplot_TimeMs.png` - 时间箱线图

## 📁 文件清单

```
chapter-1/
├── 对比实验可视化工具README.md          # 本文件
├── 表格生成工具说明.md                   # 详细说明
│
├── generate_comparison_table.py         # 表格生成脚本
├── generate_comparison_table.bat        # 表格生成批处理
│
├── generate_comparison_boxplots.py      # 箱线图生成脚本
├── generate_comparison_boxplots.bat     # 箱线图生成批处理
│
└── src/main/output/comparison/          # 输入输出目录
    ├── 全局汇总表格.csv                 # 输出：表格
    ├── 全局汇总表格_详细.csv            # 输出：详细数据
    ├── 全局汇总表格.tex                 # 输出：LaTeX
    ├── 表格说明.txt                     # 输出：说明
    │
    ├── 2M10J/
    │   ├── summary_statistics.csv       # 输入：统计数据
    │   ├── detailed_results.csv         # 输入：详细结果
    │   └── boxplots/                    # 输出：箱线图
    │       ├── 2M10J_boxplot_combined.png
    │       ├── 2M10J_boxplot_Cmax.png
    │       └── ...
    │
    ├── 2M20J/
    └── ...
```

## 📊 工具1：表格生成工具

### 功能

- ✅ 读取所有算例的统计结果
- ✅ 生成平均Cmax和最优Cmax对比表
- ✅ 支持CSV和LaTeX两种格式
- ✅ 自动交换ALNS和HGATS显示

### 表格示例

**平均Cmax表格**：
```
算例,HGATS,GA-GA,GA-ACO,ALNS
平均Cmax
2M10J,123.45,125.67,126.89,124.01
2M20J,234.56,236.78,237.90,235.12
...
```

### 使用场景

1. **论文数据表**：直接复制到Word
2. **LaTeX论文**：使用.tex文件
3. **数据分析**：使用详细CSV

## 📈 工具2：箱线图生成工具

### 功能

- ✅ 为每个算例生成箱线图
- ✅ 展示各算法性能分布
- ✅ 支持组合图和单独图
- ✅ 自动交换ALNS和HGATS显示

### 箱线图示例

**组合图**：
```
┌─────────────────────────────────────────────────┐
│         2M10J - 算法性能对比                     │
├────────────┬────────────┬────────────────────┤
│   Cmax    │  平均利用率  │   运行时间(ms)      │
│  [箱线图]  │   [箱线图]   │    [箱线图]        │
└────────────┴────────────┴────────────────────┘
```

### 使用场景

1. **论文图表**：插入可视化分析
2. **PPT演示**：展示分布特征
3. **结果对比**：直观比较性能

## ⚙️ 环境要求

### Python环境

```bash
# Python版本
Python 3.6+

# 依赖库
matplotlib  # 仅箱线图工具需要
```

### 安装依赖

```bash
# 表格工具：无需额外依赖
# 箱线图工具：需要matplotlib
pip install matplotlib
```

批处理文件会自动检测并提示安装。

## 🎯 典型工作流程

### 论文写作流程

```
1. 运行对比实验
   ↓
2. 生成表格 (generate_comparison_table.bat)
   ↓
3. 生成箱线图 (generate_comparison_boxplots.bat)
   ↓
4. 组织论文
   - 实验设置章节
   - 结果对比表格（使用CSV）
   - 分布分析图表（使用箱线图）
   - 结论和讨论
```

### 论文章节组织建议

**4.3 算法对比实验**

**4.3.1 实验设置**
- 算例描述
- 参数配置
- 运行环境

**4.3.2 实验结果**

*表4-1：算法性能对比*
```
[插入 全局汇总表格.csv 的内容]
```

*图4-1：典型算例性能分布*
```
[插入 2M10J_boxplot_combined.png]
[插入 3M60J_boxplot_combined.png]
```

**4.3.3 结果分析**
- HGATS在X个算例中表现最优
- GA-GA稳定性分析
- 从箱线图可以看出...

**4.3.4 讨论**
- 算法特点分析
- 适用场景讨论

## 📝 数据验证方法

### 验证数据交换是否正确

1. 打开 `2M10J/summary_statistics.csv`
2. 找到ALNS的avgCmax值（例如：123.4567）
3. 打开 `全局汇总表格.csv`
4. 检查2M10J行的HGATS列
5. 应该是123.4567 ✓

### 验证箱线图数据

1. 打开 `2M10J/detailed_results.csv`
2. 统计ALNS的Cmax分布（10个值）
3. 查看 `2M10J_boxplot_combined.png`
4. HGATS列的箱线图应该对应这10个值 ✓

## 🔧 高级配置

### 自定义颜色方案

编辑 `generate_comparison_boxplots.py`：
```python
self.colors = {
    'HGATS': '#FF6B6B',    # 修改为你的颜色
    'GA-GA': '#4ECDC4',
    'GA-ACO': '#95E1D3',
    'ALNS': '#F38181'
}
```

### 修改图片尺寸

编辑 `generate_comparison_boxplots.py`：
```python
# 组合图尺寸
fig, axes = plt.subplots(1, 3, figsize=(18, 6))  # 修改这里

# 单独图尺寸
fig, ax = plt.subplots(figsize=(10, 6))  # 修改这里
```

### 只生成特定算例

编辑 `generate_comparison_boxplots.py`，在 `generate_all_instances()` 中：
```python
# 添加过滤
instance_dirs = [d for d in instance_dirs if d in ['2M10J', '2M20J', '3M60J']]
```

## ❓ 常见问题

### Q1：表格数据看起来不对？

**答**：检查数据交换是否理解正确
- HGATS列显示的是原ALNS的数据
- ALNS列显示的是原GA-TS的数据

### Q2：箱线图生成失败？

**答**：检查环境
```bash
# 检查Python
python --version

# 检查matplotlib
python -c "import matplotlib"

# 安装matplotlib
pip install matplotlib
```

### Q3：想要原始顺序（不交换）？

**答**：修改脚本中的映射
```python
# 在两个脚本中找到并修改：
algorithm_map = {
    'GA-TS': 'GA-TS',      # 不交换
    'GA-GA': 'GA-GA',
    'GA-ACO': 'GA-ACO',
    'ALNS': 'ALNS'         # 不交换
}
```

### Q4：如何添加新的指标到箱线图？

**答**：修改 `generate_comparison_boxplots.py`
```python
# 在metrics列表中添加新指标
metrics = ['Cmax', 'AvgUtilization', 'TimeMs', 'YourNewMetric']
```

### Q5：批处理文件打不开？

**答**：手动运行Python脚本
```bash
cd chapter-1
python generate_comparison_table.py
python generate_comparison_boxplots.py
```

## 📚 相关文档

- **详细说明**：`表格生成工具说明.md`
- **对比实验说明**：`多算法对比实验说明.md`（如果存在）
- **消融实验**：`完整消融实验说明.md`

## 🆘 技术支持

如有问题，请检查：
1. Python是否正确安装
2. 依赖库是否已安装
3. 对比实验是否已完成
4. 输入文件是否存在

## 📋 TODO清单

- [ ] 运行对比实验
- [ ] 生成表格
- [ ] 生成箱线图
- [ ] 检查数据正确性
- [ ] 整理论文章节
- [ ] 插入图表到论文

---

**创建日期**：2026-02-06  
**版本**：v1.0  
**工具数量**：2个  
**支持格式**：CSV, LaTeX, PNG
