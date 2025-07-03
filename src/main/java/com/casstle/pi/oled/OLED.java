package com.casstle.pi.oled;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import com.pi4j.context.Context;
import com.pi4j.io.exception.IOException;
import com.pi4j.io.gpio.digital.DigitalOutput;
import com.pi4j.io.gpio.digital.DigitalState;
import com.pi4j.io.spi.Spi;
import com.pi4j.io.spi.SpiBus;
import com.pi4j.io.spi.SpiMode;

public class OLED {
	private static final int WIDTH = 128, HEIGHT = 128;
	private static final int SPI_SPEED = 10000000;
//	private static final int SPI_SPEED = 32000000;

	final byte WRITE_COMMAND = (byte) 0x5C;

	private final Spi spi;
	private final DigitalOutput rst;
	private final DigitalOutput dc;
	private final Context pi4j;

	public OLED(Context pi4j, int chipSelect, int rstPin, int dcPin) throws OLEDException {
		System.out.println("[OLED]: Starting...");
		this.pi4j = pi4j;

		System.out.println("[OLED]: Initialize SPI");
		try {
			var spiConfig = Spi.newConfigBuilder(pi4j)
            .id("SPI-"+chipSelect)
            .bus(SpiBus.BUS_0)
            .name("OLED")
            .channel(chipSelect)
            .baud(SPI_SPEED) //bit-banging from Bit to SPI-Byte
            .provider("pigpio-spi")
            .build();
			
			spi = pi4j.create(spiConfig);
		} catch (Exception e) {
			throw new OLEDException("Initialize SPI failed: " + e.getMessage());
		}

		System.out.println("[OLED]: Initialize GPIO pins");
		var rstConfig = DigitalOutput.newConfigBuilder(pi4j)
									 .id("RST-"+chipSelect)
									 .address(rstPin)
									 .initial(DigitalState.HIGH)
									 .shutdown(DigitalState.HIGH)
									 .provider("pigpio-digital-output")
									 .build();
		rst = pi4j.create(rstConfig);

		var dcConfig = DigitalOutput.newConfigBuilder(pi4j)
									.id("DC-"+chipSelect)
									.address(dcPin)
									.initial(DigitalState.LOW)
									.shutdown(DigitalState.LOW)
									.provider("pigpio-digital-output")
									.build();
		dc = this.pi4j.create(dcConfig);

		Reset();
		Initialize();
	}

	private BufferedImage displayBuffer = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
	private AtomicBoolean writeBusy = new AtomicBoolean(false);

	private Thread WriteDisplayBufferThread;
	private Runnable WriteDisplayBufferRunner = () -> {
		// Data structure [B,G,R,B,G,R,...]
		int[] data = new int[WIDTH * HEIGHT * 3];
		data = displayBuffer.getData().getPixels(0, 0, WIDTH, HEIGHT, data);

		// Shift display buffer data to 6:6:6 format
		byte[] bdata = new byte[WIDTH * HEIGHT * 3];
		for (int i = 0; i < WIDTH * HEIGHT * 3; i++)
			//bdata[i] = (byte) (0xFF & (data[i] >> 2));
			bdata[i] = (byte) data[i];

		final int parLines = 4; // send 4 lines at once - maximum payload length restricted by the SPI driver
		for (int i = 0; i < HEIGHT / parLines; i++) {
			// noinspection CatchMayIgnoreException
			try {
				Write(WRITE_COMMAND, Arrays.copyOfRange(bdata, i * 128 * 3 * parLines, (i + 1) * 128 * 3 * parLines));
			} catch (OLEDException e) {
			}
		}

		writeBusy.set(false);
	};

	/**
	 * Check if current display content is invalidated and trigger
	 * Paint(...)-method. Has to be called cyclic in the main Thread.
	 */
	public void RepaintIfNeeded() {
		// Check if content to draw available and no write operation active
		if (content != null && !writeBusy.get()) {
			// Calculate time since last repaint
			int dt = (int) (System.currentTimeMillis() - content.lastRepaintMillis);
			// if autorefresh is set and refresh time exceeds request repaint
			if (content.autorefresh && dt > 1000 / content.framerate)
				content.Invalidate();

			// Check if content needs repaint - may be triggered from the OLEDContent Object
			// or by the autorefresh timer
			if (content.Invalidated()) {
				content.lastRepaintMillis = System.currentTimeMillis();
				content.Paint(displayBuffer.getGraphics());
				// Send new data to OLED
				writeBusy.set(true);
				WriteDisplayBufferThread = new Thread(WriteDisplayBufferRunner);
				WriteDisplayBufferThread.start();
				content.repaint.set(false);
			}
		}
	}

	private OLEDContent content;

	/**
	 * Set display content
	 *
	 * @param c Display content to draw or null
	 */
	public void SetContent(OLEDContent c) {
		if (c != null)
			c.Invalidate();
		content = c;
	}

	/**
	 * Remove current display content
	 */
	public void ClearScreen() throws OLEDException {
		SetContent(null);

		// Empty data
		final int parLines = 4; // send 4 lines at once - maximum payload length restricted by the SPI driver
		byte[] lclear = new byte[WIDTH * 3 * parLines];
		for (int i = 0; i < HEIGHT / parLines; i++) {
			Write(WRITE_COMMAND, lclear);
		}
	}

