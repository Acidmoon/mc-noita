package com.mcnoita.wand.eval;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/** Guards the evaluator boundary without starting a Minecraft world. */
@Tag("architecture")
class PureEvaluatorArchitectureTest {
    private static final List<String> PURE_PACKAGES = List.of(
        "com/mcnoita/wand/model", "com/mcnoita/wand/eval", "com/mcnoita/spell/action", "com/mcnoita/spell/plan"
    );
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
        "import net.minecraft", "import net.fabricmc", "import com.mcnoita.entity", "import com.mcnoita.item",
        "import com.mcnoita.network", "import com.mcnoita.world", "import com.mcnoita.client", "import com.mcnoita.persistence"
    );

    @Test
    void pureEvaluatorPackagesDoNotDependOnMinecraftOrExecutionLayers() throws IOException {
        Path sourceRoot = Path.of("src", "main", "java");
        for (String packagePath : PURE_PACKAGES) {
            try (Stream<Path> files = Files.walk(sourceRoot.resolve(packagePath))) {
                for (Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                    String source = Files.readString(file);
                    for (String forbiddenImport : FORBIDDEN_IMPORTS) {
                        assertFalse(source.contains(forbiddenImport), () -> file + " imports forbidden dependency " + forbiddenImport);
                    }
                }
            }
        }
    }
}
