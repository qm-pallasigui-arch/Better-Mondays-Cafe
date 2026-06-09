package ui;

import pos.MenuItem;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Add / Edit dialog for menu items.
 * Fixed 780×660 dialog. Two-column layout: image left, form right.
 * Ingredient list container + pre-populated Packaging container.
 */
public class MenuItemDialog extends JDialog {

    // ── Palette ───────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = new Color(0xF9F9F8);
    private static final Color BG_SURFACE = Color.WHITE;
    private static final Color BG_SUBTLE = new Color(0xF4F3F0);
    private static final Color BG_PKG = new Color(0xF0F5FF);
    private static final Color BORDER = new Color(0xE2E0D9);
    private static final Color BORDER_PKG = new Color(0xC5D5F5);
    private static final Color TEXT_PRI = new Color(0x1A1A18);
    private static final Color TEXT_MUTED = new Color(0x7A7975);
    private static final Color TEXT_HINT = new Color(0xA8A6A0);
    private static final Color ACCENT = new Color(0x534AB7);
    private static final Color ACCENT_HVR = new Color(0x3D379A);
    private static final Color ACCENT_LIGHT = new Color(0xEEEDF9);
    private static final Color PKG_ACCENT = new Color(0x3B6FD4);
    private static final Color DANGER = new Color(0xA32D2D);

    private static final String[] CATEGORIES = { "Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food" };
    private static final String[] PACKAGING_DEFAULTS = { "Cup", "Cup Holder", "Lid", "Straw" };
    private static final Path RESOURCE_IMAGES_DIR = Paths.get("src", "main", "resources", "images");

    // Fixed dialog size
    private static final int DLG_W = 780;
    private static final int DLG_H = 720;

    // ── Fields ────────────────────────────────────────────────────────────────
    private final JTextField nameField = styledField();
    private final JComboBox<String> catBox = new JComboBox<>(CATEGORIES);
    private final JTextField hotField = styledField();
    private final JTextField regField = styledField();
    private final JTextField lgField = styledField();

    private final JComboBox<String> ingPickerBox = new JComboBox<>();
    private final JTextField ingNameField = styledField();
    private final JTextField ingQtyField = styledField();

    private final JPanel ingListPanel = new JPanel();
    private final JPanel pkgListPanel = new JPanel();

    private final LinkedHashMap<String, JTextField> ingRows = new LinkedHashMap<>();
    private final LinkedHashMap<String, JTextField> pkgRows = new LinkedHashMap<>();

    private ImageDropZone dropZone;
    private String selectedImagePath = null;
    private boolean confirmed = false;

