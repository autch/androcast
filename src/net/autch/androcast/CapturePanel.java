/*
 * Copyright (C) 2009-2010 Autch.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.autch.androcast;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class CapturePanel extends JPanel {
	private final JFrame parent;
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
