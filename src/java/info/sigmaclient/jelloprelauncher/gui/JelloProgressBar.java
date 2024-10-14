package info.sigmaclient.jelloprelauncher.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.geom.Rectangle2D.Double;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicProgressBarUI;

public class JelloProgressBar extends BasicProgressBarUI {
   int width = 500;
   int height = 26;

   protected Dimension getPreferredInnerVertical() {
      return new Dimension(this.width, this.height);
   }

   protected Dimension getPreferredInnerHorizontal() {
      return new Dimension(this.width, this.height);
   }

   protected void paintDeterminate(Graphics g, JComponent c) {
      Graphics2D g2d = (Graphics2D)g.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      int iStrokWidth = 1;
      int width = this.progressBar.getWidth();
      int height = this.progressBar.getHeight();
      Rectangle2D rect = new Double(0.0D, 0.0D, (double)width, (double)height);
      g2d.setColor(Color.BLACK);
      g2d.fill(rect);
      int iInnerHeight = height - iStrokWidth * 4;
      int iInnerWidth = width - iStrokWidth * 4;
      g2d.setColor(new Color(-11448236));
      RoundRectangle2D fill = new java.awt.geom.RoundRectangle2D.Double(0.0D, 0.0D, (double)iInnerWidth, (double)iInnerHeight, (double)iInnerHeight, (double)iInnerHeight);
      g2d.fill(fill);
      g2d.setColor(Color.BLACK);
      RoundRectangle2D fill1 = new java.awt.geom.RoundRectangle2D.Double(1.0D, 1.0D, (double)(iInnerWidth - 2), (double)(iInnerHeight - 2), (double)(iInnerHeight - 2), (double)(iInnerHeight - 2));
      g2d.fill(fill1);
      iInnerWidth = (int)Math.round((double)(iInnerWidth - 4) * this.getProgress());
      g2d.setColor(Color.WHITE);
      RoundRectangle2D fill2 = new java.awt.geom.RoundRectangle2D.Double(2.0D, 2.0D, (double)iInnerWidth, (double)(iInnerHeight - 4), (double)(iInnerHeight - 4), (double)(iInnerHeight - 4));
      g2d.fill(fill2);
      g2d.dispose();
   }

   private double getProgress() {
      double dProgress = this.progressBar.getPercentComplete();
      if (dProgress < 0.05D) {
         return 0.05D;
      } else {
         return Math.min(dProgress, 1.0D);
      }
   }

   protected void paintIndeterminate(Graphics g, JComponent c) {
      super.paintIndeterminate(g, c);
   }
}
