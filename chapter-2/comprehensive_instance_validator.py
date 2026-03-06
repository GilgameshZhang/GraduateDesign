#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
算例文件综合验证器

验证算例文件的完整性和正确性，包括：
1. 文件格式正确性
2. 工件数量一致性
3. 机器数量和编号正确性
4. 工序数据完整性
5. 时间参数合理性
"""

import os
import re
from typing import List, Dict, Tuple

class InstanceValidator:
    def __init__(self):
        self.errors = []
        self.warnings = []

    def validate_file(self, filepath: str) -> bool:
        """验证单个算例文件"""
        self.errors = []
        self.warnings = []

        print(f"\n🔍 验证算例文件: {os.path.basename(filepath)}")

        try:
            with open(filepath, 'r', encoding='utf-8') as f:
                lines = [line.strip() for line in f.readlines() if line.strip()]
        except Exception as e:
            self.errors.append(f"文件读取失败: {e}")
            return False

        if len(lines) < 3:
            self.errors.append("文件行数不足，至少需要3行")
            return False

        # 验证第一行：总机器数 工件数
        if not self._validate_header(lines[0]):
            return False

        # 解析基本信息
        header_parts = lines[0].split()
        total_machines = int(header_parts[0])
        expected_jobs = int(header_parts[1])

        # 验证第二行：打印机配置
        printer_count = self._validate_printer_line(lines[1])
        if printer_count is None:
            return False

        # 验证第三行：批处理机配置
        batch_count = self._validate_batch_line(lines[2])
        if batch_count is None:
            return False

        # 计算离散机数量
        discrete_count = total_machines - printer_count - batch_count
        if discrete_count <= 0:
            self.errors.append(f"离散机数量计算错误: {discrete_count}")
            return False

        # 验证工件数据行
        actual_jobs = len(lines) - 3
        if actual_jobs != expected_jobs:
            self.errors.append(f"工件数量不匹配: 期望{expected_jobs}个，实际{actual_jobs}个")
            return False

        # 验证每个工件
        machine_ranges = {
            'printer': (1, printer_count),
            'batch': (printer_count + 1, printer_count + batch_count),
            'discrete': (printer_count + batch_count + 1, total_machines)
        }

        for job_idx in range(expected_jobs):
            job_line_idx = 3 + job_idx
            if not self._validate_job_line(lines[job_line_idx], job_idx, machine_ranges):
                return False

        return self._report_results()

    def _validate_header(self, line: str) -> bool:
        """验证文件头"""
        parts = line.split()
        if len(parts) != 2:
            self.errors.append(f"文件头格式错误: {line}")
            return False

        try:
            total_machines = int(parts[0])
            job_count = int(parts[1])

            if total_machines <= 0 or job_count <= 0:
                self.errors.append(f"机器数或工件数不能为负数或零: {total_machines}, {job_count}")
                return False

        except ValueError:
            self.errors.append(f"文件头包含非数字: {line}")
            return False

        return True

    def _validate_printer_line(self, line: str) -> int:
        """验证打印机配置行"""
        parts = line.split()
        if len(parts) < 1:
            self.errors.append(f"打印机行格式错误: {line}")
            return None

        try:
            printer_count = int(parts[0])
            if printer_count <= 0:
                self.errors.append(f"打印机数量不能为负数或零: {printer_count}")
                return None

            # 验证每个打印机的参数（7个参数：编号+6个属性）
            expected_params = 1 + printer_count * 7
            if len(parts) != expected_params:
                self.errors.append(f"打印机参数数量错误: 期望{expected_params}个，实际{len(parts)}个")
                return None

        except ValueError:
            self.errors.append(f"打印机行包含非数字: {line}")
            return None

        return printer_count

    def _validate_batch_line(self, line: str) -> int:
        """验证批处理机配置行"""
        parts = line.split()
        if len(parts) < 1:
            self.errors.append(f"批处理机行格式错误: {line}")
            return None

        try:
            batch_count = int(parts[0])
            if batch_count < 0:
                self.errors.append(f"批处理机数量不能为负数: {batch_count}")
                return None

            # 验证每个批处理机的参数（2个参数：编号+加工时间）
            expected_params = 1 + batch_count * 2
            if len(parts) != expected_params:
                self.errors.append(f"批处理机参数数量错误: 期望{expected_params}个，实际{len(parts)}个")
                return None

        except ValueError:
            self.errors.append(f"批处理机行包含非数字: {line}")
            return None

        return batch_count

    def _validate_job_line(self, line: str, job_idx: int, machine_ranges: Dict) -> bool:
        """验证工件行"""
        parts = line.split()
        if len(parts) < 5:
            self.errors.append(f"J{job_idx+1}: 工件行格式错误，至少需要5个参数")
            return False

        try:
            discrete_ops = int(parts[0])
            length = int(parts[1])
            width = int(parts[2])
            height = int(parts[3])
            discrete_ops_confirm = int(parts[4])

            if discrete_ops != discrete_ops_confirm:
                self.errors.append(f"J{job_idx+1}: 离散工序数量不一致 ({discrete_ops} != {discrete_ops_confirm})")
                return False

            # 验证尺寸合理性
            if length <= 0 or width <= 0 or height <= 0:
                self.errors.append(f"J{job_idx+1}: 工件尺寸不能为负数或零: {length}×{width}×{height}")
                return False

            if max(length, width) > 1000:  # 尺寸过大警告
                self.warnings.append(f"J{job_idx+1}: 工件尺寸可能过大: {length}×{width}×{height}")

            # 验证工序数据
            idx = 5
            operation_count = 0

            # 打印工序
            if idx + 2 >= len(parts):
                self.errors.append(f"J{job_idx+1}: 打印工序数据不完整")
                return False

            print_machine_count = int(parts[idx])
            print_machine = int(parts[idx + 1])
            print_time = int(parts[idx + 2])

            if print_machine_count != 1:
                self.errors.append(f"J{job_idx+1}: 打印工序机器数量错误，应该是1台")
                return False

            if print_machine < machine_ranges['printer'][0] or print_machine > machine_ranges['printer'][1]:
                self.errors.append(f"J{job_idx+1}: 打印机编号超出范围: M{print_machine} (应为M{machine_ranges['printer'][0]}-M{machine_ranges['printer'][1]})")
                return False

            if print_time != 0:
                self.errors.append(f"J{job_idx+1}: 打印时间应该是0: {print_time}")
                return False

            idx += 3
            operation_count += 1

            # 批处理工序
            if idx + 2 >= len(parts):
                self.errors.append(f"J{job_idx+1}: 批处理工序数据不完整")
                return False

            batch_machine_count = int(parts[idx])
            batch_machine = int(parts[idx + 1])
            batch_time = int(parts[idx + 2])

            if batch_machine_count != 1:
                self.errors.append(f"J{job_idx+1}: 批处理工序机器数量错误，应该是1台")
                return False

            if batch_machine < machine_ranges['batch'][0] or batch_machine > machine_ranges['batch'][1]:
                self.errors.append(f"J{job_idx+1}: 批处理机编号超出范围: M{batch_machine} (应为M{machine_ranges['batch'][0]}-M{machine_ranges['batch'][1]})")
                return False

            if batch_time != 0:
                self.errors.append(f"J{job_idx+1}: 批处理时间应该是0: {batch_time}")
                return False

            idx += 3
            operation_count += 1

            # 离散工序
            for op_idx in range(discrete_ops):
                if idx >= len(parts):
                    self.errors.append(f"J{job_idx+1} 离散工序{op_idx+1}: 数据不完整")
                    return False

                machine_count = int(parts[idx])
                idx += 1

                # 验证离散工序的机器
                for _ in range(machine_count):
                    if idx + 1 >= len(parts):
                        self.errors.append(f"J{job_idx+1} 离散工序{op_idx+1}: 机器-时间对不完整")
                        return False

                    machine_id = int(parts[idx])
                    process_time = int(parts[idx + 1])

                    if machine_id < machine_ranges['discrete'][0] or machine_id > machine_ranges['discrete'][1]:
                        self.errors.append(f"J{job_idx+1} 离散工序{op_idx+1}: 离散机编号超出范围: M{machine_id} (应为M{machine_ranges['discrete'][0]}-M{machine_ranges['discrete'][1]})")
                        return False

                    if process_time <= 0:
                        self.errors.append(f"J{job_idx+1} 离散工序{op_idx+1}: 加工时间不能为负数或零: {process_time}")
                        return False

                    if process_time < 600 or process_time > 1200:
                        self.warnings.append(f"J{job_idx+1} 离散工序{op_idx+1}: 加工时间超出推荐范围: {process_time} (建议600-1200)")

                    idx += 2

                operation_count += 1

            # 验证工序总数
            expected_total_ops = 2 + discrete_ops  # 打印 + 批处理 + 离散
            if operation_count != expected_total_ops:
                self.errors.append(f"J{job_idx+1}: 工序总数不匹配，期望{expected_total_ops}个，实际{operation_count}个")
                return False

        except ValueError as e:
            self.errors.append(f"J{job_idx+1}: 包含非数字参数 - {e}")
            return False
        except IndexError as e:
            self.errors.append(f"J{job_idx+1}: 数据索引错误 - {e}")
            return False

        return True

    def _report_results(self) -> bool:
        """报告验证结果"""
        if self.errors:
            print("❌ 发现错误:"            for error in self.errors[:10]:  # 只显示前10个错误
                print(f"   {error}")
            if len(self.errors) > 10:
                print(f"   ... 还有{len(self.errors) - 10}个错误")
            return False

        if self.warnings:
            print("⚠️ 发现警告:"            for warning in self.warnings[:5]:  # 只显示前5个警告
                print(f"   {warning}")
            if len(self.warnings) > 5:
                print(f"   ... 还有{len(self.warnings) - 5}个警告")

        print("✅ 算例文件验证通过")
        return True

def main():
    """主函数"""
    instance_dir = r'src\main\resources\instance'

    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return

    files = [f for f in os.listdir(instance_dir) if f.endswith('.txt')]
    files.sort()

    print(f"开始验证 {len(files)} 个算例文件...")

    validator = InstanceValidator()
    valid_count = 0
    total_errors = 0
    total_warnings = 0

    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        if validator.validate_file(filepath):
            valid_count += 1
        total_errors += len(validator.errors)
        total_warnings += len(validator.warnings)

    # 最终汇总
    print(f"\n{'='*80}")
    print("验证结果汇总")
    print(f"{'='*80}")
    print(f"总文件数: {len(files)}")
    print(f"验证通过: {valid_count}")
    print(f"验证失败: {len(files) - valid_count}")
    print(f"总错误数: {total_errors}")
    print(f"总警告数: {total_warnings}")

    if valid_count == len(files):
        print("\n🎉 所有算例文件验证通过！可以正常用于算法测试。")
    else:
        print(f"\n⚠️ 有 {len(files) - valid_count} 个文件需要修复。")
        print("请检查上述错误信息并修复相应问题。")

if __name__ == '__main__':
    main()

