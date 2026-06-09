package persistence.sqlite;

import persistence.AppDatabase;
import persistence.StaffScheduleRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class SQLiteStaffScheduleRepository implements StaffScheduleRepository {

    @Override
    public void saveSchedule(String username, Map<Integer, String> dayShiftMap) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                // Remove existing schedule for this user
                try (PreparedStatement del = connection.prepareStatement(
                        "DELETE FROM staff_schedules WHERE username = ?")) {
                    del.setString(1, username);
                    del.executeUpdate();
                }
                // Insert each day's shift
                try (PreparedStatement ins = connection.prepareStatement(
                        "INSERT INTO staff_schedules(username, day_of_week, shift_type) VALUES (?, ?, ?)")) {
                    for (Map.Entry<Integer, String> entry : dayShiftMap.entrySet()) {
                        ins.setString(1, username);
                        ins.setInt(2, entry.getKey());
                        ins.setString(3, entry.getValue());
                        ins.executeUpdate();
                    }
                }
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(true);
            }
        }
    }

    @Override
    public Map<Integer, String> loadSchedule(String username) throws Exception {
        Map<Integer, String> schedule = new HashMap<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT day_of_week, shift_type FROM staff_schedules WHERE username = ?")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    schedule.put(rs.getInt("day_of_week"), rs.getString("shift_type"));
                }
            }
        }
        return schedule;
    }
}
