import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import org.junit.Test;

public class NIOFileTest {

    @Test
    public void testHelloFile() throws Exception {
        RandomAccessFile file = new RandomAccessFile("target/hello.txt", "rw");
        FileChannel fc = file.getChannel();
        System.out.println(fc.map(MapMode.READ_ONLY, 0, file.length()).isDirect());

        
//        fc.write(ByteBuffer.wrap("Hello, world!\n".getBytes()));
//
//        // Creates a gap in the file filled with unknown bytes.
//        fc.write(ByteBuffer.wrap("Hello, world2!\n".getBytes()), 50);
        file.close();
    }
    
//    @Test
    public void testReadOnly() throws Exception {
        System.out.println(ByteBuffer.allocate(10).hasArray());
        System.out.println(ByteBuffer.allocate(10).asReadOnlyBuffer().hasArray());
        System.out.println(ByteBuffer.allocateDirect(10).hasArray());
        System.out.println(ByteBuffer.wrap(new byte[10]).hasArray());
    }
    
}
