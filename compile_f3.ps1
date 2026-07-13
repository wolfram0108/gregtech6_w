# Гейт F3-client-render: core_unit_v2 + client-render файлы вне core-688 (GT_Client, render entity/player,
# TESR/GuiContainer/NEI multitileentity вне ядра). Локальный вспомогательный скрипт исполнителя F3,
# не трогает основной compile_core.ps1 (паттерн — compile_fluid.ps1).
param([string]$log="f3_probe.log")
$javac="C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe"
$cp=(Get-Content core_cp_value.txt -Raw).Trim()
Remove-Item -Recurse -Force build\f3_probe -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build\f3_probe | Out-Null
& $javac -d build\f3_probe -cp $cp -encoding UTF-8 -proc:none -nowarn -Xmaxerrs 100000 "@core_unit_v2_files.txt" "@mirror_files.txt" "@f3_extra_files.txt" 2> $log
$n=(Select-String -Path $log -Pattern ': error:' -AllMatches).Count
"ERRORS: $n"
