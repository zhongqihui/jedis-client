package com.zqh.jedis;

import org.junit.Before;
import org.junit.Test;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * author: zqh
 * email：zqhfsf@gmail.com
 * date: 2018/3/6 9:53
 * description: Jedis 测试
 **/
public class JedisTest {
    private Jedis jedis;

    @Before
    public void connectionJedis() throws Exception {
        jedis = new Jedis("127.0.0.1", 6379);
        System.out.println(jedis.flushDB());
    }

    @Test
    public void testKey() throws Exception {
        jedis.set("a", "AAA");
        jedis.set("b", "BBB");

        //exists 判断key是否存在
        Boolean a = jedis.exists("a");
        Boolean b = jedis.exists("bb");
        System.out.println(a);
        System.out.println(b);

        String s = jedis.randomKey(); // 获取随机key
        System.out.println(s);

        jedis.expire("a", 1); // 设置key的过期时间
        System.out.println(jedis.get("a"));
        Thread.sleep(10L);
        System.out.println(jedis.pttl("a")); // 几豪秒之后过期时间
        Thread.sleep(1500L);
        System.out.println(jedis.get("a"));

        jedis.set("bb", "cc");
        System.out.println(jedis.renamenx("b", "bb")); // 如果oldkey不存在,报错;newkey存在，则rename失败
        //System.out.println(jedis.rename("b", "bb")); // 如果oldkey不存在,报错;newkey存在，则覆盖

        System.out.println(jedis.del("b")); // 删除

        Set<String> keys = jedis.keys("b*"); // 正则获取keys
        System.out.println(keys);
    }

    @Test
    public void testString() throws Exception {
        jedis.set("hello", "hello");
        System.out.println(jedis.get("hello"));

        jedis.append("hello", " world");
        System.out.println(jedis.get("hello"));

        // 设置过期时间 setex >> setexpire的缩写
        jedis.setex("hello2", 2, "你好");
        System.out.println(jedis.get("hello2"));
        Thread.sleep(2000L);
        System.out.println(jedis.get("hello2"));

        // 多个key，value一起插入
        jedis.mset("hello3", "你", "hello4", "好");
        System.out.println(jedis.keys("*"));

        // 删除多个key
        jedis.del("hello3", "hello4");
        System.out.println(jedis.keys("*"));
    }

    @Test
    public void testList() throws Exception {
        String key = "myList";
        jedis.del(key);

        for (int i = 65; i < 70; i++) {
            jedis.rpush(key, String.valueOf((char) i)); // 队列中从右边添加元素
        }

        for (int i = 101; i > 96; i--) {
            jedis.lpush(key, String.valueOf((char) i)); // 队列中从左边添加元素
        }

        System.out.println(jedis.llen(key)); // 获取队列的长度
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束
        System.out.println(jedis.lindex(key, 0)); // 获取某个下标的元素
        System.out.println(jedis.lset(key, 0, "aa")); // 设置某个下标的value，index不存在时，报错
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束
        jedis.rpush(key, "F", "G"); // 队列的右边插入F，G
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束
        jedis.lpush(key, "second", "first"); // 队列左边插入
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束

        jedis.lpop(key);// 左边出队列
        jedis.rpop(key);// 右边出队列
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束

        // count > 0: 从头往尾移除值为 value 的元素，count为移除的个数。
        // count < 0: 从尾往头移除值为 value 的元素，count为移除的个数。
        // count = 0: 移除所有值为 value 的元素。
        int count = 1;
        jedis.lpush(key, "a");
        jedis.rpush(key, "a");
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束
        jedis.lrem(key, count, "a");
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束

        jedis.ltrim(key, 0, 5);//删除区间以外的
        System.out.println(jedis.lrange(key, 0, -1)); // 所有的元素，下标从0开始，-1结束

    }

    @Test
    public void testSet() throws Exception {
        String key = "mySet";
        System.out.println(jedis.flushDB());

        jedis.sadd(key, "a", "b", "a"); // 往set中添加元素
        System.out.println(key + "的集合为：" + jedis.smembers(key));
        System.out.println(key + "的集合长度：" + jedis.scard(key));

        String key2 = "mySet2";
        jedis.sadd(key2, "a", "d");
        jedis.sinterstore("mySet3", key, key2); // key,key2的交集，并保存在mySet3中
        System.out.println("mySet2集合为：" + jedis.smembers(key2));
        System.out.println("mySet和mySet2的交集：" + jedis.smembers("mySet3"));

        jedis.sunionstore("mySet4", key, key2); // 求并集，并保存在mySet4中
        System.out.println("mySet和mySet2的并集：" + jedis.smembers("mySet4"));

        jedis.sdiffstore("mySet5", key, key2); // key集合中，key2集合没有的元素，并存储在一个关键的结果集
        System.out.println("keySet集合中，keySet2集合没有的元素：" + jedis.smembers("mySet5"));

        System.out.println("f 是否是keySet的成员：" + jedis.sismember(key, "f"));
        System.out.println("a 是否是keySet的成员：" + jedis.sismember(key, "a"));

        System.out.println("从keySet中随机获取一个元素：" + jedis.srandmember(key));

        jedis.smove(key, key2, "b");
        System.out.println("将keySet中的b移动到keySet2中,此时keySet：" + jedis.smembers(key) + ";keySet2:" + jedis.smembers(key2));

        System.out.println("删除并获取keySet2的一个成员" + jedis.spop(key2)); //由于set是无序的，因此，这里是随机删除
        System.out.println("keySet2删除了一个成员之后在获取：" + jedis.smembers(key2));

    }

    @Test
    public void testSortSet() throws Exception {
        String key = "mySortSet";
        System.out.println(jedis.flushDB());

        Map<String, Double> map = new HashMap<>();
        map.put("b", 1.1);
        map.put("c", 1.2);
        map.put("a", 1.0);

        jedis.zadd(key, map); // 按照double中的大小进行排序。
        jedis.zadd(key, 1.5, "f");
        jedis.zadd(key, 1.3, "g");

        System.out.println("初始化：" + jedis.zrange(key, 0, -1)); // 获取set
        System.out.println("大小：" + jedis.zcard(key)); // 获取set的长度

        jedis.zrem(key, "a");
        System.out.println("删除a之后的：" + jedis.zrange(key, 0, -1));

        System.out.println("逆序：" + jedis.zrevrange(key, 0, -1));
    }

    @Test
    public void testHash() throws Exception {
        String key = "myHash";
        Map<String, String> map = new HashMap<>();
        map.put("a", "0");
        map.put("b", "1");
        map.put("c", "2");

        jedis.hmset(key, map);
        jedis.hset(key, "d", "3");

        System.out.println(jedis.hkeys(key));
        System.out.println(jedis.hvals(key));
    }


    @Test
    public void test() {
        String dd = jedis.get("dd");
        System.out.println(dd);
    }



}
