@echo off 

set "filename=%~1"
for /f "tokens=1,2,3 delims=_." %%a in ("%filename%") do (
    set "instance=%%a"
    set "q1=%%b"
    set "q2=%%c"
)

java -jar validator.jar input/%instance%.txt %q1% %q2% output/%filename%
