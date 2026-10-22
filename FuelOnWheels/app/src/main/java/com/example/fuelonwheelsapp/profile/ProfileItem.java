package com.example.fuelonwheelsapp.profile;

public class ProfileItem {
    private int iconId;
    private String item;
    private String subtitle;

    public ProfileItem(int iconId, String item, String subtitle) {
        this.iconId = iconId;
        this.item = item;
        this.subtitle = subtitle;
    }

    public ProfileItem(int iconId, String item) {
        this(iconId, item, "");
    }

    public int getIconId() {
        return iconId;
    }

    public void setIconId(int iconId) {
        this.iconId = iconId;
    }

    public String getItem() {
        return item;
    }

    public void setItem(String item) {
        this.item = item;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }
}
