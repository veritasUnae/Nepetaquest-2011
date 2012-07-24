package quest.field;

import quest.*;
import quest.field.trans.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/*
 * FieldModule is the core field engine class. It wraps around various
 * essential objects (one Room, one Player, one shared DrawOp, one TextBox, 
 * up to one running FieldScript, and up to one running Transition).
 * See the .java files for each class for the details of how they work.
 * 
 * Other than those objects, FieldModule contains relatively little state.
 * 
 * viewX and viewY are the viewport's top-left corner.
 * 
 * fadeAlpha and fadeDelta are a simple "fade to black" system, currently
 * used when switching between rooms. Might be useful when switching to
 * a menu, a cutscene, etc.
 * 
 * switchingRooms, roomSwitchTimer, destRoom, destX and destY are
 * all concerned with switching from one room to another.
 */

public class FieldModule extends Module {

	public static final int type = 2;
	public static FieldModule module = null;
	
	private Room currentRoom;
	private FieldScript currentScript;
	private TextBox textBox;
	private DrawOp drawOp;
	private Transition currentTransition;
	
	//FieldModule state
	private int viewX, viewY;
	private int fadeAlpha = 0, fadeDelta = 0;
	private int switchingRooms = -1;
	private int roomSwitchTimer = 0;
	private String destRoom = null;
	private int destX, destY;
	
	//Player state
	private Player player;
	private boolean pmoving = false;
	private int pdir = Actor.SOUTH, pdir_prev = Actor.SOUTH;
	
	public FieldModule()
	{
		module = this;
		frameBuffer = new BufferedImage( Game.SCREENW, Game.SCREENH, BufferedImage.TYPE_INT_ARGB);
		
		player = new Player( 475, 45, Actor.SOUTH);
		viewX = player.x-Game.SCREENW/2;
		viewY = player.y-Game.SCREENH/2;
		
		currentRoom = new Room( Game.DATAPATH+"lawnring.nq2");
		currentScript = new FieldScript(currentRoom.getInitScript());
		currentTransition = null;
		textBox = new TextBox();
		drawOp = new DrawOp();
		
		player.init(currentRoom);
		
		Game.getGame().setBackgroundMusic( Game.MUSICPATH+"field_carcrab.ogg");
	}
	
	public static FieldModule get()
	{
		/*
		 * The FieldModule can be accessed globally.
		 * This is probably bad practice, but I could see no reason
		 * not to do it anyway.
		 */
		
		return module;
	}
	
	private void render()
	{
		//blank the framebuffer
		Graphics g = frameBuffer.getGraphics();
		g.setColor( Color.black);
		g.fillRect( 0, 0, frameBuffer.getWidth(null), frameBuffer.getHeight(null));
		
		//retrieve the current room's tilemap and draw it
		Tilemap tilemap = currentRoom.getTilemap();
		tilemap.drawTo( drawOp, currentRoom.getTileset(), viewX, viewY);
		
		//render the player
		player.draw( drawOp);
		
		//render the room objects
		List<RObject> objects = currentRoom.getObjectList();
		for( int i=0; i<objects.size(); i++) 
		{
			if ( objects.get(i).isEnabled() )
			objects.get(i).onDraw( drawOp);
		}
		
		//flush the drawing queue
		drawOp.flush( g, viewX, viewY);
		
		//draw the text box as an overlay
		textBox.drawTo( g);
		
		//when fading to black, darken everything on-screen
		if ( fadeAlpha > 0 )
		{
			g.setColor( new Color( 0, 0, 0, fadeAlpha));
			g.fillRect( 0, 0, frameBuffer.getWidth(null), frameBuffer.getHeight(null));
		}
	}
	
