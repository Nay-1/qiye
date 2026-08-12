#!/bin/bash
# 后端启动脚本（自动注入 JVM 代理，用于访问 OpenCode Go 海外服务）
# 用法：./start-backend.sh   （若本机代理端口不是 7890，修改下方代理行）
cd "$(dirname "$0")"

# 加载本地密钥 .env（不入库）；未配置时 AI 自动降级为 mock，业务不受影响
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

export JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890"
exec mvn -q -B spring-boot:run
