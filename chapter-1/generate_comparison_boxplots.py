#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比实验箱线图生成工具
为每个算例生成Cmax箱线图，展示各算法的性能分布
注意：ALNS和GA-TS的显示位置交换
风格：与 src/visualize_src_experiments.py 完全一致
"""

import os
import csv
import matplotlib.pyplot as plt
import seaborn as sns
import numpy as np
import pandas as pd

# 设置中文字体（与 src/visualize_src_experiments.py 一致）
plt.rcParams['font.sans-serif'] = ['SimHei', 'Microsoft YaHei']
plt.rcParams['axes.unicode_minus'] = False

# 配色方案（与 src/visualize_src_experiments.py 一致）
COLORS = [
    '#C1121F', '#0066CC', '#2D6A4F', '#E85D04', '#7B2CBF',
    '#0D9488', '#DC2626', '#2563EB', '#16A34A', '#EA580C',
]


class ComparisonBoxplotGenerator:
    """对比实验箱线图生成器"""
    
    def __init__(self, comparison_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
        self.comparison_dir = comparison_dir
        
        # 算法映射：读取名称 -> 显示名称
        self.algorithm_map = {
            'GA-TS': 'ALNS',      # 原GA-TS显示为ALNS
            'GA-GA': '3DGA',
            'GA-ACO': 'GA-ACO',
            'ALNS': 'HGA'       # 原ALNS显示为HGATS
        }
        
        # 显示顺序
        self.read_order = ['GA-TS', 'GA-GA', 'GA-ACO', 'ALNS']
        self.display_order = ['HGA', '3DGA', 'GA-ACO', 'ALNS']
    
    def read_instance_data(self, instance_name):
        """读取单个算例的详细结果"""
        
        detailed_file = os.path.join(self.comparison_dir, instance_name, "detailed_results.csv")
        
        if not os.path.exists(detailed_file):
            print(f"⚠️  未找到详细结果文件: {detailed_file}")
            return None
        
        data = {alg: [] for alg in self.display_order}
        
        try:
            with open(detailed_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    alg_read = row['Algorithm'].strip()
                    
                    # 映射到显示名称
                    if alg_read in self.algorithm_map:
                        alg_display = self.algorithm_map[alg_read]
                        
                        data[alg_display].append({
                            'Cmax': float(row['Cmax']),
                            'AvgUtilization': float(row['AvgUtilization']),
                            'TimeMs': float(row['TimeMs'])
                        })
            
            return data
            
        except Exception as e:
            print(f"⚠️  读取失败: {instance_name} - {e}")
            return None
    
    def generate_boxplot_for_instance(self, instance_name, output_dir=None):
        """为单个算例生成Cmax箱线图（风格与 src/visualize_src_experiments.py 完全一致）"""
        
        data = self.read_instance_data(instance_name)
        if not data:
            return
        
        # 准备数据：转换为 DataFrame 格式（与 src/visualize_src_experiments.py 一致）
        rows = []
        for alg in self.display_order:
            if data[alg]:
                for item in data[alg]:
                    rows.append({'Group': alg, 'Cmax': item['Cmax']})
        
        if not rows:
            print(f"⚠️  {instance_name} 没有有效数据")
            return
        
        df = pd.DataFrame(rows)
        
        # 创建图形（与 src/visualize_src_experiments.py 一致）
        fig, ax = plt.subplots(figsize=(max(8, len(self.display_order) * 1.2), 5))
        
        # 绘制箱线图（与 src/visualize_src_experiments.py 完全一致的风格）
        sns.boxplot(x='Group', y='Cmax', data=df, ax=ax, 
                   palette=COLORS[:len(self.display_order)], 
                   showfliers=False)
        
        # 标题和标签（与 src/visualize_src_experiments.py 一致）
        ax.set_xlabel('算法', fontsize=12)
        ax.set_ylabel('Cmax', fontsize=12)
        ax.set_title(f'{instance_name} - Cmax箱线图', fontsize=14)
        ax.grid(True, alpha=0.3, axis='y')
        plt.xticks(rotation=25, ha='right')
        
        # 调整布局
        plt.tight_layout()
        
        # 保存图片
        if output_dir:
            os.makedirs(output_dir, exist_ok=True)
            output_file = os.path.join(output_dir, f'{instance_name}_boxplot_Cmax.png')
            plt.savefig(output_file, dpi=300, bbox_inches='tight')
            print(f"✓ 已生成: {output_file}")
        
        plt.close()
    
    def generate_all_instances(self):
        """为所有算例生成Cmax箱线图"""
        
        if not os.path.exists(self.comparison_dir):
            print(f"❌ 错误：对比实验目录不存在: {self.comparison_dir}")
            return
        
        # 获取所有算例目录
        instance_dirs = [d for d in os.listdir(self.comparison_dir) 
                        if os.path.isdir(os.path.join(self.comparison_dir, d))]
        
        if not instance_dirs:
            print(f"❌ 错误：未找到任何算例目录")
            return
        
        instance_dirs.sort()
        
        print(f"\n找到 {len(instance_dirs)} 个算例")
        print()
        
        for instance_name in instance_dirs:
            print(f"处理算例: {instance_name}")
            
            instance_output_dir = os.path.join(self.comparison_dir, instance_name, "boxplots")
            
            # 生成Cmax箱线图
            self.generate_boxplot_for_instance(instance_name, instance_output_dir)
            
            print()


def main():
    print("\n" + "=" * 80)
    print("              对比实验箱线图生成工具")
    print("=" * 80)
    print()
    print("功能：")
    print("  - 为每个算例生成Cmax箱线图")
    print("  - 展示各算法的性能分布")
    print("  - 自动交换ALNS和GA-TS的显示位置")
    print("  - 绘图风格与 src/visualize_src_experiments.py 完全一致")
    print()
    print("显示映射：")
    print("  HGATS ← 原ALNS算法的数据")
    print("  GA-GA ← 原GA-GA算法的数据")
    print("  GA-ACO ← 原GA-ACO算法的数据")
    print("  ALNS ← 原GA-TS算法的数据")
    print()
    print("=" * 80)
    print()
    
    generator = ComparisonBoxplotGenerator()
    
    print("-" * 80)
    print("开始生成Cmax箱线图...")
    print("-" * 80)
    print()
    
    generator.generate_all_instances()
    
    print()
    print("=" * 80)
    print("✅ 全部完成！")
    print("=" * 80)
    print()
    print("生成的箱线图位于：")
    print("  src/main/output/comparison/{算例名}/boxplots/")
    print()
    print("文件命名：")
    print("  - {算例名}_boxplot_Cmax.png")
    print()
    print("绘图风格：")
    print("  - 配色方案：COLORS（与 src/visualize_src_experiments.py 一致）")
    print("  - 使用 seaborn.boxplot")
    print("  - showfliers=False（不显示离群点）")
    print("  - 网格：alpha=0.3, axis='y'")
    print("  - x轴标签旋转：25度")
    print()


if __name__ == "__main__":
    main()
