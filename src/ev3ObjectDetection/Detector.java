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
	private static final float ADJUSTMENT_ANGLE=20; //the angle between when the US sensor first scans an object and its centre.
	private static final int MAX_NUM_OF_BLOCKS=2;
	private static final double ODOMETER_ERROR = 0.04;
	
	
	public Detector(Odometer odo, Navigation nav, SampleProvider usSensor, float[] usData, ColorReader colorReader) {
		this.odo = odo;
		this.nav = nav;
		this.usSensor = usSensor;
		this.usData = usData;
		this.colorReader = colorReader;
	}
	
	@Override
	public void run() {
		//turn to starting scan angle
		nav.turnTo(wrapAngle(-ADJUSTMENT_ANGLE),true);
		
		double[] blockAngles = new double[MAX_NUM_OF_BLOCKS];
		boolean isObject = false;
		boolean isBlock = false;
		
		Sound.beep();
		//start turning
		nav.setSpeeds(-SCAN_SPEED, SCAN_SPEED);
		Sound.beep();
		
		int numObjects=0;
		
		//scan about 90 degrees from the start angle and then stop the motor.
		//The end angle should be the first angle at which we can detect an object located @ 90degrees.
		//Not catching angles over 180 since we start at 360 - adjustment angle.
		while (odo.getAng()<90-ADJUSTMENT_ANGLE || odo.getAng()>180) {
			if (getFilteredData()<MAX_DISTANCE && !isObject) {
				//if we start seeing an object while there are already two objects in memory, we break the loop
				if (numObjects==MAX_NUM_OF_BLOCKS) {
					System.out.println("TOO MANY OBJECTS!");
					break;
				}
				//latch angle in array and increment counter
				blockAngles[numObjects] = odo.getAng();
				numObjects++;
				//flag that we're starting to scan an object
				isObject = true;
				Sound.beepSequenceUp();
			} else if (getFilteredData()==MAX_DISTANCE && isObject) { //when we read a max dist value, flag that it's the end of the object
				isObject = false;
				Sound.beepSequence();
			}
		}
		nav.setSpeeds(0, 0);
		
		//check each object by turning to its latched angle and traveling forward until the color sensor detects an object.
		for (int i=0;i<numObjects;i++) {
			Sound.buzz();
			nav.turnTo(wrapAngle(blockAngles[i]+ADJUSTMENT_ANGLE),true);
			nav.setSpeeds(TRAVEL_SPEED,TRAVEL_SPEED);
			
			while (!colorReader.isObject()) {}
			
			nav.setSpeeds(0, 0);
			
			isBlock = colorReader.isBlock();
			if (isBlock) {
				if(numObjects>1){
					detourSecondBlock(blockAngles[i]);
				} else {
					moveBlockToEndPoint();
				}
				break;
			} else { //if the object is not a block, return to origin and go to next iteration.
				Sound.beep();
				nav.setSpeeds(-TRAVEL_SPEED,-TRAVEL_SPEED);
				while (Math.abs(odo.getX())>0+ODOMETER_ERROR && Math.abs(odo.getY())>0+ODOMETER_ERROR){}
				nav.setSpeeds(0,0);
			}
		}
		// What if we didn't find the blue block after all this?
				if (!isBlock){
					numObjects = 0;
					isObject = false;
					if (blockAngles[0] >= 38){
						nav.turnTo(0,true);
						nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
						while (odo.getX() < 70) {};
						nav.turnTo(0, true);
						nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
						while (odo.getY() < 70) {};
						nav.turnTo(180-ADJUSTMENT_ANGLE/2, true);
					} else {
						nav.turnTo(90,true);
						nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
						while (odo.getY() < 70) {};
						nav.turnTo(0, true);
						nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
						while (odo.getX() < 70) {};
						nav.turnTo(180-ADJUSTMENT_ANGLE/2, true);
					}
					// Same as the other side
					nav.setSpeeds(-TRAVEL_SPEED, TRAVEL_SPEED);
					while (odo.getAng()<270-ADJUSTMENT_ANGLE/2) {
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
						Sound.buzz();
						nav.turnTo(wrapAngle(blockAngles[i]+ADJUSTMENT_ANGLE),true);
						nav.setSpeeds(TRAVEL_SPEED,TRAVEL_SPEED);
						
						while (!colorReader.isObject()) {}
						
						nav.setSpeeds(0, 0);
						
						isBlock = colorReader.isBlock();
						
							if (isBlock) {
								Sound.beep();
								Sound.beep();
								moveBlockToEndPoint();
								break;
							} else {
								Sound.beep();
								nav.setSpeeds(-TRAVEL_SPEED,-TRAVEL_SPEED);
								while (Math.abs(odo.getX())>69.96 && Math.abs(odo.getY())>69.96){}
								nav.setSpeeds(0,0);
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
	
	private void moveBlockToEndPoint() {
			Sound.beepSequenceUp();
			
			nav.turnTo(wrapAngle(odo.getAng()+180), true);
			
			clawMotor.setSpeed(200);
			clawMotor.rotateTo(115);
			
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

	
	// Move the block to endpoint if we assume there's a second block
	private void detourSecondBlock(double angle){
		nav.turnTo(odo.getAng()+180, true);
		
		clawMotor.setSpeed(150);
		clawMotor.rotateTo(115);
		
		nav.travelTo(5, 5);
		if (angle<=35){
			nav.turnTo(90, true);
			nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
			while (odo.getY() < 75){}
			nav.setSpeeds(0, 0);
			nav.setSpeeds(-TRAVEL_SPEED, TRAVEL_SPEED);
			while (odo.getAng() < 180){}
			nav.setSpeeds(0, 0);
			nav.setSpeeds(-TRAVEL_SPEED, -TRAVEL_SPEED);
			while (odo.getX() < 75){}
			nav.setSpeeds(0, 0);
		} else {
			nav.turnTo(0,true);
			nav.setSpeeds(TRAVEL_SPEED, TRAVEL_SPEED);
			while (odo.getX() < 75){}
			nav.setSpeeds(0, 0);
			nav.setSpeeds(TRAVEL_SPEED, -TRAVEL_SPEED);
			while (odo.getAng() > 270 || odo.getAng() < 90 ){}
			nav.setSpeeds(0, 0);
			nav.setSpeeds(-TRAVEL_SPEED, -TRAVEL_SPEED);
			while (odo.getY() < 75){}
			nav.setSpeeds(0, 0);
		}
		
	}
	

}
