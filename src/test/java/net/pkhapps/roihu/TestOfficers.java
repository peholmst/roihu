package net.pkhapps.roihu;

import net.pkhapps.roihu.base.security.Officer;

/** Training officers for tests, by the emails the development realm gives them. */
public final class TestOfficers {

    public static final Officer ANNA = new Officer("anna@example.invalid");
    public static final Officer BERTIL = new Officer("bertil@example.invalid");

    private TestOfficers() {
    }
}
