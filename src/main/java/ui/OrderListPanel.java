package ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.EnumMap;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

public class OrderListPanel extends JPanel {
    public enum OrderStatus {
        PREPARING("Preparing", new Color(0xFEF3C7), new Color(0xB45309)),
        READY("Ready", new Color(0xDCFCE7), new Color(0x15803D)),
        CANCELLED("Cancelled", new Color(0xFEE2E2), new Color(0xDC2626)),
        COMPLETED("Complete", new Color(0xDBEAFE), new Color(0x1D4ED8));

        private final String label;
        private final Color background;
        private final Color foreground;

        OrderStatus(String label, Color background, Color foreground) {
            this.label = label;
            this.background = background;
            this.foreground = foreground;
        }

        public String getLabel() {
            return label;
        }

        public Color getBackground() {
            return background;
        }

        public Color getForeground() {
            return foreground;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    public static final class OrderRecord {
        private final String orderNumber;
        private final String customerName;
        private final String tableName;
        private final double total;
        private final List<String> items;
        private final Date createdAt;
        private OrderStatus status;

        public OrderRecord(String orderNumber, String customerName, String tableName, double total, List<String> items, OrderStatus status) {
            this.orderNumber = orderNumber;
            this.customerName = customerName;
            this.tableName = tableName;
            this.total = total;
            this.items = new ArrayList<>(items);
            this.createdAt = new Date();
            this.status = status;
        }

        public String getOrderNumber() {
            return orderNumber;
        }

        public String getCustomerName() {
            return customerName;
        }

        public String getTableName() {
            return tableName;
        }

        public double getTotal() {
            return total;
        }

        public List<String> getItems() {
            return items;
        }

        public Date getCreatedAt() {
            return createdAt;
        }

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(OrderStatus status) {
            this.status = status;
        }
    }

    private static final AtomicInteger SAMPLE_COUNTER = new AtomicInteger(1);
    private final List<OrderRecord> allOrders = new ArrayList<>();
    private final JPanel listPanel = new JPanel();
    private final JLabel countLabel = new JLabel();
    private final Map<OrderStatus, JButton> filterButtons = new EnumMap<>(OrderStatus.class);
    private OrderStatus activeFilter = null;
    private Runnable refreshCallback;

    private static final Font TITLE_FONT = new Font("Segoe UI", Font.BOLD, 26);
    private static final Font BODY_FONT = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font BOLD_FONT = new Font("Segoe UI", Font.BOLD, 13);
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("MMM d, h:mm a");

    public OrderListPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_PRIMARY);
        setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        add(buildHeader(), BorderLayout.NORTH);

        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setOpaque(false);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.getViewport().setBackground(AppTheme.BG_PRIMARY);
        add(scrollPane, BorderLayout.CENTER);

        seedSampleData();
        refreshList();
    }

    public void addOrder(String orderNumber, String customerName, String tableName, double total, List<String> items) {
        allOrders.add(0, new OrderRecord(orderNumber, customerName, tableName, total, items, OrderStatus.PREPARING));
        refreshList();
    }

    public void addOrder(String orderNumber, String customerName, String tableName, double total, String... items) {
        addOrder(orderNumber, customerName, tableName, total, Arrays.asList(items));
    }

    public void setRefreshCallback(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    public List<OrderRecord> getRecentOrders(int max) {
        return allOrders.stream()
            .sorted(Comparator.comparing(OrderRecord::getCreatedAt).reversed())
            .limit(Math.max(0, max))
            .toList();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout(0, 12));
        header.setOpaque(false);

        JLabel title = new JLabel("Order List");
        title.setFont(TITLE_FONT);
        title.setForeground(AppTheme.FG_PRIMARY);
        header.add(title, BorderLayout.NORTH);

        JPanel metaRow = new JPanel(new BorderLayout());
        metaRow.setOpaque(false);

        JLabel subtitle = new JLabel("Track live orders, filter by status, and update progress without leaving the POS shell.");
        subtitle.setFont(BODY_FONT);
        subtitle.setForeground(AppTheme.FG_MUTED);
        metaRow.add(subtitle, BorderLayout.WEST);

        countLabel.setFont(BOLD_FONT);
        countLabel.setForeground(AppTheme.FG_PRIMARY);
        metaRow.add(countLabel, BorderLayout.EAST);

        header.add(metaRow, BorderLayout.CENTER);
        header.add(buildFilters(), BorderLayout.SOUTH);
        return header;
    }

    private JPanel buildFilters() {
        JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filters.setOpaque(false);

        JButton allButton = createFilterButton("All", null, true);
        filters.add(allButton);

        for (OrderStatus status : OrderStatus.values()) {
            JButton button = createFilterButton(status.getLabel(), status, false);
            filterButtons.put(status, button);
            filters.add(button);
        }
        return filters;
    }

