package com.kethy.utils;

import org.springframework.util.StringUtils;

/**
 * 雪花算法
 */
@SuppressWarnings("all")
public class SnowFlow {

    // 自增序列
    private int sequence = 0;
    private long lastTimestamp = System.currentTimeMillis();

    private static final SnowFlow INSTANCE = new SnowFlow();
    private final IdGen idGen = new IdGen();

    private SnowFlow() {
    }

    public static SnowFlow getInstance() {
        return INSTANCE;
    }

    private synchronized long nextId() {
        // 获取当前毫秒数
        long timestamp = System.currentTimeMillis();

        // 如果服务器时间有问题(时钟后退) 报错。
        if (timestamp < lastTimestamp) {
            throw new RuntimeException(String.format(
                    "Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
        }
        // 如果上次生成时间和当前时间相同,在同一毫秒内
        if (lastTimestamp == timestamp) {
            sequence++;
            if (sequence > 999) {
                sequence = 1;
                // 自旋等待到下一毫秒
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            // 如果和上次生成时间不同, 重置sequence
            sequence = 1;
        }
        lastTimestamp = timestamp;

        return Long.valueOf(timestamp + leftPad(String.valueOf(sequence), 3, "0"));
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }


    /**
     * 获取全局唯一id，保证位数在16个长度内，避免返回到前端失去精度
     * 1毫秒至多生成999个id
     *
     * @return long
     */
    public long id() {
        return nextId();
    }

    /**
     * 获取16位全局唯一id
     *
     * @return String
     */
    public String id16() {
        return leftPad(Long.toHexString(idGen.nextId()), 16, "8");
    }

    static class IdGen {
        private long workerId;
        private long datacenterId;
        private long sequence = 0L;
        // Thu, 04 Nov 2010 01:42:54 GMT
        private long twepoch = 1288834974657L;
        // 节点ID长度
        private long workerIdBits = 5L;
        // 数据中心ID长度
        private long datacenterIdBits = 5L;
        // 最大支持机器节点数0~31，一共32个
        private long maxWorkerId = ~(-1L << workerIdBits);
        // 最大支持数据中心节点数0~31，一共32个
        private long maxDatacenterId = ~(-1L << datacenterIdBits);
        // 序列号12位
        private long sequenceBits = 12L;
        // 机器节点左移12位
        private long workerIdShift = sequenceBits;
        // 数据中心节点左移17位
        private long datacenterIdShift = sequenceBits + workerIdBits;
        // 时间毫秒数左移22位
        private long timestampLeftShift = sequenceBits + workerIdBits + datacenterIdBits;
        // 4095
        private long sequenceMask = ~(-1L << sequenceBits);
        private long lastTimestamp = -1L;

        public IdGen() {
            this(0L, 0L);
        }

        public IdGen(long workerId, long datacenterId) {
            if (workerId > maxWorkerId || workerId < 0) {
                throw new IllegalArgumentException(String.format("worker Id can't be greater than %d or less than 0", maxWorkerId));
            }
            if (datacenterId > maxDatacenterId || datacenterId < 0) {
                throw new IllegalArgumentException(String.format("datacenter Id can't be greater than %d or less than 0", maxDatacenterId));
            }
            this.workerId = workerId;
            this.datacenterId = datacenterId;
        }

        public synchronized long nextId() {
            // 获取当前毫秒数
            long timestamp = timeGen();
            // 如果服务器时间有问题(时钟后退) 报错。
            if (timestamp < lastTimestamp) {
                throw new RuntimeException(String.format("Clock moved backwards.  Refusing to generate id for %d milliseconds", lastTimestamp - timestamp));
            }
            // 如果上次生成时间和当前时间相同,在同一毫秒内
            if (lastTimestamp == timestamp) {
                // sequence自增，因为sequence只有12bit，所以和sequenceMask相与一下，去掉高位
                sequence = (sequence + 1) & sequenceMask;
                // 判断是否溢出,也就是每毫秒内超过4095，当为4096时，与sequenceMask相与，sequence就等于0
                if (sequence == 0) {
                    // 自旋等待到下一毫秒
                    timestamp = tilNextMillis(lastTimestamp);
                }
            } else {
                // 如果和上次生成时间不同,重置sequence，就是下一毫秒开始，sequence计数重新从0开始累加
                sequence = 0L;
            }
            lastTimestamp = timestamp;
            // time                                         datacenterId    workerId    sequence
            // 000000000000000000000000000000000000000000   00000           00000       000000000000
            return ((timestamp - twepoch) << timestampLeftShift) | (datacenterId << datacenterIdShift)
                    | (workerId << workerIdShift) | sequence;
        }

        private long tilNextMillis(long lastTimestamp) {
            long timestamp = timeGen();
            while (timestamp <= lastTimestamp) {
                timestamp = timeGen();
            }
            return timestamp;
        }

        private long timeGen() {
            return System.currentTimeMillis();
        }
    }


    private static String leftPad(String str, int size, String padStr) {
        if (str == null) {
            return null;
        } else {
            if (StringUtils.isEmpty(padStr)) {
                padStr = " ";
            }

            int padLen = padStr.length();
            int strLen = str.length();
            int pads = size - strLen;
            if (pads <= 0) {
                return str;
            } else if (padLen == 1 && pads <= 8192) {
                return leftPad(str, size, padStr.charAt(0));
            } else if (pads == padLen) {
                return padStr.concat(str);
            } else if (pads < padLen) {
                return padStr.substring(0, pads).concat(str);
            } else {
                char[] padding = new char[pads];
                char[] padChars = padStr.toCharArray();

                for (int i = 0; i < pads; ++i) {
                    padding[i] = padChars[i % padLen];
                }

                return (new String(padding)).concat(str);
            }
        }
    }

    private static String leftPad(String str, int size, char padChar) {
        if (str == null) {
            return null;
        } else {
            int pads = size - str.length();
            if (pads <= 0) {
                return str;
            } else {
                return pads > 8192 ? leftPad(str, size, String.valueOf(padChar)) : repeat(padChar, pads).concat(str);
            }
        }
    }

    private static String repeat(char ch, int repeat) {
        if (repeat <= 0) {
            return "";
        } else {
            char[] buf = new char[repeat];

            for (int i = repeat - 1; i >= 0; --i) {
                buf[i] = ch;
            }
            return new String(buf);
        }
    }
}
