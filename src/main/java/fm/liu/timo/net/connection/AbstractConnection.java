/*
 * Copyright 1999-2012 Alibaba Group.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package fm.liu.timo.net.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import fm.liu.timo.mysql.packet.MySQLPacket;
import fm.liu.timo.net.NIOActor;
import fm.liu.timo.net.NIOHandler;
import fm.liu.timo.net.NIOProcessor;
import fm.liu.timo.util.TimeUtil;

/**
 * @author xianmao.hexm
 */
public abstract class AbstractConnection implements NIOConnection {
    protected final SocketChannel                     channel;
    protected NIOProcessor                            processor;
    protected NIOHandler                              handler;
    protected ReentrantLock                           closedLock = new ReentrantLock();
    protected volatile int                            readBufferOffset;
    protected volatile ByteBuffer                     readBuffer;
    protected volatile ByteBuffer                     writeBuffer;
    protected final ConcurrentLinkedQueue<ByteBuffer> writeQueue =
            new ConcurrentLinkedQueue<ByteBuffer>();
    private final NIOActor                            actor;
    protected AtomicBoolean                           closed     = new AtomicBoolean(false);
    protected volatile int                            state;
    protected Variables                               variables  = new Variables();

    protected long   id;
    protected String host;
    protected int    port;
    protected int    localPort;

    static public class State {
        public static final int connecting     = 0;
        public static final int authenticating = 1;
        public static final int idle           = 2;
        public static final int borrowed       = 3;
        public static final int running        = 4;

        public static String getStateDesc(int state) {
            switch (state) {
                case connecting:
                    return "connecting";
                case authenticating:
                    return "authenticating";
                case idle:
                    return "idle";
                case borrowed:
                    return "borrowed";
                case running:
                    return "running";
                default:
                    return "unknow";
            }
        }
    }

    public String getStateDesc() {
        return State.getStateDesc(state);
    }

    public boolean isRunning() {
        return this.state == State.running;
    }

    public AbstractConnection(SocketChannel channel, NIOProcessor processor) {
        this.channel = channel;
        this.processor = processor;
        this.setReadBuffer(processor.getBufferPool().allocate());
        this.actor = new NIOActor(this);
        long now = TimeUtil.currentTimeMillis();
        this.variables.setUpTime(now);
        this.variables.update();
    }

    public SocketChannel getChannel() {
        return channel;
    }

    public NIOProcessor getProcessor() {
        return processor;
    }

    public ByteBuffer getReadBuffer() {
        return readBuffer;
    }

    public ConcurrentLinkedQueue<ByteBuffer> getWriteQueue() {
        return writeQueue;
    }

    public void read() throws IOException {
        actor.read();
    }

    public void onRead(int got) {
        if (isClosed()) {
            return;
        }
        this.variables.update();
        ByteBuffer buffer = this.getReadBuffer();
        // 循环处理字节信息
        int offset = readBufferOffset, length = 0, position = buffer.position();
        for (;;) {
            length = MySQLPacket.getPacketLength(buffer, offset);
            if (length == -1) {
                if (!buffer.hasRemaining()) {
                    buffer = checkReadBuffer(buffer, offset, position);
                }
                break;
            }
            if (position >= offset + length) {
                buffer.position(offset);
                byte[] data = new byte[length];
                buffer.get(data, 0, length);
                handle(data);
                offset += length;
                if (position == offset) {
                    if (readBufferOffset != 0) {
                        readBufferOffset = 0;
                    }
                    buffer.clear();
                    break;
                } else {
                    readBufferOffset = offset;
                    buffer.position(position);
                    continue;
                }
            } else {
                if (!buffer.hasRemaining()) {
                    buffer = checkReadBuffer(buffer, offset, position);
                }
                break;
            }
        }
    }

    private ByteBuffer checkReadBuffer(ByteBuffer buffer, int offset, int position) {
        if (offset == 0) {
            if (buffer.capacity() >= MySQLPacket.MAX_PACKET_SIZE) {
                throw new IllegalArgumentException("packet size over the limit.");
            }
            int size = buffer.capacity() << 1;
            size = (size > MySQLPacket.MAX_PACKET_SIZE) ? MySQLPacket.MAX_PACKET_SIZE : size;
            ByteBuffer newBuffer = allocate(size);
            buffer.position(offset);
            newBuffer.put(buffer);
            setReadBuffer(newBuffer);
            recycle(buffer);
            return newBuffer;
        } else {
            buffer.position(offset);
            buffer.compact();
            readBufferOffset = 0;
            return buffer;
        }
    }

    public ByteBuffer checkWriteBuffer(ByteBuffer buffer, int capacity) {
        if (capacity > buffer.remaining()) {
            write(buffer);
            // 如果分配的buffer比要求的capacity还小，则按着capacity分配
            int size = this.processor.getBufferPool().getChunkSize();
            if (capacity > size) {
                size = capacity;
            }
            return allocate(capacity);
        } else {
            return buffer;
        }
    }

    public final void recycle(ByteBuffer buffer) {
        this.processor.getBufferPool().recycle(buffer);
    }

    public void check() {
        actor.check();
    }

    public final void write(byte[] data) {
        ByteBuffer buffer = allocate();
        buffer = writeToBuffer(data, buffer);
        write(buffer);
    }

    public final void write(ByteBuffer buffer) {
        writeQueue.offer(buffer);
        try {
            check();
        } catch (Exception e) {
            this.close(e.getMessage());
        }
    }

    public ByteBuffer allocate() {
        return processor.getBufferPool().allocate();
    }

    public ByteBuffer allocate(int size) {
        return processor.getBufferPool().allocate(size);
    }

    public ByteBuffer writeToBuffer(byte[] src, ByteBuffer buffer) {
        int offset = 0;
        int length = src.length;
        int remaining = buffer.remaining();
        while (length > 0) {
            if (remaining >= length) {
                buffer.put(src, offset, length);
                break;
            } else {
                buffer.put(src, offset, remaining);
                write(buffer);
                buffer = allocate();// 重新申请一个buffer
                offset += remaining;
                length -= remaining;
                remaining = buffer.remaining();
                continue;
            }
        }
        return buffer;
    }

    public boolean isClosed() {
        return closed.get();
    }

    public void cleanup() {
        if (channel != null) {
            try {
                channel.close();
            } catch (Throwable e) {}
        }
        // 清理回收readBuffer
        if (getReadBuffer() != null) {
            recycle(getReadBuffer());
            this.setReadBuffer(null);
            this.readBufferOffset = 0;
        }

        // 清理回收writeQueue中的writeBuffer
        if (this.getWriteBuffer() != null) {
            getWriteBuffer().clear();
            recycle(getWriteBuffer());
            setWriteBuffer(null);
        }
        ByteBuffer buffer = null;
        while ((buffer = writeQueue.poll()) != null) {
            buffer.clear();
            recycle(buffer);
        }
    }

    public void setReadBuffer(ByteBuffer readBuffer) {
        this.readBuffer = readBuffer;
    }

    public ByteBuffer getWriteBuffer() {
        return writeBuffer;
    }

    public void setWriteBuffer(ByteBuffer writeBuffer) {
        this.writeBuffer = writeBuffer;
    }

    public Variables getVariables() {
        return variables;
    }

    public long getID() {
        return id;
    }

    public void setID(long id) {
        this.id = id;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String ip) {
        this.host = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getLocalPort() {
        return localPort;
    }

    public NIOActor getActor() {
        return actor;
    }

    public void setState(int state) {
        this.state = state;
    }

    public void setHandler(NIOHandler handler) {
        this.handler = handler;
    }
}
