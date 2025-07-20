package li.cil.oc2.lua.api;

import li.cil.oc2.common.system.SignalSystem;
import li.cil.oc2.lua.machine.LuaMachine;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.LibFunction;
import org.luaj.vm2.lib.TwoArgFunction;

/**
 * Computer API for Lua environment
 * Provides computer.* functions for system control
 */
public class ComputerAPI extends TwoArgFunction {
    
    private final LuaMachine luaMachine;
    private final long startTime;
    private final SignalSystem signalSystem;
    
    public ComputerAPI(LuaMachine luaMachine) {
        this.luaMachine = luaMachine;
        this.startTime = System.currentTimeMillis();
        this.signalSystem = new SignalSystem();
    }
    
    @Override
    public LuaValue call(LuaValue modname, LuaValue env) {
        LuaTable computer = new LuaTable();
        
        // computer.realTime() - Get real world time in seconds
        computer.set("realTime", new RealTimeFunction());
        
        // computer.uptime() - Get computer uptime in seconds
        computer.set("uptime", new UptimeFunction());
        
        // computer.energy() - Get energy level
        computer.set("energy", new EnergyFunction());
        
        // computer.maxEnergy() - Get maximum energy capacity
        computer.set("maxEnergy", new MaxEnergyFunction());
        
        // computer.beep([frequency], [duration]) - Play beep sound
        computer.set("beep", new BeepFunction());
        
        // computer.getBootAddress() - Get boot device address
        computer.set("getBootAddress", new GetBootAddressFunction());
        
        // computer.setBootAddress(address) - Set boot device address
        computer.set("setBootAddress", new SetBootAddressFunction());
        
        // computer.shutdown([reboot]) - Shutdown or restart computer
        computer.set("shutdown", new ShutdownFunction());
        
        // computer.pushSignal(name, ...) - Push signal to event queue
        computer.set("pushSignal", new PushSignalFunction());
        
        // computer.pullSignal([timeout]) - Pull signal from event queue
        computer.set("pullSignal", new PullSignalFunction());
        
        env.set("computer", computer);
        return computer;
    }
    
    private class RealTimeFunction extends LibFunction {
        @Override
        public LuaValue call() {
            return LuaValue.valueOf(System.currentTimeMillis() / 1000.0);
        }
    }
    
    private class UptimeFunction extends LibFunction {
        @Override
        public LuaValue call() {
            return LuaValue.valueOf((System.currentTimeMillis() - startTime) / 1000.0);
        }
    }
    
    private class EnergyFunction extends LibFunction {
        @Override
        public LuaValue call() {
            int stored = luaMachine.getHost().getEnergyStorage().getEnergyStored();
            return LuaValue.valueOf(stored);
        }
    }
    
    private class MaxEnergyFunction extends LibFunction {
        @Override
        public LuaValue call() {
            int max = luaMachine.getHost().getEnergyStorage().getMaxEnergyStored();
            return LuaValue.valueOf(max);
        }
    }
    
    private class BeepFunction extends LibFunction {
        @Override
        public LuaValue call(LuaValue frequency, LuaValue duration) {
            // Default values
            float freq = frequency.isnil() ? 1000.0f : (float) frequency.todouble();
            float dur = duration.isnil() ? 0.1f : (float) duration.todouble();
            
            // Clamp values
            freq = Math.max(20.0f, Math.min(20000.0f, freq));
            dur = Math.max(0.05f, Math.min(5.0f, dur));
            
            // TODO: Implement actual sound playing
            // For now, just log the beep
            // OpenComputersMod.LOGGER.debug("Beep: {}Hz for {}s", freq, dur);
            
            return LuaValue.NIL;
        }
    }
    
    private class GetBootAddressFunction extends LibFunction {
        @Override
        public LuaValue call() {
            String bootAddress = luaMachine.getBootManager().getBootAddress();
            return bootAddress != null ? LuaValue.valueOf(bootAddress) : LuaValue.NIL;
        }
    }
    
    private class SetBootAddressFunction extends LibFunction {
        @Override
        public LuaValue call(LuaValue address) {
            String addr = address.isnil() ? null : address.tojstring();
            boolean success = luaMachine.getBootManager().setBootAddress(addr);
            return LuaValue.valueOf(success);
        }
    }
    
    private class ShutdownFunction extends LibFunction {
        @Override
        public LuaValue call(LuaValue reboot) {
            boolean shouldReboot = !reboot.isnil() && reboot.toboolean();
            
            if (shouldReboot) {
                // Schedule restart
                new Thread(() -> {
                    try {
                        Thread.sleep(1000); // 1 second delay
                        luaMachine.restart();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            } else {
                // Shutdown
                luaMachine.stop();
            }
            
            return LuaValue.NIL;
        }
    }
    
    private class PushSignalFunction extends LibFunction {
        @Override
        public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
            if (args.narg() == 0) return LuaValue.FALSE;
            
            String name = args.arg(1).tojstring();
            Object[] signalArgs = new Object[args.narg() - 1];
            
            for (int i = 0; i < signalArgs.length; i++) {
                LuaValue arg = args.arg(i + 2);
                signalArgs[i] = luaToJava(arg);
            }
            
            boolean success = signalSystem.pushSignal(name, signalArgs);
            return LuaValue.valueOf(success);
        }
        
        private Object luaToJava(LuaValue value) {
            if (value.isnil()) {
                return null;
            } else if (value.isboolean()) {
                return value.toboolean();
            } else if (value.isint()) {
                return value.toint();
            } else if (value.isnumber()) {
                return value.todouble();
            } else if (value.isstring()) {
                return value.tojstring();
            }
            return value.toString();
        }
    }
    
    private class PullSignalFunction extends LibFunction {
        @Override
        public LuaValue call(LuaValue timeout) {
            double timeoutSeconds = timeout.isnil() ? 0.0 : timeout.todouble();
            
            LuaValue[] signal = signalSystem.pullSignal(timeoutSeconds);
            if (signal == null) {
                return LuaValue.NIL;
            }
            
            // Convert array to varargs
            return org.luaj.vm2.LuaValue.varargsOf(signal);
        }
    }
}