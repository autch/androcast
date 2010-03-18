/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package net.autch.androcast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 *
 * @author nishimura
 */
public class AdbChannel {
	private SocketChannel adbChan;
	private final String adbHost;
	private final int adbPort;

	// Where to find the ADB bridge.
	static final String ADB_HOST = "127.0.0.1"; //$NON-NLS-1$
	static final int ADB_PORT = 5037;
	static final String DEFAULT_ENCODING = "ISO-8859-1"; //$NON-NLS-1$
	static final int WAIT_TIME = 5; // spin-wait sleep, in ms
	static final int STD_TIMEOUT = 5000; // standard delay, in ms
	private static final byte[] nudge = { 0 };

	/**
	 * Response from ADB.
	 */
	static class AdbResponse {
		public AdbResponse() {
			// ioSuccess = okay = timeout = false;
			message = "";
		}
		public boolean ioSuccess; // read all expected data, no timeoutes
		public boolean okay; // first 4 bytes in response were "OKAY"?
		public boolean timeout; // TODO: implement
		public String message; // diagnostic string
	}

	/**
	 * Create an ASCII string preceeded by four hex digits. The opening "####"
	 * is the length of the rest of the string, encoded as ASCII hex (case
	 * doesn't matter). "port" and "host" are what we want to forward to. If
	 * we're on the host side connecting into the device, "addrStr" should be
	 * null.
	 */
	static byte[] formAdbRequest(String req) {
		String resultStr = String.format("%04X%s", req.length(), req); //$NON-NLS-1$
		byte[] result;
		try {
			result = resultStr.getBytes(DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace(); // not expected
			return null;
		}
		assert result.length == req.length() + 4;
		return result;
	}

	/**
	 * Reads from the socket until the array is filled, or no more data is coming (because
	 * the socket closed or the timeout expired).
	 *
	 * @param chan the opened socket to read from. It must be in non-blocking
	 *      mode for timeouts to work
	 * @param data the buffer to store the read data into.
	 * @return "true" if all data was read.
	 * @throws IOException
	 */
	boolean read(byte[] data) {
		try {
			read(data, -1, STD_TIMEOUT);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * Reads from the socket until the array is filled, the optional length
	 * is reached, or no more data is coming (because the socket closed or the
	 * timeout expired). After "timeout" milliseconds since the
	 * previous successful read, this will return whether or not new data has
	 * been found.
	 *
	 * @param chan the opened socket to read from. It must be in non-blocking
	 *      mode for timeouts to work
	 * @param data the buffer to store the read data into.
	 * @param length the length to read or -1 to fill the data buffer completely
	 * @param timeout The timeout value. A timeout of zero means "wait forever".
	 * @throws IOException
	 */
	void read(byte[] data, int length, int timeout) throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
		int numWaits = 0;

		while (buf.position() != buf.limit()) {
			int count;

			count = adbChan.read(buf);
			if (count < 0) {
				throw new IOException("EOF");
			} else if (count == 0) {
				// TODO: need more accurate timeout?
				if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
					throw new IOException("timeout");
				}
				// non-blocking spin
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
				}
				numWaits++;
			} else {
				numWaits = 0;
			}
		}
	}

	/**
	 * Write until all data in "data" is written or the connection fails.
	 * @param chan the opened socket to write to.
	 * @param data the buffer to send.
	 * @return "true" if all data was written.
	 */
	boolean write(byte[] data) {
		try {
			write(data, -1, STD_TIMEOUT);
		} catch (IOException e) {
			return false;
		}

		return true;
	}

	/**
	 * Write until all data in "data" is written, the optional length is reached,
	 * the timeout expires, or the connection fails. Returns "true" if all
	 * data was written.
	 * @param chan the opened socket to write to.
	 * @param data the buffer to send.
	 * @param length the length to write or -1 to send the whole buffer.
	 * @param timeout The timeout value. A timeout of zero means "wait forever".
	 * @throws IOException
	 */
	void write(byte[] data, int length, int timeout)
	throws IOException {
		ByteBuffer buf = ByteBuffer.wrap(data, 0, length != -1 ? length : data.length);
		int numWaits = 0;

		while (buf.position() != buf.limit()) {
			int count;

			count = adbChan.write(buf);
			if (count < 0) {
				throw new IOException("channel EOF");
			} else if (count == 0) {
				// TODO: need more accurate timeout?
				if (timeout != 0 && numWaits * WAIT_TIME > timeout) {
					throw new IOException("timeout");
				}
				// non-blocking spin
				try {
					Thread.sleep(WAIT_TIME);
				} catch (InterruptedException ie) {
				}
				numWaits++;
			} else {
				numWaits = 0;
			}
		}
	}

