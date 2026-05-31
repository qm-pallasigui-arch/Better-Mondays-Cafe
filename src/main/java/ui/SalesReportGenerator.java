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
 * using ReportLab (Python). The Java side queries the DB, serialises
 * the data as a tiny Python literal, writes a self-contained script,
 * runs it, then opens the PDF(s) with the system viewer.
 *
 * Usage from MonitoringPanel:
 * SalesReportGenerator.generateWeekly(parentComponent);
 * SalesReportGenerator.generateMonthly(parentComponent);
 * SalesReportGenerator.generate(parentComponent); // both
 */
public class SalesReportGenerator {

    // ── Menu categories (must match what is stored in the DB) ────────────────
    private static final List<String> CATEGORIES = List.of(
            "Espresso & Coffee",
            "Specialty Drinks",
            "Tea Latte",
            "Non-Coffee",
            "Fruit Tea",
            "Herbal Tea",
            "Sandwiches",
            "Pandesal Pairs",
            "Pastries");

    private static final List<String> DAYS = List.of(
            "Monday", "Tuesday", "Wednesday", "Thursday",
            "Friday", "Saturday", "Sunday");

    // ── Report mode flags ─────────────────────────────────────────────────────
    private static final int MODE_WEEKLY = 1;
    private static final int MODE_MONTHLY = 2;
    private static final int MODE_BOTH = MODE_WEEKLY | MODE_MONTHLY;

    // ─────────────────────────────────────────────────────────────────────────

    /** Generates only the Weekly Sales Report PDF and opens it. */
    public static void generateWeekly(java.awt.Component parent) {
        runWorker(parent, MODE_WEEKLY);
    }

    /** Generates only the Monthly Sales Report PDF and opens it. */
    public static void generateMonthly(java.awt.Component parent) {
        runWorker(parent, MODE_MONTHLY);
    }

    /** Generates both Weekly and Monthly Sales Report PDFs and opens them. */
    public static void generate(java.awt.Component parent) {
        runWorker(parent, MODE_BOTH);
    }

    // ─── Worker ───────────────────────────────────────────────────────────────

