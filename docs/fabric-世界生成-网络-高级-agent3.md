# Fabric 1.20.4 世界生成、网络通信与高级主题

> 本文档对应知识大纲的第 7/8/9 章，涵盖世界生成与维度、网络通信、以及高级主题（能源系统、GUI、渲染进阶、配置系统）。

---

## 7. 世界生成与维度

### 7.1 三层架构：ConfiguredFeature → PlacedFeature → BiomeModification

Fabric 1.20.4 的世界生成遵循三层架构。每一层职责分明，逐层细化：

| 层级 | 类/概念 | 职责 | 存储位置 |
|------|---------|------|---------|
| 第一层 | `ConfiguredFeature<FC, F>` | 定义**生成什么**（方块类型、大小、配置参数） | `BuiltinRegistries.CONFIGURED_FEATURE` 或 JSON |
| 第二层 | `PlacedFeature` | 定义**在哪里生成**（放置修饰符链：次数、高度、方形散布、生物群系过滤） | `BuiltinRegistries.PLACED_FEATURE` 或 JSON |
| 第三层 | `BiomeModifications` (Fabric API) | 将 `PlacedFeature` **挂接到具体的生物群系**和生成阶段 | Fabric API 运行时注册 |

#### 7.1.1 ConfiguredFeature 注册

`ConfiguredFeature` 将 `Feature`（生成逻辑）与 `FeatureConfig`（配置参数）配对。最简单的注册方式是通过数据生成或代码直接创建：

```java
// 使用 Fabric API 的 BuiltinRegistries（1.20.4 兼容）
public static final Identifier EXAMPLE_FEATURE_ID = new Identifier("mymod", "example_feature");

public static final ConfiguredFeature<?, ?> EXAMPLE_CONFIGURED = new ConfiguredFeature<>(
    Feature.ORE,  // 使用原版 Feature 类型
    new OreFeatureConfig(
        OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,  // 目标方块规则
        ModBlocks.EXAMPLE_ORE.getDefaultState(),      // 要放置的方块
        8                                              // 矿脉大小
    )
);

// 在 ModInitializer 中注册
@Override
public void onInitialize() {
    Registry.register(
        BuiltinRegistries.CONFIGURED_FEATURE,
        EXAMPLE_FEATURE_ID,
        EXAMPLE_CONFIGURED
    );
}
```

**关于 `BuiltinRegistries` 的说明**：在 1.20.4 中，`BuiltinRegistries.CONFIGURED_FEATURE` 和 `BuiltinRegistries.PLACED_FEATURE` 仍然可用并推荐用于代码注册。`RegistryKeys.CONFIGURED_FEATURE` 和 `RegistryKeys.PLACED_FEATURE` 是注册键常量，用于引用这些注册表。

#### 7.1.2 PlacedFeature 注册

`PlacedFeature` 包装 `ConfiguredFeature` 并附加放置修饰符列表。修饰符按顺序执行：

```java
public static final PlacedFeature EXAMPLE_PLACED = new PlacedFeature(
    RegistryEntry.of(EXAMPLE_CONFIGURED),
    List.of(
        CountPlacementModifier.of(5),                // 每区块尝试 5 次
        SquarePlacementModifier.of(),                 // 在区块内随机 X/Z 偏移 (0-15)
        HeightmapPlacementModifier.of(Heightmap.Type.OCEAN_FLOOR_WG),  // 基于高度图定位
        BiomePlacementModifier.of()                   // 过滤生物群系
    )
);

// 注册
Registry.register(
    BuiltinRegistries.PLACED_FEATURE,
    EXAMPLE_FEATURE_ID,  // 通常使用与 configured feature 不同的 ID
    EXAMPLE_PLACED
);
```

**常用放置修饰符一览：**

| 修饰符 | 用途 | 参数说明 |
|--------|------|---------|
| `CountPlacementModifier.of(count)` | 每区块生成 count 次 | int |
| `CountOnEveryLayerPlacementModifier.of(count)` | 每层生成 count 次 | int |
| `RarityFilterPlacementModifier.of(chance)` | 1/chance 概率通过 | int |
| `SquarePlacementModifier.of()` | 随机 X/Z 偏移 (0-15) | 无参数 |
| `HeightmapPlacementModifier.of(type)` | 基于高度图决定 Y | `WORLD_SURFACE_WG`, `OCEAN_FLOOR_WG`, `MOTION_BLOCKING` 等 |
| `HeightRangePlacementModifier.uniform(min, max)` | Y 范围均匀分布 | `VerticalAnchor` |
| `BiomePlacementModifier.of()` | 按生物群系过滤 | 无参数 |
| `BlockPredicateFilterPlacementModifier.of(predicate)` | 方块谓词过滤 | `BlockPredicate` |
| `RandomOffsetPlacementModifier.of(xz, y)` | 随机偏移 | `VerticalAnchor` |

#### 7.1.3 BiomeModifications — 挂接到生物群系

使用 Fabric API 的 `BiomeModifications.addFeature()` 将 PlacedFeature 挂接到特定生物群系：

```java
BiomeModifications.addFeature(
    BiomeSelectors.foundInOverworld(),                     // 生物群系列选择器
    GenerationStep.Feature.UNDERGROUND_ORES,                // 生成阶段
    RegistryKey.of(RegistryKeys.PLACED_FEATURE, EXAMPLE_FEATURE_ID)  // 引用 PlacedFeature
);
```

**常用 `BiomeSelectors`：**

| 选择器 | 用途 |
|--------|------|
| `BiomeSelectors.foundInOverworld()` | 所有主世界生物群系 |
| `BiomeSelectors.foundInTheNether()` | 所有下界生物群系 |
| `BiomeSelectors.foundInTheEnd()` | 所有末地生物群系 |
| `BiomeSelectors.includeByKey(biomeKey)` | 指定特定生物群系 |
| `BiomeSelectors.tag(TagKey.of(RegistryKeys.BIOME, tagId))` | 按标签选择 |
| `BiomeSelectors.all()` | 所有生物群系 |

**`GenerationStep.Feature` 生成阶段顺序（从早到晚）：**

```
RAW_GENERATION → LAKES → LOCAL_MODIFICATIONS → UNDERGROUND_STRUCTURES →
SURFACE_STRUCTURES → STRONGHOLDS → UNDERGROUND_ORES → UNDERGROUND_DECORATION →
VEGETAL_DECORATION → TOP_LAYER_MODIFICATION → FLUID_SPRINGS
```

#### 7.1.4 JSON 数据驱动方式（替代方案）

除了代码注册，也可以通过 JSON 数据包定义。Fabric 1.20.4 会自动加载 `data/<modid>/worldgen/` 下的 JSON 文件。

**configured_feature JSON** (`data/mymod/worldgen/configured_feature/example_ore.json`)：

```json
{
  "type": "minecraft:ore",
  "config": {
    "size": 8,
    "discard_chance_on_air_exposure": 0.0,
    "targets": [
      {
        "target": {
          "predicate_type": "minecraft:tag_match",
          "tag": "minecraft:stone_ore_replaceables"
        },
        "state": {
          "Name": "mymod:example_ore"
        }
      }
    ]
  }
}
```

**placed_feature JSON** (`data/mymod/worldgen/placed_feature/example_ore.json`)：

```json
{
  "feature": "mymod:example_ore",
  "placement": [
    { "type": "minecraft:count", "count": 8 },
    { "type": "minecraft:in_square" },
    { "type": "minecraft:height_range", "height": {
      "type": "minecraft:trapezoid",
      "min_inclusive": { "above_bottom": 0 },
      "max_inclusive": { "absolute": 64 }
    }},
    { "type": "minecraft:biome" }
  ]
}
```

---

### 7.2 自定义矿物：OreConfiguredFeatures、OrePlacedFeatures 模式

自定义矿石是模组开发中最常见的需求。Fabric 1.20.4 中推荐使用以下模式：

#### 7.2.1 矿脉生成配置

```java
public class ModOreGeneration {

    public static final Identifier ORE_EXAMPLE_ID = new Identifier("mymod", "ore_example");

    // 定义 ConfiguredFeature
    public static final ConfiguredFeature<?, ?> ORE_EXAMPLE_CONFIGURED = new ConfiguredFeature<>(
        Feature.ORE,
        new OreFeatureConfig(
            OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,  // 替换主世界石头
            ModBlocks.EXAMPLE_ORE.getDefaultState(),
            8  // 矿脉大小（最大方块数）
        )
    );

    // 定义 PlacedFeature
    public static final PlacedFeature ORE_EXAMPLE_PLACED = new PlacedFeature(
        RegistryEntry.of(ORE_EXAMPLE_CONFIGURED),
        List.of(
            CountPlacementModifier.of(8),                 // 每区块最多 8 条矿脉
            SquarePlacementModifier.of(),                  // XZ 散布
            HeightRangePlacementModifier.uniform(
                VerticalAnchor.absolute(-64),
                VerticalAnchor.absolute(64)
            ),                                             // Y 范围 -64 到 64
            BiomePlacementModifier.of()                    // 生物群系过滤
        )
    );

    // 注册 ConfiguredFeature 和 PlacedFeature
    public static void registerOres() {
        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, ORE_EXAMPLE_ID, ORE_EXAMPLE_CONFIGURED);
        Registry.register(BuiltinRegistries.PLACED_FEATURE, ORE_EXAMPLE_ID, ORE_EXAMPLE_PLACED);

        // 挂接到生物群系
        BiomeModifications.addFeature(
            BiomeSelectors.foundInOverworld(),
            GenerationStep.Feature.UNDERGROUND_ORES,
            RegistryKey.of(RegistryKeys.PLACED_FEATURE, ORE_EXAMPLE_ID)
        );
    }
}
```

