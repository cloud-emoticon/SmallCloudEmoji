package orz.sorz.lab.smallcloudemoji.daogenerator;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class AppDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "org.sorz.lab.smallcloudemoji.db");

        addEntities(schema);

        new DaoGenerator().generateAll(schema, "app/src-gen");
    }

    private static void addEntities(Schema schema) {
        Entity repository = schema.addEntity("Repository");
        repository.addIdProperty().index();
        repository.addStringProperty("url").notNull().unique().index();
        repository.addStringProperty("alias");
        repository.addBooleanProperty("hidden").index();
        repository.addIntProperty("order");
        repository.addDateProperty("lastUpdateDate");

        Entity category = schema.addEntity("Category");
        category.addIdProperty().index();
        category.addStringProperty("name");
        category.addBooleanProperty("hidden").index();
        category.addDateProperty("lastUpdateDate").index();
        Property repositoryId = category.addLongProperty("repositoryId").index().getProperty();
        category.addToOne(repository, repositoryId);

        ToMany repositoryToCategories = repository.addToMany(category, repositoryId);
        repositoryToCategories.setName("categories");

        Entity entry = schema.addEntity("Entry");
        entry.addIdProperty().index();
        entry.addStringProperty("emoticon").notNull();
        entry.addStringProperty("description");
        Property lastUsed = entry.addDateProperty("lastUsed").indexDesc(null, false).getProperty();
        entry.addDateProperty("lastUpdateDate").index();
        Property categoryId = entry.addLongProperty("categoryId").index().getProperty();
        entry.addToOne(category, categoryId);

        ToMany categoryToEntries = category.addToMany(entry, categoryId);
        categoryToEntries.setName("entries");
        categoryToEntries.orderDesc(lastUsed);
    }

}
