package com.routinequest.app.data;

public class Title {
    public static final int COMMON = 1, RARE = 2, EPIC = 3, LEGENDARY = 4;

    public String id;
    public String name;
    public String condition;
    public int rarity;
    public boolean unlocked;

    public Title(String id, String name, String condition, int rarity) {
        this.id = id; this.name = name;
        this.condition = condition; this.rarity = rarity;
    }

    public String rarityLabel() {
        switch (rarity) {
            case RARE: return "희귀";
            case EPIC: return "영웅";
            case LEGENDARY: return "전설";
            default: return "일반";
        }
    }

    public int rarityColor() {
        switch (rarity) {
            case RARE: return 0xFF60A5FA;
            case EPIC: return 0xFFA78BFA;
            case LEGENDARY: return 0xFFF5C842;
            default: return 0xFF6B6890;
        }
    }
}
