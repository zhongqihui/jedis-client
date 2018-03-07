package com.zqh.jedis;

import java.util.UUID;

import redis.clients.jedis.Jedis;


public class JedisLock {

    private static final Lock NO_LOCK = new Lock(new UUID(0L, 0L), 0L);

    private static final int ONE_SECOND = 1000;

    public static final int DEFAULT_EXPIRY_TIME_MILLIS = Integer.getInteger("com.github.jedis.lock.expiry.millis", 60 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRE_TIMEOUT_MILLIS = Integer.getInteger("com.github.jedis.lock.acquiry.millis", 10 * ONE_SECOND);
    public static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = Integer.getInteger("com.github.jedis.lock.acquiry.resolution.millis", 100);

    private final Jedis jedis;

    private final String lockKeyPath;

    private final int lockExpiryInMillis;
    private final int acquiryTimeoutInMillis;
    private final UUID lockUUID;

    private Lock lock = null;

    protected static class Lock {
        private UUID uuid;
        private long expiryTime;

        protected Lock(UUID uuid, long expiryTimeInMillis) {
            this.uuid = uuid;
            this.expiryTime = expiryTimeInMillis;
        }

        protected static Lock fromString(String text) {
            try {
                String[] parts = text.split(":");
                UUID theUUID = UUID.fromString(parts[0]);
                long theTime = Long.parseLong(parts[1]);
                return new Lock(theUUID, theTime);
            } catch (Exception any) {
                return NO_LOCK;
            }
        }

        public UUID getUUID() {
            return uuid;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        @Override
        public String toString() {
            return uuid.toString() + ":" + expiryTime;
        }

        boolean isExpired() {
            return getExpiryTime() < System.currentTimeMillis();
        }

        boolean isExpiredOrMine(UUID otherUUID) {
            return this.isExpired() || this.getUUID().equals(otherUUID);
        }
    }

    public JedisLock(Jedis jedis, String lockKey) {
        this(jedis, lockKey, DEFAULT_ACQUIRE_TIMEOUT_MILLIS, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    public JedisLock(Jedis jedis, String lockKey, int acquireTimeoutMillis) {
        this(jedis, lockKey, acquireTimeoutMillis, DEFAULT_EXPIRY_TIME_MILLIS);
    }

    public JedisLock(Jedis jedis, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis) {
        this(jedis, lockKey, acquireTimeoutMillis, expiryTimeMillis, UUID.randomUUID());
    }

    public JedisLock(Jedis jedis, String lockKey, int acquireTimeoutMillis, int expiryTimeMillis, UUID uuid) {
        this.jedis = jedis;
        this.lockKeyPath = lockKey;
        this.acquiryTimeoutInMillis = acquireTimeoutMillis;
        this.lockExpiryInMillis = expiryTimeMillis + 1;
        this.lockUUID = uuid;
        ;
    }

    public UUID getLockUUID() {
        return lockUUID;
    }

    public String getLockKeyPath() {
        return lockKeyPath;
    }

    public synchronized boolean acquire() throws InterruptedException {
        return acquire(jedis);
    }

    protected synchronized boolean acquire(Jedis jedis) throws InterruptedException {
        int timeout = acquiryTimeoutInMillis;
        while (timeout >= 0) {

            final Lock newLock = asLock(System.currentTimeMillis() + lockExpiryInMillis);
            if (jedis.setnx(lockKeyPath, newLock.toString()) == 1) {
                this.lock = newLock;
                return true;
            }

            final String currentValueStr = jedis.get(lockKeyPath);
            final Lock currentLock = Lock.fromString(currentValueStr);
            if (currentLock.isExpiredOrMine(lockUUID)) {
                String oldValueStr = jedis.getSet(lockKeyPath, newLock.toString());
                if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
                    this.lock = newLock;
                    return true;
                }
            }

            timeout -= DEFAULT_ACQUIRY_RESOLUTION_MILLIS;
            Thread.sleep(DEFAULT_ACQUIRY_RESOLUTION_MILLIS);
        }

        return false;
    }

    public boolean renew() throws InterruptedException {
        final Lock lock = Lock.fromString(jedis.get(lockKeyPath));
        if (!lock.isExpiredOrMine(lockUUID)) {
            return false;
        }

        return acquire(jedis);
    }

    public synchronized void release() {
        release(jedis);
    }

    protected synchronized void release(Jedis jedis) {
        if (isLocked()) {
            jedis.del(lockKeyPath);
            this.lock = null;
        }
    }

    public synchronized boolean isLocked() {
        return this.lock != null;
    }

    public synchronized long getLockExpiryTimeInMillis() {
        return this.lock.getExpiryTime();
    }

    private Lock asLock(long expires) {
        return new Lock(lockUUID, expires);
    }


}
