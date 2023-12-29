package robo;

import robocode.*;
import java.awt.Color;
import robocode.util.Utils;
import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.geom.Point2D;

public class R extends AdvancedRobot {
	
	boolean peek;					//Não virar se tiver um robô lá
	double enemyEnergy = 100;		//energia do inimigo
	String desvBullet = "linear"; 	//há 3 estrategias para desviar um projetil: 
										//linear: quando o inimigo dispara, torna num angulo de 90 graus ao inimigo e anda para frente
										//static: quando o inimigo dispara, torna-se na direcao do inimigo e anda para frente
										//back: quando o inimigo dispara, torna-se numa angulo de 90 graus ao inimigo mas anda para a direcao contrario de qual andou
	static String shootingType = "normal";
	double shotsHitted = 0;			//conta para os projeteis acertados
	double shotsMissed = 0;			//conta para os projeteis falhados
	double bulletsHited = 0;		//conta para os projetils sofridos durante ao usar uma estrategia para desviar
	double enemyBulletsFired = 0;	//conta para os projetils disparados do inimigo durante ao usar uma estrategia para desviar
	
	
    public void run() {
    	setColors(Color.blue, Color.white, Color.blue, Color.blue, Color.blue);
    	do {
    		if(getRadarTurnRemaining() == 0.0) {
    			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    		}
    		isNearWall();		//funcao para que se estamos perto da parede, andar mais para o centro
    		execute();
    	}while (true);
    }
    public void isNearWall() {
    	double h = getBattleFieldHeight();
		double w = getBattleFieldWidth();
		double x = getX();
		double y = getY();
		if(y<(h/7)) {
			turnRight(360-getHeading());
			ahead(150);
		} else if (y>(h-h/7)) {
			turnRight(180-getHeading());
			ahead(150);
		} else if (x<(w/7)) {
			turnRight(90-getHeading());
			ahead(150);
		} else if (x>(w-w/7)) {
			turnRight(270-getHeading());
			ahead(150);
		}
    }
    public void onScannedRobot(ScannedRobotEvent e) {
    	double bearing = e.getBearing();
    	double angle = Math.toRadians(getHeading() + bearing % 360);
    	double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());
        double enemyHeading = e.getHeading();
        double enemyVelocity = e.getVelocity();
        
