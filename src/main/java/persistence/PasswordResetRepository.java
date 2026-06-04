package persistence;

import staff.PasswordResetRequest;
import java.util.List;

public interface PasswordResetRepository {

    /** Called from the login screen — user submits a reset request. */
    void createRequest(String username) throws Exception;

    /** Returns all requests ordered by requested_at DESC. */
    List<PasswordResetRequest> listAllRequests() throws Exception;

    /** Returns only PENDING requests. */
    List<PasswordResetRequest> listPendingRequests() throws Exception;

    /** Admin approves: applies newPasswordHash to users table, marks request APPROVED. */
    void approveRequest(int requestId, String username, String newPasswordHash) throws Exception;

    /** Admin rejects: marks request REJECTED. */
    void rejectRequest(int requestId) throws Exception;

    /** Returns true if this user already has a PENDING request. */
    boolean hasPendingRequest(String username) throws Exception;
}
