# -*- coding: utf-8 -*-
"""
消融实验雷达图绘制脚本（第二章）
数据：平均值与最好值，对比 HGA / -LS / -IS
字体：中文宋体18号，英文 Times New Roman 18号
"""

import os
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

# 设置中英文字体：中文宋体 18 号，英文 Times New Roman 18 号
matplotlib.rcParams['font.size'] = 18
matplotlib.rcParams['font.sans-serif'] = ['Times New Roman', 'SimSun', 'SimHei']
matplotlib.rcParams['axes.unicode_minus'] = False

instances = ['C5', 'C10', 'C15', 'C20']

# 平均值 (Average)
avg_data = {
    'HGA': [32113, 39362, 49625, 49826],
    '-LS': [32113, 40711, 52178, 54431],
    '-IS': [32115, 39502, 50480, 56456],
}

# 最好值 (Best)
best_data = {
    'HGA': [32113, 39181, 47829, 47931],
    '-LS': [32113, 39332, 50868, 52112],
    '-IS': [32113, 39354, 48311, 54179],
}


def normalize_for_radar(values_per_instance, r_min=0.15, r_max=1.0):
    """
    按实例（每根轴）做 min-max 归一化，并映射到 [r_min, r_max]。
    目标为 makespan 越小越好，故 (max-x)/(max-min) 越大越好。
    保证归一化后无 0 值，避免顶点落在圆心，使每条线都呈多边形。
    """
    arr = np.array(values_per_instance)
    n_inst = arr.shape[1]
    out = np.zeros_like(arr, dtype=float)
    for j in range(n_inst):
        col = arr[:, j]
        vmin, vmax = col.min(), col.max()
        if vmax > vmin:
            t = (vmax - col) / (vmax - vmin)  # t in [0, 1]
            out[:, j] = r_min + (r_max - r_min) * t
        else:
            out[:, j] = r_max
    return out


def plot_radar(instances, data_dict, title, filename, colors=None):
    """
    绘制一张雷达图。
    - 每个轴为一个算例（实例）
    - 每条线为一个算法（HGA, -LS, -IS）
    """
    algorithms = list(data_dict.keys())
    matrix = np.array([data_dict[alg] for alg in algorithms])
    normalized = normalize_for_radar(matrix)

    n_inst = len(instances)
    angles = np.linspace(0, 2 * np.pi, n_inst, endpoint=False).tolist()
    angles += angles[:1]  # 闭合

    fig, ax = plt.subplots(figsize=(8, 8), subplot_kw=dict(projection='polar'))

    if colors is None:
        colors = ['#1f77b4', '#ff7f0e', '#2ca02c']

    for i, alg in enumerate(algorithms):
        values = normalized[i].tolist()
        values += values[:1]
        ax.plot(angles, values, 'o-', linewidth=2, label=alg, color=colors[i])
        ax.fill(angles, values, alpha=0.15, color=colors[i])

    ax.set_xticks(angles[:-1])
    ax.set_xticklabels(instances, fontsize=18)
    ax.set_ylim(0.1, 1.05)
    ax.set_yticks([0.2, 0.4, 0.6, 0.8, 1.0])
    ax.set_yticklabels(['0.2', '0.4', '0.6', '0.8', '1.0'], fontsize=16)
    ax.set_title(title, fontsize=20, pad=20)
    ax.legend(loc='upper right', bbox_to_anchor=(1.35, 1.0), fontsize=16)
    ax.grid(True)
    plt.tight_layout()
    plt.savefig(filename, dpi=150, bbox_inches='tight')
    plt.close()
    print(f'已保存: {filename}')


if __name__ == '__main__':
    out_dir = os.path.dirname(os.path.abspath(__file__))
    os.makedirs(out_dir, exist_ok=True)

    # 平均值雷达图
    plot_radar(
        instances,
        avg_data,
        title='消融实验雷达图（平均值）',
        filename=os.path.join(out_dir, 'ablation_radar_average.png'),
    )
    # 最好值雷达图
    plot_radar(
        instances,
        best_data,
        title='消融实验雷达图（最好值）',
        filename=os.path.join(out_dir, 'ablation_radar_best.png'),
    )
