# PowerShell 7 安装并设为 Trae 默认终端计划

## Summary

- 目标：在 Windows 上安装 PowerShell 7（PowerShell 7），并将其配置为 `Trae` 与 `Trae CN` 的默认集成终端（Integrated Terminal）。
- 实施策略：优先使用 Windows 程序包管理器（Windows Package Manager / WinGet）安装 `MSI` 包，而不是默认的 `MSIX` 包，确保获得稳定的经典安装路径 `C:\Program Files\PowerShell\7\pwsh.exe`，便于两个 Electron / VS Code 系应用显式绑定终端 Profile。
- 配置范围：仅修改两个用户配置文件：
  - `c:\Users\mark\AppData\Roaming\Trae\User\settings.json`
  - `c:\Users\mark\AppData\Roaming\Trae CN\User\settings.json`

## Current State Analysis

- 已确认 `Trae` 用户配置文件存在：`c:\Users\mark\AppData\Roaming\Trae\User\settings.json`。
- 已确认 `Trae CN` 用户配置文件存在：`c:\Users\mark\AppData\Roaming\Trae CN\User\settings.json`。
- 当前 `Trae` 配置中已存在 `"terminal.integrated.defaultProfile.windows": "Command Prompt"`，说明其默认终端仍为命令提示符（Command Prompt）。
- 当前 `Trae CN` 的 `settings.json` 仅包含主题配置，尚未发现终端默认 Profile 配置。
- 已在以下常见位置检索 `pwsh.exe`，当前未发现 PowerShell 7 可执行文件，说明本机大概率尚未安装 PowerShell 7：
  - `c:\Program Files`
  - `c:\Program Files (x86)`
  - `c:\Users\mark\AppData\Local\Microsoft\WindowsApps`
- 已参考微软官方文档《在 Windows 上安装 PowerShell 7》：
  - 官方推荐 Windows 客户端优先使用 WinGet。
  - 但从 PowerShell 7.6.0 开始，`winget install --id Microsoft.PowerShell --source winget` 默认安装 `MSIX` 包。
  - `MSIX` 安装存在沙盒限制（Sandbox Limitations），而本任务需要为开发工具配置稳定终端路径，因此采用 `winget --installer-type wix` 安装 `MSI` 更稳妥。

## Proposed Changes

### 1. 安装 PowerShell 7（PowerShell 7）

- 执行安装前检查：
  - 校验 `winget` 是否可用。
  - 若 `winget` 可用，则优先执行：

```powershell
winget search --id Microsoft.PowerShell --exact
winget install --id Microsoft.PowerShell --source winget --installer-type wix
```

- 选择 `MSI`（Windows Installer）而不是默认 `MSIX`（App Package）的原因：
  - 经典安装路径稳定，便于工具显式配置。
  - 避免应用商店包（Store/MSIX）路径与沙盒行为带来的不确定性。
  - 官方文档明确说明 `MSIX` 存在系统级配置限制，不适合作为开发环境中需要稳定落地路径的首选方案。
- 若 `winget` 不可用，则采用官方文档提供的稳定版 `MSI` 包下载链接进行手动安装：
  - `https://github.com/PowerShell/PowerShell/releases/download/v7.6.1/PowerShell-7.6.1-win-x64.msi`
- 安装完成后，预期可执行文件路径为：
  - `C:\Program Files\PowerShell\7\pwsh.exe`

### 2. 为 Trae 配置 PowerShell 7 默认终端

- 修改文件：`c:\Users\mark\AppData\Roaming\Trae\User\settings.json`
- 变更内容：
  - 保留现有无关配置。
  - 新增或更新 `terminal.integrated.profiles.windows`，显式声明 `PowerShell 7` Profile。
  - 将 `terminal.integrated.defaultProfile.windows` 从 `Command Prompt` 改为 `PowerShell 7`。
- 推荐写入结构：

```json
{
  "terminal.integrated.profiles.windows": {
    "PowerShell 7": {
      "path": "C:\\Program Files\\PowerShell\\7\\pwsh.exe"
    }
  },
  "terminal.integrated.defaultProfile.windows": "PowerShell 7"
}
```

