# Fabric 1.20.4 模组开发 — 打包发布、常见陷阱与学习资源

> 本文档涵盖三个子主题：打包发布（第 10 章）、常见陷阱与最佳实践（第 11 章）、学习资源（第 12 章）。

---

## 10. 打包发布

### 10.1 Gradle Build 与产物

#### 10.1.1 Fabric Loom 构建流程

Fabric 模组的构建基于 `fabric-loom` Gradle 插件。它负责处理 Minecraft 的反混淆（deobfuscation）、映射（mappings）以及关键的 `remapJar` 任务。

```groovy
// build.gradle 基础配置
plugins {
    id 'fabric-loom' version '1.10-SNAPSHOT'
    id 'maven-publish'
}
```

#### 10.1.2 remapJar 与非 remap 版本的区别

理解这两个产物的区别至关重要：

| 产物 | 任务 | 说明 |
|------|------|------|
| `build/libs/<mod>-<version>.jar` | `remapJar` | **最终发布用 JAR**。已从 Yarn/ intermediary 映射重映射回 Minecraft 的混淆命名空间，可在游戏环境中运行 |
| `build/libs/<mod>-<version>-dev.jar` | `jar` | **开发用 JAR**。包含 Yarn 命名空间的代码，仅用于开发环境（如依赖其他模组开发），不能直接用于游戏 |
| `build/libs/<mod>-<version>-sources.jar` | `sourcesJar` | **源码 JAR**。包含可读的 Java 源码，用于 IDE 调试 |

**关键点**：发布到 CurseForge 或 Modrinth 时**必须使用 `remapJar` 产物**，而非 `jar` 产物。非 remap 的 JAR 在游戏环境中会因类名不匹配而崩溃。

#### 10.1.3 remapJar 的自定义配置

```groovy
remapJar {
    // 自定义 JAR 文件名
    archiveFileName = "${project.archives_base_name}-${project.mod_version}.jar"
    
    // 如果使用了 Shadow 插件打包依赖（fat JAR）
    inputFile.set(shadowJar.archiveFile)
    
    // 启用 jar-in-jar 嵌套依赖
    addNestedDependencies = true
}
```

#### 10.1.4 完整的 build.gradle 示例

```groovy
// build.gradle
plugins {
    id 'fabric-loom' version '1.10-SNAPSHOT'
    id 'me.modmuss50.mod-publish-plugin' version '0.8.0'
}

repositories {
    mavenCentral()
    maven { url "https://maven.fabricmc.net/" }
}

dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}

remapJar {
    archiveFileName = "${project.archives_base_name}-${project.mod_version}.jar"
}
```

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048M
minecraft_version=1.20.4
loader_version=0.16.10
yarn_mappings=1.20.4+build.3
fabric_version=0.96.11+1.20.4
mod_version=1.0.0
archives_base_name=my-mod
```

**构建命令**：
```bash
./gradlew build           # 执行完整构建，生成 remapJar
./gradlew clean build     # 清理后重新构建
./gradlew remapJar        # 只生成 remapJar
```

产物路径：`build/libs/my-mod-1.0.0.jar`

---

### 10.2 上传到 CurseForge

#### 10.2.1 创建 CurseForge 项目

1. 登录 [CurseForge](https://www.curseforge.com/) 或 [authors.curseforge.com](https://authors.curseforge.com/)
2. 点击 **Create a Project**
3. 选择 **Minecraft Mods** 分类
4. 填写项目信息：
   - **Project Name**：模组名称
   - **Project Description**：简短描述
   - **Mod Loader**：选择 Fabric
   - **Game Version**：选择 Minecraft 1.20.4
5. 同意分发协议后提交

#### 10.2.2 生成 CurseForge API Token

1. 访问 [authors.curseforge.com/account/api-tokens](https://authors.curseforge.com/account/api-tokens)
2. 点击 **Generate API Token**
3. 输入 Token 名称（如 `fabric-publish`）
4. 复制生成的 Token（**仅显示一次，务必保存**）

#### 10.2.3 上传文件

**手动上传**：
1. 进入项目页面，点击 **Files** 选项卡
2. 点击 **Upload File**
3. 选择 `build/libs/` 下的 remap JAR
4. 选择支持的 Minecraft 版本和 Mod Loader
5. 填写版本类型（Release / Beta / Alpha）
6. 填写更新日志（Changelog）
7. 标记所需依赖（如 Fabric API）

**自动上传（推荐）**：见 10.4 GitHub Actions CI/CD 章节。

---

### 10.3 上传到 Modrinth

#### 10.3.1 创建 Modrinth 项目

1. 登录 [Modrinth](https://modrinth.com/)
2. 访问 [modrinth.com/dashboard](https://modrinth.com/dashboard) → **Create Project**
3. 填写项目信息：
   - **Project Name** 和 **Slug**（URL 标识符）
   - **Description**：模组描述
   - **License**：选择你的许可证
   - **Categories**：选择 Fabric 等分类
   - **Game Versions**：选择 Minecraft 1.20.4
   - **Loaders**：选择 Fabric
4. 提交创建

#### 10.3.2 生成 Modrinth API Token

1. 访问 [modrinth.com/settings/account](https://modrinth.com/settings/account)
2. 在 **API Tokens** 区域点击 **New Token**
3. 选择权限范围（至少需要 `Create Mod Version` 和 `Upload Mod Version`）
4. 复制生成的 Token

#### 10.3.3 版本发布系统

Modrinth 的版本发布系统核心概念：

| 概念 | 说明 |
|------|------|
| **Version** | 一个具体的模组版本，对应一个 JAR 文件 |
| **Version Number** | 版本号（如 `1.0.0`） |
| **Version Type** | `release`（正式版）、`beta`（测试版）、`alpha`（预览版）|
| **Loaders** | 支持的模组加载器（Fabric、Forge、NeoForge 等）|
| **Game Versions** | 支持的 Minecraft 版本列表 |
| **Dependencies** | 依赖关系（required / optional / incompatible）|

---

### 10.4 GitHub Actions CI/CD

#### 10.4.1 workflow_dispatch 触发

使用 `workflow_dispatch` 允许手动触发构建和发布流程：

```yaml
# .github/workflows/publish.yml
name: Build and Publish

