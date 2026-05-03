@echo off
setlocal
set GRADLE_VERSION=8.7
set BASE_DIR=%~dp0
set GRADLE_USER_HOME=%BASE_DIR%\.gradle
set DIST_DIR=%GRADLE_USER_HOME%\wrapper\dists\gradle-%GRADLE_VERSION%-bin
set GRADLE_BIN=%DIST_DIR%\gradle-%GRADLE_VERSION%\bin\gradle.bat
set ZIP_PATH=%DIST_DIR%\gradle-%GRADLE_VERSION%-bin.zip
set DIST_URL=https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip

if not exist "%GRADLE_BIN%" (
  if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
  if not exist "%ZIP_PATH%" (
    powershell -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%ZIP_PATH%'"
  )
  powershell -Command "Expand-Archive -Force '%ZIP_PATH%' '%DIST_DIR%'"
)

"%GRADLE_BIN%" %*
endlocal
