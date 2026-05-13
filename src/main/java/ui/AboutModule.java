package ui;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class AboutModule extends JPanel {

    public AboutModule() {
        super(new BorderLayout(8, 8));
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setText("Better Mondays Coffee Cafe Management System\n\nVersion: 1.0\n\nCompany Info:\nA Swing-based POS and cafe management app for ordering, inventory, monitoring, and reporting.\n\nBuilt for local SQLite now, Firebase-ready later.");

        JButton sys = new JButton("System Info");
        sys.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "OS: " + System.getProperty("os.name") + "\nJava: " + System.getProperty("java.version") + "\nUser: " + System.getProperty("user.name"),
                "System Info", JOptionPane.INFORMATION_MESSAGE));

        JPanel top = new JPanel(new GridLayout(1, 1));
        top.add(new JLabel("About"));
        add(top, BorderLayout.NORTH);
        add(area, BorderLayout.CENTER);
        add(sys, BorderLayout.SOUTH);
    }
}
