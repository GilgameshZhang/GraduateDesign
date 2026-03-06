#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import random

def fix_operation_format(filepath):
    """修复算例文件中离散工序的格式问题"""
    print(f"\n修复工序格式: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析配置
    header = lines[0].strip().split()
    total_machines = int(header[0])

    print_line = lines[1].strip().split()
    print_count = int(print_line[0])

    batch_line = lines[2].strip().split()
    batch_count = int(batch_line[0])

    discrete_start = print_count + batch_count + 1
    discrete_end = total_machines

    print(f"  离散机范围: M{discrete_start}-M{discrete_end}")

    fixed_lines = lines[:3]

    # 修复每个工件
    for i in range(3, len(lines)):
        if not lines[i].strip():
            continue

        parts = lines[i].strip().split()
        if len(parts) < 5:
            continue

        discrete_oper_count = int(parts[0])

        # 修复工件尺寸（确保合理）
        length = int(parts[1])
        width = int(parts[2])
        height = int(parts[3])

        # 缩小过大的尺寸
        max_dim = max(length, width)
        if max_dim > 100:
            scale = 100.0 / max_dim
            length = max(10, int(length * scale))
            width = max(10, int(width * scale))
            height = max(10, int(height * scale))

        # 为每个工序生成合理的机器分配
        new_parts = [str(discrete_oper_count), str(length), str(width), str(height), str(discrete_oper_count)]

        # 为每个离散工序分配机器
        for oper_idx in range(discrete_oper_count):
            # 随机决定这个工序有多少台机器 (1-4台)
            machine_count = random.randint(1, 4)
            new_parts.append(str(machine_count))

            # 分配机器
            used_machines = set()
            for _ in range(machine_count):
                # 选择未使用的离散机
                available = [m for m in range(discrete_start, discrete_end + 1) if m not in used_machines]
                if not available:
                    available = list(range(discrete_start, discrete_end + 1))

                machine = random.choice(available)
                used_machines.add(machine)

                # 生成合理的加工时间 (600-1200)
                time = random.randint(600, 1200)

                new_parts.extend([str(machine), str(time)])

        fixed_lines.append(' '.join(new_parts) + '\n')
        print(f"  工件{i-2}: {discrete_oper_count}个离散工序 → 重新分配机器")

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print("  ✅ 工序格式修复完成")

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"修复 {len(files)} 个算例文件的工序格式\n")

    # 重点修复有问题的文件
    problematic_files = ['J50P5B4D10_10.txt']

    for filename in problematic_files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_operation_format(filepath)
        except Exception as e:
            print(f"❌ 错误: {e}")

    print(f"\n{'='*60}")
    print(f"工序格式修复完成！")
    print(f"{'='*60}")

if __name__ == '__main__':
    random.seed(42)
    main()

