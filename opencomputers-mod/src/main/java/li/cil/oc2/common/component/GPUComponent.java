package li.cil.oc2.common.component;

import li.cil.oc2.api.component.ComponentException;
import li.cil.oc2.api.component.IComponent;
import li.cil.oc2.common.blockentity.ScreenBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Graphics Processing Unit component
 * Provides graphics API for drawing text and simple graphics on screens
 */
public class GPUComponent implements IComponent {
    
    private final String address;
    private final int tier;
    private final Level level;
    
    // GPU state
    private int resolutionX = 80;
    private int resolutionY = 25;
    private int foregroundColor = 0xFFFFFF;
    private int backgroundColor = 0x000000;
    private String boundScreenAddress = null;
    
    // GPU capabilities by tier
    private final int maxResolutionX;
    private final int maxResolutionY;
    private final int maxColors;
    
    public GPUComponent(Level level, int tier) {
        this.address = UUID.randomUUID().toString();
        this.tier = tier;
        this.level = level;
        
        // Set capabilities based on tier
        switch (tier) {
            case 1 -> {
                maxResolutionX = 50;
                maxResolutionY = 16;
                maxColors = 16; // 4-bit color
            }
            case 2 -> {
                maxResolutionX = 80;
                maxResolutionY = 25;
                maxColors = 256; // 8-bit color
            }
            case 3 -> {
                maxResolutionX = 160;
                maxResolutionY = 50;
                maxColors = 16777216; // 24-bit color
            }
            default -> {
                maxResolutionX = 80;
                maxResolutionY = 25;
                maxColors = 256;
            }
        }
        
        // Set default resolution
        resolutionX = Math.min(80, maxResolutionX);
        resolutionY = Math.min(25, maxResolutionY);
    }
    
    @Override
    public String getAddress() {
        return address;
    }
    
    @Override
    public String getType() {
        return "gpu";
    }
    
    @Override
    public Object invoke(String method, Object... args) throws ComponentException {
        return switch (method) {
            case "bind" -> bind(args.length > 0 ? (String) args[0] : null);
            case "getScreen" -> boundScreenAddress;
            case "getBackground" -> backgroundColor;
            case "setBackground" -> setBackground(args.length > 0 ? (Integer) args[0] : 0, 
                                                args.length > 1 ? (Boolean) args[1] : false);
            case "getForeground" -> foregroundColor;
            case "setForeground" -> setForeground(args.length > 0 ? (Integer) args[0] : 0xFFFFFF, 
                                                args.length > 1 ? (Boolean) args[1] : false);
            case "getResolution" -> new Object[]{resolutionX, resolutionY};
            case "setResolution" -> setResolution((Integer) args[0], (Integer) args[1]);
            case "maxResolution" -> new Object[]{maxResolutionX, maxResolutionY};
            case "get" -> getText((Integer) args[0], (Integer) args[1]);
            case "set" -> setText((Integer) args[0], (Integer) args[1], (String) args[2], 
                                args.length > 3 ? (Boolean) args[3] : true);
            case "copy" -> copy((Integer) args[0], (Integer) args[1], (Integer) args[2], 
                               (Integer) args[3], (Integer) args[4], (Integer) args[5]);
            case "fill" -> fill((Integer) args[0], (Integer) args[1], (Integer) args[2], 
                               (Integer) args[3], (String) args[4]);
            case "getDepth" -> getColorDepth();
            case "setDepth" -> setColorDepth((Integer) args[0]);
            case "maxDepth" -> getMaxColorDepth();
            case "getPaletteColor" -> getPaletteColor((Integer) args[0]);
            case "setPaletteColor" -> setPaletteColor((Integer) args[0], (Integer) args[1]);
            default -> throw new ComponentException("Unknown method: " + method);
        };
    }
    
    private Object bind(String screenAddress) throws ComponentException {
        if (screenAddress == null) {
            boundScreenAddress = null;
            return null;
        }
        
        // Find screen by address (simplified - in real implementation would use component registry)
        ScreenBlockEntity screen = findScreenByAddress(screenAddress);
        if (screen == null) {
            throw new ComponentException("No such screen: " + screenAddress);
        }
        
        String oldAddress = boundScreenAddress;
        boundScreenAddress = screenAddress;
        
        // Bind GPU to screen
        screen.bindGPU(this);
        
        return oldAddress;
    }
    
    private Object setBackground(int color, boolean isPaletteIndex) {
        int oldColor = backgroundColor;
        backgroundColor = isPaletteIndex ? getPaletteColor(color) : color;
        return oldColor;
    }
    
    private Object setForeground(int color, boolean isPaletteIndex) {
        int oldColor = foregroundColor;
        foregroundColor = isPaletteIndex ? getPaletteColor(color) : color;
        return oldColor;
    }
    
    private Object setResolution(int x, int y) throws ComponentException {
        if (x <= 0 || y <= 0 || x > maxResolutionX || y > maxResolutionY) {
            throw new ComponentException("Invalid resolution");
        }
        
        Object[] oldResolution = {resolutionX, resolutionY};
        resolutionX = x;
        resolutionY = y;
        
        // Update bound screen resolution
        ScreenBlockEntity screen = getBoundScreen();
        if (screen != null) {
            screen.setResolution(x, y);
        }
        
        return oldResolution;
    }
    
