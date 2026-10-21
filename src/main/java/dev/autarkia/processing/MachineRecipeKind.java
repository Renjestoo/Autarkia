package dev.autarkia.processing;

/** The machine recipe types of 03 section 5.3; each has its own registered RecipeType and serializer. */
public enum MachineRecipeKind {
    CRUSHING("crushing"),
    ALLOYING("alloying");

    public final String id;

    MachineRecipeKind(String id) {
        this.id = id;
    }
}
