package com.herocraft.hungergames.kit;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class Kit {

    private final String id;
    private String displayName;
    private ItemStack icon;
    private final List<ItemStack> items = new ArrayList<>();

    public Kit(String id, String displayName, ItemStack icon) {
        this.id = id;
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public ItemStack getIcon() {
        return icon;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public List<ItemStack> getItems() {
        return items;
    }

    public void addItem(ItemStack item) {
        items.add(item);
    }

    public void clearItems() {
        items.clear();
    }
}
