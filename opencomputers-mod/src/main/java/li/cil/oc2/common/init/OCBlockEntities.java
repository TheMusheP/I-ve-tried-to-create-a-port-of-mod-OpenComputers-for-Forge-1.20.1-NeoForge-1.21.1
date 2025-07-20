package li.cil.oc2.common.init;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc2.common.blockentity.ScreenBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registration of all OpenComputers block entities
 */
public class OCBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = 
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, OpenComputersMod.MOD_ID);
    
    public static final RegistryObject<BlockEntityType<ComputerCaseBlockEntity>> COMPUTER_CASE = 
        BLOCK_ENTITIES.register("computer_case", () -> 
            BlockEntityType.Builder.of(ComputerCaseBlockEntity::new,
                OCBlocks.COMPUTER_CASE_T1.get(),
                OCBlocks.COMPUTER_CASE_T2.get(),
                OCBlocks.COMPUTER_CASE_T3.get()
            ).build(null));
    
    public static final RegistryObject<BlockEntityType<ScreenBlockEntity>> SCREEN = 
        BLOCK_ENTITIES.register("screen", () -> 
            BlockEntityType.Builder.of(ScreenBlockEntity::new,
                OCBlocks.SCREEN_T1.get(),
                OCBlocks.SCREEN_T2.get(),
                OCBlocks.SCREEN_T3.get()
            ).build(null));
}