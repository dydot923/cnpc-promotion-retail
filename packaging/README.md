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
- 生成适合专网软件分发的 MSI：`.\packaging\build-windows.ps1 -PackageType Msi -DefenderScan`
- 仅生成免安装版：`.\packaging\build-windows.ps1 -PackageType AppImage`
- 已经完成过前后端构建时，可使用 `-SkipBuild` 直接重新生成安装包。

安装包位于 `release/installer/`，免安装目录位于 `release/package/`，免安装传输包和 SHA-256/安全清单位于 `release/`。

国企专网正式下发必须使用企业代码签名证书，并强制验签：

```powershell
.\packaging\build-windows.ps1 `
  -PackageType Msi `
  -SigningCertificateThumbprint "企业代码签名证书指纹" `
  -SigningCertificateStore LocalMachine `
  -TimestampUrl "内网时间戳服务地址" `
  -RequireSignature `
  -DefenderScan
```

该流程会同时签署桌面启动程序、PostgreSQL 原生程序和最终 MSI；任一验签失败都会终止构建。详细要求见 [ENTERPRISE-DEPLOYMENT.md](ENTERPRISE-DEPLOYMENT.md)。

## 使用

双击程序后，系统会在本机 `127.0.0.1:18083` 启动服务并自动打开浏览器。再次双击会直接打开已经运行的页面。退出时使用 Windows 系统托盘中的 `CNPC Smart Retail` 图标。

当前离线包自带 Java 21 和 PostgreSQL 16，不需要外部数据库服务。业务数据持久保存在 `%LOCALAPPDATA%\CNPCSmartRetail`，日志位于其中的 `logs\application.log`。
