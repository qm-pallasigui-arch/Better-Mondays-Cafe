package persistence;

import loginregister.UserDataManager.Role;
import java.util.List;
import loginregister.UserAccount;

public interface UserRepository {

    void saveUser(String username, String plainPassword, Role role) throws Exception;

    boolean verifyCredentials(String username, String plainPassword) throws Exception;

    Role getUserRole(String username) throws Exception;

    List<UserAccount> listUsers() throws Exception;

    void updateUserRole(String username, Role role) throws Exception;

    void updateUsername(String currentUsername, String newUsername, String currentPassword) throws Exception;

    void updatePassword(String username, String currentPassword, String newPassword) throws Exception;
}
