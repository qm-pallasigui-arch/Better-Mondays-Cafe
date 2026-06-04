package loginregister;

import pos.POSSystem;
import ui.AppTheme;
import util.FieldAssist;
import java.awt.Dimension;
import java.util.stream.Collectors;
import persistence.StaffShiftRepository;
import persistence.sqlite.SQLiteStaffShiftRepository;

/**
 *
 * @author Miguel
 */
public class Login extends javax.swing.JFrame {

    public Login() {
        initComponents();
        setResizable(true);
        AppTheme.applyResponsiveFrameSize(this, 0.38, 0.55, new Dimension(700, 520));
        jPanel2.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        stylePrimaryButton(jButton1);
        stylePrimaryButton(jButton2);
        jButton1.setText("Forgot Password");
        jButton1.setVisible(true);
        jButton1.setEnabled(true);
        jButton1.addActionListener(evt -> showForgotPasswordDialog());
        FieldAssist.installAutocomplete(jTextField1, () -> UserDataManager.listUsers().stream()
                .map(UserAccount::getUsername)
                .collect(Collectors.toList()));
        AppTheme.applyToFrame(this);

        if (UserDataManager.listUsers().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "No user accounts were found. A default admin account has been created:\n"
                            + "Username: admin\nPassword: Admin123!",
                    "Initial Account Created",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated
    // Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel2 = new javax.swing.JPanel();
        jPasswordField1 = new javax.swing.JPasswordField();
        jTextField1 = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jButton1 = new javax.swing.JButton();
        jButton2 = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Better Mondays Coffeee Cafe Management System");
        setBackground(new java.awt.Color(204, 204, 204));
        setIconImage(new javax.swing.ImageIcon(getClass().getResource("/images/logo.png")).getImage());
        setResizable(true);

        jPanel2.setBackground(new java.awt.Color(29, 44, 63));

        jPasswordField1.setForeground(new java.awt.Color(51, 51, 51));
        jPasswordField1.setToolTipText("Enter your password");

        jTextField1.setForeground(new java.awt.Color(51, 51, 51));
        jTextField1.setToolTipText("Enter your username");
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setText("Username");

        jLabel2.setForeground(new java.awt.Color(255, 255, 255));
        jLabel2.setText("Password");

        jButton1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton1.setText("Forgot Password");

        jButton2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton2.setText("Login");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/logo.png"))); // NOI18N

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap(208, Short.MAX_VALUE)
                                .addComponent(jLabel3)
                                .addGap(208, 208, 208))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(150, 150, 150)
                                .addGroup(jPanel2Layout
                                        .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(jLabel2)
                                        .addComponent(jLabel1)
                                        .addComponent(jPasswordField1, javax.swing.GroupLayout.DEFAULT_SIZE, 323,
                                                Short.MAX_VALUE)
                                        .addComponent(jTextField1))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(jPanel2Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                                        .addComponent(jButton2)
                                        .addComponent(jButton1))
                                .addGap(0, 0, Short.MAX_VALUE)));

        jPanel2Layout.setVerticalGroup(
                jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                                .addContainerGap(29, Short.MAX_VALUE)
                                .addComponent(jLabel3)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jButton2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jButton1)
                                .addGap(62, 62, 62)));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(30, 30, 30)));
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE,
                                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGap(30, 30, 30)));

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jTextField1ActionPerformed
    }// GEN-LAST:event_jTextField1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
        String username = jTextField1.getText().trim();
        String password = new String(jPasswordField1.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserDataManager.verifyCredentials(username, password)) {
            UserDataManager.Role userRole = UserDataManager.getUserRole(username);

            // ── Auto-start shift on login ──────────────────────────────────────
            try {
                StaffShiftRepository shiftRepo = new SQLiteStaffShiftRepository();
                shiftRepo.startShift(username);
            } catch (Exception e) {
                // Non-fatal: log the error but still allow the user to proceed
                java.util.logging.Logger.getLogger(Login.class.getName())
                        .log(java.util.logging.Level.WARNING,
                                "Could not auto-start shift for " + username, e);
                javax.swing.JOptionPane.showMessageDialog(this,
                        "Login successful, but shift could not be started automatically.\n" + e.getMessage(),
                        "Shift Warning", javax.swing.JOptionPane.WARNING_MESSAGE);
            }
            // ──────────────────────────────────────────────────────────────────

            new POSSystem(username, userRole).setVisible(true);
            this.dispose();
        } else {
            String errorMessage = "Invalid username or password.";
            if ("admin".equalsIgnoreCase(username) && "Admin123!".equals(password)) {
                errorMessage += "\nIf this is a previously created admin account, the default password may no longer apply."
                        + "\nUse Forgot Password to reset the admin password.";
            }
            javax.swing.JOptionPane.showMessageDialog(this,
                    errorMessage,
                    "Login Failed", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }// GEN-LAST:event_jButton2ActionPerformed

    private void stylePrimaryButton(javax.swing.JButton button) {
        if (button == null) {
            return;
        }
        button.setPreferredSize(new Dimension(130, 40));
        button.setMinimumSize(new Dimension(130, 40));
        button.setMaximumSize(new Dimension(160, 44));
        button.setMargin(new java.awt.Insets(8, 18, 8, 18));
        button.setFont(new java.awt.Font("Segoe UI", java.awt.Font.BOLD, 13));
    }

    private void showForgotPasswordDialog() {
        javax.swing.JTextField usernameField = new javax.swing.JTextField(20);
        javax.swing.JPasswordField newPasswordField = new javax.swing.JPasswordField(20);
        javax.swing.JPasswordField confirmPasswordField = new javax.swing.JPasswordField(20);

        javax.swing.JPanel panel = new javax.swing.JPanel(new java.awt.GridLayout(0, 1, 8, 8));
        panel.add(new javax.swing.JLabel("Username"));
        panel.add(usernameField);
        panel.add(new javax.swing.JLabel("New password"));
        panel.add(newPasswordField);
        panel.add(new javax.swing.JLabel("Confirm new password"));
        panel.add(confirmPasswordField);

        int result = javax.swing.JOptionPane.showConfirmDialog(
                this,
                panel,
                "Forgot Password",
                javax.swing.JOptionPane.OK_CANCEL_OPTION,
                javax.swing.JOptionPane.PLAIN_MESSAGE);
        if (result != javax.swing.JOptionPane.OK_OPTION) {
            return;
        }

        String username = usernameField.getText().trim();
        String newPassword = new String(newPasswordField.getPassword()).trim();
        String confirmPassword = new String(confirmPasswordField.getPassword()).trim();

        if (username.isEmpty() || newPassword.isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Username and new password are required.",
                    "Forgot Password", javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPassword.equals(confirmPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this, "Passwords do not match.", "Forgot Password",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!isStrongPassword(newPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Forgot Password",
                    javax.swing.JOptionPane.WARNING_MESSAGE);
            return;
        }

        boolean userExists = UserDataManager.listUsers().stream()
                .anyMatch(account -> account.getUsername().equalsIgnoreCase(username));
        if (!userExists) {
            javax.swing.JOptionPane.showMessageDialog(this, "Username not found.", "Forgot Password",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserDataManager.resetPassword(username, newPassword)) {
            javax.swing.JOptionPane.showMessageDialog(this, "Password reset successfully.", "Forgot Password",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE);
        } else {
            javax.swing.JOptionPane.showMessageDialog(this, "Unable to reset password.", "Forgot Password",
                    javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean isStrongPassword(String password) {
        return password != null
                && password.length() >= 8
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    public static void main(String args[]) {
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Login.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }

        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration//GEN-END:variables
}