on:
  workflow_dispatch:
    inputs:
      release_type:
        description: 'Release type'
        required: true
        default: 'release'
        type: choice
        options:
          - release
          - beta
          - alpha
  push:
    tags:
      - 'v*'    # 推送 v1.0.0 等标签时自动触发
```

#### 10.4.2 完整发布工作流（使用 mod-publish-plugin）

```yaml
name: Build and Publish Mod

on:
  workflow_dispatch:
  push:
    tags: [ 'v*' ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      
      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
      
      - name: Make Gradle Wrapper Executable
        run: chmod +x ./gradlew
      
      - name: Build
        run: ./gradlew build
      
      - name: Publish to CurseForge & Modrinth
        run: ./gradlew publishMods
        env:
          CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
```

#### 10.4.3 使用 MC-Publish Action（备选方案）

如果你不想修改 `build.gradle`，可以使用独立的 GitHub Action：

```yaml
name: Publish with MC-Publish

on:
  workflow_dispatch:

jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: 17
          distribution: temurin
      
      - run: chmod +x ./gradlew
      - run: ./gradlew build
      
      - name: Publish to CurseForge and Modrinth
        uses: Kir-Antipov/mc-publish@v3.3
        with:
          curseforge-token: ${{ secrets.CURSEFORGE_TOKEN }}
          curseforge-id: 123456
          modrinth-token: ${{ secrets.MODRINTH_TOKEN }}
          modrinth-id: abcdef
          files: build/libs/!(*-dev|*-sources).jar
          name: "Release ${{ env.VERSION }}"
          version: "${{ env.VERSION }}"
          loaders: fabric
          game-versions: "1.20.4"
          java: 17
```

#### 10.4.4 Secrets 管理 API Token

在 GitHub 仓库中配置 Secrets（**切勿将 Token 明文写入代码**）：

1. 打开仓库页面 → **Settings** → **Secrets and variables** → **Actions**
2. 点击 **New repository secret**
3. 分别添加以下 Secrets：
   - `CURSEFORGE_TOKEN`：CurseForge API Token
   - `MODRINTH_TOKEN`：Modrinth API Token
4. 在工作流中通过 `${{ secrets.CURSEFORGE_TOKEN }}` 引用

> **安全提醒**：Token 泄露可能导致他人冒用你的身份上传文件。务必：
> - 不要在日志中打印 Token
> - 不要将 Token 提交到 Git 仓库
> - 定期轮换 Token
> - 使用最小权限原则（仅授予发布所需权限）

---

### 10.5 多版本维护策略

#### 10.5.1 Git 分支管理

常用分支策略：

```
main                    # 最新版本（如 1.21）
├── 1.20.x             # 1.20 系列的维护分支
├── 1.19.x             # 1.19 系列的维护分支
├── 1.18.x             # 1.18 系列的维护分支
└── dev/feature-xxx    # 功能开发分支
```

**工作流程**：
1. 在 `main` 分支上开发最新版本的模组
2. 当需要向下移植（backport）时，创建对应版本的维护分支
3. 使用 `git cherry-pick` 将特定提交移植到旧版本分支
4. 每个分支独立发布对应 Minecraft 版本的模组

#### 10.5.2 Stonecutter 预处理器条件编译

**Stonecutter** 是一个 Gradle 插件，支持在单一代码库中维护多版本 Fabric 模组：

```groovy
// settings.gradle
plugins {
    id 'io.shcm.shsupercm.fabric.stonecutter' version '1.5'
}

stonecutter {
    // 定义支持的 Minecraft 版本
    versions '1.20.4', '1.20.2', '1.19.4'
    
    create(rootProject) {
        // 所有版本的共享代码
    }
}
```

**条件编译语法**（在 Java 源码中使用注释标记）：

```java
//? if >=1.20.2 {
// 这个代码块仅在 1.20.2+ 版本中编译
ResourceKey<DimensionOptions> overworld = ResourceKey.create(
    Registry.DIMENSION_KEY,
    DimensionOptions.OVERWORLD_ID
);
//?} elif >=1.19 {
// 这个代码块仅在 1.19 ~ 1.20.1 版本中编译
RegistryKey<DimensionOptions> overworld = RegistryKey.of(
    Registry.DIMENSION_KEY,
    DimensionOptions.OVERWORLD_ID
);
//?}
```

**优势**：
- 单一代码库，无需反复合并分支
- 版本差异通过注释条件控制，清晰可见
- IDE 插件支持语法高亮

**劣势**：
- 学习曲线较陡
- 代码可读性略有降低
- 需要额外的 Gradle 配置

---

### 10.6 模组元数据：fabric.mod.json

#### 10.6.1 完整字段示例

```json
{
  "schemaVersion": 1,
  "id": "my-mod",
  "version": "1.0.0",
  "name": "My Awesome Mod",
  "description": "This mod adds awesome new features to Minecraft!",
  "authors": [
    "YourName",
    {
      "name": "ContributorName",
      "contact": {
        "homepage": "https://example.com",
        "sources": "https://github.com/YourName/MyMod"
      }
    }
  ],
  "contributors": [
    "SomeHelper"
  ],
  "contact": {
    "homepage": "https://modrinth.com/mod/my-mod",
    "sources": "https://github.com/YourName/MyMod",
    "issues": "https://github.com/YourName/MyMod/issues"
  },
  "license": "MIT",
  "icon": "assets/my-mod/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": [
      "com.example.MyMod"
    ],
    "client": [
      "com.example.MyModClient"
    ]
  },
  "mixins": [
    "my-mod.mixins.json",
    {
      "config": "my-mod.client.mixins.json",
      "environment": "client"
    }
  ],
  "depends": {
    "fabricloader": ">=0.16.10",
    "minecraft": "~1.20.4",
    "java": ">=17",
    "fabric-api": "*"
  },
  "recommends": {
    "modmenu": ">=7.0.0"
  },
  "breaks": {
    "incompatible-mod": "*"
  }
}
```

#### 10.6.2 字段详解

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `schemaVersion` | int | 是 | 必须为 `1`（Loader 0.4.0+） |
| `id` | string | 是 | 模组唯一标识符。正则：`^[a-z][a-z0-9-_]{1,63}$`，2-64 字符，**全小写** |
| `version` | string | 是 | 模组版本号，建议遵循语义化版本（SemVer 2.0.0） |
| `name` | string | 否 | 显示名称，缺省时等于 `id` |
| `description` | string | 否 | 模组描述，缺省为空字符串 |
| `authors` | Person[] | 否 | 作者列表，每个元素可以是字符串或含 `name`/`contact` 的对象 |
| `contributors` | Person[] | 否 | 贡献者列表，格式同 `authors` |
| `contact` | object | 否 | 联系方式，支持 `email`、`homepage`、`issues`、`sources`、`irc` 等 |
| `license` | string/string[] | 否 | SPDX 许可证标识符，如 `"MIT"`、`"CC0-1.0"`、`["MIT", "Apache-2.0"]` |
| `icon` | string/object | 否 | 图标路径（128x128 的 PNG），如 `"assets/my-mod/icon.png"` |
| `environment` | string | 否 | `"*"`（均可）、`"client"`（仅客户端）、`"server"`（仅服务端） |
| `entrypoints` | object | 否 | 入口点，至少 `"main"` |
| `mixins` | array | 否 | Mixin 配置文件列表 |
| `depends` | object | 否 | 依赖声明，使用版本范围 |
| `recommends` | object | 否 | 推荐安装的模组 |
| `breaks` | object | 否 | 不兼容的模组声明 |

#### 10.6.3 版本约束语法

| 语法 | 含义 | 示例 |
|------|------|------|
| `"1.0.0"` | 精确版本 | `"minecraft": "1.20.4"` |
| `">=1.0.0"` | 大于等于 | `"fabricloader": ">=0.16.10"` |
| `"~1.0.0"` | 兼容版本（允许补丁版本升级） | `"minecraft": "~1.20.4"`（匹配 1.20.x） |
| `"^1.0.0"` | 兼容版本（允许次要版本升级） | `"minecraft": "^1.20.0"`（匹配 >=1.20.0 且 <1.21.0） |
| `"*"` | 任意版本 | `"fabric-api": "*"` |

---

### 10.7 许可证选择建议

#### 10.7.1 常见许可证对比

| 许可证 | 类型 | 说明 | 适用场景 |
|--------|------|------|----------|
| **MIT** | 宽松 | 允许任何人使用、修改、分发，只需保留版权声明和许可声明 | **推荐用于大多数 Fabric 模组** |
| **CC0-1.0** | 公共领域 | 放弃所有版权，作品进入公共领域 | 示例代码/模板模组 |
| **LGPL-3.0** | 弱保护 | 允许在商业项目中使用（如整合包），修改后的库文件必须开源 | 库类模组 |
| **GPL-3.0** | 强保护 | 衍生作品也必须使用 GPL 开源 | 你希望强制他人开源的模组 |
| **ARR** | 专有 | All Rights Reserved，保留所有权利 | 你不希望他人以任何形式使用你的模组 |
| **CC BY 4.0** | 宽松（资产） | 仅需署名即可使用，常用于材质/纹理/模型等资产文件 | 模组的艺术资产 |

#### 10.7.2 推荐策略

1. **代码部分使用 MIT**：这是 Fabric 社区最常用的许可证，兼容性好，整合包作者无需额外顾虑。
2. **资产部分使用 CC BY 4.0**：允许他人复用你的材质和模型，只需署名。
3. **示例模版使用 CC0-1.0**：不强制下游用户继承你的许可证选择。

**在 fabric.mod.json 中的写法**：
```json
"license": "MIT"
```

如果代码和资产使用不同许可证，可在项目根目录放置多个 LICENSE 文件：
```
LICENSE           # MIT（代码）
LICENSE_ASSETS    # CC BY 4.0（资产）
```

> **建议**：如果你不确定选择什么许可证，选择 **MIT**。它是模组社区最广泛接受的开源许可证，既不限制他人使用，又保留署名权。

---

## 11. 常见陷阱与最佳实践

### 11.1 Client/Server 分离错误

#### 11.1.1 问题描述

这是 Fabric 模组开发中**最常见、最容易导致服务端崩溃**的问题。Minecraft 客户端和服务端使用不同的 JAR 包——服务端不包含 `net.minecraft.client` 包下的任何类。

**典型错误**：
```
java.lang.RuntimeException: Cannot load class net.minecraft.client.MinecraftClient 
in environment type SERVER
```

#### 11.1.2 错误写法：在 Mixin 方法上使用 @Environment

```java
// ❌ 严重错误！@Environment 不能阻止 Mixin 在服务端被加载！
@Mixin(TitleScreenMixin.class)
public class MyMixin {
    