	/**
	 * Abstract display content class. To draw on the display extend this class and
	 * set the display content with oled.SetContent(...). The Paint(...)-method will
	 * be called every time the display needs a redraw. This can be triggered
	 * manually by calling the Invalidate()-method within the OLEDContent object or
	 * automatically if auto refresh is enabled. To enable the auto refresh call
	 * EnableAutoRefresh(...). <br>
	 * <br>
	 * Important: The oled.RepaintIfNeeded()-method has to be called cyclic in the
	 * main Thread.
	 */
	public static abstract class OLEDContent {
		private AtomicBoolean repaint = new AtomicBoolean(false);
		private boolean autorefresh = false;
		private int framerate = 20;
		private long lastRepaintMillis = 0;

		/**
		 * Enable auto refresh to call Paint(...) automatically with the given framerate
		 *
		 * @param framerate Framerate
		 */
		protected void EnableAutoRefresh(int framerate) {
			autorefresh = true;
			this.framerate = framerate;
		}

		/**
		 * Disable auto refresh. Paint needs to be triggered by calling the
		 * Invalidate()-method
		 */
		protected void DisableAutoRefresh() {
			autorefresh = false;
		}

		/**
		 * Trigger repaint
		 */
		protected void Invalidate() {
			repaint.set(true);
		}

		/**
		 * Check if the display content needs repaint
		 *
		 * @return true if invalidated
		 */
		protected boolean Invalidated() {
			return repaint.get();
		}

		/**
		 * Overwrite this method to draw on the display
		 *
		 * @param g Graphics Object of the display
		 */
		protected abstract void Paint(Graphics g);
	}

	/**
	 * Display hardware reset
	 */
	private void Reset() {
		System.out.println("[OLED]: Reset");

		rst.high();
		// noinspection CatchMayIgnoreException
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		rst.low();
		// noinspection CatchMayIgnoreException
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
		rst.high();
		// noinspection CatchMayIgnoreException
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
		}
	}

	/**
	 * Initialize display parameters
	 *
	 * @throws OLEDException OLED Exception
	 */
	private void Initialize() throws OLEDException {
		System.out.println("[OLED]: Initialize...");

		// Copied from Waveshare sample code
		Write((byte) 0xfd, (byte) 0x12); // command lock
		Write((byte) 0xfd, (byte) 0xB1); // command lock

		Write((byte) 0xae); // Display off
		Write((byte) 0xa4); // Normal display mode

		Write((byte) 0x15, (byte) 0x00, (byte) 0x7F); // set column address 0-127
		Write((byte) 0x75, (byte) 0x00, (byte) 0x7F); // set row address 0-127

		Write((byte) 0xB3, (byte) 0xF1); // Set display frequency (b7-b4) and Prescaler (b3-b0)

		Write((byte) 0xCA, (byte) 0x7F); // ?

		Write((byte) 0xa0, (byte) 0x74); // set re-map & data format, Horizontal address increment

		Write((byte) 0xa1, (byte) 0x00); // set display start line, start 00 line

		Write((byte) 0xa2, (byte) 0x00); // set display offset

		Write((byte) 0xAB); // ?

		Write((byte) 0x01); // ?

		Write((byte) 0xB4, (byte) 0xA0, (byte) 0xB5, (byte) 0x55); // ?

		Write((byte) 0xC1, (byte) 0xC8, (byte) 0x80, (byte) 0xC0); // ?

		Write((byte) 0xC7, (byte) 0x0F); // ?

		Write((byte) 0xB1, (byte) 0x32); // ?

		Write((byte) 0xB2, (byte) 0xA4, (byte) 0x00, (byte) 0x00); // ?

		Write((byte) 0xBB, (byte) 0x17); // ?

		Write((byte) 0xB6, (byte) 0x01); // ?

		Write((byte) 0xBE, (byte) 0x05); // ?

		Write((byte) 0xA6); // Normal non-inverse display mode

		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Write((byte) 0xaf); // turn on oled panel

		ClearScreen();

		System.out.println("[OLED]: Ready");
	}

	/**
	 * Send command
	 *
	 * @param command Command to send
	 * @throws OLEDException OLED Exception
	 */
	private void Write(byte command) throws OLEDException {
		Write(command, null);
	}

	/**
	 * Send command with parameter
	 *
	 * @param command Command to send
	 * @param d1      Command parameter
	 * @throws OLEDException OLED Exception
	 */
	private void Write(byte command, byte d1) throws OLEDException {
		Write(command, new byte[] { d1 });
	}

	/**
	 * Send command with multiple parameter
	 *
	 * @param command Command to send
	 * @param d1      Command parameter
	 * @param d2      Command parameter
	 * @throws OLEDException OLED Exception
	 */
	private void Write(byte command, byte d1, byte d2) throws OLEDException {
		Write(command, new byte[] { d1, d2 });
	}

	/**
	 * Send command with multiple parameter
	 *
	 * @param command Command to send
	 * @param d1      Command parameter
	 * @param d2      Command parameter
	 * @param d3      Command parameter
	 * @throws OLEDException OLED Exception
	 */
	private void Write(byte command, byte d1, byte d2, byte d3) throws OLEDException {
		Write(command, new byte[] { d1, d2, d3 });
	}

	/**
	 * Send command with data array
	 *
	 * @param command Command to send
	 * @param data    Data to send
	 * @throws OLEDException OLED Exception
	 */
	private void Write(byte command, byte[] data) throws OLEDException {
		try {
			dc.low();
			spi.write(command);

			if (data != null) {
				dc.high();
				spi.write(data);
			}
		} catch (IOException e) {
			throw new OLEDException("OLED IO Exception: " + e.getMessage());
		}
	}

	/**
	 * Exception on OLED errors
	 */
	public static class OLEDException extends Exception {
		public OLEDException(String message) {
			super(message);
		}
	}

}
