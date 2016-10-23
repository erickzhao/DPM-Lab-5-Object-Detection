package ev3ObjectDetection;

import lejos.hardware.*;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.*;
import lejos.robotics.SampleProvider;

public class ObjectDetectionLab {

	private static final EV3LargeRegulatedMotor leftMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("D"));
	private static final EV3LargeRegulatedMotor rightMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("A"));
	private static final Port usPort = LocalEV3.get().getPort("S1");		
	private static final Port colorPort = LocalEV3.get().getPort("S4");
	
	public static final double WHEEL_RADIUS = 2.13;
	public static final double WHEEL_BASE = 15.13;

	
	public static void main(String[] args) {
		
		int buttonChoice;
		final TextLCD t = LocalEV3.get().getTextLCD();
		
		
		do {
			t.clear();
			
			t.drawString("< Left | Right >", 0, 0);
			t.drawString("       |        ", 0, 1);
			t.drawString(" detect| push   ", 0, 2);
			t.drawString(" object| block  ", 0, 3);
			
			buttonChoice = Button.waitForAnyPress();
		} while (buttonChoice != Button.ID_LEFT && buttonChoice != Button.ID_RIGHT);
		
		
		if (buttonChoice == Button.ID_LEFT) {
			//set up color sensor
			@SuppressWarnings("resource")
			SensorModes colorSensor = new EV3ColorSensor(colorPort);
			SampleProvider colorValue = colorSensor.getMode("RGB");
			float[] colorData = new float[colorValue.sampleSize()];
			
			//initialize color reader and LCD display
			ColorReader colorReader = new ColorReader(colorValue,colorData);
			LCDInfo lcd = new LCDInfo(colorReader);
			colorReader.start();
			
			while(Button.waitForAnyPress() != Button.ID_ESCAPE);
			System.exit(0);
		} else {
			t.clear();
			@SuppressWarnings("resource")    
			SensorModes usSensor = new EV3UltrasonicSensor(usPort);
			SampleProvider usValue = usSensor.getMode("Distance");
			float[] usData = new float[usValue.sampleSize()];
			
			Odometer odo = new Odometer(leftMotor, rightMotor, 30, true);
			Localizer usl = new Localizer(odo, usValue, usData);
			
			usl.doLocalization();
		}
		
		
		
	}
}
