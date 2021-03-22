package s2a.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class TestSimpleThreadPool{
	 public static void main(String args[])throws Exception{
		 
		 
	 //variable to store the sum
	 AtomicInteger sum=new AtomicInteger();
	        
	  ExecutorService exec = new ThreadPoolExecutor(3, 3, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());

	  List<CallableTask<String>> l1 = new ArrayList<> ();
	  List<Future<String>> f1;

	  /* Create 5 Callable Tasks */
	  CallableTask<String> task1 = new CallableTask<> ("Task1");
	  CallableTask<String> task2 = new CallableTask<> ("Task2");
	  CallableTask<String> task3 = new CallableTask<> ("Task3");
	  CallableTask<String> task4 = new CallableTask<> ("Task4");
	  CallableTask<String> task5 = new CallableTask<> ("Task5");

	  /* Adding the tasks to the list of type Callable */
	  l1.add(task1);
	  l1.add(task2);
	  l1.add(task3);
	  l1.add(task4);
	  l1.add(task5);
	  
	  for (int i=0;i<=20;i++){
		  exec.submit(task1);
		  System.out.println(((ThreadPoolExecutor) exec).getActiveCount());
	  }

	  /*Submitting all the tasks in the list l1 to the Thread pool */
	 // f1 = exec.invokeAll(l1);
	  exec.shutdown();
	  //exec.awaitTermination(5, TimeUnit.SECONDS);

	  //System.out.println(((ThreadPoolExecutor) exec).getActiveCount());
	  /* Priniting the resultes returned by the tasks */
	 // for(Future obj : f1)
	  // System.out.println(obj.get());
	 }
}
