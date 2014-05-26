package org.sorz.lab.smallcloudemoji;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Hold the group name and a list of emojis.
 */
public class EmoticonGroup extends ArrayList<Emoticon> {
    private final String groupName;

    public EmoticonGroup(String groupName) {
        super();
        this.groupName = groupName;
    }

    public EmoticonGroup(String groupName, Collection<Emoticon> emoticons) {
        super(emoticons);
        this.groupName = groupName;
    }

    @Override
    public String toString() {
        return groupName;
    }
}
