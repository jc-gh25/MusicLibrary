package music.library;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Loads environment variables from a local .env file and makes them available
 * as system properties. Spring will automatically pick up those properties.
 *
 * Spring Boot already supports external configuration via `application.properties`
 * or environment variables, but this initializer loads a simple `.env`
 * file for development convenience.
 */
public class EnvLoader implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Logger log = LoggerFactory.getLogger(EnvLoader.class);
    private static final String ENV_FILE = ".env";

    @Override
    public void initialize(ConfigurableApplicationContext context) {
        Path envPath = Path.of(ENV_FILE);

        // If the file does not exist, just skip – CI/CD will typically set vars via Docker or environment.
        if (!Files.exists(envPath)) return;

        try (var lines = Files.lines(envPath)) {
            Map<String, String> props = lines
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#"))   // ignore comments & blanks
                .filter(l -> l.contains("="))
                .collect(java.util.stream.Collectors.toMap(
                    s -> s.split("=", 2)[0],
                    s -> {
                        String[] parts = s.split("=", 2);
                        return parts.length > 1 ? parts[1] : "";
                    }));

            // Populate System properties – Spring reads them automatically.
            props.forEach((k, v) -> System.setProperty(k, v));
        } catch (IOException e) {
            log.warn("Unable to read .env file at '{}': {}", envPath.toAbsolutePath(), e.getMessage());
        }
    }
}
