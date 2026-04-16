package com.bupt.tarecruitment.common.skill;

/**
 * 验证技能目录中的扩展技能和相关性映射。
 */
public final class SkillCatalogRuleTest {
    private SkillCatalogRuleTest() {
    }

    public static void main(String[] args) {
        assertTrue(SkillCatalog.areRelatedSkills("Java", "Python"), "Java and Python should count as related programming skills.");
        assertTrue(SkillCatalog.areRelatedSkills("Big Data", "Spark"), "Big Data and Spark should count as related data skills.");
        assertTrue(SkillCatalog.areRelatedSkills("Cloud Computing", "Docker"), "Cloud Computing and Docker should count as related platform skills.");
        assertTrue(SkillCatalog.commonSkills().contains("Big Data"), "Big Data should be visible in the common skills catalogue.");
        assertTrue(SkillCatalog.commonSkills().contains("Cloud Computing"), "Cloud Computing should be visible in the common skills catalogue.");
        assertTrue(SkillCatalog.commonSkills().contains("Backend Development"), "Backend Development should be visible in the common skills catalogue.");

        System.out.println("SkillCatalog rule test passed.");
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}
