package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import persistence.MenuRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.*;
import util.StringUtil;

public class MenuMaintenancePanel extends JPanel {

    @FunctionalInterface
    public interface OnMenuItemSavedCallback {
        void onSaved(String itemName);
    }

    // ── Colors ──────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = new Color(0xF9F9F8);
    private static final Color BG_SURFACE = Color.WHITE;
    private static final Color BG_SUBTLE = new Color(0xF4F3F0);
    private static final Color BORDER_COLOR = new Color(0xE2E0D9);
    private static final Color TEXT_PRIMARY = new Color(0x1A1A18);
    private static final Color TEXT_MUTED = new Color(0x7A7975);
    private static final Color TEXT_HINT = new Color(0xA8A6A0);
    private static final Color ROW_SELECTED = new Color(0xEBEBF8);
    private static final Color ACCENT = new Color(0x534AB7);

    // Category pill colors: [background, foreground]
    private static final Map<String, Color[]> PILL_COLORS = new LinkedHashMap<>();
    static {
        PILL_COLORS.put("Coffee", new Color[] { new Color(0xEEEDFE), new Color(0x3C3489) });
        PILL_COLORS.put("Non-Coffee", new Color[] { new Color(0xE1F5EE), new Color(0x085041) });
        PILL_COLORS.put("Fruit Tea", new Color[] { new Color(0xFAEEDA), new Color(0x633806) });
        PILL_COLORS.put("Herbal Tea", new Color[] { new Color(0xEAF3DE), new Color(0x27500A) });
        PILL_COLORS.put("Food", new Color[] { new Color(0xFAECE7), new Color(0x712B13) });
    }

    // ── Category → image subfolder mapping ──────────────────────────────────
    // Keys must match the category strings stored in your MenuItem objects.
    // Values must match the actual folder names inside your "images/" directory.
    private static final Map<String, String[]> CATEGORY_FOLDERS = new LinkedHashMap<>();
    static {
        CATEGORY_FOLDERS.put("Coffee", new String[] { "Espresso & Coffee" });
        CATEGORY_FOLDERS.put("Non-Coffee", new String[] { "Non-Coffee", "Specialty Drinks", "Tea Latte" });
        CATEGORY_FOLDERS.put("Fruit Tea", new String[] { "Fruit Tea" });
        CATEGORY_FOLDERS.put("Herbal Tea", new String[] { "Herbal Tea" });
        CATEGORY_FOLDERS.put("Food", new String[] { "Pastries", "Sandwiches", "Pandesal Pairs" });
    }

    private static final String[] IMAGE_EXTENSIONS = { ".png", ".jpg", ".jpeg", ".webp" };

    // Toggle this to see exactly what the resolver is doing/trying in the console.
    private static final boolean DEBUG_IMAGE_RESOLUTION = true;

    // ── Icon constants ───────────────────────────────────────────────────────
    private static final String ICON_BACKUP = "\u2194";
    private static final String ICON_ADD = "+";
    private static final String ICON_EDIT = "\u25A6";
    private static final String ICON_DELETE = "\u00D7";
    private static final String ICON_MENU = "\u2261";
    private static final String ICON_SEARCH = "\u25CB ";

    // ── State ────────────────────────────────────────────────────────────────
    private final MenuRepository repo;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> categoryFilter;
    private final JTextField searchField;
    private final boolean isAdmin;
    private List<MenuItem> cachedItems = new ArrayList<>();
    private OnMenuItemSavedCallback onItemSavedCallback = null;

    // Cached resolved images root so we don't re-scan the filesystem on every
    // selection.
    private java.io.File cachedImagesRoot = null;
    private boolean imagesRootSearched = false;

    // Detail panel components
    private final JLabel detailName = makeDetailValue();
    private final JLabel detailCat = makeDetailValue();
    private final JLabel priceHot = makePriceLabel();
    private final JLabel priceReg = makePriceLabel();
    private final JLabel priceLarge = makePriceLabel();
    private final JPanel ingredientList = new JPanel();
    private final JLabel statusLabel = new JLabel("0 items");
    private final JLabel detailImage = new JLabel();

    // ── Constructor ──────────────────────────────────────────────────────────
    public MenuMaintenancePanel() throws Exception {
        this(true);
    }

