package dev.autarkia.registry;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.autarkia.Autarkia;
import java.util.Optional;
import net.minecraft.advancements.triggers.SimpleCriterionTrigger;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.advancements.triggers.CriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The custom criterion triggers of 08b section 12: {@code autarkia:colony_event}, {@code autarkia:worker_event} and
 * {@code autarkia:equipment_event}, each matching a string field {@code event}.
 */
public final class EventTrigger extends SimpleCriterionTrigger<EventTrigger.Instance> {
    public static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, Autarkia.MOD_ID);
    public static final DeferredHolder<CriterionTrigger<?>, EventTrigger> COLONY = TRIGGERS.register("colony_event", EventTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, EventTrigger> WORKER = TRIGGERS.register("worker_event", EventTrigger::new);
    public static final DeferredHolder<CriterionTrigger<?>, EventTrigger> EQUIPMENT = TRIGGERS.register("equipment_event", EventTrigger::new);

    /** A criterion instance: the event string it waits for. */
    public record Instance(Optional<Holder<LootItemCondition>> player, String event) implements SimpleCriterionTrigger.SimpleInstance {
        public static final Codec<Instance> CODEC = RecordCodecBuilder.create(i -> i.group(
            LootItemCondition.CODEC.optionalFieldOf("player").forGetter(Instance::player),
            Codec.STRING.fieldOf("event").forGetter(Instance::event)).apply(i, Instance::new));
    }

    @Override
    public Codec<Instance> codec() {
        return Instance.CODEC;
    }

    /** Fire the event for a player; every criterion with the same event string completes. */
    public void fire(ServerPlayer player, String event) {
        trigger(player, inst -> inst.event().equals(event));
    }
}
