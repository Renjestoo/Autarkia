package dev.autarkia.colony;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.resources.Identifier;

/**
 * A colony template, data file {@code data/autarkia/colony_template/<id>.json} (05 section 3.1). The trigger is
 * {@code all(triggers) and (any is empty or one any-group is all true)}; each condition compares a signal with a value
 * or with factor times another signal and must hold for its hold ticks. A template builds a built-in structure plan or
 * runs a named handler.
 */
public record ColonyTemplate(int index, int tier, List<Condition> triggers, List<List<Condition>> any, Site site, Optional<Identifier> plan,
                             Optional<String> handler, int base, int maxOpen, int maxBuilt, int cooldown, Optional<String> sign, ReasonSpec reason) {

    /** One trigger condition. */
    public record Condition(String signal, String op, double value, Optional<String> valueSignal, double factor, int hold) {
        public static final Codec<Condition> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("signal").forGetter(Condition::signal),
            Codec.STRING.fieldOf("op").forGetter(Condition::op),
            Codec.DOUBLE.optionalFieldOf("value", 0.0).forGetter(Condition::value),
            Codec.STRING.optionalFieldOf("value_signal").forGetter(Condition::valueSignal),
            Codec.DOUBLE.optionalFieldOf("factor", 1.0).forGetter(Condition::factor),
            Codec.INT.optionalFieldOf("hold", 0).forGetter(Condition::hold)).apply(i, Condition::new));

        /** Return whether the comparison holds for x against the threshold t. */
        public boolean test(double x, double t) {
            return switch (op) {
                case ">=" -> x >= t;
                case "<=" -> x <= t;
                case ">" -> x > t;
                case "<" -> x < t;
                default -> throw new IllegalStateException("Unknown trigger op '" + op + "' in a colony template");
            };
        }
    }

    /** The site rule, search radius and score weights {@code [wR, wD, wL, wA]}. */
    public record Site(String rule, int radius, List<Double> weights) {
        public static final Codec<Site> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("rule").forGetter(Site::rule),
            Codec.INT.optionalFieldOf("radius", 24).forGetter(Site::radius),
            Codec.DOUBLE.listOf().optionalFieldOf("weights", List.of(0.4, 0.25, 0.25, 0.1)).forGetter(Site::weights)).apply(i, Site::new));
    }

    /** The card reason: language key, the signal for the argument and its scale (for example S03 x 100 = percent). */
    public record ReasonSpec(String key, String signal, double scale) {
        public static final Codec<ReasonSpec> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("key").forGetter(ReasonSpec::key),
            Codec.STRING.fieldOf("signal").forGetter(ReasonSpec::signal),
            Codec.DOUBLE.optionalFieldOf("scale", 1.0).forGetter(ReasonSpec::scale)).apply(i, ReasonSpec::new));
    }

    public static final Codec<ColonyTemplate> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.INT.fieldOf("index").forGetter(ColonyTemplate::index),
        Codec.INT.fieldOf("tier").forGetter(ColonyTemplate::tier),
        Condition.CODEC.listOf().optionalFieldOf("triggers", List.of()).forGetter(ColonyTemplate::triggers),
        Condition.CODEC.listOf().listOf().optionalFieldOf("any", List.of()).forGetter(ColonyTemplate::any),
        Site.CODEC.fieldOf("site").forGetter(ColonyTemplate::site),
        Identifier.CODEC.optionalFieldOf("plan").forGetter(ColonyTemplate::plan),
        Codec.STRING.optionalFieldOf("handler").forGetter(ColonyTemplate::handler),
        Codec.INT.fieldOf("base").forGetter(ColonyTemplate::base),
        Codec.INT.optionalFieldOf("max_open", 1).forGetter(ColonyTemplate::maxOpen),
        Codec.INT.optionalFieldOf("max_built", 0).forGetter(ColonyTemplate::maxBuilt),
        Codec.INT.optionalFieldOf("cooldown", 6000).forGetter(ColonyTemplate::cooldown),
        Codec.STRING.optionalFieldOf("sign").forGetter(ColonyTemplate::sign),
        ReasonSpec.CODEC.fieldOf("reason").forGetter(ColonyTemplate::reason)
    ).apply(i, ColonyTemplate::new));

    /** Return every condition in hold-counter order (triggers first, then each any-group). */
    public List<Condition> allConditions() {
        java.util.List<Condition> out = new java.util.ArrayList<>(triggers);
        any.forEach(out::addAll);
        return out;
    }
}
