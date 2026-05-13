package assets.Utility;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

public class MatchResultPopupDialog extends JDialog {
    private static final int DEFAULT_SCREEN_WIDTH = 1536;
    private static final int DEFAULT_SCREEN_HEIGHT = 896;

    private final Runnable onYes;
    private final Runnable onBackToMenu;

    public MatchResultPopupDialog(Window owner, Runnable onYes, Runnable onBackToMenu) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.onYes = onYes;
        this.onBackToMenu = onBackToMenu;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(new PopupPanel(owner));

        Dimension size = owner != null ? owner.getSize() : null;
        if (size == null || size.width <= 0 || size.height <= 0) {
            size = new Dimension(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
        }
        setSize(size);
    }

    private void chooseYes() {
        dispose();
        if (onYes != null) {
            onYes.run();
        }
    }

    private void chooseBackToMenu() {
        dispose();
        if (onBackToMenu != null) {
            onBackToMenu.run();
        }
    }

    private final class PopupPanel extends JPanel {
        private final Window owner;
        private Rectangle yesButton;
        private Rectangle backButton;
        private boolean hoverYes;
        private boolean hoverBack;

        private final Font titleFont = FontManager.getFont(38f).deriveFont(Font.BOLD);
        private final Font buttonFont = FontManager.getFont(24f).deriveFont(Font.BOLD);

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
                    if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                        chooseYes();
                    } else if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
                        chooseBackToMenu();
                    }
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            paintBackground(g2, owner);
            paintPopup(g2);
            g2.dispose();
        }

        private void paintBackground(Graphics2D g2, Window ownerWindow) {
            try {
                if (ownerWindow != null) {
                    int w = getWidth();
                    int h = getHeight();
                    BufferedImage buf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ig = buf.createGraphics();
                    ig.setColor(new Color(0, 0, 0, 0));
                    ig.fillRect(0, 0, w, h);
                    if (ownerWindow instanceof RootPaneContainer rootPaneContainer) {
                        rootPaneContainer.getContentPane().paint(ig);
                    } else {
                        ownerWindow.paint(ig);
                    }
                    ig.dispose();

                    int size = 5;
                    float weight = 1.0f / (size * size);
                    float[] data = new float[size * size];
                    for (int i = 0; i < data.length; i++) data[i] = weight;
                    ConvolveOp op = new ConvolveOp(new Kernel(size, size, data), ConvolveOp.EDGE_NO_OP, null);
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
            String title = "Play again?";
            String yesText = "Yes";
            String backText = "Back to Menu";

            FontMetrics titleFm = g2.getFontMetrics(titleFont);
            FontMetrics btnFm = g2.getFontMetrics(buttonFont);

            int padX = 36;
            int padY = 22;
            int btnPadX = 28;
            int btnPadY = 14;
            int btnGap = 14;

            int titleW = titleFm.stringWidth(title);
            int b1 = btnFm.stringWidth(yesText.toUpperCase()) + btnPadX * 2;
            int b2 = btnFm.stringWidth(backText.toUpperCase()) + btnPadX * 2;
            int buttonWidth = Math.max(b1, b2);
            int buttonHeight = btnFm.getHeight() + btnPadY * 2;

            int panelWidth = Math.max(titleW, buttonWidth) + padX * 2;
            int panelHeight = padY * 2 + titleFm.getHeight() + btnGap + buttonHeight * 2 + btnGap;

            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;

            g2.setColor(new Color(8, 6, 4, 100));
            g2.fillRoundRect(panelX + 6, panelY + 8, panelWidth, panelHeight, 14, 14);

            g2.setColor(new Color(70, 55, 40, 230));
            g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

            g2.setColor(new Color(30, 24, 18, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

            g2.setFont(titleFont);
            g2.setColor(new Color(245, 242, 238));
            int titleX = panelX + (panelWidth - titleW) / 2;
            int titleY = panelY + padY + titleFm.getAscent();
            g2.drawString(title, titleX, titleY);

            int buttonX = panelX + (panelWidth - buttonWidth) / 2;
            int startY = titleY + padY + btnGap;

            yesButton = new Rectangle(buttonX, startY, buttonWidth, buttonHeight);
            paintButton(g2, yesButton, yesText.toUpperCase(), hoverYes);

            backButton = new Rectangle(buttonX, startY + buttonHeight + btnGap, buttonWidth, buttonHeight);
            paintButton(g2, backButton, backText.toUpperCase(), hoverBack);
        }

        private void paintButton(Graphics2D g2, Rectangle bounds, String text, boolean hovered) {
            Color base = new Color(115, 90, 60);
            Color hover = new Color(140, 110, 80);
            g2.setColor(hovered ? hover : base);
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

            GradientPaint gp = new GradientPaint(
                    bounds.x, bounds.y, new Color(160, 130, 100, 40),
                    bounds.x, bounds.y + bounds.height, new Color(0, 0, 0, 0)
            );
            g2.setPaint(gp);
            g2.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height / 2, 10, 10);
            g2.setPaint(null);

            g2.setColor(new Color(40, 30, 22, 180));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 10, 10);

            g2.setFont(buttonFont);
            g2.setColor(new Color(246, 244, 240));
            FontMetrics fm = g2.getFontMetrics();
            int textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2;
            int textY = bounds.y + (bounds.height + fm.getAscent()) / 2 - 3;
            g2.drawString(text, textX, textY);
        }

        private void handleMouseClick(int x, int y) {
            if (yesButton != null && yesButton.contains(x, y)) {
                chooseYes();
            } else if (backButton != null && backButton.contains(x, y)) {
                chooseBackToMenu();
            }
        }

        private void handleMouseMove(int x, int y) {
            boolean wasYesHovered = hoverYes;
            boolean wasBackHovered = hoverBack;

            hoverYes = yesButton != null && yesButton.contains(x, y);
            hoverBack = backButton != null && backButton.contains(x, y);

            if (wasYesHovered != hoverYes || wasBackHovered != hoverBack) {
                repaint();
            }
        }
    }
}