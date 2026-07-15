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
    private static final List<Path> POLICY_SOURCES = List.of(
        Path.of("src", "main", "java", "com", "mcnoita", "entity", "SparkBoltProjectileEntity.java"),
        Path.of("src", "main", "java", "com", "mcnoita", "entity", "BombEntity.java"),
        Path.of("src", "main", "java", "com", "mcnoita", "world", "NoitaTemporaryLightManager.java")
    );
    private static final List<String> FORBIDDEN_RAW_CALLS = List.of(
        ".createExplosion(", ".breakBlock(", ".setBlockState(", ".spawnEntity(",
        ".getOtherEntities(", ".getWorld().raycast(", "FallingBlockEntity.spawnFromBlock(", ".requestTeleport("
    );
    private static final List<String> SCOPED_READ_SOURCES = List.of(
        "SparkBoltProjectileEntity.java", "BombEntity.java", "NoitaTemporaryLightManager.java"
    );

    @Test
    void spellWorldCodeUsesTheWorldMutationBoundary() throws IOException {
        for (Path source : POLICY_SOURCES) {
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

    @Test
    void bombAndTemporaryLightDoNotReadBlocksOutsideWorldQueryService() throws IOException {
        for (Path source : POLICY_SOURCES) {
            if (!SCOPED_READ_SOURCES.contains(source.getFileName().toString())) {
                continue;
            }
            for (String line : Files.readAllLines(source)) {
                if (line.contains("WorldQueryService.") || line.contains("WorldMutationService.")) {
                    continue;
                }
                assertFalse(line.contains(".getBlockState("),
                    () -> source + " bypasses WorldQueryService: " + line.trim());
                assertFalse(line.contains(".setBlockState("),
                    () -> source + " bypasses WorldMutationService: " + line.trim());
            }
        }
    }
}
