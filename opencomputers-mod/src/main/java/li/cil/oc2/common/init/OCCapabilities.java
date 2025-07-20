package li.cil.oc2.common.init;

import li.cil.oc2.api.component.IComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.eventbus.api.IEventBus;

/**
 * Registration of OpenComputers capabilities
 */
public class OCCapabilities {
    
    public static final Capability<IComponent> COMPONENT = 
        CapabilityManager.get(new CapabilityToken<IComponent>(){});
    
    public static void register(IEventBus bus) {
        // Capabilities are registered automatically in Forge 1.20.1
        // The capability tokens above are sufficient
    }
}