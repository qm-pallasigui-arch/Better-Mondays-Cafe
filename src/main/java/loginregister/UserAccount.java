package loginregister;

public class UserAccount {

    private final int staffId;
    private final String username;
    private final UserDataManager.Role role;
    private final String fullName;
    private final int age;
    private final String birthdate;
    private final String address;
    private final String mobile;
    private final String gender;
    private final String createdAt;

    public UserAccount(int staffId, String username, UserDataManager.Role role,
            String fullName, int age, String birthdate,
            String address, String mobile, String gender, String createdAt) {
        this.staffId = staffId;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.age = age;
        this.birthdate = birthdate;
        this.address = address;
        this.mobile = mobile;
        this.gender = gender;
        this.createdAt = createdAt;
    }

    /** Legacy constructor for code that doesn't have the new profile fields. */
    public UserAccount(String username, UserDataManager.Role role, String createdAt) {
        this(0, username, role, "", 0, "", "", "", "", createdAt);
    }

    public int getStaffId() { return staffId; }
    public String getUsername() { return username; }
    public UserDataManager.Role getRole() { return role; }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public String getBirthdate() { return birthdate; }
    public String getAddress() { return address; }
    public String getMobile() { return mobile; }
    public String getGender() { return gender; }
    public String getCreatedAt() { return createdAt; }
}
