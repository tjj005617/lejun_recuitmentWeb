# SSH 免密登录虚拟机配置指南

## 前置条件

- Windows 10/11 本地机器
- VMware/VirtualBox 虚拟机（Ubuntu）
- 虚拟机 IP 地址（如 `192.168.150.105`）

---

## 步骤一：生成 SSH 密钥

在 Windows 终端（CMD/PowerShell/Git Bash）执行：

```bash
ssh-keygen -t ed25519
```

提示 `Enter file...` 时**直接按回车**，使用默认路径：

```
C:\Users\你的用户名\.ssh\id_ed25519
```

提示输入密码时**直接按两次回车**（设置空密码）。

生成成功后会显示：

```
Your identification has been saved in C:\Users\你的用户名/.ssh/id_ed25519
Your public key has been saved in C:\Users\你的用户名/.ssh/id_ed25519.pub
```

---

## 步骤二：查看公钥内容

```bash
type C:\Users\你的用户名\.ssh\id_ed25519.pub
```

输出类似：

```
ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIxxxxxxxxxxxxxxxxxxxxxxx user@COMPUTER-NAME
```

**复制这整行内容**。

---

## 步骤三：首次 SSH 登录虚拟机

```bash
ssh root@192.168.150.105
```

首次会提示：

```
The authenticity of host '192.168.150.105' can't be established.
Are you sure you want to continue connecting (yes/no)?
```

输入 `yes`，然后输入密码登录。

---

## 步骤四：将公钥添加到虚拟机

登录虚拟机后执行：

```bash
mkdir -p ~/.ssh && echo "粘贴你的公钥内容" >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys
```

**注意**：把 `粘贴你的公钥内容` 替换为步骤二复制的完整公钥。

示例：

```bash
mkdir -p ~/.ssh && echo "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAIxxxxxxxxxxxxxxxxxxxxxxx user@COMPUTER-NAME" >> ~/.ssh/authorized_keys && chmod 700 ~/.ssh && chmod 600 ~/.ssh/authorized_keys
```

---

## 步骤五：退出并测试

```bash
exit
```

回到 Windows 终端，测试免密登录：

```bash
ssh root@192.168.150.105
```

**不需要输入密码**即表示配置成功。

---

## 常见问题

### Q1: 提示 `Permission denied (publickey)`

检查虚拟机 `/root/.ssh/authorized_keys` 文件权限：

```bash
chmod 700 /root/.ssh
chmod 600 /root/.ssh/authorized_keys
```

### Q2: Windows 没有 `ssh-copy-id` 命令

Windows 没有 `ssh-copy-id`，使用步骤四的手动方式即可。

### Q3: 密钥保存到了错误位置

如果之前输入错误，先删除错误文件：

```bash
del "C:\Users\你的用户名\错误文件名"
```

然后重新执行 `ssh-keygen -t ed25519`。

---

## 验证命令

```bash
# 测试连接（不需要密码）
ssh root@192.168.150.105 "hostname && docker ps"
```

返回虚拟机主机名和容器列表即表示配置成功。
