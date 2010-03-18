package net.autch.androcast;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import net.autch.androcast.AdbChannel.AdbResponse;

public class LiveCaptureChannel {
	private AdbChannel adbChan;
	private final String dev_serial;
	private final FrameBuffer rawImage;
	private byte[] imageBytes;
	private final byte[] request = AdbChannel.formAdbRequest("framebuffer:"); //$NON-NLS-1$
	private final byte[] buf_version = new byte[4];
	private byte[] buf_header;
	private final ByteBuffer bb_version;
	private ByteBuffer bb_header;
	private int version;

	public LiveCaptureChannel(String device) {
		dev_serial = device;
		adbChan = new AdbChannel();
		rawImage = new FrameBuffer();

		buf_header = null;
		bb_header = null;

		bb_version = ByteBuffer.wrap(buf_version);
		bb_version.order(ByteOrder.LITTLE_ENDIAN);
	}

	public FrameBuffer start() throws IOException {
		adbChan.open();
		adbChan.setDevice(dev_serial);

		if (adbChan.write(request) == false)
			throw new IOException("failed asking for frame buffer");

		AdbResponse resp = adbChan.readAdbResponse(false /* readDiagString */);
		if (!resp.ioSuccess || !resp.okay) {
			adbChan.close();
			throw new IOException("Got timeout or unhappy response from ADB fb req: " + resp.message);
		}

		if (adbChan.read(buf_version) == false) {
			throw new IOException("got partial reply from ADB fb:");
		}
		bb_version.rewind();
		version = bb_version.getInt(); 

		if(buf_header == null) {
			buf_header = new byte[FrameBuffer.getHeaderSize(version) * 4];
			bb_header = ByteBuffer.wrap(buf_header);
			bb_header.order(ByteOrder.LITTLE_ENDIAN);
		}
		if (adbChan.read(buf_header) == false) {
			throw new IOException("got partial reply from ADB fb:");
		}
		bb_header.rewind();
		if(rawImage.readHeader(version, bb_header) == false){
			throw new IOException("Unsupported protocol: " + version);
		}

		if(imageBytes == null || imageBytes.length != rawImage.size){
			imageBytes = new byte[rawImage.size];
			rawImage.data = imageBytes;
		}

		if(version != 16) {
			// 新しいデバイスでは one-shot しか撮れない :-(
			if (adbChan.read(imageBytes) == false) {
				throw new IOException("got truncated reply from ADB fb data");
			}
			adbChan.close();
		}

		return rawImage;
	}

	public FrameBuffer get() throws IOException {
		// 古いデバイスは nudge すれば次のフレームがもらえる
		if(version == 16) {
			adbChan.nudge();
			if (adbChan.read(imageBytes) == false) {
				throw new IOException("got truncated reply from ADB fb data");
			}
			rawImage.data = imageBytes;
			return rawImage;
		} else {
			// 新しいデバイスでは start() をやり直す
			return start(); 
		}
	}

	public void finish() throws IOException {
		if(adbChan == null) return;
		adbChan.close();
		adbChan = null;
	}

	public FrameBuffer getRawImage() {
		return rawImage;
	}

	public int getPreferedFramerate() {
		int framerate;

		switch(version) {
		case 16:
			framerate = 10;
			break;
		default:
			framerate = 5;	
		}
		return framerate;
	}
}
