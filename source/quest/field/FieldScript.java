package quest.field;

import java.util.*;
import java.io.*;
import quest.*;

/*
 * FieldScript interprets scripts (presented as Strings or
 * text Files) written in a simple, unique scripting language.
 * See associated documentation for details.
 * 
 * [Note: this class is a bit of a mess. If you can't understand
 * it, that's because it's badly written, not because it's doing
 * anything particularly complicated or interesting. You might
 * want to rewrite it. It's mostly self-contained, so that shouldn't
 * be particularly difficult. -Frost]
 */

public class FieldScript {

	private Scanner scanner;
	boolean errorFlag = false;
	
	//set if an action is being executed
	private boolean blocking = false;
	private String currentAction = "";
	
	//variables for the WAIT action
	private int waitTime;
	
	//for the MOVE action
	private int moveDir, moveDist;
	private Actor moveActor;
	
	//for the SOUND action
	private Sound soundData = null;
	
	FieldScript( String string)
	{
		scanner = new Scanner( string);
	}
	
	FieldScript( File file)
	{
		try {
			scanner = new Scanner( file);
		} catch ( FileNotFoundException exFNF ) { errorFlag = true; }
	}
	
	/*
	 * Executes until a blocking function or the eof is reached.
	 * Should be called once every step.
	 * If the script comes to an end or finds an error, the method returns false.
	 */
	
	public boolean run()
	{
		FieldModule module = FieldModule.get();
		if ( errorFlag ) return false;
		
		//if an action is in process, execute it
		if ( blocking )
		{
			if ( currentAction.equals("WAIT") )
			{
				waitTime--;
				if ( waitTime <= 0 ) { blocking = false; }
			}
			
			if ( currentAction.equals("MOVE") )
			{
				int distToMove = Actor.MOVESPEED;
				if ( distToMove > moveDist ) distToMove = moveDist;
				
				moveActor.move( moveDir, distToMove);
				
				moveDist -= Actor.MOVESPEED;
				if ( moveDist <= 0 ) { blocking = false; }
			}
		}
		
		//if not, or if the current action completed, process some more commands
		if ( !blocking )
		{
			boolean finished = false;
			while ( !finished )
			{
				try {
					//retrieve the command
					String command = scanner.next().toUpperCase();
					
					//ERROR, prints diagnostic message
					if ( command.equals("ERROR") )
					{
						System.out.println( parseString());
					}
					
					//WAIT, pauses execution
					if( command.equals("WAIT") )
					{
						//get the duration
						int time = Integer.parseInt( scanner.next());
						
						currentAction = "WAIT";
						waitTime = time;
						blocking = true;
					}
					
					//TEXT, opens a textbox
					if ( command.equals("TEXT") )
					{
						String speaker = scanner.next();
						module.textMessage( speaker, parseString());
						finished = true;
					}
					
					//ENABLE, enables an object
					if ( command.equals("ENABLE") )
					{
						//retrieve the object
						String objectID = scanner.next();
						RObject object = module.getObject( objectID);
						if ( object == null ) return false;
						
						//enable it
						if ( !object.isEnabled() ) object.enable();
					}
					
					//DISABLE, disables an object
					if ( command.equals("DISABLE") )
					{
						//retrieve the object
						String objectID = scanner.next();
						RObject object = module.getObject( objectID);
						if ( object == null ) return false;
						
						//enable it
						if ( object.isEnabled() ) object.disable();
					}
					
					//MOVE, moves an actor
					if ( command.equals("MOVE") )
					{
						//retrieve the actor
						String actorID = scanner.next();
						Actor actor = module.getActor( actorID);
						if ( actor == null ) return false;
						
						//retrieve the direction of movement
						String dirString = scanner.next();
						int dir;
						switch( dirString.charAt(0) )
						{
							case 'N': case 'n': dir = Actor.NORTH; break;
							case 'E': case 'e': dir = Actor.EAST; break;
							case 'S': case 's': dir = Actor.SOUTH; break;
							case 'W': case 'w': dir = Actor.WEST; break;
							default: return false;
						}
						
						//retrieve the distance
						int dist = Integer.parseInt( scanner.next());
						
						//begin the action, block until it's complete
						currentAction = "MOVE";
						moveDir = dir;
						moveDist = dist;
						moveActor = actor;
						blocking = true;
					}
					
					//FACE, changes an actor's direction
					if ( command.equals("FACE") )
					{
						//retrieve the actor
						String actorID = scanner.next();
						Actor actor = module.getActor( actorID);
						if ( actor == null ) return false;
						
						//retrieve the direction
						String dirString = scanner.next();
						int dir;
						switch( dirString.charAt(0) )
						{
							case 'N': case 'n': dir = Actor.NORTH; break;
							case 'E': case 'e': dir = Actor.EAST; break;
							case 'S': case 's': dir = Actor.SOUTH; break;
							case 'W': case 'w': dir = Actor.WEST; break;
							default: return false;
						}
						
						//perform the action
						actor.face( dir);
					}
					
					//ANIM, causes an actor to animate
					if ( command.equals("ANIM") )
					{
						//retrieve the actor
						String actorID = scanner.next();
						Actor actor = module.getActor( actorID);
						if ( actor == null ) return false;
						
						//retrieve the animation data
						String anim = scanner.next();
						int rate = Integer.parseInt(scanner.next());
						int repeats = Integer.parseInt(scanner.next());
						
						//perform the action
						actor.animate( anim, rate, repeats);
					}
					
					//SOUND, plays a sound effect
					if ( command.equals("SOUND") )
					{
						//retrieve the filename
						String filename = scanner.next();
						
						//load and play the sound
						soundData = new Sound(Game.SOUNDPATH + "field/" + filename);
						soundData.play();
					}
					
					if ( blocking ) finished = true;
				} 
				catch ( NoSuchElementException exNSE ) { return false; }
			}
		}
		
		return true;
	}
	
	/*
	 * Utility function.
	 * Retrieve a single-quoted string from the Scanner.
	 */
	
	private String parseString()
	{
		String s = scanner.next();
		
		if ( s.charAt(0) != '\'' ) return s.substring(1);
		
		while ( s.charAt(s.length()-1) != '\'' )
		{
			if ( !scanner.hasNext() ) return s.substring(1);
			
			String n = scanner.next();
			s = s.concat( " " + n);
		}
		
		return s.substring( 1, s.length()-1);
	}
}
