package li.cil.oc2.lua.machine;

import li.cil.oc2.api.component.ComponentException;
import li.cil.oc2.api.component.IComponent;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.CoerceJavaToLua;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for managing OpenComputers components
 * Provides thread-safe component storage and lookup functionality
 */
public class ComponentRegistry {
    
    // Thread-safe storage for components
    private final Map<String, IComponent> componentsByAddress = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> componentsByType = new ConcurrentHashMap<>();
    private final Map<String, String> primaryComponents = new ConcurrentHashMap<>();
    
    /**
     * Add a component to the registry
     */
    public void addComponent(IComponent component) {
        if (component == null || component.getAddress() == null) return;
        
        String address = component.getAddress();
        String type = component.getType();
        
        // Add to address map
        componentsByAddress.put(address, component);
        
        // Add to type map
        componentsByType.computeIfAbsent(type, k -> ConcurrentHashMap.newKeySet()).add(address);
        
        // Set as primary if first of its type
        primaryComponents.putIfAbsent(type, address);
        
        // Notify component of connection
        try {
            component.onConnect();
        } catch (Exception e) {
            // Ignore connection errors
        }
    }
    
    /**
     * Remove a component from the registry
     */
    public void removeComponent(String address) {
        IComponent component = componentsByAddress.remove(address);
        if (component == null) return;
        
        String type = component.getType();
        
        // Remove from type map
        Set<String> addresses = componentsByType.get(type);
        if (addresses != null) {
            addresses.remove(address);
            if (addresses.isEmpty()) {
                componentsByType.remove(type);
                primaryComponents.remove(type);
            } else if (address.equals(primaryComponents.get(type))) {
                // Set new primary if this was the primary
                primaryComponents.put(type, addresses.iterator().next());
            }
        }
        
        // Notify component of disconnection
        try {
            component.onDisconnect();
        } catch (Exception e) {
            // Ignore disconnection errors
        }
    }
    
    /**
     * Clear all components
     */
    public void clear() {
        // Notify all components of disconnection
        for (IComponent component : componentsByAddress.values()) {
            try {
                component.onDisconnect();
            } catch (Exception e) {
                // Ignore disconnection errors
            }
        }
        
        componentsByAddress.clear();
        componentsByType.clear();
        primaryComponents.clear();
    }
    
    /**
     * Check if component exists
     */
    public boolean exists(String address) {
        return componentsByAddress.containsKey(address);
    }
    
    /**
     * Get component by address
     */
    public IComponent getComponent(String address) {
        return componentsByAddress.get(address);
    }
    
    /**
     * Get component type by address
     */
    public String getType(String address) {
        IComponent component = componentsByAddress.get(address);
        return component != null ? component.getType() : null;
    }
    
    /**
     * Get all addresses for a component type
     */
    public Set<String> getAddressesByType(String type) {
        return componentsByType.getOrDefault(type, Collections.emptySet());
    }
    
    /**
     * Get primary component address for a type
     */
    public String getPrimary(String type) {
        return primaryComponents.get(type);
    }
    
    /**
     * Set primary component for a type
     */
    public void setPrimary(String type, String address) {
        if (componentsByAddress.containsKey(address)) {
            IComponent component = componentsByAddress.get(address);
            if (type.equals(component.getType())) {
                primaryComponents.put(type, address);
            }
        }
    }
    
    /**
     * Invoke method on component
     */
    public Object invoke(String address, String method, Object... args) throws ComponentException {
        IComponent component = componentsByAddress.get(address);
        if (component == null) {
            throw new ComponentException("No such component: " + address);
        }
        
        if (!component.isValid()) {
            throw new ComponentException("Component is invalid: " + address);
        }
        
        return component.invoke(method, args);
    }
    
    /**
     * Get component methods as Lua table
     */
    public LuaValue getMethods(String address) {
        IComponent component = componentsByAddress.get(address);
        if (component == null) {
            return LuaValue.NIL;
        }
        
        String[] methods = component.methods();
        LuaTable table = new LuaTable();
        
        for (int i = 0; i < methods.length; i++) {
            table.set(i + 1, LuaValue.valueOf(methods[i]));
        }
        
        return table;
    }
    
    /**
     * List components as Lua table
     */
    public LuaValue listComponents(String typeFilter) {
        LuaTable result = new LuaTable();
        
        for (Map.Entry<String, IComponent> entry : componentsByAddress.entrySet()) {
            IComponent component = entry.getValue();
            String type = component.getType();
            
            // Apply type filter if specified
            if (typeFilter != null && !typeFilter.equals(type)) {
                continue;
            }
            
            // Check if component is still valid
            if (!component.isValid()) {
                continue;
            }
            
            result.set(entry.getKey(), LuaValue.valueOf(type));
        }
        
        return result;
    }
    
    /**
     * Create component proxy as Lua table
     */
    public LuaTable createProxy(String address) {
        IComponent component = componentsByAddress.get(address);
        if (component == null || !component.isValid()) {
            return null;
        }
        
        LuaTable proxy = new LuaTable();
        
        // Add basic component info
        proxy.set("address", LuaValue.valueOf(component.getAddress()));
        proxy.set("type", LuaValue.valueOf(component.getType()));
        
        // Add component methods
        String[] methods = component.methods();
        for (String methodName : methods) {
            proxy.set(methodName, new ComponentMethodFunction(component, methodName));
        }
        
        return proxy;
    }
    
    /**
     * Get all component types
     */
    public Set<String> getComponentTypes() {
        return new HashSet<>(componentsByType.keySet());
    }
    
    /**
     * Get component count by type
     */
    public int getComponentCount(String type) {
        Set<String> addresses = componentsByType.get(type);
        return addresses != null ? addresses.size() : 0;
    }
    
    /**
     * Lua function for component method calls
     */
    private static class ComponentMethodFunction extends org.luaj.vm2.lib.LibFunction {
        private final IComponent component;
        private final String methodName;
        
        public ComponentMethodFunction(IComponent component, String methodName) {
            this.component = component;
            this.methodName = methodName;
        }
        
        @Override
        public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
            try {
                // Convert Lua args to Java objects
                Object[] javaArgs = new Object[args.narg()];
                for (int i = 0; i < javaArgs.length; i++) {
                    javaArgs[i] = luaToJava(args.arg(i + 1));
                }
                
                // Invoke component method
                Object result = component.invoke(methodName, javaArgs);
                
                // Convert result back to Lua
                return CoerceJavaToLua.coerce(result);
            } catch (ComponentException e) {
                return LuaValue.error(e.getMessage());
            } catch (Exception e) {
                return LuaValue.error("Component error: " + e.getMessage());
            }
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
            } else if (value.istable()) {
                // Convert table to map
                LuaTable table = value.checktable();
                Map<Object, Object> map = new HashMap<>();
                
                for (LuaValue key : table.keys()) {
                    map.put(luaToJava(key), luaToJava(table.get(key)));
                }
                return map;
            }
            
            return value.toString();
        }
    }
}