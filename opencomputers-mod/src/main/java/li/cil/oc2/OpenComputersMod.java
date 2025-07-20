package li.cil.oc2;

import li.cil.oc2.common.init.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main mod class for OpenComputers 2.0
 * Port from Forge 1.12.2 to Forge 1.20.1 and NeoForge 1.21.1
 */
@Mod(OpenComputersMod.MOD_ID)
public class OpenComputersMod {
    public static final String MOD_ID = "opencomputers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    public OpenComputersMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        
        // Register all mod components
        OCBlocks.BLOCKS.register(modEventBus);
        OCItems.ITEMS.register(modEventBus);
        OCBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        OCMenuTypes.MENU_TYPES.register(modEventBus);
        OCCapabilities.register(modEventBus);
        li.cil.oc2.common.init.OCEntityTypes.ENTITY_TYPES.register(modEventBus);
        
        LOGGER.info("OpenComputers 2.0 initializing for Forge 1.20.1...");
    }
}