	public void step()
	{
		//get keyboard input information. used in various places below.
		InputMap input = Game.getGame().getInputMap();
		
		//TEMP: quit the demo when Q is pressed.
		if ( input.isKeyDown( KeyEvent.VK_Q) ) 
		{ 
			exitFlag = true; 
			successor = Game.MODULE_MENU; 
			return;
		}
		
		//TEMP: test the Transition system when X is pressed.
		if ( input.isKeyPressed( KeyEvent.VK_X) ) 
		{
			currentTransition = new FragmentTransition(frameBuffer);
		}
		
		//if a Transition is running, the entire field engine is suspended,
		//except for the code below.
		if ( currentTransition != null )
		{
			if ( currentTransition.iterate() == false )
			{
				currentTransition = null;
			}
			else
			{
				Image frame = currentTransition.getFrame();
				Graphics g = frameBuffer.getGraphics();
				g.drawImage( frame, 0, 0, null);
				
				return;
			}
		}
		
		//various things might cause the entire field engine to be paused.
		//if so, room objects shouldn't receive any events other than onDraw,
		//and Nepeta should not respond to player input.
		boolean isPaused = (currentScript != null || switchingRooms >= 0 );
		if ( !isPaused )
		{
			//invoke certain event methods for each room object
			Iterator<RObject> it = currentRoom.getObjectList().iterator();
			while ( it.hasNext() )
			{
				RObject obj = it.next();
				if ( obj.isEnabled() == false ) continue;
				
				//onInteract
				if ( input.isKeyPressed( KeyEvent.VK_E) )
				if ( obj.isInteractable( player.x, player.y, player.w, player.h, player.dir) ) 
				obj.onInteract();
				
				//onStep
				obj.onStep();
				
				//onContact
				Rectangle obounds = obj.getBounds();
				Rectangle pbounds = new Rectangle(player.x-player.w/2,player.y-player.h/2,
												  player.w, player.h);
				if ( obounds.intersects(pbounds) ) obj.onContact();
			}
		}
		
		//handle movement
		if ( !isPaused ) movePlayer();
		moveViewport();
		
		//perform the current script until it's either finished
		//completely, or finished for the current step.
		if ( currentScript != null && !textBox.isVisible() ) 
		{
			if ( currentScript.run() == false ) 
			currentScript = null;
		}
		
		//update the TextBox
		if ( input.isKeyPressed( KeyEvent.VK_E) ) textBox.buttonPressed();
		textBox.step();
		
		//handle fading to black
		fadeAlpha += fadeDelta;
		if ( fadeAlpha < 0 ) fadeAlpha = 0;
		if ( fadeAlpha > 255 ) fadeAlpha = 255;
		
		//handle switching to a different room
		if ( switchingRooms >= 0 )
		{
			roomSwitchTimer--;
			fadeDelta = 20;
			
			if ( roomSwitchTimer <= 0 )
			{
				currentRoom = new Room( destRoom);
				currentScript = new FieldScript( currentRoom.getInitScript());
				player.teleport( destX, destY);
				viewX = player.x - Game.SCREENW/2;
				viewY = player.y - Game.SCREENH/2;
				
				switchingRooms = -1;
				destRoom = null;
				roomSwitchTimer = 0;
				fadeDelta = -20;
			}
		}
		
		//render to the framebuffer
		//we disable this if a transition has been called, to prevent a one-frame "jitter".
		if ( currentTransition == null ) render();
	}
	
	public void exit()
	{
	}
	
	/*
	 * Public methods, used to trigger various module-wide events.
	 * Namely: switching to a different room, executing a FieldScript,
	 * displaying a text box, and triggering a battle.
	 */
	
	public void switchToRoom( String roompath, int mode, int destX, int destY)
	{
		/*
		 * mode can have the following values:
		 * 0	switch to the given room immediately
		 * 1	delay for twenty frames, fade to black, then switch rooms
		 */
		
		if ( switchingRooms >= 0 ) return;
		
		switchingRooms = mode;
		destRoom = Game.DATAPATH + roompath;
		this.destX = destX;
		this.destY = destY;
		
		if ( mode == 0 ) roomSwitchTimer = 0;
		else roomSwitchTimer = 15;
	}
	