    @Environment(EnvType.CLIENT)  // 这个注解在这里无效！
    @Inject(method = "init", at = @At("HEAD"))
    private void onInit(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        // ... 客户端逻辑
    }
}
```

**为什么这是错误的**：
- `@Environment` 注解只控制字段/方法在编译时的可访问性，**不能阻止 Mixin 注入**
- Fabric Loader 0.15.0+ 改进了环境剥离逻辑，现在会导致服务端 `NoSuchFieldError` 崩溃
- 单机测试可能正常（集成服务器共享 JVM），但部署到专用服务器后必定崩溃

#### 11.1.3 正确做法：分离 Mixin 配置文件

**步骤 1：创建两个 Mixin JSON 文件**

`src/main/resources/my-mod.mixins.json`（通用，双端加载）：
```json
{
  "required": true,
  "package": "com.example.mixin.common",
  "compatibilityLevel": "JAVA_17",
  "mixins": [
    "SomeCommonMixin"
  ]
}
```

`src/client/resources/my-mod.client.mixins.json`（仅客户端加载）：
```json
{
  "required": true,
  "package": "com.example.mixin.client",
  "compatibilityLevel": "JAVA_17",
  "client": [
    "TitleScreenMixin"
  ]
}
```

**步骤 2：在 fabric.mod.json 中注册**

```json
"mixins": [
  "my-mod.mixins.json",
  {
    "config": "my-mod.client.mixins.json",
    "environment": "client"
  }
]
```

**步骤 3：将客户端 Mixin 类放入 client 源码集**

目录结构：
```
src/
├── main/
│   ├── java/com/example/mixin/common/SomeCommonMixin.java
│   └── resources/my-mod.mixins.json
└── client/
    ├── java/com/example/mixin/client/TitleScreenMixin.java
    └── resources/my-mod.client.mixins.json
