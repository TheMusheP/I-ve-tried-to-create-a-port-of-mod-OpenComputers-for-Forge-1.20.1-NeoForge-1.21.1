package li.cil.oc2.common.init;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.container.ComputerCaseMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registration of OpenComputers menu types
 */
public class OCMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, OpenComputersMod.MOD_ID);
    
    public static final RegistryObject<MenuType<ComputerCaseMenu>> COMPUTER_CASE = 
        MENU_TYPES.register("computer_case", () -> 
            IForgeMenuType.create(ComputerCaseMenu::new));
}