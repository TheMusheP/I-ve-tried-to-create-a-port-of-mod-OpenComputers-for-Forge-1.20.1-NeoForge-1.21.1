package li.cil.oc2.lua.machine;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc2.lua.api.ComponentAPI;
import li.cil.oc2.lua.api.ComputerAPI;
import net.minecraft.nbt.CompoundTag;
import org.luaj.vm2.*;
import org.luaj.vm2.lib.*;
import org.luaj.vm2.lib.jse.*;

import java.io.InputStream;

/**
 * Lua Virtual Machine for OpenComputers
 * Manages Lua execution, sandboxing, and timeout handling
 */
public class LuaMachine {
    
    private static final int TIMEOUT_MS = 5000; // 5 second timeout
    private static final int MEMORY_LIMIT = 1024 * 1024; // 1MB memory limit
    
    private final ComputerCaseBlockEntity host;
    private final ComponentRegistry componentRegistry;
    
    private Globals globals;
    private LuaThread mainThread;
    private boolean isRunning = false;
    private boolean shouldStop = false;
    private long startTime;
    private int instructionCount = 0;
    private static final int INSTRUCTION_LIMIT = 100000; // Instructions per tick
    
    // Boot manager
    private BootManager bootManager;
    
    public LuaMachine(ComputerCaseBlockEntity host, ComponentRegistry componentRegistry) {
        this.host = host;
        this.componentRegistry = componentRegistry;
        this.bootManager = new BootManager(this);
        
        initializeLua();
    }
    
    private void initializeLua() {
        // Create sandboxed Lua environment
        globals = new Globals();
        
        // Load safe standard libraries
        globals.load(new JseBaseLib());
        globals.load(new PackageLib());
        globals.load(new Bit32Lib());
        globals.load(new TableLib());
        globals.load(new StringLib());
        globals.load(new CoroutineLib());
        globals.load(new JseMathLib());
        
        // Load OpenComputers APIs
        globals.load(new ComponentAPI(componentRegistry));
        globals.load(new ComputerAPI(this));
        
        // Set up sandbox environment
        setupSandbox();
        
        // Set up timeout hook
        setupTimeoutHook();
    }
    
    private void setupSandbox() {
        // Remove dangerous functions
        globals.set("dofile", LuaValue.NIL);
        globals.set("loadfile", LuaValue.NIL);
        globals.set("require", LuaValue.NIL);
        
        // Limit io library
        LuaValue io = globals.get("io");
        if (io.istable()) {
            io.set("open", LuaValue.NIL);
            io.set("popen", LuaValue.NIL);
            io.set("tmpfile", LuaValue.NIL);
        }
        
        // Limit os library
        LuaValue os = globals.get("os");
        if (os.istable()) {
            os.set("execute", LuaValue.NIL);
            os.set("exit", LuaValue.NIL);
            os.set("getenv", LuaValue.NIL);
            os.set("remove", LuaValue.NIL);
            os.set("rename", LuaValue.NIL);
            os.set("tmpname", LuaValue.NIL);
        }
    }
    
    private void setupTimeoutHook() {
        // Set up debug hook for timeout checking
        globals.setmetatable(new LuaTable() {{
            set("__call", new TwoArgFunction() {
                @Override
                public LuaValue call(LuaValue arg1, LuaValue arg2) {
                    checkTimeout();
                    return LuaValue.NIL;
                }
            });
        }});
    }
    
    private void checkTimeout() {
        instructionCount++;
        
        // Check instruction limit
        if (instructionCount > INSTRUCTION_LIMIT) {
            throw new LuaError("too long without yielding");
        }
        
        // Check time limit
        if (System.currentTimeMillis() - startTime > TIMEOUT_MS) {
            throw new LuaError("execution timeout");
        }
        
        // Check if should stop
        if (shouldStop) {
            throw new LuaError("machine stopped");
        }
    }
    
    public void start() {
        if (isRunning) return;
        
        isRunning = true;
        shouldStop = false;
        
        try {
            // Boot sequence
            bootManager.boot();
        } catch (Exception e) {
            OpenComputersMod.LOGGER.error("Failed to start Lua machine: ", e);
            stop();
        }
    }
    
    public void stop() {
        isRunning = false;
        shouldStop = true;
        
        if (mainThread != null) {
            mainThread.interrupt();
            mainThread = null;
        }
    }
    
    public void restart() {
        stop();
        
        // Wait a tick before restarting
        new Thread(() -> {
            try {
                Thread.sleep(50); // 1 tick delay
                initializeLua();
                start();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }
    
    public void tick() {
        if (!isRunning) return;
        
        startTime = System.currentTimeMillis();
        instructionCount = 0;
        
        try {
            // Process any pending Lua coroutines
            if (mainThread != null && mainThread.isAlive()) {
                // Continue execution
                LuaValue result = mainThread.resume(LuaValue.NIL);
                
                if (result.isnil()) {
                    // Thread finished
                    mainThread = null;
                }
            }
        } catch (LuaError e) {
            OpenComputersMod.LOGGER.warn("Lua execution error: {}", e.getMessage());
            
            if (e.getMessage().contains("timeout") || e.getMessage().contains("too long")) {
                // Timeout error - restart machine
                restart();
            }
        } catch (Exception e) {
            OpenComputersMod.LOGGER.error("Unexpected error in Lua machine: ", e);
            stop();
        }
    }
    
    public LuaValue execute(String code) throws LuaError {
        if (!isRunning) {
            throw new LuaError("machine not running");
        }
        
        startTime = System.currentTimeMillis();
        instructionCount = 0;
        
        try {
            LuaValue chunk = globals.load(code);
            return chunk.call();
        } catch (LuaError e) {
            throw e;
        } catch (Exception e) {
            throw new LuaError(e.getMessage());
        }
    }
    
    public LuaValue loadFile(String filename) throws LuaError {
        try {
            // Load file from mod resources
            InputStream is = getClass().getResourceAsStream("/assets/opencomputers/lua/" + filename);
            if (is == null) {
                throw new LuaError("file not found: " + filename);
            }
            
            String content = new String(is.readAllBytes());
            is.close();
            
            return globals.load(content, filename);
        } catch (Exception e) {
            throw new LuaError("failed to load file: " + e.getMessage());
        }
    }
    
    public void onComponentsChanged() {
        // Notify Lua environment about component changes
        if (isRunning && globals != null) {
            try {
                // Signal component_available or component_unavailable events
                LuaValue signalHandler = globals.get("computer").get("pushSignal");
                if (!signalHandler.isnil()) {
                    signalHandler.call(LuaValue.valueOf("component_changed"));
                }
            } catch (Exception e) {
                // Ignore signal errors
            }
        }
    }
    
    public Globals getGlobals() {
        return globals;
    }
    
    public ComputerCaseBlockEntity getHost() {
        return host;
    }
    
    public ComponentRegistry getComponentRegistry() {
        return componentRegistry;
    }
    
    public BootManager getBootManager() {
        return bootManager;
    }
    
    public boolean isRunning() {
        return isRunning;
    }
    
    // NBT serialization
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("IsRunning", isRunning);
        // TODO: Serialize Lua state if needed
        return tag;
    }
    
    public void deserializeNBT(CompoundTag tag) {
        isRunning = tag.getBoolean("IsRunning");
        // TODO: Deserialize Lua state if needed
        
        if (isRunning) {
            // Restart machine after loading
            initializeLua();
        }
    }
}