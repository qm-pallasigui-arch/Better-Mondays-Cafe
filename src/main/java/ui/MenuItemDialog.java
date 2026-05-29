package ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import pos.Menu;
import pos.MenuItem;

/** Modal dialog for creating/editing menu items with category-specific ingredient editors. */
public class MenuItemDialog extends JDialog {
    private boolean confirmed = false;
    private final JTextField nameField = new JTextField(32);
    private final JComboBox<String> category = new JComboBox<>(new String[]{"Coffee", "Non-Coffee", "Fruit Tea", "Herbal Tea", "Food"});
    private final JTextField hotField = new JTextField(12);
    private final JTextField icedRegField = new JTextField(12);
    private final JTextField icedLargeField = new JTextField(12);

    private final CategoryIngredientEditor ingredientEditor = new CategoryIngredientEditor();

    public MenuItemDialog(java.awt.Window owner, MenuItem existing) {
        super(owner, "Menu Item", ModalityType.APPLICATION_MODAL);
        build(existing);
    }

    private void build(MenuItem existing) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JPanel form = new JPanel();
        form.setOpaque(false);
        form.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Dimension fieldSize = new Dimension(280, 34);
        nameField.setPreferredSize(fieldSize);
        hotField.setPreferredSize(fieldSize);
        icedRegField.setPreferredSize(fieldSize);
        icedLargeField.setPreferredSize(fieldSize);
        category.setPreferredSize(fieldSize);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0; form.add(new JLabel("Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.weightx = 0; form.add(new JLabel("Category"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(category, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.weightx = 0; form.add(new JLabel("Hot price"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(hotField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.weightx = 0; form.add(new JLabel("Iced regular"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(icedRegField, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.weightx = 0; form.add(new JLabel("Iced large"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; form.add(icedLargeField, gbc);

        JLabel helper = new JLabel("Use decimal prices, e.g., 95.0");
        helper.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 11));
        gbc.gridx = 1; gbc.gridy++; gbc.weightx = 1; form.add(helper, gbc);

        gbc.gridx = 0; gbc.gridy++; gbc.gridwidth = 2; form.add(new JLabel("Ingredients"), gbc);
        gbc.gridy++; gbc.fill = GridBagConstraints.BOTH; gbc.weightx = 1; gbc.weighty = 1;
        JScrollPane sp = new JScrollPane(ingredientEditor.getComponent());
        sp.setPreferredSize(new Dimension(420, 190));
        sp.setBorder(AppTheme.inputBorderRegular());
        form.add(sp, gbc);

        p.add(form, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        JButton ok = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        ok.setPreferredSize(new Dimension(104, 34));
        cancel.setPreferredSize(new Dimension(104, 34));
        ok.putClientProperty("appTheme.variant", "primary");
        ok.addActionListener(e -> { if (validateAndSave()) { confirmed = true; setVisible(false); } });
        cancel.addActionListener(e -> { confirmed = false; setVisible(false); });
        btns.add(ok); btns.add(cancel);
        p.add(btns, BorderLayout.SOUTH);

        category.addActionListener(e -> ingredientEditor.useTemplateFor(getCategory(), ingredientEditor.getMap()));

        if (existing != null) {
            nameField.setText(existing.getName());
            category.setSelectedItem(existing.getCategory());
            hotField.setText(String.valueOf(existing.getHotPrice()));
            icedRegField.setText(String.valueOf(existing.getIcedRegularPrice()));
            icedLargeField.setText(String.valueOf(existing.getIcedLargePrice()));
            ingredientEditor.useTemplateFor(String.valueOf(category.getSelectedItem()), existing.getIngredients());
        } else {
            ingredientEditor.useTemplateFor(String.valueOf(category.getSelectedItem()), new HashMap<>());
        }

        getContentPane().add(p);
        pack();
        setMinimumSize(new Dimension(560, 520));
        AppTheme.applyToComponent(p);
    }

