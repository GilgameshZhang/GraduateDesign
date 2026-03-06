#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import random

def get_machine_config(filename):
    """根据文件名获取机器配置"""
    if 'J20P3B2D5' in filename:
        return 10, 3, 2, 5  # 总机数, 打印机, 批处理机, 离散机
    elif 'J20P4B3D8' in filename:
        return 15, 4, 3, 8
    elif 'J20P5B4D10' in filename:
        return 19, 5, 4, 10
    elif 'J50P5B4D10' in filename:
        return 19, 5, 4, 10
    else:
        # 默认配置
        return 10, 3, 2, 5

def fix_instance_file(filepath):
    """修复单个算例文件"""
    filename = os.path.basename(filepath)
    total_machines, print_count, batch_count, discrete_count = get_machine_config(filename)

    discrete_start = print_count + batch_count + 1
    discrete_end = total_machines

    print(f"\n修复: {filename}")
    print(f"  配置: 打印{print_count} + 批处理{batch_count} + 离散{discrete_count} = {total_machines}")
    print(f"  离散机范围: M{discrete_start}-M{discrete_end}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 修复每一行
    for i in range(3, len(lines)):  # 从第4行开始
        if not lines[i].strip():
            continue

        parts = lines[i].split()
        if len(parts) < 6:
            continue

        # 修复离散工序部分
        new_line = ' '.join(parts[:5])  # 保留前5个元素

        idx = 5
        while idx < len(parts):
            if idx + 1 >= len(parts):
                break

            try:
                machine_no = int(parts[idx])
                time = int(parts[idx + 1])

                # 修复机器编号
                if machine_no < discrete_start or machine_no > discrete_end:
                    machine_no = random.randint(discrete_start, discrete_end)

                # 修复时间
                if time <= 0 or time < 600 or time > 1200:
                    time = random.randint(600, 1200)

                new_line += f' {machine_no} {time}'
                idx += 2
            except (ValueError, IndexError):
                break

        lines[i] = new_line

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines))

    print("  ✅ 修复完成")

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"发现 {len(files)} 个算例文件")

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_instance_file(filepath)
        except Exception as e:
            print(f"❌ 处理 {filename} 时出错: {e}")

    print(f"\n{'='*80}")
    print("批量修复完成！")
    print(f"{'='*80}")

if __name__ == '__main__':
    random.seed(42)
    main()

