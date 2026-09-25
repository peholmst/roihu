package net.pkhapps.roihu.base.security;

/**
 * Access-control roles. Distinct from a {@code Position}, which is a function on the fire ground;
 * see {@code CONTEXT.md}.
 */
public final class Roles {

    /** An authenticated user this deployment admits as a training officer. */
    public static final String OFFICER = "OFFICER";

    private Roles() {
    }
}
