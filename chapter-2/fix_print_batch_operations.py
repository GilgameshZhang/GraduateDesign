#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import random

def fix_print_batch_operations(filepath):
    """修复算例文件中打印和批处理工序的机器分配"""
    print(f"\n修复打印批处理工序: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析配置
    header = lines[0].strip().split()
    total_machines = int(header[0])

    print_line = lines[1].strip().split()
    print_machine_count = int(print_line[0])

    batch_line = lines[2].strip().split()
    batch_machine_count = int(batch_line[0])

    # 计算机器范围
    print_start = 1
    print_end = print_machine_count
    batch_start = print_machine_count + 1
    batch_end = print_machine_count + batch_machine_count
    discrete_start = print_machine_count + batch_machine_count + 1
    discrete_end = total_machines

    print(f"  打印机: M{print_start}-M{print_end}")
    print(f"  批处理机: M{batch_start}-M{batch_end}")
    print(f"  离散机: M{discrete_start}-M{discrete_end}")

    fixed_lines = lines[:3]

    # 修复每个工件
    for i in range(3, len(lines)):
        if not lines[i].strip():
            continue

        parts = lines[i].strip().split()
        if len(parts) < 5:
            continue

        discrete_oper_count = int(parts[0])

        # 修复工件尺寸
        length = int(parts[1])
        width = int(parts[2])
        height = int(parts[3])

        # 缩小过大尺寸
        max_dim = max(length, width)
        if max_dim > 100:
            scale = 100.0 / max_dim
            length = max(10, int(length * scale))
            width = max(10, int(width * scale))
            height = max(10, int(height * scale))

        # 构建修复后的行
        new_parts = [str(discrete_oper_count), str(length), str(width), str(height), str(discrete_oper_count)]

        # 打印工序：随机选择一台打印机，时间为0
        print_machine = random.randint(print_start, print_end)
        new_parts.extend(['1', str(print_machine), '0'])

        # 批处理工序：随机选择一台批处理机，时间为0
        batch_machine = random.randint(batch_start, batch_end)
        new_parts.extend(['1', str(batch_machine), '0'])

        # 为每个离散工序生成数据
        for oper_idx in range(discrete_oper_count):
            # 随机选择1-4台离散机
            machine_count = random.randint(1, 4)
            new_parts.append(str(machine_count))

            # 选择不重复的离散机
            available_machines = list(range(discrete_start, discrete_end + 1))
            selected_machines = random.sample(available_machines, min(machine_count, len(available_machines)))

            for machine in selected_machines:
                time = random.randint(600, 1200)
                new_parts.extend([str(machine), str(time)])

        fixed_lines.append(' '.join(new_parts) + '\n')
        print(f"  工件{i-2}: 修复完成 (打印M{print_machine}, 批处理M{batch_machine}, {discrete_oper_count}个离散工序)")

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print("  ✅ 打印批处理工序修复完成")

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"修复 {len(files)} 个算例文件的打印批处理工序\n")

    # 重点修复有问题的文件
    problematic_files = ['J50P5B4D10_10.txt']

    for filename in problematic_files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_print_batch_operations(filepath)
        except Exception as e:
            print(f"❌ 错误: {e}")

    print(f"\n{'='*60}")
    print(f"打印批处理工序修复完成！")
    print(f"{'='*60}")

if __name__ == '__main__':
    random.seed(42)
    main()

