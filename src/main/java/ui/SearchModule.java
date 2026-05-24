package ui;

import persistence.AppDatabase;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.geom.RoundRectangle2D;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class SearchModule extends JPanel {

    // ── Colors ────────────────────────────────────────────────────────────────
    private static final Color BG_PAGE = AppTheme.BG_PRIMARY;
    private static final Color BG_CARD = AppTheme.BG_SURFACE;
    private static final Color BG_INPUT = AppTheme.BG_SURFACE;
    private static final Color BG_HOVER = new Color(0xF3F4F6);
    private static final Color BORDER_SUBTLE = AppTheme.BORDER;
    private static final Color BORDER_FOCUS = AppTheme.ACCENT;
    private static final Color TEXT_PRIMARY = AppTheme.FG_PRIMARY;
    private static final Color TEXT_SECONDARY = AppTheme.FG_MUTED;
    private static final Color TEXT_HINT = AppTheme.FG_SUBTLE;
    private static final Color ACCENT = AppTheme.ACCENT;
    private static final Color ACCENT_HOVER = AppTheme.ACCENT_DARK;
    private static final Color ROW_ALT = new Color(0xF9FAFB);
    private static final Color ROW_HOVER_CLR = AppTheme.BG_BADGE_BLUE;
    private static final Color HEADER_BG = AppTheme.BG_PRIMARY;

    // Status badge colors
    private static final Color MENU_BG = AppTheme.BG_BADGE_BLUE;
    private static final Color MENU_FG = AppTheme.ACCENT_DARK;
    private static final Color INV_BG = AppTheme.BG_BADGE_GREEN;
    private static final Color INV_FG = AppTheme.SUCCESS;
    private static final Color SALE_BG = AppTheme.BG_BADGE_YELLOW;
    private static final Color SALE_FG = AppTheme.WARNING;

    // Availability badge colors
    private static final Color AVAIL_BG = AppTheme.BG_BADGE_GREEN;
    private static final Color AVAIL_FG = AppTheme.SUCCESS;
    private static final Color UNAVAIL_BG = AppTheme.BG_BADGE_RED;
    private static final Color UNAVAIL_FG = AppTheme.DANGER;

    // ── Fonts ─────────────────────────────────────────────────────────────────
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.PLAIN, 17);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BTN = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 10);
    private static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_HEADER = new Font("Segoe UI", Font.PLAIN, 12);

    // ── Fields ────────────────────────────────────────────────────────────────
    private final PlaceholderTextField queryField = new PlaceholderTextField("Search by name, category…");
    private final JComboBox<String> targetBox = new StyledComboBox(new String[] { "Menu", "Inventory", "Sales" });
    private final PlaceholderTextField fromDateField = new PlaceholderTextField("yyyy-MM-dd");
    private final PlaceholderTextField toDateField = new PlaceholderTextField("yyyy-MM-dd");
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new String[] { "Type", "Name", "Details", "Status" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JLabel statusLabel = new JLabel(" ");

    // ── Constructor ───────────────────────────────────────────────────────────
    public SearchModule() {
        super(new BorderLayout());
        setBackground(BG_PAGE);
        setBorder(new EmptyBorder(24, 28, 24, 28));

        add(buildTopBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Top bar: title + controls ─────────────────────────────────────────────
    private JPanel buildTopBar() {
        JPanel wrapper = new JPanel(new BorderLayout(0, 16));
        wrapper.setOpaque(false);
        wrapper.setBorder(new EmptyBorder(0, 0, 16, 0));

        // Title row
        JLabel title = new JLabel("Search");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_PRIMARY);
        wrapper.add(title, BorderLayout.NORTH);

        // Controls card
        CardPanel card = new CardPanel(12, BG_CARD);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));
        card.setLayout(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(0, 0, 0, 10);
        g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = 0;
        g.weightx = 0;

        // Query
        g.gridx = 0;
        g.weightx = 0;
        card.add(fieldLabel("Keyword"), g);
        g.gridx = 1;
        g.weightx = 1;
        AppTheme.styleSearchField(queryField);
        card.add(queryField, g);

        // Target
        g.gridx = 2;
        g.weightx = 0;
        card.add(fieldLabel("Target"), g);
        g.gridx = 3;
        g.weightx = 0.3;
        card.add(targetBox, g);

        // Spacer
        g.gridx = 4;
        g.weightx = 0.6;
        card.add(Box.createHorizontalGlue(), g);

        // Date row
        g.gridy = 1;
        g.insets = new Insets(10, 0, 0, 10);
        g.gridx = 0;
        g.weightx = 0;
        card.add(fieldLabel("From date"), g);
        g.gridx = 1;
        g.weightx = 1;
        AppTheme.styleSearchField(fromDateField);
        card.add(fromDateField, g);
        g.gridx = 2;
        g.weightx = 0;
        card.add(fieldLabel("To date"), g);
        g.gridx = 3;
        g.weightx = 0.3;
        AppTheme.styleSearchField(toDateField);
        card.add(toDateField, g);

        // Search button
        JButton btn = buildSearchButton();
        g.gridx = 4;
        g.weightx = 0;
        g.insets = new Insets(10, 0, 0, 0);
        card.add(btn, g);

        // Allow Enter key on query field
        queryField.addActionListener(e -> runSearch());

        wrapper.add(card, BorderLayout.CENTER);
        return wrapper;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_LABEL);
        l.setForeground(TEXT_SECONDARY);
        return l;
    }

    private void styleInput(JTextField field) {
        field.setFont(FONT_INPUT);
        field.setForeground(TEXT_PRIMARY);
        field.setBackground(BG_INPUT);
        field.setCaretColor(ACCENT);
        field.setBorder(AppTheme.inputBorderRegular());
        field.setPreferredSize(new Dimension(field.getPreferredSize().width, 34));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                field.setBorder(AppTheme.focusInputBorder(BORDER_FOCUS));
            }

            public void focusLost(FocusEvent e) {
                field.setBorder(AppTheme.inputBorderRegular());
            }
        });
    }

    private JButton buildSearchButton() {
        JButton btn = new JButton("Search") {
            private boolean hovered = false;
            {
                addMouseListener(new java.awt.event.MouseAdapter() {
                    public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered ? ACCENT_HOVER : ACCENT);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
                g2.setFont(FONT_BTN);
                g2.setColor(Color.WHITE);
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(getText(), (getWidth() - fm.stringWidth(getText())) / 2,
                        (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(100, 34));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addActionListener((ActionEvent e) -> runSearch());
        return btn;
    }

    // ── Table area ────────────────────────────────────────────────────────────
    private JPanel buildTableArea() {
        JTable table = new JTable(tableModel);
        table.setFont(FONT_TABLE);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER_SUBTLE);
        table.setSelectionBackground(ROW_HOVER_CLR);
        table.setSelectionForeground(TEXT_PRIMARY);
        table.setFocusable(false);
        table.setBackground(BG_CARD);
        table.setIntercellSpacing(new Dimension(0, 0));

        // Column widths
        table.getColumnModel().getColumn(0).setMaxWidth(110);
        table.getColumnModel().getColumn(0).setMinWidth(90);
        table.getColumnModel().getColumn(3).setMaxWidth(150);
        table.getColumnModel().getColumn(3).setMinWidth(90);

        // Header
        JTableHeader header = table.getTableHeader();
        header.setFont(FONT_HEADER);
        header.setBackground(HEADER_BG);
        header.setForeground(TEXT_SECONDARY);
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER_SUBTLE));
        header.setPreferredSize(new Dimension(header.getWidth(), 36));
        ((DefaultTableCellRenderer) header.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);

        // Type column — badge renderer
        table.getColumnModel().getColumn(0).setCellRenderer(new BadgeCellRenderer());

        // Status column — smart renderer for menu availability, inventory alerts, and sales dates
        table.getColumnModel().getColumn(3).setCellRenderer(new StatusCellRenderer());

        // Alternating row renderer for other columns
        DefaultTableCellRenderer altRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int row, int col) {
                super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                setFont(FONT_TABLE);
                setForeground(col == 2 ? TEXT_SECONDARY : TEXT_PRIMARY);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                if (!sel)
                    setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                return this;
            }
        };
        table.getColumnModel().getColumn(1).setCellRenderer(altRenderer);
        table.getColumnModel().getColumn(2).setCellRenderer(altRenderer);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(BG_CARD);
        scroll.setBorder(AppTheme.inputBorderRegular());

        JPanel card = new JPanel(new BorderLayout());
        card.setOpaque(false);
        card.add(scroll, BorderLayout.CENTER);
        return card;
    }

    // ── Status bar ────────────────────────────────────────────────────────────
    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(10, 2, 0, 0));
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(TEXT_HINT);
        bar.add(statusLabel);
        return bar;
    }

    // ── Search logic ──────────────────────────────────────────────────────────
    private void runSearch() {
        tableModel.setRowCount(0);
        String q = queryField.getText().trim().toLowerCase();
        String target = targetBox.getSelectedItem().toString();
        long start = System.currentTimeMillis();
        try {
            switch (target) {
                case "Menu" -> searchMenu(q);
                case "Inventory" -> searchInventory(q);
                case "Sales" -> searchSales(q);
            }
            long ms = System.currentTimeMillis() - start;
            statusLabel.setText(tableModel.getRowCount() + " result" +
                    (tableModel.getRowCount() == 1 ? "" : "s") + "  ·  " + ms + " ms");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Search failed: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            statusLabel.setText("Search failed.");
        }
    }

    private void searchMenu(String q) throws Exception {
        try (Connection c = AppDatabase.openConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT name, category, hot_price, iced_regular_price, iced_large_price, is_available FROM menu_items ORDER BY name");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String cat = rs.getString("category");
                if (!matches(q, name, cat))
                    continue;
                String details = cat
                        + "  ·  Hot ₱" + String.format("%.2f", rs.getDouble("hot_price"))
                        + "  ·  Iced ₱" + String.format("%.2f", rs.getDouble("iced_regular_price"))
                        + " / ₱" + String.format("%.2f", rs.getDouble("iced_large_price"));
                boolean available = rs.getBoolean("is_available");
                tableModel.addRow(new Object[] { "Menu", name, details, available });
            }
        }
    }

    private void searchInventory(String q) throws Exception {
        try (Connection c = AppDatabase.openConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT name, quantity, unit, alert_level FROM inventory_items ORDER BY name");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("name");
                String unit = rs.getString("unit");
                if (!matches(q, name, unit))
                    continue;
                double quantity = rs.getDouble("quantity");
                double alert = rs.getDouble("alert_level");
                String status = quantity <= 0 ? "Out of Stock"
                    : quantity <= alert ? "Low Stock"
                    : "Good";
                String details = "Qty: " + String.format("%.1f", quantity)
                    + " " + unit;
                tableModel.addRow(new Object[] { "Inventory", name, details, status });
            }
        }
    }

    private void searchSales(String q) throws Exception {
        LocalDate from = parseDate(fromDateField.getText().trim());
        LocalDate to = parseDate(toDateField.getText().trim());
        try (Connection c = AppDatabase.openConnection();
                PreparedStatement ps = c.prepareStatement(
                        "SELECT product_name, quantity, total, sold_at FROM sales_records ORDER BY sold_at DESC");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String name = rs.getString("product_name");
                String soldAt = rs.getString("sold_at");
                LocalDate d = parseDateSafe(soldAt);
                if ((from != null && d.isBefore(from)) || (to != null && d.isAfter(to)))
                    continue;
                if (!matches(q, name, soldAt))
                    continue;
                String details = "Qty: " + rs.getInt("quantity")
                        + "  ·  Total: ₱" + String.format("%.2f", rs.getDouble("total"));
                tableModel.addRow(new Object[] { "Sales", name, details, soldAt.substring(0, 10) });
            }
        }
    }

    private boolean matches(String q, String... fields) {
        if (q.isEmpty())
            return true;
        for (String f : fields)
            if (f != null && f.toLowerCase().contains(q))
                return true;
        return false;
    }

    private LocalDate parseDate(String s) {
        if (s == null || s.isBlank())
            return null;
        try {
            return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDate parseDateSafe(String s) {
        try {
            return LocalDate.parse(s.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            return LocalDate.now();
        }
    }

    // ── Inner helpers ─────────────────────────────────────────────────────────

    /**
     * Renders menu availability and inventory alert statuses as pill badges.
     * Sales dates remain plain text.
     */
    private static class StatusCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean foc, int row, int col) {
            Object rowType = table.getValueAt(row, 0);
            if (!(rowType instanceof String type)) {
                super.getTableCellRendererComponent(table, value, sel, foc, row, col);
                setFont(FONT_TABLE);
                setForeground(TEXT_SECONDARY);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                if (!sel)
                    setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                return this;
            }

            if ("Sales".equalsIgnoreCase(type)) {
                super.getTableCellRendererComponent(table, value, sel, foc, row, col);
                setFont(FONT_TABLE);
                setForeground(TEXT_SECONDARY);
                setBorder(new EmptyBorder(0, 14, 0, 14));
                if (!sel)
                    setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
                return this;
            }

            String label;
            Color badgeBg;
            Color badgeFg;
            if ("Menu".equalsIgnoreCase(type)) {
                boolean available = value instanceof Boolean b ? b : Boolean.parseBoolean(String.valueOf(value));
                badgeBg = available ? AVAIL_BG : UNAVAIL_BG;
                badgeFg = available ? AVAIL_FG : UNAVAIL_FG;
                label = available ? "Available" : "Unavailable";
            } else {
                label = value == null ? "Good" : value.toString();
                switch (label) {
                    case "Out of Stock" -> {
                        badgeBg = UNAVAIL_BG;
                        badgeFg = UNAVAIL_FG;
                    }
                    case "Low Stock" -> {
                        badgeBg = SALE_BG;
                        badgeFg = SALE_FG;
                    }
                    default -> {
                        badgeBg = AVAIL_BG;
                        badgeFg = AVAIL_FG;
                    }
                }
            }

            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(sel ? ROW_HOVER_CLR : (row % 2 == 0 ? BG_CARD : ROW_ALT));

            JLabel badge = new JLabel(label) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(badgeBg);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(FONT_BADGE);
            badge.setForeground(badgeFg);
            badge.setOpaque(false);
            badge.setBorder(new EmptyBorder(3, 9, 3, 9));
            badge.setHorizontalAlignment(SwingConstants.CENTER);

            cell.setBorder(new EmptyBorder(0, 10, 0, 10));
            cell.add(badge);
            return cell;
        }
    }

    /** Table cell renderer that draws a colored badge for the type column. */
    private static class BadgeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean sel, boolean foc, int row, int col) {
            JPanel cell = new JPanel(new GridBagLayout());
            cell.setBackground(row % 2 == 0 ? BG_CARD : ROW_ALT);
            if (sel)
                cell.setBackground(ROW_HOVER_CLR);

            String text = value == null ? "" : value.toString();
            Color[] colors = switch (text) {
                case "Menu" -> new Color[] { MENU_BG, MENU_FG };
                case "Inventory" -> new Color[] { INV_BG, INV_FG };
                default -> new Color[] { SALE_BG, SALE_FG };
            };

            JLabel badge = new JLabel(text) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(colors[0]);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            badge.setFont(FONT_BADGE);
            badge.setForeground(colors[1]);
            badge.setOpaque(false);
            badge.setBorder(new EmptyBorder(3, 8, 3, 8));
            badge.setHorizontalAlignment(SwingConstants.CENTER);

            cell.setBorder(new EmptyBorder(0, 10, 0, 10));
            cell.add(badge);
            return cell;
        }
    }

    /** Rounded border. */
    private static class RoundedBorder extends javax.swing.border.AbstractBorder {
        private final int radius;
        private final Color color;

        RoundedBorder(int radius, Color color) {
            this.radius = radius;
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(new RoundRectangle2D.Float(x + 0.4f, y + 0.4f, w - 0.8f, h - 0.8f, radius, radius));
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(radius / 2, radius / 2, radius / 2, radius / 2);
        }
    }

    /** Panel with rounded corners and solid background. */
    private static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bg;

        RoundedPanel(int radius, Color bg) {
            this.radius = radius;
            this.bg = bg;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), radius, radius));
            g2.setColor(BORDER_SUBTLE);
            g2.setStroke(new BasicStroke(0.8f));
            g2.draw(new RoundRectangle2D.Float(0.4f, 0.4f, getWidth() - 0.8f, getHeight() - 0.8f, radius, radius));
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Styled combo box. */
    private static class StyledComboBox extends JComboBox<String> {
        StyledComboBox(String[] items) {
            super(items);
            setFont(FONT_INPUT);
            setForeground(TEXT_PRIMARY);
            setBackground(BG_INPUT);
            setPreferredSize(new Dimension(130, 34));
            setBorder(AppTheme.inputBorderPill());
            setFocusable(false);
        }
    }

    /** Text field with placeholder text. */
    private static class PlaceholderTextField extends JTextField {
        private final String placeholder;

        PlaceholderTextField(String placeholder) {
            this.placeholder = placeholder;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && !isFocusOwner()) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_LCD_HRGB);
                g2.setFont(FONT_INPUT);
                g2.setColor(TEXT_HINT);
                Insets ins = getInsets();
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(placeholder, ins.left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }
}
