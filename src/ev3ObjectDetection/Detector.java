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
	private static final float MOTOR_SPEED=50;
	
	public Detector(Odometer odo, Navigation nav, SampleProvider usSensor, float[] usData, ColorReader colorReader) {
		this.odo = odo;
		this.nav = nav;
		this.usSensor = usSensor;
		this.usData = usData;
		this.colorReader = colorReader;
	}
	
	@Override
	public void run() {
		
		double[] blockAngles = new double[2];
		double angle = 0;
		boolean isObject = false;
		
		Sound.beep();
		nav.setSpeeds(-MOTOR_SPEED, MOTOR_SPEED);
		Sound.beep();
		
		int numObjects=0;
		
		while (odo.getAng()<90) {
			if (numObjects>=1) {
				break;
			}
			if (getFilteredData()<MAX_DISTANCE && !isObject) {
				numObjects++;
				blockAngles[numObjects] = odo.getAng();
				isObject = true;
				Sound.beepSequenceUp();
			} else if (getFilteredData()==MAX_DISTANCE && isObject) {
				isObject = false;
				Sound.beepSequence();
			}
		}
		
		for (int i=0;i<numObjects;i++) {
			nav.turnTo(blockAngles[i]+34,true);
			nav.setSpeeds(MOTOR_SPEED+30,MOTOR_SPEED+30);
			
			while (!colorReader.isObject()) {}
			
			nav.setSpeeds(0, 0);
			
			if (colorReader.isBlock()) {
				moveBlockToEndPoint();
				break;
			} else {
				nav.setSpeeds(-(MOTOR_SPEED+30),-(MOTOR_SPEED+30));
				while (odo.getX()>0 && odo.getY()>0){}
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
			
			nav.turnTo(odo.getAng()+180, true);
			
			clawMotor.setSpeed(200);
			clawMotor.rotateTo(105);
			
			nav.travelTo(65,65);
	}
}
