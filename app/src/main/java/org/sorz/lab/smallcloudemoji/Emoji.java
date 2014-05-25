package org.sorz.lab.smallcloudemoji;

/**
 * Store a emoji and its note.
 */
public class Emoji {
    private final String entity;
    private final String note;
    private final boolean star;

    public Emoji(String entity, String note, boolean star) {
        this.entity = entity;
        this.note = note;
        this.star = star;
    }

    public String getNote() {
        return note;
    }

    public boolean hasStar() {
        return star;
    }

    @Override
    public String toString() {
        return this.entity;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Emoji)
            if (((Emoji) o).entity.equals(entity))
                return true;
        return false;
    }

    @Override
    public int hashCode() {
        return entity.hashCode();
    }
}
