package com.shivali.fileorganizer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {

    public static final String LOG_FILE_NAME = "organizer-log.txt";

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean dryRun = false;
        String folderArg = null;

        for (String arg : args) {
            if (arg.equals("--dry-run")) {
                dryRun = true;
            } else {
                folderArg = arg;
            }
        }

        Path targetDir = (folderArg != null)
                ? Paths.get(folderArg)
                : Paths.get(System.getProperty("user.home"), "Downloads");

        if (!java.nio.file.Files.isDirectory(targetDir)) {
            System.err.println("Not a directory: " + targetDir);
            System.exit(1);
        }

        try (OrganizerLogger logger = new OrganizerLogger(targetDir.resolve(LOG_FILE_NAME))) {
            logger.log("Smart File Organizer starting on: " + targetDir + (dryRun ? " (dry run)" : ""));

            FileOrganizer organizer = new FileOrganizer(targetDir, dryRun, logger);

            Runtime.getRuntime().addShutdownHook(new Thread(logger::printSummary));

            organizer.organizeExistingFiles();
            organizer.watch(); // blocks until Ctrl+C
        }
    }
}
