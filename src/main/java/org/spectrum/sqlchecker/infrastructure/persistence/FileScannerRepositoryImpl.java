package org.spectrum.sqlchecker.infrastructure.persistence;

import lombok.extern.slf4j.Slf4j;
import org.spectrum.sqlchecker.domain.scanner.repository.FileScannerRepository;
import org.spectrum.sqlchecker.domain.shared.enumeration.ProjectType;
import org.springframework.stereotype.Repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 文件扫描器仓储实现
 *
 * @author Spectrum SQL Checker
 * @since 1.0.0
 */
@Slf4j
@Repository
public class FileScannerRepositoryImpl implements FileScannerRepository {

    @Override
    public List<java.io.File> scanFiles(String rootPath, List<String> includes, List<String> excludes) {
        List<java.io.File> files = new ArrayList<>();
        Path root = Path.of(rootPath);

        if (!Files.exists(root)) {
            return files;
        }

        try (Stream<Path> paths = Files.walk(root)) {
            paths.filter(Files::isRegularFile)
                    .filter(p -> matchesInclude(p, includes))
                    .filter(p -> !matchesExclude(p, excludes))
                    .forEach(p -> files.add(p.toFile()));
        } catch (IOException e) {
            // Return partial results
        }

        return files;
    }

    @Override
    public String readFileContent(java.io.File file) {
        try {
            return Files.readString(file.toPath());
        } catch (IOException e) {
            log.error("Failed to read file: {}", file.getPath(), e);
            return "";
        }
    }

    @Override
    public ProjectType detectProjectType(String rootPath) {
        Path root = Path.of(rootPath);

        // 检查是否有 pom.xml (Maven)
        if (Files.exists(root.resolve("pom.xml"))) {
            return ProjectType.MAVEN;
        }

        // 检查是否有 build.gradle 或 build.gradle.kts (Gradle)
        if (Files.exists(root.resolve("build.gradle")) ||
            Files.exists(root.resolve("build.gradle.kts"))) {
            return ProjectType.GRADLE;
        }

        // 检查是否有 package.json (Node.js)
        if (Files.exists(root.resolve("package.json"))) {
            return ProjectType.NODEJS;
        }

        // 检查是否有 requirements.txt 或 setup.py (Python)
        if (Files.exists(root.resolve("requirements.txt")) ||
            Files.exists(root.resolve("setup.py"))) {
            return ProjectType.PYTHON;
        }

        return ProjectType.AUTO;
    }

    @Override
    public boolean isAccessible(String path) {
        try {
            Path p = Path.of(path);
            return Files.exists(p) && Files.isReadable(p);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public int getLineCount(java.io.File file) {
        try {
            return (int) Files.lines(file.toPath()).count();
        } catch (IOException e) {
            return 0;
        }
    }

    private boolean matchesInclude(Path path, List<String> includes) {
        if (includes == null || includes.isEmpty()) {
            return true;
        }
        String pathStr = path.toString();
        return includes.stream().anyMatch(pattern -> matchesPattern(pathStr, pattern));
    }

    private boolean matchesExclude(Path path, List<String> excludes) {
        if (excludes == null || excludes.isEmpty()) {
            return false;
        }
        String pathStr = path.toString();
        return excludes.stream().anyMatch(pattern -> matchesPattern(pathStr, pattern));
    }

    private boolean matchesPattern(String text, String pattern) {
        String regex = pattern
                .replace(".", "\\.")
                .replace("*", ".*")
                .replace("?", ".");
        return text.matches(regex);
    }
}
