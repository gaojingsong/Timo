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
/**
 * (created at 2011-11-22)
 */
package re.ovo.timo.server.response;

import java.nio.ByteBuffer;

import re.ovo.timo.TimoServer;
import re.ovo.timo.config.Fields;
import re.ovo.timo.mysql.PacketUtil;
import re.ovo.timo.net.mysql.EOFPacket;
import re.ovo.timo.net.mysql.ErrorPacket;
import re.ovo.timo.net.mysql.FieldPacket;
import re.ovo.timo.net.mysql.ResultSetHeaderPacket;
import re.ovo.timo.net.mysql.RowDataPacket;
import re.ovo.timo.server.ServerConnection;

/**
 * 加入了offline状态推送，用于心跳语句。
 * 
 * @author <a href="mailto:shuo.qius@alibaba-inc.com">QIU Shuo</a>
 * @author xianmao.hexm
 */
public class ShowTimoStatus {

    private static final int FIELD_COUNT = 1;
    private static final ResultSetHeaderPacket header = PacketUtil.getHeader(FIELD_COUNT);
    private static final FieldPacket[] fields = new FieldPacket[FIELD_COUNT];
    private static final EOFPacket eof = new EOFPacket();
    private static final RowDataPacket status = new RowDataPacket(FIELD_COUNT);
    private static final EOFPacket lastEof = new EOFPacket();
    private static final ErrorPacket error = PacketUtil.getShutdown();
    static {
        int i = 0;
        byte packetId = 0;
        header.packetId = ++packetId;
        fields[i] = PacketUtil.getField("STATUS", Fields.FIELD_TYPE_VAR_STRING);
        fields[i++].packetId = ++packetId;
        eof.packetId = ++packetId;
        status.add("ON".getBytes());
        status.packetId = ++packetId;
        lastEof.packetId = ++packetId;
    }

    public static void response(ServerConnection c) {
        if (TimoServer.getInstance().isOnline()) {
            ByteBuffer buffer = c.allocate();
            buffer = header.write(buffer, c);
            for (FieldPacket field : fields) {
                buffer = field.write(buffer, c);
            }
            buffer = eof.write(buffer, c);
            buffer = status.write(buffer, c);
            buffer = lastEof.write(buffer, c);
            c.write(buffer);
        } else {
            error.write(c);
        }
    }

}
