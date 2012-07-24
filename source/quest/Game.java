package quest;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import quest.battle.*;
import quest.field.*;
import java.net.*;
import java.io.File;
import java.io.FileReader;

public class Game extends JFrame implements WindowListener, KeyListener, Runnable {
	
	private static final long serialVersionUID = -4708027921426744660L;
	public static final int SCREENW = 320, SCREENH = 240;
	public static final int MODULE_QUIT = -1, MODULE_BLANK = 0, MODULE_BATTLE = 1;
	public static final int MODULE_FIELD = 2, MODULE_MENU = 3, MODULE_PRELOADER = 4, MODULE_TUTORIAL = 5, MODULE_GAMEOVER = 6;
	public static final String SPRITEPATH = "/quest/sprites/";
	public static final String SOUNDPATH = "/quest/sound/";
	public static final String MUSICPATH = "/quest/sound/bg/";
	public static final String DATAPATH = "/quest/data/";
	
	private static Game singletonGame = null;
	
	private Module currentModule = null;
	private Music currentMusic = null;
	private Whiteboard whiteboard;
	private State state;
	private InputMap primaryInput, secondaryInput;
	
	private String runningDir;

	public int volume = 100;
	
	private Game() {
		super("Nepetaquest 2011 Internal Build - Sunday, May 29th");
		setSize( SCREENW*2, SCREENH*2);
		setResizable(false);
		
		//center on the desktop
		Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation( (ss.width-getWidth())/2, (ss.height-getHeight())/2);
		setVisible(true);
		
		//account for the size of the window frame
		Insets i = getInsets();
		setSize( getWidth()+i.left+i.right, getHeight()+i.top+i.bottom);
		
		state = new State();
		whiteboard = new Whiteboard();
		primaryInput = new InputMap();
		secondaryInput = new InputMap();
		singletonGame = this;
		/*
		try {
			URL path = getClass().getResource("Game.class");
			runningDir = path.toURI().toString().replaceFirst("file:","").replaceFirst("jar:","").replaceFirst("nepquest.jar!/","").replace("quest/Game.class","");
			FileReader f = new FileReader(runningDir + "designSpecs.txt");
		} catch(Exception e) {e.printStackTrace();}
		*/
	}
	public static void main(String[] args) {
		
		Game game = new Game();
		game.state = new State();
		game.addWindowListener(game);
		game.addKeyListener(game);
		game.switchModuleTo(MODULE_PRELOADER);
		if(args.length > 0 ) {
			if(args[0].equals("-silent")) {
				game.volume = 0;
			}
		}
		
		Thread stepThread = new Thread(game);
		stepThread.start();
		try {
			stepThread.join();
		} catch (InterruptedException ie) {}
	}
	public synchronized void paint(Graphics g) {
		
		Image frameBuffer = null;
		if ( currentModule != null ) 
			frameBuffer = currentModule.getFrameBuffer();
		if ( frameBuffer != null )	
		{
			Insets i = getInsets();
			int w = frameBuffer.getWidth(null);
			int h = frameBuffer.getHeight(null);
			
			g.drawImage(currentModule.getFrameBuffer(),i.left,i.top,i.left+w*2,i.top+h*2,0,0,w,h,null);
		}
		
		notifyAll();
	}
	public synchronized void run() {
		
		long timeStart = System.currentTimeMillis();
		
		while(true) {
			
			translateInput();
			currentModule.step();
			repaint();
			
			if(currentModule.saveFlag) {
				save();
				currentModule.saveFlag = false;
			}
			if(currentModule.exitFlag) {
				switchModuleTo(currentModule.successor);
				translateInput();
				currentModule.step();
			}
			
			//timing
			try {
				wait();
				long timePassed = System.currentTimeMillis() - timeStart;
				if ( timePassed < 30 ) Thread.sleep( 30 - timePassed);
				timeStart = System.currentTimeMillis();
			} catch (InterruptedException ie) {}
		}
	}
	
	/*
	 * Major interface functions for modules.
	 */
	
