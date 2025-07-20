package li.cil.oc2.common.system;

import li.cil.oc2.OpenComputersMod;
import org.luaj.vm2.LuaValue;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Signal system for computer events and inter-computer communication
 * Implements computer.pullSignal() and computer.pushSignal() functionality
 */
public class SignalSystem {
    
    private final BlockingQueue<Signal> signalQueue = new LinkedBlockingQueue<>();
    private static final int MAX_SIGNALS = 256; // Maximum signals in queue
    
    /**
     * Push a signal to the queue
     */
    public boolean pushSignal(String name, Object... args) {
        if (signalQueue.size() >= MAX_SIGNALS) {
            // Remove oldest signal if queue is full
            signalQueue.poll();
        }
        
        Signal signal = new Signal(name, args);
        boolean success = signalQueue.offer(signal);
        
        if (success) {
            OpenComputersMod.LOGGER.debug("Signal pushed: {} with {} args", name, args.length);
        }
        
        return success;
    }
    
    /**
     * Pull a signal from the queue with timeout
     * @param timeout Timeout in seconds, 0 for no timeout
     * @return Signal as Lua values, or null if timeout
     */
    public LuaValue[] pullSignal(double timeout) {
        try {
            Signal signal;
            
            if (timeout <= 0) {
                // No timeout - block indefinitely
                signal = signalQueue.take();
            } else {
                // With timeout
                long timeoutMs = (long) (timeout * 1000);
                signal = signalQueue.poll(timeoutMs, TimeUnit.MILLISECONDS);
                
                if (signal == null) {
                    return null; // Timeout
                }
            }
            
            // Convert signal to Lua values
            LuaValue[] result = new LuaValue[signal.args.length + 1];
            result[0] = LuaValue.valueOf(signal.name);
            
            for (int i = 0; i < signal.args.length; i++) {
                result[i + 1] = toLuaValue(signal.args[i]);
            }
            
            OpenComputersMod.LOGGER.debug("Signal pulled: {} with {} args", signal.name, signal.args.length);
            return result;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }
    
    /**
     * Check if there are pending signals
     */
    public boolean hasPendingSignals() {
        return !signalQueue.isEmpty();
    }
    
    /**
     * Get number of pending signals
     */
    public int getPendingSignalCount() {
        return signalQueue.size();
    }
    
    /**
     * Clear all pending signals
     */
    public void clearSignals() {
        signalQueue.clear();
    }
    
    /**
     * Convert Java object to Lua value
     */
    private LuaValue toLuaValue(Object obj) {
        if (obj == null) {
            return LuaValue.NIL;
        } else if (obj instanceof Boolean) {
            return LuaValue.valueOf((Boolean) obj);
        } else if (obj instanceof Integer) {
            return LuaValue.valueOf((Integer) obj);
        } else if (obj instanceof Double) {
            return LuaValue.valueOf((Double) obj);
        } else if (obj instanceof Float) {
            return LuaValue.valueOf((Float) obj);
        } else if (obj instanceof String) {
            return LuaValue.valueOf((String) obj);
        } else {
            return LuaValue.valueOf(obj.toString());
        }
    }
    
    /**
     * Standard OpenComputers signals
     */
    public static class Signals {
        public static final String COMPONENT_ADDED = "component_added";
        public static final String COMPONENT_REMOVED = "component_removed";
        public static final String COMPONENT_UNAVAILABLE = "component_unavailable";
        public static final String COMPONENT_AVAILABLE = "component_available";
        public static final String INTERRUPTED = "interrupted";
        public static final String KEY_DOWN = "key_down";
        public static final String KEY_UP = "key_up";
        public static final String TOUCH = "touch";
        public static final String DRAG = "drag";
        public static final String DROP = "drop";
        public static final String SCROLL = "scroll";
        public static final String WALK = "walk";
        public static final String REDSTONE_CHANGED = "redstone_changed";
        public static final String MODEM_MESSAGE = "modem_message";
        public static final String INVENTORY_CHANGED = "inventory_changed";
        public static final String BUS_MESSAGE = "bus_message";
    }
    
    /**
     * Internal signal representation
     */
    private static class Signal {
        final String name;
        final Object[] args;
        final long timestamp;
        
        Signal(String name, Object... args) {
            this.name = name;
            this.args = args != null ? args : new Object[0];
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    /**
     * Push standard component signals
     */
    public void pushComponentAdded(String address, String type) {
        pushSignal(Signals.COMPONENT_ADDED, address, type);
    }
    
    public void pushComponentRemoved(String address, String type) {
        pushSignal(Signals.COMPONENT_REMOVED, address, type);
    }
    
    public void pushComponentUnavailable(String address) {
        pushSignal(Signals.COMPONENT_UNAVAILABLE, address);
    }
    
    public void pushComponentAvailable(String address) {
        pushSignal(Signals.COMPONENT_AVAILABLE, address);
    }
    
    public void pushInterrupted() {
        pushSignal(Signals.INTERRUPTED);
    }
    
    public void pushKeyDown(char character, int code, String player) {
        pushSignal(Signals.KEY_DOWN, character, code, player);
    }
    
    public void pushKeyUp(char character, int code, String player) {
        pushSignal(Signals.KEY_UP, character, code, player);
    }
    
    public void pushTouch(double x, double y, int button, String player) {
        pushSignal(Signals.TOUCH, x, y, button, player);
    }
    
    public void pushRedstoneChanged(int oldValue, int newValue) {
        pushSignal(Signals.REDSTONE_CHANGED, oldValue, newValue);
    }
}