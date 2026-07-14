package com.mcnoita.wand.eval;

/**
 * Explicit policy for invoking an already-selected target action. Normal
 * Draw owns mana, Hand insertion and automatic use consumption; Call/Copy do
 * none of those unless a specialized action requests it here.
 */
public record InvocationPolicy(
    boolean checkTargetMana,
    boolean checkTargetUses,
    boolean enterHand,
    boolean automaticUseConsumption,
    boolean allowTargetDraw,
    boolean restoreMana,
    boolean restoreCastDelay,
    boolean restoreRecharge
) {
    public static final InvocationPolicy CALL = new InvocationPolicy(false, false, false, false, true,
        false, false, false);
    public static final InvocationPolicy COPY_NO_DRAW = new InvocationPolicy(false, false, false, false, false,
        false, false, false);
    public static final InvocationPolicy FILTERED_COPY = new InvocationPolicy(false, false, false, false, false,
        true, true, true);

    public InvocationPolicy withTargetDraw(boolean allowDraw) {
        return new InvocationPolicy(checkTargetMana, checkTargetUses, enterHand, automaticUseConsumption, allowDraw,
            restoreMana, restoreCastDelay, restoreRecharge);
    }
}
