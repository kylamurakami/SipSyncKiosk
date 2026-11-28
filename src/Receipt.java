import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Receipt {
    private static final int W = 44;
    private final Order order;

    public Receipt(Order order) { this.order = order; }

    public String generateReceipt() {
        // Paid orders (seen by the administrator) say "Paid"; a fresh order still says to pay at the counter
        String status = order.getPayment().isConfirmed() ? "Paid" : "Unpaid - pay at the counter";
        return build(order.numberText(), order.getOrderDate(), order.getItems(), order.getPayment().getMethod(),
                order.getTotalAmount(), status);
    }

    /** Preview shown before the customer confirms the order (no order number yet). */
    public static String preview(List<OrderItem> items, String method, double total) {
        return build("(given after you confirm)", LocalDateTime.now(), items, method, total, "Not yet placed");
    }

    private static String build(String no, LocalDateTime dt, List<OrderItem> items, String method, double total, String status) {
        StringBuilder sb = new StringBuilder();
        String line = "-".repeat(W);
        sb.append(center("SipSync Cafe")).append('\n');
        sb.append(center("Where Every Sip Meets Innovation")).append('\n').append(line).append('\n');
        sb.append("Order No : ").append(no).append('\n');
        sb.append("Date     : ").append(dt.format(DateTimeFormatter.ofPattern("MMM dd, yyyy  hh:mm a"))).append('\n');
        sb.append(line).append('\n');
        for (OrderItem i : items) {
            sb.append(row(i.getQuantity() + " x " + i.getItem().getName(), Theme.peso(i.calculateSubtotal()))).append('\n');
            if (!i.getOptionsText().isEmpty()) {
                for (String s : wrap(i.getOptionsText(), W - 4)) sb.append("    ").append(s).append('\n');
            }
        }
        sb.append(line).append('\n');
        sb.append(row("TOTAL", Theme.peso(total))).append('\n').append(line).append('\n');
        sb.append("Payment  : ").append(method == null ? "(choose a method)" : method).append('\n');
        sb.append("Status   : ").append(status).append('\n');
        return sb.toString();
    }

    private static String center(String s) {
        int pad = Math.max(0, (W - s.length()) / 2);
        return " ".repeat(pad) + s;
    }

    private static String row(String left, String right) {
        int space = Math.max(1, W - left.length() - right.length());
        return left + " ".repeat(space) + right;
    }

    private static List<String> wrap(String text, int width) {
        List<String> out = new java.util.ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String word : text.split(" ")) {
            if (cur.length() + word.length() + 1 > width && cur.length() > 0) { out.add(cur.toString()); cur.setLength(0); }
            if (cur.length() > 0) cur.append(' ');
            cur.append(word);
        }
        if (cur.length() > 0) out.add(cur.toString());
        return out;
    }
}