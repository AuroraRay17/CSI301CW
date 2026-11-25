package com.example.team18;

enum PetItem {
    GREET("greet", R.string.pet_greet, 5, 5 * 60, Category.ACTION, R.drawable.ic_shop_greet),
    PET("pet", R.string.pet_pat, 25, 25 * 60, Category.ACTION, R.drawable.ic_shop_pat),
    BLUE_CUSHION("blue_cushion", R.string.pet_blue_cushion, 15, 0,
            RoomSlot.CUSHION, R.drawable.ic_shop_cushion);

    enum Category { ACTION, ROOM }

    enum RoomSlot {
        CUSHION("cushion", R.string.pet_shop_cushions);

        final String key;
        final int label;

        RoomSlot(String key, int label) {
            this.key = key;
            this.label = label;
        }
    }

    final String key;
    final int label;
    final int price;
    final Category category;
    final RoomSlot roomSlot;
    final int icon;
    private final int legacyThresholdSeconds;

    PetItem(String key, int label, int price, int legacyThresholdSeconds, Category category, int icon) {
        this(key, label, price, legacyThresholdSeconds, category, null, icon);
    }

    PetItem(String key, int label, int price, int legacyThresholdSeconds, RoomSlot roomSlot, int icon) {
        this(key, label, price, legacyThresholdSeconds, Category.ROOM, roomSlot, icon);
    }

    PetItem(String key, int label, int price, int legacyThresholdSeconds,
            Category category, RoomSlot roomSlot, int icon) {
        this.key = key;
        this.label = label;
        this.price = price;
        this.legacyThresholdSeconds = legacyThresholdSeconds;
        this.category = category;
        this.roomSlot = roomSlot;
        this.icon = icon;
    }

    boolean unlockedBeforeUpgrade(long completedSeconds) {
        return legacyThresholdSeconds > 0 && completedSeconds >= legacyThresholdSeconds;
    }
}
