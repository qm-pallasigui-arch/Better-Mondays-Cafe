package ui;

import loginregister.UserDataManager.Role;
import persistence.AccountRoleRepository;
import persistence.ProfilePictureRepository;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
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
    private JSpinner birthdateSpinner;
    private JTextField contactField;
    private JTextField addressField;
    private JComboBox<String> genderBox;
    private JComboBox<String> roleBox;
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
            birthdateSpinner = addDateSpinner(formCard, "Date of Birth");
            contactField = addTextField(formCard, "Email / Contact", "");
            addressField = addTextField(formCard, "Address", "");
            genderBox = addComboBox(formCard, "Gender", new String[] { "Male", "Female", "Other" });
            roleBox = addComboBox(formCard, "Role", new String[] { "STAFF", "ADMIN" });

            JButton close = secondaryButton("Close");
            close.addActionListener(e -> dispose());
            saveButton = primaryButton("Create Staff");
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
        Role role = Role.valueOf((String) roleBox.getSelectedItem());

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
        if (birthdate.isAfter(LocalDate.now())) {
            showValidation("Date of birth cannot be in the future.");
            return;
        }

        int age = Period.between(birthdate, LocalDate.now()).getYears();
        String birthdateText = birthdate.format(DATE_FORMAT);

        saveButton.setEnabled(false);
        new SwingWorker<Void, Void>() {
            private String errorMessage;
            private boolean pictureSaveFailed;

            @Override
            protected Void doInBackground() {
                try {
                    accountRoleRepository.createUser(username, password, role,
                            fullName, age, birthdateText, address, contact, gender);
                } catch (Exception ex) {
                    errorMessage = ex.getMessage();
                    return null;
                }

                if (selectedPictureBytes != null && selectedPictureBytes.length > 0) {
                    try {
                        profilePictureRepository.savePicture(username, selectedPictureBytes);
                    } catch (Exception ex) {
                        pictureSaveFailed = true;
                    }
                }
                return null;
            }

            @Override
            protected void done() {
                saveButton.setEnabled(true);
                if (errorMessage != null) {
                    JOptionPane.showMessageDialog(StaffRegistrationDialog.this,
                            "Failed to create account:\n" + errorMessage,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (onCreated != null) {
                    onCreated.run();
                }
                if (pictureSaveFailed) {
                    JOptionPane.showMessageDialog(StaffRegistrationDialog.this,
                            "Account was created, but the profile picture could not be saved.",
                            "Picture Not Saved", JOptionPane.WARNING_MESSAGE);
                }
                JOptionPane.showMessageDialog(StaffRegistrationDialog.this,
                        "Account \"" + username + "\" created successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            }
        }.execute();
    }

    private LocalDate selectedBirthdate() {
        Date date = (Date) birthdateSpinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
