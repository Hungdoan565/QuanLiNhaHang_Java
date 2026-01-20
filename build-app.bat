@echo off
REM ========================================================
REM RestaurantPOS - Build Windows Application Script
REM Creates standalone .exe installer with bundled JRE
REM ========================================================

echo.
echo ========================================
echo   RestaurantPOS - Build Application
echo ========================================
echo.

REM Check Java version
echo [1/4] Checking Java version...
java -version 2>&1 | findstr "17" >nul
if errorlevel 1 (
    echo ERROR: Java 17+ required!
    echo Please install JDK 17 or higher.
    pause
    exit /b 1
)
echo OK - Java 17+ detected

REM Clean and build Fat JAR
echo.
echo [2/4] Building Fat JAR with Maven...
call mvn clean package -DskipTests
if errorlevel 1 (
    echo ERROR: Maven build failed!
    pause
    exit /b 1
)
echo OK - JAR built successfully

REM Create output directory
set OUTPUT_DIR=target\installer
if not exist "%OUTPUT_DIR%" mkdir "%OUTPUT_DIR%"

REM Find the shaded JAR
set JAR_FILE=target\restaurant-pos-1.0.0.jar
if not exist "%JAR_FILE%" (
    echo ERROR: JAR file not found: %JAR_FILE%
    pause
    exit /b 1
)

echo.
echo [3/4] Creating Windows Application with jpackage...

REM Check if jpackage is available
where jpackage >nul 2>&1
if errorlevel 1 (
    echo WARNING: jpackage not found in PATH
    echo.
    echo Your Fat JAR is ready at: %JAR_FILE%
    echo Run with: java -jar %JAR_FILE%
    echo.
    echo To create .exe installer, ensure JAVA_HOME\bin is in PATH
    echo and jpackage is available (JDK 14+)
    pause
    exit /b 0
)

REM Create Windows installer with jpackage
jpackage ^
    --type exe ^
    --name "RestaurantPOS" ^
    --app-version "1.0.0" ^
    --vendor "Restaurant" ^
    --description "Hệ thống Quản lý Nhà hàng" ^
    --input target ^
    --main-jar restaurant-pos-1.0.0.jar ^
    --main-class com.restaurant.Main ^
    --dest "%OUTPUT_DIR%" ^
    --win-dir-chooser ^
    --win-menu ^
    --win-shortcut ^
    --win-menu-group "Restaurant" ^
    --java-options "-Xms256m" ^
    --java-options "-Xmx1024m"

if errorlevel 1 (
    echo ERROR: jpackage failed!
    echo.
    echo Your Fat JAR is still available at: %JAR_FILE%
    echo Run with: java -jar %JAR_FILE%
    pause
    exit /b 1
)

echo.
echo [4/4] Build Complete!
echo ========================================
echo.
echo Outputs:
echo   JAR:       %JAR_FILE%
echo   Installer: %OUTPUT_DIR%\RestaurantPOS-1.0.0.exe
echo.
echo To run JAR:  java -jar %JAR_FILE%
echo To install:  Run RestaurantPOS-1.0.0.exe
echo.
pause
