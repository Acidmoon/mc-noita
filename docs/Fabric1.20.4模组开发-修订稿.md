# Fabric 1.20.4 模组开发实战指南

> **搜集时间**：2026-05-29 | **状态**：修订稿（三线审查已通过）
> **覆盖版本**：Minecraft 1.20.4 + Fabric Loader 0.15.x + Fabric API 0.92.0+ + JDK 17

---

Fabric 是 Minecraft 模组开发中最轻量、更新最快的加载器。本文聚焦 **1.20.4** 这一特定版本，从环境搭建到打包发布，覆盖 Fabric 模组开发的完整技术栈。所有代码示例均针对 Fabric 1.20.4 的 API 编写（已标注与 1.21+ 的差异），适配 Yarn 映射。

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

推荐 IntelliJ IDEA Community Edition。安装 **Minecraft Development 插件**（Marketplace 搜索 "Minecraft Development"，插件 ID：`com.demonwav.minecraft-dev`），提供 Fabric 项目自动生成、Mixin 语法高亮、运行配置自动创建等功能。

### 1.3 项目创建方式

**方式一（推荐）**：访问 [Fabric 官方模板生成器](https://fabricmc.net/develop/template/)，填写 Mod Name / Mod ID / Package Name，选择 Minecraft Version `1.20.4`，映射可选 Yarn 或 Mojang，点击 Generate 下载 ZIP，解压后用 IDEA 打开。

**方式二**：克隆 `git clone https://github.com/FabricMC/fabric-example-mod.git`，修改 `gradle.properties` 适配 1.20.4。

**方式三**：安装 MCDev 插件后，`File → New → Project → Minecraft` 按向导创建。

### 1.4 Fabric Loom Gradle 配置

**关键依赖版本（1.20.4）：**

| 组件 | 最低版本 | 推荐版本 |
|------|---------|---------|
| Fabric Loader | 0.14.21 | 0.15.11+ |
| Fabric Loom | 1.5+ | 1.6-SNAPSHOT |
| Fabric API | 0.88.0+ | 0.92.0+1.20.4 |

**gradle.properties：**
```properties
org.gradle.jvmargs=-Xmx2G
minecraft_version=1.20.4
loader_version=0.15.11
yarn_mappings=1.20.4+build.3
fabric_version=0.92.0+1.20.4
mod_version=1.0.0
maven_group=com.example
archives_base_name=my-mod
```

**build.gradle：**
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

方案一（settings.gradle 添加 HanBing 镜像）、方案二（build.gradle 添加阿里云 Maven）、方案三（gradle.properties 配置代理）——在初稿中有完整配置。

### 1.6 Gradle Wrapper

项目根目录包含 `gradlew` / `gradlew.bat`，这就是 **Gradle Wrapper**——自动下载项目所需的 Gradle 版本，确保所有开发者使用一致版本。始终用 `./gradlew` 而非系统 `gradle`。

### 1.7 关键 Gradle 任务

| 任务 | 命令 | 用途 |
|------|------|------|
| genSources | `./gradlew genSources` | 下载并反编译 Minecraft 源码 |
| runClient | `./gradlew runClient` | 启动测试客户端 |
| runServer | `./gradlew runServer` | 启动测试服务器 |
| build | `./gradlew build` | 编译并打包 JAR |
| runDatagen | `./gradlew runDatagen` | 运行数据生成器 |

---

## 2. 项目结构与配置

### 2.1 标准项目目录

```
my-mod/
├── src/main/
│   ├── java/com/example/mymod/
│   │   ├── MyMod.java                  # 主入口（ModInitializer）
│   │   ├── MyModClient.java            # 客户端入口
│   │   ├── ModItems.java / ModBlocks.java
│   │   ├── ModBlockEntities.java / ModEntities.java
│   │   └── data/ModDataGenerator.java  # 数据生成入口
│   └── resources/
│       ├── fabric.mod.json
│       ├── assets/mymod/{textures,models,lang}/
│       └── mixin.mymod.json
├── build.gradle / gradle.properties / settings.gradle
```

### 2.2 fabric.mod.json 核心字段

`schemaVersion: 1`、`id`（全小写唯一标识）、`version`（语义化版本）、`environment`（`*`/`client`/`server`）、`entrypoints`（main/client/server/fabric-datagen）、`depends`（版本约束）、`mixins`。

### 2.3 Entrypoints 体系

`main` → `ModInitializer`（双端执行，注册物品/方块/事件）。`client` → `ClientModInitializer`（仅客户端，密钥绑定/渲染注册）。`server` → `DedicatedServerModInitializer`（仅专用服务器）。`fabric-datagen` → `DataGeneratorEntrypoint`（仅在 runDatagen 时加载）。

### 2.4 依赖版本语法

`>=1.0.0`（大于等于）、`~1.20.4`（近似兼容）、`*`（任意版本）、`1.0.0`（精确版本）。

### 2.5 映射（Mapping）原理

Minecraft 的源码被 Mojang 混淆（类名变成 `a`、`b`、`func_12345_a`）。映射就是"翻译表"，把混淆名还原为可读名称。Fabric 用 **Intermediary** 作为中间层——模组基于 Intermediary 编译，运行时再转换为当前版本的混淆名，这也是 Fabric 能快速适配新版本的技术基础。

| 方案 | 维护方 | 许可证 | 特点 |
|------|--------|--------|------|
| **Yarn** | Fabric 社区 | CC0 | 开源，Fabric 原生 |
| **Mojang** | Mojang | 专有 | 官方提供 |
| **Parchment** | ParchmentMC | MIT | 在 Mojang 基础上增加参数名 |

---

## 3. 核心内容开发

### 3.1 自定义物品

```java
public static final Item MY_ITEM = new Item(new Item.Settings()
    .maxCount(64).rarity(Rarity.COMMON));

public static final Item SPECIAL_ITEM = new Item(new Item.Settings()
    .maxCount(16).rarity(Rarity.RARE).fireproof()) {
    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (!context.getWorld().isClient) {
            context.getPlayer().sendMessage(Text.literal("右键触发！"), false);
        }
        return ActionResult.SUCCESS;
    }
};

// 注册
Registry.register(Registries.ITEM, new Identifier("mymod", "my_item"), MY_ITEM);
```

**Item.Settings 方法：** `.maxCount()`（堆叠）、`.maxDamage()`（耐久）、`.rarity()`（稀有度）、`.fireproof()`（防火）、`.food()`（食物）。

### 3.2 自定义方块

```java
public static final Block MY_BLOCK = new Block(AbstractBlock.Settings.copy(Blocks.STONE)
    .strength(3.0f, 10.0f).requiresTool()
    .sounds(BlockSoundGroup.STONE).luminance(state -> 15));
Registry.register(Registries.BLOCK, new Identifier("mymod", "my_block"), MY_BLOCK);
Registry.register(Registries.ITEM, new Identifier("mymod", "my_block"),
    new BlockItem(MY_BLOCK, new Item.Settings()));
```

### 3.3 状态属性

```java
public class MyDirectionalBlock extends Block {
    public static final DirectionProperty FACING = DirectionProperty.of("facing",
        Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static final BooleanProperty LIT = BooleanProperty.of("lit");

    public MyDirectionalBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState()
            .with(FACING, Direction.NORTH).with(LIT, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}
```

### 3.4 实体方块（BlockEntity）

```java
// 1. BlockEntity 类
public class MyBlockEntity extends BlockEntity {
    private int counter = 0;
    public MyBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MY_BLOCK_ENTITY, pos, state);
    }
    @Override public void writeNbt(NbtCompound nbt) { nbt.putInt("counter", counter); super.writeNbt(nbt); }
    @Override public void readNbt(NbtCompound nbt) { super.readNbt(nbt); counter = nbt.getInt("counter"); }
}

// 2. 注册 BlockEntityType
public static final BlockEntityType<MyBlockEntity> MY_BLOCK_ENTITY = Registry.register(
    Registries.BLOCK_ENTITY_TYPE, new Identifier("mymod", "my_block_entity"),
    FabricBlockEntityTypeBuilder.create(MyBlockEntity::new, ModBlocks.MY_BLOCK).build());

// 3. 实现 EntityBlock 接口的方块
public class MyEntityBlock extends Block implements EntityBlock {
    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new MyBlockEntity(pos, state);
    }
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world.isClient ? null : (w, p, s, be) -> ((MyBlockEntity) be).tick();
    }
}
```

### 3.5 自定义物品栏分组

```java
// 新建分组（必须设置 id，build() 内部会调用 Registry.register）
public static final ItemGroup MY_GROUP = FabricItemGroup.builder()
    .id(new Identifier("mymod", "general"))
    .icon(() -> new ItemStack(ModItems.MY_ITEM))
    .displayName(Text.literal("My Mod"))
    .entries(((context, entries) -> {
        entries.add(ModItems.MY_ITEM);
        entries.add(ModBlocks.MY_BLOCK);
    }))
    .build();

// 添加到原版分组
ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS).register(entries -> {
    entries.add(ModItems.MY_ITEM);
});
```

### 3.6 / 3.7 工具/护甲/食物

工具：实现 `ToolMaterial` 接口，使用 `SwordItem` / `PickaxeItem` 等。护甲：实现 `ArmorMaterial` 接口（8 个方法）。食物：`new FoodComponent.Builder().hunger(6).saturationModifier(0.6f).build()`。

---

## 4. 数据生成

### 4.1 启用与配置

```gradle
// build.gradle
fabricApi { configureDataGeneration() }
loom {
    runs {
        datagen {
            inherit server
            vmArg "-Dfabric-api.datagen"
            vmArg "-Dfabric-api.datagen.output-dir=${file("src/main/generated")}"
            vmArg "-Dfabric-api.datagen.modid=mymod"
        }
    }
}
sourceSets { main { resources { srcDirs += ['src/main/generated'] } } }
```

**重要：** 数据生成的输出目录必须在 `sourceSets` 中注册，否则生成的 JSON 不会被打包进 JAR。

```java
public class ModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModBlockLootTableProvider::new);
        pack.addProvider(ModEnglishLangProvider::new);
    }
}
```

### 4.2 / 4.3 配方生成

```java
// Fabric 1.20.4 使用 generateRecipes(Consumer<RecipeJsonProvider>)
public class ModRecipeProvider extends FabricRecipeProvider {
    @Override
    protected void generateRecipes(Consumer<RecipeJsonProvider> exporter) {
        ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.MY_ITEM, 1)
            .pattern("XXX").pattern(" Y ").pattern("ZZZ")
            .input('X', Items.IRON_INGOT).input('Y', Items.DIAMOND).input('Z', Items.STONE)
            .criterion("has_diamond", conditionsFromItem(Items.DIAMOND))
            .offerTo(exporter);
    }
}
```

> 1.20.4 的 `generateRecipes()` 接收 `Consumer<RecipeJsonProvider>`，与 1.21+ 的 `generate(RecipeExporter)` 签名不同。

### 4.4 语言文件生成

```java
// 1.20.4 构造函数只接受 FabricDataOutput，generateTranslations 仅单参数
public class ModEnglishLangProvider extends FabricLanguageProvider {
    public ModEnglishLangProvider(FabricDataOutput dataOutput) { super(dataOutput); }

    @Override
    public void generateTranslations(TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.MY_ITEM, "My Item");
        translationBuilder.add(ModBlocks.MY_BLOCK, "My Block");
    }
}
```

---

## 5. 事件系统与 Mixin

### 5.1 Fabric 事件系统

使用回调接口 + `EventFactory`，通过 `InteractionResult` 返回值实现链式处理。

常用事件：`AttackBlockCallback`（攻击方块）、`PlayerBlockBreakEvents.BEFORE/AFTER`（破坏方块）、`UseBlockCallback`（右键方块）、`UseEntityCallback`（右键实体）、`EntityJoinWorldCallback`（实体加入世界）、`ServerLifecycleEvents`（服务器生命周期）、`ServerTickEvents`（每 tick）。

### 5.2 Mixin 推荐实践

**优先级：** Fabric API 事件 → `@Inject` → `@ModifyExpressionValue` → `@WrapOperation`。避免 `@Overwrite` 和 `@Redirect`。

```java
@Mixin(MinecraftServer.class)
public class ExampleMixin {
    @Inject(method = "tick", at = @At("HEAD"))
    private void onServerTick(CallbackInfo info) { }
}
```

| Mixin 类型 | 风险 | 用途 |
|-----------|------|------|
| `@Accessor`/`@Invoker` | 极低 | 暴露私有字段/方法 |
| `@Inject` | 低 | 在方法指定位置注入 |
| `@ModifyExpressionValue` | 低 | 修改表达式返回值 |
| `@WrapOperation` | 中 | 包装操作，可链式 |
| `@Overwrite` | **极高** | **避免使用** |

### 5.3 Access Widener

`src/main/resources/mymod.accesswidener`：
```
accessWidener v2 named
accessible class net/minecraft/world/level/block/entity/BrewingStandBlockEntity
```

`build.gradle` 配置：`loom { accessWidenerPath = file("src/main/resources/mymod.accesswidener") }`

---

## 6. 自定义实体与 AI

```java
// 注册
public static final EntityType<MyEntity> MY_ENTITY = Registry.register(
    Registries.ENTITY_TYPE, new Identifier("mymod", "my_entity"),
    FabricEntityTypeBuilder.create(MobCategory.MONSTER, MyEntity::new)
        .dimensions(EntityDimensions.fixed(1.0f, 2.0f)).build());
FabricDefaultAttributeRegistry.register(MY_ENTITY, MyEntity.createMobAttributes());

// AI
goalSelector.add(1, new SwimGoal(this));
goalSelector.add(2, new MeleeAttackGoal(this, 1.0D, false));
targetSelector.add(1, new ActiveTargetGoal<>(this, PlayerEntity.class, true));

// 生成
SpawnRestriction.register(MY_ENTITY, SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, (type, world, reason, pos, random) -> true);
BiomeModifications.addSpawn(BiomeSelectors.foundInOverworld(), MobCategory.MONSTER, MY_ENTITY, 100, 1, 4);
```

---

## 7. 世界生成与维度

三层架构：`ConfiguredFeature`（生成什么）→ `PlacedFeature`（如何放置）→ `BiomeModification`（在哪生成）。

```java
// ConfiguredFeature
ConfiguredFeature<?, ?> MY_ORE = new ConfiguredFeature<>(Feature.ORE,
    new OreFeatureConfig(OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,
        ModBlocks.MY_ORE_BLOCK.getDefaultState(), 8));
// PlacedFeature
PlacedFeature MY_ORE_PLACED = new PlacedFeature(
    RegistryEntry.of(MY_ORE),
    List.of(CountPlacementModifier.of(20), SquarePlacementModifier.of(),
        HeightRangePlacementModifier.uniform(VerticalAnchor.aboveBottom(0), VerticalAnchor.absolute(64)),
        BiomePlacementModifier.of()));
// 注册
// 注意：BuiltinRegistries 的注册可在静态初始化器中完成，
// 这与标准 Registry 不同（标准物品/方块注册必须在 onInitialize 中）
Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, id("my_ore"), MY_ORE);
Registry.register(BuiltinRegistries.PLACED_FEATURE, id("my_ore"), MY_ORE_PLACED);
// BiomeModification
BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
    GenerationStep.Feature.UNDERGROUND_ORES,
    RegistryKey.of(RegistryKeys.PLACED_FEATURE, id("my_ore")));
```

**自定义维度：** Java 端 `RegistryKey` + `dimension_type` JSON + `dimension` JSON。传送门用 **Custom Portal API**（Kyrptonaught）。

---

## 8. 网络通信

**C2S（客户端→服务端）：** 客户端 `ClientPlayNetworking.send(ID, buf)` → 服务端 `ServerPlayNetworking.registerGlobalReceiver(ID, (server, player, handler, buf, sender) -> server.execute(() -> { ... }))`。

**S2C（服务端→客户端）：** 服务端 `ServerPlayNetworking.send(player, ID, buf)` → 客户端 `ClientPlayNetworking.registerGlobalReceiver(ID, (client, handler, buf, sender) -> client.execute(() -> { ... }))`。

**线程安全：** 网络回调可能在网络线程执行，访问游戏数据必须在 `server.execute()` / `client.execute()` 中。

---

## 9. 高级主题

### 9.1 Transfer API

Fabric 不内置能量系统，用 `Storage<T>` 泛型接口 + `Transaction` 原子操作 + `SingleVariantStorage<T>` 实现。

### 9.2 自定义 GUI

```java
// 注册（Yarn: Registries.SCREEN_HANDLER, Mojang: BuiltInRegistries.MENU）
public static final ScreenHandlerType<MyScreenHandler> MY_SCREEN_HANDLER = Registry.register(
    Registries.SCREEN_HANDLER, new Identifier("mymod", "my_screen"),
    new ScreenHandlerType<>((syncId, inv, buf) -> new MyScreenHandler(syncId, inv, buf.readBlockPos())));
```

### 9.3 BlockEntityRenderer

```java
// 1.20.4 使用 BlockEntityRendererFactories（BlockEntityRendererRegistry 已弃用）
BlockEntityRendererFactories.register(ModBlockEntities.MY_BLOCK_ENTITY, MyBlockEntityRenderer::new);
```

### 9.4 配置系统

Cloth Config API + Mod Menu。`@Config(name = "mymod")` 注解配置类。

---

## 10. 打包发布

`./gradlew build` → `build/libs/` 下三个 JAR：`my-mod-1.0.0.jar`（发布用 remapJar）、`-dev.jar`（开发用）、`-sources.jar`（源码）。

**为什么需要 remap？** 模组代码基于 Yarn（可读名称）编译，但 Minecraft 运行时使用混淆名。`remapJar` 将编译时的映射名转回运行时混淆名。直接使用开发版 JAR 会因类名不匹配而崩溃。

发布平台：CurseForge（最大）、Modrinth（开源）、GitHub Releases。CI/CD 用 GitHub Actions + mod-publish-plugin。

许可证推荐：MIT（最宽松，推荐大多数模组）。

---

## 11. 常见陷阱与最佳实践

- **Client/Server 分离**：客户端类不能出现在服务端代码中，用 `@Environment(EnvType.CLIENT)` 隔离
- **Mixin 冲突**：避免 `@Overwrite`，用完整方法描述符 `tick()V`，用 `@Inject`/`@WrapOperation` 代替 `@Redirect`
- **资源路径**：Mod ID 全小写，`assets/<modid>/textures/` / `models/` / `lang/`
- **注册时机**：所有 `Registry.register()` 必须在 `onInitialize()` 中完成
- **版本锁定**：`fabric.mod.json` 中 `depends` 声明明确版本约束

---

## 12. 学习资源

- **官方文档**：[docs.fabricmc.net/1.20.4/](https://docs.fabricmc.net/1.20.4/)
- **Fabric Wiki**：[wiki.fabricmc.net](https://wiki.fabricmc.net/)
- **Fabric 模板生成器**：[fabricmc.net/develop/template/](https://fabricmc.net/develop/template/)
- **Kaupenjoe 教程（英文）**：[github.com/Tutorials-By-Kaupenjoe](https://github.com/Tutorials-By-Kaupenjoe)
- **Flandre芙兰 B 站教程（中文）**：[space.bilibili.com/4550069](https://space.bilibili.com/4550069/)
- **fabric-example-mod**：[github.com/FabricMC/fabric-example-mod](https://github.com/FabricMC/fabric-example-mod)

---

## 关键数据速览

| 维度 | 数据 |
|------|------|
| Fabric Loader 推荐版本 | 0.15.11+ |
| Fabric API 推荐版本 | 0.92.0+1.20.4 |
| JDK 版本 | JDK 17 LTS |
| 映射推荐 | Yarn（Fabric 原生）或 Parchment |
| Entrypoints | main / client / server / fabric-datagen |
| 发布平台 | CurseForge / Modrinth / GitHub |

---

## 信息来源

**A 级：** [Fabric 1.20.4 官方文档](https://docs.fabricmc.net/1.20.4/) / [Fabric Wiki](https://wiki.fabricmc.net/) / [Minecraft Wiki](https://zh.minecraft.wiki/)
**B 级：** [Kaupenjoe 教程](https://github.com/Tutorials-By-Kaupenjoe) / [fabric-example-mod](https://github.com/FabricMC/fabric-example-mod)
**C 级：** [Flandre芙兰 B 站](https://space.bilibili.com/4550069/) / [MC百科](https://www.mcmod.cn/)
