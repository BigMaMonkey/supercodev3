package coder3Gun;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Gun3 {

    private static float[] charLM = new float[27 * 27 * 27 * 27];

    public static void main(String[] args) throws IOException {

        byte[] bytes = Files.readAllBytes(Path.of(args[0]));

        ByteBuffer byteBuffer = ByteBuffer.wrap(bytes);
        byteBuffer.order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(charLM);
        byteBuffer.clear();

        System.out.println("completed");
    }
}
