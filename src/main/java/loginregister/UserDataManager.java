package loginregister;

import java.io.FileOutputStream;
import java.io.IOException;

public class UserDataManager {

    private static final String FILE_PATH = "users.txt";

    public static void saveUser(String username, String password) throws IOException {
        String data = username + "," + password + "\n";
        try (FileOutputStream fos = new FileOutputStream(FILE_PATH, true)) {
            fos.write(data.getBytes());
        }
    }
}
