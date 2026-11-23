import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

/** Customer kiosk: browse menu, filter by budget, quiz, customize, cart and place order. No login needed. */
public class CustomerFrame extends JFrame {
    private static final int IMG = 200;
    private final Customer customer = new Customer();

    private final JPanel grid = new JPanel();
    private final JScrollPane menuScroll;
    private final JLabel countLabel = Theme.label(" ", Font.PLAIN, 14, Theme.MOCHA);
    private final JTextField minField = Theme.field(6);
    private final JTextField maxField = Theme.field(6);
    private String category = "All";
    private Double minBudget, maxBudget;
    private int cols = 3;

    private final JPanel cartList = new JPanel();
    private final JLabel cartTitle = Theme.label("Your Cart", Font.BOLD, 22, Theme.PRIMARY);
    private final JLabel toast = Theme.label(" ", Font.BOLD, 14, Theme.SECONDARY);
    private final JLabel totalLabel = Theme.label("", Font.BOLD, 24, Theme.PRIMARY);
    private final Theme.Btn placeBtn = new Theme.Btn("Place Order");
    private final Theme.Btn clearBtn = Theme.Btn.outline("Clear Cart");
    private Timer toastTimer;

    public CustomerFrame() {
        Theme.prep(this, "SipSync Cafe - Customer");
        setLayout(new BorderLayout());

        Theme.Btn quiz = new Theme.Btn("Coffee Personality Quiz", Theme.SECONDARY, Theme.PRIMARY, Color.WHITE, false);
        quiz.addActionListener(e -> new QuizDialog(this).setVisible(true));
        Theme.Btn home = Theme.Btn.outline("Cancel & Home");
        home.addActionListener(e -> goHome());
        add(Theme.header("Customer Kiosk  |  Order here", quiz, home), BorderLayout.NORTH);

        // ---- menu area
        JPanel center = new JPanel(new BorderLayout(0, 10));
        center.setBackground(Theme.CREAM);
        center.setBorder(new EmptyBorder(14, 18, 14, 10));
        center.add(buildFilters(), BorderLayout.NORTH);

        grid.setBackground(Theme.CREAM);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(Theme.CREAM);
        wrap.add(grid, BorderLayout.NORTH);
        menuScroll = Theme.scroll(wrap);
        menuScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        menuScroll.addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
                int c = Math.max(1, (menuScroll.getViewport().getWidth() - 10) / 262);
                if (c != cols) { cols = c; refreshMenu(); }
            }
        });
        center.add(menuScroll, BorderLayout.CENTER);
        add(center, BorderLayout.CENTER);
        add(buildCartPanel(), BorderLayout.EAST);

        refreshMenu();
        refreshCart();
    }

    // ------------------------------------------------------------- filters
    private JPanel buildFilters() {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);

        JPanel cats = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        cats.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        String[] names = new String[DataStore.CATEGORIES.length + 1];
        names[0] = "All";
        System.arraycopy(DataStore.CATEGORIES, 0, names, 1, DataStore.CATEGORIES.length);
        for (String n : names) {
            Theme.Chip chip = new Theme.Chip(n);
            chip.setFont(Theme.font(Font.BOLD, 16));
            if (n.equals("All")) chip.setSelected(true);
            chip.addActionListener(e -> { category = n; refreshMenu(); });
            g.add(chip);
            cats.add(chip);
        }

        JPanel budget = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        budget.setOpaque(false);
        budget.add(Theme.label("Budget:  \u20B1", Font.BOLD, 16, Theme.ESPRESSO));
        budget.add(minField);
        budget.add(Theme.label("to  \u20B1", Font.BOLD, 16, Theme.ESPRESSO));
        budget.add(maxField);
        Theme.Btn apply = new Theme.Btn("Apply");
        apply.setMargin(new Insets(6, 18, 6, 18));
        apply.addActionListener(e -> applyBudget());
        Theme.Btn reset = Theme.Btn.outline("Reset");
        reset.setMargin(new Insets(6, 18, 6, 18));
        reset.addActionListener(e -> {
            minField.setText(""); maxField.setText(""); minBudget = null; maxBudget = null; refreshMenu();
        });
        maxField.addActionListener(e -> applyBudget());
        minField.addActionListener(e -> applyBudget());
        budget.add(apply);
        budget.add(reset);
        budget.add(Box.createHorizontalStrut(14));
        budget.add(countLabel);

        p.add(cats);
        p.add(budget);
        return p;
    }

    private void applyBudget() {
        try {
            String a = minField.getText().trim(), b = maxField.getText().trim();
            Double lo = a.isEmpty() ? null : Double.parseDouble(a);
            Double hi = b.isEmpty() ? null : Double.parseDouble(b);
            if ((lo != null && lo < 0) || (hi != null && hi < 0)) throw new NumberFormatException();
            if (lo != null && hi != null && lo > hi) {
                Theme.info(this, "Check your budget", "The minimum amount cannot be higher than the maximum.");
                return;
            }
            minBudget = lo; maxBudget = hi;
            refreshMenu();
        } catch (NumberFormatException ex) {
            Theme.info(this, "Check your budget", "Please enter the budget as numbers only, for example 100 to 130.");
        }
    }

    // ---------------------------------------------------------------- menu
    private void refreshMenu() {
        grid.removeAll();
        grid.setLayout(new GridLayout(0, cols, 16, 16));
        int shown = 0;
        for (MenuItem it : DataStore.get().getMenu()) {
            if (!category.equals("All") && !it.getCategory().equals(category)) continue;
            if (minBudget != null && it.getPrice() < minBudget) continue;
            if (maxBudget != null && it.getPrice() > maxBudget) continue;
            grid.add(card(it));
            shown++;
        }
        countLabel.setText(shown + (shown == 1 ? " item" : " items") + " shown");
        if (shown == 0) {
            grid.setLayout(new BorderLayout());
            grid.add(Theme.label("No items match your budget. Try a different range.", Font.PLAIN, 18, Theme.MOCHA), BorderLayout.CENTER);
        }
        grid.revalidate();
        grid.repaint();
        SwingUtilities.invokeLater(() -> menuScroll.getVerticalScrollBar().setValue(0));
    }

    private JPanel card(MenuItem it) {
        JPanel c = new JPanel(new BorderLayout(0, 8));
        c.setBackground(Theme.CREAM);
        c.setBorder(new CompoundBorder(new LineBorder(Theme.LATTE, 2, true), new EmptyBorder(12, 12, 12, 12)));

        JLabel img = new JLabel(Theme.icon(it.getImageFile(), IMG), SwingConstants.CENTER);
        c.add(img, BorderLayout.NORTH);

        JPanel info = new JPanel(new GridLayout(0, 1, 0, 2));
        info.setOpaque(false);
        JLabel name = Theme.label(it.getName(), Font.BOLD, 18, Theme.ESPRESSO);
        name.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel price = Theme.label(it.isAvailable() ? Theme.peso(it.getPrice()) : "Currently unavailable",
                Font.BOLD, 20, it.isAvailable() ? Theme.PRIMARY : Theme.MOCHA);
        price.setHorizontalAlignment(SwingConstants.CENTER);
        info.add(name);
        info.add(price);
        c.add(info, BorderLayout.CENTER);

        JPanel btns = new JPanel(new GridLayout(1, 0, 8, 0));
        btns.setOpaque(false);
        Theme.Btn add = new Theme.Btn("Add to Cart");
        add.setMargin(new Insets(8, 8, 8, 8));
        add.setEnabled(it.isAvailable());
        add.addActionListener(e -> addToCart(it, Customization.defaultFor(it)));
        btns.add(add);
        if (it.isDrink()) {
            Theme.Btn custom = Theme.Btn.outline("Customize");
            custom.setMargin(new Insets(8, 8, 8, 8));
            custom.setEnabled(it.isAvailable());
            custom.addActionListener(e -> {
                CustomizeDialog d = new CustomizeDialog(this, it, null);
                d.setVisible(true);
                if (d.getResult() != null) addToCart(it, d.getResult());
            });
            btns.add(custom);
        }
        c.add(btns, BorderLayout.SOUTH);
        return c;
    }

    // ---------------------------------------------------------------- cart
    private JPanel buildCartPanel() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBackground(Theme.CREAM);
        p.setPreferredSize(new Dimension(420, 100));
        p.setBorder(new CompoundBorder(new MatteBorder(0, 3, 0, 0, Theme.LATTE), new EmptyBorder(14, 16, 14, 16)));

        JPanel top = new JPanel(new GridLayout(2, 1));
        top.setOpaque(false);
        top.add(cartTitle);
        top.add(toast);
        p.add(top, BorderLayout.NORTH);

        cartList.setLayout(new BoxLayout(cartList, BoxLayout.Y_AXIS));
        cartList.setBackground(Theme.CREAM);
        JPanel wrap = new WidthTrackingPanel(new BorderLayout());
        wrap.setBackground(Theme.CREAM);
        wrap.add(cartList, BorderLayout.NORTH);
        JScrollPane sp = Theme.scroll(wrap);
        sp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        p.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 10));
        bottom.setOpaque(false);
        bottom.add(totalLabel, BorderLayout.NORTH);
        JPanel btns = new JPanel(new GridLayout(1, 0, 8, 0));
        btns.setOpaque(false);
        clearBtn.addActionListener(e -> {
            if (Theme.confirm(this, "Clear cart?", "Remove every item from your cart?", "Clear", "Keep items")) {
                customer.clearCart();
                refreshCart();
            }
        });
        placeBtn.addActionListener(e -> placeOrder());
        btns.add(clearBtn);
        btns.add(placeBtn);
        bottom.add(btns, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        return p;
    }

    public void addToCart(MenuItem it, Customization c) {
        customer.selectItem(it, c);
        refreshCart();
        toast.setText("\u2713 Added: " + it.getName());
        if (toastTimer != null) toastTimer.stop();
        toastTimer = new Timer(2200, e -> toast.setText(" "));
        toastTimer.setRepeats(false);
        toastTimer.start();
    }

    private void refreshCart() {
        cartList.removeAll();
        List<OrderItem> items = customer.getCart();
        int n = customer.getCartCount();
        cartTitle.setText("Your Cart" + (n > 0 ? " (" + n + ")" : ""));
        if (items.isEmpty()) {
            JLabel empty = Theme.label("<html>Your cart is empty.<br>Tap <b>Add to Cart</b> on any item.</html>", Font.PLAIN, 16, Theme.MOCHA);
            empty.setBorder(new EmptyBorder(16, 4, 0, 0));
            cartList.add(empty);
        }
        for (OrderItem oi : items) cartList.add(cartRow(oi));
        totalLabel.setText("Total:  " + Theme.peso(customer.getCartTotal()));
        placeBtn.setEnabled(!items.isEmpty());
        clearBtn.setEnabled(!items.isEmpty());
        cartList.revalidate();
        cartList.repaint();
    }

    private JPanel cartRow(OrderItem oi) {
        JPanel row = new JPanel(new BorderLayout(0, 4));
        row.setBackground(Theme.CREAM);
        row.setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.LATTE), new EmptyBorder(8, 2, 8, 2)));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(Theme.label(oi.getItem().getName(), Font.BOLD, 16, Theme.ESPRESSO), BorderLayout.WEST);
        Theme.Btn rm = new Theme.Btn("Remove", Theme.CREAM, Theme.LATTE, Theme.SECONDARY, false);
        rm.setFont(Theme.font(Font.BOLD, 13));
        rm.setMargin(new Insets(2, 8, 2, 8));
        rm.addActionListener(e -> { customer.removeItem(oi); refreshCart(); });
        top.add(rm, BorderLayout.EAST);
        row.add(top, BorderLayout.NORTH);

        if (!oi.getOptionsText().isEmpty()) {
            row.add(Theme.label("<html><div style='width:270px'>" + oi.getOptionsText() + "</div></html>", Font.PLAIN, 12, Theme.MOCHA), BorderLayout.CENTER);
        }

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        JPanel qty = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        qty.setOpaque(false);
        Theme.Btn minus = qtyButton("\u2212");
        Theme.Btn plus = qtyButton("+");
        minus.addActionListener(e -> { customer.changeQuantity(oi, -1); refreshCart(); });
        plus.addActionListener(e -> { customer.changeQuantity(oi, +1); refreshCart(); });
        JLabel q = Theme.label(String.valueOf(oi.getQuantity()), Font.BOLD, 17, Theme.ESPRESSO);
        q.setHorizontalAlignment(SwingConstants.CENTER);
        q.setPreferredSize(new Dimension(28, 30));
        qty.add(minus);
        qty.add(q);
        qty.add(plus);
        bottom.add(qty, BorderLayout.WEST);
        bottom.add(Theme.label(Theme.peso(oi.calculateSubtotal()), Font.BOLD, 17, Theme.PRIMARY), BorderLayout.EAST);
        row.add(bottom, BorderLayout.SOUTH);
        return row;
    }

    private Theme.Btn qtyButton(String t) {
        Theme.Btn b = new Theme.Btn(t, Theme.SECONDARY, Theme.PRIMARY, Color.WHITE, false);
        b.setMargin(new Insets(0, 0, 0, 0));
        b.setPreferredSize(new Dimension(34, 30));
        return b;
    }

    // ------------------------------------------------------------ checkout
    private void placeOrder() {
        CheckoutDialog d = new CheckoutDialog(this, customer);
        d.setVisible(true);
        Order o = d.getPlacedOrder();
        if (o != null) {
            new ReceiptDialog(this, o).setVisible(true);
            dispose();
            StartFrame.goHome();
        } else {
            refreshCart();
        }
    }

    private void goHome() {
        if (customer.getCartCount() > 0
                && !Theme.confirm(this, "Leave the kiosk?", "Your cart will be emptied if you go back to the start.", "Yes, go back", "Stay")) {
            return;
        }
        dispose();
        StartFrame.goHome();
    }

    /** Keeps the cart list exactly as wide as the cart panel so rows (and the Remove button) are never cut off. */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        WidthTrackingPanel(LayoutManager layout) { super(layout); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 18; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 80; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }
}