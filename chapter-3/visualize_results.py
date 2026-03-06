"""
MOGWO实验结果可视化脚本

用于生成Pareto前沿对比图、性能指标对比图等

依赖: pip install pandas matplotlib seaborn numpy scipy
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
from pathlib import Path

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'DejaVu Sans']  # 用来正常显示中文标签
plt.rcParams['axes.unicode_minus'] = False  # 用来正常显示负号

# 设置绘图风格
sns.set_style("whitegrid")
plt.rcParams['figure.dpi'] = 300


def plot_pareto_front(csv_files, labels, output_path="pareto_comparison.png"):
    """
    绘制多个算法的Pareto前沿对比图
    
    参数:
        csv_files: CSV文件路径列表
        labels: 算法名称列表
        output_path: 输出图片路径
    """
    plt.figure(figsize=(10, 8))
    
    colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728']
    markers = ['o', 's', '^', 'D']
    
    for i, (csv_file, label) in enumerate(zip(csv_files, labels)):
        try:
            df = pd.read_csv(csv_file)
            plt.scatter(df['Cmax'], df['Energy'], 
                       alpha=0.6, s=80, 
                       color=colors[i % len(colors)],
                       marker=markers[i % len(markers)],
                       label=label,
                       edgecolors='black', linewidth=0.5)
        except Exception as e:
            print(f"警告: 无法读取 {csv_file}: {e}")
    
    plt.xlabel('Cmax (Makespan)', fontsize=14, fontweight='bold')
    plt.ylabel('Total Energy Consumption', fontsize=14, fontweight='bold')
    plt.title('Pareto Front Comparison', fontsize=16, fontweight='bold')
    plt.legend(fontsize=12, loc='best', framealpha=0.9)
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    print(f"✅ Pareto前沿对比图已保存: {output_path}")
    plt.close()


def plot_performance_comparison(comparison_csv, output_dir="output/plots"):
    """
    从对比实验结果CSV绘制性能指标对比图
    
    参数:
        comparison_csv: ComparisonExperiment生成的CSV文件路径
        output_dir: 输出目录
    """
    Path(output_dir).mkdir(parents=True, exist_ok=True)
    
    try:
        df = pd.read_csv(comparison_csv)
    except Exception as e:
        print(f"错误: 无法读取 {comparison_csv}: {e}")
        return
    
    algorithms = df['Algorithm'].unique()
    
    # 1. ParetoFrontSize箱线图
    plt.figure(figsize=(10, 6))
    data_to_plot = [df[df['Algorithm'] == alg]['ParetoFrontSize'].values 
                    for alg in algorithms]
    box = plt.boxplot(data_to_plot, labels=algorithms, patch_artist=True,
                      boxprops=dict(facecolor='lightblue', alpha=0.7),
                      medianprops=dict(color='red', linewidth=2))
    plt.ylabel('Pareto Front Size', fontsize=12, fontweight='bold')
    plt.title('Pareto Front Size Comparison', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3, axis='y')
    plt.tight_layout()
    plt.savefig(f"{output_dir}/pf_size_comparison.png", dpi=300, bbox_inches='tight')
    print(f"✅ PF大小对比图已保存: {output_dir}/pf_size_comparison.png")
    plt.close()
    
    # 2. RunTime箱线图
    plt.figure(figsize=(10, 6))
    data_to_plot = [df[df['Algorithm'] == alg]['RunTime'].values 
                    for alg in algorithms]
    box = plt.boxplot(data_to_plot, labels=algorithms, patch_artist=True,
                      boxprops=dict(facecolor='lightgreen', alpha=0.7),
                      medianprops=dict(color='red', linewidth=2))
    plt.ylabel('Run Time (seconds)', fontsize=12, fontweight='bold')
    plt.title('Run Time Comparison', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3, axis='y')
    plt.tight_layout()
    plt.savefig(f"{output_dir}/runtime_comparison.png", dpi=300, bbox_inches='tight')
    print(f"✅ 运行时间对比图已保存: {output_dir}/runtime_comparison.png")
    plt.close()
    
    # 3. Hypervolume箱线图
    plt.figure(figsize=(10, 6))
    data_to_plot = [df[df['Algorithm'] == alg]['Hypervolume'].values 
                    for alg in algorithms]
    box = plt.boxplot(data_to_plot, labels=algorithms, patch_artist=True,
                      boxprops=dict(facecolor='lightyellow', alpha=0.7),
                      medianprops=dict(color='red', linewidth=2))
    plt.ylabel('Hypervolume', fontsize=12, fontweight='bold')
    plt.title('Hypervolume Comparison', fontsize=14, fontweight='bold')
    plt.grid(True, alpha=0.3, axis='y')
    plt.tight_layout()
    plt.savefig(f"{output_dir}/hypervolume_comparison.png", dpi=300, bbox_inches='tight')
    print(f"✅ 超体积对比图已保存: {output_dir}/hypervolume_comparison.png")
    plt.close()
    
    # 4. 综合性能雷达图
    plot_radar_chart(df, algorithms, output_dir)
    
    # 5. 统计表格
    generate_statistics_table(df, algorithms, output_dir)


def plot_radar_chart(df, algorithms, output_dir):
    """
    绘制雷达图（多维性能对比）
    """
    # 计算每个算法的平均指标（归一化）
    metrics = ['ParetoFrontSize', 'Hypervolume', 'Spacing']
    
    fig, ax = plt.subplots(figsize=(10, 10), subplot_kw=dict(projection='polar'))
    
    angles = np.linspace(0, 2 * np.pi, len(metrics), endpoint=False).tolist()
    angles += angles[:1]  # 闭合
    
    colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728']
    
    for i, alg in enumerate(algorithms):
        alg_data = df[df['Algorithm'] == alg]
        
        # 计算归一化值
        values = []
        for metric in metrics:
            if metric == 'Spacing':
                # Spacing越小越好，取倒数
                val = 1.0 / alg_data[metric].mean() if alg_data[metric].mean() > 0 else 0
            else:
                val = alg_data[metric].mean()
            values.append(val)
        
        # 归一化到0-1
        max_vals = [df[df['Algorithm'] == alg][metric].mean() 
                    for alg in algorithms for metric in [m]]
        
        values += values[:1]  # 闭合
        
        ax.plot(angles, values, 'o-', linewidth=2, label=alg, 
                color=colors[i % len(colors)])
        ax.fill(angles, values, alpha=0.15, color=colors[i % len(colors)])
    
    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(metrics, fontsize=11)
    ax.set_ylim(0, None)
    ax.set_title('Multi-dimensional Performance Comparison', 
                 fontsize=14, fontweight='bold', pad=20)
    ax.legend(loc='upper right', bbox_to_anchor=(1.3, 1.1), fontsize=11)
    ax.grid(True, alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(f"{output_dir}/radar_comparison.png", dpi=300, bbox_inches='tight')
    print(f"✅ 雷达图已保存: {output_dir}/radar_comparison.png")
    plt.close()


def generate_statistics_table(df, algorithms, output_dir):
    """
    生成统计表格
    """
    stats = []
    
    for alg in algorithms:
        alg_data = df[df['Algorithm'] == alg]
        stats.append({
            'Algorithm': alg,
            'PF Size (avg±std)': f"{alg_data['ParetoFrontSize'].mean():.1f}±{alg_data['ParetoFrontSize'].std():.1f}",
            'Run Time (avg±std)': f"{alg_data['RunTime'].mean():.2f}±{alg_data['RunTime'].std():.2f}",
            'Hypervolume (avg±std)': f"{alg_data['Hypervolume'].mean():.4f}±{alg_data['Hypervolume'].std():.4f}",
            'Spacing (avg±std)': f"{alg_data['Spacing'].mean():.4f}±{alg_data['Spacing'].std():.4f}",
            'Min Cmax': f"{alg_data['MinCmax'].mean():.2f}",
            'Min Energy': f"{alg_data['MinEnergy'].mean():.2f}"
        })
    
    stats_df = pd.DataFrame(stats)
    
    # 保存为CSV
    stats_csv = f"{output_dir}/statistics_summary.csv"
    stats_df.to_csv(stats_csv, index=False)
    print(f"✅ 统计摘要已保存: {stats_csv}")
    
    # 打印到控制台
    print("\n" + "="*80)
    print("统计摘要")
    print("="*80)
    print(stats_df.to_string(index=False))
    print("="*80)


def statistical_test(comparison_csv):
    """
    进行统计显著性检验（Wilcoxon秩和检验）
    """
    try:
        from scipy.stats import wilcoxon
    except ImportError:
        print("⚠️  需要安装scipy: pip install scipy")
        return
    
    try:
        df = pd.read_csv(comparison_csv)
    except Exception as e:
        print(f"错误: 无法读取 {comparison_csv}: {e}")
        return
    
    algorithms = df['Algorithm'].unique()
    
    if len(algorithms) < 2:
        print("至少需要两个算法进行对比")
        return
    
    print("\n" + "="*80)
    print("Wilcoxon秩和检验（Hypervolume）")
    print("="*80)
    
    # 以第一个算法为基准
    base_alg = algorithms[0]
    base_hv = df[df['Algorithm'] == base_alg]['Hypervolume'].values
    
    for alg in algorithms[1:]:
        alg_hv = df[df['Algorithm'] == alg]['Hypervolume'].values
        
        if len(base_hv) == len(alg_hv) and len(base_hv) > 0:
            try:
                statistic, p_value = wilcoxon(base_hv, alg_hv)
                
                significance = ""
                if p_value < 0.001:
                    significance = "*** (高度显著)"
                elif p_value < 0.01:
                    significance = "** (非常显著)"
                elif p_value < 0.05:
                    significance = "* (显著)"
                else:
                    significance = "n.s. (不显著)"
                
                print(f"{base_alg} vs {alg}:")
                print(f"  统计量: {statistic:.4f}")
                print(f"  p值: {p_value:.6f} {significance}")
                print()
            except Exception as e:
                print(f"  检验失败: {e}")
        else:
            print(f"{base_alg} vs {alg}: 数据长度不匹配，跳过")
    
    print("="*80)


# ========== 使用示例 ==========

def example_usage():
    """
    使用示例
    """
    print("MOGWO实验结果可视化示例\n")
    
    # 示例1: 绘制Pareto前沿对比
    print("示例1: Pareto前沿对比")
    csv_files = [
        "output/mogwo/quick/MOGWO_pareto_front.csv",
        "output/nsgaii/NSGAII_pareto_front.csv",
    ]
    labels = ["MOGWO", "NSGA-II"]
    
    # 检查文件是否存在
    existing_files = []
    existing_labels = []
    for csv_file, label in zip(csv_files, labels):
        if Path(csv_file).exists():
            existing_files.append(csv_file)
            existing_labels.append(label)
        else:
            print(f"  ⚠️  文件不存在: {csv_file}")
    
    if existing_files:
        plot_pareto_front(existing_files, existing_labels, "output/plots/pareto_comparison.png")
    else:
        print("  ⚠️  没有找到Pareto前沿文件，跳过")
    
    print()
    
    # 示例2: 绘制性能对比图
    print("示例2: 性能指标对比")
    comparison_csv = "output/comparison/comparison_results.csv"
    
    if Path(comparison_csv).exists():
        plot_performance_comparison(comparison_csv, "output/plots")
        statistical_test(comparison_csv)
    else:
        print(f"  ⚠️  文件不存在: {comparison_csv}")
        print("  请先运行 ComparisonExperiment 生成对比结果")


if __name__ == "__main__":
    example_usage()
    
    print("\n" + "="*80)
    print("可视化完成！")
    print("="*80)
    print("\n使用提示:")
    print("1. 确保已安装依赖: pip install pandas matplotlib seaborn scipy")
    print("2. 修改example_usage()中的文件路径以匹配你的实际路径")
    print("3. 运行此脚本: python visualize_results.py")
    print("\n生成的图片可用于论文和答辩展示")
