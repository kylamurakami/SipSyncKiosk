import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Sales Analytics tab for the administrator.
 * Shows KPI cards, best sellers (with % of sales), sales by category and payment method,
 * a daily trend, the busiest hours and a full item ranking. Only paid orders are counted.
 * The period can be Today, Yesterday, This Week, This Month, All Time, a picked date range or a picked month.
 * Everything is drawn with plain Swing (no extra libraries needed).
 */
public class SalesDashboard extends JPanel {

    private enum Range {
        TODAY("Today"), YESTERDAY("Yesterday"), WEEK("This Week"), MONTH("This Month"),
        ALL("All Time"), DATES("Pick Dates"), MONTH_PICK("Pick Month");
        final String label;
        Range(String label) { this.label = label; }
    }

    private static final Color[] PALETTE = {
            new Color(0x5A2530), new Color(0xC4956A), new Color(0x8D6E63),
            new Color(0xE0A458), new Color(0x7A9E7E), new Color(0xD9B99B)};
    private static final Color BAR = new Color(0xC4956A);
    private static final Color GRID = new Color(0xD9C3B0);
    private static final Color GOOD = new Color(0x2E7D32);
    private static final Color BAD = new Color(0xC62828);
    private static final DateTimeFormatter LONG_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");

    /** One line of the item ranking. */
    private static class Row {
        String name, category;
        int qty;
        double sales;
    }

    private Range range = Range.TODAY;
    private LocalDate pickFrom = LocalDate.now().minusDays(6);
    private LocalDate pickTo = LocalDate.now();
    private int pickMonth = LocalDate.now().getMonthValue();
    private int pickYear = LocalDate.now().getYear();
    private boolean adjusting;

    private List<Row> lastRows = new ArrayList<>();
    private double lastTotal;
    private LocalDate lastFrom, lastTo;

    // pickers
    private final JSpinner fromSpin = dateSpinner(pickFrom);
    private final JSpinner toSpin = dateSpinner(pickTo);
    private final JPanel datesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JComboBox<String> monthBox = new JComboBox<>();
    private final JSpinner yearSpin = new JSpinner(new SpinnerNumberModel(pickYear, 2000, 2100, 1));
    private final JPanel monthPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final JLabel periodLbl = Theme.label(" ", Font.BOLD, 15, Theme.ESPRESSO);

    // KPI cards
    private final JLabel totalVal = Theme.label("", Font.BOLD, 28, Theme.PRIMARY);
    private final JLabel totalNote = Theme.label(" ", Font.BOLD, 13, Theme.MOCHA);
    private final JLabel ordersVal = Theme.label("0", Font.BOLD, 28, Theme.PRIMARY);
    private final JLabel ordersNote = Theme.label(" ", Font.PLAIN, 13, Theme.MOCHA);
    private final JLabel avgVal = Theme.label("", Font.BOLD, 28, Theme.PRIMARY);
    private final JLabel avgNote = Theme.label("per paid order", Font.PLAIN, 13, Theme.MOCHA);
    private final JLabel bestVal = Theme.label("-", Font.BOLD, 20, Theme.PRIMARY);
    private final JLabel bestNote = Theme.label(" ", Font.PLAIN, 13, Theme.MOCHA);
    private final JLabel peakVal = Theme.label("-", Font.BOLD, 28, Theme.PRIMARY);
    private final JLabel peakNote = Theme.label(" ", Font.PLAIN, 13, Theme.MOCHA);

    // charts
    private final HBarChart bestChart = new HBarChart();
    private final DonutChart catChart = new DonutChart();
    private final DonutChart payChart = new DonutChart();
    private final ColumnChart trendChart = new ColumnChart();
    private final ColumnChart hourChart = new ColumnChart();
    private final JLabel trendTitle = Theme.label("Daily Sales, Last 7 Days", Font.BOLD, 17, Theme.PRIMARY);

    // ranking table
    private final DefaultTableModel rankModel = new DefaultTableModel(
            new String[]{"Rank", "Item", "Category", "Qty Sold", "Sales", "% of Sales"}, 0) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable rankTable = new JTable(rankModel);
    private JPanel rankCard;

