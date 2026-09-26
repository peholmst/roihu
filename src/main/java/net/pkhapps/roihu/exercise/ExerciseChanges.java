package net.pkhapps.roihu.exercise;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tells whoever follows an exercise that something about it changed: a position was taken, taken
 * over or released, or the exercise changed state or was deleted. It says only that, never what:
 * followers read the database again, which alone decides every change. In-process, so it assumes
 * a single application node.
 */
@Component
class ExerciseChanges {

    private static final Logger log = LoggerFactory.getLogger(ExerciseChanges.class);

    private final Map<JoinCode, Set<Runnable>> followers = new ConcurrentHashMap<>();

    Subscription subscribe(JoinCode joinCode, Runnable onChange) {
        // Adding inside compute, so that the last follower cancelling at the same moment cannot
        // drop the set this one is being added to.
        followers.compute(joinCode, (code, followersOfCode) -> {
            var added = followersOfCode == null ? ConcurrentHashMap.<Runnable>newKeySet() : followersOfCode;
            added.add(onChange);
            return added;
        });
        return () -> followers.computeIfPresent(joinCode, (code, followersOfCode) -> {
            followersOfCode.remove(onChange);
            return followersOfCode.isEmpty() ? null : followersOfCode;
        });
    }

    /**
     * Tells the followers of the exercise once the current transaction has committed, so that
     * what they read again includes the change.
     */
    void publish(JoinCode joinCode) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyFollowers(joinCode);
                }
            });
        } else {
            notifyFollowers(joinCode);
        }
    }

    /**
     * A follower that fails, such as a browser that went away as it was told, must neither keep
     * the change from the others nor fail the operation that made it, which has committed.
     */
    private void notifyFollowers(JoinCode joinCode) {
        followers.getOrDefault(joinCode, Set.of()).forEach(follower -> {
            try {
                follower.run();
            } catch (RuntimeException failure) {
                log.warn("A follower of an exercise failed to hear of a change", failure);
            }
        });
    }
}
