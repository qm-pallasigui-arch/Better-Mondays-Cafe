package persistence;

import staff.LeaveRequest;
import java.util.List;

public interface LeaveRequestRepository {

    void createRequest(String username, String startDate, String endDate) throws Exception;

    List<LeaveRequest> listAllRequests() throws Exception;

    List<LeaveRequest> listRequestsForUser(String username) throws Exception;

    void approveRequest(int id) throws Exception;

    void rejectRequest(int id) throws Exception;
}
