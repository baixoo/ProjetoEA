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
    void controllersStayTypedAndThin() throws IOException {
        List<Path> offenders = Files.walk(SOURCE_ROOT)
                .filter(path -> path.toString().endsWith("Controller.java"))
                .filter(this::hasControllerAntiPattern)
                .collect(Collectors.toList());

        assertTrue(offenders.isEmpty(), "Controllers must stay typed and thin: " + offenders);
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
            String content = Files.readString(path);
            return content.contains(".repository.")
                    || content.contains(".entity.")
                    || content.contains(".mapper.");
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private boolean hasControllerAntiPattern(Path path) {
        try {
            String content = Files.readString(path);
            return content.contains("ResponseEntity<?>")
                    || content.contains("ResponseEntity.notFound(")
                    || content.contains("ResponseEntity.badRequest(")
                    || content.contains("Map.of(")
                    || content.contains("try {")
                    || content.lines().anyMatch(line -> line.contains("ResponseEntity.status(")
                    && !line.contains("HttpStatus.CREATED")
                    && !line.contains("HttpStatus.NO_CONTENT"));
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
