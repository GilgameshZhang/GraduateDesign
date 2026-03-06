#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def validate_machine_counts(filepath):
    """验证离散处理工序中声明的机器数量是否与实际数量一致"""
    print(f"\n验证: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析第一行
    header = lines[0].strip().split()
    total_machines = int(header[0])
    job_count = int(header[1])

    # 解析第二行（打印机）
    print_line = lines[1].strip().split()
    print_machine_count = int(print_line[0])

    # 解析第三行（批处理机）
    batch_line = lines[2].strip().split()
    batch_machine_count = int(batch_line[0])

    # 计算离散机范围
    discrete_machine_count = total_machines - print_machine_count - batch_machine_count
    discrete_start = print_machine_count + batch_machine_count + 1
    discrete_end = total_machines

    print(f"  离散机: M{discrete_start}-M{discrete_end} ({discrete_machine_count}台)")

    errors = []

    # 检查每个工件的离散处理部分
    for job_idx in range(job_count):
        line_idx = 3 + job_idx
        if line_idx >= len(lines):
            errors.append(f"J{job_idx+1}: 数据不足")
            continue

        parts = lines[line_idx].strip().split()
        if len(parts) < 5:
            errors.append(f"J{job_idx+1}: 格式错误")
            continue

        discrete_oper_count = int(parts[0])
        idx = 5  # 从第6个元素开始

        print(f"  J{job_idx+1}: 离散工序数={discrete_oper_count}")

        for oper_idx in range(discrete_oper_count):
            if idx >= len(parts):
                errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 数据不完整")
                break

            # 读取声明的机器数量
            declared_machine_count = int(parts[idx])
            idx += 1

            # 统计实际的机器-时间对数量
            actual_machine_count = 0

            for _ in range(declared_machine_count):
                if idx + 1 >= len(parts):
                    errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 机器-时间对不完整")
                    break

                machine_no = int(parts[idx])
                time = int(parts[idx + 1])
                idx += 2
                actual_machine_count += 1

                # 检查机器编号是否在离散机范围内（可选检查）
                if machine_no < discrete_start or machine_no > discrete_end:
                    errors.append(f"J{job_idx+1} 工序{oper_idx+1}: M{machine_no} 不在离散机范围内 (应为M{discrete_start}-M{discrete_end})")

            # 检查声明数量与实际数量是否一致
            if declared_machine_count != actual_machine_count:
                errors.append(f"J{job_idx+1} 工序{oper_idx+1}: 声明{declared_machine_count}台机器，实际{actual_machine_count}台")

            print(f"    工序{oper_idx+1}: 声明{declared_machine_count}台 → 实际{actual_machine_count}台")

    if errors:
        print(f"  ❌ 发现 {len(errors)} 个错误:")
        for error in errors[:5]:  # 只显示前5个
            print(f"    {error}")
        if len(errors) > 5:
            print(f"    ... 还有{len(errors)-5}个错误")
    else:
        print("  ✅ 所有工序的机器数量对应正确")

    return errors

def main():
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"发现 {len(files)} 个算例文件")

    total_errors = 0
    files_with_errors = []

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            errors = validate_machine_counts(filepath)
            if errors:
                total_errors += len(errors)
                files_with_errors.append(filename)
        except Exception as e:
            print(f"❌ 处理 {filename} 时出错: {e}")
            import traceback
            traceback.print_exc()

    print(f"\n{'='*100}")
    print("验证结果汇总")
    print(f"{'='*100}")
    print(f"总文件数: {len(files)}")
    print(f"有错误的文件: {len(files_with_errors)}")
    print(f"总错误数: {total_errors}")

    if files_with_errors:
        print("
有错误的算例文件:")
        for filename in files_with_errors:
            print(f"  - {filename}")
        print(f"\n需要修复 {len(files_with_errors)} 个文件中的机器数量对应问题")
    else:
        print("\n✅ 所有算例文件的离散处理机器数量都正确对应！")

if __name__ == '__main__':
    main()
