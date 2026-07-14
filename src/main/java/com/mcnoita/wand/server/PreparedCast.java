package com.mcnoita.wand.server;

import com.mcnoita.catalog.CatalogSnapshot;
import com.mcnoita.spell.plan.ResolvedCast;
import com.mcnoita.spell.server.budget.BudgetRequest;
import com.mcnoita.wand.adapter.MinecraftWandAdapter;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.item.ItemStack;

/** Evaluated, frozen cast whose only mutable stack is an isolated replacement copy. */
public record PreparedCast(
    CastIntent intent,
    CastBinding binding,
    CatalogSnapshot snapshot,
    MinecraftWandAdapter.LoadedWand loadedWand,
    ResolvedCast resolvedCast,
    ItemStack replacementStack,
    UUID executionId,
    BudgetRequest budgetRequest,
    long serverTick
) {
    public PreparedCast {
        intent = Objects.requireNonNull(intent, "intent");
        binding = Objects.requireNonNull(binding, "binding");
        snapshot = Objects.requireNonNull(snapshot, "snapshot");
        loadedWand = Objects.requireNonNull(loadedWand, "loadedWand");
        resolvedCast = Objects.requireNonNull(resolvedCast, "resolvedCast");
        replacementStack = Objects.requireNonNull(replacementStack, "replacementStack");
        executionId = Objects.requireNonNull(executionId, "executionId");
        budgetRequest = Objects.requireNonNull(budgetRequest, "budgetRequest");
        if (serverTick < 0L || resolvedCast.status() != ResolvedCast.Status.ACCEPTED
            || !executionId.equals(budgetRequest.executionId())
            || resolvedCast.catalogEpoch() != snapshot.epoch()
            || !resolvedCast.catalogHash().equals(snapshot.hash())) {
            throw new IllegalArgumentException("prepared cast must retain one accepted catalog snapshot and execution identity");
        }
    }
}
