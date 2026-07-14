package com.mcnoita.wand.eval;

import java.util.List;

/** Immutable stack frame used to restore nested Call/Copy/Divide state. */
public record CallFrame(
    InvocationKind kind,
    String callerSpellId,
    String targetSpellId,
    int recursionLevel,
    int divideIteration,
    int drawSuppressionDepth,
    int shotScopeId,
    String payloadNodePath,
    List<String> tracePath
) {
    public CallFrame {
        callerSpellId = callerSpellId == null ? "" : callerSpellId;
        targetSpellId = targetSpellId == null ? "" : targetSpellId;
        payloadNodePath = payloadNodePath == null || payloadNodePath.isBlank() ? "root" : payloadNodePath;
        tracePath = List.copyOf(tracePath);
        if (recursionLevel < 0 || divideIteration < 0 || drawSuppressionDepth < 0 || shotScopeId < 0) {
            throw new IllegalArgumentException("call frame counters must not be negative");
        }
    }
}
