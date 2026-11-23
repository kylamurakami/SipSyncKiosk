import java.util.ArrayList;
import java.util.List;

/** The kiosk customer. Customers never log in, so this class does not extend User. */
public class Customer {
    private static int counter = 1;
    private final int customerId = counter++;
    private final List<OrderItem> cart = new ArrayList<>();

    public void selectItem(MenuItem item, Customization c) {
        for (OrderItem oi : cart) {
            if (oi.sameAs(item, c)) { oi.setQuantity(oi.getQuantity() + 1); return; }
        }
        cart.add(new OrderItem(item, c, 1));
    }

    public void changeQuantity(OrderItem oi, int delta) {
        int q = oi.getQuantity() + delta;
        if (q <= 0) cart.remove(oi); else oi.setQuantity(q);
    }

    public void removeItem(OrderItem oi) { cart.remove(oi); }
    public void clearCart() { cart.clear(); }

    public double getCartTotal() {
        double t = 0;
        for (OrderItem oi : cart) t += oi.calculateSubtotal();
        return t;
    }

    public int getCartCount() {
        int n = 0;
        for (OrderItem oi : cart) n += oi.getQuantity();
        return n;
    }

    /** Saves the order as Pending with payment unconfirmed, then empties the cart. */
    public Order placeOrder(String paymentMethod) {
        Order o = DataStore.get().placeOrder(cart, paymentMethod);
        cart.clear();
        return o;
    }

    public List<OrderItem> getCart() { return cart; }
    public int getCustomerId() { return customerId; }
}