#### 7.2.2 多重目标矿石配置

使用 `OreFeatureConfig.Rules` 可以针对不同类型的石头进行替换：

```java
// 既替换主世界的石头（stone），也替换深板岩（deepslate）
List<OreFeatureConfig.Target> targets = List.of(
    OreFeatureConfig.createTarget(
        OreFeatureConfig.Rules.BASE_STONE_OVERWORLD,
        ModBlocks.EXAMPLE_ORE.getDefaultState()
    ),
    OreFeatureConfig.createTarget(
        OreFeatureConfig.Rules.DEEPSLATE_ORE_REPLACEABLES,
        ModBlocks.DEEPSLATE_EXAMPLE_ORE.getDefaultState()
    )
);

new OreFeatureConfig(targets, veinSize);
```

#### 7.2.3 不同深度的矿石分布策略

| 策略 | 高度范围 | 修饰符 |
|------|---------|--------|
| 均匀分布 | 全高度 | `HeightRangePlacementModifier.uniform(VerticalAnchor.absolute(-64), VerticalAnchor.absolute(320))` |
| 三角分布 | 特定区域密集 | `HeightRangePlacementModifier.trapezoid(VerticalAnchor.absolute(0), VerticalAnchor.absolute(64))` |
| 偏向底部 | 越深越多 | `CountPlacementModifier.of(count)` + `HeightRangePlacementModifier.uniform(VerticalAnchor.bottom(), VerticalAnchor.absolute(32))` |

---

### 7.3 自定义树木：TreeConfiguredFeatures、TrunkPlacer/FoliagePlacer

#### 7.3.1 使用原版树木配置（简单方式）

如果不需要自定义树干和树叶形状，可以直接使用原版的 `TreeConfiguredFeatures` 和 `TreePlacedFeatures` 中的配置生成树木：

```java
// 直接使用原版树木配置 + 自定义方块提供器
public static final ConfiguredFeature<?, ?> CUSTOM_TREE_CONFIGURED = new ConfiguredFeature<>(
    Feature.TREE,
    new TreeFeatureConfig.Builder(
        BlockStateProvider.of(ModBlocks.CUSTOM_LOG),       // 自定义原木
        new StraightTrunkPlacer(4, 2, 0),                  // 树干放置器（基础高度4，随机增量2，额外0）
        BlockStateProvider.of(ModBlocks.CUSTOM_LEAVES),     // 自定义树叶
        new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),  // 树叶放置器
        new TwoLayersFeatureSize(1, 0, 1)                  // 两层级特征尺寸
    ).build()
);
```

#### 7.3.2 原版内置 TrunkPlacer 类型

| 类 | 说明 | 构造函数参数 |
|----|------|------------|
| `StraightTrunkPlacer` | 笔直向上 | `(baseHeight, randomHeight, extraHeight)` |
| `ForkingTrunkPlacer` | 分叉分支 | `(baseHeight, randomHeight, extraHeight)` |
| `GiantTrunkPlacer` | 2x2 大型树干 | `(baseHeight, randomHeight, extraHeight)` |
| `BendingTrunkPlacer` | 弯曲树干 | `(baseHeight, randomHeight, extraHeight)` |
| `MegaJungleTrunkPlacer` | 巨型丛林树干 | `(baseHeight, randomHeight, extraHeight)` |
| `DarkOakTrunkPlacer` | 深色橡木 | `(baseHeight, randomHeight, extraHeight)` |
| `CherryTrunkPlacer` | 樱花树干 | `(baseHeight, randomHeight, extraHeight, cornerBranchProb, hangingLeavesChance, hangingLeavesCount)` |

#### 7.3.3 原版内置 FoliagePlacer 类型

| 类 | 说明 |
|----|------|
| `BlobFoliagePlacer` | 团状树叶（橡木风格） |
| `PineFoliagePlacer` | 锥形树叶（松树风格） |
| `SpruceFoliagePlacer` | 层状树叶（云杉风格） |
| `RandomSpreadFoliagePlacer` | 随机散布（丛林风格） |
| `BushFoliagePlacer` | 灌木状 |
| `AcaciaFoliagePlacer` | 金合欢树冠 |
| `CherryFoliagePlacer` | 樱花树冠 |
| `DarkOakFoliagePlacer` | 深色橡木树冠 |
| `MegaPineFoliagePlacer` | 巨型松树 |
| `LargeOakFoliagePlacer` | 大型橡木（Fancy Oak） |

#### 7.3.4 自定义 TrunkPlacer（高级）

创建自定义树干逻辑需要三步：

**第一步：创建 TrunkPlacer 子类**

```java
public class CustomTrunkPlacer extends TrunkPlacer {

    public CustomTrunkPlacer(int baseHeight, int randomHeight, int extraHeight) {
        super(baseHeight, randomHeight, extraHeight);
    }

    // 为序列化提供 Codec
    public static final Codec<CustomTrunkPlacer> CODEC = RecordCodecBuilder.create(instance ->
        fillTrunkPlacerFields(instance).apply(instance, CustomTrunkPlacer::new)
    );

    @Override
    protected TrunkPlacerType<?> getType() {
        return ModTrunkPlacers.CUSTOM_TRUNK_PLACER;
    }

    @Override
    public List<FoliagePlacer.TreeNode> generate(
            TestableWorld world,
            BlockPos.Mutable pos,
            Random random,
            int height,
            BlockState logState,
            Predicate<BlockState> predicate) {

        // 这里实现自定义树干生成逻辑
        // 放置树干方块并返回叶子的 TreeNode 列表
        for (int i = 0; i < height; i++) {
            setBlockState(world, pos, logState);
            pos.move(Direction.UP);
        }

        return List.of(new FoliagePlacer.TreeNode(pos.toImmutable(), 0, false));
    }
}
```

**第二步：注册 TrunkPlacerType（需要 Accessor/Mixin）**

由于 `TrunkPlacerType` 的构造函数是私有的，需要借助 Fabric API 的 Access Widener 或 Mixin：

```java
// 使用 Mixin 或 Accessor 调用注册方法
// 方案一：使用 Accessor
// @Accessor("register") 静态方法

// 方案二：直接使用 Registry 注册（1.20.4 可用）
public class ModTrunkPlacers {
    public static final TrunkPlacerType<CustomTrunkPlacer> CUSTOM_TRUNK_PLACER =
        Registry.register(
            BuiltinRegistries.TRUNK_PLACER_TYPE,
            new Identifier("mymod", "custom_trunk_placer"),
            new TrunkPlacerType<>(CustomTrunkPlacer.CODEC)
        );

    public static void register() {
        // 触发静态初始化
    }
}
```

> **注意**：在 1.20.4 中，`BuiltinRegistries.TRUNK_PLACER_TYPE` 仍然可用。如果你遇到访问限制，可以通过 `Fabric Transitive Access Wideners` 来获得访问权限。

**第三步：在 TreeFeatureConfig 中使用**

```java
new TreeFeatureConfig.Builder(
    BlockStateProvider.of(ModBlocks.CUSTOM_LOG),
    ModTrunkPlacers.CUSTOM_TRUNK_PLACER.create(4, 2, 0),  // baseHeight, randomHeight, extraHeight
    BlockStateProvider.of(ModBlocks.CUSTOM_LEAVES),
    new BlobFoliagePlacer(ConstantIntProvider.create(2), ConstantIntProvider.create(0), 3),
    new TwoLayersFeatureSize(1, 0, 1)
).build()
```

---

### 7.4 自定义生物群系：JSON 定义 + 数据生成

自定义生物群系在 Fabric 1.20.4 中主要通过 JSON 数据包实现，也可通过数据生成器自动生成。

#### 7.4.1 JSON 结构

文件位于 `data/mymod/worldgen/biome/custom_biome.json`：

```json
{
  "temperature": 0.8,
  "downfall": 0.4,
  "has_precipitation": true,
  "temperature_modifier": "none",
  "effects": {
    "sky_color": 7907327,
    "water_color": 4159204,
    "water_fog_color": 329011,
    "fog_color": 12638463,
    "grass_color": 8372060,
    "foliage_color": 6583581,
    "mood_sound": {
      "sound": "minecraft:ambient.cave",
      "tick_delay": 6000,
      "block_search_extent": 8,
      "offset": 2.0
    },
    "music": {
      "sound": "minecraft:music.overworld.badlands",
      "min_delay": 12000,
      "max_delay": 24000,
      "replace_current_music": false
    },
    "particles": {
      "probability": 0.002,
      "options": {
        "type": "minecraft:white_ash"
      }
    }
  },
  "carvers": [
    "minecraft:cave",
    "minecraft:cave_extra_underground",
    "minecraft:canyon"
  ],
  "features": [
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    [],
    []
  ],
  "spawners": {
    "ambient": [],
    "axolotls": [],
    "creature": [],
    "misc": [],
    "monster": [],
    "underground_water_creature": [],
    "water_ambient": [],
    "water_creature": []
  },
  "spawn_costs": {}
}
```

