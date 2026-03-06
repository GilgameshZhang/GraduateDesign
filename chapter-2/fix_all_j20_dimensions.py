#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def fix_dimensions_in_file(filepath):
    """修复单个文件中的工件尺寸"""
    print(f"修复尺寸: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    fixed_lines = lines[:3]  # 保留前3行
    fixes = 0

    # 从第4行开始处理每个工件
    for i in range(3, len(lines)):
        if not lines[i].strip():
            continue

        parts = lines[i].strip().split()

        # 解析尺寸
        discrete_oper_count = int(parts[0])
        length = int(parts[1])
        width = int(parts[2])
        height = int(parts[3])

        # 检查是否需要修复
        max_dimension = max(length, width)
        if max_dimension > 100:
            # 按比例缩小
            scale_factor = 100.0 / max_dimension
            new_length = max(10, int(length * scale_factor))
            new_width = max(10, int(width * scale_factor))
            new_height = max(10, int(height * scale_factor))

            print(f"  J{i-2}: {length}×{width}×{height} → {new_length}×{new_width}×{new_height}")

            # 构建修复后的行
            new_parts = [str(discrete_oper_count), str(new_length), str(new_width), str(new_height)] + parts[4:]
            fixed_lines.append(' '.join(new_parts) + '\n')
            fixes += 1
        else:
            fixed_lines.append(lines[i])

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print(f"  ✅ 修复了{fixes}个工件的尺寸")

def main():
    instance_dir = r'src\main\resources\instance'

    # 修复所有J20系列文件
    j20_files = [f for f in os.listdir(instance_dir) if f.startswith('J20')]

    print(f"发现 {len(j20_files)} 个J20系列文件\n")

    total_fixes = 0
    for filename in j20_files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_dimensions_in_file(filepath)
            total_fixes += 1
        except Exception as e:
            print(f"❌ 错误: {e}")

    print(f"\n{'='*60}")
    print(f"J20系列零件尺寸修复完成！")
    print(f"共处理 {len(j20_files)} 个文件")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

