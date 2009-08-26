package net.autch.androcast;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;

import net.autch.androcast.CustomAdbHelper.AdbResponse;

import com.android.ddmlib.Device;
import com.android.ddmlib.Log;
import com.android.ddmlib.RawImage;

public class LiveCaptureChannel {
	private final Device device;
	private SocketChannel adbChan;
	private RawImage rawImage;
	private byte[] imageBytes;
	 
    // Where to find the ADB bridge.
    final static String ADB_HOST = "127.0.0.1"; //$NON-NLS-1$
    final static int ADB_PORT = 5037;
    private final static byte[] nudge = { 0 };

	public LiveCaptureChannel(Device dev) {
		device = dev;
	}
	
	public RawImage start() throws IOException {
		rawImage = new RawImage();
        byte[] request = CustomAdbHelper.formAdbRequest("framebuffer:"); //$NON-NLS-1$
        
        adbChan = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(ADB_HOST), ADB_PORT));
        adbChan.configureBlocking(true);
        CustomAdbHelper.setDevice(adbChan, device);

        if (CustomAdbHelper.write(adbChan, request) == false)
            throw new IOException("failed asking for frame buffer");

        AdbResponse resp = CustomAdbHelper.readAdbResponse(adbChan, false /* readDiagString */);
        if (!resp.ioSuccess || !resp.okay) {
            adbChan.close();
            throw new IOException("Got timeout or unhappy response from ADB fb req: " + resp.message);
        }

        byte[] reply = new byte[16];
        if (CustomAdbHelper.read(adbChan, reply) == false) {
        	throw new IOException("got partial reply from ADB fb:");
        }
        ByteBuffer buf = ByteBuffer.wrap(reply);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        rawImage.bpp = buf.getInt();
        rawImage.size = buf.getInt();
        rawImage.width = buf.getInt();
        rawImage.height = buf.getInt();

        Log.d("ddms", "image params: bpp=" + rawImage.bpp + ", size="
                + rawImage.size + ", width=" + rawImage.width
                + ", height=" + rawImage.height);

        imageBytes = new byte[rawImage.size];
    	rawImage.data = imageBytes;
    	
    	return rawImage;
	}
	
	public RawImage get() throws IOException {
    	nudge(adbChan);

    	if (CustomAdbHelper.read(adbChan, imageBytes) == false) {
            throw new IOException("got truncated reply from ADB fb data");
        }
    	rawImage.data = imageBytes;
    	return rawImage;
	}

	private static void nudge(SocketChannel adbChan) throws IOException{
        if(CustomAdbHelper.write(adbChan, nudge) == false) {
        	throw new IOException("nudge failed");
        }
	}
	
	public void finish() throws IOException {
        if(adbChan == null) return;
        adbChan.close();
        adbChan = null;
	}

	public RawImage getRawImage() {
		return rawImage;
	}
}
