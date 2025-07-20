package li.cil.oc2.api.component;

/**
 * Component interface for OpenComputers components
 * All hardware components (GPU, screen, filesystem, etc.) implement this
 */
public interface IComponent {
    
    /**
     * Get component's unique address (UUID)
     */
    String getAddress();
    
    /**
     * Get component type (gpu, screen, filesystem, eeprom, etc.)
     */
    String getType();
    
    /**
     * Invoke a method on this component
     * @param method Method name to invoke
     * @param args Method arguments
     * @return Method result or null
     */
    Object invoke(String method, Object... args) throws ComponentException;
    
    /**
     * Get all available methods for this component
     * @return Array of method names
     */
    String[] methods();
    
    /**
     * Get documentation for a specific method
     * @param method Method name
     * @return Method documentation
     */
    String doc(String method);
    
    /**
     * Check if component is connected and functional
     */
    boolean isValid();
    
    /**
     * Called when component is attached to a computer
     */
    default void onConnect() {}
    
    /**
     * Called when component is detached from a computer
     */
    default void onDisconnect() {}
}