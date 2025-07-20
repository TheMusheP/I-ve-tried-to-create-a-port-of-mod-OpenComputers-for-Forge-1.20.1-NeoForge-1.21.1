package li.cil.oc2.common.container;

import li.cil.oc2.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc2.common.init.OCMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Container menu for Computer Case GUI
 * Handles component slot management and player inventory
 */
public class ComputerCaseMenu extends AbstractContainerMenu {
    
    private final ComputerCaseBlockEntity blockEntity;
    private final ContainerLevelAccess access;
    
    // Constructor for server-side (from BlockEntity)
    public ComputerCaseMenu(int windowId, Inventory playerInventory, ComputerCaseBlockEntity blockEntity) {
        super(OCMenuTypes.COMPUTER_CASE.get(), windowId);
        this.blockEntity = blockEntity;
        this.access = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        
        addComponentSlots();
        addPlayerInventory(playerInventory);
    }
    
    // Constructor for client-side (from network)
    public ComputerCaseMenu(int windowId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(windowId, playerInventory, 
             (ComputerCaseBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }
    
    private void addComponentSlots() {
        var inventory = blockEntity.getComponentInventory();
        int tier = blockEntity.getTier();
        
        // Component slots layout based on tier
        int slotsPerRow = switch (tier) {
            case 1 -> 4; // 2x4 grid for T1
            case 2 -> 4; // 3x4 grid for T2  
            case 3 -> 4; // 4x4 grid for T3
            default -> 4;
        };
        
        int rows = inventory.getSlots() / slotsPerRow;
        int startX = 8;
        int startY = 18;
        
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < slotsPerRow; col++) {
                int index = row * slotsPerRow + col;
                if (index < inventory.getSlots()) {
                    addSlot(new ComponentSlot(inventory, index, 
                           startX + col * 18, startY + row * 18));
                }
            }
        }
    }
    
    private void addPlayerInventory(Inventory playerInventory) {
        // Player inventory (3x9)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 
                        8 + col * 18, 84 + row * 18));
            }
        }
        
        // Player hotbar (1x9)
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            
            int componentSlots = blockEntity.getComponentInventory().getSlots();
            
            if (index < componentSlots) {
                // Moving from component slots to player inventory
                if (!this.moveItemStackTo(itemstack1, componentSlots, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving from player inventory to component slots
                if (!this.moveItemStackTo(itemstack1, 0, componentSlots, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, blockEntity.getBlockState().getBlock());
    }
    
    public ComputerCaseBlockEntity getBlockEntity() {
        return blockEntity;
    }
    
    /**
     * Special slot for components that only accepts component items
     */
    private static class ComponentSlot extends SlotItemHandler {
        
        public ComponentSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int xPosition, int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
        }
        
        @Override
        public int getMaxStackSize() {
            return 1; // Only one component per slot
        }
        
        @Override
        public boolean mayPlace(ItemStack stack) {
            // Only allow items with IComponent capability
            return super.mayPlace(stack) && 
                   stack.getCapability(li.cil.oc2.common.init.OCCapabilities.COMPONENT).isPresent();
        }
    }
}