# Fabric 1.20.4 模组开发 — 数据生成、事件系统与 Mixin、自定义实体与 AI

> 本文档针对 Fabric 1.20.4 (Minecraft 1.20.4, Fabric API 0.91.x+, Java 17+)
> 涵盖三大主题：Data Generation 数据生成、事件系统与 Mixin 实践、自定义实体与 AI 系统。

---

# 4. 数据生成（Data Generation）

## 4.1 DataGeneratorEntrypoint 接口

数据生成（Data Generation）是 Fabric API 提供的代码生成工具，能以编程方式自动生成方块状态 JSON、物品模型、配方、战利品表、语言文件等资源文件，避免手动编写大量 JSON。

### 4.1.1 启用数据生成

**`build.gradle` 配置：**

```gradle
fabricApi {
    configureDataGeneration()
}
```

这会自动添加数据生成的依赖和 Gradle 任务 `runDatagen`。

### 4.1.2 创建 DataGeneratorEntrypoint 类

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class ModDataGenerator implements DataGeneratorEntrypoint {

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        // 创建一个数据包（Pack），所有 Provider 都注册到该 Pack 上
        FabricDataGenerator.Pack pack = generator.createPack();

        // 添加各种 Provider（使用构造方法引用）
        pack.addProvider(ModModelProvider::new);
        pack.addProvider(ModRecipeProvider::new);
        pack.addProvider(ModBlockLootTableProvider::new);
        pack.addProvider(ModEnglishLangProvider::new);
        pack.addProvider(ModChineseLangProvider::new);
        pack.addProvider(ModItemTagProvider::new);
    }
}
```

### 4.1.3 注册到 `fabric.mod.json`

```json
{
  "entrypoints": {
    "fabric-datagen": [
      "com.example.mod.data.ModDataGenerator"
    ],
    "main": [
      "com.example.mod.Mod"
    ]
  }
}
```

### 4.1.4 运行数据生成

```bash
./gradlew runDatagen
```

生成的文件默认输出到 `src/main/generated/` 目录。在 `build.gradle` 中需要添加以下配置以使其被正确包含：

```gradle
loom {
    runs {
        datagen {
            inherit server
            name "Data Generation"
            vmArg "-Dfabric-api.datagen"
            vmArg "-Dfabric-api.datagen.output-dir=${file("src/main/generated")}"
            vmArg "-Dfabric-api.datagen.modid=examplemod"

            runDir "build/datagen"
        }
    }
}

sourceSets {
    main {
        resources {
            srcDirs += [
                    'src/main/generated'
            ]
        }
    }
}
```

---

## 4.2 Provider 体系

Fabric API 提供了多种内置 Provider 基类，覆盖了模组开发中常见的数据生成需求：

| Provider 类 | 用途 |
|---|---|
| `FabricModelProvider` | 方块状态 JSON 和物品模型 |
| `FabricRecipeProvider` | 合成、熔炼等配方 |
| `FabricBlockLootTableProvider` | 方块掉落战利品表 |
| `FabricLootTableProvider` | 广义战利品表（方块/实体/箱子） |
| `FabricLanguageProvider` | 语言文件（本地化翻译） |
| `FabricTagProvider<T>` | 标签生成（ItemTag, BlockTag 等） |
| `FabricAdvancementProvider` | 进度生成 |
| `FabricDynamicRegistryProvider` | 动态注册表（世界生成数据等） |

### 4.2.1 Provider 注册方式

所有 Provider 通过 `pack.addProvider(ProviderConstructor::new)` 注册。构造方法引用确保 Fabric 延迟实例化 Provider 并正确管理依赖顺序。

---

## 4.3 模型生成

### 4.3.1 FabricModelProvider 基础

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricModelProvider;
import net.minecraft.data.client.*;
import net.minecraft.util.Identifier;

public class ModModelProvider extends FabricModelProvider {

    public ModModelProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockStateModelGenerator gen) {
        // 方块状态模型生成
    }

    @Override
    public void generateItemModels(ItemModelGenerator gen) {
        // 物品模型生成
    }
}
```

### 4.3.2 方块状态 JSON 生成

**最简方块 (立方体六面同材质)：**

```java
gen.registerSimpleCubeAll(ModBlocks.SIMPLE_BLOCK);
```
- 需要纹理：`assets/examplemod/textures/block/simple_block.png`
- 生成：方块状态 JSON + `block/simple_block.json` (cube_all 模型)

**不同面纹理的方块（朝北放置）：**

```java
Identifier side = Identifier.of("examplemod", "block/machine_side");
Identifier end = Identifier.of("examplemod", "block/machine_top");
gen.registerNorthDefaultHorizontalRotatable(ModBlocks.MACHINE_BLOCK, side, end);
```

**原木类方块（轴旋转）：**

```java
gen.registerAxisRotated(ModBlocks.LOG_BLOCK,
    Identifier.of("minecraft", "block/oak_log_top"),     // 端面纹理
    Identifier.of("examplemod", "block/log_side"));      // 侧面纹理
```

**自定义方块状态（使用 Multipart）：**

```java
gen.blockStateCollector.accept(MultipartBlockStateSupplier.create(ModBlocks.FENCE_BLOCK)
    .with(When.create().set(Properties.NORTH, true),
        BlockStateVariant.create()
            .put(VariantSettings.MODEL, Identifier.of("mineblock", "block/fence_post")))
    .with(When.create().set(Properties.EAST, true),
        BlockStateVariant.create()
            .put(VariantSettings.MODEL, Identifier.of("mineblock", "block/fence_side"))
            .put(VariantSettings.Y, VariantSettings.Rotation.R90))
);
```

**父级物品模型自动生成：**

注册 `BlockItem` 后，数据生成器默认会为其生成一个父级方块模型的物品模型。如需覆盖：

```java
gen.registerParentedItemModel(ModBlocks.SIMPLE_BLOCK.asItem(),
    Identifier.of("examplemod", "block/simple_block"));
```

### 4.3.3 物品模型生成

```java
@Override
public void generateItemModels(ItemModelGenerator gen) {
    // 普通物品（使用 item/generated）
    gen.register(ModItems.RUBY, Models.GENERATED);

    // 手持工具（使用 item/handheld）
    gen.register(ModItems.RUBY_SWORD, Models.HANDHELD);

    // 自定义纹理路径
    Identifier modelId = Models.GENERATED.upload(
        ModItems.CUSTOM_ITEM,
        TextureMap.layer0(Identifier.of("examplemod", "item/special_icon")),
        gen.modelCollector
    );
    gen.output.accept(ModItems.CUSTOM_ITEM, ItemModels.basic(modelId));

    // 覆盖 BlockItem 的默认方块模型引用
    gen.register(ModBlocks.SIMPLE_BLOCK.asItem(), Models.GENERATED);
}
```

### 4.3.4 关键 API 说明（1.20.4）

- 1.20.4 使用 `BlockStateModelGenerator` 和 `ItemModelGenerator`（1.21+ 中改为 `BlockModelGenerators` / `ItemModelGenerators`）
- 构造方法仅需 `FabricDataOutput output`，没有 `CompletableFuture` 参数
- 严格模式默认启用：如果模组中有方块缺少方块状态模型，数据生成会抛出异常

---

## 4.4 配方生成

### 4.4.1 FabricRecipeProvider 基础

