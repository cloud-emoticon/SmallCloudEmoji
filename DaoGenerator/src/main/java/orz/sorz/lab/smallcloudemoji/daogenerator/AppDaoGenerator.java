package orz.sorz.lab.smallcloudemoji.daogenerator;

import de.greenrobot.daogenerator.DaoGenerator;
import de.greenrobot.daogenerator.Entity;
import de.greenrobot.daogenerator.Schema;

public class AppDaoGenerator {

    public static void main(String[] args) throws Exception {
        Schema schema = new Schema(1, "org.sorz.lab.smallcloudemoji.models");

        addRepository(schema);

        new DaoGenerator().generateAll(schema, "app/src-gen");

    }

    private static void addRepository(Schema schema) {
        Entity repository = schema.addEntity("Repository");
        repository.addIdProperty();
        repository.addStringProperty("url").notNull();
        repository.addStringProperty("alias");
        repository.addDateProperty("lastUpdateDate");
    }
}
