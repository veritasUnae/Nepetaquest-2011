package quest.field;

import quest.*;
import java.util.*;

/*
 * A hashmap that loads and stores sprites on request based on
 * their filename. Prevents multiple loading of sprites. Should be used
 * only when duplicate requests are a possibility.
 * 
 * Provides no unloading capability; it can only be replaced with
 * a newly-constructed and empty bank.
 */

public class SpriteBank {
	
	Map<String,Sprite> map = new HashMap<String,Sprite>(100);
	
	public Sprite loadSprite( String filepath)
	{
		if ( map.containsKey( filepath) ) 
		{
			return map.get( filepath);
		}
		else
		{
			Sprite sprite = new Sprite( filepath);
			map.put( filepath, sprite);
			return sprite;
		}
	}
}
