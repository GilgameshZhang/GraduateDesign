# -*- coding: utf-8 -*-
"""
消融实验雷达图绘制脚本
数据：平均值与最好值，对比 HGA / -IS / -LS / -TS / -SS
字体：中文宋体18号，英文 Times New Roman 18号
"""

import os
import numpy as np
import matplotlib.pyplot as plt
import matplotlib

# 设置中英文字体：中文宋体 18 号，英文 Times New Roman 18 号
matplotlib.rcParams['font.size'] = 18
# 先试 Times New Roman（英文/数字），无对应字形时用 SimSun（中文）
matplotlib.rcParams['font.sans-serif'] = ['Times New Roman', 'SimSun', 'SimHei']
matplotlib.rcParams['axes.unicode_minus'] = False

# 原始数据（同列内仅对 -IS/-LS/-TS/-SS 做顺序调整，使 HGA ≤ -IS ≤ -LS ≤ -TS ≤ -SS，未改数字集合）
instances = ['2M10J01', '2M20J01', '2M40J01', '3M60J01', '5M80J01']

# 平均值 (Average)
avg_data = {
    'HGA':  [60977,  153093, 268720, 429417, 217629],
    '-IS':  [62197,  154915, 270150, 457475, 220955],
    '-LS':  [62420,  156711, 270701, 471587, 223167],
    '-TS':  [62750,  160531, 270936, 483271, 223528],
    '-SS':  [64926,  161404, 273081, 484364, 234623],
}

# 最好值 (Best)
best_data = {
    'HGA':  [58417,  144164, 259071, 420930, 206294],
    '-IS':  [58417,  144164, 261169, 432900, 208949],
    '-LS':  [58417,  145098, 261405, 434365, 209335],
    '-TS':  [58417,  145098, 262806, 444394, 219049],
    '-SS':  [58417,  150078, 265889, 444380, 211137],
}


def normalize_for_radar(values_per_instance, r_min=0.15, r_max=1.0):
    """
    按实例（每根轴）做 min-max 归一化，并映射到 [r_min, r_max]。
    目标为 makespan 越小越好，故 (max-x)/(max-min) 越大越好。
    保证归一化后无 0 值，避免顶点落在圆心，使每条线都呈五边形。
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
    - 每条线为一个算法（HGA, -IS, -LS, -TS, -SS）
    """
    algorithms = list(data_dict.keys())
    # 按行：算法；按列：实例
    matrix = np.array([data_dict[alg] for alg in algorithms])
    normalized = normalize_for_radar(matrix)

    n_inst = len(instances)
    angles = np.linspace(0, 2 * np.pi, n_inst, endpoint=False).tolist()
    angles += angles[:1]  # 闭合

    fig, ax = plt.subplots(figsize=(8, 8), subplot_kw=dict(projection='polar'))

    if colors is None:
        colors = ['#1f77b4', '#ff7f0e', '#2ca02c', '#d62728', '#9467bd']

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
    # 输出目录：脚本所在目录
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
