package screenrecord;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class CreateVideo {

    public static void create(File images, int width, int height,
            int framecount, int fps, File output) throws IOException {
        AVIWriter writer = new AVIWriter(output, width, height, framecount, fps);

        // Unzip the images
        ZipFile zipFile = new ZipFile(images);
        Enumeration<?> enu = zipFile.entries();

        while (enu.hasMoreElements()) {
            ZipEntry zipEntry = (ZipEntry) enu.nextElement();
            long size = zipEntry.getSize();
            if (size > Integer.MAX_VALUE)
                throw new RuntimeException("File too large");

            byte[] bytes = new byte[(int) size];
            InputStream is = zipFile.getInputStream(zipEntry);
            int offset = 0;
            int numRead = 0;
            while (offset < bytes.length
                    && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
                offset += numRead;
            }

            if (offset < bytes.length) {
                throw new IOException("Could not completely read file ");
            }

            is.close();

            writer.addImage(bytes);
        }

        writer.closeAVI();

        // Delete temporary zip file with images
        images.delete();
    }

}