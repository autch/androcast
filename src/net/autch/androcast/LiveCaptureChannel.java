package net.autch.androcast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.autch.androcast.AdbChannel.AdbResponse;

public class LiveCaptureChannel {
	private AdbChannel adbChan;
	private final String dev_serial;
	private FrameBuffer rawImage;
	private byte[] imageBytes;

	public LiveCaptureChannel(String device) {
		dev_serial = device;
		adbChan = new AdbChannel();
	}

	public FrameBuffer start() throws IOException {
		rawImage = new FrameBuffer();

		adbChan.open();
		adbChan.setDevice(dev_serial);

		byte[] request = adbChan.formAdbRequest("framebuffer:"); //$NON-NLS-1$

		if (adbChan.write(request) == false)
			throw new IOException("failed asking for frame buffer");

		AdbResponse resp = adbChan.readAdbResponse(false /* readDiagString */);
		if (!resp.ioSuccess || !resp.okay) {
			adbChan.close();
			throw new IOException("Got timeout or unhappy response from ADB fb req: " + resp.message);
		}

		byte[] reply = new byte[4];
		if (adbChan.read(reply) == false) {
			throw new IOException("got partial reply from ADB fb:");
		}
		ByteBuffer buf = ByteBuffer.wrap(reply);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		int version = buf.getInt(); 

		reply = new byte[FrameBuffer.getHeaderSize(version) * 4];
		if (adbChan.read(reply) == false) {
			throw new IOException("got partial reply from ADB fb:");
		}
		buf = ByteBuffer.wrap(reply);
		buf.order(ByteOrder.LITTLE_ENDIAN);
		if(rawImage.readHeader(version, buf) == false){
			throw new IOException("Unsupported protocol: " + version);
		}

		imageBytes = new byte[rawImage.size];
		rawImage.data = imageBytes;

		return rawImage;
	}

	public FrameBuffer get() throws IOException {
		//adbChan.nudge();

		if (adbChan.read(imageBytes) == false) {
			throw new IOException("got truncated reply from ADB fb data");
		}
		adbChan.nudge();
		rawImage.data = imageBytes;
		return rawImage;
	}

	public void finish() throws IOException {
		if(adbChan == null) return;
		adbChan.close();
		adbChan = null;
	}

	public FrameBuffer getRawImage() {
		return rawImage;
	}

}
