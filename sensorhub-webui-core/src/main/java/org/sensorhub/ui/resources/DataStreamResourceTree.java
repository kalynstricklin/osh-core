/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.resources;

import com.vaadin.event.Action;
import com.vaadin.v7.ui.TreeTable;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.feature.FeatureId;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEBuilders;
import org.vast.swe.SWEHelper;
import org.vast.util.TimeExtent;

import java.util.List;

import static java.util.UUID.randomUUID;


@SuppressWarnings({"deprecation"})
public class DataStreamResourceTree extends ResourceTree<DataStreamInfo, DataStreamInfo.Builder>
{
    private static final String NAMES_PROPERTY = "Name";
    private static final String DESCRIPTION_PROPERTY = "Description";
    private static final String VALIDTIME_PROPERTY = "Valid Time";
    private static final String RECORD_STRUCT_PROPERTY = "Record Structure";
    private static final String RECORD_ENCODING_PROPERTY = "Record Encoding";
    private static final String SYSTEM_ID_PROPERTY = "System ID";


    static void getActions(Class<?> moduleClass, List<Action> actions)
    {
        ResourceTree.getActions(moduleClass, actions);
    }

    @Override
    protected DataStreamInfo buildResourceFromTree(TreeTable tree, Object parentId) {
        var builder = new DataStreamInfo.Builder();
        super.buildResourceFromTree(tree, parentId, builder);
        return builder.build();
    }

    static DataStreamInfo newDataStreamInfo() {
        SWEHelper sweFactory = new SWEHelper();
        FeatureId sysId = new FeatureId(BigId.NONE, randomUUID().toString());

        var record = new SMLHelper().createRecord()
                .name("Output1")
                .addField("sampleTime", new SWEHelper().createTime()
                        .asSamplingTimeIsoUTC()
                        .label("Sample Time")
                        .description("Time of data collection"))
                .build();
        var dataEncoding = sweFactory.newTextEncoding(",", "\n");

        return new DataStreamInfo.Builder()
                .withName("New DataStream")
                .withSystem(sysId)
                .withRecordDescription(record)
                .withRecordEncoding(dataEncoding)
                .build();
    }
    
    @Override
    public Object renderResourceAsTree(TreeTable tree, Object parentId, DataStreamInfo dataStreamInfo)
    {
        tree.setPageLength(tree.getPageLength()+7);
        var id = tree.addItem(new Object[] {"Datastream Resource", null}, null);
        if (parentId != null)
            tree.setParent(id, parentId);

        // Add all DataStreamInfo properties
        toTreeItem(tree, id, NAMES_PROPERTY, dataStreamInfo.getName());
        toTreeItem(tree, id, DESCRIPTION_PROPERTY, dataStreamInfo.getName());
        toTreeItem(tree, id, SYSTEM_ID_PROPERTY, dataStreamInfo.getSystemID());
//        toTreeItem(tree, id, VALIDTIME_PROPERTY, dataStreamInfo.getValidTime());
//        toTreeItem(tree, id, RECORD_STRUCT_PROPERTY, DataRecordResourceTree::newDataRecord, dataStreamInfo.getRecordStructure());
        toTreeItem(tree, id, RECORD_ENCODING_PROPERTY, dataStreamInfo.getRecordEncoding());

        return id;
    }
    

    @Override
    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, DataStreamInfo.Builder builder)
    {

        if(NAMES_PROPERTY.equals(itemName)){
            var name = readStringList(itemValue);
            System.out.println("name: "+ name);
            builder.withName(name.toString());
        }
        else if(DESCRIPTION_PROPERTY.equals(itemName)){
            var desc = readStringList(itemValue);
            builder.withDescription(desc.toString());
        }
        else if (RECORD_STRUCT_PROPERTY.equals(itemName))
        {
            var record = new SMLHelper().createRecord()
                    .addField("sampleTime", new SWEHelper().createTime()
                            .asSamplingTimeIsoUTC()
                            .label("Sample Time")
                            .description("Time of data collection"))
                    .build();
            builder.withRecordDescription(record);
        }
        else if (VALIDTIME_PROPERTY.equals(itemName))
        {
            var tf = readTemporalFilter(itemValue);
            builder.withValidTime(TimeExtent.parse(tf.toString()));
        }
        else if (RECORD_ENCODING_PROPERTY.equals(itemName))
        {
            var re = new DataStreamInfo().getRecordEncoding();
            builder.withRecordEncoding(re);
        }
        else if (SYSTEM_ID_PROPERTY.equals(itemName))
        {
            var sysID = readIdList(itemValue);
            builder.withSystem((FeatureId) sysID);
        }
    }

}
