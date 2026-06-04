package loginregister;

public class UserAccount {

    private final String username;
    private final UserDataManager.Role role;
    private final String createdAt;

    // --- New personal profile fields (null until saved) ---
    private String fullName;
    private String dateOfBirth; // "YYYY-MM-DD"
    private String email;
    private String employmentStart; // "YYYY-MM-DD"

    // --- Original constructor — nothing else in your codebase needs to change ---
    public UserAccount(String username, UserDataManager.Role role, String createdAt) {
        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
    }

    // --- Extended constructor used when loading profile data from DB ---
    public UserAccount(
            String username,
            UserDataManager.Role role,
            String createdAt,
            String fullName,
            String dateOfBirth,
            String email,
            String employmentStart) {

        this.username = username;
        this.role = role;
        this.createdAt = createdAt;
        this.fullName = fullName;
        this.dateOfBirth = dateOfBirth;
        this.email = email;
        this.employmentStart = employmentStart;
    }

    // --- Original getters ---
    public String getUsername() {
        return username;
    }

    public UserDataManager.Role getRole() {
        return role;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    // --- New profile getters ---
    public String getFullName() {
        return fullName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public String getEmail() {
        return email;
    }

    public String getEmploymentStart() {
        return employmentStart;
    }
}