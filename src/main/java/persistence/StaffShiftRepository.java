package persistence;

import java.util.List;
import staff.StaffShift;

public interface StaffShiftRepository {

    void startShift(String username) throws Exception;

    void endShift(String username, String notes) throws Exception;

    List<StaffShift> findShifts(String username) throws Exception;

    List<StaffShift> findAllShifts() throws Exception;

    void updateShift(int id, String startedAt, String endedAt, String notes) throws Exception;
}