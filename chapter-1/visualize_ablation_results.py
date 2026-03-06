# -*- coding: utf-8 -*-
"""
消融实验结果可视化脚本
生成条形图、箱线图和表格
"""

import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import os

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei', 'Arial Unicode MS']
plt.rcParams['axes.unicode_minus'] = False

# 设置样式
sns.set_style("whitegrid")
sns.set_palette("husl")

def load_data(base_path="src/main/output/ablation"):
    """加载实验数据"""
    detailed_file = os.path.join(base_path, "ablation_detailed_results.csv")
    summary_file = os.path.join(base_path, "ablation_summary.csv")
    
    if not os.path.exists(detailed_file):
        print(f"错误: 未找到详细结果文件 {detailed_file}")
        return None, None
    
    if not os.path.exists(summary_file):
        print(f"错误: 未找到汇总结果文件 {summary_file}")
        return None, None
    
    detailed_df = pd.read_csv(detailed_file)
    summary_df = pd.read_csv(summary_file)
    
    return detailed_df, summary_df

def plot_cmax_comparison(summary_df, output_dir):
    """绘制Cmax对比图"""
    fig, axes = plt.subplots(1, 3, figsize=(18, 5))
    
    instances = summary_df['Instance'].unique()
    
    for idx, instance in enumerate(instances):
        ax = axes[idx]
        data = summary_df[summary_df['Instance'] == instance]
        
        # 绘制条形图
        x = range(len(data))
        bars = ax.bar(x, data['AvgCmax'], yerr=data['StdDev'], 
                     capsize=5, alpha=0.7, edgecolor='black')
        
        # 添加数值标签
        for i, (cmax, rpd) in enumerate(zip(data['AvgCmax'], data['RPD(%)'])):
            ax.text(i, cmax + data['StdDev'].iloc[i] + 5, 
                   f'{cmax:.1f}\n({rpd:+.1f}%)', 
                   ha='center', va='bottom', fontsize=9)
        
        # 设置标签
        ax.set_xlabel('配置', fontsize=11, fontweight='bold')
        ax.set_ylabel('平均Cmax', fontsize=11, fontweight='bold')
        ax.set_title(f'{instance}', fontsize=12, fontweight='bold')
        ax.set_xticks(x)
        ax.set_xticklabels(data['ConfigName'], rotation=45, ha='right', fontsize=9)
        ax.grid(axis='y', alpha=0.3)
        
        # 高亮基线
        bars[0].set_color('green')
        bars[0].set_alpha(0.9)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'cmax_comparison.png'), dpi=300, bbox_inches='tight')
    print(f"已保存: cmax_comparison.png")
    plt.close()

def plot_rpd_heatmap(summary_df, output_dir):
    """绘制RPD热力图"""
    # 创建数据透视表
    pivot_data = summary_df.pivot(index='ConfigName', columns='Instance', values='RPD(%)')
    
    # 绘制热力图
    fig, ax = plt.subplots(figsize=(10, 6))
    sns.heatmap(pivot_data, annot=True, fmt='.2f', cmap='RdYlGn_r', 
                center=0, cbar_kws={'label': 'RPD (%)'},
                linewidths=0.5, linecolor='gray', ax=ax)
    
    ax.set_title('相对性能损失（RPD）热力图', fontsize=14, fontweight='bold', pad=15)
    ax.set_xlabel('测试算例', fontsize=11, fontweight='bold')
    ax.set_ylabel('配置', fontsize=11, fontweight='bold')
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'rpd_heatmap.png'), dpi=300, bbox_inches='tight')
    print(f"已保存: rpd_heatmap.png")
    plt.close()

def plot_boxplot(detailed_df, output_dir):
    """绘制箱线图"""
    fig, axes = plt.subplots(1, 3, figsize=(18, 5))
    
    instances = detailed_df['Instance'].unique()
    
    for idx, instance in enumerate(instances):
        ax = axes[idx]
        data = detailed_df[detailed_df['Instance'] == instance]
        
        # 绘制箱线图
        configs = data['ConfigName'].unique()
        box_data = [data[data['ConfigName'] == config]['Cmax'].values 
                   for config in configs]
        
        bp = ax.boxplot(box_data, labels=configs, patch_artist=True,
                       showmeans=True, meanline=True)
        
        # 设置颜色
        colors = plt.cm.Set3(np.linspace(0, 1, len(configs)))
        for patch, color in zip(bp['boxes'], colors):
            patch.set_facecolor(color)
            patch.set_alpha(0.7)
        
        ax.set_xlabel('配置', fontsize=11, fontweight='bold')
        ax.set_ylabel('Cmax', fontsize=11, fontweight='bold')
        ax.set_title(f'{instance} - Cmax分布', fontsize=12, fontweight='bold')
        ax.set_xticklabels(configs, rotation=45, ha='right', fontsize=9)
        ax.grid(axis='y', alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'cmax_distribution.png'), dpi=300, bbox_inches='tight')
    print(f"已保存: cmax_distribution.png")
    plt.close()

