package com.zqh.jedis;

import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * author: zqh
 * email：zqhfsf@gmail.com
 * date: 2018/3/7 15:02
 * description: Jedis 分布式锁机制，使用Lua脚本解决原子性的问题
 **/
public class JedisLuaLock {
    private static final Long SUCCESS_FLAG = 1L;
    private static final StringBuffer LOCK_SCRIPT = new StringBuffer();
    private static final StringBuffer UN_LOCK_SCRIPT = new StringBuffer();
    private static final StringBuffer RESET_LOCK_SCRIPT = new StringBuffer();
    private Jedis jedis;
    private String lockKey;
    private String lockValue;
    private long expireTime;

    public JedisLuaLock(Jedis jedis, String lockKey, String lockValue, long expireTime) {
        this.jedis = jedis;
        this.lockKey = lockKey;
        this.lockValue = lockValue;
        this.expireTime = expireTime;
    }

    static {
        // 加锁脚本
        LOCK_SCRIPT.append("if (redis.call('setnx', KEYS[1], ARGV[1]) == 1) then\n")
                .append("\tredis.call('expire', KEYS[1], tonumber(ARGV[2]))\n")
                .append("\treturn 1\n")
                .append("else\n")
                .append("\treturn 0\n")
                .append("end");

        // 解锁脚本
        UN_LOCK_SCRIPT.append("if (redis.call('get', KEYS[1]) == ARGV[1]) then\n")
                .append("\tredis.call('del', KEYS[1])\n")
                .append("\treturn 1\n")
                .append("else\n")
                .append("\treturn 0\n")
                .append("end");

        // 解锁脚本
        RESET_LOCK_SCRIPT.append("if (redis.call('get', KEYS[1]) == ARGV[1]) then\n")
                .append("\tredis.call('expire', KEYS[1], tonumber(ARGV[2]))\n")
                .append("\treturn 1\n")
                .append("else\n")
                .append("\treturn 0\n")
                .append("end");
    }

    // 加锁
    public boolean lock() {
        List<String> argList = new ArrayList<>();
        argList.add(lockValue);
        argList.add(String.valueOf(expireTime));

        Object eval = jedis.eval(LOCK_SCRIPT.toString(), Collections.singletonList(lockKey), argList);

        return SUCCESS_FLAG.equals(eval);
    }

    // 解锁
    public boolean unLock() {
        Object eval = jedis.eval(UN_LOCK_SCRIPT.toString(), Collections.singletonList(lockKey), Collections.singletonList(lockValue));
        return SUCCESS_FLAG.equals(eval);
    }

    // 重置锁失效时间
    public boolean reset() {
        List<String> argList = new ArrayList<>();
        argList.add(lockValue);
        argList.add(String.valueOf(expireTime));

        Object eval = jedis.eval(RESET_LOCK_SCRIPT.toString(), Collections.singletonList(lockKey), argList);

        return SUCCESS_FLAG.equals(eval);
    }

    public static void main(String[] args) {
        Jedis jedis = new Jedis("127.0.01", 6379);
        JedisLuaLock lock = new JedisLuaLock(jedis, "test", "192.168.62.169", 30);
        System.out.println(lock.lock());
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println(jedis.pttl("test"));
        System.out.println(lock.reset());
        System.out.println(jedis.pttl("test"));

        System.out.println(lock.unLock());
    }
}
