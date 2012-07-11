package screenrecord.test;

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.io.File;

import screenrecord.RecordScreen;

public class ScreenRecordTest {

    public static void main(String... args) throws Exception {
        File temp = new File(System.getProperty("user.home")
                + "/Desktop");
        
        RecordScreen screen = new RecordScreen(2, 0.6f, temp, new Rectangle(
                Toolkit.getDefaultToolkit().getScreenSize()));
        screen.start();
        Thread.sleep(30000);
        screen.stop();

        screen.createVideo(new File(System.getProperty("user.home")
                + "/Desktop/movie.avi"));
    }

}
