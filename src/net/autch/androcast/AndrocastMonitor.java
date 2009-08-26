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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import com.android.ddmlib.Device;
import com.android.ddmlib.RawImage;

public class AndrocastMonitor extends SwingWorker<Object, Boolean> {
	private final CapturePanel panel;
	private final LiveCaptureChannel channel;
	private boolean landscape;
	private boolean resize;
	private BufferedImage image;
	private int width, height; // w/h of rawImage
	private int raw_width, raw_height; // w/h of rotated rawImage
	private int img_width, img_height; // w/h of rendered img
	private double zoom;

	public AndrocastMonitor(CapturePanel cmp, Device dev) {
		super();

		channel = new LiveCaptureChannel(dev);
		panel = cmp;
		landscape = false;
		resize = true;
		zoom = 1.0;
	}

	@Override
	protected Object doInBackground() {
		RawImage rawImage;
		try {
			rawImage = channel.start();
			if (rawImage == null) {
				System.err.println("LiveCaptureChannel.start() returned null");
				return null;
			}
			while (!isCancelled()) {
				rawImage = channel.get();
				assert rawImage.bpp == 16;

				synchronized(panel) {
					if (resize) {
						width = raw_width = rawImage.width;
						height = raw_height = rawImage.height;
						if (landscape) {
							raw_width = height;
							raw_height = width;
						}
						img_width = (int) (raw_width * zoom);
						img_height = (int) (raw_height * zoom);

						// reconstruct image buffer
						image = new BufferedImage(raw_width, raw_height,
								BufferedImage.TYPE_INT_ARGB);

						SwingUtilities.invokeAndWait(new Runnable() {
							public void run() {
								panel.setImage(image, img_width, img_height, raw_width, raw_height);
							}
						});
						resize = false;
					}
					if (landscape) {
						transformRawImageL(image, rawImage);
					} else {
						transformRawImageP(image, rawImage);
					}
				}
				publish(true);

				try {
					Thread.sleep(1000 / 30); // 15.15fps
				} catch (InterruptedException ie) {
					// thru
				}
			}
		} catch (IOException ioe) {
			System.err.println("Unable to get frame buffer: " + ioe.getMessage());
		} catch (Exception e) {
			System.err.println("Uncaught exception: " + e);
			e.printStackTrace();
		} finally {
			try {
				if(channel != null)
					channel.finish();
			} catch (IOException e) {
				System.err.println("Nudge failed " + e.getMessage());
			}
		}
		return null; // return your result
	}

	@Override
	protected synchronized void process(List<Boolean> b) {
		panel.repaint();
	}

	public synchronized void render(Graphics g) {
		g.drawImage(image, 0, 0, img_width, img_height, 0, 0, raw_width, raw_height, null);
	}

	private static void transformRawImageP(BufferedImage image,
			RawImage rawImage) {
		// convert raw data to an Image
		byte[] buffer = rawImage.data;
		int index = 0;
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++) {

				int value = buffer[index++] & 0x00FF;
				value |= (buffer[index++] << 8) & 0x0FF00;

				int r = ((value >> 11) & 0x01F) << 3;
				int g = ((value >> 5) & 0x03F) << 2;
				int b = ((value >> 0) & 0x01F) << 3;

				value = 0xFF << 24 | r << 16 | g << 8 | b;

				image.setRGB(x, y, value);
			}
		}
	}

	private static void transformRawImageL(BufferedImage image,
			RawImage rawImage) {
		// convert raw data to an Image
		byte[] buffer = rawImage.data;
		int index = 0;
		for (int y = 0; y < rawImage.height; y++) {
			for (int x = 0; x < rawImage.width; x++) {

				int value = buffer[index++] & 0x00FF;
				value |= (buffer[index++] << 8) & 0x0FF00;

				int r = ((value >> 11) & 0x01F) << 3;
				int g = ((value >> 5) & 0x03F) << 2;
				int b = ((value >> 0) & 0x01F) << 3;

				value = 0xFF << 24 | r << 16 | g << 8 | b;

				image.setRGB(y, rawImage.width - x - 1, value);
			}
		}
	}

	public synchronized BufferedImage getImage() {
		return image;
	}

	public void setLandscape(boolean f) {
		landscape = f;
		resize = true;
	}

	public void setZoom(double z) {
		zoom = z;
		resize = true;
	}
}
