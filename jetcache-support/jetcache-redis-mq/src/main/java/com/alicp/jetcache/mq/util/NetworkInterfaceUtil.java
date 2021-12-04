/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.alicp.jetcache.mq.util;

import java.lang.management.ManagementFactory;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.validator.routines.InetAddressValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author chenzhh
 */
public class NetworkInterfaceUtil {
    private static final Logger log = LoggerFactory.getLogger(NetworkInterfaceUtil.class);
    /**
     * format: "pid@hostname"
      */
    private final static String HOST_NAME = ManagementFactory.getRuntimeMXBean().getName();

    public static void main(String[] args) {
       System.out.println(NetworkInterfaceUtil.getMachineId());
    }

    /**
     * 获取唯一id
     * @return
     */
    public  static  String getMachineId(){
          return getIPStr().replaceAll("\\.","")+getPid();
    }
    /**
     * 获取进程号
     * @return
     */
    public static int getPid() {
        try {
            return Integer.parseInt(HOST_NAME.substring(0, HOST_NAME.indexOf('@')));
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * 获取ip地址
     * @return
     */
    public  static  String  getIPStr(){
          byte[]  ip = getIP();
           if(ipCheck(ip)){
              return ipToIPv4Str(ip);
           }
           if(ipV6Check(ip)){
               return ipToIPv6Str(ip);
           }
           return null;
    }
    /**
     * 获取IP地址
     * @return
     */
    public static byte[] getIP() {
        try {
            Enumeration allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            InetAddress ip = null;
            byte[] internalIP = null;
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    ip = (InetAddress) addresses.nextElement();
                    if (ip != null && ip instanceof Inet4Address) {
                        byte[] ipByte = ip.getAddress();
                        if (ipByte.length == 4) {
                            if (ipCheck(ipByte)) {
                                if (!isInternalIP(ipByte)) {
                                    return ipByte;
                                } else if (internalIP == null) {
                                    internalIP = ipByte;
                                }
                            }
                        }
                    } else if (ip != null && ip instanceof Inet6Address) {
                        byte[] ipByte = ip.getAddress();
                        if (ipByte.length == 16) {
                            if (ipV6Check(ipByte)) {
                                if (!isInternalV6IP(ip)) {
                                    return ipByte;
                                }
                            }
                        }
                    }
                }
            }
            if (internalIP != null) {
                return internalIP;
            } else {
                throw new RuntimeException("Can not get local ip");
            }
        } catch (Exception e) {
            throw new RuntimeException("Can not get local ip", e);
        }
    }

    private static boolean isInternalIP(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        }

        //10.0.0.0~10.255.255.255
        //172.16.0.0~172.31.255.255
        //192.168.0.0~192.168.255.255
        if (ip[0] == (byte) 10) {

            return true;
        } else if (ip[0] == (byte) 172) {
            if (ip[1] >= (byte) 16 && ip[1] <= (byte) 31) {
                return true;
            }
        } else if (ip[0] == (byte) 192) {
            if (ip[1] == (byte) 168) {
                return true;
            }
        }
        return false;
    }

    private static boolean isInternalV6IP(InetAddress inetAddr) {
        if (inetAddr.isAnyLocalAddress() // Wild card ipv6
            || inetAddr.isLinkLocalAddress() // Single broadcast ipv6 address: fe80:xx:xx...
            || inetAddr.isLoopbackAddress() //Loopback ipv6 address
            || inetAddr.isSiteLocalAddress()) { // Site local ipv6 address: fec0:xx:xx...
            return true;
        }
        return false;
    }

    private static boolean ipCheck(byte[] ip) {
        if (ip.length != 4) {
            throw new RuntimeException("illegal ipv4 bytes");
        }

        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet4Address(ipToIPv4Str(ip));
    }

    private static boolean ipV6Check(byte[] ip) {
        if (ip.length != 16) {
            throw new RuntimeException("illegal ipv6 bytes");
        }

        InetAddressValidator validator = InetAddressValidator.getInstance();
        return validator.isValidInet6Address(ipToIPv6Str(ip));
    }

    public static String ipToIPv4Str(byte[] ip) {
        if (ip.length != 4) {
            return null;
        }
        return new StringBuilder().append(ip[0] & 0xFF).append(".").append(
            ip[1] & 0xFF).append(".").append(ip[2] & 0xFF)
            .append(".").append(ip[3] & 0xFF).toString();
    }

    private static String ipToIPv6Str(byte[] ip) {
        if (ip.length != 16) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ip.length; i++) {
            String hex = Integer.toHexString(ip[i] & 0xFF);
            if (hex.length() < 2) {
                sb.append(0);
            }
            sb.append(hex);
            if (i % 2 == 1 && i < ip.length - 1) {
                sb.append(":");
            }
        }
        return sb.toString();
    }


}