```

> **核心原则**：永远不要在 Mixin 注入方法上使用 `@Environment(EnvType.CLIENT)`。始终使用分离的 Mixin 配置文件和 `environment` 字段。

#### 11.1.4 其他 Client/Server 分离注意事项

| 场景 | 正确做法 |
|------|----------|
| 客户端渲染代码 | 放在 `client` 源码集，不要在 `main` 中引用 `MinecraftClient` |
| 按键绑定 | 在 `client` entrypoint 中注册（`onInitializeClient()`） |
| 客户端与服务端通信 | 使用 Fabric API 网络包（`CustomPayload`） |
| 数据同步 | 使用 `DataTracker` 同步实体数据 |

---

### 11.2 Mixin 冲突预防

#### 11.2.1 Mixin 类型风险等级

| Mixin 类型 | 风险等级 | 说明 |
|-----------|---------|------|
| `@Inject` | 低 | 回调注入，多个模组可叠加。**优先使用** |
| `@ModifyArg` | 中 | 修改方法参数值，需注意多个模组修改同一参数 |
| `@ModifyVariable` | 中 | 修改局部变量值，注意变量作用域 |
| `@WrapOperation` | 中 | 包装操作调用，可替代某些 `@Redirect` 场景 |
| `@Redirect` | **高** | 重定向方法调用。**一个目标只能被一个模组重定向** |
| `@ModifyConstant` | **高** | 修改常量值。**一个常量只能被一个模组修改** |
| `@Overwrite` | **极高** | 完全替换目标方法。**强烈不推荐**，会导致与其他所有模组不兼容 |

#### 11.2.2 避免使用 @Overwrite

```java
// ❌ 强烈反对：@Overwrite 会完全覆盖原方法，与其他模组完全不兼容
@Overwrite
public void tick() {
    // 自己实现的全部逻辑
    // 其他模组的 Mixin 注入全部失效！
}
```

```java
// ✅ 推荐：使用 @Inject 在方法末尾添加逻辑
@Inject(method = "tick", at = @At("TAIL"))
private void onTick(CallbackInfo ci) {
    // 在原有逻辑之后附加执行
    // 其他模组也可以同时注入
}
```

#### 11.2.3 使用完整方法描述符

当存在方法重载时，必须使用完整描述符来唯一标识目标方法：

```java
// ❌ 可能导致注入到错误的重载版本
@Inject(method = "tick", at = @At("HEAD"))

// ✅ 使用完整描述符以避免歧义
@Inject(method = "tick()V", at = @At("HEAD"))
```

常见方法的描述符：
- `tick()V` — 无参数、无返回值的方法
- `damage(Lnet/minecraft/entity/damage/DamageSource;F)Z` — 接受 DamageSource 和 float，返回 boolean
- `interact(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;)Lnet/minecraft/util/ActionResult;` — 接受 PlayerEntity 和 Hand，返回 ActionResult

#### 11.2.4 使用 @Unique 和命名前缀

```java
// ❌ 常见的命名冲突：多个模组可能使用相同的字段名
private int counter;
private void helperMethod() { }

// ✅ 使用 @Unique + 模组 ID 前缀来避免冲突
@Unique
private int mymod$counter;

