# -------------------------------
# Vault 自动解封脚本 (PowerShell)
# -------------------------------

# 设置你的 Vault 容器名
$vaultContainer = "open-disk-vault-1"

# 三个 Unseal Keys（从你 init 的结果中复制）
$unsealKeys = @(
    "2c96218f392f98e5b1772ecb4e94c2d134fa1e47fc235bcba5cfb2fa7528db1b63",
    "5a82c59ae847b45af630e3e5ecb5c7de670f1b9813e9242531108a1a2366d33a45",
    "65a391e59bb07e1bc02c66683bfdd4630fa2e761190050af36f0c67c9f66611434"
)

# 遍历执行 unseal 命令
foreach ($key in $unsealKeys) {
    Write-Host "`nUnsealing with key: $($key.Substring(0, 6))..."
    docker exec -i $vaultContainer vault operator unseal <<<$key
    Start-Sleep -Seconds 1
}
