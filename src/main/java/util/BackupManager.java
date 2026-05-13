package util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public final class BackupManager {
    private BackupManager() {}

    public static Path backupDatabase(Path dbPath) throws IOException {
        Path backups = dbPath.getParent().resolve("backups");
        Files.createDirectories(backups);
        String ts = ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        Path dest = backups.resolve("coffee-cafe-" + ts + ".db");
        Files.copy(dbPath, dest, StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }
}
