package assets.Utility;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.List;
import javax.swing.*;

public class SurvivalLeaderboardPopupDialog extends JDialog {
    private static final int DEFAULT_SCREEN_WIDTH  = 1536;
    private static final int DEFAULT_SCREEN_HEIGHT = 896;

    public SurvivalLeaderboardPopupDialog(Window owner) {
        super(owner, ModalityType.APPLICATION_MODAL);
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(new PopupPanel(owner));

        Dimension size = owner != null ? owner.getSize() : null;
        if (size == null || size.width <= 0 || size.height <= 0) {
            size = new Dimension(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
        }
        setSize(size);
    }

    private final class PopupPanel extends JPanel {
        private final Window owner;
        private Rectangle backButton;
        private boolean hoverBack;

        private final Font titleFont   = FontManager.getFont(52f).deriveFont(Font.BOLD);
        private final Font sectionFont = FontManager.getFont(34f).deriveFont(Font.BOLD);
        private final Font rowFont     = FontManager.getFont(28f).deriveFont(Font.PLAIN);
        private final Font buttonFont  = FontManager.getFont(28f).deriveFont(Font.BOLD);

        PopupPanel(Window owner) {
            this.owner = owner;
            setOpaque(false);
            setFocusable(true);
            setFocusTraversalKeysEnabled(false);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    handleMouseClick(e.getX(), e.getY());
                }
            });

            addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
                @Override
                public void mouseMoved(java.awt.event.MouseEvent e) {
                    handleMouseMove(e.getX(), e.getY());
                }
            });

            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(java.awt.event.KeyEvent e) {
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE
                            || e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        dispose();
                    }
                }
            });
        }

        @Override
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(this::requestFocusInWindow);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            paintBackground(g2, owner);
            paintPopup(g2);
            g2.dispose();
        }

        private void paintBackground(Graphics2D g2, Window ownerWindow) {
            try {
                if (ownerWindow != null) {
                    int w = getWidth(), h = getHeight();
                    BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig = buf.createGraphics();
                    ig.setColor(new Color(0, 0, 0, 0));
                    ig.fillRect(0, 0, w, h);
                    if (ownerWindow instanceof RootPaneContainer rpc) {
                        rpc.getContentPane().paint(ig);
                    } else {
                        ownerWindow.paint(ig);
                    }
                    ig.dispose();

                    int sz = 5;
                    float weight = 1.0f / (sz * sz);
                    float[] data = new float[sz * sz];
                    for (int i = 0; i < data.length; i++) data[i] = weight;
                    ConvolveOp op = new ConvolveOp(new Kernel(sz, sz, data), ConvolveOp.EDGE_NO_OP, null);
                    BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    op.filter(buf, dst);

                    Composite old = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                    g2.drawImage(dst, 0, 0, null);
                    g2.setComposite(old);

                    g2.setColor(new Color(0, 0, 0, 120));
                    g2.fillRect(0, 0, w, h);
                    return;
                }
            } catch (Exception ignored) {
            }
            g2.setColor(new Color(0, 0, 0, 150));
            g2.fillRect(0, 0, getWidth(), getHeight());
        }

        private void paintPopup(Graphics2D g2) {
            // ── Load data ─────────────────────────────────────────────────────
            String mainTitle   = "Leaderboard";
            String survTitle   = "Survival Scores";
            String arcadeTitle = "Arcade Speedrun";
            String backText    = "Back";

            List<SurvivalLeaderboardManager.Entry> rawSurv = SurvivalLeaderboardManager.loadEntries();
            final List<SurvivalLeaderboardManager.Entry> survEntries =
                    rawSurv.size() > 10 ? rawSurv.subList(0, 10) : rawSurv;

            List<ArcadeSpeedrunManager.Entry> rawArcade = ArcadeSpeedrunManager.loadEntries();
            final List<ArcadeSpeedrunManager.Entry> arcadeEntries =
                    rawArcade.size() > 10 ? rawArcade.subList(0, 10) : rawArcade;

            // ── Measure fonts ─────────────────────────────────────────────────
            FontMetrics titleFm   = g2.getFontMetrics(titleFont);
            FontMetrics sectionFm = g2.getFontMetrics(sectionFont);
            FontMetrics rowFm     = g2.getFontMetrics(rowFont);
            FontMetrics btnFm     = g2.getFontMetrics(buttonFont);

            // ── Layout constants ──────────────────────────────────────────────
            int padX      = 56;
            int padY      = 44;
            int colGap    = 48;   // gap between the two columns
            int dividerW  = 2;    // width of the vertical separator
            int rowGap    = 12;
            int btnPadX   = 48;
            int btnPadY   = 20;
            int sectionGapBelowTitle = 14;  // space between section header and first row

            // Placeholder strings for empty leaderboards
            String survEmpty = "No survival scores yet.";
            String arcEmpty  = "No speedrun times yet.";

            // ── Row heights ───────────────────────────────────────────────────
            int rowH    = rowFm.getHeight();
            int maxRows = Math.max(
                    survEntries.isEmpty() ? 1 : survEntries.size(),
                    arcadeEntries.isEmpty() ? 1 : arcadeEntries.size());
            int listH   = maxRows * (rowH + rowGap) - rowGap;
            int btnH    = btnFm.getHeight() + btnPadY * 2;
            int btnW    = btnFm.stringWidth(backText.toUpperCase()) + btnPadX * 2;

            // ── Panel dimensions: pick innerW first, then split 50/50 ─────────
            // Minimum needed by each side independently
            int minSurvW = survEntries.isEmpty()
                    ? rowFm.stringWidth(survEmpty)
                    : survEntries.stream().mapToInt(e -> rowFm.stringWidth(formatSurvRow(survEntries.indexOf(e) + 1, e))).max().orElse(0);
            minSurvW = Math.max(minSurvW, sectionFm.stringWidth(survTitle));

            int minArcW = arcadeEntries.isEmpty()
                    ? rowFm.stringWidth(arcEmpty)
                    : arcadeEntries.stream().mapToInt(e -> rowFm.stringWidth(formatArcadeRow(arcadeEntries.indexOf(e) + 1, e))).max().orElse(0);
            minArcW = Math.max(minArcW, sectionFm.stringWidth(arcadeTitle));

            // innerW must fit both columns + divider gap, the main title, and the button
            int mainTitleW = titleFm.stringWidth(mainTitle);
            int minInner = minSurvW + colGap + dividerW + colGap + minArcW;
            int innerW = Math.max(minInner, Math.max(mainTitleW, btnW));
            innerW = Math.max(innerW, 860);  // hard minimum total inner width

            // Each column gets exactly half (minus the divider+gap allocation)
            int colW = (innerW - colGap - dividerW - colGap) / 2;

            int sectionHeaderH = sectionFm.getHeight() + sectionGapBelowTitle;
            int panelW = innerW + padX * 2;
            int panelH = padY * 2
                       + titleFm.getHeight() + 20
                       + sectionHeaderH
                       + listH + 24
                       + btnH;

            int panelX = (getWidth()  - panelW) / 2;
            int panelY = (getHeight() - panelH) / 2;

            // ── Draw panel background ─────────────────────────────────────────
            g2.setColor(new Color(8, 6, 4, 100));
            g2.fillRoundRect(panelX + 6, panelY + 8, panelW, panelH, 14, 14);
            g2.setColor(new Color(70, 55, 40, 230));
            g2.fillRoundRect(panelX, panelY, panelW, panelH, 14, 14);
            g2.setColor(new Color(30, 24, 18, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(panelX, panelY, panelW, panelH, 14, 14);

            // ── Main title ────────────────────────────────────────────────────
            g2.setFont(titleFont);
            g2.setColor(new Color(245, 242, 238));
            int mainTitleX = panelX + (panelW - titleFm.stringWidth(mainTitle)) / 2;
            int mainTitleY = panelY + padY + titleFm.getAscent();
            g2.drawString(mainTitle, mainTitleX, mainTitleY);

            // ── Column origins: divider exactly in the middle ─────────────────
            int contentY  = mainTitleY + 20;
            int leftColX  = panelX + padX;
            int dividerX  = leftColX + colW + colGap;   // centre of the panel interior
            int rightColX = dividerX + dividerW + colGap;

            // ── Vertical divider ──────────────────────────────────────────────
            int divTop    = contentY;
            int divBottom = panelY + panelH - padY - btnH - 16;
            g2.setColor(new Color(200, 180, 150, 100));
            g2.setStroke(new BasicStroke(dividerW));
            g2.drawLine(dividerX, divTop, dividerX, divBottom);
            g2.setStroke(new BasicStroke(2f));

            // ── Section headers ───────────────────────────────────────────────
            int sectionY = contentY + sectionFm.getAscent();
            g2.setFont(sectionFont);

            // Survival header with accent underline
            g2.setColor(new Color(255, 210, 100));   // golden accent
            g2.drawString(survTitle, leftColX, sectionY);
            int survTitleW = sectionFm.stringWidth(survTitle);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(leftColX, sectionY + 4, leftColX + survTitleW, sectionY + 4);

            // Arcade header with accent underline
            g2.setColor(new Color(100, 220, 255));   // cyan accent
            g2.drawString(arcadeTitle, rightColX, sectionY);
            int arcTitleW = sectionFm.stringWidth(arcadeTitle);
            g2.drawLine(rightColX, sectionY + 4, rightColX + arcTitleW, sectionY + 4);
            g2.setStroke(new BasicStroke(2f));

            // ── Row content ───────────────────────────────────────────────────
            int firstRowY = sectionY + sectionFm.getDescent() + sectionGapBelowTitle;
            g2.setFont(rowFont);

            // Survival rows
            if (survEntries.isEmpty()) {
                g2.setColor(new Color(180, 170, 160));
                g2.drawString(survEmpty, leftColX, firstRowY + rowFm.getAscent());
            } else {
                for (int i = 0; i < survEntries.size(); i++) {
                    // Alternate row shading
                    if (i % 2 == 0) {
                        g2.setColor(new Color(255, 255, 255, 12));
                        g2.fillRoundRect(leftColX - 6, firstRowY + i * (rowH + rowGap) - 2,
                                         colW + 12, rowH, 4, 4);
                    }
                    // Rank highlight for top 3
                    g2.setColor(rankColor(i));
                    String row = formatSurvRow(i + 1, survEntries.get(i));
                    g2.drawString(row, leftColX, firstRowY + rowFm.getAscent() + i * (rowH + rowGap));
                }
            }

            // Arcade rows
            if (arcadeEntries.isEmpty()) {
                g2.setColor(new Color(180, 170, 160));
                g2.drawString(arcEmpty, rightColX, firstRowY + rowFm.getAscent());
            } else {
                for (int i = 0; i < arcadeEntries.size(); i++) {
                    if (i % 2 == 0) {
                        g2.setColor(new Color(255, 255, 255, 12));
                        g2.fillRoundRect(rightColX - 6, firstRowY + i * (rowH + rowGap) - 2,
                                         colW + 12, rowH, 4, 4);
                    }
                    g2.setColor(rankColor(i));
                    String row = formatArcadeRow(i + 1, arcadeEntries.get(i));
                    g2.drawString(row, rightColX, firstRowY + rowFm.getAscent() + i * (rowH + rowGap));
                }
            }

            // ── Back button ───────────────────────────────────────────────────
            int buttonX = panelX + (panelW - btnW) / 2;
            int buttonY = panelY + panelH - padY - btnH;
            backButton = new Rectangle(buttonX, buttonY, btnW, btnH);
            paintButton(g2, backButton, backText.toUpperCase(), hoverBack);
        }

        /** Gold → silver → bronze → white for ranks 1-3+. */
        private Color rankColor(int index) {
            return switch (index) {
                case 0 -> new Color(255, 215, 80);   // gold
                case 1 -> new Color(200, 200, 215);  // silver
                case 2 -> new Color(205, 145, 100);  // bronze
                default -> new Color(230, 225, 220); // normal
            };
        }

        private String formatSurvRow(int rank, SurvivalLeaderboardManager.Entry entry) {
            return rank + ".  " + entry.playerName() + "  —  " + entry.score() + " pts";
        }

        private String formatArcadeRow(int rank, ArcadeSpeedrunManager.Entry entry) {
            return rank + ".  " + entry.playerName() + "  —  "
                   + ArcadeSpeedrunManager.formatTime(entry.timeMs());
        }

        private void paintButton(Graphics2D g2, Rectangle bounds, String text, boolean hovered) {
            Color base  = new Color(115, 90, 60);
            Color hover = new Color(140, 110, 80);
            g2.setColor(hovered ? hover : base);
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

            GradientPaint gp = new GradientPaint(
                    bounds.x, bounds.y,              new Color(160, 130, 100, 40),
                    bounds.x, bounds.y + bounds.height, new Color(0, 0, 0, 0));
            g2.setPaint(gp);
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height / 2, 10, 10);
            g2.setPaint(null);

            g2.setColor(new Color(40, 30, 22, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

            g2.setFont(buttonFont);
            g2.setColor(new Color(246, 244, 240));
            FontMetrics fm = g2.getFontMetrics();
            int textX = bounds.x + (bounds.width  - fm.stringWidth(text)) / 2;
            int textY = bounds.y + (bounds.height  + fm.getAscent())       / 2 - 3;
            g2.drawString(text, textX, textY);
        }

        private void handleMouseClick(int x, int y) {
            if (backButton != null && backButton.contains(x, y)) {
                dispose();
            }
        }

        private void handleMouseMove(int x, int y) {
            boolean wasBackHovered = hoverBack;
            hoverBack = backButton != null && backButton.contains(x, y);
            if (wasBackHovered != hoverBack) {
                repaint();
            }
        }
    }
}