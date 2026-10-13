package com.fredplenter.network;

import net.minecraft.server.level.ServerPlayer;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ServerPacketRateLimiter {

    private static final int GLOBAL_INTERVAL_TICKS = 1;
    private static final Map<UUID, PlayerLimits> LIMITS = new HashMap<>();

    private ServerPacketRateLimiter() {
    }

    public static int tryAcquire(ServerPlayer player, Action action) {
        int now = player.tickCount;
        PlayerLimits limits = LIMITS.computeIfAbsent(player.getUUID(), ignored -> new PlayerLimits());
        int globalRemaining = remaining(now, limits.lastAnyPacket, GLOBAL_INTERVAL_TICKS);
        int actionRemaining = remaining(now, limits.lastByAction.get(action), action.intervalTicks);
        int remaining = Math.max(globalRemaining, actionRemaining);
        if (remaining > 0) {
            return remaining;
        }
        limits.lastAnyPacket = now;
        limits.lastByAction.put(action, now);
        return 0;
    }

    public static void clear(UUID playerId) {
        LIMITS.remove(playerId);
    }

    private static int remaining(int now, Integer previousTick, int interval) {
        if (previousTick == null) {
            return 0;
        }
        int elapsed = now - previousTick;
        return elapsed >= interval ? 0 : interval - Math.max(0, elapsed);
    }

    private static final class PlayerLimits {
        private Integer lastAnyPacket;
        private final EnumMap<Action, Integer> lastByAction = new EnumMap<>(Action.class);
    }

    public enum Action {
        REQUEST_PROGRESS(20),
        CODEX_RESEARCH(5),
        COMPLETE_RESEARCH(5),
        START_RESEARCH(5),
        CREATE_ASPECT(2),
        LEARN_NOTE(5),
        SCAN_ITEM(40);

        private final int intervalTicks;

        Action(int intervalTicks) {
            this.intervalTicks = intervalTicks;
        }

        public int intervalTicks() {
            return intervalTicks;
        }
    }
}
