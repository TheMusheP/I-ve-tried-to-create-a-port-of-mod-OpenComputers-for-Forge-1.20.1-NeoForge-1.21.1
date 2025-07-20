package li.cil.oc2.common.entity;

import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.init.OCCapabilities;
import li.cil.oc2.lua.machine.ComponentRegistry;
import li.cil.oc2.lua.machine.LuaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Robot Entity - Mobile computer with inventory and tools
 * Programmable through Lua with movement and interaction capabilities
 */
public class RobotEntity extends PathfinderMob {
    
    private static final EntityDataAccessor<Boolean> RUNNING = 
        SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> ENERGY = 
        SynchedEntityData.defineId(RobotEntity.class, EntityDataSerializers.INT);
    
    // Robot components
    private final ItemStackHandler inventory;
    private final ItemStackHandler componentInventory;
    private final EnergyStorage energyStorage;
    private final LazyOptional<ItemStackHandler> inventoryOptional;
    private final LazyOptional<IEnergyStorage> energyOptional;
    
    // Lua integration
    private LuaMachine luaMachine;
    private ComponentRegistry componentRegistry;
    private RobotComponent robotComponent;
    
    // Robot state
    private Direction facing = Direction.NORTH;
    private boolean isRunning = false;
    private int selectedSlot = 0;
    
    // Energy consumption
    private static final int ENERGY_PER_TICK = 5;
    private static final int ENERGY_PER_MOVE = 50;
    private static final int ENERGY_PER_TURN = 25;
    private static final int ENERGY_PER_USE = 30;
    
    public RobotEntity(EntityType<? extends RobotEntity> type, Level level) {
        super(type, level);
        
        // Initialize inventory (16 slots)
        this.inventory = new ItemStackHandler(16) {
            @Override
            protected void onContentsChanged(int slot) {
                RobotEntity.this.onInventoryChanged();
            }
        };
        this.inventoryOptional = LazyOptional.of(() -> inventory);
        
        // Initialize component inventory (8 slots)
        this.componentInventory = new ItemStackHandler(8) {
            @Override
            protected void onContentsChanged(int slot) {
                RobotEntity.this.onComponentInventoryChanged();
            }
            
            @Override
            public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                return stack.getCapability(OCCapabilities.COMPONENT).isPresent();
            }
        };
        
        // Initialize energy storage (50,000 RF)
        this.energyStorage = new EnergyStorage(50000, 5000, 0);
        this.energyOptional = LazyOptional.of(() -> energyStorage);
        
        // Initialize Lua components
        this.componentRegistry = new ComponentRegistry();
        this.luaMachine = new LuaMachine(null, componentRegistry);
        this.robotComponent = new RobotComponent(this);
        