- 这样处理的原因：
  - 显式绑定 `pwsh.exe` 路径，避免依赖 PATH 环境变量（Environment Variable）或自动探测结果。
  - 即使系统中同时存在 Windows PowerShell 5.1（`powershell.exe`）和 PowerShell 7（`pwsh.exe`），也能稳定命中目标版本。

### 3. 为 Trae CN 配置 PowerShell 7 默认终端

- 修改文件：`c:\Users\mark\AppData\Roaming\Trae CN\User\settings.json`
- 变更内容：
  - 保留当前 `"workbench.colorTheme": "Light"`。
  - 新增 `terminal.integrated.profiles.windows`。
  - 新增 `terminal.integrated.defaultProfile.windows` 并设置为 `PowerShell 7`。
- 推荐结果结构：

```json
{
  "workbench.colorTheme": "Light",
  "terminal.integrated.profiles.windows": {
    "PowerShell 7": {
      "path": "C:\\Program Files\\PowerShell\\7\\pwsh.exe"
    }
  },
  "terminal.integrated.defaultProfile.windows": "PowerShell 7"
}
```

### 4. 重启与生效验证

- 为避免 Electron 应用缓存旧终端配置，关闭并重新打开：
  - `Trae`
  - `Trae CN`
- 在两个应用中分别新建集成终端，确认默认启动命令不再是：
  - `C:\WINDOWS\System32\WindowsPowerShell\v1.0\powershell.exe`
  - 或 `cmd.exe`
- 预期结果应为启动：
  - `C:\Program Files\PowerShell\7\pwsh.exe`

## Assumptions & Decisions

- 决策：采用 `WinGet + MSI` 作为首选安装方案。
  - 原因：这是兼顾官方推荐、可维护性与稳定路径控制的折中方案。
- 决策：不采用默认 `WinGet MSIX` 安装。
  - 原因：默认 `MSIX` 更偏单用户分发与商店式安装，不利于终端工具做稳定显式路径配置。
- 假设：当前机器为 `x64` Windows。
  - 依据：用户环境与常规桌面开发机形态；若后续发现为 `ARM64`，则改为官方 `win-arm64.msi` 包。
- 假设：两个应用的终端配置键与 VS Code 兼容。
  - 依据：两者目录结构与设置文件布局均符合 VS Code / Electron 系应用常见模式，且 `Trae` 现有配置已使用 `terminal.integrated.defaultProfile.windows`。
- 决策：只改用户级配置（User Settings），不改工作区级配置（Workspace Settings）。
  - 原因：用户目标是将其作为应用默认终端，而不是仅某个项目默认终端。

## Verification Steps

### 安装验证

- 检查文件是否存在：
  - `C:\Program Files\PowerShell\7\pwsh.exe`
- 检查版本输出：

```powershell
& "C:\Program Files\PowerShell\7\pwsh.exe" -v
```

### 配置验证

- 打开并检查以下文件内容是否包含目标键：
  - `c:\Users\mark\AppData\Roaming\Trae\User\settings.json`
  - `c:\Users\mark\AppData\Roaming\Trae CN\User\settings.json`
- 检查两个文件中均满足：
  - `terminal.integrated.defaultProfile.windows` 为 `PowerShell 7`
  - `terminal.integrated.profiles.windows.PowerShell 7.path` 为 `C:\\Program Files\\PowerShell\\7\\pwsh.exe`

### 运行验证

- 在 `Trae` 中新建终端，确认默认 Shell 为 `pwsh.exe`。
- 在 `Trae CN` 中新建终端，确认默认 Shell 为 `pwsh.exe`。
- 在两个终端中执行：

```powershell
$PSVersionTable.PSVersion
```

- 验证主版本号为 `7`。

## 风险与边界

- 若本机缺少 `winget`，则需要退回到手动下载安装 `MSI`，这不影响整体方案。
- 若安装后 `pwsh.exe` 实际落点不是 `C:\Program Files\PowerShell\7\pwsh.exe`，则需要以实际路径修正两个设置文件。
- 若 `Trae` 或 `Trae CN` 内部对终端 Profile 名称做了额外封装，仍可退化为仅设置可执行路径对应的 Profile，并通过 UI 侧再次确认默认终端项。
