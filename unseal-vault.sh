#!/bin/sh
# 请确保脚本有执行权限：chmod +x unseal-vault.sh

# Vault 容器名称
VAULT_CONTAINER="open-disk-vault-1"

# 定义三个 Unseal Key（空格分隔）
KEYS="2c96218f392f98e5b1772ecb4e94c2d134fa1e47fc235bcba5cfb2fa7528db1b63 5a82c59ae847b45af630e3e5ecb5c7de670f1b9813e9242531108a1a2366d33a45 65a391e59bb07e1bc02c66683bfdd4630fa2e761190050af36f0c67c9f66611434"

# 循环遍历每个 Key 来解封 Vault
for key in $KEYS; do
    # 仅显示 key 的前6位，避免全部暴露
    short_key=$(echo "$key" | cut -c1-6)
    echo "Unsealing with key: ${short_key}..."

    # 将 key 通过标准输入传递给 docker exec 命令
    echo "$key" | docker exec -i "$VAULT_CONTAINER" vault operator unseal

    # 暂停1秒，确保命令有足够时间执行
    sleep 1
done

echo "Vault unseal 操作已完成！"
