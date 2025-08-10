package levels;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import javax.swing.SwingUtilities;

/**
 * LevelManager loads the attribute list and manages the user's levels/skills.
 * Data files:
 *  - data/levels/attributes.json (source of attribute names)
 *  - data/system/userLevels.json (persisted user progression)
 */
public class LevelManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path ATTRIBUTES_PATH = Path.of("data", "levels", "attributes.json");
    private static final Path USER_LEVELS_PATH = Path.of("data", "system", "userLevels.json");

    private static List<String> attributes = Collections.emptyList();
    private static UserLevels userLevels = new UserLevels();
    private static final List<Runnable> listeners = new ArrayList<>();

    public static void initialize() {
        attributes = loadAttributes();
        userLevels = loadOrCreateUserLevels(attributes);
        // Derive attribute levels from skills on startup (and persist if needed)
        recomputeAttributesFromSkills();
        save();
    }

    public static List<String> getAttributes() {
        return attributes;
    }

    public static Map<String, Integer> getAttributeXp() { return userLevels.getAttributesXp(); }

    public static Map<String, SkillInfo> getAvailableSkills() { return userLevels.getAvailableSkills(); }

    // --- Public helpers for UI/progress ---
    /** Returns the total accumulated XP for a given attribute (sum of all associated skill XP). */
    public static int getTotalXpForAttribute(String attribute) {
        if (attribute == null) return 0;
        String key = findAttributeKey(attribute);
        if (key == null) key = attribute; // fall back to provided
        int sum = 0;
        for (Map.Entry<String, SkillInfo> e : userLevels.getAvailableSkills().entrySet()) {
            SkillInfo info = e.getValue();
            if (info != null && info.getAttribute() != null && info.getAttribute().equalsIgnoreCase(key)) {
                sum += Math.max(0, info.getExperience());
            }
        }
        return sum;
    }

    /** Returns XP remaining to next level for an attribute; 0 if at max level (99). */
    public static int getXpToNextLevelForAttribute(String attribute) {
        int total = getTotalXpForAttribute(attribute);
        int lvl = xpToLevel(total);
        if (lvl >= 99) return 0;
        int[] th = getScaledThresholds();
        int nextThreshold = th[Math.min(99, Math.max(1, lvl + 1))];
        int rem = nextThreshold - total;
        return Math.max(0, rem);
    }

    /** Returns 0..1 progress within the current level for an attribute. */
    public static double getLevelProgressRatioForAttribute(String attribute) {
        int total = getTotalXpForAttribute(attribute);
        int lvl = xpToLevel(total);
        if (lvl <= 0) return 0.0;
        if (lvl >= 99) return 1.0;
        int[] th = getScaledThresholds();
        int cur = th[Math.max(1, lvl)];
        int next = th[Math.min(99, lvl + 1)];
        int den = Math.max(1, next - cur);
        int num = Math.max(0, total - cur);
        return Math.max(0.0, Math.min(1.0, (double) num / den));
    }

    // --- Update operations ---

    /** Adds a new skill if not present and persists to disk. */
    public static synchronized void addSkill(String skillName, String attributeAssociated) {
        if (skillName == null || skillName.isBlank()) return;
        Map<String, SkillInfo> skills = userLevels.getAvailableSkills();
        if (!skills.containsKey(skillName)) {
            // Default attribute casing: store as provided; caller should pass canonical names
            String attr = attributeAssociated != null ? attributeAssociated : inferDefaultAttribute();
            skills.put(skillName, new SkillInfo(attr, 0));
            save();
            System.out.println("[Levels] Added new skill: " + skillName + (attr != null ? " ("+attr+")" : ""));
        }
    }

    /**
     * Adds experience to a skill. Skill XP is not stored yet (out of current scope),
     * so we ensure the skill exists and log the intent.
     */
    public static synchronized void addExpOnSkill(String skillName, int amount) {
        if (skillName == null || skillName.isBlank()) return;
        if (amount <= 0) amount = 1;
        Map<String, SkillInfo> skills = userLevels.getAvailableSkills();
        SkillInfo info = skills.get(skillName);
        if (info == null) {
            // Create skill with default attribute if not exists
            String attr = inferDefaultAttribute();
            info = new SkillInfo(attr, 0);
            skills.put(skillName, info);
        }
        info.setExperience(Math.max(0, info.getExperience() + amount));
        // Update attribute XP level based on all associated skills (computed each save)
        recomputeAttributesFromSkills();
        save();
        System.out.println("[Levels] +" + amount + " XP -> skill '" + skillName + "' (total=" + info.getExperience() + ")");
    }

    // --- Internal helpers ---

    private static List<String> loadAttributes() {
        try (BufferedReader reader = Files.newBufferedReader(ATTRIBUTES_PATH, StandardCharsets.UTF_8)) {
            Type rootType = new TypeToken<Map<String, List<String>>>(){}.getType();
            Map<String, List<String>> root = GSON.fromJson(reader, rootType);
            List<String> list = root != null ? root.get("attributes") : null;
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            System.err.println("Failed to load attributes.json: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static UserLevels loadOrCreateUserLevels(List<String> attrs) {
        try {
            if (Files.exists(USER_LEVELS_PATH)) {
                String content = Files.readString(USER_LEVELS_PATH, StandardCharsets.UTF_8);
                UserLevels loaded = parseUserLevels(content, attrs);
                ensureAttributesPresent(loaded, attrs);
                return loaded;
            } else {
                // Create default structure with 0 XP and empty skills
                UserLevels defaults = createDefaultLevels(attrs);
                saveUserLevels(defaults);
                return defaults;
            }
        } catch (Exception e) {
            System.err.println("Failed to load/create userLevels.json: " + e.getMessage());
            // Fallback in-memory defaults
            UserLevels defaults = createDefaultLevels(attrs);
            return defaults;
        }
    }

    private static UserLevels parseUserLevels(String json, List<String> attrs) {
        try {
            JsonObject root = JsonParser.parseString(json).getAsJsonObject();
            UserLevels result = new UserLevels();

            // attributesXp
            Map<String, Integer> attrsXp = new LinkedHashMap<>();
            for (String a : attrs) attrsXp.put(a, 0);
            if (root.has("attributesXp") && root.get("attributesXp").isJsonObject()) {
                JsonObject obj = root.getAsJsonObject("attributesXp");
                for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                    try { attrsXp.put(e.getKey(), e.getValue().getAsInt()); } catch (Exception ignored) {}
                }
            }
            result.setAttributesXp(attrsXp);

            // availableSkills: support old schema (array of strings) and new schema (object map)
            Map<String, SkillInfo> skills = new LinkedHashMap<>();
            if (root.has("availableSkills")) {
                JsonElement as = root.get("availableSkills");
                if (as.isJsonArray()) {
                    JsonArray arr = as.getAsJsonArray();
                    for (JsonElement el : arr) {
                        if (el.isJsonPrimitive()) {
                            String name = el.getAsString();
                            if (name != null && !name.isBlank()) {
                                skills.put(name, new SkillInfo(inferDefaultAttribute(), 0));
                            }
                        } else if (el.isJsonObject()) {
                            JsonObject o = el.getAsJsonObject();
                            // try to detect a {name, attribute, experience} structure if any
                            String name = o.has("name") ? o.get("name").getAsString() : null;
                            String attr = o.has("attribute") ? o.get("attribute").getAsString() : inferDefaultAttribute();
                            int xp = o.has("experience") ? safeInt(o.get("experience")) : 0;
                            if (name != null && !name.isBlank()) {
                                skills.put(name, new SkillInfo(attr, xp));
                            }
                        }
                    }
                } else if (as.isJsonObject()) {
                    JsonObject obj = as.getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : obj.entrySet()) {
                        String name = e.getKey();
                        JsonElement val = e.getValue();
                        if (val != null && val.isJsonObject()) {
                            JsonObject so = val.getAsJsonObject();
                            String attr = so.has("attribute") ? so.get("attribute").getAsString() : inferDefaultAttribute();
                            int xp = so.has("experience") ? safeInt(so.get("experience")) : 0;
                            skills.put(name, new SkillInfo(attr, xp));
                        } else if (val != null && val.isJsonPrimitive()) {
                            // primitive value, treat as attribute name with 0 xp
                            String attr = val.getAsString();
                            skills.put(name, new SkillInfo(attr, 0));
                        }
                    }
                }
            }
            result.setAvailableSkills(skills);
            return result;
        } catch (Exception ex) {
            // Fallback to straight Gson; may throw again which caller handles
            try (java.io.StringReader sr = new java.io.StringReader(json)) {
                return GSON.fromJson(sr, UserLevels.class);
            }
        }
    }

    private static int safeInt(JsonElement el) {
        try { return el.getAsInt(); } catch (Exception ignored) { return 0; }
    }

    private static void ensureAttributesPresent(UserLevels levels, List<String> attrs) {
        if (levels.getAttributesXp() == null) {
            levels.setAttributesXp(new LinkedHashMap<>());
        }
        Map<String, Integer> map = levels.getAttributesXp();
        for (String a : attrs) {
            map.putIfAbsent(a, 0);
        }
        if (levels.getAvailableSkills() == null) {
            levels.setAvailableSkills(new LinkedHashMap<>());
        }
    }

    private static UserLevels createDefaultLevels(List<String> attrs) {
        UserLevels defaults = new UserLevels();
        Map<String, Integer> map = new LinkedHashMap<>();
        for (String a : attrs) {
            map.put(a, 0);
        }
        defaults.setAttributesXp(map);
        defaults.setAvailableSkills(new LinkedHashMap<>());
        return defaults;
    }

    public static void save() {
        saveUserLevels(userLevels);
    }

    private static void saveUserLevels(UserLevels levels) {
        try {
            File dir = USER_LEVELS_PATH.getParent().toFile();
            if (!dir.exists()) dir.mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(USER_LEVELS_PATH.toFile(), StandardCharsets.UTF_8))) {
                GSON.toJson(levels, writer);
            }
            notifyLevelsChanged();
        } catch (Exception e) {
            System.err.println("Failed to save userLevels.json: " + e.getMessage());
        }
    }

    // --- Derived attribute levels ---
    private static void recomputeAttributesFromSkills() {
        Map<String, Integer> attrTotals = new LinkedHashMap<>();
        for (String attr : attributes) {
            attrTotals.put(attr, 0);
        }
        for (Map.Entry<String, SkillInfo> e : userLevels.getAvailableSkills().entrySet()) {
            SkillInfo info = e.getValue();
            String attr = info.getAttribute();
            if (attr == null) continue;
            // Normalize to find the attribute key if needed (case-insensitive match)
            String key = findAttributeKey(attr);
            if (key != null) {
                attrTotals.put(key, attrTotals.getOrDefault(key, 0) + Math.max(0, info.getExperience()));
            }
        }
        // Level formula: OSRS-like curve capped at 99 using computed thresholds scaled to target cap.
        Map<String, Integer> target = userLevels.getAttributesXp();
        for (String attr : attributes) {
            int sumXp = Math.max(0, attrTotals.getOrDefault(attr, 0));
            target.put(attr, xpToLevel(sumXp));
        }
    }

    // Convert total XP to a level in [0,99], 3,250,000 -> 99, ~halfway XP -> ~92
    private static int xpToLevel(int xp) {
        if (xp <= 0) return 0;
        int[] thresholds = getScaledThresholds();
        if (xp >= thresholds[99]) return 99;
        // Binary search for highest level whose threshold <= xp
        int lo = 1, hi = 99, ans = 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            if (thresholds[mid] <= xp) {
                ans = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;
            }
        }
        // UX tweak: ensure any XP > 1 shows at least level 1
        if (xp > 1 && ans == 0) return 1;
        return Math.min(Math.max(ans, 0), 99);
    }

    // --- OSRS-like thresholds (computed, not hardcoded) ---
    private static final int MAX_LEVEL = 99;
    private static volatile int[] SCALED_THRESHOLDS; // lazily built

    private static int[] getScaledThresholds() {
        int[] local = SCALED_THRESHOLDS;
        if (local == null) {
            synchronized (LevelManager.class) {
                local = SCALED_THRESHOLDS;
                if (local == null) {
                    SCALED_THRESHOLDS = local = buildScaledOsrsThresholds();
                }
            }
        }
        return local;
    }

    private static int[] buildScaledOsrsThresholds() {
        // Build base OSRS thresholds using: diff(L) = floor((L-1) + 300*2^((L-1)/7));
        // total(L) = sum_{k=2..L} diff(k) / 4; total(99) ~ 13,034,431
        int[] base = new int[MAX_LEVEL + 1];
        base[1] = 0;
        int points = 0;
        for (int lvl = 2; lvl <= MAX_LEVEL; lvl++) {
            double inc = Math.floor((lvl - 1) + 300.0 * Math.pow(2.0, (lvl - 1) / 7.0));
            points += (int) inc;
            base[lvl] = points / 4;
        }
        int baseMax = base[MAX_LEVEL];
        // Target cap set by design: 3,250,000 for level 99
        double scale = 3_250_000.0 / Math.max(1, baseMax);
        int[] out = new int[MAX_LEVEL + 1];
        out[1] = 0;
        for (int l = 2; l <= MAX_LEVEL; l++) {
            int scaled = (int) Math.round(base[l] * scale);
            if (scaled <= out[l - 1]) scaled = out[l - 1] + 1; // ensure strictly increasing
            out[l] = scaled;
        }
        return out;
    }

    private static String inferDefaultAttribute() {
        // Fallback attribute when one is not provided; choose a sensible default
        if (!attributes.isEmpty()) return attributes.get(0);
        return "Intelligence"; // conservative default
    }

    private static String findAttributeKey(String name) {
        if (name == null) return null;
        for (String a : attributes) {
            if (a.equalsIgnoreCase(name)) return a;
        }
        return null;
    }

    // --- Listener support for UI updates ---
    public static void addLevelsListener(Runnable listener) {
        if (listener == null) return;
        synchronized (listeners) { listeners.add(listener); }
    }

    public static void removeLevelsListener(Runnable listener) {
        if (listener == null) return;
        synchronized (listeners) { listeners.remove(listener); }
    }

    private static void notifyLevelsChanged() {
        java.util.List<Runnable> copy;
        synchronized (listeners) { copy = new ArrayList<>(listeners); }
        if (copy.isEmpty()) return;
        Runnable dispatcher = () -> {
            for (Runnable r : copy) {
                try { r.run(); } catch (Throwable ignored) {}
            }
        };
        if (SwingUtilities.isEventDispatchThread()) dispatcher.run();
        else SwingUtilities.invokeLater(dispatcher);
    }
}
