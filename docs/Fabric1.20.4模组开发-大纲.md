# Fabric 1.20.4 模组开发 — 知识大纲

> **搜集阶段**：Phase 1.1 完成 | **状态**：待用户确认
> **搜集日期**：2026-05-29 | **搜索轮次**：3 轮（中英文，覆盖环境/API/生态）

---

## 1. 开发环境搭建
- 1.1 JDK 版本选择（JDK 17 LTS）、Adoptium 下载
- 1.2 IntelliJ IDEA + MCDev 插件配置
- 1.3 Fabric Loom Gradle 插件（版本选择、构建配置）
- 1.4 项目创建方式（模板生成器 / 手动克隆示例仓库）
- 1.5 国内网络优化（BMCLAPI 镜像、阿里云 Maven、代理配置）
- 1.6 关键依赖版本（Loader 0.15.3+、Fabric API 0.92.0+、Loom 1.5+）
- 关键来源：docs.fabricmc.net/1.20.4/

## 2. 项目结构与配置
- 2.1 fabric.mod.json 逐字段解析（id/name/version/icon/environment/entrypoints/mixins/depends/suggests）
- 2.2 Entrypoints 体系（main / client / server / fabric-datagen 的加载时机和职责）
- 2.3 依赖声明方式（Maven 版本语法：`>=`、`~`、`*` 通配符）
- 2.4 映射方案选择（Yarn vs Mojang vs Parchment 对比和转换）
- 2.5 Gradle 多项目配置 / 拆分 client 和 common sources
- 关键来源：docs.fabricmc.net/1.20.4/develop/getting-started/project-structure

## 3. 核心内容开发（物品/方块）
- 3.1 自定义物品：Item 子类、Item.Settings（stacksTo/maxDamage/food/rarity/fireproof）
- 3.2 自定义方块：Block 子类、Block.Settings（strength/sound/lightLevel/requiresCorrectTool）
- 3.3 状态属性（BlockState Properties）：BooleanProperty/DirectionProperty/IntegerProperty/EnumProperty
- 3.4 实体方块（BlockEntity）：创建、注册、NBT 持久化、tick 逻辑
- 3.5 自定义物品栏分组（FabricItemGroup）
- 3.6 工具/武器/护甲：ToolMaterial / ArmorMaterial 接口实现
- 3.7 食物：FoodComponent 配置
- 关键来源：wiki.fabricmc.net/tutorial:items / tutorial:blocks

## 4. 数据生成（Data Generation）
- 4.1 DataGeneratorEntrypoint：启用配置、Provider 体系
- 4.2 模型生成（ModelProvider）：方块状态 JSON、物品模型、BlockStates
- 4.3 配方生成（RecipeProvider）：Shaped/Shapeless/Smelting/自定义配方类型
- 4.4 战利品表生成（LootTableProvider）
- 4.5 语言文件生成（LanguageProvider）
- 4.6 标签生成（TagProvider）
- 4.7 FabricDynamicRegistryProvider 与世界生成数据
- 关键来源：docs.fabricmc.net/1.20.4/develop/data-generation

## 5. 事件系统与 Mixin
- 5.1 Fabric 事件系统架构（EventFactory.createArrayBacked、InteractionResult 链式处理）
- 5.2 常用事件索引（AttackBlock / PlayerBlockBreak / UseBlock / EntityJoinWorld / ServerLifecycle 等）
- 5.3 Mixin 推荐实践（Inject/ModifyExpressionValue/WrapOperation/Accessor/Invoker）
- 5.4 Access Widener：用法 vs Mixin Accessor 的取舍
- 5.5 Fabric 环境下 Mixin 的调试（@Debug(export=true)、--mixin.verbose）
- 5.6 MixinExtras 库：@WrapOperation、@ModifyExpressionValue
- 关键来源：docs.fabricmc.net/1.20.4/develop/events / wiki.fabricmc.net/tutorial:mixin

## 6. 自定义实体与 AI
- 6.1 EntityType 注册（FabricEntityTypeBuilder）
- 6.2 AI Goal 系统（SwimGoal / MeleeAttackGoal / WanderAroundGoal / LookAtEntityGoal）
- 6.3 生成规则（SpawnRestriction、BiomeModifications）
- 6.4 实体渲染（EntityRenderer + EntityModelLayer 注册）
- 6.5 实体属性（EntityAttributes、createLivingAttributes）

## 7. 世界生成与维度
- 7.1 三层架构：ConfiguredFeature → PlacedFeature → BiomeModification
- 7.2 自定义矿物/树木/结构生成
- 7.3 自定义生物群系（JSON + 数据生成）
- 7.4 自定义维度（RegistryKey + dimension_type JSON + dimension JSON）
- 7.5 传送门：Custom Portal API 使用
- 关键来源：wiki.fabricmc.net/tutorial:ores / tutorial:trees / tutorial:dimensions

## 8. 网络通信
- 8.1 Fabric 网络 API 基础（ServerPlayNetworking / ClientPlayNetworking）
- 8.2 Packet 编解码（PacketByteBuf 读写）
- 8.3 C2S / S2C 通信模式
- 8.4 线程安全注意事项（context.server().execute()）
- 关键来源：docs.fabricmc.net/1.20.4/develop/networking

## 9. 高级主题
- 9.1 能源系统：Fabric Transfer API（Storage<T>、Transaction、FluidVariant）
- 9.2 自定义 GUI：AbstractContainerMenu + AbstractContainerScreen + MenuType
- 9.3 渲染进阶：BlockEntityRenderer（BER）、HUD 覆盖、粒子效果
- 9.4 配置系统：Cloth Config API + Mod Menu 集成
- 9.5 数据附加（Attachment）：Fabric API 1.20.5 之前的替代方案

## 10. 打包发布
- 10.1 Gradle build 与 JAR 结构
- 10.2 上传到 CurseForge / Modrinth
- 10.3 GitHub Actions CI/CD 自动发布
- 10.4 多版本维护策略

## 11. 常见陷阱与最佳实践
- 11.1 Client/Server 分离错误（不可在服务端引用 client 类）
- 11.2 Mixin 冲突预防（避免 @Overwrite、优先@WrapOperation）
- 11.3 资源路径规范（全小写 modid、assets 结构）
- 11.4 注册时机与顺序
- 11.5 版本兼容性管理
- 11.6 性能考量（事件监听器轻量化、避免每 tick 频繁 NBT 读写）

## 12. 学习资源
- 12.1 官方文档：docs.fabricmc.net/1.20.4/
- 12.2 Kaupenjoe YouTube 教程（Fabric 1.20.x 播放列表）
- 12.3 Flandre芙兰 B 站视频教程（Fabric 1.20.1，35 集）
- 12.4 Fabric Wiki 中文版
- 12.5 GitHub 示例代码仓库

---

## 大纲总结

| 维度 | 数据 |
|------|------|
| 子主题总数 | **12 个**（覆盖环境→配置→内容→数据生成→事件→实体→世界→网络→高级→发布→陷阱→资源） |
| 预计 Agent 子代理分配 | **4 个**（每人 3 个子主题） |
| 核心权威来源 | docs.fabricmc.net/1.20.4/（官网文档） |
