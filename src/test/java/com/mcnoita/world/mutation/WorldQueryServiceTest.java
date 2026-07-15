package com.mcnoita.world.mutation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Pure regression coverage for bounded entity collection and explosion safety. */
@Tag("regression")
class WorldQueryServiceTest {
    @Test
    void resultLimitIsClampedBeforeTheMinecraftTraversalStarts() {
        assertEquals(0, WorldQueryService.boundedResultLimit(-1));
        assertEquals(1, WorldQueryService.boundedResultLimit(1));
        assertEquals(WorldMutationPolicy.MAX_QUERY_RESULTS,
            WorldQueryService.boundedResultLimit(WorldMutationPolicy.MAX_QUERY_RESULTS + 1));
        assertEquals(WorldMutationPolicy.MAX_QUERY_RESULTS, WorldQueryService.boundedResultLimit(Integer.MAX_VALUE));
    }

    @Test
    void explosionEntityLimitIsBoundedBeforeCollection() {
        assertEquals(5, WorldMutationService.boundedExplosionEntityLimit(1.0f));
        assertEquals(WorldMutationPolicy.MAX_QUERY_RESULTS, WorldMutationService.boundedExplosionEntityLimit(16.0f));
    }

    @Test
    void raycastChecksAreBoundedBeforeMinecraftCanTraverseBlocks() {
        assertEquals(3, WorldMutationPolicy.raycastBlockChecks(
            new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0), new net.minecraft.util.math.Vec3d(1.1, 0.0, 0.0)
        ));
        assertEquals(0, WorldMutationPolicy.raycastBlockChecks(
            new net.minecraft.util.math.Vec3d(0.0, 0.0, 0.0), new net.minecraft.util.math.Vec3d(64.1, 0.0, 0.0)
        ));
    }

    @Test
    void servicesKeepUnboundedVanillaEntityCollectionOutOfTheBoundary() throws IOException {
        String querySource = Files.readString(Path.of(
            "src", "main", "java", "com", "mcnoita", "world", "mutation", "WorldQueryService.java"
        )).replace("\r\n", "\n");
        assertTrue(querySource.contains(".collectEntitiesByType("));
        assertTrue(querySource.contains("tryEntities"));
        assertTrue(querySource.contains("permitsRaycast"));
        assertTrue(querySource.contains("candidates,\n            limit"));
        assertFalse(querySource.contains(".getOtherEntities("));
        assertFalse(querySource.contains(".getEntitiesByType("));

        String mutationSource = Files.readString(Path.of(
            "src", "main", "java", "com", "mcnoita", "world", "mutation", "WorldMutationService.java"
        )).replace("\r\n", "\n");
        assertTrue(mutationSource.contains("if (!terrainRequested)"));
        assertFalse(mutationSource.contains(".createExplosion("));
    }
}
