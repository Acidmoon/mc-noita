# Fabric 1.20.4 模组开发实战指南

> **搜集时间**：2026-05-29 | **状态**：初稿（待审查）
> **信息来源**：4 个 Agent 的深度搜索，共 6,777 行原始素材
> **覆盖版本**：Minecraft 1.20.4 + Fabric Loader 0.15.x + Fabric API 0.92.0+ + JDK 17

---

## 引言

Fabric 是 Minecraft 模组开发中最轻量、更新最快的加载器。本文聚焦 **1.20.4** 这一特定版本，从环境搭建到打包发布，覆盖 Fabric 模组开发的完整技术栈。所有代码示例均为 Fabric 1.20.4 可用的 API，并标注了版本特有的注意事项。

---

## 1. 开发环境搭建

### 1.1 JDK 版本

| 项目 | 说明 |
|------|------|
| **最低要求** | JDK 17（Minecraft 1.20.4 的硬性要求） |
| **推荐发行版** | Eclipse Temurin（Adoptium） |
| **下载地址** | https://adoptium.net/temurin/releases/?version=17 |

安装后验证：`java -version` 应显示 `openjdk version "17.0.x"`。设置 `JAVA_HOME` 环境变量。

### 1.2 IntelliJ IDEA + MCDev 插件

推荐 IntelliJ IDEA Community Edition（免费版）。安装 **Minecraft Development 插件**（Marketplace 搜索 "Minecraft Development"，插件 ID：`com.demonwav.minecraft-dev`），提供以下功能：
- Fabric 项目自动生成
- Mixin 语法高亮、访问器生成、影子字段
- 构建系统集成
- 运行配置自动创建

### 1.3 项目创建方式

