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
package re.ovo.timo.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author xianmao.hexm
 */
public class ConcurrentHashMapMain {

    private final ConcurrentMap<String, String> cm;

    public ConcurrentHashMapMain() {
        cm = new ConcurrentHashMap<String, String>();
        cm.put("abcdefg", "abcdefghijk");
    }

    public void tGet() {
        for (int i = 0; i < 1000000; i++) {
            cm.get("abcdefg");
        }
    }

    public void tGetNone() {
        for (int i = 0; i < 1000000; i++) {
            cm.get("abcdefghijk");
        }
    }

    public void tEmpty() {
        for (int i = 0; i < 1000000; i++) {
            cm.isEmpty();
        }
    }

    public void tRemove() {
        for (int i = 0; i < 1000000; i++) {
            cm.remove("abcdefg");
        }
    }

}
