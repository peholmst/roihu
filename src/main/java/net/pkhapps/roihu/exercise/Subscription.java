package net.pkhapps.roihu.exercise;

/** Following an exercise's changes, until cancelled. */
@FunctionalInterface
public interface Subscription {

    void cancel();
}
