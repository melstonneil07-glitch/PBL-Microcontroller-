package gui;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.*;

/**
 * 3D visualization of the Nuvoton MS51FB9AE (8051 1T core, 24 MHz, TSSOP-20).
 * Pure Java2D (no external libraries), so it drops into any Swing simulator.
 *
 * Package geometry (TSSOP-20): body ~6.5 x 4.4 mm, 10 pins per long side, 0.65 mm pitch.
 * Pin 1 is at the bottom-left of the top view, numbering runs counter-clockwise.
 *
 * Mouse:  drag = rotate | wheel = zoom | double-click = toggle auto-rotate | hover = pin label
 *
 * Simulator hooks:
 *   chip.setInstruction("MOV A,#55H");   // text shown on the HUD
 *   chip.pulse();                        // flash the package after each STEP / RUN tick
 *   chip.setPortValue(0, p0Value);       // light pins according to port latch (needs mapping below)
 *   chip.setPinLevel(7, true);           // or drive a single pin directly
 *   chip.highlightPin(3, 600);           // orange glow for 600 ms
 *   chip.reset();                        // RESET button
 */
public class Chip3DPanel extends JPanel {

    private final String[] pinLabel = new String[21];
    private final boolean[] pinHigh = new boolean[21];
    private final int[][] portPin = new int[4][8];

    private int highlightPin = -1;
    private long highlightUntil = 0;

    private double yaw = Math.toRadians(-32), pitch = Math.toRadians(30), zoom = 1.0;
    private boolean autoRotate = true, dragging = false;
    private int lastX, lastY, hoverPin = -1;
    private float activity = 0f;
    private String instruction = "None";
    private final Timer timer;

    private static final class Box {
        double cx, cy, cz, sx, sy, sz;
        int pin;
        int kind;
        Color color;

        Box(double cx, double cy, double cz,
            double sx, double sy, double sz,
            int pin, int kind, Color c) {

            this.cx = cx;
            this.cy = cy;
            this.cz = cz;
            this.sx = sx;
            this.sy = sy;
            this.sz = sz;
            this.pin = pin;
            this.kind = kind;
            this.color = c;
        }
    }

    private static final class Face {
        Path2D.Double path = new Path2D.Double();
        double depth;
        Color fill;
        int pin;
        boolean bodyTop;
    }

    private static final int[][] FACE_IDX = {
            {1,3,7,5},
            {0,4,6,2},
            {2,3,7,6},
            {0,1,5,4},
            {4,5,7,6},
            {0,1,3,2}
    };

    private static final double[][] FACE_N = {
            {1,0,0},
            {-1,0,0},
            {0,1,0},
            {0,-1,0},
            {0,0,1},
            {0,0,-1}
    };

    private static final double BODY_L = 6.5;
    private static final double BODY_W = 4.4;
    private static final double BODY_T = 1.0;
    private static final double PITCH = 0.65;

    private static final double BODY_TOP = 0.6;
    private static final double BODY_Y = 0.1;

    private final List<Box> boxes = new ArrayList<>();
    private List<Face> lastFaces = new ArrayList<>();

    @SuppressWarnings("OverridableMethodCallInConstructor")
    public Chip3DPanel() {

        setPreferredSize(new Dimension(640, 460));
        setBackground(new Color(0x10141C));

        for (int i = 1; i <= 20; i++) {
            pinLabel[i] = "Pin " + i;
        }

        pinLabel[1] = "P0.5 / AIN4 / T0 / IC6 / PWM2";
        pinLabel[2] = "P0.6 / AIN3 / TXD";
        pinLabel[3] = "P0.7 / AIN2 / RXD";

        for (int[] row : portPin) {
            java.util.Arrays.fill(row, 0);
        }

        mapPortBit(0, 5, 1);
        mapPortBit(0, 6, 2);
        mapPortBit(0, 7, 3);

        buildModel();
        installMouse();

        timer = new Timer(30, e -> {

            if (autoRotate && !dragging) {
                yaw += 0.008;
            }

            if (activity > 0) {
                activity = Math.max(0f, activity - 0.04f);
            }

            if (highlightPin > 0 &&
                    System.currentTimeMillis() > highlightUntil) {

                highlightPin = -1;
            }

            repaint();
        });

        timer.start();
    }

    public void setPinLabel(int pin, String label) {

        if (pin >= 1 && pin <= 20) {
            pinLabel[pin] = label;
        }
    }

    public void setPinLevel(int pin, boolean high) {

        if (pin >= 1 && pin <= 20) {
            pinHigh[pin] = high;
        }
    }

    public void mapPortBit(int port, int bit, int pin) {

        portPin[port][bit] = pin;
    }

