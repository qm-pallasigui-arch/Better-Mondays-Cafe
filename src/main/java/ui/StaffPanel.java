package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.StaffShiftRepository;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.table.DefaultTableModel;

public class StaffPanel extends JPanel {

    private final StaffShiftRepository shiftRepo;
    private final String username;
    private final Role currentRole;
    private final AccountRoleRepository accountRoleRepository;

    private JButton startBtn = new JButton("Start Shift");
    private JButton endBtn = new JButton("End Shift");
    private JTextArea notesArea = new JTextArea(4, 30);
    private JTable usersTable = new JTable();
    private DefaultTableModel usersModel = new DefaultTableModel(new String[]{"Username", "Role", "Created At"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private JButton makeAdminBtn = new JButton("Make Admin");
    private JButton makeStaffBtn = new JButton("Make Staff");
    private JButton refreshUsersBtn = new JButton("Refresh Users");

    public StaffPanel(StaffShiftRepository shiftRepo, AccountRoleRepository accountRoleRepository, String username, Role currentRole) {
        this.shiftRepo = shiftRepo;
        this.accountRoleRepository = accountRoleRepository;
        this.username = username;
        this.currentRole = currentRole;

        setLayout(new BorderLayout());
        JPanel shiftPanel = new JPanel(new BorderLayout(8, 8));
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
        top.add(new JLabel("Signed in as: " + username + " (" + currentRole.name() + ")"));
        top.add(startBtn);
        top.add(endBtn);
        shiftPanel.add(top, BorderLayout.NORTH);
        shiftPanel.add(new JScrollPane(notesArea), BorderLayout.CENTER);
        shiftPanel.setBorder(BorderFactory.createTitledBorder("Shift Actions"));

        if (currentRole == Role.ADMIN) {
            JPanel adminPanel = createAdminPanel();
            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, shiftPanel, adminPanel);
            splitPane.setResizeWeight(0.35);
            add(splitPane, BorderLayout.CENTER);
            loadUsers();
        } else {
            add(shiftPanel, BorderLayout.CENTER);
        }

        startBtn.addActionListener(this::onStart);
        endBtn.addActionListener(this::onEnd);
        AppTheme.applyToComponent(this);
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createTitledBorder("Admin Account Management"));
        usersTable.setModel(usersModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        makeAdminBtn.addActionListener(e -> updateSelectedUserRole(Role.ADMIN));
        makeStaffBtn.addActionListener(e -> updateSelectedUserRole(Role.STAFF));
        refreshUsersBtn.addActionListener(e -> loadUsers());

        return panel;
    }

    private void loadUsers() {
        usersModel.setRowCount(0);
        try {
            List<UserAccount> users = accountRoleRepository.listUsers();
            for (UserAccount u : users) {
                usersModel.addRow(new Object[]{u.getUsername(), u.getRole().name(), u.getCreatedAt()});
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to load users: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedUserRole(Role newRole) {
        int row = usersTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select an account.", "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        String selectedUser = usersModel.getValueAt(row, 0).toString();
        if (selectedUser.equalsIgnoreCase(username) && newRole != currentRole) {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "You are changing your own role. Continue?",
                    "Confirm",
                    JOptionPane.YES_NO_OPTION);
            if (confirm != JOptionPane.YES_OPTION) {
                return;
            }
        }
        try {
            accountRoleRepository.updateUserRole(selectedUser, newRole);
            loadUsers();
            JOptionPane.showMessageDialog(this, "Updated " + selectedUser + " to " + newRole.name(), "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to update role: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onStart(ActionEvent e) {
        try {
            shiftRepo.startShift(username);
            JOptionPane.showMessageDialog(this, "Shift started", "OK", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to start shift: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEnd(ActionEvent e) {
        try {
            String notes = notesArea.getText().trim();
            shiftRepo.endShift(username, notes);
            JOptionPane.showMessageDialog(this, "Shift ended", "OK", JOptionPane.INFORMATION_MESSAGE);
            notesArea.setText("");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Failed to end shift: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
