package s2a.test;

import java.util.concurrent.Callable;

public class DoStuff implements Callable {
    Integer in;
    
    public Integer call(){
    	long sum = 0;
    	for (long i=0;i<=100000000L;i++){
    		sum+=i;
    	}
      in = 5;
      return in;
    }
    
    public DoStuff(Object input){
       in = (Integer) input;
    }
}

