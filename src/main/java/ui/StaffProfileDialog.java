package ui;

import persistence.ProfilePictureRepository;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.text.DefaultFormatter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Modern Staff Profile Dialog with drag-and-drop profile picture support.
 */
public class StaffProfileDialog extends JDialog {

    // ── Palette ──────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = new Color(0xF4F5F7);
    private static final Color BG_CARD = new Color(0xFFFFFF);
    private static final Color BG_INPUT = new Color(0xF7F8FA);
    private static final Color BG_INPUT_FOCUS = new Color(0xEEF3FF);
    private static final Color ACCENT = new Color(0x3B6AE8);
    private static final Color ACCENT_HOVER = new Color(0x2A58D6);
    private static final Color ACCENT_SOFT = new Color(0xE8EDFD);
    private static final Color BORDER_IDLE = new Color(0xDEE2EC);
    private static final Color BORDER_FOCUS = new Color(0x3B6AE8);
    private static final Color TEXT_PRIMARY = new Color(0x1A1F36);
    private static final Color TEXT_SECONDARY = new Color(0x6B7280);
    private static final Color TEXT_MUTED = new Color(0x9CA3AF);
    private static final Color BADGE_ADMIN_BG = new Color(0xEEF3FF);
    private static final Color BADGE_ADMIN_FG = new Color(0x3B6AE8);
    private static final Color BADGE_STAFF_BG = new Color(0xEAF4F0);
    private static final Color BADGE_STAFF_FG = new Color(0x2D7A5F);
    private static final Color DROP_HIGHLIGHT = new Color(0xD0DFFB);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_AVATAR = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_HINT = new Font("Segoe UI", Font.PLAIN, 10);

    // ── Model ─────────────────────────────────────────────────────────────────
    private final String username;
    private final String role;
    private ProfileSaveHandler saveHandler;
    private ProfilePictureRepository pictureRepository;

    // ── Picture state ─────────────────────────────────────────────────────────
    /** Raw bytes of the currently selected picture (null = none / use initials). */
    private byte[] pendingPictureBytes = null;
    /** True when the user has chosen a new picture during this session. */
    private boolean pictureChanged = false;

    // ── Avatar panel reference (so we can repaint on picture change) ──────────
    private AvatarPanel avatarPanel;

    // ── Input components ──────────────────────────────────────────────────────
    private JTextField fullNameField;
    private JSpinner dobSpinner;
    private JTextField emailField;
    private JSpinner employmentStartSpinner;
    private JTextField accountCreatedField;

    // ── Constructor ───────────────────────────────────────────────────────────
    public StaffProfileDialog(Window owner, String username, String role,
            String accountCreated) {
        super(owner, "Staff Profile", ModalityType.APPLICATION_MODAL);
        this.username = username;
        this.role = role;

        setUndecorated(false);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(accountCreated), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setMinimumSize(new Dimension(460, 0));
        setLocationRelativeTo(owner);
    }

    // ── Header card ───────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel card = card();
        card.setLayout(new BorderLayout(14, 0));
        card.setBorder(new EmptyBorder(16, 16, 16, 16));