@Unique
private void mymod$helperMethod() { }
```

`$` 符号在 Java 字节码中是合法的，但常规 Java 开发中很少使用在标识符中，因此冲突概率极低。

#### 11.2.5 使用 Priority 控制注入顺序

```java
// 默认 priority 为 1000，值越大执行越早
@Mixin(value = TargetClass.class, priority = 500)
public class MyMixin {
    // priority 较低，较晚执行
}
```

---

### 11.3 资源路径规范

#### 11.3.1 Mod ID 命名规范

```json
{
  "id": "my-mod"   // ✅ 正确：全小写，连字符分隔
}
```

```json
{
  "id": "My_Mod"   // ❌ 错误：禁止大写字母
}
```

规则：
- 只能包含小写字母 `a-z`、数字 `0-9`、连字符 `-`、下划线 `_`
- 必须以小写字母开头
- 长度 2-64 字符
- 常用风格：蛇形（`my_mod`）或连字符（`my-mod`）

#### 11.3.2 assets 目录结构

```
src/main/resources/
├── assets/
│   └── <modid>/              # 必须与 mod id 完全一致
│       ├── blockstates/       # 方块状态 JSON
│       ├── models/            # 模型 JSON
│       │   ├── block/
│       │   └── item/
│       ├── textures/          # 贴图 PNG
│       │   ├── block/
│       │   ├── item/
│       │   ├── entity/
│       │   └── gui/
│       ├── lang/              # 语言文件
│       │   ├── zh_cn.json
│       │   └── en_us.json
│       ├── sounds/            # 音效 OGG
│       └── icons/             # 模组图标
├── data/
│   └── <modid>/
│       ├── recipes/           # 合成配方
│       ├── loot_tables/       # 掉落物表
│       ├── advancements/      # 进度
│       └── tags/              # 标签
└── fabric.mod.json
```

**常见错误**：
- `assets/modid/` 写成了 `assets.modid/`（用点而非斜杠）→ 资源不会被加载
- Mod ID 大小写不匹配（`assets/MyMod/` 但 id 是 `my-mod`）→ 资源不会被加载
- 文件路径在 Windows 上区分大小写 → `Textures/` 和 `textures/` 是不同的目录

---

### 11.4 注册时机与顺序

#### 11.4.1 核心原则

**所有 Registry.register() 调用必须在 onInitialize() 中执行**，否则模组加载可能失败。

```java
// ✅ 正确：在入口点方法中注册
@Override
public void onInitialize() {
    Registry.register(Registries.ITEM, new Identifier("my-mod", "my_item"), MY_ITEM);
    Registry.register(Registries.BLOCK, new Identifier("my-mod", "my_block"), MY_BLOCK);
}
```

```java
// ❌ 错误：在静态初始化器中注册（可能导致注册时机过早）
public class MyMod {
    public static final Item MY_ITEM = Registry.register(
        Registries.ITEM,
        new Identifier("my-mod", "my_item"),
        new Item(new FabricItemSettings())
    );
    
    static {
        // 这里可能会在 onInitialize 之前被调用
    }
}
```

> **注意**：在静态字段声明中直接调用 `Registry.register()` 是目前 Fabric 社区的常见写法，它实际上是安全的——因为 Java 类加载时机由 Fabric Loader 控制。但需确保该类在 `onInitialize()` 中被引用以触发初始化。

#### 11.4.2 正确的注册模式

```java
public class MyMod implements ModInitializer {
    public static final String MOD_ID = "my-mod";
    
    @Override
    public void onInitialize() {
        ModItems.register();
        ModBlocks.register();
        ModBlockEntities.register();
        // ... 其他注册
    }
}
```

```java
// ModItems.java
public class ModItems {
    public static final Item MY_ITEM = new Item(new FabricItemSettings());
    
    public static void register() {
        Registry.register(Registries.ITEM, 
            new Identifier(MyMod.MOD_ID, "my_item"), MY_ITEM);
    }
}
```

---

### 11.5 版本兼容性

#### 11.5.1 depends 声明

在 `fabric.mod.json` 中使用 `depends` 声明严格的版本依赖：

```json
"depends": {
    "fabricloader": ">=0.16.10",
    "minecraft": "~1.20.4",
    "java": ">=17",
    "fabric-api": "*"
}
```

#### 11.5.2 版本匹配检查清单

| 检查项 | 说明 |
|--------|------|
| Fabric Loader 版本 | 检查所使用的 Loader 是否支持目标 Minecraft 版本 |
| Yarn Mappings 版本 | 必须与 Minecraft 版本精确匹配 |
| Fabric API 版本 | 必须与 Minecraft 版本匹配（`0.96.x+1.20.4`）|
| Java 版本 | 1.20.4 要求 Java 17+ |
| 下游依赖版本 | 检查所有第三方库的版本兼容性 |

---

### 11.6 性能考量

#### 11.6.1 事件监听器轻量化

```java
// ❌ 不推荐：每 tick 执行重量级操作
ServerTickEvent.EVENT.register(server -> {
    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
        // 每次 tick 循环遍历所有玩家执行复杂逻辑
        heavyOperation(player);
    }
});
```

```java
// ✅ 推荐：使用节流（throttling）控制执行频率
private int tickCounter = 0;

ServerTickEvent.EVENT.register(server -> {
    tickCounter++;
    if (tickCounter % 20 == 0) {   // 每秒只执行一次
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            lightOperation(player);
        }
    }
});
```

#### 11.6.2 避免频繁 NBT 读写

```java
// ❌ 不推荐：每次 tick 都读写 NBT
@Override
public void tick() {
    CompoundTag nbt = this.createNbt();
    int value = nbt.getInt("myValue");
    nbt.putInt("myValue", value + 1);
    this.readNbt(nbt);
}
```

```java
// ✅ 推荐：缓存到内存变量，仅在必要时持久化
private int cachedValue = 0;
private boolean dirty = false;

