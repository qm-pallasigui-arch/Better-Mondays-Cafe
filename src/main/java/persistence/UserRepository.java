package persistence;

import loginregister.UserDataManager.Role;

public interface UserRepository {

    void saveUser(String username, String plainPassword, Role role) throws Exception;

    boolean verifyCredentials(String username, String plainPassword) throws Exception;

    Role getUserRole(String username) throws Exception;
}
