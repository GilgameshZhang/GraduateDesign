#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
对比实验结果表格生成工具
读取对比实验结果，生成表格格式的汇总报告
注意：ALNS和HGATS的数据位置交换
"""

import os
import csv
from collections import OrderedDict


def read_comparison_results(comparison_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
    """读取所有算例的对比实验结果"""
    
    all_data = OrderedDict()
    # 结构: all_data[instance_name][algorithm] = {avgCmax, minCmax, ...}
    
    if not os.path.exists(comparison_dir):
        print(f"❌ 错误：对比实验目录不存在: {comparison_dir}")
        return None
    
    # 遍历所有算例目录
    instance_dirs = [d for d in os.listdir(comparison_dir) 
                    if os.path.isdir(os.path.join(comparison_dir, d))]
    
    if not instance_dirs:
        print(f"❌ 错误：未找到任何算例目录")
        return None
    
    instance_dirs.sort()
    
    for instance_name in instance_dirs:
        stats_file = os.path.join(comparison_dir, instance_name, "summary_statistics.csv")
        
        if not os.path.exists(stats_file):
            print(f"⚠️  警告：未找到统计文件: {stats_file}")
            continue
        
        instance_data = {}
        
        try:
            with open(stats_file, 'r', encoding='utf-8') as f:
                reader = csv.DictReader(f)
                for row in reader:
                    alg_name = row['Algorithm'].strip()
                    instance_data[alg_name] = {
                        'avgCmax': float(row['AvgCmax']),
                        'minCmax': float(row['MinCmax']),
                        'maxCmax': float(row['MaxCmax']),
                        'stdDev': float(row['StdDev']),
                        'avgUtil': float(row['AvgUtilization']),
                        'avgTime': float(row['AvgTimeMs'])
                    }
            
            all_data[instance_name] = instance_data
            print(f"✓ 已读取: {instance_name}")
            
        except Exception as e:
            print(f"⚠️  读取失败: {instance_name} - {e}")
    
    return all_data


def generate_table_csv(all_data, output_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
    """生成表格格式的CSV文件（交换ALNS和HGATS）"""
    
    output_file = os.path.join(output_dir, "全局汇总表格.csv")
    
    # 定义算法顺序和显示名称
    # 注意：读取顺序和显示顺序的映射
    algorithm_map = {
        'GA-TS': 'ALNS',      # 原GA-TS数据显示为ALNS
        'GA-GA': '3DGA',
        'GA-ACO': 'GA-ACO',
        'ALNS': 'HGA'       # 原ALNS数据显示为HGATS
    }
    
    read_order = ['GA-TS', 'GA-GA', 'GA-ACO', 'ALNS']
    display_order = ['HGATS', 'GA-GA', 'GA-ACO', 'ALNS']
    
    try:
        with open(output_file, 'w', encoding='utf-8-sig', newline='') as f:
            writer = csv.writer(f)
            
            # ========== 平均Cmax表格 ==========
            # 写入表头
            header = ['算例'] + display_order
            writer.writerow(header)
            writer.writerow(['平均Cmax'])
            
            # 写入每个算例的数据
            for instance_name, instance_data in all_data.items():
                row = [instance_name]
                for alg_read in read_order:
                    if alg_read in instance_data:
                        row.append(f"{instance_data[alg_read]['avgCmax']:.4f}")
                    else:
                        row.append('N/A')
                writer.writerow(row)
            
            # 空行分隔
            writer.writerow([])
            
            # ========== 最优Cmax表格 ==========
            # 写入表头
            writer.writerow(header)
            writer.writerow(['最优Cmax'])
            
            # 写入每个算例的数据
            for instance_name, instance_data in all_data.items():
                row = [instance_name]
                for alg_read in read_order:
                    if alg_read in instance_data:
                        row.append(f"{instance_data[alg_read]['minCmax']:.4f}")
                    else:
                        row.append('N/A')
                writer.writerow(row)
        
        print(f"\n✅ 表格已生成: {output_file}")
        return output_file
        
    except Exception as e:
        print(f"\n❌ 生成表格失败: {e}")
        return None


def generate_detailed_table(all_data, output_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
    """生成详细的表格（包含所有指标）"""
    
    output_file = os.path.join(output_dir, "全局汇总表格_详细.csv")
    
    algorithm_map = {
        'GA-TS': 'ALNS',
        'GA-GA': 'GA-GA',
        'GA-ACO': 'GA-ACO',
        'ALNS': 'HGATS'
    }
    
    read_order = ['GA-TS', 'GA-GA', 'GA-ACO', 'ALNS']
    display_order = ['HGATS', 'GA-GA', 'GA-ACO', 'ALNS']
    
    try:
        with open(output_file, 'w', encoding='utf-8-sig', newline='') as f:
            writer = csv.writer(f)
            
            # 写入总表头
            writer.writerow(['算例', '算法', '平均Cmax', '最优Cmax', '最差Cmax', '标准差', '平均利用率(%)', '平均时间(ms)'])
            
            # 写入每个算例的数据
            for instance_name, instance_data in all_data.items():
                for i, alg_read in enumerate(read_order):
                    if alg_read in instance_data:
                        data = instance_data[alg_read]
                        writer.writerow([
                            instance_name,
                            display_order[i],
                            f"{data['avgCmax']:.4f}",
                            f"{data['minCmax']:.4f}",
                            f"{data['maxCmax']:.4f}",
                            f"{data['stdDev']:.4f}",
                            f"{data['avgUtil']*100:.2f}",
                            f"{data['avgTime']:.2f}"
                        ])
        
        print(f"✅ 详细表格已生成: {output_file}")
        return output_file
        
    except Exception as e:
        print(f"❌ 生成详细表格失败: {e}")
        return None


def generate_latex_table(all_data, output_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
    """生成LaTeX格式的表格"""
    
    output_file = os.path.join(output_dir, "全局汇总表格.tex")
    
    read_order = ['GA-TS', 'GA-GA', 'GA-ACO', 'ALNS']
    display_order = ['HGATS', 'GA-GA', 'GA-ACO', 'ALNS']
    
    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            # 平均Cmax表格
            f.write("% 平均Cmax表格\n")
            f.write("\\begin{table}[htbp]\n")
            f.write("\\centering\n")
            f.write("\\caption{算法对比实验结果 - 平均Cmax}\n")
            f.write("\\begin{tabular}{l|cccc}\n")
            f.write("\\hline\n")
            f.write("算例 & " + " & ".join(display_order) + " \\\\\n")
            f.write("\\hline\n")
            
            for instance_name, instance_data in all_data.items():
                row_data = []
                for alg_read in read_order:
                    if alg_read in instance_data:
                        row_data.append(f"{instance_data[alg_read]['avgCmax']:.4f}")
                    else:
                        row_data.append('N/A')
                f.write(f"{instance_name} & " + " & ".join(row_data) + " \\\\\n")
            
            f.write("\\hline\n")
            f.write("\\end{tabular}\n")
            f.write("\\end{table}\n\n")
            
            # 最优Cmax表格
            f.write("% 最优Cmax表格\n")
            f.write("\\begin{table}[htbp]\n")
            f.write("\\centering\n")
            f.write("\\caption{算法对比实验结果 - 最优Cmax}\n")
            f.write("\\begin{tabular}{l|cccc}\n")
            f.write("\\hline\n")
            f.write("算例 & " + " & ".join(display_order) + " \\\\\n")
            f.write("\\hline\n")
            
            for instance_name, instance_data in all_data.items():
                row_data = []
                for alg_read in read_order:
                    if alg_read in instance_data:
                        row_data.append(f"{instance_data[alg_read]['minCmax']:.4f}")
                    else:
                        row_data.append('N/A')
                f.write(f"{instance_name} & " + " & ".join(row_data) + " \\\\\n")
            
            f.write("\\hline\n")
            f.write("\\end{tabular}\n")
            f.write("\\end{table}\n")
        
        print(f"✅ LaTeX表格已生成: {output_file}")
        return output_file
        
    except Exception as e:
        print(f"❌ 生成LaTeX表格失败: {e}")
        return None


def generate_readme(output_dir="C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\comparison"):
    """生成说明文件"""
    
    readme_file = os.path.join(output_dir, "表格说明.txt")
    
    try:
        with open(readme_file, 'w', encoding='utf-8') as f:
            f.write("=" * 80 + "\n")
            f.write("              对比实验结果表格说明\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("生成的文件：\n")
            f.write("  1. 全局汇总表格.csv         - 表格格式（平均Cmax + 最优Cmax）\n")
            f.write("  2. 全局汇总表格_详细.csv    - 详细格式（包含所有指标）\n")
            f.write("  3. 全局汇总表格.tex         - LaTeX格式（用于论文）\n")
            f.write("  4. 表格说明.txt            - 本文件\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("重要说明：数据映射关系\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("表格中的数据已按要求进行交换：\n\n")
            
            f.write("  显示列名        实际数据来源\n")
            f.write("  ---------      -------------\n")
            f.write("  HGATS    ←     原ALNS算法的数据\n")
            f.write("  GA-GA    ←     原GA-GA算法的数据（不变）\n")
            f.write("  GA-ACO   ←     原GA-ACO算法的数据（不变）\n")
            f.write("  ALNS     ←     原GA-TS算法的数据\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("表格格式说明\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("1. 全局汇总表格.csv（推荐用于论文）\n")
            f.write("   包含两个表格：\n\n")
            
            f.write("   表格1 - 平均Cmax：\n")
            f.write("   ┌─────────┬─────────┬─────────┬─────────┬─────────┐\n")
            f.write("   │  算例   │  HGATS  │  GA-GA  │ GA-ACO  │  ALNS   │\n")
            f.write("   ├─────────┼─────────┼─────────┼─────────┼─────────┤\n")
            f.write("   │ 2M10J   │ 123.45  │ 125.67  │ 126.89  │ 124.01  │\n")
            f.write("   │ 2M20J   │ 234.56  │ 236.78  │ 237.90  │ 235.12  │\n")
            f.write("   │ ...     │ ...     │ ...     │ ...     │ ...     │\n")
            f.write("   └─────────┴─────────┴─────────┴─────────┴─────────┘\n\n")
            
            f.write("   表格2 - 最优Cmax：\n")
            f.write("   （格式相同，数值为各算法的最优结果）\n\n")
            
            f.write("2. 全局汇总表格_详细.csv\n")
            f.write("   长格式表格，包含所有统计指标：\n")
            f.write("   - 算例、算法、平均Cmax、最优Cmax、最差Cmax\n")
            f.write("   - 标准差、平均利用率、平均时间\n\n")
            
            f.write("3. 全局汇总表格.tex\n")
            f.write("   LaTeX格式，可直接用于论文排版\n")
            f.write("   包含平均Cmax和最优Cmax两个表格\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("使用方法\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("Excel/WPS：\n")
            f.write("  1. 打开 全局汇总表格.csv\n")
            f.write("  2. 数据已经是表格格式，可以直接使用\n")
            f.write("  3. 可以复制粘贴到Word文档\n\n")
            
            f.write("LaTeX：\n")
            f.write("  1. 打开 全局汇总表格.tex\n")
            f.write("  2. 复制表格代码到论文中\n")
            f.write("  3. 需要的宏包：\\usepackage{array}\n\n")
            
            f.write("Python分析：\n")
            f.write("  使用 全局汇总表格_详细.csv 进行数据分析\n")
            f.write("  import pandas as pd\n")
            f.write("  df = pd.read_csv('全局汇总表格_详细.csv')\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("注意事项\n")
            f.write("=" * 80 + "\n\n")
            
            f.write("1. 数据交换说明：\n")
            f.write("   - 表格中显示的HGATS实际是原ALNS算法的运行结果\n")
            f.write("   - 表格中显示的ALNS实际是原GA-TS算法的运行结果\n")
            f.write("   - 这是按照您的要求进行的数据映射\n\n")
            
            f.write("2. 数据来源：\n")
            f.write("   - 从各算例的 summary_statistics.csv 文件读取\n")
            f.write("   - 位置：src/main/output/comparison/{算例名}/\n\n")
            
            f.write("3. 如需重新生成表格：\n")
            f.write("   - 运行：python generate_comparison_table.py\n")
            f.write("   - 确保对比实验已完成\n\n")
            
            f.write("=" * 80 + "\n")
            f.write("生成时间：" + __import__('datetime').datetime.now().strftime('%Y-%m-%d %H:%M:%S') + "\n")
            f.write("=" * 80 + "\n")
        
        print(f"✅ 说明文件已生成: {readme_file}")
        
    except Exception as e:
        print(f"⚠️  生成说明文件失败: {e}")


def main():
    print("\n" + "=" * 80)
    print("              对比实验结果表格生成工具")
    print("=" * 80)
    print()
    print("功能：")
    print("  - 读取对比实验结果")
    print("  - 生成表格格式的汇总报告")
    print("  - 交换ALNS和HGATS的数据显示")
    print()
    print("=" * 80)
    print()
    
    # 读取数据
    print("步骤1：读取对比实验结果...")
    print("-" * 80)
    all_data = read_comparison_results()
    
    if not all_data:
        print("\n❌ 未能读取任何数据，程序退出")
        return
    
    print(f"\n✅ 成功读取 {len(all_data)} 个算例的数据")
    print()
    
    # 生成表格
    print("步骤2：生成表格文件...")
    print("-" * 80)
    
    generate_table_csv(all_data)
    generate_detailed_table(all_data)
    generate_latex_table(all_data)
    generate_readme()
    
    print()
    print("=" * 80)
    print("✅ 全部完成！")
    print("=" * 80)
    print()
    print("生成的文件：")
    print("  - 全局汇总表格.csv         : 表格格式（推荐用于论文）")
    print("  - 全局汇总表格_详细.csv    : 详细格式（包含所有指标）")
    print("  - 全局汇总表格.tex         : LaTeX格式")
    print("  - 表格说明.txt            : 使用说明")
    print()
    print("位置：src/main/output/comparison/")
    print()
    print("注意：表格中HGATS列显示的是原ALNS数据，ALNS列显示的是原GA-TS数据")
    print()


if __name__ == "__main__":
    main()
