package net.autch.androcast;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class CapturePanel extends JPanel {
	private JFrame parent;
	private BufferedImage image;
	private int img_width, img_height;
	private int raw_width, raw_height;
	
	CapturePanel(JFrame parent) {
		super();
		this.parent = parent;
		setSize(320, 240);
		setOpaque(true);
	}

	@Override
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		synchronized(this) {
			if(image != null) {
				g.drawImage(image, 0, 0, img_width, img_height, 0, 0, raw_width, raw_height, null);
			}
		}
	}

	public void setImage(BufferedImage im, int dw, int dh, int sw, int sh) {
		image = im;
		img_width = dw; img_height = dh;
		raw_width = sw; raw_height = sh;
		setSize(img_width, img_height);
		setPreferredSize(new Dimension(img_width, img_height));
		parent.pack();
	}

	public BufferedImage getImage() {
		return image;
	}
}
