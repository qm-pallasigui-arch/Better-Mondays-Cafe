package ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.function.Consumer;

/**
 * OrderQueuePanel — Kanban-style kitchen order queue with three swim lanes:
 * Pending → Preparing → Ready
 *
 * Wiring (done once, in your main frame / app entry point):
 *
 * OrderingPanel orderingPanel = new OrderingPanel(onRefresh);
 * OrderQueuePanel orderQueuePanel = new OrderQueuePanel();
 *
 * // POS → Kitchen: new card appears when a transaction is confirmed
 * orderingPanel.setOrderQueuePanel(orderQueuePanel);
 *
 * // Kitchen → POS: "Complete" in kitchen marks order done in the POS strip
 * orderQueuePanel.setOnKitchenCompleted(posOrderId ->
 * SwingUtilities.invokeLater(() ->
 * orderingPanel.markOrderCompletedFromKitchen(posOrderId)));
 */
public class OrderQueuePanel extends JPanel {

    // ─── Receipt Object ─────────────────────────────────────────
    /**
     * Complete receipt data from the POS system.
     * Contains all order details needed for kitchen display and tracking.
     */
    public static class Receipt {
        public final int orderId;
        public final String customerName;
        public final List<ReceiptItem> items;
        public final String timestamp;
        public final double subtotal;
        public final double vat;
        public final double totalInclusive;
        public final double cash;
        public final double change;
        public final String discountType; // NONE, PWD, SENIOR, etc.

