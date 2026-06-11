@REM Maven Wrapper for Windows
@echo off
set MAVEN_PROJECTBASEDIR=%~dp0
set MAVEN_HOME=%USERPROFILE%\.m2\wrapper\dists\apache-maven-3.9.9
if not exist "%MAVEN_HOME%\bin\mvn.cmd" (
    echo Downloading Maven...
    powershell -Command "Invoke-WebRequest -Uri 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.9/apache-maven-3.9.9-bin.zip' -OutFile '%TEMP%\maven.zip'"
    powershell -Command "Expand-Archive -Path '%TEMP%\maven.zip' -DestinationPath '%USERPROFILE%\.m2\wrapper\dists' -Force"
)
"%MAVEN_HOME%\bin\mvn.cmd" %*
