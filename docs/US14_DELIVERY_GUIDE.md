# US-14 交付说明

## 1. 本次完成的工作

本次围绕 `US-14 Check TA Workload` 完成了以下内容：

- 补齐了模块负责人（MO）端的申请评审状态流。
  - 在申请评审页面中，模块负责人现在可以将申请修改为 `SHORTLISTED`、`ACCEPTED`、`REJECTED`。
  - 评审备注继续复用已有的 `reviewerNote` 字段。
  - 只允许模块负责人处理自己岗位下的申请，`WITHDRAWN` 申请不可再次评审。

- 新增了管理员端 TA 工作量统计服务。
  - 仅统计 `ACCEPTED` 状态的 TA 岗位分配。
  - 自动计算每位 TA 的总周工时。
  - 自动检测排期冲突。
  - 为后续 `US-15` 预留了可复用的数据接口。

- 改造了管理员端首页。
  - 新增 “TA workload control” 区域。
  - 支持输入周工时上限，默认值为 `10`。
  - 支持展示每位已录用 TA 的岗位、排期、总周工时、过载预警和冲突详情。
  - 风险展示规则：
    - 冲突：红色
    - 仅过载：橙色
    - 正常：绿色

- 新增了 US-14 自动化测试。
  - 覆盖 `ACCEPTED` 统计逻辑。
  - 覆盖总工时累计。
  - 覆盖过载判断。
  - 覆盖排期冲突检测。
  - 覆盖上游评审状态联动。

## 2. 本次涉及的核心代码

- 上游评审流
  - `src/main/java/com/bupt/tarecruitment/application/ApplicationDecisionService.java`
  - `src/main/java/com/bupt/tarecruitment/ui/ApplicationReviewPage.java`

- 工作量统计与冲突检测
  - `src/main/java/com/bupt/tarecruitment/admin/AdminWorkloadService.java`
  - `src/main/java/com/bupt/tarecruitment/admin/WorkloadSummary.java`
  - `src/main/java/com/bupt/tarecruitment/admin/AcceptedAssignment.java`
  - `src/main/java/com/bupt/tarecruitment/admin/WorkloadConflict.java`
  - `src/main/java/com/bupt/tarecruitment/common/schedule/ScheduleSlot.java`

- 管理员端界面
  - `src/main/java/com/bupt/tarecruitment/ui/AdminDashboardPage.java`
  - `src/main/java/com/bupt/tarecruitment/ui/UiServices.java`

- 自动化测试
  - `src/test/java/com/bupt/tarecruitment/US14SmokeTest.java`
  - `scripts/test-us14.ps1`

### 2.1 给其他 US 预留的接口

当前 `US-14` 预留的是可直接在 Java 代码中复用的服务层接口和数据结构，不是独立的 HTTP / REST API。后续其他 US 如果要接 JavaFX 页面、JSP 页面或 Controller，建议直接复用下面这些能力，而不是重复实现工时统计和排期冲突判断逻辑。

- 服务主入口：`AdminWorkloadService`
  - `listAcceptedTaWorkloads(int weeklyHourLimit)`
  - 用途：按 TA 汇总所有 `ACCEPTED` 岗位，返回总工时、已录用岗位列表、冲突列表、是否过载、是否存在冲突。
  - 适用场景：管理员总览、批量巡检、后续导出或统一预警。
  - `getAcceptedTaWorkload(String applicantUserId, int weeklyHourLimit)`
  - 用途：查询单个 TA 当前已录用岗位的工作量摘要。
  - 适用场景：后续 `US-15 Prevent Schedule Conflict` 在单次检查某个申请人时做预校验。
  - 返回约定：当该 TA 还没有任何 `ACCEPTED` 岗位时，返回 `Optional.empty()`。

- 稳定返回结构
  - `WorkloadSummary`
  - 字段：`applicantUserId`、`applicantDisplayName`、`totalWeeklyHours`、`acceptedAssignments`、`conflicts`、`overloaded`、`hasConflict`。
  - `AcceptedAssignment`
  - 字段：`jobId`、`title`、`moduleOrActivity`、`weeklyHours`、`scheduleSlots`。
  - `WorkloadConflict`
  - 字段：`jobIdA`、`jobTitleA`、`jobIdB`、`jobTitleB`、`overlapSlot`。

- 排期冲突公共能力：`ScheduleSlot`
  - `parse(String rawValue)`：将 `MON-09:00-11:00` 解析成结构化时间段。
  - `overlaps(ScheduleSlot other)`：判断两个时间段是否真实重叠。
  - `overlapWith(ScheduleSlot other)`：返回重叠区间。
  - `format()`：输出统一时间段格式，便于界面展示和测试断言。
  - 当前规则说明：边界相接不算冲突，例如 `MON-09:00-10:00` 和 `MON-10:00-11:00` 不会被判定为 overlap。

- UI 层接入点
  - `UiServices.adminWorkloadService()`
  - 作用：后续其他 JavaFX 页面如果要展示单个 TA 的 workload / conflict 信息，可以直接从 `UiServices` 取到同一套服务实例。

