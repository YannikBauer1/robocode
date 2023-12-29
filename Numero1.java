package Ro;

import java.awt.Color;

import robocode.*;
import robocode.util.Utils;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.geom.Point2D;


public class Numero1 extends AdvancedRobot {
	
	double enemyEnergy = 100;						// Energia do inimigo
	
	static String shootingStrategy = "static";		// H� 2 estrategias para disparar: static e linear
	double shotsHitted = 0;							// Conta os proj�teis acertados
	double shotsMissed = 0;							// Conta os proj�teis falhados
	
	int movementDirection = 1;						// Vari�vel utilizada no dodge
	
    public void run() {
    	setColors(Color.blue, Color.white, Color.blue, Color.blue, Color.blue);
    	do {
	        if ( getRadarTurnRemaining() == 0.0 )						// In�cio da batalha a rota��o do radar � 0
	            setTurnRadarRightRadians( Double.POSITIVE_INFINITY ); 	// Rodar o radar infinitamente para a direita
	        	setAdjustRadarForGunTurn(true);  						// Radar n�o roda com a rota��o da arma
	        	setAdjustGunForRobotTurn(true);							// Arma n�o roda com a rotac�o do rob�
	        	setAdjustRadarForRobotTurn(true);						// Radar n�o roda com a rota��o do rob�
	        execute();
	    } while ( true );
    }
    public void onScannedRobot(ScannedRobotEvent e) {
    	double angleToEnemy = getHeadingRadians() + e.getBearingRadians(); 								// Define vari�vel do �ngulo face o inimigo
    	double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians()); 			// Define vari�vel com o necess�rio para o radar rodar
    	double extraTurn = Math.min(Math.atan(36.0/e.getDistance()),Rules.RADAR_TURN_RATE_RADIANS);  	// Extra rota��o do radar para compensar o movimento do inimigo
    	if (radarTurn < 0) { 						// Ajuste do radar
            radarTurn -= extraTurn;
    	} else {
            radarTurn += extraTurn;
    	}
    	setTurnRadarRightRadians(radarTurn);  		// Roda o radar o calculado anteriormente
    	
    	dodge_shoot obj = new dodge_shoot(e);		// Cria um objeto da classe shoot
    	obj.Dodge();
    	
    	if (getEnergy()>20){						// Se a nossa energia for superior a 20, disparar
    		obj.fire();
    	}
    	