def plot_component_contribution(summary_df, output_dir):
    """绘制组件贡献度对比图"""
    # 只选择V0和移除单个组件的配置
    configs_of_interest = ['V0-Full', 'V1-NoHeuristic', 'V2-NoTabu', 'V3-SimpleHeuristic']
    data = summary_df[summary_df['ConfigID'].isin(configs_of_interest)]
    
    # 计算每个组件的平均RPD
    component_rpd = {}
    for config_id in ['V1-NoHeuristic', 'V2-NoTabu', 'V3-SimpleHeuristic']:
        config_data = data[data['ConfigID'] == config_id]
        component_rpd[config_id] = config_data['RPD(%)'].mean()
    
    # 组件名称映射
    component_names = {
        'V1-NoHeuristic': '启发式初始化',
        'V2-NoTabu': '禁忌搜索',
        'V3-SimpleHeuristic': 'Skyline启发式规则'
    }
    
    # 绘制条形图
    fig, ax = plt.subplots(figsize=(10, 6))
    
    components = list(component_rpd.keys())
    rpd_values = list(component_rpd.values())
    colors = ['#FF6B6B', '#4ECDC4', '#45B7D1']
    
    bars = ax.barh([component_names[c] for c in components], rpd_values, 
                   color=colors, alpha=0.8, edgecolor='black')
    
    # 添加数值标签
    for i, (bar, val) in enumerate(zip(bars, rpd_values)):
        ax.text(val + 0.5, bar.get_y() + bar.get_height()/2, 
               f'{val:.2f}%', va='center', fontweight='bold')
    
    ax.set_xlabel('平均RPD (%)', fontsize=12, fontweight='bold')
    ax.set_title('各组件对算法性能的贡献度', fontsize=14, fontweight='bold', pad=15)
    ax.grid(axis='x', alpha=0.3)
    ax.set_xlim(0, max(rpd_values) * 1.2)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'component_contribution.png'), dpi=300, bbox_inches='tight')
    print(f"已保存: component_contribution.png")
    plt.close()

def plot_utilization_comparison(summary_df, output_dir):
    """绘制利用率对比图"""
    fig, ax = plt.subplots(figsize=(12, 6))
    
    instances = summary_df['Instance'].unique()
    x = np.arange(len(summary_df['ConfigName'].unique()))
    width = 0.25
    
    for i, instance in enumerate(instances):
        data = summary_df[summary_df['Instance'] == instance]
        offset = width * (i - 1)
        ax.bar(x + offset, data['AvgUtilization'] * 100, width, 
              label=instance, alpha=0.8, edgecolor='black')
    
    ax.set_xlabel('配置', fontsize=11, fontweight='bold')
    ax.set_ylabel('平均材料利用率 (%)', fontsize=11, fontweight='bold')
    ax.set_title('材料利用率对比', fontsize=12, fontweight='bold')
    ax.set_xticks(x)
    ax.set_xticklabels(summary_df[summary_df['Instance'] == instances[0]]['ConfigName'], 
                       rotation=45, ha='right')
    ax.legend()
    ax.grid(axis='y', alpha=0.3)
    
    plt.tight_layout()
    plt.savefig(os.path.join(output_dir, 'utilization_comparison.png'), dpi=300, bbox_inches='tight')
    print(f"已保存: utilization_comparison.png")
    plt.close()

def generate_summary_table(summary_df, output_dir):
    """生成汇总表格"""
    # 选择关键列
    table_df = summary_df[['ConfigName', 'Instance', 'AvgCmax', 'MinCmax', 
                           'StdDev', 'AvgUtilization', 'RPD(%)']].copy()
    
    # 格式化数值
    table_df['AvgCmax'] = table_df['AvgCmax'].apply(lambda x: f'{x:.2f}')
    table_df['MinCmax'] = table_df['MinCmax'].apply(lambda x: f'{x:.2f}')
    table_df['StdDev'] = table_df['StdDev'].apply(lambda x: f'{x:.2f}')
    table_df['AvgUtilization'] = table_df['AvgUtilization'].apply(lambda x: f'{x*100:.2f}%')
    table_df['RPD(%)'] = table_df['RPD(%)'].apply(lambda x: f'{x:+.2f}%')
    
    # 重命名列
    table_df.columns = ['配置', '算例', '平均Cmax', '最优Cmax', 
                        '标准差', '平均利用率', 'RPD']
    
    # 保存为HTML
    html_file = os.path.join(output_dir, 'summary_table.html')
    table_df.to_html(html_file, index=False, border=1)
    print(f"已保存: summary_table.html")

def main():
    print("========================================")
    print("   消融实验结果可视化")
    print("========================================\n")
    
    # 加载数据
    print("加载数据...")
    detailed_df, summary_df = load_data()
    
    if detailed_df is None or summary_df is None:
        print("数据加载失败，请先运行消融实验")
        return
    
    print(f"详细数据: {len(detailed_df)} 条记录")
    print(f"汇总数据: {len(summary_df)} 条记录\n")
    
    # 创建输出目录
    output_dir = "src/main/output/ablation/figures"
    os.makedirs(output_dir, exist_ok=True)
    
    # 生成图表
    print("生成图表...")
    print("-" * 40)
    
    plot_cmax_comparison(summary_df, output_dir)
    plot_rpd_heatmap(summary_df, output_dir)
    plot_boxplot(detailed_df, output_dir)
    plot_component_contribution(summary_df, output_dir)
    plot_utilization_comparison(summary_df, output_dir)
    generate_summary_table(summary_df, output_dir)
    
    print("-" * 40)
    print(f"\n所有图表已保存到: {output_dir}/")
    print("\n========================================")
    print("可视化完成！")
    print("========================================")

if __name__ == "__main__":
    main()
