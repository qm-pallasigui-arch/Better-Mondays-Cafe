package ui;

import loginregister.UserAccount;
import persistence.AccountRoleRepository;
import persistence.ProfilePictureRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

public class StaffProfileDialog extends JDialog {

    private final StaffDialogView view;
    private final UserAccount account;
    private final AccountRoleRepository accountRoleRepository;
    private final ProfilePictureRepository profilePictureRepository;
    private final String currentUsername;
    private final Runnable onDeleted;

    public StaffProfileDialog(Window owner, UserAccount account,
            AccountRoleRepository accountRoleRepository,
            ProfilePictureRepository profilePictureRepository,
            String currentUsername,
            Runnable onDeleted) {
        super(owner, "Staff Profile", ModalityType.APPLICATION_MODAL);
        this.account = account;
        this.accountRoleRepository = accountRoleRepository;
        this.profilePictureRepository = profilePictureRepository;
        this.currentUsername = currentUsername;
        this.onDeleted = onDeleted;
        this.view = new StaffDialogView(account);

        setResizable(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(fittedContent(view.root()));
        pack();
        setSize(new Dimension(650, Math.min(getHeight(), 760)));
        setMinimumSize(new Dimension(650, Math.min(getHeight(), 760)));
        revalidate();
        repaint();
        setLocationRelativeTo(owner);
    }

    public StaffProfileDialog(Window owner, UserAccount account) {
        this(owner, account, null, null, "", null);
    }

    private JScrollPane fittedContent(JPanel root) {
        JScrollPane scrollPane = new JScrollPane(root);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getViewport().setBackground(new Color(0xF3F6FA));
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        return scrollPane;
    }

    private final class StaffDialogView extends StaffProfileForm {
        private final UserAccount account;
        private final byte[] pictureBytes;

        StaffDialogView(UserAccount account) {
            super("Personal Details");
            this.account = account;
            this.pictureBytes = loadPicture(account.getUsername());
        }

        JPanel root() {
            JPanel root = new JPanel(new BorderLayout(0, 16));
            root.setOpaque(true);
            root.setBackground(new Color(0xF3F6FA));
            root.setBorder(new EmptyBorder(22, 22, 22, 22));

            root.add(profileHeader(), BorderLayout.NORTH);
            root.add(detailsCard(), BorderLayout.CENTER);
            root.add(canDeleteAccount()
                    ? footer(deleteButton(), closeButton())
                    : footer(closeButton()), BorderLayout.SOUTH);
            return root;
        }

        private JPanel profileHeader() {
            JPanel card = plainCard(new BorderLayout(18, 0));

            JComponent avatar = avatarSquare(firstInitial(), pictureBytes);
            card.add(avatar, BorderLayout.WEST);

            JPanel text = new JPanel();
            text.setOpaque(false);
            text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

            JLabel eyebrow = new JLabel("STAFF PROFILE");
            eyebrow.setFont(new Font("Segoe UI", Font.BOLD, 11));
            eyebrow.setForeground(new Color(0x64748B));

            JLabel name = new JLabel(displayName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 26));
            name.setForeground(new Color(0x0F172A));

            JLabel username = new JLabel("@" + safe(account.getUsername()));
            username.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            username.setForeground(new Color(0x475569));

            JLabel badge = new JLabel(roleText().isBlank() ? "STAFF" : roleText());
            badge.setOpaque(true);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
            badge.setForeground(new Color(0x1D4ED8));
            badge.setBackground(new Color(0xDBEAFE));
            badge.setBorder(new EmptyBorder(5, 10, 5, 10));

            text.add(eyebrow);
            text.add(Box.createVerticalStrut(7));
            text.add(name);
            text.add(Box.createVerticalStrut(5));
            text.add(username);
            text.add(Box.createVerticalStrut(12));
            text.add(badge);

            card.add(text, BorderLayout.CENTER);
            return card;
        }

        private JPanel detailsCard() {
            JPanel card = plainCard(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 16, 0);

            JLabel title = new JLabel("Personal Details");
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setForeground(new Color(0x0F172A));
            card.add(title, gbc);

            addDetail(card, "Full Name", account.getFullName(), 0, 1);
            addDetail(card, "Date of Birth", account.getBirthdate(), 1, 1);
            addDetail(card, "Email / Contact", account.getMobile(), 0, 2);
            addDetail(card, "Gender", account.getGender(), 1, 2);
            addDetail(card, "Age", account.getAge() > 0 ? String.valueOf(account.getAge()) : "", 0, 3);
            addDetail(card, "Address", account.getAddress(), 1, 3);
            addDetail(card, "Account Created", account.getCreatedAt(), 0, 4);
            return card;
        }

        private JPanel plainCard(LayoutManager layout) {
            JPanel card = new JPanel(layout);
            card.setOpaque(true);
            card.setBackground(Color.WHITE);
            card.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                    new EmptyBorder(20, 20, 20, 20)));
            return card;
        }

        private void addDetail(JPanel parent, String label, String value, int x, int y) {
            JPanel group = new JPanel();
            group.setOpaque(false);
            group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

            JLabel labelComponent = new JLabel(label);
            labelComponent.setFont(new Font("Segoe UI", Font.BOLD, 11));
            labelComponent.setForeground(new Color(0x475569));

            JTextField field = new JTextField(safe(value));
            field.setEditable(false);
            field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            field.setForeground(new Color(0x334155));
            field.setBackground(new Color(0xF8FAFC));
            field.setPreferredSize(new Dimension(0, 40));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                    new EmptyBorder(9, 12, 9, 12)));

            group.add(labelComponent);
            group.add(Box.createVerticalStrut(6));
            group.add(field);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = x;
            gbc.gridy = y;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, x == 0 ? 0 : 10, 14, x == 0 ? 10 : 0);
            parent.add(group, gbc);
        }

        private JPanel footer(JButton... buttons) {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footer.setOpaque(false);
            for (JButton button : buttons) {
                footer.add(button);
            }
            return footer;
        }

        private JButton closeButton() {
            JButton close = secondaryButton("Close");
            close.addActionListener(e -> dispose());
            return close;
        }

        private JButton deleteButton() {
            JButton delete = dangerButton("Delete");
            delete.addActionListener(e -> onDeleteAccount());
            return delete;
        }

        private String displayName() {
            String fullName = safe(account.getFullName()).trim();
            return fullName.isEmpty() ? safe(account.getUsername()) : fullName;
        }

        private String roleText() {
            return account.getRole() != null ? account.getRole().name() : "";
        }

        private String firstInitial() {
            String source = displayName();
            return source.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(source.charAt(0)));
        }
    }

    private byte[] loadPicture(String username) {
        if (profilePictureRepository == null) {
            return null;
        }
        try {
            byte[] data = profilePictureRepository.loadPicture(username);
            return data != null && data.length > 0 ? data : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean canDeleteAccount() {
        return accountRoleRepository != null
                && account != null
                && !StaffProfileForm.safe(account.getUsername()).equals(currentUsername);
    }

    private void onDeleteAccount() {
        String target = StaffProfileForm.safe(account.getUsername());
        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Delete staff account \"" + target + "\"?\nThis cannot be undone.",
                "Delete Staff Account",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            if (profilePictureRepository != null) {
                profilePictureRepository.deletePicture(target);
            }
            accountRoleRepository.deleteUser(target);
            if (onDeleted != null) {
                onDeleted.run();
            }
            JOptionPane.showMessageDialog(this,
                    "Staff account \"" + target + "\" deleted.",
                    "Deleted",
                    JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to delete staff account:\n" + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    static class StaffProfileForm {
        private static final Color BG_PAGE = new Color(0xF3F6FA);
        private static final Color BG_CARD = Color.WHITE;
        private static final Color BG_INPUT = new Color(0xF8FAFC);
        private static final Color BG_INPUT_READONLY = new Color(0xF1F5F9);
        private static final Color ACCENT = new Color(0x2563EB);
        private static final Color ACCENT_HOVER = new Color(0x1D4ED8);
        private static final Color ACCENT_SOFT = new Color(0xDBEAFE);
        private static final Color BORDER = new Color(0xE2E8F0);
        private static final Color TEXT_PRIMARY = new Color(0x0F172A);
        private static final Color TEXT_SECONDARY = new Color(0x475569);
        private static final Color TEXT_MUTED = new Color(0x94A3B8);
        private static final Color DANGER = new Color(0xEF4444);
        private static final Color DANGER_HOVER = new Color(0xDC2626);
        private static final Color STAFF_BG = new Color(0xDCFCE7);
        private static final Color STAFF_FG = new Color(0x166534);
        private static final Color ADMIN_BG = new Color(0xE0E7FF);
        private static final Color ADMIN_FG = new Color(0x3730A3);

        private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 28);
        private static final Font SUBTITLE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
        private static final Font NAME_FONT = new Font("Segoe UI", Font.BOLD, 20);
        private static final Font LABEL_FONT = new Font("Segoe UI", Font.BOLD, 11);
        private static final Font VALUE_FONT = new Font("Segoe UI", Font.PLAIN, 13);
        private static final Font BUTTON_FONT = new Font("Segoe UI", Font.BOLD, 13);
        private static final Font AVATAR_FONT = new Font("Segoe UI", Font.BOLD, 34);
        private static final Font SECTION_FONT = new Font("Segoe UI", Font.BOLD, 15);
        private static final Font BADGE_FONT = new Font("Segoe UI", Font.BOLD, 11);

        private final String sectionTitle;
        private int row;
        private int column;

        StaffProfileForm(String sectionTitle) {
            this.sectionTitle = sectionTitle;
        }

        JPanel createRoot() {
            JPanel root = new JPanel(new BorderLayout(0, 14));
            root.setBackground(BG_PAGE);
            root.setBorder(new EmptyBorder(22, 22, 22, 22));
            return root;
        }

        JPanel createHeader(String title, String role, String initial) {
            return createHeader(title, role, initial, null);
        }

        JPanel createHeader(String title, String role, String initial, byte[] imageBytes) {
            return createSummaryHeader(title, "Create and manage staff access",
                    avatarSquare(initial, imageBytes), title, role,
                    new String[][] {
                            { "Account", "New staff member" },
                            { "Picture", "Optional profile photo" }
                    });
        }

        JPanel createSummaryHeader(String title, String subtitle, JComponent imageComponent,
                String displayName, String role, String[][] facts) {
            JPanel card = roundedPanel();
            card.setLayout(new BorderLayout(18, 0));
            card.setBorder(new EmptyBorder(22, 22, 22, 22));

            JPanel titlePanel = new JPanel();
            titlePanel.setOpaque(false);
            titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

            JLabel titleLabel = new JLabel(safe(title));
            titleLabel.setFont(TITLE_FONT);
            titleLabel.setForeground(TEXT_PRIMARY);
            titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel subtitleLabel = new JLabel(safe(subtitle));
            subtitleLabel.setFont(SUBTITLE_FONT);
            subtitleLabel.setForeground(TEXT_SECONDARY);
            subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

            titlePanel.add(titleLabel);
            titlePanel.add(Box.createVerticalStrut(4));
            titlePanel.add(subtitleLabel);

            JPanel summary = new JPanel(new BorderLayout(18, 0));
            summary.setOpaque(false);
            summary.setBorder(new EmptyBorder(18, 0, 0, 0));
            summary.add(imageComponent, BorderLayout.WEST);
            summary.add(summaryText(displayName, role, facts), BorderLayout.CENTER);

            JPanel content = new JPanel(new BorderLayout());
            content.setOpaque(false);
            content.add(titlePanel, BorderLayout.NORTH);
            content.add(summary, BorderLayout.CENTER);
            card.add(content, BorderLayout.CENTER);
            return card;
        }

        JPanel createFormCard() {
            row = 0;
            column = 0;
            JPanel card = roundedPanel();
            card.setLayout(new GridBagLayout());
            card.setBorder(new EmptyBorder(20, 22, 8, 22));

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = row++;
            gbc.gridwidth = 2;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, 0, 16, 0);

            JLabel section = new JLabel(sectionTitle);
            section.setFont(SECTION_FONT);
            section.setForeground(TEXT_PRIMARY);
            card.add(section, gbc);
            return card;
        }

        JPanel wrapFormCard(JPanel formCard) {
            JPanel wrapper = new JPanel(new BorderLayout());
            wrapper.setOpaque(false);
            wrapper.add(formCard, BorderLayout.CENTER);
            return wrapper;
        }

        JTextField addTextField(JPanel parent, String label, String value) {
            JTextField field = textField(value, true);
            addInput(parent, label, field);
            return field;
        }

        JPasswordField addPasswordField(JPanel parent, String label) {
            JPasswordField field = passwordField();
            addInput(parent, label, field);
            return field;
        }

        JComboBox<String> addComboBox(JPanel parent, String label, String[] values) {
            JComboBox<String> combo = new JComboBox<>(values);
            combo.setFont(VALUE_FONT);
            combo.setBackground(BG_INPUT);
            combo.setPreferredSize(new Dimension(0, 40));
            combo.setBorder(BorderFactory.createLineBorder(BORDER));
            addInput(parent, label, combo);
            return combo;
        }

        JSpinner addDateSpinner(JPanel parent, String label) {
            JSpinner spinner = dateSpinner();
            addInput(parent, label, spinner);
            return spinner;
        }

        void addCustomInput(JPanel parent, String label, JComponent input) {
            addInput(parent, label, input);
        }

        void addReadOnlyField(JPanel parent, String label, String value) {
            JTextField field = textField(value, false);
            field.setBackground(BG_INPUT_READONLY);
            addInput(parent, label, field);
        }

        JPanel createFooter(JButton... buttons) {
            JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
            footer.setOpaque(false);
            for (JButton button : buttons) {
                footer.add(button);
            }
            return footer;
        }

        JButton primaryButton(String text) {
            JButton button = baseButton(text);
            button.setForeground(Color.WHITE);
            button.setBackground(ACCENT);
            button.setBorderPainted(false);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (button.isEnabled())
                        button.setBackground(ACCENT_HOVER);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    button.setBackground(ACCENT);
                }
            });
            return button;
        }

        JButton secondaryButton(String text) {
            JButton button = baseButton(text);
            button.setForeground(TEXT_PRIMARY);
            button.setBackground(BG_CARD);
            button.setBorder(BorderFactory.createLineBorder(BORDER));
            return button;
        }

        JButton dangerButton(String text) {
            JButton button = baseButton(text);
            button.setForeground(Color.WHITE);
            button.setBackground(DANGER);
            button.setBorderPainted(false);
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    if (button.isEnabled())
                        button.setBackground(DANGER_HOVER);
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    button.setBackground(DANGER);
                }
            });
            return button;
        }

        static String safe(String value) {
            return value != null ? value : "";
        }

        JPanel avatarSquare(String initial, byte[] imageBytes) {
            JPanel avatar = new JPanel() {
                private final Image image = imageBytes != null ? new ImageIcon(imageBytes).getImage() : null;

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    RoundRectangle2D.Float shape = new RoundRectangle2D.Float(
                            0, 0, getWidth() - 1, getHeight() - 1, 18, 18);
                    g2.setColor(ACCENT_SOFT);
                    g2.fill(shape);
                    if (image != null) {
                        Shape oldClip = g2.getClip();
                        g2.setClip(shape);
                        drawImageCover(g2, image, getWidth(), getHeight());
                        g2.setClip(oldClip);
                    } else {
                        g2.setColor(ACCENT);
                        g2.setFont(AVATAR_FONT);
                        String label = safe(initial).isEmpty() ? "?" : initial;
                        FontMetrics fm = g2.getFontMetrics();
                        int x = (getWidth() - fm.stringWidth(label)) / 2;
                        int y = (getHeight() - fm.getHeight()) / 2 + fm.getAscent();
                        g2.drawString(label, x, y);
                    }
                    g2.setColor(BORDER);
                    g2.draw(shape);
                    g2.dispose();
                }
            };
            avatar.setOpaque(false);
            avatar.setPreferredSize(new Dimension(112, 112));
            avatar.setMinimumSize(new Dimension(112, 112));
            return avatar;
        }

        private JPanel summaryText(String displayName, String role, String[][] facts) {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel name = new JLabel(safe(displayName));
            name.setFont(NAME_FONT);
            name.setForeground(TEXT_PRIMARY);
            name.setAlignmentX(Component.LEFT_ALIGNMENT);

            JLabel badge = roleBadge(role);
            badge.setAlignmentX(Component.LEFT_ALIGNMENT);

            panel.add(name);
            panel.add(Box.createVerticalStrut(7));
            panel.add(badge);
            panel.add(Box.createVerticalStrut(14));
            panel.add(factList(facts));
            return panel;
        }

        private JPanel factList(String[][] facts) {
            JPanel panel = new JPanel(new GridLayout(0, 2, 12, 8));
            panel.setOpaque(false);
            panel.setAlignmentX(Component.LEFT_ALIGNMENT);
            for (String[] fact : facts) {
                panel.add(factPair(fact[0], fact.length > 1 ? fact[1] : ""));
            }
            return panel;
        }

        private JPanel factPair(String label, String value) {
            JPanel panel = new JPanel();
            panel.setOpaque(false);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

            JLabel labelComponent = new JLabel(safe(label).toUpperCase());
            labelComponent.setFont(LABEL_FONT);
            labelComponent.setForeground(TEXT_MUTED);

            JLabel valueComponent = new JLabel(safe(value));
            valueComponent.setFont(VALUE_FONT);
            valueComponent.setForeground(TEXT_SECONDARY);

            panel.add(labelComponent);
            panel.add(Box.createVerticalStrut(2));
            panel.add(valueComponent);
            return panel;
        }

        private JLabel roleBadge(String role) {
            String roleText = safe(role).isBlank() ? "STAFF" : safe(role).toUpperCase();
            boolean admin = "ADMIN".equals(roleText);
            JLabel badge = new JLabel(roleText);
            badge.setFont(BADGE_FONT);
            badge.setForeground(admin ? ADMIN_FG : STAFF_FG);
            badge.setBackground(admin ? ADMIN_BG : STAFF_BG);
            badge.setOpaque(true);
            badge.setBorder(new EmptyBorder(5, 10, 5, 10));
            return badge;
        }

        private void addInput(JPanel parent, String label, JComponent input) {
            JPanel group = new JPanel();
            group.setOpaque(false);
            group.setLayout(new BoxLayout(group, BoxLayout.Y_AXIS));

            JLabel labelComponent = new JLabel(label);
            labelComponent.setFont(LABEL_FONT);
            labelComponent.setForeground(TEXT_SECONDARY);
            labelComponent.setAlignmentX(Component.LEFT_ALIGNMENT);
            input.setAlignmentX(Component.LEFT_ALIGNMENT);

            group.add(labelComponent);
            group.add(Box.createVerticalStrut(6));
            group.add(input);

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = column;
            gbc.gridy = row;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            gbc.insets = new Insets(0, column == 0 ? 0 : 10, 16, column == 0 ? 10 : 0);
            parent.add(group, gbc);

            column++;
            if (column > 1) {
                column = 0;
                row++;
            }
        }

        private JTextField textField(String value, boolean editable) {
            JTextField field = new JTextField(safe(value));
            field.setEditable(editable);
            field.setFont(VALUE_FONT);
            field.setForeground(editable ? TEXT_PRIMARY : TEXT_SECONDARY);
            field.setBackground(BG_INPUT);
            field.setPreferredSize(new Dimension(0, 40));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(9, 12, 9, 12)));
            return field;
        }

        private JPasswordField passwordField() {
            JPasswordField field = new JPasswordField();
            field.setFont(VALUE_FONT);
            field.setForeground(TEXT_PRIMARY);
            field.setBackground(BG_INPUT);
            field.setPreferredSize(new Dimension(0, 40));
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    new EmptyBorder(9, 12, 9, 12)));
            return field;
        }

        private JSpinner dateSpinner() {
            JSpinner spinner = new JSpinner(new SpinnerDateModel());
            spinner.setEditor(new JSpinner.DateEditor(spinner, "yyyy-MM-dd"));
            spinner.setFont(VALUE_FONT);
            spinner.setPreferredSize(new Dimension(0, 40));
            spinner.setBorder(BorderFactory.createLineBorder(BORDER));
            return spinner;
        }

        private JButton baseButton(String text) {
            JButton button = new JButton(text);
            button.setFont(BUTTON_FONT);
            button.setFocusPainted(false);
            button.setPreferredSize(new Dimension(128, 40));
            button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return button;
        }

        private JPanel roundedPanel() {
            JPanel panel = new JPanel();
            panel.setOpaque(true);
            panel.setBackground(BG_CARD);
            panel.setBorder(BorderFactory.createLineBorder(BORDER));
            return panel;
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
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            g2.drawImage(image, drawX, drawY, drawWidth, drawHeight, null);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, oldInterpolation);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, oldRender);
            g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, oldAlpha);
        }
    }
}
