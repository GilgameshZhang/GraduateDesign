#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
按照机器数从小到大顺序生成算例
命名规则：J{工件数}P{打印机数}B{批处理机数}D{离散机数}_{实例编号}.txt
"""

import os
import random
from typing import List, Tuple

def generate_instance_configs(min_machines: int = 5, max_machines: int = 12) -> List[Tuple[int, int, int, int]]:
    """
    生成所有可能的机器配置，按照总机器数从小到大排序

    Args:
        min_machines: 最小总机器数
        max_machines: 最大总机器数

    Returns:
        配置列表，每个配置为 (总机器数, 打印机数, 批处理机数, 离散机数)
    """
    configs = []

    for total_machines in range(min_machines, max_machines + 1):
        for printers in range(1, total_machines - 1):  # 至少1个打印机
            for batches in range(total_machines - printers):  # 批处理机可以为0
                discretes = total_machines - printers - batches
                if discretes >= 1:  # 至少1个离散机
                    configs.append((total_machines, printers, batches, discretes))

    # 按照总机器数排序
    configs.sort(key=lambda x: x[0])
    return configs

def generate_ordered_instances(job_counts: List[int] = [20, 50],
                             min_machines: int = 5,
                             max_machines: int = 12,
                             instances_per_config: int = 2,
                             output_dir: str = "src/main/resources/instance"):
    """
    按照机器数从小到大顺序生成算例

    Args:
        job_counts: 工件数量列表
        min_machines: 最小总机器数
        max_machines: 最大总机器数
        instances_per_config: 每种配置生成的实例数
        output_dir: 输出目录
    """
    configs = generate_instance_configs(min_machines, max_machines)

    print(f"生成算例配置组合：{len(configs)}个")
    print("\n配置列表（按机器数从小到大排序）：")
    for i, (total, p, b, d) in enumerate(configs, 1):
        print(f"{i:2d}. 总{total}台机器 (P{p}+B{b}+D{d})")

    instance_count = 0

    # 为每个配置生成算例
    for config in configs:
        total_machines, printers, batches, discretes = config

        for instance_id in range(1, instances_per_config + 1):
            for job_count in job_counts:
                filename = f"J{job_count}P{printers}B{batches}D{discretes}_{instance_id:02d}.txt"
                filepath = os.path.join(output_dir, filename)

                # 设置随机种子以确保可重现性
                seed = total_machines * 1000 + job_count * 100 + instance_id
                random.seed(seed)

                try:
                    generate_single_instance(filepath, job_count, printers, batches, discretes)
                    print(f"✓ {filename}")
                    instance_count += 1
                except Exception as e:
                    print(f"✗ {filename} - 生成失败: {e}")

    print(f"\n共生成算例文件：{instance_count}个")

def generate_single_instance(filepath: str, job_count: int, printers: int, batches: int, discretes: int):
    """
    生成单个算例文件
    """
    lines = []

    # 第一行：总机器数 工件数
    total_machines = printers + batches + discretes
    lines.append(f"{total_machines} {job_count}")

    # 第二行：打印机配置
    printer_configs = []
    for i in range(printers):
        length = random.randint(60, 120) * 10  # 600-1200
        width = random.randint(40, 60) * 10    # 400-600
        height = random.randint(40, 60) * 10   # 400-600
        layer_height = round(random.uniform(0.025, 0.12), 3)
        switch_time = random.randint(300, 600)
        print_time = random.randint(10, 30)
        printer_configs.append(f"{i+1} {length} {width} {height} {layer_height} {switch_time} {print_time}")
    lines.append(f"{printers} {' '.join(printer_configs)}")

    # 第三行：批处理机配置
    batch_configs = []
    for i in range(batches):
        process_time = random.randint(60, 120) * 10  # 600-1200
        batch_configs.append(f"{i+1} {process_time}")
    lines.append(f"{batches} {' '.join(batch_configs)}")

    # 生成工件数据
    for job_id in range(job_count):
        # 工件尺寸
        length = random.randint(10, 600)
        width = random.randint(10, 600)
        height = random.randint(10, 400)

        # 离散工序数 (1-8)
        discrete_ops = random.randint(1, 8)
        total_ops = 2 + discrete_ops

        job_line = f"{total_ops} {length} {width} {height}"

        # 打印工序：1台打印机，时间0
        printer_id = (job_id % printers) + 1
        job_line += f" 1 {printer_id} 0"

        # 批处理工序：1台批处理机，时间0
        batch_id = (job_id % batches) + 1 + printers
        job_line += f" 1 {batch_id} 0"

        # 离散工序
        for op in range(discrete_ops):
            # 每工序可选机器数 (2-5台)
            machine_count = min(5, max(2, random.randint(2, discretes)))
            job_line += f" {machine_count}"

            # 随机选择机器
            available_machines = list(range(printers + batches + 1, total_machines + 1))
            selected_machines = random.sample(available_machines, machine_count)

            for machine_id in selected_machines:
                process_time = random.randint(600, 1200)
                job_line += f" {machine_id} {process_time}"

        lines.append(job_line)

    # 写入文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')

def main():
    """主函数"""
    print("=== 算例顺序生成器 ===\n")

    # 确保输出目录存在
    output_dir = "src/main/resources/instance"
    os.makedirs(output_dir, exist_ok=True)

    # 生成算例
    generate_ordered_instances(
        job_counts=[20, 50],      # 工件数量
        min_machines=5,           # 最小总机器数
        max_machines=12,          # 最大总机器数
        instances_per_config=2,   # 每种配置生成2个实例
        output_dir=output_dir
    )

    print("\n✅ 算例生成完成！")
    print("命名规则：J{工件数}P{打印机数}B{批处理机数}D{离散机数}_{实例编号}.txt")
    print("生成顺序：按照总机器数从小到大排序")

if __name__ == "__main__":
    main()
