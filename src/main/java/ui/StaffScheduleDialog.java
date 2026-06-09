package ui;

import persistence.AccountRoleRepository;
import persistence.ProfilePictureRepository;
import persistence.StaffScheduleRepository;
import loginregister.UserAccount;
import loginregister.UserDataManager.Role;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StaffScheduleDialog extends JDialog {

    private static final Color BG_CELL = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY = new Color(0x0F172A);
    private static final Color TEXT_SECONDARY = new Color(0x64748B);
    private static final Color AFTERNOON_BG = new Color(0xFEF3C7);
    private static final Color AFTERNOON_FG = new Color(0xB45309);
    private static final Color NIGHT_BG = new Color(0xDBEAFE);
    private static final Color NIGHT_FG = new Color(0x1D4ED8);
    private static final Color REST_BG = new Color(0xFEE2E2);
    private static final Color REST_FG = new Color(0xDC2626);
    private static final Color NEW_ROW_BG = new Color(0xEFF6FF);

    private static final String[] DAY_LABELS = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };

    private static final String[][] SHIFT_OPTIONS = {
        { "Afternoon", "11:00AM - 4:00PM" },
        { "Night",     "4:00PM - 11:00PM" },
        { "Rest Day",  "Rest Day" }
    };

    private final AccountRoleRepository accountRoleRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final StaffScheduleRepository scheduleRepository;
    private final String newUsername;
    private final String newPassword;
    private final String newFullName;
    private final int newAge;
    private final String newBirthdate;
    private final String newAddress;
    private final String newContact;
    private final String newGender;
    private final byte[] newPictureBytes;
    private final Runnable onCreated;

    // day (1-7) → shift value ("afternoon", "night", "rest")
    private final Map<Integer, String> newSchedule = new HashMap<>();
    private final Map<String, Map<Integer, String>> existingSchedules = new HashMap<>();

    // UI refs for save validation
    private final JButton saveBtn = new JButton("Save & Create Account");
    private final JLabel[] dayChoiceLabels = new JLabel[7];

    public StaffScheduleDialog(
            Window owner,
            AccountRoleRepository accountRoleRepository,
            ProfilePictureRepository profilePictureRepository,
            StaffScheduleRepository scheduleRepository,
            String newUsername, String newPassword,
            String newFullName, int newAge, String newBirthdate,
            String newAddress, String newContact, String newGender,
            byte[] newPictureBytes,
            Runnable onCreated) {

        super(owner, "Schedule Setup", ModalityType.APPLICATION_MODAL);
        this.accountRoleRepository = accountRoleRepository;
        this.profilePictureRepository = profilePictureRepository;
        this.scheduleRepository = scheduleRepository;
        this.newUsername = newUsername;
        this.newPassword = newPassword;
        this.newFullName = newFullName;
        this.newAge = newAge;
        this.newBirthdate = newBirthdate;
        this.newAddress = newAddress;
        this.newContact = newContact;
        this.newGender = newGender;
        this.newPictureBytes = newPictureBytes;
        this.onCreated = onCreated;

        loadExistingSchedules();

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        buildUI();
        pack();
        setSize(new Dimension(820, Math.min(getHeight() + 40, 640)));
        setLocationRelativeTo(owner);
        validateSave();
    }

    private void loadExistingSchedules() {
        try {
            List<UserAccount> users = accountRoleRepository.listUsers();
            for (UserAccount u : users) {
                if (!u.getUsername().equals(newUsername)) {
                    Map<Integer, String> sched = scheduleRepository.loadSchedule(u.getUsername());
                    existingSchedules.put(u.getUsername(), sched);
                }
            }
        } catch (Exception ignored) {}
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 16));
        root.setBackground(new Color(0xF9FAFB));
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Set Weekly Schedule");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Assign work days and shift for \"" + newFullName + "\". "
                + "All 7 days must be set before saving.");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(TEXT_SECONDARY);

        JPanel titleStack = new JPanel();
        titleStack.setLayout(new BoxLayout(titleStack, BoxLayout.Y_AXIS));
        titleStack.setOpaque(false);
        titleStack.add(title);
        titleStack.add(subtitle);

        root.add(titleStack, BorderLayout.NORTH);
        root.add(buildScheduleTable(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JPanel buildScheduleTable() {
        JPanel table = new JPanel(new GridBagLayout());
        table.setBackground(BG_CELL);

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, 0, 0);

        // Header row
        c.gridy = 0;
        c.gridx = 0;
        c.weightx = 1.2;
        c.weighty = 0;
        table.add(headerCell("Staff"), c);

        for (int i = 0; i < 7; i++) {
            c.gridx = i + 1;
            c.weightx = 0.9;
            table.add(headerCell(DAY_LABELS[i]), c);
        }

        // Load all users (including the new one) and build rows
        try {
            List<UserAccount> users = accountRoleRepository.listUsers();
            boolean newUserListed = users.stream().anyMatch(u -> u.getUsername().equals(newUsername));
            int rowIdx = 1;

            if (!newUserListed) {
                // Create a synthetic UserAccount for the new staff member
                UserAccount newUser = new UserAccount(0, newUsername, Role.STAFF,
                        newFullName, newAge, newBirthdate, newAddress, newContact, newGender, "");
                c.gridy = rowIdx++;
                addNewStaffRow(table, c, newUser);
            }

            for (UserAccount user : users) {
                if (user.getUsername().equals(newUsername)) {
                    c.gridy = rowIdx++;
                    addNewStaffRow(table, c, user);
                } else {
                    c.gridy = rowIdx++;
                    addExistingStaffRow(table, c, user);
                }
            }
        } catch (Exception ex) {
            c.gridy = 1;
            UserAccount fallback = new UserAccount(0, newUsername, Role.STAFF,
                    newFullName, newAge, newBirthdate, newAddress, newContact, newGender, "");
            addNewStaffRow(table, c, fallback);
        }

        return table;
    }

    private void addNewStaffRow(JPanel table, GridBagConstraints c, UserAccount user) {
        // Staff column
        JPanel staffCell = buildStaffCell(user, true);
        c.gridx = 0;
        c.weightx = 1.2;
        table.add(staffCell, c);

        // Day cells with "Choose Shift" buttons
        for (int dayIdx = 0; dayIdx < 7; dayIdx++) {
            c.gridx = dayIdx + 1;
            c.weightx = 0.9;
            table.add(buildEditableDayCell(dayIdx), c);
        }
    }

    private void addExistingStaffRow(JPanel table, GridBagConstraints c, UserAccount user) {
        JPanel staffCell = buildStaffCell(user, false);
        c.gridx = 0;
        c.weightx = 1.2;
        table.add(staffCell, c);

        Map<Integer, String> sched = existingSchedules.getOrDefault(user.getUsername(), new HashMap<>());

        for (int dayIdx = 0; dayIdx < 7; dayIdx++) {
            c.gridx = dayIdx + 1;
            c.weightx = 0.9;
            int day = dayIdx + 1;
            String shift = sched.getOrDefault(day, "rest");
            table.add(buildReadOnlyDayCell(shift), c);
        }
    }

    private JPanel buildStaffCell(UserAccount user, boolean isNew) {
        JPanel cell = new JPanel(new BorderLayout(8, 0));
        cell.setBackground(isNew ? NEW_ROW_BG : BG_CELL);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(8, 10, 8, 10)));

        JLabel avatar = new JLabel(String.valueOf(Character.toUpperCase(user.getUsername().charAt(0)))) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0x2563EB));
                int d = Math.min(getWidth(), getHeight());
                g2.fillOval(0, 0, d - 1, d - 1);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                FontMetrics fm = g2.getFontMetrics();
                String t = getText();
                g2.drawString(t, (getWidth() - fm.stringWidth(t)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(30, 30));
        avatar.setMinimumSize(new Dimension(30, 30));
        avatar.setOpaque(false);

        JPanel nameStack = new JPanel();
        nameStack.setLayout(new BoxLayout(nameStack, BoxLayout.Y_AXIS));
        nameStack.setOpaque(false);

        String displayName = (user.getFullName() != null && !user.getFullName().isEmpty())
                ? user.getFullName() : user.getUsername();
        JLabel nameLabel = new JLabel(isNew ? displayName + " (new)" : displayName);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        nameLabel.setForeground(TEXT_PRIMARY);

        JLabel roleLabel = new JLabel(user.getRole().name());
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        roleLabel.setForeground(TEXT_SECONDARY);

        nameStack.add(nameLabel);
        nameStack.add(roleLabel);

        cell.add(avatar, BorderLayout.WEST);
        cell.add(nameStack, BorderLayout.CENTER);
        return cell;
    }

    private JPanel buildEditableDayCell(int dayIdx) {
        int day = dayIdx + 1;
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(NEW_ROW_BG);
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                BorderFactory.createEmptyBorder(6, 6, 6, 6)));
        cell.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Choice display label — fills the cell
        JLabel choiceLabel = new JLabel("Choose Shift", SwingConstants.CENTER);
        choiceLabel.setFont(new Font("Segoe UI", Font.BOLD, 10));
        choiceLabel.setForeground(TEXT_SECONDARY);
        choiceLabel.setOpaque(true);
        choiceLabel.setBackground(new Color(0xF1F5F9));
        choiceLabel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xCBD5E1)),
                new EmptyBorder(8, 10, 8, 10)));
        choiceLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        dayChoiceLabels[dayIdx] = choiceLabel;

        // Click anywhere on the cell opens the popup
        java.awt.event.MouseAdapter clicker = new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                showShiftPopup(day, dayIdx, cell);
            }
        };
        cell.addMouseListener(clicker);
        choiceLabel.addMouseListener(clicker);

        cell.add(choiceLabel);
        return cell;
    }

    private JPanel buildReadOnlyDayCell(String shift) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(BG_CELL);
        cell.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));

        JLabel pill = readOnlyPill(shift);
        cell.add(pill);
        return cell;
    }

    private JLabel readOnlyPill(String shift) {
        JLabel lbl = new JLabel(shiftDisplay(shift), SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        lbl.setPreferredSize(new Dimension(74, 22));

        switch (shift) {
            case "afternoon":
                lbl.setBackground(AFTERNOON_BG);
                lbl.setForeground(AFTERNOON_FG);
                break;
            case "night":
                lbl.setBackground(NIGHT_BG);
                lbl.setForeground(NIGHT_FG);
                break;
            default:
                lbl.setBackground(REST_BG);
                lbl.setForeground(REST_FG);
                break;
        }
        return lbl;
    }

    private String shiftDisplay(String shift) {
        switch (shift) {
            case "afternoon": return "11:00AM - 4:00PM";
            case "night": return "4:00PM - 11:00PM";
            default: return "Rest Day";
        }
    }

    private void showShiftPopup(int day, int dayIdx, JComponent anchor) {
        JPopupMenu menu = new JPopupMenu();

        for (String[] opt : SHIFT_OPTIONS) {
            String label = opt[0] + " (" + opt[1] + ")";
            String value = opt[0].toLowerCase().replace(" ", "");
            JMenuItem item = new JMenuItem(label);
            item.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            item.addActionListener(e -> {
                newSchedule.put(day, value);
                updateDayCell(dayIdx, value);
                validateSave();
            });
            menu.add(item);
        }

        menu.show(anchor, 0, anchor.getHeight());
    }

    private void updateDayCell(int dayIdx, String value) {
        JLabel choice = dayChoiceLabels[dayIdx];

        choice.setText(shiftDisplay(value));
        choice.setFont(new Font("Segoe UI", Font.BOLD, 10));

        switch (value) {
            case "afternoon":
                choice.setBackground(AFTERNOON_BG);
                choice.setForeground(AFTERNOON_FG);
                break;
            case "night":
                choice.setBackground(NIGHT_BG);
                choice.setForeground(NIGHT_FG);
                break;
            default:
                choice.setBackground(REST_BG);
                choice.setForeground(REST_FG);
                break;
        }
    }

    private void validateSave() {
        saveBtn.setEnabled(newSchedule.size() == 7);
    }

    private JPanel headerCell(String text) {
        JPanel cell = new JPanel(new GridBagLayout());
        cell.setBackground(new Color(0xF8FAFC));
        cell.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(8, 6, 8, 6)));

        JLabel lbl = new JLabel(text, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(TEXT_SECONDARY);
        cell.add(lbl);
        return cell;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);

        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        cancel.setForeground(TEXT_PRIMARY);
        cancel.setBackground(BG_CELL);
        cancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(8, 22, 8, 22)));
        cancel.setFocusPainted(false);
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(e -> dispose());

        saveBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        saveBtn.setForeground(Color.WHITE);
        saveBtn.setBackground(new Color(0x2563EB));
        saveBtn.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
        saveBtn.setFocusPainted(false);
        saveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        saveBtn.addActionListener(e -> onSave());

        footer.add(cancel);
        footer.add(saveBtn);
        return footer;
    }

    private void onSave() {
        if (newSchedule.size() != 7) {
            JOptionPane.showMessageDialog(this,
                    "Please assign a shift for every day of the week.",
                    "Incomplete Schedule", JOptionPane.WARNING_MESSAGE);
            return;
        }

        setEnabled(false);

        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    accountRoleRepository.createUser(newUsername, newPassword, Role.STAFF,
                            newFullName, newAge, newBirthdate, newAddress, newContact, newGender);

                    if (newPictureBytes != null && newPictureBytes.length > 0) {
                        try {
                            profilePictureRepository.savePicture(newUsername, newPictureBytes);
                        } catch (Exception ignored) {}
                    }

                    // Save schedule (all 7 days are set)
                    scheduleRepository.saveSchedule(newUsername, newSchedule);

                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                if (errorMsg != null) {
                    JOptionPane.showMessageDialog(StaffScheduleDialog.this,
                            "Failed to create account:\n" + errorMsg,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    setEnabled(true);
                    return;
                }

                if (onCreated != null) {
                    onCreated.run();
                }
                JOptionPane.showMessageDialog(StaffScheduleDialog.this,
                        "Account \"" + newUsername + "\" created with schedule.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        }.execute();
    }
}