**方式一（推荐）**：访问 [Fabric 官方模板生成器](https://fabricmc.net/develop/template/)，填写 Mod Name/Mod ID/Package Name，选择 Minecraft Version `1.20.4`，映射可选 Yarn 或 Mojang，点击 Generate 下载 ZIP，解压后用 IDEA 打开。

**方式二**：克隆 `git clone https://github.com/FabricMC/fabric-example-mod.git`，修改 `gradle.properties` 适配 1.20.4。

**方式三**：安装 MCDev 插件后，`File → New → Project → Minecraft` 按向导创建。

### 1.4 Fabric Loom Gradle 配置

**关键依赖版本（1.20.4）：**

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Fabric Loader | 0.14.21 | 0.15.11+ |
| Fabric Loom | 1.5+ | 1.6-SNAPSHOT |
| Fabric API | 0.88.0+ | 0.92.0+1.20.4 |

**gradle.properties** 示例：
```properties
org.gradle.jvmargs=-Xmx2G -XX:+CMSClassUnloadingEnabled
minecraft_version=1.20.4
loader_version=0.15.11
yarn_mappings=1.20.4+build.3
fabric_version=0.92.0+1.20.4
mod_version=1.0.0
maven_group=com.example
archives_base_name=my-mod
```

**build.gradle** 关键配置：
```gradle
plugins {
    id 'fabric-loom' version '1.6-SNAPSHOT'
}
dependencies {
    minecraft "com.mojang:minecraft:${project.minecraft_version}"
    mappings "net.fabricmc:yarn:${project.yarn_mappings}:v2"
    modImplementation "net.fabricmc:fabric-loader:${project.loader_version}"
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
}
```

### 1.5 国内网络优化

中国内地用户常遇到 Gradle 依赖下载超时，可用以下方案：

**方案一：settings.gradle 添加镜像**
```groovy
pluginManagement {
    repositories {
        maven { name = 'HanBing'; url = 'https://repository.hanbings.io/proxy' }
        maven { name = 'Fabric'; url = 'https://maven.fabricmc.net/' }
        gradlePluginPortal()
    }
}
```

**方案二：build.gradle 添加阿里云 Maven**
```groovy
repositories {
    maven { url = 'https://maven.aliyun.com/nexus/content/groups/public' }
    maven { url = 'https://maven.fabricmc.net/' }
    mavenCentral()
}
```

**方案三：gradle.properties 配置代理**
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 1.6 Gradle Wrapper

项目根目录包含 `gradlew`（Linux/Mac）和 `gradlew.bat`（Windows）脚本。这就是 **Gradle Wrapper**——它自动下载项目所需的 Gradle 版本，确保所有开发者（和 CI）使用完全相同的 Gradle 版本构建。永远用 `./gradlew` 代替系统安装的 `gradle`。

### 1.7 Gradle 关键任务

| 任务 | 命令 | 用途 |
|------|------|------|
| genSources | `./gradlew genSources` | 下载并反编译 Minecraft 源码 |
| runClient | `./gradlew runClient` | 启动测试用 Minecraft 客户端 |
| runServer | `./gradlew runServer` | 启动测试用 Minecraft 服务器 |
| build | `./gradlew build` | 编译并打包模组 JAR |
| runDatagen | `./gradlew runDatagen` | 运行数据生成器 |

---

## 2. 项目结构与配置

### 2.1 标准项目目录

```
my-mod/
├── src/main/
│   ├── java/com/example/mymod/
│   │   ├── MyMod.java                  # 主入口（ModInitializer）
│   │   ├── MyModClient.java            # 客户端入口（ClientModInitializer）
│   │   ├── ModItems.java               # 物品注册
│   │   ├── ModBlocks.java              # 方块注册
│   │   ├── ModBlockEntities.java       # 方块实体注册
│   │   ├── ModEntities.java            # 实体注册
│   │   ├── ModItemGroups.java          # 物品栏分组
│   │   ├── data/ModDataGenerator.java  # 数据生成入口
│   │   └── mixin/ExampleMixin.java     # Mixin 类
│   └── resources/
│       ├── fabric.mod.json             # 模组描述文件
│       ├── assets/mymod/
│       │   ├── textures/               # 贴图文件
│       │   ├── models/                 # 模型 JSON
│       │   └── lang/                   # 语言文件
│       └── mixin.mymod.json            # Mixin 配置
├── build.gradle
├── gradle.properties
└── settings.gradle
```

### 2.2 fabric.mod.json 全字段解析

```json
{
  "schemaVersion": 1,
  "id": "mymod",
  "version": "1.0.0",
  "name": "My Mod",
  "description": "描述文字",
  "authors": ["Your Name"],
  "contact": {
    "homepage": "https://example.com",
    "sources": "https://github.com/you/mymod"
  },
  "license": "MIT",
  "icon": "assets/mymod/icon.png",
  "environment": "*",
  "entrypoints": {
    "main": ["com.example.mymod.MyMod"],
    "client": ["com.example.mymod.MyModClient"],
    "fabric-datagen": ["com.example.mymod.data.ModDataGenerator"]
  },
  "mixins": [{"config": "mixin.mymod.json", "environment": "*"}],
  "depends": {
    "fabricloader": ">=0.15.11",
    "minecraft": "~1.20.4",
    "java": ">=17",
    "fabric-api": "*"
  },
  "suggests": {
    "another-mod": "*"
  }
}
```

**核心字段说明**：

| 字段 | 类型 | 说明 |
|------|------|------|
| `schemaVersion` | int | 固定为 1 |
| `id` | string | 模组唯一标识，全小写、无空格（如 `my-mod`、`mymod`） |
| `version` | string | 遵循语义化版本，如 `1.0.0` |
| `environment` | string | `*`（双端）、`client`（仅客户端）、`server`（仅服务端） |
| `entrypoints` | object | 入口点配置（见下文） |
| `mixins` | array | Mixin 配置 JSON 路径 |
| `depends` | object | 依赖声明 |

### 2.3 Entrypoints 体系

| Entrypoint | 接口 | 加载时机 | 职责 |
|-----------|------|---------|------|
| `main` | `ModInitializer` | 游戏启动时，双端执行 | 注册物品/方块/实体/事件监听 |
| `client` | `ClientModInitializer` | 仅客户端 | 密钥绑定、实体渲染、HUD |
| `server` | `DedicatedServerModInitializer` | 仅专用服务器 | 服务端专属初始化 |
| `fabric-datagen` | `DataGeneratorEntrypoint` | 运行 runDatagen 时 | 数据生成 Provider |

### 2.4 依赖版本语法

| 语法 | 含义 | 示例 |
|------|------|------|
| `>=1.0.0` | 大于等于 | `"fabricloader": ">=0.15.11"` |
| `>=1.0.0 <2.0.0` | 范围 | 闭区间版本范围 |
| `~1.20.4` | 近似（兼容 ~1.20.x） | `"minecraft": "~1.20.4"` |
| `*` | 任意版本 | `"fabric-api": "*"` |
| `1.0.0` | 精确版本 | 单个版本号 |

### 2.5 映射（Mapping）原理

Minecraft 的源码被 Mojang 混淆（obfuscation）——类名、方法名被改为 `a`、`b`、`func_12345_a` 等无意义名称。映射（mapping）就是一张"翻译表"，把这些混淆名还原为可读的名称。

Fabric 使用 **Intermediary** 映射作为中间层：模组基于 Intermediary 编译，游戏运行时再转换为当前版本的混淆名。这意味着模组在不同 Minecraft 版本间有一定的兼容性，这也是 Fabric 能快速适配新版本的技术基础。

**映射方案对比：**

| 方案 | 维护方 | 许可证 | 特点 |
|------|--------|--------|------|
| **Yarn** | Fabric 社区 | CC0 | 开源免费，参数名可读性好 |
| **Mojang** | Mojang | 专有 | 官方提供，方法名较全 |
| **Parchment** | ParchmentMC | MIT | 在 Mojang 基础上增加参数名，可读性最好 |

Yarn 和 Mojang 的方法名不同。例如获取世界实例：
- Yarn：`world.getBlockState(pos)`
- Mojang：`level.getBlockState(pos)`

---

## 3. 核心内容开发

### 3.1 自定义物品

**创建基本物品：**

```java
// ModItems.java
public class ModItems {
    // 简单物品
    public static final Item MY_ITEM = new Item(new Item.Settings()
        .maxCount(64)
        .rarity(Rarity.COMMON)
    );

    // 自定义行为物品
    public static final Item SPECIAL_ITEM = new Item(new Item.Settings()
        .maxCount(16)
        .rarity(Rarity.RARE)
        .fireproof()  // 不会被岩浆/火焰销毁
    ) {
        @Override
        public ActionResult useOnBlock(ItemUsageContext context) {
            if (!context.getWorld().isClient) {
                context.getPlayer().sendMessage(Text.literal("方块被右键了！"), false);
            }
            return ActionResult.SUCCESS;
        }
    };

    public static void registerModItems() {
        Registry.register(Registries.ITEM, new Identifier("mymod", "my_item"), MY_ITEM);
        Registry.register(Registries.ITEM, new Identifier("mymod", "special_item"), SPECIAL_ITEM);
    }
}
```

然后在主类 `onInitialize()` 中调用 `ModItems.registerModItems()`。

**Item.Settings 常用方法：**

| 方法 | 参数 | 说明 |
|------|------|------|
| `.maxCount(int)` | 1-99 | 最大堆叠数（默认 64） |
| `.maxDamage(int)` | 耐久值 | 设置耐久度（设置后 maxCount 失效） |
| `.rarity(Rarity)` | COMMON/UNCOMMON/RARE/EPIC | 物品名称颜色 |
| `.fireproof()` | 无 | 免疫火焰/岩浆 |
| `.food(FoodComponent)` | FoodComponent | 食物属性 |
| `.recipeRemainder(Item)` | Item | 合成后留下的物品（如桶） |

### 3.2 自定义方块

```java
// ModBlocks.java
public class ModBlocks {
    // 简单方块
    public static final Block MY_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.STONE)
        .strength(3.0f, 10.0f)     // 挖掘硬度 / 爆炸抗性
        .requiresTool()             // 需要合适工具
        .sounds(BlockSoundGroup.STONE)
        .luminance(state -> 15)     // 自发光
    );

    public static void registerModBlocks() {
        Registry.register(Registries.BLOCK, new Identifier("mymod", "my_block"), MY_BLOCK);
    }

    // 同时注册 BlockItem
    public static void registerModBlockItems() {
        Registry.register(Registries.ITEM, new Identifier("mymod", "my_block"),
            new BlockItem(MY_BLOCK, new Item.Settings()));
    }
}
```

**Block.Settings 常用方法：**

| 方法 | 说明 |
|------|------|
| `.strength(float hardness, float resistance)` | 硬度和爆炸抗性 |
| `.requiresTool()` | 需要正确工具才能掉落 |
| `.sounds(BlockSoundGroup)` | 声音类型 |
| `.luminance(Function<BlockState, Integer>)` | 光照等级 0-15 |
| `.nonOpaque()` | 非完整方块（透明纹理） |
| `.noCollision()` | 无碰撞（如植物） |
| `.slipperiness(float)` | 摩擦系数 |
| `.ticksRandomly()` | 随机 tick |

### 3.3 状态属性（BlockState Properties）

```java
public class MyDirectionalBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.of("facing",
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    public MyDirectionalBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
            .with(FACING, Direction.NORTH)
            .with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
```

### 3.4 实体方块（BlockEntity）

需要：BlockEntity 类 + BlockEntityType 注册 + 带 BlockEntity 的方块

```java
// 1. BlockEntity 类
public class MyBlockEntity extends BlockEntity {
    private int counter = 0;

    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_BLOCK_ENTITY, pos, state);
    }

    @Override
    public void writeNbt(NbtCompound nbt) {
        nbt.putInt("counter", counter);
        super.writeNbt(nbt);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        counter = nbt.getInt("counter");
    }
}

// 2. 注册 BlockEntityType
public static final BlockEntityType<MyBlockEntity> MY_BLOCK_ENTITY = Registry.register(
    Registries.BLOCK_ENTITY_TYPE,
    new Identifier("mymod", "my_block_entity"),
    FabricBlockEntityTypeBuilder.create(MyBlockEntity::new, ModBlocks.MY_BLOCK).build()
);

// 3. 创建带 BlockEntity 的方块（实现 EntityBlock 接口）
public class MyEntityBlock extends Block implements EntityBlock {
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MyBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (world1, pos, state1, be) -> ((MyBlockEntity) be).tick();
    }
}
```

### 3.5 自定义物品栏分组

```java
public static final ItemGroup MY_GROUP = FabricItemGroup.builder()
    .icon(() -> new ItemStack(ModItems.MY_ITEM))
    .displayName(Text.literal("My Mod"))
    .entries(((context, entries) -> {
        entries.add(ModItems.MY_ITEM);
        entries.add(ModBlocks.MY_BLOCK);
    }))
    .build();
```

如果需要将物品添加到原版物品栏（而非新建分组），使用 `ItemGroupEvents.modifyEntriesEvent()`：
```java
ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
    entries.add(ModItems.MY_ITEM);
});
```

### 3.6 工具/武器/护甲

**工具材料：** 实现 `ToolMaterial` 接口（或使用内置枚举如 `ToolMaterials.DIAMOND`）。
**护甲材料：** 实现 `ArmorMaterial` 接口（需实现 8 个方法：耐久乘数、防护值、附魔能力、装备声音等）。

工具注册示例：
```java
Registry.register(Registries.ITEM, new Identifier("mymod", "my_sword"),
    new SwordItem(MyToolMaterial.INSTANCE, 3, -2.4f, new Item.Settings()));
```

### 3.7 食物

```java
public static final FoodComponent MY_FOOD = new FoodComponent.Builder()
    .hunger(6)              // 恢复饱食度
    .saturationModifier(0.6f) // 饱和度修正
    .statusEffect(new StatusEffectInstance(StatusEffects.REGENERATION, 100, 0), 1.0f)
    .build();

public static final Item MY_FOOD_ITEM = new Item(new Item.Settings()
    .food(MY_FOOD));
```

---

## 4. 数据生成

### 4.1 启用与配置

**build.gradle 配置：**
```gradle
fabricApi {
    configureDataGeneration()
}
```

**DataGeneratorEntrypoint 类：**
```java
public class ModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModBlockLootTableProvider::new);
        pack.addProvider(ModEnglishLangProvider::new);
        pack.addProvider(ModTagProvider::new);
    }
}
```

**fabric.mod.json 注册：**
```json
"entrypoints": {
    "fabric-datagen": ["com.example.mymod.data.ModDataGenerator"]
}
```

运行：`./gradlew runDatagen`，输出到 `src/main/generated/`。

**重要：** 在 `build.gradle` 中配置数据生成输出目录，确保生成的 JSON 被包含在 JAR 中：
```gradle
loom {
    runs {
        datagen {
            inherit server
            name "Data Generation"
            vmArg "-Dfabric-api.datagen"
            vmArg "-Dfabric-api.datagen.output-dir=${file("src/main/generated")}"
            vmArg "-Dfabric-api.datagen.modid=mymod"
        }
    }
}
sourceSets {
    main { resources { srcDirs += ['src/main/generated'] } }
}
```

### 4.2 Provider 体系

| Provider | 用途 | 关键类 |
|----------|------|--------|
| 模型 | 生成方块状态 JSON 和物品模型 | `FabricModelProvider` |
| 配方 | 生成合成配方 | `FabricRecipeProvider` |
| 战利品表 | 生成方块掉落表 | `FabricBlockLootTableProvider` |
| 语言 | 生成 en_us/zh_cn 语言文件 | `FabricLanguageProvider` |
| 标签 | 生成方块/物品标签 | `FabricTagProvider` |
| 动态注册 | 世界生成等动态数据 | `FabricDynamicRegistryProvider` |

### 4.3 配方生成示例

```java
public class ModRecipeProvider extends FabricRecipeProvider {
    @Override
    protected void generateRecipes(Consumer<RecipeJsonProvider> exporter) {
        // 有序合成
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.MY_ITEM, 1)
            .pattern("XXX")
            .pattern(" Y ")
            .pattern("ZZZ")
            .input('X', Items.IRON_INGOT)
            .input('Y', Items.DIAMOND)
            .input('Z', Items.STONE)
            .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
            .offerTo(exporter);

        // 无序合成
        ShapelessRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.SPECIAL_ITEM, 2)
            .input(Items.EMERALD, 2)
            .input(ModItems.MY_ITEM)
            .criterion("has_my_item", conditionsFromItem(ModItems.MY_ITEM))
            .offerTo(exporter);

        // 熔炉烧炼
        CookingRecipeJsonBuilder.createSmelting(
                Ingredient.ofItems(ModItems.RAW_ORE),
                RecipeCategory.MISC,
                ModItems.INGOT,
                1.0f, 200)
            .criterion("has_raw_ore", conditionsFromItem(ModItems.RAW_ORE))
            .offerTo(exporter);
    }
}
```

> **注意：** 1.20.4 的 `generateRecipes()` 接收 `Consumer<RecipeJsonProvider>`，与 1.21+ 的 `generate(RecipeExporter)` 签名不同。

### 4.4 语言文件生成

```java
public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataOutput dataOutput) {
        super(dataOutput);
    }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.MY_ITEM, "My Item");
        translationBuilder.add(ModBlocks.MY_BLOCK, "My Block");
        translationBuilder.add(ModItems.MY_GROUP, "My Mod Tab");
    }
}
```

> **注意：** 1.20.4 的 `FabricLanguageProvider` 构造函数只接受 `FabricDataOutput`，`generateTranslations()` 只有单参数版本 `TranslationBuilder`。1.21+ 的 `HolderLookup.Provider` 双参数签名在 1.20.4 中不可用。

---

## 5. 事件系统与 Mixin

### 5.1 Fabric 事件系统

Fabric 使用回调接口 + `EventFactory`，通过 `InteractionResult` 返回值实现链式处理。

```java
// 方块破坏前事件
PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, entity) -> {
    if (/* 条件 */) {
        return false; // 取消破坏
    }
    return true; // 允许破坏
});

// 方块破坏后事件
PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, entity) -> {
    // 方块已被破坏
});
```

**常用事件索引：**

| 事件 | 用途 |
|------|------|
| `AttackBlockCallback` | 攻击方块时 |
| `PlayerBlockBreakEvents` | 方块破坏前/后 |
| `UseBlockCallback` | 右键方块 |
| `UseEntityCallback` | 右键实体 |
| `EntityJoinWorldCallback` | 实体加入世界 |
| `ServerLifecycleEvents` | 服务器启动/停止 |
| `ServerTickEvents` | 服务器/世界每 tick |
| `LootTableEvents` | 修改战利品表 |

### 5.2 Mixin 推荐实践

**Mixin 使用优先级：** 优先使用 Fabric API 事件系统 → 需要修改原版代码时使用 `@Inject` → 需要修改方法返回值时使用 `@ModifyExpressionValue` → 需要包装操作时使用 `@WrapOperation`。

**@Inject（最常用，低风险）：**
```java
@Mixin(MinecraftServer.class)
public class ExampleMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onServerTick(CallbackInfo info) {
        // 每 tick 执行
    }
}
```

**推荐的 Mixin 类型（按风险从低到高）：**

| Mixin 类型 | 风险 | 用途 |
|-----------|------|------|
| `@Accessor` / `@Invoker` | 极低 | 暴露私有字段/方法 |
| `@Inject` | 低 | 在方法指定位置注入代码 |
| `@ModifyExpressionValue` | 低 | 修改表达式返回值 |
| `@WrapOperation` | 中 | 包装操作，可链式调用 |
| `@Redirect` | 高 | 重定向方法调用（不推荐）|
| `@Overwrite` | 极高 | 完全替换方法体（不推荐）|

**避免 `@Overwrite`：** 它完全替换整个方法体，两个模组同时 `@Overwrite` 同一方法时只有一个生效。替代方案：`@Inject + CallbackInfo.cancel()` 或 `@WrapOperation`。

### 5.3 Access Widener

在 `src/main/resources/` 下创建 `mymod.accesswidener`：
```
accessWidener v2 named

accessible class net/minecraft/world/level/block/entity/BrewingStandBlockEntity
accessible field net/minecraft/world/entity/player/Player sleepTimer I
```

在 `build.gradle` 中配置：
```gradle
loom {
    accessWidenerPath = file("src/main/resources/mymod.accesswidener")
}
```

在 `fabric.mod.json` 中注册：
```json
"accessWidener": "mymod.accesswidener"
```

### 5.4 Mixin 调试

```java
@Debug(export = true)  // 导出混入后的 class 文件到 .mixin.out/
@Mixin(TargetClass.class)
public class DebugMixin { ... }
```

启动参数：添加 `-Dmixin.debug.export=true` 到 JVM 参数。

---

## 6. 自定义实体与 AI

### 6.1 EntityType 注册

```java
public static final EntityType<MyEntity> MY_ENTITY = Registry.register(
    Registries.ENTITY_TYPE,
    new Identifier("mymod", "my_entity"),
    FabricEntityTypeBuilder.create(MobCategory.MONSTER, MyEntity::new)
        .dimensions(EntityDimensions.fixed(1.0f, 2.0f))
        .trackRangeBlocks(64)
        .trackedUpdateRate(3)
        .fireImmune()
        .build()
);

// 注册默认属性
FabricDefaultAttributeRegistry.register(MY_ENTITY, MyEntity.createMobAttributes());
```

### 6.2 AI Goal 系统

```java
public class MyEntity extends PathAwareEntity {
    @Override
    protected void initGoals() {
        this.goalSelector.add(1, new SwimGoal(this));
        this.goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.add(3, new WanderAroundFarGoal(this, 0.8D));
        this.goalSelector.add(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.add(5, new LookAroundGoal(this));
        this.targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
}
```

### 6.3 生成规则

```java
SpawnRestriction.register(MY_ENTITY, SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
    (type, world, spawnReason, pos, random) -> 
        pos.getY() < 40 && world.getBaseLightLevel(pos, 0) > 8);

BiomeModifications.addSpawn(
    BiomeSelectors.foundInOverworld(),
    MobCategory.MONSTER, MY_ENTITY, 100, 1, 4);
```

### 6.4 实体渲染

```java
// 在 ClientModInitializer 中
EntityRendererRegistry.register(ModEntities.MY_ENTITY, MyEntityRenderer::new);
EntityModelLayerRegistry.registerModelLayer(ModModelLayers.MY_ENTITY, MyEntityModel::getTexturedModelData);
```

---

## 7. 世界生成与维度

### 7.1 三层架构

| 层级 | 类 | 职责 | 位置 |
|------|------|------|------|
| ConfiguredFeature | `ConfiguredFeature<FC, F>` | 定义生成什么（方块、大小、参数） | 代码注册或 JSON |
| PlacedFeature | `PlacedFeature` | 定义如何放置（次数、高度、修饰符链） | 代码注册或 JSON |
| BiomeModification | `BiomeModifications` | 定义在哪生成（选择生物群系+阶段） | Fabric API 运行时 |

```java
// ConfiguredFeature 注册
public static final ConfiguredFeature<?, ?> MY_ORE = new ConfiguredFeature<>(
    Feature.ORE,
    new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,
        ModBlocks.MY_ORE_BLOCK.getDefaultState(), 8));

// PlacedFeature 注册
public static final PlacedFeature MY_ORE_PLACED = new PlacedFeature(
    RegistryEntry.of(MY_ORE),
    List.of(CountPlacementModifier.of(20), SquarePlacementModifier.of(),
        HeightRangePlacementModifier.uniform(VerticalAnchor.aboveBottom(0), VerticalAnchor.absolute(64)),
        BiomePlacementModifier.of()));

// 注册（BuiltinRegistries 在 1.20.4 可用但标记 @Deprecated，推荐后续切换至数据生成）
Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, id("my_ore"), MY_ORE);
Registry.register(BuiltinRegistries.PLACED_FEATURE, id("my_ore"), MY_ORE_PLACED);

// BiomeModification 挂接
BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
    GenerationStep.Feature.UNDERGROUND_ORES,
    RegistryKey.of(RegistryKeys.PLACED_FEATURE, id("my_ore")));
```

### 7.2 常用放置修饰符

| 修饰符 | 用途 |
|--------|------|
| `CountPlacementModifier.of(n)` | 每区块 n 次 |
| `RarityFilterPlacementModifier.of(chance)` | 1/chance 概率 |
| `SquarePlacementModifier.of()` | 随机 X/Z 偏移 |
| `HeightmapPlacementModifier.of(type)` | 基于高度图决定 Y |
| `HeightRangePlacementModifier.uniform(min, max)` | Y 范围均匀分布 |
| `BiomePlacementModifier.of()` | 按生物群系过滤 |

### 7.3 自定义维度

需要：Java 端 RegistryKey + dimension_type JSON + dimension JSON

**Java 注册：**
```java
public static final RegistryKey<DimensionOptions> DIM_KEY =
    RegistryKey.of(RegistryKeys.DIMENSION, new Identifier("mymod", "my_dimension"));
public static final RegistryKey<World> WORLD_KEY =
    RegistryKey.of(RegistryKeys.WORLD, new Identifier("mymod", "my_dimension"));
```

**dimension_type JSON**（`data/mymod/dimension_type/my_dimension_type.json`）定义环境参数（ambient_light、has_skylight、height、ultrawarm 等）。

**dimension JSON**（`data/mymod/dimension/my_dimension.json`）引用 dimension_type 和生成器配置。

### 7.4 传送门（Custom Portal API）

```groovy
repositories {
    maven { url = "https://maven.kyrptonaught.dev" }
}
dependencies {
    modImplementation 'net.kyrptonaught:customportalapi:0.0.1-beta66-1.20'
    include 'net.kyrptonaught:customportalapi:0.0.1-beta66-1.20'
}
```

```java
CustomPortalBuilder.beginPortal()
    .frameBlock(Blocks.GOLD_BLOCK)
    .lightWithItem(Items.ENDER_EYE)
    .destDimID(new Identifier("mymod", "my_dimension"))
    .tintColor(234, 183, 8)
    .registerPortal();
```

---

## 8. 网络通信

### 8.1 基础 API（1.20.4 旧版，非 Payload 模式）

**C2S（客户端→服务端）：**
```java
// 服务端接收
ServerPlayNetworking.registerGlobalReceiver(PACKET_ID, (server, player, handler, buf, sender) -> {
    BlockPos pos = buf.readBlockPos();
    server.execute(() -> {
        // 在服务线程安全执行
    });
});

// 客户端发送
ClientPlayNetworking.send(PACKET_ID, PacketByteBufs.create().writeBlockPos(pos));
```

**S2C（服务端→客户端）：**
```java
// 客户端接收
ClientPlayNetworking.registerGlobalReceiver(PACKET_ID, (client, handler, buf, sender) -> {
    int data = buf.readInt();
    client.execute(() -> {
        // 在客户端线程安全执行
    });
});

// 服务端发送
ServerPlayNetworking.send(player, PACKET_ID, PacketByteBufs.create().writeInt(data));
```

### 8.2 PacketByteBuf 常用读写方法

| 方法 | 用途 |
|------|------|
| `writeInt/readInt` | 整数 |
| `writeString/readString` | 字符串 |
| `writeBlockPos/readBlockPos` | 方块坐标 |
| `writeIdentifier/readIdentifier` | 标识符 |
| `writeNbt/readNbt` | NBT 数据 |
| `writeItemStack/readItemStack` | 物品栈 |
| `writeVarInt/readVarInt` | 可变长度整数（网络优化）|

### 8.3 线程安全

Fabric 网络回调可能在网络线程执行。访问游戏世界数据时，必须切换到主线程：
```java
server.execute(() -> {
    // 安全操作游戏数据
});
client.execute(() -> {
    // 安全操作客户端数据
});
```

---

## 9. 高级主题

### 9.1 Fabric Transfer API（能源/流体）

Fabric 不像 Forge 那样内置能量系统，而是提供通用传输框架。

**核心概念：**
- `Storage<T>` — 泛型存储接口
- `FluidVariant` / `ItemVariant` — 带 NBT 的流体/物品类型
- `SingleVariantStorage<T>` — 单槽存储实现
- `Transaction` — 原子化传输操作

```java
// 在 BlockEntity 中暴露能量存储
public class MyMachineBlockEntity extends BlockEntity {
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getDefaultVariant() {
            return FluidVariant.blank();
        }
        @Override
        protected long getCapacity(FluidVariant variant) {
            return 8000; // 8 桶
        }
    };
}
```

### 9.2 自定义 GUI

注册 MenuType → 创建 ScreenHandler → 创建 Screen → 在 BlockEntity 中打开 GUI

```java
// 注册 ScreenHandlerType
// 注意：Yarn 映射中使用 Registries.SCREEN_HANDLER，Mojang 映射中使用 BuiltInRegistries.MENU
public static final ScreenHandlerType<MyScreenHandler> MY_SCREEN_HANDLER = Registry.register(
    Registries.SCREEN_HANDLER,
    new Identifier("mymod", "my_screen"),
    new ScreenHandlerType<>((syncId, inv, buf) -> new MyScreenHandler(syncId, inv, buf.readBlockPos())));
```

### 9.3 BlockEntityRenderer（BER）

```java
// 注册（1.20.4 中 BlockEntityRendererRegistry 已弃用，使用 BlockEntityRendererFactories）
BlockEntityRendererFactories.register(ModBlockEntities.MY_BLOCK_ENTITY, MyBlockEntityRenderer::new);

// 渲染逻辑
public class MyBlockEntityRenderer implements BlockEntityRenderer<MyBlockEntity> {
    @Override
    public void render(MyBlockEntity entity, float tickDelta, MatrixStack matrices,
            VertexConsumerProvider vertexConsumers, int light, int overlay) {
        // 使用矩阵栈和顶点缓冲渲染
    }
}
```

### 9.4 配置系统（Cloth Config API）

依赖配置：
```groovy
modImplementation("me.shedaniel.cloth:cloth-config-fabric:12.0.109") { exclude(group: "net.fabricmc.fabric-api") }
modImplementation "com.terraformersmc:modmenu:8.0.0"
```

```java
@Config(name = "mymod")
public class ModConfig {
    @Entry public static boolean enableFeature = true;
    @Entry(min = 0, max = 100) public static int someValue = 50;
}
```

---

## 10. 打包发布

### 10.1 构建与产物

```bash
./gradlew build        # 生成最终 JAR
```

`build/libs/` 下产出三种 JAR：
- `my-mod-1.0.0.jar` — **发布用**（remapJar 产物，已映射到混淆名）
- `my-mod-1.0.0-dev.jar` — 开发用（未 remap，不能用于游戏）
- `my-mod-1.0.0-sources.jar` — 源码 JAR

**为什么需要 remap？** 模组代码基于 Yarn（可读名称）编译，但 Minecraft 运行时使用混淆名。`remapJar` 任务将你的模组从编译时的映射名称转换回运行时能识别的混淆名称——这个过程叫重映射（remapping）。如果直接使用开发版 JAR，游戏会因为找不到类而崩溃。

**关键提醒：** 发布到 CurseForge/Modrinth 时必须使用 `remapJar` 产物。

### 10.2 发布平台

| 平台 | 特点 | URL |
|------|------|------|
| **CurseForge** | 最大平台，用户最多 | curseforge.com |
| **Modrinth** | 开源，开发者友好 | modrinth.com |
| **GitHub Releases** | 配合 CI/CD | github.com |

### 10.3 CI/CD 自动发布（GitHub Actions）

```yaml
name: Publish
on: [workflow_dispatch]
jobs:
  publish:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { java-version: 17, distribution: temurin }
      - run: ./gradlew build publishMods
        env:
          MODRINTH_TOKEN: ${{ secrets.MODRINTH_TOKEN }}
          CURSEFORGE_TOKEN: ${{ secrets.CURSEFORGE_TOKEN }}
```

### 10.4 许可证选择

| 许可证 | 特点 | 适用场景 |
|--------|------|---------|
| MIT | 最宽松，允许任意使用 | 推荐大多数模组使用 |
| LGPL-3.0 | 需开源修改后的库代码 | 库/API 模组 |
| CC0 | 放弃所有权利 | 数据包/资源包 |
| ARR | 保留所有权利 | 不想被分发的模组 |

---

## 11. 常见陷阱与最佳实践

### 11.1 Client/Server 分离

**错误写法：** 在服务端加载了客户端类 → `ClassNotFoundException`。

**正确做法：**
- 客户端专属代码放在 `client` entrypoint 中
- `fabric.mod.json` 中设 `"environment": "client"` 的模组只在客户端加载
- 使用 `@Environment(EnvType.CLIENT)` 注解隔离客户端方法
- 避免在服务端代码中直接引用 `net.minecraft.client` 包下的类

### 11.2 Mixin 冲突预防

| 风险等级 | 做法 |
|---------|------|
| ✅ 推荐 | 用 Fabric API 事件代替 Mixin |
| ✅ 推荐 | 用 `@Inject` 而不是 `@Overwrite` |
| ✅ 推荐 | 用完整方法描述符 `tick()V` 而非 `tick` |
| ❌ 避免 | `@Overwrite` — 会静默覆盖其他模组 |
| ❌ 避免 | `@Redirect` — 多个模组无法共存 |

### 11.3 资源路径规范

- Mod ID 必须**全小写**，用短横线分隔（如 `my-mod`）
- 资源路径：`assets/<modid>/textures/`、`assets/<modid>/models/`、`assets/<modid>/lang/`
- 每个物品/方块至少需要：纹理 PNG + 模型 JSON + 语言文件条目

### 11.4 注册时机

所有 `Registry.register()` 调用**必须在 `onInitialize()` 中完成**，不能在游戏运行时动态注册。

### 11.5 常见报错速查

| 错误 | 可能原因 | 解决方案 |
|------|---------|---------|
| `Mixin apply failed` | Mixin 目标类结构变更 | 检查映射版本 |
| `ClassNotFoundException` (client) | 在服务端引用了客户端类 | 用 `@Environment(CLIENT)` 隔离 |
| `NullPointerException` (registry) | 注册顺序问题 | 确保在 onInitialize 中注册 |
| 模型显示为紫黑方块 | 纹理/模型 JSON 缺失 | 检查 assets 结构 |
| `java.lang.VerifyError` | Mixin 冲突 | 检查 `@Overwrite` 使用 |

---

## 12. 学习资源

### 12.1 官方文档

- [Fabric 1.20.4 文档](https://docs.fabricmc.net/1.20.4/) — 最权威的起点
- [Fabric Wiki](https://wiki.fabricmc.net/) — 社区维护的教程集合
- [Fabric 模板生成器](https://fabricmc.net/develop/template/) — 快速创建项目
- [Fabric API 版本查询](https://fabricmc.net/develop/) — 查询各版本对应 API

### 12.2 视频教程

- **Kaupenjoe YouTube 教程**（英文）：最系统的 Fabric 教程，有 GitHub 示例代码仓库，覆盖 1.20.x。搜索 "Kaupenjoe Fabric 1.20"。
- **Flandre芙兰 B 站教程**（中文）：35 集 Fabric 1.20.1 系列，从环境配置到维度的完整流程。B 站空间：[space.bilibili.com/4550069](https://space.bilibili.com/4550069/)。

### 12.3 GitHub 示例

- [fabric-example-mod](https://github.com/FabricMC/fabric-example-mod) — 官方示例
- [Kaupenjoe Fabric 教程代码](https://github.com/Tutorials-By-Kaupenjoe) — 查看其 GitHub 组织下的 1.20.x 相关仓库

### 12.4 社区

- Fabric Discord：含 `#zh_modding` 中文频道
- MC百科：[mcmod.cn](https://www.mcmod.cn/) — 模组信息查询
- MineBBS：[minebbs.com](https://www.minebbs.com/) — 中文论坛

### 12.5 五阶段学习路线

| 阶段 | 内容 | 预计时间 |
|------|------|---------|
| 1 | Java 基础（面向对象、泛型、注解） | 2-4 周 |
| 2 | 搭建环境 + 第一个物品/方块 | 1-2 周 |
| 3 | BlockEntity + 事件 + 配方 | 2-3 周 |
| 4 | 实体 + 世界生成 + 数据生成 | 3-4 周 |
| 5 | 网络 + GUI + 打包发布 | 2-3 周 |

---

## 关键数据速览

| 维度 | 数据 |
|------|------|
| Fabric Loader 推荐版本 | 0.15.11+ |
| Fabric API 推荐版本 | 0.92.0+1.20.4 |
| JDK 版本 | JDK 17 LTS |
| 映射方案推荐 | Yarn（Fabric 原生）或 Parchment（可读性最佳）|
| Entrypoints 类型 | main / client / server / fabric-datagen |
| 核心 Provider 数 | 6+（模型/配方/战利品表/语言/标签/动态注册）|
| 常用事件数 | 10+ 个 |
| 主要发布平台 | 3 个（CurseForge/Modrinth/GitHub）|
| 中文视频教程 | Flandre芙兰 35 集 |

---

## 信息来源

### A 级来源
- [Fabric 1.20.4 官方文档](https://docs.fabricmc.net/1.20.4/)
- [Fabric Wiki](https://wiki.fabricmc.net/)
- [Fabric 模板生成器](https://fabricmc.net/develop/template/)
- [Fabric API 版本查询](https://fabricmc.net/develop/)
- [Minecraft Wiki](https://zh.minecraft.wiki/)

### B 级来源
- [Kaupenjoe 教程](https://github.com/Tutorials-By-Kaupenjoe)
- [fabric-example-mod](https://github.com/FabricMC/fabric-example-mod)
- [Adoptium JDK](https://adoptium.net/)

### C 级来源
- [Flandre芙兰 B 站教程](https://space.bilibili.com/4550069/)
- [MC百科](https://www.mcmod.cn/)
- [MineBBS](https://www.minebbs.com/)
