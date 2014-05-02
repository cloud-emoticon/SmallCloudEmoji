package org.sorz.lab.smallcloudemoji;

/**
 * Store a emoji and its note.
 */
public class Emoji {
    private final String entity;
    private final String note;

    public Emoji(String entity, String note) {
        this.entity = entity;
        this.note = note;
    }

    public Emoji(String entity) {
        this.entity = entity;
        this.note = "";
    }

    public String getNote() {
        return note;
    }

    @Override
    public String toString() {
        return this.entity;
    }

}
