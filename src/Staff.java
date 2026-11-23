import java.util.List;

/** The single hard-coded Staff / Barista account. */
public class Staff extends User {
    public Staff() { super(1, "Staff / Barista", "staff", "staff123"); }

    @Override public String getRole() { return "Staff"; }

    public List<Order> viewIncomingOrders() { return DataStore.get().getOrders(); }
    public void confirmPayment(Order o) { o.getPayment().confirmPayment(); }
    public boolean updateOrderStatus(Order o) { return o.updateStatus(); }
}