@Override
public void tick() {
    cachedValue++;
    dirty = true;
}

// 仅在区块保存或数据变动时写入 NBT
@Override
public void writeNbt(CompoundTag nbt) {
    super.writeNbt(nbt);
    nbt.putInt("myValue", cachedValue);
}

@Override
public void readNbt(CompoundTag nbt) {
    super.readNbt(nbt);
    cachedValue = nbt.getInt("myValue");
}
```

#### 11.6.3 性能优化速查表

| 场景 | 优化建议 |
|------|----------|
| 高频事件监听 | 使用计数器节流，降低执行频率 |
| NBT 频繁访问 | 缓存到内存变量，使用 `dirty` 标记按需写入 |
| 区块遍历 | 避免每 tick 遍历区块，使用区块事件回调 |
| 实体扫描 | 使用精准过滤器（类型、标签），避免全量遍历 |
| 对象创建 | 高频场景使用对象池，减少 GC 压力 |
| 异步操作 | 文件 I/O、网络请求使用 `CompletableFuture` |
| 性能分析 | 使用 [Spark](https://spark.lucko.me/) 定位热点 |

---

### 11.7 常见报错与排查思路

#### 11.7.1 报错速查表

| 错误信息 | 可能原因 | 排查方向 |
|----------|----------|----------|
| `ClassNotFoundException: net.minecraft.client...` | 服务端加载了客户端类 | 检查 Mixin 配置，分离 client/server 类 |
| `Mixin apply failed` / `InvalidMixinException` | Mixin 目标方法签名不匹配 | 检查 Minecraft 版本是否匹配，更新 Yarn mappings |
| `NoSuchFieldError` | Fabric Loader 0.15+ 环境剥离导致 | 移除 Mixin 方法上的 `@Environment` |
| `Registry entry is missing from local registry` | 注册表同步错误 | 检查 Fabric API 版本，检查模组兼容性 |
| `NullPointerException: Ticking entity` | 实体 tick 中某对象为 null | 检查实体初始化顺序，添加 null 检查 |
| `MixinTargetAlreadyLoadedException` | 目标类在 Mixin 应用前已被加载 | 调整模组加载顺序，使用较晚的 Hook |
| `Duplicate entry` | 注册了重复的 Registry 条目 | 检查是否重复调用了 `Registry.register()` |
| 游戏启动后资源缺失（紫黑方块） | 资源路径错误 | 检查 assets 目录结构和 mod id 大小写 |

#### 11.7.2 通用排查步骤

**第一步：阅读崩溃日志**

崩溃日志位于 `crash-reports/crash-<日期>-<时间>.txt` 或 `logs/latest.log`。

重点关注：
```
---- Minecraft Crash Report ----
// 异常摘要

Description: Ticking entity

// 异常类型
java.lang.NullPointerException: Cannot invoke "..."