在 Fabric 1.20.4 中，`FabricRecipeProvider` 的 API 使用 `generateRecipes(Consumer<RecipeJsonProvider>)` 方法。构造方法需要 `FabricDataOutput` 和 `CompletableFuture<RegistryWrapper.WrapperLookup>`。

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.minecraft.data.server.recipe.RecipeJsonProvider;
import net.minecraft.data.server.recipe.ShapedRecipeJsonBuilder;
import net.minecraft.data.server.recipe.ShapelessRecipeJsonBuilder;
import net.minecraft.data.server.recipe.CookingRecipeJsonBuilder;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.RecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class ModRecipeProvider extends FabricRecipeProvider {

    public ModRecipeProvider(FabricDataOutput output,
                             CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void generateRecipes(Consumer<RecipeJsonProvider> exporter) {
        // 所有配方在此方法中生成
    }
}
```

### 4.4.2 有序合成（ShapedRecipeJsonBuilder）

```java
// 合成台配方：用 4 个橡木原木合成 4 个橡木木板
ShapedRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, Items.OAK_PLANKS, 4)
    .pattern("ll")
    .pattern("ll")
    .define('l', Items.OAK_LOG)
    .group("planks")
    .criterion(FabricRecipeProvider.hasItem(Items.OAK_LOG),
        FabricRecipeProvider.conditionsFromItem(Items.OAK_LOG))
    .offerTo(exporter);
```

**使用标签作为原料：**

```java
ShapedRecipeJsonBuilder.create(RecipeCategory.MISC, ModItems.GOLDEN_APPLE_BLOCK)
    .pattern("GGG")
    .pattern("GAG")
    .pattern("GGG")
    .define('G', Items.GOLD_INGOT)
    .define('A', Items.APPLE)
    .criterion(FabricRecipeProvider.hasItem(Items.APPLE),
        FabricRecipeProvider.conditionsFromItem(Items.APPLE))
    .offerTo(exporter, Identifier.of("examplemod", "golden_apple_block_craft"));
```

### 4.4.3 无序合成（ShapelessRecipeJsonBuilder）

```java
// 无序合成：泥土 + 水桶 → 泥巴
ShapelessRecipeJsonBuilder.create(RecipeCategory.BUILDING_BLOCKS, Items.MUD)
    .requires(Items.DIRT)
    .requires(Items.WATER_BUCKET)
    .criterion(FabricRecipeProvider.hasItem(Items.DIRT),
        FabricRecipeProvider.conditionsFromItem(Items.DIRT))
    .offerTo(exporter, Identifier.of("examplemod", "mud_from_dirt"));
```

### 4.4.4 熔炉/高炉配方

**熔炉冶炼（多输入 → 单输出）：**

```java
FabricRecipeProvider.offerSmelting(
    exporter,
    List.of(ModItems.RAW_ORE, ModBlocks.ORE_BLOCK),  // 输入
    ModItems.INGOT,                                    // 输出
    0.7f,                                              // 经验值
    200,                                               // 烹饪时间（ticks）
    "examplemod_ingot"                                 // 分组
);

// 高炉冶炼（速度翻倍）
FabricRecipeProvider.offerBlasting(
    exporter,
    List.of(ModItems.RAW_ORE, ModBlocks.ORE_BLOCK),
    ModItems.INGOT,
    0.7f,
    100,   // 100 ticks（高炉）
    "examplemod_ingot"
);
```

**使用 CookingRecipeJsonBuilder 单独控制：**

```java
// 熔炉：粘土 → 砖块
CookingRecipeJsonBuilder.createSmelting(
        Ingredient.ofItems(Items.CLAY),
        RecipeCategory.BUILDING_BLOCKS,
        Items.BRICK,
        0.3f,
        200
)
.criterion(FabricRecipeProvider.hasItem(Items.CLAY),
    FabricRecipeProvider.conditionsFromItem(Items.CLAY))
.offerTo(exporter, Identifier.of("examplemod", "brick_from_clay"));

// 烟熏炉（食物专用）
CookingRecipeJsonBuilder.createSmoking(
        Ingredient.ofItems(Items.BEEF),
        RecipeCategory.FOOD,
        Items.COOKED_BEEF,
        0.35f,
        100
)
.criterion(FabricRecipeProvider.hasItem(Items.BEEF),
    FabricRecipeProvider.conditionsFromItem(Items.BEEF))
.offerTo(exporter, Identifier.of("examplemod", "cooked_beef_smoking"));

// 营火烹饪（慢速，不消耗燃料）
CookingRecipeJsonBuilder.createCampfireCooking(
        Ingredient.ofItems(Items.SALMON),
        RecipeCategory.FOOD,
        Items.COOKED_SALMON,
        0.35f,
        600   // 600 ticks（营火较慢）
)
.criterion(FabricRecipeProvider.hasItem(Items.SALMON),
    FabricRecipeProvider.conditionsFromItem(Items.SALMON))
.offerTo(exporter, Identifier.of("examplemod", "cooked_salmon_campfire"));
```

### 4.4.5 配方解锁条件

使用 `criterion()` 设置解锁条件，常用方法：

| 方法 | 用途 |
|---|---|
| `FabricRecipeProvider.conditionsFromItem(Items.X)` | 获得某物品时解锁 |
| `FabricRecipeProvider.conditionsFromTag(TagKey)` | 获得某标签物品时解锁 |
| `FabricRecipeProvider.hasItem(Items.X)` | 获取物品的字符串 ID |
| `FabricRecipeProvider.getHasName(Items.X)` | 获取物品的英文 ID 字符串 |

---

## 4.5 战利品表生成

### 4.5.1 FabricBlockLootTableProvider 基础

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricBlockLootTableProvider;
import net.minecraft.block.Blocks;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.loot.provider.number.ConstantLootNumberProvider;

import java.util.concurrent.CompletableFuture;

public class ModBlockLootTableProvider extends FabricBlockLootTableProvider {

    protected ModBlockLootTableProvider(FabricDataOutput dataOutput,
                                        CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generate() {
        // 方块战利品表在此生成
    }
}
```

### 4.5.2 常见掉落类型

```java
@Override
public void generate() {
    // 1. 掉落实体自身（最常见）
    addDrop(ModBlocks.SIMPLE_BLOCK);

    // 2. 什么也不掉落
    addDrop(ModBlocks.GLASS_BLOCK, dropsNothing());

    // 3. 掉落其他物品（如：石英矿石掉落石英）
    addDrop(ModBlocks.ORE_BLOCK, oreDrops(ModBlocks.ORE_BLOCK, ModItems.RAW_ORE));

    // 4. 精准采集 + 普通掉落
    addDropWithSilkTouch(ModBlocks.GLASS_BLOCK, Blocks.AIR);
    // 带精准采集时掉落自身，否则掉落替代物品
    addDropWithSilkTouch(ModBlocks.ORE_GLASS_BLOCK, oreDrops(ModBlocks.ORE_GLASS_BLOCK, ModItems.RAW_ORE));

    // 5. 剪刀（树叶、蜘蛛网等）
    addDrop(ModBlocks.WEB_BLOCK, shearsOrSilkTouchDrop(ModBlocks.WEB_BLOCK));

    // 6. 作物掉落
    addDrop(ModBlocks.CROP_BLOCK,
        cropDrops(ModBlocks.CROP_BLOCK, ModItems.SEEDS, ModItems.CROP, ConstantLootNumberProvider.create(3.0f)));
}
```

### 4.5.3 实体掉落战利品表

对于实体掉落，需要实现 `FabricLootTableProvider` 接口或手动构建 `LootTable`：

