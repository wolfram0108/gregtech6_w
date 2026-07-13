# Гейт ядра-среза: компиляция юнита-688 (центр gregapi + мосты) + compat-mirror, изолированно.
# Печатает число ошибок. Воспроизводимый механический судья для петли распространения центров.
param([string]$log="core_probe_v2.log")
$javac="C:\Program Files\Eclipse Adoptium\jdk-25.0.3.9-hotspot\bin\javac.exe"
$cp=(Get-Content core_cp_value.txt -Raw).Trim()
Remove-Item -Recurse -Force build\core_probe_v2 -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force build\core_probe_v2 | Out-Null
& $javac -d build\core_probe_v2 -cp $cp -encoding UTF-8 -proc:none -nowarn -Xmaxerrs 100000 "@core_unit_v2_files.txt" "@mirror_files.txt" 2> $log
$n=(Select-String -Path $log -Pattern ': error:' -AllMatches).Count
"ERRORS: $n"
