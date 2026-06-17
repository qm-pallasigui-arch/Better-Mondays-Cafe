package ui;

import controller.InventoryController;
import controller.InventoryRowView;
import inventory.Inventory;
import inventory.InventoryItem;
import inventory.InventoryBatch;
import inventory.analytics.InventoryPolicyService;
import notifications.Notification;
import notifications.NotificationService;
import persistence.sqlite.SQLiteInventoryRepository;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * InventoryPanel — self-contained panel for the Inventory Management tab.
 */
public class InventoryPanel extends JPanel {

    // ── Modern color palette ──────────────────────────────────────────────────
    private static final Color BG_PAGE = Color.WHITE;
    private static final Color BG_CARD = Color.WHITE;
    private static final Color BG_SURFACE = new Color(0xF9FAFB);
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY = new Color(0x111827);
    private static final Color TEXT_SECONDARY = new Color(0x374151);
    private static final Color TEXT_MUTED = new Color(0x9CA3AF);
    private static final Color ACCENT = new Color(0x1D4ED8);
    private static final Color ACCENT_HOVER = new Color(0x1E40AF);
    private static final Color ROW_BASE = Color.WHITE;
    private static final Color ROW_ALT = new Color(0xF9FAFB);
    private static final Color ROW_HOVER = new Color(0xF3F4F6);
    private static final Color SELECTION_BG = new Color(0xEFF6FF);
    private static final Color SELECTION_FG = new Color(0x1D4ED8);

    private static final Color STATUS_GOOD_FG = new Color(0x16A34A);
    private static final Color STATUS_LOW_FG = new Color(0xD97706);
    private static final Color STATUS_OUT_FG = new Color(0xEA580C);
    private static final Color STATUS_EXPIRED_FG = new Color(0xDC2626);