    public MenuMaintenancePanel(boolean isAdmin) throws Exception {
        super(new BorderLayout());
        this.isAdmin = isAdmin;
        setBackground(BG_PAGE);
        repo = new SQLiteMenuRepository();

        model = new DefaultTableModel(
                new String[] { "Name", "Category", "Hot", "Iced Regular", "Iced Large" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = buildTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateDetailPanel();
        });

        categoryFilter = new JComboBox<>(
                new String[] { "All", "Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food" });
        searchField = new JTextField(22);
        styleComboBox(categoryFilter);
        styleTextField(searchField, "Search items\u2026");

        add(buildMenuPanel(), BorderLayout.CENTER);
        reload();
    }

    public void setOnMenuItemSavedCallback(OnMenuItemSavedCallback callback) {
        this.onItemSavedCallback = callback;
    }

    // ── Main Panel ────────────────────────────────────────────────────────────
    private JPanel buildMenuPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_PAGE);
        panel.add(buildToolbar(), BorderLayout.NORTH);
        panel.add(buildContentArea(), BorderLayout.CENTER);
        panel.add(buildStatusBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 14, 10, 14)));

        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        filters.add(makeIconLabel(ICON_MENU));
        filters.add(categoryFilter);
        filters.add(Box.createHorizontalStrut(4));
        filters.add(makeSearchBox());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        if (isAdmin) {
            JButton addBtn = makeButton(ICON_ADD + " Add", true, false);
            JButton editBtn = makeButton(ICON_EDIT + " Edit", false, false);
            JButton deleteBtn = makeButton(ICON_DELETE + " Delete", false, true);

            addBtn.addActionListener(e -> onAdd());
            editBtn.addActionListener(e -> onEdit());
            deleteBtn.addActionListener(e -> onDelete());

            actions.add(addBtn);
            actions.add(editBtn);
            actions.add(deleteBtn);
        }

        categoryFilter.addActionListener(e -> applyFilters());
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(DocumentEvent e) {
                applyFilters();
            }
        });

        bar.add(filters, BorderLayout.CENTER);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel makeSearchBox() {
        JPanel wrap = new JPanel(new BorderLayout(0, 0));
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(220, 32));
        JLabel icon = new JLabel(ICON_SEARCH);
        icon.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        icon.setForeground(TEXT_HINT);
        wrap.add(icon, BorderLayout.WEST);
        wrap.add(searchField, BorderLayout.CENTER);
        return wrap;
    }

    // ── Content Area ──────────────────────────────────────────────────────────
    private JPanel buildContentArea() {
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(BG_SURFACE);

        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BORDER_COLOR);
        content.add(tableScroll, BorderLayout.CENTER);
        content.add(buildDetailPanel(), BorderLayout.EAST);
        return content;
    }

    // ── Detail Panel ──────────────────────────────────────────────────────────
    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SURFACE);
        panel.setBorder(new MatteBorder(0, 1, 0, 0, BORDER_COLOR));
        panel.setPreferredSize(new Dimension(270, 0));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SUBTLE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 18, 10, 12)));
        JLabel title = new JLabel("Item Details");
        title.setFont(new Font(Font.DIALOG, Font.BOLD, 12));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        JPanel body = buildDetailBody();
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_SURFACE);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        panel.add(buildDetailFooter(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildDetailBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_SURFACE);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        // ── Image placeholder ──────────────────────────────────────────────
        detailImage.setAlignmentX(LEFT_ALIGNMENT);
        detailImage.setVisible(false);
        detailImage.setHorizontalAlignment(SwingConstants.CENTER);
        detailImage.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(0, 0, 0, 0)));
        // Fix preferred size so the panel doesn't jump when an image loads
        detailImage.setPreferredSize(new Dimension(242, 130));
        detailImage.setMaximumSize(new Dimension(242, 130));
        body.add(detailImage);
        body.add(Box.createVerticalStrut(10));

        body.add(makeFieldRow("NAME", detailName));
        body.add(Box.createVerticalStrut(10));
        body.add(makeFieldRow("CATEGORY", detailCat));
        body.add(Box.createVerticalStrut(12));

        body.add(makeDetailLabel("PRICING"));
        body.add(Box.createVerticalStrut(5));
        body.add(buildPriceChips());
        body.add(Box.createVerticalStrut(14));

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(sep);
        body.add(Box.createVerticalStrut(14));

        body.add(makeDetailLabel("INGREDIENTS"));
        body.add(Box.createVerticalStrut(6));

        ingredientList.setLayout(new BoxLayout(ingredientList, BoxLayout.Y_AXIS));
        ingredientList.setBackground(BG_SURFACE);
        ingredientList.setAlignmentX(LEFT_ALIGNMENT);
        body.add(ingredientList);
        body.add(Box.createVerticalGlue());

        setDetailEmpty();
        return body;
    }

    private JPanel buildDetailFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(BG_SUBTLE);
        footer.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(8, 12, 8, 12)));

        JButton backupBtn = new JButton(ICON_BACKUP + "  Backup & Restore") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BG_SUBTLE : BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        backupBtn.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        backupBtn.setForeground(TEXT_MUTED);
        backupBtn.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        backupBtn.setOpaque(false);
        backupBtn.setContentAreaFilled(false);
        backupBtn.setFocusPainted(false);
        backupBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        backupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        backupBtn.addActionListener(e -> openBackupDialog());

        footer.add(backupBtn, BorderLayout.CENTER);
        return footer;
    }

    private void openBackupDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Backup & Restore",
                Dialog.ModalityType.APPLICATION_MODAL);
        dlg.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        try {
            dlg.getContentPane().add(new BackupPanel());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Could not open Backup panel: " + ex.getMessage());
            return;
        }
        dlg.pack();
        dlg.setMinimumSize(new Dimension(680, 520));
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
    }

    private JPanel buildPriceChips() {
        JPanel row = new JPanel(new GridLayout(1, 3, 6, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 52));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(buildPriceChip("HOT", priceHot));
        row.add(buildPriceChip("ICED REG", priceReg));
        row.add(buildPriceChip("ICED LG", priceLarge));
        return row;
    }

    private JPanel buildPriceChip(String label, JLabel valueLabel) {
        JPanel chip = new JPanel(new BorderLayout(0, 2));
        chip.setBackground(BG_SUBTLE);
        chip.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(6, 6, 6, 6)));
        JLabel lbl = new JLabel(label, SwingConstants.CENTER);
        lbl.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
        lbl.setForeground(TEXT_HINT);
        valueLabel.setHorizontalAlignment(SwingConstants.CENTER);
        chip.add(lbl, BorderLayout.NORTH);
        chip.add(valueLabel, BorderLayout.CENTER);
        return chip;
    }

    private JPanel makeFieldRow(String labelText, JLabel value) {
        JPanel row = new JPanel(new BorderLayout(0, 3));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        row.add(makeDetailLabel(labelText), BorderLayout.NORTH);
        row.add(value, BorderLayout.CENTER);
        return row;
    }

    private void setDetailEmpty() {
        detailName.setText("\u2014");
        detailCat.setText("\u2014");
        priceHot.setText("\u2014");
        priceReg.setText("\u2014");
        priceLarge.setText("\u2014");
        detailImage.setIcon(null);
        detailImage.setVisible(false);
        ingredientList.removeAll();
        JLabel none = new JLabel("Select a menu item");
        none.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
        none.setForeground(TEXT_HINT);
        ingredientList.add(none);
        ingredientList.revalidate();
        ingredientList.repaint();
    }

    // ── Image resolution ──────────────────────────────────────────────────────
    /**
     * Resolves the best image file for a menu item.
     *
     * Strategy (in priority order):
     * 1. item.getImagePath() — stored absolute/relative path (non-blank wins
     * immediately, as long as the file actually exists).
     * 2. Auto-search in images/&lt;CategoryFolder&gt;/ for a file whose normalised
     * name matches the item name, preferring a "Hot " / "Iced " prefixed file
     * that matches whichever price variants the item actually has.
     *
     * "Normalised" means lower-cased and stripped of everything that is not a-z
     * or 0-9, so "Caramel Macchiato" matches "caramel-macchiato.png",
     * "Hot Caramel Macchiato.jpg", "Iced Caramel Macchiato.jpg" etc.
     */
    private String resolveImagePath(MenuItem item) {
        // 1. Honour whatever path is already stored on the item
        String stored = item.getImagePath();
        if (stored != null && !stored.isBlank()) {
            java.io.File f = new java.io.File(stored);
            if (f.exists() && f.isFile()) {
                debugLog("Using stored image path for '" + item.getName() + "': " + stored);
                return f.getAbsolutePath();
            }
            debugLog("Stored image path for '" + item.getName() + "' does not exist on disk: " + stored);
        }

        // 2. Auto-resolve from the images/ folder tree
        java.io.File imagesRoot = findImagesRoot();
        if (imagesRoot == null)
            return null;

        String[] folders = CATEGORY_FOLDERS.getOrDefault(item.getCategory(), new String[] {});
        String nameNorm = normalize(item.getName());

        boolean hasHot = item.getHotPrice() > 0;
        boolean hasIced = item.getIcedRegularPrice() > 0 || item.getIcedLargePrice() > 0;
        List<String> preferredPrefixes = new ArrayList<>();
        if (hasHot)
            preferredPrefixes.add("hot");
        if (hasIced)
            preferredPrefixes.add("iced");
        if (preferredPrefixes.isEmpty()) {
            preferredPrefixes.add("hot");
            preferredPrefixes.add("iced");
        }

        for (String folder : folders) {
            java.io.File dir = new java.io.File(imagesRoot, folder);
            if (!dir.exists() || !dir.isDirectory())
                continue;

            java.io.File[] files = dir.listFiles();
            if (files == null || files.length == 0)
                continue;

            List<java.io.File> candidates = new ArrayList<>();
            for (java.io.File f : files) {
                if (!f.isFile())
                    continue;
                String fname = f.getName();
                boolean hasExt = false;
                for (String ext : IMAGE_EXTENSIONS) {
                    if (fname.toLowerCase().endsWith(ext)) {
                        hasExt = true;
                        break;
                    }
                }
                if (!hasExt)
                    continue;

                String fnameNoPrefix = stripLeadingTempWord(normalize(stripExtension(fname)));
                if (fnameNoPrefix.equals(nameNorm) || fnameNoPrefix.startsWith(nameNorm)
                        || fnameNoPrefix.contains(nameNorm)) {
                    candidates.add(f);
                }
            }

            if (candidates.isEmpty())
                continue;

            java.io.File best = null;
            outer: for (String prefix : preferredPrefixes) {
                for (java.io.File c : candidates) {
                    if (normalize(stripExtension(c.getName())).startsWith(prefix)) {
                        best = c;
                        break outer;
                    }
                }
            }
            if (best == null) {
                for (java.io.File c : candidates) {
                    if (normalize(stripExtension(c.getName())).equals(nameNorm)) {
                        best = c;
                        break;
                    }
                }
            }
            if (best == null)
                best = candidates.get(0);

            return best.getAbsolutePath();
        }

        return null;
    }

    /**
     * Finds the images/ root directory. Checks multiple candidate locations so
     * the app works whether launched from the project root, an out/ or bin/
     * build folder, or an IDE run configuration — then walks a few levels up
     * from the working directory as a last resort. Result is cached after the
     * first successful (or failed) lookup.
     */
    private java.io.File findImagesRoot() {
        if (imagesRootSearched)
            return cachedImagesRoot;
        imagesRootSearched = true;

        String sep = java.io.File.separator;
        java.net.URL classUrl = getClass().getProtectionDomain().getCodeSource().getLocation();
        List<String> candidates = new ArrayList<>();

        if (classUrl != null) {
            try {
                java.io.File classRoot = new java.io.File(classUrl.toURI());
                java.io.File cursor = classRoot;
                for (int i = 0; i < 6 && cursor != null; i++) {
                    candidates.add(cursor.getPath() + sep + "resources" + sep + "images");
                    candidates.add(cursor.getPath() + sep + "src" + sep + "main" + sep + "resources" + sep + "images");
                    candidates.add(cursor.getPath() + sep + "images");
                    cursor = cursor.getParentFile();
                }
            } catch (Exception ignored) {
            }
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null) {
            candidates.add(userDir + sep + "resources" + sep + "images");
            candidates.add(userDir + sep + "src" + sep + "main" + sep + "resources" + sep + "images");
            candidates.add(userDir + sep + "images");

            java.io.File cursor = new java.io.File(userDir);
            for (int i = 0; i < 4 && cursor != null; i++) {
                candidates.add(cursor.getPath() + sep + "resources" + sep + "images");
                candidates.add(cursor.getPath() + sep + "images");
                cursor = cursor.getParentFile();
            }
        }

        for (String path : candidates) {
            if (path == null)
                continue;
            java.io.File dir = new java.io.File(path);
            if (dir.exists() && dir.isDirectory()) {
                cachedImagesRoot = dir;
                return dir;
            }
        }

        return null;
    }

    /** Lower-cases and keeps only a-z / 0-9 characters. */
    private static String normalize(String s) {
        return s == null ? "" : s.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    /** Strips the file extension, e.g. "latte.png" → "latte". */
    private static String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? filename : filename.substring(0, dot);
    }

    /** Strips a leading "hot" or "iced" token from an already-normalized string. */
    private static String stripLeadingTempWord(String normalized) {
        if (normalized.startsWith("hot"))
            return normalized.substring(3);
        if (normalized.startsWith("iced"))
            return normalized.substring(4);
        return normalized;
    }

    private static void debugLog(String msg) {
        if (DEBUG_IMAGE_RESOLUTION)
            System.out.println("[MenuMaintenance/Image] " + msg);
    }

    // ── Detail panel population ───────────────────────────────────────────────
    private void updateDetailPanel() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount()) {
            setDetailEmpty();
            return;
        }
        String name = String.valueOf(model.getValueAt(row, 0));
        Optional<MenuItem> opt = cachedItems.stream()
                .filter(i -> StringUtil.normalizeName(i.getName())
                        .equals(StringUtil.normalizeName(name)))
                .findFirst();
        if (opt.isEmpty()) {
            setDetailEmpty();
            return;
        }
        MenuItem item = opt.get();

        detailName.setText(item.getName());
        detailCat.setText(item.getCategory());
        priceHot.setText(item.getHotPrice() > 0 ? "\u20B1" + (int) item.getHotPrice() : "\u2014");
        priceReg.setText(item.getIcedRegularPrice() > 0 ? "\u20B1" + (int) item.getIcedRegularPrice() : "\u2014");
        priceLarge.setText(item.getIcedLargePrice() > 0 ? "\u20B1" + (int) item.getIcedLargePrice() : "\u2014");

        // ── Image loading (auto-resolved) ────────────────────────────────────
        loadItemImage(item);

        // ── Ingredients ──────────────────────────────────────────────────────
        ingredientList.removeAll();
        if (item.getIngredients().isEmpty()) {
            JLabel none = new JLabel("None configured");
            none.setFont(new Font(Font.DIALOG, Font.ITALIC, 12));
            none.setForeground(TEXT_HINT);
            ingredientList.add(none);
        } else {
            item.getIngredients().forEach((ing, qty) -> ingredientList.add(buildIngRow(ing, qty)));
        }
        ingredientList.revalidate();
        ingredientList.repaint();
    }

    /**
     * Resolves, scales, and displays the image for {@code item} in
     * {@link #detailImage}.
     * Hides the label when no usable image is found.
     *
     * The method runs the file I/O on a background thread so the UI stays
     * responsive while large images are decoded. The label is updated back on
     * the EDT. Each invocation is tagged with the requesting item's name so a
     * slow/stale background load can never overwrite the panel for a item the
     * user has since deselected (fixes a race when clicking through rows fast).
     */
    private volatile String pendingImageRequestFor = null;

    private void loadItemImage(MenuItem item) {
        final String requestedFor = item.getName();
        pendingImageRequestFor = requestedFor;

        // Show a "loading" placeholder immediately so the panel doesn't shift
        detailImage.setIcon(null);
        detailImage.setText("\u25A2"); // ▢ light placeholder glyph
        detailImage.setForeground(BORDER_COLOR);
        detailImage.setVisible(true);

        SwingWorker<ImageIcon, Void> worker = new SwingWorker<>() {
            @Override
            protected ImageIcon doInBackground() {
                String imgPath = resolveImagePath(item);
                if (imgPath == null || imgPath.isBlank())
                    return null;

                java.io.File file = new java.io.File(imgPath);
                if (!file.exists() || !file.isFile()) {
                    debugLog("Resolved path does not exist at load time: " + imgPath);
                    return null;
                }

                try {
                    java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(file);
                    if (img == null) {
                        debugLog("ImageIO could not decode file (unsupported format?): " + imgPath);
                        return null;
                    }

                    // Scale to fit inside the 242×130 chip while keeping aspect ratio
                    int tw = 242, th = 130;
                    double scale = Math.min((double) tw / img.getWidth(),
                            (double) th / img.getHeight());
                    int iw = Math.max(1, (int) (img.getWidth() * scale));
                    int ih = Math.max(1, (int) (img.getHeight() * scale));

                    java.awt.image.BufferedImage scaled = new java.awt.image.BufferedImage(iw, ih,
                            java.awt.image.BufferedImage.TYPE_INT_ARGB);
                    java.awt.Graphics2D g2 = scaled.createGraphics();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                            java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.drawImage(img, 0, 0, iw, ih, null);
                    g2.dispose();

                    return new ImageIcon(scaled);
                } catch (Exception ex) {
                    System.err.println("[MenuMaintenance] Could not load image: " + ex.getMessage());
                    return null;
                }
            }

            @Override
            protected void done() {
                // If the user has selected a different row while this was loading,
                // discard the result instead of stomping on the newer request.
                if (!requestedFor.equals(pendingImageRequestFor))
                    return;
                try {
                    ImageIcon icon = get();
                    if (icon != null) {
                        detailImage.setIcon(icon);
                        detailImage.setText(null);
                        detailImage.setVisible(true);
                    } else {
                        // No image available — hide the slot entirely
                        detailImage.setIcon(null);
                        detailImage.setText(null);
                        detailImage.setVisible(false);
                    }
                } catch (Exception ignored) {
                    detailImage.setIcon(null);
                    detailImage.setText(null);
                    detailImage.setVisible(false);
                }
            }
        };
        worker.execute();
    }

    private JPanel buildIngRow(String name, double qty) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_SUBTLE);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(6, 8, 6, 8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        row.setAlignmentX(LEFT_ALIGNMENT);
        JLabel n = new JLabel(name);
        n.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        n.setForeground(TEXT_PRIMARY);
        JLabel q = new JLabel(qty % 1 == 0 ? (int) qty + "g" : qty + "g");
        q.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        q.setForeground(TEXT_MUTED);
        row.add(n, BorderLayout.WEST);
        row.add(q, BorderLayout.EAST);
        return row;
    }

    // ── Status Bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SUBTLE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(5, 14, 5, 14)));
        statusLabel.setFont(new Font(Font.DIALOG, Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_HINT);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ── Table ─────────────────────────────────────────────────────────────────
    private JTable buildTable() {
        JTable t = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                boolean sel = isRowSelected(row);
                c.setBackground(sel ? ROW_SELECTED : (row % 2 == 0 ? BG_SURFACE : BG_PAGE));
                c.setForeground(col == 0 ? TEXT_PRIMARY : TEXT_MUTED);
                return c;
            }
        };
        t.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(ROW_SELECTED);
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setBackground(BG_SURFACE);
        t.setForeground(TEXT_PRIMARY);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JTableHeader header = t.getTableHeader();
        header.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
        header.setBackground(BG_SUBTLE);
        header.setForeground(TEXT_MUTED);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

        int[] widths = { 200, 110, 75, 100, 95 };
        for (int i = 0; i < widths.length; i++)
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 2; i <= 4; i++)
            t.getColumnModel().getColumn(i).setCellRenderer(right);

        t.getColumnModel().getColumn(1).setCellRenderer(new CategoryPillRenderer());

        t.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            int lastHover = -1;

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int r = t.rowAtPoint(e.getPoint());
                if (r != lastHover) {
                    lastHover = r;
                    t.repaint();
                }
            }
        });
        return t;
    }

    // ── Data ops ──────────────────────────────────────────────────────────────
    private void reload() {
        try {
            cachedItems = repo.findAll();
            if (cachedItems == null)
                cachedItems = new ArrayList<>();
            if (cachedItems.isEmpty()) {
                try {
                    persistence.Phase2Bootstrap.seedCatalogIfEmpty();
                    cachedItems = repo.findAll();
                    if (cachedItems == null)
                        cachedItems = new ArrayList<>();
                } catch (Exception seedEx) {
                    System.err.println("Warning: Could not seed catalog: " + seedEx.getMessage());
                }
            }
            applyFilters();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Unable to load menu: " + e.getMessage());
            cachedItems = new ArrayList<>();
        }
    }

    private void applyFilters() {
        model.setRowCount(0);
        String cat = categoryFilter.getSelectedItem() == null
                ? "All"
                : categoryFilter.getSelectedItem().toString();
        String fieldText = searchField.getText() == null ? "" : searchField.getText().trim();
        String q = (fieldText.equals("Search items\u2026") || fieldText.equals("Search items…"))
                ? ""
                : fieldText.toLowerCase();
        int count = 0;

        if (cachedItems != null) {
            for (MenuItem item : cachedItems) {
                boolean catOk = "All".equalsIgnoreCase(cat)
                        || normalizeCategory(item.getCategory())
                                .equalsIgnoreCase(normalizeCategory(cat));
                boolean srchOk = q.isEmpty()
                        || item.getName().toLowerCase().contains(q)
                        || item.getCategory().toLowerCase().contains(q);
                if (catOk && srchOk) {
                    model.addRow(new Object[] {
                            item.getName(), item.getCategory(),
                            formatPrice(item.getHotPrice()),
                            formatPrice(item.getIcedRegularPrice()),
                            formatPrice(item.getIcedLargePrice())
                    });
                    count++;
                }
            }
        }
        statusLabel.setText(count + " item" + (count == 1 ? "" : "s"));
        if (model.getRowCount() > 0) {
            table.setRowSelectionInterval(0, 0);
            updateDetailPanel();
        } else {
            setDetailEmpty();
        }
    }

    private String formatPrice(double p) {
        return p > 0 ? "\u20B1" + (int) p : "\u2014";
    }

    private void onAdd() {
        MenuItemDialog dlg = new MenuItemDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed())
            return;
        try {
            MenuItem item = createMenuItem(
                    normalizeCategory(dlg.getCategory().trim()), dlg.getName().trim(),
                    dlg.getHot(), dlg.getIcedRegular(), dlg.getIcedLarge());
            applyIngredientsToItem(item, dlg);
            if (dlg.getImagePath() != null)
                item.setImagePath(dlg.getImagePath());
            Menu.getInstance().saveItem(item);
            if (onItemSavedCallback != null)
                onItemSavedCallback.onSaved(item.getName());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error adding item: " + ex.getMessage());
        }
    }

    private void onEdit() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to edit.");
            return;
        }
        String name = model.getValueAt(r, 0).toString();
        try {
            Optional<MenuItem> opt = repo.findByName(StringUtil.normalizeName(name));
            if (opt.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Item not found.");
                return;
            }
            MenuItem item = opt.get();
            MenuItemDialog dlg = new MenuItemDialog(SwingUtilities.getWindowAncestor(this), item);
            dlg.setLocationRelativeTo(this);
            dlg.setVisible(true);
            if (!dlg.isConfirmed())
                return;
            MenuItem edited = createMenuItem(
                    normalizeCategory(dlg.getCategory().trim()),
                    StringUtil.normalizeName(dlg.getName().trim()),
                    dlg.getHot(), dlg.getIcedRegular(), dlg.getIcedLarge());
            applyIngredientsToItem(edited, dlg);
            if (dlg.getImagePath() != null)
                edited.setImagePath(dlg.getImagePath());
            else
                edited.setImagePath(item.getImagePath());
            if (!StringUtil.normalizeName(name).equals(StringUtil.normalizeName(dlg.getName().trim())))
                Menu.getInstance().removeItem(name);
            Menu.getInstance().saveItem(edited);
            if (onItemSavedCallback != null)
                onItemSavedCallback.onSaved(edited.getName());
            reload();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error editing item: " + ex.getMessage());
        }
    }

    private void onDelete() {
        int r = table.getSelectedRow();
        if (r < 0) {
            JOptionPane.showMessageDialog(this, "Select a row to delete.");
            return;
        }
        String name = model.getValueAt(r, 0).toString();
        int confirm = JOptionPane.showConfirmDialog(this,
                "Delete \"" + name + "\"? This cannot be undone.",
                "Delete item", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;
        try {
            Menu.getInstance().removeItem(name);
            if (onItemSavedCallback != null)
                onItemSavedCallback.onSaved(null);
            reload();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Delete failed: " + e.getMessage());
        }
    }

    private void applyIngredientsToItem(MenuItem item, MenuItemDialog dlg) {
        Map<String, Double> ingMap = dlg.getIngredientsMap();
        if (ingMap != null && !ingMap.isEmpty()) {
            ingMap.forEach((k, v) -> {
                try {
                    item.addIngredient(k, v);
                } catch (Exception ignored) {
                }
            });
        } else {
            parseAndSetIngredients(item, dlg.getIngredientsCsv());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private MenuItem createMenuItem(String category, String name,
            double hot, double reg, double large) {
        return switch (category) {
            case "Coffee" -> new CoffeeItem(name, hot, reg, large);
            case "Non-Coffee" -> new NonCoffeeItem(name, hot, reg, large);
            case "Fruit Tea" -> new FruitTeaItem(name, hot, reg, large);
            case "Herbal Tea" -> new HerbalTeaItem(name, hot, reg, large);
            case "Food" -> new FoodItem(name, "Food",
                    hot > 0 ? hot : (reg > 0 ? reg : large));
            default -> new CoffeeItem(name, hot, reg, large);
        };
    }

    private String normalizeCategory(String category) {
        if (category == null)
            return "Coffee";
        return switch (category.trim()) {
            case "NonCoffee" -> "Non-Coffee";
            case "FruitTea" -> "Fruit Tea";
            case "HerbalTea" -> "Herbal Tea";
            default -> category.trim();
        };
    }

    private void parseAndSetIngredients(MenuItem item, String csv) {
        if (csv == null || csv.trim().isEmpty())
            return;
        for (String p : csv.split(Pattern.quote(","))) {
            String[] kv = p.split(":", 2);
            if (kv.length == 2) {
                try {
                    item.addIngredient(StringUtil.normalizeName(kv[0].trim()),
                            Double.parseDouble(kv[1].trim()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ── Factory helpers ───────────────────────────────────────────────────────
    private static JLabel makeDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.DIALOG, Font.BOLD, 10));
        l.setForeground(TEXT_HINT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel makeDetailValue() {
        JLabel l = new JLabel("\u2014");
        l.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        l.setForeground(TEXT_PRIMARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel makePriceLabel() {
        JLabel l = new JLabel("\u2014");
        l.setFont(new Font(Font.DIALOG, Font.BOLD, 13));
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private static JLabel makeIconLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(Font.DIALOG, Font.PLAIN, 14));
        l.setForeground(TEXT_HINT);
        return l;
    }

    private static void styleTextField(JTextField field, String placeholder) {
        field.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_SURFACE);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(5, 8, 5, 8)));
        field.setPreferredSize(new Dimension(200, 30));
        field.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override
            public void focusGained(java.awt.event.FocusEvent e) {
                if (field.getText().equals(placeholder)) {
                    field.setText("");
                    field.setForeground(TEXT_PRIMARY);
                }
            }

            @Override
            public void focusLost(java.awt.event.FocusEvent e) {
                if (field.getText().isEmpty()) {
                    field.setText(placeholder);
                    field.setForeground(TEXT_HINT);
                }
            }
        });
        field.setText(placeholder);
        field.setForeground(TEXT_HINT);
    }

    private static void styleComboBox(JComboBox<?> box) {
        box.setFont(new Font(Font.DIALOG, Font.PLAIN, 13));
        box.setBackground(BG_SURFACE);
        box.setForeground(TEXT_PRIMARY);
        box.setBorder(BorderFactory.createLineBorder(BORDER_COLOR));
        box.setPreferredSize(new Dimension(130, 30));
    }

    private static JButton makeButton(String text, boolean primary, boolean danger) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                Color bg;
                if (primary)
                    bg = ACCENT;
                else if (danger)
                    bg = getModel().isRollover() ? new Color(0xFCEBEB) : BG_SURFACE;
                else
                    bg = getModel().isRollover() ? BG_SUBTLE : BG_SURFACE;
                g2.setColor(bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setFont(new Font(Font.DIALOG, Font.PLAIN, 12));
        btn.setForeground(primary ? Color.WHITE : (danger ? new Color(0xA32D2D) : TEXT_PRIMARY));
        btn.setBorder(new CompoundBorder(
                new LineBorder(primary ? ACCENT : BORDER_COLOR, 1, true),
                new EmptyBorder(5, 14, 5, 14)));
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(86, 30));
        return btn;
    }

    // ── Category pill renderer ────────────────────────────────────────────────
    private static class CategoryPillRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, col);
            String cat = value == null ? "" : value.toString();
            Color[] colors = PILL_COLORS.getOrDefault(cat, new Color[] { BG_SUBTLE, TEXT_MUTED });
            label.setOpaque(true);
            label.setBackground(isSelected ? ROW_SELECTED : colors[0]);
            label.setForeground(colors[1]);
            label.setFont(new Font(Font.DIALOG, Font.BOLD, 11));
            label.setBorder(new EmptyBorder(2, 8, 2, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }
}