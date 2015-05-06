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
package re.ovo.timo.manager;

import org.apache.log4j.Logger;

import re.ovo.timo.config.ErrorCode;
import re.ovo.timo.manager.handler.ClearHandler;
import re.ovo.timo.manager.handler.ReloadHandler;
import re.ovo.timo.manager.handler.RollbackHandler;
import re.ovo.timo.manager.handler.SelectHandler;
import re.ovo.timo.manager.handler.ShowHandler;
import re.ovo.timo.manager.handler.StopHandler;
import re.ovo.timo.manager.handler.SwitchHandler;
import re.ovo.timo.manager.parser.ManagerParse;
import re.ovo.timo.manager.response.KillConnection;
import re.ovo.timo.manager.response.Offline;
import re.ovo.timo.manager.response.Online;
import re.ovo.timo.net.handler.FrontendQueryHandler;
import re.ovo.timo.net.mysql.OkPacket;

/**
 * @author xianmao.hexm
 */
public class ManagerQueryHandler implements FrontendQueryHandler {
    private static final Logger LOGGER = Logger.getLogger(ManagerQueryHandler.class);

    private final ManagerConnection source;

    public ManagerQueryHandler(ManagerConnection source) {
        this.source = source;
    }

    @Override
    public void query(String sql) {
        ManagerConnection c = this.source;
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(new StringBuilder().append(c).append(sql).toString());
        }
        int rs = ManagerParse.parse(sql);
        switch (rs & 0xff) {
            case ManagerParse.SELECT:
                SelectHandler.handle(sql, c, rs >>> 8);
                break;
            case ManagerParse.SET:
                c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
                break;
            case ManagerParse.SHOW:
                ShowHandler.handle(sql, c, rs >>> 8);
                break;
            case ManagerParse.SWITCH:
                SwitchHandler.handler(sql, c, rs >>> 8);
                break;
            case ManagerParse.KILL_CONN:
                KillConnection.response(sql, rs >>> 8, c);
                break;
            case ManagerParse.OFFLINE:
                Offline.execute(sql, c);
                break;
            case ManagerParse.ONLINE:
                Online.execute(sql, c);
                break;
            case ManagerParse.STOP:
                StopHandler.handle(sql, c, rs >>> 8);
                break;
            case ManagerParse.RELOAD:
                ReloadHandler.handle(sql, c, rs >>> 8);
                break;
            case ManagerParse.ROLLBACK:
                RollbackHandler.handle(sql, c, rs >>> 8);
                break;
            case ManagerParse.CLEAR:
                ClearHandler.handle(sql, c, rs >>> 8);
                break;
            default:
                c.writeErrMessage(ErrorCode.ER_YES, "Unsupported statement");
        }
    }

}
