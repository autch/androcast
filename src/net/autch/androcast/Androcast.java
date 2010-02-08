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

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class Androcast {
	AdbChannel adbChan;
	private String[] devices;

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

		adbChan = new AdbChannel();
		try {
			adbChan.open();
			devices = adbChan.getDevices();

			final Androcast app = this;

			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					JFrame f = new AndrocastFrame("Androcast", app);
					f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
					f.setVisible(true);
				}
			});

			adbChan.close();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(null, e.getMessage(),
					"Error detecting devices", JOptionPane.ERROR_MESSAGE);
		} finally {
			adbChan = null;
		}
	}

	public String[] getDevices() {
		return devices;
	}
}
