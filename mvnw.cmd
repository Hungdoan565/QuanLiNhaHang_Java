@REM ----------------------------------------------------------------------------
@REM Maven Wrapper startup batch script for Windows
@REM ----------------------------------------------------------------------------

@echo off
setlocal

set MAVEN_WRAPPER_JAR="%~dp0.mvn\wrapper\maven-wrapper.jar"
set MAVEN_WRAPPER_PROPERTIES="%~dp0.mvn\wrapper\maven-wrapper.properties"

@REM Download maven-wrapper.jar if not exists
if not exist %MAVEN_WRAPPER_JAR% (
    echo Downloading Maven wrapper...
    powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%~dp0.mvn\wrapper\maven-wrapper.jar'}"
)

set JAVA_EXE=java.exe
%JAVA_EXE% -jar %MAVEN_WRAPPER_JAR% %*
