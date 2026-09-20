# Smart File Organizer

Watches your messy Downloads (or any) folder in real time and automatically sorts files into category folders the moment they finish downloading — and catches duplicate files by content, even if they were renamed.

No frameworks, no dependencies — just core Java (NIO.2, `WatchService`, `MessageDigest`). Good for showing off Java fundamentals beyond CRUD/Spring Boot work.

## Why it's fun to build

- **It feels like magic.** Drop a file into the watched folder and watch it teleport into the right subfolder within a couple seconds, live, while the program is running.
- **Real-world messiness, handled properly:** it waits for browser downloads to actually finish (skips `.crdownload` / `.part` files, and polls file size until it stops changing) instead of grabbing a half-written file.
- **Duplicate detection by content, not filename** — using SHA-256 hashing, so `photo.jpg` and `photo (2).jpg` with identical bytes both get flagged even though their names differ.
- **Name collisions handled** — moving two different files that happen to share a name doesn't overwrite either one; it appends `(1)`, `(2)`, etc.

## How it works

| Class | Responsibility |
|---|---|
| `Main` | Parses args, sets up logging, kicks off the initial sweep + live watch |
| `FileOrganizer` | Core logic: initial sweep, `WatchService` event loop, stability check, name-collision handling |
| `FileCategorizer` | Extension → category lookup table (Images, Documents, Videos, Audio, Archives, Installers, Code, Others) |
| `DuplicateTracker` | SHA-256 content hashing to catch duplicate files regardless of filename |
| `OrganizerLogger` | Timestamped logging to console + a log file left in the watched folder |

## Running it

Compile:
```
javac -d out $(find src -name "*.java")      # macOS/Linux
```
```
javac -d out (Get-ChildItem -Recurse -Filter *.java src | ForEach-Object { $_.FullName })   # Windows PowerShell
```

Run it on your Downloads folder:
```
java -cp out com.shivali.fileorganizer.Main
```

Or point it at any folder, and/or preview without actually moving anything:
```
java -cp out com.shivali.fileorganizer.Main "C:\Users\you\Desktop\messy-folder" --dry-run
```

It'll sort whatever's already there, then sit and watch for new files. Press `Ctrl+C` to stop — it'll print a summary of how many files it organized and how many duplicates it caught.

## Try it

1. Run it against a scratch folder with a pile of old downloads.
2. While it's running, download a new file (any type) into that folder — watch it get sorted within a couple seconds.
3. Copy an existing file, rename the copy, and drop it in — it should land in `Duplicates/` instead of its normal category.

## Possible extensions (good talking points for interviews)

- Swap the flat category map for a small rules file (JSON/YAML) so users can customize categories without recompiling.
- Add a JavaFX or Swing tray-icon UI instead of a console app.
- Multi-threaded hashing for large files/folders using an `ExecutorService`.
- Package as a native executable with `jpackage` so it can run without a visible terminal.