```java
// 手动构建实体战利品表（在某个 Provider 或主类中）
LootTable.Builder lootTable = LootTable.builder()
    .pool(LootPool.builder()
        .rolls(ConstantLootNumberProvider.create(1.0f))
        .with(ItemEntry.builder(ModItems.CUSTOM_DROP)
            .conditionally(RandomChanceLootCondition.builder(0.5f))  // 50% 概率
        )
    );

// 注册到实体（在 ModInitializer 中）
Registry.register(Registries.LOOT_TABLE,
    Identifier.of("examplemod", "entities/custom_entity"),
    lootTable.build());
```

---

## 4.6 语言文件生成

### 4.6.1 FabricLanguageProvider 基础

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModEnglishLangProvider extends FabricLanguageProvider {

    protected ModEnglishLangProvider(FabricDataOutput dataOutput,
                                     CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, "en_us", registryLookup);  // "en_us" 是默认语言，可省略
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup,
                                     TranslationBuilder translationBuilder) {
        // 在此添加翻译
    }
}
```

### 4.6.2 添加翻译条目

```java
@Override
public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup,
                                 TranslationBuilder translationBuilder) {
    // 物品翻译
    translationBuilder.add(ModItems.RUBY, "Ruby");
    translationBuilder.add(ModItems.RUBY_SWORD, "Ruby Sword");

    // 方块翻译
    translationBuilder.add(ModBlocks.SIMPLE_BLOCK, "Simple Block");
    translationBuilder.add(ModBlocks.ORE_BLOCK, "Ore Block");

    // 条目组（ItemGroup / Creative Tab）
    translationBuilder.add(ModItemGroups.MAIN_TITLE, "Example Mod");

    // 实体类型
    translationBuilder.add(ModEntities.CUSTOM_ENTITY, "Custom Entity");

    // 自定义翻译键
    translationBuilder.add("text.examplemod.greeting", "Hello there!");
    translationBuilder.add("text.examplemod.info", "Welcome, %s!");
}
```

### 4.6.3 中文语言文件生成

```java
public class ModChineseLangProvider extends FabricLanguageProvider {

    protected ModChineseLangProvider(FabricDataOutput dataOutput,
                                     CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, "zh_cn", registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup,
                                     TranslationBuilder translationBuilder) {
        translationBuilder.add(ModItems.RUBY, "红宝石");
        translationBuilder.add(ModItems.RUBY_SWORD, "红宝石剑");
        translationBuilder.add(ModBlocks.SIMPLE_BLOCK, "简单方块");
        translationBuilder.add(ModBlocks.ORE_BLOCK, "矿石方块");
        translationBuilder.add(ModItemGroups.MAIN_TITLE, "示例模组");
        translationBuilder.add(ModEntities.CUSTOM_ENTITY, "自定义实体");
        translationBuilder.add("text.examplemod.greeting", "你好！");
        translationBuilder.add("text.examplemod.info", "欢迎，%s！");
    }
}
```

更多语言只需创建新的 Provider 实例，使用对应语言代码（如 `zh_cn`、`ja_jp`、`ko_kr` 等）。

---

## 4.7 FabricDynamicRegistryProvider 与世界生成数据

### 4.7.1 概述

`FabricDynamicRegistryProvider` 用于生成动态注册表（Dynamic Registry）数据，主要包括世界生成相关数据：维度、配置特征（Configured Feature）、放置特征（Placed Feature）、生物群系（Biome）、维度类型（Dimension Type）等。

### 4.7.2 基础用法

```java
package com.example.mod.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryWrapper;

import java.util.concurrent.CompletableFuture;

public class ModWorldGenProvider extends FabricDynamicRegistryProvider {

    public ModWorldGenProvider(FabricDataOutput output,
                               CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
        // 将世界生成 JSON 数据从数据包中传递到输出目录
        // 注意：JSON 文件需要放置在:
        // src/main/resources/data/examplemod/worldgen/configured_feature/
        // src/main/resources/data/examplemod/worldgen/placed_feature/
        // 等位置

        // 通过 RegistryKey 添加
        entries.add(ModConfiguredFeatures.ORE_FEATURE_KEY);
        entries.add(ModPlacedFeatures.ORE_PLACED_KEY);
    }

    @Override
    public String getName() {
        return "World Gen";
    }
}
```

### 4.7.3 注册到 Entrypoint

```java
public class ModDataGenerator implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();
        pack.addProvider(ModWorldGenProvider::new);
    }
}
```

### 4.7.4 数据驱动流程

Fabric 1.20.4 的世界生成采用数据驱动方式：

1. 在 `src/main/resources/data/examplemod/worldgen/` 下创建 JSON 文件
2. 在代码中定义 `RegistryKey` 引用这些 JSON
3. `FabricDynamicRegistryProvider` 在数据生成时将这些 JSON 复制到输出目录
4. 游戏加载时自动注册到动态注册表

典型目录结构：
```
src/main/resources/data/examplemod/worldgen/
├── configured_feature/
│   └── example_ore.json
├── placed_feature/
│   └── example_ore_placed.json
├── biome/
│   └── example_biome.json
└── dimension/
    └── example_dimension.json
```

---

# 5. 事件系统与 Mixin

## 5.1 Fabric 事件系统架构

### 5.1.1 核心概念

Fabric 事件系统允许模组在特定游戏行为发生时获得通知并作出响应，无需使用 Mixin 修改底层代码。核心类：

- `net.fabricmc.fabric.api.event.Event` — 事件实例，持有所有已注册的回调
- `net.fabricmc.fabric.api.event.EventFactory` — 事件工厂，用于创建 Event 实例
- Callback Interface — 回调接口，定义事件触发时调用的方法签名

### 5.1.2 EventFactory.createArrayBacked()

这是 Fabric 创建事件的标准方式。它使用数组存储所有已注册的监听器，当事件触发时依次调用。

**基本重载：**

```java
public static <T> Event<T> createArrayBacked(Class<T> type,
                                              T emptyInvoker,
                                              Function<T[], T> invokerFactory)
```

- `type` — 回调接口的 Class 对象
- `emptyInvoker` — 无监听器时的默认行为（避免空数组迭代的性能开销）
- `invokerFactory` — 将监听器数组合并为单个调用者的工厂方法

### 5.1.3 InteractionResult 链式处理

Fabric 事件通常返回 `net.minecraft.util.ActionResult` 或 `net.minecraft.util.TypedActionResult<T>` 来实现链式处理：

| 返回值 | 含义 | 行为 |
|---|---|---|
| `PASS` | 传递 | 继续调用下一个监听器 |
| `SUCCESS` | 成功 | 停止调用，表示操作成功 |
| `FAIL` | 失败 | 停止调用，表示操作被拒绝/取消 |
| `CONSUME` | 消费 | 停止调用，表示已处理 |

**典型的事件回调循环模式：**

```java
EventFactory.createArrayBacked(
    MyCallback.class,
    (listeners) -> (arg) -> {
        for (MyCallback listener : listeners) {
            ActionResult result = listener.method(arg);
            if (result != ActionResult.PASS) {
                return result;
            }
        }
        return ActionResult.PASS;
    }
);
```

### 5.1.4 自定义事件示例

**步骤 1：定义回调接口 + 事件实例**

```java
package com.example.mod.event;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.entity.passive.SheepEntity;

