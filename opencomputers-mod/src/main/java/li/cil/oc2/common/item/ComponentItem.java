package li.cil.oc2.common.item;

import li.cil.oc2.api.component.ComponentException;
import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.init.OCCapabilities;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Base class for all OpenComputers component items
 * Provides IComponent capability for hardware components
 */
public class ComponentItem extends Item {
    
    private final String componentType;
    private final int tier;
    
    public ComponentItem(String componentType, int tier, Properties properties) {
        super(properties);
        this.componentType = componentType;
        this.tier = tier;
    }
    
    public String getComponentType() {
        return componentType;
    }
    
    public int getTier() {
        return tier;
    }
    
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable net.minecraft.nbt.CompoundTag nbt) {
        return new ComponentCapabilityProvider(stack, componentType, tier);
    }
    
    /**
     * Capability provider that creates IComponent instances for component items
     */
    private static class ComponentCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final String componentType;
        private final int tier;
        private final LazyOptional<IComponent> componentOptional;
        
        public ComponentCapabilityProvider(ItemStack stack, String componentType, int tier) {
            this.stack = stack;
            this.componentType = componentType;
            this.tier = tier;
            this.componentOptional = LazyOptional.of(() -> new ComponentInstance(stack, componentType, tier));
        }
        
        @Override
        public <T> LazyOptional<T> getCapability(net.minecraftforge.common.capabilities.Capability<T> cap, @Nullable net.minecraft.core.Direction side) {
            if (cap == OCCapabilities.COMPONENT) {
                return componentOptional.cast();
            }
            return LazyOptional.empty();
        }
    }
    
    /**
     * Component implementation for component items
     */
    private static class ComponentInstance implements IComponent {
        private final ItemStack stack;
        private final String componentType;
        private final int tier;
        private final String address;
        
        public ComponentInstance(ItemStack stack, String componentType, int tier) {
            this.stack = stack;
            this.componentType = componentType;
            this.tier = tier;
            
            // Generate or retrieve component address from NBT
            if (stack.hasTag() && stack.getTag().contains("ComponentAddress")) {
                this.address = stack.getTag().getString("ComponentAddress");
            } else {
                this.address = UUID.randomUUID().toString();
                if (!stack.hasTag()) {
                    stack.setTag(new net.minecraft.nbt.CompoundTag());
                }
                stack.getTag().putString("ComponentAddress", address);
            }
        }
        
        @Override
        public String getAddress() {
            return address;
        }
        
        @Override
        public String getType() {
            return componentType;
        }
        
        @Override
        public Object invoke(String method, Object... args) throws ComponentException {
            // Basic component methods - specific implementations will override
            return switch (method) {
                case "type" -> componentType;
                case "tier" -> tier;
                case "address" -> address;
                default -> throw new ComponentException("Unknown method: " + method);
            };
        }
        
        @Override
        public String[] methods() {
            return new String[]{"type", "tier", "address"};
        }
        
        @Override
        public String doc(String method) {
            return switch (method) {
                case "type" -> "Get component type";
                case "tier" -> "Get component tier";
                case "address" -> "Get component address";
                default -> "No documentation available";
            };
        }
        
        @Override
        public boolean isValid() {
            return !stack.isEmpty();
        }
    }
}