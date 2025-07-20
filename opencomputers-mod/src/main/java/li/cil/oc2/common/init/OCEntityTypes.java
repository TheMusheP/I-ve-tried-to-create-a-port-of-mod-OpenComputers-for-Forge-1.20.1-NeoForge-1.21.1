package li.cil.oc2.common.init;

import li.cil.oc2.OpenComputersMod;
import li.cil.oc2.common.entity.RobotEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Registration of all OpenComputers entities
 */
public class OCEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, OpenComputersMod.MOD_ID);
    
    public static final RegistryObject<EntityType<RobotEntity>> ROBOT = 
        ENTITY_TYPES.register("robot", () -> 
            EntityType.Builder.of(RobotEntity::new, MobCategory.MISC)
                .sized(0.6F, 1.8F)
                .clientTrackingRange(8)
                .build("robot"));
}