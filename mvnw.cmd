@ECHO OFF
SETLOCAL

SET BASE_DIR=%~dp0
SET WRAPPER_PROPERTIES=%BASE_DIR%\.mvn\wrapper\maven-wrapper.properties

IF NOT EXIST "%WRAPPER_PROPERTIES%" (
  ECHO Missing maven-wrapper.properties at %WRAPPER_PROPERTIES%
  EXIT /B 1
)

FOR /F "usebackq tokens=1,* delims==" %%A IN ("%WRAPPER_PROPERTIES%") DO (
  IF "%%A"=="distributionUrl" SET DIST_URL=%%B
)

IF "%DIST_URL%"=="" (
  ECHO distributionUrl is missing in maven-wrapper.properties
  EXIT /B 1
)

FOR %%I IN ("%DIST_URL%") DO SET DIST_BASENAME=%%~nxI
SET DIST_NAME=%DIST_BASENAME:-bin.zip=%

IF "%MAVEN_USER_HOME%"=="" (
  SET M2_HOME_DIR=%USERPROFILE%\.m2
) ELSE (
  SET M2_HOME_DIR=%MAVEN_USER_HOME%
)

SET CACHE_DIR=%M2_HOME_DIR%\wrapper\dists\%DIST_NAME%
SET MAVEN_DIR=%CACHE_DIR%\%DIST_NAME%
SET MAVEN_BIN=%MAVEN_DIR%\bin\mvn.cmd

IF NOT EXIST "%MAVEN_BIN%" (
  IF NOT EXIST "%CACHE_DIR%" MKDIR "%CACHE_DIR%"
  SET TMP_ZIP=%CACHE_DIR%\%DIST_BASENAME%

  IF NOT EXIST "%TMP_ZIP%" (
    powershell -NoProfile -ExecutionPolicy Bypass -Command "Invoke-WebRequest -Uri '%DIST_URL%' -OutFile '%TMP_ZIP%'"
  )

  powershell -NoProfile -ExecutionPolicy Bypass -Command "Expand-Archive -Path '%TMP_ZIP%' -DestinationPath '%CACHE_DIR%' -Force"
)

CALL "%MAVEN_BIN%" %*
ENDLOCAL
