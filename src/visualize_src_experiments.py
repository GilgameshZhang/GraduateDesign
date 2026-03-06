#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Article/src 下实验结果可视化：自动搜索除田口实验外的全部对比/消融实验，
为每个算例生成箱线图与迭代曲线对比图。

排除：田口实验
包含：邻域消融实验、消融实验、对比试验/对比实验

使用方法（在 Article 目录下）：
  python src/visualize_src_experiments.py
或在 src 目录下：
  python visualize_src_experiments.py

迭代曲线横轴为时间：实验时间固定 180s，每条 run 的代数按「时间=代数×(180/该run最大代数)」映射到 [0,180]，可在脚本内修改 EXPERIMENT_TIME_SEC。
"""

import os
import re
import glob
from collections import defaultdict

import numpy as np
import pandas as pd
import matplotlib.pyplot as plt
import seaborn as sns

# 脚本所在目录 = Article/src，result 在 src/main/resources/result
SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
RESULT_BASE = os.path.join(SCRIPT_DIR, 'main', 'resources', 'result')

# 排除的实验类型
SKIP_EXPERIMENTS = {'田口实验'}

# 每次实验固定运行时间（秒），迭代图横轴 0～该值。代数按 run 内最大代数线性映射到 [0, 实验时间]。
EXPERIMENT_TIME_SEC = 180

plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

# 配色：各组差别鲜明
COLORS = [
    '#C1121F', '#0066CC', '#2D6A4F', '#E85D04', '#7B2CBF',
    '#0D9488', '#DC2626', '#2563EB', '#16A34A', '#EA580C',
]


def get_result_base():
    """返回 result 根目录"""
    return RESULT_BASE


def list_experiment_types():
    """列出 result 下所有实验类型（排除田口实验）"""
    base = get_result_base()
    if not os.path.isdir(base):
        return []
    types = [
        d for d in os.listdir(base)
        if os.path.isdir(os.path.join(base, d)) and d not in SKIP_EXPERIMENTS
    ]
    return types


def parse_report(report_path):
    """
    解析 实验报告.txt，提取：
    - final_makespan: 最后一次出现的 最优Makespan
    - iteration_series: [(gen, makespan), ...] 按代数顺序
    """
    if not os.path.isfile(report_path):
        return None, None
    try:
        with open(report_path, 'r', encoding='utf-8') as f:
            content = f.read()
    except Exception:
        return None, None

    # 格式1：代数  N: 最优Makespan=xxx（邻域消融/消融实验）
    pattern1 = re.compile(r'代数\s+(\d+):\s*最优Makespan=([\d.]+)')
    # 格式2：Gen  N | 最优: xxx（对比试验-随机密钥GA）
    pattern2 = re.compile(r'Gen\s+(\d+)\s+\|\s+最优:\s+([\d.]+)')
    # 格式3：代数  N | 最优 Makespan: xxx（对比试验-PSO）
    pattern3 = re.compile(r'代数\s+(\d+)\s+\|\s+最优\s*Makespan:\s*([\d.]+)')
    series = []
    for g, m in pattern1.findall(content):
        series.append((int(g), float(m)))
    for g, m in pattern2.findall(content):
        series.append((int(g), float(m)))
    for g, m in pattern3.findall(content):
        series.append((int(g), float(m)))
    if not series:
        return None, None
    series.sort(key=lambda x: x[0])
    final_makespan = series[-1][1]
    return final_makespan, series


def collect_neighborhood_ablation(base_dir):
    """
    邻域消融实验：顶层为各 run 目录（完整算法_run*、无N1_run*、...），
    可能有 邻域消融实验总结_<instance>。按组收集每个 run 的 makespan 与迭代序列。
    返回: { instance_name: { group: { 'makespans': [], 'curves': [ [(g,m),...], ... ] } } }
    """
    out = defaultdict(lambda: defaultdict(lambda: {'makespans': [], 'curves': []}))
    instance = None
    summary_dirs = glob.glob(os.path.join(base_dir, '邻域消融实验总结_*'))
    if summary_dirs:
        # 取第一个总结目录名中的算例名
        name = os.path.basename(summary_dirs[0])
        if name.startswith('邻域消融实验总结_'):
            instance = name.replace('邻域消融实验总结_', '').strip()
    if not instance:
        instance = '邻域消融实验'

    # 组名前缀（与目录名匹配）
    run_dirs = [
        d for d in os.listdir(base_dir)
        if os.path.isdir(os.path.join(base_dir, d))
           and not d.startswith('邻域消融实验总结')
    ]
    for run_name in run_dirs:
        # 组名：去掉 _run数字_ 及后面部分
        parts = re.match(r'^(.+?)_run\d+_', run_name)
        group = parts.group(1) if parts else run_name
        report_path = os.path.join(base_dir, run_name, '实验报告.txt')
        final_m, series = parse_report(report_path)
        if final_m is not None:
            out[instance][group]['makespans'].append(final_m)
        if series:
            out[instance][group]['curves'].append(series)
    return dict(out)


def collect_ablation_by_instance(exp_base, sub_key):
    """
    消融实验：结构为 消融实验/J10/消融实验总结_J10P3B2D10_01/<run_dirs>。
    返回: { instance_name: { group: { 'makespans': [], 'curves': [] } } }
    """
    out = defaultdict(lambda: defaultdict(lambda: {'makespans': [], 'curves': []}))
    # sub_key 如 'J10', 'J50'
    level_dir = os.path.join(exp_base, sub_key)
    if not os.path.isdir(level_dir):
        return dict(out)
    summary_dirs = glob.glob(os.path.join(level_dir, '消融实验总结_*'))
    for sum_dir in summary_dirs:
        name = os.path.basename(sum_dir)
        if not name.startswith('消融实验总结_'):
            continue
        instance = name.replace('消融实验总结_', '').strip()
        run_dirs = [
            d for d in os.listdir(sum_dir)
            if os.path.isdir(os.path.join(sum_dir, d))
               and not d.endswith('.png') and d != '消融实验报告.txt'
        ]
        for run_name in run_dirs:
            parts = re.match(r'^(.+?)_run\d+_', run_name)
            group = parts.group(1) if parts else run_name
            report_path = os.path.join(sum_dir, run_name, '实验报告.txt')
            if not os.path.isfile(report_path):
                continue
            final_m, series = parse_report(report_path)
            if final_m is not None:
                out[instance][group]['makespans'].append(final_m)
            if series:
                out[instance][group]['curves'].append(series)
    return dict(out)


def collect_comparison_experiment(base_dir):
    """
    对比试验/对比实验：结构为 <base_dir>/<算法名>/<算例名>/<算例名>_run*_实验*_*/实验报告.txt
    算法名包括：随机密钥GA、PSO 等（凡在该目录下的算法子目录均会参与对比）。
    返回: { instance_name: { algorithm: { 'makespans': [], 'curves': [] } } }
    """
    out = defaultdict(lambda: defaultdict(lambda: {'makespans': [], 'curves': []}))
    if not os.path.isdir(base_dir):
        return dict(out)
    # 算法子目录：随机密钥GA、PSO 等，全部参与对比
    algo_dirs = [
        d for d in os.listdir(base_dir)
        if os.path.isdir(os.path.join(base_dir, d))
    ]
    for algo in algo_dirs:
        algo_path = os.path.join(base_dir, algo)
        instance_dirs = [
            d for d in os.listdir(algo_path)
            if os.path.isdir(os.path.join(algo_path, d))
               and d != 'SimpleTest'
               and not d.endswith('.txt')
        ]
        for instance in instance_dirs:
            inst_path = os.path.join(algo_path, instance)
            run_dirs = [
                d for d in os.listdir(inst_path)
                if os.path.isdir(os.path.join(inst_path, d))
                   and os.path.isfile(os.path.join(inst_path, d, '实验报告.txt'))
            ]
            for run_name in run_dirs:
                report_path = os.path.join(inst_path, run_name, '实验报告.txt')
                final_m, series = parse_report(report_path)
                if final_m is not None:
                    out[instance][algo]['makespans'].append(final_m)
                if series:
                    out[instance][algo]['curves'].append(series)
    return dict(out)


def _curve_to_time_series(curve, total_sec):
    """单条 (gen, makespan) 序列按该 run 最大代数线性映射到 [0, total_sec]，返回 (time, makespan) 列表。"""
    if not curve:
        return []
    max_gen = max(g for g, _ in curve)
    if max_gen <= 0:
        return [(0, curve[0][1])]
    return [(g * total_sec / max_gen, m) for g, m in curve]


def build_mean_curve_in_time(curves_list, total_sec=EXPERIMENT_TIME_SEC):
    """
    多条 (gen, makespan) 序列：每条 run 代数不同，按 时间 = 代数×(total_sec/该run最大代数) 映射到 [0, total_sec]，
    在时间轴上对齐后取每时刻 best-so-far 的均值。返回 (times, mean_best) 用于绘图。
    """
    if not curves_list:
        return [], []
    # 时间网格（秒），步长 2s 足够平滑
    time_grid = np.arange(0, total_sec + 1, 2.0)
    time_grid = time_grid.tolist()
    if time_grid[-1] != total_sec:
        time_grid.append(total_sec)
    run_best = []
    for curve in curves_list:
        t2m = _curve_to_time_series(curve, total_sec)
        if not t2m:
            run_best.append([np.nan] * len(time_grid))
            continue
        # 按时间排序，并计算各时刻的 best-so-far
        t2m.sort(key=lambda x: x[0])
        best_so_far = None
        values = []
        idx = 0
        for t in time_grid:
            while idx < len(t2m) and t2m[idx][0] <= t:
                v = t2m[idx][1]
                best_so_far = v if best_so_far is None else min(best_so_far, v)
                idx += 1
            values.append(best_so_far if best_so_far is not None else np.nan)
        run_best.append(values)
    run_best = np.array(run_best)
    mean_best = np.nanmean(run_best, axis=0)
    return time_grid, mean_best.tolist()


def build_mean_curve(curves_list):
    """
    多条 (gen, makespan) 序列 -> 对齐到公共代数，取每代 best-so-far 的均值。
    （保留用于不需要时间轴的场景；迭代图已改用 build_mean_curve_in_time。）
    """
    if not curves_list:
        return [], []
    all_gens = set()
    for curve in curves_list:
        for g, _ in curve:
            all_gens.add(g)
    gens = sorted(all_gens)
    run_best = []
    for curve in curves_list:
        g2m = dict(curve)
        best_so_far = None
        values = []
        for g in gens:
            if g in g2m:
                v = g2m[g]
                best_so_far = v if best_so_far is None else min(best_so_far, v)
            values.append(best_so_far if best_so_far is not None else np.nan)
        run_best.append(values)
    run_best = np.array(run_best)
    mean_best = np.nanmean(run_best, axis=0)
    return gens, mean_best.tolist()


def plot_boxplot(instance_name, group_data, output_path, title_suffix=''):
    """group_data: { group_name: [makespan1, makespan2, ...] }"""
    rows = []
    for group, values in group_data.items():
        for v in values:
            rows.append({'Group': group, 'Makespan': v})
    if not rows:
        return
    df = pd.DataFrame(rows)
    fig, ax = plt.subplots(figsize=(max(8, len(group_data) * 1.2), 5))
    sns.boxplot(x='Group', y='Makespan', data=df, ax=ax, palette=COLORS[:len(group_data)], showfliers=False)
    ax.set_xlabel('实验组', fontsize=12)
    ax.set_ylabel('Makespan', fontsize=12)
    ax.set_title(f'{instance_name} - 箱线图{title_suffix}', fontsize=14)
    ax.grid(True, alpha=0.3, axis='y')
    plt.xticks(rotation=25, ha='right')
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"  箱线图已保存: {output_path}")


def plot_iteration_curves(instance_name, group_curves, output_path, title_suffix=''):
    """
    group_curves: { group_name: [ [(g,m),...], [(g,m),...], ... ] }
    每条 run 代数不同，按「实验时间固定 180s」将代数映射到时间：时间 = 代数 × (180/该run最大代数)，
    在时间轴上对齐后绘制组内均值曲线。
    """
    fig, ax = plt.subplots(figsize=(10, 6))
    for i, (group, curves_list) in enumerate(group_curves.items()):
        if not curves_list:
            continue
        times, mean_best = build_mean_curve_in_time(curves_list, EXPERIMENT_TIME_SEC)
        if not times:
            continue
        color = COLORS[i % len(COLORS)]
        ax.plot(times, mean_best, label=group, color=color, linewidth=2)
    ax.set_xlabel('时间 (s)', fontsize=12)
    ax.set_ylabel('最优 Makespan（均值）', fontsize=12)
    ax.set_title(f'{instance_name} - 迭代曲线对比{title_suffix}', fontsize=14)
    ax.legend(loc='best', fontsize=9)
    ax.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(output_path, dpi=300, bbox_inches='tight')
    plt.close()
    print(f"  迭代曲线图已保存: {output_path}")


def process_neighborhood_ablation():
    """处理 邻域消融实验"""
    base = os.path.join(get_result_base(), '邻域消融实验')
    if not os.path.isdir(base):
        return
    print("\n【邻域消融实验】")
    data = collect_neighborhood_ablation(base)
    for instance, groups in data.items():
        group_makespans = {g: info['makespans'] for g, info in groups.items() if info['makespans']}
        group_curves = {g: info['curves'] for g, info in groups.items() if info['curves']}
        if not group_makespans and not group_curves:
            continue
        out_dir = os.path.join(base, f'邻域消融实验总结_{instance}')
        if not os.path.isdir(out_dir):
            out_dir = base
        if group_makespans:
            plot_boxplot(
                instance, group_makespans,
                os.path.join(out_dir, f'邻域消融_箱线图_{instance}.png'),
                title_suffix='（邻域消融）'
            )
        if group_curves:
            plot_iteration_curves(
                instance, group_curves,
                os.path.join(out_dir, f'邻域消融_迭代曲线对比_{instance}.png'),
                title_suffix='（邻域消融）'
            )


def process_ablation():
    """处理 消融实验（J10、J50 等按规模/算例汇总）"""
    base = os.path.join(get_result_base(), '消融实验')
    if not os.path.isdir(base):
        return
    print("\n【消融实验】")
    subdirs = [d for d in os.listdir(base) if os.path.isdir(os.path.join(base, d))]
    for sub_key in subdirs:
        data = collect_ablation_by_instance(base, sub_key)
        for instance, groups in data.items():
            group_makespans = {g: info['makespans'] for g, info in groups.items() if info['makespans']}
            group_curves = {g: info['curves'] for g, info in groups.items() if info['curves']}
            if not group_makespans and not group_curves:
                continue
            out_dir = os.path.join(base, sub_key, f'消融实验总结_{instance}')
            if not os.path.isdir(out_dir):
                out_dir = os.path.join(base, sub_key)
            if group_makespans:
                plot_boxplot(
                    instance, group_makespans,
                    os.path.join(out_dir, f'消融_箱线图_{instance}.png'),
                    title_suffix='（消融）'
                )
            if group_curves:
                plot_iteration_curves(
                    instance, group_curves,
                    os.path.join(out_dir, f'消融_迭代曲线对比_{instance}.png'),
                    title_suffix='（消融）'
                )


def process_comparison_experiment():
    """处理对比试验/对比实验：按算例输出箱线图与迭代曲线对比图。"""
    base = get_result_base()
    for folder_name in ('对比试验', '对比实验'):
        comp_dir = os.path.join(base, folder_name)
        if not os.path.isdir(comp_dir):
            continue
        print(f"\n【{folder_name}】")
        data = collect_comparison_experiment(comp_dir)
        if not data:
            print(f"  未找到有效数据")
            continue
        out_dir = os.path.join(comp_dir, '输出')
        os.makedirs(out_dir, exist_ok=True)
        for instance, groups in data.items():
            group_makespans = {g: info['makespans'] for g, info in groups.items() if info['makespans']}
            group_curves = {g: info['curves'] for g, info in groups.items() if info['curves']}
            if not group_makespans and not group_curves:
                continue
            if group_makespans:
                plot_boxplot(
                    instance, group_makespans,
                    os.path.join(out_dir, f'对比_箱线图_{instance}.png'),
                    title_suffix='（对比实验）'
                )
            if group_curves:
                plot_iteration_curves(
                    instance, group_curves,
                    os.path.join(out_dir, f'对比_迭代曲线对比_{instance}.png'),
                    title_suffix='（对比实验）'
                )


def main():
    print("=" * 60)
    print("Article/src 实验结果可视化（排除田口实验）")
    print("=" * 60)
    base = get_result_base()
    if not os.path.isdir(base):
        print(f"未找到 result 目录: {base}")
        return
    types = list_experiment_types()
    print(f"将处理以下实验类型: {types}")

    process_neighborhood_ablation()
    process_ablation()
    process_comparison_experiment()

    print("\n" + "=" * 60)
    print("全部完成。邻域消融/消融/对比实验每个算例已生成：箱线图、迭代曲线对比图。")
    print("=" * 60)


if __name__ == '__main__':
    main()
