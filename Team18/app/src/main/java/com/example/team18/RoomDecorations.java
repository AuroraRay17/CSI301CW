package com.example.team18;

import android.content.SharedPreferences;

import java.util.Set;

final class RoomDecorations {
    private final SharedPreferences preferences;

    RoomDecorations(SharedPreferences preferences) {
        this.preferences = preferences;
    }

    PetItem equipped(PetItem.RoomSlot slot, Set<PetItem> owned) {
        String chosenKey = preferences.getString(preferenceKey(slot), null);
        if (chosenKey == null) {
            // Older versions showed an owned cushion automatically.
            return slot == PetItem.RoomSlot.CUSHION && owned.contains(PetItem.BLUE_CUSHION)
                    ? PetItem.BLUE_CUSHION : null;
        }
        for (PetItem item : owned) {
            if (item.roomSlot == slot && item.key.equals(chosenKey)) return item;
        }
        return null;
    }

    void equip(PetItem.RoomSlot slot, PetItem item) {
        preferences.edit().putString(preferenceKey(slot), item == null ? "" : item.key).apply();
    }

    private static String preferenceKey(PetItem.RoomSlot slot) {
        return "room_equipped_" + slot.key;
    }
}
