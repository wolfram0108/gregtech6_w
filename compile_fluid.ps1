# Гейт F5-fluid: core_unit_v2 + Ocean/River/Swamp (смежные fluid-блоки, не входящие в core-688 напрямую).
# Локальный вспомогательный скрипт исполнителя, не трогает основной compile_core.ps1.
param([string]$log="fluid_probe.log")
$javac="C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe"
$cp=(Get-Content core_cp_value.txt -Raw).Trim()
Remove-Item -Recurse -Force build\fluid_probe -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build\fluid_probe | Out-Null
& $javac -d build\fluid_probe -cp $cp -encoding UTF-8 -proc:none -nowarn -Xmaxerrs 100000 "@C:\Users\vova\AppData\Local\Temp\claude\D--Temp-MC-NEW\e6532b09-1da0-4d31-b5a4-b65bf554a57c\scratchpad\fluid_files.txt" "@mirror_files.txt" 2> $log
$n=(Select-String -Path $log -Pattern ': error:' -AllMatches).Count
"ERRORS: $n"
