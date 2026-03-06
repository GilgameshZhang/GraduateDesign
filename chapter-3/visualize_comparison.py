#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
算法对比实验结果可视化工具

功能：
1. 绘制Pareto前沿对比图
2. 绘制HV对比箱线图
3. 绘制IGD对比箱线图
4. 生成统计显著性检验报告

作者：AI Assistant
版本：1.0
"""

import os
import glob
import numpy as np
import matplotlib.pyplot as plt
import seaborn as sns
from scipy import stats
import pandas as pd

# 设置中文字体（Windows）
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

# 算法配置
ALGORITHMS = ['NSGAII', 'NSGAII-Basic', 'MOEAD', 'MOGWO', 'SPEA2']
ALGORITHM_NAMES = {
    'NSGAII': 'NSGA-II',
    'NSGAII-Basic': 'NSGA-II-Basic',
    'MOEAD': 'MOEA/D',
    'MOGWO': 'MOGWO',
    'SPEA2': 'SPEA2'
}
# 帕累托前沿图用色：红、蓝、绿、橙、紫，差别鲜明
COLORS = {
    'NSGAII': '#C1121F',       # 深红
    'NSGAII-Basic': '#0066CC', # 蓝
    'MOEAD': '#2D6A4F',        # 深绿
    'MOGWO': '#E85D04',        # 橙
    'SPEA2': '#7B2CBF'         # 紫
}
MARKERS = {
    'NSGAII': 'o',
    'NSGAII-Basic': 'v',
    'MOEAD': 's',
    'MOGWO': '^',
    'SPEA2': 'D'
}


def get_experiment_base_dir():
    """返回对比实验输出根目录"""
    return os.path.join(os.path.dirname(os.path.abspath(__file__)), 'output', 'comparison_experiment')


def find_all_experiments():
    """搜索全部对比实验结果目录，返回 [(exp_dir, instance_name), ...]"""
    base_dir = get_experiment_base_dir()
    
    if not os.path.exists(base_dir):
        print(f"❌ 找不到输出目录: {base_dir}")
        return []
    
    # 匹配所有一级子目录（每个子目录为一次实验）
    pattern = os.path.join(base_dir, '*')
    dirs = [d for d in glob.glob(pattern) if os.path.isdir(d)]
    
    if not dirs:
        print(f"❌ 未找到任何实验结果目录")
        return []
    
    # 按修改时间倒序（最新的在前），并提取算例名
    result = []
    for d in sorted(dirs, key=os.path.getmtime, reverse=True):
        name = os.path.basename(d.rstrip(os.sep))
        # 算例名：去掉末尾 _日期_时间 部分，若没有则用整个目录名
        parts = name.rsplit('_', 2)
        if len(parts) >= 3 and parts[-1].isdigit() and parts[-2].isdigit():
            instance_name = parts[0]
        else:
            instance_name = name
        result.append((d, instance_name))
    
    return result


def find_latest_experiment(instance_name=None):
    """找到最新的实验结果目录（保留兼容）"""
    all_exp = find_all_experiments()
    if not all_exp:
        return None
    if instance_name:
        matched = [(d, iname) for d, iname in all_exp if iname == instance_name]
        if not matched:
            return None
        return matched[0][0]
    return all_exp[0][0]


def read_pareto_front(file_path):
    """读取Pareto前沿文件"""
    if not os.path.exists(file_path):
        print(f"⚠️ 文件不存在: {file_path}")
        return None, None
    
    try:
        data = np.loadtxt(file_path, skiprows=4)
        if len(data.shape) == 1:
            data = data.reshape(1, -1)
        return data[:, 0], data[:, 1]  # Cmax, Energy
    except Exception as e:
        print(f"❌ 读取文件失败: {file_path}")
        print(f"   错误: {e}")
        return None, None


def read_metrics_from_csv(csv_path):
    """从CSV文件中读取指标数据（优先使用）"""
    metrics = {alg: {'hv': [], 'igd': []} for alg in ALGORITHMS}
    
    if not os.path.exists(csv_path):
        print(f"⚠️ CSV文件不存在: {csv_path}")
        return None
    
    try:
        # 读取CSV文件
        data = pd.read_csv(csv_path)
        
        # 按算法分组提取数据
        for alg in ALGORITHMS:
            alg_data = data[data['Algorithm'] == alg]
            metrics[alg]['hv'] = alg_data['HV'].tolist()
            metrics[alg]['igd'] = alg_data['IGD'].tolist()
        
        print(f"✅ 成功从CSV文件读取指标数据")
        return metrics
    
    except Exception as e:
        print(f"❌ 读取CSV文件失败: {e}")
        return None


def read_metrics_from_report(report_path):
    """从对比报告中读取指标数据（备用方法）"""
    metrics = {alg: {'hv': [], 'igd': []} for alg in ALGORITHMS}
    
    if not os.path.exists(report_path):
        print(f"⚠️ 报告文件不存在: {report_path}")
        return metrics
    
    try:
        with open(report_path, 'r', encoding='utf-8') as f:
            content = f.read()
        
        # 查找详细数据部分
        detail_section = content.split('详细数据')[1] if '详细数据' in content else ''
        
        for alg in ALGORITHMS:
            # 提取HV值
            if f'【{alg}】' in detail_section:
                alg_section = detail_section.split(f'【{alg}】')[1].split('【')[0]
                
                # 提取HV值
                if 'HV值:' in alg_section:
                    hv_section = alg_section.split('HV值:')[1].split('IGD值:')[0]
                    hv_values = []
                    for line in hv_section.split('\n'):
                        if '运行' in line and ':' in line:
                            try:
                                value = float(line.split(':')[1].strip())
                                hv_values.append(value)
                            except:
                                pass
                    metrics[alg]['hv'] = hv_values
                
                # 提取IGD值
                if 'IGD值:' in alg_section:
                    igd_section = alg_section.split('IGD值:')[1].split('\n\n')[0]
                    igd_values = []
                    for line in igd_section.split('\n'):
                        if '运行' in line and ':' in line:
                            try:
                                value = float(line.split(':')[1].strip())
                                igd_values.append(value)
                            except:
                                pass
                    metrics[alg]['igd'] = igd_values
        
        return metrics
    
    except Exception as e:
        print(f"❌ 读取报告失败: {e}")
        return metrics


def plot_pareto_fronts(exp_dir, instance_name):
    """绘制Pareto前沿对比图"""
    print("\n🎨 绘制Pareto前沿对比图...")
    
    fig, ax = plt.subplots(figsize=(10, 7))
    
    has_data = False
    
    for alg in ALGORITHMS:
        file_path = os.path.join(exp_dir, alg, 'AlgorithmParetoFront.txt')
        cmax, energy = read_pareto_front(file_path)
        
        if cmax is not None and energy is not None:
            ax.scatter(cmax, energy, 
                      c=COLORS[alg], 
                      marker=MARKERS[alg], 
                      label=ALGORITHM_NAMES[alg],
                      s=80, 
                      alpha=0.7,
                      edgecolors='black',
                      linewidths=0.5)
            has_data = True
    
    if not has_data:
        print("⚠️ 没有找到任何Pareto前沿数据")
        plt.close()
        return
    
    ax.set_xlabel('最大完工时间 (Cmax)', fontsize=13, fontweight='bold')
    ax.set_ylabel('总能耗', fontsize=13, fontweight='bold')
    ax.set_title(f'帕累托前沿对比图 - {instance_name}', fontsize=15, fontweight='bold')
    ax.legend(fontsize=11, loc='upper right')
    ax.grid(True, alpha=0.3, linestyle='--')
    
    plt.tight_layout()
    output_file = os.path.join(exp_dir, 'pareto_front_comparison.png')
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ Pareto前沿对比图已保存: {output_file}")
    plt.close()


def plot_hv_boxplot(metrics, exp_dir, instance_name):
    """绘制HV箱线图"""
    print("\n📊 绘制HV对比箱线图...")
    
    # 准备数据
    data = []
    for alg in ALGORITHMS:
        if metrics[alg]['hv']:
            for hv in metrics[alg]['hv']:
                data.append({
                    'Algorithm': ALGORITHM_NAMES[alg],
                    'HV': hv
                })
    
    if not data:
        print("⚠️ 没有找到HV数据")
        return
    
    df = pd.DataFrame(data)
    
    # 绘制箱线图
    fig, ax = plt.subplots(figsize=(10, 6))
    
    # 使用seaborn绘制箱线图（不显示离群点）
    sns.boxplot(x='Algorithm', y='HV', data=df, ax=ax,
                palette=[COLORS[alg] for alg in ALGORITHMS],
                width=0.6, showfliers=False)
    
    ax.set_xlabel('算法', fontsize=13, fontweight='bold')
    ax.set_ylabel('Hypervolume (HV)', fontsize=13, fontweight='bold')
    ax.set_title(f'HV对比图 - {instance_name}', fontsize=15, fontweight='bold')
    ax.grid(True, alpha=0.3, axis='y', linestyle='--')
    
    plt.tight_layout()
    output_file = os.path.join(exp_dir, 'hv_comparison.png')
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ HV对比箱线图已保存: {output_file}")
    plt.close()


def plot_igd_boxplot(metrics, exp_dir, instance_name):
    """绘制IGD箱线图"""
    print("\n📊 绘制IGD对比箱线图...")
    
    # 准备数据
    data = []
    for alg in ALGORITHMS:
        if metrics[alg]['igd']:
            for igd in metrics[alg]['igd']:
                data.append({
                    'Algorithm': ALGORITHM_NAMES[alg],
                    'IGD': igd
                })
    
    if not data:
        print("⚠️ 没有找到IGD数据")
        return
    
    df = pd.DataFrame(data)
    
    # 绘制箱线图
    fig, ax = plt.subplots(figsize=(10, 6))
    
    # 使用seaborn绘制箱线图（不显示离群点）
    sns.boxplot(x='Algorithm', y='IGD', data=df, ax=ax,
                palette=[COLORS[alg] for alg in ALGORITHMS],
                width=0.6, showfliers=False)
    
    ax.set_xlabel('算法', fontsize=13, fontweight='bold')
    ax.set_ylabel('Inverted Generational Distance (IGD)', fontsize=13, fontweight='bold')
    ax.set_title(f'IGD对比图 - {instance_name}', fontsize=15, fontweight='bold')
    ax.grid(True, alpha=0.3, axis='y', linestyle='--')
    
    plt.tight_layout()
    output_file = os.path.join(exp_dir, 'igd_comparison.png')
    plt.savefig(output_file, dpi=300, bbox_inches='tight')
    print(f"✅ IGD对比箱线图已保存: {output_file}")
    plt.close()


def statistical_test(metrics, exp_dir):
    """进行统计显著性检验"""
    print("\n📈 进行统计显著性检验...")
    
    report_file = os.path.join(exp_dir, 'statistical_test_report.txt')
    
    with open(report_file, 'w', encoding='utf-8') as f:
        f.write("=" * 80 + "\n")
        f.write("统计显著性检验报告\n")
        f.write("=" * 80 + "\n\n")
        f.write("检验方法: Wilcoxon秩和检验 (成对比较)\n")
        f.write("显著性水平: α = 0.05\n\n")
        
        # HV检验
        f.write("-" * 80 + "\n")
        f.write("Hypervolume (HV) 检验结果\n")
        f.write("-" * 80 + "\n\n")
        
        for i, alg1 in enumerate(ALGORITHMS):
            for alg2 in ALGORITHMS[i+1:]:
                if metrics[alg1]['hv'] and metrics[alg2]['hv']:
                    try:
                        statistic, p_value = stats.wilcoxon(
                            metrics[alg1]['hv'], 
                            metrics[alg2]['hv']
                        )
                        
                        f.write(f"{ALGORITHM_NAMES[alg1]} vs {ALGORITHM_NAMES[alg2]}:\n")
                        f.write(f"  p-value = {p_value:.6f}\n")
                        
                        if p_value < 0.01:
                            f.write(f"  结论: 差异极显著 (p < 0.01) **\n")
                        elif p_value < 0.05:
                            f.write(f"  结论: 差异显著 (p < 0.05) *\n")
                        else:
                            f.write(f"  结论: 差异不显著 (p ≥ 0.05)\n")
                        
                        # 判断哪个更好
                        mean1 = np.mean(metrics[alg1]['hv'])
                        mean2 = np.mean(metrics[alg2]['hv'])
                        if mean1 > mean2:
                            f.write(f"  {ALGORITHM_NAMES[alg1]} 优于 {ALGORITHM_NAMES[alg2]}\n")
                        else:
                            f.write(f"  {ALGORITHM_NAMES[alg2]} 优于 {ALGORITHM_NAMES[alg1]}\n")
                        
                        f.write("\n")
                    except Exception as e:
                        f.write(f"{ALGORITHM_NAMES[alg1]} vs {ALGORITHM_NAMES[alg2]}: 检验失败 ({e})\n\n")
        
        # IGD检验
        f.write("-" * 80 + "\n")
        f.write("IGD 检验结果\n")
        f.write("-" * 80 + "\n\n")
        
        for i, alg1 in enumerate(ALGORITHMS):
            for alg2 in ALGORITHMS[i+1:]:
                if metrics[alg1]['igd'] and metrics[alg2]['igd']:
                    try:
                        statistic, p_value = stats.wilcoxon(
                            metrics[alg1]['igd'], 
                            metrics[alg2]['igd']
                        )
                        
                        f.write(f"{ALGORITHM_NAMES[alg1]} vs {ALGORITHM_NAMES[alg2]}:\n")
                        f.write(f"  p-value = {p_value:.6f}\n")
                        
                        if p_value < 0.01:
                            f.write(f"  结论: 差异极显著 (p < 0.01) **\n")
                        elif p_value < 0.05:
                            f.write(f"  结论: 差异显著 (p < 0.05) *\n")
                        else:
                            f.write(f"  结论: 差异不显著 (p ≥ 0.05)\n")
                        
                        # 判断哪个更好（IGD越小越好）
                        mean1 = np.mean(metrics[alg1]['igd'])
                        mean2 = np.mean(metrics[alg2]['igd'])
                        if mean1 < mean2:
                            f.write(f"  {ALGORITHM_NAMES[alg1]} 优于 {ALGORITHM_NAMES[alg2]}\n")
                        else:
                            f.write(f"  {ALGORITHM_NAMES[alg2]} 优于 {ALGORITHM_NAMES[alg1]}\n")
                        
                        f.write("\n")
                    except Exception as e:
                        f.write(f"{ALGORITHM_NAMES[alg1]} vs {ALGORITHM_NAMES[alg2]}: 检验失败 ({e})\n\n")
        
        f.write("=" * 80 + "\n")
        f.write("报告结束\n")
        f.write("=" * 80 + "\n")
    
    print(f"✅ 统计检验报告已保存: {report_file}")


def process_one_experiment(exp_dir, instance_name):
    """对单次实验目录生成全部对比图与报告。返回是否成功。"""
    csv_path = os.path.join(exp_dir, 'MetricsData.csv')
    metrics = read_metrics_from_csv(csv_path)
    if metrics is None:
        report_path = os.path.join(exp_dir, 'ComparisonReport.txt')
        metrics = read_metrics_from_report(report_path)
    
    plot_pareto_fronts(exp_dir, instance_name)
    if any(metrics[alg]['hv'] for alg in ALGORITHMS):
        plot_hv_boxplot(metrics, exp_dir, instance_name)
    if any(metrics[alg]['igd'] for alg in ALGORITHMS):
        plot_igd_boxplot(metrics, exp_dir, instance_name)
    if any(metrics[alg]['hv'] for alg in ALGORITHMS):
        statistical_test(metrics, exp_dir)
    return True


def main():
    """主函数：自行搜索全部对比实验结果目录，逐个生成图像与报告。"""
    print("\n" + "=" * 80)
    print("算法对比实验结果可视化工具")
    print("=" * 80)
    
    print("\n🔍 搜索全部对比实验结果...")
    experiments = find_all_experiments()
    
    if not experiments:
        print("\n❌ 未找到任何实验结果，请先运行对比实验。")
        return
    
    base_dir = get_experiment_base_dir()
    print(f"✅ 在 {base_dir} 下共找到 {len(experiments)} 个实验目录\n")
    
    for i, (exp_dir, instance_name) in enumerate(experiments, 1):
        print("\n" + "-" * 60)
        print(f"[{i}/{len(experiments)}] 算例: {instance_name}")
        print(f"    目录: {exp_dir}")
        try:
            process_one_experiment(exp_dir, instance_name)
            print(f"    ✅ 已生成图像与报告")
        except Exception as e:
            print(f"    ❌ 处理失败: {e}")
    
    print("\n" + "=" * 80)
    print("✅ 全部可视化完成！")
    print("=" * 80)
    print(f"\n共处理 {len(experiments)} 个实验，每个实验目录下生成:")
    print("  - pareto_front_comparison.png  (Pareto前沿对比图)")
    print("  - hv_comparison.png            (HV对比箱线图)")
    print("  - igd_comparison.png           (IGD对比箱线图)")
    print("  - statistical_test_report.txt  (统计检验报告)")
    print()


if __name__ == '__main__':
    main()