    private static final Color ALERT_CRITICAL_FG = new Color(0xDC2626);
    private static final Color ALERT_WARN_FG = new Color(0xD97706);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUB = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 11);

    private static final String SEARCH_PLACEHOLDER = "Search ingredients";

    // ── State ─────────────────────────────────────────────────────────────────
    private final boolean isAdmin;
    private InventoryController inventoryController;
    private final Runnable monitoringRefresh;
    private final List<InventoryRowView> rowsCache = new ArrayList<>();
    private final NotificationService notificationService = new NotificationService();
    private List<Notification> latestAlerts = List.of();
    private int hoveredRow = -1;

    // ── Widgets ───────────────────────────────────────────────────────────────
    private JTable inventoryTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;
    private JComboBox<String> categoryFilter;
    private JTextArea detailArea;
    private JLabel dateSubtitle;
    private JButton bellBtn;
    private JPanel detailBody;
    private JLabel detailEmptyLabel;

    // ── Constructor ───────────────────────────────────────────────────────────
    public InventoryPanel(boolean isAdmin, InventoryController inventoryController, Runnable monitoringRefresh) {
        super(new BorderLayout(0, 16));
        this.isAdmin = isAdmin;
        this.inventoryController = inventoryController;
        this.monitoringRefresh = monitoringRefresh;
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(20, 28, 20, 28));

        add(buildHeader(), BorderLayout.NORTH);
        add(buildMainCard(), BorderLayout.CENTER);

        refresh();
    }

    public void refresh() {
        if (inventoryController == null)
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        rowsCache.clear();
        rowsCache.addAll(inventoryController.buildInventoryRows());
        applyFilters();
        refreshAlertsBadge();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel title = new JLabel("Inventory Management");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);

        dateSubtitle = new JLabel();
        dateSubtitle.setFont(FONT_SUB);
        dateSubtitle.setForeground(TEXT_MUTED);

        JPanel titleStack = new JPanel(new BorderLayout(0, 3));
        titleStack.setOpaque(false);
        titleStack.add(title, BorderLayout.NORTH);
        titleStack.add(dateSubtitle, BorderLayout.SOUTH);
        header.add(titleStack, BorderLayout.WEST);

        // ── Modern bell icon button ───────────────────────────────────────────
        bellBtn = new JButton() {
            private boolean mouseOver;

            {
                setPreferredSize(new Dimension(36, 36));
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        mouseOver = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        mouseOver = false;
                        repaint();
                    }
                });
                addActionListener(e -> openAlertsPopup());
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                int W = getWidth(), H = getHeight();

                // Determine alert severity
                boolean hasCrit = !latestAlerts.isEmpty() && latestAlerts.stream()
                        .anyMatch(n -> n.getSeverity() == Notification.Severity.CRITICAL);
                boolean hasAny = !latestAlerts.isEmpty();

                // ── Hover background ─────────────────────────────────────────
                if (mouseOver) {
                    // Faint red tint on critical, neutral gray otherwise
                    Color hoverBg = hasCrit
                            ? new Color(220, 38, 38, 28)
                            : new Color(243, 244, 246, 200);
                    g2.setColor(hoverBg);
                    g2.fillRoundRect(1, 1, W - 2, H - 2, 8, 8);
                }

                // ── Bell icon color ──────────────────────────────────────────
                Color iconColor = hasCrit ? new Color(0xDC2626)
                        : hasAny ? new Color(0xD97706)
                                : new Color(0x9CA3AF);
                g2.setColor(iconColor);

                // ── Bell geometry (centred in 36×36 button) ──────────────────
                // Icon canvas: 20×20 px, centred with 8 px padding on each side
                float pad = 8f;
                float iW = W - pad * 2; // 20
                float iH = H - pad * 2; // 20
                float iX = pad;
                float iY = pad;
                float cx = iX + iW / 2f;

                // Key y-coordinates
                float stemTopY = iY; // very top (stem)
                float domeTopY = iY + iH * 0.12f; // dome arc top
                float shelfY = iY + iH * 0.76f; // shelf (flat bottom bar)
                float shelfBotY = iY + iH * 0.88f; // shelf bottom edge
                float clapY = shelfBotY + 1f; // clapper dot

                // Half-widths at shelf and neck
                float shelfHW = iW * 0.50f; // half-width at shelf
                float neckHW = iW * 0.14f; // half-width at neck (near stem)

                // ── Bell body (filled Path2D) ────────────────────────────────
                Path2D bell = new Path2D.Float();

                // Start: bottom-left of shelf
                bell.moveTo(cx - shelfHW, shelfBotY);
                // Shelf bottom → left edge
                bell.lineTo(cx - shelfHW, shelfY);
                // Left side: curves inward from shelf up to neck
                bell.curveTo(
                        cx - shelfHW, shelfY - iH * 0.18f, // ctrl 1
                        cx - neckHW, domeTopY + iH * 0.18f, // ctrl 2
                        cx - neckHW, domeTopY // end
                );
                // Dome top arc (left → right)
                bell.curveTo(
                        cx - neckHW, domeTopY - iH * 0.10f, // ctrl 1
                        cx + neckHW, domeTopY - iH * 0.10f, // ctrl 2
                        cx + neckHW, domeTopY // end
                );
                // Right side: mirror of left
                bell.curveTo(
                        cx + neckHW, domeTopY + iH * 0.18f,
                        cx + shelfHW, shelfY - iH * 0.18f,
                        cx + shelfHW, shelfY);
                // Shelf right → bottom-right
                bell.lineTo(cx + shelfHW, shelfBotY);
                // Shelf bottom arc (slightly rounded)
                bell.curveTo(
                        cx + shelfHW, shelfBotY + 1f,
                        cx - shelfHW, shelfBotY + 1f,
                        cx - shelfHW, shelfBotY);
                bell.closePath();
                g2.fill(bell);

                // ── Clapper dot ──────────────────────────────────────────────
                float clapR = 1.8f;
                g2.fillOval(
                        (int) (cx - clapR), (int) clapY,
                        (int) (clapR * 2), (int) (clapR * 2));

                // ── Stem (hanging point at top centre) ───────────────────────
                g2.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine((int) cx, (int) stemTopY, (int) cx, (int) (domeTopY + 1));

                // ── Floating badge (top-right corner) ───────────────────────
                if (!latestAlerts.isEmpty()) {
                    int count = latestAlerts.size();
                    String label = count > 99 ? "99+" : String.valueOf(count);
                    Font badgeFont = new Font("Segoe UI", Font.BOLD, 9);
                    g2.setFont(badgeFont);
                    FontMetrics bfm = g2.getFontMetrics();
                    int badgeDiam = Math.max(14, bfm.stringWidth(label) + 6);
                    int bx = W - badgeDiam - 1;
                    int by = 0;

                    // Red fill
                    g2.setColor(new Color(0xEF4444));
                    g2.fillOval(bx, by, badgeDiam, badgeDiam);

                    // White outline so it pops against the bell
                    g2.setColor(Color.WHITE);
                    g2.setStroke(new BasicStroke(1.5f));
                    g2.drawOval(bx, by, badgeDiam, badgeDiam);

                    // Count text
                    g2.setColor(Color.WHITE);
                    g2.setFont(badgeFont);
                    bfm = g2.getFontMetrics();
                    g2.drawString(label,
                            bx + (badgeDiam - bfm.stringWidth(label)) / 2,
                            by + (badgeDiam - bfm.getHeight()) / 2 + bfm.getAscent());
                }

                g2.dispose();
            }
        };

        header.add(bellBtn, BorderLayout.EAST);
        return header;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main card
    // ─────────────────────────────────────────────────────────────────────────
    private JPanel buildMainCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(18, 20, 20, 20));
        card.add(buildControlsRow(), BorderLayout.NORTH);
        card.add(buildTableAndDetail(), BorderLayout.CENTER);
        return card;
    }

    // ── Controls row ──────────────────────────────────────────────────────────
    private JPanel buildControlsRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);

        searchField = new JTextField(20);
        searchField.setText(SEARCH_PLACEHOLDER);
        searchField.setFont(FONT_BODY);
        searchField.setForeground(TEXT_MUTED);
        searchField.setBackground(BG_CARD);
        searchField.setCaretColor(ACCENT);
        searchField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(6, 10, 6, 10)));
        searchField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (SEARCH_PLACEHOLDER.equals(searchField.getText())) {
                    searchField.setText("");
                    searchField.setForeground(TEXT_PRIMARY);
                }
                searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1), new EmptyBorder(6, 10, 6, 10)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText(SEARCH_PLACEHOLDER);
                    searchField.setForeground(TEXT_MUTED);
                }
                searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(6, 10, 6, 10)));
            }
        });
        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                applyFilters();
            }
        });

        categoryFilter = new JComboBox<>(
                new String[] { "All", "Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food" });
        categoryFilter.setFont(FONT_BODY);
        categoryFilter.setBackground(BG_CARD);
        categoryFilter.addActionListener(e -> applyFilters());

        left.add(searchField);
        left.add(categoryFilter);
        row.add(left, BorderLayout.WEST);

        if (isAdmin) {
            JButton addBtn = buildPrimaryButton("+ Add Item", e -> openAddDialog());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(addBtn);
            row.add(right, BorderLayout.EAST);
        }
        return row;
    }

    // ── Table + detail ────────────────────────────────────────────────────────
    private JPanel buildTableAndDetail() {
        JPanel wrap = new JPanel(new BorderLayout(16, 0));
        wrap.setOpaque(false);
        wrap.add(buildTable(), BorderLayout.CENTER);
        wrap.add(buildDetail(), BorderLayout.EAST);
        return wrap;
    }

    private JScrollPane buildTable() {
        tableModel = new DefaultTableModel(
                new String[] { "Item Name", "Quantity", "Alert Level", "ABC", "Reorder Guide", "Batches", "Status",
                        "Actions" },
                0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return isAdmin && c == 7;
            }
        };

        inventoryTable = new JTable(tableModel) {
            @Override
            public Component prepareRenderer(TableCellRenderer r, int row, int col) {
                Component c = super.prepareRenderer(r, row, col);
                if (!isRowSelected(row))
                    c.setBackground(row == hoveredRow ? ROW_HOVER : row % 2 == 0 ? ROW_BASE : ROW_ALT);
                return c;
            }
        };

        inventoryTable.getTableHeader().setBackground(Color.WHITE);
        inventoryTable.getTableHeader().setForeground(TEXT_MUTED);
        inventoryTable.getTableHeader().setFont(FONT_HEADER);
        inventoryTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
        inventoryTable.getTableHeader().setReorderingAllowed(false);
        ((DefaultTableCellRenderer) inventoryTable.getTableHeader().getDefaultRenderer())
                .setHorizontalAlignment(SwingConstants.LEFT);

        inventoryTable.setFont(FONT_BODY);
        inventoryTable.setRowHeight(40);
        inventoryTable.setShowGrid(false);
        inventoryTable.setBackground(Color.WHITE);
        inventoryTable.setSelectionBackground(SELECTION_BG);
        inventoryTable.setSelectionForeground(SELECTION_FG);
        inventoryTable.setIntercellSpacing(new Dimension(0, 0));
        inventoryTable.setFillsViewportHeight(true);

        int[] widths = { 160, 100, 100, 60, 170, 130, 100, 72 };
        for (int i = 0; i < widths.length; i++)
            inventoryTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        StdRenderer std = new StdRenderer();
        for (int i = 0; i < 8; i++)
            inventoryTable.getColumnModel().getColumn(i).setCellRenderer(std);

        inventoryTable.getColumnModel().getColumn(3).setCellRenderer(new AbcTierRenderer());
        inventoryTable.getColumnModel().getColumn(4).setCellRenderer(new ReorderGuideRenderer());
        inventoryTable.getColumnModel().getColumn(5).setCellRenderer(new BatchSummaryRenderer());
        inventoryTable.getColumnModel().getColumn(6).setCellRenderer(new StatusPillRenderer());
        if (isAdmin) {
            inventoryTable.getColumnModel().getColumn(7).setCellRenderer(new DotMenuRenderer());
            inventoryTable.getColumnModel().getColumn(7).setCellEditor(new DotMenuEditor());
        }

        inventoryTable.addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                int row = inventoryTable.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    inventoryTable.repaint();
                }
            }
        });
        inventoryTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseExited(MouseEvent e) {
                hoveredRow = -1;
                inventoryTable.repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                int col = inventoryTable.columnAtPoint(e.getPoint());
                int row = inventoryTable.rowAtPoint(e.getPoint());
                if (row < 0)
                    return;
                if (col == 5) {
                    Object val = tableModel.getValueAt(row, 5);
                    if (val instanceof InventoryRowView rv && inventoryController != null) {
                        new InventoryBatchModal(
                                SwingUtilities.getWindowAncestor(InventoryPanel.this),
                                rv.getName(), inventoryController, () -> refresh()).setVisible(true);
                    }
                } else if (col == 7 && isAdmin) {
                    inventoryTable.editCellAt(row, col);
                }
            }
        });

        inventoryTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateDetail();
        });

        JScrollPane scroll = new JScrollPane(inventoryTable);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        scroll.getViewport().setBackground(Color.WHITE);
        return scroll;
    }

    private JPanel buildDetail() {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_SURFACE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 16, 16, 16));
        card.setPreferredSize(new Dimension(300, 0));

        JLabel detailTitle = new JLabel("Ingredient Details");
        detailTitle.setFont(FONT_BOLD);
        detailTitle.setForeground(TEXT_PRIMARY);
        card.add(detailTitle, BorderLayout.NORTH);

        // ── Scrollable body ───────────────────────────────────────────────
        detailBody = new JPanel();
        detailBody.setLayout(new BoxLayout(detailBody, BoxLayout.Y_AXIS));
        detailBody.setBackground(BG_SURFACE);

        detailEmptyLabel = new JLabel("Select an ingredient to see details.");
        detailEmptyLabel.setFont(FONT_BODY);
        detailEmptyLabel.setForeground(TEXT_MUTED);
        detailEmptyLabel.setBorder(new EmptyBorder(6, 4, 6, 4));
        detailEmptyLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        detailBody.add(detailEmptyLabel);

        JScrollPane detailScroll = new JScrollPane(detailBody);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        detailScroll.getViewport().setBackground(BG_SURFACE);
        detailScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        card.add(detailScroll, BorderLayout.CENTER);

        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Filter / rebuild
    // ─────────────────────────────────────────────────────────────────────────
    private void applyFilters() {
        if (tableModel == null)
            return;
        tableModel.setRowCount(0);

        String query = getSearchQuery();
        String cat = categoryFilter == null ? "All" : Objects.toString(categoryFilter.getSelectedItem(), "All");

        InventoryPolicyService svc = new InventoryPolicyService();
        Map<String, InventoryItem> itemsByName = new LinkedHashMap<>();
        for (InventoryRowView r : rowsCache)
            itemsByName.put(r.getName(),
                    new InventoryItem(r.getName(), r.getQuantity(), r.getUnit(), r.getAlertLevel()));
        Map<String, String> abcTiers = svc.classifyAbc(itemsByName);

        for (InventoryRowView row : rowsCache) {
            if (!matchesFilters(row, query, cat))
                continue;
            InventoryItem item = itemsByName.get(row.getName());
            String tier = abcTiers.getOrDefault(row.getName(), "C");
            ReorderInfo ri = new ReorderInfo(
                    svc.computeRecommendedEoq(item),
                    svc.computeReorderPoint(item),
                    row.getQuantity(), row.getUnit());
            tableModel.addRow(new Object[] {
                    row.getName(),
                    row.getQuantity() + " " + row.getUnit(),
                    row.getAlertLevel() + " " + row.getUnit(),
                    tier, ri, row, row.getStatus(), "···"
            });
        }

        if (tableModel.getRowCount() > 0)
            inventoryTable.setRowSelectionInterval(0, 0);
        else if (detailArea != null)
            detailArea.setText("No ingredients match the current filter.");
    }

    private boolean matchesFilters(InventoryRowView row, String query, String cat) {
        boolean catMatch = "All".equalsIgnoreCase(cat) || containsToken(row.getCategories(), cat);
        if (!catMatch)
            return false;
        if (query.isEmpty())
            return true;
        String hay = (row.getName() + " " + row.getUnit() + " " + row.getStatus()
                + " " + safe(row.getCategories())).toLowerCase();
        return hay.contains(query);
    }

    private String getSearchQuery() {
        if (searchField == null)
            return "";
        String t = searchField.getText() == null ? "" : searchField.getText().trim();
        return (t.isEmpty() || SEARCH_PLACEHOLDER.equalsIgnoreCase(t)) ? "" : t.toLowerCase();
    }

    private boolean containsToken(String csv, String token) {
        if (csv == null || token == null)
            return false;
        for (String p : csv.split(","))
            if (p.trim().equalsIgnoreCase(token.trim()))
                return true;
        return false;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Detail panel
    // ─────────────────────────────────────────────────────────────────────────

    private void updateDetail() {
        if (detailBody == null || inventoryTable == null)
            return;

        int sel = inventoryTable.getSelectedRow();
        if (sel < 0) {
            showDetailEmpty("Select an ingredient to see details.");
            return;
        }

        String name = String.valueOf(inventoryTable.getValueAt(sel, 0));
        List<InventoryRowView> rows = inventoryController.buildInventoryRows();

        InventoryPolicyService svc = new InventoryPolicyService();
        Map<String, InventoryItem> byName = new LinkedHashMap<>();
        for (InventoryRowView r : rows)
            byName.put(r.getName(), new InventoryItem(r.getName(), r.getQuantity(), r.getUnit(), r.getAlertLevel()));
        Map<String, String> tiers = svc.classifyAbc(byName);

        for (InventoryRowView r : rows) {
            if (!r.getName().equals(name))
                continue;

            InventoryItem item = byName.get(name);
            double eoq = svc.computeRecommendedEoq(item);
            double rop = svc.computeReorderPoint(item);
            String tier = tiers.getOrDefault(name, "C");

            detailBody.removeAll();

            // ── Section: basic info ──────────────────────────────────────
            addSectionLabel("ITEM");
            addDetailRow("Name", r.getName());
            addDetailRow("Quantity", r.getQuantity() + " " + r.getUnit());
            addDetailRow("Alert Level", r.getAlertLevel() + " " + r.getUnit());
            addDetailRow("Status", r.getStatus());
            addDetailRow("Updated", r.getLastUpdated() == null || r.getLastUpdated().isBlank()
                    ? "N/A"
                    : r.getLastUpdated());
            addDetailRow("Categories", r.getCategories() == null || r.getCategories().isBlank()
                    ? "N/A"
                    : r.getCategories());

            // ── Section: replenishment insights ──────────────────────────
            detailBody.add(Box.createVerticalStrut(10));
            addSectionLabel("REPLENISHMENT INSIGHTS");

            // ABC tier on one row, description on the next as a sub-label
            String tierDesc = tier.equals("A") ? "high priority — ~80% of usage"
                    : tier.equals("B") ? "medium priority — next ~15%"
                            : "low priority — remaining ~5%";
            addDetailRow("ABC Tier", tier);
            addSubLabel(tierDesc);

            addDetailRow("EOQ", "~" + Math.round(eoq) + " " + r.getUnit());

            boolean reorderNow = r.getQuantity() <= rop;
            addDetailRowHighlighted(
                    "ROP",
                    "~" + Math.round(rop) + " " + r.getUnit(),
                    reorderNow ? STATUS_OUT_FG : null);
            if (reorderNow)
                addSubLabelColored("← reorder now", STATUS_OUT_FG);

            addDetailRow("Deduction", "FEFO");

            detailBody.add(Box.createVerticalGlue());
            detailBody.revalidate();
            detailBody.repaint();
            return;
        }

        showDetailEmpty("No detail found for the selected item.");
    }

    private void showDetailEmpty(String message) {
        detailBody.removeAll();
        detailEmptyLabel.setText(message);
        detailBody.add(detailEmptyLabel);
        detailBody.revalidate();
        detailBody.repaint();
    }

    private void addSectionLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 10));
        lbl.setForeground(TEXT_MUTED);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(4, 4, 2, 4));
        detailBody.add(lbl);
    }

    /** Muted italic sub-line shown below a row (e.g. tier description). */
    private void addSubLabel(String text) {
        addSubLabelColored(text, TEXT_MUTED);
    }

    private void addSubLabelColored(String text, Color color) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        lbl.setForeground(color);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setBorder(new EmptyBorder(0, 8, 5, 8));
        detailBody.add(lbl);
    }

    private void addDetailRow(String label, String value) {
        addDetailRowHighlighted(label, value, null);
    }

    private void addDetailRowHighlighted(String label, String value, Color valueColor) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(BG_SURFACE);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(6, 8, 6, 8)));
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel nameLbl = new JLabel(label);
        nameLbl.setFont(FONT_BODY);
        nameLbl.setForeground(TEXT_SECONDARY);

        JLabel valLbl = new JLabel(value);
        valLbl.setFont(FONT_BOLD);
        valLbl.setForeground(valueColor != null ? valueColor : TEXT_MUTED);

        row.add(nameLbl, BorderLayout.WEST);
        row.add(valLbl, BorderLayout.EAST);
        detailBody.add(row);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Alerts badge + popup
    // ─────────────────────────────────────────────────────────────────────────
    private void refreshAlertsBadge() {
        if (bellBtn == null)
            return;
        latestAlerts = notificationService.evaluate(rowsCache);
        bellBtn.repaint();
    }

    /**
     * Opens a lightweight dropdown popup anchored below the bell button.
     * Vertical scroll only — no horizontal scrollbar, fixed 480 px width.
     * Long messages are truncated with an ellipsis so they never overflow.
     * A "Show All Alerts" footer button opens the full InventoryAlertsModal.
     */
    private void openAlertsPopup() {
        final int POPUP_W = 480;
        final int ROW_H = 40;
        final int MAX_ROWS = 5;
        final int BADGE_W_CRIT = 62, BADGE_W_WARN = 60, BADGE_W_INFO = 36;
        final int H_PAD = 14;
        final int BADGE_GAP = 8;

        JPopupMenu popup = new JPopupMenu();
        popup.setLayout(new BorderLayout());
        popup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        popup.setBackground(Color.WHITE);

        // ── Header ───────────────────────────────────────────────────────────
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(11, 14, 10, 14)));

        JLabel headerLbl = new JLabel("Inventory Alerts");
        headerLbl.setFont(FONT_BOLD);
        headerLbl.setForeground(TEXT_PRIMARY);
        headerLbl.setOpaque(false);

        if (!latestAlerts.isEmpty()) {
            boolean hasCrit = latestAlerts.stream()
                    .anyMatch(n -> n.getSeverity() == Notification.Severity.CRITICAL);
            Color cntColor = hasCrit ? new Color(0xDC2626) : new Color(0xD97706);
            JLabel cntBadge = new JLabel(String.valueOf(latestAlerts.size())) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(cntColor);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    super.paintComponent(g);
                    g2.dispose();
                }
            };
            cntBadge.setFont(new Font("Segoe UI", Font.BOLD, 10));
            cntBadge.setForeground(Color.WHITE);
            cntBadge.setOpaque(false);
            cntBadge.setHorizontalAlignment(SwingConstants.CENTER);
            cntBadge.setPreferredSize(new Dimension(latestAlerts.size() > 9 ? 26 : 20, 16));

            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
            titleRow.setOpaque(false);
            titleRow.add(headerLbl);
            titleRow.add(cntBadge);
            headerPanel.add(titleRow, BorderLayout.CENTER);
        } else {
            headerPanel.add(headerLbl, BorderLayout.CENTER);
        }
        popup.add(headerPanel, BorderLayout.NORTH);

        // ── Alert list ────────────────────────────────────────────────────────
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(Color.WHITE);

        if (latestAlerts.isEmpty()) {
            JLabel empty = new JLabel("No active alerts — inventory looks healthy.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(new EmptyBorder(14, H_PAD, 14, H_PAD));
            empty.setAlignmentX(Component.LEFT_ALIGNMENT);
            listPanel.add(empty);
        } else {
            FontMetrics msgFm = new JLabel().getFontMetrics(FONT_BODY);

            for (int i = 0; i < latestAlerts.size(); i++) {
                Notification n = latestAlerts.get(i);

                boolean isCrit = n.getSeverity() == Notification.Severity.CRITICAL;
                boolean isWarn = n.getSeverity() == Notification.Severity.WARNING;
                Color rowBg = isCrit ? new Color(0xFEE2E2)
                        : isWarn ? new Color(0xFEF3C7)
                                : new Color(0xE0F2FE);
                Color badgeFg = isCrit ? new Color(0xDC2626)
                        : isWarn ? new Color(0xD97706)
                                : new Color(0x0284C7);
                String sevText = isCrit ? "CRITICAL" : isWarn ? "WARNING" : "INFO";
                int badgePx = isCrit ? BADGE_W_CRIT : isWarn ? BADGE_W_WARN : BADGE_W_INFO;

                int textBudget = POPUP_W - 2 * H_PAD - badgePx - BADGE_GAP - 12;
                String rawMsg = n.getMessage();
                String dispMsg = rawMsg;
                if (msgFm.stringWidth(rawMsg) > textBudget) {
                    String ellipsis = "…";
                    int ellW = msgFm.stringWidth(ellipsis);
                    StringBuilder sb = new StringBuilder();
                    for (char c : rawMsg.toCharArray()) {
                        if (msgFm.stringWidth(sb.toString() + c) + ellW > textBudget)
                            break;
                        sb.append(c);
                    }
                    dispMsg = sb.toString().stripTrailing() + ellipsis;
                }

                JPanel rowPanel = new JPanel(new BorderLayout(BADGE_GAP, 0));
                rowPanel.setBackground(rowBg);
                rowPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_H - 1));
                rowPanel.setPreferredSize(new Dimension(POPUP_W, ROW_H - 1));
                rowPanel.setBorder(new EmptyBorder(0, H_PAD, 0, H_PAD));
                rowPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

                final Color bFg = badgeFg;
                JLabel badge = new JLabel(sevText) {

                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(bFg);
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                        super.paintComponent(g);
                        g2.dispose();
                    }
                };
                badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
                badge.setForeground(Color.WHITE);
                badge.setOpaque(false);
                badge.setHorizontalAlignment(SwingConstants.CENTER);
                badge.setPreferredSize(new Dimension(badgePx, 18));

                JPanel badgeWrap = new JPanel(new GridBagLayout());
                badgeWrap.setOpaque(false);
                badgeWrap.add(badge);

                JLabel msg = new JLabel(
                        dispMsg);
                msg.setFont(FONT_BODY);
                msg.setForeground(TEXT_PRIMARY);
                if (!dispMsg.equals(rawMsg))
                    msg.setToolTipText(rawMsg);

                rowPanel.add(badgeWrap, BorderLayout.WEST);
                rowPanel.add(msg, BorderLayout.CENTER);
                listPanel.add(rowPanel);

                if (i < latestAlerts.size() - 1) {
                    JSeparator sep = new JSeparator();
                    sep.setForeground(isCrit ? new Color(0xFCA5A5) : new Color(0xE5E7EB));
                    sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
                    sep.setAlignmentX(Component.LEFT_ALIGNMENT);
                    listPanel.add(sep);
                }
            }
        }

        // ── Scroll pane ───────────────────────────────────────────────────────
        JScrollPane scroll = new JScrollPane(
                listPanel);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        int listH = latestAlerts.isEmpty()
                ? 52
                : Math.min(latestAlerts.size(), MAX_ROWS) * ROW_H
                        + 4;
        scroll.setPreferredSize(new Dimension(POPUP_W, listH));
        scroll.getVerticalScrollBar().setUnitIncrement(ROW_H);
        scroll.getViewport().setBackground(Color.WHITE);
        popup.add(scroll, BorderLayout.CENTER);

        // ── "Show All Alerts" footer ──────────────────────────────────────────
        JPanel footer = new JPanel(
                new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR),
                new EmptyBorder(8, 14, 8, 14)));

        JButton showMoreBtn = new JButton("Show All Alerts") {
            private boolean hov;
            {
                setContentAreaFilled(false);
                setBorderPainted(false);
                setFocusPainted(false);
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
                addActionListener(e -> {
                    popup.setVisible(false);
                    Window owner = SwingUtilities.getWindowAncestor(InventoryPanel.this);
                    new InventoryAlertsModal(owner, latestAlerts).setVisible(true);
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? ACCENT_HOVER : ACCENT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(FONT_BODY);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        showMoreBtn.setPreferredSize(new Dimension(POPUP_W - 28, 32));

        footer.add(showMoreBtn, BorderLayout.CENTER);
        popup.add(footer, BorderLayout.SOUTH);

        popup.setPreferredSize(new Dimension(POPUP_W, listH + 42 + 50));

        int xOff = Math.min(0, -(POPUP_W - bellBtn.getWidth()));
        popup.show(bellBtn, xOff, bellBtn.getHeight() + 4);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add / Edit dialogs
    // ─────────────────────────────────────────────────────────────────────────
    private void openAddDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new AddItemModal(owner, inventoryController, () -> {
            refresh();
            if (monitoringRefresh != null)
                monitoringRefresh.run();
            JOptionPane.showMessageDialog(this, "Item added successfully.", "Item Added",
                    JOptionPane.INFORMATION_MESSAGE);
        }).setVisible(true);
    }

    private void openEditDialog(int tableRow) {
        String itemName = inventoryTable.getValueAt(tableRow, 0).toString();
        InventoryItem item = Inventory.getInstance().getItem(itemName);
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Item not found.");
            return;
        }

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = owner instanceof Frame
                ? new JDialog((Frame) owner, "Edit — " + itemName, true)
                : new JDialog((Dialog) owner, "Edit — " + itemName, true);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(20, 24, 16, 24));

        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 0, 6, 12);
        gc.anchor = GridBagConstraints.WEST;

        addDialogRow(content, gc, 0, "Item", new JLabel(item.getName()), false);
        JTextField qtyField = styledField(String.valueOf(item.getQuantity()));
        JTextField alertField = styledField(String.valueOf(item.getAlertLevel()));
        addDialogRow(content, gc, 1, "Quantity", qtyField, true);
        addDialogRow(content, gc, 2, "Alert Level", alertField, true);

        dialog.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, BORDER_COLOR));

        JButton cancel = buildOutlineButton("Cancel");
        cancel.addActionListener(e -> dialog.dispose());

        JButton save = buildPrimaryButton("Save", e -> {
            try {
                Double.parseDouble(qtyField.getText().trim());
                Double.parseDouble(alertField.getText().trim());
                inventoryController.updateItem(itemName, itemName,
                        qtyField.getText().trim(), item.getUnit(), alertField.getText().trim());
                refresh();
                if (monitoringRefresh != null)
                    monitoringRefresh.run();
                dialog.dispose();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Enter valid numbers.", "Validation",
                        JOptionPane.WARNING_MESSAGE);
            }
        });

        footer.add(cancel);
        footer.add(save);
        dialog.add(footer, BorderLayout.SOUTH);
        dialog.pack();
        dialog.setMinimumSize(new Dimension(320, dialog.getHeight()));
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void addDialogRow(JPanel p, GridBagConstraints gc, int row,
            String label, Component field, boolean fill) {
        gc.gridx = 0;
        gc.gridy = row;
        gc.weightx = 0;
        gc.fill = GridBagConstraints.NONE;
        JLabel lbl = new JLabel(label + ":");
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_MUTED);
        p.add(lbl, gc);
        gc.gridx = 1;
        gc.weightx = 1;
        gc.fill = fill ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        p.add(field, gc);
    }

    private JTextField styledField(String text) {
        JTextField f = new JTextField(text, 14);
        f.setFont(FONT_BODY);
        f.setBackground(Color.WHITE);
        f.setForeground(TEXT_PRIMARY);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(5, 8, 5, 8)));
        f.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 1), new EmptyBorder(5, 8, 5, 8)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(5, 8, 5, 8)));
            }
        });
        return f;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Button helpers
    // ─────────────────────────────────────────────────────────────────────────
    private JButton buildPrimaryButton(String text, java.awt.event.ActionListener action) {
        JButton btn = new JButton(text) {
            private boolean hov;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
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
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setFont(FONT_BODY);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(110, 34));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener(action);
        return btn;
    }

    private JButton buildOutlineButton(String text) {
        JButton btn = new JButton(text) {
            private boolean hov;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hov = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hov = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hov ? new Color(0xF3F4F6) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(FONT_BODY);
                g2.setColor(TEXT_SECONDARY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(),
                        (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(88, 34));
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return btn;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Value object for reorder guide column
    // ─────────────────────────────────────────────────────────────────────────
    static final class ReorderInfo {
        final double eoq, rop, quantity;
        final String unit;

        ReorderInfo(double eoq, double rop, double quantity, String unit) {
            this.eoq = eoq;
            this.rop = rop;
            this.quantity = quantity;
            this.unit = unit;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Cell renderers
    // ─────────────────────────────────────────────────────────────────────────
    static class StdRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setFont(FONT_BODY);
            setBorder(new EmptyBorder(0, 12, 0, 8));
            if (!sel) {
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);
                setForeground(TEXT_SECONDARY);
            }
            return this;
        }
    }

    static class AbcTierRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(FONT_BOLD);
            if (!sel) {
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);
                String tier = v == null ? "C" : v.toString();
                setForeground(switch (tier) {
                    case "A" -> STATUS_GOOD_FG;
                    case "B" -> STATUS_LOW_FG;
                    default -> TEXT_MUTED;
                });
            }
            return this;
        }
    }

    static class ReorderGuideRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(FONT_BODY);
            if (!sel)
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);
            if (v instanceof ReorderInfo i) {
                setText("EOQ~" + Math.round(i.eoq) + "  \u00B7  ROP~" + Math.round(i.rop));
                if (!sel)
                    setForeground(i.quantity <= i.rop ? STATUS_EXPIRED_FG : TEXT_MUTED);
            } else {
                setText("\u2014");
                if (!sel)
                    setForeground(TEXT_MUTED);
            }
            return this;
        }
    }

    static class BatchSummaryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(FONT_BODY);
            if (!sel)
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);

            String text = "\u2014";
            Color fg = TEXT_MUTED;

            if (v instanceof InventoryRowView rv) {
                List<InventoryBatch> batches = rv.getBatches();
                if (batches != null && !batches.isEmpty()) {
                    int expired = 0, expiring = 0, active = 0;
                    LocalDate today = LocalDate.now();
                    for (InventoryBatch b : batches) {
                        if (b.isArchived())
                            continue;
                        active++;
                        String exp = b.getExpiryDate();
                        if (exp == null || exp.isBlank())
                            continue;
                        try {
                            LocalDate d = LocalDate.parse(exp);
                            if (!d.isAfter(today))
                                expired++;
                            else if (!d.isAfter(today.plusDays(7)))
                                expiring++;
                        } catch (Exception ignored) {
                        }
                    }
                    if (expired > 0) {
                        text = expired + " expired";
                        fg = STATUS_EXPIRED_FG;
                    } else if (expiring > 0) {
                        text = expiring + " expiring";
                        fg = STATUS_LOW_FG;
                    } else if (active > 0) {
                        text = active + " batch" + (active == 1 ? "" : "es");
                        fg = TEXT_SECONDARY;
                    }
                }
            }
            setText(text);
            if (!sel)
                setForeground(fg);
            return this;
        }
    }

    static class StatusPillRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            String text = v == null ? "" : v.toString();
            Color bg, fg;
            switch (text) {
                case "Low Stock" -> {
                    bg = new Color(0xFAEEDA);
                    fg = STATUS_LOW_FG;
                }
                case "Out of Stock" -> {
                    bg = new Color(0xFFEDD5);
                    fg = STATUS_OUT_FG;
                }
                case "Expired" -> {
                    bg = new Color(0xFCEBEB);
                    fg = STATUS_EXPIRED_FG;
                }
                default -> {
                    bg = new Color(0xEAF3DE);
                    fg = STATUS_GOOD_FG;
                }
            }
            final Color pillBg = bg, pillFg = fg;
            final String pillText = text.isEmpty() ? "Good" : text;

            return new JComponent() {
                {
                    setOpaque(false);
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(sel ? t.getSelectionBackground() : (row % 2 == 0 ? ROW_BASE : ROW_ALT));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setFont(FONT_BADGE);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(pillText), ph = 20, pw = tw + 18;
                    int px = (getWidth() - pw) / 2, py = (getHeight() - ph) / 2;
                    g2.setColor(pillBg);
                    g2.fillRoundRect(px, py, pw, ph, ph, ph);
                    g2.setColor(pillFg);
                    g2.drawString(pillText, px + 9, py + ph - (ph - fm.getAscent()) / 2 - 1);
                    g2.dispose();
                }
            };
        }
    }

    static class DotMenuRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            return new JComponent() {
                {
                    setOpaque(false);
                    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                }

                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(sel ? t.getSelectionBackground() : (row % 2 == 0 ? ROW_BASE : ROW_ALT));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                    g2.setColor(sel ? TEXT_SECONDARY : TEXT_MUTED);
                    FontMetrics fm = g2.getFontMetrics();
                    String dots = "\u2022\u2022\u2022";
                    g2.drawString(dots,
                            (getWidth() - fm.stringWidth(dots)) / 2,
                            (getHeight() + fm.getAscent() - fm.getDescent()) / 2);
                    g2.dispose();
                }
            };
        }
    }

    class DotMenuEditor extends AbstractCellEditor implements TableCellEditor {
        private final JPanel cell = new JPanel(new BorderLayout());
        private int editingRow = -1;

        DotMenuEditor() {
            cell.setOpaque(true);
            JLabel dots = new JLabel("\u2022\u2022\u2022", SwingConstants.CENTER);
            dots.setFont(new Font("Segoe UI", Font.BOLD, 14));
            dots.setForeground(TEXT_MUTED);
            dots.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            cell.add(dots, BorderLayout.CENTER);
            cell.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (editingRow < 0)
                        return;
                    String itemName = inventoryTable.getValueAt(editingRow, 0).toString();

                    JPopupMenu menu = new JPopupMenu();
                    menu.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(BORDER_COLOR, 1),
                            new EmptyBorder(4, 0, 4, 0)));

                    JMenuItem editItem = styledMenuItem("Edit", false);
                    JMenuItem delItem = styledMenuItem("Delete", true);

                    editItem.addActionListener(ev -> {
                        fireEditingStopped();
                        openEditDialog(editingRow);
                    });
                    delItem.addActionListener(ev -> {
                        int ok = JOptionPane.showConfirmDialog(InventoryPanel.this,
                                "<html>Remove <b>" + itemName + "</b> from inventory?</html>",
                                "Confirm Delete", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (ok == JOptionPane.YES_OPTION) {
                            inventoryController.removeItem(itemName);
                            refresh();
                        }
                        fireEditingStopped();
                    });

                    menu.add(editItem);
                    menu.addSeparator();
                    menu.add(delItem);
                    menu.show(cell, e.getX(), e.getY());
                }
            });
        }

        private JMenuItem styledMenuItem(String text, boolean danger) {
            JMenuItem item = new JMenuItem(text);
            item.setFont(FONT_BODY);
            item.setForeground(danger ? STATUS_EXPIRED_FG : TEXT_PRIMARY);
            item.setBorder(new EmptyBorder(6, 14, 6, 14));
            item.setBackground(Color.WHITE);
            item.setOpaque(true);
            return item;
        }

        @Override
        public Object getCellEditorValue() {
            return "\u2022\u2022\u2022";
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int col) {
            editingRow = row;
            cell.setBackground(isSelected ? table.getSelectionBackground() : (row % 2 == 0 ? ROW_BASE : ROW_ALT));
            return cell;
        }
    }
}