- 与上游评审流的联动接口
  - `ApplicationDecisionService.updateStatus(String organiserUserId, String applicationId, ApplicationStatus nextStatus, String reviewerNote)`
  - 作用：当 MO 将申请改为 `ACCEPTED`、`REJECTED`、`SHORTLISTED` 后，`AdminWorkloadService` 下一次查询会直接基于最新仓库数据重算，不需要额外同步步骤。

- 当前边界
  - 当前预留的是 service 层复用点，还没有单独封装 REST Controller 或 JSP Endpoint。
  - 当前统计口径固定为“只统计 `ACCEPTED` 状态岗位”。
  - 如果后续其他 US 需要做“录用前冲突预判”或“申请提交前提示”，建议复用 `getAcceptedTaWorkload(...)` 和 `ScheduleSlot`，在外层补业务规则，不要直接改写 `US-14` 现有统计口径。

## 3. 当前项目如何启动

### 3.1 环境要求

- Windows
- PowerShell
- JDK 21+
- JavaFX SDK
  - 推荐路径：`C:\Java\javafx-sdk-21.0.2`
  - 或通过环境变量 `JAVA_FX_HOME` 指向 JavaFX SDK
- 如果需要运行 JSP Web 演示版，还需要：
  - Docker Desktop

### 3.2 推荐启动方式：JavaFX 主界面

当前和 `US-14` 最相关的主界面是 JavaFX UI，推荐使用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1
powershell -ExecutionPolicy Bypass -File scripts\run-javafx.ps1
```

如果需要直接从某个页面启动，可以使用：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-javafx.ps1 com.bupt.tarecruitment.ui.LoginPage
powershell -ExecutionPolicy Bypass -File scripts\run-javafx.ps1 com.bupt.tarecruitment.ui.ApplicationReviewPage
powershell -ExecutionPolicy Bypass -File scripts\run-javafx.ps1 com.bupt.tarecruitment.ui.AdminDashboardPage
```

说明：

- `scripts/run.ps1` 现在默认启动 JavaFX 入口。
- 如果要演示本次 `US-14` 的主要功能，建议直接使用 `run-javafx.ps1`。

### 3.3 JSP Web 演示版启动方式

如果只需要启动仓库里的 JSP Web 壳，可以运行：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-run.ps1
```

如果 `8080` 被占用，可以改用其他端口：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-run.ps1 -Port 8090
```

清理 Docker 相关资源：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\web-clean.ps1 -RemoveRuntimeData
```

说明：

- JSP Web 演示版主要验证 Web 壳可以打包和运行。
- 本次 `US-14` 的核心管理员功能仍然在 JavaFX 界面中。

## 4. 演示账号

- Applicant
  - `ta001 / 123456`
  - `ta002 / 123456`

- Module organiser
  - `mo001 / 123456`

- Admin
  - `admin001 / 123456`

## 5. 本次功能如何手动验证

### 5.1 上游评审流验证

1. 启动 JavaFX：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-javafx.ps1
```

2. 使用 `mo001 / 123456` 登录。
3. 进入 `Application Review` 页面。
4. 对某条申请执行：
   - `Shortlist`
   - `Accept`
   - `Reject`
5. 确认页面刷新后状态与备注正确显示。

### 5.2 管理员工作量验证

1. 使用 `admin001 / 123456` 登录。
2. 进入管理员首页。
3. 查看 “TA workload control” 区域。
4. 确认以下内容是否展示：
   - 已录用 TA 列表
   - accepted 岗位
   - 总周工时
   - 冲突明细
   - 风险颜色高亮
5. 修改工时阈值后点击 `Refresh`，确认过载结果会变化。

## 6. 自动化测试如何启动

### 6.1 US-14 专项测试

这是本次功能最直接的自动化验证入口：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test-us14.ps1
```

通过标志：

```text
US14 smoke test passed.
```

### 6.2 项目现有其他测试

可以按需运行仓库里原有的 smoke test：

```powershell
powershell -ExecutionPolicy Bypass -File scripts\test.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-us02-us04.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-us03.ps1
powershell -ExecutionPolicy Bypass -File scripts\test-us05.ps1
```

说明：

- 这些脚本会编译整个主源码，因此依赖 JavaFX SDK 已配置。
- 如果 JavaFX SDK 未配置，构建会在脚本前置阶段失败。

## 7. 本次本地提交拆分

本次功能按 4 条本地提交整理，便于组内 PR 展示：

```text
feat: 补齐模块负责人端申请评审状态流
feat: 新增TA工作量统计与排期冲突检测服务
feat: 实现管理员端TA工作量管控界面
test: 补充US14工作量管控与状态联动测试
```

## 8. 当前状态说明

- 本次与 `US-14` 相关的代码已经完成并通过专项测试。
- Java/JavaFX 编译、US-14 逻辑 smoke test、US-14 页面 scene 级自检都已完成。
- Docker Web 测试已验证通过，并已清理容器、镜像与运行时数据。
