package com.mcnoita.wand.eval;

/** Result propagated through nested Divide calls without mutating pile policy. */
public record ActionInvocationResult(boolean invoked, int maxDivideIteration) {
    public static final ActionInvocationResult SKIPPED = new ActionInvocationResult(false, 0);

    public ActionInvocationResult {
        if (maxDivideIteration < 0) {
            throw new IllegalArgumentException("divide iteration must not be negative");
        }
    }

    public ActionInvocationResult merge(ActionInvocationResult other) {
        return new ActionInvocationResult(invoked || other.invoked,
            Math.max(maxDivideIteration, other.maxDivideIteration));
    }
}