	public void executeScript( FieldScript fs)
	{
		currentScript = fs;
	}
	
	public void textMessage( String speaker, String text)
	{
		textBox.setText( speaker, text);
	}
	
	public void TEMPORARY_battlefwoosh()
	{
		currentTransition = new FragmentTransition(frameBuffer);
		pmoving = false;
	}
	
	/*
	 * Getters.
	 */

	public Room getRoom()
	{
		return currentRoom;
	}
	
	public Player getPlayer()
	{
		return player;
	}
	
	public RObject getObject( String id)
	{
		/*
		 * FieldScripts identify room objects based on their
		 * name (aka their string id). getObject and getActor
		 * are used pretty much exclusively by FieldScript.
		 */
		
		id = id.toLowerCase();
		
		List<RObject> list = currentRoom.getObjectList();
		
		Iterator<RObject> it = list.iterator();
		while( it.hasNext() )
		{
			RObject obj = it.next();
			if ( obj.id.equals(id) ) return obj;
		}
		
		return null;
	}
	
	public Actor getActor( String id)
	{
		id = id.toLowerCase();
		
		if ( id.equals("player") ) return player;
		if ( id.equals("nepeta") ) return player;
		
		Map<String,Actor> map = currentRoom.getActorMap();
		if ( map.containsKey( id) ) return map.get( id);
		else return null;
	}
	
	/*
	 * Utility functions used by step().
	 */
	
	private void movePlayer()
	{
		InputMap input = Game.getGame().getInputMap();
		int[] dirkeys = { KeyEvent.VK_W, KeyEvent.VK_D, KeyEvent.VK_S, KeyEvent.VK_A};
		
		//if a key has been pressed, start moving in that direction
		for( int i=0; i<4; i++)
		if ( input.isKeyPressed( dirkeys[i] ) ) { pmoving = true; pdir_prev = pdir; pdir = i; }
		
		//if the current key has been released, either revert the direction or stop
		if ( input.isKeyDown( dirkeys[pdir]) == false )
		{ 
			if ( input.isKeyDown( dirkeys[pdir_prev]) && pdir_prev != pdir ) pdir = pdir_prev; 
			else { pdir_prev = pdir; pmoving = false; }
		}
		
		//perform movement
		if ( pmoving ) player.move( pdir, 3);
	}
	
	private void moveViewport()
	{
		//move the viewport towards the player, with a slight delay
		int dx = player.x-Game.SCREENW/2, dy = player.y-Game.SCREENH/2;
		if ( viewX < dx ) viewX += (dx-viewX)/3;
		if ( viewX > dx ) viewX += (dx-viewX)/3;
		if ( viewY < dy ) viewY += (dy-viewY)/3;
		if ( viewY > dy ) viewY += (dy-viewY)/3;
		
		//lock the viewport to the viewable region
		int rw = currentRoom.getPixelWidth();
		int rh = currentRoom.getPixelHeight();
		if ( viewX < 0 ) viewX = 0; 
		if ( viewY < 0 ) viewY = 0; 
		if ( viewX > rw-Game.SCREENW ) viewX = rw-Game.SCREENW;
		if ( viewY > rh-Game.SCREENH ) viewY = rh-Game.SCREENH;
		
		//if the room is smaller than the viewport, center it
		if ( rw < Game.SCREENW ) viewX = -(Game.SCREENW-rw)/2;
		if ( rh < Game.SCREENH ) viewY = -(Game.SCREENH-rh)/2;
	}
}


/*
 * To my successor(s): I've done what I can to make the field engine 
 * understandable. If you find something that you absolutely can't 
 * make sense of, feel free to try and get in touch with me through 
 * the MSPA forums.
 * 
 * I hope you have as much fun finishing this game as I did starting it.
 * 
 * - Frost
 */


