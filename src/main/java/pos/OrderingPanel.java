package pos;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.io.File;
import inventory.Inventory;
import inventory.InventoryBatch;
import monitoring.SalesRecord;
import persistence.AppDatabase;
import persistence.sqlite.SQLiteInventoryRepository;
import ui.AppTheme;
import java.awt.Dialog;
import java.time.LocalDate;

public class OrderingPanel extends JPanel {

    // ─── Dark Slate Cafe palette ───
    private static final Color BG_PRIMARY = AppTheme.BG_PRIMARY;
    private static final Color BG_SURFACE = AppTheme.BG_SURFACE;
    private static final Color BG_CARD = AppTheme.BG_SURFACE;
    private static final Color BG_INPUT = AppTheme.BG_SURFACE;
    private static final Color FG_PRIMARY = AppTheme.FG_PRIMARY;
    private static final Color FG_MUTED = AppTheme.FG_MUTED;
    private static final Color ACCENT = AppTheme.ACCENT;
    private static final Color ACCENT_HOVER = AppTheme.ACCENT_DARK;
    private static final Color SUCCESS = AppTheme.SUCCESS;
    private static final Color DANGER = AppTheme.DANGER;
    private static final Color BORDER = AppTheme.BORDER;
    private static final Color TEXT_MUTED = AppTheme.FG_SUBTLE;
    private static final Color LOG_BG = AppTheme.BG_PRIMARY;

    // Status colors
    private static final Color ST_WAITING_BG = AppTheme.BG_BADGE_YELLOW;
    private static final Color ST_WAITING_FG = AppTheme.WARNING;
    private static final Color ST_PREPARING_BG = new Color(0xEDE9FE);
    private static final Color ST_PREPARING_FG = new Color(0x7C3AED);
    private static final Color ST_READY_BG = AppTheme.BG_BADGE_GREEN;
    private static final Color ST_READY_FG = AppTheme.SUCCESS;
    private static final Color ST_DONE_BG = AppTheme.BG_BADGE_BLUE;
    private static final Color ST_DONE_FG = AppTheme.ACCENT;
    private static final Color ST_CANCEL_BG = AppTheme.BG_BADGE_RED;
    private static final Color ST_CANCEL_FG = AppTheme.DANGER;

