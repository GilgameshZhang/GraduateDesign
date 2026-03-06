@echo off
cd /d "%~dp0"
mvn clean compile -DskipTests -q

