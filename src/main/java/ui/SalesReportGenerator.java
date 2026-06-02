package ui;

import persistence.AppDatabase;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generates a Weekly Sales Report PDF and/or a Monthly Sales Report PDF
 * using ReportLab (Python).
 *
 * Data is pulled live from sales_transactions + sales_transaction_items,
 * grouped by product_name — exactly what MonitoringPanel records.
 *
 * Usage from MonitoringPanel:
 * SalesReportGenerator.generateWeekly(parentComponent);
 * SalesReportGenerator.generateMonthly(parentComponent);
 * SalesReportGenerator.generate(parentComponent); // both
 */
public class SalesReportGenerator {

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday");

    private static final int MODE_WEEKLY = 1;
    private static final int MODE_MONTHLY = 2;
    private static final int MODE_BOTH = MODE_WEEKLY | MODE_MONTHLY;

    // ── Public API ────────────────────────────────────────────────────────────

    public static void generateWeekly(java.awt.Component parent) {
        runWorker(parent, MODE_WEEKLY);
    }

    public static void generateMonthly(java.awt.Component parent) {
        runWorker(parent, MODE_MONTHLY);
    }

    public static void generate(java.awt.Component parent) {
        runWorker(parent, MODE_BOTH);
    }

    // ── Background worker ─────────────────────────────────────────────────────

