package ev3ObjectDetection;

import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.utility.Timer;
import lejos.utility.TimerListener;

public class LCDInfo implements TimerListener{
	public static final int LCD_REFRESH = 100;
	private ColorReader colorReader;
	private Timer lcdTimer;
	private TextLCD LCD = LocalEV3.get().getTextLCD();;
	
	// arrays for displaying data
	private float [] color;
	
	/**
	 * 
	 * @param colorReader Corresponding color sensor.
	 */
	public LCDInfo(ColorReader colorReader) {
		this.lcdTimer = new Timer(LCD_REFRESH, this);
		this.colorReader = colorReader;
		color = new float[3];
		lcdTimer.start();
	}
	
	public void timedOut() { 
		color = colorReader.getReadings();
		LCD.clear();
		LCD.drawString("R: ", 0, 0);
		LCD.drawString("G: ", 0, 1);
		LCD.drawString("B: ", 0, 2);
		LCD.drawString(String.valueOf(color[0]), 3, 0);
		LCD.drawString(String.valueOf(color[1]), 3, 1);
		LCD.drawString(String.valueOf(color[2]), 3, 2);
		
		if (colorReader.isObject()) {
			LCD.drawString("BLOCK DETECTED", 0, 4);
			if (colorReader.isBlock()) {
				LCD.drawString("BLOCK", 0, 5);
			} else {
				LCD.drawString("NOT BLOCK", 0, 5);
			}
		} else {
			LCD.drawString("NO BLOCK DETECTED", 0, 4);
		}
	}
}
