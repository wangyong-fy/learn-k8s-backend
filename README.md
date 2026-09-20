# learn-k8s-backend

K8s 全栈演示 — **后端**（Spring Boot 3 + JDK17 + Maven + MySQL 8）

## 技术栈

- Spring Boot 3.3.5 / JDK 17
- Spring Data JPA + MySQL Connector
- 多阶段 Dockerfile（Maven 构建 → JRE 运行），可被 kaniko 无 daemon 构建

## 接口

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/messages` | 查询全部消息（按 id 倒序） |
| POST | `/api/messages` | 新增消息，body `{"content":"..."}` |
| GET | `/api/health` | 健康检查，返回处理请求的 Pod 名与记录数 |

`/api/health` 返回示例：

```json
{"pod":"backend-79f6f895b8-tk8gk","count":3,"status":"UP"}
```

`pod` 字段可用来证明 2 副本负载均衡；`count` 递增可证明数据真实落库。

## 配置（全部走环境变量，密码不写死）

`src/main/resources/application.properties` 中：

```
DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD
```

在 K8s 里由 `k8s/backend.yaml` 通过 **Secret `mysql-secret`** 注入，仓库里**不含任何真实密码**。

## K8s 清单

`k8s/backend.yaml` — Deployment(2 副本) + Service(ClusterIP:8080)
`k8s/ingress.yaml` — Ingress `k8s-demo.local`：`/api` → backend、`/` → frontend

## CI/CD

`Jenkinsfile`（声明式流水线）：

1. `Checkout` 拉取本仓库
2. `Build & Push Backend` — 起 kaniko Pod 构建并推送 `wanyongdoker/learn-k8s-backend:<TAG>`
3. `Build & Push Frontend` — 同上，构建前端仓库 `learn-k8s-frontend`
4. `Deploy to K8s` — `kubectl apply` + `set image` + `rollout status`

> 需在 `app` 与 `jenkins` 两个命名空间预先创建 `dockerhub` 镜像拉取/推送密钥。

## 本地运行

```bash
mvn -s settings.xml spring-boot:run
```

依赖可用的 MySQL（默认连 `mysql.app.svc.cluster.local:3306/appdb`，本地可通过 `DB_HOST` 等变量覆盖）。
