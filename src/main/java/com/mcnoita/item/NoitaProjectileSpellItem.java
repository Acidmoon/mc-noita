package com.mcnoita.item;

import com.mcnoita.spell.NoitaProjectileSpellSpec;

public class NoitaProjectileSpellItem extends NoitaSpellItem {
    private final NoitaProjectileSpellSpec projectileSpec;

    public NoitaProjectileSpellItem(NoitaProjectileSpellSpec projectileSpec, Settings settings) {
        super(projectileSpec.template(), settings);
        this.projectileSpec = projectileSpec;
    }

    public NoitaProjectileSpellSpec getProjectileSpec() {
        return projectileSpec;
    }
}
