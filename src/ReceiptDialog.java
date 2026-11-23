import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Shown after the order is saved: order number, on-screen receipt and the "pay at the counter" reminder. */
public class ReceiptDialog extends JDialog {
    public ReceiptDialog(Component parent, Order order) {
        super(SwingUtilities.getWindowAncestor(parent), "Order Placed", ModalityType.APPLICATION_MODAL);
        setIconImage(Theme.loadImage("logo_main.png"));

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.CREAM);
        p.setBorder(new EmptyBorder(22, 30, 22, 30));

        JPanel top = new JPanel(new GridLayout(3, 1, 0, 2));
        top.setOpaque(false);
        JLabel a = Theme.label("Order placed!", Font.BOLD, 30, Theme.PRIMARY);
        JLabel b = Theme.label("Your order number", Font.PLAIN, 15, Theme.MOCHA);
        JLabel c = Theme.label(order.numberText(), Font.BOLD, 44, Theme.ESPRESSO);
        for (JLabel l : new JLabel[]{a, b, c}) l.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(a);
        top.add(b);
        top.add(c);
        p.add(top, BorderLayout.NORTH);

        JTextArea receipt = new JTextArea(new Receipt(order).generateReceipt());
        receipt.setEditable(false);
        receipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        receipt.setForeground(Theme.ESPRESSO);
        receipt.setBackground(Color.WHITE);
        receipt.setBorder(new EmptyBorder(14, 14, 14, 14));
        JScrollPane sp = Theme.scroll(receipt);
        sp.setPreferredSize(new Dimension(470, 330));
        p.add(sp, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 12));
        bottom.setOpaque(false);
        JLabel note = Theme.label("<html><div style='text-align:center;width:440px'>Please pay at the counter. "
                + "Our staff will confirm your payment and start preparing your order.</div></html>", Font.BOLD, 16, Theme.SECONDARY);
        note.setHorizontalAlignment(SwingConstants.CENTER);
        bottom.add(note, BorderLayout.NORTH);
        Theme.Btn done = new Theme.Btn("Done");
        done.setFont(Theme.font(Font.BOLD, 20));
        done.addActionListener(e -> dispose());
        bottom.add(done, BorderLayout.CENTER);
        p.add(bottom, BorderLayout.SOUTH);

        setContentPane(p);
        pack();
        setLocationRelativeTo(parent);
    }
}
