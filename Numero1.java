package Ro;

import java.awt.Color;

import robocode.*;
import robocode.util.Utils;

import static robocode.util.Utils.normalRelativeAngleDegrees;
import java.awt.geom.Point2D;


public class Numero1 extends AdvancedRobot {
	
	double enemyEnergy = 100;						// Energia do inimigo
	
	static String shootingStrategy = "static";		// Há 2 estrategias para disparar: static e linear
	double shotsHitted = 0;							// Conta os projéteis acertados
	double shotsMissed = 0;							// Conta os projéteis falhados
	
	int movementDirection = 1;						// Variável utilizada no dodge
	
    public void run() {
    	setColors(Color.blue, Color.white, Color.blue, Color.blue, Color.blue);
    	do {
	        if ( getRadarTurnRemaining() == 0.0 )						// Início da batalha a rotação do radar é 0
	            setTurnRadarRightRadians( Double.POSITIVE_INFINITY ); 	// Rodar o radar infinitamente para a direita
	        	setAdjustRadarForGunTurn(true);  						// Radar não roda com a rotação da arma
	        	setAdjustGunForRobotTurn(true);							// Arma não roda com a rotacão do robô
	        	setAdjustRadarForRobotTurn(true);						// Radar não roda com a rotação do robô
	        execute();
	    } while ( true );
    }
    public void onScannedRobot(ScannedRobotEvent e) {
    	double angleToEnemy = getHeadingRadians() + e.getBearingRadians(); 								// Define variável do ângulo face o inimigo
    	double radarTurn = Utils.normalRelativeAngle(angleToEnemy - getRadarHeadingRadians()); 			// Define variável com o necessário para o radar rodar
    	double extraTurn = Math.min(Math.atan(36.0/e.getDistance()),Rules.RADAR_TURN_RATE_RADIANS);  	// Extra rotação do radar para compensar o movimento do inimigo
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
    	if (shotsMissed >= 7 && (shotsHitted/shotsMissed) < 0.2) {		// Se os tiros falhados forem maior que 8 e a razão entre os acertos e falhados menor que 1, não disparar
    		if (shootingStrategy=="static") {
    			shootingStrategy="linear";
        		System.out.println("change shooting Strategy from static to linear");
    		}else if (shootingStrategy=="linear") {
    			shootingStrategy="static";
        		System.out.println("change shooting Strategy from linear to static");
        		
    		}
    	   	shotsHitted = 0;						// Definir novamente a variável como 0
        	shotsMissed = 0;						// Definir novamente a variável como 0
    	}
    }
    
    public void onHitRobot(HitRobotEvent e) {	// Com o robô frente a nós disparar balas de potência máxima
    	fire(3);
		scan();
	}
    public void onHitWall(HitWallEvent e) {		// Se estivermos frente à parede e colidirmos, rodamos e avançamos, mas de costas para a parede apenas avançamos 
    	double h = getBattleFieldHeight();
		double w = getBattleFieldWidth();
		double x = getX();
		double y = getY();
		double bearing=e.getBearing();
		
		// Aplica-se quando batemos numa parede que não na esquina, dirigir para o meio do campo
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
    		setTurnRight(event.getBearing()+90-30*movementDirection);		// Ficar perpendicular ao robô inimigo		
        	double changeInEnergy = enemyEnergy-event.getEnergy();			// Calcular variância na energia do inimigo				
        	if (changeInEnergy>0 && changeInEnergy<=3) {					// Se a variância calculada for superior a 0 e inferior a 3, desviar
        		movementDirection = -movementDirection;						// Definir vairiável negativa para seguir caminho oposto
        		setAhead((event.getDistance()/4+25) * movementDirection);	// Fórmula para desviar, tendo em conta a variável "movementDirection", movemos para a direção oposta de onde nos dirigiamos
        	}
    	}

    	public void fire() {
    		if (shootingStrategy=="static") {								// Se "static" usar função "staticShooting"
    			staticShooting();
    		} else if (shootingStrategy=="linear") {						// Se "linear" usar função "linearShooting"
    			linearShooting();
    		}
    	}
    	
    	public void staticShooting() {
    		double absoluteBearing = getHeadingRadians() + event.getBearingRadians();						// Obter ângulo do inimigo em função da nossa posição
            setTurnGunRightRadians(Utils.normalRelativeAngle(absoluteBearing - getGunHeadingRadians()));	// Virar a arma na direção do inimigo
    		if(getGunHeat() == 0) {
    			setFire(Math.min(400 / event.getDistance(), 3)); 											// Disparar a bala se a arma não estiver sobreaquecida
           	}
    	}
    	
    	public void linearShooting() {
    		setColors(Color.green, Color.white, Color.green, Color.green, Color.green);		// Cor verde quando usamos a estratégia linearShooting
    		
    		double rX = getX();														// X atual do meu robô
            double rY = getY();														// Y atual do meu robô
           	double absBearing = event.getBearingRadians() + getHeadingRadians();	// Obter ângulo do inimigo em função da nossa posição
           	double predictX = rX + event.getDistance() * Math.sin(absBearing);		// Fórmula para obter X do inimigo perante as nossas coordenadas
           	double predictY = rY + event.getDistance() * Math.cos(absBearing);		// Fórmula para obter Y do inimigo perante as nossas coordenadas
           	double enemyHeading = event.getHeadingRadians(); 						// Direção que o inimigo se está a mover
           	double enemyVelocity = event.getVelocity();								// Velocidade do inimigo

        	double bulletPower = 3.0;
           	double deltaTime = 0; 													// Unidades de tempo que passaram

            	
           	// Incrementa 1 valor ao deltaTime -- Fórmula define a força da bala que iremos dispar --- Classe do java utilizada para obtermos a nossa função de distância consoante a nossa posiçãp
           	while ((++deltaTime) * (20 - 3.0 * bulletPower) < Point2D.Double.distance(rX, rY, predictX, predictY)) {
           		predictX += Math.sin(enemyHeading) * enemyVelocity;		//Calcula o X que se prevê que o inimigo estará, multiplicando o sin da direção que estava a tomar pela sua velocidade
           		predictY += Math.cos(enemyHeading) * enemyVelocity;		//Calcula o Y que se prevê que o inimigo estará, multiplicando o cos da direção que estava a tomar pela sua velocidade
           		
           		if (predictX > getBattleFieldWidth() || predictX < 0 || predictY > getBattleFieldHeight() || predictY < 0) {
           			break;		// Se X ou Y que prevermos for fora do campo de batalha, terminamos o loop
           		}
            }
            	
            double angle = Utils.normalAbsoluteAngle(Math.atan2(predictX - rX, predictY - rY)); 		// Classe "Utils" permite descobrirmos o ângulo de disparo
           	setTurnGunRightRadians(Utils.normalRelativeAngle(absBearing - getGunHeadingRadians())); 	// Virar a arma para a direção que queremos disparar
           	setTurnGunRightRadians(Utils.normalRelativeAngle(angle - getGunHeadingRadians()));
    		if(getGunHeat() == 0) {
           		setFire(bulletPower); 		// Disparar a bala se a arma não estiver sobreaquecida
           	}
    	}
    }
}