    	enemyEnergy = e.getEnergy();
    }
    
    public void onBulletHit(BulletHitEvent e) {
    	++shotsHitted;								// Contar tiros que acertamos
    	enemyEnergy=e.getEnergy();					// Obter energia do inimigo
    }

    public void onBulletMissed(BulletMissedEvent e) {
    	++shotsMissed;													// Conta os tiros falhados -> que acertam a parede
    	if (shotsMissed >= 7 && (shotsHitted/shotsMissed) < 0.2) {		// Se os tiros falhados forem maior que 8 e a raz�o entre os acertos e falhados menor que 1, n�o disparar
    		if (shootingStrategy=="static") {
    			shootingStrategy="linear";
        		System.out.println("change shooting Strategy from static to linear");
    		}else if (shootingStrategy=="linear") {
    			shootingStrategy="static";
        		System.out.println("change shooting Strategy from linear to static");
        		
    		}
    	   	shotsHitted = 0;						// Definir novamente a vari�vel como 0
        	shotsMissed = 0;						// Definir novamente a vari�vel como 0
    	}
    }
    
    public void onHitRobot(HitRobotEvent e) {	// Com o rob� frente a n�s disparar balas de pot�ncia m�xima
    	fire(3);
		scan();
	}
    public void onHitWall(HitWallEvent e) {		// Se estivermos frente � parede e colidirmos, rodamos e avan�amos, mas de costas para a parede apenas avan�amos 
    	double h = getBattleFieldHeight();
		double w = getBattleFieldWidth();
		double x = getX();
		double y = getY();
		double bearing=e.getBearing();
		
		// Aplica-se quando batemos numa parede que n�o na esquina, dirigir para o meio do campo
    	if ((x>100 && x<w-100 && y<100) || (x>w-100 && y>100 && y<h-100) || (x>100 && x<w-100 && y>h-100) || (x<100 && y>100 && y<h-100)) {
    		if(bearing>0) {
    			setTurnLeft(180-bearing);
    		} else {
    			setTurnRight(180+bearing);
    		}
    	} else if (x<100 || x>w-100) {		// Aplica-se quando estamos na esquina, dirigir para o meio do campo
    		if(bearing>0) {
    			setTurnLeft(90-bearing+45);
    		} else {
    			setTurnRight(90+bearing+45);
    		}
    	}
		setAhead(150);
	}

    class dodge_shoot {						// Classe organizada do dodge e shoot
    	ScannedRobotEvent event;
    	dodge_shoot(ScannedRobotEvent e){
    		this.event=e;
    	}
    	
    	public void Dodge() {    		
    		setTurnRight(event.getBearing()+90-30*movementDirection);		// Ficar perpendicular ao rob� inimigo		
        	double changeInEnergy = enemyEnergy-event.getEnergy();			// Calcular vari�ncia na energia do inimigo				
        	if (changeInEnergy>0 && changeInEnergy<=3) {					// Se a vari�ncia calculada for superior a 0 e inferior a 3, desviar
        		movementDirection = -movementDirection;						// Definir vairi�vel negativa para seguir caminho oposto
        		setAhead((event.getDistance()/4+25) * movementDirection);	// F�rmula para desviar, tendo em conta a vari�vel "movementDirection", movemos para a dire��o oposta de onde nos dirigiamos
        	}
    	}

    	public void fire() {
    		if (shootingStrategy=="static") {								// Se "static" usar fun��o "staticShooting"
    			staticShooting();
    		} else if (shootingStrategy=="linear") {						// Se "linear" usar fun��o "linearShooting"
    			linearShooting();
    		}
    	}
    	
    	public void staticShooting() {
    		double absoluteBearing = getHeadingRadians() + event.getBearingRadians();						// Obter �ngulo do inimigo em fun��o da nossa posi��o
            setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));	// Virar a arma na dire��o do inimigo
    		if(getGunHeat() == 0) {
    			setFire(Math.min(400 / event.getDistance(), 3)); 											// Disparar a bala se a arma n�o estiver sobreaquecida
           	}
    	}
    	
    	public void linearShooting() {
    		setColors(Color.green, Color.white, Color.green, Color.green, Color.green);		// Cor verde quando usamos a estrat�gia linearShooting
    		
    		double rX = getX();														// X atual do meu rob�
            double rY = getY();														// Y atual do meu rob�
           	double absBearing = event.getBearingRadians() + getHeadingRadians();	// Obter �ngulo do inimigo em fun��o da nossa posi��o
           	double predictX = rX + event.getDistance() * Math.sin(absBearing);		// F�rmula para obter X do inimigo perante as nossas coordenadas
           	double predictY = rY + event.getDistance() * Math.cos(absBearing);		// F�rmula para obter Y do inimigo perante as nossas coordenadas
           	double enemyHeading = event.getHeadingRadians(); 						// Dire��o que o inimigo se est� a mover
           	double enemyVelocity = event.getVelocity();								// Velocidade do inimigo

        	double bulletPower = 3.0;
           	double deltaTime = 0; 													// Unidades de tempo que passaram

            	
           	// Incrementa 1 valor ao deltaTime -- F�rmula define a for�a da bala que iremos dispar --- Classe do java utilizada para obtermos a nossa fun��o de dist�ncia consoante a nossa posi��p
           	while ((++deltaTime) * (20 - 3.0 * bulletPower) < Point2D.Double.distance(rX, rY, predictX, predictY)) {
           		predictX += Math.sin(enemyHeading) * enemyVelocity;		//Calcula o X que se prev� que o inimigo estar�, multiplicando o sin da dire��o que estava a tomar pela sua velocidade
           		predictY += Math.cos(enemyHeading) * enemyVelocity;		//Calcula o Y que se prev� que o inimigo estar�, multiplicando o cos da dire��o que estava a tomar pela sua velocidade
           		
           		if (predictX > getBattleFieldWidth() || predictX < 0 || predictY > getBattleFieldHeight() || predictY < 0) {
           			break;		// Se X ou Y que prevermos for fora do campo de batalha, terminamos o loop
           		}
            }
            	
            double angle = Utils.normalAbsoluteAngle(Math.atan2(predictX - rX, predictY - rY)); 		// Classe "Utils" permite descobrirmos o �ngulo de disparo
           	setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians())); 	// Virar a arma para a dire��o que queremos disparar
           	setTurnGunRightRadians(Utils.normalRelativeAngle(angle - getGunHeadingRadians()));
    		if(getGunHeat() == 0) {
           		setFire(bulletPower); 		// Disparar a bala se a arma n�o estiver sobreaquecida
           	}
    	}
    }
}

