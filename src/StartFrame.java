import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/** Page 1 (welcome) and Page 2 (choose Customer / Staff / Administrator). */
public class StartFrame extends JFrame {
    private static StartFrame instance;
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    public static StartFrame get() {
        if (instance == null) instance = new StartFrame();
        return instance;
    }

    /** Back to the welcome page (used after an order is placed). */
    public static void goHome() {
        StartFrame s = get();
        s.cards.show(s.root, "welcome");
        s.setVisible(true);
    }

    /** Back to the "who are you" page (used by the login screens). */
    public static void goRoles() {
        StartFrame s = get();
        s.cards.show(s.root, "roles");
        s.setVisible(true);
    }

    private StartFrame() {
        Theme.prep(this, "SipSync Cafe Kiosk");
        root.add(welcomePage(), "welcome");
        root.add(rolePage(), "roles");
        setContentPane(root);
    }

    private JPanel welcomePage() {
        JPanel page = new JPanel(new GridBagLayout()); // GridBagLayout centers the column both ways
        page.setBackground(Theme.CREAM);

        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel logo = new JLabel(Theme.icon("logo_welcome.png", 380), SwingConstants.CENTER);
        logo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel welcome = Theme.label("Welcome to SipSync Cafe", Font.BOLD, 40, Theme.PRIMARY);
        welcome.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel tagline = Theme.label("Where Every Sip Meets Innovation", Font.ITALIC, 22, Theme.MOCHA);
        tagline.setAlignmentX(Component.CENTER_ALIGNMENT);

        Theme.Btn start = new Theme.Btn("Get Started");
        start.setFont(Theme.font(Font.BOLD, 24));
        Dimension d = new Dimension(300, 66);
        start.setPreferredSize(d);
        start.setMaximumSize(d);
        start.setAlignmentX(Component.CENTER_ALIGNMENT);
        start.addActionListener(e -> cards.show(root, "roles"));

        col.add(logo);
        col.add(Box.createVerticalStrut(6));
        col.add(welcome);
        col.add(Box.createVerticalStrut(8));
        col.add(tagline);
        col.add(Box.createVerticalStrut(34));
        col.add(start);
        page.add(col);

        // keep the logo proportional to the window height so everything stays on screen
        page.addComponentListener(new ComponentAdapter() {
            int last = 380;
            @Override public void componentResized(ComponentEvent e) {
                int s = Math.max(180, Math.min(420, (int) (page.getHeight() * 0.42)));
                s = (s / 20) * 20;
                if (s != last) { last = s; logo.setIcon(Theme.icon("logo_welcome.png", s)); }
            }
        });
        return page;
    }

    private JPanel rolePage() {
        JPanel page = new JPanel(new BorderLayout());
        page.setBackground(Theme.CREAM);
        Theme.Btn back = Theme.Btn.outline("Back");
        back.addActionListener(e -> cards.show(root, "welcome"));
        page.add(Theme.header("Self-Service Kiosk", back), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(Theme.CREAM);
        JPanel col = new JPanel();
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));
        col.setOpaque(false);

        JLabel title = Theme.label("Who is using the kiosk?", Font.BOLD, 36, Theme.PRIMARY);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = Theme.label("Please choose how you would like to continue", Font.PLAIN, 18, Theme.MOCHA);
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 28, 0));
        row.setOpaque(false);
        Theme.Btn customer = roleButton("Customer", "Browse the menu and place your order", Theme.PRIMARY, Theme.SECONDARY);
        Theme.Btn staff = roleButton("Staff / Barista", "Log in to manage incoming orders", Theme.SECONDARY, Theme.PRIMARY);
        Theme.Btn admin = roleButton("Administrator", "Log in to manage the menu and sales", Theme.ESPRESSO, Theme.MOCHA);
        row.add(customer);
        row.add(staff);
        row.add(admin);
        row.setAlignmentX(Component.CENTER_ALIGNMENT);

        customer.addActionListener(e -> { setVisible(false); new CustomerFrame().setVisible(true); });
        staff.addActionListener(e -> { setVisible(false); new LoginFrame("Staff").setVisible(true); });
        admin.addActionListener(e -> { setVisible(false); new LoginFrame("Admin").setVisible(true); });

        col.add(title);
        col.add(Box.createVerticalStrut(8));
        col.add(sub);
        col.add(Box.createVerticalStrut(36));
        col.add(row);
        center.add(col);
        page.add(center, BorderLayout.CENTER);
        return page;
    }

    private Theme.Btn roleButton(String title, String desc, Color fill, Color hover) {
        String html = "<html><div style='text-align:center;width:210px'><span style='font-size:22px'>" + title
                + "</span><br><br><span style='font-size:13px;font-weight:normal'>" + desc + "</span></div></html>";
        Theme.Btn b = new Theme.Btn(html, fill, hover, Color.WHITE, false);
        b.setPreferredSize(new Dimension(290, 190));
        return b;
    }
}