        public Receipt(int orderId, String customerName, List<ReceiptItem> items,
                String timestamp, double subtotal, double vat, double totalInclusive,
                double cash, double change, String discountType) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.items = new ArrayList<>(items);
            this.timestamp = timestamp;
            this.subtotal = subtotal;
            this.vat = vat;
            this.totalInclusive = totalInclusive;
            this.cash = cash;
            this.change = change;
            this.discountType = discountType;
        }
    }

    /**
     * Individual line item on a receipt.
     */
    public static class ReceiptItem {
        public final String description;
        public final int quantity;
        public final double unitPrice;
        public final double lineTotal;

        public ReceiptItem(String description, int quantity, double unitPrice, double lineTotal) {
            this.description = description;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
            this.lineTotal = lineTotal;
        }

        @Override
        public String toString() {
            return description + " x" + quantity;
        }
    }

    // ─── Palette ───────────────────────────────────────────────────
    private static final Color BG = new Color(0xF5, 0xF6, 0xFA);
    private static final Color LANE_BG = Color.WHITE;
    private static final Color HEADER_BG = Color.WHITE;
    private static final Color CARD_BG = new Color(0xF8, 0xF9, 0xFC);
    private static final Color CARD_BORDER = new Color(0xE3, 0xE6, 0xF0);
    private static final Color LANE_BORDER = new Color(0xE3, 0xE6, 0xF0);
    private static final Color TOPBAR_BG = new Color(0x25, 0x63, 0xEB);

    private static final Color TEXT_PRIMARY = new Color(0x1A, 0x23, 0x40);
    private static final Color TEXT_SECONDARY = new Color(0x4B, 0x5B, 0x72);
    private static final Color TEXT_MUTED = new Color(0x8A, 0x9A, 0xB0);
    private static final Color TEXT_EMPTY = new Color(0xB0, 0xBD, 0xD0);

    private static final Color ACCENT_PENDING = new Color(0xF5, 0x9E, 0x0B);
    private static final Color ACCENT_PROGRESS = new Color(0x25, 0x63, 0xEB);
    private static final Color ACCENT_READY = new Color(0x10, 0xB9, 0x81);

    private static final Color BADGE_PENDING_BG = new Color(0xFE, 0xF3, 0xC7);
    private static final Color BADGE_PENDING_FG = new Color(0x92, 0x40, 0x0E);
    private static final Color BADGE_PROGRESS_BG = new Color(0xDB, 0xEA, 0xFE);
    private static final Color BADGE_PROGRESS_FG = new Color(0x1E, 0x40, 0xAF);
    private static final Color BADGE_READY_BG = new Color(0xD1, 0xFA, 0xE5);
    private static final Color BADGE_READY_FG = new Color(0x06, 0x5F, 0x46);

    private static final Color BTN_START = new Color(0x25, 0x63, 0xEB);
    private static final Color BTN_START_HOVER = new Color(0x1D, 0x4E, 0xD8);
    private static final Color BTN_READY = new Color(0x10, 0xB9, 0x81);
    private static final Color BTN_READY_HOVER = new Color(0x05, 0x96, 0x69);
    private static final Color BTN_COMPLETE = new Color(0x10, 0xB9, 0x81);
    private static final Color BTN_COMPLETE_HOVER = new Color(0x05, 0x96, 0x69);
    private static final Color BTN_CANCEL_BG = new Color(0xFE, 0xE2, 0xE2);
    private static final Color BTN_CANCEL_FG = new Color(0x99, 0x1B, 0x1B);
    private static final Color BTN_CANCEL_HOVER = new Color(0xFE, 0xCA, 0xCA);

    // ─── Order model ───────────────────────────────────────────────
    private static class OrderCard {
        /** The order ID that ties this card back to the POS CompletedOrder. */
        public final int posOrderId;
        public final String customerName;
        public final List<String> items;
        public final LocalTime createdAt;
        public String status; // "PENDING" | "PROGRESS" | "READY" | "DONE"
        public Receipt receipt; // Full receipt data (if available)

        public OrderCard(int posOrderId, String customerName, List<String> items) {
            this.posOrderId = posOrderId;
            this.customerName = customerName;
            this.items = new ArrayList<>(items);
            this.createdAt = LocalTime.now();
            this.status = "PENDING";
            this.receipt = null;
        }

        public OrderCard(Receipt receipt) {
            this.posOrderId = receipt.orderId;
            this.customerName = receipt.customerName;
            this.items = new ArrayList<>();
            for (ReceiptItem item : receipt.items) {
                this.items.add(item.description + " x" + item.quantity);
            }
            this.createdAt = LocalTime.now();
            this.status = "PENDING";
            this.receipt = receipt;
        }
    }

    // ─── State ─────────────────────────────────────────────────────
    private final List<OrderCard> pendingOrders = new ArrayList<>();
    private final List<OrderCard> progressOrders = new ArrayList<>();
    private final List<OrderCard> readyOrders = new ArrayList<>();

    /**
     * All orders ever added, keyed by posOrderId, so we can find them quickly
     * when the POS panel calls markOrderCompleted().
     */
    private final Map<Integer, OrderCard> allOrdersById = new HashMap<>();

    private JPanel pendingCardsPanel;
    private JPanel progressCardsPanel;
    private JPanel readyCardsPanel;

    private JLabel pendingBadge;
    private JLabel progressBadge;
    private JLabel readyBadge;

    private JLabel statPendingVal;
    private JLabel statProgressVal;
    private JLabel statReadyVal;
    private JLabel statCompletedVal;

    private int completedCount = 0;

    /**
     * Optional callback fired when the kitchen marks an order "Complete" or
     * "Cancelled". Receives the posOrderId so OrderingPanel can update its own
     * strip. Set via {@link #setOnKitchenCompleted(Consumer)}.
     */
    private Consumer<Integer> onKitchenCompleted;

    // ─── Constructor ───────────────────────────────────────────────
    public OrderQueuePanel() {
        setLayout(new BorderLayout(0, 0));
        setBackground(BG);
        rebuildLayout();
    }

    // ─── Public API ────────────────────────────────────────────────

    /**
     * Register a callback that fires when the kitchen marks an order "Complete"
     * or cancels it. Use this to keep the POS order strip in sync:
     *
     * <pre>
     * orderQueuePanel.setOnKitchenCompleted(
     *         posOrderId -> SwingUtilities.invokeLater(
     *                 () -> orderingPanel.markOrderCompletedFromKitchen(posOrderId)));
     * </pre>
     */
    public void setOnKitchenCompleted(Consumer<Integer> callback) {
        this.onKitchenCompleted = callback;
    }

    /**
     * Called by OrderingPanel after a transaction is confirmed.
     * Adds a new order card to the Pending lane with full receipt data.
     *
     * @param receipt the complete receipt object containing all order details
     */
    public void addOrder(Receipt receipt) {
        OrderCard card = new OrderCard(receipt);
        allOrdersById.put(receipt.orderId, card);
        pendingOrders.add(card);
        SwingUtilities.invokeLater(this::refreshAllLanes);
    }

    /**
     * Called by OrderingPanel after a transaction is confirmed.
     * Adds a new order card to the Pending lane, keyed by the POS order ID.
     *
     * @param posOrderId   the order ID from OrderingPanel (used for cross-panel
     *                     sync)
     * @param customerName the customer's name
     * @param items        human-readable item strings, e.g. "Iced Latte (Regular
     *                     Iced) x2"
     */
    public void addOrder(int posOrderId, String customerName, List<String> items) {
        OrderCard card = new OrderCard(posOrderId, customerName, items);
        allOrdersById.put(posOrderId, card);
        pendingOrders.add(card);
        SwingUtilities.invokeLater(this::refreshAllLanes);
    }

    /**
     * Legacy overload kept for any existing callers that do not supply a
     * posOrderId.
     * Assigns a negative synthetic ID to avoid collisions with real POS IDs.
     */
    public void addOrder(String customerName, List<String> items) {
        addOrder(-(allOrdersById.size() + 1), customerName, items);
    }

    /**
     * Called by OrderingPanel when staff mark an order as done from the POS side.
     * If the kitchen card is still in any active lane it is removed and the
     * completed count incremented, keeping both panels in sync.
     *
     * @param posOrderId the POS order ID to mark as completed
     */
    public void markOrderCompleted(int posOrderId) {
        OrderCard card = allOrdersById.get(posOrderId);
        if (card == null || "DONE".equals(card.status))
            return;

        boolean removed = pendingOrders.remove(card)
                || progressOrders.remove(card)
                || readyOrders.remove(card);

        if (removed) {
            card.status = "DONE";
            completedCount++;
            SwingUtilities.invokeLater(this::refreshAllLanes);
        }
    }

    // ─── Layout ────────────────────────────────────────────────────

    private void rebuildLayout() {
        removeAll();
        setLayout(new BorderLayout(0, 0));
        add(buildTopBar(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 0));
        center.setOpaque(false);
        center.add(buildStatsStrip(), BorderLayout.NORTH);
        center.add(buildBody(), BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);

        revalidate();
        repaint();
    }

    // ─── Top Bar ───────────────────────────────────────────────────

    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout(12, 0));
        bar.setBackground(TOPBAR_BG);
        bar.setBorder(new EmptyBorder(12, 20, 12, 20));

        JLabel title = new JLabel("Kitchen");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Color.WHITE);
        bar.add(title, BorderLayout.WEST);

        return bar;
    }

    // ─── Stats Strip ───────────────────────────────────────────────

    private JPanel buildStatsStrip() {
        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        strip.setBackground(HEADER_BG);
        strip.setBorder(new EmptyBorder(8, 20, 8, 20));

        statPendingVal = new JLabel("0");
        statProgressVal = new JLabel("0");
        statReadyVal = new JLabel("0");
        statCompletedVal = new JLabel("0");

        strip.add(buildStatChip("Pending", statPendingVal, ACCENT_PENDING));
        strip.add(Box.createRigidArea(new Dimension(8, 0)));
        strip.add(buildStatChip("Preparing", statProgressVal, ACCENT_PROGRESS));
        strip.add(Box.createRigidArea(new Dimension(8, 0)));
        strip.add(buildStatChip("Ready", statReadyVal, ACCENT_READY));
        strip.add(Box.createRigidArea(new Dimension(8, 0)));
        strip.add(buildStatChip("Completed", statCompletedVal, TEXT_MUTED));

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(HEADER_BG);
        wrapper.add(strip, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildStatChip(String label, JLabel valueLabel, Color accent) {
        JPanel chip = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 18));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 60));
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
            }
        };
        chip.setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));
        chip.setOpaque(false);
        chip.setBorder(new EmptyBorder(5, 10, 5, 12));

        JPanel dot = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillOval(0, 0, 7, 7);
                g2.dispose();
            }
        };
        dot.setOpaque(false);
        dot.setPreferredSize(new Dimension(7, 7));

        JLabel lbl = new JLabel(label);
        lbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lbl.setForeground(TEXT_MUTED);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        valueLabel.setForeground(accent);

        chip.add(dot);
        chip.add(lbl);
        chip.add(valueLabel);
        return chip;
    }

    // ─── Kanban Body ───────────────────────────────────────────────

    private JPanel buildBody() {
        JPanel body = new JPanel(new GridLayout(1, 3, 12, 0));
        body.setBackground(BG);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        JComponent[] pendingParts = buildLane("Pending", ACCENT_PENDING, BADGE_PENDING_BG, BADGE_PENDING_FG);
        JComponent[] progressParts = buildLane("Preparing", ACCENT_PROGRESS, BADGE_PROGRESS_BG, BADGE_PROGRESS_FG);
        JComponent[] readyParts = buildLane("Ready", ACCENT_READY, BADGE_READY_BG, BADGE_READY_FG);

        pendingCardsPanel = (JPanel) ((JScrollPane) pendingParts[1].getComponent(0)).getViewport().getView();
        progressCardsPanel = (JPanel) ((JScrollPane) progressParts[1].getComponent(0)).getViewport().getView();
        readyCardsPanel = (JPanel) ((JScrollPane) readyParts[1].getComponent(0)).getViewport().getView();

        pendingBadge = (JLabel) pendingParts[2];
        progressBadge = (JLabel) progressParts[2];
        readyBadge = (JLabel) readyParts[2];

        body.add(pendingParts[0]);
        body.add(progressParts[0]);
        body.add(readyParts[0]);
        return body;
    }

    /** Returns [wrapperPanel, scrollWrapper, badgeLabel] */
    private JComponent[] buildLane(String title, Color accent, Color badgeBg, Color badgeFg) {
        JPanel wrapper = new JPanel(new BorderLayout(0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(LANE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setBorder(BorderFactory.createCompoundBorder(
                new RoundedLineBorder(LANE_BORDER, 1, 12),
                new EmptyBorder(0, 0, 8, 0)));

        // Accent stripe at the top
        JPanel stripe = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(accent);
                g2.fillRoundRect(0, 0, getWidth(), getHeight() * 2, 12, 12);
                g2.dispose();
            }
        };
        stripe.setPreferredSize(new Dimension(0, 3));
        stripe.setOpaque(false);

        // Count badge
        JLabel badge = new JLabel("0", SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(badgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
        badge.setForeground(badgeFg);
        badge.setOpaque(false);
        badge.setPreferredSize(new Dimension(26, 20));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        titleLbl.setForeground(accent);

        JPanel headerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        headerRow.setOpaque(false);
        headerRow.add(titleLbl);
        headerRow.add(badge);

        JPanel laneHeader = new JPanel(new BorderLayout());
        laneHeader.setOpaque(false);
        laneHeader.add(stripe, BorderLayout.NORTH);
        laneHeader.add(headerRow, BorderLayout.CENTER);
        laneHeader.setBorder(new EmptyBorder(8, 10, 6, 10));
        wrapper.add(laneHeader, BorderLayout.NORTH);

        JPanel cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(LANE_BG);
        cardsPanel.setBorder(new EmptyBorder(4, 8, 4, 8));

        JScrollPane scroll = new JScrollPane(cardsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(LANE_BG);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        scroll.getVerticalScrollBar().setBackground(LANE_BG);

        JPanel scrollWrapper = new JPanel(new BorderLayout());
        scrollWrapper.setOpaque(false);
        scrollWrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(scrollWrapper, BorderLayout.CENTER);

        return new JComponent[] { wrapper, scrollWrapper, badge };
    }

    // ─── Order Card Widget ─────────────────────────────────────────

    private JPanel buildCardWidget(OrderCard order) {
        JPanel card = new JPanel(new BorderLayout(0, 8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(CARD_BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(10, 12, 10, 12));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));
        card.setAlignmentX(LEFT_ALIGNMENT);

        Color statusBadgeBg = switch (order.status) {
            case "PROGRESS" -> BADGE_PROGRESS_BG;
            case "READY" -> BADGE_READY_BG;
            default -> BADGE_PENDING_BG;
        };
        Color statusBadgeFg = switch (order.status) {
            case "PROGRESS" -> BADGE_PROGRESS_FG;
            case "READY" -> BADGE_READY_FG;
            default -> BADGE_PENDING_FG;
        };
        String statusText = switch (order.status) {
            case "PROGRESS" -> "Preparing";
            case "READY" -> "Ready";
            default -> "Pending";
        };

        // ── Top row: order number + status chip + timestamp ──
        JLabel orderNumLbl = new JLabel("#" + String.format("%05d", order.posOrderId));
        orderNumLbl.setFont(new Font("Segoe UI", Font.BOLD, 14));
        orderNumLbl.setForeground(TEXT_PRIMARY);

        JLabel statusChip = new JLabel(statusText, SwingConstants.CENTER) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(statusBadgeBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                super.paintComponent(g);
                g2.dispose();
            }
        };
        statusChip.setFont(new Font("Segoe UI", Font.BOLD, 10));
        statusChip.setForeground(statusBadgeFg);
        statusChip.setOpaque(false);
        statusChip.setBorder(new EmptyBorder(2, 7, 2, 7));

        JLabel timeLbl = new JLabel(order.createdAt.format(DateTimeFormatter.ofPattern("hh:mm a")));
        timeLbl.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        timeLbl.setForeground(TEXT_MUTED);

        JPanel topLeft = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        topLeft.setOpaque(false);
        topLeft.add(orderNumLbl);
        topLeft.add(statusChip);

        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        topRow.add(topLeft, BorderLayout.WEST);
        topRow.add(timeLbl, BorderLayout.EAST);
        card.add(topRow, BorderLayout.NORTH);

        // ── Customer name + item list ──
        JLabel custLbl = new JLabel(order.customerName);
        custLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        custLbl.setForeground(TEXT_MUTED);

        JPanel itemsPanel = new JPanel();
        itemsPanel.setLayout(new BoxLayout(itemsPanel, BoxLayout.Y_AXIS));
        itemsPanel.setOpaque(false);
        itemsPanel.setBorder(new EmptyBorder(0, 0, 2, 0));
        itemsPanel.add(custLbl);
        itemsPanel.add(Box.createVerticalStrut(4));

        for (String item : order.items) {
            JPanel itemRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 1));
            itemRow.setOpaque(false);

            JPanel bullet = new JPanel() {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(TEXT_MUTED);
                    g2.fillOval(0, 3, 5, 5);
                    g2.dispose();
                }
            };
            bullet.setOpaque(false);
            bullet.setPreferredSize(new Dimension(6, 11));

            JLabel itemLbl = new JLabel(item);
            itemLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            itemLbl.setForeground(TEXT_SECONDARY);

            itemRow.add(bullet);
            itemRow.add(itemLbl);
            itemsPanel.add(itemRow);
        }
        card.add(itemsPanel, BorderLayout.CENTER);

        // ── Action buttons (vary by status) ──
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        actions.setOpaque(false);

        switch (order.status) {

            case "PENDING" -> {
                JButton startBtn = makeRoundedButton("Preparing →", BTN_START, BTN_START_HOVER, Color.WHITE, 10);
                startBtn.addActionListener(e -> {
                    pendingOrders.remove(order);
                    order.status = "PROGRESS";
                    progressOrders.add(order);
                    refreshAllLanes();
                });

                JButton cancelBtn = makeRoundedButton("Cancel", BTN_CANCEL_BG, BTN_CANCEL_HOVER, BTN_CANCEL_FG, 10);
                cancelBtn.addActionListener(e -> {
                    int confirm = JOptionPane.showConfirmDialog(
                            SwingUtilities.windowForComponent(card),
                            "Cancel Order #" + String.format("%05d", order.posOrderId) + "?",
                            "Cancel Order", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                    if (confirm == JOptionPane.YES_OPTION) {
                        pendingOrders.remove(order);
                        order.status = "DONE"; // treat cancellation as done for stat purposes
                        completedCount++;
                        refreshAllLanes();
                        // Notify POS so the strip card also reflects the cancellation
                        if (onKitchenCompleted != null) {
                            onKitchenCompleted.accept(order.posOrderId);
                        }
                    }
                });

                actions.add(startBtn);
                actions.add(cancelBtn);
            }

            case "PROGRESS" -> {
                JButton readyBtn = makeRoundedButton("Mark ready", BTN_READY, BTN_READY_HOVER, Color.WHITE, 10);
                readyBtn.addActionListener(e -> {
                    progressOrders.remove(order);
                    order.status = "READY";
                    readyOrders.add(order);
                    refreshAllLanes();
                });
                actions.add(readyBtn);
            }

            case "READY" -> {
                JButton doneBtn = makeRoundedButton("Complete", BTN_COMPLETE, BTN_COMPLETE_HOVER, Color.WHITE, 10);
                doneBtn.addActionListener(e -> {
                    readyOrders.remove(order);
                    order.status = "DONE";
                    completedCount++;
                    refreshAllLanes();

                    // ── Notify POS that this order is done ──
                    if (onKitchenCompleted != null) {
                        onKitchenCompleted.accept(order.posOrderId);
                    }

                    JOptionPane.showMessageDialog(
                            SwingUtilities.windowForComponent(card),
                            "Order #" + String.format("%05d", order.posOrderId)
                                    + " (" + order.customerName + ") has been completed.",
                            "Order Complete", JOptionPane.INFORMATION_MESSAGE);
                });
                actions.add(doneBtn);
            }
        }

        JPanel bottomRow = new JPanel(new BorderLayout(6, 0));
        bottomRow.setOpaque(false);
        bottomRow.setBorder(new EmptyBorder(2, 0, 0, 0));
        bottomRow.add(actions, BorderLayout.EAST);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    // ─── Lane Refresh ──────────────────────────────────────────────

    private void refreshAllLanes() {
        rebuildLane(pendingCardsPanel, pendingOrders, pendingBadge, ACCENT_PENDING);
        rebuildLane(progressCardsPanel, progressOrders, progressBadge, ACCENT_PROGRESS);
        rebuildLane(readyCardsPanel, readyOrders, readyBadge, ACCENT_READY);
        updateStats();
    }

    private void rebuildLane(JPanel cardsPanel, List<OrderCard> orders,
            JLabel badge, Color accent) {
        cardsPanel.removeAll();

        if (orders.isEmpty()) {
            cardsPanel.add(Box.createVerticalStrut(24));
            JLabel emptyLbl = new JLabel("No orders here", SwingConstants.CENTER);
            emptyLbl.setFont(new Font("Segoe UI", Font.ITALIC, 12));
            emptyLbl.setForeground(TEXT_EMPTY);
            emptyLbl.setAlignmentX(CENTER_ALIGNMENT);
            emptyLbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
            cardsPanel.add(emptyLbl);
        } else {
            for (OrderCard o : orders) {
                cardsPanel.add(buildCardWidget(o));
                cardsPanel.add(Box.createVerticalStrut(8));
            }
        }

        badge.setText(String.valueOf(orders.size()));
        cardsPanel.revalidate();
        cardsPanel.repaint();
    }

    private void updateStats() {
        statPendingVal.setText(String.valueOf(pendingOrders.size()));
        statProgressVal.setText(String.valueOf(progressOrders.size()));
        statReadyVal.setText(String.valueOf(readyOrders.size()));
        statCompletedVal.setText(String.valueOf(completedCount));
    }

    // ─── Button Factory ────────────────────────────────────────────

    private JButton makeRoundedButton(String text, Color normalBg, Color hoverBg,
            Color fg, int fontSize) {
        boolean[] hovered = { false };
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(hovered[0] ? hoverBg : normalBg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        btn.setFont(new Font("Segoe UI", Font.BOLD, fontSize));
        btn.setForeground(fg);
        btn.setOpaque(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(5, 12, 5, 12));
        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered[0] = true;
                btn.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered[0] = false;
                btn.repaint();
            }
        });
        return btn;
    }

    // ─── Inner: RoundedLineBorder ──────────────────────────────────

    private static class RoundedLineBorder extends javax.swing.border.AbstractBorder {
        private final Color color;
        private final int thickness;
        private final int radius;

        RoundedLineBorder(Color color, int thickness, int radius) {
            this.color = color;
            this.thickness = thickness;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(thickness));
            g2.drawRoundRect(x, y, width - 1, height - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(thickness, thickness, thickness, thickness);
        }
    }
}