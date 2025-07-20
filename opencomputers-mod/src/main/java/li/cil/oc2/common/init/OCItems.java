package li.cil.oc2.common.init;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.item.ComponentItem;
import li.cil.oc2.common.item.EEPROMItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registration of all OpenComputers items
 */
public class OCItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, OpenComputersMod.MOD_ID);
    
    // Block Items
    public static final RegistryObject<Item> COMPUTER_CASE_T1_ITEM = ITEMS.register("computer_case_t1", 
        () -> new BlockItem(OCBlocks.COMPUTER_CASE_T1.get(), new Item.Properties()));
    public static final RegistryObject<Item> COMPUTER_CASE_T2_ITEM = ITEMS.register("computer_case_t2", 
        () -> new BlockItem(OCBlocks.COMPUTER_CASE_T2.get(), new Item.Properties()));
    public static final RegistryObject<Item> COMPUTER_CASE_T3_ITEM = ITEMS.register("computer_case_t3", 
        () -> new BlockItem(OCBlocks.COMPUTER_CASE_T3.get(), new Item.Properties()));
    
    // Screen Block Items
    public static final RegistryObject<Item> SCREEN_T1_ITEM = ITEMS.register("screen_t1", 
        () -> new BlockItem(OCBlocks.SCREEN_T1.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCREEN_T2_ITEM = ITEMS.register("screen_t2", 
        () -> new BlockItem(OCBlocks.SCREEN_T2.get(), new Item.Properties()));
    public static final RegistryObject<Item> SCREEN_T3_ITEM = ITEMS.register("screen_t3", 
        () -> new BlockItem(OCBlocks.SCREEN_T3.get(), new Item.Properties()));
    
    // Component Items
    
    // CPU Components
    public static final RegistryObject<Item> CPU_T1 = ITEMS.register("cpu_t1",
        () -> new ComponentItem("cpu", 1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CPU_T2 = ITEMS.register("cpu_t2",
        () -> new ComponentItem("cpu", 2, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> CPU_T3 = ITEMS.register("cpu_t3",
        () -> new ComponentItem("cpu", 3, new Item.Properties().stacksTo(1)));
    
    // Memory Components
    public static final RegistryObject<Item> MEMORY_T1 = ITEMS.register("memory_t1",
        () -> new ComponentItem("memory", 1, new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> MEMORY_T2 = ITEMS.register("memory_t2",
        () -> new ComponentItem("memory", 2, new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> MEMORY_T3 = ITEMS.register("memory_t3",
        () -> new ComponentItem("memory", 3, new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> MEMORY_T4 = ITEMS.register("memory_t4",
        () -> new ComponentItem("memory", 4, new Item.Properties().stacksTo(2)));
    public static final RegistryObject<Item> MEMORY_T5 = ITEMS.register("memory_t5",
        () -> new ComponentItem("memory", 5, new Item.Properties().stacksTo(2)));
    public static final RegistryObject<Item> MEMORY_T6 = ITEMS.register("memory_t6",
        () -> new ComponentItem("memory", 6, new Item.Properties().stacksTo(1)));
    
    // Graphics Cards
    public static final RegistryObject<Item> GRAPHICS_CARD_T1 = ITEMS.register("graphics_card_t1",
        () -> new ComponentItem("gpu", 1, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRAPHICS_CARD_T2 = ITEMS.register("graphics_card_t2",
        () -> new ComponentItem("gpu", 2, new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GRAPHICS_CARD_T3 = ITEMS.register("graphics_card_t3",
        () -> new ComponentItem("gpu", 3, new Item.Properties().stacksTo(1)));
    
    // EEPROM
    public static final RegistryObject<Item> EEPROM = ITEMS.register("eeprom",
        () -> new EEPROMItem(new Item.Properties().stacksTo(1)));
    
    // TODO: Add more component items
}