    // ── Constructor ───────────────────────────────────────────────────────────
    public MenuItemDialog(Window owner, MenuItem existing) {
        super(owner,
                existing == null ? "Add Menu Item" : "Edit Menu Item",
                ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setUndecorated(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_PAGE);
        root.add(buildHeader(existing), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);
        setContentPane(root);

        addDefaultPackaging();
        if (existing != null)
            populate(existing);
        else
            refreshIngEmptyState();

        setSize(DLG_W, DLG_H);
        setPreferredSize(new Dimension(DLG_W, DLG_H));
        setMinimumSize(new Dimension(DLG_W, DLG_H));
        setMaximumSize(new Dimension(DLG_W, DLG_H));
        setLocationRelativeTo(owner);
    }

    // ── Header ────────────────────────────────────────────────────────────────
    private JPanel buildHeader(MenuItem existing) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(BG_SURFACE);
        h.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(14, 20, 14, 20)));

        JLabel title = new JLabel(existing == null ? "New Menu Item" : "Edit — " + existing.getName());
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(TEXT_PRI);

        JLabel sub = new JLabel(existing == null
                ? "Fill in the details below and optionally add a photo."
                : "Update the fields you want to change.");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        sub.setForeground(TEXT_MUTED);

        JPanel txt = new JPanel(new GridLayout(2, 1, 0, 2));
        txt.setOpaque(false);
        txt.add(title);
        txt.add(sub);
        h.add(txt, BorderLayout.WEST);
        return h;
    }

    // ── Body ─────────────────────────────────────────────────────────────────
    private JPanel buildBody() {
        // Fixed-size body so everything is predictable
        JPanel body = new JPanel(null); // absolute layout for pixel-perfect sizing
        body.setBackground(BG_PAGE);
        body.setPreferredSize(new Dimension(DLG_W, DLG_H - 60 - 58)); // minus header & footer

        // Drop zone: left column, fixed 190×190, top-aligned
        dropZone = new ImageDropZone();
        dropZone.setBounds(20, 16, 190, 190);
        body.add(dropZone);

        // Right column form, starts at x=228
        int fx = 228, fy = 16, fw = DLG_W - fx - 20;
        body.add(buildFormPanel(fx, fy, fw));

        return body;
    }

    // ── Form panel (absolute positioned inside body) ──────────────────────────
    private JPanel buildFormPanel(int x, int y, int w) {
        // Use a real layout inside the form panel (BoxLayout)
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(BG_PAGE);
        p.setBounds(x, y, w, DLG_H - 60 - 58 - 16); // fill remaining height

        // ── Row 1: Name + Category ────────────────────────────────────────────
        styleComboBox(catBox);
        JPanel r1 = fixedRow(new GridLayout(1, 2, 10, 0), w, 54);
        r1.add(field("Name *", nameField));
        r1.add(field("Category *", catBox));
        p.add(r1);
        p.add(vgap(8));

        // ── Row 2: Three prices ───────────────────────────────────────────────
        JPanel r2 = fixedRow(new GridLayout(1, 3, 10, 0), w, 54);
        r2.add(field("Hot (₱)", hotField));
        r2.add(field("Iced Regular (₱)", regField));
        r2.add(field("Iced Large (₱)", lgField));
        p.add(r2);
        p.add(vgap(3));

        JLabel hint = new JLabel("Leave a price blank or 0 if not offered");
        hint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        hint.setForeground(TEXT_HINT);
        hint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(hint);
        p.add(vgap(10));

        // Divider
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        p.add(sep);
        p.add(vgap(10));

        // ── Ingredients section ───────────────────────────────────────────────
        p.add(sectionPill("INGREDIENTS", ACCENT, ACCENT_LIGHT));
        p.add(vgap(6));
        p.add(buildIngAdder());
        p.add(vgap(6));

        ingListPanel.setLayout(new BoxLayout(ingListPanel, BoxLayout.Y_AXIS));
        ingListPanel.setBackground(BG_SURFACE);
        JScrollPane ingScroll = listScroll(ingListPanel, BORDER, BG_SURFACE);
        ingScroll.setPreferredSize(new Dimension(w, 130));
        ingScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        ingScroll.setAlignmentX(LEFT_ALIGNMENT);
        p.add(ingScroll);
        p.add(vgap(12));

        // ── Packaging section ─────────────────────────────────────────────────
        p.add(sectionPill("PACKAGING / MATERIALS", PKG_ACCENT, new Color(0xDEEAFF)));
        p.add(vgap(3));

        JLabel pkgHint = new JLabel("Cup, Cup Holder, Lid & Straw are included by default.");
        pkgHint.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        pkgHint.setForeground(TEXT_HINT);
        pkgHint.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pkgHint);
        p.add(vgap(6));

        pkgListPanel.setLayout(new BoxLayout(pkgListPanel, BoxLayout.Y_AXIS));
        pkgListPanel.setBackground(BG_PKG);
        JScrollPane pkgScroll = listScroll(pkgListPanel, BORDER_PKG, BG_PKG);
        pkgScroll.setPreferredSize(new Dimension(w, 168));
        pkgScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 168));
        pkgScroll.setAlignmentX(LEFT_ALIGNMENT);
        p.add(pkgScroll);

        return p;
    }

    // ── Ingredient adder ──────────────────────────────────────────────────────
    private JPanel buildIngAdder() {
        String[] known = {
                "Coffee Beans", "Milk", "Water", "Sweetener", "Espresso Shot",
                "Matcha Powder", "Chocolate Syrup", "Caramel Syrup", "Vanilla Syrup",
                "Strawberry Syrup", "Mango Syrup", "Passion Fruit Syrup", "Peach Syrup",
                "Ube Syrup", "Green Tea", "Chamomile", "Peppermint Leaves", "Earl Grey", "Cinnamon"
        };
        ingPickerBox.addItem("— pick existing —");
        for (String k : known)
            ingPickerBox.addItem(k);
        styleComboBox(ingPickerBox);
        ingPickerBox.setPreferredSize(new Dimension(148, 28));
        ingPickerBox.addActionListener(e -> {
            Object s = ingPickerBox.getSelectedItem();
            if (s != null && !s.toString().startsWith("—")) {
                ingNameField.setText(s.toString());
                ingQtyField.requestFocus();
            }
        });

        ingNameField.setPreferredSize(new Dimension(110, 28));
        ingQtyField.setPreferredSize(new Dimension(50, 28));
        ingQtyField.addActionListener(e -> addIngRow());

        JButton add = accentButton("+ Add");
        add.addActionListener(e -> addIngRow());

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        row.setOpaque(false);
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(ingPickerBox);
        row.add(muted("or"));
        row.add(ingNameField);
        row.add(ingQtyField);
        row.add(muted("g"));
        row.add(add);
        return row;
    }

    private void addIngRow() {
        String name = ingNameField.getText().trim();
        String qtyStr = ingQtyField.getText().trim();
        if (name.isEmpty()) {
            ingNameField.requestFocus();
            return;
        }

        for (String pkg : PACKAGING_DEFAULTS)
            if (pkg.equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this,
                        "\"" + name + "\" is in the Packaging/Materials section.",
                        "Already in Packaging", JOptionPane.INFORMATION_MESSAGE);
                return;
            }

        double qty = 0;
        try {
            qty = Double.parseDouble(qtyStr);
        } catch (NumberFormatException ignored) {
        }

        if (ingRows.containsKey(name)) {
            ingRows.get(name).setText(qty == 0 ? "" : String.valueOf(qty));
            clearAdder();
            return;
        }

        if (ingRows.isEmpty())
            ingListPanel.removeAll();

        JTextField qf = qtyField();
        qf.setText(qty == 0 ? "" : String.valueOf(qty));
        ingRows.put(name, qf);
        ingListPanel.add(listRow(name, qf, false));
        ingListPanel.revalidate();
        ingListPanel.repaint();
        clearAdder();
    }

    private void clearAdder() {
        ingNameField.setText("");
        ingQtyField.setText("");
        ingPickerBox.setSelectedIndex(0);
        ingNameField.requestFocus();
    }

    private void refreshIngEmptyState() {
        if (ingRows.isEmpty()) {
            ingListPanel.removeAll();
            JPanel c = new JPanel(new GridBagLayout());
            c.setBackground(BG_SURFACE);
            c.setPreferredSize(new Dimension(100, 90));
            JLabel l = new JLabel("No ingredients added yet");
            l.setFont(new Font("Segoe UI", Font.ITALIC, 11));
            l.setForeground(TEXT_HINT);
            c.add(l);
            ingListPanel.add(c);
            ingListPanel.revalidate();
            ingListPanel.repaint();
        }
    }

    private void addDefaultPackaging() {
        for (String name : PACKAGING_DEFAULTS) {
            JTextField qf = qtyField();
            pkgRows.put(name, qf);
            pkgListPanel.add(listRow(name, qf, true));
        }
    }

    // ── Generic list row ──────────────────────────────────────────────────────
    private JPanel listRow(String name, JTextField qtyField, boolean isPkg) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(isPkg ? BG_PKG : BG_SUBTLE);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, isPkg ? BORDER_PKG : BORDER),
                new EmptyBorder(8, 10, 8, 10)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        row.setAlignmentX(LEFT_ALIGNMENT);

        JLabel dot = new JLabel("●");
        dot.setFont(new Font("Segoe UI", Font.PLAIN, 7));
        dot.setForeground(isPkg ? PKG_ACCENT : ACCENT);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        nameLbl.setForeground(TEXT_PRI);
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        left.setOpaque(false);
        left.add(dot);
        left.add(nameLbl);

        JLabel unit = new JLabel(isPkg ? "pcs" : "g");
        unit.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        unit.setForeground(TEXT_MUTED);

        JButton rm = new JButton("✕");
        rm.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        rm.setForeground(DANGER);
        rm.setOpaque(false);
        rm.setContentAreaFilled(false);
        rm.setFocusPainted(false);
        rm.setBorder(new EmptyBorder(2, 5, 2, 5));
        rm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rm.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                rm.setForeground(new Color(0xDC2626));
            }

            public void mouseExited(MouseEvent e) {
                rm.setForeground(DANGER);
            }
        });
        rm.addActionListener(e -> {
            if (isPkg) {
                pkgRows.remove(name);
                pkgListPanel.remove(row);
                pkgListPanel.revalidate();
                pkgListPanel.repaint();
            } else {
                ingRows.remove(name);
                ingListPanel.remove(row);
                ingListPanel.revalidate();
                ingListPanel.repaint();
                refreshIngEmptyState();
            }
        });

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.setOpaque(false);
        right.add(qtyField);
        right.add(unit);
        right.add(Box.createHorizontalStrut(4));
        right.add(rm);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.EAST);
        return row;
    }

    // ── Footer ────────────────────────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER),
                new EmptyBorder(11, 20, 11, 20)));

        JLabel req = new JLabel("* required fields");
        req.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        req.setForeground(TEXT_HINT);

        JButton cancel = ghostButton("Cancel");
        JButton save = accentButton("Save Item");
        cancel.setPreferredSize(new Dimension(88, 32));
        save.setPreferredSize(new Dimension(108, 32));
        cancel.addActionListener(e -> dispose());
        save.addActionListener(e -> onSave());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(cancel);
        btns.add(save);

        bar.add(req, BorderLayout.WEST);
        bar.add(btns, BorderLayout.EAST);
        return bar;
    }

    // ── Populate ──────────────────────────────────────────────────────────────
    private void populate(MenuItem item) {
        nameField.setText(item.getName());
        for (int i = 0; i < CATEGORIES.length; i++)
            if (CATEGORIES[i].equalsIgnoreCase(item.getCategory())) {
                catBox.setSelectedIndex(i);
                break;
            }
        if (item.getHotPrice() > 0)
            hotField.setText(String.valueOf((int) item.getHotPrice()));
        if (item.getIcedRegularPrice() > 0)
            regField.setText(String.valueOf((int) item.getIcedRegularPrice()));
        if (item.getIcedLargePrice() > 0)
            lgField.setText(String.valueOf((int) item.getIcedLargePrice()));

        ingListPanel.removeAll();
        item.getIngredients().forEach((k, v) -> {
            boolean isPkg = false;
            for (String p : PACKAGING_DEFAULTS)
                if (p.equalsIgnoreCase(k)) {
                    isPkg = true;
                    break;
                }
            String val = v % 1 == 0 ? String.valueOf((int) (double) v) : String.valueOf(v);
            if (isPkg) {
                if (pkgRows.containsKey(k))
                    pkgRows.get(k).setText(val);
                else {
                    JTextField qf = qtyField();
                    qf.setText(val);
                    pkgRows.put(k, qf);
                    pkgListPanel.add(listRow(k, qf, true));
                }
            } else {
                JTextField qf = qtyField();
                qf.setText(val);
                ingRows.put(k, qf);
                ingListPanel.add(listRow(k, qf, false));
            }
        });
        refreshIngEmptyState();

        String ip = item.getImagePath();
        if (ip != null && !ip.isBlank()) {
            File f = new File(ip);
            if (f.exists()) {
                dropZone.loadImage(f);
                selectedImagePath = ip;
            }
        }
    }

    // ── Save ──────────────────────────────────────────────────────────────────
    private void onSave() {
        if (nameField.getText().trim().isEmpty()) {
            shake(nameField);
            nameField.requestFocus();
            return;
        }
        confirmed = true;
        dispose();
    }

    // ── Public accessors ──────────────────────────────────────────────────────
    public boolean isConfirmed() {
        return confirmed;
    }

    public String getName() {
        return nameField.getText().trim();
    }

    public String getCategory() {
        return (String) catBox.getSelectedItem();
    }

    public double getHot() {
        return parsePrice(hotField);
    }

    public double getIcedRegular() {
        return parsePrice(regField);
    }

    public double getIcedLarge() {
        return parsePrice(lgField);
    }

    public String getImagePath() {
        return selectedImagePath;
    }

    public Map<String, Double> getIngredientsMap() {
        Map<String, Double> m = new LinkedHashMap<>();
        ingRows.forEach((k, tf) -> safePut(m, k, tf));
        pkgRows.forEach((k, tf) -> safePut(m, k, tf));
        return m;
    }

    private void safePut(Map<String, Double> m, String k, JTextField tf) {
        try {
            if (!tf.getText().trim().isEmpty())
                m.put(k, Double.parseDouble(tf.getText().trim()));
        } catch (NumberFormatException ignored) {
        }
    }

    public String getIngredientsCsv() {
        StringBuilder sb = new StringBuilder();
        getIngredientsMap().forEach((k, v) -> {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(k).append(":").append(v);
        });
        return sb.toString();
    }

    // ── Image drop zone ───────────────────────────────────────────────────────
    private class ImageDropZone extends JPanel {
        private BufferedImage preview = null;
        private boolean hovering = false;

        ImageDropZone() {
            setBackground(BG_SUBTLE);
            setBorder(new LineBorder(BORDER, 1, true));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    browseImage();
                }

                public void mouseEntered(MouseEvent e) {
                    hovering = true;
                    repaint();
                }

                public void mouseExited(MouseEvent e) {
                    hovering = false;
                    repaint();
                }
            });
            new DropTarget(this, DnDConstants.ACTION_COPY, new DropTargetAdapter() {
                public void drop(DropTargetDropEvent e) {
                    try {
                        e.acceptDrop(DnDConstants.ACTION_COPY);
                        @SuppressWarnings("unchecked")
                        java.util.List<File> files = (java.util.List<File>) e.getTransferable()
                                .getTransferData(DataFlavor.javaFileListFlavor);
                        if (!files.isEmpty())
                            loadImage(files.get(0));
                        e.dropComplete(true);
                    } catch (Exception ignored) {
                        e.dropComplete(false);
                    }
                }

                public void dragEnter(DropTargetDragEvent e) {
                    hovering = true;
                    repaint();
                }

                public void dragExit(DropTargetEvent e) {
                    hovering = false;
                    repaint();
                }
            }, true);
        }

        void browseImage() {
            JFileChooser fc = new JFileChooser();
            fc.setDialogTitle("Choose Item Image");
            fc.setFileFilter(
                    new FileNameExtensionFilter("Images (*.jpg,*.jpeg,*.png,*.webp)", "jpg", "jpeg", "png", "webp"));
            fc.setAcceptAllFileFilterUsed(false);
            if (fc.showOpenDialog(MenuItemDialog.this) == JFileChooser.APPROVE_OPTION)
                loadImage(fc.getSelectedFile());
        }

        void loadImage(File file) {
            try {
                BufferedImage img = readImage(file);
                if (img == null)
                    throw new IOException("Unsupported image format");
                Path saveDir = getImageSaveDirectory();
                Files.createDirectories(saveDir);
                String ext = ext(file.getName());
                String safe = nameField.getText().trim().replaceAll("[^a-zA-Z0-9_\\-]", "_");
                if (safe.isEmpty())
                    safe = "item_" + System.currentTimeMillis();
                Path dest = saveDir.resolve(safe + "." + ext);
                Files.copy(file.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
                selectedImagePath = dest.toAbsolutePath().toString();
                preview = img;
                hovering = false;
                repaint();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(MenuItemDialog.this,
                        "Could not load image: " + ex.getMessage(), "Image Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private Path getImageSaveDirectory() {
            String category = Objects.toString(catBox.getSelectedItem(), "Coffee").trim();
            String folder = mapCategoryToResourceFolder(category, nameField.getText().trim());
            return RESOURCE_IMAGES_DIR.resolve(folder);
        }

        private String mapCategoryToResourceFolder(String category, String itemName) {
            if (category.equalsIgnoreCase("Coffee")) {
                return "Espresso & Coffee";
            }
            if (category.equalsIgnoreCase("Non-Coffee") || category.equalsIgnoreCase("Fruit Tea")
                    || category.equalsIgnoreCase("Herbal Tea")) {
                return category;
            }
            if (category.equalsIgnoreCase("Food")) {
                String normalized = itemName == null ? "" : itemName.toLowerCase();
                if (normalized.contains("pandesal") || normalized.contains("pande"))
                    return "Pandesal Pairs";
                if (normalized.contains("sandwich") || normalized.contains("ham") || normalized.contains("cheese")
                        || normalized.contains("club"))
                    return "Sandwiches";
                return "Pastries";
            }
            return category;
        }

        private BufferedImage readImage(File file) throws IOException {
            BufferedImage img = ImageIO.read(file);
            if (img != null)
                return img;

            Image iconImage = Toolkit.getDefaultToolkit().createImage(file.getAbsolutePath());
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(iconImage, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (tracker.isErrorAny())
                return null;

            int w = iconImage.getWidth(null);
            int h = iconImage.getHeight(null);
            if (w <= 0 || h <= 0)
                return null;

            BufferedImage buffered = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = buffered.createGraphics();
            g2.drawImage(iconImage, 0, 0, null);
            g2.dispose();
            return buffered;
        }

        private String ext(String n) {
            int d = n.lastIndexOf('.');
            return d >= 0 ? n.substring(d + 1).toLowerCase() : "png";
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            int w = getWidth(), h = getHeight();
            if (preview != null) {
                double sc = Math.min((double) (w - 12) / preview.getWidth(), (double) (h - 12) / preview.getHeight());
                int iw = (int) (preview.getWidth() * sc), ih = (int) (preview.getHeight() * sc);
                int ix = (w - iw) / 2, iy = (h - ih) / 2;
                g2.setClip(new RoundRectangle2D.Float(ix, iy, iw, ih, 10, 10));
                g2.drawImage(preview, ix, iy, iw, ih, null);
                g2.setClip(null);
                if (hovering) {
                    g2.setColor(new Color(0, 0, 0, 110));
                    g2.fill(new RoundRectangle2D.Float(ix, iy, iw, ih, 10, 10));
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    String msg = "Change photo";
                    g2.drawString(msg, (w - fm.stringWidth(msg)) / 2, h / 2 + fm.getAscent() / 2 - 2);
                }
            } else {
                g2.setColor(hovering ? new Color(0xEBEBF8) : BG_SUBTLE);
                g2.fillRoundRect(3, 3, w - 6, h - 6, 10, 10);
                g2.setColor(hovering ? ACCENT : BORDER);
                g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0,
                        new float[] { 6, 4 }, 0));
                g2.drawRoundRect(3, 3, w - 7, h - 7, 10, 10);
                g2.setStroke(new BasicStroke(1.5f));
                int cx = w / 2, cy = h / 2 - 16, is = 28;
                g2.setColor(hovering ? ACCENT : TEXT_HINT);
                g2.drawRoundRect(cx - is / 2, cy - is / 2 + 2, is, is - 4, 5, 5);
                g2.drawPolyline(new int[] { cx - 9, cx - 2, cx + 11 }, new int[] { cy + 7, cy - 2, cy + 7 }, 3);
                g2.fillOval(cx + 3, cy - 10, 6, 6);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 11));
                g2.setColor(hovering ? ACCENT : TEXT_MUTED);
                FontMetrics fm = g2.getFontMetrics();
                String l1 = "Click or drag", l2 = "to add photo";
                g2.drawString(l1, (w - fm.stringWidth(l1)) / 2, cy + 27);
                g2.setFont(new Font("Segoe UI", Font.PLAIN, 10));
                g2.setColor(TEXT_HINT);
                fm = g2.getFontMetrics();
                g2.drawString(l2, (w - fm.stringWidth(l2)) / 2, cy + 40);
                g2.setColor(new Color(0xC8C6BF));
                String fmt = "JPG · PNG · WEBP";
                g2.drawString(fmt, (w - fm.stringWidth(fmt)) / 2, h - 10);
            }
            g2.dispose();
        }
    }

    // ── Utilities ─────────────────────────────────────────────────────────────
    private static JTextField styledField() {
        JTextField f = new JTextField();
        f.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        f.setForeground(TEXT_PRI);
        f.setBackground(BG_SURFACE);
        f.setCaretColor(ACCENT);
        f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(5, 8, 5, 8)));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(ACCENT, 1, true), new EmptyBorder(5, 8, 5, 8)));
            }

            public void focusLost(FocusEvent e) {
                f.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(5, 8, 5, 8)));
            }
        });
        return f;
    }

    private static JTextField qtyField() {
        JTextField f = styledField();
        f.setPreferredSize(new Dimension(70, 32));
        f.setMaximumSize(new Dimension(80, 32));
        return f;
    }

    private static void styleComboBox(JComboBox<?> b) {
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setBackground(BG_SURFACE);
        b.setForeground(TEXT_PRI);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(2, 4, 2, 4)));
    }

    private static JPanel field(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_HINT);
        p.add(l, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    /** A row panel with a fixed height, aligned left for BoxLayout. */
    private static JPanel fixedRow(LayoutManager lm, int w, int h) {
        JPanel p = new JPanel(lm);
        p.setOpaque(false);
        p.setAlignmentX(LEFT_ALIGNMENT);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, h));
        p.setPreferredSize(new Dimension(w, h));
        return p;
    }

    private static JScrollPane listScroll(JPanel content, Color border, Color bg) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(new LineBorder(border, 1, true));
        sp.setBackground(bg);
        sp.getViewport().setBackground(bg);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sp.getVerticalScrollBar().setUnitIncrement(10);
        return sp;
    }

    private static JPanel sectionPill(String text, Color fg, Color bg) {
        JPanel w = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        w.setOpaque(false);
        w.setAlignmentX(LEFT_ALIGNMENT);
        w.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel pill = new JLabel(text) {
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
        pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        pill.setForeground(fg);
        pill.setBorder(new EmptyBorder(3, 8, 3, 8));
        pill.setOpaque(false);
        w.add(pill);
        return w;
    }

    private static Component vgap(int h) {
        return Box.createRigidArea(new Dimension(0, h));
    }

    private static JLabel muted(String t) {
        JLabel l = new JLabel(t);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        l.setForeground(TEXT_HINT);
        return l;
    }

    private static double parsePrice(JTextField f) {
        try {
            return Double.parseDouble(f.getText().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static JButton accentButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? ACCENT_HVR : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.BOLD, 12));
        b.setForeground(Color.WHITE);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(7, 16, 7, 16));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton ghostButton(String text) {
        JButton b = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? BG_SUBTLE : BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 7, 7);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        b.setForeground(TEXT_MUTED);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(BORDER, 1, true), new EmptyBorder(6, 14, 6, 14)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static void shake(JComponent c) {
        Point o = c.getLocation();
        int[] off = { -6, 6, -4, 4, -2, 2, 0 };
        javax.swing.Timer t = new javax.swing.Timer(40, null);
        int[] i = { 0 };
        t.addActionListener(e -> {
            if (i[0] >= off.length) {
                t.stop();
                c.setLocation(o);
                return;
            }
            c.setLocation(o.x + off[i[0]++], o.y);
        });
        t.start();
    }
}