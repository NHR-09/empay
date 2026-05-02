@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
C:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run
pause