    private static void runWorker(java.awt.Component parent, int mode) {
        javax.swing.SwingWorker<Void, String> worker = new javax.swing.SwingWorker<>() {
            String weeklyPath, monthlyPath;

            @Override
            protected Void doInBackground() throws Exception {
                publish("Querying database…");

                String tmpDir = System.getProperty("java.io.tmpdir");
                weeklyPath = tmpDir + File.separator + "weekly_sales_report.pdf";
                monthlyPath = tmpDir + File.separator + "monthly_sales_report.pdf";

                // Only load the data we actually need
                Map<String, Map<String, int[]>> weeklyData = (mode & MODE_WEEKLY) != 0 ? loadWeeklyData()
                        : Collections.emptyMap();
                List<Map<String, int[]>> monthlyData = (mode & MODE_MONTHLY) != 0 ? loadMonthlyData()
                        : Collections.emptyList();

                publish("Building PDF…");

                String script = buildPythonScript(weeklyData, monthlyData,
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
                // Optionally wire to a progress label
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

    // ─── DB queries ──────────────────────────────────────────────────────────

    private static Map<String, Map<String, int[]>> loadWeeklyData() {
        Map<String, Integer> defaultPrices = new LinkedHashMap<>();
        defaultPrices.put("Espresso & Coffee", 120);
        defaultPrices.put("Specialty Drinks", 150);
        defaultPrices.put("Tea Latte", 110);
        defaultPrices.put("Non-Coffee", 90);
        defaultPrices.put("Fruit Tea", 100);
        defaultPrices.put("Herbal Tea", 95);
        defaultPrices.put("Sandwiches", 180);
        defaultPrices.put("Pandesal Pairs", 80);
        defaultPrices.put("Pastries", 75);

        Map<String, Map<String, int[]>> result = new LinkedHashMap<>();
        for (String day : DAYS) {
            Map<String, int[]> row = new LinkedHashMap<>();
            for (String cat : CATEGORIES)
                row.put(cat, new int[] { 0, defaultPrices.getOrDefault(cat, 0) });
            result.put(day, row);
        }

        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(today.getDayOfWeek().getValue() - 1);
        LocalDate weekEnd = weekStart.plusDays(6);

        String sql = "SELECT strftime('%w', st.created_at) as dow, " +
                "       sti.category, " +
                "       SUM(sti.quantity) as total_orders, " +
                "       AVG(sti.price)    as avg_price " +
                "FROM sales_transaction_items sti " +
                "JOIN sales_transactions st ON st.id = sti.transaction_id " +
                "WHERE date(st.created_at) BETWEEN ? AND ? " +
                "GROUP BY dow, sti.category";

        try (Connection conn = AppDatabase.openConnection();
                PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ps.setString(2, weekEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                int dow = rs.getInt("dow");
                String dayName = dowToName(dow);
                String cat = rs.getString("category");
                int orders = rs.getInt("total_orders");
                int price = (int) rs.getDouble("avg_price");
                if (dayName != null && CATEGORIES.contains(cat)) {
                    result.get(dayName).put(cat, new int[] { orders,
                            price == 0 ? defaultPrices.getOrDefault(cat, 0) : price });
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    private static List<Map<String, int[]>> loadMonthlyData() {
        List<Map<String, int[]>> weeks = new ArrayList<>();
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        for (int w = 0; w < 4; w++) {
            Map<String, int[]> weekMap = new LinkedHashMap<>();
            for (String cat : CATEGORIES)
                weekMap.put(cat, new int[] { 0, 0 });

            LocalDate wStart = monthStart.plusWeeks(w);
            LocalDate wEnd = wStart.plusDays(6);

            String sql = "SELECT sti.category, " +
                    "       SUM(sti.quantity) as total_orders, " +
                    "       SUM(sti.total)    as total_revenue " +
                    "FROM sales_transaction_items sti " +
                    "JOIN sales_transactions st ON st.id = sti.transaction_id " +
                    "WHERE date(st.created_at) BETWEEN ? AND ? " +
                    "GROUP BY sti.category";

            try (Connection conn = AppDatabase.openConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, wStart.format(DateTimeFormatter.ISO_LOCAL_DATE));
                ps.setString(2, wEnd.format(DateTimeFormatter.ISO_LOCAL_DATE));
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String cat = rs.getString("category");
                    if (CATEGORIES.contains(cat))
                        weekMap.put(cat, new int[] {
                                rs.getInt("total_orders"),
                                (int) rs.getDouble("total_revenue")
                        });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            weeks.add(weekMap);
        }
        return weeks;
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

    // ─── Python script builder ────────────────────────────────────────────────

    private static String buildPythonScript(
            Map<String, Map<String, int[]>> weeklyData,
            List<Map<String, int[]>> monthlyData,
            String weeklyPath,
            String monthlyPath,
            int mode) {

        StringBuilder sb = new StringBuilder();

        sb.append("# AUTO-GENERATED — do not edit\n");
        sb.append("import os, sys\n");
        sb.append("try:\n");
        sb.append("    from reportlab.lib import colors\n");
        sb.append("    from reportlab.lib.pagesizes import A4\n");
        sb.append("    from reportlab.lib.styles import ParagraphStyle\n");
        sb.append("    from reportlab.lib.units import cm\n");
        sb.append("    from reportlab.platypus import SimpleDocTemplate, Paragraph, Table, TableStyle\n");
        sb.append("    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT\n");
        sb.append("except ImportError:\n");
        sb.append("    os.system(sys.executable + ' -m pip install reportlab -q')\n");
        sb.append("    from reportlab.lib import colors\n");
        sb.append("    from reportlab.lib.pagesizes import A4\n");
        sb.append("    from reportlab.lib.styles import ParagraphStyle\n");
        sb.append("    from reportlab.lib.units import cm\n");
        sb.append("    from reportlab.platypus import SimpleDocTemplate, Paragraph, Table, TableStyle\n");
        sb.append("    from reportlab.lib.enums import TA_CENTER, TA_LEFT, TA_RIGHT\n\n");

        // Always emit both data structures so the helper functions compile,
        // but only populate the ones we need.
        if ((mode & MODE_WEEKLY) != 0) {
            sb.append("WEEKLY = {\n");
            for (String day : DAYS) {
                sb.append("  '").append(day).append("': {\n");
                Map<String, int[]> catMap = weeklyData.getOrDefault(day, Collections.emptyMap());
                for (String cat : CATEGORIES) {
                    int[] v = catMap.getOrDefault(cat, new int[] { 0, 0 });
                    sb.append("    '").append(cat.replace("'", "\\'"))
                            .append("': [").append(v[0]).append(", ").append(v[1]).append("],\n");
                }
                sb.append("  },\n");
            }
            sb.append("}\n\n");
        }

        if ((mode & MODE_MONTHLY) != 0) {
            sb.append("MONTHLY = [\n");
            for (Map<String, int[]> week : monthlyData) {
                sb.append("  {\n");
                for (String cat : CATEGORIES) {
                    int[] v = week.getOrDefault(cat, new int[] { 0, 0 });
                    sb.append("    '").append(cat.replace("'", "\\'"))
                            .append("': [").append(v[0]).append(", ").append(v[1]).append("],\n");
                }
                sb.append("  },\n");
            }
            sb.append("]\n\n");
        }

        sb.append("CATEGORIES = ").append(pythonList(CATEGORIES)).append("\n");
        sb.append("DAYS       = ").append(pythonList(DAYS)).append("\n\n");

        sb.append("WEEKLY_PATH  = r'").append(weeklyPath.replace("\\", "\\\\")).append("'\n");
        sb.append("MONTHLY_PATH = r'").append(monthlyPath.replace("\\", "\\\\")).append("'\n\n");

        // ── Shared helpers ────────────────────────────────────────
        sb.append("""
                HEADER_BG  = colors.HexColor('#4DA6D8')
                COL_HDR_BG = colors.HexColor('#D9E9F5')
                TOTAL_BG   = colors.HexColor('#F0F0F0')
                WHITE      = colors.white
                DARK       = colors.HexColor('#1A1A1A')
                BORDER     = colors.HexColor('#888888')
                ZEBRA      = colors.HexColor('#F7FBFF')

                def cell(text, bold=False, align='CENTER', size=8, color=None):
                    from reportlab.platypus import Paragraph
                    if color is None: color = DARK
                    amap = {'CENTER': TA_CENTER, 'LEFT': TA_LEFT, 'RIGHT': TA_RIGHT}
                    st = ParagraphStyle('c',
                        fontName='Helvetica-Bold' if bold else 'Helvetica',
                        fontSize=size, textColor=color,
                        alignment=amap.get(align, TA_CENTER), leading=size+3)
                    return Paragraph(str(text), st)

                def peso(v):
                    return f'P{v:,.0f}'

                def title_para(text, size=14, bold=True, space_after=4):
                    from reportlab.platypus import Paragraph
                    return Paragraph(text, ParagraphStyle('t',
                        fontName='Helvetica-Bold' if bold else 'Helvetica',
                        fontSize=size, alignment=TA_CENTER,
                        textColor=DARK, spaceAfter=space_after))

                """);

        // ── Weekly function (only emitted when needed) ─────────────────────
        if ((mode & MODE_WEEKLY) != 0) {
            sb.append("""
                    def build_weekly():
                        from datetime import date, timedelta
                        today = date.today()
                        ws = today - timedelta(days=today.weekday())
                        we = ws + timedelta(days=6)
                        period = f"{ws.strftime('%B %d')} - {we.strftime('%B %d, %Y')}"

                        doc = SimpleDocTemplate(WEEKLY_PATH, pagesize=A4,
                            leftMargin=1.5*cm, rightMargin=1.5*cm,
                            topMargin=1.5*cm,  bottomMargin=1.5*cm)
                        story = []

                        story.append(title_para('Better Mondays Cafe', 16))
                        story.append(title_para('Restaurant Weekly Sales Report', 13))
                        story.append(title_para(period, 10, bold=False, space_after=12))

                        grand_rev    = sum(WEEKLY[d][c][0]*WEEKLY[d][c][1] for d in DAYS for c in CATEGORIES)
                        grand_orders = sum(WEEKLY[d][c][0] for d in DAYS for c in CATEGORIES)

                        col_w = [2.1*cm, 4.0*cm, 2.2*cm, 2.0*cm, 2.5*cm, 2.5*cm]
                        hdr = [cell('Day',bold=True,align='CENTER'),
                               cell('Category',bold=True,align='LEFT'),
                               cell('Number of Orders',bold=True),
                               cell('Price',bold=True),
                               cell('Amount',bold=True),
                               cell('% of Weekly Sales',bold=True)]
                        tdata = [hdr]
                        spans, ts_extra = [], []
                        row = 1

                        for day in DAYS:
                            day_orders = sum(WEEKLY[day][c][0] for c in CATEGORIES)
                            day_rev    = sum(WEEKLY[day][c][0]*WEEKLY[day][c][1] for c in CATEGORIES)
                            pct        = (day_rev/grand_rev*100) if grand_rev else 0
                            day_start  = row

                            for i, cat in enumerate(CATEGORIES):
                                o, p = WEEKLY[day][cat]
                                tdata.append([
                                    cell(day if i==0 else '', bold=(i==0)),
                                    cell(cat, align='LEFT'),
                                    cell(str(o)),
                                    cell(peso(p)),
                                    cell(peso(o*p)),
                                    cell(f'{pct:.0f}%' if i==0 else '', bold=(i==0)),
                                ])
                                row += 1

                            tdata.append([cell(''), cell('Total',bold=True,align='LEFT'),
                                          cell(str(day_orders),bold=True), cell(''),
                                          cell(peso(day_rev),bold=True), cell('')])
                            spans += [('SPAN',(0,day_start),(0,row-1)),
                                      ('SPAN',(5,day_start),(5,row-1))]
                            ts_extra += [
                                ('BACKGROUND',(0,row),(-1,row),TOTAL_BG),
                                ('FONTNAME',(0,row),(-1,row),'Helvetica-Bold'),
                                ('LINEABOVE',(0,row),(-1,row),0.5,BORDER),
                                ('LINEBELOW',(0,row),(-1,row),1,BORDER),
                            ]
                            row += 1

                        tdata.append([cell('Weekly Total',bold=True,color=WHITE),
                                      cell(''),
                                      cell(str(grand_orders),bold=True,color=WHITE),
                                      cell(''),
                                      cell(peso(grand_rev),bold=True,color=WHITE),
                                      cell('100%',bold=True,color=WHITE)])
                        gt_row = row

                        base = [
                            ('FONTNAME',(0,0),(-1,-1),'Helvetica'),
                            ('FONTSIZE',(0,0),(-1,-1),8),
                            ('GRID',(0,0),(-1,-1),0.4,BORDER),
                            ('VALIGN',(0,0),(-1,-1),'MIDDLE'),
                            ('TOPPADDING',(0,0),(-1,-1),3),
                            ('BOTTOMPADDING',(0,0),(-1,-1),3),
                            ('LEFTPADDING',(0,0),(-1,-1),4),
                            ('BACKGROUND',(0,0),(-1,0),COL_HDR_BG),
                            ('FONTNAME',(0,0),(-1,0),'Helvetica-Bold'),
                            ('FONTSIZE',(0,0),(-1,0),9),
                            ('ROWBACKGROUNDS',(0,1),(-1,gt_row-1),[WHITE, ZEBRA]),
                            ('BACKGROUND',(0,gt_row),(-1,gt_row),HEADER_BG),
                            ('FONTNAME',(0,gt_row),(-1,gt_row),'Helvetica-Bold'),
                            ('LINEABOVE',(0,gt_row),(-1,gt_row),1,BORDER),
                            ('SPAN',(0,gt_row),(1,gt_row)),
                        ]
                        t = Table(tdata, colWidths=col_w, repeatRows=1)
                        t.setStyle(TableStyle(base + ts_extra + spans))
                        story.append(t)
                        doc.build(story)
                        print(f'[OK] Weekly  -> {WEEKLY_PATH}')

                    """);
        }

        // ── Monthly function (only emitted when needed) ────────────────────
        if ((mode & MODE_MONTHLY) != 0) {
            sb.append("""
                    def build_monthly():
                        from datetime import date
                        today = date.today()
                        month_name = today.strftime('%B %Y')

                        doc = SimpleDocTemplate(MONTHLY_PATH, pagesize=A4,
                            leftMargin=1.5*cm, rightMargin=1.5*cm,
                            topMargin=1.5*cm,  bottomMargin=1.5*cm)
                        story = []

                        story.append(title_para('Better Mondays Cafe', 16))
                        story.append(title_para('Restaurant Monthly Sales Report', 13))
                        story.append(title_para(month_name, 10, bold=False, space_after=12))

                        n = len(MONTHLY)
                        col_w = [3.5*cm] + [1.5*cm, 2.0*cm]*n + [2.2*cm, 2.0*cm]

                        h1 = [cell('Category',bold=True,align='LEFT')]
                        for w in range(1, n+1):
                            h1 += [cell(f'Week {w}',bold=True), cell('')]
                        h1 += [cell('Monthly Total',bold=True), cell('Most Sold Week',bold=True)]

                        h2 = [cell('')]
                        for _ in range(n):
                            h2 += [cell('Orders',bold=True), cell('Amount',bold=True)]
                        h2 += [cell('Amount',bold=True), cell('')]

                        tdata  = [h1, h2]
                        spans  = [('SPAN',(1+w*2,0),(2+w*2,0)) for w in range(n)]
                        row    = 2
                        grand  = 0

                        for cat in CATEGORIES:
                            rev_per_week = [MONTHLY[w][cat][1] for w in range(n)]
                            ord_per_week = [MONTHLY[w][cat][0] for w in range(n)]
                            monthly_tot  = sum(rev_per_week)
                            grand       += monthly_tot
                            best_wk      = ord_per_week.index(max(ord_per_week)) + 1 if any(ord_per_week) else 1
                            r = [cell(cat, align='LEFT')]
                            for w in range(n):
                                r += [cell(str(MONTHLY[w][cat][0])), cell(peso(MONTHLY[w][cat][1]))]
                            r += [cell(peso(monthly_tot),bold=True),
                                  cell(f'Week {best_wk}',bold=True)]
                            tdata.append(r)
                            row += 1

                        gr = [cell('Monthly Grand Total',bold=True,color=WHITE)]
                        for w in range(n):
                            wt_o = sum(MONTHLY[w][c][0] for c in CATEGORIES)
                            wt_r = sum(MONTHLY[w][c][1] for c in CATEGORIES)
                            gr += [cell(str(wt_o),bold=True,color=WHITE),
                                   cell(peso(wt_r),bold=True,color=WHITE)]
                        gr += [cell(peso(grand),bold=True,color=WHITE), cell('')]
                        tdata.append(gr)
                        gt_row = row

                        ts = TableStyle([
                            ('FONTNAME',(0,0),(-1,-1),'Helvetica'),
                            ('FONTSIZE',(0,0),(-1,-1),8),
                            ('GRID',(0,0),(-1,-1),0.4,BORDER),
                            ('VALIGN',(0,0),(-1,-1),'MIDDLE'),
                            ('TOPPADDING',(0,0),(-1,-1),3),
                            ('BOTTOMPADDING',(0,0),(-1,-1),3),
                            ('LEFTPADDING',(0,0),(-1,-1),4),
                            ('BACKGROUND',(0,0),(-1,1),COL_HDR_BG),
                            ('FONTNAME',(0,0),(-1,1),'Helvetica-Bold'),
                            ('LINEBELOW',(0,1),(-1,1),1,BORDER),
                            ('ROWBACKGROUNDS',(0,2),(-1,gt_row-1),[WHITE, ZEBRA]),
                            ('BACKGROUND',(0,gt_row),(-1,gt_row),HEADER_BG),
                            ('FONTNAME',(0,gt_row),(-1,gt_row),'Helvetica-Bold'),
                            ('LINEABOVE',(0,gt_row),(-1,gt_row),1,BORDER),
                        ] + spans)

                        t = Table(tdata, colWidths=col_w, repeatRows=2)
                        t.setStyle(ts)
                        story.append(t)
                        doc.build(story)
                        print(f'[OK] Monthly -> {MONTHLY_PATH}')

                    """);
        }

        // ── Call only what was requested ──────────────────────────────────
        if ((mode & MODE_WEEKLY) != 0)
            sb.append("build_weekly()\n");
        if ((mode & MODE_MONTHLY) != 0)
            sb.append("build_monthly()\n");

        return sb.toString();
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private static String pythonList(List<String> items) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0)
                sb.append(", ");
            sb.append("'").append(items.get(i).replace("'", "\\'")).append("'");
        }
        sb.append("]");
        return sb.toString();
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