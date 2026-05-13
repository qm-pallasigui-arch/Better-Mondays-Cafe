package persistence.sqlite;

import persistence.AppDatabase;
import persistence.StaffShiftRepository;
import staff.StaffShift;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SQLiteStaffShiftRepository implements StaffShiftRepository {

    @Override
    public void startShift(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO staff_shifts(username) VALUES (?)")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public void endShift(String username, String notes) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                // find last open shift
                long shiftId = -1;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT id FROM staff_shifts WHERE username = ? AND ended_at IS NULL ORDER BY id DESC LIMIT 1")) {
                    find.setString(1, username);
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next()) shiftId = rs.getLong("id");
                    }
                }
                if (shiftId == -1) return;
                try (PreparedStatement upd = connection.prepareStatement(
                        "UPDATE staff_shifts SET ended_at = CURRENT_TIMESTAMP, notes = ? WHERE id = ?")) {
                    upd.setString(1, notes);
                    upd.setLong(2, shiftId);
                    upd.executeUpdate();
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
    public List<StaffShift> findShifts(String username) throws Exception {
        List<StaffShift> shifts = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "SELECT id, username, started_at, ended_at, notes FROM staff_shifts WHERE username = ? ORDER BY id DESC")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(new StaffShift(
                            rs.getLong("id"),
                            rs.getString("username"),
                            rs.getString("started_at"),
                            rs.getString("ended_at"),
                            rs.getString("notes")
                    ));
                }
            }
        }
        return shifts;
    }
}
