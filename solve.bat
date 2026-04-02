@set CLASSPATH=lib\*;target\*
@set MAIN_CLASS=oeis.a136094.Main
@set ARGS=%*

@set DBG=
::@set DBG=-Xverify:none -Djdk.attach.allowAttachSelf
::@set DBG=-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=8000

@for /f %%i in ('powershell -Command "Get-Date -Format yyyyMMdd_HHmmss"') do @set TIMESTAMP=%%i
@set SOLVE_LOG=solve_%TIMESTAMP%.log

java %DBG% -cp %CLASSPATH% %MAIN_CLASS% %ARGS% 2>&1 | powershell -Command "$input | Tee-Object %SOLVE_LOG%"