    public SalesDashboard() {
        setLayout(new BorderLayout(0, 10));
        setOpaque(false);

        // ---- top bar, row 1: period buttons + export
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setOpaque(false);
        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        chips.setOpaque(false);
        ButtonGroup g = new ButtonGroup();
        for (Range r : Range.values()) {
            Theme.Chip c = new Theme.Chip(r.label);
            c.setFont(Theme.font(Font.BOLD, 15));
            c.setSelected(r == range);
            c.addActionListener(e -> { range = r; updatePickers(); refresh(); });
            g.add(c);
            chips.add(c);
        }
        topBar.add(chips, BorderLayout.WEST);
        Theme.Btn export = Theme.Btn.outline("Export CSV");
        export.addActionListener(e -> exportCsv());
        topBar.add(export, BorderLayout.EAST);

        // ---- row 2: date pickers (only for Pick Dates / Pick Month) + what is being shown
        fromSpin.setToolTipText("Click the month, day or year, then use the arrows");
        toSpin.setToolTipText("Click the month, day or year, then use the arrows");
        datesPanel.setOpaque(false);
        datesPanel.add(Theme.label("From", Font.BOLD, 15, Theme.ESPRESSO));
        datesPanel.add(fromSpin);
        datesPanel.add(Theme.label("To", Font.BOLD, 15, Theme.ESPRESSO));
        datesPanel.add(toSpin);
        fromSpin.addChangeListener(e -> datesChanged(true));
        toSpin.addChangeListener(e -> datesChanged(false));

        for (Month m : Month.values()) monthBox.addItem(m.getDisplayName(TextStyle.FULL, Locale.ENGLISH));
        monthBox.setSelectedIndex(pickMonth - 1);
        monthBox.setFont(Theme.font(Font.PLAIN, 15));
        yearSpin.setFont(Theme.font(Font.PLAIN, 15));
        yearSpin.setEditor(new JSpinner.NumberEditor(yearSpin, "#"));
        monthPanel.setOpaque(false);
        monthPanel.add(Theme.label("Month", Font.BOLD, 15, Theme.ESPRESSO));
        monthPanel.add(monthBox);
        monthPanel.add(yearSpin);
        monthBox.addActionListener(e -> monthChanged());
        yearSpin.addChangeListener(e -> monthChanged());

        JPanel pickRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 0));
        pickRow.setOpaque(false);
        pickRow.add(datesPanel);
        pickRow.add(monthPanel);
        pickRow.add(periodLbl);
        updatePickers();

        JPanel top = new JPanel(new BorderLayout(0, 8));
        top.setOpaque(false);
        top.add(topBar, BorderLayout.NORTH);
        top.add(pickRow, BorderLayout.CENTER);
        add(top, BorderLayout.NORTH);

        // ---- scrollable content
        ScrollPanel content = new ScrollPanel(new GridBagLayout());
        content.setBackground(Theme.CREAM);
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.weightx = 1;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 12, 0);
        int y = 0;

        JPanel kpis = new JPanel(new GridLayout(1, 5, 12, 0));
        kpis.setOpaque(false);
        kpis.add(kpi("Total Sales", totalVal, totalNote));
        kpis.add(kpi("Paid Orders", ordersVal, ordersNote));
        kpis.add(kpi("Average Order", avgVal, avgNote));
        kpis.add(kpi("Best Seller", bestVal, bestNote));
        kpis.add(kpi("Busiest Hour", peakVal, peakNote));
        gc.gridy = y++;
        content.add(kpis, gc);

        JPanel row2 = new JPanel(new GridLayout(1, 3, 12, 0));
        row2.setOpaque(false);
        row2.setPreferredSize(new Dimension(10, 270));
        row2.add(card("Best Sellers (% of sales)", bestChart));
        row2.add(card("Sales by Category", catChart));
        row2.add(card("Sales by Payment Method", payChart));
        gc.gridy = y++;
        content.add(row2, gc);

        JPanel row3 = new JPanel(new GridLayout(1, 2, 12, 0));
        row3.setOpaque(false);
        row3.setPreferredSize(new Dimension(10, 260));
        row3.add(card(trendTitle, trendChart));
        row3.add(card("Busiest Hours", hourChart));
        gc.gridy = y++;
        content.add(row3, gc);

        Theme.styleTable(rankTable);
        rankTable.getColumnModel().getColumn(0).setMaxWidth(70);
        JPanel tableBox = new JPanel(new BorderLayout());
        tableBox.setOpaque(false);
        tableBox.add(rankTable.getTableHeader(), BorderLayout.NORTH);
        tableBox.add(rankTable, BorderLayout.CENTER);
        rankCard = card("Item Ranking", tableBox);
        gc.gridy = y++;
        content.add(rankCard, gc);

        gc.gridy = y;
        gc.weighty = 1;
        JPanel filler = new JPanel();
        filler.setOpaque(false);
        content.add(filler, gc);

        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(null);
        sp.getViewport().setBackground(Theme.CREAM);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        add(sp, BorderLayout.CENTER);

        refresh();
        new Timer(3000, e -> { if (isShowing()) refresh(); }).start();
    }

    // ------------------------------------------------------------ pickers
    private static JSpinner dateSpinner(LocalDate d) {
        JSpinner s = new JSpinner(new SpinnerDateModel(toDate(d), null, null, Calendar.DAY_OF_MONTH));
        s.setEditor(new JSpinner.DateEditor(s, "MMM dd, yyyy"));
        s.setFont(Theme.font(Font.PLAIN, 15));
        return s;
    }

    private static Date toDate(LocalDate d) { return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant()); }

    private static LocalDate toLocal(Date d) { return LocalDate.ofInstant(d.toInstant(), ZoneId.systemDefault()); }

    private void updatePickers() {
        datesPanel.setVisible(range == Range.DATES);
        monthPanel.setVisible(range == Range.MONTH_PICK);
        revalidate();
        repaint();
    }

    /** From / To changed. If the dates cross each other, the other one is moved along. */
    private void datesChanged(boolean fromChanged) {
        if (adjusting) return;
        adjusting = true;
        try {
            pickFrom = toLocal((Date) fromSpin.getValue());
            pickTo = toLocal((Date) toSpin.getValue());
            if (pickFrom.isAfter(pickTo)) {
                if (fromChanged) { pickTo = pickFrom; toSpin.setValue(toDate(pickTo)); }
                else { pickFrom = pickTo; fromSpin.setValue(toDate(pickFrom)); }
            }
        } finally {
            adjusting = false;
        }
        refresh();
    }

    private void monthChanged() {
        pickMonth = monthBox.getSelectedIndex() + 1;
        pickYear = (Integer) yearSpin.getValue();
        refresh();
    }

    // ------------------------------------------------------------ building blocks
    private JPanel kpi(String title, JLabel value, JLabel note) {
        JPanel c = new JPanel(new GridLayout(3, 1, 0, 2));
        c.setBackground(Theme.CREAM);
        c.setBorder(new CompoundBorder(new LineBorder(Theme.LATTE, 2, true), new EmptyBorder(12, 16, 12, 16)));
        c.add(Theme.label(title, Font.PLAIN, 15, Theme.MOCHA));
        c.add(value);
        c.add(note);
        return c;
    }

    private JPanel card(String title, JComponent body) {
        return card(Theme.label(title, Font.BOLD, 17, Theme.PRIMARY), body);
    }

    private JPanel card(JLabel title, JComponent body) {
        JPanel c = new JPanel(new BorderLayout(0, 6));
        c.setBackground(Theme.CREAM);
        c.setBorder(new CompoundBorder(new LineBorder(Theme.LATTE, 2, true), new EmptyBorder(10, 14, 10, 14)));
        c.add(title, BorderLayout.NORTH);
        c.add(body, BorderLayout.CENTER);
        return c;
    }

    // ------------------------------------------------------------ data
    /** First and last day of the selected period ({null, null} for all time). */
    private LocalDate[] window(LocalDate today) {
        switch (range) {
            case TODAY: return new LocalDate[]{today, today};
            case YESTERDAY: return new LocalDate[]{today.minusDays(1), today.minusDays(1)};
            case WEEK: return new LocalDate[]{today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), today};
            case MONTH: return new LocalDate[]{today.withDayOfMonth(1), today};
            case DATES: return new LocalDate[]{pickFrom, pickTo};
            case MONTH_PICK: {
                YearMonth ym = YearMonth.of(pickYear, pickMonth);
                return new LocalDate[]{ym.atDay(1), ym.atEndOfMonth()};
            }
            default: return new LocalDate[]{null, null};
        }
    }

    private static double sumPaid(List<Order> paid, LocalDate from, LocalDate to) {
        double t = 0;
        for (Order o : paid) {
            LocalDate d = o.getOrderDate().toLocalDate();
            if (!d.isBefore(from) && !d.isAfter(to)) t += o.getTotalAmount();
        }
        return t;
    }

    private static String periodText(LocalDate from, LocalDate to) {
        if (from == null) return "All recorded orders";
        if (from.equals(to)) return from.format(LONG_DATE);
        return from.format(LONG_DATE) + " to " + to.format(LONG_DATE);
    }

    private static String pct(double part, double total) {
        return total <= 0 ? "0%" : String.format("%.0f%%", part / total * 100);
    }

    private static String hourShort(int h) { return (h % 12 == 0 ? 12 : h % 12) + (h < 12 ? "a" : "p"); }

    private static String hourLong(int h) { return (h % 12 == 0 ? 12 : h % 12) + (h < 12 ? " AM" : " PM"); }

    private static String shortPeso(double v) {
        if (v >= 10000) return String.format("₱%.0fk", v / 1000);
        if (v >= 1000) return String.format("₱%.1fk", v / 1000);
        return "₱" + Math.round(v);
    }

    /** Reads the orders again and redraws everything. */
    public void refresh() {
        LocalDate today = LocalDate.now();
        LocalDate[] w = window(today);
        LocalDate from = w[0], to = w[1];
        lastFrom = from;
        lastTo = to;
        periodLbl.setText("Showing: " + periodText(from, to));

        List<Order> allPaid = new ArrayList<>();
        List<Order> paid = new ArrayList<>();
        int unpaid = 0;
        for (Order o : DataStore.get().getOrders()) {
            LocalDate d = o.getOrderDate().toLocalDate();
            boolean inRange = (from == null || !d.isBefore(from)) && (to == null || !d.isAfter(to));
            if (o.getPayment().isConfirmed()) {
                allPaid.add(o);
                if (inRange) paid.add(o);
            } else if (inRange) {
                unpaid++;
            }
        }

        // ---- totals per item / category / payment / hour
        Map<String, double[]> items = new LinkedHashMap<>(); // name -> {qty, sales}
        Map<String, Double> cats = new LinkedHashMap<>();
        Map<String, Double> pays = new LinkedHashMap<>();
        double[] hours = new double[24];
        double total = 0;
        for (Order o : paid) {
            total += o.getTotalAmount();
            String method = o.getPayment().getMethod() == null ? "Unknown" : o.getPayment().getMethod();
            pays.merge(method, o.getTotalAmount(), Double::sum);
            hours[o.getOrderDate().getHour()] += o.getTotalAmount();
            for (OrderItem oi : o.getItems()) {
                double sub = oi.calculateSubtotal();
                double[] a = items.computeIfAbsent(oi.getItem().getName(), k -> new double[2]);
                a[0] += oi.getQuantity();
                a[1] += sub;
                cats.merge(oi.getItem().getCategory(), sub, Double::sum);
            }
        }

        // ---- ranking (every menu item, so the ones that do not sell show up at the bottom)
        List<Row> rows = new ArrayList<>();
        for (MenuItem m : DataStore.get().getMenu()) {
            Row r = new Row();
            r.name = m.getName();
            r.category = m.getCategory();
            double[] a = items.remove(m.getName());
            if (a != null) { r.qty = (int) a[0]; r.sales = a[1]; }
            rows.add(r);
        }
        // items that were sold earlier but are no longer on the menu still count
        for (Map.Entry<String, double[]> e : items.entrySet()) {
            Row r = new Row();
            r.name = e.getKey();
            r.category = "(removed)";
            r.qty = (int) e.getValue()[0];
            r.sales = e.getValue()[1];
            rows.add(r);
        }
        rows.sort(Comparator.comparingInt((Row x) -> -x.qty).thenComparingDouble(x -> -x.sales));
        lastRows = rows;
        lastTotal = total;

        // ---- KPI cards
        totalVal.setText(Theme.peso(total));
        if (from == null) {
            totalNote.setText("all recorded orders");
            totalNote.setForeground(Theme.MOCHA);
        } else {
            long days = ChronoUnit.DAYS.between(from, to) + 1;
            LocalDate pf;
            String prevLabel;
            switch (range) {
                case WEEK: pf = from.minusWeeks(1); prevLabel = "last week"; break;
                case MONTH: pf = from.minusMonths(1); prevLabel = "last month"; break;
                case MONTH_PICK: pf = from.minusMonths(1); prevLabel = "previous month"; break;
                case DATES: pf = from.minusDays(days); prevLabel = "previous period"; break;
                case YESTERDAY: pf = from.minusDays(1); prevLabel = "the day before"; break;
                default: pf = from.minusDays(1); prevLabel = "yesterday"; break;
            }
            LocalDate pt = pf.plusDays(days - 1);
            if (range == Range.MONTH || range == Range.MONTH_PICK) {
                LocalDate end = pf.withDayOfMonth(pf.lengthOfMonth());
                if (pt.isAfter(end)) pt = end;
            }
            double prev = sumPaid(allPaid, pf, pt);
            if (prev <= 0) {
                totalNote.setText(total > 0 ? "no sales to compare" : "no sales yet");
                totalNote.setForeground(Theme.MOCHA);
            } else {
                double ch = (total - prev) / prev * 100;
                totalNote.setText(String.format("%+.0f%% vs %s", ch, prevLabel));
                totalNote.setForeground(ch >= 0 ? GOOD : BAD);
            }
        }

        ordersVal.setText(String.valueOf(paid.size()));
        ordersNote.setText(unpaid + " waiting for payment");
        avgVal.setText(Theme.peso(paid.isEmpty() ? 0 : total / paid.size()));

        Row best = !rows.isEmpty() && rows.get(0).qty > 0 ? rows.get(0) : null;
        bestVal.setText(best == null ? "-" : best.name);
        bestNote.setText(best == null ? " " : best.qty + " sold, " + pct(best.sales, total) + " of sales");

        int peak = -1;
        for (int h = 0; h < 24; h++) if (hours[h] > 0 && (peak < 0 || hours[h] > hours[peak])) peak = h;
        peakVal.setText(peak < 0 ? "-" : hourLong(peak));
        peakNote.setText(peak < 0 ? " " : Theme.peso(hours[peak]) + " in sales");

        // ---- best sellers chart
        List<String> bl = new ArrayList<>(), bn = new ArrayList<>();
        List<Double> bv = new ArrayList<>();
        for (Row r : rows) {
            if (r.qty <= 0 || bl.size() >= 5) break;
            bl.add(r.name);
            bv.add((double) r.qty);
            bn.add(r.qty + " sold (" + pct(r.sales, total) + ")");
        }
        bestChart.set(bl, bv, bn);

        catChart.set(cats, Theme.peso(total));
        payChart.set(pays, Theme.peso(total));

        // ---- busiest hours chart
        int lo = 24, hi = -1;
        for (int h = 0; h < 24; h++) if (hours[h] > 0) { lo = Math.min(lo, h); hi = Math.max(hi, h); }
        if (hi < 0) { lo = 8; hi = 18; }
        lo = Math.max(0, lo - 1);
        hi = Math.min(23, hi + 1);
        while (hi - lo < 5) { if (hi < 23) hi++; if (lo > 0) lo--; }
        List<String> hl = new ArrayList<>();
        double[] hv = new double[hi - lo + 1];
        for (int h = lo; h <= hi; h++) { hl.add(hourShort(h)); hv[h - lo] = hours[h]; }
        hourChart.set(hl, hv, peak < 0 ? -1 : peak - lo);

        // ---- trend chart (uses every paid order, so it shows the days around the selected period too)
        LocalDate tFrom, tTo = today;
        String title;
        switch (range) {
            case MONTH: tFrom = from; title = "Daily Sales This Month"; break;
            case MONTH_PICK: tFrom = from; tTo = to; title = "Daily Sales, " + from.format(DateTimeFormatter.ofPattern("MMMM yyyy")); break;
            case DATES: tFrom = from; tTo = to; title = "Daily Sales, " + periodText(from, to); break;
            case ALL: tFrom = today.minusDays(29); title = "Daily Sales, Last 30 Days"; break;
            case YESTERDAY: tTo = today.minusDays(1); tFrom = tTo.minusDays(6); title = "Daily Sales, 7 Days Up to Yesterday"; break;
            default: tFrom = today.minusDays(6); title = "Daily Sales, Last 7 Days"; break;
        }
        boolean monthly = range == Range.DATES && ChronoUnit.DAYS.between(tFrom, tTo) > 92;
        List<String> dl = new ArrayList<>();
        double[] dv;
        int hlIdx;
        if (monthly) {
            title = "Monthly Sales, " + periodText(from, to);
            YearMonth a = YearMonth.from(tFrom), b = YearMonth.from(tTo);
            int n = (int) ChronoUnit.MONTHS.between(a, b) + 1;
            dv = new double[n];
            for (Order o : allPaid) {
                long idx = ChronoUnit.MONTHS.between(a, YearMonth.from(o.getOrderDate()));
                if (idx >= 0 && idx < n) dv[(int) idx] += o.getTotalAmount();
            }
            DateTimeFormatter mf = DateTimeFormatter.ofPattern("MMM yy");
            for (int i = 0; i < n; i++) dl.add(a.plusMonths(i).format(mf));
            long h = ChronoUnit.MONTHS.between(a, YearMonth.from(today));
            hlIdx = h >= 0 && h < n ? (int) h : -1;
        } else {
            int n = (int) ChronoUnit.DAYS.between(tFrom, tTo) + 1;
            dv = new double[n];
            for (Order o : allPaid) {
                long idx = ChronoUnit.DAYS.between(tFrom, o.getOrderDate().toLocalDate());
                if (idx >= 0 && idx < n) dv[(int) idx] += o.getTotalAmount();
            }
            DateTimeFormatter df = DateTimeFormatter.ofPattern(n <= 7 ? "EEE d" : n <= 31 ? "d" : "MMM d");
            for (int i = 0; i < n; i++) dl.add(tFrom.plusDays(i).format(df));
            LocalDate hlDay = range == Range.YESTERDAY ? today.minusDays(1) : today;
            hlIdx = !hlDay.isBefore(tFrom) && !hlDay.isAfter(tTo) ? (int) ChronoUnit.DAYS.between(tFrom, hlDay) : -1;
        }
        trendTitle.setText(title);
        trendChart.set(dl, dv, hlIdx);

        // ---- ranking table
        rankModel.setRowCount(0);
        int rank = 1;
        for (Row r : rows) {
            rankModel.addRow(new Object[]{rank++, r.name, r.category, r.qty, Theme.peso(r.sales), pct(r.sales, total)});
        }
        rankCard.setPreferredSize(new Dimension(10, 76 + (rows.size() + 1) * rankTable.getRowHeight()));
        rankCard.revalidate();
    }

    // ------------------------------------------------------------ export
    private static String csv(String s) { return "\"" + s.replace("\"", "\"\"") + "\""; }

    private void exportCsv() {
        JFileChooser fc = new JFileChooser();
        String stamp = lastFrom == null ? "all_time" : lastFrom.equals(lastTo) ? lastFrom.toString() : lastFrom + "_to_" + lastTo;
        fc.setSelectedFile(new File("sipsync_sales_" + stamp + ".csv"));
        if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        File f = fc.getSelectedFile();
        if (!f.getName().toLowerCase().endsWith(".csv")) f = new File(f.getParentFile(), f.getName() + ".csv");
        try (PrintWriter w = new PrintWriter(f, StandardCharsets.UTF_8)) {
            w.println("SipSync Cafe - Sales Report");
            w.println("Period," + csv(periodText(lastFrom, lastTo)));
            w.println("Generated," + csv(LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy hh:mm a"))));
            w.println("Total sales (PHP)," + String.format(Locale.US, "%.2f", lastTotal));
            w.println();
            w.println("Rank,Item,Category,Qty Sold,Sales (PHP),% of Sales");
            int rank = 1;
            for (Row r : lastRows) {
                w.println(rank++ + "," + csv(r.name) + "," + csv(r.category) + "," + r.qty + ","
                        + String.format(Locale.US, "%.2f", r.sales) + ","
                        + String.format(Locale.US, "%.1f", lastTotal <= 0 ? 0 : r.sales / lastTotal * 100));
            }
            Theme.info(this, "Report saved", "Saved to " + f.getName());
        } catch (IOException ex) {
            Theme.info(this, "Could not save", "The report could not be saved. Try a different folder.");
        }
    }

    // ------------------------------------------------------------ helper panel
    /** A panel that always matches the width of the scroll pane, so the cards stretch. */
    private static class ScrollPanel extends JPanel implements Scrollable {
        ScrollPanel(LayoutManager lm) { super(lm); }
        @Override public Dimension getPreferredScrollableViewportSize() { return getPreferredSize(); }
        @Override public int getScrollableUnitIncrement(Rectangle r, int o, int d) { return 20; }
        @Override public int getScrollableBlockIncrement(Rectangle r, int o, int d) { return 120; }
        @Override public boolean getScrollableTracksViewportWidth() { return true; }
        @Override public boolean getScrollableTracksViewportHeight() { return false; }
    }

    // ------------------------------------------------------------ charts
    private static abstract class Chart extends JComponent {
        protected boolean empty = true;

        @Override protected void paintComponent(Graphics g0) {
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (empty) {
                g.setFont(Theme.font(Font.ITALIC, 15));
                g.setColor(Theme.MOCHA);
                FontMetrics fm = g.getFontMetrics();
                String s = "No paid orders to show yet.";
                g.drawString(s, (getWidth() - fm.stringWidth(s)) / 2, getHeight() / 2);
            } else {
                draw(g, getWidth(), getHeight());
            }
            g.dispose();
        }

        abstract void draw(Graphics2D g, int w, int h);

        static int baseline(FontMetrics fm, int centerY) {
            return centerY - (fm.getAscent() + fm.getDescent()) / 2 + fm.getAscent();
        }

        static String fit(FontMetrics fm, String s, int maxW) {
            if (fm.stringWidth(s) <= maxW) return s;
            while (s.length() > 1 && fm.stringWidth(s + "...") > maxW) s = s.substring(0, s.length() - 1);
            return s + "...";
        }
    }

    /** Horizontal bars: name on the left, bar, then a note such as "12 sold (18%)". */
    private static class HBarChart extends Chart {
        private List<String> labels = new ArrayList<>(), notes = new ArrayList<>();
        private List<Double> values = new ArrayList<>();

        void set(List<String> l, List<Double> v, List<String> n) {
            labels = l; values = v; notes = n;
            empty = l.isEmpty();
            repaint();
        }

        @Override void draw(Graphics2D g, int w, int h) {
            int n = labels.size();
            int rowH = Math.min(46, h / Math.max(n, 1));
            double max = 0;
            for (double v : values) max = Math.max(max, v);
            int labelW = Math.min(140, w / 3);
            int barX = labelW + 8;
            int barMax = Math.max(20, w - barX - 110);
            int barH = Math.max(8, Math.min(22, rowH - 10));
            for (int i = 0; i < n; i++) {
                int cy = 4 + i * rowH + rowH / 2;
                g.setFont(Theme.font(Font.PLAIN, 14));
                g.setColor(Theme.ESPRESSO);
                FontMetrics fm = g.getFontMetrics();
                g.drawString(fit(fm, labels.get(i), labelW), 0, baseline(fm, cy));
                int bw = (int) Math.max(4, values.get(i) / max * barMax);
                g.setColor(PALETTE[i % PALETTE.length]);
                g.fillRoundRect(barX, cy - barH / 2, bw, barH, 8, 8);
                g.setFont(Theme.font(Font.BOLD, 13));
                g.setColor(Theme.ESPRESSO);
                g.drawString(notes.get(i), barX + bw + 8, baseline(g.getFontMetrics(), cy));
            }
        }
    }

    /** Donut with a legend (name + percent) on the right. */
    private static class DonutChart extends Chart {
        private final List<String> labels = new ArrayList<>();
        private final List<Double> values = new ArrayList<>();
        private double total;
        private String center = "";

        void set(Map<String, Double> data, String center) {
            labels.clear();
            values.clear();
            total = 0;
            this.center = center;
            List<Map.Entry<String, Double>> es = new ArrayList<>(data.entrySet());
            es.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            for (Map.Entry<String, Double> e : es) {
                if (e.getValue() > 0) { labels.add(e.getKey()); values.add(e.getValue()); total += e.getValue(); }
            }
            empty = labels.isEmpty();
            repaint();
        }

        @Override void draw(Graphics2D g, int w, int h) {
            int d = Math.max(40, Math.min(h - 8, w / 2));
            int x = 4, y = (h - d) / 2;
            double start = 90;
            for (int i = 0; i < labels.size(); i++) {
                double ext = -values.get(i) / total * 360;
                g.setColor(PALETTE[i % PALETTE.length]);
                g.fill(new Arc2D.Double(x, y, d, d, start, ext, Arc2D.PIE));
                start += ext;
            }
            double hd = d * 0.58;
            g.setColor(Theme.CREAM);
            g.fill(new Ellipse2D.Double(x + (d - hd) / 2, y + (d - hd) / 2, hd, hd));

            g.setFont(Theme.font(Font.BOLD, 15));
            g.setColor(Theme.PRIMARY);
            FontMetrics fm = g.getFontMetrics();
            String c = fit(fm, center, (int) hd - 6);
            g.drawString(c, x + (d - fm.stringWidth(c)) / 2, baseline(fm, y + d / 2));

            int lx = x + d + 18, rowH = 24, n = labels.size();
            int ly = Math.max(0, (h - n * rowH) / 2);
            g.setFont(Theme.font(Font.PLAIN, 14));
            fm = g.getFontMetrics();
            for (int i = 0; i < n; i++) {
                int cy = ly + i * rowH + rowH / 2;
                g.setColor(PALETTE[i % PALETTE.length]);
                g.fillRoundRect(lx, cy - 6, 12, 12, 4, 4);
                g.setColor(Theme.ESPRESSO);
                g.drawString(fit(fm, labels.get(i), w - lx - 20 - 48), lx + 20, baseline(fm, cy));
                String p = pct(values.get(i), total);
                g.drawString(p, w - 4 - fm.stringWidth(p), baseline(fm, cy));
            }
        }
    }

    /** Vertical columns with the value on top and a label underneath. */
    private static class ColumnChart extends Chart {
        private String[] labels = new String[0];
        private double[] values = new double[0];
        private int highlight = -1;

        void set(List<String> l, double[] v, int hi) {
            labels = l.toArray(new String[0]);
            values = v;
            highlight = hi;
            double sum = 0;
            for (double x : v) sum += x;
            empty = sum <= 0;
            repaint();
        }

        @Override void draw(Graphics2D g, int w, int h) {
            int n = labels.length;
            int top = 22, bottom = 24, left = 4, right = 4;
            int plotH = h - top - bottom;
            double max = 0;
            for (double v : values) max = Math.max(max, v);
            double slot = (double) (w - left - right) / n;
            int bw = (int) Math.max(3, Math.min(48, slot * 0.62));
            int base = top + plotH;

            g.setColor(GRID);
            g.drawLine(left, base, w - right, base);
            g.setFont(Theme.font(Font.PLAIN, 12));
            FontMetrics fm = g.getFontMetrics();
            int maxLabel = 0;
            for (String s : labels) maxLabel = Math.max(maxLabel, fm.stringWidth(s));
            int step = Math.max(1, (int) Math.ceil((maxLabel + 6) / slot));

            for (int i = 0; i < n; i++) {
                double cx = left + slot * i + slot / 2;
                int bh = (int) (values[i] / max * plotH);
                if (values[i] > 0 && bh < 2) bh = 2;
                g.setColor(i == highlight ? Theme.PRIMARY : BAR);
                g.fillRoundRect((int) (cx - bw / 2.0), base - bh, bw, bh, 6, 6);
                if (values[i] > 0) {
                    String t = shortPeso(values[i]);
                    if (fm.stringWidth(t) + 4 <= slot) {
                        g.setColor(Theme.ESPRESSO);
                        g.drawString(t, (int) (cx - fm.stringWidth(t) / 2.0), base - bh - 4);
                    }
                }
                if (i % step == 0) {
                    g.setColor(Theme.MOCHA);
                    g.drawString(labels[i], (int) (cx - fm.stringWidth(labels[i]) / 2.0), base + 16);
                }
            }
        }
    }
}