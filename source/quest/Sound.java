package quest;

import java.net.*;
import javax.sound.sampled.*;

/*
 * A simple sound effect wrapper.
 */

public class Sound {

	private Clip clip;
	
	public Sound( String filepath)
	{
		try {
			
			//open a stream to the given filepath
			AudioInputStream stream;
			URL url = getClass().getResource(filepath);
			stream = AudioSystem.getAudioInputStream( url);
			Line.Info info = new Line.Info(Clip.class);
			
			//build a clip linked to that stream
			clip = (Clip) AudioSystem.getLine(info);
			clip.open( stream);
			
		} catch ( Exception e ) { e.printStackTrace(); return; }
	}
	
	public void play()
	{
		stop();
		clip.start();
	}
	
	public void repeat( int count)
	{
		if ( count < 1 ) return;
		
		stop();
		clip.loop( count-1);
	}
	
	public void loop()
	{
		stop();
		clip.loop( Clip.LOOP_CONTINUOUSLY);
	}
	
	public void stop()
	{
		clip.setFramePosition( 0);
		clip.stop();
	}
	
	public void close()
	{
		clip.close();
	}
}
