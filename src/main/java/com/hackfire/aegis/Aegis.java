package com.hackfire.aegis;

import com.hackfire.aegis.block.LauncherBlock;
import com.hackfire.aegis.block.RadarBlock;
import com.hackfire.aegis.block.RadarBlockEntity;
import com.hackfire.aegis.entity.MissileEntity;
import com.hackfire.aegis.item.MissileItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

@Mod(Aegis.MODID)
public class Aegis {
    public static final String MODID = "aegis";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(BuiltInRegistries.ENTITY_TYPE, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final DeferredBlock<RadarBlock> RADAR = BLOCKS.register("radar",
            () -> new RadarBlock(BlockBehaviour.Properties.of()
                    .strength(3.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> RADAR_ITEM = ITEMS.registerSimpleBlockItem("radar", RADAR);
    public static final DeferredBlock<LauncherBlock> LAUNCHER = BLOCKS.register("launcher",
            () -> new LauncherBlock(BlockBehaviour.Properties.of()
                    .strength(4.0f).requiresCorrectToolForDrops().sound(SoundType.METAL)));
    public static final DeferredItem<BlockItem> LAUNCHER_ITEM = ITEMS.registerSimpleBlockItem("launcher", LAUNCHER);
    public static final DeferredItem<MissileItem> MISSILE = ITEMS.register("missile",
            () -> new MissileItem(new net.minecraft.world.item.Item.Properties().stacksTo(16)));

    public static final Supplier<BlockEntityType<RadarBlockEntity>> RADAR_BE = BLOCK_ENTITIES.register("radar",
            () -> BlockEntityType.Builder.of(RadarBlockEntity::new, RADAR.get()).build(null));

    public static final Supplier<EntityType<MissileEntity>> MISSILE_ENTITY = ENTITIES.register("missile",
            () -> EntityType.Builder.<MissileEntity>of(MissileEntity::new, MobCategory.MISC)
                    .sized(0.4f, 0.4f).clientTrackingRange(16).updateInterval(1).fireImmune()
                    .build("missile"));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("aegis",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.aegis"))
                    .icon(() -> MISSILE.get().getDefaultInstance())
                    .displayItems((params, output) -> {
                        output.accept(RADAR_ITEM.get());
                        output.accept(LAUNCHER_ITEM.get());
                        output.accept(MISSILE.get());
                    }).build());

    public Aegis(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        BLOCK_ENTITIES.register(modEventBus);
        ENTITIES.register(modEventBus);
        TABS.register(modEventBus);
    }
}
