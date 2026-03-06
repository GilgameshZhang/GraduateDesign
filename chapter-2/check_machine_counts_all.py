#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def check_machine_counts(filepath):
    """检查算例文件中离散处理工序的机器数量对应"""
    print(f"\n检查: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    errors = []

    # 检查每个工件的离散处理部分
    for job_idx in range(len(lines) - 3):  # 从第4行开始
        line_idx = 3 + job_idx
        if line_idx >= len(lines):
            break

        parts = lines[line_idx].strip().split()
        if len(parts) < 5:
            continue

        discrete_oper_count = int(parts[0])
        idx = 5

        for oper_idx in range(discrete_oper_count):
            if idx >= len(parts):
                errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 数据不完整")
                break

            # 读取声明的机器数量
            declared_count = int(parts[idx])
            idx += 1

            # 计算实际的机器-时间对数量
            actual_count = 0
            for _ in range(declared_count):
                if idx + 1 >= len(parts):
                    errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 机器-时间对不完整")
                    break
                idx += 2  # 跳过机器编号和时间
                actual_count += 1

            if declared_count != actual_count:
                errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 声明{declared_count}台，实际{actual_count}台")

    if errors:
        print(f"  ❌ 发现 {len(errors)} 个错误:")
        for error in errors[:5]:  # 只显示前5个
            print(f"    {error}")
        if len(errors) > 5:
            print(f"    ... 还有{len(errors)-5}个错误")
    else:
        print("  ✅ 所有工序机器数量对应正确")

    return errors

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"检查 {len(files)} 个算例文件\n")

    total_errors = 0
    files_with_errors = []

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            errors = check_machine_counts(filepath)
            if errors:
                total_errors += len(errors)
                files_with_errors.append(filename)
                print(f"文件 {filename} 有 {len(errors)} 个错误")
        except Exception as e:
            print(f"❌ 处理 {filename} 时出错: {e}")

    print(f"\n{'='*80}")
    print("检查结果汇总")
    print(f"{'='*80}")
    print(f"总文件数: {len(files)}")
    print(f"有错误的文件: {len(files_with_errors)}")
    print(f"总错误数: {total_errors}")

    if files_with_errors:
        print("
需要修复的文件:")
        for filename in files_with_errors:
            print(f"  - {filename}")
    else:
        print("\n✅ 所有算例文件的离散处理机器数量都正确对应！")

if __name__ == '__main__':
    main()

