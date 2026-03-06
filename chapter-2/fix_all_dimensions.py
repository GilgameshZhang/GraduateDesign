#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

def fix_dimensions(filepath):
    """修复算例文件中工件的尺寸"""
    print(f"修复尺寸: {os.path.basename(filepath)}")

    with open(filepath, 'r', encoding='utf-8') as f:
        lines = f.readlines()

    # 保留前3行
    fixed_lines = lines[:3]

    size_fixes = 0

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
            size_fixes += 1
        else:
            fixed_lines.append(lines[i])

    # 写回文件
    with open(filepath, 'w', encoding='utf-8') as f:
        f.writelines(fixed_lines)

    print(f"  ✅ 修复了{size_fixes}个工件的尺寸")

def main():
    instance_dir = r'src\main\resources\instance'

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"批量修复 {len(files)} 个算例文件的工件尺寸\n")

    total_fixes = 0
    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fix_dimensions(filepath)
            total_fixes += 1
        except Exception as e:
            print(f"❌ 错误: {e}")

    print(f"\n{'='*60}")
    print(f"修复完成！共处理 {len(files)} 个文件")
    print(f"{'='*60}")

if __name__ == '__main__':
    main()

