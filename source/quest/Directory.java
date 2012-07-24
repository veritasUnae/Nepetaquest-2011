package quest;

import java.io.*;
import java.util.*;

/*
 * A wrapper for directory traversal. Effectively a "stream factory"
 * for files within its directory.
 */

public class Directory {

	String path;
	List<String> files;
	
	public Directory( String path)
	{
		this.path = path;
		files = new ArrayList<String>();
		
		InputStream index = getClass().getResourceAsStream( path+"/index.txt");
		if ( index == null ) return;
		
		BufferedReader reader = new BufferedReader( new InputStreamReader(index));
		
		try {
			
			String line;
			while ( (line = reader.readLine()) != null )
			{
				files.add(line);
			}
		
		} catch ( IOException ioe ) { return; }
	}
	
	public boolean fileExists( String name)
	{
		if ( files.contains(name) ) return true;
		return false;
	}
	
	public InputStream getFileAsStream( String name)
	{
		if ( fileExists(name) )
		{
			return getClass().getResourceAsStream( path+name);
		}
		
		return null;
	}
	
	public List<String> getFileList()
	{
		return files;
	}
}
