package ui;

import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.ProfilePictureRepository;
import persistence.sqlite.SQLiteStaffScheduleRepository;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class StaffRegistrationDialog extends JDialog {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;

    private final AccountRoleRepository accountRoleRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final Runnable onCreated;
    private final StaffRegistrationView view = new StaffRegistrationView();

    private JTextField fullNameField;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmField;
    private JTextField birthdateField;
    private LocalDate selectedDate;
    private JTextField contactField;
    private JTextField addressField;
    private JComboBox<String> genderBox;
    private JButton saveButton;
    private PictureDropPanel pictureDropPanel;
    private byte[] selectedPictureBytes;

    public StaffRegistrationDialog(Window owner, AccountRoleRepository accountRoleRepository,
            ProfilePictureRepository profilePictureRepository, Runnable onCreated) {
        super(owner, "Staff Registration", ModalityType.APPLICATION_MODAL);
        this.accountRoleRepository = accountRoleRepository;
        this.profilePictureRepository = profilePictureRepository;
        this.onCreated = onCreated;

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(view.root());
        pack();
        setSize(new Dimension(650, Math.min(getHeight(), 780)));
        setMinimumSize(new Dimension(650, Math.min(getHeight(), 780)));
        setLocationRelativeTo(owner);
    }

    private final class StaffRegistrationView extends StaffProfileDialog.StaffProfileForm {

        StaffRegistrationView() {
            super("ACCOUNT INFORMATION");
        }

        JPanel root() {
            JPanel root = createRoot();
            pictureDropPanel = new PictureDropPanel();
            root.add(createSummaryHeader("Staff Registration", "Create staff access and profile details",
                    pictureDropPanel,
                    "New Staff",
                    "STAFF",
                    new String[][] {
                            { "Account", "New profile" },
                            { "Photo", "Click or drag image" }
                    }), BorderLayout.NORTH);

            JPanel formCard = createFormCard();
            fullNameField = addTextField(formCard, "Full Name", "");
            usernameField = addTextField(formCard, "Username", "");
            passwordField = addPasswordField(formCard, "Password");
            confirmField = addPasswordField(formCard, "Confirm Password");

            birthdateField = new JTextField();
            birthdateField.setFont(VALUE_FONT);
            birthdateField.setForeground(TEXT_SECONDARY);
            birthdateField.setBackground(BG_INPUT);
            birthdateField.setPreferredSize(new Dimension(0, 40));
            birthdateField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(9, 12, 9, 12)));
            birthdateField.setEditable(false);
            birthdateField.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            birthdateField.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    new DatePickerDialog(StaffRegistrationDialog.this, birthdateField, selectedDate).setVisible(true);
                }
            });
            addCustomInput(formCard, "Date of Birth", birthdateField);

            contactField = addTextField(formCard, "Email / Contact", "");
            addressField = addTextField(formCard, "Address", "");
            genderBox = addComboBox(formCard, "Gender", new String[] { "Male", "Female", "Other" });

            JButton close = secondaryButton("Close");
            close.addActionListener(e -> dispose());
            saveButton = primaryButton("Proceed for Schedule");
            saveButton.addActionListener(e -> onSave());

            root.add(wrapFormCard(formCard), BorderLayout.CENTER);
            root.add(createFooter(close, saveButton), BorderLayout.SOUTH);
            return root;
        }
    }

    private void onSave() {
        String fullName = fullNameField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String confirm = new String(confirmField.getPassword()).trim();
        LocalDate birthdate = selectedBirthdate();
        String contact = contactField.getText().trim();
        String address = addressField.getText().trim();
        String gender = (String) genderBox.getSelectedItem();

        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty()) {
            showValidation("Full name, username, and password are required.");
            return;
        }
        if (!password.equals(confirm)) {
            showValidation("Passwords do not match.");
            return;
        }
        if (!isStrongPassword(password)) {
            showValidation("Password must be at least 8 characters and include a number and a special character.");
            return;
        }
        if (birthdate == null) {
            showValidation("Please select a date of birth.");
            return;
        }
        if (!birthdate.isBefore(LocalDate.now())) {
            showValidation("Age Restriction: Date of birth must be before today.");
            return;
        }

        int age = Period.between(birthdate, LocalDate.now()).getYears();
        if (age < 15) {
            showValidation("Age Restriction: Staff must be at least 15 years old.");
            return;
        }
        String birthdateText = birthdate.format(DATE_FORMAT);

        // Open schedule dialog — actual save happens there
        StaffScheduleDialog dialog = new StaffScheduleDialog(
                this,
                accountRoleRepository,
                profilePictureRepository,
                new SQLiteStaffScheduleRepository(),
                username, password, fullName, age, birthdateText,
                address, contact, gender,
                selectedPictureBytes,
                onCreated);
        dialog.setVisible(true);
        dispose();
    }

    private LocalDate selectedBirthdate() {
        return selectedDate;
    }

    private boolean isStrongPassword(String password) {
        return password.length() >= 8
                && password.matches(".*\\d.*")
                && password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");
    }

    private void showValidation(String message) {
        JOptionPane.showMessageDialog(this, message, "Validation", JOptionPane.WARNING_MESSAGE);
    }

    private void choosePicture() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Profile Picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "png", "jpg", "jpeg", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadPictureFile(chooser.getSelectedFile());
        }
    }

    private void loadPictureFile(File file) {
        if (file == null) {
            return;
        }
        String name = file.getName().toLowerCase();
        if (!(name.endsWith(".png") || name.endsWith(".jpg")
                || name.endsWith(".jpeg") || name.endsWith(".gif"))) {
            showValidation("Profile picture must be a PNG, JPG, JPEG, or GIF file.");
            return;
        }
        if (file.length() > MAX_IMAGE_BYTES) {
            showValidation("Profile picture must be 5 MB or smaller.");
            return;
        }
        try {
            byte[] imageBytes = Files.readAllBytes(file.toPath());
            if (ImageIO.read(file) == null) {
                showValidation("Selected file is not a readable image.");
                return;
            }
            selectedPictureBytes = imageBytes;
            pictureDropPanel.setImage(imageBytes, file.getName());
        } catch (IOException ex) {
            showValidation("Unable to load profile picture:\n" + ex.getMessage());
        }
    }

    // ─── Date Picker Dialog ──────────────────────────────────────
    private class DatePickerDialog extends JDialog {
        private final JTextField targetField;
        private YearMonth currentMonth;
        private LocalDate pickedDate;
        private LocalDate hoveredDate;
        private final JLabel monthLabel = new JLabel("", SwingConstants.CENTER);
        private final JPanel gridPanel;
        private final JLabel[] dayLabels = new JLabel[42];
        private static final Color BLUE = new Color(0x2563EB);
        private static final Color BLUE_HOVER = new Color(0x1D4ED8);
        private static final Color GRAY_MUTED = new Color(0x9CA3AF);
        private static final Color GRAY_BG = new Color(0xF3F4F6);
        private static final Color GRID_BORDER = new Color(0xE5E7EB);

        DatePickerDialog(JDialog parent, JTextField field, LocalDate initial) {
            super(parent, "Select Date", ModalityType.APPLICATION_MODAL);
            this.targetField = field;
            this.currentMonth = initial != null ? YearMonth.from(initial) : YearMonth.now();
            this.pickedDate = initial;

            setResizable(false);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);

            JPanel root = new JPanel(new BorderLayout(0, 0));
            root.setBackground(Color.WHITE);
            root.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));

            root.add(buildHeader(), BorderLayout.NORTH);
            gridPanel = new JPanel(new GridLayout(0, 7, 0, 0));
            gridPanel.setBackground(Color.WHITE);
            root.add(gridPanel, BorderLayout.CENTER);
            root.add(buildFooter(), BorderLayout.SOUTH);

            setContentPane(root);
            renderGrid();
            pack();
            setLocationRelativeTo(parent);
        }

        private JPanel buildHeader() {
            JPanel header = new JPanel(new BorderLayout(8, 0));
            header.setBackground(Color.WHITE);
            header.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

            JPanel nav = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
            nav.setOpaque(false);

            nav.add(navButton("<<", e -> { currentMonth = currentMonth.minusYears(1); renderGrid(); }));
            nav.add(navButton("<", e -> { currentMonth = currentMonth.minusMonths(1); renderGrid(); }));
            monthLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
            monthLabel.setForeground(new Color(0x0F172A));
            monthLabel.setPreferredSize(new Dimension(160, 30));
            nav.add(monthLabel);
            nav.add(navButton(">", e -> { currentMonth = currentMonth.plusMonths(1); renderGrid(); }));
            nav.add(navButton(">>", e -> { currentMonth = currentMonth.plusYears(1); renderGrid(); }));

            header.add(nav, BorderLayout.CENTER);
            return header;
        }

        private JButton navButton(String text, java.awt.event.ActionListener listener) {
            JButton btn = new JButton(text);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
            btn.setForeground(new Color(0x475569));
            btn.setBackground(Color.WHITE);
            btn.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(listener);
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { btn.setForeground(BLUE); }
                @Override public void mouseExited(MouseEvent e) { btn.setForeground(new Color(0x475569)); }
            });
            return btn;
        }

        private JPanel buildFooter() {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
            footer.setBackground(Color.WHITE);
            footer.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));

            JButton cancel = new JButton("Cancel");
            cancel.setFont(new Font("Segoe UI", Font.BOLD, 12));
            cancel.setForeground(new Color(0x0F172A));
            cancel.setBackground(Color.WHITE);
            cancel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(GRID_BORDER),
                    new EmptyBorder(7, 18, 7, 18)));
            cancel.setFocusPainted(false);
            cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cancel.addActionListener(e -> dispose());

            JButton apply = new JButton("Apply");
            apply.setFont(new Font("Segoe UI", Font.BOLD, 12));
            apply.setForeground(Color.WHITE);
            apply.setBackground(new Color(0x0F172A));
            apply.setBorder(BorderFactory.createEmptyBorder(8, 22, 8, 22));
            apply.setFocusPainted(false);
            apply.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            apply.addActionListener(e -> {
                if (pickedDate != null) {
                    targetField.setText(pickedDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                    selectedDate = pickedDate;
                }
                dispose();
            });
            apply.addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { apply.setBackground(new Color(0x1E293B)); }
                @Override public void mouseExited(MouseEvent e) { apply.setBackground(new Color(0x0F172A)); }
            });

            footer.add(cancel);
            footer.add(apply);
            return footer;
        }

        private void renderGrid() {
            gridPanel.removeAll();
            monthLabel.setText(currentMonth.getMonth().getDisplayName(
                    java.time.format.TextStyle.SHORT, java.util.Locale.ENGLISH)
                    + " " + currentMonth.getYear());

            String[] dayNames = { "Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun" };
            for (String d : dayNames) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
                lbl.setForeground(GRAY_MUTED);
                lbl.setPreferredSize(new Dimension(36, 28));
                gridPanel.add(lbl);
            }

            LocalDate firstOfMonth = currentMonth.atDay(1);
            int startDow = firstOfMonth.getDayOfWeek().getValue() - 1; // Mon=0 … Sun=6
            int daysInMonth = currentMonth.lengthOfMonth();
            LocalDate today = LocalDate.now();

            for (int i = 0; i < 42; i++) {
                final int cellIndex = i;
                JLabel lbl = new JLabel("", SwingConstants.CENTER);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lbl.setPreferredSize(new Dimension(36, 28));
                lbl.setOpaque(true);
                lbl.setBackground(Color.WHITE);
                lbl.setForeground(new Color(0x0F172A));

                int dayNum;
                boolean isCurrentMonth;
                if (i < startDow) {
                    int prevDays = YearMonth.from(currentMonth.minusMonths(1)).lengthOfMonth();
                    dayNum = prevDays - startDow + i + 1;
                    isCurrentMonth = false;
                } else if (i >= startDow + daysInMonth) {
                    dayNum = i - startDow - daysInMonth + 1;
                    isCurrentMonth = false;
                } else {
                    dayNum = i - startDow + 1;
                    isCurrentMonth = true;
                }

                LocalDate cellDate = isCurrentMonth
                        ? currentMonth.atDay(dayNum)
                        : (i < startDow
                                ? currentMonth.minusMonths(1).atDay(dayNum)
                                : currentMonth.plusMonths(1).atDay(dayNum));

                lbl.setText(String.valueOf(dayNum));
                lbl.setForeground(isCurrentMonth ? new Color(0x0F172A) : GRAY_MUTED);

                boolean isDisabled = !cellDate.isBefore(today);

                if (pickedDate != null && pickedDate.equals(cellDate)) {
                    lbl.setBackground(BLUE);
                    lbl.setForeground(Color.WHITE);
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                } else if (today.equals(cellDate)) {
                    lbl.setBackground(GRAY_BG);
                }

                if (!isDisabled) {
                    lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }
                lbl.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        if (isDisabled) return;
                        pickedDate = cellDate;
                        renderGrid();
                    }
                    @Override public void mouseEntered(MouseEvent e) {
                        if (isDisabled) return;
                        if (pickedDate == null || !pickedDate.equals(cellDate)) {
                            lbl.setBackground(new Color(0xE5E7EB));
                        }
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        if (isDisabled) return;
                        if (pickedDate != null && pickedDate.equals(cellDate)) {
                            lbl.setBackground(BLUE);
                        } else if (today.equals(cellDate)) {
                            lbl.setBackground(GRAY_BG);
                        } else {
                            lbl.setBackground(Color.WHITE);
                        }
                    }
                });

                gridPanel.add(lbl);
                dayLabels[i] = lbl;
            }

            gridPanel.revalidate();
            gridPanel.repaint();
        }
    }

    private final class PictureDropPanel extends JPanel {
        private Image image;
        private String fileName = "Add photo";

        PictureDropPanel() {
            setPreferredSize(new Dimension(112, 112));
            setMinimumSize(new Dimension(112, 112));
            setMaximumSize(new Dimension(112, 112));
            setBackground(new Color(0xDBEAFE));
            setBorder(new EmptyBorder(0, 0, 0, 0));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    choosePicture();
                }
            });
            setTransferHandler(new TransferHandler() {
                @Override
                public boolean canImport(TransferSupport support) {
                    return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
                }

                @Override
                public boolean importData(TransferSupport support) {
                    if (!canImport(support)) {
                        return false;
                    }
                    try {
                        @SuppressWarnings("unchecked")
                        List<File> files = (List<File>) support.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty()) {
                            loadPictureFile(files.get(0));
                            return true;
                        }
                    } catch (Exception ex) {
                        showValidation("Unable to load dropped image:\n" + ex.getMessage());
                    }
                    return false;
                }
            });
        }

        void setImage(byte[] imageBytes, String fileName) throws IOException {
            ImageIcon icon = new ImageIcon(imageBytes);
            this.image = icon.getImage();
            this.fileName = fileName;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            RoundRectangle2D.Float shape = new RoundRectangle2D.Float(
                    0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
            g2.setColor(new Color(0xDBEAFE));
            g2.fill(shape);
            if (image != null) {
                Shape oldClip = g2.getClip();
                g2.setClip(shape);
                drawImageCover(g2, image, getWidth(), getHeight());
                g2.setClip(oldClip);
            } else {
                g2.setColor(new Color(0x2563EB));
                g2.setFont(new Font("Segoe UI", Font.BOLD, 30));
                String plus = "+";
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(plus, (getWidth() - fm.stringWidth(plus)) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent() - 12);

                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                fm = g2.getFontMetrics();
                g2.drawString(fileName, (getWidth() - fm.stringWidth(fileName)) / 2, getHeight() - 28);
            }
            g2.setColor(new Color(0xE2E8F0));
            g2.draw(shape);
            g2.dispose();
        }

        private void drawImageCover(Graphics2D g2, Image image, int targetWidth, int targetHeight) {
            int imageWidth = image.getWidth(null);
            int imageHeight = image.getHeight(null);
            if (imageWidth <= 0 || imageHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
                return;
            }

            double scale = Math.max(
                    targetWidth / (double) imageWidth,
                    targetHeight / (double) imageHeight);
            int drawWidth = (int) Math.round(imageWidth * scale);
            int drawHeight = (int) Math.round(imageHeight * scale);
            int drawX = (targetWidth - drawWidth) / 2;
            int drawY = (targetHeight - drawHeight) / 2;

            Object oldInterpolation = g2.getRenderingHint(RenderingHints.KEY_INTERPOLATION);
            Object oldRender = g2.getRenderingHint(RenderingHints.KEY_RENDERING);
            Object oldAlpha = g2.getRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, oldRender);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlpha);
        }
    }
}