**关键字段说明：**

| 字段 | 类型 | 说明 |
|------|------|------|
| `temperature` | float | 温度 (-0.5 ~ 2.0)，影响下雪/降雨边界 |
| `downfall` | float | 降雨量 (0.0 ~ 1.0) |
| `has_precipitation` | boolean | 是否有降水（雨/雪） |
| `temperature_modifier` | string | `"none"` 或 `"frozen"` |
| `effects.sky_color` | int | 天空颜色（十进制 RGB） |
| `effects.water_color` | int | 水体颜色 |
| `effects.water_fog_color` | int | 水下雾颜色 |
| `effects.grass_color` | int | 草颜色（可选，不设置时自动根据温度计算） |
| `effects.foliage_color` | int | 树叶颜色（可选） |

**`features` 数组**：这是一个包含 10 个 JSON 数组的数组，每个索引对应 `GenerationStep.Feature` 的一个阶段：
```
[0] RAW_GENERATION
[1] LAKES
[2] LOCAL_MODIFICATIONS
[3] UNDERGROUND_STRUCTURES
[4] SURFACE_STRUCTURES
[5] STRONGHOLDS
[6] UNDERGROUND_ORES
[7] UNDERGROUND_DECORATION
[8] VEGETAL_DECORATION
[9] TOP_LAYER_MODIFICATION
```

#### 7.4.2 Fabric 数据生成器生成生物群系

使用 Fabric API 的 Data Generation API 自动生成生物群系 JSON：

```java
public class ModBiomeProvider extends FabricDynamicRegistryProvider {
    public ModBiomeProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    protected void configure(RegistriesFuture registries) {
        // 获取已有注册表
        CompletableFuture<Registry<Biome>> biomeRegistry = registries.getCompletableFuture(RegistryKeys.BIOME);

        // 生成自定义生物群系
        addBiome(biomeRegistry);
    }

    private void addBiome(CompletableFuture<Registry<Biome>> registry) {
        Biome biome = createCustomBiome();
        add(new Identifier("mymod", "custom_biome"), biome);
    }

    private Biome createCustomBiome() {
        SpawnSettings.Builder spawnSettings = new SpawnSettings.Builder();
        // 添加生物生成设置
        spawnSettings.spawn(SpawnGroup.CREATURE, new SpawnSettings.SpawnEntry(
            EntityType.SHEEP, 12, 4, 4
        ));

        GenerationSettings.Builder genSettings = new GenerationSettings.Builder();
        // 添加特征（按生成阶段索引）
        // genSettings.feature(GenerationStep.Feature.VEGETAL_DECORATION, placedFeatureEntry);

        return new Biome.Builder()
            .precipitation(true)
            .temperature(0.8f)
            .downfall(0.4f)
            .effects(new BiomeEffects.Builder()
                .skyColor(7907327)
                .waterColor(4159204)
                .waterFogColor(329011)
                .fogColor(12638463)
                .grassColor(8372060)
                .foliageColor(6583581)
                .build())
            .spawnSettings(spawnSettings.build())
            .generationSettings(genSettings.build())
            .build();
    }
}
```

---

### 7.5 自定义维度

自定义维度涉及两个核心注册表：`DimensionType`（维度类型配置）和 `LevelStem`（包含生成器配置）。

#### 7.5.1 维度类型 DimensionType JSON

文件位于 `data/mymod/dimension_type/custom_dimension_type.json`：

```json
{
  "has_skylight": true,
  "has_ceiling": false,
  "has_ender_dragon_fight": false,
  "ambient_light": 0.0,
  "coordinate_scale": 1.0,
  "min_y": -64,
  "height": 384,
  "logical_height": 384,
  "monster_spawn_light_level": {
    "type": "minecraft:uniform",
    "min_inclusive": 0,
    "max_inclusive": 7
  },
  "monster_spawn_block_light_limit": 0,
  "infiniburn": "#minecraft:infiniburn_overworld",
  "skybox": "overworld",
  "cardinal_light": "default",
  "has_fixed_time": false
}
```

**DimensionType 字段详解：**

| 字段 | 类型 | 说明 | 主世界值 | 下界值 | 末地值 |
|------|------|------|---------|-------|-------|
| `has_skylight` | boolean | 是否有天空光照 | true | false | true |
| `has_ceiling` | boolean | 是否有天花板 | false | true | false |
| `ambient_light` | float | 环境光 (0~1) | 0.0 | 0.1 | 0.25 |
| `coordinate_scale` | double | 坐标缩放 | 1.0 | 8.0 | 1.0 |
| `min_y` | int | 最小 Y (16的倍数) | -64 | 0 | 0 |
| `height` | int | 总高度 (16的倍数) | 384 | 256 | 256 |
| `logical_height` | int | 逻辑高度 | 384 | 128 | 256 |
| `infiniburn` | string | 无限燃烧方块标签 | `#infiniburn_overworld` | `#infiniburn_nether` | `#infiniburn_end` |
| `skybox` | string | 天空盒 | `overworld` | `none` | `end` |
| `cardinal_light` | string | 光照模式 | `default` | `nether` | `default` |
| `has_fixed_time` | boolean | 固定时间 | false | true | true |
| `ultrawarm` | boolean | (旧版)超热 | false | true | false |

#### 7.5.2 维度 JSON

文件位于 `data/mymod/dimension/custom_dimension.json`：

```json
{
  "type": "mymod:custom_dimension_type",
  "generator": {
    "type": "minecraft:noise",
    "settings": "minecraft:overworld",
    "biome_source": {
      "type": "minecraft:multi_noise",
      "preset": "minecraft:overworld"
    }
  }
}
```

**BiomeSource 类型选择：**

| 类型 | 说明 | 配置方式 |
|------|------|---------|
| `minecraft:multi_noise` | 多噪声生物群系（类似主世界） | `preset` 或自定义 `biomes` 列表 |
| `minecraft:fixed` | 单一生物群系 | `biome: "minecraft:plains"` |
| `minecraft:checkerboard` | 棋盘格模式 | `biomes: [...]`, `scale: N` |
| `minecraft:the_end` | 末地样式 | 无参数 |

**自定义 multi_noise 示例：**

```json
{
  "biome_source": {
    "type": "minecraft:multi_noise",
    "biomes": [
      {
        "biome": "minecraft:plains",
        "parameters": {
          "temperature": 0.0,
          "humidity": 0.0,
          "continentalness": 0.0,
          "erosion": 0.0,
          "depth": 0.0,
          "weirdness": 0.0,
          "offset": 0.0
        }
      },
      {
        "biome": "minecraft:desert",
        "parameters": {
          "temperature": 0.8,
          "humidity": -0.5,
          "continentalness": 0.2,
          "erosion": 0.0,
          "depth": 0.0,
          "weirdness": 0.0,
          "offset": 0.0
        }
      }
    ]
  }
}
```

#### 7.5.3 使用 RegistryKey 代码注册维度（可选）

除了纯 JSON 方式，也可以通过 Java 代码注册维度：

```java
public class ModDimensions {
    // 定义 RegistryKey
    public static final RegistryKey<DimensionType> CUSTOM_DIMENSION_TYPE_KEY =
        RegistryKey.of(RegistryKeys.DIMENSION_TYPE, new Identifier("mymod", "custom_dimension_type"));

    public static final RegistryKey<LevelStem> CUSTOM_DIMENSION_KEY =
        RegistryKey.of(RegistryKeys.LEVEL_STEM, new Identifier("mymod", "custom_dimension"));

    public static final RegistryKey<World> CUSTOM_DIMENSION_WORLD_KEY =
        RegistryKey.of(RegistryKeys.WORLD, new Identifier("mymod", "custom_dimension"));

    // 通过数据生成器添加 LevelStem
    public static void bootstrap(Registerable<LevelStem> context) {
        RegistryEntryLookup<DimensionType> dimensionTypeLookup = context.getRegistryLookup(RegistryKeys.DIMENSION_TYPE);
        RegistryEntryLookup<Biome> biomeLookup = context.getRegistryLookup(RegistryKeys.BIOME);

        // 创建多噪声生物群系源
        MultiNoiseBiomeSource biomeSource = MultiNoiseBiomeSource.createFromList(
            new Climate.ParameterList<>(List.of(
                Climate.parameters(
                    0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F  // temperature, humidity, continentalness, erosion, depth, weirdness, offset
                ),
                // ... 更多生物群系
            ))
        );

        // 创建噪声区块生成器
        NoiseBasedChunkGenerator chunkGenerator = new NoiseBasedChunkGenerator(
            biomeSource,
            context.getRegistryLookup(RegistryKeys.NOISE_SETTINGS)
                .getOrThrow(NoiseGeneratorSettings.OVERWORLD)  // 使用主世界噪声设置
        );

        // 创建并注册 LevelStem
        LevelStem levelStem = new LevelStem(
            dimensionTypeLookup.getOrThrow(CUSTOM_DIMENSION_TYPE_KEY),
            chunkGenerator
        );

        context.register(CUSTOM_DIMENSION_KEY, levelStem);
    }
}
```

