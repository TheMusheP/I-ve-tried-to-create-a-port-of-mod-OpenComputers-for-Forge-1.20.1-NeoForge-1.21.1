package li.cil.oc2.common.entity;

import li.cil.oc2.api.component.ComponentException;
import li.cil.oc2.api.component.IComponent;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * Robot component for Lua API
 * Provides robot.* functions for movement and interaction
 */
public class RobotComponent implements IComponent {
    
    private final RobotEntity robot;
    private final String address;
    
    public RobotComponent(RobotEntity robot) {
        this.robot = robot;
        this.address = UUID.randomUUID().toString();
    }
    
    @Override
    public String getAddress() {
        return address;
    }
    
    @Override
    public String getType() {
        return "robot";
    }
    
    @Override
    public Object invoke(String method, Object... args) throws ComponentException {
        return switch (method) {
            // Movement
            case "move" -> {
                if (args.length > 0 && args[0] instanceof Integer direction) {
                    yield switch (direction) {
                        case 0 -> robot.moveDown();
                        case 1 -> robot.moveUp();
                        case 2 -> robot.moveBack();
                        case 3 -> robot.moveForward();
                        case 4 -> robot.turnRight();
                        case 5 -> robot.turnLeft();
                        default -> false;
                    };
                }
                yield robot.moveForward();
            }
            case "forward" -> robot.moveForward();
            case "back" -> robot.moveBack();
            case "up" -> robot.moveUp();
            case "down" -> robot.moveDown();
            case "turnLeft" -> robot.turnLeft();
            case "turnRight" -> robot.turnRight();
            case "turnAround" -> robot.turnAround();
            
            // Actions
            case "swing" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Swing in specific direction
                    yield robot.swing();
                }
                yield robot.swing();
            }
            case "use" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Use in specific direction
                    yield robot.use();
                }
                yield robot.use();
            }
            case "place" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Place block in specific direction
                    yield false;
                }
                yield false;
            }
            case "suck" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Suck items from specific direction
                    yield false;
                }
                yield false;
            }
            case "drop" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Drop items in specific direction
                    yield false;
                }
                yield false;
            }
            
            // Inventory
            case "inventorySize" -> robot.getInventory().getSlots();
            case "select" -> {
                if (args.length > 0 && args[0] instanceof Integer slot) {
                    if (slot >= 1 && slot <= robot.getInventory().getSlots()) {
                        robot.setSelectedSlot(slot - 1); // Convert to 0-based
                        yield true;
                    }
                }
                yield false;
            }
            case "count" -> {
                int slot = robot.getSelectedSlot();
                if (args.length > 0 && args[0] instanceof Integer argSlot) {
                    slot = argSlot - 1; // Convert to 0-based
                }
                if (slot >= 0 && slot < robot.getInventory().getSlots()) {
                    ItemStack stack = robot.getInventory().getStackInSlot(slot);
                    yield stack.getCount();
                }
                yield 0;
            }
            case "space" -> {
                int slot = robot.getSelectedSlot();
                if (args.length > 0 && args[0] instanceof Integer argSlot) {
                    slot = argSlot - 1; // Convert to 0-based
                }
                if (slot >= 0 && slot < robot.getInventory().getSlots()) {
                    ItemStack stack = robot.getInventory().getStackInSlot(slot);
                    yield stack.isEmpty() ? 64 : 64 - stack.getCount();
                }
                yield 0;
            }
            case "transferTo" -> {
                if (args.length >= 1 && args[0] instanceof Integer toSlot) {
                    int amount = args.length > 1 && args[1] instanceof Integer ? 
                                (Integer) args[1] : Integer.MAX_VALUE;
                    
                    int fromSlot = robot.getSelectedSlot();
                    int to = toSlot - 1; // Convert to 0-based
                    
                    if (to >= 0 && to < robot.getInventory().getSlots() && to != fromSlot) {
                        ItemStack fromStack = robot.getInventory().getStackInSlot(fromSlot);
                        ItemStack toStack = robot.getInventory().getStackInSlot(to);
                        
                        if (!fromStack.isEmpty()) {
                            int transferAmount = Math.min(amount, fromStack.getCount());
                            
                            if (toStack.isEmpty()) {
                                // Move to empty slot
                                ItemStack newStack = fromStack.split(transferAmount);
                                robot.getInventory().setStackInSlot(to, newStack);
                                yield transferAmount;
                            } else if (ItemStack.isSameItemSameTags(fromStack, toStack)) {
                                // Merge with existing stack
                                int canTransfer = Math.min(transferAmount, 
                                                         toStack.getMaxStackSize() - toStack.getCount());
                                if (canTransfer > 0) {
                                    fromStack.shrink(canTransfer);
                                    toStack.grow(canTransfer);
                                    yield canTransfer;
                                }
                            }
                        }
                    }
                }
                yield 0;
            }
            
            // Status
            case "name" -> "Robot";
            case "energy" -> robot.getEnergyStorage().getEnergyStored();
            case "maxEnergy" -> robot.getEnergyStorage().getMaxEnergyStored();
            case "durability" -> {
                ItemStack tool = robot.getSelectedItem();
                if (!tool.isEmpty() && tool.isDamageableItem()) {
                    yield (double) (tool.getMaxDamage() - tool.getDamageValue()) / tool.getMaxDamage();
                }
                yield 1.0;
            }
            
            // Detect/Compare
            case "detect" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Detect block in specific direction
                    yield false;
                }
                // Detect block in front
                yield !robot.level().getBlockState(robot.blockPosition().relative(Direction.NORTH)).isAir();
            }
            case "compare" -> {
                if (args.length > 0 && args[0] instanceof Integer side) {
                    // TODO: Compare with block in specific direction
                    yield false;
                }
                yield false;
            }
            case "compareTo" -> {
                if (args.length > 0 && args[0] instanceof Integer slot) {
                    int compareSlot = slot - 1; // Convert to 0-based
                    int currentSlot = robot.getSelectedSlot();
                    
                    if (compareSlot >= 0 && compareSlot < robot.getInventory().getSlots()) {
                        ItemStack current = robot.getInventory().getStackInSlot(currentSlot);
                        ItemStack compare = robot.getInventory().getStackInSlot(compareSlot);
                        yield ItemStack.isSameItemSameTags(current, compare);
                    }
                }
                yield false;
            }
            
            default -> throw new ComponentException("Unknown method: " + method);
        };
    }
    
    @Override
    public String[] methods() {
        return new String[]{
            // Movement
            "move", "forward", "back", "up", "down", 
            "turnLeft", "turnRight", "turnAround",
            
            // Actions
            "swing", "use", "place", "suck", "drop",
            
            // Inventory
            "inventorySize", "select", "count", "space", "transferTo",
            
            // Status
            "name", "energy", "maxEnergy", "durability",
            
            // Detect/Compare
            "detect", "compare", "compareTo"
        };
    }
    
    @Override
    public String doc(String method) {
        return switch (method) {
            case "move" -> "move([direction:number]):boolean -- Move in direction (0=down, 1=up, 2=back, 3=forward, 4=right, 5=left)";
            case "forward" -> "forward():boolean -- Move forward";
            case "back" -> "back():boolean -- Move back";
            case "up" -> "up():boolean -- Move up";
            case "down" -> "down():boolean -- Move down";
            case "turnLeft" -> "turnLeft():boolean -- Turn left";
            case "turnRight" -> "turnRight():boolean -- Turn right";
            case "turnAround" -> "turnAround():boolean -- Turn around";
            
            case "swing" -> "swing([side:number]):boolean -- Swing tool";
            case "use" -> "use([side:number]):boolean -- Use item";
            case "place" -> "place([side:number]):boolean -- Place block";
            case "suck" -> "suck([side:number]):boolean -- Suck items";
            case "drop" -> "drop([side:number]):boolean -- Drop items";
            
            case "inventorySize" -> "inventorySize():number -- Get inventory size";
            case "select" -> "select(slot:number):boolean -- Select inventory slot";
            case "count" -> "count([slot:number]):number -- Get item count in slot";
            case "space" -> "space([slot:number]):number -- Get free space in slot";
            case "transferTo" -> "transferTo(slot:number[, count:number]):number -- Transfer items to slot";
            
            case "name" -> "name():string -- Get robot name";
            case "energy" -> "energy():number -- Get energy level";
            case "maxEnergy" -> "maxEnergy():number -- Get maximum energy";
            case "durability" -> "durability():number -- Get tool durability";
            
            case "detect" -> "detect([side:number]):boolean -- Detect block";
            case "compare" -> "compare([side:number]):boolean -- Compare with block";
            case "compareTo" -> "compareTo(slot:number):boolean -- Compare with slot";
            
            default -> "No documentation available";
        };
    }
    
    @Override
    public boolean isValid() {
        return robot != null && !robot.isRemoved();
    }
}