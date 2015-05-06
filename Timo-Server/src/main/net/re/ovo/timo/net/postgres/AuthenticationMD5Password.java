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
package re.ovo.timo.net.postgres;

/**
 * <pre>
 * AuthenticationMD5Password (B)
 * Byte1('R') Identifies the message as an authentication request. 
 * Int32(12) Length of message contents in bytes, including self. 
 * Int32(5) Specifies that an MD5-encrypted password is required. 
 * Byte4 The salt to use when encrypting the password.
 * </pre>
 * 
 * @author xianmao.hexm 2012-6-26
 */
public class AuthenticationMD5Password extends PostgresPacket {

}
