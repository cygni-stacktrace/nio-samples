package se.cygni.stacktrace.nio;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.concurrent.Callable;

import se.cygni.stacktrace.nio.Timer.Event;

public class ReadBufferMain {
    
    public static Timer timer = new Timer();

    static abstract class ReadStrategy {
        private String name;

        public ReadStrategy(String name) {
            this.name = name;
        }

        public ByteBuffer read(String filename) throws IOException {
            Event e = timer.startEvent(name);
            try {
                return readBuffer(filename);
            } finally {
                timer.stopEvent(e);
            }
        }

        protected abstract ByteBuffer readBuffer(String filename)
                throws IOException;

        @Override
        public String toString() {
            return name;
        }
    }

    final static ReadStrategy NORMAL_READ = new ReadStrategy("NORMAL_READ") {
        public ByteBuffer readBuffer(String filename) throws IOException {
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(filename, "r");
                FileChannel fc = file.getChannel();
                ByteBuffer buf = ByteBuffer.allocate((int) fc.size());
                fc.read(buf);
                buf.flip();
                return buf.asReadOnlyBuffer();
            } finally {
                closeSilently(file);
            }
        }
    };

    final static ReadStrategy MAPPED_READ = new ReadStrategy("MAPPED_READ") {
        public ByteBuffer readBuffer(String filename) throws IOException {
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(filename, "r");
                FileChannel fc = file.getChannel();
                ByteBuffer buf = fc.map(MapMode.READ_ONLY, 0, fc.size());
                return buf;
            } finally {
                closeSilently(file);
            }
        }
    };

    final static ReadStrategy DIRECT_READ = new ReadStrategy("DIRECT_READ") {
        public ByteBuffer readBuffer(String filename) throws IOException {
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(filename, "r");
                FileChannel fc = file.getChannel();
                ByteBuffer buf = ByteBuffer.allocateDirect((int) fc.size());
                fc.read(buf);
                buf.flip();
                return buf;
            } finally {
                closeSilently(file);
            }
        }
    };

    final static ReadStrategy RFILE_READ = new ReadStrategy("RFILE_READ") {
        public ByteBuffer readBuffer(String filename) throws IOException {
            RandomAccessFile file = null;
            try {
                file = new RandomAccessFile(filename, "r");
                byte[] buf = new byte[(int) file.length()];
                int n = file.read(buf);
                if (n != file.length()) {
                    throw new RuntimeException("Didn't read all bytes.");
                }
                return ByteBuffer.wrap(buf, 0, n);
            } finally {
                closeSilently(file);
            }
        }
    };

    final static ReadStrategy STREAM_READ = new ReadStrategy("STREAM_READ") {
        public ByteBuffer readBuffer(String filename) throws IOException {
            File file = new File(filename);
            InputStream in = null;
            try {
                in = new FileInputStream(file);
                byte[] buf = new byte[(int) file.length()];
                int n = in.read(buf);
                if (n != file.length()) {
                    throw new RuntimeException("Didn't read all bytes.");
                }
                return ByteBuffer.wrap(buf, 0, n);
            } finally {
                closeSilently(in);
            }
        }
    };

    static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static <T> T retry(Callable<T> c, int retries) throws Exception {
        OutOfMemoryError error = null;
        int i = 0;
        try {
            for (; i < retries; i++) {
                try {
                    return c.call();
                } catch (OutOfMemoryError e) {
                    runGC();
                    error = e;
                }
            }
        } finally {
            System.out.println("Allocation retries: " + i);
        }
        throw error;
    }

    static void runGC() throws InterruptedException {
        System.gc();
        System.runFinalization();
        Thread.sleep(2000);
    }

    protected static Byte xor(ByteBuffer src) {
        Event e = timer.startEvent("XOR");
        try {
            if (src.hasArray()) {
                return xorArray(src.array());
            } else {
                return xorBuffer(src);
            }
        } finally {
            timer.stopEvent(e);
        }
    }

    protected static Byte xorBuffer(ByteBuffer src) {
        byte b = 0;
        src.clear();
        while (src.hasRemaining()) {
            b ^= src.get();
        }
        return b;
    }

    protected static Byte xorArray(byte[] src) {
        byte b = 0;
        for (int i = 0; i < src.length; i++) {
            b ^= src[i];
        }
        return b;
    }

    protected static Byte xor3(ByteBuffer src) {
        Event e = timer.startEvent("XOR3");
        try {
            byte[] bytes = new byte[65535];
            byte b = 0;
            int n = Math.min(src.remaining(), bytes.length);
            while (n > 0) {
                src.get(bytes, 0, n);
                for (int i = 0; i < n; i++) {
                    b ^= bytes[i];
                }
                n = Math.min(src.remaining(), bytes.length);
            }
            return b;
        } finally {
            timer.stopEvent(e);
        }
    }

    protected static Byte xor(String filename, ReadStrategy rs)
            throws IOException {
        Event e = timer.startEvent("XOR_TOTAL");
        Byte r = 0;
        try {
            ByteBuffer buf = null;
            try {
                buf = rs.read(filename);
                return r = xor(buf);
            } finally {
                if (buf != null && buf.isDirect()) {
                    ((sun.nio.ch.DirectBuffer) buf).cleaner().clean();
                }
            }
        } finally {
            timer.stopEvent(e);
        }
    }

//    void example() {
//        ByteBuffer buf = null;
//        try {
//            buf = allocateByteBuffer();
//            doWork(buf);
//        } finally {
//            if (buf != null && buf.isDirect()) {
//                ((sun.nio.ch.DirectBuffer) buf).cleaner().clean();
//            }
//        }
//    }

    public static void main(String[] args) throws Exception {
        String filename = args[0];
        ByteBuffer buf = DIRECT_READ.read(filename);
        int t = 0;
        for (int i = 0; i < 10; i++) {
            t ^= xor(buf);
        }
        System.out.println(t);
        timer.printSummaries();

//        xor(filename, MAPPED_READ);
//        xor(filename, DIRECT_READ);
//        xor(filename, RFILE_READ);
//        xor(filename, STREAM_READ);

    }

}
