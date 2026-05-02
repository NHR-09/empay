@echo off
set PGBIN=C:\postgres\16\pgsql\bin
set PGDATA=C:\postgres\16\pgsql\data
set PGPASSWORD=postgres
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%

echo ==========================================
echo  EmPay HRMS - Setup and Run
echo ==========================================

:: Step 1 - Register and start PostgreSQL service
echo [1/4] Starting PostgreSQL...
sc query PostgreSQL16 >nul 2>&1
if errorlevel 1 (
    %PGBIN%\pg_ctl.exe register -N "PostgreSQL16" -D "%PGDATA%"
)
net start PostgreSQL16 >nul 2>&1
timeout /t 3 /nobreak >nul

:: Step 2 - Run schema (creates DB + tables if not exists)
echo [2/4] Applying schema...
%PGBIN%\psql.exe -U postgres -tc "SELECT 1 FROM pg_database WHERE datname='empay_hrms'" | findstr /C:"1" >nul 2>&1
if errorlevel 1 (
    %PGBIN%\psql.exe -U postgres -f "empay_schema.sql"
    echo Schema applied.
) else (
    echo Database already exists, skipping schema.
)

:: Step 3 - Run migrations
echo [3/4] Applying migrations...
%PGBIN%\psql.exe -U postgres -d empay_hrms -f "migrate_enums.sql" >nul 2>&1
%PGBIN%\psql.exe -U postgres -d empay_hrms -f "migrate.sql" >nul 2>&1
echo Migrations done.

:: Step 4 - Start Spring Boot
echo [4/4] Starting EmPay application...
C:\maven\apache-maven-3.9.6\bin\mvn.cmd spring-boot:run

pause
