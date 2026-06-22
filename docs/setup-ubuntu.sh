#!/bin/bash
# ============================================
# Ubuntu 中间件一键安装脚本
# 适用于 Ubuntu 20.04/22.04/24.04
# ============================================

set -e

echo "===== 更新系统 ====="
sudo apt update && sudo apt upgrade -y

# ============================================
# 1. MySQL 8.0
# ============================================
echo "===== 安装 MySQL ====="
sudo apt install -y mysql-server
sudo systemctl start mysql
sudo systemctl enable mysql

# 设置 root 密码（改成你自己的）
sudo mysql -e "ALTER USER 'root'@'localhost' IDENTIFIED WITH mysql_native_password BY 'root123';"
sudo mysql -e "FLUSH PRIVILEGES;"

echo "MySQL 安装完成，验证："
mysql --version

# ============================================
# 2. Redis
# ============================================
echo "===== 安装 Redis ====="
sudo apt install -y redis-server
sudo systemctl start redis-server
sudo systemctl enable redis-server

# 设置密码（改成你自己的）
sudo sed -i 's/# requirepass foobared/requirepass redis123/' /etc/redis/redis.conf
sudo systemctl restart redis-server

echo "Redis 安装完成，验证："
redis-cli --version

# ============================================
# 3. RabbitMQ
# ============================================
echo "===== 安装 RabbitMQ ====="
# 安装依赖
sudo apt install -y curl gnupg apt-transport-https

# 添加 Erlang 源
curl -1sLf "https://keys.openpgp.org/vks/v1/B8BC2D6B2378FE5E8E98A35E51DDD621B71C1717" | sudo gpg --dearmor | sudo tee /usr/share/keyrings/rabbitmq.gpg > /dev/null

curl -1sLf "https://packagecloud.io/rabbitmq/erlang/gpgkey" | sudo gpg --dearmor | sudo tee /usr/share/keyrings/rabbitmq_erlang.gpg > /dev/null

curl -1sLf "https://packagecloud.io/rabbitmq/rabbitmq-server/gpgkey" | sudo gpg --dearmor | sudo tee /usr/share/keyrings/rabbitmq_server.gpg > /dev/null

# 添加源
echo "deb [signed-by=/usr/share/keyrings/rabbitmq_erlang.gpg] https://packagecloud.io/rabbitmq/erlang/ubuntu/ $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/rabbitmq_erlang.list

echo "deb [signed-by=/usr/share/keyrings/rabbitmq_server.gpg] https://packagecloud.io/rabbitmq/rabbitmq-server/ubuntu/ $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/rabbitmq_server.list

sudo apt update
sudo apt install -y rabbitmq-server

sudo systemctl start rabbitmq-server
sudo systemctl enable rabbitmq-server

# 启用管理插件（Web管理界面：http://localhost:15672）
sudo rabbitmq-plugins enable rabbitmq_management

echo "RabbitMQ 安装完成，默认账号 guest/guest"

# ============================================
# 4. ElasticSearch 8.x
# ============================================
echo "===== 安装 ElasticSearch ====="
# 安装 Java（ES 依赖）
sudo apt install -y openjdk-17-jdk

# 添加 ES 源
curl -fsSL https://artifacts.elastic.co/GPG-KEY-elasticsearch | sudo gpg --dearmor -o /usr/share/keyrings/elastic.gpg

echo "deb [signed-by=/usr/share/keyrings/elastic.gpg] https://artifacts.elastic.co/packages/8.x/apt stable main" | sudo tee /etc/apt/sources.list.d/elastic-8.x.list

sudo apt update
sudo apt install -y elasticsearch

# 配置 ES（单节点模式 + 关闭安全认证，开发用）
sudo sed -i 's/#cluster.name: my-application/cluster.name: ai-interview/' /etc/elasticsearch/elasticsearch.yml
sudo sed -i 's/#network.host: 192.168.0.1/network.host: 0.0.0.0/' /etc/elasticsearch/elasticsearch.yml
sudo sed -i 's/#discovery.type: single-node/discovery.type: single-node/' /etc/elasticsearch/elasticsearch.yml

# 关闭安全认证（开发环境）
echo "xpack.security.enabled: false" | sudo tee -a /etc/elasticsearch/elasticsearch.yml

# 调整 JVM 内存（2G 服务器建议 512m）
sudo sed -i 's/-Xms1g/-Xms512m/' /etc/elasticsearch/jvm.options
sudo sed -i 's/-Xmx1g/-Xmx512m/' /etc/elasticsearch/jvm.options

sudo systemctl start elasticsearch
sudo systemctl enable elasticsearch

echo "ElasticSearch 安装完成，验证："
curl -s http://localhost:9200 | head -5

# ============================================
# 完成
# ============================================
echo ""
echo "=========================================="
echo "所有中间件安装完成！"
echo "=========================================="
echo ""
echo "服务状态："
sudo systemctl status mysql --no-pager -l | head -3
sudo systemctl status redis-server --no-pager -l | head -3
sudo systemctl status rabbitmq-server --no-pager -l | head -3
sudo systemctl status elasticsearch --no-pager -l | head -3
echo ""
echo "连接信息："
echo "MySQL:      localhost:3306  用户: root  密码: root123"
echo "Redis:      localhost:6379  密码: redis123"
echo "RabbitMQ:   localhost:5672  管理界面: http://localhost:15672 (guest/guest)"
echo "ElasticSearch: http://localhost:9200"
echo ""
echo "Java 客户端依赖（加到 pom.xml）："
echo "  - MySQL: mysql-connector-java"
echo "  - Redis: spring-boot-starter-data-redis"
echo "  - Redisson: redisson-spring-boot-starter"
echo "  - RabbitMQ: spring-boot-starter-amqp"
echo "  - ElasticSearch: spring-boot-starter-data-elasticsearch"
