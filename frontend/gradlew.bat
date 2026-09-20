@rem
@rem Dincharya — self-bootstrapping Gradle wrapper (Windows).
@rem Downloads the official wrapper JAR on first use; the repo stays text-only.
@rem

@echo off
setlocal

set DIRNAME=%~dp0
set JAR=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

if not exist "%JAR%" (
    echo Gradle wrapper JAR not found - downloading it once...
    powershell -NoProfile -Command "try { Invoke-WebRequest -Uri 'https://raw.githubusercontent.com/gradle/gradle/v8.7.0/gradle/wrapper/gradle-wrapper.jar' -OutFile '%JAR%' } catch { exit 1 }"
    if errorlevel 1 (
        echo ERROR: could not download the wrapper JAR. Check your network connection.
        exit /b 1
    )
)

if defined JAVA_HOME (set JAVACMD=%JAVA_HOME%\bin\java.exe) else (set JAVACMD=java)

"%JAVACMD%" -cp "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
