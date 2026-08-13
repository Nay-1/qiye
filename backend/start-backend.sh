#!/bin/bash
# 后端启动脚本（直连外部 AI 服务，不注入本地代理）
# 用法：./start-backend.sh
cd "$(dirname "$0")"

# 加载本地密钥 .env（不入库）；未配置时 AI 自动降级为 mock，业务不受影响
if [ -f .env ]; then
  set -a
  . ./.env
  set +a
fi

# 清空 shell 里的系统代理环境变量（.bashrc 的 proxy 函数可能已 export），确保 Java 直连
unset HTTP_PROXY HTTPS_PROXY ALL_PROXY
unset http_proxy https_proxy all_proxy

# 直连模式：不注入 JVM 代理。如需走本地代理（如 FlClash:7890），取消下行注释
# export JAVA_TOOL_OPTIONS="-Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890 -Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890"
exec mvn -q -B spring-boot:run