// 堆栈跟踪 — 找到第一个"at"指向你的模组
at com.example.mymod.MyModMixin.onTick(MyModMixin.java:25)
```

**第二步：启用调试日志**

在 JVM 参数中添加：
```bash
-Dfabric.debug=true
```

或使用 Mixin 详细日志：
```bash
--mixin.verbose
```

**第三步：二分排除法**

1. 将 `mods/` 目录中的模组分为两批
2. 只加载第一批启动测试
3. 若不崩溃 → 问题在第二批
4. 继续二分第二批，直到定位到问题模组

**第四步：常见修复手段**

- 升级 Fabric Loader 和 Fabric API 到最新版
- 检查 `fabric.mod.json` 中的版本约束
- 移除 OptiFine（改用 Sodium + Iris）
- 在关键调用点添加 `null` 安全检查

---

## 12. 学习资源

### 12.1 官方文档

#### 12.1.1 Fabric 官方文档（推荐起点）

- **网址**：[docs.fabricmc.net/1.20.4/](https://docs.fabricmc.net/1.20.4/)
- **语言**：英文（有中文版）
- **内容**：官方维护的最新文档，代码经过最新 Minecraft 版本测试
- **适合**：所有阶段开发者

**核心章节**：
- [开发环境搭建](https://docs.fabricmc.net/1.20.4/develop/getting-started)
- [物品添加](https://docs.fabricmc.net/1.20.4/develop/items)
- [方块添加](https://docs.fabricmc.net/1.20.4/develop/blocks)
- [事件系统](https://docs.fabricmc.net/1.20.4/develop/events)
- [数据生成](https://docs.fabricmc.net/1.20.4/develop/data-generation)

#### 12.1.2 Fabric Wiki

- **网址**：[wiki.fabricmc.net](https://wiki.fabricmc.net/)
- **中文版**：[wiki.fabricmc.net/zh_cn](https://wiki.fabricmc.net/zh_cn:start)
- **内容**：历史最悠久的 Fabric 文档，涵盖基础知识、教程、社区资源
- **注意**：部分内容可能较旧，建议与官方文档对照使用

**关键页面**：
- [Fabric Loom 文档](https://wiki.fabricmc.net/documentation:fabric_loom)
- [fabric.mod.json 规范](https://wiki.fabricmc.net/documentation:fabric_mod_json_spec)
- [Mixin 注册教程](https://wiki.fabricmc.net/tutorial:mixin_registration)
- [网络通信教程](https://wiki.fabricmc.net/tutorial:networking)
- [模组发布教程（中文）](https://wiki.fabricmc.net/zh_cn:tutorial:publishing_mods_using_github_actions)

---

### 12.2 Kaupenjoe YouTube 教程体系

#### 12.2.1 基本信息

- **频道**：[Modding By Kaupenjoe](https://www.youtube.com/@ModdingByKaupenjoe)
- **播放列表**：Fabric 1.20.x Modding Tutorial
- **GitHub 代码**：[Tutorials-By-Kaupenjoe/Fabric-Tutorial-1.20.X](https://github.com/Tutorials-By-Kaupenjoe/Fabric-Tutorial-1.20.X)
- **付费课程**：[courses.kaupenjoe.net](https://courses.kaupenjoe.net/p/modding-by-kaupenjoe-fabric-modding-for-minecraft-1-20-1)（$59.99，135 节课，22+ 小时）

#### 12.2.2 教程特色

- **每集对应一个代码分支**：GitHub 仓库中每个视频对应一个独立分支（如 `1-setup`、`2-customItems`），方便对比学习
- **覆盖全面**：从环境搭建到高级主题（自定义维度、网络通信、数据生成等）
- **更新及时**：跟随 Minecraft 版本升级同步更新教程

#### 12.2.3 核心教程目录（部分）

| 集数 | 内容 |
|------|------|
| 1 | 开发环境搭建（JDK、IntelliJ IDEA、Gradle）|
| 2 | 添加第一个物品和创造模式物品栏 |
| 3 | 添加方块 |
| 4 | 方块状态和模型 |
| 5 | 合成配方 |
| 6 | 掉落物表 |
| 7 | 简单方块实体 |
| 8 | 自定义合成配方类型 |
| 9 | 数据生成（Data Generation）|
| 10 | 事件与 Mixin 基础 |
| ... | ... |

---

### 12.3 Flandre芙兰 B 站视频教程

#### 12.3.1 基本信息

- **UP 主**：[Flandre芙兰](https://space.bilibili.com/4550069)
- **教程系列**：我的世界 Fabric 1.20.1 开发教程（35 集）
- **GitHub 代码**：[Flandre923/Minecraft-Fabric-tutorialmod-1.20.1](https://github.com/Flandre923/Minecraft-Fabric-tutorialmod-1.20.1)
- **语言**：中文

#### 12.3.2 教程目录（共 35 集）

| 集数 | 内容 |
|------|------|
| Ep01 | 环境配置、添加物品、物品栏、贴图、模型和语言文件 |
| Ep02 | 添加方块、生成 BlockState、Model |
| Ep03 | 生成合成表 |
| Ep04 | 生成掉落物表 |
| Ep05 | 带有功能的物品和方块 |
| Ep06 | 添加物品信息、多状态方块 |
| Ep07 | 添加作物 |
| Ep08 | 添加新的村民职业 |
| Ep09 | 添加新的画 |
| Ep10 | 矿物生成 |
| Ep11 | 修改原版掉落物表 |
| Ep12 | 自定义热键、客户端和服务端的概念 |
| Ep13 | 网络发包（Network Packet）|
| Ep14 | 饥渴值 |
| Ep15 | 绘制饥渴值 HUD |
| Ep16 | 添加流体 |
| Ep17-18 | 自定义物品 / 方块模型 |
| Ep19 | 自定义方块实体 |
| Ep20 | 自定义机器合成表 |
| Ep21 | 机器侧面输入输出 |
| Ep22 | 能量处理 |
| Ep23 | 机器处理流体 |
| Ep24 | 方块实体渲染（BER）|
| Ep25 | 添加实体 |
| Ep26 | 挖掘等级 |
| Ep27 | Fabric 事件介绍 |
| Ep28 | 树木及世界生成 |
| Ep29 | 地下晶洞 |
| Ep30 | 花及花的世界生成 |
| Ep31 | 实体世界生成 |
| Ep32 | 结构生成 |
| 补充 | 给模组添加配置信息 |
| 补充 | 添加新维度 |

#### 12.3.3 其他系列

Flandre 还提供了以下中文教程系列：
- 我的世界 1.18.2 Fabric Mod 开发教程（55 集，更加详细的基础入门）
- NeoForge 1.21 教程（40 集）
- Architectury 教程（9 集）
- OpenGL 学习（17 集）

> **建议**：对于中文学习者，Flandre 的教程是**最完整、最系统的 Fabric 中文视频教程**。建议按照 Ep01-Ep32 的顺序学习，涵盖从入门到进阶的全部内容。

---

### 12.4 Fabric Wiki 中文版

- **入口**：[wiki.fabricmc.net/zh_cn:start](https://wiki.fabricmc.net/zh_cn:start)
- **教程**：[wiki.fabricmc.net/zh_cn:tutorial:start](https://wiki.fabricmc.net/zh_cn:tutorial:start)

**可用中文教程**：
| 教程 | 链接 |
|------|------|
| Fabric 介绍 | [wiki.fabricmc.net/zh_cn:tutorial:introduction](https://wiki.fabricmc.net/zh_cn:tutorial:introduction) |
| 开发环境搭建 | [wiki.fabricmc.net/zh_cn:tutorial:setup](https://wiki.fabricmc.net/zh_cn:tutorial:setup) |
| 基本约定和术语 | [wiki.fabricmc.net/zh_cn:tutorial:terms](https://wiki.fabricmc.net/zh_cn:tutorial:terms) |
| 物品添加 | [wiki.fabricmc.net/zh_cn:tutorial:items](https://wiki.fabricmc.net/zh_cn:tutorial:items) |
| 方块添加 | [wiki.fabricmc.net/zh_cn:tutorial:blocks](https://wiki.fabricmc.net/zh_cn:tutorial:blocks) |
| 事件 | [wiki.fabricmc.net/zh_cn:tutorial:events](https://wiki.fabricmc.net/zh_cn:tutorial:events) |
| 命令 | [wiki.fabricmc.net/zh_cn:tutorial:commands](https://wiki.fabricmc.net/zh_cn:tutorial:commands) |
| 网络 | [wiki.fabricmc.net/zh_cn:tutorial:networking](https://wiki.fabricmc.net/zh_cn:tutorial:networking) |
| Mixin 注册 | [wiki.fabricmc.net/zh_cn:tutorial:mixin_registration](https://wiki.fabricmc.net/zh_cn:tutorial:mixin_registration) |
| 发布到 CurseForge 和 Modrinth | [链接](https://wiki.fabricmc.net/zh_cn:tutorial:publishing_mods_using_github_actions) |

---

### 12.5 GitHub 示例代码

#### 12.5.1 官方示例模组

- **仓库**：[FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod)
- **特点**：官方维护的入门模板，随 Fabric Loader 版本同步更新
- **用途**：作为新模组项目的起点，包含完整的目录结构和配置

#### 12.5.2 Kaupenjoe 教程代码

- **仓库**：[Tutorials-By-Kaupenjoe/Fabric-Tutorial-1.20.X](https://github.com/Tutorials-By-Kaupenjoe/Fabric-Tutorial-1.20.X)
- **特点**：每集一个独立分支，按主题组织
- **用途**：对照视频逐集学习，查看完整实现代码

#### 12.5.3 Flandre 教程代码

- **仓库**：[Flandre923/Minecraft-Fabric-tutorialmod-1.20.1](https://github.com/Flandre923/Minecraft-Fabric-tutorialmod-1.20.1)
- **特点**：中文注释，与 B 站视频完全对应
- **用途**：中文学习者的最佳参考代码

#### 12.5.4 其他推荐仓库

| 仓库 | 说明 |
|------|------|
| [Fabric API](https://github.com/FabricMC/fabric-api) | Fabric API 源码，学习 API 使用和 Mixin 技巧 |
| [Lithium](https://github.com/CaffeineMC/lithium-fabric) | 高性能优化模组，学习性能最佳的 Mixin 写法 |
| [Sodium](https://github.com/CaffeineMC/sodium-fabric) | 渲染优化模组，学习高级渲染 Mixin |
| [Mod Menu](https://github.com/TerraformersMC/ModMenu) | 简单的 API 模组，适合学习模组框架设计 |

---

### 12.6 社区资源

#### 12.6.1 Fabric Discord

- **邀请链接**：[discord.gg/v6v4pMv](https://discord.gg/v6v4pMv)（可通过 [fabricmc.net/discuss](https://fabricmc.net/discuss) 访问）
- **中文频道**：`#zh_modding`
- **用途**：实时技术讨论、问题求助、社区交流
- **语言**：英文为主，中文频道有中文社区成员