    private boolean validateAndSave() {
        String n = nameField.getText();
        if (n == null || n.trim().isEmpty()) {
            javax.swing.JOptionPane.showMessageDialog(this, "Name is required");
            return false;
        }
        try {
            Double.parseDouble(hotField.getText().trim().isEmpty() ? "0" : hotField.getText().trim());
            Double.parseDouble(icedRegField.getText().trim().isEmpty() ? "0" : icedRegField.getText().trim());
            Double.parseDouble(icedLargeField.getText().trim().isEmpty() ? "0" : icedLargeField.getText().trim());
        } catch (NumberFormatException ex) {
            javax.swing.JOptionPane.showMessageDialog(this, "Prices must be numeric");
            return false;
        }
        return true;
    }

    public boolean isConfirmed() { return confirmed; }
    public String getName() { return nameField.getText().trim(); }
    public String getCategory() { return String.valueOf(category.getSelectedItem()); }
    public double getHot() { try { return Double.parseDouble(hotField.getText().trim().isEmpty() ? "0" : hotField.getText().trim()); } catch (Exception e) { return 0; } }
    public double getIcedRegular() { try { return Double.parseDouble(icedRegField.getText().trim().isEmpty() ? "0" : icedRegField.getText().trim()); } catch (Exception e) { return 0; } }
    public double getIcedLarge() { try { return Double.parseDouble(icedLargeField.getText().trim().isEmpty() ? "0" : icedLargeField.getText().trim()); } catch (Exception e) { return 0; } }
    public String getIngredientsCsv() { return mapToCsv(getIngredientsMap()); }
    public Map<String, Double> getIngredientsMap() {
        return ingredientEditor.getMap();
    }

