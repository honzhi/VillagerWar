$env:JAVA_HOME = "C:\java21"
$env:PATH = "C:\Users\28671\Desktop\我的世界插件\apache-maven-3.9.9\bin;$env:PATH"

Write-Host "===== 编译项目 ====="
mvn compile 2>&1 | Select-String -Pattern "BUILD|ERROR"

if ($LASTEXITCODE -ne 0) {
    Write-Host "===== 编译失败，跳过部署 ====="
    exit 1
}

Write-Host "`n===== 打包 JAR ====="
mvn package 2>&1 | Select-String -Pattern "BUILD|Building jar"

$jarName = "villagerwar-1.0.0.jar"
$source = "target\$jarName"
$target = "C:\Users\28671\Desktop\测试服务器\plugins\$jarName"

if (Test-Path $source) {
    Copy-Item $source $target -Force
    Write-Host "`n===== 部署成功 ====="
    Write-Host "已部署到: $target"
    Write-Host "文件大小: $((Get-Item $target).Length / 1KB) KB"
} else {
    Write-Host "错误: 未找到 $source"
    exit 1
}
