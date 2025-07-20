package li.cil.oc2.common.item;

import li.cil.oc2.api.component.ComponentException;
import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.init.OCCapabilities;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * EEPROM Item - stores BIOS code and boot configuration
 * Provides persistent storage for Lua boot code and boot address
 */
public class EEPROMItem extends Item {
    
    private static final String TAG_CODE = "Code";
    private static final String TAG_DATA = "Data";
    private static final String TAG_READONLY = "ReadOnly";
    private static final String TAG_LABEL = "Label";
    private static final String TAG_ADDRESS = "ComponentAddress";
    private static final String TAG_CHECKSUM = "Checksum";
    
    public EEPROMItem(Properties properties) {
        super(properties);
    }
    
    @Override
    public @Nullable ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new EEPROMCapabilityProvider(stack);
    }
    
    /**
     * Get Lua code stored in EEPROM
     */
    public static String getCode(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_CODE) : "";
    }
    
    /**
     * Set Lua code in EEPROM
     */
    public static void setCode(ItemStack stack, String code) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_CODE, code);
        updateChecksum(stack);
    }
    
    /**
     * Get data string (boot address, etc.)
     */
    public static String getData(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_DATA) : "";
    }
    
    /**
     * Set data string
     */
    public static void setData(ItemStack stack, String data) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_DATA, data);
        updateChecksum(stack);
    }
    
    /**
     * Check if EEPROM is read-only
     */
    public static boolean isReadOnly(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(TAG_READONLY);
    }
    
    /**
     * Set read-only status
     */
    public static void setReadOnly(ItemStack stack, boolean readOnly) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putBoolean(TAG_READONLY, readOnly);
        updateChecksum(stack);
    }
    
    /**
     * Get EEPROM label
     */
    public static String getLabel(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null ? tag.getString(TAG_LABEL) : "EEPROM";
    }
    
    /**
     * Set EEPROM label
     */
    public static void setLabel(ItemStack stack, String label) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(TAG_LABEL, label);
        updateChecksum(stack);
    }
    
    /**
     * Get component address
     */
    public static String getAddress(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(TAG_ADDRESS)) {
            return tag.getString(TAG_ADDRESS);
        } else {
            // Generate new address
            String address = UUID.randomUUID().toString();
            CompoundTag newTag = stack.getOrCreateTag();
            newTag.putString(TAG_ADDRESS, address);
            return address;
        }
    }
    
    /**
     * Update checksum for data integrity
     */
    private static void updateChecksum(ItemStack stack) {
        try {
            CompoundTag tag = stack.getOrCreateTag();
            String data = tag.getString(TAG_CODE) + tag.getString(TAG_DATA) + tag.getString(TAG_LABEL);
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            tag.putString(TAG_CHECKSUM, sb.toString());
        } catch (Exception e) {
            // Ignore checksum errors
        }
    }
    
    /**
     * Capability provider for EEPROM component functionality
     */
    private static class EEPROMCapabilityProvider implements ICapabilityProvider {
        private final ItemStack stack;
        private final LazyOptional<IComponent> componentOptional;
        
        public EEPROMCapabilityProvider(ItemStack stack) {
            this.stack = stack;
            this.componentOptional = LazyOptional.of(() -> new EEPROMComponent(stack));
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
     * EEPROM component implementation
     */
    private static class EEPROMComponent implements IComponent {
        private final ItemStack stack;
        
        public EEPROMComponent(ItemStack stack) {
            this.stack = stack;
        }
        
        @Override
        public String getAddress() {
            return EEPROMItem.getAddress(stack);
        }
        
        @Override
        public String getType() {
            return "eeprom";
        }
        
        @Override
        public Object invoke(String method, Object... args) throws ComponentException {
            return switch (method) {
                case "get" -> EEPROMItem.getCode(stack);
                case "set" -> {
                    if (EEPROMItem.isReadOnly(stack)) {
                        throw new ComponentException("EEPROM is read-only");
                    }
                    if (args.length > 0 && args[0] instanceof String code) {
                        EEPROMItem.setCode(stack, code);
                        yield null;
                    }
                    throw new ComponentException("Invalid arguments for set");
                }
                case "getLabel" -> EEPROMItem.getLabel(stack);
                case "setLabel" -> {
                    if (args.length > 0 && args[0] instanceof String label) {
                        EEPROMItem.setLabel(stack, label);
                        yield null;
                    }
                    throw new ComponentException("Invalid arguments for setLabel");
                }
                case "getData" -> EEPROMItem.getData(stack);
                case "setData" -> {
                    if (args.length > 0 && args[0] instanceof String data) {
                        EEPROMItem.setData(stack, data);
                        yield null;
                    }
                    throw new ComponentException("Invalid arguments for setData");
                }
                case "makeReadonly" -> {
                    if (args.length > 0 && args[0] instanceof String checksum) {
                        // TODO: Implement checksum verification
                        EEPROMItem.setReadOnly(stack, true);
                        yield null;
                    }
                    throw new ComponentException("Invalid arguments for makeReadonly");
                }
                case "getSize" -> 4096; // 4KB EEPROM size
                case "getDataSize" -> 256; // 256 bytes data size
                case "getChecksum" -> {
                    CompoundTag tag = stack.getTag();
                    yield tag != null ? tag.getString(TAG_CHECKSUM) : "";
                }
                default -> throw new ComponentException("Unknown method: " + method);
            };
        }
        
        @Override
        public String[] methods() {
            return new String[]{"get", "set", "getLabel", "setLabel", "getData", "setData", 
                              "makeReadonly", "getSize", "getDataSize", "getChecksum"};
        }
        
        @Override
        public String doc(String method) {
            return switch (method) {
                case "get" -> "Get the currently stored code";
                case "set" -> "Set new code. Returns true on success";
                case "getLabel" -> "Get the label of the EEPROM";
                case "setLabel" -> "Set the label of the EEPROM";
                case "getData" -> "Get the data string";
                case "setData" -> "Set the data string";
                case "makeReadonly" -> "Make the EEPROM read-only";
                case "getSize" -> "Get the storage capacity of the EEPROM";
                case "getDataSize" -> "Get the data storage capacity";
                case "getChecksum" -> "Get the checksum of the stored data";
                default -> "No documentation available";
            };
        }
        
        @Override
        public boolean isValid() {
            return !stack.isEmpty();
        }
    }
}