package debug;

public class StartupProbe {
    public static void main(String[] args) {
        try {
            System.out.println("Probing UserDataManager init...");
            java.util.List<?> users = loginregister.UserDataManager.listUsers();
            System.out.println("User list size: " + users.size());
        } catch (Throwable t) {
            System.err.println("Startup probe failed:");
            t.printStackTrace(System.err);
            System.exit(2);
        }
    }
}
