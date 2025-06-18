package fi.natroutter.foxbot;

import fi.natroutter.foxlib.expiringmap.ExpirationPolicy;
import fi.natroutter.foxlib.expiringmap.ExpiringMap;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A generic cooldown handler for managing cooldowns associated with any object type.
 *
 * <p>This class allows setting, checking, and removing cooldowns for arbitrary keys (e.g., users, commands).
 *
 * @param <T> the type of object the cooldown is associated with (e.g., a user ID, name, or object)
 */
public class Cooldown<T> {

    /**
     * Represents a single cooldown entry with a start time, duration, and time unit.
     */
    public record CooldownEntry(long start, int time, TimeUnit timeUnit){};

    // Stores cooldowns mapped by their associated target
    private final ExpiringMap<T, CooldownEntry> cooldowns;

    // Default cooldown duration
    private final int defaultCooldown;

    // Default time unit for the cooldown
    private final TimeUnit defaultTimeUnit;

    /**
     * Constructs a Cooldown instance with a specified default cooldown time and unit.
     *
     */
    public Cooldown() {
        this.defaultCooldown = 5;
        this.defaultTimeUnit = TimeUnit.SECONDS;
        cooldowns = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(defaultCooldown, defaultTimeUnit)
                .build();
    }

    /**
     * Constructs a Cooldown instance with a specified default cooldown time and unit.
     *
     * @param defaultCooldown   the default cooldown duration
     * @param defaultTimeUnit   the time unit of the default cooldown
     */
    public Cooldown(int defaultCooldown, TimeUnit defaultTimeUnit) {
        this.defaultCooldown = defaultCooldown;
        this.defaultTimeUnit = defaultTimeUnit;
        cooldowns = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(defaultCooldown, defaultTimeUnit)
                .build();
    }

    /**
     * Checks whether the specified target is currently on cooldown.
     *
     * @param target the target to check
     * @return true if the target is still on cooldown; false if no cooldown exists or it has expired
     */
    public boolean hasCooldown(T target) {
        if (!cooldowns.containsKey(target)) return false;

        CooldownEntry entry = cooldowns.get(target);
        long cooldownDuration = entry.timeUnit().toMillis(entry.time());
        long elapsed = System.currentTimeMillis() - entry.start();

        if (elapsed < cooldownDuration) {
            return true;
        } else {
            cooldowns.remove(target);
            return false;
        }
    }

    /**
     * Retrieves the remaining cooldown time for the specified target, converted to the given {@link TimeUnit}.
     *
     * <p>If the target has no active cooldown, or if the cooldown has expired,
     * this method will return {@code 0} and remove the expired entry from the cache.
     *
     * @param target    the target for which to check the remaining cooldown
     * @param timeUnit  the {@link TimeUnit} to convert the remaining time into
     * @return the remaining cooldown time in the specified time unit, or {@code 0} if no cooldown is active
     */
    public long getCooldown(T target, TimeUnit timeUnit) {
        CooldownEntry entry = cooldowns.get(target);
        if (entry == null) return 0;

        long cooldownDuration = entry.timeUnit().toMillis(entry.time());
        long elapsed = System.currentTimeMillis() - entry.start();
        long remaining = cooldownDuration - elapsed;

        if (remaining > 0) {
            return timeUnit.convert(remaining, TimeUnit.MILLISECONDS);
        } else {
            // Cooldown expired — clean up and return 0
            cooldowns.remove(target);
            return 0;
        }
    }

    /**
     * Sets a cooldown for the given target using the default cooldown settings.
     *
     * @param target the target to apply the cooldown to
     */
    public void setCooldown(T target) {
        setCooldown(target,defaultCooldown, defaultTimeUnit);
    }

    /**
     * Sets a custom cooldown for the given target.
     *
     * @param target    the target to apply the cooldown to
     * @param time      the duration of the cooldown
     * @param timeUnit  the unit of time for the cooldown
     */
    public void setCooldown(T target, int time, TimeUnit timeUnit) {
        cooldowns.put(target, new CooldownEntry(System.currentTimeMillis(), time, timeUnit));
    }

    /**
     * Removes the cooldown for the specified target, if it exists.
     *
     * @param target the target whose cooldown should be removed
     */
    public void removeCooldown(T target) {
        cooldowns.remove(target);
    }

}