    // PWD / Senior badge color
    private static final Color ST_PWD_BG = new Color(0xFCE7F3);
    private static final Color ST_PWD_FG = new Color(0xBE185D);

    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 12);
    private static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_BOLD_SM = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_XSMALL = new Font("Segoe UI", Font.PLAIN, 9);
    private static final Font FONT_BADGE = new Font("Segoe UI", Font.BOLD, 9);
    private static final Font FONT_PRICE = new Font("Segoe UI", Font.BOLD, 14);

    // ─── Discount types ───
    private enum DiscountType {
        NONE("None", 0.0),
        PWD("PWD (20%)", 0.20),
        SENIOR("Senior Citizen (20%)", 0.20);

        final String label;
        final double rate;

        DiscountType(String label, double rate) {
            this.label = label;
            this.rate = rate;
        }
    }

    // ─── Menu Data ───
    private static final Map<String, List<String>> BASE_CATEGORY_ITEMS = new LinkedHashMap<>();
    private static final Map<String, List<String>> CATEGORY_ITEMS = new LinkedHashMap<>();
    private static final Map<String, String> NAME_MAP = new HashMap<>();
    private static final Map<String, String> IMAGE_MAP = new HashMap<>();
    private static final Map<String, ImageIcon> imageCache = new HashMap<>();

    static {
        BASE_CATEGORY_ITEMS.put("Espresso & Coffee", Arrays.asList(
                "Hot Brewed Coffee",
                "Hot Americano", "Iced Americano",
                "Hot Latte", "Iced Latte",
                "Hot Cappuccino", "Iced Cappuccino",
                "Hot Salted Cream Latte", "Iced Salted Cream Latte",
                "Hot Spanish Latte", "Iced Spanish Latte",
                "Hot Dark Mocha", "Iced Dark Mocha",
                "Hot White Mocha", "Iced White Mocha",
                "Hot Caramel Macchiato", "Iced Caramel Macchiato"));
        BASE_CATEGORY_ITEMS.put("Specialty Drinks", Arrays.asList(
                "Vietnamese Coffee", "Ube Espresso", "Manila Latte",
                "Iced Pumpkin Spiced Latte", "Iced Spiced Cookie Latte"));
        BASE_CATEGORY_ITEMS.put("Tea Latte", Arrays.asList(
                "Hot Matcha Latte", "Iced Matcha Latte",
                "Hot Chocolate Matcha", "Iced Chocolate Matcha",
                "Hot Matcha Espresso", "Iced Matcha Espresso",
                "Hot Hojicha Latte", "Iced Hojicha Latte",
                "Hot Chai Latte", "Iced Chai Latte"));
        BASE_CATEGORY_ITEMS.put("Non-Coffee", Arrays.asList(
                "Hot Chocolate Latte", "Iced Chocolate Latte",
                "Strawberry Latte", "Iced Mango Latte",
                "Iced Dragon Fruit Coconut Latte", "Ube Latte"));
        BASE_CATEGORY_ITEMS.put("Fruit Tea", Arrays.asList(
                "Strawberry Green Tea", "Mango Green Tea",
                "Peach Green Tea", "Passion Fruit Green Tea"));
        BASE_CATEGORY_ITEMS.put("Herbal Tea", Arrays.asList(
                "Peppermint", "Chamomile", "Earl Grey", "Cinnamon"));
        BASE_CATEGORY_ITEMS.put("Sandwiches", Arrays.asList(
                "Signature Ham & Cheese", "Classic Grilled Cheese", "Homestyle Pesto & Cheese"));
        BASE_CATEGORY_ITEMS.put("Pandesal Pairs", Arrays.asList(
                "Ham & Cheese", "Cheesy Pesto", "Spam & Cheese"));
        BASE_CATEGORY_ITEMS.put("Pastries", Arrays.asList(
                "Chocolate Crinkles", "Signature Chocolate Cookies", "S'mores Cookie",
                "Red Velvet Cream Cheese Cookie", "Brownies", "Banana Loaf Slice",
                "Chocolate Tiramisu", "Matcha Tiramisu", "Creamy Spinach",
                "Blueberry Cheesecake"));

        resetCategoryItemsToSeed();

        NAME_MAP.put("Pumpkin Spiced Latte", "Pumpkin Spice Latte");
        NAME_MAP.put("Signature Chocolate Cookies", "Chocolate Cookies");
        NAME_MAP.put("Banana Loaf Slice", "Banana Bread");

        IMAGE_MAP.put("Pumpkin Spiced Latte", "Iced Pumpkin Spiced Latte");
        IMAGE_MAP.put("Spiced Cookie Latte", "Iced Spiced Cookie Latte");
        IMAGE_MAP.put("Signature Chocolate Cookies", "Signature Cookie");
        IMAGE_MAP.put("S'mores Cookie", "S_mores Cookie");
        IMAGE_MAP.put("Red Velvet Cream Cheese Cookie", "Red Velvet Cream Cheese Cookie");
        IMAGE_MAP.put("Banana Loaf Slice", "banana bread");
        IMAGE_MAP.put("Creamy Spinach", "creamy spinach bread");
        IMAGE_MAP.put("Chocolate Cookies", "chocolate cookies");
        IMAGE_MAP.put("Banana Bread", "banana bread");
    }

    // ─── Item variant helper ───
    private static class ItemSpec {
        String displayName;
        String baseName;
        String variant;
        double price;

        ItemSpec(String displayName, String baseName, String variant, double price) {
            this.displayName = displayName;
            this.baseName = baseName;
            this.variant = variant;
            this.price = price;
        }
    }

    private ItemSpec resolveItem(String name) {
        String lower = name.toLowerCase();
        String baseName, variant;
        if (lower.startsWith("hot ")) {
            baseName = name.substring(4);
            variant = "Hot";
        } else if (lower.startsWith("iced ")) {
            baseName = name.substring(5);
            variant = "Regular Iced";
        } else {
            baseName = name;
            variant = "";
        }
        String backendName = NAME_MAP.getOrDefault(baseName, baseName);
        MenuItem item = Menu.getInstance().getMenuItem(backendName);
        if (item == null)
            return new ItemSpec(name, "", variant, 0);

        double price;
        switch (variant) {
            case "Hot" -> price = item.getHotPrice();
            case "Regular Iced" -> price = item.getIcedRegularPrice();
            default -> {
                price = item.getIcedRegularPrice();
                if (price <= 0)
                    price = item.getHotPrice();
                if (price <= 0)
                    price = item.getIcedLargePrice();
                if (price <= 0)
                    baseName = "";
            }
        }
        if (price <= 0) {
            price = item.getIcedRegularPrice();
            if (price <= 0)
                price = item.getHotPrice();
            if (price <= 0)
                price = item.getIcedLargePrice();
        }
        return new ItemSpec(name, backendName, variant, price);
    }

    // ─── Order Entry ───
    private static class OrderEntry {
        String displayName;
        String baseName;
        String variant;
        int quantity;
        double unitPrice;

        OrderEntry(String displayName, String baseName, String variant,
                int quantity, double unitPrice) {
            this.displayName = displayName;
            this.baseName = baseName;
            this.variant = variant;
            this.quantity = quantity;
            this.unitPrice = unitPrice;
        }

        String orderLabel() {
            return variant.isEmpty() ? displayName : displayName + " (" + variant + ")";
        }

        String fullLabel() {
            return orderLabel();
        }

        double lineTotal() {
            return quantity * unitPrice;
        }
    }

    // ─── Completed Order Record ───
    private static class CompletedOrder {
        int orderId;
        String customerName;
        String timestamp;
        String status;
        List<OrderEntry> items;
        final long placedAtMillis;
        final int itemCount;
        DiscountType discount;
        boolean isPriority;

        CompletedOrder(int orderId, String customerName, String timestamp,
                String status, List<OrderEntry> items, DiscountType discount) {
            this.orderId = orderId;
            this.customerName = customerName;
            this.timestamp = timestamp;
            this.status = status;
            this.items = items;
            this.discount = discount;
            this.isPriority = (discount == DiscountType.PWD || discount == DiscountType.SENIOR);
            this.placedAtMillis = System.currentTimeMillis();
            int count = 0;
            if (items != null)
                for (OrderEntry e : items)
                    count += e.quantity;
            this.itemCount = count;
        }

        double waitMinutes() {
            return (System.currentTimeMillis() - placedAtMillis) / 60000.0;
        }
    }

    private static double orderPriorityScore(CompletedOrder co) {
        double base = co.waitMinutes() + (co.itemCount * 0.5);
        return co.isPriority ? base + 10_000 : base;
    }

    // ─── Repositories ───
    private final SQLiteInventoryRepository batchRepo = new SQLiteInventoryRepository();

    // ─── State ───
    private final List<OrderEntry> orderEntries = new ArrayList<>();
    private final List<CompletedOrder> completedOrders = new ArrayList<>();
    private int orderCount = 0;
    private int transactionCounter;
    private String activeCategory = "Espresso & Coffee";
    private DiscountType currentDiscount = DiscountType.NONE;
    private final List<JButton> pillButtons = new ArrayList<>();
    private final Runnable onRefresh;

    // ─── Kitchen Queue reference (injected after construction) ───
    private ui.OrderQueuePanel orderQueuePanel;

    /**
     * Inject the kitchen queue panel so that both panels stay in sync.
     * Call this immediately after constructing both panels.
     */
    public void setOrderQueuePanel(ui.OrderQueuePanel panel) {
        this.orderQueuePanel = panel;
    }

    /**
     * Called by the kitchen queue panel when staff mark an order "Complete"
     * from the kitchen side. Keeps the POS order strip in sync.
     */
    public void markOrderCompletedFromKitchen(int posOrderId) {
        for (CompletedOrder co : completedOrders) {
            if (co.orderId == posOrderId && !"Completed".equals(co.status)) {
                co.status = "Completed";
                refreshOrderListCards();
                break;
            }
        }
    }

    // ─── UI Components ───
    private JPanel productGridPanel;
    private JPanel orderListCards;
    private JPanel orderItemsPanel;
    private JLabel subtotalLabel;
    private JLabel taxLabel;
    private JLabel discountLabel;
    private JLabel totalLabel;
    private JTextField customerNameField;
    private JLabel orderNumLabel;
    private JButton processBtn;
    private JLabel orderListHeaderCount;
    private JButton discountBtn;

    public OrderingPanel(Runnable onRefresh) {
        this.onRefresh = onRefresh;
        setLayout(new BorderLayout());
        setBackground(BG_PRIMARY);
        transactionCounter = loadTransactionCounter();
        syncDynamicMenuItems();
        buildUI();
        rebuildProductGrid();
        try {
            inventory.Inventory.getInstance().addChangeListener(() -> SwingUtilities.invokeLater(() -> {
                rebuildProductGrid();
                refreshOrderDisplay();
            }));
        } catch (Exception ignored) {
        }

        Menu.getInstance().addChangeListener(() -> SwingUtilities.invokeLater(() -> {
            imageCache.clear();
            rebuildProductGrid();
            refreshOrderDisplay();
        }));

        new javax.swing.Timer(30_000, e -> refreshOrderListCards()).start();
    }

    // ═══════════════════════════════════════════════════════════════
    // UI Construction
    // ═══════════════════════════════════════════════════════════════

    private void buildUI() {
        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(false);
        body.add(buildLeftColumn(), BorderLayout.CENTER);
        body.add(buildRightColumn(), BorderLayout.EAST);
        add(body, BorderLayout.CENTER);
    }

    // ─── Left Column ───
    private JPanel buildLeftColumn() {
        JPanel col = new JPanel(new BorderLayout(0, 8));
        col.setOpaque(false);
        col.setBorder(new EmptyBorder(10, 14, 10, 8));

        JPanel topSection = new JPanel(new BorderLayout(0, 6));
        topSection.setOpaque(false);
        topSection.add(buildOrderListStrip(), BorderLayout.NORTH);
        topSection.add(buildCategoryBar(), BorderLayout.CENTER);
        col.add(topSection, BorderLayout.NORTH);

        productGridPanel = new JPanel(new GridLayout(0, 2, 12, 12));
        productGridPanel.setOpaque(false);
        productGridPanel.setBorder(new EmptyBorder(6, 0, 0, 0));
        JScrollPane gridScroll = new JScrollPane(productGridPanel);
        gridScroll.setBorder(null);
        gridScroll.getViewport().setBackground(BG_PRIMARY);
        gridScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        col.add(gridScroll, BorderLayout.CENTER);

        return col;
    }

    // ─── Order List Strip ───
    private JPanel buildOrderListStrip() {
        JPanel strip = new JPanel(new BorderLayout(0, 6));
        strip.setBackground(LOG_BG);
        strip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(12, 14, 12, 14)));

        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        JLabel lbl = new JLabel("Order Queue");
        lbl.setFont(FONT_SUBTITLE);
        lbl.setForeground(FG_PRIMARY);

        orderListHeaderCount = new JLabel("0 active");
        orderListHeaderCount.setFont(FONT_SMALL);
        orderListHeaderCount.setForeground(FG_MUTED);

        JLabel seeAll = new JLabel("See All");
        seeAll.setFont(FONT_SMALL);
        seeAll.setForeground(ACCENT);
        seeAll.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        seeAll.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                showSeeAllModal();
            }

            public void mouseEntered(MouseEvent e) {
                seeAll.setForeground(ACCENT_HOVER);
            }

            public void mouseExited(MouseEvent e) {
                seeAll.setForeground(ACCENT);
            }
        });

        JLabel hint = new JLabel("Tap card to mark done");
        hint.setFont(FONT_XSMALL);
        hint.setForeground(TEXT_MUTED);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(lbl);
        leftGroup.add(orderListHeaderCount);
        leftGroup.add(hint);
        hdr.add(leftGroup, BorderLayout.WEST);
        JPanel rightHdr = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightHdr.setOpaque(false);
        rightHdr.add(seeAll);
        hdr.add(rightHdr, BorderLayout.EAST);
        strip.add(hdr, BorderLayout.NORTH);

        orderListCards = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        orderListCards.setOpaque(false);
        JPanel centerWrap = new JPanel();
        centerWrap.setLayout(new BoxLayout(centerWrap, BoxLayout.Y_AXIS));
        centerWrap.setOpaque(false);
        orderListCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        centerWrap.add(Box.createVerticalGlue());
        centerWrap.add(orderListCards);
        centerWrap.add(Box.createVerticalGlue());
        JScrollPane cardScroll = new JScrollPane(centerWrap);
        cardScroll.setBorder(null);
        cardScroll.setBackground(LOG_BG);
        cardScroll.getViewport().setBackground(LOG_BG);
        cardScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        cardScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        cardScroll.setPreferredSize(new Dimension(0, 90));

        strip.add(cardScroll, BorderLayout.SOUTH);
        return strip;
    }

    private void refreshOrderListCards() {
        orderListCards.removeAll();

        List<CompletedOrder> active = new ArrayList<>();
        for (CompletedOrder co : completedOrders) {
            if (!"Completed".equals(co.status))
                active.add(co);
        }
        active.sort((a, b) -> Double.compare(orderPriorityScore(b), orderPriorityScore(a)));

        if (active.isEmpty()) {
            JLabel empty = new JLabel("No active orders");
            empty.setFont(FONT_SMALL);
            empty.setForeground(FG_MUTED);
            orderListCards.add(empty);
        } else {
            for (int i = 0; i < active.size(); i++) {
                orderListCards.add(createOrderCard(active.get(i), i == 0));
            }
        }
        orderListHeaderCount.setText(active.size() + " active");
        orderListCards.revalidate();
        orderListCards.repaint();
    }

    private JPanel createOrderCard(CompletedOrder co, boolean serveNext) {
        JPanel card = new JPanel(new BorderLayout(0, 2));
        card.setBackground(BG_SURFACE);

        boolean isPwdOrSenior = co.isPriority;
        Color borderColor = isPwdOrSenior ? ST_PWD_FG : (serveNext ? ACCENT : BORDER);
        int borderWidth = (isPwdOrSenior || serveNext) ? 2 : 1;

        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(borderColor, borderWidth, true),
                new EmptyBorder(10, 10, 10, 10)));
        card.setPreferredSize(new Dimension(162, 68));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setToolTipText("Click to mark as Completed");

        card.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                card.setBackground(new Color(0xDCFCE7));
            }

            public void mouseExited(MouseEvent e) {
                card.setBackground(BG_SURFACE);
            }

            public void mouseClicked(MouseEvent e) {
                co.status = "Completed";
                refreshOrderListCards();
                // ── Sync to kitchen queue ──
                if (orderQueuePanel != null) {
                    orderQueuePanel.markOrderCompleted(co.orderId);
                }
            }
        });

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel name = new JLabel(co.customerName);
        name.setFont(FONT_SMALL);
        name.setForeground(FG_PRIMARY);
        header.add(name, BorderLayout.WEST);

        JPanel badges = new JPanel(new FlowLayout(FlowLayout.RIGHT, 3, 0));
        badges.setOpaque(false);
        if (isPwdOrSenior) {
            JLabel pwdTag = makeBadge(co.discount == DiscountType.PWD ? "PWD" : "Senior", ST_PWD_BG, ST_PWD_FG);
            badges.add(pwdTag);
        }
        if (serveNext) {
            JLabel nextTag = new JLabel("▲ Next");
            nextTag.setFont(FONT_XSMALL);
            nextTag.setForeground(ACCENT);
            badges.add(nextTag);
        }
        header.add(badges, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JPanel meta = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        meta.setOpaque(false);
        JLabel orderId = new JLabel(fmtOrder(co.orderId));
        orderId.setFont(FONT_XSMALL);
        orderId.setForeground(TEXT_MUTED);
        meta.add(orderId);

        JLabel waitLbl = new JLabel(Math.max(0, Math.round(co.waitMinutes())) + "m · " + co.itemCount + " items");
        waitLbl.setFont(FONT_XSMALL);
        waitLbl.setForeground(TEXT_MUTED);
        meta.add(waitLbl);

        meta.add(makeBadgeForStatus(co.status));
        card.add(meta, BorderLayout.SOUTH);
        return card;
    }

    private JLabel makeBadge(String text, Color bg, Color fg) {
        JLabel b = new JLabel(text);
        b.setFont(FONT_BADGE);
        b.setBorder(new EmptyBorder(2, 7, 2, 7));
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        return b;
    }

    private JLabel makeBadgeForStatus(String status) {
        return switch (status) {
            case "Waiting" -> makeBadge(status, ST_WAITING_BG, ST_WAITING_FG);
            case "Preparing" -> makeBadge(status, ST_PREPARING_BG, ST_PREPARING_FG);
            case "Ready to Serve" -> makeBadge(status, ST_READY_BG, ST_READY_FG);
            case "Completed" -> makeBadge(status, ST_DONE_BG, ST_DONE_FG);
            default -> makeBadge(status, ST_CANCEL_BG, ST_CANCEL_FG);
        };
    }

    // ─── Category Tabs ───
    private JPanel buildCategoryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 6)) {
            @Override
            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                if (getParent() != null && getParent().getWidth() > 0) {
                    doLayout();
                    int maxY = 0;
                    for (Component c : getComponents())
                        maxY = Math.max(maxY, c.getY() + c.getHeight());
                    if (maxY > 0)
                        return new Dimension(d.width, maxY + getInsets().bottom + 4);
                }
                return d;
            }
        };
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(4, 0, 2, 0));
        pillButtons.clear();

        for (String cat : CATEGORY_ITEMS.keySet()) {
            JButton btn = new JButton(cat);
            btn.setFont(FONT_SMALL);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(6, 14, 6, 14)));
            btn.setContentAreaFilled(true);
            btn.setOpaque(true);
            updatePillStyle(btn, cat.equals(activeCategory));
            btn.addActionListener(e -> showCategory(cat));
            pillButtons.add(btn);
            bar.add(btn);
        }
        return bar;
    }

    private void updatePillStyle(JButton btn, boolean active) {
        if (active) {
            btn.setBackground(ACCENT);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ACCENT, 1, true),
                    new EmptyBorder(6, 14, 6, 14)));
        } else {
            btn.setBackground(BG_SURFACE);
            btn.setForeground(FG_MUTED);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(6, 14, 6, 14)));
        }
    }

    private void showCategory(String category) {
        activeCategory = category;
        for (JButton btn : pillButtons) {
            updatePillStyle(btn, btn.getText().equals(category));
        }
        rebuildProductGrid();
    }

    public void refreshCategoryPills() {
        syncDynamicMenuItems();
        for (JButton btn : pillButtons) {
            updatePillStyle(btn, btn.getText().equals(activeCategory));
        }
        repaint();
    }

    // ─── Product Grid ───
    private void rebuildProductGrid() {
        syncDynamicMenuItems();
        productGridPanel.removeAll();
        List<String> names = CATEGORY_ITEMS.getOrDefault(activeCategory, List.of());

        for (String name : names) {
            ItemSpec spec = resolveItem(name);
            if (spec.price <= 0 || spec.baseName.isEmpty())
                continue;
            productGridPanel.add(createProductCard(spec));
        }

        if (productGridPanel.getComponentCount() == 0) {
            JLabel empty = new JLabel("No items available", SwingConstants.CENTER);
            empty.setFont(FONT_BODY);
            empty.setForeground(FG_MUTED);
            productGridPanel.add(empty);
        }

        productGridPanel.revalidate();
        productGridPanel.repaint();
    }

    public void rebuildProducts() {
        rebuildProductGrid();
    }

    private void syncDynamicMenuItems() {
        resetCategoryItemsToSeed();
        Map<String, MenuItem> allItems = Menu.getInstance().getAllItems();
        for (MenuItem item : allItems.values()) {
            String alreadyInCategory = findExistingCategoryForItem(item.getName());
            if (alreadyInCategory != null)
                continue;

            String orderingCategory = mapMenuCategoryToOrderingCategory(item.getCategory(), item.getName());
            List<String> categoryItems = CATEGORY_ITEMS.get(orderingCategory);
            if (categoryItems == null) {
                categoryItems = new ArrayList<>();
                CATEGORY_ITEMS.put(orderingCategory, categoryItems);
            } else if (!(categoryItems instanceof ArrayList)) {
                categoryItems = new ArrayList<>(categoryItems);
                CATEGORY_ITEMS.put(orderingCategory, categoryItems);
            }
            if (!containsBaseItemName(categoryItems, item.getName())) {
                categoryItems.add(item.getName());
            }
        }
        if (!CATEGORY_ITEMS.containsKey(activeCategory) && !CATEGORY_ITEMS.isEmpty()) {
            activeCategory = CATEGORY_ITEMS.keySet().iterator().next();
        }
    }

    private static void resetCategoryItemsToSeed() {
        CATEGORY_ITEMS.clear();
        for (Map.Entry<String, List<String>> entry : BASE_CATEGORY_ITEMS.entrySet()) {
            CATEGORY_ITEMS.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
    }

    private String mapMenuCategoryToOrderingCategory(String menuCategory, String itemName) {
        if (menuCategory == null)
            return "Espresso & Coffee";
        return switch (menuCategory.trim()) {
            case "Coffee" -> "Espresso & Coffee";
            case "Food" -> mapFoodItemCategory(itemName);
            default -> menuCategory.trim();
        };
    }

    private String mapFoodItemCategory(String itemName) {
        if (containsBaseItemName(CATEGORY_ITEMS.getOrDefault("Sandwiches", List.of()), itemName))
            return "Sandwiches";
        if (containsBaseItemName(CATEGORY_ITEMS.getOrDefault("Pandesal Pairs", List.of()), itemName))
            return "Pandesal Pairs";
        return "Pastries";
    }

    private String findExistingCategoryForItem(String itemName) {
        for (Map.Entry<String, List<String>> entry : CATEGORY_ITEMS.entrySet()) {
            if (containsBaseItemName(entry.getValue(), itemName))
                return entry.getKey();
        }
        return null;
    }

    private boolean containsBaseItemName(List<String> names, String targetBaseName) {
        if (targetBaseName == null || targetBaseName.isBlank())
            return false;
        String target = NAME_MAP.getOrDefault(targetBaseName, targetBaseName).toLowerCase();
        for (String existing : names) {
            String base = existing;
            if (base.toLowerCase().startsWith("hot "))
                base = base.substring(4);
            else if (base.toLowerCase().startsWith("iced "))
                base = base.substring(5);
            base = NAME_MAP.getOrDefault(base, base).toLowerCase();
            if (base.equals(target))
                return true;
        }
        return false;
    }

    // ─── Product Card ───
    private JPanel createProductCard(ItemSpec spec) {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E7EB), 1, true),
                new EmptyBorder(0, 0, 8, 0)));
        card.setPreferredSize(new Dimension(0, 160));
        card.setMinimumSize(new Dimension(260, 160));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 160));

        JLabel imageLabel = new JLabel("", SwingConstants.CENTER);
        imageLabel.setPreferredSize(new Dimension(120, 160));
        imageLabel.setMinimumSize(new Dimension(120, 160));
        imageLabel.setMaximumSize(new Dimension(120, 160));
        imageLabel.setOpaque(true);
        imageLabel.setBackground(BG_SURFACE);
        ImageIcon img = loadProductImage(activeCategory, spec.displayName);
        if (img != null) {
            imageLabel.setIcon(scaleToFit(img, 120, 160));
        } else {
            imageLabel.setText(spec.displayName == null || spec.displayName.isBlank() ? "?"
                    : spec.displayName.substring(0, 1).toUpperCase());
            imageLabel.setFont(new Font("Segoe UI", Font.BOLD, 34));
            imageLabel.setForeground(FG_MUTED);
        }
        card.add(imageLabel, BorderLayout.WEST);

        JPanel content = new JPanel(new BorderLayout(0, 6));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(12, 12, 10, 12));

        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);

        JLabel nameLbl = new JLabel(spec.displayName);
        nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLbl.setForeground(FG_PRIMARY);

        JLabel priceLbl = new JLabel("\u20B1" + String.format("%.0f", spec.price));
        priceLbl.setFont(FONT_PRICE);
        priceLbl.setForeground(ACCENT);

        header.add(nameLbl, BorderLayout.WEST);
        header.add(priceLbl, BorderLayout.EAST);
        content.add(header, BorderLayout.NORTH);

        JLabel descLbl = new JLabel("Freshly prepared and ready to add.");
        descLbl.setFont(FONT_XSMALL);
        descLbl.setForeground(FG_MUTED);
        content.add(descLbl, BorderLayout.CENTER);

        boolean available = isMenuItemAvailable(spec.baseName);
        if (!available) {
            boolean expired = hasExpiredIngredient(spec.baseName);
            JLabel unavailable = new JLabel(expired
                    ? "Not available: ingredient expired"
                    : "Not available: ingredient stock is missing");
            unavailable.setFont(FONT_XSMALL);
            unavailable.setForeground(DANGER);
            content.add(unavailable, BorderLayout.SOUTH);
            card.setBackground(new Color(0xF8FAFC));
        }

        JButton addBtn = new JButton("Add to Cart");
        addBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
        addBtn.setForeground(Color.WHITE);
        addBtn.setBackground(available ? ACCENT : new Color(0x9CA3AF));
        addBtn.setBorder(new EmptyBorder(6, 12, 6, 12));
        addBtn.setFocusPainted(false);
        addBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        addBtn.setEnabled(available);
        addBtn.setToolTipText(available
                ? "Add this item to the order"
                : buildUnavailableIngredientsTooltip(spec.baseName, hasExpiredIngredient(spec.baseName)));
        addBtn.addActionListener(e -> addOrderItem(spec));
        content.add(addBtn, BorderLayout.SOUTH);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(content, BorderLayout.CENTER);
        card.add(wrap, BorderLayout.CENTER);
        return card;
    }

    // ─── Right Column ───
    private JPanel buildRightColumn() {
        JPanel col = new JPanel(new GridBagLayout());
        col.setBackground(BG_SURFACE);
        col.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER),
                new EmptyBorder(12, 12, 12, 12)));
        col.setPreferredSize(new Dimension(340, 0));
        col.setMinimumSize(new Dimension(320, 0));

        GridBagConstraints cx = new GridBagConstraints();
        cx.fill = GridBagConstraints.HORIZONTAL;
        cx.weightx = 1.0;
        cx.gridx = 0;

        cx.gridy = 0;
        cx.weighty = 0;
        cx.insets = new Insets(0, 0, 0, 0);
        col.add(buildCustomerSection(), cx);

        orderItemsPanel = new JPanel();
        orderItemsPanel.setLayout(new BoxLayout(orderItemsPanel, BoxLayout.Y_AXIS));
        orderItemsPanel.setOpaque(false);
        JScrollPane itemsScroll = new JScrollPane(orderItemsPanel);
        itemsScroll.setBorder(null);
        itemsScroll.getViewport().setBackground(BG_SURFACE);
        itemsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        itemsScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        itemsScroll.setPreferredSize(new Dimension(0, 200));

        JPanel orderDetailsSection = new JPanel(new BorderLayout(0, 6));
        orderDetailsSection.setOpaque(false);
        JPanel odHdrRow = new JPanel(new BorderLayout());
        odHdrRow.setOpaque(false);
        JLabel odHdr = new JLabel("Order Details");
        odHdr.setFont(FONT_SUBTITLE);
        odHdr.setForeground(FG_PRIMARY);
        odHdrRow.add(odHdr, BorderLayout.WEST);
        JButton clearOrderBtn = new JButton("Clear All");
        clearOrderBtn.setFont(FONT_SMALL);
        clearOrderBtn.setForeground(Color.WHITE);
        clearOrderBtn.setBackground(DANGER);
        clearOrderBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(DANGER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        clearOrderBtn.setFocusPainted(false);
        clearOrderBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearOrderBtn.addActionListener(e -> {
            if (!orderEntries.isEmpty()) {
                int ok = JOptionPane.showConfirmDialog(this, "Clear all items from the order?",
                        "Confirm", JOptionPane.YES_NO_OPTION);
                if (ok == JOptionPane.YES_OPTION) {
                    orderEntries.clear();
                    refreshOrderDisplay();
                }
            }
        });
        odHdrRow.add(clearOrderBtn, BorderLayout.EAST);
        orderDetailsSection.add(odHdrRow, BorderLayout.NORTH);
        orderDetailsSection.add(itemsScroll, BorderLayout.CENTER);

        cx.gridy = 1;
        cx.fill = GridBagConstraints.BOTH;
        cx.weighty = 1;
        cx.insets = new Insets(6, 0, 0, 0);
        col.add(orderDetailsSection, cx);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setOpaque(false);
        bottom.add(buildSummarySection(), BorderLayout.CENTER);
        bottom.add(buildProcessBtn(), BorderLayout.SOUTH);

        cx.gridy = 2;
        cx.fill = GridBagConstraints.HORIZONTAL;
        cx.weighty = 0;
        cx.insets = new Insets(8, 0, 0, 0);
        col.add(bottom, cx);

        return col;
    }

    private JPanel buildCustomerSection() {
        JPanel sec = new JPanel(new BorderLayout(0, 6));
        sec.setOpaque(false);

        JLabel hdr = new JLabel("Customer's Information");
        hdr.setFont(FONT_SUBTITLE);
        hdr.setForeground(FG_PRIMARY);
        sec.add(hdr, BorderLayout.NORTH);

        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setOpaque(false);

        customerNameField = new JTextField(12);
        customerNameField.setFont(FONT_BODY);
        customerNameField.setForeground(FG_PRIMARY);
        customerNameField.setBackground(BG_INPUT);
        customerNameField.setCaretColor(FG_PRIMARY);
        customerNameField.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                updateProcessBtnState();
            }

            public void removeUpdate(DocumentEvent e) {
                updateProcessBtnState();
            }

            public void changedUpdate(DocumentEvent e) {
                updateProcessBtnState();
            }
        });
        customerNameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)));

        orderNumLabel = new JLabel("Order " + fmtOrder(orderCount + 1));
        orderNumLabel.setFont(FONT_SMALL);
        orderNumLabel.setForeground(FG_MUTED);
        orderNumLabel.setHorizontalAlignment(SwingConstants.RIGHT);

        row.add(customerNameField, BorderLayout.CENTER);
        row.add(orderNumLabel, BorderLayout.EAST);
        sec.add(row, BorderLayout.CENTER);

        discountBtn = new JButton("No Discount");
        discountBtn.setFont(FONT_SMALL);
        discountBtn.setForeground(FG_MUTED);
        discountBtn.setBackground(BG_SURFACE);
        discountBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(5, 10, 5, 10)));
        discountBtn.setFocusPainted(false);
        discountBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        discountBtn.addActionListener(e -> showDiscountPicker());

        JPanel discRow = new JPanel(new BorderLayout(6, 0));
        discRow.setOpaque(false);
        JLabel discLbl = new JLabel("Discount:");
        discLbl.setFont(FONT_SMALL);
        discLbl.setForeground(FG_MUTED);
        discRow.add(discLbl, BorderLayout.WEST);
        discRow.add(discountBtn, BorderLayout.CENTER);
        sec.add(discRow, BorderLayout.SOUTH);

        return sec;
    }

    private void showDiscountPicker() {
        JDialog d = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Select Discount", Dialog.ModalityType.APPLICATION_MODAL);
        d.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBackground(BG_PRIMARY);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Apply Discount");
        title.setFont(FONT_SUBTITLE);
        title.setForeground(FG_PRIMARY);
        root.add(title, BorderLayout.NORTH);

        JPanel btnPanel = new JPanel(new GridLayout(1, 3, 10, 0));
        btnPanel.setOpaque(false);

        for (DiscountType dt : DiscountType.values()) {
            String sub = dt == DiscountType.NONE ? "Full price" : "20% off";
            JButton btn = buildSizeButton(dt.label.split(" ")[0], sub,
                    dt == DiscountType.NONE ? BG_SURFACE : ST_PWD_FG);
            if (dt == DiscountType.NONE) {
                btn.setForeground(FG_PRIMARY);
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(BORDER, 1, true),
                        new EmptyBorder(12, 8, 12, 8)));
            }
            if (dt == currentDiscount) {
                btn.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(ACCENT, 2, true),
                        new EmptyBorder(10, 8, 10, 8)));
            }
            btn.addActionListener(e -> {
                currentDiscount = dt;
                updateDiscountButton();
                updateSummary();
                d.dispose();
            });
            btnPanel.add(btn);
        }
        root.add(btnPanel, BorderLayout.CENTER);

        d.setContentPane(root);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    private void updateDiscountButton() {
        if (currentDiscount == DiscountType.NONE) {
            discountBtn.setText("No Discount");
            discountBtn.setForeground(FG_MUTED);
            discountBtn.setBackground(BG_SURFACE);
        } else {
            discountBtn.setText(currentDiscount.label + " ✓");
            discountBtn.setForeground(Color.WHITE);
            discountBtn.setBackground(ST_PWD_FG);
        }
    }

    private JPanel buildSummarySection() {
        JPanel sec = new JPanel(new GridBagLayout());
        sec.setBackground(BG_INPUT);
        sec.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 12, 10, 12)));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 0, 2, 0);
        c.weightx = 1.0;

        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        JLabel osHdr = new JLabel("Order Summary");
        osHdr.setFont(FONT_SUBTITLE);
        osHdr.setForeground(FG_PRIMARY);
        sec.add(osHdr, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 1;
        JLabel sttl = new JLabel("Subtotal");
        sttl.setFont(FONT_SMALL);
        sttl.setForeground(FG_MUTED);
        sec.add(sttl, c);
        c.gridx = 1;
        subtotalLabel = new JLabel("\u20B10.00", SwingConstants.RIGHT);
        subtotalLabel.setFont(FONT_SMALL);
        subtotalLabel.setForeground(FG_PRIMARY);
        sec.add(subtotalLabel, c);

        c.gridx = 0;
        c.gridy = 2;
        JLabel tx = new JLabel("Tax (12%)");
        tx.setFont(FONT_SMALL);
        tx.setForeground(FG_MUTED);
        sec.add(tx, c);
        c.gridx = 1;
        taxLabel = new JLabel("\u20B10.00", SwingConstants.RIGHT);
        taxLabel.setFont(FONT_SMALL);
        taxLabel.setForeground(FG_PRIMARY);
        sec.add(taxLabel, c);

        c.gridx = 0;
        c.gridy = 3;
        JLabel dl = new JLabel("Discount");
        dl.setFont(FONT_SMALL);
        dl.setForeground(ST_PWD_FG);
        sec.add(dl, c);
        c.gridx = 1;
        discountLabel = new JLabel("\u20B10.00", SwingConstants.RIGHT);
        discountLabel.setFont(FONT_SMALL);
        discountLabel.setForeground(ST_PWD_FG);
        sec.add(discountLabel, c);

        JPanel dash = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(BORDER);
                float[] d2 = { 4f, 4f };
                g2.setStroke(new java.awt.BasicStroke(1f, java.awt.BasicStroke.CAP_BUTT,
                        java.awt.BasicStroke.JOIN_MITER, 1f, d2, 0f));
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        dash.setOpaque(false);
        dash.setPreferredSize(new Dimension(0, 6));
        c.gridx = 0;
        c.gridy = 4;
        c.gridwidth = 2;
        sec.add(dash, c);

        c.gridwidth = 1;
        c.gridx = 0;
        c.gridy = 5;
        JLabel ttl = new JLabel("TOTAL");
        ttl.setFont(FONT_BOLD_SM);
        ttl.setForeground(ACCENT);
        sec.add(ttl, c);
        c.gridx = 1;
        totalLabel = new JLabel("\u20B10.00", SwingConstants.RIGHT);
        totalLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        totalLabel.setForeground(ACCENT);
        sec.add(totalLabel, c);

        return sec;
    }

    private JButton buildProcessBtn() {
        processBtn = new JButton("Process Transaction");
        processBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        processBtn.setForeground(Color.WHITE);
        processBtn.setBackground(SUCCESS);
        processBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SUCCESS, 1, true),
                new EmptyBorder(12, 20, 12, 20)));
        processBtn.setFocusPainted(false);
        processBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        processBtn.addActionListener(e -> processTransaction());
        processBtn.setEnabled(false);
        return processBtn;
    }

    // ═══════════════════════════════════════════════════════════════
    // Order Management
    // ═══════════════════════════════════════════════════════════════

    private void addOrderItem(ItemSpec spec) {
        if (!spec.variant.equals("Hot")) {
            MenuItem menuItem = Menu.getInstance().getMenuItem(spec.baseName);
            if (menuItem != null && menuItem.getIcedLargePrice() > 0 && menuItem.getIcedRegularPrice() > 0) {
                showSizePicker(spec, menuItem);
                return;
            }
        }
        doAddOrderItem(spec);
    }

    private void showSizePicker(ItemSpec spec, MenuItem menuItem) {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "Choose Size \u2014 " + spec.baseName, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setResizable(false);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(AppTheme.BG_PRIMARY);
        root.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Select a size for " + spec.baseName);
        title.setFont(new Font("Segoe UI", Font.BOLD, 14));
        title.setForeground(FG_PRIMARY);
        title.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));
        root.add(title, BorderLayout.NORTH);

        JPanel btnRow = new JPanel(new GridLayout(1, 2, 12, 0));
        btnRow.setOpaque(false);

        JButton regularBtn = buildSizeButton("Regular", String.format("₱%.2f", menuItem.getIcedRegularPrice()), ACCENT);
        JButton largeBtn = buildSizeButton("Large", String.format("₱%.2f", menuItem.getIcedLargePrice()),
                AppTheme.SUCCESS);

        regularBtn.addActionListener(e -> {
            dialog.dispose();
            ItemSpec picked = new ItemSpec(spec.displayName, spec.baseName, "Regular Iced",
                    menuItem.getIcedRegularPrice());
            doAddOrderItem(picked);
        });
        largeBtn.addActionListener(e -> {
            dialog.dispose();
            String largeDisplay = spec.displayName.replaceFirst("(?i)^iced\\s+", "Iced Large ");
            if (largeDisplay.equals(spec.displayName))
                largeDisplay = spec.displayName + " (Large)";
            ItemSpec picked = new ItemSpec(largeDisplay, spec.baseName, "Large Iced", menuItem.getIcedLargePrice());
            doAddOrderItem(picked);
        });

        btnRow.add(regularBtn);
        btnRow.add(largeBtn);
        root.add(btnRow, BorderLayout.CENTER);

        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JButton buildSizeButton(String size, String price, Color accent) {
        JButton btn = new JButton("<html><center><b style='font-size:13px'>" + size
                + "</b><br><span style='font-size:11px'>" + price + "</span></center></html>");
        btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        btn.setForeground(Color.WHITE);
        btn.setBackground(accent);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(accent.darker(), 1, true),
                new EmptyBorder(12, 20, 12, 20)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setOpaque(true);
        return btn;
    }

    private void doAddOrderItem(ItemSpec spec) {
        if (!isMenuItemAvailableForQuantity(spec.baseName, 1)) {
            JOptionPane.showMessageDialog(this, "Insufficient ingredients to add this item.",
                    "Unavailable", JOptionPane.WARNING_MESSAGE);
            return;
        }
        for (OrderEntry e : orderEntries) {
            if (e.displayName.equals(spec.displayName) && e.variant.equals(spec.variant)) {
                if (!isMenuItemAvailableForQuantity(spec.baseName, e.quantity + 1)) {
                    JOptionPane.showMessageDialog(this, "Insufficient ingredients to increase quantity.",
                            "Unavailable", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                e.quantity++;
                refreshOrderDisplay();
                return;
            }
        }
        orderEntries.add(new OrderEntry(spec.displayName, spec.baseName, spec.variant, 1, spec.price));
        refreshOrderDisplay();
    }

    private void refreshOrderDisplay() {
        orderItemsPanel.removeAll();
        if (orderEntries.isEmpty()) {
            JLabel empty = new JLabel("No items in order", SwingConstants.CENTER);
            empty.setFont(FONT_SMALL);
            empty.setForeground(FG_MUTED);
            empty.setAlignmentX(Component.CENTER_ALIGNMENT);
            orderItemsPanel.add(empty);
        } else {
            for (OrderEntry entry : orderEntries) {
                orderItemsPanel.add(createOrderRow(entry));
                orderItemsPanel.add(Box.createVerticalStrut(3));
            }
        }
        updateSummary();
        orderItemsPanel.revalidate();
        orderItemsPanel.repaint();
    }

    private JPanel createOrderRow(OrderEntry entry) {
        JPanel row = new JPanel(new BorderLayout(6, 0));
        row.setBackground(BG_INPUT);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(5, 6, 5, 6)));

        JLabel thumb = new JLabel("\u2615", SwingConstants.CENTER);
        thumb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        thumb.setForeground(FG_MUTED);
        thumb.setPreferredSize(new Dimension(28, 28));
        thumb.setBackground(BG_SURFACE);
        thumb.setOpaque(true);
        ImageIcon img = loadProductImage(findCategory(entry.displayName), entry.displayName);
        if (img != null) {
            thumb.setText("");
            thumb.setIcon(new ImageIcon(img.getImage().getScaledInstance(26, 26, Image.SCALE_SMOOTH)));
        }
        row.add(thumb, BorderLayout.WEST);

        JPanel np = new JPanel(new BorderLayout(0, 1));
        np.setOpaque(false);
        JLabel nl = new JLabel(entry.orderLabel());
        nl.setFont(FONT_SMALL);
        nl.setForeground(FG_PRIMARY);
        np.add(nl, BorderLayout.NORTH);

        double lt = entry.lineTotal();
        JLabel tl = new JLabel("\u20B1" + String.format("%.2f", lt));
        tl.setFont(FONT_SMALL);
        tl.setForeground(ACCENT);
        np.add(tl, BorderLayout.SOUTH);
        row.add(np, BorderLayout.CENTER);

        JPanel qp = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 0));
        qp.setOpaque(false);
        qp.setPreferredSize(new Dimension(82, 26));
        qp.setBorder(new EmptyBorder(0, 0, 0, 2));

        JButton minus = new JButton("\u2212");
        minus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        minus.setForeground(FG_PRIMARY);
        minus.setBackground(AppTheme.BG_BADGE_BLUE);
        minus.setPreferredSize(new Dimension(18, 18));
        minus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1),
                new EmptyBorder(0, 2, 0, 2)));
        minus.setFocusPainted(false);
        minus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        minus.addActionListener(e -> {
            if (entry.quantity > 1)
                entry.quantity--;
            else
                orderEntries.remove(entry);
            refreshOrderDisplay();
        });

        JTextField ql = new JTextField(String.valueOf(entry.quantity), 3);
        ql.setFont(FONT_BOLD_SM);
        ql.setForeground(FG_PRIMARY);
        ql.setHorizontalAlignment(SwingConstants.CENTER);
        ql.setPreferredSize(new Dimension(26, 18));
        ql.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(0, 2, 0, 2)));
        ql.getDocument().addDocumentListener(new DocumentListener() {
            private void apply() {
                String txt = ql.getText().trim();
                if (txt.isEmpty())
                    return;
                try {
                    int v = Integer.parseInt(txt);
                    if (v <= 0) {
                        orderEntries.remove(entry);
                        refreshOrderDisplay();
                        return;
                    }
                    if (!isMenuItemAvailableForQuantity(entry.baseName, v)) {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(row,
                                "Insufficient ingredients for requested quantity.", "Unavailable",
                                JOptionPane.WARNING_MESSAGE));
                        ql.setText(String.valueOf(entry.quantity));
                        return;
                    }
                    entry.quantity = v;
                    refreshOrderDisplay();
                } catch (NumberFormatException ex) {
                }
            }

            public void insertUpdate(DocumentEvent e) {
                apply();
            }

            public void removeUpdate(DocumentEvent e) {
                apply();
            }

            public void changedUpdate(DocumentEvent e) {
                apply();
            }
        });

        JButton plus = new JButton("+");
        plus.setFont(new Font("Segoe UI", Font.BOLD, 12));
        plus.setForeground(Color.WHITE);
        plus.setBackground(ACCENT);
        plus.setPreferredSize(new Dimension(18, 18));
        plus.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.ACCENT_DARK, 1),
                new EmptyBorder(0, 2, 0, 2)));
        plus.setFocusPainted(false);
        plus.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        plus.addActionListener(e -> {
            if (!isMenuItemAvailableForQuantity(entry.baseName, entry.quantity + 1)) {
                JOptionPane.showMessageDialog(row, "Insufficient ingredients to increase quantity.",
                        "Unavailable", JOptionPane.WARNING_MESSAGE);
                return;
            }
            entry.quantity++;
            refreshOrderDisplay();
        });

        qp.add(minus);
        qp.add(ql);
        qp.add(plus);
        row.add(qp, BorderLayout.EAST);

        row.setMaximumSize(new Dimension(9999, 52));
        return row;
    }

    private void updateSummary() {
        double gross = 0;
        for (OrderEntry e : orderEntries)
            gross += e.lineTotal();

        double discountAmt = gross * currentDiscount.rate;
        double afterDiscount = gross - discountAmt;
        double sub = afterDiscount / 1.12;
        double vat = afterDiscount - sub;

        subtotalLabel.setText("\u20B1" + String.format("%.2f", sub));
        taxLabel.setText("\u20B1" + String.format("%.2f", vat));
        discountLabel.setText(discountAmt > 0
                ? "-\u20B1" + String.format("%.2f", discountAmt)
                : "\u20B10.00");
        discountLabel.setForeground(discountAmt > 0 ? ST_PWD_FG : FG_MUTED);
        totalLabel.setText("\u20B1" + String.format("%.2f", afterDiscount));
        updateProcessBtnState();
    }

    private void updateProcessBtnState() {
        processBtn.setEnabled(!orderEntries.isEmpty()
                && !customerNameField.getText().trim().isEmpty());
    }

    // ═══════════════════════════════════════════════════════════════
    // Transaction
    // ═══════════════════════════════════════════════════════════════

    private void processTransaction() {
        if (orderEntries.isEmpty())
            return;

        final String customerName = customerNameField.getText().trim().isEmpty()
                ? "Walk-in Customer"
                : customerNameField.getText().trim();

        double gross = 0;
        for (OrderEntry e : orderEntries)
            gross += e.lineTotal();
        double discountAmt = gross * currentDiscount.rate;
        final double totalInclusive = gross - discountAmt;

        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this), "Payment",
                JDialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(BG_PRIMARY);

        JPanel mp = new JPanel(new BorderLayout(0, 16));
        mp.setBackground(BG_PRIMARY);
        mp.setBorder(new EmptyBorder(24, 28, 24, 28));

        JLabel titleLbl = new JLabel("Complete Transaction", SwingConstants.CENTER);
        titleLbl.setFont(FONT_TITLE);
        titleLbl.setForeground(ACCENT);
        mp.add(titleLbl, BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints cx = new GridBagConstraints();
        cx.fill = GridBagConstraints.HORIZONTAL;
        cx.insets = new Insets(6, 0, 6, 0);
        cx.weightx = 1.0;

        cx.gridx = 0;
        cx.gridy = 0;
        cx.gridwidth = 2;
        JLabel cl = new JLabel("Customer: " + customerName);
        cl.setFont(FONT_BODY);
        cl.setForeground(FG_PRIMARY);
        center.add(cl, cx);

        cx.gridy = 1;
        JLabel ol = new JLabel("Order " + fmtOrder(orderCount + 1));
        ol.setFont(FONT_BODY);
        ol.setForeground(FG_MUTED);
        center.add(ol, cx);

        if (currentDiscount != DiscountType.NONE) {
            cx.gridy = 2;
            JLabel discTypeLbl = new JLabel(currentDiscount.label + " applied");
            discTypeLbl.setFont(FONT_SMALL);
            discTypeLbl.setForeground(ST_PWD_FG);
            center.add(discTypeLbl, cx);
            cx.gridy = 3;
            cx.gridwidth = 1;
            JLabel damt = new JLabel("Discount:");
            damt.setFont(FONT_BODY);
            damt.setForeground(FG_MUTED);
            center.add(damt, cx);
            cx.gridx = 1;
            JLabel damtV = new JLabel("-\u20B1" + String.format("%.2f", discountAmt), SwingConstants.RIGHT);
            damtV.setFont(FONT_BODY);
            damtV.setForeground(ST_PWD_FG);
            center.add(damtV, cx);
            cx.gridy = 4;
        } else {
            cx.gridy = 2;
            cx.gridwidth = 1;
        }

        cx.gridx = 0;
        JLabel tdl = new JLabel("Total Due:");
        tdl.setFont(FONT_BODY);
        tdl.setForeground(FG_MUTED);
        center.add(tdl, cx);
        cx.gridx = 1;
        JLabel tdv = new JLabel("\u20B1" + String.format("%.2f", totalInclusive), SwingConstants.RIGHT);
        tdv.setFont(new Font("Segoe UI", Font.BOLD, 20));
        tdv.setForeground(ACCENT);
        center.add(tdv, cx);

        cx.gridx = 0;
        cx.gridy++;
        JLabel csh = new JLabel("Cash Amount:");
        csh.setFont(FONT_BODY);
        csh.setForeground(FG_PRIMARY);
        center.add(csh, cx);
        cx.gridx = 1;
        JTextField cf = new JTextField(10);
        cf.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cf.setForeground(FG_PRIMARY);
        cf.setBackground(BG_INPUT);
        cf.setCaretColor(FG_PRIMARY);
        cf.setHorizontalAlignment(JTextField.RIGHT);
        cf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(7, 10, 7, 10)));
        center.add(cf, cx);

        cx.gridx = 0;
        cx.gridy++;
        JLabel chl = new JLabel("Change:");
        chl.setFont(FONT_BODY);
        chl.setForeground(FG_MUTED);
        center.add(chl, cx);
        cx.gridx = 1;
        JLabel chv = new JLabel("\u20B10.00", SwingConstants.RIGHT);
        chv.setFont(new Font("Segoe UI", Font.BOLD, 18));
        chv.setForeground(SUCCESS);
        center.add(chv, cx);

        cf.addActionListener(ae -> calcChange(cf, totalInclusive, chv));
        cf.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                calcChange(cf, totalInclusive, chv);
            }
        });

        mp.add(center, BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bp.setOpaque(false);

        JButton cancel = new JButton("Cancel");
        cancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        cancel.setForeground(FG_MUTED);
        cancel.setBackground(BG_SURFACE);
        cancel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(10, 24, 10, 24)));
        cancel.setFocusPainted(false);
        cancel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cancel.addActionListener(e -> dialog.dispose());

        JButton confirm = new JButton("Pay \u20B1" + String.format("%.2f", totalInclusive));
        confirm.setFont(new Font("Segoe UI", Font.BOLD, 13));
        confirm.setForeground(Color.WHITE);
        confirm.setBackground(SUCCESS);
        confirm.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(SUCCESS, 1, true),
                new EmptyBorder(10, 24, 10, 24)));
        confirm.setFocusPainted(false);
        confirm.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        final JDialog dialogRef = dialog;
        final JTextField cashField = cf;
        final double discFinal = discountAmt;
        confirm.addActionListener(e -> {
            String cs = cashField.getText().trim();
            if (cs.isEmpty()) {
                JOptionPane.showMessageDialog(dialogRef, "Enter cash amount.", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }
            try {
                double cash = Double.parseDouble(cs);
                if (cash < totalInclusive) {
                    JOptionPane.showMessageDialog(dialogRef, "Insufficient cash.", "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                double change = cash - totalInclusive;
                dialogRef.dispose();
                completeTransaction(customerName, totalInclusive, cash, change, discFinal);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialogRef, "Invalid amount.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        bp.add(cancel);
        bp.add(confirm);
        mp.add(bp, BorderLayout.SOUTH);

        dialog.getContentPane().add(mp);
        dialog.pack();
        dialog.setSize(400, dialog.getHeight() + 20);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    private void calcChange(JTextField cf, double total, JLabel chv) {
        try {
            String t = cf.getText().trim();
            if (t.isEmpty()) {
                chv.setText("\u20B10.00");
                chv.setForeground(SUCCESS);
                return;
            }
            double cash = Double.parseDouble(t);
            double change = cash - total;
            chv.setText("\u20B1" + String.format("%.2f", change));
            chv.setForeground(change >= 0 ? SUCCESS : DANGER);
        } catch (NumberFormatException e) {
            chv.setText("\u20B10.00");
            chv.setForeground(FG_MUTED);
        }
    }

    private void completeTransaction(String customerName, double totalInclusive,
            double cash, double change, double discountAmt) {
        double subtotal = totalInclusive / 1.12;
        double vat = totalInclusive - subtotal;
        String txnRef = nextTransactionRef();

        // Deduct inventory
        Inventory inv = Inventory.getInstance();
        Menu menu = Menu.getInstance();
        for (OrderEntry entry : orderEntries) {
            MenuItem mi = menu.getMenuItem(entry.baseName);
            if (mi != null) {
                for (Map.Entry<String, Double> ing : mi.getIngredients().entrySet()) {
                    inv.deductIngredient(ing.getKey(), ing.getValue() * entry.quantity);
                }
            }
        }

        // Persist sales record
        List<SalesRecord> sales = new ArrayList<>();
        for (OrderEntry entry : orderEntries) {
            sales.add(new SalesRecord(entry.fullLabel(), entry.quantity, entry.unitPrice, entry.lineTotal()));
        }
        try {
            controller.OrderController oc = new controller.OrderController(
                    new persistence.sqlite.SQLiteSalesRepository());
            oc.persistCompletedTransaction(txnRef, sales, totalInclusive, cash, change, customerName);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save error: " + ex.getMessage(),
                    "Database", JOptionPane.WARNING_MESSAGE);
        }

        // Add to POS order strip
        String ts = new SimpleDateFormat("MM/dd HH:mm").format(new Date());
        int newOrderId = orderCount + 1;
        CompletedOrder newOrder = new CompletedOrder(
                newOrderId, customerName, ts, "Waiting",
                new ArrayList<>(orderEntries), currentDiscount);
        completedOrders.add(0, newOrder);
        refreshOrderListCards();

        // ── Push to kitchen queue ──
        if (orderQueuePanel != null) {
            List<ui.OrderQueuePanel.ReceiptItem> receiptItems = new ArrayList<>();
            for (OrderEntry entry : orderEntries) {
                receiptItems.add(new ui.OrderQueuePanel.ReceiptItem(
                        entry.fullLabel(),
                        entry.quantity,
                        entry.unitPrice,
                        entry.lineTotal()));
            }

            String discountTypeStr = currentDiscount.name();
            ui.OrderQueuePanel.Receipt receipt = new ui.OrderQueuePanel.Receipt(
                    newOrderId, customerName, receiptItems,
                    ts, subtotal, vat, totalInclusive,
                    cash, change, discountTypeStr);
            orderQueuePanel.addOrder(receipt);
        }

        // Show receipt (pass snapshot of entries before clearing)
        showReceipt(customerName, txnRef, subtotal, vat, totalInclusive, cash, change, discountAmt);

        // Reset for next order
        orderCount++;
        orderEntries.clear();
        customerNameField.setText("");
        orderNumLabel.setText("Order " + fmtOrder(orderCount + 1));
        currentDiscount = DiscountType.NONE;
        updateDiscountButton();
        refreshOrderDisplay();

        if (onRefresh != null)
            onRefresh.run();
    }

    private void showReceipt(String customer, String txnRef,
            double sub, double vat, double total,
            double cash, double change, double discountAmt) {
        String line = "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\n";
        String dbl = "\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\n";

        StringBuilder sb = new StringBuilder();
        sb.append("            \u2615 Better Mondays Cafe \u2615\n");
        sb.append("            123 Main St., Manila\n");
        sb.append("            VAT REG TIN: 123-456-789\n");
        sb.append(dbl);
        sb.append(" Date: ").append(new SimpleDateFormat("MM/dd/yyyy HH:mm").format(new Date())).append("\n");
        sb.append(" Customer: ").append(customer).append("\n");
        sb.append(" ").append(txnRef).append("\n");
        if (discountAmt > 0)
            sb.append(" Discount Applied\n");
        sb.append(dbl);
        sb.append(String.format(" %-16s %2s %8s\n", "ITEM", "QTY", "AMOUNT"));
        sb.append(line);
        for (OrderEntry entry : orderEntries) {
            sb.append(String.format(" %-16s %2d %8.2f\n",
                    trunc(entry.fullLabel(), 16), entry.quantity, entry.lineTotal()));
        }
        sb.append(line);
        if (discountAmt > 0) {
            sb.append(String.format(" %-22s %8.2f\n", "Discount:", -discountAmt));
        }
        sb.append(String.format(" %-22s %8.2f\n", "Subtotal (excl VAT):", sub));
        sb.append(String.format(" %-22s %8.2f\n", "VAT (12%):", vat));
        sb.append(String.format(" %-22s %8.2f\n", "TOTAL (incl VAT):", total));
        sb.append(line);
        sb.append(String.format(" %-22s %8.2f\n", "Cash:", cash));
        sb.append(String.format(" %-22s %8.2f\n", "Change:", change));
        sb.append(dbl);
        sb.append("         Thank you! Come again!\n");
        sb.append("         *** Have a nice day ***\n");

        JDialog rd = new JDialog(SwingUtilities.getWindowAncestor(this),
                "RECEIPT - " + txnRef, JDialog.ModalityType.APPLICATION_MODAL);
        rd.getContentPane().setBackground(BG_PRIMARY);
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBackground(BG_PRIMARY);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));
        JTextArea ta = new JTextArea(sb.toString());
        ta.setFont(new Font("Monospaced", Font.PLAIN, 12));
        ta.setEditable(false);
        JScrollPane sp = new JScrollPane(ta);
        sp.setPreferredSize(new Dimension(480, 360));
        panel.add(sp, BorderLayout.CENTER);

        JPanel bp = new JPanel(new FlowLayout(FlowLayout.CENTER, 12, 0));
        bp.setOpaque(false);
        JButton print = new JButton("Print");
        print.setFont(FONT_BODY);
        print.setForeground(Color.WHITE);
        print.setBackground(ACCENT);
        print.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(8, 20, 8, 20)));
        print.setFocusPainted(false);
        print.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        print.addActionListener(ev -> {
            try {
                boolean ok = ta.print();
                if (!ok)
                    JOptionPane.showMessageDialog(rd, "Printer cancelled or failed.", "Print",
                            JOptionPane.WARNING_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(rd, "Print error: " + ex.getMessage(), "Print",
                        JOptionPane.ERROR_MESSAGE);
            }
        });

        JButton close = new JButton("Close");
        close.setFont(FONT_BODY);
        close.setForeground(Color.WHITE);
        close.setBackground(ACCENT);
        close.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true), new EmptyBorder(8, 20, 8, 20)));
        close.setFocusPainted(false);
        close.addActionListener(ev -> rd.dispose());

        bp.add(print);
        bp.add(close);
        panel.add(bp, BorderLayout.SOUTH);
        rd.add(panel);
        rd.pack();
        java.awt.Window win = SwingUtilities.getWindowAncestor(this);
        if (win != null)
            rd.setLocationRelativeTo(win);
        else
            rd.setLocationRelativeTo(null);
        rd.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // See All Modal
    // ═══════════════════════════════════════════════════════════════

    private void showSeeAllModal() {
        JDialog dialog = new JDialog(SwingUtilities.getWindowAncestor(this),
                "All Orders", JDialog.ModalityType.APPLICATION_MODAL);
        dialog.getContentPane().setBackground(BG_PRIMARY);

        JPanel mp = new JPanel(new BorderLayout(0, 12));
        mp.setBackground(BG_PRIMARY);
        mp.setBorder(new EmptyBorder(20, 20, 20, 20));

        JLabel title = new JLabel("All Recent Orders", SwingConstants.CENTER);
        title.setFont(FONT_TITLE);
        title.setForeground(ACCENT);
        mp.add(title, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 10, 10));
        grid.setOpaque(false);

        if (completedOrders.isEmpty()) {
            JLabel empty = new JLabel("No orders yet", SwingConstants.CENTER);
            empty.setFont(FONT_BODY);
            empty.setForeground(FG_MUTED);
            grid.add(empty);
        } else {
            for (CompletedOrder co : completedOrders) {
                JPanel card = new JPanel(new BorderLayout(0, 4));
                card.setBackground(BG_CARD);
                card.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(co.isPriority ? ST_PWD_FG : BORDER, co.isPriority ? 2 : 1, true),
                        new EmptyBorder(12, 12, 12, 12)));

                JPanel cardHdr = new JPanel(new BorderLayout());
                cardHdr.setOpaque(false);
                JLabel orderTitle = new JLabel("Order " + fmtOrder(co.orderId));
                orderTitle.setFont(FONT_BOLD_SM);
                orderTitle.setForeground(FG_PRIMARY);
                cardHdr.add(orderTitle, BorderLayout.WEST);
                if (co.isPriority) {
                    cardHdr.add(makeBadge(co.discount == DiscountType.PWD ? "PWD" : "Senior", ST_PWD_BG, ST_PWD_FG),
                            BorderLayout.EAST);
                }
                card.add(cardHdr, BorderLayout.NORTH);

                JPanel info = new JPanel(new GridLayout(0, 1, 0, 2));
                info.setOpaque(false);
                JLabel cn = new JLabel(co.customerName);
                cn.setFont(FONT_SMALL);
                cn.setForeground(FG_PRIMARY);
                info.add(cn);
                JLabel ts = new JLabel(co.timestamp);
                ts.setFont(FONT_SMALL);
                ts.setForeground(FG_MUTED);
                info.add(ts);
                info.add(makeBadgeForStatus(co.status));
                card.add(info, BorderLayout.CENTER);

                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                card.setToolTipText(!"Completed".equals(co.status) ? "Click to mark as Completed" : null);
                card.addMouseListener(new MouseAdapter() {
                    public void mouseClicked(MouseEvent e) {
                        if (!"Completed".equals(co.status)) {
                            co.status = "Completed";
                            refreshOrderListCards();
                            if (orderQueuePanel != null) {
                                orderQueuePanel.markOrderCompleted(co.orderId);
                            }
                            dialog.dispose();
                            showSeeAllModal();
                        }
                    }
                });

                grid.add(card);
            }
        }

        JScrollPane scroll = new JScrollPane(grid);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_PRIMARY);
        mp.add(scroll, BorderLayout.CENTER);

        JButton close = new JButton("Close");
        close.setFont(FONT_BODY);
        close.setForeground(Color.WHITE);
        close.setBackground(ACCENT);
        close.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT, 1, true),
                new EmptyBorder(8, 20, 8, 20)));
        close.setFocusPainted(false);
        close.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        close.addActionListener(e -> dialog.dispose());

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        bpanel.setOpaque(false);
        bpanel.add(close);
        mp.add(bpanel, BorderLayout.SOUTH);

        dialog.getContentPane().add(mp);
        dialog.setSize(640, 460);
        dialog.setLocationRelativeTo(SwingUtilities.getWindowAncestor(this));
        dialog.setVisible(true);
    }

    // ═══════════════════════════════════════════════════════════════
    // Helpers
    // ═══════════════════════════════════════════════════════════════

    private ImageIcon loadProductImage(String category, String prodName) {
        String key = category + "|" + prodName;
        ImageIcon cached = imageCache.get(key);
        if (cached != null)
            return cached;

        String lookupName = prodName.replaceFirst("(?i)^(iced)\\s+large\\s+", "$1 ")
                .replaceFirst("(?i)\\s*\\(large\\)$", "").trim();
        String baseName = IMAGE_MAP.getOrDefault(lookupName, lookupName);

        String lookupBaseName = lookupName;
        if (lookupBaseName.toLowerCase().startsWith("hot "))
            lookupBaseName = lookupBaseName.substring(4).trim();
        else if (lookupBaseName.toLowerCase().startsWith("iced "))
            lookupBaseName = lookupBaseName.substring(5).trim();
        lookupBaseName = NAME_MAP.getOrDefault(lookupBaseName, lookupBaseName);
        MenuItem menuItem = Menu.getInstance().getMenuItem(lookupBaseName);
        if (menuItem != null && menuItem.getImagePath() != null && !menuItem.getImagePath().isBlank()) {
            try {
                File imageFile = new File(menuItem.getImagePath());
                if (imageFile.exists()) {
                    ImageIcon icon = readImageIcon(imageFile);
                    if (icon != null) {
                        imageCache.put(key, icon);
                        return icon;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        List<String> attempts = new ArrayList<>();
        String lc = baseName.toLowerCase();
        if (lc.startsWith("hot ") || lc.startsWith("iced ")) {
            attempts.add(baseName + ".jpg");
            attempts.add(baseName + " .jpg");
            attempts.add(baseName + ".png");
            attempts.add(baseName + " .png");
        } else {
            attempts.add("Hot " + baseName + ".jpg");
            attempts.add("Hot " + baseName + " .jpg");
            attempts.add("Iced " + baseName + ".jpg");
            attempts.add("Iced " + baseName + " .jpg");
            attempts.add(baseName + ".jpg");
            attempts.add(baseName + " .jpg");
            attempts.add(baseName + ".png");
            attempts.add(baseName + " .png");
            attempts.add(baseName.toLowerCase() + ".jpg");
            attempts.add(baseName.toLowerCase() + " .jpg");
            attempts.add(baseName.toLowerCase() + ".png");
            attempts.add(baseName.toLowerCase() + " .png");
        }
        for (String fn : attempts) {
            try {
                java.net.URL url = getClass().getResource("/images/" + category + "/" + fn);
                if (url != null) {
                    BufferedImage img = ImageIO.read(url);
                    if (img != null) {
                        ImageIcon icon = new ImageIcon(img);
                        imageCache.put(key, icon);
                        return icon;
                    }
                }
            } catch (Exception e) {
            }
        }
        imageCache.put(key, null);
        return null;
    }

    private ImageIcon readImageIcon(File imageFile) {
        BufferedImage bufferedImage = null;
        try {
            bufferedImage = ImageIO.read(imageFile);
        } catch (Exception ignored) {
        }

        if (bufferedImage == null) {
            Image image = Toolkit.getDefaultToolkit().createImage(imageFile.getAbsolutePath());
            MediaTracker tracker = new MediaTracker(this);
            tracker.addImage(image, 0);
            try {
                tracker.waitForID(0);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            if (!tracker.isErrorAny() && image.getWidth(null) > 0 && image.getHeight(null) > 0) {
                bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null),
                        BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = bufferedImage.createGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
            }
        }
        return bufferedImage != null ? new ImageIcon(bufferedImage) : null;
    }

    private ImageIcon scaleToFit(ImageIcon icon, int targetWidth, int targetHeight) {
        if (icon == null || icon.getIconWidth() <= 0 || icon.getIconHeight() <= 0)
            return icon;
        double scale = Math.min((double) targetWidth / icon.getIconWidth(),
                (double) targetHeight / icon.getIconHeight());
        int width = Math.max(1, (int) Math.round(icon.getIconWidth() * scale));
        int height = Math.max(1, (int) Math.round(icon.getIconHeight() * scale));
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        BufferedImage canvas = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = canvas.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.drawImage(scaled, (targetWidth - width) / 2, (targetHeight - height) / 2, null);
        g2.dispose();
        return new ImageIcon(canvas);
    }

    private boolean isMenuItemAvailable(String baseName) {
        MenuItem item = Menu.getInstance().getMenuItem(baseName);
        if (item == null)
            return false;
        if (item.getIngredients().isEmpty())
            return true;

        Inventory inventory = Inventory.getInstance();
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, Double> ing : item.getIngredients().entrySet()) {
            double required = ing.getValue() == null ? 0.0 : ing.getValue();
            if (required <= 0)
                continue;
            inventory.InventoryItem stock = inventory.getItem(ing.getKey());
            if (stock == null || stock.getQuantity() < required)
                return false;
            try {
                List<InventoryBatch> batches = batchRepo.findBatchesForItem(ing.getKey());
                List<InventoryBatch> active = new ArrayList<>();
                for (InventoryBatch b : batches) {
                    if (!b.isArchived())
                        active.add(b);
                }
                if (active.isEmpty())
                    continue;
                double fresh = 0;
                for (InventoryBatch b : active) {
                    String exp = b.getExpiryDate();
                    if (exp != null && !exp.isBlank()) {
                        try {
                            if (LocalDate.parse(exp).isBefore(today))
                                continue;
                        } catch (Exception ignored) {
                        }
                    }
                    fresh += b.getQuantity();
                }
                if (fresh < required)
                    return false;
            } catch (Exception ignored) {
            }
        }
        return true;
    }

    private boolean hasExpiredIngredient(String baseName) {
        MenuItem item = Menu.getInstance().getMenuItem(baseName);
        if (item == null || item.getIngredients().isEmpty())
            return false;
        Inventory inventory = Inventory.getInstance();
        LocalDate today = LocalDate.now();
        for (Map.Entry<String, Double> ing : item.getIngredients().entrySet()) {
            double required = ing.getValue() == null ? 0.0 : ing.getValue();
            if (required <= 0)
                continue;
            inventory.InventoryItem stock = inventory.getItem(ing.getKey());
            if (stock == null || stock.getQuantity() < required)
                continue;
            try {
                List<InventoryBatch> batches = batchRepo.findBatchesForItem(ing.getKey());
                List<InventoryBatch> active = new ArrayList<>();
                for (InventoryBatch b : batches) {
                    if (!b.isArchived())
                        active.add(b);
                }
                if (active.isEmpty())
                    continue;
                double fresh = 0;
                for (InventoryBatch b : active) {
                    String exp = b.getExpiryDate();
                    if (exp != null && !exp.isBlank()) {
                        try {
                            if (LocalDate.parse(exp).isBefore(today))
                                continue;
                        } catch (Exception ignored) {
                        }
                    }
                    fresh += b.getQuantity();
                }
                if (fresh < required)
                    return true;
            } catch (Exception ignored) {
            }
        }
        return false;
    }

    private boolean isMenuItemAvailableForQuantity(String baseName, int qty) {
        if (qty <= 0)
            return false;
        MenuItem item = Menu.getInstance().getMenuItem(baseName);
        if (item == null)
            return false;
        if (item.getIngredients().isEmpty())
            return true;
        Inventory inventory = Inventory.getInstance();
        for (Map.Entry<String, Double> ingredient : item.getIngredients().entrySet()) {
            inventory.InventoryItem stock = inventory.getItem(ingredient.getKey());
            double required = (ingredient.getValue() == null ? 0.0 : ingredient.getValue()) * qty;
            if (stock == null || stock.getQuantity() < required)
                return false;
        }
        return true;
    }

    private String buildUnavailableIngredientsTooltip(String baseName, boolean hasExpired) {
        MenuItem item = Menu.getInstance().getMenuItem(baseName);
        if (item == null)
            return htmlTooltip("Item not found in menu.");

        Inventory inventory = Inventory.getInstance();
        LocalDate today = LocalDate.now();
        StringBuilder details = new StringBuilder();
        for (Map.Entry<String, Double> ingredient : item.getIngredients().entrySet()) {
            double required = ingredient.getValue() == null ? 0.0 : ingredient.getValue();
            inventory.InventoryItem stock = inventory.getItem(ingredient.getKey());
            double available = stock == null ? 0.0 : stock.getQuantity();

            boolean insufficientFresh = false;
            if (hasExpired && stock != null && available >= required) {
                try {
                    List<InventoryBatch> batches = batchRepo.findBatchesForItem(ingredient.getKey());
                    double fresh = 0;
                    for (InventoryBatch b : batches) {
                        if (b.isArchived())
                            continue;
                        String exp = b.getExpiryDate();
                        if (exp != null && !exp.isBlank()) {
                            try {
                                if (LocalDate.parse(exp).isBefore(today))
                                    continue;
                            } catch (Exception ignored) {
                            }
                        }
                        fresh += b.getQuantity();
                    }
                    insufficientFresh = fresh < required;
                } catch (Exception ignored) {
                }
            }

            if (stock == null || available < required || insufficientFresh) {
                if (details.length() > 0)
                    details.append("<br>");
                details.append("<b>").append(escapeHtml(ingredient.getKey()))
                        .append("</b>: need ").append(formatAmount(required))
                        .append(", have ").append(formatAmount(available));
                if (stock == null)
                    details.append(" (missing)");
                else if (insufficientFresh)
                    details.append(" (expired)");
            }
        }
        if (details.length() == 0)
            return htmlTooltip(hasExpired ? "Unavailable: ingredient is expired." : "Unavailable: insufficient stock.");
        return htmlTooltip((hasExpired ? "Expired ingredients:<br>" : "Unavailable ingredients:<br>") + details);
    }

    private String htmlTooltip(String body) {
        return "<html><div style='width:320px; font-family:Segoe UI; font-size:12px;'>" + body + "</div></html>";
    }

    private String escapeHtml(String text) {
        return text == null ? ""
                : text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String formatAmount(double amount) {
        return Math.abs(amount - Math.rint(amount)) < 0.0001
                ? String.valueOf((long) Math.rint(amount))
                : String.format("%.2f", amount);
    }

    private String findCategory(String itemName) {
        for (Map.Entry<String, List<String>> e : CATEGORY_ITEMS.entrySet()) {
            for (String n : e.getValue()) {
                if (n.equals(itemName))
                    return e.getKey();
            }
        }
        String normalized = itemName.replaceFirst("(?i)^(iced)\\s+large\\s+", "$1 ")
                .replaceFirst("(?i)\\s*\\(large\\)$", "").trim();
        if (!normalized.equals(itemName)) {
            for (Map.Entry<String, List<String>> e : CATEGORY_ITEMS.entrySet()) {
                for (String n : e.getValue()) {
                    if (n.equals(normalized))
                        return e.getKey();
                }
            }
        }
        return "";
    }

    private static String fmtOrder(int id) {
        return String.format("#%05d", id);
    }

    private int loadTransactionCounter() {
        try (java.sql.Connection conn = AppDatabase.openConnection();
                java.sql.PreparedStatement ps = conn.prepareStatement(
                        "SELECT transaction_ref FROM sales_transactions ORDER BY id DESC LIMIT 1");
                java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next())
                return parseTransactionNumber(rs.getString(1));
        } catch (Exception ignored) {
        }
        return 1000;
    }

    private int parseTransactionNumber(String ref) {
        if (ref == null)
            return 1000;
        String digits = ref.replaceAll("\\D", "");
        if (digits.isEmpty())
            return 1000;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return 1000;
        }
    }

    private String nextTransactionRef() {
        transactionCounter = Math.max(transactionCounter, loadTransactionCounter()) + 1;
        return "TXN" + String.format("%06d", transactionCounter);
    }

    private String trunc(String s, int len) {
        return s.length() > len ? s.substring(0, len - 3) + "..." : s;
    }
}