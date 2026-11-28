import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Administrator view of an order's receipt: the same on-screen receipt the customer sees. */
public class ReceiptViewDialog extends JDialog {
    public ReceiptViewDialog(Component parent, Order order) {
        super(parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent),
                "Receipt " + order.numberText(), ModalityType.APPLICATION_MODAL);
        setIconImage(Theme.loadImage("logo_main.png"));

        JPanel p = new JPanel(new BorderLayout(0, 12));
        p.setBackground(Theme.CREAM);
        p.setBorder(new EmptyBorder(20, 28, 20, 28));

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 2));
        top.setOpaque(false);
        JLabel a = Theme.label("Order " + order.numberText(), Font.BOLD, 28, Theme.PRIMARY);
        JLabel b = Theme.label("Order status: " + order.getStatus().label(), Font.PLAIN, 15, Theme.MOCHA);
        a.setHorizontalAlignment(SwingConstants.CENTER);
        b.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(a);
        top.add(b);
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

        Theme.Btn close = new Theme.Btn("Close");
        close.setFont(Theme.font(Font.BOLD, 18));
        close.addActionListener(e -> dispose());
        p.add(close, BorderLayout.SOUTH);

        setContentPane(p);
        pack();
        setLocationRelativeTo(parent);
    }
}