package com.mcnoita.wand.eval;

import java.util.ArrayList;
import java.util.List;

/** Stable, pure debug tree projection for Draw/Call/Copy/Divide inspection. */
public record EvaluationTrace(List<Entry> entries) {
    public static final EvaluationTrace EMPTY = new EvaluationTrace(List.of());

    public EvaluationTrace {
        entries = List.copyOf(entries);
    }

    public String toJson() {
        StringBuilder json = new StringBuilder("{\"entries\":[");
        for (int index = 0; index < entries.size(); index++) {
            if (index > 0) {
                json.append(',');
            }
            Entry entry = entries.get(index);
            json.append('{')
                .append("\"sequence\":").append(entry.sequence()).append(',')
                .append("\"kind\":\"").append(escape(entry.kind().name())).append("\",")
                .append("\"source\":\"").append(escape(entry.sourceSpellId())).append("\",")
                .append("\"target\":\"").append(escape(entry.targetSpellId())).append("\",")
                .append("\"pile\":\"").append(escape(entry.pile())).append("\",")
                .append("\"recursionLevel\":").append(entry.recursionLevel()).append(',')
                .append("\"iteration\":").append(entry.divideIteration()).append(',')
                .append("\"drawAllowed\":").append(entry.drawAllowed()).append(',')
                .append("\"nodePath\":\"").append(escape(entry.nodePath())).append("\",")
                .append("\"actionBudgetUsed\":").append(entry.actionBudgetUsed()).append(',')
                .append("\"reason\":\"").append(escape(entry.reason())).append("\"}");
        }
        return json.append("]}").toString();
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\n", "\\n").replace("\r", "\\r");
    }

    public record Entry(
        int sequence,
        InvocationKind kind,
        String sourceSpellId,
        String targetSpellId,
        String pile,
        int recursionLevel,
        int divideIteration,
        boolean drawAllowed,
        String nodePath,
        int actionBudgetUsed,
        String reason
    ) {
        public Entry {
            sourceSpellId = sourceSpellId == null ? "" : sourceSpellId;
            targetSpellId = targetSpellId == null ? "" : targetSpellId;
            pile = pile == null ? "" : pile;
            nodePath = nodePath == null || nodePath.isBlank() ? "root" : nodePath;
            reason = reason == null ? "" : reason;
        }
    }

    /** Mutable collector remains package-private to the evaluator. */
    static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        void add(InvocationKind kind, String source, String target, String pile, int recursionLevel,
                 int divideIteration, boolean drawAllowed, String nodePath, int actionBudgetUsed, String reason) {
            entries.add(new Entry(entries.size(), kind, source, target, pile, recursionLevel, divideIteration,
                drawAllowed, nodePath, actionBudgetUsed, reason));
        }

        EvaluationTrace build() {
            return new EvaluationTrace(entries);
        }
    }
}
