import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;

/** Coffee Personality Quiz: 3 questions, then a drink recommendation the customer can add to the cart. */
public class QuizDialog extends JDialog {
    private final CustomerFrame owner;
    private final PersonalityQuiz quiz = new PersonalityQuiz();
    private final JPanel body = new JPanel(new BorderLayout());
    private int step = 0;

    public QuizDialog(CustomerFrame owner) {
        super(owner, "SipSync Cafe - Find Your Perfect Sip!", ModalityType.APPLICATION_MODAL);
        this.owner = owner;
        setIconImage(Theme.loadImage("logo_main.png"));
        body.setBackground(Theme.CREAM);
        body.setBorder(new EmptyBorder(24, 34, 22, 34));
        setContentPane(body);
        setSize(660, 640);
        setLocationRelativeTo(owner);
        quiz.startQuiz();
        showQuestion();
    }

    private void showQuestion() {
        body.removeAll();
        JPanel top = new JPanel(new GridLayout(3, 1, 0, 6));
        top.setOpaque(false);
        JLabel t = Theme.label("Find Your Perfect Sip!", Font.BOLD, 28, Theme.PRIMARY);
        t.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel prog = Theme.label("Question " + (step + 1) + " of " + quiz.getQuestionCount(), Font.PLAIN, 15, Theme.MOCHA);
        prog.setHorizontalAlignment(SwingConstants.CENTER);
        JLabel q = Theme.label("<html><div style='text-align:center;width:520px'>" + quiz.getQuestion(step) + "</div></html>",
                Font.BOLD, 22, Theme.ESPRESSO);
        q.setHorizontalAlignment(SwingConstants.CENTER);
        top.add(t);
        top.add(prog);
        top.add(q);
        body.add(top, BorderLayout.NORTH);

        JPanel opts = new JPanel(new GridLayout(3, 1, 0, 14));
        opts.setOpaque(false);
        opts.setBorder(new EmptyBorder(24, 0, 24, 0));
        String[] letters = {"A", "B", "C"};
        Color[] fills = {Theme.PRIMARY, Theme.SECONDARY, Theme.ESPRESSO};
        String[] o = quiz.getOptions(step);
        for (int i = 0; i < 3; i++) {
            final int choice = i;
            Theme.Btn b = new Theme.Btn(letters[i] + ".   " + o[i], fills[i], Theme.MOCHA, Color.WHITE, false);
            b.setFont(Theme.font(Font.BOLD, 20));
            b.addActionListener(e -> {
                quiz.answer(step, choice);
                step++;
                if (step < quiz.getQuestionCount()) showQuestion(); else showResult();
            });
            opts.add(b);
        }
        body.add(opts, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottom.setOpaque(false);
        if (step > 0) {
            Theme.Btn back = Theme.Btn.outline("Previous");
            back.addActionListener(e -> { step--; showQuestion(); });
            bottom.add(back);
        }
        Theme.Btn cancel = Theme.Btn.outline("Cancel");
        cancel.addActionListener(e -> dispose());
        bottom.add(cancel);
        body.add(bottom, BorderLayout.SOUTH);
        body.revalidate();
        body.repaint();
    }

    private void showResult() {
        body.removeAll();
        PersonalityQuiz.Recommendation rec = quiz.getRecommendations();
        MenuItem item = DataStore.get().findByName(rec.menuItem);

        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(centered(Theme.label("Your perfect sip is", Font.PLAIN, 17, Theme.MOCHA)));
        center.add(centered(Theme.label(rec.drink, Font.BOLD, 34, Theme.PRIMARY)));
        if (item != null) {
            String sub = rec.isExactMatch() ? "Available on our menu"
                    : "Closest on our menu: " + item.getName() + (rec.temperature != null ? " (" + rec.temperature + ")" : "");
            center.add(centered(Theme.label(sub, Font.ITALIC, 16, Theme.SECONDARY)));
            center.add(Box.createVerticalStrut(10));
            center.add(centered(new JLabel(Theme.icon(item.getImageFile(), 230))));
            center.add(centered(Theme.label(item.getName() + "  \u2022  " + Theme.peso(item.getPrice()), Font.BOLD, 20, Theme.ESPRESSO)));
        }
        body.add(center, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        bottom.setOpaque(false);
        if (item != null) {
            Theme.Btn add = new Theme.Btn("Add to Cart");
            add.setEnabled(item.isAvailable());
            add.addActionListener(e -> { owner.addToCart(item, preset(item, rec)); dispose(); });
            Theme.Btn custom = Theme.Btn.outline("Customize");
            custom.setEnabled(item.isAvailable());
            custom.addActionListener(e -> {
                CustomizeDialog d = new CustomizeDialog(this, item, preset(item, rec));
                d.setVisible(true);
                if (d.getResult() != null) { owner.addToCart(item, d.getResult()); dispose(); }
            });
            bottom.add(add);
            bottom.add(custom);
        }
        Theme.Btn retake = Theme.Btn.outline("Retake Quiz");
        retake.addActionListener(e -> { step = 0; quiz.startQuiz(); showQuestion(); });
        Theme.Btn close = Theme.Btn.outline("Close");
        close.addActionListener(e -> dispose());
        bottom.add(retake);
        bottom.add(close);
        body.add(bottom, BorderLayout.SOUTH);
        body.revalidate();
        body.repaint();
    }

    private Customization preset(MenuItem item, PersonalityQuiz.Recommendation rec) {
        Customization c = Customization.defaultFor(item);
        if (c != null && rec.temperature != null && !item.isFrappe()) c.setTemperature(rec.temperature);
        return c;
    }

    private JComponent centered(JComponent c) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 2));
        p.setOpaque(false);
        p.add(c);
        return p;
    }
}
