package net.pkhapps.roihu.scenario;

import net.pkhapps.roihu.base.security.Officer;

import java.time.Instant;

/** Which officer changed something, and when. */
public record Change(Officer by, Instant at) {
}
