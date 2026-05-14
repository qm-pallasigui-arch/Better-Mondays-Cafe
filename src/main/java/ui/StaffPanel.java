package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.StaffShiftRepository;
import staff.StaffShift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;

public class StaffPanel extends JPanel {

    private final StaffShiftRepository shiftRepo;
    private final String username;
    private final Role currentRole;
    private final AccountRoleRepository accountRoleRepository;

    private final JButton startBtn = new JButton("Start Shift");
    private final JButton endBtn = new JButton("End Shift");
    private final JButton refreshShiftBtn = new JButton("Refresh Shifts");

    private final JTextArea notesArea = new JTextArea(4, 30);

    private final JTable shiftTable = new JTable();

    private final DefaultTableModel shiftModel = new DefaultTableModel(
            new String[] {
                    "ID",
                    "Username",
                    "Started At",
                    "Ended At",
                    "Notes"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JTable usersTable = new JTable();

    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[] {
                    "Username",
                    "Role",
                    "Created At"
            }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    private final JButton makeAdminBtn = new JButton("Make Admin");
    private final JButton makeStaffBtn = new JButton("Make Staff");
    private final JButton refreshUsersBtn = new JButton("Refresh Users");

    public StaffPanel(
            StaffShiftRepository shiftRepo,
            AccountRoleRepository accountRoleRepository,
            String username,
            Role currentRole) {

        this.shiftRepo = shiftRepo;
        this.accountRoleRepository = accountRoleRepository;
        this.username = username;
        this.currentRole = currentRole;

        initializeUI();
        registerEvents();

        loadShifts();

        if (currentRole == Role.ADMIN) {
            loadUsers();
        }

        AppTheme.applyToComponent(this);
    }

    private void initializeUI() {

        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JLabel userLabel = new JLabel(
                "Signed in as: "
                        + username
                        + " ("
                        + currentRole.name()
                        + ")");

        topPanel.add(userLabel);
        topPanel.add(startBtn);
        topPanel.add(endBtn);
        topPanel.add(refreshShiftBtn);

        JPanel notesPanel = new JPanel(new BorderLayout());

        notesPanel.setBorder(
                BorderFactory.createTitledBorder("Shift Notes"));

        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);

        notesPanel.add(new JScrollPane(notesArea));

        JPanel shiftPanel = new JPanel(new BorderLayout());

        shiftPanel.setBorder(
                BorderFactory.createTitledBorder("Shift History"));

        shiftTable.setModel(shiftModel);

        shiftPanel.add(new JScrollPane(shiftTable));

        JPanel shiftContent = new JPanel(new BorderLayout(10, 10));

        shiftContent.add(topPanel, BorderLayout.NORTH);
        shiftContent.add(notesPanel, BorderLayout.CENTER);
        shiftContent.add(shiftPanel, BorderLayout.SOUTH);

        if (currentRole == Role.ADMIN) {

            JPanel adminPanel = createAdminPanel();

            JSplitPane splitPane = new JSplitPane(
                    JSplitPane.VERTICAL_SPLIT,
                    shiftContent,
                    adminPanel);

            splitPane.setResizeWeight(0.55);

            add(splitPane, BorderLayout.CENTER);

        } else {

            add(shiftContent, BorderLayout.CENTER);
        }
    }

    private JPanel createAdminPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));

        panel.setBorder(
                BorderFactory.createTitledBorder("Admin Account Management"));

        usersTable.setModel(usersModel);

        usersTable.setSelectionMode(
                ListSelectionModel.SINGLE_SELECTION);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));

        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

        return panel;
    }

    private void registerEvents() {

        startBtn.addActionListener(this::onStartShift);

        endBtn.addActionListener(this::onEndShift);

        refreshShiftBtn.addActionListener(e -> loadShifts());

        refreshUsersBtn.addActionListener(e -> loadUsers());

        makeAdminBtn.addActionListener(
                e -> updateSelectedUserRole(Role.ADMIN));

        makeStaffBtn.addActionListener(
                e -> updateSelectedUserRole(Role.STAFF));
    }

    private void onStartShift(ActionEvent e) {

        try {

            shiftRepo.startShift(username);

            JOptionPane.showMessageDialog(
                    this,
                    "Shift started successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            loadShifts();

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to start shift:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onEndShift(ActionEvent e) {

        try {

            String notes = notesArea.getText().trim();

            shiftRepo.endShift(username, notes);

            notesArea.setText("");

            JOptionPane.showMessageDialog(
                    this,
                    "Shift ended successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            loadShifts();

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to end shift:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadShifts() {

        shiftModel.setRowCount(0);

        try {

            List<StaffShift> shifts = shiftRepo.findShifts(username);

            for (StaffShift shift : shifts) {

                shiftModel.addRow(new Object[] {
                        shift.getId(),
                        shift.getUsername(),
                        shift.getStartedAt(),
                        shift.getEndedAt(),
                        shift.getNotes()
                });
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load shifts:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadUsers() {

        usersModel.setRowCount(0);

        try {

            List<UserAccount> users = accountRoleRepository.listUsers();

            for (UserAccount user : users) {

                usersModel.addRow(new Object[] {
                        user.getUsername(),
                        user.getRole().name(),
                        user.getCreatedAt()
                });
            }

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to load users:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateSelectedUserRole(Role newRole) {

        int row = usersTable.getSelectedRow();

        if (row < 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please select a user.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);

            return;
        }

        String selectedUser = usersModel.getValueAt(row, 0).toString();

        try {

            accountRoleRepository.updateUserRole(
                    selectedUser,
                    newRole);

            loadUsers();

            JOptionPane.showMessageDialog(
                    this,
                    selectedUser + " updated to "
                            + newRole.name(),
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {

            JOptionPane.showMessageDialog(
                    this,
                    "Failed to update role:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}