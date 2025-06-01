FROM hashicorp/vault:1.15.0

# 将本地配置文件复制到容器中的 Vault 配置目录
COPY config/config.hcl /vault/config/config.hcl

# 设置 Vault 启动命令，指向配置文件（也可以通过命令行参数指定）
CMD ["server", "-config=/vault/config/config.hcl"]
