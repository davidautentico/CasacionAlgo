package s2a.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class TestExecutor2 {
	
 
    public static void main(String[] args) throws Exception {
        //ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ExecutorService exec = Executors.newFixedThreadPool(3);
        ArrayList<Callable> tasks = new ArrayList<Callable>();
        
        for(int i=0;i<=10000;i++){
           tasks.add(new DoStuff(i));
        }
        List<Future> results = exec.invokeAll((Collection)tasks);
        exec.shutdown();
        for(Future f : results) {
           System.out.println(f.get());
        }
    }
    
}
