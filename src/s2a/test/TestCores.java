package s2a.test;

public class TestCores {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		int processors = Runtime.getRuntime().availableProcessors();
		for(int i=0; i < processors; i++) {
		  System.out.println("core "+(i+1));
		}
	}

}