#### 7.5.4 维度相关事件

```java
// 监听维度变化
ServerLifecycleEvents.SERVER_STARTED.register(server -> {
    // 可以在这里获取维度的 World 实例
    ServerWorld customWorld = server.getWorld(ModDimensions.CUSTOM_DIMENSION_WORLD_KEY);
    if (customWorld != null) {
        // 处理自定义维度
    }
});
```

---

### 7.6 传送门：Custom Portal API（Kyrptonaught）

Kyrptonaught 的 Custom Portal API 是目前 Fabric 1.20.4 上创建自定义传送门最流行的库。

#### 7.6.1 依赖配置

在 `build.gradle` 中添加：

```groovy
repositories {
    maven { url = "https://maven.kyrptonaught.dev" }
}

dependencies {
    modImplementation 'net.kyrptonaught:customportalapi:<version>'
    include 'net.kyrptonaught:customportalapi:<version>'  // 作为 Jar-in-Jar 嵌入
}
```

推荐的版本匹配（以实际发布版为准）：
- 1.20.4 对应 customportalapi 版本 `0.0.1-beta66` 或更新

在 `fabric.mod.json` 中添加依赖声明：

```json
"depends": {
    "customportalapi": ">=0.0.1-beta66"
}
```

#### 7.6.2 基本传送门注册

```java
import net.kyrptonaught.customportalapi.api.CustomPortalBuilder;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;

public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        CustomPortalBuilder.beginPortal()
            .frameBlock(Blocks.DIAMOND_BLOCK)          // 传送门框架方块
            .lightWithItem(Items.ENDER_EYE)             // 点火物品
            .destDimID(new Identifier("mymod", "custom_dimension"))  // 目标维度
            .tintColor(45, 65, 101)                     // 传送门颜色（RGB）
            .registerPortal();                          // 注册
    }
}
```

#### 7.6.3 CustomPortalBuilder 方法详解

| 方法 | 参数 | 说明 |
|------|------|------|
| `frameBlock(Block)` | `Block` | 传送门框架方块 |
| `frameBlock(Block, int metadata)` | Block + meta | 带元数据的框架方块 |
| `lightWithItem(Item)` | `Item` | 使用物品右键点火 |
| `lightWithFluid(Fluid)` | `Fluid` | 使用流体（如熔岩）点火 |
| `lightWithWater()` | 无 | 使用水点火 |
| `onlyLightInOverworld()` | 无 | 仅在主世界可点火 |
| `destDimID(Identifier)` | `Identifier` | 目标维度 ID |
| `tintColor(int r, int g, int b)` | RGB | 传送门粒子颜色 |
| `flatPortal()` | 无 | 平面传送门（类似末地传送门） |
| `setPortalSize(int width, int height)` | w, h | 设置传送门最小/最大尺寸 |
| `registerPortal()` | 无 | 完成注册 |

#### 7.6.4 传送门框架形状

Custom Portal API 默认支持矩形框架（类似下界传送门风格）。可以通过框架方块的检测来决定传送门的形状。

```java
CustomPortalBuilder.beginPortal()
    .frameBlock(Blocks.GOLD_BLOCK)
    .lightWithItem(Items.FLINT_AND_STEEL)
    .destDimID(new Identifier("mymod", "golden_dimension"))
    .tintColor(255, 215, 0)           // 金色
    .setPortalSize(2, 2)              // 最小 2x2
    .registerPortal();
```

#### 7.6.5 平面传送门（Flat Portal）

类似末地传送门或暮色森林传送门（在地上平铺）：

```java
CustomPortalBuilder.beginPortal()
    .frameBlock(Blocks.OAK_PLANKS)
    .lightWithWater()
    .destDimID(new Identifier("mymod", "water_dimension"))
    .tintColor(0, 100, 255)
    .flatPortal()                      // 平面传送门，平铺在地面
    .registerPortal();
```

#### 7.6.6 多传送门与冲突处理

重要注意事项：
- 每个 `Identifier` 只能注册一个传送门
- 如果多个模组使用相同的框架方块但不指向同一维度，Custom Portal API 会按 `destDimID` 区分
- 建议传送门框架使用稀有或不常用的方块以避免冲突

---

## 8. 网络通信

Fabric 1.20.4 使用的是旧版（pre-1.20.5）Networking API，即 `fabric-networking-api-v1`。1.20.5+ 引入的 Payload 模式在这版本中不可用。

### 8.1 Fabric 网络 API 基础

Fabric Networking API 基于频道（Channel）模式。每个数据包使用一个 `Identifier` 作为频道 ID。

**核心类：**

| 类 | 包路径 | 用途 |
|----|--------|------|
| `ServerPlayNetworking` | `net.fabricmc.fabric.api.networking.v1` | 服务端网络操作 |
| `ClientPlayNetworking` | `net.fabricmc.fabric.api.client.networking.v1` | 客户端网络操作 |
| `PacketByteBuf` | `net.minecraft.network.PacketByteBuf` | 数据包字节缓冲区 |
| `PacketByteBufs` | `net.fabricmc.fabric.api.networking.v1` | 工具类，创建 PacketByteBuf |

#### 8.1.1 定义频道

```java
public class ModNetworking {
    // 定义频道 ID
    public static final Identifier C2S_EXAMPLE_ID = new Identifier("mymod", "c2s_example");
    public static final Identifier S2C_EXAMPLE_ID = new Identifier("mymod", "s2c_example");
}
```

### 8.2 自定义 Packet：PacketByteBuf 读写

`PacketByteBuf` 是对 Netty `ByteBuf` 的封装，提供了丰富的读写方法：

#### 8.2.1 基本类型读写

```java
// 写操作 (PacketByteBuf)
buf.writeInt(42);
buf.writeBoolean(true);
buf.writeFloat(1.5f);
buf.writeDouble(3.14);
buf.writeLong(System.currentTimeMillis());
buf.writeByte(0x7F);
buf.writeShort((short) 256);

// 读操作 (PacketByteBuf)
int intVal = buf.readInt();
boolean boolVal = buf.readBoolean();
float floatVal = buf.readFloat();
double doubleVal = buf.readDouble();
long longVal = buf.readLong();
byte byteVal = buf.readByte();
short shortVal = buf.readShort();
```

#### 8.2.2 字符串和 Identifier

```java
// 写
buf.writeString("Hello, World!");                // 最大 32767 字符（默认）
buf.writeString("Short String", 64);              // 指定最大长度
buf.writeIdentifier(new Identifier("mymod", "test"));

// 读
String msg = buf.readString();                    // 默认最大长度
String shortMsg = buf.readString(64);             // 指定最大长度
Identifier id = buf.readIdentifier();
```

#### 8.2.3 位置和 NBT

```java
// BlockPos
buf.writeBlockPos(new BlockPos(100, 64, -200));
BlockPos pos = buf.readBlockPos();

// NBT
NbtCompound nbt = new NbtCompound();
nbt.putString("name", "test");
nbt.putInt("value", 42);
buf.writeNbt(nbt);
NbtCompound readNbt = buf.readNbt();

// UUID
buf.writeUuid(UUID.randomUUID());
UUID uuid = buf.readUuid();
```

#### 8.2.4 列表和数组

```java
// 写列表（手动）
List<Integer> list = List.of(1, 2, 3);
buf.writeInt(list.size());
for (int val : list) {
    buf.writeInt(val);
}

// 读列表（手动）
int size = buf.readInt();
List<Integer> result = new ArrayList<>();
for (int i = 0; i < size; i++) {
    result.add(buf.readInt());
}

// 写入 ItemStack
buf.writeItemStack(new ItemStack(Items.DIAMOND, 3));
ItemStack stack = buf.readItemStack();

// 写入 Text 组件
buf.writeText(Text.literal("Hello"));
Text text = buf.readText();
```

### 8.3 C2S（客户端→服务端）通信模式

#### 8.3.1 客户端发送数据

```java
// 在客户端代码中发送数据到服务端
public static void sendRequestToServer(BlockPos targetPos) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeBlockPos(targetPos);

    ClientPlayNetworking.send(ModNetworking.C2S_EXAMPLE_ID, buf);
}
```

#### 8.3.2 服务端注册接收器

```java
// 在 ModInitializer（通用初始化器）中注册全局接收器
@Override
public void onInitialize() {
    ServerPlayNetworking.registerGlobalReceiver(ModNetworking.C2S_EXAMPLE_ID,
        (server, player, handler, buf, responseSender) -> {
            // 注意：此回调在 Netty 网络线程中执行！
            // 需要切换到服务线程执行
            BlockPos targetPos = buf.readBlockPos();

            server.execute(() -> {
                // 在服务线程中安全操作
                if (player.getBlockPos().getSquaredDistance(targetPos) < 100) {
                    // 验证通过后执行逻辑
                    player.getWorld().setBlockState(targetPos, Blocks.DIAMOND_BLOCK.getDefaultState());
                }
            });
        }
    );
}
```

