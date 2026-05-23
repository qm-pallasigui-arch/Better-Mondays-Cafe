package ui;

import inventory.InventoryBatch;
import inventory.validation.InventoryBatchValidator;
import persistence.InventoryRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Inventory Batch Registration panel.
 *
 * Persistence strategy (dual-write):
 * 1. Primary — InventoryRepository (SQLite via SQLiteInventoryRepository),
 * passed in from POSSystem exactly as before.
 * 2. Fallback — <user.home>/.inventory_batches.json written on every Add,
 * used to seed the table when the DB read fails or returns
 * nothing (offline / first-run guard).
 *
 * Style: aligned with SearchModule.java — same dark palette, RoundedPanel /
 * RoundedBorder / PlaceholderTextField inner classes, pill badges, and
 * custom-painted buttons.
 */
public class InventoryRegistrationPanel extends JPanel {

    // ── Persistence ───────────────────────────────────────────────────────────
    private final InventoryRepository inventoryRepository;
    private static final Path JSON_FILE = Paths.get(System.getProperty("user.home"), ".inventory_batches.json");
    private static final Path DELETED_IDS_FILE = Paths.get(System.getProperty("user.home"),
            ".inventory_deleted_ids.txt");

    // ── Colors (= SearchModule palette) ──────────────────────────────────────
    private static final Color BG_PAGE = new Color(0x1C2A3A);
    private static final Color BG_CARD = new Color(0x243447);
    private static final Color BG_INPUT = new Color(0x1C2A3A);
    private static final Color BORDER_SUBTLE = new Color(0x2E4060);
    private static final Color BORDER_FOCUS = new Color(0x2E86DE);
    private static final Color TEXT_PRIMARY = new Color(0xEAEEF2);
    private static final Color TEXT_SECONDARY = new Color(0x8FA3B8);
    private static final Color TEXT_HINT = new Color(0x566A7F);
    private static final Color ACCENT = new Color(0x2E86DE);
    private static final Color ACCENT_HOVER = new Color(0x1A6BBF);
    private static final Color ROW_ALT = new Color(0x1F3145);
    private static final Color HEADER_BG = new Color(0x1A2B3C);
    private static final Color BTN_SEC_HOV = new Color(0x2D4058);

    private static final Color BADGE_OK_BG = new Color(0x1A3A28);
    private static final Color BADGE_OK_FG = new Color(0x6FCF97);
    private static final Color BADGE_WARN_BG = new Color(0x3A2E14);
    private static final Color BADGE_WARN_FG = new Color(0xF6C86B);
    private static final Color BADGE_ERR_BG = new Color(0x3A1A1A);
    private static final Color BADGE_ERR_FG = new Color(0xF28B82);

    private static final Color BTN_DEL_BG = new Color(0x3A1A1A);
    private static final Color BTN_DEL_FG = new Color(0xF28B82);
    private static final Color BTN_DEL_HOV_BG = new Color(0x5A2020);