public interface ShearedCallback {
    Event<ShearedCallback> EVENT = EventFactory.createArrayBacked(
        ShearedCallback.class,
        (listeners) -> (player, sheep) -> {
            for (ShearedCallback listener : listeners) {
                ActionResult result = listener.interact(player, sheep);
                if (result != ActionResult.PASS) {
                    return result;
                }
            }
            return ActionResult.PASS;
        }
    );

    ActionResult interact(PlayerEntity player, SheepEntity sheep);
}
```

**步骤 2：使用 Mixin 触发事件**

```java
@Mixin(SheepEntity.class)
public class SheepShearMixin {

    @Inject(
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/passive/SheepEntity;dropItems()V"),
        method = "interactMob",
        cancellable = true
    )
    private void onShear(PlayerEntity player, Hand hand, CallbackInfoReturnable<Boolean> cir) {
        ActionResult result = ShearedCallback.EVENT.invoker()
            .interact(player, (SheepEntity) (Object) this);

        if (result == ActionResult.FAIL) {
            cir.cancel();
        }
    }
}
```

**步骤 3：注册监听器**

```java
public class Mod implements ModInitializer {
    @Override
    public void onInitialize() {
        ShearedCallback.EVENT.register((player, sheep) -> {
            player.getWorld().spawnEntity(
                new ItemEntity(player.getWorld(), sheep.getX(), sheep.getY(), sheep.getZ(),
                    new ItemStack(Items.DIAMOND))
            );
            return ActionResult.FAIL;  // 取消原生剪羊毛行为
        });
    }
}
```

### 5.1.5 带阶段的事件（createWithPhases）

需要按特定顺序执行监听器时，可使用分阶段事件：

```java
Event<MyCallback> EVENT = EventFactory.createWithPhases(
    MyCallback.class,
    listeners -> (arg) -> {
        for (MyCallback listener : listeners) {
            listener.method(arg);
        }
    },
    Event.DEFAULT_PHASE,                    // 默认阶段
    Identifier.of("mymod", "early_phase"),   // 自定义阶段
    Identifier.of("mymod", "late_phase")
);

// 注册到特定阶段
EVENT.register(Identifier.of("mymod", "early_phase"), () -> {
    // 在早期阶段执行
});
```

---

## 5.2 常用事件索引

### 5.2.1 交互事件

```java
// 攻击方块回调
AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) -> {
    // 玩家攻击方块时调用
    // 返回 ActionResult.FAIL 以阻止攻击
    return ActionResult.PASS;
});

// 使用方块回调（右键点击方块）
UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
    // 玩家右键点击方块时调用
    return ActionResult.PASS;
});

// 使用实体回调（右键点击实体）
UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    // 玩家右键点击实体时调用
    return ActionResult.PASS;
});
```

### 5.2.2 方块破坏事件

```java
PlayerBlockBreakEvents.BEFORE.register((world, player, pos, state, blockEntity) -> {
    // 方块被破坏前调用
    // 返回 false 以阻止破坏
    return true;
});

PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
    // 方块被破坏后调用
});

PlayerBlockBreakEvents.CANCELLED.register((world, player, pos, state, blockEntity) -> {
    // 方块破坏被取消时调用
});
```

### 5.2.3 实体世界事件

```java
// 实体加入世界
EntityJoinWorldCallback.EVENT.register((entity, world) -> {
    // 实体被添加到世界时调用
    // 返回 false 阻止实体加入
    return true;
});

// 右键实体回调
UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
    // 玩家右键点击实体时调用
    return ActionResult.PASS;
});
```

### 5.2.4 服务器生命周期事件

```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

// 服务器启动时
ServerLifecycleEvents.SERVER_STARTING.register(server -> {
    // 服务器即将启动，此时尚未准备好接受连接
    Mod.LOGGER.info("Server is starting...");
});

// 服务器启动完成
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    // 服务器已完全启动，所有服务已就绪
    Mod.LOGGER.info("Server started!");
});

// 服务器正在停止
ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
    // 服务器开始停止，应保存数据
    Mod.LOGGER.info("Server stopping...");
});

// 服务器已停止
ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
    // 服务器已完全停止
    Mod.LOGGER.info("Server stopped.");
});

// 同步数据加载完成（标签、战利品表等）
ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register((player, joined) -> {
    // 当数据包内容同步给玩家时调用
});
```

### 5.2.5 Tick 事件

```java
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

// 服务器 Tick 开始
ServerTickEvents.START_SERVER_TICK.register(server -> {
    // 每个游戏 tick 开始时调用
});

// 服务器 Tick 结束
ServerTickEvents.END_SERVER_TICK.register(server -> {
    // 每个游戏 tick 结束时调用
});

// 客户端 Tick
ClientTickEvents.END_CLIENT_TICK.register(client -> {
    // 客户端每个 tick 结束时调用
});
```

### 5.2.6 其他常用事件

```java
// 命令注册
CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
    dispatcher.register(CommandManager.literal("ping").executes(context -> {
        context.getSource().sendFeedback(Text.literal("Pong!"), false);
        return 1;
    }));
});

// 战利品表修改
LootTableEvents.MODIFY.register((resourceManager, lootManager, id, tableBuilder, source) -> {
    if (source.isBuiltin() && id.equals(Blocks.GRASS.getLootTableKey())) {
        LootPool.Builder poolBuilder = LootPool.builder()
            .with(ItemEntry.builder(ModItems.SPECIAL_DROP))
            .conditionally(RandomChanceLootCondition.builder(0.01f));
        tableBuilder.pool(poolBuilder);
    }
});
```

---

## 5.3 Mixin 推荐实践

### 5.3.1 @Inject（最常用）

`@Inject` 是最安全、最常用的 Mixin 注解，它在目标方法的指定位置注入自定义代码。

```java
@Mixin(Entity.class)
public class ExampleMixin {

    @Inject(at = @At("HEAD"), method = "tick", cancellable = true)
    private void onTick(CallbackInfo ci) {
        // 在 tick() 方法开始时执行
        // cancellable = true 允许取消原方法执行
    }

    @Inject(at = @At("TAIL"), method = "getName")
    private void onGetName(CallbackInfoReturnable<Text> cir) {
        // 在 getName() 方法返回后修改返回值
        Text original = cir.getReturnValue();
        cir.setReturnValue(Text.literal("Modified: ").append(original));
    }

    @Inject(
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;removeFromDimension()V"),
        method = "tick"
    )
    private void onRemoveFromDimension(CallbackInfo ci) {
        // 在 tick() 方法中调用 removeFromDimension() 之前注入
    }
}
```

### 5.3.2 @ModifyExpressionValue（安全推荐）

来自 MixinExtras 库，用于修改表达式的返回值，比 `@Redirect` 更安全，可以链式工作：

```java
@Mixin(SomeClass.class)
public class SomeMixin {

    @ModifyExpressionValue(
        method = "someMethod",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isOnFire()Z")
    )
    private boolean modifyIsOnFire(boolean original) {
        // 修改 isOnFire() 的返回值
        return original || this.customFlag;
    }
}
```

**优势：** 多个模组可以同时 `@ModifyExpressionValue` 同一个调用点，而 `@Redirect` 只能有一个生效。

### 5.3.3 @WrapOperation（推荐替代 @Redirect）

`@WrapOperation` 也是 MixinExtras 提供的注解，包装整个方法调用，允许你控制是否执行原调用：

```java
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @WrapOperation(
        method = "applyDamage",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/damage/DamageSource;getExhaustion(F)F")
    )
    private float modifyExhaustion(DamageSource source, float originalValue,
                                    Operation<Float> original) {
        // 可以修改传入值、跳过原调用或执行其他逻辑
        if (this.isCreative()) {
            return 0.0f;  // 创造模式下不消耗饥饿值
        }
        return original.call(source, originalValue);  // 正常执行
    }
}
```

### 5.3.4 @Accessor 和 @Invoker

**@Accessor — 访问私有字段：**

```java
@Mixin(Entity.class)
public interface EntityAccessor {
    @Accessor("velocityDirty")
    boolean examplemod_isVelocityDirty();