    public void setPortValue(int port, int value) {

        for (int b = 0; b < 8; b++) {

            int p = portPin[port][b];

            if (p > 0) {
                pinHigh[p] =
                        ((value >> b) & 1) == 1;
            }
        }
    }

    public void highlightPin(int pin, int millis) {

        highlightPin = pin;
        highlightUntil =
                System.currentTimeMillis() + millis;
    }

    public void setInstruction(String text) {

        instruction =
                (text == null || text.isEmpty())
                        ? "None"
                        : text;
    }

    public void pulse() {

        activity = 1f;
    }

    public void reset() {

        java.util.Arrays.fill(
                pinHigh,
                false
        );

        highlightPin = -1;
        activity = 0f;
        instruction = "None";
    }

    public void stop() {

        timer.stop();
    }

    public static JFrame openWindow(
            Chip3DPanel panel) {

        JFrame f =
                new JFrame(
                        "MS51FB9AE - 3D View"
                );

        f.setDefaultCloseOperation(
                JFrame.DISPOSE_ON_CLOSE
        );

        f.add(panel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);

        return f;
    }

    private void buildModel() {

        Color pcb =
                new Color(0x0B5D2E);

        Color body =
                new Color(0x1E1F24);

        Color lead =
                new Color(0xC8CCD2);

        boxes.add(
                new Box(
                        0,
                        -0.72,
                        0,
                        9.4,
                        0.2,
                        6.8,
                        0,
                        0,
                        pcb
                )
        );

        boxes.add(
                new Box(
                        0,
                        BODY_Y,
                        0,
                        BODY_L,
                        BODY_T,
                        BODY_W,
                        0,
                        1,
                        body
                )
        );

        for (int i = 0; i < 10; i++) {

            double x =
                    (i - 4.5) * PITCH;

            addLead(
                    x,
                    +1,
                    i + 1,
                    lead
            );

            addLead(
                    -x,
                    -1,
                    20 - i,
                    lead
            );
        }
    }

    private void addLead(
            double x,
            int side,
            int pin,
            Color c) {

        boxes.add(
                new Box(
                        x,
                        0.0,
                        side * 2.7,
                        0.30,
                        0.16,
                        1.0,
                        pin,
                        2,
                        c
                )
        );

        boxes.add(
                new Box(
                        x,
                        -0.3,
                        side * 3.05,
                        0.30,
                        0.65,
                        0.18,
                        pin,
                        2,
                        c
                )
        );
    }

    private double[] rot(
            double x,
            double y,
            double z) {

        double cy = Math.cos(yaw);
        double sy = Math.sin(yaw);
        double cp = Math.cos(pitch);
        double sp = Math.sin(pitch);

        double x1 =
                x * cy + z * sy;

        double z1 =
                -x * sy + z * cy;

        double y2 =
                y * cp - z1 * sp;

        double z2 =
                y * sp + z1 * cp;

        return new double[]{
                x1,
                y2,
                z2
        };
    }

    private Point2D.Double proj(
            double[] r,
            double cx,
            double cy,
            double scale) {

        double persp =
                40.0 / (40.0 - r[2]);

        return new Point2D.Double(
                cx + r[0] * scale * persp,
                cy - r[1] * scale * persp
        );
    }

    private Color shade(
            Color c,
            double nz,
            double ny) {

        double light =
                0.42 +
                0.58 *
                Math.max(
                        0,
                        0.25 * ny +
                        0.75 * nz
                );

        return new Color(
                Math.min(
                        255,
                        (int)
                                (c.getRed() * light)
                ),
                Math.min(
                        255,
                        (int)
                                (c.getGreen() * light)
                ),
                Math.min(
                        255,
                        (int)
                                (c.getBlue() * light)
                )
        );
    }

