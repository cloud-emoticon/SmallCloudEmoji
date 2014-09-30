package org.sorz.lab.smallcloudemoji.parsers;

import org.sorz.lab.smallcloudemoji.db.Category;
import org.sorz.lab.smallcloudemoji.db.Entry;

/**
 * Used by RepositoryXmlLoader.
 */
public interface RepositoryLoaderEventListener {
    /**
     * Be called at beginning of loading a category and it's entries.
     *
     * @param category The category that is loading currently.
     * @return Cancel whole loading process if return true.
     */
    public boolean onLoadingCategory(Category category);

    /**
     * Ce called after a entry is loaded.
     *
     * @param entry The entry that is loaded.
     * @return Cancel whole loading process if return true.
     */
    public boolean onEntryLoaded(Entry entry);
}
