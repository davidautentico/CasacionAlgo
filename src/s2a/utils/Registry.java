package s2a.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;

/**
 * Access to Windows Registry
 * @author david
 *
 */
public class Registry {
	 /**
     * 
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */
    public static final String readRegistry(String location, String key){
        try {
            // Run reg query, then read output with StreamReader (internal class)
        	String reg = "reg query " + 
                    '"'+ location + "\" /v " + key;
        	System.out.println(reg);
            Process process = Runtime.getRuntime().exec(reg);
 
            StreamReader reader = new StreamReader(process.getInputStream());
            reader.start();
            process.waitFor();
            reader.join();
 
            // Parse out the value
            String[] parsed = reader.getResult().split("\\s+");
            if (parsed.length > 1) {
                return parsed[parsed.length-1];
            }
        } catch (Exception e) {}
 
        return null;
    }
    
    /**
     * 
     * @param location path in the registry
     * @param key registry key
     * @return registry value or null if not found
     */
    public static final String writeRegistryInt(String location, String key,int value){
        try {
            // Run reg query, then read output with StreamReader (internal class)
        	String regStr = "reg add " + 
                    '"'+ location + "\" /v "+key+" /t REG_DWORD /f /d " + value;
        	//System.out.println(regStr);
            Process process = Runtime.getRuntime().exec(regStr);
 
        } catch (Exception e) {}
 
        return null;
    }
    
    public static final String writeRegistryString(String location, String key,String value){
        try {
            // Run reg query, then read output with StreamReader (internal class)
        	String regStr = "reg add " + 
                    '"'+ location + "\" /v "+key+" /t REG_SZ /f /d " + '"'+value+'"';
        	System.out.println(regStr);
            Process process = Runtime.getRuntime().exec(regStr);
 
        } catch (Exception e) {}
 
        return null;
    }
 
    static class StreamReader extends Thread {
        private InputStream is;
        private StringWriter sw= new StringWriter();
 
        public StreamReader(InputStream is) {
            this.is = is;
        }
 
        public void run() {
            try {
                int c;
                while ((c = is.read()) != -1)
                    sw.write(c);
            } catch (IOException e) { 
            }
        }
 
        public String getResult() {
            return sw.toString();
        }
    }
    public static void main(String[] args) {
 
        // Sample usage
        String value =  Registry.readRegistry("HKCU\\Software\\s2a\\Casacion", "Estado");
        System.out.println(value);
        
        Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",2);
        
        value =  Registry.readRegistry("HKCU\\Software\\s2a\\Casacion", "Estado");
        System.out.println(value);
        
        Registry.writeRegistryInt("HKCU\\Software\\s2a\\Casacion", "estado",1);
        
        value =  Registry.readRegistry("HKCU\\Software\\s2a\\Casacion", "Estado");
        System.out.println(value);
        
        Registry.writeRegistryString("HKCU\\Software\\s2a\\Casacion", "mensaje","todo OK");
        
        value =  Registry.readRegistry("HKCU\\Software\\s2a\\Casacion", "mensaje");
        System.out.println(value);
    }
}
