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
package fm.liu.timo.manager.response;

import java.util.concurrent.locks.ReentrantLock;
import org.pmw.tinylog.Logger;
import fm.liu.timo.TimoServer;
import fm.liu.timo.config.ErrorCode;
import fm.liu.timo.manager.ManagerConnection;
import fm.liu.timo.mysql.packet.OkPacket;

/**
 * @author xianmao.hexm
 */
public final class RollbackConfig {

    public static void execute(ManagerConnection c) {
        final ReentrantLock lock = TimoServer.getInstance().getConfig().getLock();
        lock.lock();
        try {
            if (TimoServer.getInstance().getConfig().rollback()) {
                Logger.info("Rollback config success by manager");
                OkPacket ok = new OkPacket();
                ok.packetId = 1;
                ok.affectedRows = 1;
                ok.serverStatus = 2;
                ok.message = "Rollback config success".getBytes();
                ok.write(c);
            } else {
                c.writeErrMessage(ErrorCode.ER_YES, "Rollback config failure");
            }
        } finally {
            lock.unlock();
        }
    }

}
