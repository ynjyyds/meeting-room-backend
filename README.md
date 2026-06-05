# 📋 智能会议室预约系统 - 后端

> 企业级会议室预约系统后端，支持 JWT 认证、预约审核、邮件通知、Excel 导出等核心功能。

---

## ✨ 技术栈

| 技术 | 说明 |
|------|------|
| Spring Boot 2.7.18 | 后端框架 |
| Spring Data JPA | 数据库 ORM |
| MySQL 5.7 | 数据库 |
| JWT | 用户认证 |
| JavaMail | 邮件通知 |
| Apache POI | Excel 导出 |

---

## 🚀 核心功能

| 模块 | 功能 |
|------|------|
| 🔐 用户认证 | 注册 / 登录 / JWT Token / 忘记密码（邮件验证码） |
| 📅 预约管理 | 创建预约 / 时间冲突检测 / 待审核状态 |
| ✅ 预约审核 | 管理员审核 / 二次冲突检测 / 拒绝填写原因 |
| 📧 邮件通知 | 审核结果自动发邮件 / 忘记密码验证码 |
| 📊 数据统计 | 会议室使用率统计 / 时段热度统计 |
| 📎 Excel 导出 | 一键导出所有预约记录 |
| 👑 管理员 | 强制取消预约 / 会议室增删改查 / 启用停用 |

---

## 📁 项目结构
meeting-room-backend/
├── src/main/java/com/meeting/
│   ├── controller/       # REST API 控制器
│   ├── service/          # 业务逻辑层
│   ├── repository/       # 数据访问层
│   ├── entity/           # 数据库实体类
│   ├── config/           # 配置类（JWT、CORS、WebSocket）
│   └── util/             # 工具类（JWT、邮件、验证码）
├── src/main/resources/
│   └── application.yml   # 配置文件
└── pom.xml               # Maven 依赖

---

## 🗄️ 数据库设计

| 表名 | 说明 | 主要字段 |
|------|------|----------|
| `user` | 用户表 | id, username, password, email, role |
| `meeting_room` | 会议室表 | id, name, capacity, location, status |
| `booking` | 预约表 | id, user_id, room_id, start_time, end_time, status, reject_reason |

**预约状态流转：**
PENDING（待审核）→ APPROVED（已通过）/ REJECTED（已拒绝）→ CANCELLED（已取消）

---

## ⚙️ 快速启动

### 1. 环境要求
- JDK 17+
- MySQL 5.7+
- Maven 3.6+

### 2. 创建数据库
CREATE DATABASE meeting_room CHARACTER SET utf8mb4

### 3. 修改配置文件
spring:
  datasource:
    password: 你的MySQL密码
 
  mail:
    username: 你的QQ邮箱@qq.com
    password: 你的QQ邮箱授权码

### 4. 运行项目
mvn spring-boot:run

### 5. 访问 API
默认端口：8080，API 地址：http://localhost:8080/api/...

## 📮 核心 API 接口
| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/register` | 用户注册 |
| POST | `/api/user/login` | 用户登录（返回 JWT） |
| POST | `/api/bookings/create` | 创建预约 |
| POST | `/api/bookings/cancel` | 取消自己的预约 |
| POST | `/api/bookings/admin/review` | 管理员审核 |
| GET | `/api/bookings/admin/all` | 获取所有预约 |
| GET | `/api/bookings/admin/export` | 导出 Excel |
| GET | `/api/rooms` | 获取会议室列表 |
| POST | `/api/rooms/admin/add` | 添加会议室 |
| GET | `/api/bookings/admin/stats/room-usage` | 使用率统计 |

## 🔗 相关仓库
前端项目：meeting-room-frontend

## 👨‍💻 作者
GitHub：@ynjyyds

## 📄 许可证
MIT License


⭐如果这个项目对你有帮助，欢迎点个 Star！