    private static void runWorker(java.awt.Component parent, int mode) {
        javax.swing.SwingWorker<Void, String> worker = new javax.swing.SwingWorker<>() {
            String weeklyPath, monthlyPath;

            @Override
            protected Void doInBackground() throws Exception {
                publish("Querying database…");

                String tmpDir = System.getProperty("java.io.tmpdir");
                weeklyPath = tmpDir + File.separator + "weekly_sales_report.pdf";
                monthlyPath = tmpDir + File.separator + "monthly_sales_report.pdf";

                // ── Pull live data from DB ─────────────────────────
                // weekly: day → product → [orders, revenue] (doubles for revenue)
                // monthly: week-index → product → [orders, revenue]
                List<String> products = new ArrayList<>();
                Map<String, Map<String, double[]>> weeklyData = new LinkedHashMap<>();
                List<Map<String, double[]>> monthlyData = new ArrayList<>();

                if ((mode & MODE_WEEKLY) != 0)
                    loadWeeklyData(products, weeklyData);

                if ((mode & MODE_MONTHLY) != 0)
                    loadMonthlyData(products, monthlyData);

                // If both modes ran, products list may have been populated by weekly;
                // if only monthly, we need to ensure products list is set.
                if (products.isEmpty() && !monthlyData.isEmpty()) {
                    // Re-derive products from monthlyData keys
                    for (Map<String, double[]> wk : monthlyData)
                        for (String p : wk.keySet())
                            if (!products.contains(p))
                                products.add(p);
                    Collections.sort(products);
                }

                publish("Building PDF…");

                String script = buildPythonScript(
                        products, weeklyData, monthlyData,
                        weeklyPath, monthlyPath, mode);

                Path scriptPath = Paths.get(tmpDir, "bm_report_gen.py");
                Files.writeString(scriptPath, script);

                ProcessBuilder pb = new ProcessBuilder("python3", scriptPath.toString());
                pb.redirectErrorStream(true);
                Process proc = pb.start();
                String output = new String(proc.getInputStream().readAllBytes());
                int exit = proc.waitFor();
                if (exit != 0)
                    throw new RuntimeException("Python script failed:\n" + output);

                publish("Opening PDF…");
                return null;
            }

            @Override
            protected void process(List<String> chunks) {
            }

            @Override
            protected void done() {
                try {
                    get();
                    if ((mode & MODE_WEEKLY) != 0)
                        openFile(weeklyPath);
                    if ((mode & MODE_MONTHLY) != 0)
                        openFile(monthlyPath);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javax.swing.JOptionPane.showMessageDialog(parent,
                            "Failed to generate report:\n" + ex.getMessage(),
                            "Report Error", javax.swing.JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }

    // ── DB queries ────────────────────────────────────────────────────────────

    /**
     * Loads weekly data from the current ISO week.
     * Populates:
     * products – sorted list of all product names that appear this week
     * out – day → product → [totalQty, totalRevenue]
     */
    private static void loadWeeklyData(
            List<String> products,
            Map<String, Map<String, double[]>> out) {

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1); // Monday
        LocalDate weekEnd = weekStart.plusDays(6);

        // Initialise all days with empty maps
        for (String day : DAYS)
            out.put(day, new LinkedHashMap<>());

        String sql = "SELECT strftime('%w', st.created_at) AS dow, " +
                "       sti.product_name, " +
                "       SUM(sti.quantity) AS total_qty, " +
                "       SUM(sti.total)    AS total_rev " +
                "FROM sales_transaction_items sti " +
                "JOIN sales_transactions st ON st.id = sti.transaction_id " +
                "WHERE date(st.created_at) BETWEEN ? AND ? " +
                "GROUP BY dow, sti.product_name " +
                "ORDER BY sti.product_name";

        Set<String> seen = new LinkedHashSet<>();
        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ps.setString(2, weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                String dayName = dowToName(rs.getInt("dow"));
                String product = rs.getString("product_name");
                double qty = rs.getDouble("total_qty");
                double rev = rs.getDouble("total_rev");
                if (dayName == null || product == null)
                    continue;
                out.get(dayName).put(product, new double[] { qty, rev });
                seen.add(product);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        products.addAll(seen);
        Collections.sort(products);

        // Back-fill zeros so every day has every product
        for (String day : DAYS)
            for (String p : products)
                out.get(day).putIfAbsent(p, new double[] { 0, 0 });
    }

    /**
     * Loads monthly data: 4 rolling weeks starting from the 1st of this month.
     * Populates:
     * products – sorted union of all product names that appear this month
     * out – list of 4 week-maps: product → [totalQty, totalRevenue]
     */
    private static void loadMonthlyData(
            List<String> products,
            List<Map<String, double[]>> out) {

        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        Set<String> seen = new LinkedHashSet<>();

        for (int w = 0; w < 4; w++) {
            Map<String, double[]> weekMap = new LinkedHashMap<>();
            LocalDate wStart = monthStart.plusWeeks(w);
            LocalDate wEnd = wStart.plusDays(6);

            String sql = "SELECT sti.product_name, " +
                    "       SUM(sti.quantity) AS total_qty, " +
                    "       SUM(sti.total)    AS total_rev " +
                    "FROM sales_transaction_items sti " +
                    "JOIN sales_transactions st ON st.id = sti.transaction_id " +
                    "WHERE date(st.created_at) BETWEEN ? AND ? " +
                    "GROUP BY sti.product_name " +
                    "ORDER BY sti.product_name";

            try (Connection conn = AppDatabase.openConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, wStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
                ps.setString(2, wEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String product = rs.getString("product_name");
                    double qty = rs.getDouble("total_qty");
                    double rev = rs.getDouble("total_rev");
                    if (product == null)
                        continue;
                    weekMap.put(product, new double[] { qty, rev });
                    seen.add(product);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            out.add(weekMap);
        }

        // Only add to products list if not already populated by loadWeeklyData
        if (products.isEmpty()) {
            products.addAll(seen);
            Collections.sort(products);
        } else {
            // Merge any monthly-only products into the existing list
            for (String p : seen)
                if (!products.contains(p))
                    products.add(p);
            Collections.sort(products);
        }

        // Back-fill zeros
        for (Map<String, double[]> wk : out)
            for (String p : products)
                wk.putIfAbsent(p, new double[] { 0, 0 });
    }

    private static String dowToName(int dow) {
        return switch (dow) {
            case 1 -> "Monday";
            case 2 -> "Tuesday";
            case 3 -> "Wednesday";
            case 4 -> "Thursday";
            case 5 -> "Friday";
            case 6 -> "Saturday";
            case 0 -> "Sunday";
            default -> null;
        };
    }

    // ── Python script builder ─────────────────────────────────────────────────

    private static String buildPythonScript(
            List<String> products,
            Map<String, Map<String, double[]>> weeklyData,
            List<Map<String, double[]>> monthlyData,
            String weeklyPath,
            String monthlyPath,
            int mode) {

        StringBuilder sb = new StringBuilder();

        // ── Imports ───────────────────────────────────────────────
        sb.append("# AUTO-GENERATED — do not edit\n");
        sb.append("import os, sys\n");
        sb.append("try:\n");
        sb.append("    from reportlab.lib import colors\n");
        sb.append("    from reportlab.lib.pagesizes import A4\n");
        sb.append("    from reportlab.lib.styles import ParagraphStyle\n");
        sb.append("    from reportlab.lib.units import cm\n");
        sb.append("    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle\n");
        sb.append("    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT\n");
        sb.append("except ImportError:\n");
        sb.append("    os.system(sys.executable + ' -m pip install reportlab -q')\n");
        sb.append("    from reportlab.lib import colors\n");
        sb.append("    from reportlab.lib.pagesizes import A4\n");
        sb.append("    from reportlab.lib.styles import ParagraphStyle\n");
        sb.append("    from reportlab.lib.units import cm\n");
        sb.append("    from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle\n");
        sb.append("    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT\n\n");

        // ── Embedded live data ────────────────────────────────────
        sb.append("PRODUCTS = ").append(pythonList(products)).append("\n");
        sb.append("DAYS     = ").append(pythonList(DAYS)).append("\n\n");

        if ((mode & MODE_WEEKLY) != 0) {
            sb.append("WEEKLY = {\n");
            for (String day : DAYS) {
                sb.append("  '").append(day).append("': {\n");
                Map<String, double[]> catMap = weeklyData.getOrDefault(day, Collections.emptyMap());
                for (String p : products) {
                    double[] v = catMap.getOrDefault(p, new double[] { 0, 0 });
                    sb.append("    '").append(escapePy(p))
                            .append("': [").append((long) v[0]).append(", ").append(String.format("%.2f", v[1]))
                            .append("],\n");
                }
                sb.append("  },\n");
            }
            sb.append("}\n\n");
        }

        if ((mode & MODE_MONTHLY) != 0) {
            sb.append("MONTHLY = [\n");
            for (Map<String, double[]> week : monthlyData) {
                sb.append("  {\n");
                for (String p : products) {
                    double[] v = week.getOrDefault(p, new double[] { 0, 0 });
                    sb.append("    '").append(escapePy(p))
                            .append("': [").append((long) v[0]).append(", ").append(String.format("%.2f", v[1]))
                            .append("],\n");
                }
                sb.append("  },\n");
            }
            sb.append("]\n\n");
        }

        sb.append("WEEKLY_PATH  = r'").append(weeklyPath.replace("\\", "\\\\")).append("'\n");
        sb.append("MONTHLY_PATH = r'").append(monthlyPath.replace("\\", "\\\\")).append("'\n\n");

        // ── Shared Python helpers ─────────────────────────────────
        sb.append("""
                HEADER_BG  = colors.HexColor('#4DA6D8')
                COL_HDR_BG = colors.HexColor('#D9E9F5')
                TOTAL_BG   = colors.HexColor('#EBF5FB')
                WHITE      = colors.white
                DARK       = colors.HexColor('#1A1A1A')
                BORDER     = colors.HexColor('#888888')
                ZEBRA      = colors.HexColor('#F7FBFF')

                def cell(text, bold=False, align='CENTER', size=8, color=None):
                    if color is None: color = DARK
                    amap = {'CENTER': TA_CENTER, 'LEFT': TA_LEFT, 'RIGHT': TA_RIGHT}
                    st = ParagraphStyle('c',
                        fontName='Helvetica-Bold' if bold else 'Helvetica',
                        fontSize=size, textColor=color,
                        alignment=amap.get(align, TA_CENTER), leading=size + 3)
                    return Paragraph(str(text), st)

                def peso(v):
                    return f'\u20b1{v:,.2f}'

                def title_para(text, size=14, bold=True, space_after=4):
                    return Paragraph(text, ParagraphStyle('t',
                        fontName='Helvetica-Bold' if bold else 'Helvetica',
                        fontSize=size, alignment=TA_CENTER,
                        textColor=DARK, spaceAfter=space_after))

                def subtitle_para(text):
                    return Paragraph(text, ParagraphStyle('s',
                        fontName='Helvetica', fontSize=9,
                        alignment=TA_CENTER, textColor=colors.HexColor('#555555'),
                        spaceAfter=14))

                """);

        // ── Weekly report ─────────────────────────────────────────
        if ((mode & MODE_WEEKLY) != 0) {
            sb.append("""
                    def build_weekly():
                        from datetime import date, timedelta
                        today = date.today()
                        ws    = today - timedelta(days=today.weekday())
                        we    = ws + timedelta(days=6)
                        period = f"{ws.strftime('%B %d')} \u2013 {we.strftime('%B %d, %Y')}"
                        generated = date.today().strftime('%B %d, %Y')

                        doc = SimpleDocTemplate(WEEKLY_PATH, pagesize=A4,
                            leftMargin=1.2*cm, rightMargin=1.2*cm,
                            topMargin=1.5*cm, bottomMargin=1.5*cm)
                        story = []

                        story.append(title_para('Better Mondays Cafe', 16))
                        story.append(title_para('Weekly Sales Report', 13))
                        story.append(subtitle_para(f'Period: {period}   |   Generated: {generated}'))

                        if not PRODUCTS:
                            story.append(title_para('No sales recorded for this week.', 11, bold=False))
                            doc.build(story)
                            print(f'[OK] Weekly  -> {WEEKLY_PATH}')
                            return

                        grand_orders = sum(WEEKLY[d][p][0] for d in DAYS for p in PRODUCTS)
                        grand_rev    = sum(WEEKLY[d][p][1] for d in DAYS for p in PRODUCTS)

                        # Column widths: Day | Product | Orders | Revenue | Day% | Cumulative%
                        col_w = [2.0*cm, 5.5*cm, 2.2*cm, 2.8*cm, 2.4*cm, 2.4*cm]

                        hdr = [
                            cell('Day',      bold=True, align='CENTER'),
                            cell('Product',  bold=True, align='LEFT'),
                            cell('Orders',   bold=True),
                            cell('Revenue',  bold=True),
                            cell('% of Day', bold=True),
                            cell('% of Week',bold=True),
                        ]
                        tdata    = [hdr]
                        spans    = []
                        ts_extra = []
                        row_idx  = 1

                        for day in DAYS:
                            day_orders = sum(WEEKLY[day][p][0] for p in PRODUCTS)
                            day_rev    = sum(WEEKLY[day][p][1] for p in PRODUCTS)
                            day_start  = row_idx
                            week_pct   = (day_rev / grand_rev * 100) if grand_rev else 0

                            # Only include products that had at least one order today
                            active = [(p, WEEKLY[day][p]) for p in PRODUCTS if WEEKLY[day][p][0] > 0]
                            if not active:
                                tdata.append([
                                    cell(day, bold=True),
                                    cell('(no sales)', align='LEFT', color=colors.HexColor('#999999')),
                                    cell('0'), cell(peso(0)), cell('0%'), cell(f'{week_pct:.1f}%', bold=True),
                                ])
                                spans += [('SPAN', (0, row_idx), (0, row_idx)),
                                          ('SPAN', (5, row_idx), (5, row_idx))]
                                row_idx += 1
                            else:
                                for i, (p, (qty, rev)) in enumerate(active):
                                    day_pct = (rev / day_rev * 100) if day_rev else 0
                                    tdata.append([
                                        cell(day if i == 0 else '', bold=(i == 0)),
                                        cell(p, align='LEFT'),
                                        cell(str(int(qty))),
                                        cell(peso(rev)),
                                        cell(f'{day_pct:.1f}%'),
                                        cell(f'{week_pct:.1f}%' if i == 0 else '', bold=(i == 0)),
                                    ])
                                    row_idx += 1

                                spans += [('SPAN', (0, day_start), (0, row_idx - 1)),
                                          ('SPAN', (5, day_start), (5, row_idx - 1))]

                            # Day subtotal row
                            tdata.append([
                                cell(''), cell('Day Total', bold=True, align='LEFT'),
                                cell(str(int(day_orders)), bold=True),
                                cell(peso(day_rev), bold=True),
                                cell('100%', bold=True), cell(''),
                            ])
                            ts_extra += [
                                ('BACKGROUND',  (0, row_idx), (-1, row_idx), TOTAL_BG),
                                ('FONTNAME',    (0, row_idx), (-1, row_idx), 'Helvetica-Bold'),
                                ('LINEABOVE',   (0, row_idx), (-1, row_idx), 0.5, BORDER),
                                ('LINEBELOW',   (0, row_idx), (-1, row_idx), 0.8, BORDER),
                            ]
                            row_idx += 1

                        # Grand total row
                        tdata.append([
                            cell('WEEKLY TOTAL', bold=True, color=WHITE),
                            cell('', color=WHITE),
                            cell(str(int(grand_orders)), bold=True, color=WHITE),
                            cell(peso(grand_rev), bold=True, color=WHITE),
                            cell('', color=WHITE),
                            cell('100%', bold=True, color=WHITE),
                        ])
                        gt_row = row_idx
                        spans.append(('SPAN', (0, gt_row), (1, gt_row)))

                        base_style = [
                            ('FONTNAME',      (0, 0),   (-1, -1),        'Helvetica'),
                            ('FONTSIZE',      (0, 0),   (-1, -1),        8),
                            ('GRID',          (0, 0),   (-1, -1),        0.4, BORDER),
                            ('VALIGN',        (0, 0),   (-1, -1),        'MIDDLE'),
                            ('TOPPADDING',    (0, 0),   (-1, -1),        3),
                            ('BOTTOMPADDING', (0, 0),   (-1, -1),        3),
                            ('LEFTPADDING',   (0, 0),   (-1, -1),        5),
                            ('RIGHTPADDING',  (0, 0),   (-1, -1),        5),
                            # Header row
                            ('BACKGROUND',    (0, 0),   (-1, 0),         COL_HDR_BG),
                            ('FONTNAME',      (0, 0),   (-1, 0),         'Helvetica-Bold'),
                            ('FONTSIZE',      (0, 0),   (-1, 0),         9),
                            ('LINEBELOW',     (0, 0),   (-1, 0),         1, BORDER),
                            # Zebra rows
                            ('ROWBACKGROUNDS',(0, 1),   (-1, gt_row - 1),[WHITE, ZEBRA]),
                            # Grand total row
                            ('BACKGROUND',    (0, gt_row), (-1, gt_row), HEADER_BG),
                            ('FONTNAME',      (0, gt_row), (-1, gt_row), 'Helvetica-Bold'),
                            ('LINEABOVE',     (0, gt_row), (-1, gt_row), 1.2, BORDER),
                        ]

                        t = Table(tdata, colWidths=col_w, repeatRows=1)
                        t.setStyle(TableStyle(base_style + ts_extra + spans))
                        story.append(t)
                        doc.build(story)
                        print(f'[OK] Weekly  -> {WEEKLY_PATH}')

                    """);
        }

        // ── Monthly report ────────────────────────────────────────
        if ((mode & MODE_MONTHLY) != 0) {
            sb.append(
                    """
                            def build_monthly():
                                from datetime import date
                                today      = date.today()
                                month_name = today.strftime('%B %Y')
                                generated  = today.strftime('%B %d, %Y')

                                doc = SimpleDocTemplate(MONTHLY_PATH, pagesize=A4,
                                    leftMargin=1.2*cm, rightMargin=1.2*cm,
                                    topMargin=1.5*cm, bottomMargin=1.5*cm)
                                story = []

                                story.append(title_para('Better Mondays Cafe', 16))
                                story.append(title_para('Monthly Sales Report', 13))
                                story.append(subtitle_para(f'Month: {month_name}   |   Generated: {generated}'))

                                if not PRODUCTS:
                                    story.append(title_para('No sales recorded for this month.', 11, bold=False))
                                    doc.build(story)
                                    print(f'[OK] Monthly -> {MONTHLY_PATH}')
                                    return

                                n     = len(MONTHLY)   # number of weeks (always 4)
                                # col widths: Product | (Orders, Revenue) x n | Monthly Total | Best Week
                                col_w = [4.5*cm] + [1.4*cm, 2.4*cm] * n + [2.4*cm, 2.2*cm]

                                # Header row 1 — week spans
                                h1 = [cell('Product', bold=True, align='LEFT')]
                                for w in range(1, n + 1):
                                    h1 += [cell(f'Week {w}', bold=True), cell('')]
                                h1 += [cell('Total', bold=True), cell('Best Week', bold=True)]

                                # Header row 2 — sub-headers
                                h2 = [cell('')]
                                for _ in range(n):
                                    h2 += [cell('Orders', bold=True), cell('Revenue', bold=True)]
                                h2 += [cell('Revenue', bold=True), cell('')]

                                tdata = [h1, h2]
                                spans = [('SPAN', (1 + w * 2, 0), (2 + w * 2, 0)) for w in range(n)]
                                row_idx = 2
                                grand_rev = 0

                                for p in PRODUCTS:
                                    rev_per_week = [MONTHLY[w][p][1] for w in range(n)]
                                    ord_per_week = [MONTHLY[w][p][0] for w in range(n)]
                                    total_rev    = sum(rev_per_week)
                                    total_ord    = sum(ord_per_week)
                                    grand_rev   += total_rev
                                    best_wk      = ord_per_week.index(max(ord_per_week)) + 1 if any(o > 0 for o in ord_per_week) else '-'

                                    r = [cell(p, align='LEFT')]
                                    for w in range(n):
                                        r += [cell(str(int(MONTHLY[w][p][0]))), cell(peso(MONTHLY[w][p][1]))]
                                    r += [cell(peso(total_rev), bold=True),
                                          cell(f'Week {best_wk}' if best_wk != '-' else '-', bold=(best_wk != '-'))]
                                    tdata.append(r)
                                    row_idx += 1

                                # Grand total row
                                gt_orders = [cell(str(int(sum(MONTHLY[w][p][0] for p in PRODUCTS)))) for w in range(n)]
                                gt_rev    = [cell(peso(sum(MONTHLY[w][p][1] for p in PRODUCTS)), bold=True, color=WHITE) for w in range(n)]
                                gr = [cell('MONTHLY TOTAL', bold=True, color=WHITE)]
                                for w in range(n):
                                    gr += [
                                        cell(str(int(sum(MONTHLY[w][p][0] for p in PRODUCTS))), bold=True, color=WHITE),
                                        cell(peso(sum(MONTHLY[w][p][1] for p in PRODUCTS)), bold=True, color=WHITE),
                                    ]
                                gr += [cell(peso(grand_rev), bold=True, color=WHITE), cell('', color=WHITE)]
                                tdata.append(gr)
                                gt_row = row_idx

                                ts = TableStyle([
                                    ('FONTNAME',      (0, 0),   (-1, -1),        'Helvetica'),
                                    ('FONTSIZE',      (0, 0),   (-1, -1),        8),
                                    ('GRID',          (0, 0),   (-1, -1),        0.4, BORDER),
                                    ('VALIGN',        (0, 0),   (-1, -1),        'MIDDLE'),
                                    ('TOPPADDING',    (0, 0),   (-1, -1),        3),
                                    ('BOTTOMPADDING', (0, 0),   (-1, -1),        3),
                                    ('LEFTPADDING',   (0, 0),   (-1, -1),        5),
                                    ('RIGHTPADDING',  (0, 0),   (-1, -1),        5),
                                    # Header rows
                                    ('BACKGROUND',    (0, 0),   (-1, 1),         COL_HDR_BG),
                                    ('FONTNAME',      (0, 0),   (-1, 1),         'Helvetica-Bold'),
                                    ('LINEBELOW',     (0, 1),   (-1, 1),         1, BORDER),
                                    # Zebra rows
                                    ('ROWBACKGROUNDS',(0, 2),   (-1, gt_row - 1),[WHITE, ZEBRA]),
                                    # Grand total row
                                    ('BACKGROUND',    (0, gt_row), (-1, gt_row), HEADER_BG),
                                    ('FONTNAME',      (0, gt_row), (-1, gt_row), 'Helvetica-Bold'),
                                    ('LINEABOVE',     (0, gt_row), (-1, gt_row), 1.2, BORDER),
                                ] + spans)

                                t = Table(tdata, colWidths=col_w, repeatRows=2)
                                t.setStyle(ts)
                                story.append(t)
                                doc.build(story)
                                print(f'[OK] Monthly -> {MONTHLY_PATH}')

                            """);
        }

        // ── Call what was requested ───────────────────────────────
        if ((mode & MODE_WEEKLY) != 0)
            sb.append("build_weekly()\n");
        if ((mode & MODE_MONTHLY) != 0)
            sb.append("build_monthly()\n");

        return sb.toString();
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    private static String pythonList(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("'").append(escapePy(items.get(i))).append("'");
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Escapes a string for embedding inside a Python single-quoted string literal.
     */
    private static String escapePy(String s) {
        return s.replace("\\", "\\\\").replace("'", "\\'");
    }

    private static void openFile(String path) {
        try {
            File f = new File(path);
            if (f.exists() && Desktop.isDesktopSupported())
                Desktop.getDesktop().open(f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}