    private static String mapToCsv(Map<String, Double> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> {
            if (sb.length() > 0) sb.append(",");
            sb.append(k).append(":").append(v);
        });
        return sb.toString();
    }

    // --- Category-specific ingredient editor ---
    private static final class CategoryIngredientEditor {
        private final JPanel root = new JPanel(new BorderLayout());
        private final JPanel addPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        private final JPanel list = new JPanel(new GridBagLayout());
        private final LinkedHashMap<String, JTextField> inputs = new LinkedHashMap<>();
        private final JComboBox<String> existingIngredientCombo = new JComboBox<>();
        private final JTextField newIngredientField = new JTextField(14);
        private String currentCategory = "Coffee";
        private static final String EXISTING_PLACEHOLDER = "Select existing ingredient";

        private static final Map<String, List<String>> TEMPLATES = new LinkedHashMap<>();
        static {
            TEMPLATES.put("Coffee", List.of("Coffee Beans", "Milk", "Water", "Sweetener"));
            TEMPLATES.put("Non-Coffee", List.of("Base Powder", "Milk", "Sweetener", "Topping"));
            TEMPLATES.put("Fruit Tea", List.of("Tea Base", "Fruit Syrup", "Fruit Bits", "Sweetener"));
            TEMPLATES.put("Herbal Tea", List.of("Tea Leaves", "Honey", "Lemon", "Water"));
            TEMPLATES.put("Food", List.of("Main Ingredient", "Sauce", "Cheese", "Garnish"));
        }

        CategoryIngredientEditor() {
            root.setOpaque(false);
            addPanel.setOpaque(false);
            list.setOpaque(false);
            JButton addButton = new JButton("Add Ingredient Field");
            addButton.addActionListener(e -> addRequestedIngredientField());

            existingIngredientCombo.setPreferredSize(new Dimension(190, 30));
            newIngredientField.setPreferredSize(new Dimension(170, 30));
            newIngredientField.setToolTipText("Or type a new ingredient");

            addPanel.add(existingIngredientCombo);
            addPanel.add(new JLabel("or"));
            addPanel.add(newIngredientField);
            addPanel.add(addButton);

            root.add(addPanel, BorderLayout.NORTH);
            root.add(list, BorderLayout.CENTER);
        }

        java.awt.Component getComponent() { return root; }

        void useTemplateFor(String category, Map<String, Double> existing) {
            currentCategory = category == null || category.isBlank() ? "Coffee" : category;
            Map<String, Double> source = existing == null ? new HashMap<>() : new HashMap<>(existing);
            List<String> fields = new ArrayList<>(TEMPLATES.getOrDefault(currentCategory, List.of("Ingredient")));
            for (String key : source.keySet()) {
                if (!fields.contains(key)) fields.add(key);
            }

            inputs.clear();
            list.removeAll();

            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(5, 2, 5, 2);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int row = 0;
            for (String label : fields) {
                gbc.gridx = 0; gbc.gridy = row; gbc.weightx = 0;
                JLabel l = new JLabel(label);
                l.setFont(new java.awt.Font("Segoe UI", java.awt.Font.PLAIN, 12));
                list.add(l, gbc);

                gbc.gridx = 1; gbc.weightx = 1;
                JTextField tf = new JTextField(14);
                tf.setPreferredSize(new Dimension(170, 30));
                Double existingValue = source.get(label);
                if (existingValue != null) tf.setText(String.valueOf(existingValue));
                list.add(tf, gbc);
                inputs.put(label, tf);
                row++;
            }

            list.revalidate();
            list.repaint();
            refreshIngredientChoices(fields);
        }

        private void refreshIngredientChoices(List<String> currentFields) {
            LinkedHashSet<String> options = new LinkedHashSet<>();
            options.addAll(TEMPLATES.getOrDefault(currentCategory, List.of()));
            options.addAll(getExistingIngredientsForCategory(currentCategory));
            options.addAll(currentFields);
            options.removeIf(v -> v == null || v.isBlank());

            List<String> ordered = new ArrayList<>(options);
            Collections.sort(ordered);

            existingIngredientCombo.removeAllItems();
            existingIngredientCombo.addItem(EXISTING_PLACEHOLDER);
            for (String option : ordered) {
                if (!inputs.containsKey(option)) {
                    existingIngredientCombo.addItem(option);
                }
            }
            existingIngredientCombo.setSelectedItem(EXISTING_PLACEHOLDER);
        }

        private List<String> getExistingIngredientsForCategory(String category) {
            LinkedHashSet<String> ingredientNames = new LinkedHashSet<>();
            for (MenuItem item : Menu.getInstance().getAllItems().values()) {
                if (isSameCategory(item.getCategory(), category)) {
                    ingredientNames.addAll(item.getIngredients().keySet());
                }
            }
            return new ArrayList<>(ingredientNames);
        }

        private boolean isSameCategory(String a, String b) {
            if (a == null || b == null) return false;
            return normalizeCategoryKey(a).equals(normalizeCategoryKey(b));
        }

        private String normalizeCategoryKey(String category) {
            return category.trim().toLowerCase().replace("-", "").replace(" ", "");
        }

        private void addRequestedIngredientField() {
            String custom = newIngredientField.getText() == null ? "" : newIngredientField.getText().trim();
            String selected = String.valueOf(existingIngredientCombo.getSelectedItem());

            String ingredient = !custom.isEmpty() ? custom :
                    (selected == null || EXISTING_PLACEHOLDER.equals(selected) ? "" : selected.trim());

            if (ingredient.isEmpty()) {
                javax.swing.JOptionPane.showMessageDialog(root, "Type a new ingredient or pick an existing one.");
                return;
            }

            if (inputs.containsKey(ingredient)) {
                inputs.get(ingredient).requestFocusInWindow();
                return;
            }

            Map<String, Double> existing = getMap();
            existing.put(ingredient, 0.0);
            useTemplateFor(currentCategory, existing);
            newIngredientField.setText("");
            JTextField created = inputs.get(ingredient);
            if (created != null) {
                created.requestFocusInWindow();
            }
        }

        Map<String, Double> getMap() {
            Map<String, Double> out = new LinkedHashMap<>();
            for (Map.Entry<String, JTextField> e : inputs.entrySet()) {
                String value = e.getValue().getText() == null ? "" : e.getValue().getText().trim();
                if (value.isEmpty()) continue;
                try {
                    out.put(e.getKey(), Double.parseDouble(value));
                } catch (Exception ignored) {
                }
            }
            return out;
        }
    }
}
