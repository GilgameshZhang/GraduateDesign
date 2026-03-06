#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def validate_all_instances():
    """最终验证所有算例文件的离散处理机器数量对应"""
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"最终验证 {len(files)} 个算例文件\n")

    total_errors = 0
    valid_files = 0

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            errors = validate_single_file(filepath)
            if not errors:
                valid_files += 1
                print(f"✅ {filename}")
            else:
                total_errors += len(errors)
                print(f"❌ {filename}: {len(errors)} 个错误")
                for error in errors[:2]:  # 只显示前2个错误
                    print(f"    {error}")
        except Exception as e:
            print(f"❌ {filename}: 处理出错 - {e}")
            total_errors += 1

    print(f"\n{'='*80}")
    print("最终验证结果")
    print(f"{'='*80}")
    print(f"总文件数: {len(files)}")
    print(f"验证通过: {valid_files}")
    print(f"有错误: {len(files) - valid_files}")
    print(f"总错误数: {total_errors}")

    if valid_files == len(files):
        print("\n🎉 所有算例文件的离散处理机器数量都正确对应！")
        print("✅ 可以正常用于算法测试")
    else:
        print(f"\n⚠️ 还有 {len(files) - valid_files} 个文件需要修复")

def validate_single_file(filepath):
    """验证单个文件"""
    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 解析第一行
    header = lines[0].strip().split()
    job_count = int(header[1])

    errors = []

    # 检查每个工件
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

    return errors

if __name__ == '__main__':
    validate_all_instances()

