package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.StaffShiftRepository;
import staff.PasswordResetRequest;
import staff.StaffShift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StaffPanel extends JPanel {

    private final StaffShiftRepository shiftRepo;
    private final String username;
    private final Role currentRole;
    private final AccountRoleRepository accountRoleRepository;

    // --- Shift controls ---
    private final JButton startBtn = new JButton("Start Shift");
    private final JButton endBtn = new JButton("End Shift");
    private final JButton refreshShiftBtn = new JButton("Refresh Shifts");
    private final JTextArea notesArea = new JTextArea(4, 30);

    // --- Live clock ---
    private final JLabel clockLabel = new JLabel();
    private static final DateTimeFormatter CLOCK_FMT =
            DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy   hh:mm:ss a");

    // --- Shift history table (own shifts) ---
    // Col 0 = DB row id (hidden), Col 1 = Staff ID, Col 2 = Username, ...
    private final JTable shiftTable = new JTable();
    private final DefaultTableModel shiftModel = new DefaultTableModel(
            new String[] { "_dbid", "Staff ID", "Username", "Started At", "Ended At", "Notes" }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // --- All Staff Shifts table (admin only) ---
    private final JTable allShiftsTable = new JTable();
    private final DefaultTableModel allShiftsModel = new DefaultTableModel(
            new String[] { "_dbid", "Staff ID", "Username", "Started At", "Ended At", "Notes" }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };
    private TableRowSorter<DefaultTableModel> allShiftsSorter;
    private final JTextField shiftSearchField = new JTextField(20);
    private final JButton refreshAllShiftsBtn = new JButton("Refresh");
    private final JComboBox<String> shiftStatusFilter = new JComboBox<>(
            new String[] { "All", "Active", "Completed" });

    // --- Users table ---
    private final JTable usersTable = new JTable();
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[] { "Staff ID", "Username", "Full Name", "Role", "Gender", "Mobile", "Created At" }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // --- Admin controls ---
    private final JButton makeAdminBtn = new JButton("Make Admin");
    private final JButton makeStaffBtn = new JButton("Make Staff");
    private final JButton refreshUsersBtn = new JButton("Refresh Users");
    private final JButton createAccountBtn = new JButton("Create Account");
    private final JButton viewDetailsBtn = new JButton("View Details");
    private final JTextField userSearchField = new JTextField(20);
    private TableRowSorter<DefaultTableModel> usersSorter;

    // --- Shift editing (admin only) ---
    private final JButton editShiftBtn = new JButton("Edit Selected Shift");

    // --- Password reset requests (admin only) ---
    private final JTable resetRequestsTable = new JTable();
    private final DefaultTableModel resetRequestsModel = new DefaultTableModel(
            new String[] { "_id", "Username", "Status", "Requested At", "Resolved At" }, 0) {
        @Override public boolean isCellEditable(int row, int col) { return false; }
    };
    private TableRowSorter<DefaultTableModel> resetRequestsSorter;
    private final JButton approveRequestBtn  = new JButton("Approve");
    private final JButton rejectRequestBtn   = new JButton("Reject");
    private final JButton refreshRequestsBtn = new JButton("Refresh");
    private final JComboBox<String> requestStatusFilter = new JComboBox<>(
            new String[] { "All", "Pending", "Approved", "Rejected" });

    // --- Admin-initiated password reset ---
    private final JButton resetPasswordBtn = new JButton("Reset Password");

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
        styleShiftButtons();
        registerEvents();
        refreshShiftState();

        if (currentRole == Role.ADMIN) {
            loadUsers();
            loadAllShifts();
            loadPasswordRequests();
        }

        AppTheme.applyToComponent(this);
    }

    private void initializeUI() {

        setLayout(new BorderLayout(10, 10));

        // Live clock — updated every second
        clockLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT));
        new Timer(1000, e -> clockLabel.setText(LocalDateTime.now().format(CLOCK_FMT))).start();

        // Top bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        topPanel.add(new JLabel(
                "Signed in as: " + username + " (" + currentRole.name() + ")"));
        topPanel.add(startBtn);
        topPanel.add(endBtn);
        topPanel.add(refreshShiftBtn);

        // Clock bar below the buttons
        JPanel clockBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        clockBar.add(new JLabel("Current date & time:"));
        clockBar.add(clockLabel);

        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(topPanel, BorderLayout.NORTH);
        headerPanel.add(clockBar, BorderLayout.SOUTH);

        // Notes
        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder("Shift Notes"));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesPanel.add(new JScrollPane(notesArea));

        // Shift history table
        JPanel shiftPanel = new JPanel(new BorderLayout(5, 5));
        shiftPanel.setBorder(BorderFactory.createTitledBorder("My Shift History"));
        shiftTable.setModel(shiftModel);
        AppTheme.applyTableDefaults(shiftTable);
        shiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        hideColumn(shiftTable, 0);
        shiftPanel.add(new JScrollPane(shiftTable), BorderLayout.CENTER);

        // Edit shift button
        JPanel shiftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shiftBtnPanel.add(editShiftBtn);
        editShiftBtn.setEnabled(false);
        shiftPanel.add(shiftBtnPanel, BorderLayout.SOUTH);

        // Assemble shift section (my shifts tab content)
        JPanel shiftContent = new JPanel(new BorderLayout(10, 10));
        shiftContent.add(headerPanel, BorderLayout.NORTH);
        shiftContent.add(notesPanel, BorderLayout.CENTER);
        shiftContent.add(shiftPanel, BorderLayout.SOUTH);

        if (currentRole == Role.ADMIN) {
            // Admin view: tabbed pane with My Shifts, All Staff Shifts, Account Management
            JTabbedPane tabbedPane = new JTabbedPane();
            tabbedPane.addTab("My Shift", shiftContent);
            tabbedPane.addTab("All Staff Shifts", createAllShiftsPanel());
            tabbedPane.addTab("Account Management", createAdminPanel());
            add(tabbedPane, BorderLayout.CENTER);
        } else {
            add(shiftContent, BorderLayout.CENTER);
        }
    }

    private void styleShiftButtons() {
        stylePrimaryShiftButton(startBtn, 124, 38, false);
        stylePrimaryShiftButton(endBtn, 118, 38, true);
        stylePrimaryShiftButton(refreshShiftBtn, 128, 38, false);
        stylePrimaryShiftButton(editShiftBtn, 160, 38, false);
        // Apply same sizing/styling to admin/account-management controls
        if (currentRole == Role.ADMIN) {
            stylePrimaryShiftButton(makeAdminBtn, 140, 38, false);
            stylePrimaryShiftButton(makeStaffBtn, 140, 38, false);
            stylePrimaryShiftButton(refreshUsersBtn, 128, 38, false);
            stylePrimaryShiftButton(refreshAllShiftsBtn, 128, 38, false);
            stylePrimaryShiftButton(createAccountBtn, 160, 38, false);
            stylePrimaryShiftButton(viewDetailsBtn, 140, 38, false);
            stylePrimaryShiftButton(resetPasswordBtn, 160, 38, true);
            stylePrimaryShiftButton(approveRequestBtn, 120, 38, false);
            stylePrimaryShiftButton(rejectRequestBtn, 100, 38, true);
            stylePrimaryShiftButton(refreshRequestsBtn, 100, 38, false);
        }
    }

    private void stylePrimaryShiftButton(JButton button, int width, int height, boolean danger) {
        if (button == null) {
            return;
        }
        button.putClientProperty("appTheme.variant", danger ? "danger" : "primary");
        button.setPreferredSize(new Dimension(width, height));
        button.setMinimumSize(new Dimension(width, height));
        button.setMaximumSize(new Dimension(width + 30, height + 6));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setFocusPainted(false);
    }

    private JPanel createAllShiftsPanel() {

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // --- Toolbar ---
        FilterRow toolbar = new FilterRow();
        toolbar.addLabeled("Search username:", shiftSearchField);
        toolbar.addLabeled("Status:", shiftStatusFilter);
        toolbar.add(refreshAllShiftsBtn);

        // --- Table ---
        allShiftsTable.setModel(allShiftsModel);
        AppTheme.applyTableDefaults(allShiftsTable);
        allShiftsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        allShiftsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        allShiftsSorter = new TableRowSorter<>(allShiftsModel);
        allShiftsTable.setRowSorter(allShiftsSorter);
        hideColumn(allShiftsTable, 0);

        // Summary label (updated after load)
        JLabel summaryLabel = new JLabel(" ");
        summaryLabel.setName("allShiftsSummary");

        JPanel bottomBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        bottomBar.add(summaryLabel);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(new JScrollPane(allShiftsTable), BorderLayout.CENTER);
        panel.add(bottomBar, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createAdminPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JTabbedPane innerTabs = new JTabbedPane();
        innerTabs.addTab("Users", createUsersSubPanel());
        innerTabs.addTab("Password Requests", createPasswordRequestsPanel());
        panel.add(innerTabs, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createUsersSubPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        FilterRow searchPanel = new FilterRow();
        searchPanel.addLabeled("Search:", userSearchField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(createAccountBtn);
        buttons.add(viewDetailsBtn);
        buttons.add(resetPasswordBtn);
        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(searchPanel, BorderLayout.NORTH);
        topBar.add(buttons, BorderLayout.SOUTH);

        usersTable.setModel(usersModel);
        AppTheme.applyTableDefaults(usersTable);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersSorter = new TableRowSorter<>(usersModel);
        usersTable.setRowSorter(usersSorter);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPasswordRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Toolbar
        FilterRow toolbar = new FilterRow();
        toolbar.addLabeled("Status:", requestStatusFilter);
        toolbar.add(refreshRequestsBtn);

        // Action buttons
        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        approveRequestBtn.setEnabled(false);
        rejectRequestBtn.setEnabled(false);
        actionBar.add(approveRequestBtn);
        actionBar.add(rejectRequestBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(toolbar, BorderLayout.NORTH);
        topBar.add(actionBar, BorderLayout.SOUTH);

        // Table — col 0 hidden (DB id)
        resetRequestsTable.setModel(resetRequestsModel);
        AppTheme.applyTableDefaults(resetRequestsTable);
        resetRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resetRequestsSorter = new TableRowSorter<>(resetRequestsModel);
        resetRequestsTable.setRowSorter(resetRequestsSorter);
        hideColumn(resetRequestsTable, 0);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(resetRequestsTable), BorderLayout.CENTER);
        return panel;
    }

    private void registerEvents() {

        startBtn.addActionListener(this::onStartShift);
        endBtn.addActionListener(this::onEndShift);
        refreshShiftBtn.addActionListener(e -> refreshShiftState());
        editShiftBtn.addActionListener(e -> onEditShift());

        // Enable edit button only when a row is selected
        shiftTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                editShiftBtn.setEnabled(shiftTable.getSelectedRow() >= 0);
            }
        });

        // Live user search
        userSearchField.getDocument().addDocumentListener(
                new javax.swing.event.DocumentListener() {

                    @Override
                    public void insertUpdate(javax.swing.event.DocumentEvent e) {
                        applyUserFilter();
                    }

                    @Override
                    public void removeUpdate(javax.swing.event.DocumentEvent e) {
                        applyUserFilter();
                    }

                    @Override
                    public void changedUpdate(javax.swing.event.DocumentEvent e) {
                        applyUserFilter();
                    }
                });

        refreshUsersBtn.addActionListener(e -> loadUsers());
        makeAdminBtn.addActionListener(e -> updateSelectedUserRole(Role.ADMIN));
        makeStaffBtn.addActionListener(e -> updateSelectedUserRole(Role.STAFF));
        createAccountBtn.addActionListener(e -> onCreateAccount());

        viewDetailsBtn.setEnabled(false);
        resetPasswordBtn.setEnabled(false);
        viewDetailsBtn.addActionListener(e -> onViewUserDetails());
        resetPasswordBtn.addActionListener(e -> onAdminResetPassword());
        usersTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                boolean sel = usersTable.getSelectedRow() >= 0;
                viewDetailsBtn.setEnabled(sel);
                resetPasswordBtn.setEnabled(sel);
            }
        });

        // Password requests tab
        refreshRequestsBtn.addActionListener(e -> loadPasswordRequests());
        requestStatusFilter.addActionListener(e -> applyRequestsFilter());
        approveRequestBtn.addActionListener(e -> onApproveRequest());
        rejectRequestBtn.addActionListener(e -> onRejectRequest());
        resetRequestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = resetRequestsTable.getSelectedRow();
                boolean sel = row >= 0;
                if (sel) {
                    int modelRow = resetRequestsTable.convertRowIndexToModel(row);
                    String status = resetRequestsModel.getValueAt(modelRow, 2).toString();
                    approveRequestBtn.setEnabled("PENDING".equals(status));
                    rejectRequestBtn.setEnabled("PENDING".equals(status));
                } else {
                    approveRequestBtn.setEnabled(false);
                    rejectRequestBtn.setEnabled(false);
                }
            }
        });

        // All-shifts tab events (admin only)
        if (currentRole == Role.ADMIN) {
            refreshAllShiftsBtn.addActionListener(e -> loadAllShifts());

            shiftSearchField.getDocument().addDocumentListener(
                    new javax.swing.event.DocumentListener() {

                        @Override
                        public void insertUpdate(javax.swing.event.DocumentEvent e) {
                            applyAllShiftsFilter();
                        }

                        @Override
                        public void removeUpdate(javax.swing.event.DocumentEvent e) {
                            applyAllShiftsFilter();
                        }

                        @Override
                        public void changedUpdate(javax.swing.event.DocumentEvent e) {
                            applyAllShiftsFilter();
                        }
                    });

            shiftStatusFilter.addActionListener(e -> applyAllShiftsFilter());
        }
    }

    private void onStartShift(ActionEvent e) {

        setShiftButtonsEnabled(false);

        new SwingWorker<Void, Void>() {

            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    shiftRepo.startShift(username);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to start shift:\n" + errorMsg);
                } else {
                    JOptionPane.showMessageDialog(
                            StaffPanel.this,
                            "Shift started successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                refreshShiftState();
                if (currentRole == Role.ADMIN)
                    loadAllShifts();
            }
        }.execute();
    }

    private void onEndShift(ActionEvent e) {

        setShiftButtonsEnabled(false);
        final String notes = notesArea.getText().trim();

        new SwingWorker<Void, Void>() {

            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    shiftRepo.endShift(username, notes);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to end shift:\n" + errorMsg);
                } else {
                    notesArea.setText("");
                    JOptionPane.showMessageDialog(
                            StaffPanel.this,
                            "Shift ended successfully.",
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
                refreshShiftState();
                if (currentRole == Role.ADMIN)
                    loadAllShifts();
            }
        }.execute();
    }

    private void onEditShift() {

        int viewRow = shiftTable.getSelectedRow();
        if (viewRow < 0)
            return;

        int modelRow = shiftTable.convertRowIndexToModel(viewRow);

        Object idObj    = shiftModel.getValueAt(modelRow, 0); // hidden DB id
        Object shiftUser = shiftModel.getValueAt(modelRow, 2);
        Object startedAt = shiftModel.getValueAt(modelRow, 3);
        Object endedAt   = shiftModel.getValueAt(modelRow, 4);
        Object notes     = shiftModel.getValueAt(modelRow, 5);

        if (currentRole != Role.ADMIN && !username.equals(shiftUser)) {
            JOptionPane.showMessageDialog(
                    this,
                    "You may only edit your own shifts.",
                    "Permission Denied",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField startField = new JTextField(
                startedAt != null ? startedAt.toString() : "", 20);
        JTextField endField = new JTextField(
                endedAt != null ? endedAt.toString() : "", 20);
        JTextArea notesField = new JTextArea(
                notes != null ? notes.toString() : "", 3, 20);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Started At:"));
        form.add(startField);
        form.add(new JLabel("Ended At:"));
        form.add(endField);
        form.add(new JLabel("Notes:"));
        form.add(new JScrollPane(notesField));

        int result = JOptionPane.showConfirmDialog(
                this, form,
                "Edit Shift #" + idObj,
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION)
            return;

        try {
            shiftRepo.updateShift(
                    (Integer) idObj,
                    startField.getText().trim(),
                    endField.getText().trim(),
                    notesField.getText().trim());

            JOptionPane.showMessageDialog(
                    this,
                    "Shift updated successfully.",
                    "Success",
                    JOptionPane.INFORMATION_MESSAGE);

            refreshShiftState();
            if (currentRole == Role.ADMIN)
                loadAllShifts();

        } catch (Exception ex) {
            showError("Failed to update shift:\n" + ex.getMessage());
        }
    }

    private void refreshShiftState() {

        setShiftButtonsEnabled(false);
        shiftModel.setRowCount(0);

        new SwingWorker<List<StaffShift>, Void>() {

            private String errorMsg;

            @Override
            protected List<StaffShift> doInBackground() {
                try {
                    return shiftRepo.findShifts(username);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<StaffShift> shifts = get();

                    for (StaffShift shift : shifts) {
                        shiftModel.addRow(new Object[] {
                                shift.getId(),       // col 0 hidden DB id
                                shift.getStaffId(),  // col 1 Staff ID
                                shift.getUsername(),
                                shift.getStartedAt(),
                                shift.getEndedAt(),
                                shift.getNotes()
                        });
                    }

                    // Derive active-shift state from the most recent record
                    boolean onShift = !shifts.isEmpty()
                            && shifts.get(0).getEndedAt() == null;

                    startBtn.setEnabled(!onShift);
                    endBtn.setEnabled(onShift);
                    notesArea.setEnabled(onShift);

                    if (errorMsg != null) {
                        showError("Failed to load shifts:\n" + errorMsg);
                    }

                } catch (Exception ex) {
                    showError("Failed to load shifts:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadAllShifts() {

        allShiftsModel.setRowCount(0);

        new SwingWorker<List<StaffShift>, Void>() {

            private String errorMsg;

            @Override
            protected List<StaffShift> doInBackground() {
                try {
                    return shiftRepo.findAllShifts();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<StaffShift> shifts = get();

                    for (StaffShift shift : shifts) {
                        allShiftsModel.addRow(new Object[] {
                                shift.getId(),       // col 0 hidden DB id
                                shift.getStaffId(),  // col 1 Staff ID
                                shift.getUsername(),
                                shift.getStartedAt(),
                                shift.getEndedAt(),
                                shift.getNotes()
                        });
                    }

                    applyAllShiftsFilter();

                    if (errorMsg != null) {
                        showError("Failed to load all shifts:\n" + errorMsg);
                    }

                } catch (Exception ex) {
                    showError("Failed to load all shifts:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void loadUsers() {

        usersModel.setRowCount(0);

        new SwingWorker<List<UserAccount>, Void>() {

            private String errorMsg;

            @Override
            protected List<UserAccount> doInBackground() {
                try {
                    return accountRoleRepository.listUsers();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    List<UserAccount> users = get();

                    for (UserAccount user : users) {
                        usersModel.addRow(new Object[] {
                                user.getStaffId(),
                                user.getUsername(),
                                user.getFullName(),
                                user.getRole().name(),
                                user.getGender(),
                                user.getMobile(),
                                user.getCreatedAt()
                        });
                    }

                    if (errorMsg != null) {
                        showError("Failed to load users:\n" + errorMsg);
                    }

                } catch (Exception ex) {
                    showError("Failed to load users:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateSelectedUserRole(Role newRole) {

        int viewRow = usersTable.getSelectedRow();

        if (viewRow < 0) {
            JOptionPane.showMessageDialog(
                    this,
                    "Please select a user.",
                    "Validation",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String selectedUser = usersModel.getValueAt(modelRow, 1).toString(); // col 1 = Username

        // Self-demotion guard
        if (selectedUser.equals(username) && newRole != Role.ADMIN) {
            JOptionPane.showMessageDialog(
                    this,
                    "You cannot demote yourself. Another admin must do this.",
                    "Action Not Allowed",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Are you sure you want to change " + selectedUser
                        + "'s role to " + newRole.name() + "?",
                "Confirm Role Change",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm != JOptionPane.YES_OPTION)
            return;

        new SwingWorker<Void, Void>() {

            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    accountRoleRepository.updateUserRole(selectedUser, newRole);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to update role:\n" + errorMsg);
                } else {
                    loadUsers();

                    JOptionPane.showMessageDialog(
                            StaffPanel.this,
                            selectedUser + " updated to " + newRole.name(),
                            "Success",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyUserFilter() {
        String text = userSearchField.getText().trim();
        if (text.isEmpty()) {
            usersSorter.setRowFilter(null);
        } else {
            // Case-insensitive match on Username (col 1) or Full Name (col 2)
            usersSorter.setRowFilter(
                    RowFilter.regexFilter("(?i)" + text, 1, 2));
        }
    }

    private void applyAllShiftsFilter() {
        String search = shiftSearchField.getText().trim();
        String status = (String) shiftStatusFilter.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> usernameFilter = null;
        RowFilter<DefaultTableModel, Integer> statusFilter = null;

        if (!search.isEmpty()) {
            usernameFilter = RowFilter.regexFilter("(?i)" + search, 2); // Username col
        }

        if ("Active".equals(status)) {
            // Ended At (col 4) is null or blank → active shift
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(4);
                    return val == null || val.toString().isBlank();
                }
            };
        } else if ("Completed".equals(status)) {
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(4);
                    return val != null && !val.toString().isBlank();
                }
            };
        }

        if (usernameFilter != null && statusFilter != null) {
            allShiftsSorter.setRowFilter(RowFilter.andFilter(
                    java.util.Arrays.asList(usernameFilter, statusFilter)));
        } else if (usernameFilter != null) {
            allShiftsSorter.setRowFilter(usernameFilter);
        } else if (statusFilter != null) {
            allShiftsSorter.setRowFilter(statusFilter);
        } else {
            allShiftsSorter.setRowFilter(null);
        }
    }

    private void onCreateAccount() {
        // --- Form fields ---
        JTextField fullNameField     = new JTextField(24);
        JTextField usernameField     = new JTextField(24);
        JPasswordField passwordField = new JPasswordField(24);
        JPasswordField confirmField  = new JPasswordField(24);
        JTextField ageField          = new JTextField(24);
        JTextField birthdateField    = new JTextField(24);
        birthdateField.setToolTipText("YYYY-MM-DD");
        JTextField addressField      = new JTextField(24);
        JTextField mobileField       = new JTextField(24);
        JComboBox<String> genderBox  = new JComboBox<>(new String[]{"Male", "Female", "Other"});
        JComboBox<String> roleBox    = new JComboBox<>(new String[]{"STAFF", "ADMIN"});

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Full Name:"));       form.add(fullNameField);
        form.add(new JLabel("Username:"));        form.add(usernameField);
        form.add(new JLabel("Password:"));        form.add(passwordField);
        form.add(new JLabel("Confirm Password:")); form.add(confirmField);
        form.add(new JLabel("Age:"));             form.add(ageField);
        form.add(new JLabel("Birthdate (YYYY-MM-DD):")); form.add(birthdateField);
        form.add(new JLabel("Address:"));         form.add(addressField);
        form.add(new JLabel("Mobile Number:"));   form.add(mobileField);
        form.add(new JLabel("Gender:"));          form.add(genderBox);
        form.add(new JLabel("Role:"));            form.add(roleBox);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.add(new JLabel("Fill in the new account details:"), BorderLayout.NORTH);
        wrapper.add(form, BorderLayout.CENTER);

        int result = JOptionPane.showConfirmDialog(
                this, wrapper,
                "Create New Account",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);

        if (result != JOptionPane.OK_OPTION) return;

        String fullName  = fullNameField.getText().trim();
        String uname     = usernameField.getText().trim();
        String password  = new String(passwordField.getPassword()).trim();
        String confirm   = new String(confirmField.getPassword()).trim();
        String ageText   = ageField.getText().trim();
        String birthdate = birthdateField.getText().trim();
        String address   = addressField.getText().trim();
        String mobile    = mobileField.getText().trim();
        String gender    = (String) genderBox.getSelectedItem();
        Role   role      = Role.valueOf((String) roleBox.getSelectedItem());

        if (fullName.isEmpty() || uname.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Full name, username, and password are required.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!password.equals(confirm)) {
            JOptionPane.showMessageDialog(this,
                    "Passwords do not match.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (password.length() < 8
                || !password.matches(".*\\d.*")
                || !password.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int age = 0;
        if (!ageText.isEmpty()) {
            try { age = Integer.parseInt(ageText); }
            catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this,
                        "Age must be a number.", "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        final int finalAge = age;
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    UserDataManager.createUser(uname, password, role,
                            fullName, finalAge, birthdate, address, mobile, gender);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to create account:\n" + errorMsg);
                } else {
                    loadUsers();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Account \"" + uname + "\" created successfully.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadPasswordRequests() {
        resetRequestsModel.setRowCount(0);
        new SwingWorker<List<PasswordResetRequest>, Void>() {
            private String errorMsg;

            @Override
            protected List<PasswordResetRequest> doInBackground() {
                try {
                    return UserDataManager.listAllResetRequests();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }

            @Override
            protected void done() {
                try {
                    for (PasswordResetRequest r : get()) {
                        resetRequestsModel.addRow(new Object[] {
                                r.getId(),
                                r.getUsername(),
                                r.getStatus().name(),
                                r.getRequestedAt(),
                                r.getResolvedAt() != null ? r.getResolvedAt() : ""
                        });
                    }
                    applyRequestsFilter();
                    if (errorMsg != null) showError("Failed to load requests:\n" + errorMsg);
                } catch (Exception ex) {
                    showError("Failed to load requests:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void applyRequestsFilter() {
        String selected = (String) requestStatusFilter.getSelectedItem();
        if (selected == null || "All".equals(selected)) {
            resetRequestsSorter.setRowFilter(null);
        } else {
            String statusValue = selected.toUpperCase();
            resetRequestsSorter.setRowFilter(RowFilter.regexFilter("(?i)^" + statusValue + "$", 2));
        }
    }

    private void onApproveRequest() {
        int viewRow = resetRequestsTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = resetRequestsTable.convertRowIndexToModel(viewRow);
        int requestId  = (int) resetRequestsModel.getValueAt(modelRow, 0);
        String reqUser = resetRequestsModel.getValueAt(modelRow, 1).toString();

        // Admin enters the new password for this user
        JPasswordField pwField      = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("New password for \"" + reqUser + "\":")); form.add(pwField);
        form.add(new JLabel("Confirm password:"));                     form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Approve Reset — Set New Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newPw  = new String(pwField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();

        if (newPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPw.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPw.length() < 8
                || !newPw.matches(".*\\d.*")
                || !newPw.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String finalPw = newPw;
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    UserDataManager.approveResetRequest(requestId, reqUser, finalPw);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to approve request:\n" + errorMsg);
                } else {
                    loadPasswordRequests();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Password reset approved for \"" + reqUser + "\".",
                            "Approved", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void onRejectRequest() {
        int viewRow = resetRequestsTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = resetRequestsTable.convertRowIndexToModel(viewRow);
        int requestId  = (int) resetRequestsModel.getValueAt(modelRow, 0);
        String reqUser = resetRequestsModel.getValueAt(modelRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reject the password reset request for \"" + reqUser + "\"?",
                "Confirm Reject", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    UserDataManager.rejectResetRequest(requestId);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to reject request:\n" + errorMsg);
                } else {
                    loadPasswordRequests();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Request for \"" + reqUser + "\" has been rejected.",
                            "Rejected", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void onAdminResetPassword() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String targetUser = usersModel.getValueAt(modelRow, 1).toString();

        JPasswordField pwField      = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("New password for \"" + targetUser + "\":")); form.add(pwField);
        form.add(new JLabel("Confirm password:"));                        form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Reset Password for " + targetUser,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        String newPw   = new String(pwField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();

        if (newPw.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password cannot be empty.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (!newPw.equals(confirm)) {
            JOptionPane.showMessageDialog(this, "Passwords do not match.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (newPw.length() < 8
                || !newPw.matches(".*\\d.*")
                || !newPw.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            JOptionPane.showMessageDialog(this,
                    "Password must be at least 8 characters and include a number and a special character.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        final String finalPw = newPw;
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    UserDataManager.resetPassword(targetUser, finalPw);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to reset password:\n" + errorMsg);
                } else {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Password for \"" + targetUser + "\" has been reset.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private static void hideColumn(JTable table, int col) {
        TableColumn tc = table.getColumnModel().getColumn(col);
        tc.setMinWidth(0);
        tc.setMaxWidth(0);
        tc.setWidth(0);
        tc.setPreferredWidth(0);
        tc.setResizable(false);
    }

    private void onViewUserDetails() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = usersTable.convertRowIndexToModel(viewRow);

        // Pull every column from the model (0=StaffID, 1=Username, 2=FullName,
        // 3=Role, 4=Gender, 5=Mobile, 6=CreatedAt)
        String staffId   = usersModel.getValueAt(modelRow, 0).toString();
        String uname     = usersModel.getValueAt(modelRow, 1).toString();
        String fullName  = usersModel.getValueAt(modelRow, 2).toString();
        String role      = usersModel.getValueAt(modelRow, 3).toString();
        String gender    = usersModel.getValueAt(modelRow, 4).toString();
        String mobile    = usersModel.getValueAt(modelRow, 5).toString();
        String createdAt = usersModel.getValueAt(modelRow, 6).toString();

        // Fetch age, birthdate, address from the live user list (not in table columns)
        String age = "", birthdate = "", address = "";
        try {
            for (UserAccount u : accountRoleRepository.listUsers()) {
                if (u.getUsername().equals(uname)) {
                    age       = String.valueOf(u.getAge());
                    birthdate = u.getBirthdate();
                    address   = u.getAddress();
                    break;
                }
            }
        } catch (Exception ignored) {}

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Staff ID:"));      form.add(readOnlyField(staffId));
        form.add(new JLabel("Username:"));      form.add(readOnlyField(uname));
        form.add(new JLabel("Full Name:"));     form.add(readOnlyField(fullName));
        form.add(new JLabel("Role:"));          form.add(readOnlyField(role));
        form.add(new JLabel("Gender:"));        form.add(readOnlyField(gender));
        form.add(new JLabel("Age:"));           form.add(readOnlyField(age));
        form.add(new JLabel("Birthdate:"));     form.add(readOnlyField(birthdate));
        form.add(new JLabel("Mobile Number:")); form.add(readOnlyField(mobile));
        form.add(new JLabel("Address:"));       form.add(readOnlyField(address));
        form.add(new JLabel("Created At:"));    form.add(readOnlyField(createdAt));

        JOptionPane.showMessageDialog(this, form,
                "Account Details — " + uname,
                JOptionPane.PLAIN_MESSAGE);
    }

    private static JTextField readOnlyField(String value) {
        JTextField f = new JTextField(value != null ? value : "", 24);
        f.setEditable(false);
        return f;
    }

    /** Enables or disables both shift action buttons together. */
    private void setShiftButtonsEnabled(boolean enabled) {
        startBtn.setEnabled(enabled);
        endBtn.setEnabled(enabled);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }
}