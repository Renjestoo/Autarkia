package dev.autarkia.processing;

/** Machine state read by Jade and the Colony Core (03 section 4.6). */
public record MachineStatus(State state, int queue, float progress, int idleTicks, int energy, int capacity) {
    /** Machine run state. */
    public enum State {
        RUNNING(0x55FF55), IDLE_NO_INPUT(0xAAAAAA), BLOCKED_OUTPUT_FULL(0xFFAA00), NO_POWER(0xFF5555), NO_CONDITION(0xFF5555);

        public final int color;

        State(int color) {
            this.color = color;
        }

        /** Return the language key of the status text. */
        public String key() {
            return "status.autarkia." + name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** A block entity that reports a machine status. */
    public interface View {
        /** Return the current status. */
        MachineStatus machineStatus();
    }
}