    @Accessor("velocityDirty")
    @Mutable  // 允许修改 final 字段
    void examplemod_setVelocityDirty(boolean dirty);
}
```

命名规范：方法名使用 `modid_` 前缀以避免与其他模组的 Accessor 冲突。

**@Invoker — 调用私有方法：**

```java
@Mixin(VillagerEntity.class)
public interface VillagerEntityInvoker {

    @Invoker("setLevel")
    void examplemod_setLevel(int level);

    @Invoker("<init>")
    static VillagerEntityInvoker newVillager(EntityType<VillagerEntity> type, World world) {
        // 调用私有构造方法
        throw new AssertionError();
    }
}
```

使用方式：
```java
// 访问字段
boolean dirty = ((EntityAccessor) entity).examplemod_isVelocityDirty();

// 调用私有方法
((VillagerEntityInvoker) villager).examplemod_setLevel(5);
```

---

## 5.4 Access Widener

### 5.4.1 概述

Access Widener（AW）是 Fabric 的另一种访问权限扩宽机制，通过配置文件直接修改类、方法、字段的可见性。与 Mixin Accessor 相比：

| 特性 | Mixin Accessor | Access Widener |
|---|---|---|
| 访问私有字段 | 是 | 是 |
| 调用私有方法 | 是 | 是 |
| 使 final 类可继承 | 否 | 是 |
| 访问私有内部类 | 否 | 是 |
| 多个模组兼容性 | 好（命名隔离） | 好 |
| 适用范围 | 仅限该模组 | 全局生效 |

### 5.4.2 文件格式 (v2)

创建 `src/main/resources/examplemod.accesswidener`：

```accesswidener
accessWidener v2 named

# 使字段可访问
accessible class net/minecraft/class_Entity field_velocityDirty Z

# 使方法可访问
accessible class net/minecraft/class_Entity method_isInsideWall ()Z

# 使类可继承（移除 final）
extendable class net/minecraft/class_ScreenHandler

# 移除字段的 final（可修改）
mutable class net/minecraft/class_Entity field_inChurn Z

# 同时需要 accessible + mutable 时需写两行
accessible class net/minecraft/class_Entity field_inChurn Z
mutable class net/minecraft/class_Entity field_inChurn Z
```

### 5.4.3 注册 AW

**`build.gradle`：**

```gradle
loom {
    accessWidenerPath = file("src/main/resources/examplemod.accesswidener")
}
```

**`fabric.mod.json`：**

```json
{
  "accessWidener": "examplemod.accesswidener"
}
```

### 5.4.4 v2 专属特性

- `transitive-accessible` — 扩宽的访问权限会传递给依赖此模组的下游模组
- `transitive-extendable` — 同理，使类的可继承性传递
- 适用于 API 模组暴露内部接口给下游使用

### 5.4.5 Accessor vs AW 选择决策树

1. **只需读/写字段或调用方法？** → Mixin `@Accessor` / `@Invoker`
2. **需要使 final 类可继承？** → Access Widener
3. **需要访问私有内部类？** → Access Widener
4. **需要调用私有构造方法？** → `@Invoker("<init>")`（Mixin）
5. **写 API 给其他模组用？** → 考虑 `transitive` Access Widener

---

## 5.5 @Overwrite 为什么应该避免

### 5.5.1 问题

```java
@Mixin(Entity.class)
public class EntityMixin {

    @Overwrite
    public void tick() {
        // 完全覆盖原方法的实现
        // !!! 这会导致与任何其他 @Overwrite 同一方法的模组冲突 !!!
    }
}
```

### 5.5.2 主要问题

1. **完全不兼容** — 两个模组同时 `@Overwrite` 同一方法时，只有最后一个加载的生效
2. **无法追踪变化** — 代码审查时难以判断这是覆盖还是注入
3. **维护灾难** — Minecraft 版本更新时原方法变化，覆盖代码容易过时
4. **没有 fallback** — 不像 `@Inject` 可以调用 `super`

### 5.5.3 替代方案

| 需求 | 推荐方案 |
|---|---|
| 在方法开头/结尾添加逻辑 | `@Inject(at = @At("HEAD"))` / `@Inject(at = @At("TAIL"))` |
| 修改方法返回值 | `@Inject` + `CallbackInfoReturnable` |
| 修改表达式值 | `@ModifyExpressionValue` (MixinExtras) |
| 包装方法调用 | `@WrapOperation` (MixinExtras) |
| 完全替换逻辑（极端情况） | 先考虑是否能通过组合现有逻辑实现 |

---

## 5.6 Mixin 调试

### 5.6.1 导出转换后的类

**全局导出（VM 参数）：**

```bash
# JVM 启动参数，导出所有被 Mixin 修改的类
-Dmixin.debug.export=true
```

在 IntelliJ 的 run configuration 的 VM options 中添加，或通过 Gradle：

```gradle
loom {
    runs {
        client {
            vmArg "-Dmixin.debug.export=true"
        }
    }
}
```

导出文件位于 `run/.mixin.out/` 目录，按类名组织。

**单类导出（代码级别）：**

```java
@Mixin(Entity.class)
@Debug(export = true)  // 仅导出这个 Mixin 类的转换结果
public class EntityMixin {
    // ...
}
```

### 5.6.2 详细 Mixin 日志

```bash
--mixin.verbose  # JVM 启动参数，输出 Mixin 的详细处理信息
```

这会输出每个 Mixin 何时应用、应用到哪些类、注入点等信息。

### 5.6.3 导出文件中 MixinExtras 的命名前缀

在导出的类中，MixinExtras 注解的方法会使用以下前缀标识：

| 注解 | 前缀 |
|---|---|
| `@ModifyExpressionValue` | `modifyExpressionValue$` |
| `@ModifyReceiver` | `modifyReceiver$` |
| `@ModifyReturnValue` | `modifyReturnValue$` |
| `@WrapWithCondition` | `wrapWithCondition$` |
| `@WrapOperation` | `wrapOperation$` |

---

## 5.7 MixinExtras 库

### 5.7.1 添加依赖

MixinExtras 不包含在 Minecraft 或 Fabric API 中，需要手动添加：

```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    implementation include("com.github.llamalad7:mixinextras-fabric:0.3.6")
}
```

### 5.7.2 @ModifyExpressionValue 详解

修改目标表达式的返回值，不改变原有调用结构：

```java
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @ModifyExpressionValue(
        method = "tickMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;isTouchingWater()Z")
    )
    private boolean alwaysSwim(boolean original) {
        // 让实体在水中移动得更快（总是返回 true）
        return true;
    }
}
```

### 5.7.3 @WrapOperation 详解

包装一个方法调用，可以控制是否执行、修改参数、修改返回值：

```java
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @WrapOperation(
        method = "tick",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/effect/StatusEffects;getPotionEffect(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/entity/effect/StatusEffect;)Lnet/minecraft/entity/effect/StatusEffectInstance;")
    )
    private StatusEffectInstance modifyPotionEffect(LivingEntity entity, StatusEffect effect,
                                                      Operation<StatusEffectInstance> original) {
        if (effect == StatusEffects.POISON) {
            return null;  // 免疫中毒效果
        }
        return original.call(entity, effect);
    }
}
```

### 5.7.4 @ModifyReturnValue

简化修改方法返回值的模式：

```java
@Mixin(PlayerEntity.class)
public class PlayerEntityMixin {

