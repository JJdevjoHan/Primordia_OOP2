package assets.Utility;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.function.Consumer;
import javax.swing.*;

public class SurvivalLeaderboardEntryDialog extends JDialog {
    private static final int DEFAULT_SCREEN_WIDTH = 1536;
    private static final int DEFAULT_SCREEN_HEIGHT = 896;

    private final Consumer<String> onRecord;

    public SurvivalLeaderboardEntryDialog(Window owner, int finalScore, Consumer<String> onRecord) {
        super(owner, ModalityType.APPLICATION_MODAL);
        this.onRecord = onRecord;
        setUndecorated(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setContentPane(new PopupPanel(owner, finalScore));

        Dimension size = owner != null ? owner.getSize() : null;
        if (size == null || size.width <= 0 || size.height <= 0) {
            size = new Dimension(DEFAULT_SCREEN_WIDTH, DEFAULT_SCREEN_HEIGHT);
        }
        setSize(size);
    }

    private void recordName(String name) {
        if (onRecord != null) {
            onRecord.accept(name);
        }
        dispose();
    }

    private final class PopupPanel extends JPanel {
        private final Window owner;
        private final int finalScore;
        private final JTextField nameField = new JTextField();
        private final JButton recordButton = createRecordButton();
        private Rectangle dialogBounds;

        private final Font titleFont = FontManager.getFont(46f).deriveFont(Font.BOLD);
        private final Font bodyFont = FontManager.getFont(28f).deriveFont(Font.PLAIN);
        private final Font buttonFont = FontManager.getFont(30f).deriveFont(Font.BOLD);

        PopupPanel(Window owner, int finalScore) {
            this.owner = owner;
            this.finalScore = finalScore;
            setLayout(null);
            setOpaque(false);

            nameField.setFont(FontManager.getFont(28f));
            nameField.setForeground(new Color(246, 244, 240));
            nameField.setBackground(new Color(50, 38, 28));
            nameField.setCaretColor(new Color(246, 244, 240));
            nameField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(30, 24, 18, 180), 2),
                    BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            nameField.setColumns(18);

            styleButton(recordButton);
            recordButton.addActionListener(e -> recordCurrentName());

            add(nameField);
            add(recordButton);

            addKeyListener(new java.awt.event.KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        recordCurrentName();
                    }
                }
            });

            nameField.addActionListener(e -> recordCurrentName());

            SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            SwingUtilities.invokeLater(nameField::requestFocusInWindow);
        }

        @Override
        public void doLayout() {
            super.doLayout();

            int panelWidth = Math.min(620, getWidth() - 80);
            int panelHeight = 248;
            int panelX = (getWidth() - panelWidth) / 2;
            int panelY = (getHeight() - panelHeight) / 2;
            dialogBounds = new Rectangle(panelX, panelY, panelWidth, panelHeight);

            int fieldWidth = panelWidth - 64;
            int fieldHeight = 48;
            int fieldX = panelX + 32;
            int fieldY = panelY + 118;

            nameField.setBounds(fieldX, fieldY, fieldWidth, fieldHeight);
            recordButton.setBounds(panelX + (panelWidth - 200) / 2, panelY + 176, 200, 48);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            paintBackground(g2, owner);
            paintDialog(g2);
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
                    for (int i = 0; i < data.length; i++) {
                        data[i] = weight;
                    }
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

        private void paintDialog(Graphics2D g2) {
            if (dialogBounds == null) {
                return;
            }

            int panelX = dialogBounds.x;
            int panelY = dialogBounds.y;
            int panelWidth = dialogBounds.width;
            int panelHeight = dialogBounds.height;

            g2.setColor(new Color(8, 6, 4, 100));
            g2.fillRoundRect(panelX + 6, panelY + 8, panelWidth, panelHeight, 14, 14);

            g2.setColor(new Color(70, 55, 40, 230));
            g2.fillRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

            g2.setColor(new Color(30, 24, 18, 170));
            g2.setStroke(new BasicStroke(2f));
            g2.drawRoundRect(panelX, panelY, panelWidth, panelHeight, 14, 14);

            g2.setFont(titleFont);
            g2.setColor(new Color(245, 242, 238));
            String title = "Record Survival Score";
            FontMetrics titleFm = g2.getFontMetrics();
            int titleW = titleFm.stringWidth(title);
            int titleX = panelX + (panelWidth - titleW) / 2;
            int titleY = panelY + 42;
            g2.drawString(title, titleX, titleY);

            g2.setFont(bodyFont);
            String subtitle = "Final Score: " + finalScore;
            FontMetrics bodyFm = g2.getFontMetrics();
            int subtitleW = bodyFm.stringWidth(subtitle);
            int subtitleX = panelX + (panelWidth - subtitleW) / 2;
            int subtitleY = panelY + 76;
            g2.setColor(new Color(230, 220, 210));
            g2.drawString(subtitle, subtitleX, subtitleY);

            String prompt = "Enter your name for the leaderboard";
            int promptW = bodyFm.stringWidth(prompt);
            int promptX = panelX + (panelWidth - promptW) / 2;
            int promptY = panelY + 106;
            g2.setColor(new Color(220, 208, 194));
            g2.drawString(prompt, promptX, promptY);
        }

        private void styleButton(JButton button) {
            button.setFont(buttonFont);
            button.setForeground(new Color(246, 244, 240));
            button.setBackground(new Color(115, 90, 60));
            button.setFocusPainted(false);
            button.setRolloverEnabled(true);
            button.setBorder(BorderFactory.createEmptyBorder());
            button.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(140, 110, 80));
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent evt) {
                    button.setBackground(new Color(115, 90, 60));
                }
            });
        }

        private JButton createRecordButton() {
            final String label = "RECORD";
            JButton button = new JButton("") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    g2.setColor(new Color(8, 6, 4, 100));
                    g2.fillRoundRect(4, 4, getWidth(), getHeight(), 10, 10);

                    g2.setColor(getBackground());
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

                    GradientPaint gp = new GradientPaint(
                            0,
                            0,
                            new Color(160, 130, 100, 40),
                            0,
                            getHeight(),
                            new Color(0, 0, 0, 0)
                    );
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 10, 10);
                    g2.setPaint(null);

                    g2.setColor(new Color(40, 30, 22, 180));
                    g2.setStroke(new BasicStroke(2f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);

                    g2.setFont(buttonFont);
                    g2.setColor(new Color(246, 244, 240));
                    FontMetrics fm = g2.getFontMetrics();
                    int textX = (getWidth() - fm.stringWidth(label)) / 2;
                    int textY = (getHeight() + fm.getAscent()) / 2 - 3;
                    g2.drawString(label, textX, textY);
                    g2.dispose();
                }
            };

            styleButton(button);
            return button;
        }

        private void recordCurrentName() {
            String name = nameField.getText();
            if (name == null || name.trim().isEmpty()) {
                name = "Anonymous";
            } else {
                name = name.trim();
            }
            SurvivalLeaderboardEntryDialog.this.recordName(name);
        }
    }
}