# BUPT TA Recruitment System 启动说明

## 1. 项目运行形态

当前仓库实际上支持两种运行方式：

- **控制台 / Swing 演示版**
  - 通过 PowerShell 脚本启动的纯 Java 应用。
- **JSP Web 演示版**
  - 基于 Tomcat 9 的轻量部署方式，主要用于运行 `src/main/webapp` 下的登录注册页面。

需要注意的是，这个项目目前 **不是** 一个标准的 Maven 或 Gradle Web 工程，Web 版是通过仓库里的自定义脚本拼装出来的。

## 2. 环境要求

### 控制台 / Swing 版

- Windows
- PowerShell
- JDK 21 或更高版本

### JSP Web 版

- Windows
- PowerShell
- Docker Desktop
- 终端里可以直接使用 Docker
- 本地用于打包的 JDK 21 或更高版本
- 容器内使用 Tomcat 9

### 推荐版本

- JDK：21
- Docker：当前稳定版 Docker Desktop
- Tomcat：9

## 3. 控制台 / Swing 版启动方式

先编译主程序：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1
```

启动默认主程序：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1
```

启动具体演示功能：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us00
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us01
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us01-ui
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us02-ui
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 job-post-ui
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 cv-review-ui
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 us04-ui job001 ta001
```

运行现有 smoke test：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-us02-us04.ps1
```

## 4. JSP Web 版启动方式

如果只想先生成 Web 打包产物，可以执行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-build.ps1
```

如果要直接启动 Tomcat 9 的 Docker 容器：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-run.ps1
```

默认访问地址：

```text
http://localhost:8080/
```

如果想指定其他端口，例如 `8090`：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-run.ps1 -Port 8090
```

停止并清理 Web 容器和镜像：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-clean.ps1
```

如果连运行时数据也一起清空：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-clean.ps1 -RemoveRuntimeData
```

## 5. 演示账号

仓库初始账号来自 `data/users.txt`：

- Applicant：`ta001 / demo-ta-password`
- Module organiser：`mo001 / demo-mo-password`
- Admin：`admin001 / demo-admin-password`

JSP Web 版还支持直接通过 `register.jsp` 注册新账号。

## 6. Web 版运行说明

- Web 打包产物会被组装到 `out/web/ROOT`
- Web 运行时数据目录为 `out/web-runtime-data`
- 第一次执行 `web-run.ps1` 时，会把仓库里的 `data/` 复制到 `out/web-runtime-data`
- 只要 `out/web-runtime-data` 不删，Web 页面中新注册的账号在容器重启后依然保留
- 如果执行 `web-clean.ps1 -RemoveRuntimeData`，则会把这部分运行时数据一起重置

## 7. 已知说明与限制

- 当前仓库没有 Maven、Gradle 或 WAR 打包配置
- JSP 页面基于 **Tomcat 9**，因为页面中使用的是 `javax.servlet`
- 当前源码已经使用了 Java 21 相关能力，所以脚本统一要求 **JDK 21+**
- 现有脚本会先把源码放到临时英文目录再编译，用来规避当前仓库中文路径带来的编码和命令长度问题
