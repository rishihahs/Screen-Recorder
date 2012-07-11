package screenrecord;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Influenced by https://code.google.com/p/jpsxdec/source/browse/trunk/src/jpsxdec/util/aviwriter/AviWriter.java?r=28
public class AVIWriter {

    private RandomAccessFile file;

    private int width;
    private int height;
    private int framecount;
    private double framerate;

    private long movieOffset;

    private AVIEntryList entryList;

    public AVIWriter(File file, int width, int height, int framecount,
            double framerate) throws IOException {
        this.file = new RandomAccessFile(file, "rw");
        this.width = width;
        this.height = height;
        this.framecount = framecount;
        this.framerate = framerate;

        createHeader();
    }

    public void addImage(byte[] jpegImage) throws IOException {
        byte[] fcc = new byte[] { '0', '0', 'd', 'b' };
        int useLength = jpegImage.length;
        long position = file.getFilePointer();
        int extra = (useLength + (int) position) % 4;
        if (extra > 0)
            useLength = useLength + extra;

        entryList.addAVIEntry((int) position, useLength);

        file.write(fcc);
        file.write(intToBytes(useLength));
        file.write(jpegImage);

        if (extra > 0)
            for (int i = 0; i < extra; i++)
                file.write(0);
    }

    public void closeAVI() throws IOException {
        byte[] entriesBytes = entryList.getBytes();
        file.write(entriesBytes);
        long size = file.length();
        file.seek(4);
        file.write(intToBytes((int) size - 8));
        file.seek(movieOffset + 4);
        file.write(intToBytes((int) (size - 8 - movieOffset - entriesBytes.length)));
        file.close();
    }

    private void createHeader() throws IOException {
        file.write(new RIFF().getBytes());
        file.write(new AVIHeader().getBytes());
        file.write(new AVIList().getBytes());
        file.write(new AVIStreamHeader().getBytes());
        file.write(new AVIStreamFormat().getBytes());
        file.write(new AVIJunk().getBytes());

        movieOffset = file.getFilePointer();

        file.write(new AVIMovieList().getBytes());
        entryList = new AVIEntryList();
    }

    private class RIFF {

