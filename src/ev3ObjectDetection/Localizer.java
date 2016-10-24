package ev3ObjectDetection;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;

public class Localizer {

	private Odometer odo;
	private SampleProvider usSensor;
	private float[] usData;
	private Navigation nav;
	
	private boolean noiseZone = false;
	private static final Port colorPort = LocalEV3.get().getPort("S4");
	private static final float MAX_DISTANCE = 28;
	private static final float EDGE_DISTANCE = (float) 11.4;
	private static final float MARGIN_DISTANCE = (float) 0.3; 
	private static final float MOTOR_SPEED = 75;
	private static final double GRID_WIDTH = 30.48;
	private static final double CENTRE_TO_US_SENSOR = 11.7;
	
	public Localizer(Odometer odo, SampleProvider usSensor, float[] usData) {
		this.odo = odo;
		this.usSensor = usSensor;
		this.usData = usData;
		this.nav = new Navigation(odo);
	}
	
	public void doLocalization() {
		double [] pos = new double [3];
		double angleA = 0;
		double angleB = 0;
		// rotate the robot until it sees no wall
		
		nav.setSpeeds(MOTOR_SPEED,-MOTOR_SPEED);
		
		while (true) {
			if (getFilteredData()>=MAX_DISTANCE) {
				break;
			}
		}
		// find first angle and latch
		while (true) {
			if (!noiseZone && getFilteredData()<EDGE_DISTANCE+MARGIN_DISTANCE) {
				angleA = odo.getAng();
				noiseZone = true;
				Sound.beep();
			}
			if (noiseZone && getFilteredData()<EDGE_DISTANCE-MARGIN_DISTANCE){
				angleA = (angleA + odo.getAng())/2;
				noiseZone = false;
				Sound.beep();
				break;
			}
		}
		
		// switch direction and wait until it sees no wall
		
		nav.setSpeeds(-MOTOR_SPEED,MOTOR_SPEED);
		
		while (true) {
			if (getFilteredData()>=MAX_DISTANCE) {
				break;
			}
		}
		
		// find second angle and latch
		while (true) {
			if (!noiseZone && getFilteredData()<EDGE_DISTANCE+MARGIN_DISTANCE) {
				angleB = odo.getAng();
				noiseZone = true;
				Sound.beep();
			}
			if (noiseZone && getFilteredData()<EDGE_DISTANCE-MARGIN_DISTANCE){
				angleB = (angleB + odo.getAng())/2;
				noiseZone = false;
				Sound.beep();
				break;
			}
		}
		// angleA is clockwise from angleB, so assume the average of the
		// angles to the right of angleB is 45 degrees past 'north'
		
		double endAngle = getEndAngle(angleA,angleB);
		if (endAngle<0) {
			nav.turnTo(endAngle+360,true);
		} else if (endAngle>360){
			nav.turnTo(endAngle-360,true);
		} else {
			nav.turnTo(endAngle, true);
		}
		
		// update the odometer position
		resetPosition();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		moveToLocation();
	}
	
	public float getFilteredData() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0]*100;
		
		if (distance > MAX_DISTANCE) distance = MAX_DISTANCE;
		
		return distance;
	}
	
	private double getEndAngle(double a, double b) {
		if (a > b) {
			return ((a+b)/2 - 225);
		}
		return ((a+b)/2 - 45);
	}
	
	private void moveToLocation() {
		double distanceX, distanceY;
		
		nav.turnTo(180,true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		distanceX = GRID_WIDTH-getFilteredData()-CENTRE_TO_US_SENSOR;
		
		nav.turnTo(270, true);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		distanceY = GRID_WIDTH-getFilteredData()-CENTRE_TO_US_SENSOR;
		
		nav.travelTo(distanceX,distanceY);
		nav.turnTo(0, true);
		resetPosition();
		startScanning();
	}
	
	private void resetPosition() {
		odo.setPosition(new double [] {0.0, 0.0, 0.0}, new boolean [] {true, true, true});
	}
	
	private void startScanning() {

		//set up color sensor
		@SuppressWarnings("resource")
		SensorModes colorSensor = new EV3ColorSensor(colorPort);
		SampleProvider colorValue = colorSensor.getMode("RGB");
		float[] colorData = new float[colorValue.sampleSize()];
		
		//initialize color reader and LCD display
		ColorReader colorReader = new ColorReader(colorValue,colorData);
		//LCDInfo lcd = new LCDInfo(colorReader);
		colorReader.start();
		
		Detector detect = new Detector(odo, nav, usSensor, usData, colorReader);
		detect.start();
	}
}
