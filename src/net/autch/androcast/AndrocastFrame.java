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
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.filechooser.FileNameExtensionFilter;

public class AndrocastFrame extends JFrame implements ItemListener {
	private static final String RE_EMULATOR_SERIAL = "emulator-(\\d+)";

	private static final String[] zoomLabels = { "50%", "75%", "100%", "150%",
	"200%" };
	private static final double[] zoomValues = { 0.5, 0.75, 1.0, 1.50, 2.0 };
	private static final int zoomDefault = 2;

	private final Androcast application;
	private JToolBar tbPanel;
	private CapturePanel screenPanel;
	private JComboBox devCombo, zoomCombo;
	private JToggleButton startButton;
	private JButton captureButton;
	private JCheckBox landscapeCheck;
	private AndrocastMonitor worker;
	private JButton packButton;

	public AndrocastFrame(String arg0, Androcast app) throws HeadlessException {
		super(arg0);
		application = app;
		initComponents();
	}

	private void initComponents() {
		tbPanel = new JToolBar();
		tbPanel.setFloatable(true);

		tbPanel.add(new JLabel("Device: "));
		devCombo = new JComboBox();
		for (String d : application.getDevices()) {
			devCombo.addItem(getDeviceCaption(d));
		}
		tbPanel.add(devCombo);

		startButton = new JToggleButton("Start");
		startButton.addItemListener(this);
		tbPanel.add(startButton);

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

		packButton = new JButton("Pack");
		packButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pack();
			}
		});
		tbPanel.add(packButton);

		add(tbPanel, BorderLayout.PAGE_START);

		screenPanel = new CapturePanel(this);
		add(screenPanel, BorderLayout.CENTER);

		setSize(400, 600);

		// pack();
	}

	public static String getDeviceCaption(String dev) {
		String name = isEmulator(dev) ? "EMU: " : "DEV: ";
		return name + dev;
	}

	private static boolean isEmulator(String serial) {
		return serial.matches(RE_EMULATOR_SERIAL);
	}

	class CaptureAction extends AbstractAction {
		public void actionPerformed(ActionEvent e) {
			BufferedImage image = screenPanel.getImage();
			JFileChooser fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileNameExtensionFilter("PNG file", "png"));
			if (fc.showSaveDialog(AndrocastFrame.this) == JFileChooser.APPROVE_OPTION) {
				try {
					ImageIO.write(image, "png", fc.getSelectedFile());
				} catch (Exception ex) {
					JOptionPane.showMessageDialog(AndrocastFrame.this,
							MessageFormat.format("Unable to save a capture: {0}\n{1}",
									fc.getSelectedFile(), ex.getMessage()),
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
				String[] devices = application.getDevices();
				for (String d : devices) {
					if (devCombo.getSelectedItem().equals(getDeviceCaption(d))) {
						worker = new AndrocastMonitor(screenPanel, d);
						worker.setZoom(zoomValues[zoomCombo.getSelectedIndex()]);
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
				break;
			}
		}
	}
}
