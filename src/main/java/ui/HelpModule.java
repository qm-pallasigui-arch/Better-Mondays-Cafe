package ui;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import javax.swing.JTabbedPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class HelpModule extends JPanel {

    public HelpModule() {
        super(new BorderLayout());
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Quick Start", pane(load("/help/quick-start.txt")));
        tabs.addTab("Ordering", pane(load("/help/ordering.txt")));
        tabs.addTab("Inventory", pane(load("/help/inventory.txt")));
        tabs.addTab("Reports", pane(load("/help/reports.txt")));
        tabs.addTab("FAQ", pane(load("/help/faq.txt")));
        add(tabs, BorderLayout.CENTER);
        AppTheme.applyToComponent(this);
    }

    private String load(String resource) {
        try (InputStream in = getClass().getResourceAsStream(resource)) {
            if (in == null) return "Help text not available.";
            try (BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (Exception e) {
            return "Help text not available: " + e.getMessage();
        }
    }

    private JScrollPane pane(String text) {
        JTextArea a = new JTextArea(text);
        a.setEditable(false);
        a.setLineWrap(true);
        a.setWrapStyleWord(true);
        return new JScrollPane(a);
    }
}