    @ModifyReturnValue(
        method = "getAttackCooldownProgressPerTick",
        at = @At("RETURN")
    )
    private float modifyAttackSpeed(float original) {
        return original * 2.0f;  // 攻击速度加倍
    }
}
```

### 5.7.5 @WrapWithCondition

条件性执行某段代码：

```java
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @WrapWithCondition(
        method = "tickMovement",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setVelocity(DDD)V")
    )
    private boolean cancelKnockback(LivingEntity instance, double x, double y, double z) {
        // 阻止击退效果
        return false;
    }
}
```

---

# 6. 自定义实体与 AI

## 6.1 EntityType 注册

### 6.1.1 使用 FabricEntityTypeBuilder

```java
package com.example.mod.entity;

import com.example.mod.Mod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModEntities {

    // 自定义生物实体（扩展 PathAwareEntity 或 MobEntity）
    public static final EntityType<ModZombieEntity> MOD_ZOMBIE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("examplemod", "mod_zombie"),
        FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ModZombieEntity::new)
            .dimensions(EntityDimensions.fixed(0.6f, 1.95f))  // 碰撞箱：宽 * 高
            .trackRangeBlocks(64)       // 追踪范围
            .trackedUpdateRate(3)       // 更新频率（tick）
            .forceTrackedVelocityUpdates(true)  // 强制速度同步
            .fireImmune()               // 免疫火焰
            .build()
    );

    // 被动生物（动物）
    public static final EntityType<ModAnimalEntity> MOD_ANIMAL = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("examplemod", "mod_animal"),
        FabricEntityTypeBuilder.create(SpawnGroup.CREATURE, ModAnimalEntity::new)
            .dimensions(EntityDimensions.fixed(0.8f, 0.8f))
            .trackRangeBlocks(10)
            .build()
    );

    // 弹射物实体（ProjectileEntity）
    public static final EntityType<ModProjectileEntity> MOD_PROJECTILE = Registry.register(
        Registries.ENTITY_TYPE,
        Identifier.of("examplemod", "mod_projectile"),
        FabricEntityTypeBuilder.<ModProjectileEntity>create(SpawnGroup.MISC, ModProjectileEntity::new)
            .dimensions(EntityDimensions.fixed(0.25f, 0.25f))
            .trackRangeBlocks(4)
            .trackedUpdateRate(20)
            .build()
    );
}
```

### 6.1.2 SpawnGroup 对照

| SpawnGroup | 用途 | 动物容量 |
|---|---|---|
| `CREATURE` | 被动动物（牛、羊等） | 10 |
| `MONSTER` | 敌对生物（僵尸、骷髅等） | 70 |
| `AMBIENT` | 环境生物（蝙蝠） | 15 |
| `AXOLOTLS` | 美西螈 | 30 |
| `UNDERGROUND_WATER_CREATURE` | 地下水生生物 | 5 |
| `WATER_CREATURE` | 水生生物（鱿鱼） | 5 |
| `WATER_AMBIENT` | 鱼类 | 20 |
| `MISC` | 杂项（弹射物、掉落物等） | -1（不限） |

### 6.1.3 FabricEntityTypeBuilder 主要配置项

| 方法 | 说明 |
|---|---|
| `.dimensions(EntityDimensions.fixed(w, h))` | 设置碰撞箱 |
| `.trackRangeBlocks(int)` | 追踪范围（默认 64） |
| `.trackedUpdateRate(int)` | 更新间隔（tick，默认 3） |
| `.forceTrackedVelocityUpdates(boolean)` | 是否强制同步速度 |
| `.fireImmune()` | 免疫火焰 |
| `.specificSpawnBlocks(Block...)` | 特定生成方块 |
| `.maxBlockTrackingRange(int)` | 最大方块追踪范围 |
| `.disableSaving()` | 禁止保存到 NBT |
| `.disableSummon()` | 禁止通过命令召唤 |
| `.spawnableFarFromPlayer()` | 允许在远离玩家处生成 |

---

## 6.2 AI Goal 系统

### 6.2.1 Goal 系统架构

Minecraft 的生物 AI 基于 `Goal` 系统，每个生物维护两个 `GoalSelector`：

- **`goalSelector`** — 行为 Goal（移动、攻击、游泳等）
- **`targetSelector`** — 目标选择 Goal（选择攻击目标）

每个 Goal 有优先级数值，**数值越低优先级越高**。多个 Goal 可同时运行，只要它们使用不同的互斥控制位（Controls）。

### 6.2.2 常用内置 Goal

**行为 Goal（goalSelector）：**

| 类名 | 优先级示例 | 用途 |
|---|---|---|
| `SwimGoal` | 0 | 游泳（让生物在水中浮起） |
| `EscapeDangerGoal` | 1 | 受到伤害时逃跑 |
| `MeleeAttackGoal` | 2 | 近战攻击目标 |
| `WanderAroundGoal` | 3 | 随机漫步 |
| `WanderAroundFarGoal` | 3 | 远处随机漫步 |
| `LookAtEntityGoal` | 4 | 看向附近的实体 |
| `LookAroundGoal` | 5 | 随机环顾四周 |
| `TemptGoal` | 3 | 被食物吸引跟随玩家 |
| `BreedGoal` | 2 | 繁殖 |
| `FollowParentGoal` | 3 | 跟随父母 |
| `EscapeSunlightGoal` | 2 | 躲避阳光 |

**目标选择 Goal（targetSelector）：**

| 类名 | 用途 |
|---|---|
| `ActiveTargetGoal<T>` | 主动寻找某类实体作为目标 |
| `NearestAttackableTargetGoal<T>` | 攻击最近的某类实体 |
| `HurtByTargetGoal` | 被攻击后反击 |
| `RevengeGoal` | 被攻击后报复 |
| `TrackOwnerAttackerGoal` | 追踪主人的攻击者（狼） |
| `AttackWithOwnerGoal` | 和主人一起攻击（狼） |

### 6.2.3 添加 Goal 到自定义实体

在你的实体类中重写 `registerGoals()`：

```java
package com.example.mod.entity;

import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.mob.PathAwareEntity;
import net.minecraft.entity.player.PlayerEntity;

public class ModZombieEntity extends PathAwareEntity {
    // ... 构造方法等 ...

    @Override
    protected void registerGoals() {
        // 行为 Goal（优先级数值越小越优先）

        // 0: 游泳（最高优先级）
        this.goalSelector.addGoal(0, new SwimGoal(this));

        // 1: 近战攻击
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));

        // 2: 跟随玩家（如果持有小麦）
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.0D, Ingredient.ofItems(Items.WHEAT), false));

        // 3: 随机漫步
        this.goalSelector.addGoal(3, new WanderAroundGoal(this, 0.8D));

        // 4: 看向玩家
        this.goalSelector.addGoal(4, new LookAtEntityGoal(this, PlayerEntity.class, 8.0F));

        // 5: 随机环顾
        this.goalSelector.addGoal(5, new LookAroundGoal(this));

        // 目标选择 Goal
        // 0: 攻击最近的玩家（最高优先级）
        this.targetSelector.addGoal(0, new ActiveTargetGoal<>(this, PlayerEntity.class, true));

        // 1: 被攻击后反击
        this.targetSelector.addGoal(1, new RevengeGoal(this));
    }
}
```

### 6.2.4 创建自定义 Goal

```java
package com.example.mod.entity.ai;

