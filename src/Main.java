import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        Theme.init();
        SwingUtilities.invokeLater(() -> StartFrame.get().setVisible(true));
    }
}
