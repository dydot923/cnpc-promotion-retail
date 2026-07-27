# 国企专网部署与安全送检

## 发布要求

正式下发包必须由单位信息部门或软件供应链平台构建，不能直接下发开发机生成的未签名文件。构建机需要：

- Java 21、Maven、Node.js 和 WiX Toolset 3；
- Windows SDK Signing Tools（`signtool.exe`）；
- 由单位内部 CA 或受终端信任 CA 签发、带私钥和 Code Signing EKU 的证书；
- 如专网要求长期验签，使用单位内部时间戳服务。

证书应安装在构建账号的 `CurrentUser\My` 或受控构建机的 `LocalMachine\My` 证书库中。不要把 PFX 文件或密码提交到代码库。

## 正式构建

```powershell
.\packaging\build-windows.ps1 `
  -PackageType Msi `
  -SigningCertificateThumbprint "证书指纹" `
  -SigningCertificateStore LocalMachine `
  -TimestampUrl "内网时间戳服务地址" `
  -RequireSignature `
  -DefenderScan
```

`-RequireSignature` 会在证书缺失、启动程序验签失败、PostgreSQL 原生文件签名失败或 MSI 验签失败时终止发布。构建结果包括 MSI、`.sha256` 文件和 `-security.txt` 安全清单。

## 终端安全策略

信息部门应以“发布者证书”为首选白名单条件，而不是长期维护单个版本哈希。送检资料应包含：

- MSI 的 SHA-256 和 Authenticode 发布者；
- 安全清单及本机 Defender 扫描结果；
- 安装目录：`%ProgramFiles%\CNPC Smart Retail`；
- 数据目录：`%LOCALAPPDATA%\CNPCSmartRetail`；
- 本地监听：`127.0.0.1:18083`，不监听外部网卡；
- 子进程：`postgres.exe`、`initdb.exe`、`pg_ctl.exe`，均由同一企业证书签名；
- 应用不要求访问互联网，也不要求安装 Java 或 PostgreSQL。

如果终端仍告警，需要记录安全软件名称、规则/病毒名称、告警文件路径和事件编号，由信息部门按签名发布者或事件规则放行。不要通过关闭杀毒软件、添加全盘排除、修改文件后缀或加密压缩包绕过检测。

## 验收检查

```powershell
Get-AuthenticodeSignature ".\CNPC Smart Retail-1.1.1.msi" |
  Format-List Status,StatusMessage,SignerCertificate

Get-FileHash ".\CNPC Smart Retail-1.1.1.msi" -Algorithm SHA256
```

`Status` 必须为 `Valid`，哈希必须与随包 `.sha256` 文件一致。只有同时满足这两项，才进入专网软件分发平台。
