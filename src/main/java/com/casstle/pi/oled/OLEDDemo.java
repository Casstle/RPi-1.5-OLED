package com.casstle.pi.oled;

import java.io.IOException;

import com.casstle.pi.oled.OLED.OLEDException;
import com.casstle.pi.util.PrintInfo;
import com.pi4j.Pi4J;
import com.pi4j.boardinfo.util.BoardInfoHelper;
import com.pi4j.context.Context;
import com.pi4j.library.pigpio.PiGpio;
import com.pi4j.plugin.linuxfs.provider.i2c.LinuxFsI2CProvider;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalInputProvider;
import com.pi4j.plugin.pigpio.provider.gpio.digital.PiGpioDigitalOutputProvider;
import com.pi4j.plugin.pigpio.provider.pwm.PiGpioPwmProvider;
import com.pi4j.plugin.pigpio.provider.serial.PiGpioSerialProvider;
import com.pi4j.plugin.pigpio.provider.spi.PiGpioSpiProvider;
import com.pi4j.plugin.raspberrypi.platform.RaspberryPiPlatform;

//RST - GPIO 25 pin 22    DC - GPIO 24 pin 18

/**
 * Waveshare 1.5" RGB OLED demo code. See GitHub page for more info. <br>
 * <br>
 * Contact: oled@mail.perske.eu GitHub: https://github.com/DrMarcel/RPi-1.5-OLED
 * Licence: CC0
 * 
 * @author DrMarcel
 */
public class OLEDDemo {
//	private static final byte TCA9534_REG_ADDR_OUT_PORT = 0x01;
//	private static final byte TCA9534_REG_ADDR_CFG = 0x03;

	public static void main(String[] args) {
		try {
			new OLEDDemo();
		} catch (OLEDException | IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Create new OLED demo and stay in infinite loop.
	 * 
	 * @throws OLEDException OLEDException
	 * @throws IOException   IOException
	 */
	public OLEDDemo() throws OLEDException, IOException {
		
        // Initialize Pi4J context
        final var piGpio = PiGpio.newNativeInstance();
        Context pi4j = Pi4J.newContextBuilder()
                .noAutoDetect()
                .add(new RaspberryPiPlatform() {
                    @Override
                    protected String[] getProviders() {
                        return new String[]{};
                    }
                })
                .add(PiGpioDigitalInputProvider.newInstance(piGpio),
                        PiGpioDigitalOutputProvider.newInstance(piGpio) ,
                        PiGpioSpiProvider.newInstance(piGpio)
                )
                .build();

        // Initialize OLED
        
		OLED oled = new OLED(pi4j, 0, 27, 25);
		OLED oled2 = new OLED(pi4j, 1, 22, 24);
		
		System.out.println("OLED Demo screen initialized");

		// Initialize display content
		Clock clock = new Clock(Clock.Mode.Digital);
		ColorPalette colorPalette = new ColorPalette(ColorPalette.Mode.Bars);
		FlyingBird flyingBird = new FlyingBird();

		System.out.println("Objects created");
		// noinspection InfiniteLoopStatement
		colorPalette.SetMode(ColorPalette.Mode.Bars);
		oled.SetContent(colorPalette);
		Sleep(3000, oled);
		oled2.SetContent(colorPalette);
		Sleep(3000, oled2);

		System.out.println("Bars done");
		clock.SetMode(Clock.Mode.Digital);
		oled.SetContent(clock);
		Sleep(2000, oled);
		oled2.SetContent(clock);
		Sleep(2000, oled2);

		System.out.println("Digital Clock done");
		colorPalette.SetMode(ColorPalette.Mode.Smooth);
		oled.SetContent(colorPalette);
		Sleep(3000, oled);
		oled2.SetContent(colorPalette);
		Sleep(3000, oled2);

		System.out.println("Smooth done");
		clock.SetMode(Clock.Mode.Analog);
		oled.SetContent(clock);
		Sleep(2000, oled);
		oled2.SetContent(clock);
		Sleep(2000, oled2);

		System.out.println("Analog clock done");
		oled.SetContent(flyingBird);
		Sleep(5000, oled);
		oled2.SetContent(flyingBird);
		Sleep(5000, oled2);
		System.out.println("Flying bird done");

		oled.ClearScreen();
		oled2.ClearScreen();
		
		System.out.println("OLED Demo screen initialized");

		pi4j.shutdown();
		}

	/**
	 * Sleep while checking for display updates.
	 * 
	 * @param millis Sleep time in milliseconds
	 */
	private void Sleep(int millis, OLED oled) {
		for (int i = 0; i < millis; i++) {
			oled.RepaintIfNeeded();
			// noinspection CatchMayIgnoreException
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
			}
		}
	}
}