**完整的 C2S 示例——玩家请求在特定位置放置方块：**

```java
// 通用初始化器中注册
public class MyMod implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerPlayNetworking.registerGlobalReceiver(ModNetworking.C2S_EXAMPLE_ID,
            (server, player, handler, buf, responseSender) -> {
                BlockPos pos = buf.readBlockPos();
                String blockId = buf.readString();
                int count = buf.readInt();

                server.execute(() -> {
                    // 验证权限和距离
                    if (player != null && player.isCreative()) {
                        // 执行操作
                        player.getServerWorld().setBlockState(pos,
                            Registry.BLOCK.get(new Identifier(blockId)).getDefaultState());
                    }
                });
            }
        );
    }
}
```

### 8.4 S2C（服务端→客户端）通信模式

#### 8.4.1 服务端发送数据到单个玩家

```java
// 在服务端代码中发送数据到特定玩家
public static void sendToPlayer(ServerPlayerEntity player, int someValue, String message) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeInt(someValue);
    buf.writeString(message);

    ServerPlayNetworking.send(player, ModNetworking.S2C_EXAMPLE_ID, buf);
}
```

#### 8.4.2 服务端广播到所有玩家

```java
// 广播到维度中的所有玩家
public static void broadcastToAll(ServerWorld world, Text title) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeText(title);

    for (ServerPlayerEntity player : world.getPlayers()) {
        ServerPlayNetworking.send(player, ModNetworking.S2C_EXAMPLE_ID, buf);
    }
}
```

#### 8.4.3 客户端注册接收器

```java
// 在 ClientModInitializer 中注册客户端接收器
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.S2C_EXAMPLE_ID,
            (client, handler, buf, responseSender) -> {
                // 在网络线程中执行，需切换到客户端线程
                int value = buf.readInt();
                String message = buf.readString();

                client.execute(() -> {
                    // 在客户端线程中安全操作
                    if (client.player != null) {
                        client.player.sendMessage(
                            Text.literal("Received: " + message + " (" + value + ")"));
                    }
                });
            }
        );
    }
}
```

### 8.5 线程安全注意事项

这是 Fabric 网络编程中最容易出错的地方：

```
┌─────────────────────────────────────────────────────────┐
│  网络线程（Netty Worker）                                 │
│  ├─ PacketByteBuf 的读写必须在此线程完成                    │
│  ├─ 关闭 buf 前必须读取所有写入的数据                       │
│  └─ 在这里调用 context.server().execute() 切换线程           │
│                                                          │
│          │ execute()                                     │
│          ▼                                                │
│  服务端/客户端线程（Main Thread）                            │
│  ├─ 所有 Minecraft API 调用必须在此执行                      │
│  ├─ 方块操作、实体操作、玩家交互                              │
│  └─ World.setBlockState(), player.sendMessage() 等          │
└─────────────────────────────────────────────────────────┘
```

**关键规则：**

1. **`PacketByteBuf` 只能在网络回调中读取**，不要保存 `buf` 引用在线程切换后读取
2. **所有世界/实体/玩家操作必须通过 `server.execute()` 或 `client.execute()`** 切回到主线程
3. **不要在 lambda 中捕获可变的外部变量**——要么复制为 final，要么使用不可变对象
4. **对收到的数据做验证**——永远不要信任客户端发来的数据（坐标、数量等）

```java
// ❌ 错误：在线程切换后读取 buf
ServerPlayNetworking.registerGlobalReceiver(ID,
    (server, player, handler, buf, responseSender) -> {
        server.execute(() -> {
            int val = buf.readInt();  // 可能抛出异常！buf 可能已被释放
        });
    }
);

// ✅ 正确：在线程切换前读取 buf
ServerPlayNetworking.registerGlobalReceiver(ID,
    (server, player, handler, buf, responseSender) -> {
        int val = buf.readInt();  // 在网络线程中读取
        server.execute(() -> {
            // 使用已读取的值
            player.sendMessage(Text.literal("Value: " + val));
        });
    }
);
```

### 8.6 `canSend()` 的已知问题

在 Fabric 1.20.4 中，`ServerPlayNetworking.canSend()` 存在一个已知 Bug（Issue #3541），即使客户端已注册频道并成功接收数据包，服务端也可能返回 `false`。

**建议的解决方案：**
- 不依赖 `canSend()` 做逻辑判断
- 总是先发送数据包，即使 `canSend()` 返回 false
- 或者为登录/频道协商实现自定义握手流程

```java
// 避免依赖 canSend()，直接尝试发送
public static void trySend(ServerPlayerEntity player, Identifier channel, PacketByteBuf buf) {
    // 直接发送，不检查 canSend()
    ServerPlayNetworking.send(player, channel, buf);
}
```

### 8.7 完整 C2S/S2C 通信示例

```java
// ModNetworking.java — 定义所有频道
public class ModNetworking {
    public static final Identifier OPEN_CHEST = new Identifier("mymod", "open_chest");
    public static final Identifier CHEST_CONTENTS = new Identifier("mymod", "chest_contents");
    public static final Identifier SYNC_HEALTH = new Identifier("mymod", "sync_health");
}
```

```java
// 客户端 → 服务端：请求打开箱子
public class OpenChestPacket {
    public static void send(BlockPos chestPos) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBlockPos(chestPos);
        ClientPlayNetworking.send(ModNetworking.OPEN_CHEST, buf);
    }
}

// 服务端注册
ServerPlayNetworking.registerGlobalReceiver(ModNetworking.OPEN_CHEST,
    (server, player, handler, buf, responseSender) -> {
        BlockPos pos = buf.readBlockPos();
        server.execute(() -> {
            if (player.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest) {
                // 处理箱子打开逻辑
                player.openHandledScreen(chest);
            }
        });
    }
);
```

```java
// 服务端 → 客户端：同步玩家血量
public static void syncHealth(ServerPlayerEntity player, float health) {
    PacketByteBuf buf = PacketByteBufs.create();
    buf.writeFloat(health);
    ServerPlayNetworking.send(player, ModNetworking.SYNC_HEALTH, buf);
}

// 客户端注册
ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC_HEALTH,
    (client, handler, buf, responseSender) -> {
        float health = buf.readFloat();
        client.execute(() -> {
            // 更新客户端 UI 等
            System.out.println("Health synced: " + health);
        });
    }
);
```

---

## 9. 高级主题

### 9.1 能源系统：Fabric Transfer API

Fabric Transfer API 是 Fabric 官方的流体、物品和能量传输 API，基于泛型 `Storage<T>` 接口。

#### 9.1.1 核心概念

| 类/接口 | 说明 |
|---------|------|
| `Storage<T>` | 泛型存储接口，提供 `insert`、`extract`、`iterator` 方法 |
| `Transaction` | 原子操作上下文，支持模拟操作和提交/回滚 |
| `FluidVariant` | 流体的不可变表示（流体 + NBT） |
| `ItemVariant` | 物品的不可变表示（物品 + NBT） |
| `SingleVariantStorage<T>` | 存储单种 Variant 的简单实现 |

#### 9.1.2 依赖配置

```groovy
dependencies {
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"
    // Transfer API 是 Fabric API 的一部分，无需额外依赖
}
```

#### 9.1.3 Storage<T> 接口

```java
public interface Storage<T> {
    // 尝试插入指定数量的资源
    long insert(T variant, long maxAmount, TransactionContext transaction);

    // 尝试提取指定数量的资源
    long extract(T variant, long maxAmount, TransactionContext transaction);

    // 获取可用资源的迭代器
    Iterator<StorageView<T>> iterator();

    // 模拟插入（在事务中但不提交）
    default long simulateInsert(T variant, long maxAmount, TransactionContext transaction);

    // 模拟提取（在事务中但不提交）
    default long simulateExtract(T variant, long maxAmount, TransactionContext transaction);

    // 获取存储容量
    default long getCapacity(T variant);
}
```

#### 9.1.4 Transaction 原子操作

`Transaction` 是 Fabric Transfer API 的核心机制，所有存储操作必须在事务上下文中执行：

```java
// 基本事务模式
try (Transaction transaction = Transaction.openOuter()) {
    long inserted = storage.insert(variant, 100, transaction);
    long extracted = storage.extract(variant, 50, transaction);

    if (inserted > 0 && extracted > 0) {
        transaction.commit();   // 提交更改
    } else {
        transaction.abort();    // 回滚，不应用任何更改
    }
}
// 事务自动关闭
```

**事务的关键特性：**
- **原子性**：要么全部提交，要么全部回滚
- **模拟支持**：可以在事务中模拟操作而不实际执行
- **嵌套事务**：`openOuter()` 是最外层，内层通过 `TransactionContext` 传递

```java
// 模拟（验证可行性）
try (Transaction testTx = Transaction.openOuter()) {
    long canInsert = storage.simulateInsert(variant, 100, testTx);
    long canExtract = storage.simulateExtract(variant, 50, testTx);

    if (canInsert >= 100 && canExtract >= 50) {
        // 模拟通过，重新执行
        testTx.abort();  // 回滚模拟

        try (Transaction realTx = Transaction.openOuter()) {
            storage.insert(variant, 100, realTx);
            storage.extract(variant, 50, realTx);
            realTx.commit();
        }
    } else {
        testTx.abort();
    }
}
```

