package gui;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/** FCFS scheduler view: process table, ready queue summary and Gantt chart, fed by the Core's PROCS message. */
public final class SchedulerPanel extends JPanel {

    private static final Color[] PID_COLORS = {
        new Color(16, 139, 151), new Color(214, 122, 47), new Color(98, 94, 190),
        new Color(41, 135, 91), new Color(190, 72, 105), new Color(120, 144, 156)
    };
    private static final Color IDLE = new Color(205, 213, 222);
    private static final Color INK = new Color(37, 55, 75);

    private final DefaultTableModel model = new DefaultTableModel(
            new String[]{"PID", "Name", "State", "Arr", "Exec", "Start", "Finish", "Wait", "TAT"}, 0) {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    private final JLabel summary = new JLabel("Scheduler: waiting for Core");
    private final JLabel averages = new JLabel(" ");
    private final GanttPanel gantt = new GanttPanel();
    private final Map<Integer, String> names = new java.util.HashMap<>();

    public SchedulerPanel() {
        super(new BorderLayout(4, 4));
        setOpaque(false);

        JTable table = new JTable(model);
        table.setFont(new Font("Consolas", Font.PLAIN, 12));
        table.setRowHeight(18);
        table.setFillsViewportHeight(true);
        table.setEnabled(false);
        int[] widths = {32, 42, 90, 36, 40, 44, 50, 40, 36};
        for (int i = 0; i < widths.length; i++) table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);

        JPanel top = new JPanel(new GridLayout(2, 1, 0, 1));
        top.setOpaque(false);
        summary.setFont(new Font("Segoe UI", Font.BOLD, 12));
        summary.setForeground(INK);
        averages.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        averages.setForeground(new Color(105, 123, 143));
        top.add(summary);
        top.add(averages);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setPreferredSize(new Dimension(300, 80));
        gantt.setPreferredSize(new Dimension(300, 44));

        add(top, BorderLayout.NORTH);
        add(tableScroll, BorderLayout.CENTER);
        add(gantt, BorderLayout.SOUTH);
    }

    /** Applies a PROCS message (already parsed into key/value fields). */
    public void update(Map<String, String> f) {
        String running = f.getOrDefault("RUNNING_PNAME", "-");
        String ready = f.getOrDefault("READY", "");
        summary.setText(f.getOrDefault("ALGO", "?") + "   t=" + f.getOrDefault("CLOCK", "0")
                + "   Running: " + running + "   Ready: " + (ready.isEmpty() ? "(empty)" : ready.replace(",", " > ")));
        averages.setText("Context switches: " + f.getOrDefault("CTX", "0")
                + "   Avg wait: " + f.getOrDefault("AVG_WAIT", "0")
                + "   Avg turnaround: " + f.getOrDefault("AVG_TAT", "0"));

        model.setRowCount(0);
        names.clear();
        String table = f.getOrDefault("TABLE", "");
        if (!table.isEmpty()) {
            for (String row : table.split(";")) {
                String[] c = row.split(":");
                if (c.length < 9) continue;
                names.put(parseInt(c[0]), c[1]);
                model.addRow(new Object[]{c[0], c[1], c[2], c[3], c[4], c[5], c[6], c[7], c[8]});
            }
        }

        List<int[]> segments = new ArrayList<>();
        String g = f.getOrDefault("GANTT", "");
        if (!g.isEmpty()) {
            for (String seg : g.split(";")) {
                String[] c = seg.split(":");
                if (c.length == 3) segments.add(new int[]{parseInt(c[0]), parseInt(c[1]), parseInt(c[2])});
            }
        }
        gantt.setSegments(segments);
    }

    public void clear() {
        model.setRowCount(0);
        gantt.setSegments(new ArrayList<>());
        summary.setText("Scheduler: waiting for Core");
        averages.setText(" ");
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return 0; }
    }

    /** Horizontal Gantt chart: one coloured bar per executed segment, grey for CPU idle. */
    private final class GanttPanel extends JPanel {
        private List<int[]> segments = new ArrayList<>();

        GanttPanel() { setBackground(new Color(249, 251, 253)); setBorder(BorderFactory.createLineBorder(new Color(211, 221, 231))); }

        void setSegments(List<int[]> segments) { this.segments = segments; repaint(); }

        @Override protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth() - 12, barY = 6, barH = 20;
            g.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            if (segments.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("Gantt chart: no execution yet", 8, 20);
                return;
            }
            int total = segments.get(segments.size() - 1)[2];
            double scale = (double) w / Math.max(total, 1);
            for (int[] s : segments) {
                int x = 6 + (int) Math.round(s[1] * scale);
                int width = Math.max(1, (int) Math.round((s[2] - s[1]) * scale));
                g.setColor(s[0] == 0 ? IDLE : PID_COLORS[(s[0] - 1) % PID_COLORS.length]);
                g.fillRect(x, barY, width, barH);
                g.setColor(Color.WHITE);
                g.drawRect(x, barY, width, barH);
                String label = s[0] == 0 ? "idle" : names.getOrDefault(s[0], "P" + s[0]);
                if (g.getFontMetrics().stringWidth(label) < width - 2) {
                    g.setColor(s[0] == 0 ? INK : Color.WHITE);
                    g.drawString(label, x + 3, barY + 14);
                }
                g.setColor(INK);
                g.drawString(String.valueOf(s[1]), x, barY + barH + 11);
            }
            g.setColor(INK);
            g.drawString(String.valueOf(total), 6 + w - 8, barY + barH + 11);
        }
    }
}
