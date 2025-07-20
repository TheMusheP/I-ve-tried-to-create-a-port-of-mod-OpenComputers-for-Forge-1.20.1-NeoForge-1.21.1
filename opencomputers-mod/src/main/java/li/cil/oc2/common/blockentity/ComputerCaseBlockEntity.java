package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.block.ComputerCaseBlock;
import li.cil.oc2.common.container.ComputerCaseMenu;
import li.cil.oc2.common.init.OCBlockEntities;
import li.cil.oc2.common.init.OCCapabilities;
import li.cil.oc2.lua.machine.ComponentRegistry;
import li.cil.oc2.lua.machine.LuaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Computer Case Block Entity - Core computer housing with component slots and Lua machine
 * Handles component inventory, energy storage, and Lua execution
 */
public class ComputerCaseBlockEntity extends BlockEntity implements MenuProvider {
    
    private static final int ENERGY_PER_TICK = 10; // RF consumed per tick when running
    
    private final int tier;
    private final int componentSlots;
    private final int maxEnergy;
    
    // Component inventory
    private final ItemStackHandler componentInventory;
    private final LazyOptional<ItemStackHandler> inventoryOptional;
    
    // Energy storage
    private final EnergyStorage energyStorage;
    private final LazyOptional<IEnergyStorage> energyOptional;
    
    // Lua machine and component management
    private LuaMachine luaMachine;
    private ComponentRegistry componentRegistry;
    private boolean isRunning = false;
    private int ticksSinceLastComponentScan = 0;
    
    public ComputerCaseBlockEntity(BlockPos pos, BlockState state) {
        super(OCBlockEntities.COMPUTER_CASE.get(), pos, state);
        
        // Determine tier from block
        if (state.getBlock() instanceof ComputerCaseBlock caseBlock) {
            this.tier = caseBlock.getTier();
        } else {
            this.tier = 1;
        }
        
        // Configure based on tier
        this.componentSlots = switch (tier) {
            case 1 -> 8;   // T1: 8 component slots
            case 2 -> 12;  // T2: 12 component slots
            case 3 -> 16;  // T3: 16 component slots
            default -> 8;
        };
        
        this.maxEnergy = 10000 * tier; // 10k/20k/30k RF
        
        // Initialize inventory
        this.componentInventory = new ItemStackHandler(componentSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                ComputerCaseBlockEntity.this.setChanged();
                ComputerCaseBlockEntity.this.scheduleComponentScan();
            }
            
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                // Only allow items with IComponent capability
                return stack.getCapability(OCCapabilities.COMPONENT).isPresent();
            }
            
            @Override
            public int getSlotLimit(int slot) {
                return 1; // Only one component per slot
            }
        };
        this.inventoryOptional = LazyOptional.of(() -> componentInventory);
        
        // Initialize energy storage
        this.energyStorage = new EnergyStorage(maxEnergy, maxEnergy / 10, 0) {
            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                int extracted = super.extractEnergy(maxExtract, simulate);
                if (extracted > 0 && !simulate) {
                    ComputerCaseBlockEntity.this.setChanged();
                }
                return extracted;
            }
            
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                int received = super.receiveEnergy(maxReceive, simulate);
                if (received > 0 && !simulate) {
                    ComputerCaseBlockEntity.this.setChanged();
                }
                return received;
            }
        };
        this.energyOptional = LazyOptional.of(() -> energyStorage);
        
        // Initialize component registry
        this.componentRegistry = new ComponentRegistry();
        
        // Initialize Lua machine
        this.luaMachine = new LuaMachine(this, componentRegistry);
    }
    
    public int getTier() {
        return tier;
    }
    
    public ItemStackHandler getComponentInventory() {
        return componentInventory;
    }
    
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    public LuaMachine getLuaMachine() {
        return luaMachine;
    }
    
    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public void start() {
        if (!isRunning && hasEnoughEnergy()) {
            isRunning = true;
            luaMachine.start();
            setChanged();
        }
    }
    
    public void stop() {
        if (isRunning) {
            isRunning = false;
            luaMachine.stop();
            setChanged();
        }
    }
    
    public void restart() {
        stop();
        start();
    }
    
    private boolean hasEnoughEnergy() {
        return energyStorage.getEnergyStored() >= ENERGY_PER_TICK;
    }
    
    private void scheduleComponentScan() {
        ticksSinceLastComponentScan = 0;
    }
    
    /**
     * Scan component inventory and update component registry
     */
    private void scanComponents() {
        componentRegistry.clear();
        
        for (int i = 0; i < componentInventory.getSlots(); i++) {
            ItemStack stack = componentInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LazyOptional<IComponent> componentOpt = stack.getCapability(OCCapabilities.COMPONENT);
                componentOpt.ifPresent(component -> {
                    componentRegistry.addComponent(component);
                });
            }
        }
        
        // Notify Lua machine of component changes
        if (luaMachine != null) {
            luaMachine.onComponentsChanged();
        }
    }
    
    // Static tick methods for BlockEntityTicker
    public static void clientTick(Level level, BlockPos pos, BlockState state, ComputerCaseBlockEntity blockEntity) {
        // Client-side ticking (if needed)
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, ComputerCaseBlockEntity blockEntity) {
        blockEntity.tick();
    }
    
    private void tick() {
        if (level == null || level.isClientSide) return;
        
        // Scan components periodically or when inventory changes
        ticksSinceLastComponentScan++;
        if (ticksSinceLastComponentScan >= 20) { // Scan every second
            scanComponents();
            ticksSinceLastComponentScan = 0;
        }
        
        // Tick Lua machine if running
        if (isRunning) {
            if (hasEnoughEnergy()) {
                // Consume energy
                energyStorage.extractEnergy(ENERGY_PER_TICK, false);
                
                // Tick Lua machine
                luaMachine.tick();
            } else {
                // Stop if out of energy
                stop();
            }
        }
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        if (tag.contains("Inventory")) {
            componentInventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(tag.get("Energy"));
        }
        
        isRunning = tag.getBoolean("IsRunning");
        
        if (tag.contains("LuaMachine")) {
            luaMachine.deserializeNBT(tag.getCompound("LuaMachine"));
        }
        
        // Schedule component scan after loading
        scheduleComponentScan();
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        tag.put("Inventory", componentInventory.serializeNBT());
        tag.put("Energy", energyStorage.serializeNBT());
        tag.putBoolean("IsRunning", isRunning);
        tag.put("LuaMachine", luaMachine.serializeNBT());
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryOptional.invalidate();
        energyOptional.invalidate();
    }
    
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        if (cap == ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return super.getCapability(cap, side);
    }
    
    // MenuProvider implementation
    @Override
    public Component getDisplayName() {
        return Component.translatable("container.opencomputers.computer_case");
    }
    
    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new ComputerCaseMenu(windowId, playerInventory, this);
    }
}