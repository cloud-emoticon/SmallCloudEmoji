package org.sorz.lab.smallcloudemoji;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Hold the group name and a list of emojis.
 */
public class EmojiGroup extends ArrayList<Emoji> {
    private final String groupName;

    public EmojiGroup(String groupName) {
        super();
        this.groupName = groupName;
    }

    public EmojiGroup(String groupName, Collection<Emoji> emojis) {
        super(emojis);
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return groupName;
    }
}
