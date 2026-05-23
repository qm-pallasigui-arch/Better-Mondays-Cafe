package util;

import java.awt.Font;
import java.awt.GraphicsEnvironment;
import javax.swing.JLabel;

/**
 * Helper to ensure a JLabel's font can display specified glyphs; if not,
 * finds and applies a fallback font present on the system that can.
 */
public final class FontHelper {

    private static final String[] COMMON_FALLBACKS = new String[] {
            "Segoe UI Symbol", "Segoe UI Emoji", "Segoe UI Historic", "Arial Unicode MS", "Symbola", "Noto Sans Symbols"
    };

    private FontHelper() {}

    public static void ensureGlyphs(JLabel label, char... glyphs) {
        if (label == null || glyphs == null || glyphs.length == 0) return;
        Font f = label.getFont();
        boolean all = true;
        for (char c : glyphs) {
            if (!f.canDisplay(c)) { all = false; break; }
        }
        if (all) return;

        Font fb = findFallbackFont(f.getStyle(), f.getSize(), glyphs);
        if (fb != null) {
            label.setFont(fb);
        }
    }

    private static Font findFallbackFont(int style, int size, char[] glyphs) {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] available = ge.getAvailableFontFamilyNames();

        // Try common fallbacks first
        for (String cand : COMMON_FALLBACKS) {
            for (String avail : available) {
                if (avail.equalsIgnoreCase(cand)) {
                    Font f = new Font(avail, style, size);
                    if (canDisplayAll(f, glyphs)) return f;
                }
            }
        }

        // Fallback: scan all available fonts
        for (String avail : available) {
            Font f = new Font(avail, style, size);
            if (canDisplayAll(f, glyphs)) return f;
        }
        return null;
    }

    private static boolean canDisplayAll(Font f, char[] glyphs) {
        for (char c : glyphs) if (!f.canDisplay(c)) return false;
        return true;
    }
}
