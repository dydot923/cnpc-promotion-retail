# Windows 离线运行包

打包脚本会把 React 前端、Spring Boot 后端和 Java 21 运行时合并为 Windows 桌面程序。目标电脑不需要安装 Java、Node.js、Maven、Docker 或 PostgreSQL。

## 构建

构建电脑需要 Java 21（含 `jpackage`）、Maven 和 Node.js：

```powershell
.\packaging\build-windows.ps1
```

- 默认生成带桌面快捷方式的 `.exe` 安装包；若构建电脑没有 WiX Toolset 3，脚本会从 WiX 官方 GitHub 发布页下载固定版本并校验 SHA-256。
- WiX 下载不可用时，自动退回生成免安装目录和可传输的 ZIP；解压后双击 `CNPC Smart Retail.exe` 即可运行。
- 强制生成安装包：`.\packaging\build-windows.ps1 -PackageType Exe`
- 仅生成免安装版：`.\packaging\build-windows.ps1 -PackageType AppImage`
- 已经完成过前后端构建时，可使用 `-SkipBuild` 直接重新生成安装包。

安装包位于 `release/installer/`，免安装目录位于 `release/package/`，免安装传输包位于 `release/`。

## 使用

双击程序后，系统会在本机 `127.0.0.1:18083` 启动服务并自动打开浏览器。再次双击会直接打开已经运行的页面。退出时使用 Windows 系统托盘中的 `CNPC Smart Retail` 图标。

当前离线包使用项目自带的内存数据仓库，不需要数据库服务；运行期间新增的数据会在退出程序后清空，重新启动时恢复演示基线。日志位于 `%LOCALAPPDATA%\CNPCSmartRetail\logs\application.log`。
