package quest;

import java.io.*;
import javax.sound.sampled.*;
import com.jcraft.jogg.*;
import com.jcraft.jorbis.*;

/*
 * A wrapper around a JOrbis-interpreted datastream from an .ogg file.
 * Plays automatically in its own thread once initialised.
 */

public class Music implements Runnable {

	private boolean isPaused = false;
	private Thread thread;
	
	private InputStream stream;
	private String filepath;
	
	private byte[] buffer;
	private int bufferPos = 0;
	private final static int BUFFER_SIZE = 1024*2;
	
	//JOgg objects
	private Packet oPacket;
	private Page oPage;
	private StreamState oStreamState;
	private SyncState oSyncState;
	
	//JOrbis objects
	private Block vBlock;
	private DspState vDspState;
	private Comment vComment;
	private Info vInfo;
	
	//Soundsystem objects
	private SourceDataLine sLineout = null;
	private float[][][] sPcmInfo;
	private int[] sPcmIndex;
	private byte[] sBuffer;
	
	Music( String filepath)
	{
		this.filepath = filepath;
		
		thread = new Thread( this);
		thread.start();
	}
	
	public void run()
	{
		while( true )
		{
			//open a stream to the file
			try {
				stream = getClass().getResource(filepath).openStream();//new FileInputStream( new File( filepath));
			} catch (Exception exFNF ) { exFNF.printStackTrace(); return; }
			
			//construct JOgg and JOrbis objects
			oPacket = new Packet();
			oPage = new Page();
			oStreamState = new StreamState();
			oSyncState = new SyncState();
			vDspState = new DspState();
			vBlock = new Block( vDspState);
			vComment = new Comment();
			vInfo = new Info();
			
			//initialise JOrbis
			oSyncState.init();
			oSyncState.buffer( BUFFER_SIZE);
			buffer = oSyncState.data;
			bufferPos = 0;
			
			//clean up the previous objects
			System.gc();
			
			//parse the file
			readHeader();
			initSoundsys();
			decodeBody();
			
			cleanUp();
		}
	}
	
	public void stop()
	{
		isPaused = true;
	}
	
	public String getFilepath()
	{
		return filepath;
	}
	
	private void readHeader()
	{
		boolean done = false;
		int packet = 1;
		
		while ( !done )
		{
			//read in BUFFER_SIZE bytes of data
			int bytesread = 0;
			try {
				bytesread = stream.read( buffer, bufferPos, BUFFER_SIZE);
			} catch ( IOException exIO ) { exIO.printStackTrace(); return; }
			
			//tell syncstate that bytes have been read
			oSyncState.wrote( bytesread);
			
			//pass a page of data to the ogg parser
			if ( oSyncState.pageout( oPage) != 0 ) 
			{
				//behave differently based on the packet just read in
				switch( packet )
				{
					case 1:
						
						//initialise streamstate, info and comment
						oStreamState.init( oPage.serialno());
						oStreamState.reset();
						vInfo.init();
						vComment.init();
						
						//pass data around (i frankly don't know what this does)
						oStreamState.pagein( oPage);
						oStreamState.packetout( oPacket);
						vInfo.synthesis_headerin( vComment, oPacket);
						
						packet++;
						
					break;
					
					case 2: case 3:
						
						oStreamState.pagein( oPage);
						if ( oStreamState.packetout( oPacket) == 0 ) break;
						
						vInfo.synthesis_headerin( vComment, oPacket);
						
						packet++;
						if ( packet >= 4 ) done = true;
						
					break;	
				}
			}
			
			//fix up the buffer
			bufferPos = oSyncState.buffer( BUFFER_SIZE);
			buffer = oSyncState.data;
		}
	}

	private void initSoundsys()
	{
		sBuffer = new byte[BUFFER_SIZE*2];
		vDspState.synthesis_init( vInfo);
		vBlock.init( vDspState);
		
		int channels = vInfo.channels;
		int rate = vInfo.rate;
		
		AudioFormat aFormat = new AudioFormat( (float)rate, 16, channels, true, false);
		DataLine.Info aInfo = new DataLine.Info( SourceDataLine.class, aFormat, AudioSystem.NOT_SPECIFIED);
		
		//open the output audio data line
		try {
			sLineout = (SourceDataLine) AudioSystem.getLine( aInfo);
			sLineout.open( aFormat);
			sLineout.start();
		} catch ( Exception ex ) { ex.printStackTrace(); return; }
		
		sPcmInfo = new float[1][][];
		sPcmIndex = new int[vInfo.channels];
	}

	private void decodeBody()
	{
		while ( true )
		{
			if ( oSyncState.pageout( oPage) != 0 )
			{
				oStreamState.pagein( oPage);
				if ( oPage.granulepos() == 0 ) break;
				
				while ( true )
				{
					//allow for accurate stopping behaviour
					while ( isPaused ) 
					{
						try {
						Thread.sleep( 100); sLineout.stop();
						} catch ( InterruptedException exI ) { exI.printStackTrace(); return; }
					}
					
					if ( oStreamState.packetout( oPacket) == 0 ) break;
					
					decodePacket();
				}
				
				if ( oPage.eos() != 0 ) break;
			}
			
			bufferPos = oSyncState.buffer( BUFFER_SIZE);
			buffer = oSyncState.data;
			
			if ( bufferPos == -1 ) break;
				
			int count = -1;
			try {
				count = stream.read( buffer, bufferPos, BUFFER_SIZE);
			} catch ( IOException exIO ) { return; }
			  catch ( IndexOutOfBoundsException exIOOB ) { }
				
			oSyncState.wrote( count);
			if ( count == 0 ) break;
		}
	}
	
	private void decodePacket()
	{
		int samples, range;
		
		if ( vBlock.synthesis( oPacket) == 0 )
		vDspState.synthesis_blockin( vBlock);
		
		while ( ( samples = vDspState.synthesis_pcmout( sPcmInfo, sPcmIndex) ) > 0 )
		{
			if ( samples < BUFFER_SIZE*2 ) range = samples;
			else range = BUFFER_SIZE*2;
			
			for( int i=0; i<vInfo.channels; i++)
			{
				int index = i*2;
				
				for( int j=0; j<range; j++)
				{
					int value = (int) (sPcmInfo[0][i][sPcmIndex[i] + j] * 32767);
					
					if (value > 32767 ) value = 32767;
                    if (value < -32768 ) value = -32768;
                    if (value < 0 ) value = value | 32768;
                    
                    sBuffer[index] = (byte)value;
                    sBuffer[index+1] = (byte)(value>>>8);
                    
                    index += 2*vInfo.channels;
				}
			}
			
			sLineout.write( sBuffer, 0, 2 * vInfo.channels * range);
			vDspState.synthesis_read( range);
		}
	}

	private void cleanUp()
	{
		oStreamState.clear();
		vBlock.clear();
		vDspState.clear();
		vInfo.clear();
		oSyncState.clear();
		
		try { 
			stream.close();
		} catch ( IOException exIO ) { exIO.printStackTrace(); return; }
	}
}
