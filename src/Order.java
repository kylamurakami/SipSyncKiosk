import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Order {
    public enum Status {
        PENDING("Pending"), PREPARING("Preparing"), READY("Ready"), SERVED("Served");
        private final String label;
        Status(String label) { this.label = label; }
        public String label() { return label; }
        public Status next() { return ordinal() + 1 < values().length ? values()[ordinal() + 1] : null; }
    }

    private final int orderNumber;
    private final LocalDateTime orderDate;
    private Status status = Status.PENDING;
    private final List<OrderItem> items;
    private final Payment payment;
    private double totalAmount;

    /** A new order placed right now. */
    public Order(int orderNumber, List<OrderItem> items, String paymentMethod) {
        this(orderNumber, items, paymentMethod, LocalDateTime.now());
    }

    /** Used when orders are loaded back from the saved file (keeps the original date and time). */
    public Order(int orderNumber, List<OrderItem> items, String paymentMethod, LocalDateTime orderDate) {
        this.orderNumber = orderNumber;
        this.orderDate = orderDate;
        this.items = new ArrayList<>(items);
        this.totalAmount = calculateTotal();
        this.payment = new Payment(orderNumber, paymentMethod, totalAmount);
    }

    public double calculateTotal() {
        double t = 0;
        for (OrderItem i : items) t += i.calculateSubtotal();
        return t;
    }

    /** Moves to the next status. Preparing is not allowed until the payment is confirmed. */
    public boolean updateStatus() {
        if (status == Status.PENDING && !payment.isConfirmed()) return false;
        Status n = status.next();
        if (n == null) return false;
        status = n;
        return true;
    }

    public String itemsSummary() {
        StringBuilder sb = new StringBuilder();
        for (OrderItem i : items) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(i.getQuantity()).append("x ").append(i.getItem().getName());
        }
        return sb.toString();
    }

    public String numberText() { return String.format("#%03d", orderNumber); }

    public int getOrderNumber() { return orderNumber; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public Status getStatus() { return status; }
    public List<OrderItem> getItems() { return items; }
    public Payment getPayment() { return payment; }
    public double getTotalAmount() { return totalAmount; }
}