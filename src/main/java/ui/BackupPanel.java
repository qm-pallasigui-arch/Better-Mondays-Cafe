package ui;

import persistence.AppDatabase;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BackupPanel extends JPanel {

    // ── Constants ─────────────────────────────────────────────────────────────

    private static final Path DB_PATH = Paths.get("data", "coffee-cafe.db");
    private static final DateTimeFormatter FILE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Palette
    private static final Color BLUE_HEADER = new Color(0x1A5FA8);
    private static final Color BLUE_BTN = new Color(0x1E6FCC);
    private static final Color BLUE_HOVER = new Color(0x1558A8);
    private static final Color RED_BORDER = new Color(0xFCA5A5);
    private static final Color RED_BG = new Color(0xFFF1F2);
    private static final Color RED_TITLE = new Color(0xB91C1C);
    private static final Color RED_BTN_FG = new Color(0xDC2626);
    private static final Color RED_BTN_HBG = new Color(0xFEE2E2);
    private static final Color ROW_ALT = new Color(0xF8F9FA);
    private static final Color ROW_NORMAL = Color.WHITE;
    private static final Color BORDER_COLOR = new Color(0xE5E7EB);
    private static final Color TEXT_PRIMARY = new Color(0x111827);
    private static final Color TEXT_SECONDARY = new Color(0x6B7280);
    private static final Color TEXT_MONO = new Color(0x374151);
    private static final Color GREEN_ICON = new Color(0x15803D);

    private static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_LABEL = new Font("Segoe UI", Font.BOLD, 11);
    private static final Font FONT_MONO = new Font("Consolas", Font.PLAIN, 12);

    // ── Painted icon helpers ──────────────────────────────────────────────────

    /** Down-arrow (↓) drawn with Graphics2D — used on Backup button. */
    private static Component makeArrowDown(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(10, 14));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int cx = getWidth() / 2;
                // shaft
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, 1, cx, getHeight() - 4);
                // arrowhead
                int[] xs = { cx - 4, cx + 4, cx };
                int[] ys = { getHeight() - 5, getHeight() - 5, getHeight() - 1 };
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
    }

    /** Up-arrow (↑) drawn with Graphics2D — used on Restore button. */
    private static Component makeArrowUp(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(10, 14));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int cx = getWidth() / 2;
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(cx, getHeight() - 2, cx, 4);
                int[] xs = { cx - 4, cx + 4, cx };
                int[] ys = { 4, 4, 0 };
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
    }

    /** Circular reset arrow (↺) drawn with Graphics2D — used on Reset button. */
    private static Component makeResetArrow(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(14, 14));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // Arc ~300 degrees
                int pad = 2;
                g2.drawArc(pad, pad, getWidth() - pad * 2 - 1, getHeight() - pad * 2 - 1, 60, 300);
                // Arrowhead at end of arc (near 60 deg)
                int cx = getWidth() / 2, cy = getHeight() / 2;
                int r = (getWidth() - pad * 2) / 2;
                double ang = Math.toRadians(60);
                int ax = (int) (cx + r * Math.cos(ang));
                int ay = (int) (cy - r * Math.sin(ang));
                int[] xs = { ax - 3, ax + 3, ax };
                int[] ys = { ay - 1, ay - 1, ay + 4 };
                g2.fillPolygon(xs, ys, 3);
                g2.dispose();
            }
        };
    }

    /** Filled red warning triangle with "!" — used in danger card header. */
    private static JComponent makeWarningIcon() {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(18, 18));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                int W = getWidth(), H = getHeight();
                int[] xs = { W / 2, 0, W };
                int[] ys = { 0, H - 1, H - 1 };
                g2.setColor(RED_TITLE);
                g2.fillPolygon(xs, ys, 3);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 10));
                FontMetrics fm = g2.getFontMetrics();
                String bang = "!";
                g2.drawString(bang, (W - fm.stringWidth(bang)) / 2, H - 3);
                g2.dispose();
            }
        };
    }

    /** Green checkmark (✓) drawn with Graphics2D. */
    private static JComponent makeCheckIcon(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(13, 13));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                // tick: short left leg, long right leg
                g2.drawLine(1, 6, 4, 10);
                g2.drawLine(4, 10, 11, 2);
                g2.dispose();
            }
        };
    }

    /** Red X cross (✗) drawn with Graphics2D. */
    private static JComponent makeCrossIcon(Color color) {
        return new JComponent() {
            {
                setPreferredSize(new Dimension(13, 13));
                setOpaque(false);
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(color);
                g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(2, 2, 10, 10);
                g2.drawLine(10, 2, 2, 10);
                g2.dispose();
            }
        };
    }

    /** Anti-alias helper. */
    private static Graphics2D aa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g2;
    }

    // ── State ─────────────────────────────────────────────────────────────────

    private final DefaultTableModel historyModel = new DefaultTableModel(
            new String[] { "File Name", "Type", "Size", "Timestamp" }, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable historyTable = new JTable(historyModel);
    private final JLabel statusLabel = new JLabel("Ready");
    private final JLabel rowCountLabel = new JLabel("0 entries");

    private final JButton backupBtn = createPrimaryButton("Backup Now", () -> makeArrowDown(Color.WHITE));
    private final JButton restoreBtn = createGhostButton("Restore from File", () -> makeArrowUp(TEXT_PRIMARY));
    private final JButton resetProdBtn = createDangerButton("Reset for Production", () -> makeResetArrow(RED_BTN_FG));

    // ── Constructor ───────────────────────────────────────────────────────────

    public BackupPanel() {
        super(new BorderLayout(0, 16));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(new Color(0xF3F4F6));

        backupBtn.addActionListener(e -> onBackup());
        restoreBtn.addActionListener(e -> onRestore());
        resetProdBtn.addActionListener(e -> onResetForProduction());

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(buildMainCard());
        center.add(Box.createVerticalStrut(16));
        center.add(buildDangerCard());

        add(center, BorderLayout.CENTER);
        add(buildStatusBar(), BorderLayout.SOUTH);
    }

    // ── Card: Backup & Restore ────────────────────────────────────────────────

    private JPanel buildMainCard() {
        JPanel card = createCard();
        card.setLayout(new BorderLayout(0, 0));
        card.add(buildCardHeader("Database Backup & Restore", "data/coffee-cafe.db"), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 0));
        body.setOpaque(false);
        body.add(buildActionRow(), BorderLayout.NORTH);
        body.add(buildHistorySection(), BorderLayout.CENTER);

        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildCardHeader(String title, String subtitle) {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setOpaque(false);
        header.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 14, 10, 14)));

        // "DB" pill on the left
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setOpaque(false);

        JLabel pill = new JLabel("DB");
        pill.setFont(new Font("Segoe UI", Font.BOLD, 10));
        pill.setForeground(Color.WHITE);
        pill.setBackground(BLUE_HEADER);
        pill.setOpaque(true);
        pill.setBorder(new EmptyBorder(2, 6, 2, 6));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(FONT_LABEL);
        titleLbl.setForeground(TEXT_SECONDARY);

        left.add(pill);
        left.add(titleLbl);

        JLabel subLbl = new JLabel(subtitle);
        subLbl.setFont(FONT_MONO);
        subLbl.setForeground(TEXT_SECONDARY);

        header.add(left, BorderLayout.WEST);
        header.add(subLbl, BorderLayout.EAST);
        return header;
    }

    private JPanel buildActionRow() {
        JPanel row = new JPanel(new BorderLayout(12, 0));
        row.setOpaque(false);
        row.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(10, 14, 10, 14)));

        JLabel info = new JLabel(
                "<html><span style='color:#6B7280;font-size:11px'>"
                        + "Backup creates a full copy of the database (.db). "
                        + "Restore replaces the current database — the app will restart automatically."
                        + "</span></html>");
        info.setFont(FONT_BODY);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(backupBtn);
        btns.add(restoreBtn);

        row.add(info, BorderLayout.CENTER);
        row.add(btns, BorderLayout.EAST);
        return row;
    }

    private JPanel buildHistorySection() {
        JPanel subHeader = new JPanel(new BorderLayout(8, 0));
        subHeader.setOpaque(false);
        subHeader.setBorder(new CompoundBorder(
                new MatteBorder(0, 0, 1, 0, BORDER_COLOR),
                new EmptyBorder(8, 14, 8, 14)));

        JLabel histLbl = new JLabel("Session Backup History");
        histLbl.setFont(FONT_LABEL);
        histLbl.setForeground(TEXT_SECONDARY);

        rowCountLabel.setFont(FONT_MONO);
        rowCountLabel.setForeground(TEXT_SECONDARY);

        subHeader.add(histLbl, BorderLayout.WEST);
        subHeader.add(rowCountLabel, BorderLayout.EAST);

        styleHistoryTable();
        JScrollPane scroll = new JScrollPane(historyTable);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setPreferredSize(new Dimension(0, 180));
        scroll.getViewport().setBackground(ROW_NORMAL);

        JPanel section = new JPanel(new BorderLayout(0, 0));
        section.setOpaque(false);
        section.add(subHeader, BorderLayout.NORTH);
        section.add(scroll, BorderLayout.CENTER);
        return section;
    }

    private void styleHistoryTable() {
        historyTable.setFont(FONT_BODY);
        historyTable.setRowHeight(34);
        historyTable.setGridColor(BORDER_COLOR);
        historyTable.setShowGrid(true);
        historyTable.setIntercellSpacing(new Dimension(0, 0));
        historyTable.setSelectionBackground(new Color(0xDBEAFE));
        historyTable.setSelectionForeground(TEXT_PRIMARY);
        historyTable.setFillsViewportHeight(true);
        historyTable.setFocusable(false);

        JTableHeader th = historyTable.getTableHeader();
        th.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                JLabel lbl = new JLabel(v == null ? "" : v.toString());
                lbl.setOpaque(true);
                lbl.setBackground(BLUE_HEADER);
                lbl.setForeground(Color.WHITE);
                lbl.setFont(FONT_LABEL);
                lbl.setBorder(new EmptyBorder(0, 12, 0, 12));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                return lbl;
            }
        });
        th.setPreferredSize(new Dimension(0, 36));
        th.setBorder(BorderFactory.createEmptyBorder());
        th.setReorderingAllowed(false);

        historyTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int r, int c) {
                String val = v == null ? "" : v.toString();
                if (c == 1) {
                    JLabel badge = new JLabel(val, SwingConstants.CENTER);
                    badge.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    badge.setOpaque(true);
                    badge.setBorder(new EmptyBorder(2, 10, 2, 10));
                    switch (val) {
                        case "Backup" -> {
                            badge.setBackground(new Color(0xDBEAFE));
                            badge.setForeground(new Color(0x1E40AF));
                        }
                        case "Restore" -> {
                            badge.setBackground(new Color(0xDCFCE7));
                            badge.setForeground(new Color(0x166534));
                        }
                        case "Reset" -> {
                            badge.setBackground(new Color(0xFEE2E2));
                            badge.setForeground(new Color(0x991B1B));
                        }
                        default -> {
                            badge.setBackground(new Color(0xF3F4F6));
                            badge.setForeground(TEXT_SECONDARY);
                        }
                    }
                    JPanel wrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
                    wrap.setBackground(r % 2 == 0 ? ROW_NORMAL : ROW_ALT);
                    wrap.add(badge);
                    return wrap;
                }
                super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                setBackground(sel ? new Color(0xDBEAFE) : (r % 2 == 0 ? ROW_NORMAL : ROW_ALT));
                setForeground(sel ? TEXT_PRIMARY : (c == 0 ? TEXT_MONO : (c == 3 ? TEXT_SECONDARY : TEXT_PRIMARY)));
                setFont(c == 0 || c == 3 ? FONT_MONO : FONT_BODY);
                setBorder(new EmptyBorder(0, 12, 0, 12));
                return this;
            }
        });

        TableColumnModel cm = historyTable.getColumnModel();
        cm.getColumn(1).setPreferredWidth(80);
        cm.getColumn(1).setMaxWidth(100);
        cm.getColumn(2).setPreferredWidth(80);
        cm.getColumn(2).setMaxWidth(100);
        cm.getColumn(3).setPreferredWidth(160);
        cm.getColumn(3).setMaxWidth(180);
    }

    // ── Card: Danger Zone ─────────────────────────────────────────────────────

    private JPanel buildDangerCard() {
        JPanel card = new JPanel(new BorderLayout(0, 0));
        card.setBackground(Color.WHITE);
        card.setBorder(new CompoundBorder(
                new LineBorder(RED_BORDER, 1, true),
                new EmptyBorder(0, 0, 0, 0)));

        // Red header bar with painted warning icon
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 10));
        header.setBackground(RED_BG);
        header.setBorder(new MatteBorder(0, 0, 1, 0, RED_BORDER));
        header.add(makeWarningIcon());
        JLabel title = new JLabel("PRODUCTION RESET");
        title.setFont(FONT_LABEL);
        title.setForeground(RED_TITLE);
        header.add(title);

        // Body
        JPanel body = new JPanel(new BorderLayout(0, 12));
        body.setOpaque(false);
        body.setBorder(new EmptyBorder(14, 14, 14, 14));

        JPanel columns = new JPanel(new GridLayout(1, 2, 20, 0));
        columns.setOpaque(false);
        columns.add(buildDangerColumn("WILL ERASE", new Color(0x991B1B), RED_TITLE,
                new String[] {
                        "All staff shift records",
                        "Sales & transaction history",
                        "Inventory batch quantities",
                        "All user accounts",
                        "Password reset requests"
                }, false));
        columns.add(buildDangerColumn("WILL KEEP", new Color(0x166534), GREEN_ICON,
                new String[] {
                        "Menu items & ingredients",
                        "Inventory item definitions"
                }, true));

        JPanel footer = new JPanel(new BorderLayout(0, 6));
        footer.setOpaque(false);
        JLabel note = new JLabel("Default admin and staff accounts will be re-created automatically after reset.");
        note.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        note.setForeground(TEXT_SECONDARY);

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(resetProdBtn);

        footer.add(note, BorderLayout.CENTER);
        footer.add(btnRow, BorderLayout.SOUTH);

        body.add(columns, BorderLayout.CENTER);
        body.add(footer, BorderLayout.SOUTH);

        card.add(header, BorderLayout.NORTH);
        card.add(body, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildDangerColumn(String heading, Color headingColor, Color iconColor,
            String[] items, boolean keep) {
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel hdr = new JLabel(heading);
        hdr.setFont(FONT_LABEL);
        hdr.setForeground(headingColor);
        hdr.setBorder(new EmptyBorder(0, 0, 6, 0));
        col.add(hdr);

        for (String item : items) {
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
            row.setOpaque(false);

            // Painted icon — no font glyph dependency at all
            JComponent ico = keep ? makeCheckIcon(iconColor) : makeCrossIcon(iconColor);

            JLabel txt = new JLabel(item);
            txt.setFont(FONT_BODY);
            txt.setForeground(TEXT_SECONDARY);
            row.add(ico);
            row.add(txt);
            col.add(row);
        }
        return col;
    }

    // ── Status bar ────────────────────────────────────────────────────────────

    private JPanel buildStatusBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setOpaque(false);
        bar.setBorder(new EmptyBorder(6, 2, 0, 2));
        statusLabel.setFont(FONT_MONO);
        statusLabel.setForeground(TEXT_SECONDARY);
        bar.add(statusLabel, BorderLayout.WEST);
        return bar;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void onBackup() {
        if (!Files.exists(DB_PATH)) {
            showError("Database file not found at: " + DB_PATH.toAbsolutePath());
            return;
        }
        String timestamp = LocalDateTime.now().format(FILE_FMT);
        String defaultName = "backup-" + timestamp + ".db";

        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Save Backup As");
        chooser.setSelectedFile(new java.io.File(defaultName));
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite Database (*.db)", "db"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        Path dest = chooser.getSelectedFile().toPath();
        if (!dest.toString().endsWith(".db"))
            dest = dest.resolveSibling(dest.getFileName() + ".db");

        final Path finalDest = dest;
        backupBtn.setEnabled(false);
        setStatus("Creating backup…");

        new SwingWorker<Long, Void>() {
            @Override
            protected Long doInBackground() throws Exception {
                Files.copy(DB_PATH, finalDest, StandardCopyOption.REPLACE_EXISTING);
                return Files.size(finalDest);
            }

            @Override
            protected void done() {
                backupBtn.setEnabled(true);
                try {
                    long bytes = get();
                    String size = formatSize(bytes);
                    String ts = LocalDateTime.now().format(DISPLAY_FMT);
                    addHistoryRow(finalDest.getFileName().toString(), "Backup", size, ts);
                    setStatus("Backup saved: " + finalDest.toAbsolutePath());
                    showStyledInfo("Backup Created",
                            "<html><b>Backup created successfully.</b><br><br>"
                                    + "<span style='color:#6B7280'>File: " + finalDest.toAbsolutePath()
                                    + "<br>Size: " + size + "</span></html>");
                } catch (Exception ex) {
                    setStatus("Backup failed.");
                    showError("Backup failed:\n" + rootCause(ex));
                }
            }
        }.execute();
    }

    private void onRestore() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Select Backup File to Restore");
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("SQLite Database (*.db)", "db"));
        if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
            return;

        Path src = chooser.getSelectedFile().toPath();
        if (!Files.exists(src)) {
            showError("Selected file does not exist.");
            return;
        }

        int confirm = showStyledConfirm("Confirm Restore",
                "<html><b>Restore database?</b><br><br>"
                        + "<span style='color:#6B7280'>Source: " + src.toAbsolutePath() + "<br><br>"
                        + "This will replace the current database. All unsaved changes will be lost.<br>"
                        + "The application will restart automatically after restore.</span></html>");
        if (confirm != JOptionPane.YES_OPTION)
            return;

        restoreBtn.setEnabled(false);
        setStatus("Restoring…");

        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                AppDatabase.resetForRestore();
                Files.copy(src, DB_PATH, StandardCopyOption.REPLACE_EXISTING);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    String ts = LocalDateTime.now().format(DISPLAY_FMT);
                    addHistoryRow(src.getFileName().toString(), "Restore", "—", ts);
                    showStyledInfo("Restore Complete",
                            "<html><b>Restore complete.</b><br><br>"
                                    + "<span style='color:#6B7280'>The application will now close and must be restarted manually.</span></html>");
                    System.exit(0);
                } catch (Exception ex) {
                    restoreBtn.setEnabled(true);
                    setStatus("Restore failed.");
                    showError("Restore failed:\n" + rootCause(ex));
                }
            }
        }.execute();
    }

    private void onResetForProduction() {
        int first = showStyledConfirm("Reset for Production — Step 1 of 2",
                "<html><b>Reset for production?</b><br><br>"
                        + "<span style='color:#6B7280'>This will permanently erase all operational data including shifts, "
                        + "sales, transactions, inventory batches, and all user accounts.<br><br>"
                        + "Menu items, ingredients, and inventory definitions will be kept.<br><br>"
                        + "<b style='color:#B91C1C'>This cannot be undone.</b></span></html>");
        if (first != JOptionPane.YES_OPTION)
            return;

        JDialog dialog = createStyledDialog("Reset for Production — Step 2 of 2");
        dialog.setResizable(false);

        JPanel panel = new JPanel(new BorderLayout(0, 14));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        panel.setBackground(Color.WHITE);
        panel.setPreferredSize(new Dimension(460, 180));

        JLabel warningLbl = new JLabel(
                "<html><div style='width:380px;color:#374151'>"
                        + "Type <b>RESET</b> to confirm the production reset. "
                        + "This action <b style='color:#B91C1C'>cannot be undone.</b>"
                        + "</div></html>");
        warningLbl.setFont(FONT_BODY);

        JPanel inputBlock = new JPanel(new BorderLayout(0, 6));
        inputBlock.setOpaque(false);

        JLabel inputLabel = new JLabel("Confirmation word");
        inputLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        inputLabel.setForeground(TEXT_SECONDARY);

        JTextField input = new JTextField();
        input.setFont(FONT_MONO);
        input.setPreferredSize(new Dimension(0, 36));
        input.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(4, 10, 4, 10)));

        inputBlock.add(inputLabel, BorderLayout.NORTH);
        inputBlock.add(input, BorderLayout.CENTER);

        JButton cancelBtn = createGhostButton("Cancel", null);
        JButton confirmBtn = createDangerButton("Reset production data", null);
        confirmBtn.setEnabled(false);
        confirmBtn.setPreferredSize(new Dimension(200, 34));

        input.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            void update() {
                confirmBtn.setEnabled("RESET".equals(input.getText().trim()));
            }

            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                update();
            }
        });

        cancelBtn.addActionListener(e -> dialog.dispose());
        confirmBtn.addActionListener(e -> {
            dialog.dispose();
            executeProductionReset();
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(cancelBtn);
        btnRow.add(confirmBtn);

        panel.add(warningLbl, BorderLayout.NORTH);
        panel.add(inputBlock, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private void executeProductionReset() {
        resetProdBtn.setEnabled(false);
        setStatus("Resetting…");
        new SwingWorker<Void, Void>() {
            private String errorMsg;

            @Override
            protected Void doInBackground() {
                try {
                    persistence.AppDatabase.resetForProduction();
                } catch (Exception ex) {
                    errorMsg = ex.getMessage();
                }
                return null;
            }

            @Override
            protected void done() {
                resetProdBtn.setEnabled(true);
                if (errorMsg != null) {
                    setStatus("Reset failed.");
                    showError("Production reset failed:\n" + errorMsg);
                } else {
                    String ts = LocalDateTime.now().format(DISPLAY_FMT);
                    addHistoryRow("— production reset —", "Reset", "—", ts);
                    setStatus("Production reset completed at " + ts);
                    showStyledInfo("Reset Complete",
                            "<html><b>Production reset complete.</b><br><br>"
                                    + "<span style='color:#6B7280'>All operational data has been erased.<br>"
                                    + "Default admin and staff accounts have been re-created.<br><br>"
                                    + "The system is ready for live use.</span></html>");
                }
            }
        }.execute();
    }

    // ── Styled dialog helpers ─────────────────────────────────────────────────

    private JDialog createStyledDialog(String title) {
        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog d = (owner instanceof Frame)
                ? new JDialog((Frame) owner, title, true)
                : new JDialog((Dialog) owner, title, true);
        d.getRootPane().setBorder(new LineBorder(BORDER_COLOR, 1));
        return d;
    }

    private void showStyledInfo(String title, String html) {
        JDialog dialog = createStyledDialog(title);
        dialog.setResizable(false);
        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        panel.setBackground(Color.WHITE);

        JLabel msg = new JLabel(html.replace("<html>", "<html><div style='width:380px'>"));
        msg.setFont(FONT_BODY);

        JButton ok = createPrimaryButton("Done", null);
        ok.addActionListener(e -> dialog.dispose());

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        btnRow.setOpaque(false);
        btnRow.add(ok);

        panel.add(msg, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private int showStyledConfirm(String title, String html) {
        JDialog dialog = createStyledDialog(title);
        dialog.setResizable(false);
        int[] result = { JOptionPane.NO_OPTION };

        JPanel panel = new JPanel(new BorderLayout(0, 16));
        panel.setBorder(new EmptyBorder(24, 28, 24, 28));
        panel.setBackground(Color.WHITE);

        JLabel msg = new JLabel(html.replace("<html>", "<html><div style='width:380px'>"));
        msg.setFont(FONT_BODY);

        JButton cancel = createGhostButton("Cancel", null);
        JButton confirm = createDangerButton("Continue", null);
        cancel.addActionListener(e -> dialog.dispose());
        confirm.addActionListener(e -> {
            result[0] = JOptionPane.YES_OPTION;
            dialog.dispose();
        });

        JPanel btnRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnRow.setOpaque(false);
        btnRow.add(cancel);
        btnRow.add(confirm);

        panel.add(msg, BorderLayout.CENTER);
        panel.add(btnRow, BorderLayout.SOUTH);
        dialog.add(panel);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
        return result[0];
    }

    private void showError(String msg) {
        showStyledInfo("Error", "<html><span style='color:#B91C1C'>" + msg + "</span></html>");
    }

    // ── Table helpers ─────────────────────────────────────────────────────────

    private void addHistoryRow(String file, String type, String size, String ts) {
        historyModel.insertRow(0, new Object[] { file, type, size, ts });
        rowCountLabel.setText(historyModel.getRowCount()
                + (historyModel.getRowCount() == 1 ? " entry" : " entries"));
    }

    // ── Button factories ──────────────────────────────────────────────────────

    /**
     * iconSupplier — a java.util.function.Supplier<Component> that returns a
     * freshly painted icon component each time. Pass null for no icon.
     */
    private static JButton createPrimaryButton(String text,
            java.util.function.Supplier<Component> iconSupplier) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(hovered && isEnabled() ? BLUE_HOVER : (isEnabled() ? BLUE_BTN : new Color(0xAFC4E0)));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        buildButtonContent(btn, text, iconSupplier, Color.WHITE);
        btn.setForeground(Color.WHITE);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        styleBtn(btn);
        return btn;
    }

    private static JButton createGhostButton(String text,
            java.util.function.Supplier<Component> iconSupplier) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(hovered ? new Color(0xF3F4F6) : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(BORDER_COLOR);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        buildButtonContent(btn, text, iconSupplier, TEXT_PRIMARY);
        btn.setForeground(TEXT_PRIMARY);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        styleBtn(btn);
        return btn;
    }

    private static JButton createDangerButton(String text,
            java.util.function.Supplier<Component> iconSupplier) {
        JButton btn = new JButton() {
            private boolean hovered = false;
            {
                addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent e) {
                        hovered = true;
                        repaint();
                    }

                    public void mouseExited(MouseEvent e) {
                        hovered = false;
                        repaint();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = aa(g);
                g2.setColor(hovered ? RED_BTN_HBG : Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(RED_BTN_FG);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        buildButtonContent(btn, text, iconSupplier, RED_BTN_FG);
        btn.setForeground(RED_BTN_FG);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        styleBtn(btn);
        return btn;
    }

    /**
     * Replaces the button's default text rendering with a JPanel containing a
     * painted icon component + JLabel. This avoids any glyph rendering via fonts.
     */
    private static void buildButtonContent(JButton btn, String text,
            java.util.function.Supplier<Component> iconSupplier, Color fgColor) {
        btn.setLayout(new BorderLayout());
        JPanel inner = new JPanel(new FlowLayout(FlowLayout.CENTER, 5, 0));
        inner.setOpaque(false);
        if (iconSupplier != null) {
            Component ico = iconSupplier.get();
            // Vertically center the icon inside the button height
            JPanel icoWrap = new JPanel(new GridBagLayout());
            icoWrap.setOpaque(false);
            icoWrap.add(ico);
            inner.add(icoWrap);
        }
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_BOLD);
        lbl.setForeground(fgColor);
        inner.add(lbl);
        btn.add(inner, BorderLayout.CENTER);
    }

    private static void styleBtn(JButton btn) {
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(180, 34));
        btn.setMargin(new Insets(0, 0, 0, 0));
    }

    // ── Card container ────────────────────────────────────────────────────────

    private static JPanel createCard() {
        JPanel p = new JPanel();
        p.setBackground(Color.WHITE);
        p.setBorder(new CompoundBorder(
                new LineBorder(BORDER_COLOR, 1, true),
                new EmptyBorder(0, 0, 0, 0)));
        return p;
    }

    // ── Misc helpers ──────────────────────────────────────────────────────────

    private void setStatus(String msg) {
        statusLabel.setText(msg);
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024)
            return bytes + " B";
        if (bytes < 1024 * 1024)
            return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024));
    }

    private static String rootCause(Throwable t) {
        while (t.getCause() != null)
            t = t.getCause();
        return t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
    }
}