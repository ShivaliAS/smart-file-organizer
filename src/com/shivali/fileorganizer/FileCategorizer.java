package com.shivali.fileorganizer;

import java.util.Map;

/**
 * Decides which category folder a file belongs in, based on its extension.
 * Also flags files that are still mid-download so we don't move them too early.
 */
public class FileCategorizer {

    private static final Map<String, String> EXTENSION_MAP = Map.ofEntries(
            // Images
            Map.entry("jpg", "Images"), Map.entry("jpeg", "Images"), Map.entry("png", "Images"),
            Map.entry("gif", "Images"), Map.entry("bmp", "Images"), Map.entry("svg", "Images"),
            Map.entry("webp", "Images"), Map.entry("heic", "Images"),

            // Documents
            Map.entry("pdf", "Documents"), Map.entry("doc", "Documents"), Map.entry("docx", "Documents"),
            Map.entry("xls", "Documents"), Map.entry("xlsx", "Documents"), Map.entry("ppt", "Documents"),
            Map.entry("pptx", "Documents"), Map.entry("txt", "Documents"), Map.entry("csv", "Documents"),
            Map.entry("md", "Documents"),

            // Videos
            Map.entry("mp4", "Videos"), Map.entry("mkv", "Videos"), Map.entry("mov", "Videos"),
            Map.entry("avi", "Videos"), Map.entry("wmv", "Videos"), Map.entry("flv", "Videos"),

            // Audio
            Map.entry("mp3", "Audio"), Map.entry("wav", "Audio"), Map.entry("flac", "Audio"),
            Map.entry("aac", "Audio"), Map.entry("m4a", "Audio"),

            // Archives
            Map.entry("zip", "Archives"), Map.entry("rar", "Archives"), Map.entry("7z", "Archives"),
            Map.entry("tar", "Archives"), Map.entry("gz", "Archives"),

            // Installers
            Map.entry("exe", "Installers"), Map.entry("msi", "Installers"),
            Map.entry("dmg", "Installers"), Map.entry("apk", "Installers"),

            // Code
            Map.entry("java", "Code"), Map.entry("py", "Code"), Map.entry("js", "Code"),
            Map.entry("ts", "Code"), Map.entry("html", "Code"), Map.entry("css", "Code"),
            Map.entry("json", "Code"), Map.entry("xml", "Code"), Map.entry("c", "Code"),
            Map.entry("cpp", "Code"), Map.entry("sql", "Code")
    );

    /** Extensions used by browsers for in-progress downloads. Never touch these. */
    private static final String[] INCOMPLETE_SUFFIXES = {".crdownload", ".part", ".partial", ".download", ".tmp"};

    public static String categorize(String fileName) {
        String ext = getExtension(fileName).toLowerCase();
        return EXTENSION_MAP.getOrDefault(ext, "Others");
    }

    public static boolean isIncompleteDownload(String fileName) {
        String lower = fileName.toLowerCase();
        for (String suffix : INCOMPLETE_SUFFIXES) {
            if (lower.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    private static String getExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot == -1 || dot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(dot + 1);
    }
}