import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.mob.PathAwareEntity;

import java.util.EnumSet;

public class CustomGoal extends Goal {

    private final PathAwareEntity entity;

    public CustomGoal(PathAwareEntity entity) {
        this.entity = entity;
        // 声明该 Goal 会使用的控制类型
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public boolean canStart() {
        // 返回 true 时启动该 Goal
        return this.entity.getHealth() < 10.0f;  // 血量低于一半时启动
    }

    @Override
    public boolean shouldContinue() {
        // 返回 true 时继续执行（默认调用 canStart）
        return this.entity.getHealth() < 15.0f;
    }

    @Override
    public void start() {
        // Goal 启动时调用
        System.out.println("Custom goal started!");
    }

    @Override
    public void stop() {
        // Goal 停止时调用
        System.out.println("Custom goal stopped!");
    }

    @Override
    public void tick() {
        // 每个 tick 调用一次（20 次/秒）
        // 在此实现 Goal 的核心行为
        if (this.entity.getTarget() != null) {
            this.entity.getNavigation()
                .startMovingTo(this.entity.getTarget(), 1.5D);
        }
    }
}
```

### 6.2.5 Goal 控制类型（Controls）

| 控制类型 | 说明 |
|---|---|
| `Control.MOVE` | 移动控制（有移动控制的 Goal 互斥） |
| `Control.LOOK` | 视线控制（有视线控制的 Goal 互斥） |
| `Control.JUMP` | 跳跃控制 |
| `Control.TARGET` | 目标选择控制 |

声明 `this.setControls(EnumSet.of(Control.MOVE, Control.LOOK))` 告诉系统该 Goal 会使用移动和视线，防止其他使用相同控制类型的 Goal 同时运行。

---

## 6.3 实体属性

### 6.3.1 EntityAttributes

实体的属性（Attributes）定义了其基本特性值，如生命值、移动速度、攻击力等。

Fabric 1.20.4 使用 `net.minecraft.entity.attribute.EntityAttributes` 类（Yarn 映射）。

### 6.3.2 通用实体属性

| 属性 | 默认值 | 说明 |
|---|---|---|
| `GENERIC_MAX_HEALTH` | 20.0 | 最大生命值 |
| `GENERIC_FOLLOW_RANGE` | 32.0 | 追踪范围 |
| `GENERIC_MOVEMENT_SPEED` | 0.7 | 移动速度 |
| `GENERIC_ATTACK_DAMAGE` | 2.0 | 攻击伤害 |
| `GENERIC_ATTACK_SPEED` | 4.0 | 攻击速度 |
| `GENERIC_ARMOR` | 0.0 | 护甲值 |
| `GENERIC_ARMOR_TOUGHNESS` | 0.0 | 护甲韧性 |
| `GENERIC_KNOCKBACK_RESISTANCE` | 0.0 | 击退抗性 |
| `GENERIC_LUCK` | 0.0 | 幸运值 |
| `GENERIC_FLYING_SPEED` | 0.4 | 飞行速度 |
| `PLAYER_BLOCK_BREAK_SPEED` | 1.0 | 方块破坏速度 |
| `ZOMBIE_SPAWN_REINFORCEMENTS` | 0.0 | 僵尸增援概率 |

### 6.3.3 为实体构造属性

```java
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.attribute.EntityAttribute;

public static DefaultAttributeContainer createCustomAttributes() {
    return LivingEntity.createLivingAttributes()
        .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)       // 40 点生命
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)     // 6 点攻击力
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)    // 速度
        .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 32.0)     // 追踪范围
        .add(EntityAttributes.GENERIC_ARMOR, 4.0)             // 4 点护甲
        .add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, 2.0)   // 2 点护甲韧性
        .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, 0.5) // 50% 击退抗性
        .add(EntityAttributes.GENERIC_ATTACK_SPEED, 2.0)      // 攻击速度
        .build();
}
```

---

## 6.4 生成规则

### 6.4.1 FabricDefaultAttributeRegistry 注册

**所有 LivingEntity 类型必须注册属性，否则游戏会崩溃。**

```java
package com.example.mod;

import com.example.mod.entity.ModEntities;
import com.example.mod.entity.ModZombieEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;

public class Mod implements ModInitializer {

    @Override
    public void onInitialize() {
        // 注册实体属性（必须在实体注册之后）
        FabricDefaultAttributeRegistry.register(
            ModEntities.MOD_ZOMBIE,
            HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.25)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 20.0)
                .build()
        );

        // 被动动物使用 createLivingAttributes
        FabricDefaultAttributeRegistry.register(
            ModEntities.MOD_ANIMAL,
            LivingEntity.createLivingAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 10.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.2)
                .build()
        );

        // 直接使用已有实体的属性 + 自定义覆盖
        FabricDefaultAttributeRegistry.register(
            ModEntities.MOD_ZOMBIE,
            ZombieEntity.createZombieAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 50.0)  // 覆盖为 50
                .build()
        );
    }
}
```

### 6.4.2 SpawnRestriction

控制实体能在哪些条件下自然生成：

```java
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.world.Heightmap;

// 在 ModInitializer 中注册
SpawnRestriction.register(
    ModEntities.MOD_ZOMBIE,
    SpawnRestriction.Location.ON_GROUND,      // 生成位置：地面
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,  // 高度图类型
    HostileEntity::canSpawnInDark               // 生成条件（黑暗中生成）
);

// 自定义生成条件
SpawnRestriction.register(
    ModEntities.MOD_ANIMAL,
    SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
    (entity, world, spawnReason, pos, random) -> {
        return world.getLightLevel(LightType.SKY, pos) > 8;  // 白天生成
    }
);
```

**SpawnRestriction.Location 类型：**

| 类型 | 说明 |
|---|---|
| `ON_GROUND` | 地面上生成 |
| `IN_WATER` | 水中生成 |
| `UNRESTRICTED` | 无限制 |
| `CEILING` | 天花板上生成 |

### 6.4.3 BiomeModifications.addSpawn()

将实体添加到生物群系的自然生成列表：

```java
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionCriteria;
import net.minecraft.entity.SpawnGroup;

// 在 ModInitializer 中注册
BiomeModifications.addSpawn(
    BiomeSelectionCriteria.categories(Biome.Category.FOREST),  // 在森林类生物群系中生成
    SpawnGroup.MONSTER,                                         // 生成组
    ModEntities.MOD_ZOMBIE,                                     // 实体类型
    100,    // 权重（越大生成概率越高）
    2,      // 最小生成数量
    4       // 最大生成数量
);

// 更多筛选条件示例
BiomeModifications.addSpawn(
    BiomeSelectionCriteria.tag(BiomeTags.IS_OVERWORLD),  // 所有主世界生物群系
    SpawnGroup.MONSTER,
    ModEntities.MOD_ZOMBIE,
    50, 2, 4
);

// 指定具体群系
BiomeModifications.addSpawn(
    BiomeSelectionCriteria.includeByKey(BiomeKeys.PLAINS, BiomeKeys.DESERT),
    SpawnGroup.CREATURE,
    ModEntities.MOD_ANIMAL,
    10, 3, 6
);

