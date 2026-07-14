package com.mcnoita.world.mutation;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Prevents spell entities from bypassing the policy after the G05 migration. */
@Tag("architecture")
class WorldMutationArchitectureTest {
    private static final List<String> ENTITY_SOURCES = List.of(
        "SparkBoltProjectileEntity.java", "BombEntity.java"
    );
    private static final List<String> FORBIDDEN_RAW_CALLS = List.of(
        ".createExplosion(", ".breakBlock(", ".setBlockState(", ".spawnEntity(",
        ".getOtherEntities(", "FallingBlockEntity.spawnFromBlock("
    );

    @Test
    void spellEntitiesUseTheWorldMutationBoundary() throws IOException {
        Path entityRoot = Path.of("src", "main", "java", "com", "mcnoita", "entity");
        for (String sourceName : ENTITY_SOURCES) {
            Path source = entityRoot.resolve(sourceName);
            for (String line : Files.readAllLines(source)) {
                if (line.contains("WorldMutationService.") || line.contains("WorldQueryService.")) {
                    continue;
                }
                for (String forbidden : FORBIDDEN_RAW_CALLS) {
                    assertFalse(line.contains(forbidden), () -> source + " bypasses the world mutation boundary: " + line.trim());
                }
            }
        }
    }
}
