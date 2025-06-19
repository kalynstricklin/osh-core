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
import net.opengis.sensorml.v20.PhysicalSystem;
import org.vast.sensorML.SMLBuilders;
import org.vast.sensorML.SMLFactory;

import java.util.List;


@SuppressWarnings({"deprecation"})
public class SystemResourceTree extends ResourceTree<PhysicalSystem, SMLBuilders.PhysicalSystemBuilder>
{
    private static final String PARENT_PROPERTY = "With Parent Systems";
    private static final String DATASTREAM_PROPERTY = "With Datastreams";
    

    // add child systems
    // add datastreams


    static void getActions(Class<?> filterClass, List<Action> actions)
    {
        ResourceTree.getActions(filterClass, actions);
    }
    
    
    static PhysicalSystem newPhysicalSystem() { return new SMLBuilders.PhysicalSystemBuilder(new SMLFactory()).build(); }
    
    
    @Override
    public Object renderResourceAsTree(TreeTable tree, Object parentId, PhysicalSystem physicalSystem)
    {
        tree.setPageLength(tree.getPageLength()+7);
        var id = tree.addItem(new Object[] {"System Module", null}, null);
        if (parentId != null)
            tree.setParent(id, parentId);
        
        toTreeItem(tree, id, PARENT_PROPERTY, SystemResourceTree::newPhysicalSystem, physicalSystem);
        toTreeItem(tree, id, DATASTREAM_PROPERTY, DataStreamResourceTree::newDataStreamInfo, physicalSystem);
        
        return id;
    }
    

    @Override
    protected PhysicalSystem buildResourceFromTree(TreeTable tree, Object parentId)
    {
        var builder = new SMLBuilders.PhysicalSystemBuilder(new SMLFactory());
        super.buildResourceFromTree(tree, parentId, builder);
        return builder.build();
    }


    @Override
    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, SMLBuilders.PhysicalSystemBuilder builder)
    {

        if (PARENT_PROPERTY.equals(itemName) && Boolean.parseBoolean(itemValue))
        {
            var rootItemId = tree.getChildren(itemId).iterator().next();
            var subTree = new SystemResourceTree();
            var resource = subTree.buildResourceFromTree(tree, rootItemId);
            builder.addComponent(resource.getName(), resource);
        }
        else if(DATASTREAM_PROPERTY.equals(itemName) && Boolean.parseBoolean(itemValue))
        {
            var rootItemId = tree.getChildren(itemId).iterator().next();
            var subTree = new DataStreamResourceTree();
            var resource = subTree.buildResourceFromTree(tree, rootItemId);
            builder.addOutput(resource.getName(), resource.getRecordStructure());
        }
    }

}
