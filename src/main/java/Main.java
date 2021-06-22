import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {
    Logger logger = LoggerFactory.getLogger("reproduction");
    final String bigString = "10KB string: " + "xxxxxxxxxx".repeat(1000);

    public void run(){
        while(true) {
            logger.debug(bigString);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] s) {
        new Main().run();
    }
}
