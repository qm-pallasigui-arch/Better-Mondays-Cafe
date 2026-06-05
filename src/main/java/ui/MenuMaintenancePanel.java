package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.*;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.table.*;
import persistence.MenuRepository;
import persistence.sqlite.SQLiteMenuRepository;
import pos.*;
import pos.MenuItem;
import util.StringUtil;

public class MenuMaintenancePanel extends JPanel {

    // ── Colors ──────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = new Color(0xF9F9F8);
    private static final Color BG_SURFACE = Color.WHITE;
    private static final Color BG_SUBTLE = new Color(0xF4F3F0);
    private static final Color BORDER_COLOR = new Color(0xE2E0D9);
    private static final Color TEXT_PRIMARY = new Color(0x1A1A18);
    private static final Color TEXT_MUTED = new Color(0x7A7975);
    private static final Color TEXT_HINT = new Color(0xA8A6A0);
    private static final Color ROW_HOVER = new Color(0xF4F3F0);
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

    // ── State ────────────────────────────────────────────────────────────────
    private final MenuRepository repo;
    private final DefaultTableModel model;
    private final JTable table;
    private final JComboBox<String> categoryFilter;
    private final JTextField searchField;
    private List<MenuItem> cachedItems = new ArrayList<>();

    // Detail panel components
    private final JLabel detailName = makeDetailValue();
    private final JLabel detailCat = makeDetailValue();
    private final JLabel priceHot = makePriceLabel();
    private final JLabel priceReg = makePriceLabel();
    private final JLabel priceLarge = makePriceLabel();
    private final JPanel ingredientList = new JPanel();
    private final JLabel statusLabel = new JLabel("0 items");

    // ── Constructor ──────────────────────────────────────────────────────────
    public MenuMaintenancePanel() throws Exception {
        super(new BorderLayout());
        setBackground(BG_PAGE);
        repo = new SQLiteMenuRepository();

        // Table model
        model = new DefaultTableModel(new String[] { "Name", "Category", "Hot", "Iced Regular", "Iced Large" }, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        // Table
        table = buildTable();
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                updateDetailPanel();
        });

        // Toolbar controls
        categoryFilter = new JComboBox<>(
                new String[] { "All", "Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food" });
        searchField = new JTextField(22);
        styleComboBox(categoryFilter);
        styleTextField(searchField, "Search items…");

        // Tabs
        JTabbedPane tabs = buildTabs();
        add(tabs, BorderLayout.CENTER);

