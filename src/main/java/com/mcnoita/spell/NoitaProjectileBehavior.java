package com.mcnoita.spell;

public enum NoitaProjectileBehavior {
    BOLT,
    ARROW,
    ORB,
    BOUNCY,
    DISC,
    EXPLOSIVE,
    FUSED_EXPLOSIVE,
    MINE,
    BLACK_HOLE,
    WHITE_HOLE,
    BEAM,
    DRILL,
    CHAINSAW,
    HEAL,
    TELEPORT,
    SWAPPER,
    SUMMON,
    MIST,
    RANDOM;

    public boolean usesBombEntity() {
        return this == FUSED_EXPLOSIVE || this == MINE;
    }

    public boolean bounces() {
        return this == BOUNCY || this == DISC;
    }

    public boolean explodesOnCollision() {
        return this == EXPLOSIVE;
    }

    public boolean digsOnCollision() {
        return this == DRILL || this == CHAINSAW;
    }

    public boolean isBeamLike() {
        return this == BEAM || this == DRILL || this == CHAINSAW;
    }
}
