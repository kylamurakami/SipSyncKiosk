import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.util.Map;
import java.util.function.Consumer;

/** Customize Drink: size, temperature, milk type, sweetness and add-ons. The price updates live. */
public class CustomizeDialog extends JDialog {
    private final MenuItem item;
    private final Customization c;
    private Customization result;
    private final JLabel totalLabel = Theme.label("", Font.BOLD, 22, Theme.PRIMARY);

    public CustomizeDialog(Component parent, MenuItem item, Customization start) {
        super(SwingUtilities.getWindowAncestor(parent), "Customize " + item.getName(), ModalityType.APPLICATION_MODAL);
        this.item = item;
        this.c = start != null ? start.copy() : Customization.defaultFor(item);
        setIconImage(Theme.loadImage("logo_main.png"));

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Theme.CREAM);
        p.setBorder(new EmptyBorder(20, 26, 18, 26));

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        head.setOpaque(false);
        head.add(new JLabel(Theme.icon(item.getImageFile(), 90)));
        JPanel t = new JPanel(new GridLayout(2, 1));
        t.setOpaque(false);
        t.add(Theme.label(item.getName(), Font.BOLD, 26, Theme.PRIMARY));
        t.add(Theme.label("Base price " + Theme.peso(item.getPrice()), Font.PLAIN, 15, Theme.MOCHA));
        head.add(t);
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(head);

        String[] sizes = Customization.SIZES.keySet().toArray(new String[0]);
        String[] milks = Customization.MILKS.keySet().toArray(new String[0]);
        p.add(single("Size", sizes, Customization.SIZES, c.getSize(), c::setSize, null));
        p.add(single("Temperature", Customization.TEMPERATURES, null, c.getTemperature(), c::setTemperature,
                item.isFrappe() ? "Hot" : null));
        p.add(single("Milk type", milks, Customization.MILKS, c.getMilkType(), c::setMilkType, null));
        p.add(single("Sweetness", Customization.SWEETNESS, null, c.getSweetness(), c::setSweetness, null));
        p.add(addOns());

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.setBorder(new EmptyBorder(14, 0, 0, 0));
        bottom.add(totalLabel, BorderLayout.WEST);
        JPanel b = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        b.setOpaque(false);
        Theme.Btn cancel = Theme.Btn.outline("Cancel");
        cancel.addActionListener(e -> dispose());
        Theme.Btn add = new Theme.Btn("Add to Cart");
        add.addActionListener(e -> { result = c; dispose(); });
        b.add(cancel);
        b.add(add);
        bottom.add(b, BorderLayout.EAST);
        bottom.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(bottom);

        update();
        setContentPane(p);
        pack();
        setLocationRelativeTo(parent);
    }

    private void update() { totalLabel.setText("Total: " + Theme.peso(item.getPrice() + c.getExtraCost())); }

    private JPanel single(String title, String[] opts, Map<String, Double> prices, String selected,
                          Consumer<String> onPick, String disabled) {
        JPanel sec = new JPanel(new BorderLayout(0, 4));
        sec.setOpaque(false);
        sec.setBorder(new EmptyBorder(12, 0, 0, 0));
        sec.setAlignmentX(Component.LEFT_ALIGNMENT);
        sec.add(Theme.label(title, Font.BOLD, 16, Theme.ESPRESSO), BorderLayout.NORTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (String o : opts) {
            double extra = prices == null ? 0 : prices.get(o);
            Theme.Chip chip = new Theme.Chip(extra > 0 ? o + "  +" + Theme.peso(extra) : o);
            chip.setSelected(o.equals(selected));
            if (o.equals(disabled)) chip.setEnabled(false);
            chip.addActionListener(e -> { onPick.accept(o); update(); });
            g.add(chip);
            row.add(chip);
        }
        sec.add(row, BorderLayout.CENTER);
        return sec;
    }

    private JPanel addOns() {
        JPanel sec = new JPanel(new BorderLayout(0, 4));
        sec.setOpaque(false);
        sec.setBorder(new EmptyBorder(12, 0, 0, 0));
        sec.setAlignmentX(Component.LEFT_ALIGNMENT);
        sec.add(Theme.label("Add-ons (optional)", Font.BOLD, 16, Theme.ESPRESSO), BorderLayout.NORTH);
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.setOpaque(false);
        for (Map.Entry<String, Double> e : Customization.ADD_ONS.entrySet()) {
            Theme.Chip chip = new Theme.Chip(e.getKey() + "  +" + Theme.peso(e.getValue()));
            chip.setSelected(c.getAddOns().contains(e.getKey()));
            chip.addActionListener(ev -> {
                if (chip.isSelected()) c.getAddOns().add(e.getKey()); else c.getAddOns().remove(e.getKey());
                update();
            });
            row.add(chip);
        }
        row.setPreferredSize(new Dimension(520, 90));
        sec.add(row, BorderLayout.CENTER);
        return sec;
    }

    public Customization getResult() { return result; }
}
