package ev3ObjectDetection;

import lejos.robotics.SampleProvider;

public class ColorReader extends Thread {
	private SampleProvider colorSensor;
	private float[] colorData;
	private boolean isObject = false;
	private boolean isBlock = false;
	
	private static final float MIN_OBJECT_READING_VALUE= (float)0.015;
	
	public ColorReader(SampleProvider colorSensor, float[] colorData) {
		this.colorSensor = colorSensor;
		this.colorData = colorData;
	}
	
	@Override
	public void run() {
		while (true) {
			colorSensor.fetchSample(colorData, 0);
			readSensor();
		}
	}
	
	/**
	 * Function that reads the sensor and determines whether we're facing an object or block.
	 * 
	 * By experimentation, we figured out that with styrofoam blocks, the green reading is
	 * approximately double the value of the red reading, and that the opposite is true for
	 * the wooden blocks.
	 * 
	 * Also, we have a minimum threshold value for detecting a set of color values as an object.
	 */
	private void readSensor() {
		float redReading = colorData[0];
		float greenReading = colorData[1];
		
		isObject = (redReading >MIN_OBJECT_READING_VALUE || greenReading > MIN_OBJECT_READING_VALUE) ? true:false;
		
		if (isObject) {
			isBlock = (greenReading > redReading) ? true:false;
		}
	}
	
	/**
	 * Getter for color data.
	 * @return The colorData RGB readings 
	 */
	public float[] getReadings() {
		return this.colorData;
	}
	
	/**
	 * Determines whether or not an object is detected by the color sensor.
	 * @return Boolean indicating whether nor not we have an object detected.
	 */
	public boolean isObject() {
		return this.isObject;
	}
	
	/**
	 * Determines whether or not a styrofoam block is detected by the color sensor.
	 * @return Boolean indicating whether or not we have a styrofoam block detected.
	 */
	public boolean isBlock() {
		return this.isBlock;
	}
}
