package coder3Gun;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Gun3 {

    static float[] charLM = new float[27 * 27 * 27 * 27];
    static int LINEBUFSIZE = 1024;

    public static void main(String[] args) throws IOException {

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(charLM);
        byteBuffer.clear();

        bytes = null; // for gc

        byte[] buffer = new byte[LINEBUFSIZE * 2];
        short[] lineBuffer = new short[LINEBUFSIZE];

        int bytesRead;

//        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
//        int c = 0;
//        while ((c = reader.read()) >= 0) {
//            if (c == 0)
//            {
//                System.out.println(c);
//            }
//        }

        while ((bytesRead = System.in.read(buffer)) > 0) {
            byteBuffer = ByteBuffer.wrap(buffer);
            byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(lineBuffer);
            byteBuffer.clear();
        }

        System.out.println("completed");


    }
}