#### 12.6.2 MC百科

- **网址**：[mcmod.cn](https://www.mcmod.cn/)
- **用途**：
  - 搜索已有模组，了解功能和 API
  - 发布和介绍自己的模组
  - 模组关系查询（前置/附属/联动）

#### 12.6.3 MineBBS

- **网址**：[minebbs.com](https://www.minebbs.com/)
- **用途**：中文 Minecraft 社区论坛，有模组开发板块
- **内容**：教程、问答、资源分享

#### 12.6.4 其他社区资源

| 资源 | 链接 | 说明 |
|------|------|------|
| Fabric 官网 | [fabricmc.net](https://fabricmc.net/) | 官方入口，下载 Loader 和 API |
| Fabric 文档（新版） | [docs.fabricmc.net](https://docs.fabricmc.net/) | 官方文档（推荐首选）|
| Fabric Wiki | [wiki.fabricmc.net](https://wiki.fabricmc.net/) | 社区维护的 Wiki |
| Fabric GitHub | [github.com/FabricMC](https://github.com/FabricMC) | 源码和示例 |
| Modrinth | [modrinth.com](https://modrinth.com/) | 模组发布平台 |
| CurseForge | [curseforge.com/minecraft/mc-mods](https://www.curseforge.com/minecraft/mc-mods) | 模组发布平台 |

---

### 学习路线建议

```
第一阶段：入门基础
  ├── 完成 Flandre 的 Fabric 1.20.1 教程 Ep01-Ep10（中文）
  │   或 Kaupenjoe 的 Fabric 1.20.x 前 10 集（英文）
  ├── 对应阅读 Fabric Wiki 中文版的物品/方块/配方章节
  └── 目标：能添加物品、方块、合成配方和掉落物

第二阶段：进阶功能
  ├── 学习事件系统、DataGen、网络通信（Ep11-Ep20）
  ├── 阅读官方文档对应章节
  └── 目标：能实现自定义方块实体、GUI、数据生成

第三阶段：高级主题
  ├── 世界生成、实体、自定义维度（Ep21-Ep32）
  ├── 深入学习 Mixin（阅读 Fabric API 源码）
  └── 目标：能实现完整功能模组

第四阶段：发布与维护
  ├── 配置 GitHub Actions CI/CD
  ├── 创建 CurseForge 和 Modrinth 项目
  ├── 编写 fabric.mod.json 元数据
  └── 目标：成功发布模组

第五阶段：最佳实践
  ├── 学习 Client/Server 分离的正确做法
  ├── 掌握 Mixin 冲突预防技巧
  ├── 应用性能优化原则
  └── 目标：写出稳定、高效、兼容性好的模组
```

---

> **文档信息**：本文档基于 Fabric 1.20.4 编写。部分内容（如版本号、API 版本）需根据实际开发环境调整。建议始终参考 [docs.fabricmc.net](https://docs.fabricmc.net/) 获取最新信息。
