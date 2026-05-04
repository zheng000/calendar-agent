@rem
@rem Gradle startup script for Windows
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem  Gradle startup script for Windows
@rem ##########################################################################

set APP_HOME=%~dp0
set APP_BASE_NAME=%~n0
set CLASSPATH=%APP_HOME%gradle\wrapper\gradle-wrapper.jar

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome
set JAVA_HOME=
set JAVA_EXE=java.exe
where java.exe >nul 2>&1 || goto fail
goto exec

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto exec

:fail
echo ERROR: JAVA_HOME is not set and no 'java' command could be found.
goto end

:exec
set JAVA_OPTS=-Xmx64m -Xms64m
"%JAVA_EXE%" %JAVA_OPTS% -classpath "%CLASSPATH%" org.gradle.wrapper.GradleWrapperMain %*

:end
