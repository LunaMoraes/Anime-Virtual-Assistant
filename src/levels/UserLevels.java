package levels;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * POJO representing user progression for attributes and skills.
 */
public class UserLevels {
    private Map<String, Integer> attributesXp = new LinkedHashMap<>();
    // Map of skillName -> { attribute, experience }
    private Map<String, SkillInfo> availableSkills = new LinkedHashMap<>();

    public Map<String, Integer> getAttributesXp() {
        return attributesXp;
    }

    public void setAttributesXp(Map<String, Integer> attributesXp) {
        this.attributesXp = attributesXp;
    }

    public Map<String, SkillInfo> getAvailableSkills() {
        return availableSkills;
    }

    public void setAvailableSkills(Map<String, SkillInfo> availableSkills) {
        this.availableSkills = availableSkills;
    }
}
