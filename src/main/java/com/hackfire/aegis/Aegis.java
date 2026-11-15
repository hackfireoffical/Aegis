package com.hackfire.aegis;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(Aegis.MODID)
public class Aegis {
    public static final String MODID = "aegis";

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);

    public static final DeferredBlock<Block> RADAR = BLOCKS.registerSimpleBlock("radar",
            BlockBehaviour.Properties.of().strength(3.0f).requiresCorrectToolForDrops());
    public static final DeferredItem<BlockItem> RADAR_ITEM = ITEMS.registerSimpleBlockItem("radar", RADAR);
    public static final DeferredItem<Item> MISSILE = ITEMS.registerSimpleItem("missile",
            new Item.Properties().stacksTo(16));

    public Aegis(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}
