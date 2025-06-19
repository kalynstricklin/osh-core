package org.sensorhub.ui.resources;

import com.vaadin.event.Action;
import com.vaadin.v7.ui.TreeTable;
import net.opengis.swe.v20.DataRecord;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.feature.FeatureId;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;

import java.net.URI;
import java.util.List;

import static java.util.UUID.randomUUID;


@SuppressWarnings({"deprecation"})
public class DataRecordResourceTree extends DataTree<DataRecord, SWEBuilders.DataRecordBuilder>
{
    private static final String NAME_PROPERTY = "Name";
    private static final String DEFINITION_PROPERTY = "Definition";
    private static final String FIELD_PROPERTY = "Field";

    static DataRecord newDataRecord() {
        return new SMLHelper().createRecord()
                .name("Output1")
                .addField("sampleTime", new SWEHelper().createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .build();
    }

    @Override
    public Object renderResourceAsTree(TreeTable tree, Object parentId, DataRecord record) {
        tree.setPageLength(tree.getPageLength()+5);
        var id = tree.addItem(new Object[] {"DataRecord Resource", null}, null);
        if (parentId != null)
            tree.setParent(id, parentId);

        toTreeItem(tree, id, NAME_PROPERTY, record.getName());
        toTreeItem(tree, id, DEFINITION_PROPERTY, record.getDefinition());
        toTreeItem(tree, id, FIELD_PROPERTY, record.getFieldList());
        return id;
    }

    static void getActions(Class<?> filterClass, List<Action> actions)
    {
        ResourceTree.getActions(filterClass, actions);
    }


    @Override
    protected DataRecord buildResourceFromTree(TreeTable tree, Object parent)
    {
        var builder = new SWEHelper().createRecord();
        super.buildResourceFromTree(tree, parent, builder);
        return builder.build();
    }

    @Override
    protected void fromTreeItem(TreeTable tree, Object parentId, String itemName, String itemValue, SWEBuilders.DataRecordBuilder builder) {
        if (NAME_PROPERTY.equals(itemName))
        {
            var name = readStringList(itemValue);
            builder.name(name.toString());
        }
        else if (DEFINITION_PROPERTY.equals(itemName))
        {
            var def = readStringList(itemValue);
            builder.definition(def.toString());
        }
        else if(FIELD_PROPERTY.equals(itemName)){
            builder.addField(itemName, new SWEHelper().createText()
                    .label(itemName)
                    .description("")
            );
        }
    }
}
