package persistence;

import java.util.List;
import loginregister.UserAccount;
import loginregister.UserDataManager.Role;

/**
 * Firebase-ready abstraction for account and role management.
 * A future Firebase adapter can implement this interface without changing UI
 * code.
 */
public interface AccountRoleRepository {

    List<UserAccount> listUsers() throws Exception;

    void updateUserRole(String username, Role role) throws Exception;

    void deleteUser(String username) throws Exception;

    void createUser(String username, String plainPassword, Role role,
            String fullName, int age, String birthdate,
            String address, String mobile, String gender) throws Exception;

    void updateUserDetails(String username, String fullName, int age, String birthdate,
            String address, String mobile, String gender) throws Exception;
}