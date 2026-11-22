@echo off
REM Build and run I-ITSM on Windows. Needs JDK 17+.
REM   run.bat        demo + tests
REM   run.bat test   tests only
REM   run.bat web    web app at http://localhost:8080 (data kept in .\data)
REM   run.bat reset  same, but wipe .\data first and start from the demo set-up
cd /d %~dp0
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > sources.txt
javac -encoding UTF-8 -d out @sources.txt
del sources.txt
if "%1"=="web" goto web
if "%1"=="reset" goto reset
if "%1"=="test" goto test
java -cp out itsm.Main
java -cp out itsm.TestRunner
goto :eof
:web
java -cp out itsm.web.DashboardServer 8080
goto :eof
:reset
java -cp out itsm.web.DashboardServer 8080 --reset
goto :eof
:test
java -cp out itsm.TestRunner
