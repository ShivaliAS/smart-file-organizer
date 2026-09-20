package com.shivali.fileorganizer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Detects duplicate files by content, not by name — a file renamed or
 * re-downloaded with a different filename is still caught, because we
 * hash the actual bytes (SHA-256) rather than comparing names.
 *
 * The tracker rebuilds its index from files already sitting in the
 * category folders on construction, so duplicate detection survives an
 * application restart instead of resetting to empty each time.
 */
public class DuplicateTracker {

    private static final Set<String> CATEGORY_FOLDERS = Set.of(
            "Images", "Documents", "Videos", "Audio",
            "Archives", "Installers", "Code", "Others", "Duplicates"
    );

    private final Map<String, Path> hashToFirstSeen = new ConcurrentHashMap<>();

    public DuplicateTracker(Path rootDir) throws IOException {
        loadExistingFiles(rootDir);
    }

    /** Hashes files organized in an earlier run so restarts don't forget about them. */
    private void loadExistingFiles(Path rootDir) throws IOException {
        for (String category : CATEGORY_FOLDERS) {
            Path categoryDir = rootDir.resolve(category);
            if (!Files.isDirectory(categoryDir)) {
                continue;
            }

            try (var files = Files.walk(categoryDir)) {
                files.filter(Files::isRegularFile)
                        .filter(Files::isReadable)
                        .forEach(file -> {
                            try {
                                String hash = computeHash(file);
                                hashToFirstSeen.putIfAbsent(hash, file);
                            } catch (IOException e) {
                                // Skip files we can't currently read rather than failing startup
                            }
                        });
            }
        }
    }

    /**
     * Registers this file's hash. Returns the path of the earlier file with
     * identical content if one exists, or empty if this content is new.
     */
    public Optional<Path> checkAndRegister(Path file) throws IOException {
        String hash = computeHash(file);
        Path existing = hashToFirstSeen.putIfAbsent(hash, file);
        return Optional.ofNullable(existing);
    }

    private String computeHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream in = Files.newInputStream(file);
                 DigestInputStream digestIn = new DigestInputStream(in, digest)) {
                byte[] buffer = new byte[8192];
                while (digestIn.read(buffer) != -1) {
                    // DigestInputStream updates the digest as bytes are read
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is guaranteed available on every standard JVM
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
