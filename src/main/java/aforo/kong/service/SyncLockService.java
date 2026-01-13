package aforo.kong.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class SyncLockService {

    private static final Logger logger = LoggerFactory.getLogger(SyncLockService.class);
    private final ConcurrentHashMap<Long, ReentrantLock> orgLocks = new ConcurrentHashMap<>();

    public ReentrantLock getLockForOrg(Long orgId) {
        return orgLocks.computeIfAbsent(orgId, id -> new ReentrantLock());
    }

    public <T> T executeWithLock(Long orgId, java.util.function.Supplier<T> action) {
        ReentrantLock lock = getLockForOrg(orgId);
        lock.lock();
        try {
            logger.debug("Acquired sync lock for org: {}", orgId);
            return action.get();
        } finally {
            lock.unlock();
            logger.debug("Released sync lock for org: {}", orgId);
        }
    }

    public void executeWithLock(Long orgId, Runnable action) {
        ReentrantLock lock = getLockForOrg(orgId);
        lock.lock();
        try {
            logger.debug("Acquired sync lock for org: {}", orgId);
            action.run();
        } finally {
            lock.unlock();
            logger.debug("Released sync lock for org: {}", orgId);
        }
    }
}