    private List<Face> buildFaces(
            double cx,
            double cy,
            double scale) {

        List<Face> out =
                new ArrayList<>();

        long now =
                System.currentTimeMillis();

        for (Box b : boxes) {

            Color base = b.color;

            if (b.pin > 0) {

                if (b.pin == highlightPin &&
                        now < highlightUntil) {

                    base =
                            new Color(0xFF9F1C);

                } else if (b.pin == hoverPin) {

                    base =
                            new Color(0x4CC9F0);

                } else if (pinHigh[b.pin]) {

                    base =
                            new Color(0x3CDC64);
                }
            }

            double[][] corner =
                    new double[8][];

            for (int i = 0; i < 8; i++) {

                double x =
                        b.cx +
                        ((i & 1) == 0 ? -1 : 1)
                                * b.sx / 2;

                double y =
                        b.cy +
                        ((i & 2) == 0 ? -1 : 1)
                                * b.sy / 2;

                double z =
                        b.cz +
                        ((i & 4) == 0 ? -1 : 1)
                                * b.sz / 2;

                corner[i] =
                        rot(x, y, z);
            }

            for (int f = 0; f < 6; f++) {

                double[] n =
                        rot(
                                FACE_N[f][0],
                                FACE_N[f][1],
                                FACE_N[f][2]
                        );

                if (n[2] <= 0.0) {
                    continue;
                }

                Face face =
                        new Face();

                double d = 0;

                for (int k = 0; k < 4; k++) {

                    double[] r =
                            corner[
                                    FACE_IDX[f][k]
                            ];

                    Point2D.Double p =
                            proj(
                                    r,
                                    cx,
                                    cy,
                                    scale
                            );

                    if (k == 0) {
                        face.path.moveTo(
                                p.x,
                                p.y
                        );
                    } else {
                        face.path.lineTo(
                                p.x,
                                p.y
                        );
                    }

                    d += r[2];
                }

                face.path.closePath();

                face.depth =
                        d / 4;

                face.fill =
                        shade(
                                base,
                                n[2],
                                n[1]
                        );

                face.pin =
                        b.pin;

                face.bodyTop =
                        b.kind == 1 &&
                        f == 2;

                out.add(face);
            }
        }

        out.sort(
                Comparator.comparingDouble(
                        f -> f.depth
                )
        );

        return out;
    }

    @Override
    protected void paintComponent(
            Graphics g0) {

        super.paintComponent(g0);

        Graphics2D g =
                (Graphics2D)
                        g0.create();

        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );

        g.setRenderingHint(
                RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON
        );

        int w = getWidth();
        int h = getHeight();

        g.setPaint(
                new GradientPaint(
                        0,
                        0,
                        new Color(0x1B2735),
                        0,
                        h,
                        new Color(0x090A0F)
                )
        );

        g.fillRect(
                0,
                0,
                w,
                h
        );

        double scale =
                Math.min(
                        w / 10.0,
                        h / 7.5
                ) * zoom;

        double cx =
                w / 2.0;

        double cy =
                h / 2.0 +
                h * 0.03;

        lastFaces =
                buildFaces(
                        cx,
                        cy,
                        scale
                );

