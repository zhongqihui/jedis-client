package com.zqh.jedis;

import redis.clients.jedis.Jedis;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * author: zqh
 * email：zqhfsf@gmail.com
 * date: 2018/3/7 16:40
 * description: 主备切换线程，获取分布式锁的应用是主节点，主节点比备节点多执行了一些只能单个节点工作的线程。比如：移表线程，数据库数据加载到redis的线程，下发数据统计线程等等等
 **/
public class MasterSlaveSwitchThread extends Thread {
    private AtomicBoolean isRunnable = new AtomicBoolean(true);

    // jedis 从spring注入pool中拿
    private Jedis jedis = new Jedis("127.0.0.1", 6379);

    // 锁的key
    private String lockKey = "test";

    // 锁失效时间，必须在这时间内（单位秒）更新key，不然默认该节点（或线程）挂了。
    private long expireTime = 30L;

    // lockKey对应的value，一般为该节点独一无二的值，区分主节点，如ip，mac地址
    private String mac = "dddd";

    @Override
    public void run() {
        JedisLuaLock lock = new JedisLuaLock(jedis, lockKey, mac, expireTime);
        while (isRunnable.get()) {
            try {
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            String masterMac = jedis.get(lockKey);

            // 主节点未启动或者挂了,开始竞争主节点
            if(masterMac == null) {
                System.out.println("主节点未启动或者挂了,开始竞争主节点...");
                if(lock.lock()) {
                    System.out.println("当前节点：" + mac + "竞争成功");
                    // 竞争成功 开启其他线程 todo。
                }else {
                    System.out.println("当前节点：" + mac + "竞争失败");
                }

                continue;
            }

            // 当前节点是主节点，更新
            if(Objects.equals(masterMac,mac)) {
                if (lock.reset()) {
                    System.out.println("当前节点:" + mac + "是主节点，更新expire成功");
                } else {
                    System.out.println("当前节点:" + mac + "是主节点，更新expire失败");
                }

                continue;
            }

            //当前节点不是主节点
            System.out.println("主节点为：" + masterMac + ",当前节点：" + mac + "是从节点");

        }
    }

    public static void main(String[] args) {
        MasterSlaveSwitchThread thread = new MasterSlaveSwitchThread();
        thread.start();
    }



    public void doStop() {
        isRunnable.set(false);
    }
}
