#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import random

def check_and_fix_file(filepath):
    """检查并修复算例文件"""
    print(f"\n检查文件: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析第一行
    header = lines[0].strip().split()
    total_machines = int(header[0])
    expected_jobs = int(header[1])

    print(f"  配置: {total_machines}台机器, 期望{expected_jobs}个工件")

    # 计算实际工件数量（从第4行开始，每行一个工件）
    actual_jobs = len(lines) - 3

    print(f"  实际工件数: {actual_jobs}")

    if actual_jobs != expected_jobs:
        print(f"  ❌ 工件数量不匹配! 期望{expected_jobs}, 实际{actual_jobs}")
        return False

    # 检查和修复工件尺寸
    fixed_lines = lines[:3]  # 保留前3行

    size_fixes = 0
    for job_idx in range(expected_jobs):
        line_idx = 3 + job_idx
        parts = lines[line_idx].strip().split()

        # 解析尺寸 (length, width, height)
        discrete_oper_count = int(parts[0])
        length = int(parts[1])
        width = int(parts[2])
        height = int(parts[3])

        # 检查是否需要修复尺寸
        max_dimension = max(length, width)
        if max_dimension > 100:
            # 按比例缩小，但保持相对比例
            scale_factor = 100.0 / max_dimension
            new_length = max(10, int(length * scale_factor))
            new_width = max(10, int(width * scale_factor))
            new_height = max(10, int(height * scale_factor))

            print(f"    J{job_idx+1}: 尺寸 {length}×{width}×{height} → {new_length}×{new_width}×{new_height}")

            # 构建修复后的行
            new_parts = [str(discrete_oper_count), str(new_length), str(new_width), str(new_height)] + parts[4:]
            fixed_lines.append(' '.join(new_parts) + '\n')
            size_fixes += 1
        else:
            fixed_lines.append(lines[line_idx])

    if size_fixes > 0:
        # 写回文件
        with open(filepath, 'w', encoding='utf-8') as f:
            f.writelines(fixed_lines)
        print(f"  ✅ 修复了{size_fixes}个工件的尺寸")

    print("  ✅ 工件数量和尺寸都正确"    return True

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"检查 {len(files)} 个算例文件\n")

    valid_files = 0
    total_fixes = 0

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            if check_and_fix_file(filepath):
                valid_files += 1
        except Exception as e:
            print(f"❌ 处理 {filename} 时出错: {e}")

    print(f"\n{'='*80}")
    print("检查结果汇总")
    print(f"{'='*80}")
    print(f"总文件数: {len(files)}")
    print(f"验证通过: {valid_files}")
    print(f"需要修复: {len(files) - valid_files}")

    if valid_files == len(files):
        print("\n🎉 所有算例文件的工件数量和尺寸都正确！")
    else:
        print(f"\n⚠️ 还有 {len(files) - valid_files} 个文件有问题")

if __name__ == '__main__':
    main()

