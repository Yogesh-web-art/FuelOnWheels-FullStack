package com.example.fuelonwheelsapp.placeholder;

import java.util.ArrayList;
import java.util.List;

public class PlaceholderContent {

    public final List<PlaceholderItem> ITEMS = new ArrayList<>();

    public void addPlaceholderItem(int position,String contentType,String contentDescription) {
            addItem(createPlaceholderItem(position,contentType,contentDescription));
    }
    // PlaceholderContent.addItem(Placeholder.createPlaceholderItem(id,content,description)
    private void addItem(PlaceholderItem item) {
        ITEMS.add(item);
    }

    private static PlaceholderItem createPlaceholderItem(int position,String contentType,String contentDescription) {
        return new PlaceholderItem(position, contentType, contentDescription);
    }

    /**
     * A placeholder item representing a piece of content.
     */
    public static class PlaceholderItem {
        public final int id;
        public final String contentDescription;
        public final String contentType;

        public PlaceholderItem(int id, String contentType, String contentDescription) {
            this.id = id;
            this.contentDescription = contentDescription;
            this.contentType = contentType;
        }
    }
}