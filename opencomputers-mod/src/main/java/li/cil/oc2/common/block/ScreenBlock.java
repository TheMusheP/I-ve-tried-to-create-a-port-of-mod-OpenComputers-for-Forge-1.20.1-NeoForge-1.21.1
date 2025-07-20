package li.cil.oc2.common.block;

import li.cil.oc2.common.blockentity.ScreenBlockEntity;
import li.cil.oc2.common.init.OCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Screen block - displays text and graphics from GPU components
 * Supports 3 tiers with different resolutions and color depths
 */
public class ScreenBlock extends BaseEntityBlock {
    
    private final int tier;
    
    public ScreenBlock(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }
    
    public int getTier() {
        return tier;
    }
    
    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ScreenBlockEntity(pos, state, tier);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, OCBlockEntities.SCREEN.get(),
            level.isClientSide ? ScreenBlockEntity::clientTick : ScreenBlockEntity::serverTick);
    }
}