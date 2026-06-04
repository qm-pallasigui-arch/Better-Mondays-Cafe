package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.ProfilePictureRepository;
import persistence.StaffShiftRepository;
import staff.StaffShift;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class StaffPanel extends JPanel {

    private final StaffShiftRepository shiftRepo;
    private final String username;
    private final Role currentRole;
    private final AccountRoleRepository accountRoleRepository;
    /**
     * Optional: supply to enable profile-picture features.
     * Pass {@code null} to skip picture support.
     */
    private final ProfilePictureRepository pictureRepository;

    // ── Shift controls ────────────────────────────────────────────────────────
    private final JButton startBtn = new JButton("Start Shift");
    private final JButton endBtn = new JButton("End Shift");
    private final JButton refreshShiftBtn = new JButton("Refresh Shifts");
    private final JTextArea notesArea = new JTextArea(4, 30);

    // ── Shift history table ───────────────────────────────────────────────────
    private final JTable shiftTable = new JTable();
    private final DefaultTableModel shiftModel = new DefaultTableModel(
            new String[] { "ID", "Username", "Started At", "Ended At", "Notes" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // ── All-staff shifts table (admin) ────────────────────────────────────────
    private final JTable allShiftsTable = new JTable();
    private final DefaultTableModel allShiftsModel = new DefaultTableModel(
            new String[] { "ID", "Username", "Started At", "Ended At", "Notes" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private TableRowSorter<DefaultTableModel> allShiftsSorter;
    private final JTextField shiftSearchField = new JTextField(20);
    private final JButton refreshAllShiftsBtn = new JButton("Refresh");
    private final JComboBox<String> shiftStatusFilter = new JComboBox<>(new String[] { "All", "Active", "Completed" });

    // ── Users table (admin) ───────────────────────────────────────────────────
    private final JTable usersTable = new JTable();
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[] { "Username", "Role", "Created At" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };

    // ── Admin controls ────────────────────────────────────────────────────────
    private final JButton makeAdminBtn = new JButton("Make Admin");
    private final JButton makeStaffBtn = new JButton("Make Staff");
    private final JButton refreshUsersBtn = new JButton("Refresh Users");
    private final JButton registerStaffBtn = new JButton("Register Staff");
    /** NEW – soft-deletes the selected account so it can no longer log in. */
    private final JButton deleteStaffBtn = new JButton("Delete Staff");
    private final JTextField userSearchField = new JTextField(20);
    private TableRowSorter<DefaultTableModel> usersSorter;

    // ── Shift editing (admin) ─────────────────────────────────────────────────
    private final JButton editShiftBtn = new JButton("Edit Selected Shift");

    // ── Constructor (with picture support) ───────────────────────────────────
    public StaffPanel(StaffShiftRepository shiftRepo,
            AccountRoleRepository accountRoleRepository,
            String username,
            Role currentRole,
            ProfilePictureRepository pictureRepository) {
        this.shiftRepo = shiftRepo;
        this.accountRoleRepository = accountRoleRepository;
        this.username = username;
        this.currentRole = currentRole;
        this.pictureRepository = pictureRepository;

        initializeUI();
        styleShiftButtons();
        registerEvents();
        refreshShiftState();

        if (currentRole == Role.ADMIN) {
            loadUsers();
            loadAllShifts();
        }

        AppTheme.applyToComponent(this);
    }

    /** Convenience constructor for callers that don't supply a picture repo. */
    public StaffPanel(StaffShiftRepository shiftRepo,
            AccountRoleRepository accountRoleRepository,
            String username,
            Role currentRole) {
        this(shiftRepo, accountRoleRepository, username, currentRole, null);
    }

    // =========================================================================
    // UI Construction
    // =========================================================================

    private void initializeUI() {
        setLayout(new BorderLayout(10, 10));

        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topPanel.add(new JLabel("Signed in as: " + username + " (" + currentRole.name() + ")"));
        topPanel.add(startBtn);
        topPanel.add(endBtn);
        topPanel.add(refreshShiftBtn);

        JPanel notesPanel = new JPanel(new BorderLayout());
        notesPanel.setBorder(BorderFactory.createTitledBorder("Shift Notes"));
        notesArea.setLineWrap(true);
        notesArea.setWrapStyleWord(true);
        notesPanel.add(new JScrollPane(notesArea));

        JPanel shiftPanel = new JPanel(new BorderLayout(5, 5));
        shiftPanel.setBorder(BorderFactory.createTitledBorder("My Shift History"));
        shiftTable.setModel(shiftModel);
        AppTheme.applyTableDefaults(shiftTable);
        shiftTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        shiftPanel.add(new JScrollPane(shiftTable), BorderLayout.CENTER);

        JPanel shiftBtnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        shiftBtnPanel.add(editShiftBtn);
        editShiftBtn.setEnabled(false);
        shiftPanel.add(shiftBtnPanel, BorderLayout.SOUTH);

        JPanel shiftContent = new JPanel(new BorderLayout(10, 10));
        shiftContent.add(topPanel, BorderLayout.NORTH);
        shiftContent.add(notesPanel, BorderLayout.CENTER);
        shiftContent.add(shiftPanel, BorderLayout.SOUTH);

        if (currentRole == Role.ADMIN) {
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
        if (currentRole == Role.ADMIN) {
            stylePrimaryShiftButton(makeAdminBtn, 140, 38, false);
            stylePrimaryShiftButton(makeStaffBtn, 140, 38, false);
            stylePrimaryShiftButton(refreshUsersBtn, 128, 38, false);
            stylePrimaryShiftButton(refreshAllShiftsBtn, 128, 38, false);
            stylePrimaryShiftButton(registerStaffBtn, 148, 38, false);
            // Delete is a danger variant
            stylePrimaryShiftButton(deleteStaffBtn, 140, 38, true);
        }
    }

    private void stylePrimaryShiftButton(JButton button, int w, int h, boolean danger) {
        if (button == null)
            return;
        button.putClientProperty("appTheme.variant", danger ? "danger" : "primary");
        button.setPreferredSize(new Dimension(w, h));
        button.setMinimumSize(new Dimension(w, h));
        button.setMaximumSize(new Dimension(w + 30, h + 6));
        button.setMargin(new Insets(8, 14, 8, 14));
        button.setFocusPainted(false);
    }

    private JPanel createAllShiftsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        FilterRow toolbar = new FilterRow();
        toolbar.addLabeled("Search username:", shiftSearchField);
        toolbar.addLabeled("Status:", shiftStatusFilter);
        toolbar.add(refreshAllShiftsBtn);

        allShiftsTable.setModel(allShiftsModel);
        AppTheme.applyTableDefaults(allShiftsTable);
        allShiftsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        allShiftsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        allShiftsSorter = new TableRowSorter<>(allShiftsModel);
        allShiftsTable.setRowSorter(allShiftsSorter);

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

        FilterRow searchPanel = new FilterRow();
        searchPanel.addLabeled("Search:", userSearchField);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);
        buttons.add(registerStaffBtn);
        buttons.add(deleteStaffBtn); // ← NEW

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(searchPanel, BorderLayout.NORTH);
        topBar.add(buttons, BorderLayout.SOUTH);

        usersTable.setModel(usersModel);
        AppTheme.applyTableDefaults(usersTable);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        usersSorter = new TableRowSorter<>(usersModel);
        usersTable.setRowSorter(usersSorter);

        JLabel hintLabel = new JLabel("💡 Double-click a staff row to view their profile");
        hintLabel.setFont(hintLabel.getFont().deriveFont(Font.ITALIC, 11f));
        hintLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        panel.add(topBar, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        panel.add(hintLabel, BorderLayout.SOUTH);
        return panel;
    }

    // =========================================================================
    // Event Registration
    // =========================================================================

    private void registerEvents() {
        startBtn.addActionListener(this::onStartShift);
        endBtn.addActionListener(this::onEndShift);
        refreshShiftBtn.addActionListener(e -> refreshShiftState());
        editShiftBtn.addActionListener(e -> onEditShift());

        shiftTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                editShiftBtn.setEnabled(shiftTable.getSelectedRow() >= 0);
        });

        userSearchField.getDocument().addDocumentListener(
                simpleDocListener(this::applyUserFilter));

        refreshUsersBtn.addActionListener(e -> loadUsers());
        makeAdminBtn.addActionListener(e -> updateSelectedUserRole(Role.ADMIN));
        makeStaffBtn.addActionListener(e -> updateSelectedUserRole(Role.STAFF));
        registerStaffBtn.addActionListener(e -> showRegisterStaffDialog());
        deleteStaffBtn.addActionListener(e -> deleteSelectedStaff()); // ← NEW

        usersTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    showProfileModal();
            }
        });

        if (currentRole == Role.ADMIN) {
            refreshAllShiftsBtn.addActionListener(e -> loadAllShifts());
            shiftSearchField.getDocument().addDocumentListener(
                    simpleDocListener(this::applyAllShiftsFilter));
            shiftStatusFilter.addActionListener(e -> applyAllShiftsFilter());
        }
    }

    // =========================================================================
    // Delete Staff ← NEW
    // =========================================================================

    /**
     * Soft-deletes the selected staff account.
     * <p>
     * The account is disabled in the backing store (via
     * {@link AccountRoleRepository#deleteUser(String)}), preventing further
     * login. The picture (if any) is also removed. The row is then refreshed
     * in the table.
     * </p>
     */
    private void deleteSelectedStaff() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String targetUsername = usersModel.getValueAt(modelRow, 0).toString();

        // Guard: admins cannot delete themselves
        if (targetUsername.equals(username)) {
            JOptionPane.showMessageDialog(this,
                    "You cannot delete your own account.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Confirmation dialog
        int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Are you sure you want to delete <b>" + targetUsername + "</b>?<br>"
                        + "This will disable their account and they will no longer be able to log in.</html>",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    accountRoleRepository.deleteUser(targetUsername);
                    // Clean up the stored picture, if any
                    if (pictureRepository != null) {
                        try {
                            pictureRepository.deletePicture(targetUsername);
                        } catch (Exception ignored) {
                        }
                    }
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to delete staff account:\n" + errorMsg);
                } else {
                    loadUsers();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Account '" + targetUsername + "' has been deleted "
                                    + "and can no longer log in.",
                            "Account Deleted",
                            JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // Register Staff Dialog
    // =========================================================================

    private void showRegisterStaffDialog() {
        StaffRegistrationDialog dialog = new StaffRegistrationDialog(
                SwingUtilities.getWindowAncestor(this));

        dialog.setSaveHandler((uname, password, fullName, email, dob, empStart) -> accountRoleRepository
                .registerStaff(uname, password, fullName, email, dob, empStart));

        if (pictureRepository != null)
            dialog.setPictureRepository(pictureRepository);

        dialog.setSaveSuccessListener(this::loadUsers);
        dialog.setVisible(true);
    }

    // =========================================================================
    // Profile Modal
    // =========================================================================

    private void showProfileModal() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0)
            return;

        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String selectedUsername = usersModel.getValueAt(modelRow, 0).toString();

        new SwingWorker<UserAccount, Void>() {
            private String errorMsg;

            @Override
            protected UserAccount doInBackground() {
                try {
                    return accountRoleRepository.getUserProfile(selectedUsername);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return null;
                }
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    showError("Failed to load profile:\n" + errorMsg);
                    return;
                }
                UserAccount profile;
                try {
                    profile = get();
                } catch (Exception ex) {
                    showError("Failed to load profile:\n" + ex.getMessage());
                    return;
                }
                if (profile == null) {
                    showError("User not found: " + selectedUsername);
                    return;
                }
                showStaffProfileDialog(profile);
            }
        }.execute();
    }

    private void showStaffProfileDialog(UserAccount profile) {
        StaffProfileDialog dialog = new StaffProfileDialog(
                SwingUtilities.getWindowAncestor(this),
                profile.getUsername(),
                profile.getRole().name(),
                profile.getCreatedAt());

        dialog.populateProfile(
                profile.getFullName(),
                profile.getDateOfBirth(),
                profile.getEmail(),
                profile.getEmploymentStart());

        dialog.setSaveHandler((uname, fullName, dob, email, empStart) -> accountRoleRepository.updateUserProfile(uname,
                fullName, dob, email, empStart));

        // Wire picture repo if available
        if (pictureRepository != null)
            dialog.setPictureRepository(pictureRepository);

        dialog.setVisible(true);
        loadUsers();
    }

    // =========================================================================
    // Shift Actions
    // =========================================================================

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
                if (errorMsg != null)
                    showError("Failed to start shift:\n" + errorMsg);
                else
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Shift started successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
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
                if (errorMsg != null)
                    showError("Failed to end shift:\n" + errorMsg);
                else {
                    notesArea.setText("");
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Shift ended successfully.", "Success", JOptionPane.INFORMATION_MESSAGE);
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
            JOptionPane.showMessageDialog(this, "You may only edit your own shifts.",
                    "Permission Denied", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JTextField startField = new JTextField(startedAt != null ? startedAt.toString() : "", 20);
        JTextField endField = new JTextField(endedAt != null ? endedAt.toString() : "", 20);
        JTextArea notesField = new JTextArea(notes != null ? notes.toString() : "", 3, 20);
        notesField.setLineWrap(true);
        notesField.setWrapStyleWord(true);

        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("Started At:"));
        form.add(startField);
        form.add(new JLabel("Ended At:"));
        form.add(endField);
        form.add(new JLabel("Notes:"));
        form.add(new JScrollPane(notesField));

        int result = JOptionPane.showConfirmDialog(this, form,
                "Edit Shift #" + idObj, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        try {
            shiftRepo.updateShift((Integer) idObj,
                    startField.getText().trim(),
                    endField.getText().trim(),
                    notesField.getText().trim());
            JOptionPane.showMessageDialog(this, "Shift updated successfully.",
                    "Success", JOptionPane.INFORMATION_MESSAGE);
            refreshShiftState();
            if (currentRole == Role.ADMIN)
                loadAllShifts();
        } catch (Exception ex) {
            showError("Failed to update shift:\n" + ex.getMessage());
        }
    }

    // =========================================================================
    // Data Loaders
    // =========================================================================

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
                    for (StaffShift s : shifts)
                        shiftModel.addRow(new Object[] {
                                s.getId(), s.getUsername(),
                                s.getStartedAt(), s.getEndedAt(), s.getNotes() });
                    boolean onShift = !shifts.isEmpty() && shifts.get(0).getEndedAt() == null;
                    startBtn.setEnabled(!onShift);
                    endBtn.setEnabled(onShift);
                    notesArea.setEnabled(onShift);
                    if (errorMsg != null)
                        showError("Failed to load shifts:\n" + errorMsg);
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
                    for (StaffShift s : shifts)
                        allShiftsModel.addRow(new Object[] {
                                s.getId(), s.getUsername(),
                                s.getStartedAt(), s.getEndedAt(), s.getNotes() });
                    applyAllShiftsFilter();
                    if (errorMsg != null)
                        showError("Failed to load all shifts:\n" + errorMsg);
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
                    for (UserAccount u : users)
                        usersModel.addRow(new Object[] {
                                u.getUsername(), u.getRole().name(), u.getCreatedAt() });
                    if (errorMsg != null)
                        showError("Failed to load users:\n" + errorMsg);
                } catch (Exception ex) {
                    showError("Failed to load users:\n" + ex.getMessage());
                }
            }
        }.execute();
    }

    private void updateSelectedUserRole(Role newRole) {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String selectedUser = usersModel.getValueAt(modelRow, 0).toString();

        if (selectedUser.equals(username) && newRole != Role.ADMIN) {
            JOptionPane.showMessageDialog(this,
                    "You cannot demote yourself. Another admin must do this.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to change " + selectedUser
                        + "'s role to " + newRole.name() + "?",
                "Confirm Role Change", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
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
                if (errorMsg != null)
                    showError("Failed to update role:\n" + errorMsg);
                else {
                    loadUsers();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            selectedUser + " updated to " + newRole.name(),
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    // =========================================================================
    // Filters
    // =========================================================================

    private void applyUserFilter() {
        String text = userSearchField.getText().trim();
        usersSorter.setRowFilter(
                text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + text, 0));
    }

    private void applyAllShiftsFilter() {
        String search = shiftSearchField.getText().trim();
        String status = (String) shiftStatusFilter.getSelectedItem();

        RowFilter<DefaultTableModel, Integer> usernameFilter = null;
        RowFilter<DefaultTableModel, Integer> statusFilter = null;

        if (!search.isEmpty())
            usernameFilter = RowFilter.regexFilter("(?i)" + search, 1);

        if ("Active".equals(status)) {
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                    Object val = e.getValue(3);
                    return val == null || val.toString().isBlank();
                }
            };
        } else if ("Completed".equals(status)) {
            statusFilter = new RowFilter<>() {
                @Override
                public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> e) {
                    Object val = e.getValue(3);
                    return val != null && !val.toString().isBlank();
                }
            };
        }

        if (usernameFilter != null && statusFilter != null)
            allShiftsSorter.setRowFilter(
                    RowFilter.andFilter(java.util.Arrays.asList(usernameFilter, statusFilter)));
        else if (usernameFilter != null)
            allShiftsSorter.setRowFilter(usernameFilter);
        else if (statusFilter != null)
            allShiftsSorter.setRowFilter(statusFilter);
        else
            allShiftsSorter.setRowFilter(null);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void setShiftButtonsEnabled(boolean enabled) {
        startBtn.setEnabled(enabled);
        endBtn.setEnabled(enabled);
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static javax.swing.event.DocumentListener simpleDocListener(Runnable onChange) {
        return new javax.swing.event.DocumentListener() {
            @Override
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                onChange.run();
            }

            @Override
            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                onChange.run();
            }
        };
    }
}