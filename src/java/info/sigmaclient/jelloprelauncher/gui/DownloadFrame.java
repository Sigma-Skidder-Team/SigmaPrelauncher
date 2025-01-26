package info.sigmaclient.jelloprelauncher.gui;

import info.sigmaclient.jelloprelauncher.JelloPrelauncher;
import info.sigmaclient.jelloprelauncher.versions.Version;

import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.awt.LayoutManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Objects;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.Border;

public class DownloadFrame extends JFrame implements ActionListener {
    private final JProgressBar jProgressBar = new JProgressBar();
    private final JelloButton play = new JelloButton("Play!");
    private final JLabel label = new JLabel();
    private final JComboBox<String> comboBox;
    Thread autoPlay;

    public DownloadFrame() throws HeadlessException {
        this.setTitle("Sigma Jello Bootstrap");
        this.setResizable(false);
        this.setSize(580, 150);
        this.setLocationRelativeTo(null);
        this.setBackground(Color.BLACK);
        this.jProgressBar.setUI(new JelloProgressBar());
        this.jProgressBar.setBounds(26, 80, this.getWidth() - 50, 25);
        this.jProgressBar.setBorderPainted(false);
        this.jProgressBar.setBorder(null);
        this.jProgressBar.setVisible(false);

        ImagePanel image = new ImagePanel();
        try {
            image.setImage(ImageIO.read(Objects.requireNonNull(this.getClass().getClassLoader().getResource("logo.png"))));
        } catch (IOException var2) {
        }

        image.setBounds(26, 28, 221, 35);
        this.label.setText("Auto Play in 10s..");
        this.label.setBounds(this.getWidth() - 228, 44, 200, 20);
        this.label.setHorizontalAlignment(SwingConstants.RIGHT);
        this.label.setForeground(Color.WHITE);
        this.autoPlay = new Thread(() -> {
            for (int i = 0; i < 10; ++i) {
                this.label.setText("Auto Play in " + (10 - i) + "s..");
                if (Thread.interrupted()) {
                    return;
                }

                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException var3) {
                    return;
                }

                if (Thread.interrupted()) {
                    return;
                }
            }

            SwingUtilities.invokeLater(() -> JelloPrelauncher.shared.play());
        });
        this.play.setBounds(360, 75, 195, 30);
        this.play.setVisible(true);
        this.play.setText("Play!");
        this.play.addActionListener((action) -> SwingUtilities.invokeLater(() -> {
            this.autoPlay.interrupt();
            JelloPrelauncher.shared.play();
        }));
        this.comboBox = new JComboBox<>();
        this.comboBox.setBounds(26, 80, 195, 22);
        this.comboBox.addActionListener(this);
        this.play.setEnabled(false);
        JPanel pane = new JPanel();
        pane.add(this.label);
        pane.add(image);
        pane.add(this.jProgressBar);
        pane.add(this.play);
        pane.add(this.comboBox);
        pane.setBackground(Color.BLACK);
        pane.setLayout(null);
        this.add(pane);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        String s = (String) this.comboBox.getSelectedItem();
        if (JelloPrelauncher.shared != null) {
            JelloPrelauncher.shared.setVersion(s);
        }

        if (this.autoPlay != null) {
            this.autoPlay.interrupt();
            this.label.setText("Select version and play!");
        }

    }

    public void setVersions(HashMap<String, Version> hashMap) {
        this.play.setEnabled(true);

        for (Entry<String, Version> stringVersionEntry : hashMap.entrySet()) {
            this.comboBox.addItem(stringVersionEntry.getValue().getDisplayName());
        }

        this.autoPlay.start();
    }

    public void setProgress(int progress, String name) {
        this.play.setVisible(false);
        this.comboBox.setVisible(false);
        this.jProgressBar.setVisible(true);
        this.jProgressBar.setValue(Math.min(100, progress));
        this.label.setText(name + " " + progress + "%");
        this.repaint();
    }
}
