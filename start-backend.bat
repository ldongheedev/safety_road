@echo off
set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot
cd /d "%~dp0backend"
mvnw.cmd spring-boot:run
