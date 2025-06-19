///***************************** BEGIN LICENSE BLOCK ***************************
//
//The contents of this file are subject to the Mozilla Public License, v. 2.0.
//If a copy of the MPL was not distributed with this file, You can obtain one
//at http://mozilla.org/MPL/2.0/.
//
//Software distributed under the License is distributed on an "AS IS" basis,
//WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
//for the specific language governing rights and limitations under the License.
//
//Copyright (C) 2022 Sensia Software LLC. All Rights Reserved.
//
//******************************* END LICENSE BLOCK ***************************/
//
//package org.sensorhub.ui.resources;
//
//import com.vaadin.event.Action;
//import com.vaadin.v7.ui.TreeTable;
//import org.sensorhub.api.data.ObsData;
//import org.sensorhub.api.datastore.obs.ObsFilter;
//import org.sensorhub.ui.filter.DataStreamFilterTree;
//import org.sensorhub.ui.filter.FilterTree;
//import org.sensorhub.ui.filter.FoiFilterTree;
//import org.sensorhub.ui.filter.ResourceFilterTree;
//
//import java.util.List;
//
//
//@SuppressWarnings({"deprecation"})
//public class ObsResourceTree extends ResourceTree<ObsData, ObsData.ObsDataBuilder>
//{
//    private static final String PHENTIME_PROPERTY = "Phenomenon Time";
//    private static final String RESULT_PROPERTY = "Result";
//    private static final String DATASTREAM_ID_PROPERTY = "Datastream ID";
//    private static final String FOI_IDPROPERTY = "FOI ID";
//
//
//    static void getActions(Class<?> filterClass, List<Action> actions)
//    {
//        ResourceTree.getActions(filterClass, actions);
//    }
//
//
//    static ObsData newObsData()
//    {
//        return new ObsData.Builder().build();
//    }
//
//
//    @Override
//    protected Object renderResourceAsTree(TreeTable tree, Object parentId, ObsData obsData)
//    {
//        tree.setPageLength(tree.getPageLength()+5);
//        var id = tree.addItem(new Object[] {"Obs Resource", null}, null);
//        if (parentId != null)
//            tree.setParent(id, parentId);
//
//        toTreeItem(tree, id, PHENTIME_PROPERTY, obsData.getPhenomenonTime().toString());
//        toTreeItem(tree, id, RESULT_PROPERTY, obsData.getResult().getStringValue());
//        toTreeItem(tree, id, DATASTREAM_ID_PROPERTY, DataStreamResourceTree::newDataStreamInfo, obsData.getDataStreamID());
////        toTreeItem(tree, id, FOI_ID_PROPERTY, FoiFilterTree::newFilter, obsData.getFoiFilter());
//
//        return id;
//    }
//
//
//    @Override
//    protected ObsData buildResourceFromTree(TreeTable tree, Object parent)
//    {
//        var builder = new ObsData.Builder();
//        super.buildResourceFromTree(tree, parent, builder);
//        return builder.build();
//    }
//
//
//    @Override
//    protected void fromTreeItem(TreeTable tree, Object itemId, String itemName, String itemValue, ObsData.ObsDataBuilder builder)
//    {
//        if (PHENTIME_PROPERTY.equals(itemName))
//        {
////            var tf = readTemporalFilter(itemValue);
////            builder.withPhenomenonTime(tf);
//        }
//        else if (RESULT_PROPERTY.equals(itemName))
//        {
////            var tf = readTemporalFilter(itemValue);
////            builder.withResult(tf);
//        }
//        else if (DATASTREAM_ID_PROPERTY.equals(itemName) && Boolean.parseBoolean(itemValue))
//        {
//            var rootItemId = tree.getChildren(itemId).iterator().next();
//            var subTree = new DataStreamResourceTree();
//            var resource = subTree.buildResourceFromTree(tree, rootItemId);
//            builder.withDataStream(resource.getSystemID().getInternalID());
//        }
////        else if (FOIFILTER_PROPERTY.equals(itemName) && Boolean.parseBoolean(itemValue))
////        {
////            var rootItemId = tree.getChildren(itemId).iterator().next();
////            var subTree = new FoiFilterTree();
////            var filter = subTree.buildFilterFromTree(tree, rootItemId);
////            builder.withFois(filter);
////        }
//
//
//    }
//
//}
