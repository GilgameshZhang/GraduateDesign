#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def quick_validate(filepath):
    """快速验证算例文件的关键问题"""
    print(f"检查: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    errors = []

    # 检查文件头
    header = lines[0].strip().split()
    if len(header) != 2:
        errors.append("文件头格式错误")
        return errors

    total_machines = int(header[0])
    expected_jobs = int(header[1])

    # 检查工件数量
    actual_jobs = len(lines) - 3
    if actual_jobs != expected_jobs:
        errors.append(f"工件数量不匹配: 期望{expected_jobs}, 实际{actual_jobs}")

    # 检查打印机和批处理机配置
    printer_line = lines[1].strip().split()
    batch_line = lines[2].strip().split()

    printer_count = int(printer_line[0])
    batch_count = int(batch_line[0])
    discrete_count = total_machines - printer_count - batch_count

    if discrete_count <= 0:
        errors.append(f"离散机数量错误: {discrete_count}")

    print(f"  配置: 打印{printer_count} + 批处理{batch_count} + 离散{discrete_count} = {total_machines}")

    # 快速检查几个工件
    for i in range(min(3, expected_jobs)):  # 检查前3个工件
        job_line = lines[3 + i].strip().split()
        if len(job_line) < 8:
            errors.append(f"J{i+1}: 工件行太短")
            continue

        # 检查是否有离散工序数据
        discrete_ops = int(job_line[0])
        if discrete_ops < 1:
            errors.append(f"J{i+1}: 离散工序数错误: {discrete_ops}")

        # 检查打印工序 (位置应该有 1 M 0)
        if len(job_line) < 8 or job_line[5] != '1' or job_line[7] != '0':
            errors.append(f"J{i+1}: 打印工序格式错误")

        # 检查批处理工序 (位置应该有 1 M 0)
        if len(job_line) < 11 or job_line[8] != '1' or job_line[10] != '0':
            errors.append(f"J{i+1}: 批处理工序格式错误")

    if errors:
        print(f"  ❌ {len(errors)} 个错误")
        for error in errors[:3]:
            print(f"    {error}")
    else:
        print("  ✅ 基本检查通过")

    return errors

def main():
    instance_dir = r'src\main\resources\instance'

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"快速验证 {len(files)} 个算例文件\n")

    total_errors = 0
    valid_files = 0

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        errors = quick_validate(filepath)
        if not errors:
            valid_files += 1
        total_errors += len(errors)

    print(f"\n结果: {valid_files}/{len(files)} 个文件验证通过")
    if total_errors > 0:
        print(f"发现 {total_errors} 个错误需要修复")

if __name__ == '__main__':
    main()