    // ── Fonts (= SearchModule) ────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 17);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BTN = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // ── UI fields ─────────────────────────────────────────────────────────────
    private final PlaceholderTextField itemNameField = new PlaceholderTextField("Item Name");
    private final PlaceholderTextField skuField = new PlaceholderTextField("Auto-generated if blank");
    private final PlaceholderTextField qtyField = new PlaceholderTextField("e.g. 100");
    private final PlaceholderTextField expiryField = new PlaceholderTextField("YYYY-MM-DD");
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel batchCountLabel = new JLabel(" ");

    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Batch ID", "Item", "SKU", "Quantity", "Expiry", "Status", "" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return c == 6; // delete column uses a button editor
        }
    };

    // In-memory cache: String[]{ id, itemName, sku, qty, expiry }
    private final List<String[]> batchCache = new ArrayList<>();

    // IDs that have been deleted — persisted across restarts so the DB rows
    // (which may not support hard-delete) are always filtered out on load.
    private final Set<String> deletedIds = new HashSet<>();

    // ── Constructor (matches POSSystem call signature) ─────────────────────────
    public InventoryRegistrationPanel(InventoryRepository inventoryRepository) {
        super(new BorderLayout());
        this.inventoryRepository = inventoryRepository;
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        loadDeletedIds(); // must run before loadAllBatches()

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);

        loadAllBatches();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Top bar: title row + form card
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 14));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Title row
        JPanel titleRow = new JPanel(new BorderLayout());
        titleRow.setOpaque(false);
        JLabel title = new JLabel("Inventory Batch Registration");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        batchCountLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        batchCountLabel.setForeground(TEXT_HINT);
        batchCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        titleRow.add(title, BorderLayout.WEST);
        titleRow.add(batchCountLabel, BorderLayout.EAST);
        wrapper.add(titleRow, BorderLayout.NORTH);

        // Form card
        JPanel card = new CardPanel(12, BG_CARD);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setLayout(new GridBagLayout());

        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        g.insets = new Insets(0, 0, 0, 10);

        // Row 1: Item name | SKU | spacer
        addLabeledField(card, g, 0, "Item name", itemNameField, 1.5);
        addLabeledField(card, g, 2, "SKU (optional)", skuField, 1.0);
        g.gridx = 4;
        g.weightx = 0.5;
        card.add(Box.createHorizontalGlue(), g);

        // Row 2: Quantity | Expiry | Buttons
        g.gridy = 1;
        g.insets = new Insets(12, 0, 0, 10);
        addLabeledField(card, g, 0, "Quantity", qtyField, 0.6);
        addLabeledField(card, g, 2, "Expiry (YYYY-MM-DD)", expiryField, 1.0);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(buildPrimaryButton("+ Add Batch", this::onAdd));
        btnPanel.add(buildSecondaryButton("Clear", this::onClear));

        JPanel btnWrapper = new JPanel(new BorderLayout(0, 4));
        btnWrapper.setOpaque(false);
        JLabel spacer = new JLabel("");
        spacer.setFont(FONT_LABEL);
        btnWrapper.add(spacer, BorderLayout.NORTH);
        btnWrapper.add(btnPanel, BorderLayout.CENTER);

        g.gridx = 4;
        g.weightx = 0.8;
        g.insets = new Insets(12, 0, 0, 0);
        card.add(btnWrapper, g);

        // Enter on any field → add
        for (JTextField f : new JTextField[] { itemNameField, skuField, qtyField, expiryField })
            f.addActionListener(e -> onAdd(null));

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private void addLabeledField(JPanel card, GridBagConstraints g,
            int col, String labelText,
            JTextField field, double weight) {
        JPanel wrap = new JPanel(new BorderLayout(0, 4));
        wrap.setOpaque(false);
        wrap.add(fieldLabel(labelText), BorderLayout.NORTH);
        AppTheme.styleSearchField(field);
        wrap.add(field, BorderLayout.CENTER);
        g.gridx = col;
        g.weightx = weight;
        g.gridwidth = 2;
        card.add(wrap, g);
        g.gridwidth = 1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Table area
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildTableArea() {
        JTable table = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                return c;
            }
        };
        table.setFont(FONT_TABLE);
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setBackground(BG_CARD);
        table.setSelectionBackground(new Color(0x1A3A5C));
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFocusable(false);
        table.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(HEADER_BG);
        header.setForeground(TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        header.setReorderingAllowed(false);
        ((DefaultTableCellRenderer) header.getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        // Column widths — last column is narrow (delete button)
        int[] widths = { 95, 190, 140, 70, 110, 110, 72 };
        for (int i = 0; i < widths.length; i++)
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        table.getColumnModel().getColumn(0).setCellRenderer(new MonoIdRenderer());
        StdRenderer std = new StdRenderer();
        for (int i = 1; i <= 4; i++)
            table.getColumnModel().getColumn(i).setCellRenderer(std);
        table.getColumnModel().getColumn(5).setCellRenderer(new StatusBadgeRenderer());

        // Delete column — renderer + editor
        DeleteButtonRenderer deleteRenderer = new DeleteButtonRenderer();
        DeleteButtonEditor deleteEditor = new DeleteButtonEditor(table);
        table.getColumnModel().getColumn(6).setCellRenderer(deleteRenderer);
        table.getColumnModel().getColumn(6).setCellEditor(deleteEditor);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(AppTheme.inputBorderRegular());
        scroll.getViewport().setBackground(BG_CARD);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(scroll, BorderLayout.CENTER);
        return wrap;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Status bar
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(10, 2, 0, 0));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_HINT);
        bar.add(statusLabel);
        return bar;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Event handlers
    // ─────────────────────────────────────────────────────────────────────────
    private void onAdd(ActionEvent e) {
        String name = itemNameField.getText().trim();
        String sku = skuField.getText().trim();
        String qtys = qtyField.getText().trim();
        String expiry = expiryField.getText().trim();

        if (name.isEmpty() || qtys.isEmpty()) {
            setStatus("⚠  Item name and quantity are required.", BADGE_WARN_FG);
            return;
        }

        double qty;
        String normExpiry;
        try {
            qty = InventoryBatchValidator.parseQuantity(qtys);
            normExpiry = InventoryBatchValidator.normalizeExpiry(expiry);
        } catch (Exception ex) {
            setStatus("⚠  " + ex.getMessage(), BADGE_WARN_FG);
            return;
        }

        String finalSku = sku.isEmpty() ? autoSku(name) : sku;
        InventoryBatch batch = new InventoryBatch(finalSku, qty, normExpiry.isBlank() ? null : normExpiry);

        // 1. Primary: persist to SQLite via repository
        boolean dbOk = false;
        try {
            inventoryRepository.addBatch(name, batch);
            dbOk = true;
        } catch (Exception ex) {
            setStatus("⚠  DB save failed, using local backup: " + ex.getMessage(), BADGE_WARN_FG);
        }

        // 2. Fallback / audit: always write JSON alongside DB
        String id = batch.getId() != 0
                ? String.format("INV-%06d", batch.getId())
                : generateBatchId();
        String expiryStr = normExpiry.isBlank() ? "" : normExpiry;
        batchCache.add(new String[] { id, name, finalSku, String.valueOf(qty), expiryStr });
        writeJsonFallback();

        // Append the new row directly — do NOT call loadAllBatches() here.
        // loadAllBatches() re-queries the DB by item name and would resurrect
        // any previously deleted batches for the same item name.
        rebuildTable();

        // Keep item name, clear per-batch fields for fast consecutive entry
        skuField.setText("");
        qtyField.setText("");
        expiryField.setText("");

        int n = (int) batchCache.stream().filter(r -> r[1].equalsIgnoreCase(name)).count();
        String via = dbOk ? "" : " (local only)";
        setStatus("✓  Batch saved" + via + " — " + name + " now has "
                + n + " batch" + (n != 1 ? "es" : "") + ".", BADGE_OK_FG);
    }

    private void onClear(ActionEvent e) {
        itemNameField.setText("");
        skuField.setText("");
        qtyField.setText("");
        expiryField.setText("");
        setStatus("Fields cleared.", TEXT_HINT);
    }

    /**
     * Deletes the batch at the given view row index.
     * Removes from the repository (best-effort), updates the cache and JSON,
     * then rebuilds the table.
     */
    private void onDelete(int row) {
        if (row < 0 || row >= batchCache.size())
            return;

        String[] entry = batchCache.get(row);
        String batchId = entry[0];
        String itemName = entry[1];

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "<html>Delete batch <b>" + batchId + "</b> for <b>" + itemName + "</b>?</html>",
                "Confirm Delete",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION)
            return;

        // Try repository removal (optional — only if your repo exposes deleteBatch)
        try {
            if (inventoryRepository instanceof persistence.DeletableBatchRepository) {
                ((persistence.DeletableBatchRepository) inventoryRepository).deleteBatch(batchId);
            }
        } catch (Exception ignored) {
            // Non-fatal — remove from cache regardless
        }

        batchCache.remove(row);
        deletedIds.add(batchId); // persist so it survives restart
        saveDeletedIds();
        writeJsonFallback();
        rebuildTable();
        setStatus("✓  Batch " + batchId + " deleted.", BADGE_ERR_FG);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Load: DB first, JSON fallback
    // ─────────────────────────────────────────────────────────────────────────
    private void loadAllBatches() {
        batchCache.clear();
        boolean dbLoaded = false;

        // Try loading from repository
        try {
            List<String[]> jsonRows = readJsonFallback();
            List<String> knownItems = jsonRows.stream()
                    .map(r -> r[1])
                    .distinct()
                    .collect(java.util.stream.Collectors.toList());

            if (!knownItems.isEmpty()) {
                for (String itemName : knownItems) {
                    List<InventoryBatch> batches = inventoryRepository.findBatchesForItem(itemName);
                    for (InventoryBatch b : batches) {
                        String expiryStr = b.getExpiryDate() != null ? b.getExpiryDate() : "";
                        long batchIdValue = b.getId();
                        String batchId = batchIdValue != 0L
                                ? String.format("INV-%06d", batchIdValue)
                                : generateBatchId();
                        if (!deletedIds.contains(batchId)) {
                            batchCache.add(new String[] { batchId, itemName, b.getSku(),
                                    String.valueOf(b.getQuantity()), expiryStr });
                        }
                    }
                }
                dbLoaded = !batchCache.isEmpty();
            }
        } catch (Exception ex) {
            // DB unavailable — fall through to JSON
        }

        if (!dbLoaded) {
            readJsonFallback().stream()
                    .filter(r -> !deletedIds.contains(r[0]))
                    .forEach(batchCache::add);
        }

        rebuildTable();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Table rebuild from batchCache
    // ─────────────────────────────────────────────────────────────────────────
    private void rebuildTable() {
        tableModel.setRowCount(0);
        for (String[] row : batchCache) {
            tableModel.addRow(new Object[] {
                    row[0], row[1], row[2], row[3], row[4], computeStatus(row[4]), "Delete"
            });
        }
        int n = batchCache.size();
        batchCountLabel.setText(n + " batch" + (n != 1 ? "es" : "") + " stored");
    }

    private String computeStatus(String expiry) {
        if (expiry == null || expiry.isBlank())
            return "OK";
        try {
            LocalDate d = LocalDate.parse(expiry);
            if (d.isBefore(LocalDate.now()))
                return "EXPIRED";
            if (!d.isAfter(LocalDate.now().plusDays(7)))
                return "EXPIRING";
        } catch (Exception ignored) {
        }
        return "OK";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // JSON fallback persistence (no external libs)
    // ─────────────────────────────────────────────────────────────────────────
    private void writeJsonFallback() {
        try {
            StringBuilder sb = new StringBuilder("[\n");
            for (int i = 0; i < batchCache.size(); i++) {
                String[] r = batchCache.get(i);
                sb.append("  {")
                        .append("\"id\":").append(js(r[0])).append(",")
                        .append("\"item\":").append(js(r[1])).append(",")
                        .append("\"sku\":").append(js(r[2])).append(",")
                        .append("\"qty\":").append(js(r[3])).append(",")
                        .append("\"expiry\":").append(js(r[4]))
                        .append("}");
                if (i < batchCache.size() - 1)
                    sb.append(",");
                sb.append("\n");
            }
            sb.append("]");
            Files.writeString(JSON_FILE, sb.toString());
        } catch (IOException ex) {
            // Non-fatal — DB is primary store
        }
    }

    private List<String[]> readJsonFallback() {
        List<String[]> result = new ArrayList<>();
        if (!Files.exists(JSON_FILE))
            return result;
        try {
            String json = Files.readString(JSON_FILE);
            int pos = 0;
            while ((pos = json.indexOf('{', pos)) != -1) {
                int end = json.indexOf('}', pos);
                if (end == -1)
                    break;
                String obj = json.substring(pos + 1, end);
                String id = jv(obj, "id");
                String item = jv(obj, "item");
                String sku = jv(obj, "sku");
                String qty = jv(obj, "qty");
                String expiry = jv(obj, "expiry");
                if (id != null && item != null)
                    result.add(new String[] { id, item,
                            sku != null ? sku : "",
                            qty != null ? qty : "0",
                            expiry != null ? expiry : "" });
                pos = end + 1;
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private String js(String s) {
        if (s == null)
            return "\"\"";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private String jv(String obj, String key) {
        String tok = "\"" + key + "\":\"";
        int s = obj.indexOf(tok);
        if (s == -1)
            return null;
        s += tok.length();
        int e = obj.indexOf('"', s);
        return e == -1 ? null
                : obj.substring(s, e)
                        .replace("\\\"", "\"").replace("\\\\", "\\");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────
    // Deleted-ID tombstone persistence
    // ─────────────────────────────────────────────────────────────────────────

    /** Loads the set of deleted batch IDs from disk (one ID per line). */
    private void loadDeletedIds() {
        deletedIds.clear();
        if (!Files.exists(DELETED_IDS_FILE))
            return;
        try {
            Files.readAllLines(DELETED_IDS_FILE).stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .forEach(deletedIds::add);
        } catch (IOException ignored) {
        }
    }

    /** Persists the current deleted-ID set to disk. */
    private void saveDeletedIds() {
        try {
            Files.writeString(DELETED_IDS_FILE, String.join("\n", deletedIds));
        } catch (IOException ignored) {
        }
    }

    /**
     * Generates a unique fallback batch ID when the DB has not yet assigned one.
     * Format: INV-YYYYMMDD-XXXXX e.g. INV-20240518-00042
     */
    private static String generateBatchId() {
        String date = java.time.LocalDate.now().toString().replace("-", "");
        long seq = System.nanoTime() % 100000;
        return String.format("INV-%s-%05d", date, Math.abs(seq));
    }

    private String autoSku(String name) {
        String c = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (c.length() > 8)
            c = c.substring(0, 8);
        return c + "-" + (System.currentTimeMillis() % 100000);
    }

    private void setStatus(String msg, Color fg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(fg);
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    private void styleInput(JTextField f) {
        f.setFont(FONT_INPUT);
        f.setForeground(TEXT_PRIMARY);
        f.setBackground(BG_INPUT);
        f.setCaretColor(ACCENT);
        f.setPreferredSize(new Dimension(f.getPreferredSize().width, 34));
        f.setBorder(AppTheme.inputBorderRegular());
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(AppTheme.focusInputBorder(BORDER_FOCUS));
            }

            public void focusLost(FocusEvent e) {
                f.setBorder(AppTheme.inputBorderRegular());
            }
        });
    }

    private JButton buildPrimaryButton(String text, java.util.function.Consumer<ActionEvent> action) {
        JButton btn = new JButton(text) {
            private boolean hov;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? ACCENT_HOVER : ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setFont(FONT_BTN);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(116, 34));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action::accept);
        return btn;
    }

    private JButton buildSecondaryButton(String text, java.util.function.Consumer<ActionEvent> action) {
        JButton btn = new JButton(text) {
            private boolean hov;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? BTN_SEC_HOV : BG_CARD);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setColor(BORDER_SUBTLE);
                g2.setStroke(new BasicStroke(0.8f));
                g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 0.8f, getHeight() - 0.8f, 8, 8));
                g2.setFont(FONT_BTN);
                g2.setColor(TEXT_SECONDARY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(80, 34));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action::accept);
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cell renderers
    // ─────────────────────────────────────────────────────────────────────────

    static class MonoIdRenderer extends DefaultTableCellRenderer {
        MonoIdRenderer() {
            setHorizontalAlignment(LEFT);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setFont(FONT_MONO);
            if (!sel) {
                setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                setForeground(TEXT_HINT);
            }
            setBorder(new EmptyBorder(0, 14, 0, 8));
            return this;
        }
    }

    static class StdRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setFont(FONT_TABLE);
            if (!sel) {
                setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                setForeground(col == 1 ? TEXT_PRIMARY : TEXT_SECONDARY);
            }
            setBorder(new EmptyBorder(0, 14, 0, 8));
            return this;
        }
    }

    static class StatusBadgeRenderer extends JPanel implements TableCellRenderer {
        private final JLabel badge = new JLabel();

        StatusBadgeRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            badge.setFont(FONT_BADGE);
            badge.setOpaque(true);
            badge.setHorizontalAlignment(SwingConstants.CENTER);
            add(badge);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            String s = v == null ? "" : v.toString();
            badge.setText("  " + s + "  ");
            setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? BG_CARD : ROW_ALT));
            Color[] bc = switch (s) {
                case "EXPIRED" -> new Color[] { BADGE_ERR_BG, BADGE_ERR_FG };
                case "EXPIRING" -> new Color[] { BADGE_WARN_BG, BADGE_WARN_FG };
                default -> new Color[] { BADGE_OK_BG, BADGE_OK_FG };
            };
            badge.setBackground(bc[0]);
            badge.setForeground(bc[1]);
            badge.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            new Color(bc[1].getRed(), bc[1].getGreen(), bc[1].getBlue(), 70), 1, true),
                    new EmptyBorder(3, 6, 3, 6)));
            return this;
        }
    }

    // ── Delete button renderer ─────────────────────────────────────────────────
    /** Paints a styled "Delete" pill button in each row of the last column. */
    static class DeleteButtonRenderer extends JPanel implements TableCellRenderer {
        private final JLabel btn = new JLabel("Delete");

        DeleteButtonRenderer() {
            setLayout(new GridBagLayout());
            setOpaque(true);
            btn.setFont(FONT_BADGE);
            btn.setOpaque(true);
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.setBackground(BTN_DEL_BG);
            btn.setForeground(BTN_DEL_FG);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            new Color(BTN_DEL_FG.getRed(), BTN_DEL_FG.getGreen(), BTN_DEL_FG.getBlue(), 80), 1, true),
                    new EmptyBorder(3, 8, 3, 8)));
            add(btn);
        }

        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            setBackground(sel ? t.getSelectionBackground() : (row % 2 == 0 ? BG_CARD : ROW_ALT));
            return this;
        }
    }

    // ── Delete button editor ───────────────────────────────────────────────────
    /**
     * Makes the delete cell clickable. On click it fires onDelete(row) on the
     * enclosing panel and immediately cancels editing so the table stays clean.
     */
    class DeleteButtonEditor extends DefaultCellEditor {
        private final JPanel cell = new JPanel(new GridBagLayout());
        private final JLabel btn = new JLabel("Delete");
        private int currentRow = -1;

        DeleteButtonEditor(JTable table) {
            super(new JCheckBox()); // required superclass arg — not shown
            setClickCountToStart(1);

            btn.setFont(FONT_BADGE);
            btn.setOpaque(true);
            btn.setHorizontalAlignment(SwingConstants.CENTER);
            btn.setBackground(BTN_DEL_HOV_BG);
            btn.setForeground(BTN_DEL_FG);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(
                            new Color(BTN_DEL_FG.getRed(), BTN_DEL_FG.getGreen(), BTN_DEL_FG.getBlue(), 80), 1, true),
                    new EmptyBorder(3, 8, 3, 8)));
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            cell.setOpaque(true);
            cell.add(btn);
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int column) {
            currentRow = table.convertRowIndexToModel(row);
            cell.setBackground(isSelected ? table.getSelectionBackground()
                    : (row % 2 == 0 ? BG_CARD : ROW_ALT));
            return cell;
        }

        @Override
        public Object getCellEditorValue() {
            return "Delete";
        }

        @Override
        public boolean stopCellEditing() {
            // Fire delete on the enclosing panel, then stop editing
            SwingUtilities.invokeLater(() -> onDelete(currentRow));
            return super.stopCellEditing();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner UI helpers (mirrors SearchModule)
    // ─────────────────────────────────────────────────────────────────────────

    static class PlaceholderTextField extends JTextField {
        private final String ph;

        PlaceholderTextField(String ph) {
            this.ph = ph;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                        RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setFont(FONT_INPUT);
                g2.setColor(TEXT_HINT);
                Insets ins = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(ph, ins.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    static class RoundedPanel extends JPanel {
        private final int r;
        private final Color bg;

        RoundedPanel(int r, Color bg) {
            this.r = r;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), r, r));
            g2.setColor(BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 0.8f, getHeight() - 0.8f, r, r));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int rad;
        private final Color col;

        RoundedBorder(int rad, Color col) {
            this.rad = rad;
            this.col = col;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(col);
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(new RoundRectangle2D.Float(x + 0.4f, y + 0.4f, w - 0.8f, h - 0.8f, rad, rad));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(rad / 2, rad / 2, rad / 2, rad / 2);
        }
    }
}