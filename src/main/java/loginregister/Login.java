package loginregister;

import persistence.sqlite.SQLiteStaffShiftRepository;
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
 * Split-panel login screen for Better Mondays Coffee Cafe.
 * Left: auto-fading slideshow with café brand overlay.
 * Right: clean white form panel with username, password, login button, and
 * forgot password.
 */
public class Login extends javax.swing.JFrame {

    private static final Logger LOGGER = Logger.getLogger(Login.class.getName());

    // ── Right-panel palette (clean, minimal) ──────────────────────────────────
    private static final Color RIGHT_BG = Color.WHITE;
    private static final Color BRAND_TEXT = new Color(0x1A, 0x0F, 0x05);
    private static final Color SUBTLE_TEXT = new Color(0x6B, 0x5C, 0x4A);
    private static final Color FIELD_BORDER = new Color(0xD1, 0xC4, 0xB0);
    private static final Color FIELD_FOCUS = new Color(0x8B, 0x5E, 0x3C);
    private static final Color FIELD_BG = new Color(0xFF, 0xFD, 0xF9);
    private static final Color BTN_BG = new Color(0x1A, 0x0F, 0x05);
    private static final Color BTN_HOVER = new Color(0x3D, 0x28, 0x14);
    private static final Color FORGOT_COLOR = new Color(0x8B, 0x5E, 0x3C);

    // ── Left-panel overlay ────────────────────────────────────────────────────
    private static final Color OVERLAY_COLOR = new Color(15, 8, 3, 150);
    private static final Color OVERLAY_GRAD_TOP = new Color(0x3D, 0x20, 0x0A, 180);
    private static final Color OVERLAY_GRAD_BOT = new Color(0x1A, 0x0A, 0x02, 220);

    // ── components ────────────────────────────────────────────────────────────
    private SlideshowPanel leftPanel;
    private JPanel rightPanel;
    private SplitField usernameField;
    private SplitField passwordField;
    private JButton loginBtn;

    public Login() {
        initComponents();
        setResizable(true);
        // Wider default to accommodate side-by-side layout
        AppTheme.applyResponsiveFrameSize(this, 0.65, 0.70, new Dimension(960, 600));
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
    private void initComponents() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setTitle("Better Mondays Coffee Cafe — Login");
        loadWindowIcon();

        // Root container: weighted split — left ~62%, right ~38%
        JPanel root = new JPanel(new BorderLayout(0, 0)) {
            @Override
            public void doLayout() {
                int w = getWidth(), h = getHeight();
                int leftW = (int) (w * 0.62);
                int rightW = w - leftW;
                Component[] comps = getComponents();
                if (comps.length >= 2) {
                    comps[0].setBounds(0, 0, leftW, h);
                    comps[1].setBounds(leftW, 0, rightW, h);
                }
            }
        };
        root.setBackground(RIGHT_BG);
        setContentPane(root);

        // ── LEFT: slideshow panel ──
        leftPanel = new SlideshowPanel();
        root.add(leftPanel);

        // ── RIGHT: form panel ──
        rightPanel = buildRightPanel();
        root.add(rightPanel);

        pack();
    }

    private JPanel buildRightPanel() {
        JPanel panel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(340, super.getPreferredSize().height);
            }

