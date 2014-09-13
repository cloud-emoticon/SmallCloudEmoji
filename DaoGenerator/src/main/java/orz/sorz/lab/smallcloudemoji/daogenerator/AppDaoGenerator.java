package orz.sorz.lab.smallcloudemoji.daogenerator;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Property;
import de.greenrobot.daogenerator.Schema;
import de.greenrobot.daogenerator.ToMany;

public class AppDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "org.sorz.lab.smallcloudemoji.models");

        addRepository(schema);

        new DaoGenerator().generateAll(schema, "app/src-gen");

    }

    private static void addRepository(Schema schema) {
        Entity repository = schema.addEntity("Repository");
        Property repositoryId = repository.addIdProperty().getProperty();
        repository.addStringProperty("url").notNull().unique();
        repository.addStringProperty("alias");
        repository.addDateProperty("lastUpdateDate");

        Entity category = schema.addEntity("Category");
        Property categoryId = category.addIdProperty().getProperty();
        category.addStringProperty("name");
        category.addToOne(repository, repositoryId);

        ToMany repositoryToCategories = repository.addToMany(category, repositoryId);
        repositoryToCategories.setName("categories");

        Entity entry = schema.addEntity("Entry");
        entry.addIdProperty();
        entry.addStringProperty("emoticon").notNull();
        entry.addStringProperty("description");
        entry.addDateProperty("lastUsed");
        entry.addToOne(category, categoryId);

        ToMany categoryToEntries = category.addToMany(entry, categoryId);
        categoryToEntries.setName("entries");
    }

}
