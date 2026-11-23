import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Staff / Barista screen: view incoming orders, confirm payment, update order status. */
public class StaffFrame extends JFrame {
    private final Staff staff;
    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"Order #", "Items", "Total", "Payment Method", "Payment", "Status", "Time"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable table = new JTable(model);
    private final JTextArea detail = new JTextArea();
    private final Theme.Btn confirmBtn = new Theme.Btn("Confirm Payment");
    private final Theme.Btn statusBtn = new Theme.Btn("Update Status");
    private final JLabel hint = Theme.label(" ", Font.PLAIN, 14, Theme.MOCHA);
    private final List<Order> shown = new ArrayList<>();
    private String filter = "All";
    private String lastSignature = "";

    public StaffFrame(Staff staff) {
        this.staff = staff;
        Theme.prep(this, "SipSync Cafe - Staff / Barista");
        setLayout(new BorderLayout());

        Theme.Btn logout = Theme.Btn.outline("Log Out");
        logout.addActionListener(e -> { staff.logout(); dispose(); new LoginFrame("Staff").setVisible(true); });
        add(Theme.header("Staff / Barista  |  Incoming Orders", logout), BorderLayout.NORTH);

        JPanel body = new JPanel(new BorderLayout(0, 10));
        body.setBackground(Theme.CREAM);
        body.setBorder(new EmptyBorder(14, 18, 14, 18));

        // filter chips
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        chips.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (String f : new String[]{"All", "Pending", "Preparing", "Ready", "Served"}) {
            Theme.Chip c = new Theme.Chip(f);
            c.setFont(Theme.font(Font.BOLD, 16));
            if (f.equals("All")) c.setSelected(true);
            c.addActionListener(e -> { filter = f; refresh(true); });
            g.add(c);
            chips.add(c);
        }
        body.add(chips, BorderLayout.NORTH);

        // table
        Theme.styleTable(table);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        int[] widths = {80, 330, 90, 130, 120, 110, 90};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        table.getSelectionModel().addListSelectionListener(e -> { if (!e.getValueIsAdjusting()) updateSelection(); });

        // order details
        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        detail.setForeground(Theme.ESPRESSO);
        detail.setBackground(Color.WHITE);
        detail.setBorder(new EmptyBorder(12, 12, 12, 12));
        JPanel right = new JPanel(new BorderLayout(0, 6));
        right.setOpaque(false);
        right.setPreferredSize(new Dimension(390, 100));
        right.add(Theme.label("Order details", Font.BOLD, 18, Theme.PRIMARY), BorderLayout.NORTH);
        right.add(Theme.scroll(detail), BorderLayout.CENTER);

        JPanel mid = new JPanel(new BorderLayout(14, 0));
        mid.setOpaque(false);
        mid.add(Theme.scroll(table), BorderLayout.CENTER);
        mid.add(right, BorderLayout.EAST);
        body.add(mid, BorderLayout.CENTER);

        // actions
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        actions.setOpaque(false);
        confirmBtn.addActionListener(e -> confirmPayment());
        statusBtn.addActionListener(e -> advanceStatus());
        actions.add(confirmBtn);
        actions.add(statusBtn);
        actions.add(hint);
        body.add(actions, BorderLayout.SOUTH);
        add(body, BorderLayout.CENTER);

        refresh(true);
        updateSelection();
        // new orders arrive from the customer kiosk, so check for them every 2 seconds
        new Timer(2000, e -> refresh(false)).start();
    }

    private Order selected() {
        int r = table.getSelectedRow();
        return r >= 0 && r < shown.size() ? shown.get(r) : null;
    }

    private void refresh(boolean force) {
        List<Order> all = staff.viewIncomingOrders();
        StringBuilder sig = new StringBuilder(filter);
        for (Order o : all) sig.append(o.getOrderNumber()).append(o.getStatus()).append(o.getPayment().isConfirmed());
        if (!force && sig.toString().equals(lastSignature)) return;
        lastSignature = sig.toString();

        Order keep = selected();
        shown.clear();
        model.setRowCount(0);
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("hh:mm a");
        for (Order o : all) {
            if (!filter.equals("All") && !o.getStatus().label().equals(filter)) continue;
            shown.add(o);
            model.addRow(new Object[]{o.numberText(), o.itemsSummary(), Theme.peso(o.getTotalAmount()),
                    o.getPayment().getMethod(), o.getPayment().isConfirmed() ? "Confirmed" : "Unconfirmed",
                    o.getStatus().label(), o.getOrderDate().format(tf)});
        }
        if (keep != null && shown.contains(keep)) {
            int i = shown.indexOf(keep);
            table.setRowSelectionInterval(i, i);
        }
        updateSelection();
    }

    private void updateSelection() {
        Order o = selected();
        if (o == null) {
            detail.setText(shown.isEmpty() ? "No orders yet.\n\nNew orders from the customer kiosk will appear here." : "Select an order to see its details.");
            confirmBtn.setEnabled(false);
            statusBtn.setEnabled(false);
            statusBtn.setText("Update Status");
            hint.setText(" ");
            return;
        }
        detail.setText(new Receipt(o).generateReceipt());
        detail.setCaretPosition(0);
        boolean paid = o.getPayment().isConfirmed();
        confirmBtn.setEnabled(!paid);
        Order.Status next = o.getStatus().next();
        statusBtn.setEnabled(next != null);
        statusBtn.setText(next == null ? "Order Served" : "Mark as " + next.label());
        hint.setText(!paid ? "Check the payment at the counter, then click Confirm Payment."
                : next == null ? "This order is complete." : " ");
    }

    private void confirmPayment() {
        Order o = selected();
        if (o == null) return;
        if (Theme.confirm(this, "Confirm payment?", "Order " + o.numberText() + " - " + o.getPayment().getMethod()
                + " - " + Theme.peso(o.getTotalAmount()) + "<br><br>Only confirm after you have checked the payment.", "Confirm", "Cancel")) {
            staff.confirmPayment(o);
            refresh(true);
        }
    }

    private void advanceStatus() {
        Order o = selected();
        if (o == null) return;
        if (!staff.updateOrderStatus(o)) {
            Theme.info(this, "Payment not confirmed", "Please confirm the payment first. An order cannot be set to Preparing until it is paid.");
            return;
        }
        refresh(true);
    }
}