	public void switchModuleTo( int moduleType) {
		if ( currentModule != null ) currentModule.exit();
		currentModule = null;
		
		System.gc();
		
		//construct an appropriate module
		switch(moduleType) {
		case MODULE_QUIT:
			//Quit
			System.exit(0);
			break;
		case MODULE_BATTLE : 
			state.currentPath = 0;
			currentModule = new BattleModule();
			whiteboard.inventory = new Inventory();
			for(int i=0; i<whiteboard.battleAllyCount;i++) {
				((BattleModule) currentModule).addAlly(1,whiteboard.battleAllyNames[i]);
			}
			for(int i=0;i<whiteboard.battleEnemyCount;i++) {
				((BattleModule) currentModule).addEnemy(1,whiteboard.battleEnemyNames[i]);
			}
			((BattleModule) currentModule).inventory = whiteboard.inventory;
			((BattleModule) currentModule).finalize(); //This could be a method for all Modules...
			break;
		case MODULE_FIELD:
			currentModule = new FieldModule();
			break;
		case MODULE_PRELOADER:
			currentModule = new PreloaderModule();
			break;
		case MODULE_MENU:
			currentModule = new MenuModule(1);
			break;
		case MODULE_TUTORIAL:
			state.currentPath = 0;
			currentModule = new TutorialBattle();
			whiteboard.inventory = new Inventory();
			for(int i=0; i<whiteboard.battleAllyCount;i++) {
				((BattleModule) currentModule).addAlly(1,whiteboard.battleAllyNames[i]);
			}
			for(int i=0;i<whiteboard.battleEnemyCount;i++) {
				((BattleModule) currentModule).addEnemy(1,whiteboard.battleEnemyNames[i]);
			}
			((BattleModule) currentModule).inventory = whiteboard.inventory;
			((BattleModule) currentModule).finalize(); 
			break;
		case MODULE_GAMEOVER:
			currentModule = new GameOverModule();
			break;
		default :
			break;
		}
		
		whiteboard.wipeClean();
	}
	public void setBackgroundMusic( String filepath) {
		
		if ( filepath == null || filepath.length() == 0 || volume == 0 )
		{
			//remove the background music
			if ( currentMusic != null ) 
			{
				currentMusic.stop();
				currentMusic = null;
				System.gc();
			}
		}
		else
		{
			//if it's the current track, return
			if ( currentMusic != null )
			if ( currentMusic.getFilepath().equals( filepath) ) return;
			
			//replace the background music
			if ( currentMusic != null ) currentMusic.stop();
			currentMusic = new Music( filepath);
			System.gc();
		}
	}
	public static Game getGame() {
		return singletonGame;
	}
	public State getGameState() { 
		return state;
	}
	public Module getModule() {
		return currentModule;
	}
	public Whiteboard getWhiteboard() {
		return whiteboard;
	}
	public void save() {
		state.saveState("nepetaquest.sav");
	}
	public int getScreenHeight() {
		Insets i = getInsets();
		return getHeight() - i.top - i.bottom;
	}
	public int getScreenWidth() {
		Insets i = getInsets();
		return getWidth() - i.left - i.right;
	}
	
	/*
	 * KeyListener methods, which defer to keyEvent().
	 */
	
	public void keyPressed(KeyEvent ke) {
		primaryInput.anyKey = true;
		keyEvent( ke, InputMap.PRESSED);
	}
	public void keyReleased(KeyEvent ke) {
		keyEvent( ke, InputMap.RELEASED);
	}
	public void keyTyped(KeyEvent ke) {
	}
	
	/*
	 * Primary and secondary InputMap updaters.
	 */
	
	private void keyEvent( KeyEvent ke, int type) {
		int code = ke.getKeyCode();
		long eventTime = System.currentTimeMillis();
		if((type == InputMap.PRESSED) && (primaryInput.isKeyDown(code))) {
			return;
		}
		if(type == InputMap.RELEASED) {
			primaryInput.lastRelease[code] = eventTime;
		}
		primaryInput.keyArray[code] = type;
		if((type == InputMap.PRESSED) && (eventTime - primaryInput.lastRelease[code] < 30)) {
			primaryInput.keyArray[code] = InputMap.DOWN;
		}
	}
	private void translateInput() {
		
		for( int i=0; i<256; i++)
		{
			if ( secondaryInput.keyArray[i] == InputMap.PRESSED ) 
				secondaryInput.keyArray[i] = InputMap.DOWN;
			
			if ( secondaryInput.keyArray[i] == InputMap.RELEASED )
				secondaryInput.keyArray[i] = InputMap.UP;
			
			if ( primaryInput.keyArray[i] == InputMap.PRESSED ) 
			{
				primaryInput.keyArray[i] = InputMap.DOWN;
				secondaryInput.keyArray[i] = InputMap.PRESSED;
			}
			
			if ( primaryInput.keyArray[i] == InputMap.RELEASED ) 
			{
				primaryInput.keyArray[i] = InputMap.UP;
				secondaryInput.keyArray[i] = InputMap.RELEASED;
			}
		}
	}
	
	/*
	 * A getter for an InputMap suitable for use by modules
	 */
	
	public InputMap getInputMap()
	{
		InputMap new_map = new InputMap();
		for( int i=0; i<256; i++){
			 new_map.keyArray[i] = secondaryInput.keyArray[i];
			if(secondaryInput.keyArray[i] == InputMap.PRESSED) 
				new_map.anyKey = true;
		}
		primaryInput.anyKey = false;
		return new_map;
	}
	
	/*
	 * WindowListener methods.
	 */
	
	public void windowActivated(WindowEvent we) {		
	}
	public void windowClosed(WindowEvent arg0) {
	}
	public void windowClosing(WindowEvent arg0) {
		System.exit(0);
	}
	public void windowDeactivated(WindowEvent we) {		
	}
	public void windowDeiconified(WindowEvent we) {		
	}
	public void windowIconified(WindowEvent we) {		
	}
	public void windowOpened(WindowEvent we) {		
	}
	public String getRunningPath() {
		return runningDir;
	}
	public String[] getFiles() {	
		return new File(runningDir).list();
	}

}
