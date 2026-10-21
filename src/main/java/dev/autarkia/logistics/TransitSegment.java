package dev.autarkia.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Compact queue of items in transport on one segment (04 sections 0.1 and 0.2). Positions are integer path units
 * (1 block = 1,000 pu); server and client run the same movement code, only the server runs output steps.
 */
public final class TransitSegment {
    /** One stack in transport. */
    public static final class Entry {
        public final int id;
        public ItemStack stack;
        public int pos;
        public int prevPos;

        public Entry(int id, ItemStack stack, int pos) {
            this.id = id;
            this.stack = stack;
            this.pos = pos;
            this.prevPos = pos;
        }
    }

    private record Saved(int id, ItemStack stack, int pos) {
        static final Codec<Saved> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.fieldOf("id").forGetter(Saved::id), ItemStack.CODEC.fieldOf("stack").forGetter(Saved::stack),
            Codec.INT.fieldOf("pos").forGetter(Saved::pos)).apply(i, Saved::new));
    }

    public final List<Entry> entries = new ArrayList<>();
    public final int length;
    public boolean blocked;
    private int nextId = 1;

    public TransitSegment(int length) {
        this.length = length;
    }

    /** Return true when no entry lies within spacing of the position. */
    public boolean canInsertAt(int pos, int spacing) {
        for (Entry e : entries) {
            if (Math.abs(e.pos - pos) < spacing) {
                return false;
            }
        }
        return true;
    }

    /** Insert a stack at a position; returns the new entry. */
    public Entry insert(ItemStack stack, int pos) {
        Entry e = new Entry(nextId++, stack, pos);
        entries.add(e);
        entries.sort((a, b) -> Integer.compare(b.pos, a.pos));
        return e;
    }

    /** Insert an entry with a given id (client mirror of a server ADD). */
    public void insertWithId(int id, ItemStack stack, int pos) {
        entries.removeIf(e -> e.id == id);
        entries.add(new Entry(id, stack, pos));
        entries.sort((a, b) -> Integer.compare(b.pos, a.pos));
    }

    /** Remove an entry by id. */
    public void remove(int id) {
        entries.removeIf(e -> e.id == id);
    }

    /** Return the front entry when it reached the segment end, else null. */
    public Entry atEnd() {
        return !entries.isEmpty() && entries.getFirst().pos >= length ? entries.getFirst() : null;
    }

    /** Move all entries one tick: the front by speed up to the end, the rest by speed but keeping spacing. */
    public void move(int speed, int spacing) {
        move(speed, spacing, length);
    }

    /**
     * Move all entries one tick with the front entry held at or before frontLimit (at most the end): a belt passes the
     * position of the next belt's last entry here, so spacing also holds across the block boundary.
     */
    public void move(int speed, int spacing, int frontLimit) {
        int ahead = Integer.MAX_VALUE;
        for (int i = 0; i < entries.size(); i++) {
            Entry e = entries.get(i);
            e.prevPos = e.pos;
            int target = e.pos + speed;
            if (i == 0) {
                target = Math.min(target, Math.min(length, frontLimit));
            } else {
                target = Math.min(target, ahead - spacing);
            }
            e.pos = Math.max(e.pos, target);
            ahead = e.pos;
        }
    }

    /** Write the entries and state. */
    public void save(ValueOutput out) {
        List<Saved> list = new ArrayList<>();
        for (Entry e : entries) {
            list.add(new Saved(e.id, e.stack, e.pos));
        }
        out.store("entries", Saved.CODEC.listOf(), list);
        out.putBoolean("blocked", blocked);
        out.putInt("nextId", nextId);
    }

    /** Read the entries and state (replaces the current entries). */
    public void load(ValueInput in) {
        entries.clear();
        for (Saved s : in.read("entries", Saved.CODEC.listOf()).orElse(List.of())) {
            entries.add(new Entry(s.id(), s.stack(), s.pos()));
        }
        entries.sort((a, b) -> Integer.compare(b.pos, a.pos));
        blocked = in.getBooleanOr("blocked", false);
        nextId = Math.max(nextId, in.getIntOr("nextId", 1));
    }
}
