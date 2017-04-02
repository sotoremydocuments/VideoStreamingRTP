/* ------------------
   Server
   usage: java Server [RTSP listening port]
   ---------------------- */

package com.rtp.server;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.Iterator;
import java.util.StringTokenizer;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.Timer;

import com.rtp.rtppacket.RTPpacket;

public class Server extends JFrame implements ActionListener {

	// RTP variables:
	// ----------------
	DatagramSocket RTPsocket; // socket to be used to send and receive UDP
								// packets
	DatagramPacket senddp; // UDP packet containing the video frames

	InetAddress ClientIPAddr; // Client IP address
	int RTP_dest_port = 0; // destination port for RTP packets (given by the
							// RTSP Client)
	int RTSP_dest_port = 0;

	// GUI:
	// ----------------
	JLabel label;

	// Video variables:
	// ----------------
	int imagenb = 0; // image nb of the image currently transmitted
	VideoStream video; // VideoStream object used to access video frames
	static int MJPEG_TYPE = 26; // RTP payload type for MJPEG video
	static int FRAME_PERIOD = 100; // Frame period of the video to stream, in ms
	static int VIDEO_LENGTH = 500; // length of the video in frames

	Timer timer; // timer used to send the images at the video frame rate
	byte[] buf; // buffer used to store the images to send to the client
	int sendDelay; // the delay to send images over the wire. Ideally should be
					// equal to the frame rate of the video file, but may be
					// adjusted when congestion is detected.

	// RTSP variables
	// ----------------
	// rtsp states
	final static int INIT = 0;
	final static int READY = 1;
	final static int PLAYING = 2;
	// rtsp message types
	final static int SETUP = 3;
	final static int PLAY = 4;
	final static int PAUSE = 5;
	final static int TEARDOWN = 6;
	final static int DESCRIBE = 7;

	static int state; // RTSP Server state == INIT or READY or PLAY
	Socket RTSPsocket; // socket used to send/receive RTSP messages
	// input and output stream filters
	static BufferedReader RTSPBufferedReader;
	static BufferedWriter RTSPBufferedWriter;
	static String VideoFileName; // video file requested from the client
	static int RTSP_ID = 123456; // ID of the RTSP session
	int RTSPSeqNb = 0; // Sequence number of RTSP messages within the session

	// RTCP variables
	// ----------------
	static int RTCP_RCV_PORT = 19000; // port where the client will receive the
										// RTP packets
	static int RTCP_PERIOD = 400; // How often to check for control events
	DatagramSocket RTCPsocket;
	RtcpReceiver rtcpReceiver;
	int congestionLevel;

	// Performance optimization and Congestion control
	ImageTranslator imgTranslator;
	CongestionController cc;
	int transmission_ratio = 1;

	final static String CRLF = "\r\n";