#### 9.1.5 FluidVariant 和 ItemVariant

```java
// FluidVariant
FluidVariant waterVariant = FluidVariant.of(Fluids.WATER);
FluidVariant lavaVariant = FluidVariant.of(Fluids.LAVA);
FluidVariant customVariant = FluidVariant.of(ModFluids.CUSTOM_FLUID, nbtCompound);

// ItemVariant
ItemVariant diamondVariant = ItemVariant.of(Items.DIAMOND);
ItemVariant stackVariant = ItemVariant.of(new ItemStack(Items.DIAMOND_SWORD));
```

#### 9.1.6 SingleVariantStorage<T> 实现

最常用的存储实现，适合快速创建自定义存储：

```java
// 流体存储实现
public class CustomFluidStorage extends SingleVariantStorage<FluidVariant> {
    public CustomFluidStorage(long capacity) {
        this.capacity = capacity;
        this.variant = FluidVariant.blank();  // 初始为空
    }

    private final long capacity;

    @Override
    protected FluidVariant getBlankVariant() {
        return FluidVariant.blank();
    }

    @Override
    protected long getCapacity(FluidVariant variant) {
        return capacity;  // 所有流体共享容量
    }

    @Override
    protected boolean canInsert(FluidVariant variant) {
        // 可插入过滤
        return !variant.isBlank() && variant.getFluid() != Fluids.LAVA;
    }

    @Override
    protected boolean canExtract(FluidVariant variant) {
        // 可提取过滤
        return !variant.isBlank();
    }

    @Override
    protected void onFinalCommit() {
        // 提交后调用，可用于标记 BlockEntity 为 dirty
        markDirty();
    }
}
```

#### 9.1.7 BlockEntity 中的存储

```java
public class EnergyStorageBlockEntity extends BlockEntity {
    // 能量存储（使用 Long 作为能量 Variant）
    private final SingleVariantStorage<FluidVariant> fluidStorage = new SingleVariantStorage<>() {
        @Override
        protected FluidVariant getBlankVariant() {
            return FluidVariant.blank();
        }

        @Override
        protected long getCapacity(FluidVariant variant) {
            return 4000;  // 4000 mb
        }

        @Override
        protected void onFinalCommit() {
            markDirty();
        }
    };

    // 暴露给外部访问的 Storage<FluidVariant>
    public Storage<FluidVariant> getFluidStorage() {
        return fluidStorage;
    }

    public EnergyStorageBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ENERGY_STORAGE, pos, state);
    }
}
```

#### 9.1.8 管道/Wrench 交互

外部机器通过 `StorageUtil` 访问方块实体的存储：

```java
// 获取方块实体的流体存储
BlockEntity be = world.getBlockEntity(pos);
if (be instanceof EnergyStorageBlockEntity storageBE) {
    Storage<FluidVariant> storage = storageBE.getFluidStorage();

    try (Transaction tx = Transaction.openOuter()) {
        long extracted = storage.extract(FluidVariant.of(Fluids.WATER), 100, tx);
        tx.commit();  // 实际提取
    }
}
```

### 9.2 自定义 GUI：AbstractContainerMenu + AbstractContainerScreen

Fabric 1.20.4 的 GUI 系统分为服务端（`ScreenHandler`/`AbstractContainerMenu`）和客户端（`HandledScreen`/`AbstractContainerScreen`）。

#### 9.2.1 架构概览

```
服务端 (Server)                         客户端 (Client)
┌──────────────────┐                  ┌──────────────────────┐
│ ScreenHandler      │ ◄───网络同步──► │ HandledScreen         │
│ (AbstractContainer │                  │ (AbstractContainer    │
│  Menu)             │                  │  Screen)              │
│                    │                  │                      │
│ - 管理 slots       │                  │ - 渲染背景/前景       │
│ - 处理 shift+点击  │                  │ - 处理鼠标/键盘输入   │
│ - 验证操作合法性    │                  │ - 同步进度条等        │
└──────────────────┘                  └──────────────────────┘
```

#### 9.2.2 注册 MenuType

**重要**：Fabric 1.20.4 中 `ScreenHandlerRegistry` 已被弃用，需要使用新的注册方式。

```java
public class ModMenuTypes {
    // 创建 ScreenHandlerType
    public static final ScreenHandlerType<CustomScreenHandler> CUSTOM_MENU =
        new ScreenHandlerType<>(CustomScreenHandler::new);

    // 如果有额外数据需要同步（ExtendedScreenHandlerType）
    public static final ScreenHandlerType<CustomExtendedHandler> CUSTOM_EXTENDED =
        new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> {
            BlockPos pos = buf.readBlockPos();
            return new CustomExtendedHandler(syncId, inventory, pos);
        });

    public static void registerAll() {
        Registry.register(Registries.SCREEN_HANDLER,
            new Identifier("mymod", "custom_menu"),
            CUSTOM_MENU
        );
        Registry.register(Registries.SCREEN_HANDLER,
            new Identifier("mymod", "custom_extended"),
            CUSTOM_EXTENDED
        );
    }
}
```

#### 9.2.3 创建 ScreenHandler（服务端逻辑）

```java
public class CustomScreenHandler extends ScreenHandler {
    private final Inventory inventory;

    // 服务端构造函数：从方块实体获取 Inventory
    public CustomScreenHandler(int syncId, Inventory playerInventory, Inventory customInventory) {
        super(ModMenuTypes.CUSTOM_MENU, syncId);
        this.inventory = customInventory;

        // 添加自定义容器的槽位（3x3 示例）
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 3; ++col) {
                this.addSlot(new Slot(customInventory, col + row * 3,
                    62 + col * 18, 17 + row * 18));
            }
        }

        // 添加玩家背包槽位
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                    8 + col * 18, 84 + row * 18));
            }
        }

        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col,
                8 + col * 18, 142));
        }
    }

    // 客户端构造函数：使用 ContainerLevelAccess
    public CustomScreenHandler(int syncId, Inventory playerInventory) {
        this(syncId, playerInventory, new SimpleInventory(9));
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        // Shift+点击逻辑
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            // 从自定义容器移到玩家背包，或反之
            if (slotIndex < 9) {
                if (!this.insertItem(originalStack, 9, 45, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.insertItem(originalStack, 0, 9, false)) {
                return ItemStack.EMPTY;
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return this.inventory.canPlayerUse(player);
    }
}
```

#### 9.2.4 创建 Screen（客户端 GUI）

```java
public class CustomScreen extends HandledScreen<CustomScreenHandler> {
    // 背景纹理
    private static final Identifier TEXTURE =
        new Identifier("mymod", "textures/gui/custom_container.png");

    public CustomScreen(CustomScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        // 设置 GUI 尺寸
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        // 渲染背景纹理
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // 渲染标题和库存标签
        context.drawText(this.textRenderer, this.title, 8, 6, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle,
            8, this.backgroundHeight - 93, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // 渲染完整画面（先背景再前景）
        super.render(context, mouseX, mouseY, delta);
        // 渲染提示文字
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }
}
```

#### 9.2.5 注册 Screen

在 `ClientModInitializer` 中注册：

```java
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // 使用 HandledScreens.register（需要 Access Widener）
        HandledScreens.register(ModMenuTypes.CUSTOM_MENU, CustomScreen::new);
    }
}
```

#### 9.2.6 打开 GUI（方块侧）

方块右击打开 GUI：

```java
public class CustomBlock extends BlockWithEntity {
    public CustomBlock(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos,
                              PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof CustomBlockEntity customBE) {
                player.openHandledScreen(customBE);  // 打开 GUI
            }
        }
        return ActionResult.SUCCESS;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new CustomBlockEntity(pos, state);
    }
}
```

方块实体实现 `NamedScreenHandlerFactory`：

```java
public class CustomBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private final SimpleInventory inventory = new SimpleInventory(9);

    public CustomBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CUSTOM_BE, pos, state);
    }

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new CustomScreenHandler(syncId, playerInventory, this.inventory);
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.mymod.custom");
    }
}
```

#### 9.2.7 使用 PropertyDelegate 同步整数

用于同步进度条、能量值等：

```java
// BlockEntity 中创建 PropertyDelegate
public class FurnaceLikeBlockEntity extends BlockEntity implements NamedScreenHandlerFactory {
    private int progress = 0;
    private int maxProgress = 100;
    private int energy = 0;
    private int maxEnergy = 10000;

    private final PropertyDelegate propertyDelegate = new PropertyDelegate() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> maxProgress;
                case 2 -> energy;
                case 3 -> maxEnergy;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> maxProgress = value;
                case 2 -> energy = value;
                case 3 -> maxEnergy = value;
            }
        }

        @Override
        public int size() {
            return 4;
        }
    };

    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new FurnaceScreenHandler(syncId, inv, this.inventory, this.propertyDelegate);
    }
}
```

ScreenHandler 中接收 `PropertyDelegate`：

