# KineticItem

[简体中文](#简体中文) | [English](#english)

## 简体中文

### 模组定位

**KineticItem** 是 Kinetic 系列的物品管控与掉落物保护模组，面向大型整合包提供物品封禁、物品统一、掉落物防护、虚空救援与相关可视化管理能力，适合集中处理重复资源、限制物品和贵重掉落物。

### 主要功能

- **物品封禁**：按具体物品 ID、`@模组ID`、`#物品标签` 或带 NBT 的精确规则禁用物品。
- **物品合并 / 统一**：把多个来源物品统一替换成指定目标物品，减少大型整合包中的重复资源。
- **NBT 规则支持**：可以只处理某个特殊 NBT 变种，而不是一刀切禁用整个物品类型。
- **可视化封禁编辑器**：提供物品搜索、筛选和规则维护界面。
- **可视化合并编辑器**：选择目标物品和来源规则，不需要直接手写映射表。
- **掉落物保护**：针对指定物品配置防火、防爆、发光、无重力等保护行为。
- **虚空救援**：受保护的重要掉落物掉入虚空后，可以尝试传送回最近可用地面。
- **全局伤害类型免疫**：可以让全部掉落物忽略指定伤害类型。
- **直接伤害实体免疫**：可以按直接攻击实体 ID 屏蔽掉落物伤害，例如只针对某类弹丸。
- **创造栏与运行时联动**：被封禁或被替换的物品会参与创造模式列表和运行时物品处理逻辑。
- **KubeJS 可选兼容**：安装 KubeJS 时可使用 KineticItem 提供的物品保护相关脚本桥接能力。
- **服务端权威规则**：影响游戏规则的封禁、合并和保护配置由服务端负责最终执行与同步。

### 配置文件

```text
config/kineticcore/banitem.json
config/kineticcore/banitem.old.json
config/kineticcore/protection.toml
```

- `banitem.json`：当前物品封禁与合并规则。
- `banitem.old.json`：用于配置回退与异常恢复的备份文件。
- `protection.toml`：掉落物保护、虚空救援和全局免疫规则。

`banitem.json` 主要维护：

- `bannedItems`：封禁规则列表。
- `mergedItems`：目标物品到来源规则的合并映射。

### 使用建议

优先通过 F6 中的 KineticItem 页面进入封禁和合并编辑器。大量使用 `@mod`、`#tag` 或 NBT 规则前，建议先在测试存档确认命中范围，避免一次性影响过多物品。

### 运行环境

- Minecraft 1.20.1
- Minecraft Forge 47.4.2
- Java 17
- KineticCore：必须
- KubeJS：可选

## English

### Overview

**KineticItem** is the item-management and dropped-item protection module of the Kinetic family. It centralizes item bans, item unification, protected drops and void recovery for large modpacks.

### Key Features

- Ban rules for exact item IDs, `@mod`, `#tag`, and NBT-aware variants.
- Item unification/merge rules that replace multiple source items with one target item.
- Visual ban and merge editors.
- Per-item fire, explosion, glow and no-gravity protection.
- Void salvage for protected dropped items.
- Global immunity by damage type.
- Global immunity by direct damaging entity type.
- Runtime and creative-tab integration for banned/unified items.
- Optional KubeJS compatibility.
- Server-authoritative gameplay rules.

### Configuration

```text
config/kineticcore/banitem.json
config/kineticcore/banitem.old.json
config/kineticcore/protection.toml
```

### Requirements

- Minecraft 1.20.1
- Minecraft Forge 47.4.2
- Java 17
- KineticCore: required
- KubeJS: optional

## 开源协议与版权 (License)

Copyright (C) 2024-2026 XYAT.

本项目基于 **GNU Lesser General Public License v3.0 (LGPLv3)** 协议开源。

This project is open-sourced under the **GNU Lesser General Public License v3.0 (LGPLv3)**.
