package levels;

/**
 * Represents a skill with its associated attribute and accumulated experience.
 */
public class SkillInfo {
    private String attribute;
    private int experience;

    public SkillInfo() {}

    public SkillInfo(String attribute, int experience) {
        this.attribute = attribute;
        this.experience = experience;
    }

    public String getAttribute() {
        return attribute;
    }

    public void setAttribute(String attribute) {
        this.attribute = attribute;
    }

    public int getExperience() {
        return experience;
    }

    public void setExperience(int experience) {
        this.experience = experience;
    }
}
