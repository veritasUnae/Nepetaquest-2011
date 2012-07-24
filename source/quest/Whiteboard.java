package quest;

/*
 * A way for modules to pass data to their successor.
 * Before exiting, a module can set certain Whiteboard variables,
 * which will be read when the next module is constructed.
 * 
 * Game stores a single persistent instance as its "whiteboard" member.
 * This instance is always wiped clean at the end of switchModuleTo().
 */

public class Whiteboard {

	public boolean allFadeIn;			//tells any module that the previous module faded out to black before
										//switching, so it should fade in from black if possible.
	
	public String fldRunScript;			//has Field run the given field script once the room loads
	public String fldGotoRoom;			//has Field switch to the given room on startup
	public int fldGotoX, fldGotoY; 		//has Field change the player's current position
	
	public int battleAllyCount;
	public String[] battleAllyNames;
	public int battleEnemyCount;		//has Battle create an encounter with the given members
	public String[] battleEnemyNames;	//should always contain <bttlEnemyCount> members
	public String bttlOverrideMusic;   	//has Battle play the given music instead of the default
	public String bttlOverrideBack;		//has Battle load the given background sprite
	public Inventory inventory;

	Whiteboard()
	{
		wipeClean();
	}
	
	public void wipeClean()
	{
		allFadeIn = false;
		
		fldRunScript = null;
		fldGotoRoom = null;
		fldGotoX = -1000;
		fldGotoY = -1000;
		
		battleEnemyCount = 0;
		battleEnemyNames = null;
		battleAllyCount = 0;
		battleAllyNames = null;
		bttlOverrideMusic = null;
		bttlOverrideBack = null;
		inventory = null;
	}
}
