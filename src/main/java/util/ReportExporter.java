package util;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import persistence.AppDatabase;

public final class ReportExporter {
    private ReportExporter() {}

    public static Path exportSalesCsv(Path outPath) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add("product_name,quantity,price,total,sold_at");
        try (Connection c = AppDatabase.openConnection();
             PreparedStatement ps = c.prepareStatement("SELECT product_name, quantity, price, total, sold_at FROM sales_records ORDER BY sold_at DESC");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String row = String.format("%s,%d,%.2f,%.2f,%s",
                        rs.getString("product_name"), rs.getInt("quantity"), rs.getDouble("price"), rs.getDouble("total"), rs.getString("sold_at"));
                lines.add(row);
            }
        }
        Path p = outPath;
        try (BufferedWriter w = Files.newBufferedWriter(p)) {
            for (String l : lines) w.write(l + System.lineSeparator());
        }
        return p;
    }
}
