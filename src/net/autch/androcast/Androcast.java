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

import java.io.File;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.Device;

public class Androcast {
	private AndroidDebugBridge bridge;
	private Device[] devices;

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Androcast app = new Androcast();
		app.run(args);
	}

	public void run(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (InstantiationException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}

		AndroidDebugBridge.init(false /* debugger support */);
		try {
			createBridge();
			fetchDevices();
			final Androcast app = this;

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					JFrame f = new AndrocastFrame("Androcast", app);
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.setVisible(true);
				}
			});

		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Error detecting devices", JOptionPane.ERROR_MESSAGE);
		} finally {
			AndroidDebugBridge.terminate();
		}
	}

	public AndroidDebugBridge createBridge() throws Exception {
		String adbLocation = System
				.getProperty("com.android.screenshot.bindir"); //$NON-NLS-1$
		if (adbLocation != null && adbLocation.length() != 0) {
			adbLocation += File.separator + "adb"; //$NON-NLS-1$
		} else {
			adbLocation = "adb"; //$NON-NLS-1$
		}

		bridge = AndroidDebugBridge
				.createBridge(adbLocation, true /* forceNewBridge */);

		// we can't just ask for the device list right away, as the internal
		// thread getting
		// them from ADB may not be done getting the first list.
		// Since we don't really want getDevices() to be blocking, we wait here
		// manually.
		int count = 0;
		while (bridge.hasInitialDeviceList() == false) {
			try {
				Thread.sleep(100);
				count++;
			} catch (InterruptedException e) {
				// pass
			}

			// let's not wait > 10 sec.
			if (count > 100) {
				throw new Exception("Timeout getting device list!");
			}
		}

		return bridge;
	}

	public Device[] fetchDevices() throws Exception {
		// now get the devices
		devices = bridge.getDevices();

		if (devices.length == 0) {
			throw new Exception("No devices found!");
		}
		return devices;
	}

	public Device[] getDevices() {
		return devices;
	}
}
