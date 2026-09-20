package com.shivali.fileorganizer;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** Prints to the console and appends the same line to a log file on disk. */
public class OrganizerLogger implements AutoCloseable {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final PrintWriter fileWriter;
    private int filesMoved = 0;
    private int duplicatesFound = 0;

    public OrganizerLogger(Path logFile) throws IOException {
        this.fileWriter = new PrintWriter(Files.newBufferedWriter(
                logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND), true);
    }

    public void log(String message) {
        String line = "[" + LocalDateTime.now().format(TIME_FORMAT) + "] " + message;
        System.out.println(line);
        fileWriter.println(line);
    }

    public void recordMove() {
        filesMoved++;
    }

    public void recordDuplicate() {
        duplicatesFound++;
    }

    public void printSummary() {
        log("Summary: " + filesMoved + " file(s) organized, " + duplicatesFound + " duplicate(s) found.");
    }

    @Override
    public void close() {
        fileWriter.close();
    }
}