        for (Face f : lastFaces) {

            g.setColor(f.fill);
            g.fill(f.path);

            g.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            90
                    )
            );

            g.setStroke(
                    new BasicStroke(0.8f)
            );

            g.draw(f.path);

            if (f.bodyTop) {
                drawTopMarkings(
                        g,
                        cx,
                        cy,
                        scale
                );
            }
        }

        drawHud(
                g,
                w,
                h
        );

        g.dispose();
    }

    private void drawTopMarkings(
            Graphics2D g,
            double cx,
            double cy,
            double scale) {

        Point2D.Double o =
                proj(
                        rot(
                                0,
                                BODY_TOP,
                                0
                        ),
                        cx,
                        cy,
                        scale
                );

        Point2D.Double ex =
                proj(
                        rot(
                                1,
                                BODY_TOP,
                                0
                        ),
                        cx,
                        cy,
                        scale
                );

        Point2D.Double ez =
                proj(
                        rot(
                                0,
                                BODY_TOP,
                                1
                        ),
                        cx,
                        cy,
                        scale
                );

        AffineTransform at =
                new AffineTransform(
                        ex.x - o.x,
                        ex.y - o.y,
                        ez.x - o.x,
                        ez.y - o.y,
                        o.x,
                        o.y
                );

        AffineTransform old =
                g.getTransform();

        g.transform(at);

        if (activity > 0) {

            g.setColor(
                    new Color(
                            0.30f,
                            0.79f,
                            0.94f,
                            0.35f * activity
                    )
            );

            g.fill(
                    new Rectangle2D.Double(
                            -BODY_L / 2,
                            -BODY_W / 2,
                            BODY_L,
                            BODY_W
                    )
            );
        }

        Font base =
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        1
                );

        g.setColor(
                new Color(225, 225, 225)
        );

        g.setFont(
                base.deriveFont(0.50f)
        );

        g.drawString(
                "NUVOTON",
                -2.7f,
                -0.55f
        );

        g.setFont(
                base.deriveFont(0.62f)
        );

        g.drawString(
                "MS51FB9AE",
                -2.7f,
                0.25f
        );

        g.setFont(
                base.deriveFont(0.36f)
        );

        g.drawString(
                "8051 1T  24 MHz  TSSOP-20",
                -2.7f,
                0.95f
        );

        g.setColor(
                new Color(200, 200, 200)
        );

        g.fill(
                new Ellipse2D.Double(
                        -2.9,
                        1.35,
                        0.44,
                        0.44
                )
        );

        g.setTransform(old);
    }

    private void drawHud(
            Graphics2D g,
            int w,
            int h) {

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.BOLD,
                        15
                )
        );

        g.setColor(
                new Color(235, 240, 245)
        );

        g.drawString(
                "NUVOTON MS51FB9AE  -  3D VIEW",
                14,
                24
        );

        g.setFont(
                new Font(
                        "SansSerif",
                        Font.PLAIN,
                        12
                )
        );

        g.setColor(
                new Color(160, 175, 190)
        );

        g.drawString(
                "Current instruction: " +
                        instruction,
                14,
                44
        );

        String hint =
                "drag: rotate   wheel: zoom   " +
                "double-click: auto-rotate " +
                (autoRotate
                        ? "(on)"
                        : "(off)");

        g.drawString(
                hint,
                14,
                h - 12
        );

        if (hoverPin > 0) {

            String t =
                    "Pin " +
                    hoverPin +
                    ":  " +
                    pinLabel[hoverPin] +
                    (pinHigh[hoverPin]
                            ? "   [HIGH]"
                            : "   [LOW]");

            FontMetrics fm =
                    g.getFontMetrics();

            int tw =
                    fm.stringWidth(t)
                            + 20;

            g.setColor(
                    new Color(
                            0,
                            0,
                            0,
                            180
                    )
            );

            g.fillRoundRect(
                    w - tw - 14,
                    12,
                    tw,
                    28,
                    10,
                    10
            );

            g.setColor(
                    new Color(0x4CC9F0)
            );

            g.drawString(
                    t,
                    w - tw - 4,
                    31
            );
        }

        int ly = h - 34;

        legendDot(
                g,
                w - 250,
                ly,
                new Color(0x3CDC64),
                "HIGH"
        );

        legendDot(
                g,
                w - 180,
                ly,
                new Color(0xC8CCD2),
                "LOW"
        );

        legendDot(
                g,
                w - 115,
                ly,
                new Color(0xFF9F1C),
                "ACTIVE"
        );
    }

    private void legendDot(
            Graphics2D g,
            int x,
            int y,
            Color c,
            String s) {

        g.setColor(c);
        g.fillOval(
                x,
                y,
                10,
                10
        );

        g.setColor(
                new Color(160, 175, 190)
        );

        g.drawString(
                s,
                x + 15,
                y + 10
        );
    }

    private void installMouse() {

        MouseAdapter ma =
                new MouseAdapter() {

                    @Override
                    public void mousePressed(
                            MouseEvent e) {

                        lastX = e.getX();
                        lastY = e.getY();
                        dragging = true;
                    }

                    @Override
                    public void mouseReleased(
                            MouseEvent e) {

                        dragging = false;
                    }

                    @Override
                    public void mouseClicked(
                            MouseEvent e) {

                        if (e.getClickCount() == 2) {
                            autoRotate =
                                    !autoRotate;
                        }
                    }

                    @Override
                    public void mouseDragged(
                            MouseEvent e) {

                        yaw +=
                                (e.getX() - lastX)
                                        * 0.01;

                        pitch =
                                Math.max(
                                        -1.45,
                                        Math.min(
                                                1.45,
                                                pitch +
                                                (e.getY() - lastY)
                                                        * 0.01
                                        )
                                );

                        lastX = e.getX();
                        lastY = e.getY();
                    }

                    @Override
                    public void mouseMoved(
                            MouseEvent e) {

                        hoverPin = -1;

                        for (
                                int i =
                                        lastFaces.size() - 1;
                                i >= 0;
                                i--
                        ) {

                            Face f =
                                    lastFaces.get(i);

                            if (
                                    f.pin > 0 &&
                                    f.path.contains(
                                            e.getX(),
                                            e.getY()
                                    )
                            ) {

                                hoverPin =
                                        f.pin;

                                break;
                            }
                        }
                    }

                    @Override
                    public void mouseWheelMoved(
                            MouseWheelEvent e) {

                        zoom =
                                Math.max(
                                        0.4,
                                        Math.min(
                                                3.0,
                                                zoom *
                                                (
                                                        1.0 -
                                                        e.getWheelRotation()
                                                                * 0.08
                                                )
                                        )
                                );
                    }
                };

        addMouseListener(ma);
        addMouseMotionListener(ma);
        addMouseWheelListener(ma);
    }

    public static void main(
            String[] args) {

        SwingUtilities.invokeLater(() -> {

            Chip3DPanel p =
                    new Chip3DPanel();

            openWindow(p)
                    .setDefaultCloseOperation(
                            JFrame.EXIT_ON_CLOSE
                    );

            int[] v = {0};

            new Timer(
                    400,
                    e -> {

                        v[0] =
                                (v[0] + 0x20)
                                        & 0xFF;

                        p.setPortValue(
                                0,
                                v[0]
                        );

                        p.setInstruction(
                                "INC P0 ; demo"
                        );

                        p.pulse();
                    }
            ).start();
        });
    }
}
