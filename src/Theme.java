import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/** Colors, fonts, image helpers and reusable styled components for the whole kiosk. */
public final class Theme {
    public static final Color PRIMARY = new Color(0x542630);   // Primary Burgundy
    public static final Color SECONDARY = new Color(0x70404A); // Secondary Burgundy
    public static final Color ESPRESSO = new Color(0x49332D);  // Dark Espresso
    public static final Color MOCHA = new Color(0x806052);     // Mocha Brown
    public static final Color LATTE = new Color(0xD9C3AD);     // Latte Beige
    public static final Color CREAM = new Color(0xF3E9DD);     // Cream Background

    private static final Map<String, BufferedImage> RAW = new HashMap<>();
    private static final Map<String, ImageIcon> ICONS = new HashMap<>();

    private Theme() {}

    public static void init() {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); } catch (Exception ignored) {}
        UIManager.put("Panel.background", CREAM);
        UIManager.put("Button.disabledText", MOCHA);
        UIManager.put("ToggleButton.disabledText", MOCHA);
        UIManager.put("ScrollBar.thumb", LATTE);
        UIManager.put("ScrollBar.thumbHighlight", LATTE);
        UIManager.put("ScrollBar.thumbShadow", LATTE);
        UIManager.put("ScrollBar.track", CREAM);
        UIManager.put("ToolTip.background", CREAM);
    }

    public static Font font(int style, int size) { return new Font("Segoe UI", style, size); }

    public static String peso(double v) {
        if (Math.abs(v - Math.rint(v)) < 0.005) return "\u20B1" + String.format("%,.0f", v);
        return "\u20B1" + String.format("%,.2f", v);
    }

    // ------------------------------------------------------------------ images
    public static BufferedImage loadImage(String name) {
        if (name == null || name.isEmpty()) return null;
        if (RAW.containsKey(name)) return RAW.get(name);
        BufferedImage img = null;
        try {
            File abs = new File(name);
            if (abs.isAbsolute() && abs.exists()) {
                img = ImageIO.read(abs);
            } else {
                for (String base : new String[]{"images/", "src/images/", "../images/"}) {
                    File f = new File(base + name);
                    if (f.exists()) { img = ImageIO.read(f); break; }
                }
                if (img == null) {
                    URL u = Theme.class.getResource("/images/" + name);
                    if (u != null) img = ImageIO.read(u);
                }
            }
        } catch (Exception ignored) {}
        RAW.put(name, img);
        return img;
    }

    /** Fits the whole image inside a size x size square (no cropping, no stretching) on a cream background. */
    public static ImageIcon icon(String name, int size) {
        String key = name + "@" + size;
        ImageIcon cached = ICONS.get(key);
        if (cached != null) return cached;
        BufferedImage src = loadImage(name);
        if (src == null) src = loadImage("logo_main.png"); // placeholder when an item has no image
        ImageIcon ic = new ImageIcon(fit(src, size, size, CREAM));
        ICONS.put(key, ic);
        return ic;
    }

    public static BufferedImage fit(BufferedImage src, int w, int h, Color bg) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(bg);
        g.fillRect(0, 0, w, h);
        if (src != null) {
            double s = Math.min((double) w / src.getWidth(), (double) h / src.getHeight());
            int nw = Math.max(1, (int) Math.round(src.getWidth() * s));
            int nh = Math.max(1, (int) Math.round(src.getHeight() * s));
            g.drawImage(smooth(src, nw, nh), (w - nw) / 2, (h - nh) / 2, null);
        }
        g.dispose();
        return out;
    }

    private static BufferedImage smooth(BufferedImage src, int w, int h) {
        BufferedImage cur = src;
        int cw = cur.getWidth(), ch = cur.getHeight();
        while (cw / 2 >= w && ch / 2 >= h) { cw /= 2; ch /= 2; cur = draw(cur, cw, ch); }
        return draw(cur, w, h);
    }

    private static BufferedImage draw(BufferedImage s, int w, int h) {
        BufferedImage o = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = o.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(s, 0, 0, w, h, null);
        g.dispose();
        return o;
    }

    // ------------------------------------------------------------ components
    public static class Btn extends JButton {
        private final Color fill, hover;
        private final boolean outline;

        public Btn(String text) { this(text, PRIMARY, SECONDARY, Color.WHITE, false); }

        public Btn(String text, Color fill, Color hover, Color fg, boolean outline) {
            super(text);
            this.fill = fill; this.hover = hover; this.outline = outline;
            setForeground(fg);
            setFont(font(Font.BOLD, 16));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(10, 22, 10, 22));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public static Btn outline(String text) { return new Btn(text, CREAM, LATTE, PRIMARY, true); }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color c = !isEnabled() ? LATTE : (getModel().isRollover() || getModel().isPressed()) ? hover : fill;
            g.setColor(c);
            g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            if (outline) {
                g.setColor(isEnabled() ? PRIMARY : MOCHA);
                g.setStroke(new BasicStroke(2f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    /** Toggle "chip" used for categories, payment methods and customization options. */
    public static class Chip extends JToggleButton {
        public Chip(String text) {
            super(text);
            setFont(font(Font.BOLD, 15));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(8, 18, 8, 18));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setForeground(ESPRESSO);
            addItemListener(e -> setForeground(isSelected() ? Color.WHITE : ESPRESSO));
        }

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (isSelected()) {
                g.setColor(PRIMARY);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
            } else {
                g.setColor(getModel().isRollover() && isEnabled() ? LATTE : CREAM);
                g.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 22, 22);
                g.setColor(isEnabled() ? MOCHA : LATTE);
                g.setStroke(new BasicStroke(1.5f));
                g.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 22, 22);
            }
            g.dispose();
            super.paintComponent(g0);
        }
    }

    public static JLabel label(String text, int style, int size, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font(style, size));
        l.setForeground(color);
        return l;
    }

    public static JTextField field(int cols) {
        JTextField f = new JTextField(cols);
        styleField(f);
        return f;
    }

    public static JPasswordField passwordField(int cols) {
        JPasswordField f = new JPasswordField(cols);
        styleField(f);
        return f;
    }

    private static void styleField(JTextField f) {
        f.setFont(font(Font.PLAIN, 16));
        f.setForeground(ESPRESSO);
        f.setBackground(Color.WHITE);
        f.setCaretColor(PRIMARY);
        f.setBorder(new CompoundBorder(new LineBorder(LATTE, 2, true), new EmptyBorder(7, 10, 7, 10)));
    }

    public static JPanel header(String subtitle, JComponent... right) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(CREAM);
        p.setBorder(new CompoundBorder(new MatteBorder(0, 0, 3, 0, PRIMARY), new EmptyBorder(8, 20, 8, 20)));
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        left.setOpaque(false);
        left.add(new JLabel(icon("logo_main.png", 64)));
        JPanel text = new JPanel(new GridLayout(2, 1));
        text.setOpaque(false);
        text.add(label("SipSync Cafe", Font.BOLD, 24, PRIMARY));
        text.add(label(subtitle, Font.PLAIN, 14, MOCHA));
        left.add(text);
        JPanel r = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        r.setOpaque(false);
        for (JComponent c : right) r.add(c);
        p.add(left, BorderLayout.WEST);
        p.add(r, BorderLayout.EAST);
        return p;
    }

    public static JScrollPane scroll(Component c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(LATTE, 2));
        sp.getViewport().setBackground(CREAM);
        sp.getVerticalScrollBar().setUnitIncrement(18);
        return sp;
    }

    public static void styleTable(JTable t) {
        t.setRowHeight(40);
        t.setFont(font(Font.PLAIN, 15));
        t.setForeground(ESPRESSO);
        t.setBackground(CREAM);
        t.setGridColor(LATTE);
        t.setSelectionBackground(LATTE);
        t.setSelectionForeground(ESPRESSO);
        t.setFillsViewportHeight(true);
        t.setShowVerticalLines(false);
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(100, 42));
        t.getTableHeader().setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, false, false, r, c);
                l.setBackground(PRIMARY);
                l.setForeground(Color.WHITE);
                l.setFont(font(Font.BOLD, 15));
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
        DefaultTableCellRenderer cell = new DefaultTableCellRenderer();
        cell.setBorder(new EmptyBorder(0, 10, 0, 10));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable tb, Object v, boolean s, boolean f, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(tb, v, s, false, r, c);
                l.setBorder(new EmptyBorder(0, 10, 0, 10));
                return l;
            }
        });
    }

    public static void prep(JFrame f, String title) {
        f.setTitle(title);
        f.setIconImage(loadImage("logo_main.png"));
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setMinimumSize(new Dimension(1000, 640));
        f.setSize(1280, 800);
        f.setLocationRelativeTo(null);
        f.setExtendedState(JFrame.MAXIMIZED_BOTH);
        f.getContentPane().setBackground(CREAM);
    }

    // ----------------------------------------------------------------- dialogs
    public static JDialog baseDialog(Component parent, String title) {
        Window w = parent == null ? null : SwingUtilities.getWindowAncestor(parent);
        if (w == null && parent instanceof Window) w = (Window) parent;
        JDialog d = new JDialog(w, title, Dialog.ModalityType.APPLICATION_MODAL);
        d.setIconImage(loadImage("logo_main.png"));
        d.getContentPane().setBackground(CREAM);
        return d;
    }

    public static void info(Component parent, String title, String msg) {
        confirm(parent, title, msg, "OK", null);
    }

    /** Styled message / confirmation dialog. Pass null for "no" to show a single OK button. */
    public static boolean confirm(Component parent, String title, String msg, String yes, String no) {
        JDialog d = baseDialog(parent, title);
        boolean[] result = {false};
        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setBackground(CREAM);
        p.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 3), new EmptyBorder(24, 28, 20, 28)));
        p.add(label(title, Font.BOLD, 22, PRIMARY), BorderLayout.NORTH);
        JLabel m = label("<html><div style='width:340px'>" + msg + "</div></html>", Font.PLAIN, 16, ESPRESSO);
        p.add(m, BorderLayout.CENTER);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        b.setOpaque(false);
        if (no != null) {
            Btn n = Btn.outline(no);
            n.addActionListener(e -> d.dispose());
            b.add(n);
        }
        Btn y = new Btn(yes);
        y.addActionListener(e -> { result[0] = true; d.dispose(); });
        b.add(y);
        p.add(b, BorderLayout.SOUTH);
        d.setUndecorated(true);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return result[0];
    }

    /** Single-field prompt. Returns null if cancelled. */
    public static String prompt(Component parent, String title, String label, String initial) {
        JDialog d = baseDialog(parent, title);
        String[] result = {null};
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setBackground(CREAM);
        p.setBorder(new CompoundBorder(new LineBorder(PRIMARY, 3), new EmptyBorder(24, 28, 20, 28)));
        p.add(label(title, Font.BOLD, 22, PRIMARY), BorderLayout.NORTH);
        JPanel mid = new JPanel(new GridLayout(2, 1, 0, 6));
        mid.setOpaque(false);
        mid.add(label(label, Font.PLAIN, 16, ESPRESSO));
        JTextField f = field(18);
        f.setText(initial == null ? "" : initial);
        mid.add(f);
        p.add(mid, BorderLayout.CENTER);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        b.setOpaque(false);
        Btn n = Btn.outline("Cancel");
        n.addActionListener(e -> d.dispose());
        Btn ok = new Btn("Save");
        ok.addActionListener(e -> { result[0] = f.getText().trim(); d.dispose(); });
        f.addActionListener(e -> ok.doClick());
        b.add(n);
        b.add(ok);
        p.add(b, BorderLayout.SOUTH);
        d.setUndecorated(true);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(parent);
        d.setVisible(true);
        return result[0];
    }
}
