package com.example.team18;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.database.sqlite.SQLiteException;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.core.view.ViewCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

public final class PetShopPanel extends LinearLayout {
    private static final int COLUMNS = 3;
    private static final int SLOTS_PER_CATEGORY = 5;

    private final EnumMap<PetItem, View> tiles = new EnumMap<>(PetItem.class);
    private final EnumMap<PetItem.RoomSlot, View> emptyTiles = new EnumMap<>(PetItem.RoomSlot.class);
    private Set<PetItem> owned = EnumSet.noneOf(PetItem.class);
    private FocusDatabase database;
    private RoomDecorations decorations;
    private RabbitView scene;
    private View homeChrome;
    private Runnable onPetChanged;
    private ScrollView scroll;
    private LinearLayout actionItems;
    private LinearLayout roomItems;
    private View actionTab;
    private View roomTab;
    private TextView balance;
    private PetItem selected;
    private boolean open;
    private int panelHeight;
    private float reveal;
    private ValueAnimator transition;
    private AlertDialog activeDialog;

    public PetShopPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    void bind(FocusDatabase database, RoomDecorations decorations, RabbitView scene, View homeChrome,
              Runnable onPetChanged, Runnable onClose) {
        this.database = database;
        this.decorations = decorations;
        this.scene = scene;
        this.homeChrome = homeChrome;
        this.onPetChanged = onPetChanged;
        scroll = findViewById(R.id.shopScroll);
        actionItems = findViewById(R.id.shopActionItems);
        roomItems = findViewById(R.id.shopRoomItems);
        actionTab = findViewById(R.id.shopActionTab);
        roomTab = findViewById(R.id.shopRoomTab);
        balance = findViewById(R.id.petShopBalance);

        ViewCompat.setAccessibilityPaneTitle(this, getResources().getString(R.string.pet_shop));
        findViewById(R.id.shopCloseButton).setOnClickListener(v -> onClose.run());
        actionTab.setOnClickListener(v -> scroll.smoothScrollTo(0, 0));
        roomTab.setOnClickListener(v -> scroll.smoothScrollTo(0, roomItems.getTop()));
        scroll.setOnScrollChangeListener((v, x, y, oldX, oldY) -> {
            boolean inRoom = y + scroll.getHeight() / 2 >= roomItems.getTop();
            actionTab.setSelected(!inRoom);
            roomTab.setSelected(inRoom);
        });
    }

    boolean isOpen() {
        return open;
    }

    void setOpen(boolean show) {
        if (show == open) return;
        open = show;
        if (show) {
            int sceneHeight = scene.getHeight();
            // Give the grid a usable viewport in short windows, such as landscape.
            panelHeight = Math.max(sceneHeight / 3, Math.min(dp(178), sceneHeight / 2));
            ViewGroup.LayoutParams layout = getLayoutParams();
            layout.height = panelHeight;
            setLayoutParams(layout);
            buildGrid();
            refreshProducts();
            scroll.scrollTo(0, 0);
            actionTab.setSelected(true);
            roomTab.setSelected(false);
            homeChrome.setVisibility(INVISIBLE);
            scene.setPivotX(scene.getWidth() / 2f);
            scene.setPivotY(0f);
            applyReveal(reveal);
            setVisibility(VISIBLE);
        } else {
            dismissDialog();
            select(null);
        }
        animateReveal(show ? 1f : 0f);
    }

    void setPreviewActive(boolean active) {
        if (open) scene.showShopPreview(active ? selected : null);
    }

    void release() {
        dismissDialog();
        cancelTransition();
        scene.showShopPreview(null);
    }

