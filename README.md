# Smart File Organizer

My Downloads folder was basically a landfill — installers, screenshots, random PDFs, all sitting in one place forever. So I built this to fix it: it watches a folder in real time and sorts new files into subfolders by type the second they finish downloading. It also catches duplicate files by content (not just filename), using SHA-256 hashing, so a renamed copy of something still gets flagged.

Pure Java — no Spring, no external libraries. Just java.nio.file.WatchService and MessageDigest from the standard library. Wanted something outside my usual Spring Boot work that actually used core Java properly.

## What it does

- Watches a folder and sorts files into category folders (Images, Documents, Videos, Audio, Archives, Installers, Code, Others) as soon as they land
- Waits for downloads to actually finish before touching them — skips .crdownload / .part files and checks that file size has stopped changing before moving anything
- Flags duplicate files by content using SHA-256, even if the file's been renamed
- Won't overwrite files — if two different files share a name, it appends (1), (2), etc.
- Remembers what it's already organized across restarts, so duplicate detection doesn't reset to zero every time you run it

## Project structure

- Main — entry point, handles args and logging setup
- FileOrganizer — the actual logic: initial sweep, watch loop, stability checks
- FileCategorizer — maps file extensions to categories
- DuplicateTracker — hashes files and tracks duplicates, rebuilds its index from already-sorted files on startup
- OrganizerLogger — logs to console and to a log file left in the watched folder

## Running it

Compile:

    javac -d out $(find src -name "*.java")      # macOS/Linux

    javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })   # Windows PowerShell

Run it on your Downloads folder:

    java -cp out com.shivali.fileorganizer.Main

Or point it at a different folder, or do a dry run first to see what it would do without actually moving anything:

    java -cp out com.shivali.fileorganizer.Main "C:\Users\you\Desktop\messy-folder" --dry-run

It sorts what's already there, then keeps watching for new files. Ctrl+C to stop — it prints a quick summary before it exits.

## Try it out

1. Point it at a folder full of old downloads and run it.
2. While it's running, drop a new file into that folder — it should get sorted within a couple seconds.
3. Copy an existing file, rename the copy, and drop that in too — it should land in Duplicates/ instead of getting sorted normally.

## What I'd add if I kept going

- A config file for categories instead of hardcoding the extension map
- A simple tray-icon UI so it doesn't need a terminal window open
- Multi-threaded hashing for folders with a lot of files
