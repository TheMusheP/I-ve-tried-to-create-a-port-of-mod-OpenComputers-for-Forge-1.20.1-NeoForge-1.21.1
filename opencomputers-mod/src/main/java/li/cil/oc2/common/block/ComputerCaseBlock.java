package li.cil.oc2.common.block;

import li.cil.oc2.common.blockentity.ComputerCaseBlockEntity;
import li.cil.oc2.common.init.OCBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

/**
 * Computer Case block - main computer housing with component slots
 * Supports 3 tiers with different slot counts and energy capacity
 */
public class ComputerCaseBlock extends BaseEntityBlock {
    
    private final int tier;
    
    public ComputerCaseBlock(int tier, Properties properties) {
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
    
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ComputerCaseBlockEntity computerCase) {
                NetworkHooks.openScreen((ServerPlayer) player, computerCase, pos);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ComputerCaseBlockEntity(pos, state);
    }
    
    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, OCBlockEntities.COMPUTER_CASE.get(),
            level.isClientSide ? ComputerCaseBlockEntity::clientTick : ComputerCaseBlockEntity::serverTick);
    }
}