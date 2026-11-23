/** Payment is verified by hand: the kiosk only records the method the customer chose. */
public class Payment {
    private final int paymentId;
    private final String method;
    private final double amount;
    private boolean isConfirmed = false;

    public Payment(int paymentId, String method, double amount) {
        this.paymentId = paymentId;
        this.method = method;
        this.amount = amount;
    }

    public void confirmPayment() { isConfirmed = true; }

    public int getPaymentId() { return paymentId; }
    public String getMethod() { return method; }
    public double getAmount() { return amount; }
    public boolean isConfirmed() { return isConfirmed; }
}
