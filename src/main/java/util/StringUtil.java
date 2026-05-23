package util;

public final class StringUtil {

    private StringUtil() {}

    /**
     * Normalizes a name to Title Case with single spaces, e.g. "  lid " -> "Lid".
     */
    public static String normalizeName(String s) {
        if (s == null) return "";
        String t = s.trim().toLowerCase();
        if (t.isEmpty()) return "";
        String[] parts = t.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i];
            if (p.isEmpty()) continue;
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
            if (i < parts.length - 1) sb.append(' ');
        }
        return sb.toString();
    }
}
