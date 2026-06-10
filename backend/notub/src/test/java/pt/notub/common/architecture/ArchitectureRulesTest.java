package pt.notub.common.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRulesTest {

    private static final Path SOURCE_ROOT = Path.of("src/main/java/pt/notub");

    @Test
    void controllersDoNotImportRepositories() throws IOException {
        List<Path> offenders = Files.walk(SOURCE_ROOT)
                .filter(path -> path.toString().endsWith("Controller.java"))
                .filter(this::importsRepositoryPackage)
                .collect(Collectors.toList());

        assertTrue(offenders.isEmpty(), "Controllers must not import repositories: " + offenders);
    }

    @Test
    void sourceDoesNotUseFindAllStreamFilterForRepositoryQueries() throws IOException {
        List<Path> offenders = Files.walk(SOURCE_ROOT)
                .filter(path -> path.toString().endsWith(".java"))
                .filter(this::containsFindAllStreamFilter)
                .collect(Collectors.toList());

        assertTrue(offenders.isEmpty(), "Repository filtering must stay in JPA queries: " + offenders);
    }

    private boolean importsRepositoryPackage(Path path) {
        try {
            return Files.readString(path).contains(".repository.");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean containsFindAllStreamFilter(Path path) {
        try {
            return Files.readString(path).contains("findAll().stream().filter(");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
