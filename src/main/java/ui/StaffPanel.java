package ui;

import loginregister.UserAccount;
import loginregister.UserDataManager;
import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.AppDatabase;
import persistence.ProfilePictureRepository;
import persistence.StaffShiftRepository;
import persistence.StaffScheduleRepository;
import persistence.sqlite.SQLiteLeaveRequestRepository;
import persistence.sqlite.SQLiteStaffScheduleRepository;
import staff.LeaveRequest;
import staff.PasswordResetRequest;
import staff.StaffShift;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.Dialog.ModalityType;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffPanel extends JPanel {

    private final StaffShiftRepository shiftRepo;
    private final AccountRoleRepository accountRoleRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final StaffScheduleRepository scheduleRepo = new SQLiteStaffScheduleRepository();
    private final String username;
    private final Role currentRole;

    // Staff cards
    private final JPanel staffCardsPanel = new JPanel(new GridLayout(0, 3, 12, 12));

    // Account Management — Users table
    private final JTable usersTable = new JTable();
    private final DefaultTableModel usersModel = new DefaultTableModel(
            new String[] { "Staff ID", "Username", "Full Name", "Role", "Gender", "Mobile", "Created At" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private final JButton makeAdminBtn = new JButton("Make Admin");
    private final JButton makeStaffBtn = new JButton("Make Staff");
    private final JButton refreshUsersBtn = new JButton("Refresh Users");
    private final JButton createAccountBtn = new JButton("Create Account");
    private final JButton viewDetailsBtn = new JButton("View Details");
    private final JButton resetPasswordBtn = new JButton("Reset Password");

    // Account Management — Password Requests
    private final JTable resetRequestsTable = new JTable();
    private final DefaultTableModel resetRequestsModel = new DefaultTableModel(
            new String[] { "_id", "Username", "Status", "Requested At", "Resolved At" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    private TableRowSorter<DefaultTableModel> resetRequestsSorter;
    private final JButton approveRequestBtn = new JButton("Approve");
    private final JButton rejectRequestBtn = new JButton("Reject");
    private final JButton refreshRequestsBtn = new JButton("Refresh");
    private final JComboBox<String> requestStatusFilter = new JComboBox<>(
            new String[] { "All", "Pending", "Approved", "Rejected" });

    // Admin — Leave Request table
    private final JTable leaveRequestsTable = new JTable();
    private final DefaultTableModel leaveRequestsModel = new DefaultTableModel(
            new String[] { "_id", "Staff ID", "Username", "Start Date", "End Date", "Status", "Requested At" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };
    private TableRowSorter<DefaultTableModel> leaveRequestsSorter;
    private final JButton approveLeaveBtn  = new JButton("Approve");
    private final JButton rejectLeaveBtn   = new JButton("Reject");
    private final JButton refreshLeaveBtn  = new JButton("Refresh");
    private final JComboBox<String> leaveStatusFilter = new JComboBox<>(
            new String[] { "All", "Pending", "Approved", "Rejected" });

    // Staff — My Requests table (own leave requests)
    private final JTable myLeaveTable = new JTable();
    private final DefaultTableModel myLeaveModel = new DefaultTableModel(
            new String[] { "Type", "Start Date", "End Date", "Status", "Requested At" }, 0) {
        @Override
        public boolean isCellEditable(int row, int col) { return false; }
    };

    // Card background colors
    private static final Color CARD_BG = Color.WHITE;
    private static final Color CARD_BORDER = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY = new Color(0x0F172A);
    private static final Color TEXT_SECONDARY = new Color(0x64748B);
    private static final Color ACCENT_BLUE = new Color(0x2563EB);
    private static final Color STATUS_GREEN = new Color(0x10B981);
    private static final Color STATUS_ORANGE = new Color(0xF59E0B);
    private static final Color STATUS_RED = new Color(0xEF4444);
    private static final Color STATUS_GRAY = new Color(0x9CA3AF);
    private static final Color BG_GREEN = new Color(0xD1FAE5);
    private static final Color BG_ORANGE = new Color(0xFEF3C7);
    private static final Color BG_RED = new Color(0xFEE2E2);
    private static final Color BG_BLUE = new Color(0xDBEAFE);
    private static final Color BG_GRAY = new Color(0xF3F4F6);

    public StaffPanel(
            StaffShiftRepository shiftRepo,
            AccountRoleRepository accountRoleRepository,
            ProfilePictureRepository profilePictureRepository,
            String username,
            Role currentRole) {

        this.shiftRepo = shiftRepo;
        this.accountRoleRepository = accountRoleRepository;
        this.profilePictureRepository = profilePictureRepository;
        this.username = username;
        this.currentRole = currentRole;

        setLayout(new BorderLayout(0, 0));
        setBackground(new Color(0xF9FAFB));

        // Top header + tabs
        JPanel topSection = new JPanel(new BorderLayout(0, 16));
        topSection.setOpaque(false);
        topSection.setBorder(new EmptyBorder(20, 24, 0, 24));
        topSection.add(buildHeader(), BorderLayout.NORTH);
        topSection.add(buildNavigationTabs(), BorderLayout.SOUTH);

        add(topSection, BorderLayout.NORTH);

        // Card layout for tab switching
        cards = new CardLayout();
        cardsContainer = new JPanel(cards);
        cardsContainer.setOpaque(false);
        boolean isAdmin = currentRole == Role.ADMIN;
        if (isAdmin) {
            cardsContainer.add(buildStaffShiftsView(), "Staff Shifts");
        }
        cardsContainer.add(buildWeeklyScheduleView(), "Weekly Schedule");
        cardsContainer.add(buildAttendanceHistoryView(), "Attendance History");
        cardsContainer.add(buildStaffRequestsView(), "Staff Requests");
        cardsContainer.add(buildAccountManagementView(), "Account Management");
        add(cardsContainer, BorderLayout.CENTER);
        cards.show(cardsContainer, isAdmin ? "Staff Shifts" : "Weekly Schedule");

        // Load data
        refreshAll();
    }

    // ─── Page Header ─────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);

        JLabel title = new JLabel("Staff Management");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Management of Shifts and Attendance");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subtitle.setForeground(TEXT_SECONDARY);

        titleStack.add(title);
        titleStack.add(subtitle);

        JButton addStaffBtn = new JButton("+ Add Staff");
        addStaffBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        addStaffBtn.setForeground(Color.WHITE);
        addStaffBtn.setBackground(ACCENT_BLUE);
        addStaffBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        addStaffBtn.setFocusPainted(false);
        addStaffBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addStaffBtn.addActionListener(e -> onCreateAccount());
        addStaffBtn.setVisible(currentRole == Role.ADMIN);

        JButton staffRequestBtn = new JButton("Staff Request");
        staffRequestBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        staffRequestBtn.setForeground(Color.WHITE);
        staffRequestBtn.setBackground(new Color(0x10B981));
        staffRequestBtn.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        staffRequestBtn.setFocusPainted(false);
        staffRequestBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        staffRequestBtn.addActionListener(e -> showStaffRequestDialog());
        staffRequestBtn.setVisible(currentRole == Role.STAFF);

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(addStaffBtn);
        eastPanel.add(staffRequestBtn);

        header.add(titleStack, BorderLayout.WEST);
        header.add(eastPanel, BorderLayout.EAST);
        return header;
    }

    // ─── Navigation Tabs ─────────────────────────────────────────
    private JPanel buildNavigationTabs() {
        tabBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tabBar.setOpaque(false);

        String[] tabs = { "Staff Shifts", "Weekly Schedule", "Attendance History", "Staff Requests",
                "Account Management" };
        String defaultTab = currentRole == Role.ADMIN ? "Staff Shifts" : "Weekly Schedule";
        for (String tab : tabs) {
            JButton btn = new JButton(tab);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(TEXT_SECONDARY);
            btn.setBackground(new Color(0xF1F5F9));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 0, 1, CARD_BORDER),
                    new EmptyBorder(8, 18, 8, 18)));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> onTabSelected(tab));
            if (tab.equals("Staff Shifts")) {
                if (currentRole != Role.ADMIN) {
                    btn.setVisible(false);
                }
            }
            if (tab.equals("Account Management") && currentRole != Role.ADMIN) {
                btn.setVisible(false);
            }
            boolean active = tab.equals(defaultTab);
            if (active) {
                btn.setBackground(ACCENT_BLUE);
                btn.setForeground(Color.WHITE);
            }
            tabBar.add(btn);
        }
        return tabBar;
    }

    private void onTabSelected(String tab) {
        if ("Staff Shifts".equals(tab)) {
            if (currentRole != Role.ADMIN)
                return;
            showStaffShiftsView();
        } else if ("Weekly Schedule".equals(tab)) {
            showWeeklyScheduleView();
        } else if ("Attendance History".equals(tab)) {
            showAttendanceHistoryView();
        } else if ("Staff Requests".equals(tab)) {
            showStaffRequestsView();
        } else if ("Account Management".equals(tab)) {
            showAccountManagementView();
        }
        // Highlight active tab
        for (Component c : tabBar.getComponents()) {
            if (c instanceof JButton btn) {
                boolean active = tab.equals(btn.getText());
                btn.setBackground(active ? ACCENT_BLUE : new Color(0xF1F5F9));
                btn.setForeground(active ? Color.WHITE : TEXT_SECONDARY);
            }
        }
    }

    private CardLayout cards;
    private JPanel cardsContainer;
    private JPanel tabBar;

    private void showStaffShiftsView() {
        cards.show(cardsContainer, "Staff Shifts");
        refreshStaffCards();
    }

    private void showStaffRequestsView() {
        cards.show(cardsContainer, "Staff Requests");
        loadPasswordRequests();
        loadLeaveRequests();
        loadMyLeaveRequests();
    }

    private void showAccountManagementView() {
        cards.show(cardsContainer, "Account Management");
        loadUsers();
    }

    // ─── Staff Request Dialog ────────────────────────────────
    private void showStaffRequestDialog() {
        String[] options = { "Leave Request", "Change Password Request" };
        int choice = JOptionPane.showOptionDialog(this,
                "What would you like to do?",
                "Staff Request",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null, options, options[0]);

        if (choice == 0) {
            showLeaveRequestForm();
        } else if (choice == 1) {
            submitPasswordChangeRequest();
        }
    }

    private void showLeaveRequestForm() {
        JTextField startDateField = new JTextField(12);
        JTextField endDateField = new JTextField(12);
        JTextField reasonField = new JTextField(20);

        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0;
        gc.gridy = 0;
        form.add(new JLabel("Start Date (yyyy-MM-dd):"), gc);
        gc.gridx = 1;
        form.add(startDateField, gc);

        gc.gridx = 0;
        gc.gridy = 1;
        form.add(new JLabel("End Date (yyyy-MM-dd):"), gc);
        gc.gridx = 1;
        form.add(endDateField, gc);

        gc.gridx = 0;
        gc.gridy = 2;
        form.add(new JLabel("Reason:"), gc);
        gc.gridx = 1;
        form.add(reasonField, gc);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Leave Request", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String start = startDateField.getText().trim();
            String end = endDateField.getText().trim();
            String reason = reasonField.getText().trim();
            if (start.isEmpty() || end.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Please fill in both dates.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            // Store leave request in background
            new SwingWorker<Void, Void>() {
                private String errorMsg;

                @Override
                protected Void doInBackground() {
                    try {
                        java.sql.Connection conn = AppDatabase.openConnection();
                        String sql = "INSERT INTO staff_leave_requests (username, start_date, end_date, reason) VALUES (?, ?, ?, ?)";
                        try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                            ps.setString(1, username);
                            ps.setString(2, start);
                            ps.setString(3, end);
                            ps.setString(4, reason);
                            ps.executeUpdate();
                        }
                    } catch (Exception ex) {
                        errorMsg = ex.getMessage();
                    }
                    return null;
                }

                @Override
                protected void done() {
                    if (errorMsg != null) {
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to submit leave request:\n" + errorMsg,
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Leave request submitted successfully.",
                                "Success", JOptionPane.INFORMATION_MESSAGE);
                    }
                }
            }.execute();
        }
    }

    private void submitPasswordChangeRequest() {
        new SwingWorker<Void, Void>() {
            private boolean success;
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    UserDataManager.submitResetRequest(username);
                    success = true;
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (success) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Password change request submitted. An admin will review it.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to submit request:\n" + (errorMsg != null ? errorMsg : "unknown error"),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ─── Weekly Schedule View ─────────────────────────────────
    private void showWeeklyScheduleView() {
        cards.show(cardsContainer, "Weekly Schedule");
        refreshWeeklySchedule();
    }

    private JPanel buildWeeklyScheduleView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));

        weeklyScheduleTable = new JPanel(new GridBagLayout());
        weeklyScheduleTable.setBackground(Color.WHITE);
        panel.add(new JScrollPane(weeklyScheduleTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel weeklyScheduleTable;

    // ─── Attendance History View ────────────────────────────
    private void showAttendanceHistoryView() {
        cards.show(cardsContainer, "Attendance History");
        loadAttendanceHistory();
    }

    private JPanel buildAttendanceHistoryView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));
        panel.add(createAttendanceHistoryPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel attendanceHistoryPanel;

    private void refreshWeeklySchedule() {
        if (weeklyScheduleTable == null)
            return;
        weeklyScheduleTable.removeAll();
        weeklyScheduleTable.setLayout(new GridBagLayout());

        new SwingWorker<Void, Void>() {
            private List<UserAccount> users;
            private Map<String, Map<Integer, String>> allSchedules = new HashMap<>();
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    users = accountRoleRepository.listUsers();
                    for (UserAccount u : users) {
                        try {
                            allSchedules.put(u.getUsername(), scheduleRepo.loadSchedule(u.getUsername()));
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
                if (users == null)
                    return;
                renderScheduleTable(users, allSchedules);
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load schedules:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void renderScheduleTable(List<UserAccount> users, Map<String, Map<Integer, String>> allSchedules) {
        weeklyScheduleTable.removeAll();
        weeklyScheduleTable.setLayout(new GridBagLayout());

        String[] days = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);

        // Header row (gridy = 0)
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1.2;
        JPanel headerStaff = scheduleHeaderCell("Staff");
        headerStaff.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                new EmptyBorder(8, 6, 8, 6)));
        weeklyScheduleTable.add(headerStaff, c);

        for (int i = 0; i < 7; i++) {
            c.gridx = i + 1;
            c.weightx = 0.9;
            JPanel headerDay = scheduleHeaderCell(days[i]);
            headerDay.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                    new EmptyBorder(8, 6, 8, 6)));
            weeklyScheduleTable.add(headerDay, c);
        }

        // Staff rows (gridy = 1..N)
        int row = 1;
        for (UserAccount user : users) {
            boolean isCurrentUser = user.getUsername().equals(this.username);
            Color rowBg = isCurrentUser ? new Color(0xEFF6FF) : Color.WHITE;

            c.gridy = row;
            c.gridx = 0;
            c.weightx = 1.2;
            JPanel staffCell = scheduleStaffCell(user, rowBg);
            staffCell.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)),
                    new EmptyBorder(8, 10, 8, 10)));
            weeklyScheduleTable.add(staffCell, c);

            Map<Integer, String> sched = allSchedules.getOrDefault(user.getUsername(), new HashMap<>());
            for (int day = 1; day <= 7; day++) {
                c.gridx = day;
                c.weightx = 0.9;
                String shift = sched.getOrDefault(day, "rest");
                JPanel dayCell = scheduleDayCell(shift, rowBg);
                dayCell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE5E7EB)));
                weeklyScheduleTable.add(dayCell, c);
            }
            row++;
        }

        // Filler row to push everything to top
        c.gridy = row;
        c.gridx = 0;
        c.gridwidth = 8;
        c.weightx = 1;
        c.weighty = 1;
        JPanel filler = new JPanel();
        filler.setBackground(Color.WHITE);
        weeklyScheduleTable.add(filler, c);

        weeklyScheduleTable.revalidate();
        weeklyScheduleTable.repaint();
    }

    private JPanel scheduleHeaderCell(String text) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(new Color(0xF8FAFC));

        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(0x64748B));
        cell.add(lbl);
        return cell;
    }

    private JPanel scheduleStaffCell(UserAccount user, Color bg) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(bg);

        JLabel avatar = createAvatarLabel(user, 30, new Color(0x2563EB));

        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);

        String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName()
                : user.getUsername();
        JLabel nameLabel = new JLabel(displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(new Color(0x0F172A));

        JLabel roleLabel = new JLabel("Barista");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLabel.setForeground(new Color(0x64748B));

        nameStack.add(nameLabel);
        nameStack.add(roleLabel);

        GridBagConstraints cc = new GridBagConstraints();
        cc.gridy = 0;
        cc.fill = GridBagConstraints.NONE;
        cc.anchor = GridBagConstraints.CENTER;

        cc.gridx = 0;
        cc.weightx = 0;
        cell.add(avatar, cc);

        cc.gridx = 1;
        cc.weightx = 1;
        cc.fill = GridBagConstraints.HORIZONTAL;
        cc.insets = new Insets(0, 8, 0, 0);
        cell.add(nameStack, cc);

        return cell;
    }

    private JPanel scheduleDayCell(String shift, Color bg) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(bg);
        cell.setBorder(new EmptyBorder(0, 0, 0, 0));

        JLabel pill = new JLabel(scheduleShiftLabel(shift), SwingConstants.CENTER);
        pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        pill.setOpaque(true);
        pill.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        pill.setPreferredSize(new Dimension(74, 22));

        switch (shift) {
            case "afternoon":
                pill.setBackground(new Color(0xFEF3C7));
                pill.setForeground(new Color(0xB45309));
                break;
            case "night":
                pill.setBackground(new Color(0xDBEAFE));
                pill.setForeground(new Color(0x1D4ED8));
                break;
            default:
                pill.setBackground(new Color(0xFEE2E2));
                pill.setForeground(new Color(0xDC2626));
                break;
        }

        cell.add(pill);
        return cell;
    }

    private String scheduleShiftLabel(String shift) {
        switch (shift) {
            case "afternoon":
                return "11:00AM-4PM";
            case "night":
                return "4:00PM-11PM";
            default:
                return "Rest Day";
        }
    }

    // ─── Staff Shifts View ──────────────────────────────────────
    private JPanel buildStaffShiftsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));

        staffCardsPanel.setBackground(new Color(0xF9FAFB));
        JScrollPane scroll = new JScrollPane(staffCardsPanel);
        scroll.setBorder(null);
        scroll.setBackground(new Color(0xF9FAFB));
        scroll.getViewport().setBackground(new Color(0xF9FAFB));
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ─── Staff Requests View ─────────────────────────────────
    private JPanel buildStaffRequestsView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));

        if (currentRole == Role.ADMIN) {
            // Admin sees two inner tabs: Password Request + Leave Request
            JTabbedPane innerTabs = new JTabbedPane();
            innerTabs.addTab("Password Request", createPasswordRequestsPanel());
            innerTabs.addTab("Leave Request", createAdminLeaveRequestsPanel());
            panel.add(innerTabs, BorderLayout.CENTER);
        } else {
            // Staff sees two inner tabs: My Requests (leave + pw) + their shift context
            JTabbedPane innerTabs = new JTabbedPane();
            innerTabs.addTab("My Requests", createStaffMyRequestsPanel());
            panel.add(innerTabs, BorderLayout.CENTER);
        }

        return panel;
    }

    // ─── Admin: Leave Request panel ──────────────────────────
    private JPanel createAdminLeaveRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        FilterRow toolbar = new FilterRow();
        toolbar.addLabeled("Status:", leaveStatusFilter);
        toolbar.add(refreshLeaveBtn);

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        approveLeaveBtn.setEnabled(false);
        rejectLeaveBtn.setEnabled(false);
        actionBar.add(approveLeaveBtn);
        actionBar.add(rejectLeaveBtn);

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.add(toolbar, BorderLayout.NORTH);
        topBar.add(actionBar, BorderLayout.SOUTH);
        panel.add(topBar, BorderLayout.NORTH);

        leaveRequestsTable.setModel(leaveRequestsModel);
        leaveRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        leaveRequestsSorter = new TableRowSorter<>(leaveRequestsModel);
        leaveRequestsTable.setRowSorter(leaveRequestsSorter);
        hideColumn(leaveRequestsTable, 0);

        // Colored status badge renderer on the Status column (index 5 in model, 4 visible)
        leaveRequestsTable.getColumnModel().getColumn(5).setCellRenderer(new LeaveStatusRenderer());

        leaveRequestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = leaveRequestsTable.getSelectedRow();
                if (row >= 0) {
                    int modelRow = leaveRequestsTable.convertRowIndexToModel(row);
                    String status = leaveRequestsModel.getValueAt(modelRow, 5).toString();
                    boolean isPending = "pending".equalsIgnoreCase(status);
                    approveLeaveBtn.setEnabled(isPending);
                    rejectLeaveBtn.setEnabled(isPending);
                } else {
                    approveLeaveBtn.setEnabled(false);
                    rejectLeaveBtn.setEnabled(false);
                }
            }
        });

        refreshLeaveBtn.addActionListener(e -> loadLeaveRequests());
        leaveStatusFilter.addActionListener(e -> applyLeaveRequestsFilter());
        approveLeaveBtn.addActionListener(e -> onApproveLeaveRequest());
        rejectLeaveBtn.addActionListener(e -> onRejectLeaveRequest());

        panel.add(new JScrollPane(leaveRequestsTable), BorderLayout.CENTER);
        return panel;
    }

    // ─── Staff: My Requests panel ────────────────────────────
    private JPanel createStaffMyRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        // ── Submit form ──
        JTextField startDateField = new JTextField(12);
        JTextField endDateField   = new JTextField(12);

        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        form.add(new JLabel("Leave: From"));
        form.add(startDateField);
        form.add(new JLabel("To"));
        form.add(endDateField);

        JButton submitLeaveBtn = new JButton("Submit Leave Request");
        submitLeaveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submitLeaveBtn.setForeground(Color.WHITE);
        submitLeaveBtn.setBackground(new Color(0x10B981));
        submitLeaveBtn.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        submitLeaveBtn.setFocusPainted(false);
        submitLeaveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitLeaveBtn.addActionListener(e -> {
            String start = startDateField.getText().trim();
            String end   = endDateField.getText().trim();
            if (start.isEmpty() || end.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please fill in both dates.", "Validation",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            new SwingWorker<Void, Void>() {
                private String errorMsg;
                @Override protected Void doInBackground() {
                    try {
                        new SQLiteLeaveRequestRepository().createRequest(username, start, end);
                    } catch (Exception ex) { errorMsg = ex.getMessage(); }
                    return null;
                }
                @Override protected void done() {
                    if (errorMsg != null) {
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to submit leave request:\n" + errorMsg, "Error",
                                JOptionPane.ERROR_MESSAGE);
                    } else {
                        startDateField.setText("");
                        endDateField.setText("");
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Leave request submitted successfully.", "Success",
                                JOptionPane.INFORMATION_MESSAGE);
                        loadMyLeaveRequests();
                    }
                }
            }.execute();
        });

        JButton submitPwBtn = new JButton("Submit Password Reset");
        submitPwBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        submitPwBtn.setForeground(Color.WHITE);
        submitPwBtn.setBackground(ACCENT_BLUE);
        submitPwBtn.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
        submitPwBtn.setFocusPainted(false);
        submitPwBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        submitPwBtn.addActionListener(e -> submitPasswordChangeRequest());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        btnRow.add(submitLeaveBtn);
        btnRow.add(submitPwBtn);

        JPanel topSection = new JPanel(new BorderLayout());
        topSection.add(form,   BorderLayout.NORTH);
        topSection.add(btnRow, BorderLayout.SOUTH);
        panel.add(topSection, BorderLayout.NORTH);

        // ── My requests history table ──
        myLeaveTable.setModel(myLeaveModel);
        myLeaveTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Status column index 3 — colored badge
        myLeaveTable.getColumnModel().getColumn(3).setCellRenderer(new LeaveStatusRenderer());

        panel.add(new JScrollPane(myLeaveTable), BorderLayout.CENTER);
        return panel;
    }

    private void refreshStaffCards() {
        staffCardsPanel.removeAll();
        new SwingWorker<Void, Void>() {
            private List<UserAccount> users;
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    users = accountRoleRepository.listUsers();
                    for (UserAccount user : users) {
                        StaffShift latest = shiftRepo.findLatestShift(user.getUsername());
                        staffCardsPanel.add(buildStaffCard(user, latest));
                    }
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                staffCardsPanel.revalidate();
                staffCardsPanel.repaint();
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load staff data:\n" + errorMsg,
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private JPanel buildStaffCard(UserAccount user, StaffShift latestShift) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                new EmptyBorder(16, 16, 12, 16)));

        // ── Top row: avatar + name/role + status badge ──
        JPanel topRow = new JPanel(new BorderLayout(8, 0));
        topRow.setOpaque(false);

        JLabel avatar = createAvatarLabel(user, 42, user.getRole() == Role.ADMIN ? ACCENT_BLUE : STATUS_GREEN);

        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);

        JLabel nameLabel = new JLabel(user.getFullName() != null && !user.getFullName().isEmpty()
                ? user.getFullName()
                : user.getUsername());
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(TEXT_PRIMARY);

        JLabel roleLabel = new JLabel(user.getRole().name());
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        roleLabel.setForeground(TEXT_SECONDARY);

        nameStack.add(nameLabel);
        nameStack.add(roleLabel);

        // Status badge
        JLabel statusBadge = buildStatusBadge(latestShift);
        statusBadge.setPreferredSize(new Dimension(70, 22));

        topRow.add(avatar, BorderLayout.WEST);
        topRow.add(nameStack, BorderLayout.CENTER);
        topRow.add(statusBadge, BorderLayout.EAST);

        // ── Schedule info ──
        JPanel infoRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        infoRow.setOpaque(false);
        JLabel schedLabel = new JLabel("Set shift TBD  |  36 hrs/week");
        schedLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        schedLabel.setForeground(TEXT_SECONDARY);
        infoRow.add(schedLabel);

        // ── Quick action buttons ──
        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        actionRow.setOpaque(false);

        JButton lateBtn = new JButton("Mark As Late");
        lateBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lateBtn.setForeground(STATUS_ORANGE);
        lateBtn.setBackground(BG_ORANGE);
        lateBtn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        lateBtn.setFocusPainted(false);
        lateBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lateBtn.putClientProperty("appTheme.variant", "text");
        lateBtn.addActionListener(e -> onMarkAsLate(user.getUsername()));

        JButton absentBtn = new JButton("Absent");
        absentBtn.setFont(new Font("Segoe UI", Font.BOLD, 10));
        absentBtn.setForeground(STATUS_RED);
        absentBtn.setBackground(BG_RED);
        absentBtn.setBorder(BorderFactory.createEmptyBorder(5, 12, 5, 12));
        absentBtn.setFocusPainted(false);
        absentBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        absentBtn.putClientProperty("appTheme.variant", "text");
        absentBtn.addActionListener(e -> onMarkAsAbsent(user.getUsername()));

        actionRow.add(lateBtn);
        actionRow.add(absentBtn);

        boolean hasActiveShift = latestShift != null && latestShift.getEndedAt() == null
                && !"absent".equals(latestShift.getStatus()) && !"late".equals(latestShift.getStatus());
        boolean hasLateShift = latestShift != null && "late".equals(latestShift.getStatus());
        boolean hasAbsentShift = latestShift != null && "absent".equals(latestShift.getStatus());

        lateBtn.setEnabled(hasActiveShift);
        absentBtn.setEnabled(hasActiveShift || hasLateShift);

        // ── Bottom icon buttons ──
        JPanel iconRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        iconRow.setOpaque(false);

        JButton editBtn = new JButton(pencilIcon());
        editBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        editBtn.setForeground(new Color(0xCA8A04));
        editBtn.setBackground(new Color(0xFEF9C3));
        editBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        editBtn.setFocusPainted(false);
        editBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editBtn.setToolTipText("Edit profile");
        editBtn.addActionListener(e -> onViewUserDetails(user.getUsername()));

        JButton deleteBtn = new JButton(trashIcon());
        deleteBtn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        deleteBtn.setForeground(STATUS_RED);
        deleteBtn.setBackground(BG_RED);
        deleteBtn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        deleteBtn.setFocusPainted(false);
        deleteBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        deleteBtn.setToolTipText("Delete staff");
        deleteBtn.addActionListener(e -> onDeleteUser(user.getUsername()));

        iconRow.add(editBtn);
        iconRow.add(deleteBtn);

        card.add(topRow);
        card.add(Box.createVerticalStrut(8));
        card.add(infoRow);

        JPanel bottomStack = new JPanel();
        bottomStack.setLayout(new BoxLayout(bottomStack, BoxLayout.Y_AXIS));
        bottomStack.setOpaque(false);
        bottomStack.add(actionRow);
        bottomStack.add(Box.createVerticalStrut(4));
        bottomStack.add(iconRow);
        card.add(Box.createVerticalStrut(8));
        card.add(bottomStack);
        card.add(Box.createVerticalGlue());

        return card;
    }

    private JLabel createAvatarLabel(UserAccount user, int size, Color fallbackColor) {
        try {
            if (profilePictureRepository != null) {
                byte[] imageBytes = profilePictureRepository.loadPicture(user.getUsername());
                if (imageBytes != null && imageBytes.length > 0) {
                    Image image = new ImageIcon(imageBytes).getImage();
                    Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    JLabel avatar = new JLabel(new ImageIcon(scaled));
                    avatar.setPreferredSize(new Dimension(size, size));
                    avatar.setMinimumSize(new Dimension(size, size));
                    avatar.setHorizontalAlignment(SwingConstants.CENTER);
                    avatar.setVerticalAlignment(SwingConstants.CENTER);
                    return avatar;
                }
            }
        } catch (Exception ignored) {
        }

        JLabel avatar = new JLabel(String.valueOf(Character.toUpperCase(user.getUsername().charAt(0)))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(fallbackColor);
                int d = Math.min(getWidth(), getHeight());
                g2.fillOval(0, 0, d - 1, d - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, size >= 42 ? 16 : 12));
                FontMetrics fm = g2.getFontMetrics();
                String text = getText();
                g2.drawString(text, (getWidth() - fm.stringWidth(text)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(size, size));
        avatar.setMinimumSize(new Dimension(size, size));
        avatar.setOpaque(false);
        avatar.setHorizontalAlignment(SwingConstants.CENTER);
        avatar.setVerticalAlignment(SwingConstants.CENTER);
        return avatar;
    }

    private JLabel buildStatusBadge(StaffShift shift) {
        String text;
        Color bg;
        Color fg;

        if (shift == null) {
            text = "Off Duty";
            bg = BG_GRAY;
            fg = STATUS_GRAY;
        } else {
            String status = shift.getStatus();
            if ("late".equals(status)) {
                text = "Late";
                bg = BG_ORANGE;
                fg = STATUS_ORANGE;
            } else if ("absent".equals(status)) {
                text = "Absent";
                bg = BG_RED;
                fg = STATUS_RED;
            } else if ("active".equals(status) && shift.getEndedAt() == null) {
                text = "On Shift";
                bg = BG_GREEN;
                fg = STATUS_GREEN;
            } else if ("completed".equals(status)) {
                text = "Completed";
                bg = BG_GRAY;
                fg = STATUS_GRAY;
            } else {
                text = "Off Duty";
                bg = BG_GRAY;
                fg = STATUS_GRAY;
            }
        }

        JLabel badge = new JLabel(text, SwingConstants.CENTER);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setForeground(fg);
        badge.setBackground(bg);
        badge.setOpaque(true);
        badge.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
        return badge;
    }

    // ─── Actions ────────────────────────────────────────────────
    private void onMarkAsLate(String targetUser) {
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    shiftRepo.markAsLate(targetUser);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to mark as late:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshStaffCards();
            }
        }.execute();
    }

    private void onMarkAsAbsent(String targetUser) {
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    shiftRepo.markAsAbsent(targetUser);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to mark as absent:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshStaffCards();
            }
        }.execute();
    }

    private void onDeleteUser(String targetUser) {
        if (targetUser.equals(username)) {
            JOptionPane.showMessageDialog(this, "You cannot delete your own account.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete \"" + targetUser + "\"? This cannot be undone.",
                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    accountRoleRepository.deleteUser(targetUser);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to delete user:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                }
                refreshAll();
            }
        }.execute();
    }

    // ─── Account Management View ─────────────────────────────────
    private JPanel buildAccountManagementView() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(new EmptyBorder(16, 24, 24, 24));
        panel.add(createUsersSubPanel(), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createUsersSubPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        buttons.add(createAccountBtn);
        buttons.add(viewDetailsBtn);
        buttons.add(resetPasswordBtn);
        buttons.add(makeAdminBtn);
        buttons.add(makeStaffBtn);
        buttons.add(refreshUsersBtn);

        usersTable.setModel(usersModel);
        usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(usersModel);
        usersTable.setRowSorter(sorter);

        // Events
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

        panel.add(buttons, BorderLayout.NORTH);
        panel.add(new JScrollPane(usersTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel createPasswordRequestsPanel() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        boolean isAdmin = currentRole == Role.ADMIN;

        if (isAdmin) {
            FilterRow toolbar = new FilterRow();
            toolbar.addLabeled("Status:", requestStatusFilter);
            toolbar.add(refreshRequestsBtn);

            JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
            approveRequestBtn.setEnabled(false);
            rejectRequestBtn.setEnabled(false);
            actionBar.add(approveRequestBtn);
            actionBar.add(rejectRequestBtn);

            JPanel topBar = new JPanel(new BorderLayout());
            topBar.add(toolbar, BorderLayout.NORTH);
            topBar.add(actionBar, BorderLayout.SOUTH);
            panel.add(topBar, BorderLayout.NORTH);

            refreshRequestsBtn.addActionListener(e -> loadPasswordRequests());
            requestStatusFilter.addActionListener(e -> applyRequestsFilter());
            approveRequestBtn.addActionListener(e -> onApproveRequest());
            rejectRequestBtn.addActionListener(e -> onRejectRequest());
        }

        resetRequestsTable.setModel(resetRequestsModel);
        resetRequestsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resetRequestsSorter = new TableRowSorter<>(resetRequestsModel);
        resetRequestsTable.setRowSorter(resetRequestsSorter);
        hideColumn(resetRequestsTable, 0);

        resetRequestsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = resetRequestsTable.getSelectedRow();
                boolean sel = row >= 0;
                if (sel && isAdmin) {
                    int modelRow = resetRequestsTable.convertRowIndexToModel(row);
                    String status = resetRequestsModel.getValueAt(modelRow, 2).toString();
                    approveRequestBtn.setEnabled("PENDING".equals(status));
                    rejectRequestBtn.setEnabled("PENDING".equals(status));
                }
            }
        });

        panel.add(new JScrollPane(resetRequestsTable), BorderLayout.CENTER);
        return panel;
    }

    // ─── Attendance History ──────────────────────────────────
    private JTable attendanceTable;
    private DefaultTableModel attendanceModel;
    private JTextField attendanceSearchField;
    private JTextField attendanceDateFrom;
    private JTextField attendanceDateTo;
    private java.time.LocalDate attendanceFromDate;
    private java.time.LocalDate attendanceToDate;

    private JPanel createAttendanceHistoryPanel() {
        attendanceModel = new DefaultTableModel(
                new String[] { "Staff Name", "Date", "Clock In", "Clock Out", "Duration", "Status" }, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };
        attendanceTable = new JTable(attendanceModel);
        attendanceTable.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        attendanceTable.setRowHeight(32);
        attendanceTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        attendanceTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 11));
        attendanceTable.getTableHeader().setBackground(new Color(0xF8FAFC));

        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(8, 8, 8, 8));

        if (currentRole == Role.ADMIN) {
            panel.add(buildAttendanceControlBar(), BorderLayout.NORTH);
        }
        panel.add(new JScrollPane(attendanceTable), BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildAttendanceControlBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setOpaque(false);
        bar.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));

        // Search field (left)
        attendanceSearchField = new JTextField(18);
        attendanceSearchField.putClientProperty("JTextField.placeholderText", "Search Barista");
        attendanceSearchField.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        attendanceSearchField.setForeground(new Color(0x0F172A));
        attendanceSearchField.setBackground(new Color(0xF3F4F6));
        attendanceSearchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        attendanceSearchField.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                applyAttendanceFilter();
            }
        });

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        searchPanel.setOpaque(false);
        JLabel searchIcon = new JLabel("\uD83D\uDD0D");
        searchIcon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        searchIcon.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 6));
        searchPanel.add(searchIcon);
        searchPanel.add(attendanceSearchField);

        // Date range (right)
        attendanceDateFrom = buildDateField();
        attendanceDateTo = buildDateField();

        JLabel dateLabel = new JLabel("\uD83D\uDCC5");
        dateLabel.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        datePanel.setOpaque(false);
        datePanel.add(new JLabel("From:"));
        datePanel.add(attendanceDateFrom);
        datePanel.add(new JLabel("To:"));
        datePanel.add(attendanceDateTo);
        datePanel.add(dateLabel);

        JButton clearBtn = new JButton("Clear");
        clearBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        clearBtn.setForeground(new Color(0x64748B));
        clearBtn.setBackground(Color.WHITE);
        clearBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                new EmptyBorder(6, 12, 6, 12)));
        clearBtn.setFocusPainted(false);
        clearBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearBtn.addActionListener(e -> {
            attendanceDateFrom.setText("");
            attendanceDateTo.setText("");
            attendanceFromDate = null;
            attendanceToDate = null;
            attendanceSearchField.setText("");
            loadAttendanceHistory();
        });
        datePanel.add(clearBtn);

        bar.add(searchPanel, BorderLayout.WEST);
        bar.add(datePanel, BorderLayout.EAST);
        return bar;
    }

    private JTextField buildDateField() {
        JTextField field = new JTextField(10);
        field.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        field.setForeground(new Color(0x0F172A));
        field.setBackground(new Color(0xF3F4F6));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        field.setEditable(false);
        field.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        field.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                showAttendanceDatePicker(field);
            }
        });
        return field;
    }

    private void showAttendanceDatePicker(JTextField target) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Select Date",
                ModalityType.APPLICATION_MODAL);
        dialog.setResizable(false);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        final java.time.YearMonth[] currentMonth = { java.time.YearMonth.now() };
        JPanel gridPanel = new JPanel(new GridLayout(0, 7, 0, 0));
        gridPanel.setBackground(Color.WHITE);
        JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        monthLabel.setForeground(new Color(0x0F172A));
        monthLabel.setPreferredSize(new Dimension(160, 30));

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(Color.WHITE);
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

        // Header with nav
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(Color.WHITE);
        header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
        JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        nav.setOpaque(false);

        JButton prevYear = new JButton("<<");
        prevYear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prevYear.setForeground(new Color(0x475569));
        prevYear.setBackground(Color.WHITE);
        prevYear.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        prevYear.setFocusPainted(false);
        prevYear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        prevYear.addActionListener(ev -> {
            currentMonth[0] = currentMonth[0].minusYears(1);
            renderDateGrid(gridPanel, monthLabel, currentMonth[0], target, dialog);
        });

        JButton prevMonth = new JButton("<");
        prevMonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        prevMonth.setForeground(new Color(0x475569));
        prevMonth.setBackground(Color.WHITE);
        prevMonth.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        prevMonth.setFocusPainted(false);
        prevMonth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        prevMonth.addActionListener(ev -> {
            currentMonth[0] = currentMonth[0].minusMonths(1);
            renderDateGrid(gridPanel, monthLabel, currentMonth[0], target, dialog);
        });

        JButton nextMonth = new JButton(">");
        nextMonth.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextMonth.setForeground(new Color(0x475569));
        nextMonth.setBackground(Color.WHITE);
        nextMonth.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        nextMonth.setFocusPainted(false);
        nextMonth.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextMonth.addActionListener(ev -> {
            currentMonth[0] = currentMonth[0].plusMonths(1);
            renderDateGrid(gridPanel, monthLabel, currentMonth[0], target, dialog);
        });

        JButton nextYear = new JButton(">>");
        nextYear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nextYear.setForeground(new Color(0x475569));
        nextYear.setBackground(Color.WHITE);
        nextYear.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        nextYear.setFocusPainted(false);
        nextYear.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        nextYear.addActionListener(ev -> {
            currentMonth[0] = currentMonth[0].plusYears(1);
            renderDateGrid(gridPanel, monthLabel, currentMonth[0], target, dialog);
        });

        nav.add(prevYear);
        nav.add(prevMonth);
        nav.add(monthLabel);
        nav.add(nextMonth);
        nav.add(nextYear);
        header.add(nav, BorderLayout.CENTER);
        root.add(header, BorderLayout.NORTH);
        root.add(gridPanel, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancel.setForeground(new Color(0x0F172A));
        cancel.setBackground(Color.WHITE);
        cancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB)),
                new EmptyBorder(7, 18, 7, 18)));
        cancel.setFocusPainted(false);
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(ev -> dialog.dispose());

        JButton apply = new JButton("Apply");
        apply.setFont(new Font("Segoe UI", Font.BOLD, 12));
        apply.setForeground(Color.WHITE);
        apply.setBackground(new Color(0x0F172A));
        apply.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        apply.setFocusPainted(false);
        apply.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        final java.time.LocalDate[] picked = { null };
        apply.addActionListener(ev -> {
            if (picked[0] != null) {
                target.setText(picked[0].format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                // Update the corresponding LocalDate reference
                if (target == attendanceDateFrom)
                    attendanceFromDate = picked[0];
                if (target == attendanceDateTo)
                    attendanceToDate = picked[0];
                loadAttendanceHistory();
            }
            dialog.dispose();
        });

        footer.add(cancel);
        footer.add(apply);
        root.add(footer, BorderLayout.SOUTH);

        // Initial render
        renderDateGrid(gridPanel, monthLabel, currentMonth[0], target, dialog, picked);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(target);
        dialog.setVisible(true);
    }

    private void renderDateGrid(JPanel grid, JLabel monthLabel, java.time.YearMonth ym,
            JTextField target, JDialog dialog) {
        renderDateGrid(grid, monthLabel, ym, target, dialog, null);
    }

    private void renderDateGrid(JPanel grid, JLabel monthLabel, java.time.YearMonth ym,
            JTextField target, JDialog dialog,
            java.time.LocalDate[] pickedRef) {
        grid.removeAll();
        monthLabel.setText(ym.getMonth().getDisplayName(
                java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH) + " " + ym.getYear());

        String[] dayNames = { "Su", "Mo", "Tu", "We", "Th", "Fr", "Sa" };
        for (String d : dayNames) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(new Color(0x9CA3AF));
            grid.add(lbl);
        }

        java.time.DayOfWeek firstDow = ym.atDay(1).getDayOfWeek();
        int leading = (firstDow.getValue() % 7);
        int daysInMonth = ym.lengthOfMonth();
        java.time.LocalDate today = java.time.LocalDate.now();

        for (int i = 0; i < leading; i++) {
            grid.add(new JLabel(""));
        }

        for (int day = 1; day <= daysInMonth; day++) {
            java.time.LocalDate date = ym.atDay(day);
            boolean isToday = date.equals(today);

            JLabel lbl = new JLabel(String.valueOf(day), SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            lbl.setForeground(isToday ? Color.WHITE : new Color(0x0F172A));
            lbl.setOpaque(true);
            lbl.setBackground(isToday ? new Color(0x2563EB) : Color.WHITE);
            lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            lbl.setPreferredSize(new Dimension(34, 28));
            int d = day;
            lbl.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (pickedRef != null)
                        pickedRef[0] = date;
                    target.setText(date.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")));
                    if (target == attendanceDateFrom)
                        attendanceFromDate = date;
                    if (target == attendanceDateTo)
                        attendanceToDate = date;
                    loadAttendanceHistory();
                    dialog.dispose();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    if (!isToday)
                        lbl.setBackground(new Color(0xF3F4F6));
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    if (!isToday)
                        lbl.setBackground(Color.WHITE);
                }
            });
            grid.add(lbl);
        }

        grid.revalidate();
        grid.repaint();
    }

    private void loadAttendanceHistory() {
        attendanceModel.setRowCount(0);
        new SwingWorker<Void, Void>() {
            private List<StaffShift> shifts;
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    if (currentRole == Role.ADMIN) {
                        shifts = shiftRepo.findAllShifts();
                    } else {
                        shifts = shiftRepo.findShifts(username);
                    }
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (shifts == null) {
                    if (errorMsg != null)
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to load attendance:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                String search = attendanceSearchField != null
                        ? attendanceSearchField.getText().trim().toLowerCase()
                        : "";

                for (StaffShift s : shifts) {
                    // Apply search filter
                    if (!search.isEmpty() && !s.getUsername().toLowerCase().contains(search)) {
                        continue;
                    }

                    String date = "", clockIn = "", clockOut = "", duration = "";
                    if (s.getStartedAt() != null && !s.getStartedAt().isEmpty()) {
                        String[] parts = s.getStartedAt().split(" ");
                        date = parts.length > 0 ? parts[0] : "";
                        clockIn = parts.length > 1 ? parts[1] : "";
                    }

                    // Apply date range filter
                    if (attendanceFromDate != null && !date.isEmpty()) {
                        try {
                            java.time.LocalDate rowDate = java.time.LocalDate.parse(date);
                            if (rowDate.isBefore(attendanceFromDate))
                                continue;
                        } catch (Exception ignored) {
                        }
                    }
                    if (attendanceToDate != null && !date.isEmpty()) {
                        try {
                            java.time.LocalDate rowDate = java.time.LocalDate.parse(date);
                            if (rowDate.isAfter(attendanceToDate))
                                continue;
                        } catch (Exception ignored) {
                        }
                    }

                    if (s.getEndedAt() != null && !s.getEndedAt().isEmpty()) {
                        String[] parts = s.getEndedAt().split(" ");
                        clockOut = parts.length > 1 ? parts[1] : "";
                    }
                    if (s.getStartedAt() != null && s.getEndedAt() != null) {
                        duration = computeDuration(s.getStartedAt(), s.getEndedAt());
                    }
                    String status = s.getStatus() != null ? s.getStatus() : "";
                    attendanceModel.addRow(new Object[] {
                            s.getUsername(), date, clockIn, clockOut, duration, status
                    });
                }
                if (errorMsg != null)
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load attendance:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }.execute();
    }

    private void applyAttendanceFilter() {
        loadAttendanceHistory();
    }

    private String computeDuration(String start, String end) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            long diff = sdf.parse(end).getTime() - sdf.parse(start).getTime();
            long hours = diff / 3600000;
            long mins = (diff % 3600000) / 60000;
            return String.format("%dh %02dm", hours, mins);
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Refresh ─────────────────────────────────────────────────
    private void refreshAll() {
        if (currentRole == Role.ADMIN) {
            refreshStaffCards();
            loadLeaveRequests();
        } else {
            loadMyLeaveRequests();
        }
        loadUsers();
        loadPasswordRequests();
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
                    for (UserAccount u : get()) {
                        usersModel.addRow(new Object[] {
                                u.getStaffId(), u.getUsername(), u.getFullName(),
                                u.getRole().name(), u.getGender(), u.getMobile(), u.getCreatedAt()
                        });
                    }
                    if (errorMsg != null)
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to load users:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load users:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
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
                                r.getId(), r.getUsername(), r.getStatus().name(),
                                r.getRequestedAt(), r.getResolvedAt() != null ? r.getResolvedAt() : ""
                        });
                    }
                    applyRequestsFilter();
                    if (errorMsg != null)
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to load requests:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load requests:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    // ─── Account Management helpers (from old StaffPanel) ────────
    private void updateSelectedUserRole(Role newRole) {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0) {
            JOptionPane.showMessageDialog(this, "Please select a user.",
                    "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String selectedUser = usersModel.getValueAt(modelRow, 1).toString();
        if (selectedUser.equals(username) && newRole != Role.ADMIN) {
            JOptionPane.showMessageDialog(this, "You cannot demote yourself. Another admin must do this.",
                    "Action Not Allowed", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to change " + selectedUser + "'s role to " + newRole.name() + "?",
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
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to update role:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadUsers();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            selectedUser + " updated to " + newRole.name(), "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void onCreateAccount() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        StaffRegistrationDialog dialog = new StaffRegistrationDialog(
                owner, accountRoleRepository, profilePictureRepository, this::refreshAll);
        dialog.setVisible(true);
    }

    private void onViewUserDetails() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0)
            return;
        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String uname = usersModel.getValueAt(modelRow, 1).toString();
        onViewUserDetails(uname);
    }

    private void onViewUserDetails(String uname) {
        try {
            for (UserAccount u : accountRoleRepository.listUsers()) {
                if (u.getUsername().equals(uname)) {
                    Window owner = SwingUtilities.getWindowAncestor(this);
                    new StaffProfileDialog(owner, u, accountRoleRepository,
                            profilePictureRepository, scheduleRepo, username, this::refreshAll).setVisible(true);
                    return;
                }
            }
            JOptionPane.showMessageDialog(this, "Unable to find account details for \"" + uname + "\".",
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to load account details:\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onAdminResetPassword() {
        int viewRow = usersTable.getSelectedRow();
        if (viewRow < 0)
            return;
        int modelRow = usersTable.convertRowIndexToModel(viewRow);
        String targetUser = usersModel.getValueAt(modelRow, 1).toString();

        JPasswordField pwField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("New password for \"" + targetUser + "\":"));
        form.add(pwField);
        form.add(new JLabel("Confirm password:"));
        form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Reset Password for " + targetUser,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String newPw = new String(pwField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();

        if (newPw.isEmpty() || !newPw.equals(confirm) || newPw.length() < 8
                || !newPw.matches(".*\\d.*") || !newPw.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            JOptionPane.showMessageDialog(this,
                    "Password must match confirmation, be at least 8 characters, and include a number and special character.",
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
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to reset password:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Password for \"" + targetUser + "\" has been reset.",
                            "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void onApproveRequest() {
        int viewRow = resetRequestsTable.getSelectedRow();
        if (viewRow < 0)
            return;
        int modelRow = resetRequestsTable.convertRowIndexToModel(viewRow);
        int requestId = (int) resetRequestsModel.getValueAt(modelRow, 0);
        String reqUser = resetRequestsModel.getValueAt(modelRow, 1).toString();

        JPasswordField pwField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);
        JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
        form.add(new JLabel("New password for \"" + reqUser + "\":"));
        form.add(pwField);
        form.add(new JLabel("Confirm password:"));
        form.add(confirmField);

        int result = JOptionPane.showConfirmDialog(this, form,
                "Approve Reset — Set New Password",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String newPw = new String(pwField.getPassword()).trim();
        String cfm = new String(confirmField.getPassword()).trim();

        if (newPw.isEmpty() || !newPw.equals(cfm) || newPw.length() < 8
                || !newPw.matches(".*\\d.*") || !newPw.matches(".*[!@#$%^&*(),.?\":{}|<>].*")) {
            JOptionPane.showMessageDialog(this,
                    "Password must match confirmation, be at least 8 characters, and include a number and special character.",
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
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to approve request:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
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
        if (viewRow < 0)
            return;
        int modelRow = resetRequestsTable.convertRowIndexToModel(viewRow);
        int requestId = (int) resetRequestsModel.getValueAt(modelRow, 0);
        String reqUser = resetRequestsModel.getValueAt(modelRow, 1).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reject the password reset request for \"" + reqUser + "\"?",
                "Confirm Reject", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

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
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to reject request:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadPasswordRequests();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Request for \"" + reqUser + "\" has been rejected.",
                            "Rejected", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    // ─── Filter helpers ──────────────────────────────────────────
    private void applyRequestsFilter() {
        if (resetRequestsSorter == null) return;
        String selected = (String) requestStatusFilter.getSelectedItem();
        if (selected == null || "All".equals(selected)) {
            resetRequestsSorter.setRowFilter(null);
        } else {
            resetRequestsSorter.setRowFilter(RowFilter.regexFilter("(?i)^" + selected.toUpperCase() + "$", 2));
        }
    }

    // ─── Icon helpers ────────────────────────────────────────────
    private Icon pencilIcon() {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xCA8A04));
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawLine(x + 2, y + 10, x + 10, y + 2);
                g2.drawLine(x + 2, y + 10, x + 4, y + 11);
                g2.drawLine(x + 10, y + 2, x + 11, y + 4);
                g2.dispose();
            }
        };
    }

    private Icon trashIcon() {
        return new Icon() {
            @Override
            public int getIconWidth() {
                return 12;
            }

            @Override
            public int getIconHeight() {
                return 12;
            }

            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(STATUS_RED);
                g2.setStroke(new BasicStroke(1.5f));
                g2.drawRect(x + 2, y + 4, 8, 7);
                g2.drawLine(x + 3, y + 2, x + 9, y + 2);
                g2.drawLine(x + 5, y + 1, x + 7, y + 1);
                g2.dispose();
            }
        };
    }

    private static void hideColumn(JTable table, int col) {
        TableColumn tc = table.getColumnModel().getColumn(col);
        tc.setMinWidth(0);
        tc.setMaxWidth(0);
        tc.setWidth(0);
        tc.setPreferredWidth(0);
        tc.setResizable(false);
    }

    // ─── Leave Request data loaders ──────────────────────────────

    private void loadLeaveRequests() {
        if (currentRole != Role.ADMIN) return;
        leaveRequestsModel.setRowCount(0);
        new SwingWorker<List<LeaveRequest>, Void>() {
            private String errorMsg;
            @Override protected List<LeaveRequest> doInBackground() {
                try {
                    return new SQLiteLeaveRequestRepository().listAllRequests();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }
            @Override protected void done() {
                try {
                    for (LeaveRequest r : get()) {
                        leaveRequestsModel.addRow(new Object[] {
                                r.getId(), r.getStaffId(), r.getUsername(),
                                r.getStartDate(), r.getEndDate(),
                                r.getStatus(),
                                r.getCreatedAt()
                        });
                    }
                    applyLeaveRequestsFilter();
                    if (errorMsg != null)
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to load leave requests:\n" + errorMsg,
                                "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load leave requests:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void loadMyLeaveRequests() {
        myLeaveModel.setRowCount(0);
        new SwingWorker<List<LeaveRequest>, Void>() {
            private String errorMsg;
            @Override protected List<LeaveRequest> doInBackground() {
                try {
                    return new SQLiteLeaveRequestRepository().listRequestsForUser(username);
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                    return List.of();
                }
            }
            @Override protected void done() {
                try {
                    for (LeaveRequest r : get()) {
                        myLeaveModel.addRow(new Object[] {
                                "Leave",
                                r.getStartDate(),
                                r.getEndDate(),
                                r.getStatus(),
                                r.getCreatedAt()
                        });
                    }
                    if (errorMsg != null)
                        JOptionPane.showMessageDialog(StaffPanel.this,
                                "Failed to load your requests:\n" + errorMsg,
                                "Error", JOptionPane.ERROR_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to load your requests:\n" + ex.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        }.execute();
    }

    private void applyLeaveRequestsFilter() {
        if (leaveRequestsSorter == null) return;
        String selected = (String) leaveStatusFilter.getSelectedItem();
        if (selected == null || "All".equals(selected)) {
            leaveRequestsSorter.setRowFilter(null);
        } else {
            leaveRequestsSorter.setRowFilter(
                    RowFilter.regexFilter("(?i)^" + selected + "$", 5));
        }
    }

    private void onApproveLeaveRequest() {
        int viewRow = leaveRequestsTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = leaveRequestsTable.convertRowIndexToModel(viewRow);
        int id       = (int) leaveRequestsModel.getValueAt(modelRow, 0);
        String user  = leaveRequestsModel.getValueAt(modelRow, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Approve leave request for \"" + user + "\"?",
                "Confirm Approve", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private String errorMsg;
            @Override protected Void doInBackground() {
                try { new SQLiteLeaveRequestRepository().approveRequest(id); }
                catch (Exception ex) { errorMsg = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to approve:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadLeaveRequests();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Leave request approved for \"" + user + "\".",
                            "Approved", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    private void onRejectLeaveRequest() {
        int viewRow = leaveRequestsTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = leaveRequestsTable.convertRowIndexToModel(viewRow);
        int id       = (int) leaveRequestsModel.getValueAt(modelRow, 0);
        String user  = leaveRequestsModel.getValueAt(modelRow, 2).toString();

        int confirm = JOptionPane.showConfirmDialog(this,
                "Reject leave request for \"" + user + "\"?",
                "Confirm Reject", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new SwingWorker<Void, Void>() {
            private String errorMsg;
            @Override protected Void doInBackground() {
                try { new SQLiteLeaveRequestRepository().rejectRequest(id); }
                catch (Exception ex) { errorMsg = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Failed to reject:\n" + errorMsg, "Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    loadLeaveRequests();
                    JOptionPane.showMessageDialog(StaffPanel.this,
                            "Leave request for \"" + user + "\" rejected.",
                            "Rejected", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }.execute();
    }

    // ─── Status badge renderer for leave requests ────────────────
    private static class LeaveStatusRenderer extends javax.swing.table.DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            setHorizontalAlignment(SwingConstants.CENTER);
            if (!isSelected && value != null) {
                String s = value.toString().toLowerCase();
                switch (s) {
                    case "approved" -> { setBackground(new Color(0xD1FAE5)); setForeground(new Color(0x065F46)); }
                    case "rejected" -> { setBackground(new Color(0xFEE2E2)); setForeground(new Color(0x991B1B)); }
                    default         -> { setBackground(new Color(0xFEF3C7)); setForeground(new Color(0x92400E)); }
                }
                setOpaque(true);
            } else if (!isSelected) {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            return this;
        }
    }
}
