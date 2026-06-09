package persistence;

import java.util.Map;

public interface StaffScheduleRepository {

    void saveSchedule(String username, Map<Integer, String> dayShiftMap) throws Exception;

    Map<Integer, String> loadSchedule(String username) throws Exception;
}
