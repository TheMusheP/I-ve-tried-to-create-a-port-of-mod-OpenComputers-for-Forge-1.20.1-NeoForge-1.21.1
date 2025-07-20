package li.cil.oc2.lua.machine;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.item.EEPROMItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemStackHandler;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;

/**
 * Boot Manager for OpenComputers
 * Handles BIOS loading and boot sequence from EEPROM
 */
public class BootManager {
    
    private final LuaMachine luaMachine;
    
    public BootManager(LuaMachine luaMachine) {
        this.luaMachine = luaMachine;
    }
    
    /**
     * Perform boot sequence
     */
    public void boot() throws LuaError {
        OpenComputersMod.LOGGER.info("Starting boot sequence for computer at {}", 
                                    luaMachine.getHost().getBlockPos());
        
        // Find EEPROM component
        IComponent eeprom = findEEPROM();
        if (eeprom == null) {
            throw new LuaError("No EEPROM found - cannot boot");
        }
        
        // Get boot code from EEPROM
        String bootCode = getBootCode(eeprom);
        if (bootCode == null || bootCode.trim().isEmpty()) {
            throw new LuaError("EEPROM contains no boot code");
        }
        
        // Initialize computer environment
        initializeEnvironment();
        
        // Load and execute boot code
        try {
            OpenComputersMod.LOGGER.debug("Executing boot code from EEPROM: {}", eeprom.getAddress());
            LuaValue chunk = luaMachine.getGlobals().load(bootCode, "=bios");
            chunk.call();
            
            OpenComputersMod.LOGGER.info("Boot sequence completed successfully");
        } catch (LuaError e) {
            OpenComputersMod.LOGGER.error("Boot failed: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            OpenComputersMod.LOGGER.error("Unexpected boot error: ", e);
            throw new LuaError("Boot error: " + e.getMessage());
        }
    }
    
    /**
     * Find EEPROM component in computer inventory
     */
    private IComponent findEEPROM() {
        ComponentRegistry registry = luaMachine.getComponentRegistry();
        
        // Look for eeprom component type
        for (String address : registry.getAddressesByType("eeprom")) {
            IComponent component = registry.getComponent(address);
            if (component != null && component.isValid()) {
                return component;
            }
        }
        
        // Also check component inventory directly
        ItemStackHandler inventory = luaMachine.getHost().getComponentInventory();
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack.getItem() instanceof EEPROMItem) {
                // Try to get component capability
                var componentOpt = stack.getCapability(li.cil.oc2.common.init.OCCapabilities.COMPONENT);
                if (componentOpt.isPresent()) {
                    return componentOpt.orElse(null);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Get boot code from EEPROM component
     */
    private String getBootCode(IComponent eeprom) {
        try {
            Object result = eeprom.invoke("get");
            return result instanceof String ? (String) result : null;
        } catch (Exception e) {
            OpenComputersMod.LOGGER.warn("Failed to read EEPROM: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Initialize computer environment before boot
     */
    private void initializeEnvironment() {
        // Set up basic computer state
        LuaValue globals = luaMachine.getGlobals();
        
        // Initialize computer table if not already done by ComputerAPI
        LuaValue computer = globals.get("computer");
        if (computer.isnil()) {
            OpenComputersMod.LOGGER.warn("Computer API not initialized - boot may fail");
        }
        
        // Initialize component table if not already done by ComponentAPI
        LuaValue component = globals.get("component");
        if (component.isnil()) {
            OpenComputersMod.LOGGER.warn("Component API not initialized - boot may fail");
        }
        
        // Set up boot environment variables
        globals.set("_OSVERSION", LuaValue.valueOf("OpenComputers 2.0"));
        globals.set("_VERSION", LuaValue.valueOf("Lua 5.3"));
        
        OpenComputersMod.LOGGER.debug("Computer environment initialized");
    }
    
    /**
     * Get boot address from EEPROM data
     */
    public String getBootAddress() {
        IComponent eeprom = findEEPROM();
        if (eeprom == null) return null;
        
        try {
            Object result = eeprom.invoke("getData");
            return result instanceof String ? (String) result : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Set boot address in EEPROM data
     */
    public boolean setBootAddress(String address) {
        IComponent eeprom = findEEPROM();
        if (eeprom == null) return false;
        
        try {
            eeprom.invoke("setData", address != null ? address : "");
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}