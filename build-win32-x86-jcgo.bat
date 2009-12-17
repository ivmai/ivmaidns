@echo off
rem @(#) build-win32-x86-jcgo.bat - Windows build script for dnslook, dnszcon.
rem Used tools: JCGO, MinGW.

set PROJ_UNIX_NAME=ivmaidns
set DIST_DIR=.dist-win32-x86-jcgo

echo Building Win32/x86 executable using JCGO+MinGW...

if "%JCGO_HOME%"=="" set JCGO_HOME=C:\JCGO
if "%MINGW_ROOT%"=="" set MINGW_ROOT=C:\MinGW

if not exist "%DIST_DIR%" mkdir "%DIST_DIR%"
if not exist "%DIST_DIR%\%PROJ_UNIX_NAME%" mkdir "%DIST_DIR%\%PROJ_UNIX_NAME%"
if exist "%DIST_DIR%\%PROJ_UNIX_NAME%\dnslook.exe" del "%DIST_DIR%\%PROJ_UNIX_NAME%\dnslook.exe"
if exist "%DIST_DIR%\%PROJ_UNIX_NAME%\dnszcon.exe" del "%DIST_DIR%\%PROJ_UNIX_NAME%\dnszcon.exe"

if not exist "%DIST_DIR%\.jcgo_Out-dnslook" mkdir "%DIST_DIR%\.jcgo_Out-dnslook"
%JCGO_HOME%\jcgo -d "%DIST_DIR%\.jcgo_Out-dnslook" -src $~/goclsp/clsp_asc -src src net.sf.%PROJ_UNIX_NAME%.dnslook @$~/stdpaths.in
if errorlevel 1 goto exit

if not exist "%DIST_DIR%\.jcgo_Out-dnszcon" mkdir "%DIST_DIR%\.jcgo_Out-dnszcon"
%JCGO_HOME%\jcgo -d "%DIST_DIR%\.jcgo_Out-dnszcon" -src $~/goclsp/clsp_asc -src src net.sf.%PROJ_UNIX_NAME%.dnszcon @$~/stdpaths.in
if errorlevel 1 goto exit

echo Compiling dnslook...
"%MINGW_ROOT%\bin\gcc" -o "%DIST_DIR%\%PROJ_UNIX_NAME%\dnslook" -I%JCGO_HOME%\include -I%JCGO_HOME%\native -Os -fwrapv -fno-strict-aliasing -DJCGO_FFDATA -DJCGO_NOGC -DJCGO_NOJNI -DJCGO_NOSEGV -DJCGO_WIN32 -DJCGO_INET -DJNIIMPORT=static/**/inline -DJNIEXPORT=JNIIMPORT -DJNUBIGEXPORT=static -DJCGO_NOFP -fno-optimize-sibling-calls -s "%DIST_DIR%\.jcgo_Out-dnslook\Main.c" -lwsock32
if errorlevel 1 goto exit

echo Compiling dnszcon...
"%MINGW_ROOT%\bin\gcc" -o "%DIST_DIR%\%PROJ_UNIX_NAME%\dnszcon" -I%JCGO_HOME%\include -I%JCGO_HOME%\include\boehmgc -I%JCGO_HOME%\native -O2 -fwrapv -fno-strict-aliasing -DJCGO_FFDATA -DJCGO_THREADS -DJCGO_WIN32 -DJCGO_USEGCJ -DJCGO_INET -DGC_NO_THREAD_REDIRECTS -DJCGO_NOJNI -DJCGO_NOSEGV -DGCSTATICDATA= -DATTRIBNONGC=__attribute__((section(\".dataord\"))) -DJCGO_GCRESETDLS -DJCGO_NOFP -fno-optimize-sibling-calls -s "%DIST_DIR%\.jcgo_Out-dnszcon\Main.c" %JCGO_HOME%\libs\x86\mingw\libgcmt.a -lwsock32
if errorlevel 1 goto exit

copy /y /b GNU_GPL.txt "%DIST_DIR%\%PROJ_UNIX_NAME%"
copy /y /b README.txt "%DIST_DIR%\%PROJ_UNIX_NAME%"
echo .

"%DIST_DIR%\%PROJ_UNIX_NAME%\dnslook.exe"
echo .

"%DIST_DIR%\%PROJ_UNIX_NAME%\dnszcon.exe"
echo .

echo BUILD SUCCESSFUL

:exit
