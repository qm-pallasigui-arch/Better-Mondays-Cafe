package loginregister;

import pos.POSSystem;
import ui.AppTheme;
import util.FieldAssist;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Modern warm glassmorphism login screen for Better Mondays Coffee Cafe.
 * Features an auto-fading slideshow of café photos as background.
 */
public class Login extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(Login.class.getName());

    // ── palette (warm café tones) ─────────────────────────────────────────────
    private static final Color CARD_BG = new Color(255, 248, 235, 35);
    private static final Color CARD_BORDER = new Color(255, 220, 160, 60);
    private static final Color INPUT_BG = new Color(255, 248, 235, 40);
    private static final Color INPUT_BORDER = new Color(200, 160, 100, 80);
    private static final Color INPUT_FOCUS = new Color(210, 140, 60, 200);
    private static final Color LABEL_COLOR = new Color(30, 20, 10, 220);
    private static final Color TEXT_WHITE = new Color(10, 5, 0, 240);
    private static final Color BTN_START = new Color(0x8B, 0x5E, 0x3C);
    private static final Color BTN_END = new Color(0xC4, 0x8A, 0x4A);
    private static final Color OVERLAY_COLOR = new Color(20, 10, 5, 140);

    // ── components ────────────────────────────────────────────────────────────
    private SlideshowPanel bgPanel;
    private GlassCard cardPanel;
    private ModernField usernameField;
    private ModernField passwordField;
    private JButton loginBtn;
    private JButton forgotBtn;

    public Login() {
        initModernComponents();
        setResizable(true);
        AppTheme.applyResponsiveFrameSize(this, 0.38, 0.62, new Dimension(720, 560));
        setLocationRelativeTo(null);

        if (UserDataManager.listUsers().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "No user accounts found. A default admin account has been created:\n"
                            + "Username: admin\nPassword: Admin@123",
                    "Initial Account Created",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BUILD UI
    // ─────────────────────────────────────────────────────────────────────────
    private void initModernComponents() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Better Mondays Coffee Cafe — Login");
        loadWindowIcon();

        // root slideshow panel fills the frame
        bgPanel = new SlideshowPanel();
        bgPanel.setLayout(new GridBagLayout());
        setContentPane(bgPanel);

        // glass card
        cardPanel = new GlassCard();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setPreferredSize(new Dimension(340, 420));
        cardPanel.setMaximumSize(new Dimension(380, 460));
        cardPanel.setBorder(new EmptyBorder(30, 32, 28, 32));

        buildCardContents();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        bgPanel.add(cardPanel, gbc);

        pack();
    }

    /**
     * Loads logo.png from the classpath and sets it as the window icon.
     * Logs a warning instead of silently swallowing errors so you can
     * diagnose missing-resource issues during development.
     */
    private void loadWindowIcon() {
        String iconPath = "/images/logo.png";
        try {
            java.net.URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                Image icon = new ImageIcon(iconUrl).getImage();
                setIconImage(icon);
                LOGGER.info("Window icon loaded from: " + iconUrl);
            } else {
                LOGGER.warning("Window icon not found on classpath: " + iconPath
                        + " — make sure the file exists under src/main/resources"
                        + iconPath + " and that your resources folder is marked as"
                        + " a source root in your IDE / build tool.");
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING,
                    "Failed to load window icon from " + iconPath + ": " + ex.getMessage(), ex);
        }
    }

    private void buildCardContents() {
        // ── logo ──
        JLabel logoLabel = new JLabel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                // warm glow
                g2.setColor(new Color(200, 130, 60, 70));
                g2.fillOval(-8, -8, w + 16, h + 16);
                // circle gradient
                GradientPaint gp = new GradientPaint(0, 0, new Color(0xD4, 0x9A, 0x55),
                        w, h, new Color(0x8B, 0x5E, 0x3C));
                g2.setPaint(gp);
                g2.fillOval(0, 0, w, h);
                // coffee cup icon
                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int cx = w / 2, cy = h / 2;
                g2.drawRoundRect(cx - 9, cy - 5, 18, 14, 4, 4);
                g2.drawArc(cx + 9, cy - 1, 7, 8, -90, 180);
                g2.drawLine(cx - 4, cy - 8, cx - 2, cy - 11);
                g2.drawLine(cx, cy - 8, cx + 2, cy - 12);
                g2.drawLine(cx + 4, cy - 8, cx + 6, cy - 11);
                g2.dispose();
            }
        };
        logoLabel.setPreferredSize(new Dimension(54, 54));
        logoLabel.setMaximumSize(new Dimension(54, 54));
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── title ──
        JLabel titleLabel = new JLabel("Welcome Back");
        titleLabel.setFont(new Font("Georgia", Font.BOLD, 22));
        titleLabel.setForeground(TEXT_WHITE);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Better Mondays Coffee Cafe");
        subtitleLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        subtitleLabel.setForeground(LABEL_COLOR);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── fields ──
        usernameField = new ModernField("Username", false);
        usernameField.setAlignmentX(Component.CENTER_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        FieldAssist.installAutocomplete(usernameField.getInputField(),
                () -> UserDataManager.listUsers().stream()
                        .map(UserAccount::getUsername)
                        .collect(Collectors.toList()));

        passwordField = new ModernField("Password", true);
        passwordField.setAlignmentX(Component.CENTER_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        // ── forgot password row ──
        JPanel optRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        optRow.setOpaque(false);
        optRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        forgotBtn = makeTextButton("Forgot Password?");
        forgotBtn.addActionListener(e -> showForgotPasswordDialog());
        optRow.add(forgotBtn);

        // ── login button ──
        loginBtn = new JButton("LOGIN") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color s = getModel().isPressed() ? BTN_START.darker()
                        : getModel().isRollover() ? BTN_START.brighter() : BTN_START;
                Color e2 = getModel().isPressed() ? BTN_END.darker() : BTN_END;
                GradientPaint gp = new GradientPaint(0, 0, s, getWidth(), getHeight(), e2);
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                // subtle top highlight
                g2.setColor(new Color(255, 255, 255, 30));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        loginBtn.setPreferredSize(new Dimension(280, 44));
        loginBtn.addActionListener(e -> handleLogin());

        usernameField.getInputField().addActionListener(e -> handleLogin());
        passwordField.getInputField().addActionListener(e -> handleLogin());

        // ── assemble card ──
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(logoLabel);
        cardPanel.add(Box.createVerticalStrut(12));
        cardPanel.add(titleLabel);
        cardPanel.add(Box.createVerticalStrut(4));
        cardPanel.add(subtitleLabel);
        cardPanel.add(Box.createVerticalStrut(22));
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(10));
        cardPanel.add(optRow);
        cardPanel.add(Box.createVerticalStrut(16));
        cardPanel.add(loginBtn);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ACTIONS
    // ─────────────────────────────────────────────────────────────────────────
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both username and password.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (UserDataManager.verifyCredentials(username, password)) {
            UserDataManager.Role userRole = UserDataManager.getUserRole(username);
            new POSSystem(username, userRole).setVisible(true);
            this.dispose();
        } else {
            String msg = "Invalid username or password.";
            if ("admin".equalsIgnoreCase(username) && "Admin@123".equals(password)) {
                msg += "\nUse Forgot Password to reset the admin password.";
            }
            JOptionPane.showMessageDialog(this, msg, "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showForgotPasswordDialog() {
        JTextField uf = new JTextField(20);
        JPanel panel = new JPanel(new GridLayout(0, 1, 8, 8));
        panel.add(new JLabel("Enter your username to request a password reset."));
        panel.add(new JLabel("An admin will review and approve your request."));
        panel.add(new JLabel(" "));
        panel.add(new JLabel("Username:"));
        panel.add(uf);

        int result = JOptionPane.showConfirmDialog(this, panel,
                "Forgot Password — Request Reset",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION)
            return;

        String username = uf.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter your username.",
                    "Forgot Password", JOptionPane.WARNING_MESSAGE);
            return;
        }
        boolean exists = UserDataManager.listUsers().stream()
                .anyMatch(a -> a.getUsername().equalsIgnoreCase(username));
        if (!exists) {
            JOptionPane.showMessageDialog(this, "Username not found.",
                    "Forgot Password", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (UserDataManager.hasPendingResetRequest(username)) {
            JOptionPane.showMessageDialog(this,
                    "A reset request for \"" + username + "\" is already pending.",
                    "Forgot Password", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            UserDataManager.submitResetRequest(username);
            JOptionPane.showMessageDialog(this,
                    "<html><b>Request submitted.</b><br><br>"
                            + "Your password reset request has been sent to an admin.<br>"
                            + "Please wait for approval before trying to log in again.</html>",
                    "Forgot Password — Submitted", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Unable to submit request: " + ex.getMessage(),
                    "Forgot Password", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JButton makeTextButton(String text) {
        JButton b = new JButton(text);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(LABEL_COLOR);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setForeground(new Color(80, 50, 20));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setForeground(LABEL_COLOR);
            }
        });
        return b;
    }

    // =========================================================================
    // INNER — SlideshowPanel
    // =========================================================================
    private static class SlideshowPanel extends JPanel {

        private static final String[] IMAGE_PATHS = {
                "/images/background/bg1.jpg",
                "/images/background/bg2.jpg",
                "/images/background/bg3.jpg",
                "/images/background/bg4.jpg",
        };

        private static final int DISPLAY_MS = 5000;
        private static final int FADE_STEPS = 60;
        private static final int FRAME_MS = 33;
        private static final float BLUR_SIGMA = 1.0f;

        private final List<Image> images = new ArrayList<>();
        private int currentIdx = 0;
        private int nextIdx = 1;
        private float alpha = 1f;
        private boolean fading = false;
        private int fadeStep = 0;

        SlideshowPanel() {
            setBackground(new Color(30, 15, 5));
            loadImages();
            startTimers();
        }

        private void loadImages() {
            for (String path : IMAGE_PATHS) {
                try {
                    java.net.URL url = getClass().getResource(path);
                    if (url != null) {
                        images.add(new ImageIcon(url).getImage());
                    } else {
                        Logger.getLogger(SlideshowPanel.class.getName())
                                .warning("Slideshow image not found on classpath: " + path);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(SlideshowPanel.class.getName())
                            .log(Level.WARNING, "Failed to load slideshow image: " + path, ex);
                }
            }
        }

        private void startTimers() {
            Timer displayTimer = new Timer(DISPLAY_MS, e -> startFade());
            displayTimer.setRepeats(true);
            displayTimer.start();

            Timer fadeTimer = new Timer(FRAME_MS, e -> {
                if (fading) {
                    fadeStep++;
                    alpha = 1f - (float) fadeStep / FADE_STEPS;
                    if (fadeStep >= FADE_STEPS) {
                        currentIdx = nextIdx;
                        nextIdx = (currentIdx + 1) % Math.max(images.size(), 1);
                        alpha = 1f;
                        fading = false;
                        fadeStep = 0;
                    }
                    repaint();
                }
            });
            fadeTimer.setRepeats(true);
            fadeTimer.start();
        }

        private void startFade() {
            if (!fading && images.size() > 1) {
                nextIdx = (currentIdx + 1) % images.size();
                fading = true;
                fadeStep = 0;
            }
        }

        // ── Gaussian blur helpers ─────────────────────────────────────────────

        private static BufferedImage gaussianBlur(BufferedImage src, float sigma) {
            if (sigma <= 0)
                return src;
            int radius = (int) Math.ceil(3 * sigma);
            float[] kernel = buildGaussianKernel(sigma, radius);
            int w = src.getWidth(), h = src.getHeight();
            int[] pixels = new int[w * h];
            src.getRGB(0, 0, w, h, pixels, 0, w);
            int[] temp = new int[w * h];
            convolveH(pixels, temp, w, h, kernel, radius);
            convolveV(temp, pixels, w, h, kernel, radius);
            BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            dst.setRGB(0, 0, w, h, pixels, 0, w);
            return dst;
        }

        private static float[] buildGaussianKernel(float sigma, int radius) {
            int size = radius * 2 + 1;
            float[] k = new float[size];
            float sum = 0;
            for (int i = 0; i < size; i++) {
                float x = i - radius;
                k[i] = (float) Math.exp(-(x * x) / (2f * sigma * sigma));
                sum += k[i];
            }
            for (int i = 0; i < size; i++)
                k[i] /= sum;
            return k;
        }

        private static void convolveH(int[] src, int[] dst, int w, int h,
                float[] kernel, int radius) {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    float r = 0, g = 0, b = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int xi = Math.max(0, Math.min(w - 1, x + k));
                        int p = src[y * w + xi];
                        float weight = kernel[k + radius];
                        r += ((p >> 16) & 0xFF) * weight;
                        g += ((p >> 8) & 0xFF) * weight;
                        b += (p & 0xFF) * weight;
                    }
                    dst[y * w + x] = 0xFF000000 | ((int) r << 16) | ((int) g << 8) | (int) b;
                }
            }
        }

        private static void convolveV(int[] src, int[] dst, int w, int h,
                float[] kernel, int radius) {
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) {
                    float r = 0, g = 0, b = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int yi = Math.max(0, Math.min(h - 1, y + k));
                        int p = src[yi * w + x];
                        float weight = kernel[k + radius];
                        r += ((p >> 16) & 0xFF) * weight;
                        g += ((p >> 8) & 0xFF) * weight;
                        b += (p & 0xFF) * weight;
                    }
                    dst[y * w + x] = 0xFF000000 | ((int) r << 16) | ((int) g << 8) | (int) b;
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();

            if (images.isEmpty()) {
                GradientPaint gp = new GradientPaint(w / 2f, 0,
                        new Color(0x3D, 0x20, 0x0A),
                        w / 2f, h, new Color(0x1A, 0x0A, 0x02));
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);
            } else {
                if (fading && images.size() > 1) {
                    drawBlurredCover(g2, images.get(nextIdx), w, h, 1f);
                    drawBlurredCover(g2, images.get(currentIdx), w, h, alpha);
                } else {
                    drawBlurredCover(g2, images.get(currentIdx), w, h, 1f);
                }
            }

            g2.setColor(OVERLAY_COLOR);
            g2.fillRect(0, 0, w, h);
            g2.dispose();
        }

        private void drawBlurredCover(Graphics2D g2, Image img, int pw, int ph, float imgAlpha) {
            int iw = img.getWidth(null);
            int ih = img.getHeight(null);
            if (iw <= 0 || ih <= 0)
                return;

            double scale = Math.max((double) pw / iw, (double) ph / ih);
            int dw = (int) (iw * scale);
            int dh = (int) (ih * scale);
            int dx = (pw - dw) / 2;
            int dy = (ph - dh) / 2;

            int bw = Math.max(1, dw / 2);
            int bh = Math.max(1, dh / 2);
            BufferedImage buf = new BufferedImage(bw, bh, BufferedImage.TYPE_INT_ARGB);
            Graphics2D bg = buf.createGraphics();
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            bg.drawImage(img, 0, 0, bw, bh, null);
            bg.dispose();

            BufferedImage blurred = gaussianBlur(buf, BLUR_SIGMA);

            Composite old = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, imgAlpha));
            g2.drawImage(blurred, dx, dy, dw, dh, null);
            g2.setComposite(old);
        }
    }

    // =========================================================================
    // INNER — GlassCard
    // =========================================================================
    private static class GlassCard extends JPanel {
        GlassCard() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            RoundRectangle2D rr = new RoundRectangle2D.Float(0, 0, w, h, 22, 22);

            g2.setColor(CARD_BG);
            g2.fill(rr);

            GradientPaint topSheen = new GradientPaint(
                    0, 0, new Color(255, 240, 200, 30),
                    0, h * 0.4f, new Color(255, 240, 200, 0));
            g2.setPaint(topSheen);
            g2.fill(rr);

            g2.setColor(CARD_BORDER);
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(rr);
            g2.dispose();
        }
    }

    // =========================================================================
    // INNER — ModernField (warm palette)
    // =========================================================================
    private static class ModernField extends JPanel {
        private final JTextField textField;
        private final JPasswordField pwdField;
        private final boolean isPassword;
        private boolean showPwd = false;
        private boolean focused = false;

        ModernField(String placeholder, boolean isPassword) {
            this.isPassword = isPassword;
            setOpaque(false);
            setLayout(new BorderLayout(0, 0));
            setPreferredSize(new Dimension(276, 44));

            if (isPassword) {
                textField = null;
                pwdField = new JPasswordField();
                pwdField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                pwdField.setForeground(TEXT_WHITE);
                pwdField.setCaretColor(TEXT_WHITE);
                pwdField.setOpaque(false);
                pwdField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                styleInput(pwdField, placeholder);
                add(pwdField, BorderLayout.CENTER);
            } else {
                pwdField = null;
                textField = new JTextField();
                textField.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                textField.setForeground(TEXT_WHITE);
                textField.setCaretColor(TEXT_WHITE);
                textField.setOpaque(false);
                textField.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
                styleInput(textField, placeholder);
                add(textField, BorderLayout.CENTER);
            }

            // prefix icon
            JLabel icon = new JLabel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(30, 20, 10, focused ? 220 : 150));
                    g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    int cx = getWidth() / 2, cy = getHeight() / 2;
                    if (isPassword) {
                        g2.drawRoundRect(cx - 6, cy - 2, 12, 9, 3, 3);
                        g2.drawArc(cx - 4, cy - 7, 8, 8, 0, 180);
                    } else {
                        g2.drawOval(cx - 4, cy - 7, 8, 7);
                        g2.drawArc(cx - 7, cy + 1, 14, 8, 0, 180);
                    }
                    g2.dispose();
                }
            };
            icon.setPreferredSize(new Dimension(38, 44));
            add(icon, BorderLayout.WEST);

            // eye toggle
            if (isPassword) {
                JButton eye = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(30, 20, 10, getModel().isRollover() ? 220 : 150));
                        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                        int cx = getWidth() / 2, cy = getHeight() / 2;
                        if (showPwd) {
                            g2.drawArc(cx - 7, cy - 4, 14, 10, 0, 180);
                            g2.drawLine(cx - 8, cy + 4, cx + 8, cy - 4);
                        } else {
                            g2.drawOval(cx - 3, cy - 3, 6, 6);
                            g2.drawArc(cx - 9, cy - 6, 18, 14, 0, 180);
                            g2.drawArc(cx - 9, cy - 8, 18, 14, 180, 180);
                        }
                        g2.dispose();
                    }
                };
                eye.setPreferredSize(new Dimension(36, 44));
                eye.setContentAreaFilled(false);
                eye.setBorderPainted(false);
                eye.setFocusPainted(false);
                eye.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                eye.addActionListener(ev -> {
                    showPwd = !showPwd;
                    pwdField.setEchoChar(showPwd ? (char) 0 : '●');
                    eye.repaint();
                });
                add(eye, BorderLayout.EAST);
            }

            FocusListener fl = new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    focused = true;
                    repaint();
                }

                @Override
                public void focusLost(FocusEvent e) {
                    focused = false;
                    repaint();
                }
            };
            if (isPassword)
                pwdField.addFocusListener(fl);
            else
                textField.addFocusListener(fl);
        }

        private void styleInput(JTextField field, String placeholder) {
            field.putClientProperty("JTextField.placeholderText", placeholder);
            field.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
            field.setForeground(LABEL_COLOR);
            field.setText(placeholder);
            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(TEXT_WHITE);
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder);
                        field.setForeground(LABEL_COLOR);
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(INPUT_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(focused ? INPUT_FOCUS : INPUT_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.5f : 1f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
            super.paintComponent(g);
        }

        public JTextField getInputField() {
            return isPassword ? pwdField : textField;
        }

        public String getText() {
            return isPassword ? new String(pwdField.getPassword()) : textField.getText();
        }

        public char[] getPassword() {
            return pwdField != null ? pwdField.getPassword() : new char[0];
        }

        public void addActionListener(ActionListener al) {
            if (isPassword)
                pwdField.addActionListener(al);
            else
                textField.addActionListener(al);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MAIN
    // ─────────────────────────────────────────────────────────────────────────
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignored) {
        }

        try {
            persistence.Phase2Bootstrap.seedCatalogIfEmpty();
        } catch (Exception ex) {
            Logger.getLogger(Login.class.getName())
                    .log(Level.WARNING, "Bootstrap: " + ex.getMessage(), ex);
        }

        EventQueue.invokeLater(() -> new Login().setVisible(true));
    }
}