	// --------------------------------
	// Constructor
	// --------------------------------
	public Server() {

		// init Frame
		super("Server");

		// init RTP sending Timer
		sendDelay = FRAME_PERIOD;
		timer = new Timer(sendDelay, this);
		timer.setInitialDelay(0);
		timer.setCoalesce(true);

		// init congestion controller
		cc = new CongestionController(600);

		// allocate memory for the sending buffer
		buf = new byte[20000];

		// Handler to close the main window
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				// stop the timer and exit
				timer.stop();
				rtcpReceiver.stopRcv();
				System.exit(0);
			}
		});

		// init the RTCP packet receiver
		rtcpReceiver = new RtcpReceiver(RTCP_PERIOD);

		// GUI:
		label = new JLabel("Send frame #        ", JLabel.CENTER);
		getContentPane().add(label, BorderLayout.CENTER);

		// Video encoding and quality
		imgTranslator = new ImageTranslator(0.8f);
	}

	// ------------------------------------
	// main
	// ------------------------------------
	public static void main(String argv[]) throws Exception {

		// get RTSP socket port from the command line
		final int RTSPport = Integer.parseInt("1051");// argv[0]

		ServerSocket listenSocket = new ServerSocket(RTSPport);
		/*while (true)*/ {
			// Initiate TCP connection with the client for the RTSP session
			//final Socket acceptSocket = listenSocket.accept();
			RTCP_RCV_PORT += 1;

	/*Runnable b=		new Runnable() {
				public int a;
				public void run() {
					try {
						// create a Server object
						Server theServer = new Server();
a=1;
						// show GUI:
						theServer.pack();
						theServer.setVisible(true);
						theServer.RTSP_dest_port = RTSPport;
						theServer.RTSPsocket = acceptSocket;

						threadDataTransmission(theServer);
					} catch (Exception e) {
						System.out.println("Exception caught: " + e);
					}
				}
			}; b.run();*/
			final Server theServer = new Server();
			// show GUI:
			theServer.pack();
			theServer.setVisible(true);
			theServer.RTSP_dest_port = RTSPport;
			theServer.RTSPsocket = listenSocket.accept();
			RunDataTransmission run = new RunDataTransmission(theServer);
			run.start();
			RTCP_RCV_PORT += 1;
			new Thread(){
				public void run() {
					while(true)
				System.out.println("OK: " + theServer.transmission_ratio);
				};
			}.start();
			
			Server theServer1 = new Server();
			// show GUI:
			theServer1.pack();
			theServer1.setVisible(true);
			theServer1.RTSP_dest_port = RTSPport;
			theServer1.RTSPsocket = listenSocket.accept();
			run = new RunDataTransmission(theServer1);
			run.start();
			RTCP_RCV_PORT += 1;
			
			final Server theServer2 = new Server();
			// show GUI:
			theServer2.pack();
			theServer2.setVisible(true);
			theServer2.RTSP_dest_port = RTSPport;
			theServer2.RTSPsocket = listenSocket.accept();
			run = new RunDataTransmission(theServer2);
			run.start();
			RTCP_RCV_PORT += 1;
			
			
			// listenSocket.close();
		}
	}
	
	public static class RunDataTransmission extends Thread{

		Server theServer;
		float package_send;
		
		public float getPackage_send() {
			return package_send;
		}

		public void setPackage_send(float package_send) {
			this.package_send = package_send;
		}

		public RunDataTransmission(Server server){
			theServer = server;
		}
		
		public void run() {
			// TODO Auto-generated method stub
			try {
				threadDataTransmission(theServer);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}

	private static void threadDataTransmission(Server theServer)
			throws Exception {
		// Get Client IP address
		theServer.ClientIPAddr = theServer.RTSPsocket.getInetAddress();

		// Initiate RTSPstate
		state = INIT;

		// Set input and output stream filters:
		RTSPBufferedReader = new BufferedReader(new InputStreamReader(
				theServer.RTSPsocket.getInputStream()));
		RTSPBufferedWriter = new BufferedWriter(new OutputStreamWriter(
				theServer.RTSPsocket.getOutputStream()));

		// Wait for the SETUP message from the client
		int request_type;
		boolean done = false;
		while (!done) {
			request_type = theServer.parse_RTSP_request(); // blocking

			if (request_type == SETUP) {
				done = true;

				// update RTSP state
				state = READY;
				System.out.println("New RTSP state: READY");

				// Send response
				theServer.send_RTSP_response();

				// init the VideoStream object:
				theServer.video = new VideoStream(VideoFileName);

				// init RTP and RTCP sockets
				theServer.RTPsocket = new DatagramSocket();
				theServer.RTCPsocket = new DatagramSocket(RTCP_RCV_PORT);
			}
		}

		// loop to handle RTSP requests
		while (true) {
			// parse the request
			request_type = theServer.parse_RTSP_request(); // blocking

			if ((request_type == PLAY) && (state == READY)) {
				// send back response
				theServer.send_RTSP_response();
				// start timer
				theServer.timer.start();
				theServer.rtcpReceiver.startRcv();
				// update state
				state = PLAYING;
				System.out.println("New RTSP state: PLAYING");
			} else if ((request_type == PAUSE) && (state == PLAYING)) {
				// send back response
				theServer.send_RTSP_response();
				// stop timer
				theServer.timer.stop();
				theServer.rtcpReceiver.stopRcv();
				// update state
				state = READY;
				System.out.println("New RTSP state: READY");
			} else if (request_type == TEARDOWN) {
				// send back response
				theServer.send_RTSP_response();
				// stop timer
				theServer.timer.stop();
				theServer.rtcpReceiver.stopRcv();
				// close sockets
				theServer.RTSPsocket.close();
				theServer.RTPsocket.close();

				System.exit(0);
			} else if (request_type == DESCRIBE) {
				System.out.println("Received DESCRIBE request");
				theServer.send_RTSP_describe();
			}
		}
	}

	// ------------------------
	// Handler for timer
	// ------------------------
	public void actionPerformed(ActionEvent e) {
		byte[] frame;

		// if the current image nb is less than the length of the video
		if (imagenb < VIDEO_LENGTH) {
			// update current imagenb
			imagenb++;

			try {
				// get next frame to send from the video, as well as its size
				int image_length = video.getnextframe(buf);

				// adjust quality of the image if there is congestion detected
				if (congestionLevel > 0) {
					imgTranslator
							.setCompressionQuality(1.0f - congestionLevel * 0.2f);
					frame = imgTranslator.compress(Arrays.copyOfRange(buf, 0,
							image_length));
					image_length = frame.length;
					System.arraycopy(frame, 0, buf, 0, image_length);
				}

				// Builds an RTPpacket object containing the frame
				RTPpacket rtp_packet = new RTPpacket(MJPEG_TYPE, imagenb,
						imagenb * FRAME_PERIOD, buf, image_length);

				// get to total length of the full rtp packet to send
				int packet_length = rtp_packet.getlength();
				transmission_ratio = packet_length;

				// retrieve the packet bitstream and store it in an array of
				// bytes
				byte[] packet_bits = new byte[packet_length];
				rtp_packet.getpacket(packet_bits);

				// send the packet as a DatagramPacket over the UDP socket
				senddp = new DatagramPacket(packet_bits, packet_length,
						ClientIPAddr, RTP_dest_port);
				RTPsocket.send(senddp);

				System.out.println("Send frame #" + imagenb + ", Frame size: "
						+ image_length + " (" + buf.length + ")");
				// print the header bitstream
				rtp_packet.printheader();

				// update GUI
				label.setText("Send frame #" + imagenb);
			} catch (Exception ex) {
				System.out.println("Exception caught: " + ex);
				System.exit(0);
			}
		} else {
			// if we have reached the end of the video file, stop the timer
			timer.stop();
			rtcpReceiver.stopRcv();
		}
	}

	// ------------------------
	// Controls RTP sending rate based on traffic
	// ------------------------
	class CongestionController implements ActionListener {
		private Timer ccTimer;
		int interval; // interval to check traffic stats
		int prevLevel; // previously sampled congestion level

		public CongestionController(int interval) {
			this.interval = interval;
			ccTimer = new Timer(interval, this);
			ccTimer.start();
		}

		public void actionPerformed(ActionEvent e) {

			// adjust the send rate
			if (prevLevel != congestionLevel) {
				sendDelay = FRAME_PERIOD + congestionLevel
						* (int) (FRAME_PERIOD * 0.1);
				timer.setDelay(sendDelay);
				prevLevel = congestionLevel;
				System.out.println("Send delay changed to: " + sendDelay);
			}
		}
	}

	// ------------------------
	// Listener for RTCP packets sent from client
	// ------------------------
	class RtcpReceiver implements ActionListener {
		private Timer rtcpTimer;
		private byte[] rtcpBuf;
		int interval;

		public RtcpReceiver(int interval) {
			// set timer with interval for receiving packets
			this.interval = interval;
			rtcpTimer = new Timer(interval, this);
			rtcpTimer.setInitialDelay(0);
			rtcpTimer.setCoalesce(true);

			// allocate buffer for receiving RTCP packets
			rtcpBuf = new byte[512];
		}

		public void actionPerformed(ActionEvent e) {
			// Construct a DatagramPacket to receive data from the UDP socket
			DatagramPacket dp = new DatagramPacket(rtcpBuf, rtcpBuf.length);
			float fractionLost;

			try {
				RTCPsocket.receive(dp); // Blocking
				RTCPpacket rtcpPkt = new RTCPpacket(dp.getData(),
						dp.getLength());
				System.out.println("[RTCP] " + rtcpPkt);

				// set congestion level between 0 to 4
				fractionLost = rtcpPkt.fractionLost;
				if (fractionLost >= 0 && fractionLost <= 0.01) {
					congestionLevel = 0; // less than 0.01 assume negligible
				} else if (fractionLost > 0.01 && fractionLost <= 0.25) {
					congestionLevel = 1;
				} else if (fractionLost > 0.25 && fractionLost <= 0.5) {
					congestionLevel = 2;
				} else if (fractionLost > 0.5 && fractionLost <= 0.75) {
					congestionLevel = 3;
				} else {
					congestionLevel = 4;
				}
			} catch (InterruptedIOException iioe) {
				System.out.println("Nothing to read");
			} catch (IOException ioe) {
				System.out.println("Exception caught: " + ioe);
			}
		}

		public void startRcv() {
			rtcpTimer.start();
		}

		public void stopRcv() {
			rtcpTimer.stop();
		}
	}

	// ------------------------------------
	// Translate an image to different encoding or quality
	// ------------------------------------
	class ImageTranslator {

		private float compressionQuality;
		private ByteArrayOutputStream baos;
		private BufferedImage image;
		private Iterator<ImageWriter> writers;
		private ImageWriter writer;
		private ImageWriteParam param;
		private ImageOutputStream ios;

		public ImageTranslator(float cq) {
			compressionQuality = cq;

			try {
				baos = new ByteArrayOutputStream();
				ios = ImageIO.createImageOutputStream(baos);

				writers = ImageIO.getImageWritersByFormatName("jpeg");
				writer = (ImageWriter) writers.next();
				writer.setOutput(ios);

				param = writer.getDefaultWriteParam();
				param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				param.setCompressionQuality(compressionQuality);

			} catch (Exception ex) {
				System.out.println("Exception caught: " + ex);
				System.exit(0);
			}
		}

		public byte[] compress(byte[] imageBytes) {
			try {
				baos.reset();
				image = ImageIO.read(new ByteArrayInputStream(imageBytes));
				writer.write(null, new IIOImage(image, null, null), param);
			} catch (Exception ex) {
				System.out.println("Exception caught: " + ex);
				System.exit(0);
			}
			return baos.toByteArray();
		}

		public void setCompressionQuality(float cq) {
			compressionQuality = cq;
			param.setCompressionQuality(compressionQuality);
		}
	}

	// ------------------------------------
	// Parse RTSP Request
	// ------------------------------------
	private int parse_RTSP_request() {
		int request_type = -1;
		try {
			// parse request line and extract the request_type:
			String RequestLine = RTSPBufferedReader.readLine();
			System.out.println("RTSP Server - Received from Client:");
			System.out.println(RequestLine);

			StringTokenizer tokens = new StringTokenizer(RequestLine);
			String request_type_string = tokens.nextToken();

			// convert to request_type structure:
			if ((new String(request_type_string)).compareTo("SETUP") == 0)
				request_type = SETUP;
			else if ((new String(request_type_string)).compareTo("PLAY") == 0)
				request_type = PLAY;
			else if ((new String(request_type_string)).compareTo("PAUSE") == 0)
				request_type = PAUSE;
			else if ((new String(request_type_string)).compareTo("TEARDOWN") == 0)
				request_type = TEARDOWN;
			else if ((new String(request_type_string)).compareTo("DESCRIBE") == 0)
				request_type = DESCRIBE;

			if (request_type == SETUP) {
				// extract VideoFileName from RequestLine
				VideoFileName = tokens.nextToken();
			}

			// parse the SeqNumLine and extract CSeq field
			String SeqNumLine = RTSPBufferedReader.readLine();
			System.out.println(SeqNumLine);
			tokens = new StringTokenizer(SeqNumLine);
			tokens.nextToken();
			RTSPSeqNb = Integer.parseInt(tokens.nextToken());

			// get LastLine
			String LastLine = RTSPBufferedReader.readLine();
			System.out.println(LastLine);

			tokens = new StringTokenizer(LastLine);
			if (request_type == SETUP) {
				// extract RTP_dest_port from LastLine
				for (int i = 0; i < 3; i++)
					tokens.nextToken(); // skip unused stuff
				RTP_dest_port = Integer.parseInt(tokens.nextToken());
			} else if (request_type == DESCRIBE) {
				tokens.nextToken();
				String describeDataType = tokens.nextToken();
			} else {
				// otherwise LastLine will be the SessionId line
				tokens.nextToken(); // skip Session:
				RTSP_ID = Integer.parseInt(tokens.nextToken());
			}
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}

		return (request_type);
	}

	// Creates a DESCRIBE response string in SDP format for current media
	private String describe() {
		StringWriter writer1 = new StringWriter();
		StringWriter writer2 = new StringWriter();

		// Write the body first so we can get the size later
		writer2.write("v=0" + CRLF);
		writer2.write("m=video " + RTSP_dest_port + " RTP/AVP " + MJPEG_TYPE
				+ CRLF);
		writer2.write("a=control:streamid=" + RTSP_ID + CRLF);
		writer2.write("a=mimetype:string;\"video/MJPEG\"" + CRLF);
		String body = writer2.toString();

		writer1.write("Content-Base: " + VideoFileName + CRLF);
		writer1.write("Content-Type: " + "application/sdp" + CRLF);
		writer1.write("Content-Length: " + body.length() + CRLF);
		writer1.write(body);

		return writer1.toString();
	}

	// ------------------------------------
	// Send RTSP Response
	// ------------------------------------
	private void send_RTSP_response() {
		try {
			RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
			RTSPBufferedWriter.write("Session: " + RTSP_ID + CRLF);
			RTSPBufferedWriter.flush();
			System.out.println("RTSP Server - Sent response to Client.");
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}

	private void send_RTSP_describe() {
		String des = describe();
		try {
			RTSPBufferedWriter.write("RTSP/1.0 200 OK" + CRLF);
			RTSPBufferedWriter.write("CSeq: " + RTSPSeqNb + CRLF);
			RTSPBufferedWriter.write(des);
			RTSPBufferedWriter.flush();
			System.out.println("RTSP Server - Sent response to Client.");
		} catch (Exception ex) {
			System.out.println("Exception caught: " + ex);
			System.exit(0);
		}
	}
}