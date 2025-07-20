package li.cil.oc2.common.init;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.block.ComputerCaseBlock;
import li.cil.oc2.common.block.ScreenBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registration of all OpenComputers blocks
 */
public class OCBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, OpenComputersMod.MOD_ID);
    
    // Computer Cases
    public static final RegistryObject<Block> COMPUTER_CASE_T1 = BLOCKS.register("computer_case_t1", 
        () -> new ComputerCaseBlock(1, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.METAL)));
            
    public static final RegistryObject<Block> COMPUTER_CASE_T2 = BLOCKS.register("computer_case_t2",
        () -> new ComputerCaseBlock(2, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.METAL)));
            
    public static final RegistryObject<Block> COMPUTER_CASE_T3 = BLOCKS.register("computer_case_t3",
        () -> new ComputerCaseBlock(3, BlockBehaviour.Properties.of()
            .mapColor(MapColor.METAL)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.METAL)));
    
    // Screens
    public static final RegistryObject<Block> SCREEN_T1 = BLOCKS.register("screen_t1", 
        () -> new ScreenBlock(1, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.GLASS)));
            
    public static final RegistryObject<Block> SCREEN_T2 = BLOCKS.register("screen_t2",
        () -> new ScreenBlock(2, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.GLASS)));
            
    public static final RegistryObject<Block> SCREEN_T3 = BLOCKS.register("screen_t3",
        () -> new ScreenBlock(3, BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .requiresCorrectToolForDrops()
            .strength(2.0F, 10.0F)
            .sound(SoundType.GLASS)));
    
    // TODO: Add more blocks (Cable, Adapter, etc.)
}