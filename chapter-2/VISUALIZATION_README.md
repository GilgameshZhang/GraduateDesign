# 遗传算法调度结果可视化工具

本工具用于将遗传算法的调度结果转换为直观的甘特图和打印机零件排布图。

## 文件结构

```
chapter-2/
├── run_and_visualize.py          # 完整流程脚本（编译Java、运行算法、可视化）
├── visualize_results.py          # 可视化脚本
├── VISUALIZATION_README.md       # 本说明文件
├── src/test/java/RunAlgorithmExample.java  # 遗传算法主程序
└── visualization_results/        # 输出目录（自动创建）
    ├── gantt_chart.png          # 甘特图
    ├── printer_layout.png       # 打印机零件排布图
    └── summary_report.json      # 汇总报告
```

## 快速开始

### 方法1：一键运行完整流程

```bash
cd chapter-2
python run_and_visualize.py
```

此命令会：
1. 编译Java代码
2. 运行遗传算法
3. 自动生成可视化图表

### 方法2：仅运行可视化（已有输出文件）

如果已经运行过算法并保存了输出：

```bash
cd chapter-2
python visualize_results.py
```

## 输出文件说明

### 1. 甘特图 (gantt_chart.png)
- **内容**：展示每个机器的工作时间表
- **横轴**：时间
- **纵轴**：不同颜色的条形代表不同工件的工序
- **颜色**：不同颜色代表不同工件
- **标签**：每个条形上显示工序类型（打印/批处理/离散）

### 2. 打印机零件排布图 (printer_layout.png)
- **内容**：展示打印批次中零件在打印平台上的布局
- **横轴**：X坐标
- **纵轴**：Y坐标
- **矩形**：每个矩形代表一个零件，显示其实际尺寸和位置
- **标签**：每个零件上显示工件编号(J0, J1, etc.)
- **批次信息**：右上角显示批次编号和时间范围

### 3. 汇总报告 (summary_report.json)
包含以下统计信息：
- makespan（总完工时间）
- 机器利用率
- 工件完工时间
- 机器数量统计

## 使用说明

### 前提条件
- Python 3.6+
- JDK 8+
- matplotlib
- numpy

### 安装依赖

```bash
pip install matplotlib numpy
```

### 自定义配置

#### 修改算例文件
编辑 `run_and_visualize.py` 中的 `instance_file` 变量：

```python
instance_file = "src/main/resources/instance/J50P5B4D10_01.txt"  # 改为你想要的算例
```

#### 修改输出路径
编辑输出文件路径：

```python
output_file = "my_results.txt"  # 自定义输出文件名
```

## 可视化效果示例

### 甘特图示例
```
机器1 (打印机1)         ████████░░░░░░░░░░░░░░░░░░░
机器2 (打印机2)         ░░░░████████░░░░░░░░░░░░░░░
机器3 (批处理机1)       ░░░░░░░░████████░░░░░░░░░░░
机器4 (离散处理机1)     ░░░░░░░░░░░░████████░░░░░░░
时间轴 -----------------0----10----20----30----40--
```

### 打印机排布图示例
```
打印平台布局：
┌─────────────────────────────────┐
│          ┌─────┐                │
│          │ J5  │     ┌─────┐    │
│          └─────┘     │ J12 │    │
│                      └─────┘    │
│  ┌─────┐                       │
│  │ J3  │                       │
│  └─────┘                       │
└─────────────────────────────────┘
批次1: 时间 0.0-25.5
```

## 故障排除

### 常见问题

1. **Java编译失败**
   - 检查JDK是否正确安装
   - 确认所有Java源文件存在

2. **可视化失败**
   - 检查matplotlib和numpy是否安装
   - 确认输出文件格式正确

3. **中文显示乱码**
   - 确保系统安装了中文字体
   - 或修改代码使用英文标签

4. **算例文件不存在**
   - 检查 `src/main/resources/instance/` 目录
   - 确认算例文件名正确

### 调试模式

如需查看详细的解析过程，在 `visualize_results.py` 中取消注释：

```python
# print(f"解析到机器 {machine_id}: {len(operations)} 个工序")
# print(f"解析到工件 {job_id}: {len(operations)} 个工序")
```

## 扩展功能

### 添加新图表类型
在 `ScheduleVisualizer` 类中添加新方法：

```python
def plot_custom_chart(self, save_path=None):
    """自定义图表"""
    # 实现你的图表逻辑
    pass
```

### 导出其他格式
支持导出为PDF、SVG等格式：

```python
plt.savefig('chart.pdf', format='pdf', dpi=300)
plt.savefig('chart.svg', format='svg')
```

### 交互式图表
使用plotly创建交互式图表：

```python
import plotly.graph_objects as go
# 使用plotly创建交互式甘特图
```

## 技术细节

### 数据解析
- 使用正则表达式解析程序输出
- 支持中文和英文输出格式
- 自动处理不同格式的数值

### 布局算法
- 打印机布局使用贪心装箱算法
- 支持零件旋转优化空间利用

### 性能优化
- 批量处理大量工序数据
- 内存高效的图表生成
- 支持大规模问题实例

## 版本历史

- v1.0: 初始版本，支持基础甘特图和布局图
- v1.1: 添加汇总报告和错误处理
- v1.2: 支持批量处理和自定义配置

## 贡献

欢迎提交Issue和Pull Request来改进这个工具！

## 许可证

本项目采用MIT许可证。