        //radar fica no enemy
        double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
        double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());
        double extraTurn = Math.min(Math.atan(36.0 / e.getDistance()),Rules.RADAR_TURN_RATE_RADIANS);
        radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);
        setTurnRadarRightRadians(radarTurn);
        
        if (e.getEnergy()<enemyEnergy) {				//desviar um projetil disparado do inimigo
        	enemyBulletsFired = enemyBulletsFired+1;
        	if(desvBullet=="linear") {
        		desviarLinear(e);
        	} else if(desvBullet=="back") {
        		desviarBack(e);
        	} else if(desvBullet=="static") {
        		desviarStatic(e);
        	}
        }
        enemyEnergy=e.getEnergy();
        
        
        if (getEnergy() > 20) {
        	shotsMissed = shotsMissed + 1;
        	if(shootingType == "normal") {
        		normalShooting(e);
        	} else if(shootingType == "linear") {
        		linearShooting(e);
        	} else if(shootingType == "circular") {
        		circularShooting(e);
        	}
        }
    }
    
    public void circularShooting(ScannedRobotEvent e) {
    	double bulletPower = Math.min(3.0,getEnergy());
    	double myX = getX();
    	double myY = getY();
    	double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
    	double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
    	double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
    	double enemyHeading = e.getHeadingRadians();
    	double enemyVelocity = e.getVelocity();
    	double oldEnemyHeading = enemyHeading;
    	double enemyHeadingChange = enemyHeading - oldEnemyHeading;

    	double deltaTime = 0;
    	double battleFieldHeight = getBattleFieldHeight(), 
    	       battleFieldWidth = getBattleFieldWidth();
    	double predictedX = enemyX, predictedY = enemyY;
    	while((++deltaTime) * (20.0 - 3.0 * bulletPower) < 
    	      Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
    		predictedX += Math.sin(enemyHeading) * enemyVelocity;
    		predictedY += Math.cos(enemyHeading) * enemyVelocity;
    		enemyHeading += enemyHeadingChange;
    		if(	predictedX < 18.0 
    			|| predictedY < 18.0
    			|| predictedX > battleFieldWidth - 18.0
    			|| predictedY > battleFieldHeight - 18.0){

    			predictedX = Math.min(Math.max(18.0, predictedX), 
    			    battleFieldWidth - 18.0);	
    			predictedY = Math.min(Math.max(18.0, predictedY), 
    			    battleFieldHeight - 18.0);
    			break;
    		}
    	}
    	double theta = Utils.normalAbsoluteAngle(Math.atan2(
    	    predictedX - getX(), predictedY - getY()));

    	setTurnRadarRightRadians(Utils.normalRelativeAngle(
    	    absoluteBearing - getRadarHeadingRadians()));
    	setTurnGunRightRadians(Utils.normalRelativeAngle(
    	    theta - getGunHeadingRadians()));
    	fire(3);
    }
    
    public void linearShooting(ScannedRobotEvent e) {
    	double bulletPower = Math.min(3.0,getEnergy());
		double myX = getX();
		double myY = getY();
		double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
		double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
		double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
		double enemyHeading = e.getHeadingRadians();
		double enemyVelocity = e.getVelocity();
		
		
		double deltaTime = 0;
		double battleFieldHeight = getBattleFieldHeight(), battleFieldWidth = getBattleFieldWidth();
		double predictedX = enemyX, predictedY = enemyY;
		while((++deltaTime) * (20.0 - 3.0 * bulletPower) < Point2D.Double.distance(myX, myY, predictedX, predictedY)){		
			predictedX += Math.sin(enemyHeading) * enemyVelocity;	
			predictedY += Math.cos(enemyHeading) * enemyVelocity;
			if(	predictedX < 18.0 
				|| predictedY < 18.0
				|| predictedX > battleFieldWidth - 18.0
				|| predictedY > battleFieldHeight - 18.0){
				predictedX = Math.min(Math.max(18.0, predictedX), battleFieldWidth - 18.0);	
				predictedY = Math.min(Math.max(18.0, predictedY), battleFieldHeight - 18.0);
				break;
			}
		}
		double theta = Utils.normalAbsoluteAngle(Math.atan2(predictedX - getX(), predictedY - getY()));
		
		setTurnRadarRightRadians(Utils.normalRelativeAngle(absoluteBearing - getRadarHeadingRadians()));
		setTurnGunRightRadians(Utils.normalRelativeAngle(theta - getGunHeadingRadians()));
		fire(bulletPower);
    }
    
    public void normalShooting(ScannedRobotEvent e) {
    	double bearing = e.getBearing();
    	double angle = Math.toRadians(getHeading() + bearing % 360);
    	double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());
        double enemyHeading = e.getHeading();
        double enemyVelocity = e.getVelocity();
    	double predictedX = enemyX, predictedY = enemyY;
    	
    	predictedX += Math.sin(enemyHeading) * enemyVelocity;
    	predictedY += Math.cos(enemyHeading) * enemyVelocity;
    	
    	double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
    	
    	double theta = Utils.normalAbsoluteAngle(Math.atan2(
    	    predictedX - getX(), predictedY - getY()));

    	setTurnRadarRightRadians(Utils.normalRelativeAngle(
    	    absoluteBearing - getRadarHeadingRadians()));
    	setTurnGunRightRadians(Utils.normalRelativeAngle(
    	    theta - getGunHeadingRadians()));
    	
        setFire(Math.min(400 / e.getDistance(), 3)); // Variar a força dos tiros consoante a distância
    	
        if (peek) {
        	scan();
        }
    }
    
    public void desviarStatic(ScannedRobotEvent e) {
    	double bearing = e.getBearing();
    	System.out.println(bearing);
    	if(bearing<90 && bearing>0) {
        	setTurnRight(bearing);
        }else if (bearing>90 && bearing<180) {
        	setTurnRight(bearing);
        } else if (bearing<0 && bearing>-90) {
        	setTurnLeft(-bearing);
        } else if (bearing>-180 && bearing<-90) {
        	setTurnLeft(-bearing);
        }
    	if (headingWall()==true) {
    		back(100);
    	}else {
    		ahead(100);
    	}
    }
    public void desviarBack(ScannedRobotEvent e) {
    	double bearing = e.getBearing();
    	if(bearing<70 && bearing>0) {
        	setTurnLeft(90-bearing+180);
        }else if (bearing>110 && bearing<180) {
        	setTurnRight(bearing-90+180);
        } else if (bearing<0 && bearing>-70) {
        	setTurnRight(90+bearing+180);
        } else if (bearing>-180 && bearing<-110) {
        	setTurnLeft(-bearing+90+180);
        }
    	if (headingWall()==true) {
    		back(100);
    	}else {
    		ahead(100);
    	}
    }
    public void desviarLinear(ScannedRobotEvent e) {
    	double bearing = e.getBearing();
    	if(bearing<70 && bearing>0) {
        	setTurnLeft(90-bearing);
        }else if (bearing>110 && bearing<180) {
        	setTurnRight(bearing-90);
        } else if (bearing<0 && bearing>-70) {
        	setTurnRight(90+bearing);
        } else if (bearing>-180 && bearing<-110) {
        	setTurnLeft(-bearing+90);
        }
    	if (headingWall()==true) {
    		back(100);
    	}else {
    		ahead(100);
    	}
    }
    public boolean headingWall() {
    	double h = getBattleFieldHeight();
		double w = getBattleFieldWidth();
		double x = getX();
		double y = getY();
    	if(y<(h/5) && getHeading()>90 && getHeading()<270) {
    		return true;
		} else if (y>(h-h/5) && getHeading()<90 && getHeading()>270) {
			return true;
		} else if (x<(w/5) && getHeading()>180) {
			return true;
		} else if (x>(w-w/5) && getHeading()<180) {
			return true;
		}
    	return false;
    }
    
    public void onBulletHit(BulletHitEvent e) {
    	shotsHitted = shotsHitted + 1;
    	enemyEnergy=e.getEnergy();
    }
    
    public void onBulletHitBullet() {
    	return;
    }
    
    public void onBulletMissed(BulletMissedEvent e) {
    	shotsMissed = shotsMissed + 1;
    	if (shotsMissed>7 && (shotsHitted/shotsMissed)>0.2) {
    		if (shootingType == "normal") {
    			shootingType = "linear";
    		} else if (shootingType == "linear") {
    			shootingType = "circular";
    		} else if (shootingType == "circular") {
    			shootingType = "normal";
    		}
    		shotsHitted = 0;
    		shotsMissed = 0;
    	}
    }

	public void onHitByBullet(HitByBulletEvent e) {
    	bulletsHited=bulletsHited+1;
    	if (bulletsHited>7 && (bulletsHited/enemyBulletsFired)>0.2) {		//mudar a estrategia quando sofremos muitos projetils
    		if (desvBullet=="linear") {
    			desvBullet="static";
    		} else if (desvBullet=="static") {
    			desvBullet="back";
    		} else if (desvBullet=="back") {
    			desvBullet="linear";
    		}
			bulletsHited=0;
			enemyBulletsFired=0;
    	}
    }
    
    public void onHitRobot(HitRobotEvent e) {
		// If he's in front of us, set back up a bit.
		if (e.getBearing() > -90 && e.getBearing() < 90) {
			back(100);
		} // else he's in back of us, so set ahead a bit.
		else {
			ahead(100);
		}
		
		scan();
	}
    
    public void onHitWall(HitWallEvent e) {
		turnRight(180);
		ahead(100);
	}
    
}
	/*public void run() {
		String direction;
		double enemyEnergy = 100;

    	do {
    		if(getRadarTurnRemaining() == 0.0) {
    			setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
    		}
    		execute();
    	}while (true);

        }
	
	public void onHitWall(HitWallEvent e) {
		turnRight(180);
		ahead(100);
	}
	
    public void onScannedRobot(ScannedRobotEvent e) {
    	
    	double ngleToEnemy = e.getBearing();
    	double angle = Math.toRadians(getHeading() + ngleToEnemy % 360);
    	double enemyX = (getX() + Math.sin(angle) * e.getDistance());
        double enemyY = (getY() + Math.cos(angle) * e.getDistance());
        double distance = e.getDistance();
        double enemyHeading = e.getHeading();
        double enemyVelocity = e.getVelocity();
        
        double angleToEnemy = getHeadingRadians() + e.getBearingRadians();
        double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians());
        double extraTurn = Math.min(Math.atan(36.0 / e.getDistance()),Rules.RADAR_TURN_RATE_RADIANS);
        radarTurn += (radarTurn < 0 ? -extraTurn : extraTurn);
        setTurnRadarRightRadians(radarTurn);
        
    	double predictedX = enemyX, predictedY = enemyY;
    	predictedX += Math.sin(enemyHeading) * enemyVelocity;
    	predictedY += Math.cos(enemyHeading) * enemyVelocity;
    	
    	double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
    	
    	double theta = Utils.normalAbsoluteAngle(Math.atan2(
    	    predictedX - getX(), predictedY - getY()));

    	setTurnRadarRightRadians(Utils.normalRelativeAngle(
    	    absoluteBearing - getRadarHeadingRadians()));
    	setTurnGunRightRadians(Utils.normalRelativeAngle(
    	    theta - getGunHeadingRadians()));
    	
    	setFire(Math.min(400 / e.getDistance(), 3));
    		
    	scan();
    }
    
    public void onHitRobot(HitRobotEvent e) {
		// If he's in front of us, set back up a bit.
		if (e.getBearing() > -90 && e.getBearing() < 90) {
			back(100);
		} // else he's in back of us, so set ahead a bit.
		else {
			ahead(100);
		}
		
		scan();
	}

}*/


