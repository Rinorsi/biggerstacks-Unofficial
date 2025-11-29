# Bigger Stacks（非官方版）

NeoForge 1.21.1 版本的 Bigger Stacks 移植，负责把大部分物品的上限提升到 64 以上。全局堆叠档位定义在 `biggerstacks-rules.xml`，而特殊物品（不死图腾、药水、附魔书或任意标签）可在 `biggerstacks-template-overrides.json` 里单独覆盖。

## 使用方法

1. 将jar 同时放进客户端和服务器的 `mods/` 文件夹。
2. 游戏内运行 `/biggerstacks quicksetup` 即可调整三种堆叠档位，也可以直接编辑 `run/config/biggerstacks-rules.xml`。
3. 想让不可堆叠的物品堆叠时，在 `biggerstacks-template-overrides.json` 写入一条规则。