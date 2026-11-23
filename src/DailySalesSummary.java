import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Counts only orders whose payment has been confirmed. */
public class DailySalesSummary {
    private final LocalDate date;
    private int totalOrders;
    private double totalSales;
    private final List<Order> countedOrders = new ArrayList<>();

    public DailySalesSummary(LocalDate date) { this.date = date; }

    public void computeSummary(List<Order> orders) {
        totalOrders = 0;
        totalSales = 0;
        countedOrders.clear();
        for (Order o : orders) {
            if (o.getOrderDate().toLocalDate().equals(date) && o.getPayment().isConfirmed()) {
                totalOrders++;
                totalSales += o.getTotalAmount();
                countedOrders.add(o);
            }
        }
    }

    public LocalDate getDate() { return date; }
    public int getTotalOrders() { return totalOrders; }
    public double getTotalSales() { return totalSales; }
    public List<Order> getCountedOrders() { return countedOrders; }
}