```java
public class FurnaceScreenHandler extends ScreenHandler {
    public FurnaceScreenHandler(int syncId, PlayerInventory playerInventory,
                                 Inventory inventory, PropertyDelegate delegate) {
        super(ModMenuTypes.FURNACE, syncId);
        this.inventory = inventory;
        this.propertyDelegate = delegate;
        this.addProperties(delegate);  // 自动同步 PropertyDelegate
        // ... 添加槽位
    }
}
```

### 9.3 渲染进阶

#### 9.3.1 BlockEntityRenderer（BER）

用于对**方块实体**进行自定义渲染（动画、特殊模型等）。

**第一步：创建 BlockEntity**

```java
public class DisplayBlockEntity extends BlockEntity {
    public float rotation = 0;

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DISPLAY_BE, pos, state);
    }
}
```

**第二步：创建 BlockEntityRenderer**

```java
public class DisplayBlockEntityRenderer implements BlockEntityRenderer<DisplayBlockEntity> {

    public DisplayBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
    }

    @Override
    public void render(DisplayBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {

        matrices.push();
        // 移动到方块中心并抬高
        matrices.translate(0.5, 1.0, 0.5);

        // 随时间旋转
        float angle = (entity.getWorld().getTime() + tickDelta) * 2.0f;
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));

        // 计算光照
        int lightAbove = WorldRenderer.getLightmapCoordinates(
            entity.getWorld(), entity.getPos().up());

        // 渲染物品
        MinecraftClient.getInstance().getItemRenderer().renderItem(
            new ItemStack(Items.DIAMOND),
            ModelTransformationMode.GROUND,
            lightAbove,
            overlay,
            matrices,
            vertexConsumers,
            entity.getWorld(),
            0
        );

        matrices.pop();
    }
}
```

**第三步：注册 BER**

```java
// 在 ClientModInitializer 中
@Override
public void onInitializeClient() {
    BlockEntityRenderers.register(
        ModBlockEntities.DISPLAY_BE,
        DisplayBlockEntityRenderer::new
    );
}
```

#### 9.3.2 BlockEntityWithoutLevelRenderer（BEWLR）

用于**物品**的自定义渲染（替代物品模型）。通常用于需要动态渲染的物品。

```java
public class CustomItemRenderer implements BlockEntityWithoutLevelRenderer {
    public static final CustomItemRenderer INSTANCE = new CustomItemRenderer();

    @Override
    public void render(ItemStack stack, ModelTransformationMode mode,
                       MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                       int light, int overlay) {

        matrices.push();
        // 根据变换模式调整渲染
        if (mode == ModelTransformationMode.GUI) {
            matrices.translate(0.5, 0.5, 0);
        } else {
            matrices.translate(0.5, 1.0, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(
                (System.currentTimeMillis() / 20) % 360));
        }

        // 渲染物品或自定义模型
        MinecraftClient.getInstance().getItemRenderer().renderItem(
            new ItemStack(Items.NETHER_STAR),
            mode,
            light,
            overlay,
            matrices,
            vertexConsumers,
            null,
            0
        );
        matrices.pop();
    }
}
```

**绑定 BEWLR 到物品：**

```java
// 方式一：为特定物品注册 BEWLR
public class ModClient {
    public static void init() {
        // 使用 Fabric API 的 BuiltinItemRendererRegistry
        BuiltinItemRendererRegistry.INSTANCE.register(
            ModItems.CUSTOM_ITEM,
            CustomItemRenderer.INSTANCE
        );
        // 同时需要在 renderer JSON 中声明"builtin/entity"
    }
}

// 方式二：覆盖 getRenderer
// 在 Item 子类中实现
@Override
public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
    // ...
}
```

**BEWLR 的 render JSON** (`assets/mymod/models/item/custom_item.json`)：

```json
{
  "parent": "minecraft:builtin/entity",
  "gui_light": "front"
}
```

#### 9.3.3 HUD 覆盖

使用 Fabric API 的 `HudRenderCallback` 事件：

```java
// 在 ClientModInitializer 中注册
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            // 获取窗口尺寸
            int screenWidth = client.getWindow().getScaledWidth();
            int screenHeight = client.getWindow().getScaledHeight();

            // 绘制文本
            drawContext.drawText(
                client.textRenderer,
                Text.literal("§eCustom HUD Text"),
                10,
                10,
                0xFFFFFF,
                true
            );

            // 绘制矩形
            drawContext.fill(10, 30, 110, 35, 0x80FF0000);  // 半透明红色矩形

            // 绘制纹理
            Identifier hudTexture = new Identifier("mymod", "textures/gui/hud_element.png");
            drawContext.drawTexture(hudTexture, 10, 40, 0, 0, 16, 16, 16, 16);
        });
    }
}
```

**`DrawContext` 常用方法：**

| 方法 | 说明 |
|------|------|
| `drawText(TextRenderer, Text, x, y, color, shadow)` | 绘制文本 |
| `fill(x1, y1, x2, y2, color)` | 绘制填充矩形 |
| `drawTexture(Identifier, x, y, u, v, width, height, textureWidth, textureHeight)` | 绘制纹理 |
| `drawItem(ItemStack, x, y)` | 绘制物品图标 |
| `getMatrices()` | 获取矩阵栈 |

#### 9.3.4 粒子效果

**第一步：创建 ParticleType**

```java
// 在通用代码中注册
public class ModParticles {
    // 简单粒子（无额外数据）
    public static final SimpleParticleType CUSTOM_PARTICLE =
        FabricParticleTypes.simple();

    // 带速度的粒子
    public static final SimpleParticleType CUSTOM_VELOCITY_PARTICLE =
        FabricParticleTypes.simple(true);

    // 带数据的粒子
    public static final DefaultParticleType CUSTOM_DATA_PARTICLE =
        FabricParticleTypes.withDefaultData();

    public static void registerAll() {
        Registry.register(Registries.PARTICLE_TYPE,
            new Identifier("mymod", "custom_particle"),
            CUSTOM_PARTICLE
        );
        Registry.register(Registries.PARTICLE_TYPE,
            new Identifier("mymod", "custom_velocity_particle"),
            CUSTOM_VELOCITY_PARTICLE
        );
    }
}
```

**第二步：创建 Particle 类**

```java
public class CustomParticle extends SpriteBillboardParticle {

    protected CustomParticle(ClientWorld world, double x, double y, double z,
                             double velocityX, double velocityY, double velocityZ,
                             SpriteProvider spriteProvider) {
        super(world, x, y, z, velocityX, velocityY, velocityZ);

        // 设置速度
        this.velocityX = velocityX;
        this.velocityY = velocityY;
        this.velocityZ = velocityZ;

        // 设置透明度
        this.alpha = 0.8f;

        // 大小
        this.scale = 0.2f;

        // 生命周期（tick）
        this.maxAge = 40;

        // 颜色（RGB 0-1）
        this.red = 0.8f;
        this.green = 0.2f;
        this.blue = 0.5f;

        // 设置 sprite
        this.setSprite(spriteProvider);
    }

    @Override
    public void tick() {
        super.tick();
        // 自定义每 tick 更新逻辑
        this.velocityY -= 0.01;  // 重力效果
        this.scale *= 0.98f;     // 缩小效果
        this.alpha = (float) this.age / this.maxAge;  // 透明度变化
    }

    @Override
    public ParticleTextureSheet getType() {
        // 使用粒子纹理表
        return ParticleTextureSheet.PARTICLE_SHEET_TRANSLUCENT;
    }
}
```

**第三步：注册 ParticleFactory**

```java
// 在 ClientModInitializer 中注册
public class MyModClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ParticleFactoryRegistry.getInstance().register(
            ModParticles.CUSTOM_PARTICLE,
            (SpriteProvider spriteProvider) ->
                (ParticleEffect parameters, ClientWorld world,
                 double x, double y, double z,
                 double velocityX, double velocityY, double velocityZ) ->
                    new CustomParticle(world, x, y, z, velocityX, velocityY, velocityZ, spriteProvider)
        );
    }
}
```

**第四步：粒子纹理**

粒子纹理位于 `assets/mymod/textures/particle/custom_particle.png`，并为粒子定义 JSON：

```json
// assets/mymod/particles/custom_particle.json
{
  "textures": [
    "mymod:custom_particle"
  ]
}
```

**第五步：生成粒子**

```java
// 在任意代码中生成粒子（服务端或客户端）
@Override
public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
    if (world.isClient) {
        world.addParticle(
            ModParticles.CUSTOM_PARTICLE,        // ParticleType
            pos.getX() + 0.5,                    // X
            pos.getY() + 1.0,                    // Y
            pos.getZ() + 0.5,                    // Z
            random.nextGaussian() * 0.02,         // velocityX
            0.1,                                  // velocityY
            random.nextGaussian() * 0.02           // velocityZ
        );
    }
}
```

**常用 `ParticleTextureSheet` 类型：**

| 类型 | 说明 |
|------|------|
| `PARTICLE_SHEET_OPAQUE` | 不透明粒子 |
| `PARTICLE_SHEET_TRANSLUCENT` | 半透明粒子（支持透明通道） |
| `PARTICLE_SHEET_LIT` | 发光粒子（不受光照影响） |

---

### 9.4 配置系统：Cloth Config API + Mod Menu 集成

#### 9.4.1 依赖配置