	/**
	 * tells adb to talk to a specific device
	 *
	 * @param adbChan the socket connection to adb
	 * @param device The device to talk to.
	 * @throws IOException
	 */
	void setDevice(String device)
	throws IOException {
		// if the device is not -1, then we first tell adb we're looking to talk
		// to a specific device
		if (device != null) {
			String msg = "host:transport:" + device; //$NON-NLS-1$
			byte[] device_query = formAdbRequest(msg);

			if (write(device_query) == false)
				throw new IOException("failed submitting device (" + device +
				") request to ADB");

			AdbResponse resp = readAdbResponse(false /* readDiagString */);
			if (!resp.okay)
				throw new IOException("device (" + device +
						") request rejected: " + resp.message);
		}

	}

	/**
	 * Reads the response from ADB after a command.
	 * @param chan The socket channel that is connected to adb.
	 * @param readDiagString If true, we're expecting an OKAY response to be
	 *      followed by a diagnostic string. Otherwise, we only expect the
	 *      diagnostic string to follow a FAIL.
	 */
	AdbResponse readAdbResponse(boolean readDiagString)
	throws IOException {

		AdbResponse resp = new AdbResponse();

		byte[] reply = new byte[4];
		if (read(reply) == false) {
			return resp;
		}
		resp.ioSuccess = true;

		if (isOkay(reply)) {
			resp.okay = true;
		} else {
			readDiagString = true; // look for a reason after the FAIL
			resp.okay = false;
		}

		// not a loop -- use "while" so we can use "break"
		while (readDiagString) {
			// length string is in next 4 bytes
			byte[] lenBuf = new byte[4];
			if (read(lenBuf) == false) {
				break;
			}

			String lenStr = replyToString(lenBuf);

			int len;
			try {
				len = Integer.parseInt(lenStr, 16);
			} catch (NumberFormatException nfe) {
				break;
			}

			byte[] msg = new byte[len];
			if (read(msg) == false) {
				break;
			}

			resp.message = replyToString(msg);
			break;
		}

		return resp;
	}

	/**
	 * Checks to see if the first four bytes in "reply" are OKAY.
	 */
	static boolean isOkay(byte[] reply) {
		return reply[0] == (byte)'O' && reply[1] == (byte)'K'
		&& reply[2] == (byte)'A' && reply[3] == (byte)'Y';
	}

	/**
	 * Converts an ADB reply to a string.
	 */
	String replyToString(byte[] reply) {
		String result;
		try {
			result = new String(reply, DEFAULT_ENCODING);
		} catch (UnsupportedEncodingException uee) {
			uee.printStackTrace(); // not expected
			result = "";
		}
		return result;
	}

	public AdbChannel()
	{
		this(null, -1);
	}

	public AdbChannel(String host, int port)
	{
		adbHost = host == null ? ADB_HOST : host;
		adbPort = port == -1 ? ADB_PORT : port;
	}

	public void open() throws IOException
	{
		adbChan = SocketChannel.open(new InetSocketAddress(InetAddress.getByName(adbHost), adbPort));
		adbChan.configureBlocking(true);
	}

	public void nudge() throws IOException {
		if(write(nudge) == false) {
			throw new IOException("nudge failed");
		}
	}

	public void close() throws IOException
	{
		adbChan.close();
		adbChan = null;
	}

	String[] getDevices() throws IOException
	{
		String msg = "host:devices";
		byte[] query = formAdbRequest(msg);

		if (write(query) == false)
			throw new IOException("failed devices request to ADB");

		AdbResponse resp = readAdbResponse(true /* readDiagString */);
		if (!resp.okay)
			throw new IOException("devices request rejected: " + resp.message);

		if(resp.message.length() == 0) {
			throw new IOException("no devices detected");
		}

		String[] devStr = resp.message.split("\n");
		String[] devices = new String[devStr.length];
		int i = 0;
		for(String d: devStr) {
			String[] params = d.split("\t");
			if(params.length == 2) {
				devices[i] = params[0];
				i++;
			}
		}
		return devices;
	}
}
