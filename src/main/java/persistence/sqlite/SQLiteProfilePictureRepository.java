package persistence.sqlite;

import persistence.AppDatabase;
import persistence.ProfilePictureRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class SQLiteProfilePictureRepository implements ProfilePictureRepository {

    public SQLiteProfilePictureRepository() throws Exception {
        try {
            AppDatabase.ensureInitialized();
        } catch (Exception e) {
            throw new Exception("Unable to initialize picture repository", e);
        }
    }

    @Override
    public void savePicture(String username, byte[] imageData) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "INSERT INTO profile_pictures(username, image_data) VALUES (?, ?) " +
                                "ON CONFLICT(username) DO UPDATE SET image_data = excluded.image_data")) {
            stmt.setString(1, username);
            stmt.setBytes(2, imageData);
            stmt.executeUpdate();
        }
    }

    @Override
    public byte[] loadPicture(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "SELECT image_data FROM profile_pictures WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getBytes("image_data");
                }
            }
        }
        return null;
    }

    @Override
    public void deletePicture(String username) throws Exception {
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement stmt = conn.prepareStatement(
                        "DELETE FROM profile_pictures WHERE username = ?")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }
}