        // RIFF and FOURCC specification
        // http://msdn.microsoft.com/en-us/library/windows/desktop/dd318189(v=vs.85).aspx
        public byte[] fourcc = new byte[] { 'R', 'I', 'F', 'F' };
        public byte[] fourcc2 = new byte[] { 'A', 'V', 'I', ' ' };
        public byte[] fourcc3 = new byte[] { 'L', 'I', 'S', 'T' };
        public byte[] fourcc4 = new byte[] { 'h', 'd', 'r', 'l' };
        public int listSize = 200;
        public int fileSize = 0;

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fourcc);
            os.write(intToBytes(fileSize));
            os.write(fourcc2);
            os.write(fourcc3);
            os.write(intToBytes(listSize));
            os.write(fourcc4);
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIHeader {

        public byte[] fcc = new byte[] { 'a', 'v', 'i', 'h' };
        public int cb = 56;
        public int dwMicroSecPerFrame = 0;
        public int dwMaxBytesPerSec = 10000000;
        public int dwPaddingGranularity = 0;
        public int dwFlags = 65552;
        public int dwTotalFrames = 0;
        public int dwInitialFrames = 0;
        public int dwStreams = 1;
        public int dwSuggestedBufferSize = 0;
        public int dwWidth = 0;
        public int dwHeight = 0;
        public int[] dwReserved = new int[4];

        public AVIHeader() {
            dwMicroSecPerFrame = (int) ((1D / framerate) * 1000000D);
            dwWidth = width;
            dwHeight = height;
            dwTotalFrames = framecount;
        }

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(cb));
            os.write(intToBytes(dwMicroSecPerFrame));
            os.write(intToBytes(dwMaxBytesPerSec));
            os.write(intToBytes(dwPaddingGranularity));
            os.write(intToBytes(dwFlags));
            os.write(intToBytes(dwTotalFrames));
            os.write(intToBytes(dwInitialFrames));
            os.write(intToBytes(dwStreams));
            os.write(intToBytes(dwSuggestedBufferSize));
            os.write(intToBytes(dwWidth));
            os.write(intToBytes(dwHeight));
            os.write(intToBytes(dwReserved[0]));
            os.write(intToBytes(dwReserved[1]));
            os.write(intToBytes(dwReserved[2]));
            os.write(intToBytes(dwReserved[3]));
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIList {
        public byte[] fcc = new byte[] { 'L', 'I', 'S', 'T' };
        public int size = 124;
        public byte[] fcc2 = new byte[] { 's', 't', 'r', 'l' };

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(size));
            os.write(fcc2);
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIStreamHeader {

        public byte[] fcc = new byte[] { 's', 't', 'r', 'h' };
        public int cb = 64;
        public byte[] fccType = new byte[] { 'v', 'i', 'd', 's' };
        public byte[] fccHandler = new byte[] { 'M', 'J', 'P', 'G' };
        public int dwFlags = 0;
        public short wPriority = 0;
        public short wLanguage = 0;
        public int dwInitialFrames = 0;
        public int dwScale = 0;
        public int dwRate = 1000000;
        public int dwStart = 0;
        public int dwLength = 0;
        public int dwSuggestedBufferSize = 0;
        public int dwQuality = -1;
        public int dwSampleSize = 0;
        public int left = 0;
        public int top = 0;
        public int right = 0;
        public int bottom = 0;

        public AVIStreamHeader() {
            dwScale = (int) ((1D / framerate) * 1000000D);
            dwLength = framecount;
        }

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(cb));
            os.write(fccType);
            os.write(fccHandler);
            os.write(intToBytes(dwFlags));
            os.write(shortToBytes(wPriority));
            os.write(shortToBytes(wLanguage));
            os.write(intToBytes(dwInitialFrames));
            os.write(intToBytes(dwScale));
            os.write(intToBytes(dwRate));
            os.write(intToBytes(dwStart));
            os.write(intToBytes(dwLength));
            os.write(intToBytes(dwSuggestedBufferSize));
            os.write(intToBytes(dwQuality));
            os.write(intToBytes(dwSampleSize));
            os.write(intToBytes(left));
            os.write(intToBytes(top));
            os.write(intToBytes(right));
            os.write(intToBytes(bottom));
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIStreamFormat {

        public byte[] fcc = new byte[] { 's', 't', 'r', 'f' };
        public int cb = 40;
        public int biSize = 40;
        public int biWidth = 0;
        public int biHeight = 0;
        public short biPlanes = 1;
        public short biBitCount = 24;
        public byte[] biCompression = new byte[] { 'M', 'J', 'P', 'G' };
        public int biSizeImage = 0;
        public int biXPixelsPerMeter = 0;
        public int biYPixelsPerMeter = 0;
        public int biClrUsed = 0;
        public int biClrImportant = 0;

        public AVIStreamFormat() {
            biWidth = width;
            biHeight = height;
            biSizeImage = width * height;
        }

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(cb));
            os.write(intToBytes(biSize));
            os.write(intToBytes(biWidth));
            os.write(intToBytes(biHeight));
            os.write(shortToBytes(biPlanes));
            os.write(shortToBytes(biBitCount));
            os.write(biCompression);
            os.write(intToBytes(biSizeImage));
            os.write(intToBytes(biXPixelsPerMeter));
            os.write(intToBytes(biYPixelsPerMeter));
            os.write(intToBytes(biClrUsed));
            os.write(intToBytes(biClrImportant));
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIJunk {
        public byte[] fcc = new byte[] { 'J', 'U', 'N', 'K' };
        public int size = 1808;
        public byte[] data = new byte[size];

        public AVIJunk() {
            Arrays.fill(data, (byte) 0);
        }

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(size));
            os.write(data);
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIMovieList {

        public byte[] fcc = new byte[] { 'L', 'I', 'S', 'T' };
        public int listSize = 0;
        public byte[] fcc2 = new byte[] { 'm', 'o', 'v', 'i' };

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(listSize));
            os.write(fcc2);
            os.close();

            return os.toByteArray();
        }
    }

    private class AVIEntryList {
        public byte[] fcc = new byte[] { 'i', 'd', 'x', '1' };
        public int cb = 0;
        public List<AVIEntry> entries = new ArrayList<AVIEntry>();

        public void addAVIEntry(int dwOffset, int dwSize) {
            entries.add(new AVIEntry(dwOffset, dwSize));
        }

        public byte[] getBytes() throws IOException {
            cb = 16 * entries.size();

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(cb));
            for (AVIEntry entry : entries) {
                os.write(entry.getBytes());
            }

            os.close();

            return os.toByteArray();
        }
    }

    private class AVIEntry {
        public byte[] fcc = new byte[] { '0', '0', 'd', 'b' };
        public int dwFlags = 16;
        public int dwOffset = 0;
        public int dwSize = 0;

        public AVIEntry(int dwOffset, int dwSize) {
            this.dwOffset = dwOffset;
            this.dwSize = dwSize;
        }

        public byte[] getBytes() throws IOException {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            os.write(fcc);
            os.write(intToBytes(dwFlags));
            os.write(intToBytes(dwOffset));
            os.write(intToBytes(dwSize));
            os.close();

            return os.toByteArray();
        }
    }

    private byte[] intToBytes(int v) {
        int i = (v >>> 24) | (v << 24) | ((v << 8) & 0x00FF0000)
                | ((v >> 8) & 0x0000FF00);

        byte[] b = new byte[4];
        b[0] = (byte) (i >>> 24);
        b[1] = (byte) ((i >>> 16) & 0x000000FF);
        b[2] = (byte) ((i >>> 8) & 0x000000FF);
        b[3] = (byte) (i & 0x000000FF);

        return b;
    }

    private static byte[] shortToBytes(short v) {
        short i = (short) ((v >>> 8) | (v << 8));

        byte[] b = new byte[2];
        b[0] = (byte) (i >>> 8);
        b[1] = (byte) (i & 0x000000FF);

        return b;
    }

}