**`build.gradle`：**

```groovy
repositories {
    maven { url "https://maven.shedaniel.me/" }
    maven { url "https://maven.terraformersmc.com/releases/" }
}

dependencies {
    modImplementation "net.fabricmc.fabric-api:fabric-api:${project.fabric_version}"

    // Cloth Config v12（for 1.20.4）
    modImplementation("me.shedaniel.cloth:cloth-config-fabric:${project.clothconfig_version}") {
        exclude(group: "net.fabricmc.fabric-api")
    }

    // ModMenu v8（for 1.20.4）
    modImplementation "com.terraformersmc:modmenu:${project.modmenu_version}"
}
```

**`gradle.properties`：**

```properties
fabric_version = 0.90.0+1.20.4
clothconfig_version = 12.0.119
modmenu_version = 8.2.0
```

**`fabric.mod.json`：**

```json
{
  "depends": {
    "fabricloader": ">=0.14.22",
    "fabric-api": "*",
    "minecraft": "1.20.4",
    "cloth-config": ">=12.0.0"
  },
  "suggests": {
    "modmenu": ">=8.0.0"
  }
}
```

#### 9.4.2 创建配置类

```java
package com.example.config;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.Config;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@Config(name = "mymod")  // 配置文件名: mymod.json
public class ModConfig implements ConfigData {

    // 通用设置
    @ConfigEntry.Category("general")
    @ConfigEntry.Gui.TransitiveObject
    public GeneralConfig general = new GeneralConfig();

    // 世界生成设置
    @ConfigEntry.Category("world")
    @ConfigEntry.Gui.TransitiveObject
    public WorldGenConfig worldGen = new WorldGenConfig();

    // 客户端设置
    @ConfigEntry.Category("client")
    @ConfigEntry.Gui.TransitiveObject
    public ClientConfig client = new ClientConfig();

    // 子配置类
    public static class GeneralConfig {
        @ConfigEntry.Gui.Tooltip
        public int someInteger = 42;

        @ConfigEntry.Gui.Tooltip(count = 2)
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int boundedValue = 50;

        public boolean enableFeature = true;

        @ConfigEntry.Gui.EnumHandler(option = ConfigEntry.Gui.EnumHandler.EnumDisplayOption.BUTTON)
        public SomeEnum mode = SomeEnum.MODE_A;
    }

    public static class WorldGenConfig {
        public boolean generateCustomOre = true;
        @ConfigEntry.BoundedDiscrete(min = 1, max = 64)
        public int oreVeinSize = 8;
        @ConfigEntry.BoundedDiscrete(min = 1, max = 32)
        public int oreCountPerChunk = 6;
    }

    public static class ClientConfig {
        public boolean showHud = true;
        @ConfigEntry.ColorPicker
        public int hudColor = 0xFF0000;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int hudX = 10;
        @ConfigEntry.BoundedDiscrete(min = 0, max = 100)
        public int hudY = 10;
    }

    public enum SomeEnum {
        MODE_A, MODE_B, MODE_C
    }
}
```

#### 9.4.3 初始化配置

```java
// 在 ModInitializer 中
AutoConfig.register(ModConfig.class, me.shedaniel.autoconfig.serializer.GsonConfigSerializer::new);

// 获取配置实例
public static ModConfig getConfig() {
    return AutoConfig.getConfigHolder(ModConfig.class).getConfig();
}
```

#### 9.4.4 使用配置

```java
// 在世界生成中使用配置
public class ModOreGeneration {
    public static void registerOres() {
        ModConfig config = MyMod.getConfig();
        if (config.worldGen.generateCustomOre) {
            // 使用 config.worldGen.oreVeinSize 和 config.worldGen.oreCountPerChunk
            // ...
        }
    }
}

// 在 HUD 渲染中使用配置
HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
    ModConfig config = MyMod.getConfig();
    if (config.client.showHud) {
        drawContext.drawText(
            MinecraftClient.getInstance().textRenderer,
            Text.literal("Custom HUD"),
            config.client.hudX,
            config.client.hudY,
            config.client.hudColor,
            true
        );
    }
});
```

**`@ConfigEntry` 注解选项：**

| 注解 | 用途 |
|------|------|
| `@ConfigEntry.Gui.Tooltip` | 添加工具提示（可指定行数） |
| `@ConfigEntry.BoundedDiscrete(min, max)` | 整数滑块范围 |
| `@ConfigEntry.ColorPicker` | 颜色选择器 |
| `@ConfigEntry.Gui.EnumHandler(option=BUTTON)` | 枚举切换按钮 |
| `@ConfigEntry.Gui.TransitiveObject` | 嵌套配置对象 |
| `@ConfigEntry.Gui.CollapsibleObject` | 可折叠的嵌套对象 |
| `@ConfigEntry.Gui.RequiresRestart` | 标注需要重启游戏 |
| `@ConfigEntry.Category("name")` | 配置分类 |
| `@ConfigEntry.Gui.PrefixText` | 前缀文本 |
| `@ConfigEntry.Gui.Excluded` | 从 GUI 中排除 |

#### 9.4.5 ModMenu 集成

```java
package com.example.mod;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // 使用 AutoConfig 自动生成的配置屏幕
        return parent -> AutoConfig.getConfigScreen(ModConfig.class, parent).get();
    }
}
```

在 `fabric.mod.json` 中注册 entrypoint：

```json
{
  "entrypoints": {
    "modmenu": [
      "com.example.mod.ModMenuIntegration"
    ]
  }
}
```

#### 9.4.6 手动构建配置屏幕（无需 AutoConfig）

如果不使用 AutoConfig 注解，也可以手动构建配置屏幕：

```java
public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {
            ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.translatable("title.mymod.config"));

            // 创建通用分类
            ConfigCategoryBuilder general = builder.getOrCreateCategory(
                Text.translatable("category.mymod.general"));

            general.addEntry(EntryBuilder.start()
                .startIntField(Text.translatable("option.mymod.someInteger"), 42)
                .setDefaultValue(42)
                .setSaveConsumer(newValue -> {
                    // 保存值
                    MyMod.CONFIG.someInteger = newValue;
                })
                .build());

            general.addEntry(EntryBuilder.start()
                .startBooleanToggle(Text.translatable("option.mymod.enableFeature"), true)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> {
                    MyMod.CONFIG.enableFeature = newValue;
                })
                .build());

            // 创建分类
            ConfigCategoryBuilder world = builder.getOrCreateCategory(
                Text.translatable("category.mymod.world"));

            world.addEntry(EntryBuilder.start()
                .startIntSlider(Text.translatable("option.mymod.oreVeinSize"), 8, 1, 64)
                .setDefaultValue(8)
                .setSaveConsumer(newValue -> {
                    MyMod.CONFIG.oreVeinSize = newValue;
                })
                .build());

            // 设置保存回调
            builder.setSavingRunnable(() -> {
                MyMod.saveConfig();
            });

            return builder.build();
        };
    }
}
```

---

## 参考资源

- Fabric Wiki: [Adding Features](https://wiki.fabricmc.net/tutorial:features) — ConfiguredFeature/PlacedFeature/BiomeModifications 教程
- Fabric Wiki: [Custom Dimension](https://wiki.fabricmc.net/tutorial:dimension) — 自定义维度教程
- Fabric Wiki: [Custom Trees](https://wiki.fabricmc.net/tutorial:trees) — TrunkPlacer/FoliagePlacer 教程
- Fabric Wiki: [ScreenHandler](https://wiki.fabricmc.net/tutorial:screenhandler) — GUI 容器教程
- Fabric Wiki: [BlockEntityRenderer](https://wiki.fabricmc.net/tutorial:blockentityrenderers) — 方块实体渲染器教程
- Fabric Wiki: [Transfer API](https://wiki.fabricmc.net/tutorial:transfer-api) — 流体/物品/能量传输 API
- Fabric Wiki: [Particles](https://wiki.fabricmc.net/tutorial:particles) — 自定义粒子教程
- Fabric API Javadoc: [net.fabricmc.fabric.api.networking.v1](https://maven.fabricmc.net/docs/fabric-api-0.91.1+1.20.1/net/fabricmc/fabric/api/networking/v1/package-tree.html)
- Fabric API Javadoc: [ServerPlayNetworking](https://maven.fabricmc.net/docs/fabric-api-0.89.2+1.20.2/net/fabricmc/fabric/api/networking/v1/ServerPlayNetworking.html)
- Custom Portal API: [GitHub](https://github.com/PulseBeat02/customportalapi) — Kyrptonaught 的自定义传送门库
- Cloth Config API: [DeepWiki](https://deepwiki.com/shedaniel/cloth-config/1-overview) — 配置库文档
- ModMenu: [TerraformersMC ModMenu](https://github.com/TerraformersMC/ModMenu) — 模组菜单 API
- Minecraft Wiki: [Dimension type](https://minecraft.wiki/w/Dimension_type) — 维度类型 JSON 规范
- Minecraft Wiki: [Placed feature](https://minecraft.wiki/w/Placed_feature) — 放置修饰符参考
- Misode Worldgen Generator: [https://misode.github.io/worldgen/](https://misode.github.io/worldgen/) — 可视化世界生成配置工具
