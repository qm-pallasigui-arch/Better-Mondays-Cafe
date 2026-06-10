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
                        "INSERT INTO staff_shifts(username, started_at) VALUES (?, datetime('now','localtime'))")) {
            stmt.setString(1, username);
            stmt.executeUpdate();
        }
    }

    @Override
    public void endShift(String username, String notes) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long shiftId = -1;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT id FROM staff_shifts WHERE username = ? AND ended_at IS NULL ORDER BY id DESC LIMIT 1")) {
                    find.setString(1, username);
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next())
                            shiftId = rs.getLong("id");
                    }
                }
                if (shiftId == -1)
                    return;
                try (PreparedStatement upd = connection.prepareStatement(
                        "UPDATE staff_shifts SET ended_at = datetime('now','localtime'), notes = ?, status = 'completed' WHERE id = ?")) {
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
    public void markAsLate(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long shiftId = -1;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT id FROM staff_shifts WHERE username = ? AND ended_at IS NULL ORDER BY id DESC LIMIT 1")) {
                    find.setString(1, username);
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next())
                            shiftId = rs.getLong("id");
                    }
                }
                if (shiftId == -1)
                    return;
                try (PreparedStatement upd = connection.prepareStatement(
                        "UPDATE staff_shifts SET status = 'late' WHERE id = ?")) {
                    upd.setLong(1, shiftId);
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
    public void markAsAbsent(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection()) {
            connection.setAutoCommit(false);
            try {
                long shiftId = -1;
                try (PreparedStatement find = connection.prepareStatement(
                        "SELECT id FROM staff_shifts WHERE username = ? AND ended_at IS NULL ORDER BY id DESC LIMIT 1")) {
                    find.setString(1, username);
                    try (ResultSet rs = find.executeQuery()) {
                        if (rs.next())
                            shiftId = rs.getLong("id");
                    }
                }
                if (shiftId == -1)
                    return;
                try (PreparedStatement upd = connection.prepareStatement(
                        "UPDATE staff_shifts SET ended_at = datetime('now','localtime'), status = 'absent' WHERE id = ?")) {
                    upd.setLong(1, shiftId);
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
    public List<StaffShift> findTodayActiveShifts() throws Exception {
        List<StaffShift> shifts = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ss.id, COALESCE(u.staff_id, 0) AS staff_id, ss.username, "
                        + "ss.started_at, ss.ended_at, ss.notes, ss.status "
                        + "FROM staff_shifts ss "
                        + "LEFT JOIN users u ON u.username = ss.username "
                        + "WHERE date(ss.started_at) = date('now','localtime') "
                        + "AND ss.ended_at IS NULL "
                        + "ORDER BY ss.id ASC")) {
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRow(rs));
                }
            }
        }
        return shifts;
    }

    @Override
    public StaffShift findLatestShift(String username) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ss.id, COALESCE(u.staff_id, 0) AS staff_id, ss.username, "
                        + "ss.started_at, ss.ended_at, ss.notes, ss.status "
                        + "FROM staff_shifts ss "
                        + "LEFT JOIN users u ON u.username = ss.username "
                        + "WHERE ss.username = ? "
                        + "ORDER BY ss.id DESC LIMIT 1")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    @Override
    public List<StaffShift> findShifts(String username) throws Exception {
        List<StaffShift> shifts = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ss.id, COALESCE(u.staff_id, 0) AS staff_id, ss.username, "
                        + "ss.started_at, ss.ended_at, ss.notes, ss.status "
                        + "FROM staff_shifts ss "
                        + "LEFT JOIN users u ON u.username = ss.username "
                        + "WHERE ss.username = ? ORDER BY ss.id DESC")) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shifts.add(mapRow(rs));
                }
            }
        }
        return shifts;
    }

    @Override
    public List<StaffShift> findAllShifts() throws Exception {
        List<StaffShift> shifts = new ArrayList<>();
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "SELECT ss.id, COALESCE(u.staff_id, 0) AS staff_id, ss.username, "
                        + "ss.started_at, ss.ended_at, ss.notes, ss.status "
                        + "FROM staff_shifts ss "
                        + "LEFT JOIN users u ON u.username = ss.username "
                        + "ORDER BY ss.id DESC");
                ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                shifts.add(mapRow(rs));
            }
        }
        return shifts;
    }

    @Override
    public void updateShift(int id, String startedAt, String endedAt, String notes) throws Exception {
        try (Connection connection = AppDatabase.openConnection();
                PreparedStatement stmt = connection.prepareStatement(
                        "UPDATE staff_shifts SET started_at = ?, ended_at = ?, notes = ? WHERE id = ?")) {
            stmt.setString(1, startedAt.isBlank() ? null : startedAt);
            stmt.setString(2, endedAt.isBlank() ? null : endedAt);
            stmt.setString(3, notes);
            stmt.setInt(4, id);
            stmt.executeUpdate();
        }
    }

    private StaffShift mapRow(ResultSet rs) throws Exception {
        return new StaffShift(
                rs.getLong("id"),
                rs.getInt("staff_id"),
                rs.getString("username"),
                rs.getString("started_at"),
                rs.getString("ended_at"),
                rs.getString("notes"),
                rs.getString("status"));
    }
}
