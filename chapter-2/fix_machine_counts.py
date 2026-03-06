#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import random

def fix_machine_counts(filepath):
    """修复算例文件中离散工序的机器数量对应问题"""
    print(f"\n修复: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析第一行
    header = lines[0].strip().split()
    total_machines = int(header[0])

    # 解析第二行（打印机）
    print_line = lines[1].strip().split()
    print_machine_count = int(print_line[0])

    # 解析第三行（批处理机）
    batch_line = lines[2].strip().split()
    batch_machine_count = int(batch_line[0])

    # 计算离散机范围
    discrete_start = print_machine_count + batch_machine_count + 1
    discrete_end = total_machines

    fixed_lines = lines[:3]  # 保留前3行
    fixes = 0

    # 修复每个工件的离散处理部分
    for i in range(3, len(lines)):
        if not lines[i].strip():
            continue

        parts = lines[i].strip().split()
        if len(parts) < 5:
            fixed_lines.append(lines[i])
            continue

        discrete_oper_count = int(parts[0])
        new_parts = parts[:5]  # 保留前5个元素

        idx = 5
        oper_fixed = False

        for oper_idx in range(discrete_oper_count):
            if idx >= len(parts):
                break

            # 读取声明的机器数量
            declared_count = int(parts[idx])
            idx += 1

            # 收集实际的机器-时间对
            machines_times = []
            for _ in range(declared_count):
                if idx + 1 >= len(parts):
                    break
                machine_no = int(parts[idx])
                time = int(parts[idx + 1])
                idx += 2

                # 修复机器编号（确保在离散机范围内）
                if machine_no < discrete_start or machine_no > discrete_end:
                    machine_no = random.randint(discrete_start, discrete_end)
                    oper_fixed = True

                # 修复时间（确保在合理范围内）
                if time <= 0 or time < 600 or time > 1200:
                    time = random.randint(600, 1200)
                    oper_fixed = True

                machines_times.append((machine_no, time))

            # 检查是否缺少机器-时间对
            while len(machines_times) < declared_count:
                machine_no = random.randint(discrete_start, discrete_end)
                time = random.randint(600, 1200)
                machines_times.append((machine_no, time))
                oper_fixed = True

            # 添加修复后的数据
            new_parts.append(str(len(machines_times)))
            for m, t in machines_times:
                new_parts.append(str(m))
                new_parts.append(str(t))

        if oper_fixed:
            fixed_lines.append(' '.join(new_parts) + '\n')
            fixes += 1
        else:
            fixed_lines.append(lines[i])

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    if fixes > 0:
        print(f"  ✅ 修复了{fixes}个工序的机器数量问题")
    else:
        print("  ✅ 无需修复")

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"修复 {len(files)} 个算例文件的机器数量对应问题\n")

    total_fixes = 0
    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_machine_counts(filepath)
            total_fixes += 1
        except Exception as e:
            print(f"❌ 错误: {e}")

    print(f"\n{'='*60}")
    print(f"机器数量对应修复完成！")
    print(f"共处理 {len(files)} 个文件")
    print(f"{'='*60}")

if __name__ == '__main__':
    random.seed(42)
    main()

