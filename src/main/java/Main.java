import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.NonReadableChannelException;
import java.nio.channels.NonWritableChannelException;

public class Main {
    static Logger logger = LoggerFactory.getLogger("reproduction");
    static Logger dumper = LoggerFactory.getLogger("string-dumper");
    final String bigString = "10KB string: " + "xxxxxxxxxx".repeat(1000);

    public void run() {
        while (true) {
            dumper.debug(bigString);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] s) {

        final Runnable runnable = () -> {
            try {
                var fileInputStream = new FileInputStream("./test.log");
                var file = new File("./test.log");
                fileInputStream.readAllBytes();
                System.out.println("Size: " + file.length());
                final var fc = fileInputStream.getChannel();

                final var shared = true; // required to avoid throwing when opening a file as read-only
                try (FileLock lock = fc.tryLock(0,file.length(), shared)) {
                } catch (NonReadableChannelException| NonWritableChannelException e) {
                    logger.error("Something happened", e);
                    fc.close();
                    System.exit(1);
                }
            } catch (IOException e) {
                logger.error("Crashed on locking", e);
                e.printStackTrace();
            }
        };

        new Thread(runnable).start();
        new Main().run();

    }
}
