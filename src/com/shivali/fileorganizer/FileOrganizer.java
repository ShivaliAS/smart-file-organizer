package com.shivali.fileorganizer;

import java.io.IOException;
import java.nio.file.*;
import java.util.Optional;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.OVERFLOW;

/**
 * Watches a folder (e.g. Downloads) and automatically sorts files into
 * category subfolders as they arrive, catching duplicates by content along
 * the way. Built to solve the very ordinary problem of a Downloads folder
 * nobody ever cleans up.
 */
public class FileOrganizer {

    private static final int STABLE_CHECKS_REQUIRED = 2;
    private static final int MAX_STABILITY_ATTEMPTS = 20; // ~10s max wait for a file to finish writing
    private static final long POLL_INTERVAL_MS = 500;

    private final Path rootDir;
    private final boolean dryRun;
    private final DuplicateTracker duplicateTracker;
    private final OrganizerLogger logger;

    public FileOrganizer(Path rootDir, boolean dryRun, OrganizerLogger logger) throws IOException {
        this.rootDir = rootDir;
        this.dryRun = dryRun;
        this.logger = logger;
        this.duplicateTracker = new DuplicateTracker(rootDir);
    }

    /** Sorts whatever is already sitting in rootDir before we start watching for new arrivals. */
    public void organizeExistingFiles() throws IOException {
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(rootDir)) {
            for (Path entry : entries) {
                if (Files.isRegularFile(entry)) {
                    organizeFile(entry);
                }
            }
        }
    }

    /** Blocks forever, sorting new files the moment they finish downloading. */
    public void watch() throws IOException, InterruptedException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        rootDir.register(watchService, ENTRY_CREATE);
        logger.log("Watching " + rootDir + " for new files... (Ctrl+C to stop)");

        while (true) {
            WatchKey key = watchService.take(); // blocks until something happens

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == OVERFLOW) {
                    continue;
                }
                Path fileName = (Path) event.context();
                Path fullPath = rootDir.resolve(fileName);

                if (!Files.isRegularFile(fullPath)) {
                    continue; // skip directories, including the category folders we just created
                }
                if (FileCategorizer.isIncompleteDownload(fileName.toString())) {
                    continue; // browser hasn't finished writing this one yet
                }

                if (waitUntilStable(fullPath)) {
                    organizeFile(fullPath);
                }
            }

            boolean stillValid = key.reset();
            if (!stillValid) {
                logger.log("Watch directory is no longer accessible. Stopping.");
                break;
            }
        }
    }

    private void organizeFile(Path file) {
        try {
            String fileName = file.getFileName().toString();

            if (fileName.equals(Main.LOG_FILE_NAME) || FileCategorizer.isIncompleteDownload(fileName)) {
                return;
            }

            Optional<Path> duplicateOf = duplicateTracker.checkAndRegister(file);
            String category = duplicateOf.isPresent() ? "Duplicates" : FileCategorizer.categorize(fileName);
            Path targetDir = rootDir.resolve(category);
            Path targetPath = resolveNameCollision(targetDir, fileName);

            if (dryRun) {
                logger.log("[DRY RUN] Would move " + fileName + " -> " + category + "/" + targetPath.getFileName());
            } else {
                Files.createDirectories(targetDir);
                Files.move(file, targetPath);
                logger.log("Moved " + fileName + " -> " + category + "/" + targetPath.getFileName());
            }

            logger.recordMove();
            if (duplicateOf.isPresent()) {
                logger.recordDuplicate();
                logger.log("  (duplicate content of " + duplicateOf.get().getFileName() + ")");
            }
        } catch (IOException e) {
            logger.log("Skipped " + file.getFileName() + " — " + e.getMessage());
        }
    }

    /** If a same-named file already exists at the destination, append " (1)", " (2)", etc. */
    private Path resolveNameCollision(Path targetDir, String fileName) {
        Path candidate = targetDir.resolve(fileName);
        if (!Files.exists(candidate)) {
            return candidate;
        }

        String baseName = fileName;
        String extension = "";
        int dot = fileName.lastIndexOf('.');
        if (dot > 0) {
            baseName = fileName.substring(0, dot);
            extension = fileName.substring(dot);
        }

        int counter = 1;
        do {
            candidate = targetDir.resolve(baseName + " (" + counter + ")" + extension);
            counter++;
        } while (Files.exists(candidate));

        return candidate;
    }

    /**
     * Waits until a file's size stops changing before touching it, so we never grab
     * a file mid-write. Gives up and proceeds anyway after MAX_STABILITY_ATTEMPTS.
     */
    private boolean waitUntilStable(Path file) throws InterruptedException {
        long previousSize = -1;
        int stableCount = 0;

        for (int attempt = 0; attempt < MAX_STABILITY_ATTEMPTS; attempt++) {
            if (!Files.exists(file)) {
                return false;
            }
            long currentSize;
            try {
                currentSize = Files.size(file);
            } catch (IOException e) {
                return false;
            }

            if (currentSize == previousSize) {
                stableCount++;
                if (stableCount >= STABLE_CHECKS_REQUIRED) {
                    return true;
                }
            } else {
                stableCount = 0;
            }
            previousSize = currentSize;
            Thread.sleep(POLL_INTERVAL_MS);
        }
        return true; // proceed anyway rather than waiting forever
    }
}
