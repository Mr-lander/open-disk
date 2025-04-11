FROM minio/minio:latest

# 暴露 Minio 服务端口及控制台端口
EXPOSE 9000 9001

# 使用 /data 目录作为数据存储目录
CMD ["server", "/data", "--console-address", ":9001"]