    private JButton createFilterButton(String text, OrderStatus status, boolean allButton) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        button.setFont(BODY_FONT);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.addActionListener(e -> {
            activeFilter = status;
            refreshList();
            refreshFilterStyles(allButton ? null : status);
        });
        if (allButton) {
            button.setName("ALL_FILTER");
        }
        return button;
    }

    private void refreshFilterStyles(OrderStatus selectedStatus) {
        for (Map.Entry<OrderStatus, JButton> entry : filterButtons.entrySet()) {
            JButton button = entry.getValue();
            boolean selected = entry.getKey() == selectedStatus;
            button.setBackground(selected ? AppTheme.ACCENT : AppTheme.BG_SURFACE);
            button.setForeground(selected ? Color.WHITE : AppTheme.FG_PRIMARY);
        }
    }

    private void refreshList() {
        listPanel.removeAll();

        long visibleCount = allOrders.stream()
            .filter(order -> activeFilter == null || order.getStatus() == activeFilter)
            .count();
        countLabel.setText(visibleCount + " order" + (visibleCount == 1 ? "" : "s") + " shown");

        if (allOrders.isEmpty() || visibleCount == 0) {
            listPanel.add(createEmptyState());
        } else {
            for (OrderRecord order : allOrders) {
                if (activeFilter != null && order.getStatus() != activeFilter) {
                    continue;
                }
                listPanel.add(createOrderCard(order));
                listPanel.add(Box.createVerticalStrut(12));
            }
        }

        refreshFilterStyles(activeFilter);
        listPanel.revalidate();
        listPanel.repaint();
        if (refreshCallback != null) {
            SwingUtilities.invokeLater(refreshCallback);
        }
    }

    private JPanel createEmptyState() {
        JPanel empty = new JPanel(new GridBagLayout());
        empty.setOpaque(false);
        empty.setBorder(BorderFactory.createEmptyBorder(48, 16, 48, 16));

        JLabel label = new JLabel("No orders match the current filter.");
        label.setFont(BOLD_FONT);
        label.setForeground(AppTheme.FG_MUTED);
        empty.add(label);
        return empty;
    }

    private JPanel createOrderCard(OrderRecord order) {
        JPanel card = new JPanel(new BorderLayout(12, 10));
        card.setBackground(AppTheme.BG_SURFACE);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(AppTheme.BORDER, 1),
            BorderFactory.createEmptyBorder(14, 14, 14, 14)));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JLabel orderNumber = new JLabel(formatOrderNumber(order.getOrderNumber()));
        orderNumber.setFont(new Font("Segoe UI", Font.BOLD, 16));
        orderNumber.setForeground(AppTheme.FG_PRIMARY);
        topRow.add(orderNumber, BorderLayout.WEST);

        JLabel statusBadge = new JLabel(order.getStatus().getLabel());
        statusBadge.setOpaque(true);
        statusBadge.setBackground(order.getStatus().getBackground());
        statusBadge.setForeground(order.getStatus().getForeground());
        statusBadge.setFont(BODY_FONT);
        statusBadge.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        topRow.add(statusBadge, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        JPanel details = new JPanel(new GridBagLayout());
        details.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(3, 0, 3, 0);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.weightx = 1;

        addDetailRow(details, gc, 0, "Customer", order.getCustomerName());
        addDetailRow(details, gc, 1, "Table", order.getTableName());
        addDetailRow(details, gc, 2, "Placed", TIME_FORMAT.format(order.getCreatedAt()));
        addDetailRow(details, gc, 3, "Total", String.format("₱%.2f", order.getTotal()));
        card.add(details, BorderLayout.CENTER);

        JTextArea itemsArea = new JTextArea(String.join("\n", order.getItems()));
        itemsArea.setEditable(false);
        itemsArea.setLineWrap(true);
        itemsArea.setWrapStyleWord(true);
        itemsArea.setOpaque(false);
        itemsArea.setFont(BODY_FONT);
        itemsArea.setForeground(AppTheme.FG_MUTED);
        itemsArea.setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
        card.add(itemsArea, BorderLayout.SOUTH);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        actions.setOpaque(false);
        JComboBox<OrderStatus> statusBox = new JComboBox<>(OrderStatus.values());
        statusBox.setSelectedItem(order.getStatus());
        statusBox.addActionListener(e -> {
            OrderStatus status = (OrderStatus) statusBox.getSelectedItem();
            if (status != null) {
                order.setStatus(status);
                refreshList();
            }
        });
        statusBox.setFont(BODY_FONT);
        actions.add(statusBox);

        card.add(actions, BorderLayout.EAST);
        return card;
    }

    private void addDetailRow(JPanel panel, GridBagConstraints gc, int row, String label, String value) {
        gc.gridy = row;
        gc.gridx = 0;
        gc.weightx = 0.3;
        JLabel left = new JLabel(label + ":");
        left.setFont(BOLD_FONT);
        left.setForeground(AppTheme.FG_SUBTLE);
        panel.add(left, gc);

        gc.gridx = 1;
        gc.weightx = 0.7;
        JLabel right = new JLabel(value);
        right.setFont(BODY_FONT);
        right.setForeground(AppTheme.FG_PRIMARY);
        panel.add(right, gc);
    }

    private void seedSampleData() {
        if (!allOrders.isEmpty()) {
            return;
        }
        allOrders.add(new OrderRecord("TXN001001", "Alex Cruz", "Table 3", 248.00, Arrays.asList(
            "2x Cappuccino", "1x Chicken Sandwich"), OrderStatus.PREPARING));
        allOrders.add(new OrderRecord("TXN001002", "Maria Santos", "Table 1", 185.50, Arrays.asList(
            "1x Latte", "2x Fruit Tea"), OrderStatus.READY));
        allOrders.add(new OrderRecord("TXN001003", "Walk-in", "Counter", 96.00, Arrays.asList(
            "1x Iced Americano"), OrderStatus.COMPLETED));
    }

    private String formatOrderNumber(String orderNumber) {
        if (orderNumber == null || orderNumber.isBlank()) {
            return "#----";
        }
        String digits = orderNumber.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return orderNumber;
        }
        digits = digits.replaceFirst("^0+(?!$)", "");
        return "#" + digits;
    }
}