// 排除某些群系
BiomeModifications.addSpawn(
    BiomeSelectionCriteria.categories(Biome.Category.FOREST)
        .and(ContextBiomeSelector.negate(
            BiomeSelectionCriteria.includeByKey(BiomeKeys.FLOWER_FOREST))),
    SpawnGroup.MONSTER,
    ModEntities.MOD_ZOMBIE,
    100, 2, 4
);
```

---

## 6.5 实体渲染

### 6.5.1 客户端注册

所有渲染相关注册必须在 Client Mod Initializer 中完成：

```java
package com.example.mod.client;

import com.example.mod.ModEntities;
import com.example.mod.entity.renderer.ModZombieEntityRenderer;
import com.example.mod.entity.model.ModZombieEntityModel;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.util.Identifier;

public class ModClient implements ClientModInitializer {

    // 定义实体模型层（用于模型烘焙）
    public static final EntityModelLayer MOD_ZOMBIE_LAYER = new EntityModelLayer(
        Identifier.of("examplemod", "mod_zombie"), "main"
    );

    @Override
    public void onInitializeClient() {
        // 1. 注册实体渲染器
        EntityRendererRegistry.register(
            ModEntities.MOD_ZOMBIE,
            ModZombieEntityRenderer::new  // 构造方法引用
        );

        // 2. 注册模型层，提供 LayerDefinition
        EntityModelLayerRegistry.registerModelLayer(
            MOD_ZOMBIE_LAYER,
            ModZombieEntityModel::getTexturedModelData  // 提供静态方法返回 TexturedModelData
        );
    }
}
```

### 6.5.2 创建实体模型

```java
package com.example.mod.entity.model;

import net.minecraft.client.model.*;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.entity.Entity;

public class ModZombieEntityModel<T extends Entity> extends EntityModel<T> {

    private final ModelPart body;

    public ModZombieEntityModel(ModelPart root) {
        // 从根 ModelPart 获取特定部分的引用
        this.body = root.getChild("body");
    }

    public static TexturedModelData getTexturedModelData() {
        ModelData modelData = new ModelData();
        ModelPartData root = modelData.getRoot();

        // 添加"body"子模型
        root.addChild(
            "body",
            ModelPartBuilder.create()
                .uv(0, 0)                              // 纹理偏移
                .cuboid(-4, -4, -2, 8, 8, 4),         // 长方体（x, y, z, 宽, 高, 深）
            ModelTransform.pivot(0, 12, 0)             // 变换（位置、旋转）
        );

        return TexturedModelData.of(modelData, 64, 32);  // 纹理大小 64x32
    }

    @Override
    public void setAngles(T entity, float limbAngle, float limbDistance,
                          float animationProgress, float headYaw, float headPitch) {
        // 在此设置模型的动画角度
        // limbAngle / limbDistance — 行走动画参数
        // animationProgress — 年龄/时间相关动画
        // headYaw / headPitch — 头部旋转
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices,
                       int light, int overlay, float red, float green,
                       float blue, float alpha) {
        // 渲染模型
        body.render(matrices, vertices, light, overlay);
    }
}
```

### 6.5.3 创建实体渲染器

```java
package com.example.mod.entity.renderer;

import com.example.mod.Mod;
import com.example.mod.client.ModClient;
import com.example.mod.entity.ModZombieEntity;
import com.example.mod.entity.model.ModZombieEntityModel;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.MobEntityRenderer;
import net.minecraft.util.Identifier;

public class ModZombieEntityRenderer extends MobEntityRenderer<ModZombieEntity, ModZombieEntityModel<ModZombieEntity>> {

    public ModZombieEntityRenderer(EntityRendererFactory.Context context) {
        super(
            context,
            new ModZombieEntityModel<>(context.getPart(ModClient.MOD_ZOMBIE_LAYER)),  // 从上下文获取已烘焙的模型
            0.5f  // 阴影大小
        );
    }

    @Override
    public Identifier getTexture(ModZombieEntity entity) {
        // 返回纹理路径
        return Identifier.of("examplemod", "textures/entity/mod_zombie.png");
    }
}
```

### 6.5.4 纹理资源路径

纹理文件放置位置：
```
src/main/resources/assets/examplemod/textures/entity/mod_zombie.png
```

对应代码中的 `Identifier`：
```java
Identifier.of("examplemod", "textures/entity/mod_zombie.png")
```

> 纹理一般建议使用 64x64 或 64x32 的 PNG 文件。使用 Blockbench 等建模工具可以更方便地创建模型和纹理。

### 6.5.5 EntityRendererFactory.Context 主要方法

| 方法 | 返回类型 | 用途 |
|---|---|---|
| `getPart(EntityModelLayer)` | `ModelPart` | 获取已烘焙的模型部件 |
| `getModelLoader()` | `EntityModelLoader` | 实体模型加载器 |
| `getModelManager()` | `BakedModelManager` | 方块/物品模型管理器 |
| `getTextRenderer()` | `TextRenderer` | 文本渲染器 |
| `getBlockRenderManager()` | `BlockRenderManager` | 方块渲染管理器 |
| `getItemRenderer()` | `ItemRenderer` | 物品渲染器 |
| `getHeldItemRenderer()` | `HeldItemRenderer` | 手持物品渲染器 |

---

## 6.6 完整实体示例

以下是一个完整的自定义实体从注册到渲染的代码流程：

```java
// ===== 1. 注册 EntityType =====
// ModEntities.java
public static final EntityType<ModZombieEntity> MOD_ZOMBIE = Registry.register(
    Registries.ENTITY_TYPE,
    Identifier.of("examplemod", "mod_zombie"),
    FabricEntityTypeBuilder.create(SpawnGroup.MONSTER, ModZombieEntity::new)
        .dimensions(EntityDimensions.fixed(0.6f, 1.95f))
        .build()
);

// ===== 2. 注册属性 =====
// ModInitializer.onInitialize()
FabricDefaultAttributeRegistry.register(
    ModEntities.MOD_ZOMBIE,
    HostileEntity.createHostileAttributes()
        .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
        .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
        .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.3)
        .build()
);

// ===== 3. 生成规则 =====
// ModInitializer.onInitialize()
SpawnRestriction.register(
    ModEntities.MOD_ZOMBIE,
    SpawnRestriction.Location.ON_GROUND,
    Heightmap.Type.MOTION_BLOCKING_NO_LEAVES,
    HostileEntity::canSpawnInDark
);
BiomeModifications.addSpawn(
    BiomeSelectionCriteria.categories(Biome.Category.FOREST),
    SpawnGroup.MONSTER, ModEntities.MOD_ZOMBIE, 100, 2, 4
);

// ===== 4. 客户端渲染 =====
// ModClient.onInitializeClient()
EntityRendererRegistry.register(ModEntities.MOD_ZOMBIE, ModZombieEntityRenderer::new);
EntityModelLayerRegistry.registerModelLayer(MOD_ZOMBIE_LAYER,
    ModZombieEntityModel::getTexturedModelData);
```

> **注意事项：**
> - Fabric 1.20.4 使用 Yarn 映射，属性类为 `EntityAttributes`
> - 所有 `LivingEntity` 派生类必须调用 `FabricDefaultAttributeRegistry.register()`，否则游戏崩溃
> - 实体渲染注册必须在 `ClientModInitializer` 中，不能在 `ModInitializer` 中调用客户端相关 API
> - `BiomeModifications` 的 API 在 1.20.4 中可用，但需要依赖 Fabric API 的 `fabric-biome-api-v1` 模块
