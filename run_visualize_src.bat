@echo off
chcp 65001 >nul
cd /d "%~dp0"
python src\visualize_src_experiments.py
pause
