package s2a.test;

import java.util.concurrent.*;
import java.util.*;

public class CallableTask<String> implements Callable<String>{
 String name;

 CallableTask(String name){
  this.name = name;
 }

 public String call(){
  try{
   Thread.sleep(10000);
   System.out.println(name +" Finishes Execution");
  }
  catch(Exception e){
   System.out.println("Task Interrupted " + e);
  }
  return (String)(new Date() + " :: " + name);
 }
}