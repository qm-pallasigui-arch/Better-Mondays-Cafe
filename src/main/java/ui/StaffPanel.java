package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.StaffShiftRepository;
import staff.StaffShift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
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

    // --- Shift history table (own shifts) ---
    private final JTable shiftTable = new JTable();
    private final DefaultTableModel shiftModel = new DefaultTableModel(
            new String[] { "ID", "Username", "Started At", "Ended At", "Notes" }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // --- All Staff Shifts table (admin only) ---
    private final JTable allShiftsTable = new JTable();
    private final DefaultTableModel allShiftsModel = new DefaultTableModel(
            new String[] { "ID", "Username", "Started At", "Ended At", "Notes" }, 0) {

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
            new String[] { "Username", "Role", "Created At" }, 0) {

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    };

    // --- Admin controls ---
    private final JButton makeAdminBtn = new JButton("Make Admin");
    private final JButton makeStaffBtn = new JButton("Make Staff");
    private final JButton refreshUsersBtn = new JButton("Refresh Users");
    private final JTextField userSearchField = new JTextField(20);
    private TableRowSorter<DefaultTableModel> usersSorter;

    // --- Shift editing (admin only) ---
    private final JButton editShiftBtn = new JButton("Edit Selected Shift");

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
        refreshShiftState();

        if (currentRole == Role.ADMIN) {
            loadUsers();
            loadAllShifts();
        }

        AppTheme.applyToComponent(this);
    }

    private void initializeUI() {

        setLayout(new BorderLayout(10, 10));

        // Top bar
        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel(
                "Signed in as: " + username + " (" + currentRole.name() + ")"));
        topPanel.add(startBtn);
        topPanel.add(endBtn);
        topPanel.add(refreshShiftBtn);

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
        shiftPanel.add(new JScrollPane(shiftTable), BorderLayout.CENTER);

        // Edit shift button
        JPanel shiftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shiftBtnPanel.add(editShiftBtn);
        editShiftBtn.setEnabled(false);
        shiftPanel.add(shiftBtnPanel, BorderLayout.SOUTH);

        // Assemble shift section (my shifts tab content)
        JPanel shiftContent = new JPanel(new BorderLayout(10, 10));
        shiftContent.add(topPanel, BorderLayout.NORTH);
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

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Search bar
        FilterRow searchPanel = new FilterRow();
        searchPanel.addLabeled("Search:", userSearchField);

        // Action buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(searchPanel, BorderLayout.NORTH);
        topBar.add(buttons, BorderLayout.SOUTH);

        // Users table with sorter for live search
        usersTable.setModel(usersModel);
        AppTheme.applyTableDefaults(usersTable);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersSorter = new TableRowSorter<>(usersModel);
        usersTable.setRowSorter(usersSorter);

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);

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

        Object idObj = shiftModel.getValueAt(modelRow, 0);
        Object shiftUser = shiftModel.getValueAt(modelRow, 1);
        Object startedAt = shiftModel.getValueAt(modelRow, 2);
        Object endedAt = shiftModel.getValueAt(modelRow, 3);
        Object notes = shiftModel.getValueAt(modelRow, 4);

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
                                shift.getId(),
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
                                shift.getId(),
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
                                user.getUsername(),
                                user.getRole().name(),
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
        String selectedUser = usersModel.getValueAt(modelRow, 0).toString();

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
            // Case-insensitive match on the Username column (index 0)
            usersSorter.setRowFilter(
                    RowFilter.regexFilter("(?i)" + text, 0));
        }
    }

    private void applyAllShiftsFilter() {
        String search = shiftSearchField.getText().trim();
        String status = (String) shiftStatusFilter.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> usernameFilter = null;
        RowFilter<DefaultTableModel, Integer> statusFilter = null;

        if (!search.isEmpty()) {
            usernameFilter = RowFilter.regexFilter("(?i)" + search, 1); // Username col
        }

        if ("Active".equals(status)) {
            // Ended At (col 3) is null or blank → active shift
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(3);
                    return val == null || val.toString().isBlank();
                }
            };
        } else if ("Completed".equals(status)) {
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                    Object val = entry.getValue(3);
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