import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Administrator screen: manage the menu (items, prices, availability) and view the daily sales summary. */
public class AdminFrame extends JFrame {
    private final Administrator admin;
    private final CardLayout cards = new CardLayout();
    private final JPanel content = new JPanel(cards);

    // menu tab
    private final DefaultTableModel menuModel = new DefaultTableModel(
            new String[]{"ID", "Item", "Category", "Price", "Availability"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable menuTable = new JTable(menuModel);
    private List<MenuItem> menuRows;
    private final Theme.Btn toggleBtn = new Theme.Btn("Mark Unavailable");
    private final Theme.Btn priceBtn = Theme.Btn.outline("Edit Price");

    // sales tab
    private final DefaultTableModel salesModel = new DefaultTableModel(
            new String[]{"Order #", "Time", "Items", "Payment Method", "Total"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JLabel dateLbl = Theme.label("", Font.BOLD, 30, Theme.PRIMARY);
    private final JLabel ordersLbl = Theme.label("0", Font.BOLD, 30, Theme.PRIMARY);
    private final JLabel salesLbl = Theme.label("", Font.BOLD, 30, Theme.PRIMARY);

    public AdminFrame(Administrator admin) {
        this.admin = admin;
        Theme.prep(this, "SipSync Cafe - Administrator");
        setLayout(new BorderLayout());

        Theme.Btn logout = Theme.Btn.outline("Log Out");
        logout.addActionListener(e -> { admin.logout(); dispose(); new LoginFrame("Admin").setVisible(true); });
        add(Theme.header("Administrator", logout), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(Theme.CREAM);
        body.setBorder(new EmptyBorder(14, 18, 14, 18));

        JPanel nav = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        nav.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        Theme.Chip menuChip = new Theme.Chip("Manage Menu");
        Theme.Chip salesChip = new Theme.Chip("Daily Sales");
        for (Theme.Chip c : new Theme.Chip[]{menuChip, salesChip}) { c.setFont(Theme.font(Font.BOLD, 17)); g.add(c); nav.add(c); }
        menuChip.setSelected(true);
        menuChip.addActionListener(e -> { cards.show(content, "menu"); refreshMenu(); });
        salesChip.addActionListener(e -> { cards.show(content, "sales"); refreshSales(); });
        body.add(nav, BorderLayout.NORTH);

        content.setOpaque(false);
        content.add(menuPanel(), "menu");
        content.add(salesPanel(), "sales");
        body.add(content, BorderLayout.CENTER);
        add(body, BorderLayout.CENTER);

        refreshMenu();
        refreshSales();
        new Timer(3000, e -> refreshSales()).start();
    }

    // ---------------------------------------------------------------- menu
    private JPanel menuPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setOpaque(false);
        Theme.styleTable(menuTable);
        menuTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuTable.getColumnModel().getColumn(0).setMaxWidth(70);
        menuTable.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateMenuButtons(); });
        p.add(Theme.scroll(menuTable), BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        actions.setOpaque(false);
        Theme.Btn add = new Theme.Btn("Add Item");
        add.addActionListener(e -> addItem());
        priceBtn.addActionListener(e -> editPrice());
        toggleBtn.addActionListener(e -> toggleAvailability());
        actions.add(add);
        actions.add(priceBtn);
        actions.add(toggleBtn);
        p.add(actions, BorderLayout.SOUTH);
        updateMenuButtons();
        return p;
    }

    private MenuItem selectedItem() {
        int r = menuTable.getSelectedRow();
        return r >= 0 && r < menuRows.size() ? menuRows.get(r) : null;
    }

    private void refreshMenu() {
        MenuItem keep = menuRows == null ? null : selectedItem();
        menuRows = DataStore.get().getMenu();
        menuModel.setRowCount(0);
        for (MenuItem m : menuRows) {
            menuModel.addRow(new Object[]{m.getItemId(), m.getName(), m.getCategory(), Theme.peso(m.getPrice()),
                    m.isAvailable() ? "Available" : "Unavailable"});
        }
        if (keep != null) {
            int i = menuRows.indexOf(keep);
            if (i >= 0) menuTable.setRowSelectionInterval(i, i);
        }
        updateMenuButtons();
    }

    private void updateMenuButtons() {
        MenuItem m = menuRows == null ? null : selectedItem();
        priceBtn.setEnabled(m != null);
        toggleBtn.setEnabled(m != null);
        toggleBtn.setText(m != null && !m.isAvailable() ? "Mark Available" : "Mark Unavailable");
    }

    private void editPrice() {
        MenuItem m = selectedItem();
        if (m == null) return;
        String s = Theme.prompt(this, "Edit price", "New price for " + m.getName() + " (\u20B1)", String.valueOf((int) m.getPrice()));
        if (s == null) return;
        try {
            double v = Double.parseDouble(s);
            if (v <= 0) throw new NumberFormatException();
            admin.updatePrice(m, v);
            refreshMenu();
        } catch (NumberFormatException ex) {
            Theme.info(this, "Invalid price", "Please enter a price greater than zero, for example 120.");
        }
    }

    private void toggleAvailability() {
        MenuItem m = selectedItem();
        if (m == null) return;
        admin.updateAvailability(m, !m.isAvailable());
        refreshMenu();
    }

    private void addItem() {
        JDialog d = Theme.baseDialog(this, "Add menu item");
        JTextField name = Theme.field(20);
        JTextField price = Theme.field(20);
        JComboBox<String> cat = new JComboBox<>(DataStore.CATEGORIES);
        cat.setFont(Theme.font(Font.PLAIN, 16));
        JTextField img = Theme.field(20);
        img.setEditable(false);
        Theme.Btn browse = Theme.Btn.outline("Browse");
        browse.setMargin(new Insets(6, 14, 6, 14));
        browse.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Images", "png", "jpg", "jpeg"));
            if (fc.showOpenDialog(d) == JFileChooser.APPROVE_OPTION) img.setText(fc.getSelectedFile().getAbsolutePath());
        });

        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(Theme.CREAM);
        p.setBorder(new CompoundBorder(new LineBorder(Theme.PRIMARY, 3), new EmptyBorder(22, 28, 20, 28)));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridwidth = 2; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(4, 0, 4, 0);
        p.add(Theme.label("Add menu item", Font.BOLD, 22, Theme.PRIMARY), c);
        p.add(Theme.label("Item name", Font.BOLD, 15, Theme.ESPRESSO), c);
        p.add(name, c);
        p.add(Theme.label("Category", Font.BOLD, 15, Theme.ESPRESSO), c);
        p.add(cat, c);
        p.add(Theme.label("Price (\u20B1)", Font.BOLD, 15, Theme.ESPRESSO), c);
        p.add(price, c);
        p.add(Theme.label("Image (optional)", Font.BOLD, 15, Theme.ESPRESSO), c);
        c.gridwidth = 1; c.weightx = 1;
        p.add(img, c);
        c.gridx = 1; c.weightx = 0;
        p.add(browse, c);

        Theme.Btn cancel = Theme.Btn.outline("Cancel");
        cancel.addActionListener(e -> d.dispose());
        Theme.Btn save = new Theme.Btn("Add Item");
        save.addActionListener(e -> {
            String n = name.getText().trim();
            if (n.isEmpty()) { Theme.info(d, "Missing name", "Please enter the item name."); return; }
            if (DataStore.get().findByName(n) != null) { Theme.info(d, "Already on the menu", "An item with that name already exists."); return; }
            double v;
            try { v = Double.parseDouble(price.getText().trim()); if (v <= 0) throw new NumberFormatException(); }
            catch (NumberFormatException ex) { Theme.info(d, "Invalid price", "Please enter a price greater than zero."); return; }
            String image = img.getText().trim();
            admin.manageMenu(n, (String) cat.getSelectedItem(), v, image.isEmpty() || !new File(image).exists() ? null : image);
            d.dispose();
            refreshMenu();
        });
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        b.setOpaque(false);
        b.add(cancel);
        b.add(save);
        c.gridx = 0; c.gridwidth = 2; c.insets = new Insets(16, 0, 0, 0);
        p.add(b, c);

        d.setUndecorated(true);
        d.setContentPane(p);
        d.pack();
        d.setLocationRelativeTo(this);
        d.setVisible(true);
    }

    // --------------------------------------------------------------- sales
    private JPanel salesPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 14));
        p.setOpaque(false);

        JPanel stats = new JPanel(new GridLayout(1, 3, 16, 0));
        stats.setOpaque(false);
        stats.add(statCard("Date", dateLbl));
        stats.add(statCard("Total orders (paid)", ordersLbl));
        stats.add(statCard("Total sales", salesLbl));
        p.add(stats, BorderLayout.NORTH);

        JTable t = new JTable(salesModel);
        Theme.styleTable(t);
        p.add(Theme.scroll(t), BorderLayout.CENTER);
        p.add(Theme.label("Only orders with confirmed payment are counted.", Font.ITALIC, 14, Theme.MOCHA), BorderLayout.SOUTH);
        return p;
    }

    private JPanel statCard(String title, JLabel value) {
        JPanel c = new JPanel(new GridLayout(2, 1, 0, 4));
        c.setBackground(Theme.CREAM);
        c.setBorder(new CompoundBorder(new LineBorder(Theme.LATTE, 2, true), new EmptyBorder(14, 18, 14, 18)));
        c.add(Theme.label(title, Font.PLAIN, 15, Theme.MOCHA));
        c.add(value);
        return c;
    }

    private void refreshSales() {
        DailySalesSummary s = admin.viewDailySales();
        dateLbl.setText(s.getDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        ordersLbl.setText(String.valueOf(s.getTotalOrders()));
        salesLbl.setText(Theme.peso(s.getTotalSales()));
        salesModel.setRowCount(0);
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");
        for (Order o : s.getCountedOrders()) {
            salesModel.addRow(new Object[]{o.numberText(), o.getOrderDate().format(tf), o.itemsSummary(),
                    o.getPayment().getMethod(), Theme.peso(o.getTotalAmount())});
        }
    }
}