        reload();
    }

    // ── Tab setup ────────────────────────────────────────────────────────────
    private JTabbedPane buildTabs() {
        JTabbedPane tabs = new JTabbedPane(JTabbedPane.TOP);
        tabs.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        tabs.setBackground(BG_SURFACE);
        tabs.addTab("Menu Maintenance", buildMenuTab());
        tabs.addTab("Backup & Restore", new BackupPanel());
        return tabs;
    }

    // ── Menu Tab ─────────────────────────────────────────────────────────────
    private JPanel buildMenuTab() {
        JPanel tab = new JPanel(new BorderLayout());
        tab.setBackground(BG_PAGE);
        tab.add(buildToolbar(), BorderLayout.NORTH);
        tab.add(buildContentArea(), BorderLayout.CENTER);
        tab.add(buildStatusBar(), BorderLayout.SOUTH);
        return tab;
    }

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout(10, 0));
        bar.setBackground(BG_SURFACE);
        bar.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 14, 10, 14)));

        // Left: filters
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);
        JLabel catIcon = makeIcon("☰");
        filters.add(catIcon);
        filters.add(categoryFilter);
        filters.add(Box.createHorizontalStrut(4));
        filters.add(makeSearchBox());

        // Right: action buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        actions.setOpaque(false);

        JButton addBtn = makeButton("+ Add", true, false);
        JButton editBtn = makeButton("✎ Edit", false, false);
        JButton deleteBtn = makeButton("⌫ Delete", false, true);

        addBtn.addActionListener(e -> onAdd());
        editBtn.addActionListener(e -> onEdit());
        deleteBtn.addActionListener(e -> onDelete());
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

        actions.add(addBtn);
        actions.add(editBtn);
        actions.add(deleteBtn);

        bar.add(filters, BorderLayout.CENTER);
        bar.add(actions, BorderLayout.EAST);
        return bar;
    }

    private JPanel makeSearchBox() {
        JPanel wrap = new JPanel(new BorderLayout(0, 0));
        wrap.setOpaque(false);
        wrap.setPreferredSize(new Dimension(220, 32));
        JLabel icon = new JLabel("⌕ ");
        icon.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        icon.setForeground(TEXT_HINT);
        wrap.add(icon, BorderLayout.WEST);
        wrap.add(searchField, BorderLayout.CENTER);
        return wrap;
    }

    // ── Content Area (fixed split — detail panel not moveable) ───────────────
    private JPanel buildContentArea() {
        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(BG_SURFACE);

        JPanel detailPanel = buildDetailPanel();

        // Use BorderLayout instead of JSplitPane so the right panel is truly fixed
        JPanel content = new JPanel(new BorderLayout());
        content.setBackground(BORDER_COLOR);
        content.add(tableScroll, BorderLayout.CENTER);
        content.add(detailPanel, BorderLayout.EAST);
        return content;
    }

    // ── Detail Panel ─────────────────────────────────────────────────────────
    private JPanel buildDetailPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SURFACE);
        panel.setBorder(new MatteBorder(0, 1, 0, 0, BORDER_COLOR));
        panel.setPreferredSize(new Dimension(270, 0));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_SUBTLE);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 18, 10, 12)));
        JLabel title = new JLabel("Item Details");
        title.setFont(new Font("Segoe UI", Font.BOLD, 12));
        title.setForeground(TEXT_PRIMARY);
        header.add(title, BorderLayout.WEST);

        // Body (scrollable)
        JPanel body = buildDetailBody();
        JScrollPane scroll = new JScrollPane(body);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_SURFACE);

        panel.add(header, BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildDetailBody() {
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(BG_SURFACE);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        // Name
        body.add(makeFieldRow("NAME", detailName));
        body.add(Box.createVerticalStrut(10));

        // Category
        body.add(makeFieldRow("CATEGORY", detailCat));
        body.add(Box.createVerticalStrut(12));

        // Prices
        JLabel priceLabel = makeDetailLabel("PRICING");
        body.add(priceLabel);
        body.add(Box.createVerticalStrut(5));
        body.add(buildPriceChips());
        body.add(Box.createVerticalStrut(14));

        // Separator
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COLOR);
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        body.add(sep);
        body.add(Box.createVerticalStrut(14));

        // Ingredients
        JLabel ingLabel = makeDetailLabel("INGREDIENTS");
        body.add(ingLabel);
        body.add(Box.createVerticalStrut(6));

        ingredientList.setLayout(new BoxLayout(ingredientList, BoxLayout.Y_AXIS));
        ingredientList.setBackground(BG_SURFACE);
        ingredientList.setAlignmentX(LEFT_ALIGNMENT);
        body.add(ingredientList);

        // Filler
        body.add(Box.createVerticalGlue());

        // Default state
        setDetailEmpty();
        return body;
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
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 9));
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
        detailName.setText("—");
        detailCat.setText("—");
        priceHot.setText("—");
        priceReg.setText("—");
        priceLarge.setText("—");
        ingredientList.removeAll();
        JLabel none = new JLabel("Select a menu item");
        none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        none.setForeground(TEXT_HINT);
        ingredientList.add(none);
        ingredientList.revalidate();
        ingredientList.repaint();
    }

    private void updateDetailPanel() {
        int row = table.getSelectedRow();
        if (row < 0 || row >= model.getRowCount()) {
            setDetailEmpty();
            return;
        }
        String name = String.valueOf(model.getValueAt(row, 0));
        Optional<MenuItem> opt = cachedItems.stream()
                .filter(i -> StringUtil.normalizeName(i.getName()).equals(StringUtil.normalizeName(name)))
                .findFirst();

        if (opt.isEmpty()) {
            setDetailEmpty();
            return;
        }
        MenuItem item = opt.get();

        detailName.setText(item.getName());
        detailCat.setText(item.getCategory());
        priceHot.setText(item.getHotPrice() > 0 ? "₱" + (int) item.getHotPrice() : "—");
        priceReg.setText(item.getIcedRegularPrice() > 0 ? "₱" + (int) item.getIcedRegularPrice() : "—");
        priceLarge.setText(item.getIcedLargePrice() > 0 ? "₱" + (int) item.getIcedLargePrice() : "—");

        ingredientList.removeAll();
        if (item.getIngredients().isEmpty()) {
            JLabel none = new JLabel("None configured");
            none.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            none.setForeground(TEXT_HINT);
            ingredientList.add(none);
        } else {
            item.getIngredients().forEach((ing, qty) -> ingredientList.add(buildIngRow(ing, qty)));
        }
        ingredientList.revalidate();
        ingredientList.repaint();
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
        n.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        n.setForeground(TEXT_PRIMARY);
        JLabel q = new JLabel(qty % 1 == 0 ? (int) qty + "g" : qty + "g");
        q.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 11));
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
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
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
        t.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        t.setRowHeight(34);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setSelectionBackground(ROW_SELECTED);
        t.setSelectionForeground(TEXT_PRIMARY);
        t.setFillsViewportHeight(true);
        t.setBackground(BG_SURFACE);
        t.setForeground(TEXT_PRIMARY);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Header styling
        JTableHeader header = t.getTableHeader();
        header.setFont(new Font("Segoe UI", Font.BOLD, 11));
        header.setBackground(BG_SUBTLE);
        header.setForeground(TEXT_MUTED);
        header.setBorder(new MatteBorder(0, 0, 1, 0, BORDER_COLOR));
        header.setReorderingAllowed(false);
        header.setResizingAllowed(true);

        // Column widths
        int[] widths = { 200, 110, 75, 100, 95 };
        for (int i = 0; i < widths.length; i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        // Right-align price columns
        DefaultTableCellRenderer right = new DefaultTableCellRenderer();
        right.setHorizontalAlignment(SwingConstants.RIGHT);
        for (int i = 2; i <= 4; i++)
            t.getColumnModel().getColumn(i).setCellRenderer(right);

        // Category pill renderer
        t.getColumnModel().getColumn(1).setCellRenderer(new CategoryPillRenderer());

        // Hover highlight
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
            if (cachedItems == null) {
                cachedItems = new ArrayList<>();
            }

            // If database is empty, try to seed it
            if (cachedItems.isEmpty()) {
                try {
                    persistence.Phase2Bootstrap.seedCatalogIfEmpty();
                    cachedItems = repo.findAll();
                    if (cachedItems == null) {
                        cachedItems = new ArrayList<>();
                    }
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
        String cat = categoryFilter.getSelectedItem() == null ? "All" : categoryFilter.getSelectedItem().toString();
        String fieldText = searchField.getText() == null ? "" : searchField.getText().trim();
        // Treat placeholder text as empty search
        String q = fieldText.equals("Search items…") ? "" : fieldText.toLowerCase();
        int count = 0;

        if (cachedItems != null) {
            for (MenuItem item : cachedItems) {
                boolean catOk = "All".equalsIgnoreCase(cat)
                        || normalizeCategory(item.getCategory()).equalsIgnoreCase(normalizeCategory(cat));
                boolean srchOk = q.isEmpty() || item.getName().toLowerCase().contains(q)
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
        return p > 0 ? "₱" + (int) p : "—";
    }

    private void onAdd() {
        MenuItemDialog dlg = new MenuItemDialog(SwingUtilities.getWindowAncestor(this), null);
        dlg.setLocationRelativeTo(this);
        dlg.setVisible(true);
        if (!dlg.isConfirmed())
            return;
        try {
            MenuItem item = createMenuItem(normalizeCategory(dlg.getCategory().trim()), dlg.getName().trim(),
                    dlg.getHot(), dlg.getIcedRegular(), dlg.getIcedLarge());
            applyIngredientsToItem(item, dlg);
            Menu.getInstance().saveItem(item);
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
            if (!StringUtil.normalizeName(name).equals(StringUtil.normalizeName(dlg.getName().trim())))
                Menu.getInstance().removeItem(name);
            Menu.getInstance().saveItem(edited);
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
    private MenuItem createMenuItem(String category, String name, double hot, double reg, double large) {
        return switch (category) {
            case "Coffee" -> new CoffeeItem(name, hot, reg, large);
            case "Non-Coffee" -> new NonCoffeeItem(name, hot, reg, large);
            case "Fruit Tea" -> new FruitTeaItem(name, hot, reg, large);
            case "Herbal Tea" -> new HerbalTeaItem(name, hot, reg, large);
            case "Food" -> new FoodItem(name, "Food", hot > 0 ? hot : (reg > 0 ? reg : large));
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
                    item.addIngredient(StringUtil.normalizeName(kv[0].trim()), Double.parseDouble(kv[1].trim()));
                } catch (Exception ignored) {
                }
            }
        }
    }

    // ── Factory helpers ───────────────────────────────────────────────────────
    private static JLabel makeDetailLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 10));
        l.setForeground(TEXT_HINT);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel makeDetailValue() {
        JLabel l = new JLabel("—");
        l.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        l.setForeground(TEXT_PRIMARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private static JLabel makePriceLabel() {
        JLabel l = new JLabel("—");
        l.setFont(new Font("Segoe UI Semibold", Font.PLAIN, 13));
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    private static JLabel makeIcon(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        l.setForeground(TEXT_HINT);
        return l;
    }

    private static void styleTextField(JTextField field, String placeholder) {
        field.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_SURFACE);
        field.setCaretColor(ACCENT);
        field.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(5, 8, 5, 8)));
        field.setPreferredSize(new Dimension(200, 30));
        // Placeholder
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
        box.setFont(new Font("Segoe UI", Font.PLAIN, 13));
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
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
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
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
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
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int col) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
            String cat = value == null ? "" : value.toString();
            Color[] colors = PILL_COLORS.getOrDefault(cat, new Color[] { BG_SUBTLE, TEXT_MUTED });
            label.setOpaque(true);
            label.setBackground(isSelected ? ROW_SELECTED : colors[0]);
            label.setForeground(colors[1]);
            label.setFont(new Font("Segoe UI", Font.BOLD, 11));
            label.setBorder(new EmptyBorder(2, 8, 2, 8));
            label.setHorizontalAlignment(SwingConstants.LEFT);
            return label;
        }
    }
}