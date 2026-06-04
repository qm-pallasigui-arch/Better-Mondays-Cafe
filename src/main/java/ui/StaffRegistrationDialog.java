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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class StaffRegistrationDialog extends JDialog {

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
    private static final Color BADGE_STAFF_BG = new Color(0xEAF4F0);
    private static final Color BADGE_STAFF_FG = new Color(0x2D7A5F);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 20);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_AVATAR = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);

    // ── Input components ──────────────────────────────────────────────────────
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField fullNameField;
    private JTextField emailField;
    private JSpinner dobSpinner;
    private JSpinner employmentStartSpinner;

    // ── Picture state ─────────────────────────────────────────────────────────
    private byte[] pendingPictureBytes = null;
    private AvatarDropPanel avatarDropPanel;

    // ── Handlers ─────────────────────────────────────────────────────────────
    private RegistrationSaveHandler saveHandler;
    private ProfilePictureRepository pictureRepository;
    private Runnable saveSuccessListener;

    // ── Constructor ───────────────────────────────────────────────────────────
    public StaffRegistrationDialog(Window owner) {
        super(owner, "Register New Staff", ModalityType.APPLICATION_MODAL);
        setUndecorated(false);
        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildForm(), BorderLayout.CENTER);
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

        avatarDropPanel = new AvatarDropPanel();

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(Box.createVerticalGlue());

        JLabel title = new JLabel("Register New Staff");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel badge = new JLabel("STAFF") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BADGE_STAFF_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        badge.setFont(FONT_BADGE);
        badge.setForeground(BADGE_STAFF_FG);
        badge.setBorder(new EmptyBorder(3, 8, 3, 8));
        badge.setOpaque(false);
        badge.setAlignmentX(Component.LEFT_ALIGNMENT);

        info.add(title);
        info.add(Box.createVerticalStrut(4));
        info.add(badge);
        info.add(Box.createVerticalGlue());

        JPanel outer = new JPanel(new BorderLayout(12, 0));
        outer.setOpaque(false);
        outer.add(avatarDropPanel, BorderLayout.WEST);
        outer.add(info, BorderLayout.CENTER);

        card.add(outer, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        wrap.setBorder(new EmptyBorder(0, 0, 12, 0));
        return wrap;
    }

    // ── Form card ─────────────────────────────────────────────────────────────
    private JPanel buildForm() {
        JPanel card = card();
        card.setLayout(new GridBagLayout());
        card.setBorder(new EmptyBorder(20, 20, 20, 20));

        usernameField = styledTextField("Enter username");
        passwordField = styledPasswordField();
        fullNameField = styledTextField("Enter full name");
        emailField = styledTextField("Enter email or contact");
        dobSpinner = styledDateSpinner();
        employmentStartSpinner = styledDateSpinner();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Section: Account
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(sectionLabel("ACCOUNT CREDENTIALS"), gbc);
        addFieldRow(card, gbc, "Username *", usernameField, false);
        addFieldRow(card, gbc, "Password *", passwordField, false);

        // Divider
        gbc.gridy++;
        gbc.insets = new Insets(6, 0, 14, 0);
        card.add(divider(), gbc);

        // Section: Personal
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        card.add(sectionLabel("PERSONAL INFORMATION"), gbc);
        addFieldRow(card, gbc, "Full Name", fullNameField, false);
        addFieldRow(card, gbc, "Email / Contact", emailField, false);
        addFieldRow(card, gbc, "Date of Birth", dobSpinner, false);
        addFieldRow(card, gbc, "Employment Start", employmentStartSpinner, true);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        wrap.setBorder(new EmptyBorder(0, 0, 12, 0));
        return wrap;
    }

    private void addFieldRow(JPanel card, GridBagConstraints gbc,
            String labelText, JComponent field, boolean last) {
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SECONDARY);
        card.add(lbl, gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, last ? 0 : 14, 0);
        card.add(field, gbc);
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        row.setOpaque(false);
        JButton cancel = ghostButton("Cancel");
        JButton register = accentButton("Register");
        cancel.addActionListener(e -> dispose());
        register.addActionListener(e -> onSave());
        row.add(cancel);
        row.add(register);
        return row;
    }

    // ── Save logic ────────────────────────────────────────────────────────────
    private void onSave() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();
        String fullName = resolvedText(fullNameField, "Enter full name");
        String email = resolvedText(emailField, "Enter email or contact");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String dobText = sdf.format((Date) dobSpinner.getValue());
        String empStart = sdf.format((Date) employmentStartSpinner.getValue());

        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            usernameField.requestFocusInWindow();
            return;
        }
        if (password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.", "Validation", JOptionPane.WARNING_MESSAGE);
            passwordField.requestFocusInWindow();
            return;
        }
        if (saveHandler == null) {
            JOptionPane.showMessageDialog(this, "Save handler is not configured.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        new SwingWorker<Void, Void>() {
            private Exception error;

            @Override
            protected Void doInBackground() {
                try {
                    saveHandler.save(username, password, fullName, email, dobText, empStart);
                    // Persist picture after account creation
                    if (pendingPictureBytes != null && pictureRepository != null) {
                        pictureRepository.savePicture(username, pendingPictureBytes);
                    }
                } catch (Exception ex) {
                    error = ex;
                }
                return null;
            }

            @Override
            protected void done() {
                if (error != null) {
                    JOptionPane.showMessageDialog(StaffRegistrationDialog.this,
                            "Failed to register staff:\n" + error.getMessage(),
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                JOptionPane.showMessageDialog(StaffRegistrationDialog.this,
                        "Staff account '" + username + "' created successfully.",
                        "Success", JOptionPane.INFORMATION_MESSAGE);
                if (saveSuccessListener != null)
                    saveSuccessListener.run();
                dispose();
            }
        }.execute();
    }

    // ── Public setters ────────────────────────────────────────────────────────
    public void setSaveHandler(RegistrationSaveHandler handler) {
        this.saveHandler = handler;
    }

    public void setSaveSuccessListener(Runnable listener) {
        this.saveSuccessListener = listener;
    }

    public void setPictureRepository(ProfilePictureRepository repo) {
        this.pictureRepository = repo;
    }

    // ── AvatarDropPanel ───────────────────────────────────────────────────────

    /**
     * Circular 72×72 drop zone. Shows "+" initially; replaced by the photo
     * once the user drops or browses an image.
     */
    private class AvatarDropPanel extends JPanel {
        private boolean hovering = false;
        private boolean dropTarget = false;

        AvatarDropPanel() {
            setPreferredSize(new Dimension(72, 72));
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText("Click or drag an image to set a profile picture (optional)");

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

            int w = getWidth(), h = getHeight();
            Ellipse2D circle = new Ellipse2D.Float(1, 1, w - 2, h - 2);
            g2.setClip(circle);

            if (pendingPictureBytes != null && pendingPictureBytes.length > 0) {
                try {
                    BufferedImage img = ImageIO.read(
                            new java.io.ByteArrayInputStream(pendingPictureBytes));
                    if (img != null) {
                        double scale = Math.max((double) w / img.getWidth(),
                                (double) h / img.getHeight());
                        int sw = (int) (img.getWidth() * scale);
                        int sh = (int) (img.getHeight() * scale);
                        g2.drawImage(img, (w - sw) / 2, (h - sh) / 2, sw, sh, null);
                    } else {
                        drawPlaceholder(g2, w, h);
                    }
                } catch (Exception ex) {
                    drawPlaceholder(g2, w, h);
                }
            } else {
                drawPlaceholder(g2, w, h);
            }

            if (dropTarget) {
                g2.setColor(new Color(59, 106, 232, 90));
                g2.fill(circle);
            } else if (hovering && pendingPictureBytes != null) {
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
            g2.setColor(dropTarget ? BORDER_FOCUS : BORDER_IDLE);
            g2.setStroke(new BasicStroke(dropTarget ? 2f : 1.5f));
            g2.draw(circle);
            g2.dispose();
        }

        private void drawPlaceholder(Graphics2D g2, int w, int h) {
            g2.setColor(ACCENT_SOFT);
            g2.fillOval(0, 0, w, h);
            g2.setColor(ACCENT);
            g2.setFont(FONT_AVATAR);
            FontMetrics fm = g2.getFontMetrics();
            String plus = "+";
            g2.drawString(plus, (w - fm.stringWidth(plus)) / 2,
                    (h - fm.getHeight()) / 2 + fm.getAscent());
            if (!hovering) {
                // small "Photo" hint
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 8));
                fm = g2.getFontMetrics();
                String hint = "photo";
                g2.setColor(new Color(0x3B6AE8));
                g2.drawString(hint, (w - fm.stringWidth(hint)) / 2,
                        h / 2 + fm.getAscent() + 6);
            }
        }
    }

    // ── Image loading helpers ─────────────────────────────────────────────────

    private void browseForImage() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Profile Picture");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter(
                "Image files", "png", "jpg", "jpeg", "gif", "bmp"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
            loadImageFile(chooser.getSelectedFile());
    }

    private void loadImageFile(File file) {
        try {
            BufferedImage img = ImageIO.read(file);
            if (img == null) {
                showError("Cannot read image: " + file.getName());
                return;
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            pendingPictureBytes = baos.toByteArray();
            avatarDropPanel.repaint();
        } catch (Exception ex) {
            showError("Failed to load image:\n" + ex.getMessage());
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    private static String resolvedText(JTextField tf, String placeholder) {
        String t = tf.getText().trim();
        return t.equals(placeholder) ? "" : t;
    }

    // ── Component factories ───────────────────────────────────────────────────

    private static JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_SECTION);
        lbl.setForeground(TEXT_MUTED);
        return lbl;
    }

    private static JSeparator divider() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_IDLE);
        sep.setBackground(BORDER_IDLE);
        return sep;
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

    private static JPasswordField styledPasswordField() {
        JPasswordField pf = new JPasswordField() {
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
        pf.setOpaque(false);
        pf.setBorder(new EmptyBorder(9, 12, 9, 12));
        pf.setFont(FONT_INPUT);
        pf.setForeground(TEXT_PRIMARY);
        pf.setPreferredSize(new Dimension(0, 38));
        pf.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                pf.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                pf.repaint();
            }
        });
        return pf;
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
        btn.setPreferredSize(new Dimension(110, 36));
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
        btn.setPreferredSize(new Dimension(110, 36));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ── Functional interface ──────────────────────────────────────────────────
    @FunctionalInterface
    public interface RegistrationSaveHandler {
        void save(String username, String password, String fullName, String email,
                String dateOfBirth, String employmentStart) throws Exception;
    }
}