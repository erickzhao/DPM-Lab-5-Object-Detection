package ev3ObjectDetection;

import lejos.hardware.Sound;
import lejos.hardware.ev3.LocalEV3;
import lejos.hardware.motor.EV3LargeRegulatedMotor;
import lejos.hardware.motor.EV3MediumRegulatedMotor;
import lejos.robotics.SampleProvider;

public class Detector extends Thread{
	
	private static final EV3LargeRegulatedMotor clawMotor = new EV3LargeRegulatedMotor(LocalEV3.get().getPort("C"));
	private static final EV3MediumRegulatedMotor usMotor = new EV3MediumRegulatedMotor(LocalEV3.get().getPort("B"));
	private Odometer odo;
	private Navigation nav;
	private SampleProvider usSensor;
	private float[] usData;
	ColorReader colorReader;
	
	private static final float MAX_DISTANCE=60;
	private static final float SCAN_SPEED=30;
	private static final float TRAVEL_SPEED=80;
	private static final float ADJUSTMENT_ANGLE=20;
	
	public Detector(Odometer odo, Navigation nav, SampleProvider usSensor, float[] usData, ColorReader colorReader) {
		this.odo = odo;
		this.nav = nav;
		this.usSensor = usSensor;
		this.usData = usData;
		this.colorReader = colorReader;
	}
	
	@Override
	public void run() {
		
		nav.turnTo(360-ADJUSTMENT_ANGLE,true);
		
		double[] blockAngles = new double[2];
		boolean isObject = false;
		
		Sound.beep();
		nav.setSpeeds(-SCAN_SPEED, SCAN_SPEED);
		Sound.beep();
		
		int numObjects=0;
		
		while (odo.getAng()<80 || odo.getAng()>180) {
			if (getFilteredData()<MAX_DISTANCE && !isObject) {
				if (numObjects==2) {
					System.out.println("TOO MANY OBJECTS!");
					break;
				}
				blockAngles[numObjects] = odo.getAng();
				numObjects++;
				isObject = true;
				Sound.beepSequenceUp();
			} else if (getFilteredData()==MAX_DISTANCE && isObject) {
				isObject = false;
				Sound.beepSequence();
			}
		}
		
		nav.setSpeeds(0, 0);
		
		for (int i=0;i<numObjects;i++) {
			Sound.buzz();
			nav.turnTo(wrapAngle(blockAngles[i]+ADJUSTMENT_ANGLE),true);
			nav.setSpeeds(TRAVEL_SPEED,TRAVEL_SPEED);
			
			while (!colorReader.isObject()) {}
			
			nav.setSpeeds(0, 0);
			
			if (colorReader.isBlock()) {
				moveBlockToEndPoint();
				break;
			} else {
				Sound.beep();
				nav.setSpeeds(-TRAVEL_SPEED,-TRAVEL_SPEED);
				while (Math.abs(odo.getX())>0.04 && Math.abs(odo.getY())>0.04){}
				nav.setSpeeds(0,0);
			}
		}
	}

	public float getFilteredData() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0]*100;
		
		if (distance > MAX_DISTANCE) distance = MAX_DISTANCE;
		
		return distance;
	}
	
	private void moveBlockToEndPoint() {
			Sound.beepSequenceUp();
			
			nav.turnTo(wrapAngle(odo.getAng()+180), true);
			
			clawMotor.setSpeed(200);
			clawMotor.rotateTo(105);
			
			nav.travelTo(67,67);
			nav.turnTo(200,true);
			
			Sound.beep();
			Sound.beep();
			Sound.beep();
	}
	
	private double wrapAngle(double angle) {
		if (angle > 360) {
			return angle-360;
		} else if (angle < 0) {
			return angle+360;
		} else {
			return angle;
		}
	}
}