            @Override
            public Dimension getMinimumSize() {
                return new Dimension(300, 0);
            }
        };
        panel.setBackground(RIGHT_BG);
        panel.setLayout(new GridBagLayout());

        // Inner form container — constrained width
        JPanel form = new JPanel();
        form.setBackground(RIGHT_BG);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(new EmptyBorder(0, 40, 0, 40));
        form.setMaximumSize(new Dimension(360, Integer.MAX_VALUE));

        // ── Brand logo top-left (BMC logo, tinted dark for white background) ──
        JLabel brandLabel = new JLabel() {
            private Image logoImg;
            private BufferedImage tintedLogo;
            {
                try {
                    java.net.URL url = getClass().getResource("/images/BMC.png");
                    if (url != null) {
                        Image raw = new ImageIcon(url).getImage();
                        int targetH = 72;
                        int iw = raw.getWidth(null), ih = raw.getHeight(null);
                        int targetW = (ih > 0) ? (int) ((double) iw / ih * targetH) : 120;
                        logoImg = raw.getScaledInstance(targetW, targetH, Image.SCALE_SMOOTH);

                        // Pre-render: invert colors so white logo becomes dark on transparent
                        BufferedImage buf = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
                        Graphics2D bg = buf.createGraphics();
                        bg.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                        bg.drawImage(logoImg, 0, 0, null);
                        bg.dispose();
                        // Invert: white pixels → dark brown, black pixels → transparent
                        for (int y2 = 0; y2 < buf.getHeight(); y2++) {
                            for (int x2 = 0; x2 < buf.getWidth(); x2++) {
                                int argb = buf.getRGB(x2, y2);
                                int a = (argb >> 24) & 0xFF;
                                int r = (argb >> 16) & 0xFF;
                                int g = (argb >> 8) & 0xFF;
                                int b = (argb) & 0xFF;
                                // Brightness of the pixel
                                float brightness = (r + g + b) / (3f * 255f);
                                // Map bright pixels → dark brand color, dark pixels → transparent
                                int newA = (int) (brightness * 255);
                                int nr = 0x1A, ng = 0x0F, nb = 0x05;
                                buf.setRGB(x2, y2, (newA << 24) | (nr << 16) | (ng << 8) | nb);
                            }
                        }
                        tintedLogo = buf;
                        setPreferredSize(new Dimension(targetW, targetH));
                        setMaximumSize(new Dimension(targetW, targetH));
                    }
                } catch (Exception ex) {
                    LOGGER.log(Level.WARNING, "Failed to load logo for right panel", ex);
                }
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (tintedLogo != null) {
                    g2.drawImage(tintedLogo, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g2.setFont(new Font("Georgia", Font.BOLD, 15));
                    g2.setColor(BRAND_TEXT);
                    g2.drawString("Better Mondays", 0, 18);
                }
                g2.dispose();
            }
        };
        brandLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Welcome heading ──
        JLabel welcomeLabel = new JLabel("Welcome Back!");
        welcomeLabel.setFont(new Font("Georgia", Font.BOLD, 30));
        welcomeLabel.setForeground(BRAND_TEXT);
        welcomeLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subLabel = new JLabel("Sign in to your account to continue.");
        subLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        subLabel.setForeground(SUBTLE_TEXT);
        subLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // ── Username field ──
        JLabel userLbl = makeFieldLabel("Username");
        usernameField = new SplitField("Enter your username", false);
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        usernameField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        FieldAssist.installAutocomplete(usernameField.getInputField(),
                () -> UserDataManager.listUsers().stream()
                        .map(UserAccount::getUsername)
                        .collect(Collectors.toList()));

        // ── Password field ──
        JLabel pwdLbl = makeFieldLabel("Password");
        passwordField = new SplitField("Enter your password", true);
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        passwordField.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));

        // ── Forgot password (right-aligned) ──
        JPanel forgotRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        forgotRow.setOpaque(false);
        forgotRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
        forgotRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JButton forgotBtn = makeLinkButton("Forgot password?");
        forgotBtn.addActionListener(e -> showForgotPasswordDialog());
        forgotRow.add(forgotBtn);

        // ── Login button ──
        loginBtn = new JButton("Login Now") {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg = getModel().isPressed() ? BTN_HOVER.darker()
                        : hovered ? BTN_HOVER
                                : BTN_BG;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        loginBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        loginBtn.setForeground(Color.WHITE);
        loginBtn.setContentAreaFilled(false);
        loginBtn.setBorderPainted(false);
        loginBtn.setFocusPainted(false);
        loginBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 48));
        loginBtn.setPreferredSize(new Dimension(300, 48));
        loginBtn.addActionListener(e -> handleLogin());

        // Enter key triggers login
        usernameField.getInputField().addActionListener(e -> handleLogin());
        passwordField.getInputField().addActionListener(e -> handleLogin());

        // ── Assemble form ──
        form.add(brandLabel);
        form.add(Box.createVerticalStrut(32));
        form.add(welcomeLabel);
        form.add(Box.createVerticalStrut(6));
        form.add(subLabel);
        form.add(Box.createVerticalStrut(36));
        form.add(userLbl);
        form.add(Box.createVerticalStrut(6));
        form.add(usernameField);
        form.add(Box.createVerticalStrut(20));
        form.add(pwdLbl);
        form.add(Box.createVerticalStrut(6));
        form.add(passwordField);
        form.add(Box.createVerticalStrut(8));
        form.add(forgotRow);
        form.add(Box.createVerticalStrut(24));
        form.add(loginBtn);

        // Center the form vertically in the panel
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(form, gbc);

        // ── Thin left border separator ──
        panel.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0,
                new Color(0xE8, 0xDF, 0xD0)));

        return panel;
    }

    private JLabel makeFieldLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lbl.setForeground(BRAND_TEXT);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void loadWindowIcon() {
        String iconPath = "/images/BMC.png";
        try {
            java.net.URL iconUrl = getClass().getResource(iconPath);
            if (iconUrl != null) {
                setIconImage(new ImageIcon(iconUrl).getImage());
            } else {
                LOGGER.warning("Window icon not found: " + iconPath);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "Failed to load window icon: " + ex.getMessage(), ex);
        }
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
            try {
                new persistence.sqlite.SQLiteStaffShiftRepository().startShift(username);
            } catch (Exception ex) {
                Logger.getLogger(Login.class.getName())
                        .log(Level.WARNING, "Could not auto-start shift for " + username, ex);
            }
            new POSSystem(username, userRole).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Invalid username or password.",
                    "Login Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void showForgotPasswordDialog() {
        new ForgotPasswordDialog(this).setVisible(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private JButton makeLinkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(FORGOT_COLOR);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setMargin(new Insets(0, 0, 0, 0));
        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                b.setForeground(FORGOT_COLOR.darker());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                b.setForeground(FORGOT_COLOR);
            }
        });
        return b;
    }

    // =========================================================================
    // INNER — SlideshowPanel (left half, full bleed with brand overlay)
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
        private static final float BLUR_SIGMA = 1.2f;

        private final List<Image> images = new ArrayList<>();
        private Image logoImage = null; // BMC logo for overlay
        private int currentIdx = 0;
        private int nextIdx = 1;
        private float alpha = 1f;
        private boolean fading = false;
        private int fadeStep = 0;

        SlideshowPanel() {
            setBackground(new Color(30, 15, 5));
            setOpaque(true);
            loadImages();
            startTimers();
        }

        private void loadImages() {
            // Load BMC logo for overlay
            try {
                java.net.URL logoUrl = getClass().getResource("/images/BMC.png");
                if (logoUrl != null) {
                    logoImage = new ImageIcon(logoUrl).getImage();
                }
            } catch (Exception ex) {
                Logger.getLogger(SlideshowPanel.class.getName())
                        .log(Level.WARNING, "Failed to load logo image", ex);
            }

            for (String path : IMAGE_PATHS) {
                try {
                    java.net.URL url = getClass().getResource(path);
                    if (url != null) {
                        images.add(new ImageIcon(url).getImage());
                    } else {
                        Logger.getLogger(SlideshowPanel.class.getName())
                                .warning("Slideshow image not found: " + path);
                    }
                } catch (Exception ex) {
                    Logger.getLogger(SlideshowPanel.class.getName())
                            .log(Level.WARNING, "Failed to load image: " + path, ex);
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

        // ── Gaussian blur ─────────────────────────────────────────────────────
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
            for (int y = 0; y < h; y++)
                for (int x = 0; x < w; x++) {
                    float r = 0, g = 0, b = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int xi = Math.max(0, Math.min(w - 1, x + k));
                        int p = src[y * w + xi];
                        float wt = kernel[k + radius];
                        r += ((p >> 16) & 0xFF) * wt;
                        g += ((p >> 8) & 0xFF) * wt;
                        b += (p & 0xFF) * wt;
                    }
                    dst[y * w + x] = 0xFF000000 | ((int) r << 16) | ((int) g << 8) | (int) b;
                }
        }

        private static void convolveV(int[] src, int[] dst, int w, int h,
                float[] kernel, int radius) {
            for (int x = 0; x < w; x++)
                for (int y = 0; y < h; y++) {
                    float r = 0, g = 0, b = 0;
                    for (int k = -radius; k <= radius; k++) {
                        int yi = Math.max(0, Math.min(h - 1, y + k));
                        int p = src[yi * w + x];
                        float wt = kernel[k + radius];
                        r += ((p >> 16) & 0xFF) * wt;
                        g += ((p >> 8) & 0xFF) * wt;
                        b += (p & 0xFF) * wt;
                    }
                    dst[y * w + x] = 0xFF000000 | ((int) r << 16) | ((int) g << 8) | (int) b;
                }
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();

            // ── Draw background images ──
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

            // ── Gradient overlay (darkens bottom, adds warmth) ──
            GradientPaint overlay = new GradientPaint(
                    0, 0, OVERLAY_GRAD_TOP,
                    0, h, OVERLAY_GRAD_BOT);
            g2.setPaint(overlay);
            g2.fillRect(0, 0, w, h);

            // ── Brand text overlay ──
            paintBrandOverlay(g2, w, h);

            g2.dispose();
        }

        private void paintBrandOverlay(Graphics2D g2, int w, int h) {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int padX = 36;
            int padY = 30;

            // ── BMC logo top-left (white logo rendered as-is over dark overlay) ──
            if (logoImage != null) {
                int logoH = 100;
                int iw = logoImage.getWidth(null), ih = logoImage.getHeight(null);
                int logoW = (ih > 0) ? (int) ((double) iw / ih * logoH) : 140;
                Composite old = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.95f));
                g2.drawImage(logoImage, padX, padY, logoW, logoH, null);
                g2.setComposite(old);
            } else {
                // Fallback: text only
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                g2.drawString("Better Mondays Coffee", padX, padY + 20);
            }

            // ── Main tagline (bottom third) ──
            int textY = (int) (h * 0.60);

            // Eyebrow
            g2.setColor(new Color(0xD4, 0x9A, 0x55));
            g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
            g2.drawString("COFFEE CAFE", padX, textY);

            // Headline
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("Georgia", Font.BOLD, 36));
            FontMetrics fm = g2.getFontMetrics();
            String line1 = "Start every";
            String line2 = "morning right.";
            g2.drawString(line1, padX, textY + 46);
            g2.drawString(line2, padX, textY + 90);

            // Sub-tagline
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            g2.drawString("Where every cup tells a story.", padX, textY + 118);

            // ── Copyright footer ──
            g2.setColor(new Color(255, 255, 255, 100));
            g2.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            g2.drawString("© 2025 Better Mondays Coffee Cafe. All rights reserved.", padX, h - 24);
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
    // INNER — SplitField (clean underline-style input for right panel)
    // =========================================================================
    private static class SplitField extends JPanel {

        private final JTextField textField;
        private final JPasswordField pwdField;
        private final boolean isPassword;
        private boolean showPwd = false;
        private boolean focused = false;

        SplitField(String placeholder, boolean isPassword) {
            this.isPassword = isPassword;
            setOpaque(false);
            setLayout(new BorderLayout(0, 0));
            setPreferredSize(new Dimension(300, 48));

            if (isPassword) {
                textField = null;
                pwdField = new JPasswordField();
                styleNativeField(pwdField, placeholder);
                add(pwdField, BorderLayout.CENTER);
            } else {
                pwdField = null;
                textField = new JTextField();
                styleNativeField(textField, placeholder);
                add(textField, BorderLayout.CENTER);
            }

            // Eye toggle for password
            if (isPassword) {
                JButton eye = new JButton() {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0x8B, 0x5E, 0x3C,
                                getModel().isRollover() ? 220 : 140));
                        g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND));
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
                eye.setPreferredSize(new Dimension(36, 48));
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

        private void styleNativeField(JTextField field, String placeholder) {
            field.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            field.setForeground(new Color(0x1A, 0x0F, 0x05));
            field.setCaretColor(new Color(0x8B, 0x5E, 0x3C));
            field.setBackground(FIELD_BG);
            field.setOpaque(true);
            field.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(0, 0, 0, 0),
                    BorderFactory.createEmptyBorder(0, 12, 0, 8)));

            // Placeholder behavior
            field.putClientProperty("JTextField.placeholderText", placeholder);
            field.setForeground(new Color(0xAA, 0x99, 0x88));
            field.setText(placeholder);

            field.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (field.getText().equals(placeholder)) {
                        field.setText("");
                        field.setForeground(new Color(0x1A, 0x0F, 0x05));
                    }
                }

                @Override
                public void focusLost(FocusEvent e) {
                    if (field.getText().isEmpty()) {
                        field.setText(placeholder);
                        field.setForeground(new Color(0xAA, 0x99, 0x88));
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Rounded rectangle background
            g2.setColor(FIELD_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

            // Border: warm brown on focus, light grey otherwise
            g2.setColor(focused ? FIELD_FOCUS : FIELD_BORDER);
            g2.setStroke(new BasicStroke(focused ? 1.8f : 1.0f));
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

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