    private Object getText(int x, int y) throws ComponentException {
        ScreenBlockEntity screen = getBoundScreen();
        if (screen == null) {
            throw new ComponentException("No screen bound");
        }
        
        if (x < 1 || y < 1 || x > resolutionX || y > resolutionY) {
            throw new ComponentException("Index out of bounds");
        }
        
        return screen.getText(x - 1, y - 1); // Convert to 0-based
    }
    
    private Object setText(int x, int y, String text, boolean vertical) throws ComponentException {
        ScreenBlockEntity screen = getBoundScreen();
        if (screen == null) {
            throw new ComponentException("No screen bound");
        }
        
        if (x < 1 || y < 1 || x > resolutionX || y > resolutionY) {
            throw new ComponentException("Index out of bounds");
        }
        
        return screen.setText(x - 1, y - 1, text, foregroundColor, backgroundColor, vertical);
    }
    
    private Object copy(int x, int y, int w, int h, int tx, int ty) throws ComponentException {
        ScreenBlockEntity screen = getBoundScreen();
        if (screen == null) {
            throw new ComponentException("No screen bound");
        }
        
        if (x < 1 || y < 1 || tx < 1 || ty < 1 || w <= 0 || h <= 0) {
            throw new ComponentException("Invalid coordinates");
        }
        
        return screen.copy(x - 1, y - 1, w, h, tx - 1, ty - 1);
    }
    
    private Object fill(int x, int y, int w, int h, String character) throws ComponentException {
        ScreenBlockEntity screen = getBoundScreen();
        if (screen == null) {
            throw new ComponentException("No screen bound");
        }
        
        if (x < 1 || y < 1 || w <= 0 || h <= 0) {
            throw new ComponentException("Invalid coordinates");
        }
        
        char ch = character.isEmpty() ? ' ' : character.charAt(0);
        return screen.fill(x - 1, y - 1, w, h, ch, foregroundColor, backgroundColor);
    }
    
    private int getColorDepth() {
        return switch (tier) {
            case 1 -> 4;  // 4-bit
            case 2 -> 8;  // 8-bit  
            case 3 -> 24; // 24-bit
            default -> 8;
        };
    }
    
    private Object setColorDepth(int depth) {
        // Color depth is fixed by tier in this implementation
        return getColorDepth();
    }
    
    private int getMaxColorDepth() {
        return getColorDepth();
    }
    
    private int getPaletteColor(int index) {
        // Basic 16-color palette
        int[] basicPalette = {
            0x000000, 0x000080, 0x008000, 0x008080, 0x800000, 0x800080, 0x808000, 0xC0C0C0,
            0x808080, 0x0000FF, 0x00FF00, 0x00FFFF, 0xFF0000, 0xFF00FF, 0xFFFF00, 0xFFFFFF
        };
        
        if (index >= 0 && index < basicPalette.length) {
            return basicPalette[index];
        }
        return 0xFFFFFF; // Default to white
    }
    
    private Object setPaletteColor(int index, int color) {
        // Palette modification not implemented in this basic version
        return getPaletteColor(index);
    }
    
    private ScreenBlockEntity getBoundScreen() {
        if (boundScreenAddress == null) return null;
        return findScreenByAddress(boundScreenAddress);
    }
    
    private ScreenBlockEntity findScreenByAddress(String address) {
        // TODO: Implement proper screen lookup through component registry
        // For now, this is a placeholder
        return null;
    }
    
    @Override
    public String[] methods() {
        return new String[]{
            "bind", "getScreen", "getBackground", "setBackground", "getForeground", "setForeground",
            "getResolution", "setResolution", "maxResolution", "get", "set", "copy", "fill",
            "getDepth", "setDepth", "maxDepth", "getPaletteColor", "setPaletteColor"
        };
    }
    
    @Override
    public String doc(String method) {
        return switch (method) {
            case "bind" -> "bind(address:string):string -- Bind to screen";
            case "getScreen" -> "getScreen():string -- Get bound screen address";
            case "getBackground" -> "getBackground():number -- Get background color";
            case "setBackground" -> "setBackground(color:number[, palette:boolean]):number -- Set background color";
            case "getForeground" -> "getForeground():number -- Get foreground color";
            case "setForeground" -> "setForeground(color:number[, palette:boolean]):number -- Set foreground color";
            case "getResolution" -> "getResolution():number,number -- Get screen resolution";
            case "setResolution" -> "setResolution(x:number, y:number):boolean -- Set screen resolution";
            case "maxResolution" -> "maxResolution():number,number -- Get maximum resolution";
            case "get" -> "get(x:number, y:number):string -- Get character at position";
            case "set" -> "set(x:number, y:number, text:string[, vertical:boolean]):boolean -- Set text";
            case "copy" -> "copy(x:number, y:number, w:number, h:number, tx:number, ty:number):boolean -- Copy area";
            case "fill" -> "fill(x:number, y:number, w:number, h:number, char:string):boolean -- Fill area";
            case "getDepth" -> "getDepth():number -- Get color depth";
            case "setDepth" -> "setDepth(depth:number):number -- Set color depth";
            case "maxDepth" -> "maxDepth():number -- Get maximum color depth";
            case "getPaletteColor" -> "getPaletteColor(index:number):number -- Get palette color";
            case "setPaletteColor" -> "setPaletteColor(index:number, color:number):number -- Set palette color";
            default -> "No documentation available";
        };
    }
    
    @Override
    public boolean isValid() {
        return true;
    }
}