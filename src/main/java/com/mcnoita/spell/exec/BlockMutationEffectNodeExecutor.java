package com.mcnoita.spell.exec;

import com.mcnoita.spell.plan.BlockMutationEffectNode;
import com.mcnoita.world.mutation.WorldMutationContext;
import com.mcnoita.world.mutation.WorldMutationKind;
import com.mcnoita.world.mutation.WorldMutationPolicy;
import com.mcnoita.world.mutation.WorldMutationService;
import com.mcnoita.world.mutation.WorldQueryService;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

/**
 * The node does not contain a replacement state or area-selection algorithm,
 * so the only safe representative is one line-of-sight BREAK operation.
 */
final class BlockMutationEffectNodeExecutor implements EffectNodeExecutor<BlockMutationEffectNode> {
    private static final int MAX_RAY_STEPS = 64;

    @Override
    public Class<BlockMutationEffectNode> nodeType() {
        return BlockMutationEffectNode.class;
    }

    @Override
    public void execute(BlockMutationEffectNode node, EffectExecutionContext context) {
        if (node.kind() != BlockMutationEffectNode.MutationKind.BREAK) {
            context.rejectNode(node, "block placement and replacement require a frozen replacement state");
            return;
        }
        if (node.maximumBlocks() != 1) {
            context.rejectNode(node, "area block selection is not represented by this effect node");
            return;
        }
        if (node.radius() <= 0.0 || node.radius() > WorldMutationPolicy.MAX_QUERY_DIAMETER) {
            context.rejectNode(node, "block mutation reach is outside the policy-bounded range");
            return;
        }

        WorldMutationContext mutationContext = context.worldMutationContext(node);
        Optional<BlockPos> target = firstSolidBlock(mutationContext, context.spawnPosition(), context.direction(), node.radius());
        if (target.isEmpty()) {
            context.rejectNode(node, "no policy-readable line-of-sight block was found");
            return;
        }
        if (!WorldMutationService.breakBlock(mutationContext, target.get(), true, context.player())) {
            context.rejectNode(node, "world mutation policy rejected the target block");
        }
    }

    private static Optional<BlockPos> firstSolidBlock(
        WorldMutationContext context, Vec3d origin, Vec3d direction, double reach
    ) {
        int steps = Math.min(MAX_RAY_STEPS, Math.max(1, (int) Math.ceil(reach)));
        Vec3d step = direction.normalize();
        Set<BlockPos> visited = new HashSet<>();
        for (int index = 1; index <= steps; index++) {
            double distance = Math.min(index, reach);
            BlockPos pos = BlockPos.ofFloored(origin.add(step.multiply(distance)));
            if (!visited.add(pos)) {
                continue;
            }
            Optional<BlockState> state = WorldQueryService.blockState(context, pos, WorldMutationKind.BLOCK_CHECK);
            if (state.isEmpty()) {
                return Optional.empty();
            }
            if (!state.get().isAir()) {
                return Optional.of(pos);
            }
        }
        return Optional.empty();
    }
}
