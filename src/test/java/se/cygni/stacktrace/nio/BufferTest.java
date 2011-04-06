package se.cygni.stacktrace.nio;


import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.junit.Test;

public class BufferTest {
    
    @Test
    public void testByteBuffer() throws Exception {
        ByteBuffer buf = ByteBuffer.allocate(10);
        assertEquals(0, buf.position());
        assertEquals(10, buf.limit());
        
        buf.put((byte) 64);
        buf.put((byte) 65);
        buf.put((byte) 66);
        buf.putChar('\u4344');
        buf.put((byte) 69);
        assertEquals(6, buf.position());
        assertEquals(10, buf.limit());
        
        buf.flip();
        assertEquals(0, buf.position());
        assertEquals(6, buf.limit());

        assertEquals(64, buf.get());
        assertEquals(65, buf.get());
        assertEquals(66, buf.get());
        assertEquals(67, buf.get());
        assertEquals(68, buf.get());
        assertEquals(5, buf.position());
        assertEquals(1, buf.remaining());       
    }
    
    @Test
    public void testByteOrder() throws Exception {
        byte[] bytes = new byte[4];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());
        
        buf.putChar('\u0102');
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putChar('\u0304');
        buf.flip();
        assertArrayEquals(new byte[] {(byte) 1, (byte) 2, (byte) 4, (byte) 3}, bytes);
    }
    
    @Test
    public void testDirectBuffer() throws Exception {
        byte[] bytes = new byte[4];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        assertEquals(ByteOrder.BIG_ENDIAN, buf.order());
        
        buf.putChar('\u0102');
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putChar('\u0304');
        buf.flip();
        assertArrayEquals(new byte[] {(byte) 1, (byte) 2, (byte) 4, (byte) 3}, bytes);
    }
}
