package info.sigmaclient.jelloprelauncher.gui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.RoundRectangle2D.Double;
import javax.swing.JButton;

public class JelloButton extends JButton {
    private static final long serialVersionUID = 1L;
    float alpha = 0.5F;

    public JelloButton(String label) {
        super(label);
        this.addMouseListener(new JelloButton.ML());
    }

    protected void paintComponent(Graphics g) {
        Rectangle originalSize = super.getBounds();
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int width = (int)originalSize.getWidth();
        int height = (int)originalSize.getHeight();
        int color = blendColor(-1, -16777216, 0.05F + this.getAlpha() * 0.2F);
        g2d.setColor(new Color(color));
        RoundRectangle2D fill = new Double(0.0D, 0.0D, (double)width, (double)height, 14.0D, 14.0D);
        g2d.fill(fill);
        g2d.setColor(new Color(-1118482));
        g2d.drawString("Play", 83, 19);
        g2d.dispose();
    }

    public static int blendColor(int color1, int color2, float perc) {
        int alpha1 = color1 >> 24 & 255;
        int r1 = color1 >> 16 & 255;
        int g1 = color1 >> 8 & 255;
        int b1 = color1 & 255;
        int alpha2 = color2 >> 24 & 255;
        int r2 = color2 >> 16 & 255;
        int g2 = color2 >> 8 & 255;
        int b2 = color2 & 255;
        float iratio = 1.0F - perc;
        float alpha = (float)alpha1 * perc + (float)alpha2 * iratio;
        float red = (float)r1 * perc + (float)r2 * iratio;
        float green = (float)g1 * perc + (float)g2 * iratio;
        float blue = (float)b1 * perc + (float)b2 * iratio;
        return (int)alpha << 24 | ((int)red & 255) << 16 | ((int)green & 255) << 8 | (int)blue & 255;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public void setAlpha(float alpha) {
        this.alpha = alpha;
        this.repaint();
    }

    public class ML extends MouseAdapter {
        public void mouseExited(MouseEvent me) {
            (new Thread(() -> {
                for(float i = 1.0F; i >= 0.5F; i -= 0.03F) {
                    JelloButton.this.setAlpha(i);

                    try {
                        Thread.sleep(10L);
                    } catch (Exception var3) {
                    }
                }

            })).start();
        }

        public void mouseEntered(MouseEvent me) {
            (new Thread(() -> {
                for(float i = 0.5F; i <= 1.0F; i += 0.03F) {
                    JelloButton.this.setAlpha(i);

                    try {
                        Thread.sleep(10L);
                    } catch (Exception var3) {
                    }
                }

            })).start();
        }

        public void mousePressed(MouseEvent me) {
            (new Thread(() -> {
                for(float i = 1.0F; i >= 0.6F; i -= 0.1F) {
                    JelloButton.this.setAlpha(i);

                    try {
                        Thread.sleep(1L);
                    } catch (Exception var3) {
                    }
                }

            })).start();
        }
    }
}
