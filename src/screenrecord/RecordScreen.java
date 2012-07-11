package screenrecord;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.swing.Timer;

public class RecordScreen implements ActionListener {

    private Timer timer;
    private Rectangle rectangle;
    private File storage;

    private Robot robot;

    private float quality;
    private int shots = 0;
    private int fps;

    private ZipOutputStream zos;
    private ImageWriter writer;
    private ImageWriteParam param;
    private ByteArrayOutputStream os;

    private ImageListener listener;

    public RecordScreen(int fps, File tempStorage, Rectangle screen)
            throws IOException, AWTException {
        this(fps, 1f, tempStorage, screen);
    }

    public RecordScreen(int fps, float quality, File tempStorage,
            Rectangle screen) throws IOException, AWTException {
        this.fps = fps;
        timer = new Timer(1000 / fps, this);
        rectangle = screen;

        robot = new Robot();

        storage = new File(tempStorage, "temp.zip");

        if (quality < 0f || quality > 1f)
            throw new IllegalArgumentException(
                    "Quality must be between 0 and 1 inclusive");
        this.quality = quality;

        setUpStreams();
    }

    public void start() {
        timer.start();
    }

    public void clear() throws IOException {
        if (timer.isRunning())
            throw new RuntimeException("Still recording");

        setUpStreams();
    }

    public boolean isRunning() {
        return timer.isRunning();
    }

    public void stop() throws IOException {
        timer.stop();
        zos.close();
        os.close();
    }

    public void setImageListener(ImageListener e) {
        listener = e;
    }

    public void createVideo(File output) throws IOException {
        if (timer.isRunning())
            throw new RuntimeException("Still recording");

        CreateVideo.create(storage, rectangle.width, rectangle.height, shots,
                fps, output);
    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        BufferedImage capture;

        if (listener != null)
            capture = listener.imageRequested();
        else
            capture = robot.createScreenCapture(rectangle);

        shots++;
        add(capture);
    }

    public void add(BufferedImage image) {
        ZipEntry zipEntry = new ZipEntry(String.valueOf(System
                .currentTimeMillis()));

        try {

            writer = (ImageWriter) ImageIO.getImageWritersByFormatName("jpeg")
                    .next();

            param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.setOutput(ImageIO.createImageOutputStream(os));
            writer.write(null, new IIOImage(image, null, null), param);
            writer.dispose();

            os.flush();

            byte[] array = os.toByteArray();
            zos.putNextEntry(zipEntry);
            zos.write(array, 0, array.length);
            zos.closeEntry();
            os.reset();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    private void setUpStreams() throws IOException {
        zos = new ZipOutputStream(new FileOutputStream(storage));
        zos.setLevel(ZipOutputStream.STORED);

        os = new ByteArrayOutputStream();
    }
}