    private void animateReveal(float target) {
        cancelTransition();
        transition = ValueAnimator.ofFloat(reveal, target);
        transition.setDuration(240L);
        transition.setInterpolator(new DecelerateInterpolator());
        transition.addUpdateListener(animator -> applyReveal((float) animator.getAnimatedValue()));
        transition.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                transition = null;
                if (!open) {
                    setVisibility(GONE);
                    homeChrome.setVisibility(VISIBLE);
                }
            }
        });
        transition.start();
    }

    private void cancelTransition() {
        if (transition == null) return;
        transition.removeAllListeners();
        transition.cancel();
        transition = null;
    }

    private void applyReveal(float fraction) {
        reveal = fraction;
        float height = panelHeight * fraction;
        setTranslationY(panelHeight - height);
        float scale = 1f - height / scene.getHeight();
        scene.setScaleX(scale);
        scene.setScaleY(scale);
    }

    private void buildGrid() {
        tiles.clear();
        emptyTiles.clear();
        int gap = dp(8);
        int contentWidth = Math.min(scene.getWidth() - dp(28), dp(354));
        int size = (contentWidth - (COLUMNS - 1) * gap) / COLUMNS;
        View grid = findViewById(R.id.shopGrid);
        ViewGroup.LayoutParams layout = grid.getLayoutParams();
        layout.width = COLUMNS * size + (COLUMNS - 1) * gap;
        grid.setLayoutParams(layout);
        actionItems.removeAllViews();
        buildCategory(actionItems, null, size, gap);
        roomItems.removeAllViews();
        PetItem.RoomSlot[] slots = PetItem.RoomSlot.values();
        for (PetItem.RoomSlot slot : slots) {
            if (slots.length == 1) {
                buildCategory(roomItems, slot, size, gap);
                continue;
            }
            TextView title = new TextView(getContext());
            title.setText(slot.label);
            title.setTextColor(getContext().getColor(R.color.ink));
            LayoutParams spacing = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            spacing.topMargin = gap * 2;
            spacing.bottomMargin = gap;
            roomItems.addView(title, spacing);
            LinearLayout section = new LinearLayout(getContext());
            section.setOrientation(VERTICAL);
            roomItems.addView(section, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
            buildCategory(section, slot, size, gap);
        }
    }

    private void buildCategory(LinearLayout group, PetItem.RoomSlot roomSlot, int size, int gap) {
        int position = 0;
        if (roomSlot != null) {
            View empty = addTile(group, position++, size, gap);
            labelTile(empty, R.string.pet_shop_none, R.drawable.ic_shop_none);
            empty.setOnClickListener(v -> equip(roomSlot, null));
            emptyTiles.put(roomSlot, empty);
        }
        for (PetItem item : PetItem.values()) {
            if (item.roomSlot != roomSlot) continue;
            View tile = addTile(group, position++, size, gap);
            labelTile(tile, item.label, item.icon);
            tiles.put(item, tile);
            tile.setOnClickListener(v -> onProductTapped(item));
        }
        for (; position < SLOTS_PER_CATEGORY; position++) {
            View placeholder = addTile(group, position, size, gap);
            labelTile(placeholder, R.string.pet_shop_upcoming, R.drawable.ic_shop_upcoming);
            placeholder.setEnabled(false);
            ((TextView) placeholder.findViewById(R.id.shopTileName))
                    .setTextColor(getContext().getColor(R.color.muted_text));
        }
    }

    private View addTile(LinearLayout group, int position, int size, int gap) {
        LinearLayout row;
        if (position % COLUMNS == 0) {
            row = new LinearLayout(getContext());
            row.setOrientation(HORIZONTAL);
            LayoutParams rowLayout = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            if (position > 0) rowLayout.topMargin = gap;
            group.addView(row, rowLayout);
        } else {
            row = (LinearLayout) group.getChildAt(group.getChildCount() - 1);
        }
        View tile = LayoutInflater.from(getContext()).inflate(R.layout.tile_pet_shop, row, false);
        LayoutParams card = new LayoutParams(size, size);
        if (position % COLUMNS != 0) card.setMarginStart(gap);
        row.addView(tile, card);
        return tile;
    }

    private static void labelTile(View tile, int label, int iconResource) {
        TextView name = tile.findViewById(R.id.shopTileName);
        ImageView icon = tile.findViewById(R.id.shopTileIcon);
        name.setText(label);
        icon.setImageResource(iconResource);
    }

    private void onProductTapped(PetItem item) {
        if (!open) return;
        if (owned.contains(item)) {
            if (item.roomSlot != null) equip(item.roomSlot, item);
        } else if (selected == item) {
            confirmPurchase(item);
        } else {
            select(item);
        }
    }

    private void equip(PetItem.RoomSlot slot, PetItem item) {
        if (!open) return;
        decorations.equip(slot, item);
        onPetChanged.run();
        select(null);
    }

    private void refreshProducts() {
        balance.setText(getResources().getString(R.string.pet_shop_balance, database.coinBalance()));
        owned = database.ownedPetItems();
        renderChoices();
    }

    private void select(PetItem item) {
        updatePreview(item);
        renderChoices();
    }

    private void updatePreview(PetItem item) {
        selected = item;
        scene.showShopPreview(item);
    }

    private void renderChoices() {
        for (PetItem item : PetItem.values()) {
            View tile = tiles.get(item);
            boolean purchased = owned.contains(item);
            boolean roomItem = item.roomSlot != null;
            boolean equipped = roomItem && decorations.equipped(item.roomSlot, owned) == item;
            boolean previewing = item == selected && !purchased;
            tile.setEnabled(roomItem || !purchased);
            tile.setActivated(equipped);
            tile.setSelected(previewing);
            String value = null;
            int color = R.color.muted_text;
            if (equipped) {
                value = getResources().getString(R.string.pet_shop_equipped);
                color = R.color.ui_accent;
            } else if (purchased) {
                value = getResources().getString(R.string.pet_shop_owned);
            } else if (previewing) {
                value = getResources().getString(R.string.pet_shop_price, item.price);
                color = R.color.shop_mint_ink;
            }
            showTileValue(tile, value, color);
        }
        for (PetItem.RoomSlot slot : PetItem.RoomSlot.values()) {
            View empty = emptyTiles.get(slot);
            boolean equipped = decorations.equipped(slot, owned) == null;
            empty.setActivated(equipped);
            showTileValue(empty,
                    equipped ? getResources().getString(R.string.pet_shop_equipped) : null, R.color.ui_accent);
        }
    }

    private void showTileValue(View tile, String text, int color) {
        ImageView icon = tile.findViewById(R.id.shopTileIcon);
        TextView value = tile.findViewById(R.id.shopTileValue);
        icon.setVisibility(text == null ? VISIBLE : INVISIBLE);
        value.setVisibility(text == null ? GONE : VISIBLE);
        if (text != null) {
            value.setText(text);
            value.setTextColor(getContext().getColor(color));
        }
    }

    private void confirmPurchase(PetItem item) {
        showDialog(new MaterialAlertDialogBuilder(getContext())
                .setTitle(getResources().getString(R.string.pet_shop_buy_title, getResources().getString(item.label)))
                .setMessage(getResources().getString(R.string.pet_shop_buy_message, item.price))
                .setNegativeButton(R.string.focus_cancel, null)
                .setPositiveButton(R.string.pet_shop_buy_confirm, (dialog, which) -> purchase(item)));
    }

    private void purchase(PetItem item) {
        try {
            FocusDatabase.PurchaseResult result = database.purchasePetItem(item);
            if (result == FocusDatabase.PurchaseResult.INSUFFICIENT_FUNDS) {
                showMessage(R.string.pet_shop_insufficient);
                return;
            }
            if (item.roomSlot != null) decorations.equip(item.roomSlot, item);
            updatePreview(null);
            onPetChanged.run();
            refreshProducts();
        } catch (SQLiteException error) {
            Log.e("PetShopPanel", "Could not buy pet item", error);
            showMessage(R.string.pet_shop_purchase_failed);
        }
    }

    private void showMessage(int message) {
        showDialog(new MaterialAlertDialogBuilder(getContext())
                .setMessage(message)
                .setPositiveButton(R.string.focus_confirm, null));
    }

    private void showDialog(MaterialAlertDialogBuilder builder) {
        AlertDialog dialog = builder.create();
        activeDialog = dialog;
        dialog.setOnDismissListener(window -> {
            if (activeDialog == dialog) activeDialog = null;
        });
        dialog.show();
    }

    private void dismissDialog() {
        if (activeDialog != null) activeDialog.dismiss();
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
