import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Place Order: review the receipt, choose Cash / GCash / E-Wallet, then confirm. */
public class CheckoutDialog extends JDialog {
    private final Customer customer;
    private final JTextArea receipt = new JTextArea();
    private final Theme.Btn confirm = new Theme.Btn("Confirm Order");
    private String method;
    private Order placed;

    public CheckoutDialog(Component parent, Customer customer) {
        super(SwingUtilities.getWindowAncestor(parent), "Place Order", ModalityType.APPLICATION_MODAL);
        this.customer = customer;
        setIconImage(Theme.loadImage("logo_main.png"));

        JPanel p = new JPanel(new BorderLayout(24, 0));
        p.setBackground(Theme.CREAM);
        p.setBorder(new EmptyBorder(22, 26, 20, 26));

        // left: on-screen receipt
        JPanel left = new JPanel(new BorderLayout(0, 8));
        left.setOpaque(false);
        left.add(Theme.label("Review your order", Font.BOLD, 22, Theme.PRIMARY), BorderLayout.NORTH);
        receipt.setEditable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        receipt.setForeground(Theme.ESPRESSO);
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(new EmptyBorder(14, 14, 14, 14));
        JScrollPane sp = Theme.scroll(receipt);
        sp.setPreferredSize(new Dimension(450, 430));
        left.add(sp, BorderLayout.CENTER);

        // right: payment method
        JPanel right = new JPanel();
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(300, 400));
        right.add(left(Theme.label("Payment method", Font.BOLD, 22, Theme.PRIMARY)));
        right.add(Box.createVerticalStrut(12));
        ButtonGroup g = new ButtonGroup();
        for (String m : new String[]{"Cash", "GCash", "E-Wallet"}) {
            Theme.Chip chip = new Theme.Chip(m);
            chip.setFont(Theme.font(Font.BOLD, 20));
            chip.setMargin(new Insets(12, 20, 12, 20));
            chip.setPreferredSize(new Dimension(280, 54));
            chip.setMaximumSize(new Dimension(280, 54));
            chip.addActionListener(e -> { method = m; confirm.setEnabled(true); updateReceipt(); });
            g.add(chip);
            right.add(left(chip));
            right.add(Box.createVerticalStrut(10));
        }
        right.add(Box.createVerticalStrut(8));
        right.add(left(Theme.label("<html><div style='width:270px'>Pick a method, then confirm. You will pay at the counter, "
                + "and our staff will check your payment before your order is prepared.</div></html>", Font.PLAIN, 14, Theme.MOCHA)));

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(14, 0, 0, 0));
        bottom.add(Theme.label("Total:  " + Theme.peso(customer.getCartTotal()), Font.BOLD, 26, Theme.PRIMARY), BorderLayout.WEST);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        b.setOpaque(false);
        Theme.Btn back = Theme.Btn.outline("Back to Cart");
        back.addActionListener(e -> dispose());
        confirm.setEnabled(false);
        confirm.addActionListener(e -> { placed = customer.placeOrder(method); dispose(); });
        b.add(back);
        b.add(confirm);
        bottom.add(b, BorderLayout.EAST);

        p.add(left, BorderLayout.WEST);
        p.add(right, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);
        updateReceipt();
        setContentPane(p);
        pack();
        setLocationRelativeTo(parent);
    }

    private void updateReceipt() {
        receipt.setText(Receipt.preview(customer.getCart(), method, customer.getCartTotal()));
        receipt.setCaretPosition(0);
    }

    private JComponent left(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(c);
        return p;
    }

    public Order getPlacedOrder() { return placed; }
}
