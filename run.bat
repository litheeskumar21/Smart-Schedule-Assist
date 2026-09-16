@echo off
title Smart Schedule Assist
echo ===================================================
echo   Starting Smart Schedule Assist Application...
echo ===================================================

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "MAVEN_HOME=C:\Program Files\apache-maven-3.9.16"
set "PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%"

echo Checking Java version...
java -version

echo.
echo Starting Spring Boot application with Maven...
echo Access application at: http://localhost:8080
echo.

call mvn spring-boot:run
pause
