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
	
	private static final float MAX_DISTANCE = 60;
	private static final float MAX_DISTANCE_2 = 25;
	private static final float ANALYSE_DISTANCE = 20;
	private static final float MOTOR_SPEED = 50;
	private static final float SCAN_SPEED = 150;
	private static final int SCAN_ANGLE = 35;
	private static final long SLEEP_TIME = 800;
	
	
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
		boolean isBlock = false;
		
		Sound.beep();
		nav.setSpeeds(-MOTOR_SPEED, MOTOR_SPEED);
		Sound.beep();
		
		int numObjects=0;
		
		while (odo.getAng()<90) {
/*			if (numObjects>=1) {
				break;
			}*/
			if (getFilteredData()<MAX_DISTANCE && !isObject) {
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
/*			nav.turnTo(blockAngles[i]+34,true);
			nav.setSpeeds(MOTOR_SPEED+30,MOTOR_SPEED+30);
		
			while (!colorReader.isObject()) {}
		
			nav.setSpeeds(0, 0);*/
			
			isBlock = seekingBlock(blockAngles[i]);
				
				
				// sleep
			try {
				Thread.sleep(SLEEP_TIME);
			} catch (InterruptedException e) {}
			
		
			if (isBlock) {
				Sound.beep();
				Sound.beep();
				detourSecondBlock(blockAngles[i]);
				break;
			} else {
				Sound.beepSequenceUp();
/*				nav.setSpeeds(-(MOTOR_SPEED+30),-(MOTOR_SPEED+30));
				while (odo.getX()>0 && odo.getY()>0){}
				nav.setSpeeds(0,0);*/
				nav.travelTo(0, 0);
			}
		}
		
		// What if we didn't find the blue block after all this?
		if (!isBlock){
			nav.travelTo(-5, 70);
			nav.travelTo(75, 75);
			nav.turnTo(180, true);
			// Same as the other side
			while (odo.getAng()<90) {
				if (getFilteredData()<MAX_DISTANCE && !isObject) {
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
					isBlock = seekingBlock(blockAngles[i]);
					try {
						Thread.sleep(SLEEP_TIME);
					} catch (InterruptedException e) {}
					
				
					if (isBlock) {
						Sound.beep();
						Sound.beep();
						moveBlockToEndPoint();
						break;
					} else {
						Sound.beepSequenceUp();
						nav.setSpeeds(-MOTOR_SPEED, -MOTOR_SPEED);
						try {
							Thread.sleep(SLEEP_TIME);
						} catch (InterruptedException e) {}
						nav.setSpeeds(0, 0);
						nav.travelTo(75, 75);
					}
				}
			}
		
		
	}

	public float getFilteredData() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0]*100;
		
		if (distance > MAX_DISTANCE) distance = MAX_DISTANCE;
		
		return distance;
	}
	
	public float getFilteredData2() {
		usSensor.fetchSample(usData, 0);
		float distance = usData[0]*100;
		
		if (distance > MAX_DISTANCE_2) distance = MAX_DISTANCE_2;
		
		return distance;
	}
	
	private void moveBlockToEndPoint() {
		Sound.beepSequenceUp();
			
		nav.turnTo(odo.getAng()+180, true);
			
		clawMotor.setSpeed(150);
		clawMotor.rotateTo(115);
			
		nav.travelTo(65,65);
		nav.turnTo(225, true);
	}
	
	// Move the block to endpoint if we assume there's a second block
	private void detourSecondBlock(double angle){
		nav.turnTo(odo.getAng()+180, true);
		
		clawMotor.setSpeed(150);
		clawMotor.rotateTo(115);
		
		nav.travelTo(0, 0);
		if (angle<=39){
			nav.travelTo(-5, 70);
			nav.travelTo(65, 65);
			nav.setSpeeds(MOTOR_SPEED, -MOTOR_SPEED);
			while (odo.getAng()>=223 && odo.getAng()<=227){}
			nav.setSpeeds(0, 0);
		} else {
			nav.travelTo(70, -5);
			nav.travelTo(65, 65);
			nav.setSpeeds(-MOTOR_SPEED, MOTOR_SPEED);
			while (odo.getAng()>=223 && odo.getAng()<=227){}
			nav.setSpeeds(0, 0);
		}
		
	}
	
	// A helper method that will seek the bricks at an estimated angle
	// Return whether the brick the the one we are looking for
	private boolean seekingBlock(double angle){
		usMotor.resetTachoCount();
		usMotor.setSpeed(SCAN_SPEED);
		nav.turnTo(angle, true);
		nav.setSpeeds(MOTOR_SPEED, MOTOR_SPEED);
		boolean seeking = true;
		float lastDistance = 0;
		double orientation = 0;
		int programCount = 0;
		while (true){
			float dist = getFilteredData2();
			if (!usMotor.isMoving() && seeking){
				if(usMotor.getTachoCount() <= 0){
					usMotor.rotateTo(2*SCAN_ANGLE, true);
				} else {
					usMotor.rotateTo(-SCAN_ANGLE,true);
				}
			}
			if (seeking && dist <= ANALYSE_DISTANCE){
				usMotor.stop(true);
				seeking = false;
				orientation = usMotor.getTachoCount();
				lastDistance = getFilteredData();
				Sound.beepSequence();
			}
			if ((!seeking && dist >= lastDistance) || (programCount > 30)){
				nav.setSpeeds(0, 0);
				nav.turnTo(wrapAngle(odo.getAng() + orientation/2),false);
				usMotor.rotateTo(0, false);
				break;
			}
			if (!seeking){
				programCount++;
				lastDistance = dist;
			}
		}
		// sleep
		try {
			Thread.sleep(SLEEP_TIME);
		} catch (InterruptedException e) {}
		
		// Now we have the precise orientation of the block
		nav.setSpeeds(MOTOR_SPEED, MOTOR_SPEED);
		while (!colorReader.isObject()) {}
		return colorReader.isBlock();
	}

	
	
	
	// Wrap the angle for negative value
	public double wrapAngle(double angle){
		if (angle < 0){
			return angle + 360;
		} else if (angle >= 360){
			return angle - 360;
		} else {
			return angle;
		}
	}
	
	
}
