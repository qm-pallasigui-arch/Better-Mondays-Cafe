package util;

import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.swing.BorderFactory;
import ui.RoundedLineBorder;
import ui.AppTheme;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;

public final class FieldAssist {

    private FieldAssist() {}

    public static void installAutocomplete(JTextField field, Supplier<List<String>> suggestionsSupplier) {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(suggestionsSupplier, "suggestionsSupplier");

        DefaultListModel<String> model = new DefaultListModel<>();
        JList<String> list = new JList<>(model);
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPopupMenu popup = new JPopupMenu();
        popup.setBorder(new RoundedLineBorder(new java.awt.Color(0x2E4060), AppTheme.BORDER_THICKNESS, AppTheme.BORDER_RADIUS));
        list.setVisibleRowCount(6);
        list.setFixedCellWidth(Math.max(220, field.getPreferredSize().width));
        popup.add(new JScrollPane(list));
        popup.setFocusable(false);
        final boolean[] committingSelection = { false };

        Runnable commitSelection = () -> {
            String value = list.getSelectedValue();
            if (value != null) {
                committingSelection[0] = true;
                field.setText(value);
                field.setCaretPosition(value.length());
                SwingUtilities.invokeLater(() -> committingSelection[0] = false);
            }
            popup.setVisible(false);
            field.requestFocusInWindow();
        };

        Runnable closePopup = () -> {
            popup.setVisible(false);
            field.requestFocusInWindow();
        };

        list.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int index = list.locationToIndex(e.getPoint());
                if (index >= 0 && index < model.size()) {
                    list.setSelectedIndex(index);
                    commitSelection.run();
                } else {
                    closePopup.run();
                }
            }
        });

        list.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    commitSelection.run();
                    e.consume();
                }
            }
        });

        field.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { update(); }
            @Override public void removeUpdate(DocumentEvent e) { update(); }
            @Override public void changedUpdate(DocumentEvent e) { update(); }

            private void update() {
                SwingUtilities.invokeLater(() -> {
                    if (committingSelection[0]) {
                        popup.setVisible(false);
                        return;
                    }
                    String raw = field.getText() == null ? "" : field.getText().trim();
                    String typed = raw.toLowerCase();
                    List<String> matches = suggestionsSupplier.get().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .filter(s -> !s.isBlank())
                            .filter(s -> s.toLowerCase().contains(typed))
                            .distinct()
                            .limit(8)
                            .collect(Collectors.toList());

                    model.clear();
                    matches.forEach(model::addElement);

                    if (matches.isEmpty() || raw.isBlank()) {
                        popup.setVisible(false);
                        return;
                    }

                    if (popup.isVisible()) {
                        popup.setVisible(false);
                    }
                    popup.show(field, 0, field.getHeight());
                    if (!matches.isEmpty()) {
                        list.setSelectedIndex(0);
                    }
                });
            }
        });

        popup.addPopupMenuListener(new PopupMenuListener() {
            @Override public void popupMenuWillBecomeVisible(PopupMenuEvent e) {}
            @Override public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {}
            @Override public void popupMenuCanceled(PopupMenuEvent e) {}
        });

        field.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                popup.setVisible(false);
            }
        });
    }

    public static void installIsoDateAutoFormat(JTextField field) {
        AbstractDocument doc = (AbstractDocument) field.getDocument();
        doc.setDocumentFilter(new DocumentFilter() {
            @Override
            public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
                replace(fb, offset, 0, string, attr);
            }

            @Override
            public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String raw = new StringBuilder(current).replace(offset, offset + length, text == null ? "" : text).toString();
                String digits = raw.replaceAll("[^0-9]", "");
                if (digits.length() > 8) digits = digits.substring(0, 8);
                fb.replace(0, fb.getDocument().getLength(), formatIsoDigits(digits), attrs);
            }

            @Override
            public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
                String current = fb.getDocument().getText(0, fb.getDocument().getLength());
                String raw = new StringBuilder(current).delete(offset, offset + length).toString();
                String digits = raw.replaceAll("[^0-9]", "");
                if (digits.length() > 8) digits = digits.substring(0, 8);
                fb.replace(0, fb.getDocument().getLength(), formatIsoDigits(digits), null);
            }

            private String formatIsoDigits(String digits) {
                if (digits.isEmpty()) return "";
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < digits.length(); i++) {
                    if (i == 4 || i == 6) sb.append('-');
                    sb.append(digits.charAt(i));
                }
                return sb.toString();
            }
        });
    }
}