        // Add robot component to registry
        componentRegistry.addComponent(robotComponent);
    }
    
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.1);
    }
    
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RUNNING, false);
        this.entityData.define(ENERGY, 0);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!level().isClientSide) {
            // Update synced data
            entityData.set(RUNNING, isRunning);
            entityData.set(ENERGY, energyStorage.getEnergyStored());
            
            // Tick Lua machine if running and has energy
            if (isRunning && hasEnoughEnergy(ENERGY_PER_TICK)) {
                energyStorage.extractEnergy(ENERGY_PER_TICK, false);
                luaMachine.tick();
            } else if (!hasEnoughEnergy(ENERGY_PER_TICK)) {
                stop();
            }
            
            // Scan components periodically
            if (tickCount % 20 == 0) {
                scanComponents();
            }
        }
    }
    
    @Override
    public InteractionResult interactAt(Player player, net.minecraft.world.phys.Vec3 vec, InteractionHand hand) {
        if (!level().isClientSide) {
            // Open robot GUI
            // TODO: Implement robot GUI
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.CONSUME;
    }
    
    // Robot movement methods
    public boolean moveForward() {
        if (!hasEnoughEnergy(ENERGY_PER_MOVE)) return false;
        
        BlockPos newPos = blockPosition().relative(facing);
        if (canMoveToPosition(newPos)) {
            energyStorage.extractEnergy(ENERGY_PER_MOVE, false);
            setPos(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            return true;
        }
        return false;
    }
    
    public boolean moveBack() {
        if (!hasEnoughEnergy(ENERGY_PER_MOVE)) return false;
        
        BlockPos newPos = blockPosition().relative(facing.getOpposite());
        if (canMoveToPosition(newPos)) {
            energyStorage.extractEnergy(ENERGY_PER_MOVE, false);
            setPos(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            return true;
        }
        return false;
    }
    
    public boolean moveUp() {
        if (!hasEnoughEnergy(ENERGY_PER_MOVE)) return false;
        
        BlockPos newPos = blockPosition().above();
        if (canMoveToPosition(newPos)) {
            energyStorage.extractEnergy(ENERGY_PER_MOVE, false);
            setPos(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            return true;
        }
        return false;
    }
    
    public boolean moveDown() {
        if (!hasEnoughEnergy(ENERGY_PER_MOVE)) return false;
        
        BlockPos newPos = blockPosition().below();
        if (canMoveToPosition(newPos)) {
            energyStorage.extractEnergy(ENERGY_PER_MOVE, false);
            setPos(newPos.getX() + 0.5, newPos.getY(), newPos.getZ() + 0.5);
            return true;
        }
        return false;
    }
    
    public boolean turnLeft() {
        if (!hasEnoughEnergy(ENERGY_PER_TURN)) return false;
        
        energyStorage.extractEnergy(ENERGY_PER_TURN, false);
        facing = facing.getCounterClockWise();
        setYRot(facing.toYRot());
        return true;
    }
    
    public boolean turnRight() {
        if (!hasEnoughEnergy(ENERGY_PER_TURN)) return false;
        
        energyStorage.extractEnergy(ENERGY_PER_TURN, false);
        facing = facing.getClockWise();
        setYRot(facing.toYRot());
        return true;
    }
    
    public boolean turnAround() {
        if (!hasEnoughEnergy(ENERGY_PER_TURN * 2)) return false;
        
        energyStorage.extractEnergy(ENERGY_PER_TURN * 2, false);
        facing = facing.getOpposite();
        setYRot(facing.toYRot());
        return true;
    }
    
    // Block interaction
    public boolean swing() {
        if (!hasEnoughEnergy(ENERGY_PER_USE)) return false;
        
        BlockPos pos = blockPosition().relative(facing);
        BlockState state = level().getBlockState(pos);
        
        if (!state.isAir()) {
            energyStorage.extractEnergy(ENERGY_PER_USE, false);
            
            // Break block with current tool
            ItemStack tool = getSelectedItem();
            level().destroyBlock(pos, true);
            
            // Damage tool if applicable
            if (!tool.isEmpty() && tool.isDamageableItem()) {
                tool.hurt(1, random, null);
            }
            
            return true;
        }
        return false;
    }
    
    public boolean use() {
        if (!hasEnoughEnergy(ENERGY_PER_USE)) return false;
        
        BlockPos pos = blockPosition().relative(facing);
        BlockState state = level().getBlockState(pos);
        
        energyStorage.extractEnergy(ENERGY_PER_USE, false);
        
        // Use current item on block
        ItemStack item = getSelectedItem();
        if (!item.isEmpty()) {
            // TODO: Implement item usage
        }
        
        return true;
    }
    
    // Inventory management
    public ItemStack getSelectedItem() {
        return inventory.getStackInSlot(selectedSlot);
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < inventory.getSlots()) {
            this.selectedSlot = slot;
        }
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public ItemStackHandler getInventory() {
        return inventory;
    }
    
    public ItemStackHandler getComponentInventory() {
        return componentInventory;
    }
    
    // Energy management
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }
    
    private boolean hasEnoughEnergy(int amount) {
        return energyStorage.getEnergyStored() >= amount;
    }
    
    // Robot control
    public void start() {
        if (!isRunning && hasEnoughEnergy(ENERGY_PER_TICK)) {
            isRunning = true;
            luaMachine.start();
        }
    }
    
    public void stop() {
        if (isRunning) {
            isRunning = false;
            luaMachine.stop();
        }
    }
    
    public void restart() {
        stop();
        start();
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    public LuaMachine getLuaMachine() {
        return luaMachine;
    }
    
    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
    
    // Utility methods
    private boolean canMoveToPosition(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        BlockState stateAbove = level().getBlockState(pos.above());
        
        return state.isAir() && stateAbove.isAir() && 
               pos.getY() >= level().getMinBuildHeight() && 
               pos.getY() < level().getMaxBuildHeight();
    }
    
    private void onInventoryChanged() {
        // Handle inventory changes
    }
    
    private void onComponentInventoryChanged() {
        scanComponents();
    }
    
    private void scanComponents() {
        componentRegistry.clear();
        
        // Re-add robot component
        componentRegistry.addComponent(robotComponent);
        
        // Scan component inventory
        for (int i = 0; i < componentInventory.getSlots(); i++) {
            ItemStack stack = componentInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                LazyOptional<IComponent> componentOpt = stack.getCapability(OCCapabilities.COMPONENT);
                componentOpt.ifPresent(component -> {
                    componentRegistry.addComponent(component);
                });
            }
        }
        
        // Notify Lua machine
        if (luaMachine != null) {
            luaMachine.onComponentsChanged();
        }
    }
    
    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        
        tag.put("Inventory", inventory.serializeNBT());
        tag.put("ComponentInventory", componentInventory.serializeNBT());
        tag.put("Energy", energyStorage.serializeNBT());
        tag.putInt("Facing", facing.get3DDataValue());
        tag.putBoolean("IsRunning", isRunning);
        tag.putInt("SelectedSlot", selectedSlot);
        
        if (luaMachine != null) {
            tag.put("LuaMachine", luaMachine.serializeNBT());
        }
    }
    
    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        
        if (tag.contains("Inventory")) {
            inventory.deserializeNBT(tag.getCompound("Inventory"));
        }
        
        if (tag.contains("ComponentInventory")) {
            componentInventory.deserializeNBT(tag.getCompound("ComponentInventory"));
        }
        
        if (tag.contains("Energy")) {
            energyStorage.deserializeNBT(tag.get("Energy"));
        }
        
        facing = Direction.from3DDataValue(tag.getInt("Facing"));
        isRunning = tag.getBoolean("IsRunning");
        selectedSlot = tag.getInt("SelectedSlot");
        
        if (tag.contains("LuaMachine") && luaMachine != null) {
            luaMachine.deserializeNBT(tag.getCompound("LuaMachine"));
        }
        
        // Rescan components after loading
        scanComponents();
    }
    
    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER) {
            return inventoryOptional.cast();
        }
        if (cap == net.minecraftforge.common.capabilities.ForgeCapabilities.ENERGY) {
            return energyOptional.cast();
        }
        return super.getCapability(cap, side);
    }
    
    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inventoryOptional.invalidate();
        energyOptional.invalidate();
    }
}