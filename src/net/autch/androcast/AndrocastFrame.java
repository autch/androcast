/*
 * Copyright (C) 2009 Autch.net
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

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;
import java.text.MessageFormat;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;

import com.android.ddmlib.Device;

public class AndrocastFrame extends JFrame implements ItemListener {

	private static final String[] zoomLabels = { "50%", "75%", "100%", "150%",
			"200%" };
	private static final double[] zoomValues = { 0.5, 0.75, 1.0, 1.50, 2.0 };
	private static final int zoomDefault = 2;

	private final Androcast application;
	private JToolBar tbPanel;
	private JPanel screenPanel;
	private JComboBox devCombo, zoomCombo;
	private JToggleButton startButton;
	private JButton captureButton;
	private JCheckBox landscapeCheck;
	private AndrocastMonitor worker;
	private BufferedImage lastImage;

	public AndrocastFrame(String arg0, Androcast app) throws HeadlessException {
		super(arg0);
		application = app;
		lastImage = null;
		initComponents();
	}

	private void initComponents() {
		tbPanel = new JToolBar();
		tbPanel.setFloatable(false);
		tbPanel.add(new JLabel("Device: "));
		devCombo = new JComboBox();
		for (Device d : application.getDevices()) {
			devCombo.addItem(getDeviceCaption(d));
		}
		tbPanel.add(devCombo);
		tbPanel.addSeparator();

		startButton = new JToggleButton("Start");
		startButton.addItemListener(this);
		tbPanel.add(startButton);
		tbPanel.addSeparator();

		captureButton = new JButton("Capture");
		captureButton.setEnabled(false);
		captureButton.addActionListener(new CaptureAction());
		tbPanel.add(captureButton);
		tbPanel.addSeparator();

		landscapeCheck = new JCheckBox("Landscape", false);
		landscapeCheck.addItemListener(this);
		tbPanel.add(landscapeCheck);
		tbPanel.addSeparator();

		tbPanel.add(new JLabel("Zoom: "));
		zoomCombo = new JComboBox(zoomLabels);
		zoomCombo.setSelectedIndex(zoomDefault);
		zoomCombo.addItemListener(this);
		tbPanel.add(zoomCombo);

		add(tbPanel, BorderLayout.PAGE_START);

		screenPanel = new CapturePanel();
		add(screenPanel, BorderLayout.CENTER);

		setSize(400, 600);

		// pack();
	}

	public static String getDeviceCaption(Device dev) {
		String name = dev.isEmulator() ? "EMU: " : "DEV: ";
		return name + dev.getSerialNumber();
	}

	class CaptureAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			if (worker == null)
				throw new NullPointerException("Worker thread is not running");
			BufferedImage image = worker.getImage();
			JFileChooser fc = new JFileChooser();
			if (fc.showSaveDialog(AndrocastFrame.this) == JFileChooser.APPROVE_OPTION) {
				try {
					ImageIO.write(image, "png", fc.getSelectedFile());
				} catch (Exception ex) {
					JOptionPane
							.showMessageDialog(
									AndrocastFrame.this,
									MessageFormat
											.format(
													"Unable to save a capture: {0}\n{1}",
													fc.getSelectedFile(), ex
															.getMessage()),
									"Error saving capture",
									JOptionPane.ERROR_MESSAGE);
				}
			}
		}
	}

	public void itemStateChanged(ItemEvent e) {
		Object source = e.getItemSelectable();
		if (source == landscapeCheck) {
			if (worker != null) {
				worker.setLandscape(e.getStateChange() == ItemEvent.SELECTED);
			}
		}
		if (source == zoomCombo) {
			if (worker != null) {
				worker.setZoom(zoomValues[zoomCombo.getSelectedIndex()]);
			}
		}
		if (source == startButton) {
			switch (e.getStateChange()) {
			case ItemEvent.SELECTED:
				Device[] devices = application.getDevices();
				for (Device d : devices) {
					if (devCombo.getSelectedItem().equals(getDeviceCaption(d))) {
						worker = new AndrocastMonitor(screenPanel, d);
						worker
								.setZoom(zoomValues[zoomCombo
										.getSelectedIndex()]);
						worker.setLandscape(landscapeCheck.isSelected());
						worker.execute();
						break;
					}
				}
				devCombo.setEnabled(false);
				captureButton.setEnabled(true);
				startButton.setText("Stop");
				break;
			case ItemEvent.DESELECTED:
				worker.cancel(false);
				worker = null;
				startButton.setText("Start");
				devCombo.setEnabled(true);
				captureButton.setEnabled(false);
				break;
			}
		}
	}

	class CapturePanel extends JPanel {
		CapturePanel() {
			setSize(320, 240);
			setOpaque(true);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			if (worker != null) {
				worker.render(g);
			} else {
				// TODO add offscreen bitmap and render it when worker is not
				// running
			}
		}

	}
}
