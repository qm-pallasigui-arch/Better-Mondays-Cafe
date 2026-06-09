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
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.*;
import java.util.List;

/**
 * InventoryPanel — self-contained panel for the Inventory Management tab.
 *
 * Drop-in replacement for the inline inventory UI that was previously built
 * inside POSSystem.initComponents(). Wiring in POSSystem:
 *
 * InventoryPanel inventoryPanel = new InventoryPanel(
 * currentUserRole == Role.ADMIN, inventoryController, monitoringPanel);
 * contentPanel.add(inventoryPanel, "Inventory");
 *
 * The panel fires its own loadInventoryTable() internally. Call
 * inventoryPanel.refresh() from outside (e.g. after a sale) to reload.
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

    // Status colors
    private static final Color STATUS_GOOD_FG = new Color(0x16A34A);
    private static final Color STATUS_LOW_FG = new Color(0xD97706);
    private static final Color STATUS_OUT_FG = new Color(0xEA580C);
    private static final Color STATUS_EXPIRED_FG = new Color(0xDC2626);

    // Alert pill colors
    private static final Color ALERT_CRITICAL_BG = new Color(0xFEE2E2);
    private static final Color ALERT_CRITICAL_FG = new Color(0xDC2626);
    private static final Color ALERT_WARN_BG = new Color(0xFEF3C7);
    private static final Color ALERT_WARN_FG = new Color(0xD97706);
    private static final Color ALERT_INFO_BG = new Color(0xE0F2FE);
    private static final Color ALERT_INFO_FG = new Color(0x0284C7);

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 22);
    private static final Font FONT_SUB = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

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
    private JLabel bellCountBadge;

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

        // Start live clock
        new javax.swing.Timer(1000, e -> updateDateSubtitle()).start();
        updateDateSubtitle();

        refresh();
    }

    /** Reload data from the controller — call after any inventory mutation. */
    public void refresh() {
        if (inventoryController == null)
            inventoryController = new InventoryController(Inventory.getInstance(), new SQLiteInventoryRepository());
        rowsCache.clear();
        rowsCache.addAll(inventoryController.buildInventoryRows());
        applyFilters();
        refreshAlertsBadge();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Header: title + subtitle + bell
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

        // Bell button (right side)
        bellBtn = new JButton("🔔") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isMouseOver() ? new Color(0xF3F4F6) : BG_CARD);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_COLOR);
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }

            private boolean mouseOver;

            boolean isMouseOver() {
                return mouseOver;
            }

            {
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
            }
        };
        bellBtn.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        bellBtn.setForeground(TEXT_PRIMARY);
        bellBtn.setBackground(BG_CARD);
        bellBtn.setBorder(new EmptyBorder(8, 14, 8, 14));
        bellBtn.setFocusPainted(false);
        bellBtn.setContentAreaFilled(false);
        bellBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bellBtn.addActionListener(e -> showAlertsPopup(bellBtn));

        header.add(bellBtn, BorderLayout.EAST);
        return header;
    }

    private void updateDateSubtitle() {
        if (dateSubtitle != null)
            dateSubtitle.setText("As of " + new SimpleDateFormat("EEEE, MM/dd/yyyy hh:mm:ss a").format(new Date()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Main card: controls + table/detail
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
                g2.setStroke(new java.awt.BasicStroke(1f));
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

        // Left: search + category
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
                        BorderFactory.createLineBorder(ACCENT, 1),
                        new EmptyBorder(6, 10, 6, 10)));
            }

            public void focusLost(java.awt.event.FocusEvent e) {
                if (searchField.getText().isBlank()) {
                    searchField.setText(SEARCH_PLACEHOLDER);
                    searchField.setForeground(TEXT_MUTED);
                }
                searchField.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER_COLOR, 1),
                        new EmptyBorder(6, 10, 6, 10)));
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

        // Right: Add Item (admin only)
        if (isAdmin) {
            JButton addBtn = buildPrimaryButton("+ Add Item", e -> openAddDialog());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(addBtn);
            row.add(right, BorderLayout.EAST);
        }

        return row;
    }

    // ── Table + detail side-panel ─────────────────────────────────────────────
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
                    c.setBackground(row == hoveredRow ? ROW_HOVER
                            : row % 2 == 0 ? ROW_BASE : ROW_ALT);
                return c;
            }
        };

        // Header
        inventoryTable.getTableHeader().setBackground(Color.WHITE);
        inventoryTable.getTableHeader().setForeground(TEXT_MUTED);
        inventoryTable.getTableHeader().setFont(FONT_HEADER);
        inventoryTable.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_COLOR));
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

        // Column widths
        int[] widths = { 160, 100, 100, 60, 170, 130, 100, 72 };
        for (int i = 0; i < widths.length; i++)
            inventoryTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        // Default left-padded renderer
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

        // Hover tracking
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
                // Click on Batches column → open batch modal
                if (col == 5) {
                    Object val = tableModel.getValueAt(row, 5);
                    if (val instanceof InventoryRowView rv && inventoryController != null) {
                        new InventoryBatchModal(
                                SwingUtilities.getWindowAncestor(InventoryPanel.this),
                                rv.getName(), inventoryController,
                                () -> refresh()).setVisible(true);
                    }
                } else if (col == 7 && isAdmin) {
                    inventoryTable.editCellAt(row, col);
                }
            }
        });

        // Selection → update detail panel
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
                g2.setStroke(new java.awt.BasicStroke(1f));
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

        detailArea = new JTextArea("Select an ingredient to see its full stock and menu usage.");
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setFont(FONT_BODY);
        detailArea.setForeground(TEXT_PRIMARY);
        detailArea.setBackground(BG_SURFACE);
        detailArea.setBorder(new EmptyBorder(6, 6, 6, 6));

        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        detailScroll.getViewport().setBackground(BG_SURFACE);
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
        if (detailArea == null || inventoryTable == null)
            return;
        int sel = inventoryTable.getSelectedRow();
        if (sel < 0) {
            detailArea.setText("Select an ingredient to see details.");
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

            StringBuilder sb = new StringBuilder();
            sb.append("Name: ").append(r.getName()).append('\n');
            sb.append("Quantity: ").append(r.getQuantity()).append(' ').append(r.getUnit()).append('\n');
            sb.append("Alert Level: ").append(r.getAlertLevel()).append(' ').append(r.getUnit()).append('\n');
            sb.append("Status: ").append(r.getStatus()).append('\n');
            sb.append("Updated: ")
                    .append(r.getLastUpdated() == null || r.getLastUpdated().isBlank() ? "N/A" : r.getLastUpdated())
                    .append('\n');
            sb.append("Categories: ")
                    .append(r.getCategories() == null || r.getCategories().isBlank() ? "N/A" : r.getCategories())
                    .append('\n');
            sb.append('\n');
            sb.append("--- Replenishment Insights ---\n");
            sb.append("ABC Tier: ").append(tier).append(" (")
                    .append(tier.equals("A") ? "high priority — ~80% of usage"
                            : tier.equals("B") ? "medium priority — next ~15%"
                                    : "low priority — remaining ~5%")
                    .append(")\n");
            sb.append("Recommended Order Qty (EOQ): ~").append(Math.round(eoq)).append(' ').append(r.getUnit())
                    .append('\n');
            sb.append("Reorder Point (ROP): ~").append(Math.round(rop)).append(' ').append(r.getUnit());
            if (r.getQuantity() <= rop)
                sb.append("  ← reorder now");
            sb.append('\n');
            sb.append("Deduction order: FEFO\n");

            detailArea.setText(sb.toString());
            detailArea.setCaretPosition(0);
            return;
        }
        detailArea.setText("No detail found for the selected item.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Alerts bell
    // ─────────────────────────────────────────────────────────────────────────
    private void refreshAlertsBadge() {
        if (bellBtn == null)
            return;
        latestAlerts = notificationService.evaluate(rowsCache);
        boolean hasCritical = latestAlerts.stream().anyMatch(n -> n.getSeverity() == Notification.Severity.CRITICAL);
        boolean hasAny = !latestAlerts.isEmpty();
        if (hasCritical) {
            bellBtn.setText("🔔 " + latestAlerts.size());
            bellBtn.setForeground(ALERT_CRITICAL_FG);
        } else if (hasAny) {
            bellBtn.setText("🔔 " + latestAlerts.size());
            bellBtn.setForeground(ALERT_WARN_FG);
        } else {
            bellBtn.setText("🔔");
            bellBtn.setForeground(TEXT_PRIMARY);
        }
    }

    private void showAlertsPopup(Component anchor) {
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBackground(Color.WHITE);
        content.setBorder(new EmptyBorder(14, 14, 14, 14));
        content.setPreferredSize(new Dimension(340, 280));

        JLabel title = new JLabel("Inventory Alerts");
        title.setFont(FONT_BOLD);
        title.setForeground(TEXT_PRIMARY);
        content.add(title, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setOpaque(false);
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));

        if (latestAlerts.isEmpty()) {
            JLabel empty = new JLabel("No alerts — inventory looks healthy.");
            empty.setFont(FONT_BODY);
            empty.setForeground(TEXT_MUTED);
            empty.setBorder(new EmptyBorder(8, 4, 8, 4));
            list.add(empty);
        } else {
            for (Notification n : latestAlerts) {
                Color bg, fg;
                String badge;
                switch (n.getSeverity()) {
                    case CRITICAL -> {
                        bg = ALERT_CRITICAL_BG;
                        fg = ALERT_CRITICAL_FG;
                        badge = "CRITICAL";
                    }
                    case WARNING -> {
                        bg = ALERT_WARN_BG;
                        fg = ALERT_WARN_FG;
                        badge = "WARNING";
                    }
                    default -> {
                        bg = ALERT_INFO_BG;
                        fg = ALERT_INFO_FG;
                        badge = "INFO";
                    }
                }
                JPanel row = new JPanel(new BorderLayout(10, 0));
                row.setBackground(bg);
                row.setBorder(new EmptyBorder(8, 12, 8, 12));
                row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                row.setAlignmentX(Component.LEFT_ALIGNMENT);

                JLabel badgeLbl = new JLabel(badge);
                badgeLbl.setFont(FONT_BADGE);
                badgeLbl.setForeground(fg);
                badgeLbl.setPreferredSize(new Dimension(64, 20));
                row.add(badgeLbl, BorderLayout.WEST);

                JLabel msg = new JLabel(n.getMessage());
                msg.setFont(FONT_BODY);
                msg.setForeground(TEXT_PRIMARY);
                row.add(msg, BorderLayout.CENTER);

                list.add(row);
                list.add(Box.createVerticalStrut(5));
            }
        }

        JScrollPane scroll = new JScrollPane(list);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        content.add(scroll, BorderLayout.CENTER);

        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(BorderFactory.createLineBorder(BORDER_COLOR, 1));
        popup.add(content);
        popup.show(anchor, anchor.getWidth() - content.getPreferredSize().width, anchor.getHeight() + 6);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add / Edit dialogs
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Opens the modernized AddItemModal.
     * Replaces the old sequential JOptionPane chain.
     */
    private void openAddDialog() {
        Window owner = SwingUtilities.getWindowAncestor(this);
        new AddItemModal(owner, inventoryController, () -> {
            refresh();
            if (monitoringRefresh != null)
                monitoringRefresh.run();
            // Brief success toast
            JOptionPane.showMessageDialog(
                    this,
                    "Item added successfully.",
                    "Item Added",
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
                BorderFactory.createLineBorder(BORDER_COLOR, 1),
                new EmptyBorder(5, 8, 5, 8)));
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
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
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
                g2.setStroke(new java.awt.BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setFont(FONT_BODY);
                g2.setColor(TEXT_SECONDARY);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
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
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
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
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
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
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(FONT_BODY);
            if (!sel)
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);
            if (v instanceof ReorderInfo i) {
                setText("EOQ~" + Math.round(i.eoq) + "  ·  ROP~" + Math.round(i.rop));
                if (!sel)
                    setForeground(i.quantity <= i.rop ? STATUS_EXPIRED_FG : TEXT_MUTED);
            } else {
                setText("—");
                if (!sel)
                    setForeground(TEXT_MUTED);
            }
            return this;
        }
    }

    static class BatchSummaryRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
            super.getTableCellRendererComponent(t, v, sel, foc, row, col);
            setHorizontalAlignment(CENTER);
            setFont(FONT_BODY);
            if (!sel)
                setBackground(row % 2 == 0 ? ROW_BASE : ROW_ALT);

            String text = "—";
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
                        text = "⚠ " + expired + " expired";
                        fg = STATUS_EXPIRED_FG;
                    } else if (expiring > 0) {
                        text = "⚠ " + expiring + " expiring";
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
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
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
        public Component getTableCellRendererComponent(
                JTable t, Object v, boolean sel, boolean foc, int row, int col) {
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
                    String dots = "•••";
                    g2.drawString(dots, (getWidth() - fm.stringWidth(dots)) / 2,
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
            JLabel dots = new JLabel("•••", SwingConstants.CENTER);
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
                            BorderFactory.createLineBorder(BORDER_COLOR, 1), new EmptyBorder(4, 0, 4, 0)));

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
            return "···";
        }

        @Override
        public Component getTableCellEditorComponent(
                JTable table, Object value, boolean isSelected, int row, int col) {
            editingRow = row;
            cell.setBackground(isSelected ? table.getSelectionBackground()
                    : (row % 2 == 0 ? ROW_BASE : ROW_ALT));
            return cell;
        }
    }
}