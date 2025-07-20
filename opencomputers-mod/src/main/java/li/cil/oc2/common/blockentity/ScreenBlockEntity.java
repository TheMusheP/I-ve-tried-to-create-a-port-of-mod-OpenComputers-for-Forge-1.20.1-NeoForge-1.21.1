package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.component.GPUComponent;
import li.cil.oc2.common.init.OCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Screen Block Entity - Displays text and graphics from GPU
 * Handles text buffer, colors, and client-side rendering
 */
public class ScreenBlockEntity extends BlockEntity {
    
    // Screen dimensions
    private int resolutionX = 80;
    private int resolutionY = 25;
    
    // Text and color buffers
    private char[] textBuffer;
    private int[] foregroundBuffer;
    private int[] backgroundBuffer;
    
    // Bound GPU
    private GPUComponent boundGPU;
    private String boundGPUAddress;
    
    // Screen tier and capabilities
    private final int tier;
    private final int maxResolutionX;
    private final int maxResolutionY;
    
    // Sync flags
    private boolean needsSync = false;
    private int lastSyncTick = 0;
    
    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        this(pos, state, 1); // Default to T1 screen
    }
    
    public ScreenBlockEntity(BlockPos pos, BlockState state, int tier) {
        super(OCBlockEntities.SCREEN.get(), pos, state);
        this.tier = tier;
        
        // Set capabilities based on tier
        switch (tier) {
            case 1 -> {
                maxResolutionX = 50;
                maxResolutionY = 16;
            }
            case 2 -> {
                maxResolutionX = 80;
                maxResolutionY = 25;
            }
            case 3 -> {
                maxResolutionX = 160;
                maxResolutionY = 50;
            }
            default -> {
                maxResolutionX = 80;
                maxResolutionY = 25;
            }
        }
        
        // Initialize with default resolution
        setResolution(Math.min(resolutionX, maxResolutionX), Math.min(resolutionY, maxResolutionY));
    }
    
    public void setResolution(int x, int y) {
        if (x <= 0 || y <= 0 || x > maxResolutionX || y > maxResolutionY) {
            return;
        }
        
        resolutionX = x;
        resolutionY = y;
        
        // Resize buffers
        int size = resolutionX * resolutionY;
        textBuffer = new char[size];
        foregroundBuffer = new int[size];
        backgroundBuffer = new int[size];
        
        // Clear buffers
        clear();
        
        needsSync = true;
        setChanged();
    }
    
    public void clear() {
        if (textBuffer == null) return;
        
        for (int i = 0; i < textBuffer.length; i++) {
            textBuffer[i] = ' ';
            foregroundBuffer[i] = 0xFFFFFF;
            backgroundBuffer[i] = 0x000000;
        }
        
        needsSync = true;
        setChanged();
    }
    
    private int getIndex(int x, int y) {
        if (x < 0 || y < 0 || x >= resolutionX || y >= resolutionY) {
            return -1;
        }
        return y * resolutionX + x;
    }
    
    public String getText(int x, int y) {
        int index = getIndex(x, y);
        if (index < 0 || textBuffer == null) {
            return "";
        }
        return String.valueOf(textBuffer[index]);
    }
    
    public boolean setText(int x, int y, String text, int foreground, int background, boolean vertical) {
        if (textBuffer == null || text == null) return false;
        
        boolean changed = false;
        
        for (int i = 0; i < text.length(); i++) {
            int px = vertical ? x : x + i;
            int py = vertical ? y + i : y;
            int index = getIndex(px, py);
            
            if (index >= 0) {
                char ch = text.charAt(i);
                if (textBuffer[index] != ch || 
                    foregroundBuffer[index] != foreground || 
                    backgroundBuffer[index] != background) {
                    
                    textBuffer[index] = ch;
                    foregroundBuffer[index] = foreground;
                    backgroundBuffer[index] = background;
                    changed = true;
                }
            }
        }
        
        if (changed) {
            needsSync = true;
            setChanged();
        }
        
        return changed;
    }
    
    public boolean fill(int x, int y, int w, int h, char character, int foreground, int background) {
        if (textBuffer == null) return false;
        
        boolean changed = false;
        
        for (int py = y; py < y + h; py++) {
            for (int px = x; px < x + w; px++) {
                int index = getIndex(px, py);
                if (index >= 0) {
                    if (textBuffer[index] != character || 
                        foregroundBuffer[index] != foreground || 
                        backgroundBuffer[index] != background) {
                        
                        textBuffer[index] = character;
                        foregroundBuffer[index] = foreground;
                        backgroundBuffer[index] = background;
                        changed = true;
                    }
                }
            }
        }
        
        if (changed) {
            needsSync = true;
            setChanged();
        }
        
        return changed;
    }
    
    public boolean copy(int sx, int sy, int w, int h, int dx, int dy) {
        if (textBuffer == null) return false;
        
        // Create temporary buffers for the copy operation
        char[] tempText = new char[w * h];
        int[] tempFg = new int[w * h];
        int[] tempBg = new int[w * h];
        
        // Copy source area to temp buffers
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int srcIndex = getIndex(sx + px, sy + py);
                int tempIndex = py * w + px;
                
                if (srcIndex >= 0) {
                    tempText[tempIndex] = textBuffer[srcIndex];
                    tempFg[tempIndex] = foregroundBuffer[srcIndex];
                    tempBg[tempIndex] = backgroundBuffer[srcIndex];
                } else {
                    tempText[tempIndex] = ' ';
                    tempFg[tempIndex] = 0xFFFFFF;
                    tempBg[tempIndex] = 0x000000;
                }
            }
        }
        
        // Copy temp buffers to destination
        boolean changed = false;
        for (int py = 0; py < h; py++) {
            for (int px = 0; px < w; px++) {
                int dstIndex = getIndex(dx + px, dy + py);
                int tempIndex = py * w + px;
                
                if (dstIndex >= 0) {
                    if (textBuffer[dstIndex] != tempText[tempIndex] ||
                        foregroundBuffer[dstIndex] != tempFg[tempIndex] ||
                        backgroundBuffer[dstIndex] != tempBg[tempIndex]) {
                        
                        textBuffer[dstIndex] = tempText[tempIndex];
                        foregroundBuffer[dstIndex] = tempFg[tempIndex];
                        backgroundBuffer[dstIndex] = tempBg[tempIndex];
                        changed = true;
                    }
                }
            }
        }
        
        if (changed) {
            needsSync = true;
            setChanged();
        }
        
        return changed;
    }
    
    public void bindGPU(GPUComponent gpu) {
        this.boundGPU = gpu;
        this.boundGPUAddress = gpu != null ? gpu.getAddress() : null;
        setChanged();
    }
    
    public GPUComponent getBoundGPU() {
        return boundGPU;
    }
    
    public String getBoundGPUAddress() {
        return boundGPUAddress;
    }
    
    public int getResolutionX() {
        return resolutionX;
    }
    
    public int getResolutionY() {
        return resolutionY;
    }
    
    public int getTier() {
        return tier;
    }
    
    public int getMaxResolutionX() {
        return maxResolutionX;
    }
    
    public int getMaxResolutionY() {
        return maxResolutionY;
    }
    
    // Client-side rendering data access
    @OnlyIn(Dist.CLIENT)
    public char[] getTextBuffer() {
        return textBuffer;
    }
    
    @OnlyIn(Dist.CLIENT)
    public int[] getForegroundBuffer() {
        return foregroundBuffer;
    }
    
    @OnlyIn(Dist.CLIENT)
    public int[] getBackgroundBuffer() {
        return backgroundBuffer;
    }
    
    // Static tick methods
    public static void clientTick(Level level, BlockPos pos, BlockState state, ScreenBlockEntity blockEntity) {
        // Client-side ticking if needed
    }
    
    public static void serverTick(Level level, BlockPos pos, BlockState state, ScreenBlockEntity blockEntity) {
        blockEntity.tick();
    }
    
    private void tick() {
        if (level == null || level.isClientSide) return;
        
        // Sync to clients periodically if needed
        if (needsSync && level.getGameTime() - lastSyncTick > 5) { // Sync every 5 ticks max
            sync();
            needsSync = false;
            lastSyncTick = (int) level.getGameTime();
        }
    }
    
    private void sync() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
    
    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        
        resolutionX = tag.getInt("ResolutionX");
        resolutionY = tag.getInt("ResolutionY");
        boundGPUAddress = tag.contains("BoundGPU") ? tag.getString("BoundGPU") : null;
        
        // Recreate buffers
        setResolution(resolutionX, resolutionY);
        
        // Load text buffer
        if (tag.contains("TextBuffer")) {
            String text = tag.getString("TextBuffer");
            for (int i = 0; i < Math.min(text.length(), textBuffer.length); i++) {
                textBuffer[i] = text.charAt(i);
            }
        }
        
        // Load color buffers
        if (tag.contains("ForegroundBuffer")) {
            int[] fg = tag.getIntArray("ForegroundBuffer");
            System.arraycopy(fg, 0, foregroundBuffer, 0, Math.min(fg.length, foregroundBuffer.length));
        }
        
        if (tag.contains("BackgroundBuffer")) {
            int[] bg = tag.getIntArray("BackgroundBuffer");
            System.arraycopy(bg, 0, backgroundBuffer, 0, Math.min(bg.length, backgroundBuffer.length));
        }
    }
    
    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        
        tag.putInt("ResolutionX", resolutionX);
        tag.putInt("ResolutionY", resolutionY);
        
        if (boundGPUAddress != null) {
            tag.putString("BoundGPU", boundGPUAddress);
        }
        
        if (textBuffer != null) {
            tag.putString("TextBuffer", new String(textBuffer));
            tag.putIntArray("ForegroundBuffer", foregroundBuffer);
            tag.putIntArray("BackgroundBuffer", backgroundBuffer);
        }
    }
    
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }
    
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}