# Fabric 1.20.4 模组开发 — 环境搭建、项目结构与核心内容开发

> 本文档针对 **Minecraft 1.20.4** + **Fabric Loader 0.15.x** + **Fabric API 0.92.0+** 编写。
> 目标读者：有一定 Java 基础、初次接触 Fabric 模组开发的开发者。

---

## 目录

1. [开发环境搭建](#1-开发环境搭建)
2. [项目结构与配置](#2-项目结构与配置)
3. [核心内容开发（物品/方块）](#3-核心内容开发物品方块)

---

## 1. 开发环境搭建

### 1.1 JDK 版本选择

| 项目 | 说明 |
|------|------|
| **最低要求** | JDK 17（Minecraft 1.20.4 的硬性要求） |
| **推荐发行版** | Eclipse Temurin（Adoptium） |
| **下载地址** | https://adoptium.net/temurin/releases/?version=17 |
| **注意事项** | Minecraft 1.20.5+ 需要 JDK 21；1.20.4 只能用 JDK 17 |

安装后验证：

```bash
java -version
# 输出应包含 openjdk version "17.0.x"
```

设置环境变量 `JAVA_HOME` 指向 JDK 17 的安装路径。

### 1.2 IntelliJ IDEA + MCDev 插件

**IDE 选择**：推荐 IntelliJ IDEA Community Edition（免费版），功能完全满足模组开发需求。

**MCDev（Minecraft Development）插件安装**：
1. 打开 IntelliJ IDEA → `File` → `Settings` → `Plugins` → `Marketplace` 选项卡
2. 搜索 **"Minecraft Development"**（插件 ID：`com.demonwav.minecraft-dev`）
3. 点击 **Install** 安装

该插件提供以下功能：
- Fabric 项目的自动生成（通过 New Project 向导）
- Mixin 支持（语法高亮、访问器生成、影子字段）
- 构建系统集成
- 运行配置自动生成

### 1.3 项目创建方式

#### 方式一：Fabric 模板生成器（推荐）

访问 [Fabric 官方模板生成器](https://fabricmc.net/develop/template/)：
1. 填写 **Mod Name**（如 "My First Mod"）
2. 填写 **Mod ID**（如 "my-first-mod"，自动生成）
3. 填写 **Package Name**（如 `com.example.myfirstmod`）
4. 选择 **Minecraft Version**：`1.20.4`
5. 选择 **Mapping**：Yarn 或 Mojang（推荐 Yarn 或 Mojang + Parchment）
6. 高级选项（可选）：Kotlin 支持、数据生成 API、分离客户端/服务端源码
7. 点击 **Generate** 下载 ZIP 文件
8. 解压后用 IntelliJ IDEA 打开（选择 `build.gradle` 文件）

#### 方式二：克隆 fabric-example-mod

```
git clone https://github.com/FabricMC/fabric-example-mod.git
```

克隆后修改 `gradle.properties` 中的版本号适配 1.20.4。

#### 方式三：IDEA 内置生成（安装 MCDev 插件后）

`File` → `New` → `Project` → 左侧选择 `Minecraft` → 按向导填写信息。

### 1.4 Fabric Loom Gradle 配置

**Fabric Loom** 是 Fabric 官方维护的 Gradle 插件，负责反编译 Minecraft、处理映射、打包模组等。

#### 关键依赖版本（1.20.4）

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Fabric Loader | 0.14.21 | 0.15.11+ |
| Fabric Loom | 1.5+ | 1.6-SNAPSHOT |
| Fabric API | 0.88.0+ | 0.92.0+1.20.4 |
| Minecraft | 1.20.4 | 1.20.4 |

#### gradle.properties 示例

```properties
# Gradle 设置
org.gradle.jvmargs=-Xmx2G -XX:+CMSClassUnloadingEnabled
org.gradle.parallel=true

# Minecraft 版本
minecraft_version=1.20.4

# 映射版本（Yarn）
yarn_mappings=1.20.4+build.3

# Fabric Loader 版本
loader_version=0.15.11

# Fabric Loom 版本
loom_version=1.6-SNAPSHOT

# Fabric API 版本
fabric_version=0.92.0+1.20.4

# 模组信息
mod_version=1.0.0
maven_group=com.example
archives_base_name=fabric-example-mod
```

#### build.gradle 典型写法

```groovy
plugins {
    id 'fabric-loom' version "${loom_version}"
    id 'maven-publish'
}

repositories {
    // 第三方仓库（如需要）
    maven { url = 'https://maven.terraformersmc.com/releases/' } // ModMenu
}

dependencies {
    // Minecraft
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    
    // 映射方案
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    
    // Fabric Loader
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    
    // Fabric API（可选但强烈推荐）
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

processResources {
    // 在 fabric.mod.json 中替换 ${...} 变量
    inputs.property "version", project.version
    filteringCharset "UTF-8"
    filesMatching("fabric.mod.json") {
        expand "version": project.version
    }
}
```

### 1.5 国内网络优化

#### BMCLAPI 镜像（Fabric Loom 专用）

在 `gradle.properties` 中添加：

```properties
# BMCLAPI 镜像加速（适用于国内用户）
loom_resources_base=https://bmclapi2.bangbang93.com/assets/
loom_version_manifests=https://bmclapi2.bangbang93.com/mc/game/version_manifest.json
loom_experimental_versions=https://maven.fabricmc.net/net/minecraft/experimental_versions.json
```

#### HanBing 代理仓库

在 `settings.gradle` 中添加：

```groovy
pluginManagement {
    repositories {
        maven { url = 'https://repository.hanbings.io/proxy' }
        mavenCentral()
        gradlePluginPortal()
    }
}
```

在 `build.gradle` 中添加：

```groovy
repositories {
    maven { url = 'https://repository.hanbings.io/proxy' }
    mavenCentral()
}
```

#### 阿里云 Maven 镜像（通用加速）

在 `build.gradle` 的 `repositories` 中添加：

```groovy
repositories {
    maven { url 'https://maven.aliyun.com/nexus/content/groups/public' }
    mavenCentral()
}
```

### 1.6 Gradle 关键任务

| 任务 | 用途 | 说明 |
|------|------|------|
| `genSources` | 生成 Minecraft 可读源码 | 首次必须运行，反编译 Minecraft JAR。命令：`./gradlew genSources` |
| `runClient` | 启动开发环境客户端 | 自动加载当前模组到测试客户端。第一次启动会创建 `run/` 目录 |
| `runServer` | 启动开发环境服务端 | 首次需要同意 EULA（编辑 `run/eula.txt`，将 `eula=false` 改为 `eula=true`） |
| `build` | 构建模组产物 | 输出到 `build/libs/<archives_base_name>-<version>.jar` |
| `clean` | 清理构建产物 | 删除 `build/` 目录 |
| `migrateMappings` | 迁移映射版本 | 在不同映射方案之间迁移代码 |

**典型开发流程**：

```bash
# 1. 首次：生成 Minecraft 源码
./gradlew genSources

# 2. 编写代码后测试
./gradlew runClient

# 3. 测试服务端
./gradlew runServer

# 4. 发布
./gradlew build
```

**注意事项**：
- 不要在 IntelliJ 中运行 `idea` Gradle 任务，它可能破坏开发环境
- IDEA 首次导入项目后，建议在 Gradle 设置中将 `Build and run using` 和 `Run tests using` 都设为 `IntelliJ IDEA` 以加快构建速度
- `runClient` 和 `runServer` 的运行目录是项目根目录下的 `run/`，存放着游戏数据、配置和日志

---

## 2. 项目结构与配置

### 2.1 项目目录结构

```
my-mod/
├── build.gradle               # Gradle 构建脚本
├── settings.gradle            # Gradle 设置
├── gradle.properties          # Gradle 属性（版本号等）
├── gradlew / gradlew.bat      # Gradle Wrapper
├── gradle/                    # Gradle Wrapper 文件
│   └── wrapper/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/example/
│   │   │       ├── ExampleMod.java             # ModInitializer（主入口）
│   │   │       ├── item/ModItems.java           # 物品注册
│   │   │       ├── block/ModBlocks.java         # 方块注册
│   │   │       └── block/entity/ModBlockEntities.java
│   │   └── resources/
│   │       ├── fabric.mod.json                  # 模组元数据
│   │       ├── assets/
│   │       │   └── <modid>/
│   │       │       ├── blockstates/             # 方块状态 JSON
│   │       │       ├── models/                  # 模型 JSON
│   │       │       ├── textures/                # 纹理图片
│   │       │       └── lang/                    # 语言文件
│   │       └── data/
│   │           └── <modid>/                     # 数据包内容
│   └── client/
│       └── java/
│           └── com/example/
│               └── ExampleModClient.java        # ClientModInitializer
└── run/                                         # 游戏运行目录（自动生成）
```

### 2.2 fabric.mod.json 逐字段解析

`fabric.mod.json` 位于 `src/main/resources/` 目录下，是 Fabric Loader 识别和加载模组的核心元数据文件。

```json
{
    "schemaVersion": 1,
    "id": "my-mod",
    "version": "${version}",
    "name": "My Mod",
    "description": "A description of my mod",
    "authors": ["Author Name"],
    "contact": {
        "homepage": "https://example.com/",
        "sources": "https://github.com/example/my-mod",
        "issues": "https://github.com/example/my-mod/issues"
    },
    "license": "MIT",
    "icon": "assets/my-mod/icon.png",
    "environment": "*",
    "entrypoints": {
        "main": ["com.example.MyMod"],
        "client": ["com.example.MyModClient"],
        "server": ["com.example.MyModServer"],
        "fabric-datagen": ["com.example.MyModDataGenerator"]
    },
    "mixins": ["my-mod.mixins.json"],
    "depends": {
        "fabricloader": ">=0.15.3",
        "minecraft": "~1.20.4",
        "java": ">=17",
        "fabric-api": "*"
    },
    "suggests": {
        "some-mod": "*"
    },
    "accessWidener": "my-mod.accesswidener",
    "custom": {
        "modmenu:parent": "fabric-api"
    }
}
```

#### 字段详解

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `schemaVersion` | int | 是 | 当前为 `1` |
| `id` | string | 是 | 模组唯一标识符，只能包含小写字母、数字、短横线、下划线、点号 |
| `version` | string | 是 | 模组版本号，建议遵循 SemVer，可以引用 Gradle 变量 `${version}` |
| `name` | string | 否 | 模组显示名称，若不填则使用 id |
| `description` | string | 否 | 模组描述，建议 1-2 句话 |
| `authors` | string[] | 否 | 作者列表 |
| `contact` | object | 否 | 联系方式：homepage / sources / issues / email |
| `license` | string | 否 | 开源许可证 |
| `icon` | string | 否 | 图标路径，64x64 PNG，推荐 `assets/<modid>/icon.png` |
| `environment` | string | 否 | `"*"`（两端）、`"client"`（仅客户端）、`"server"`（仅服务端） |
| `entrypoints` | object | 否 | 入口点声明，详见下文 |
| `mixins` | string[] | 否 | Mixin 配置文件的路径列表 |
| `depends` | object | 否 | 依赖声明，key 为模组 ID，value 为版本范围 |
| `suggests` | object | 否 | 建议的模组（非强制依赖） |
| `accessWidener` | string | 否 | Access Widener 文件路径 |
| `custom` | object | 否 | 自定义数据，供其他模组使用（如 ModMenu 配置） |

#### 依赖版本语法

Fabric Loader 使用类 SemVer 的版本范围语法：

| 语法 | 含义 | 示例 |
|------|------|------|
| `*` | 匹配任意版本 | `"fabric-api": "*"` |
| `1.2.3` | 精确匹配 | `"minecraft": "1.20.4"` |
| `>=1.2.3` | 大于等于 | `"fabricloader": ">=0.15.3"` |
| `>1.2.3` | 大于 | `">1.2.0"` |
| `<=1.2.3` | 小于等于 | `"<=1.2.0"` |
| `<1.2.3` | 小于 | `"<1.3.0"` |
| `~1.2.3` | 相同 minor 版本范围（`>=1.2.3 <1.3.0-`） | `"minecraft": "~1.20.4"` |
| `^1.2.3` | 相同 major 版本范围（`>=1.2.3 <2.0.0-`） | `"^1.0.0"` |
| `1.2.x` | X-Range 通配符 | `"minecraft": "1.20.x"` |
| `>=1.0 <2.0` | 多条件组合（AND） | `">=1.0.0 <2.0.0"` |
| `["1.0", "2.0"]` | 数组表示 OR | 满足任一即可 |

在 `fabric.mod.json` 中还可以引用 Gradle 变量，例如 `"version": "${version}"`，需要在 `build.gradle` 的 `processResources` 中配置替换。

### 2.3 Entrypoints 体系

Entrypoints（入口点）是 Fabric Loader 在特定时机自动实例化并调用特定方法的类。

#### 内置入口点

| 入口点名称 | 接口 | 加载时机 | 运行环境 |
|-----------|------|---------|---------|
| `preLaunch` | `PreLaunchEntryPoint` | 游戏启动前，最早期 | 双端 |
| `main` | `ModInitializer` | 最先运行的常规入口点 | 双侧 |
| `client` | `ClientModInitializer` | `main` 之后，仅客户端 | 仅客户端 |
| `server` | `DedicatedServerModInitializer` | `main` 之后，仅服务端 | 仅服务端 |
| `fabric-datagen` | （自定义） | 数据生成模式（`--datagen`） | 独立流程 |

#### 加载顺序

```
所有模组的 preLaunch（若有）
    ↓
所有模组的 main（ModInitializer#onInitialize）
    ↓
客户端：client（ClientModInitializer#onInitializeClient）
服务端：server（DedicatedServerModInitializer#onInitServer）
    ↓
游戏启动完成（游戏循环开始）
```

#### 各入口点的职责

**ModInitializer（main）** — 必须实现，放置双端共用的逻辑：

```java
public class MyMod implements ModInitializer {
    public static final String MOD_ID = "my-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // 注册物品、方块、实体、配方、命令、音效等
        ModItems.register();
        ModBlocks.register();
    }
}
```

**ClientModInitializer（client）** — 仅客户端的初始化：

```java
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 注册实体渲染器、按键绑定、粒子工厂、HUD 等
        EntityRendererRegistry.register(ModEntities.CUSTOM, CustomRenderer::new);
    }
}
```

**DedicatedServerModInitializer（server）** — 仅服务端的逻辑。

#### 关键注意事项

> **重要**：`main` 和 `client` 的入口点实现**必须放在不同的类中**。
>
> 即使服务端永远不会调用 `client` 入口点，但如果同一个类同时实现了 `ModInitializer` 和 `ClientModInitializer`，该类在服务端启动时仍然会被类加载器加载，导致客户端专用类（如 `MinecraftClient`）被引用，触发 `NoClassDefFoundError` 崩溃。

```java
// ❌ 错误写法
public class MyMod implements ModInitializer, ClientModInitializer { ... }

// ✅ 正确写法：两个独立的类
public class MyMod implements ModInitializer { ... }
public class MyModClient implements ClientModInitializer { ... }
```

#### 在 fabric.mod.json 中声明

```json
"entrypoints": {
    "main": [
        "com.example.MyMod"
    ],
    "client": [
        "com.example.MyModClient"
    ],
    "server": [
        "com.example.MyModServer"
    ],
    "fabric-datagen": [
        "com.example.MyModDataGenerator"
    ]
}
```

每个入口点可以声明多个类（数组），按声明顺序加载。

### 2.4 映射方案选择

Minecraft 官方发布的是混淆后的代码，需要通过映射（Mappings）转换成可读的 Java 代码。

| 映射方案 | 来源 | 可读性 | 维护状态 | 使用场景 |
|---------|------|--------|---------|---------|
| **Yarn** | Fabric 社区 | 高（有 Javadoc 和参数名） | 最终版（已停止更新） | 1.20.4 及以下版本的 Fabric 模组 |
| **Mojang**（官方） | Mojang | 中（无 Javadoc、无参数名） | 活跃维护 | 当前标准方案 |
| **Parchment** | ParchmentMC 社区（叠加在 Mojang 上） | 高（追加 Javadoc 和参数名） | 活跃维护 | 最佳开发体验（推荐） |

#### Yarn 配置

```groovy
dependencies {
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
}
```

#### Mojang 官方映射

```groovy
dependencies {
    mappings loom.officialMojangMappings()
}
```

#### Mojang + Parchment（推荐方案）

```groovy
dependencies {
    mappings loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-${project.minecraft_version}:${project.parchment_version}@zip")
    }
}
```

```properties
# gradle.properties
parchment_version=2024.11.17
```

#### 方法名对照

| Yarn | Mojang | 说明 |
|------|--------|------|
| `getDefaultState()` | `defaultBlockState()` | 获取默认方块状态 |
| `getBlock()` | `getBlock()` | 获取方块 |
| `getItem()` | `getItem()` | 获取物品 |
| `writeNbt()` / `saveAdditional()` | `saveAdditional()` | NBT 写入 |
| `readNbt()` / `loadAdditional()` | `loadAdditional()` | NBT 读取 |
| `onInitialize()` | `onInitialize()` | ModInitializer 入口（不变） |

### 2.5 Gradle 配置详解

#### settings.gradle 典型写法

```groovy
pluginManagement {
    repositories {
        maven { url = 'https://maven.fabricmc.net/' }
        maven { url = 'https://repository.hanbings.io/proxy' }  // 国内镜像
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "fabric-example-mod"
```

#### gradle.properties 关键项

```properties
# Gradle 性能
org.gradle.jvmargs=-Xmx2G -XX:+CMSClassUnloadingEnabled
org.gradle.parallel=true

# Minecraft 及映射版本
minecraft_version=1.20.4
yarn_mappings=1.20.4+build.3

# Fabric 组件版本
loader_version=0.15.11
loom_version=1.6-SNAPSHOT
fabric_version=0.92.0+1.20.4

# 模组元数据（供 build.gradle 和 fabric.mod.json 引用）
mod_version=1.0.0
maven_group=com.example
archives_base_name=my-mod
```

---

## 3. 核心内容开发（物品/方块）

### 3.1 自定义物品（Item）

#### 创建基础物品

最简单的方式：直接使用 `Item` 类并传入 `Item.Settings`。

```java
// 注册一个简单物品
public static final Item RUBY = Registry.register(
    Registries.ITEM,
    new Identifier(MyMod.MOD_ID, "ruby"),
    new Item(new Item.Settings())
);
```

#### 推荐：统一注册方法

更好的做法是把物品注册封装成辅助方法：

```java
// ModItems.java
public class ModItems {
    public static <T extends Item> T register(String name, T item) {
        Registry.register(Registries.ITEM, new Identifier(MyMod.MOD_ID, name), item);
        return item;
    }

    public static final Item RUBY = register("ruby",
        new Item(new Item.Settings()));
    
    public static final Item DIAMOND_DUST = register("diamond_dust",
        new Item(new Item.Settings()));
    
    public static void initialize() {
        // 仅用于触发静态初始化
    }
}
```

在主类中调用：

```java
@Override
public void onInitialize() {
    ModItems.initialize();
}
```

#### 创建自定义 Item 子类

```java
public class CustomItem extends Item {
    public CustomItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        // 右键点击方块时的逻辑
        return ActionResult.SUCCESS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        // 右键使用物品的逻辑
        return TypedActionResult.success(player.getStackInHand(hand));
    }
}
```

注册自定义子类：

```java
public static final Item CUSTOM_ITEM = register("custom_item",
    new CustomItem(new Item.Settings().maxCount(16)));
```

#### Item.Settings 常用方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `maxCount(int)` | 最大堆叠数 | 设置最大堆叠数量（默认 64），与 `maxDamage` 互斥 |
| `maxDamage(int)` | 最大耐久 | 设置耐久度（自动设置堆叠数为 1） |
| `maxDamageIfAbsent(int)` | 最大耐久 | 仅在未设置耐久度时才设置 |
| `food(FoodProperties)` | 食物组件 | 使物品可食用 |
| `rarity(Rarity)` | 稀有度 | 影响名称颜色（COMMON/UNCOMMON/RARE/EPIC） |
| `fireproof()` | - | 物品实体免疫火焰/熔岩 |
| `recipeRemainder(Item)` | 合成剩余物 | 合成后返还的物品（如桶） |

**稀有度颜色对照**：

| 稀有度 | 颜色 |
|--------|------|
| `Rarity.COMMON` | 白色（默认） |
| `Rarity.UNCOMMON` | 黄色 |
| `Rarity.RARE` | 青色 |
| `Rarity.EPIC` | 浅紫色 |

**完整示例**：

```java
public static final Item SPECIAL_ITEM = register("special_item",
    new Item(new Item.Settings()
        .maxCount(1)
        .maxDamage(100)
        .rarity(Rarity.UNCOMMON)
        .fireproof()
        .recipeRemainder(Items.BUCKET)
    ));
```

#### FabricItemSettings 额外方法

如果使用 `FabricItemSettings`（Fabric API 提供）代替 `Item.Settings`，还有以下额外方法：

| 方法 | 说明 |
|------|------|
| `equipmentSlot(EquipmentSlotProvider)` | 设置装备槽位 |
| `customDamage(CustomDamageHandler)` | 自定义伤害处理逻辑 |

```java
new FabricItemSettings()
    .maxCount(16)
    .equipmentSlot(stack -> EquipmentSlot.HEAD);
```

### 3.2 自定义方块（Block）

#### 创建基础方块

```java
public static final Block RUBY_BLOCK = Registry.register(
    Registries.BLOCK,
    new Identifier(MyMod.MOD_ID, "ruby_block"),
    new Block(AbstractBlock.Settings.create()
        .strength(5.0f, 6.0f)          // 硬度 5.0，抗爆 6.0
        .requiresCorrectToolForDrops() // 需要正确工具才能掉落
        .sounds(BlockSoundGroup.METAL) // 金属音效
    )
);
```

**同时注册对应物品**（方块通常需要物品形式）：

```java
Registry.register(Registries.ITEM,
    new Identifier(MyMod.MOD_ID, "ruby_block"),
    new BlockItem(RUBY_BLOCK, new Item.Settings()));
```

#### Block.Settings 常用方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `strength(float)` | 硬度 | 同时设置破坏时间和抗爆性为相同值 |
| `strength(float, float)` | 硬度, 抗爆 | 分别设置破坏时间和抗爆性 |
| `hardness(float)` | 硬度 | 仅设置破坏时间 |
| `resistance(float)` | 抗爆 | 仅设置抗爆性 |
| `breakInstantly()` | - | 瞬间破坏 |
| `requiresCorrectToolForDrops()` | - | 需要正确工具才能掉落 |
| `sounds(BlockSoundGroup)` | 音效组 | 设置方块音效 |
| `lightLevel(ToIntFunction<BlockState>)` | 光照函数 | 设置光照等级（0-15） |
| `noOcclusion()` | - | 非透明方块，不裁切相邻面 |
| `noCollision()` | - | 无碰撞箱 |
| `dropsNothing()` | - | 破坏后不掉落物品 |
| `ticksRandomly()` | - | 接收随机刻更新 |
| `dynamicBounds()` | - | 动态碰撞箱 |
| `noBlockBreakParticles()` | - | 不产生破坏粒子 |
| `mapColor(MapColor)` | 地图颜色 | 设置地图上的颜色 |
| `dropsLike(Block)` | 参照方块 | 掉落物与参照方块相同 |

**常用音效组**：

```java
BlockSoundGroup.STONE    // 石头音效
BlockSoundGroup.WOOD     // 木头音效
BlockSoundGroup.METAL    // 金属音效
BlockSoundGroup.GRAVEL   // 沙砾音效
BlockSoundGroup.GLASS    // 玻璃音效
BlockSoundGroup.SLIME    // 史莱姆音效
BlockSoundGroup.ANVIL    // 铁砧音效
```

#### 完整方块创建示例

```java
// 1. 定义方块
public static final Block RUBY_BLOCK = new Block(
    AbstractBlock.Settings.create()
        .strength(5.0f, 6.0f)
        .requiresCorrectToolForDrops()
        .sounds(BlockSoundGroup.METAL)
        .lightLevel(state -> 7)       // 发光 7 级
);

// 2. 定义矿物方块（需要特定挖掘等级）
public static final Block RUBY_ORE = new Block(
    AbstractBlock.Settings.create()
        .strength(3.0f, 3.0f)
        .requiresCorrectToolForDrops()
        .sounds(BlockSoundGroup.STONE)
);

// 3. 封装注册方法
private static Block registerBlock(String name, Block block) {
    Registry.register(Registries.BLOCK, new Identifier(MyMod.MOD_ID, name), block);
    return block;
}

private static BlockItem registerBlockItem(String name, Block block) {
    Registry.register(Registries.ITEM, new Identifier(MyMod.MOD_ID, name),
        new BlockItem(block, new Item.Settings()));
    return null; // 返回 null 但注册已执行
}

// 4. 统一调用
public static void initialize() {
    registerBlockItem("ruby_block", registerBlock("ruby_block", RUBY_BLOCK));
    registerBlockItem("ruby_ore", registerBlock("ruby_ore", RUBY_ORE));
}
```

#### 创建自定义 Block 子类

```java
public class CustomBlock extends Block {
    public CustomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        // 右键点击方块时的逻辑
        if (!world.isClient) {
            player.sendMessage(Text.literal("Hello from custom block!"));
        }
        return ActionResult.SUCCESS;
    }
}
```

### 3.3 状态属性（State Properties）

#### 什么是状态属性

方块状态属性（BlockState Properties）是定义在 `BlockState` 上的键值对，用于区分方块的不同视觉或功能变体。

#### 常用属性类型

| 类型 | 创建方式 | 示例 |
|------|---------|------|
| `BooleanProperty` | `BooleanProperty.of("name")` | `true` / `false` |
| `DirectionProperty` | `DirectionProperty.of("facing")` | `NORTH`, `SOUTH` 等 |
| `IntegerProperty` | `IntegerProperty.of("name", min, max)` | `0, 1, 2, 3` |
| `EnumProperty<E>` | `EnumProperty.of("name", Enum.class)` | 自定义枚举值 |

#### 使用示例

```java
public class ChargeableBlock extends Block {
    // 1. 定义属性
    public static final BooleanProperty CHARGED = BooleanProperty.of("charged");
    public static final DirectionProperty FACING = DirectionProperty.of("facing", Direction.Type.HORIZONTAL);
    public static final IntegerProperty LEVEL = IntegerProperty.of("level", 0, 3);

    public ChargeableBlock(Settings settings) {
        super(settings);
        // 2. 设置默认状态
        setDefaultState(getDefaultState()
            .with(CHARGED, false)
            .with(FACING, Direction.NORTH)
            .with(LEVEL, 0)
        );
    }

    // 3. 为 StateManager 添加属性
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(CHARGED, FACING, LEVEL);
    }
}
```

#### 运行时操作状态

```java
// 读取状态
boolean isCharged = world.getBlockState(pos).get(CHARGED);
Direction facing = world.getBlockState(pos).get(FACING);

// 修改状态
world.setBlockState(pos, state.with(CHARGED, true));
world.setBlockState(pos, state.cycle(FACING)); // 循环方向
```

#### 对应方块状态 JSON

```json
// assets/<modid>/blockstates/chargeable_block.json
{
    "variants": {
        "charged=false,facing=north": { "model": "modid:block/chargeable_block_off" },
        "charged=false,facing=south": { "model": "modid:block/chargeable_block_off", "y": 180 },
        "charged=true,facing=north":  { "model": "modid:block/chargeable_block_on" },
        "charged=true,facing=south":  { "model": "modid:block/chargeable_block_on", "y": 180 }
    }
}
```

#### 性能注意事项

每个方块的**所有可能状态组合**在 `Block` 对象初始化时都会被枚举。每增加一个布尔属性，状态数会翻倍（2^n）。因此：

- 状态属性**应主要用于视觉/功能变体**
- 复杂状态（如大量 NBT 数据）应使用 **BlockEntity（方块实体）**
- 属性数量控制在合理范围内

### 3.4 实体方块（BlockEntity）

#### 什么时候需要使用 BlockEntity

- 需要存储自定义数据（NBT）—— 如箱子内容、熔炉进度
- 需要每 tick 更新逻辑 —— 如机器运行、生长
- 需要储存超过简单状态属性的复杂状态

#### 创建 BlockEntity 类

```java
public class CustomBlockEntity extends BlockEntity {
    private int counter = 0;

    public CustomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUSTOM_BLOCK_ENTITY, pos, state);
    }

    // NBT 持久化
    @Override
    public void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.putInt("counter", counter);
        super.writeNbt(nbt, registries);  // 务必调用 super
    }

    @Override
    public void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(nbt, registries);  // 务必先调用 super
        counter = nbt.getInt("counter");
    }

    // Tick 逻辑
    public static void tick(World world, BlockPos pos, BlockState state, CustomBlockEntity be) {
        if (!world.isClient) {
            be.counter++;
            if (be.counter % 100 == 0) {
                System.out.println("Tick: " + be.counter);
            }
        }
    }
}
```

#### 注册 BlockEntityType

```java
// ModBlockEntities.java
public class ModBlockEntities {
    public static final BlockEntityType<CustomBlockEntity> CUSTOM_BLOCK_ENTITY =
        Registry.register(
            Registries.BLOCK_ENTITY_TYPE,
            new Identifier(MyMod.MOD_ID, "custom_block_entity"),
            BlockEntityType.Builder.create(
                CustomBlockEntity::new,
                ModBlocks.CUSTOM_BLOCK  // 关联的方块
            ).build()
        );

    public static void initialize() {
        // 触发静态初始化
    }
}
```

#### 将 BlockEntity 关联到方块

方块类需要实现 `BlockEntityProvider` 接口，并提供 ticker：

```java
public class CustomBlock extends Block implements BlockEntityProvider {
    public CustomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CustomBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        // 仅在服务端运行 tick
        if (world.isClient) return null;
        
        return BlockWithEntity.checkType(
            type,
            ModBlockEntities.CUSTOM_BLOCK_ENTITY,
            CustomBlockEntity::tick
        );
    }
}
```

#### BlockWithEntity 基类

对于更复杂的带实体方块，可以直接继承 `BlockWithEntity`（它已经实现了 `BlockEntityProvider`）：

```java
public class MachineBlock extends BlockWithEntity {
    protected MachineBlock(Settings settings) {
        super(settings);
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            World world, BlockState state, BlockEntityType<T> type) {
        return BlockWithEntity.checkType(
            type,
            ModBlockEntities.MACHINE_BLOCK_ENTITY,
            MachineBlockEntity::tick
        );
    }
}
```

#### NBT 持久化要点

- **`writeNbt`** 和 **`readNbt`** 中必须调用 `super` 方法
- 在 Fabric 1.20.4 中，两个方法都带 `RegistryWrapper.WrapperLookup` 参数（Mojang 映射）
- 调用顺序：`writeNbt` 先存自定义数据再调 `super`；`readNbt` 先调 `super` 再读自定义数据
- 如果需要客户端同步，还需重写 `toUpdatePacket()` 和 `toInitialChunkDataNbt()`

#### 客户端同步

```java
@Override
public Packet<ClientPlayPacketListener> toUpdatePacket() {
    return BlockEntityUpdateS2CPacket.create(this);
}

@Override
public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registries) {
    return createNbt(registries);
}
```

触发更新：

```java
world.updateListeners(pos, state, state, Block.NOTIFY_LISTENERS);
```

### 3.5 自定义物品栏分组（Creative Tab）

在 Fabric 1.20.4 中，创建自定义创造模式物品栏需要使用 `FabricItemGroup.builder()` 和 `ResourceKey` 注册。

#### 方式一：创建独立的自定义物品栏

```java
// ModItemGroups.java
public class ModItemGroups {
    // 1. 定义 ResourceKey
    public static final ResourceKey<CreativeModeTab> CUSTOM_GROUP_KEY =
        ResourceKey.create(
            BuiltInRegistries.CREATIVE_MODE_TAB.key(),
            new Identifier(MyMod.MOD_ID, "custom_group")
        );

    // 2. 构建物品栏
    public static final CreativeModeTab CUSTOM_GROUP = FabricItemGroup.builder()
        .icon(() -> new ItemStack(ModItems.RUBY))                     // 图标
        .title(Component.translatable("itemGroup." + MyMod.MOD_ID))   // 必需
        .build();

    public static void initialize() {
        // 3. 注册物品栏
        Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            CUSTOM_GROUP_KEY,
            CUSTOM_GROUP
        );

        // 4. 向自定义物品栏添加物品
        ItemGroupEvents.modifyEntriesEvent(CUSTOM_GROUP_KEY)
            .register(entries -> {
                entries.add(ModItems.RUBY);
                entries.add(ModItems.DIAMOND_DUST);
                entries.add(ModBlocks.RUBY_BLOCK.asItem());
                entries.add(ModBlocks.RUBY_ORE.asItem());
            });
    }
}
```

在主类中调用：

```java
@Override
public void onInitialize() {
    ModItems.initialize();
    ModBlocks.initialize();
    ModBlockEntities.initialize();
    ModItemGroups.initialize();
}
```

#### 方式二：向原版物品栏添加物品

```java
// 添加到"建筑方块"分类
ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS)
    .register(entries -> {
        entries.add(ModBlocks.RUBY_BLOCK);
    });

// 添加到"原材料"分类的指定位置之后
ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
    .register(entries -> {
        entries.addAfter(Items.DIAMOND, ModItems.RUBY);
    });
```

**常用原版物品栏键**：

| 键 | 分类 |
|----|------|
| `CreativeModeTabs.BUILDING_BLOCKS` | 建筑方块 |
| `CreativeModeTabs.COLORED_BLOCKS` | 染色方块 |
| `CreativeModeTabs.NATURAL` | 自然方块 |
| `CreativeModeTabs.FUNCTIONAL` | 功能方块 |
| `CreativeModeTabs.REDSTONE` | 红石 |
| `CreativeModeTabs.TOOLS_AND_UTILITIES` | 工具与实用物品 |
| `CreativeModeTabs.COMBAT` | 战斗 |
| `CreativeModeTabs.FOOD_AND_DRINKS` | 食物与饮品 |
| `CreativeModeTabs.INGREDIENTS` | 原材料 |
| `CreativeModeTabs.SPAWN_EGGS` | 刷怪蛋 |
| `CreativeModeTabs.OP` | 管理员物品 |

#### 语言文件（翻译）

```json
// assets/<modid>/lang/en_us.json
{
    "itemGroup.my-mod": "My Mod",
    "item.my-mod.ruby": "Ruby",
    "item.my-mod.diamond_dust": "Diamond Dust",
    "block.my-mod.ruby_block": "Block of Ruby",
    "block.my-mod.ruby_ore": "Ruby Ore"
}
```

```json
// assets/<modid>/lang/zh_cn.json
{
    "itemGroup.my-mod": "我的模组",
    "item.my-mod.ruby": "红宝石",
    "item.my-mod.diamond_dust": "钻石粉",
    "block.my-mod.ruby_block": "红宝石块",
    "block.my-mod.ruby_ore": "红宝石矿石"
}
```

#### 物品排序控制

```java
ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS)
    .register(entries -> {
        // addBefore / addAfter 控制相对位置
        entries.addBefore(Items.DIAMOND, ModItems.RUBY);
        entries.addAfter(ModItems.DIAMOND_DUST, Items.EMERALD);
    });
```

### 3.6 工具与武器（ToolMaterial）

#### 创建自定义 ToolMaterial

```java
public enum CustomToolMaterial implements ToolMaterial {
    // 参数：挖掘等级、耐久、挖掘速度、攻击伤害、附魔能力、修复材料
    RUBY(3, 1561, 8.0F, 3.0F, 10, () -> Ingredient.ofItems(ModItems.RUBY));

    private final int miningLevel;
    private final int itemDurability;
    private final float miningSpeed;
    private final float attackDamage;
    private final int enchantability;
    private final Lazy<Ingredient> repairIngredient;

    CustomToolMaterial(int miningLevel, int itemDurability, float miningSpeed,
                       float attackDamage, int enchantability,
                       Supplier<Ingredient> repairIngredient) {
        this.miningLevel = miningLevel;
        this.itemDurability = itemDurability;
        this.miningSpeed = miningSpeed;
        this.attackDamage = attackDamage;
        this.enchantability = enchantability;
        this.repairIngredient = new Lazy<>(repairIngredient);
    }

    // --- 接口实现 ---
    @Override public int getDurability() { return this.itemDurability; }
    @Override public float getMiningSpeed() { return this.miningSpeed; }
    @Override public float getAttackDamage() { return this.attackDamage; }
    @Override public int getMiningLevel() { return this.miningLevel; }
    @Override public int getEnchantability() { return this.enchantability; }
    @Override public Ingredient getRepairIngredient() { return this.repairIngredient.get(); }
}
```

**参数参考**（与原版对比）：

| 参数 | 木 | 石 | 铁 | 钻石 | 下界合金 |
|------|-----|-----|-----|------|---------|
| 挖掘等级 | 0 | 1 | 2 | 3 | 4 |
| 耐久 | 59 | 131 | 250 | 1561 | 2031 |
| 挖掘速度 | 2.0 | 4.0 | 6.0 | 8.0 | 9.0 |
| 基础攻击伤害 | 0 | 1 | 2 | 3 | 4 |
| 附魔能力 | 15 | 5 | 14 | 10 | 15 |

#### 注册工具物品

```java
// 剑和锹可以直接实例化
public static final Item RUBY_SWORD = register("ruby_sword",
    new SwordItem(CustomToolMaterial.RUBY, 3, -2.4F, new Item.Settings()));

public static final Item RUBY_SHOVEL = register("ruby_shovel",
    new ShovelItem(CustomToolMaterial.RUBY, 1.5F, -3.0F, new Item.Settings()));

// 镐和斧的构造函数是 protected，需要子类
public static final Item RUBY_PICKAXE = register("ruby_pickaxe",
    new PickaxeItem(CustomToolMaterial.RUBY, 1, -2.8F, new Item.Settings()));

public static final Item RUBY_AXE = register("ruby_axe",
    new AxeItem(CustomToolMaterial.RUBY, 5, -3.0F, new Item.Settings()));

public static final Item RUBY_HOE = register("ruby_hoe",
    new HoeItem(CustomToolMaterial.RUBY, -2, -1.0F, new Item.Settings()));
```

**工具构造函数参数说明**：

| 工具 | 参数 (material, attackDamage, attackSpeed) | 说明 |
|------|---|------|
| `SwordItem` | (material, attackDamage, attackSpeed) | attackDamage 为额外伤害（基础 1） |
| `ShovelItem` | (material, attackDamage, attackSpeed) | attackDamage 为额外伤害（基础 1） |
| `PickaxeItem` | (material, attackDamage, attackSpeed) | 同上 |
| `AxeItem` | (material, attackDamage, attackSpeed) | attackDamage 为额外伤害 |
| `HoeItem` | (material, attackDamage, attackSpeed) | attackDamage 为额外伤害 |

### 3.7 护甲（ArmorMaterial）

#### 创建自定义 ArmorMaterial

```java
public class RubyArmorMaterial implements ArmorMaterial {
    // 单例
    public static final RubyArmorMaterial INSTANCE = new RubyArmorMaterial();

    // 耐久倍率（各部位基础值 × 此倍率）
    private static final int[] BASE_DURABILITY = new int[] { 13, 15, 16, 11 }; // 靴、腿、胸、头
    
    // 各部位护甲值
    private static final int[] PROTECTION_VALUES = new int[] { 3, 6, 8, 3 };

    @Override
    public int getDurability(ArmorItem.Type type) {
        return BASE_DURABILITY[type.ordinal()] * 37;  // 下界合金级耐久
    }

    @Override
    public int getProtection(ArmorItem.Type type) {
        return PROTECTION_VALUES[type.ordinal()];
    }

    @Override
    public int getEnchantability() {
        return 15;  // 与皮革相当
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(ModItems.RUBY);
    }

    @Override
    public String getName() {
        return "ruby";  // 用于纹理路径和翻译键
    }

    @Override
    public float getToughness() {
        return 2.0F;  // 韧性，减少高伤害
    }

    @Override
    public float getKnockbackResistance() {
        return 0.1F;  // 10% 击退抗性
    }
}
```

#### 注册护甲物品

```java
public static final Item RUBY_HELMET = register("ruby_helmet",
    new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.HELMET, new Item.Settings()));

public static final Item RUBY_CHESTPLATE = register("ruby_chestplate",
    new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.CHESTPLATE, new Item.Settings()));

public static final Item RUBY_LEGGINGS = register("ruby_leggings",
    new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.LEGGINGS, new Item.Settings()));

public static final Item RUBY_BOOTS = register("ruby_boots",
    new ArmorItem(RubyArmorMaterial.INSTANCE, ArmorItem.Type.BOOTS, new Item.Settings()));
```

#### 护甲纹理路径

盔甲纹理受原版硬编码限制，必须放在特定路径：

```
assets/minecraft/textures/models/armor/
    ruby_layer_1.png    // 头盔 + 胸甲 + 靴子（上半部分）
    ruby_layer_2.png    // 护腿（下半部分）
```

纹理名称为 `getName()` 方法返回的名称 + `_layer_1` / `_layer_2`，并放置在 `minecraft` 命名空间下（这是原版的限制）。

### 3.8 食物（FoodComponent / FoodProperties）

#### 基础食物配置

```java
// 可食用物品
public static final Item CHEESE = register("cheese",
    new Item(new Item.Settings()
        .food(new FoodProperties.Builder()
            .nutrition(4)                    // 回复 4 点饥饿值
            .saturationModifier(0.8f)        // 饱食度修正
            .build()
        )
    ));
```

#### FoodProperties.Builder 方法

| 方法 | 参数 | 说明 |
|------|------|------|
| `nutrition(int)` | 饥饿值 | 回复的食物条格数（半格为单位，一格 = 2） |
| `saturationModifier(float)` | 饱食度修正 | 最终饱食度 = nutrition × saturationModifier × 2 |
| `meat()` | - | 标记为肉类，狼可以食用 |
| `alwaysEdible()` | - | 饱食度满时也可食用 |
| `fast()` | - | 快速食用（如同紫颂果） |
| `effect(MobEffectInstance, float)` | 效果实例, 概率 | 食用后附加状态效果 |

#### 完整食物示例

```java
public static final FoodProperties GOLDEN_CHEESE = new FoodProperties.Builder()
    .nutrition(8)                                     // 回复 8 点
    .saturationModifier(1.2f)                         // 高饱食度
    .alwaysEdible()                                   // 满饱食也可食用
    .fast()                                           // 快速食用
    .effect(new MobEffectInstance(
        MobEffects.REGENERATION, 100, 1), 1.0f)      // 100% 获得生命恢复 II（5 秒）
    .effect(new MobEffectInstance(
        MobEffects.ABSORPTION, 2400, 0), 1.0f)        // 100% 获得伤害吸收（2 分钟）
    .meat()                                           // 标记为肉类
    .build();
```

#### 原版食物参考值

| 食物 | nutrition | saturationModifier | 总饱食度 |
|------|-----------|-------------------|---------|
| 苹果 | 4 | 0.3 | 2.4 |
| 面包 | 5 | 0.6 | 6.0 |
| 牛排 | 8 | 0.8 | 12.8 |
| 金胡萝卜 | 6 | 1.2 | 14.4 |
| 附魔金苹果 | 4 | 1.2 | 9.6 |

---

> 本文档基于 Fabric 官方文档（docs.fabricmc.net）、Fabric Wiki（wiki.fabricmc.net）及社区教程编写，涵盖 Fabric 1.20.4 模组开发从环境搭建到核心内容开发的主要知识点。
