package loginregister;

public class UserAccount {

    private final String username;
    private final UserDataManager.Role role;
    private final String createdAt;

    public UserAccount(String username, UserDataManager.Role role, String createdAt) {
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    public String getUsername() {
        return username;
    }

    public UserDataManager.Role getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
