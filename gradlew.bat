@rem Gradle wrapper launcher (Windows). Requires gradle-wrapper.jar present.
@if not exist "%~dp0gradle\wrapper\gradle-wrapper.jar" (
    echo gradle-wrapper.jar missing. Run gradle wrapper --gradle-version 8.6 or open in Android Studio.
    exit /b 1
)
@if "%JAVA_HOME%"=="" (set JAVA_EXE=java) else (set JAVA_EXE="%JAVA_HOME%\bin\java")
%JAVA_EXE% -classpath "%~dp0gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
