package dev.autarkia.processing;

/** The machine recipe types of 03 section 5.3; each has its own registered RecipeType and serializer. */
public enum MachineRecipeKind {
    CRUSHING("crushing"),
    ALLOYING("alloying"),
    WASHING("washing"),
    PRESSING("pressing"),
    DRAWING("drawing"),
    SEPARATING("separating"),
    ELECTROLYZING("electrolyzing"),
    FABRICATING("fabricating");

    public final String id;

    MachineRecipeKind(String id) {
        this.id = id;
    }
}