        avatarPanel = new AvatarPanel();

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(Box.createVerticalGlue());

        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(FONT_TITLE);
        nameLabel.setForeground(TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = roleBadge(role);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(nameLabel);
        info.add(Box.createVerticalStrut(4));
        info.add(badge);
        info.add(Box.createVerticalGlue());

        JPanel outer = new JPanel(new BorderLayout(12, 0));
        outer.setOpaque(false);
        outer.add(avatarPanel, BorderLayout.WEST);
        outer.add(info, BorderLayout.CENTER);

        card.add(outer, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        wrap.setBorder(new EmptyBorder(0, 0, 12, 0));
        return wrap;
    }

    // ── Form card ─────────────────────────────────────────────────────────────
    private JPanel buildForm(String accountCreated) {
        JPanel card = card();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        JLabel section = new JLabel("PERSONAL INFORMATION");
        section.setFont(FONT_SECTION);
        section.setForeground(TEXT_MUTED);
        section.setBorder(new EmptyBorder(0, 0, 14, 0));
        card.add(section, gbc);

        fullNameField = styledTextField("Enter full name");
        dobSpinner = styledDateSpinner();
        emailField = styledTextField("Enter email or contact");
        employmentStartSpinner = styledDateSpinner();
        accountCreatedField = styledTextField("");
        accountCreatedField.setText(accountCreated != null ? accountCreated : "");
        accountCreatedField.setEditable(false);
        accountCreatedField.setForeground(TEXT_MUTED);

        String[] labels = { "Full Name", "Date of Birth",
                "Email / Contact", "Employment Start", "Account Created" };
        Component[] inputs = { fullNameField, dobSpinner,
                emailField, employmentStartSpinner, accountCreatedField };

        for (int i = 0; i < labels.length; i++) {
            gbc.gridy++;
            gbc.insets = new Insets(0, 0, 4, 0);
            JLabel lbl = new JLabel(labels[i]);
            lbl.setFont(FONT_LABEL);
            lbl.setForeground(TEXT_SECONDARY);
            card.add(lbl, gbc);

            gbc.gridy++;
            gbc.insets = new Insets(0, 0, i == labels.length - 1 ? 0 : 14, 0);
            card.add(inputs[i], gbc);
        }

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        wrap.setBorder(new EmptyBorder(0, 0, 12, 0));
        return wrap;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        JButton close = ghostButton("Close");
        JButton save = accentButton("Save");
        close.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());
        row.add(close);
        row.add(save);
        return row;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setSaveHandler(ProfileSaveHandler handler) {
        this.saveHandler = handler;
    }

    /**
     * Wire up a picture repository. When set, the dialog will try to load
     * the existing picture on open and will persist a new picture on save.
     */
    public void setPictureRepository(ProfilePictureRepository repo) {
        this.pictureRepository = repo;
        tryLoadExistingPicture();
    }

    public void populateProfile(String fullName, String dateOfBirth,
            String email, String employmentStart) {
        if (fullName != null && !fullName.isBlank()) {
            fullNameField.setText(fullName);
            fullNameField.setForeground(TEXT_PRIMARY);
        }
        if (email != null && !email.isBlank()) {
            emailField.setText(email);
            emailField.setForeground(TEXT_PRIMARY);
        }
        dobSpinner.setValue(parseDateOrDefault(dateOfBirth));
        employmentStartSpinner.setValue(parseDateOrDefault(employmentStart));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void tryLoadExistingPicture() {
        if (pictureRepository == null)
            return;
        new SwingWorker<byte[], Void>() {
            @Override
            protected byte[] doInBackground() throws Exception {
                return pictureRepository.loadPicture(username);
            }

            @Override
            protected void done() {
                try {
                    byte[] bytes = get();
                    if (bytes != null && bytes.length > 0) {
                        pendingPictureBytes = bytes;
                        pictureChanged = false; // loaded from store, not dirty
                        avatarPanel.repaint();
                    }
                } catch (Exception ignored) {
                }
            }
        }.execute();
    }

    private void onSave() {
        String fullName = resolvedText(fullNameField, "Enter full name");
        String email = resolvedText(emailField, "Enter email or contact");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dobText = sdf.format((Date) dobSpinner.getValue());
        String empStart = sdf.format((Date) employmentStartSpinner.getValue());

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    // Persist picture first if it changed
                    if (pictureChanged && pictureRepository != null
                            && pendingPictureBytes != null) {
                        pictureRepository.savePicture(username, pendingPictureBytes);
                    }
                    if (saveHandler != null) {
                        saveHandler.save(username, fullName, dobText, email, empStart);
                    }
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    showError("Failed to save profile:\n" + error.getMessage());
                } else {
                    pictureChanged = false;
                    JOptionPane.showMessageDialog(StaffProfileDialog.this,
                            "Profile saved successfully.", "Saved",
                            JOptionPane.INFORMATION_MESSAGE);
                    dispose();
                }
            }
        }.execute();
    }

    /** Returns the field text, ignoring placeholder value. */
    private static String resolvedText(JTextField tf, String placeholder) {
        String t = tf.getText().trim();
        return t.equals(placeholder) ? "" : t;
    }

    private static Date parseDateOrDefault(String value) {
        if (value == null || value.isBlank())
            return new Date();
        try {
            return new SimpleDateFormat("yyyy-MM-dd").parse(value);
        } catch (Exception ex) {
            return new Date();
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    // ── AvatarPanel (circular, drag-and-drop, click-to-browse) ───────────────

    /**
     * A 72×72 circular panel that:
     * <ul>
     * <li>Draws the profile picture if one has been loaded/dropped.</li>
     * <li>Falls back to the username-initial avatar otherwise.</li>
     * <li>Accepts image files via drag-and-drop.</li>
     * <li>Opens a file-chooser on click.</li>
     * <li>Shows a subtle hover overlay to invite interaction.</li>
     * </ul>
     */
    private class AvatarPanel extends JPanel {

        private boolean hovering = false;
        private boolean dropTarget = false;

        AvatarPanel() {
            setPreferredSize(new Dimension(72, 72));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click or drag an image to set profile picture");

            // Hover highlight
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    browseForImage();
                }
            });

            // Drag-and-drop
            new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
                @Override
                public void dragEnter(DropTargetDragEvent e) {
                    dropTarget = true;
                    repaint();
                    e.acceptDrag(DnDConstants.ACTION_COPY);
                }

                @Override
                public void dragExit(DropTargetEvent e) {
                    dropTarget = false;
                    repaint();
                }

                @Override
                public void drop(DropTargetDropEvent e) {
                    dropTarget = false;
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        Transferable t = e.getTransferable();
                        if (t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                            @SuppressWarnings("unchecked")
                            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
                            if (!files.isEmpty())
                                loadImageFile(files.get(0));
                        }
                        e.dropComplete(true);
                    } catch (Exception ex) {
                        e.dropComplete(false);
                    }
                    repaint();
                }
            }, true);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int w = getWidth(), h = getHeight();
            Ellipse2D circle = new Ellipse2D.Float(1, 1, w - 2, h - 2);

            // Clip to circle
            g2.setClip(circle);

            if (pendingPictureBytes != null && pendingPictureBytes.length > 0) {
                // Draw the actual picture, scaled to fill
                try {
                    BufferedImage img = ImageIO.read(
                            new ByteArrayInputStream(pendingPictureBytes));
                    if (img != null) {
                        // Cover-fit: scale so the shorter side fills the circle
                        double scale = Math.max((double) w / img.getWidth(),
                                (double) h / img.getHeight());
                        int sw = (int) (img.getWidth() * scale);
                        int sh = (int) (img.getHeight() * scale);
                        int ox = (w - sw) / 2;
                        int oy = (h - sh) / 2;
                        g2.drawImage(img, ox, oy, sw, sh, null);
                    }
                } catch (Exception ignored) {
                    drawInitials(g2, w, h);
                }
            } else {
                drawInitials(g2, w, h);
            }

            // Hover / drop overlay
            if (dropTarget) {
                g2.setColor(new Color(59, 106, 232, 80));
                g2.fill(circle);
            } else if (hovering) {
                g2.setColor(new Color(0, 0, 0, 55));
                g2.fill(circle);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 9));
                FontMetrics fm = g2.getFontMetrics();
                String hint = "Change";
                g2.drawString(hint, (w - fm.stringWidth(hint)) / 2,
                        h / 2 + fm.getAscent() / 2 - 1);
            }

            g2.setClip(null);

            // Border ring
            g2.setColor(dropTarget ? BORDER_FOCUS : BORDER_IDLE);
            g2.setStroke(new BasicStroke(dropTarget ? 2f : 1.5f));
            g2.draw(circle);

            g2.dispose();
        }

        private void drawInitials(Graphics2D g2, int w, int h) {
            g2.setColor(ACCENT_SOFT);
            g2.fillOval(0, 0, w, h);
            g2.setColor(ACCENT);
            g2.setFont(FONT_AVATAR);
            FontMetrics fm = g2.getFontMetrics();
            String letter = username.isEmpty() ? "?"
                    : String.valueOf(Character.toUpperCase(username.charAt(0)));
            g2.drawString(letter,
                    (w - fm.stringWidth(letter)) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
        }
    }

    // ── Image loading helpers ─────────────────────────────────────────────────

    private void browseForImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "png", "jpg", "jpeg", "gif", "bmp", "webp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            loadImageFile(chooser.getSelectedFile());
        }
    }

    private void loadImageFile(File file) {
        try {
            // Validate it's a readable image
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showError("Cannot read image file: " + file.getName());
                return;
            }
            // Re-encode as PNG to normalise the format
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            pendingPictureBytes = baos.toByteArray();
            pictureChanged = true;
            avatarPanel.repaint();
        } catch (Exception ex) {
            showError("Failed to load image:\n" + ex.getMessage());
        }
    }

    // ── Component factories ───────────────────────────────────────────────────

    private static JLabel roleBadge(String role) {
        boolean admin = role.equalsIgnoreCase("admin");
        Color bg = admin ? BADGE_ADMIN_BG : BADGE_STAFF_BG;
        Color fg = admin ? BADGE_ADMIN_FG : BADGE_STAFF_FG;
        JLabel badge = new JLabel(role.toUpperCase()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setForeground(fg);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        badge.setOpaque(false);
        return badge;
    }

    private static JPanel card() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 14, 14));
                g2.setColor(BORDER_IDLE);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(new RoundRectangle2D.Float(0.5f, 0.5f, getWidth() - 1, getHeight() - 1, 14, 14));
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
    }

    private static JTextField styledTextField(String placeholder) {
        JTextField tf = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean focused = isFocusOwner();
                g2.setColor(focused ? BG_INPUT_FOCUS : BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(focused ? BORDER_FOCUS : BORDER_IDLE);
                g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(9, 12, 9, 12));
        tf.setFont(FONT_INPUT);
        tf.setPreferredSize(new Dimension(0, 38));

        if (!placeholder.isEmpty()) {
            tf.setText(placeholder);
            tf.setForeground(TEXT_MUTED);
            tf.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (tf.getText().equals(placeholder)) {
                        tf.setText("");
                        tf.setForeground(TEXT_PRIMARY);
                    }
                    tf.repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (tf.getText().isEmpty()) {
                        tf.setText(placeholder);
                        tf.setForeground(TEXT_MUTED);
                    }
                    tf.repaint();
                }
            });
        } else {
            tf.setForeground(TEXT_PRIMARY);
        }
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                tf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                tf.repaint();
            }
        });
        return tf;
    }

    private static JSpinner styledDateSpinner() {
        SpinnerDateModel model = new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH);
        JSpinner spinner = new JSpinner(model) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_INPUT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_IDLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(0, 38));
        spinner.setBorder(BorderFactory.createEmptyBorder());
        spinner.setOpaque(false);

        JFormattedTextField tf = editor.getTextField();
        tf.setFont(FONT_INPUT);
        tf.setForeground(TEXT_PRIMARY);
        tf.setBackground(new Color(0, 0, 0, 0));
        tf.setOpaque(false);
        tf.setBorder(new EmptyBorder(0, 12, 0, 4));
        tf.setHorizontalAlignment(SwingConstants.LEFT);
        if (tf.getFormatter() instanceof DefaultFormatter)
            ((DefaultFormatter) tf.getFormatter()).setAllowsInvalid(false);

        for (Component c : spinner.getComponents()) {
            if (c instanceof JPanel)
                for (Component btn : ((JPanel) c).getComponents())
                    if (btn instanceof JButton) {
                        ((JButton) btn).setBackground(BG_INPUT);
                        ((JButton) btn).setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
                        ((JButton) btn).setFocusPainted(false);
                        ((JButton) btn).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    }
        }
        tf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                spinner.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                spinner.repaint();
            }
        });
        return spinner;
    }

    private static JButton accentButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? ACCENT_HOVER : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(Color.WHITE);
                g2.setFont(FONT_BUTTON);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 22, 8, 22));
        btn.setPreferredSize(new Dimension(90, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    private static JButton ghostButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hover = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hover = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hover ? new Color(0xF3F4F6) : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_IDLE);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(TEXT_PRIMARY);
                g2.setFont(FONT_BUTTON);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }

            @Override
            public boolean isOpaque() {
                return false;
            }
        };
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
        btn.setPreferredSize(new Dimension(80, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Functional interface ──────────────────────────────────────────────────

    @FunctionalInterface
    public interface ProfileSaveHandler {
        void save(String username, String fullName, String dateOfBirth,
                String email, String employmentStart) throws Exception;
    }
}