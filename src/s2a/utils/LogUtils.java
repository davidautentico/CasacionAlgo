package s2a.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;

public class LogUtils {
	FileWriter writer = null;
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        
	public LogUtils(String fileName,boolean delete){
		
		File f = new File(fileName);
                
		if (f.exists() && delete) f.delete();
		
	    try{
	    	writer = new FileWriter(fileName,true);
	    }catch(Exception e){
	    	e.printStackTrace();
	    	try{
	    		writer.close();
	    	}catch(Exception e1){
		    	e1.printStackTrace();
			}
		 }
	}
	
	public void addToLog(String msg){

		if (writer!=null){
			try{
                            
                            ZonedDateTime now = ZonedDateTime.now();
                            writer.append("[ "+now.format(dtf)+"] "+msg+"\n"); 
                               writer.close();
			}catch(Exception e){
		    	e.printStackTrace();
			}
		}
	}
	
	public void close(){
		if (writer!=null){
			try{
				writer.close();
			}catch(Exception e){
		    	e.printStackTrace();
	     }
		}
	}
}
