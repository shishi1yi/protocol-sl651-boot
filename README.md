# protocol-sl651-boot

## 基础配置讲解

```yaml
#服务器端口
server:
  port: 8200

# 数据源配置 (排除数据库连接)
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
  rabbitmq:
    host: 127.0.0.1
    port: 5672
    username: guest #用户名
    password: guest #密码
    virtual-host: protocol_sl651
    #消息确认配置项
    #确认消息已发送到交换机(Exchange)
    publisher-confirm-type: correlated
    #确认消息已发送到队列(Queue)
    publisher-returns: true

sl651:
  # netty配置
  netty:
    port: 8651
    # 消息帧最大体积
    max-frame-length: 4112
    # 消息的交换机名称
    publisher-exchange: "sl651.exchange"
    # 消息的路由键
    publisher-routing-key: "sl651.publisher"
    # 消息的过期时间
    publisher-delay: 86400000
    # 小时报是否上报
    hourReportDisplay: true
    # 定时报是否上报
    regularReportDisplay: false
    # 加时报是否上报
    overtimeReportDisplay: false
    # 湖南协议解析
    hunanTransfer: false
  property:
    limits:
       # 5分钟降雨量
      - {code: "22", open: 0, close: 30}
       # 1小时降水量
      - {code: "1a", open: 0, close: 305}

```





## 启动配置

无须单独编写配置文件打包，只需在启动命令上添加参数即可

- 必填参数

> 指定MQ的连接地址

```
-Dspring.rabbitmq.host={MQ的IP} -Dspring.rabbitmq.port={MQ的端口} -Dspring.rabbitmq.username={MQ的用户} -Dspring.rabbitmq.password={MQ的密码}
```

- 选填参数

> 指定MQ的虚拟机、web端口、协议端口等

```
-Dspring.rabbitmq.virtualHost={MQ的虚拟机} -Dserver.port={web端口} -Dsl651.netty.port={协议端口}
```

