/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui;

import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.FontAwesome;
import com.vaadin.server.Page;
import com.vaadin.server.ThemeResource;
import com.vaadin.shared.MouseEventDetails.MouseButton;
import com.vaadin.ui.*;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.v7.data.Container;
import com.vaadin.v7.ui.*;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.TextArea;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.command.CommandData;
import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.data.ObsData;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.api.database.IObsSystemDatabaseModule;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.datastore.obs.ObsFilter;
import org.sensorhub.api.datastore.system.SystemFilter;
import org.sensorhub.api.feature.FeatureId;
import org.sensorhub.api.module.ModuleConfig;
import org.sensorhub.impl.system.SystemDatabaseTransactionHandler;
import org.sensorhub.ui.api.IModuleAdminPanel;
import org.sensorhub.ui.data.FieldProperty;
import org.sensorhub.ui.data.MyBeanItem;
import org.sensorhub.ui.resources.DataStreamResourceTree;
//import org.sensorhub.ui.resources.DatabaseResourceConfigForm;
import org.sensorhub.ui.resources.ResourceTree;
import org.vast.sensorML.SMLBuilders;
import org.vast.sensorML.SMLFactory;
import org.vast.sensorML.SMLHelper;
import org.vast.swe.SWEHelper;
import org.vast.util.BaseBuilder;
import org.vast.util.IResource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <p>
 * Admin panel for database modules.<br/>
 * This adds a section to view storage content in a table + histograms to
 * view the distribution of data records over time
 * </p>
 *
 * @author Alex Robin
 * @since 1.0
 */
@SuppressWarnings({ "serial", "deprecation" })
public class DatabaseAdminPanel extends DefaultModulePanel<IObsSystemDatabaseModule<?>> implements IModuleAdminPanel<IObsSystemDatabaseModule<?>>
{
    private static final Action DELETE_SYSTEM_ACTION = new Action("Delete All System Data", new ThemeResource("icons/module_delete.png"));
    private static final Action DELETE_OBS_ACTION = new Action("Delete System Observations", new ThemeResource("icons/module_delete.png"));
    
    VerticalLayout layout;
    SystemSearchList systemTable;
    TabSheet dataStreamTabs;

    TabSheet configTabs;
//
    private static final String PROP_NAME = "name";
    private static final String PROP_STATE = "state";
    private static final String PROP_RESOURCE_OBJECT = "resource";

    transient Map<Class<?>, TreeTable> resourceTables = new HashMap<>();
    protected final Map<String, Class<?>> resourceTypes = new HashMap<>();


    @Override
    public void build(final MyBeanItem<ModuleConfig> beanItem, final IObsSystemDatabaseModule<?> db)
    {
        super.build(beanItem, db);


        resourceTypes.put("System Resource", PhysicalSystem.class);
        resourceTypes.put("DataStream Resource", DataStreamInfo.class);
        resourceTypes.put("Obs Resource", ObsData.class);
        resourceTypes.put("Command Stream Resource", CommandStreamInfo.class);
        resourceTypes.put("Command Resource", CommandData.class);

        // assign default database number if not set and module hasn't been initialized yet
        if (!db.isInitialized() && db.getConfiguration().databaseNum == null)
        {
            int highestDbNum = 0;
            for (var otherDb: getParentHub().getDatabaseRegistry().getAllDatabases())
                highestDbNum = Math.max(otherDb.getDatabaseNum(), highestDbNum);
            for (var otherDb: getParentHub().getModuleRegistry().getLoadedModules(IObsSystemDatabase.class))
            {
                if (otherDb.getDatabaseNum() == null)
                    continue;
                highestDbNum = Math.max(otherDb.getDatabaseNum(), highestDbNum);
            }
            
            var nextDbNum = highestDbNum+1;
            //db.getConfiguration().databaseNum = nextDbNum;
            ((FieldProperty)beanItem.getItemProperty("databaseNum")).setValue(nextDbNum);
        }
        
        if (db != null && db.isStarted())
        {
            // section layout
            layout = new VerticalLayout();
            layout.setWidth(100.0f, Unit.PERCENTAGE);
            layout.setMargin(false);
            layout.setSpacing(true);


            //todo add components here :D (resources)

            HorizontalLayout clientDBLayout = new HorizontalLayout();
            clientDBLayout.setSpacing(true);
            Label resourceSectionlabel = new Label("Module API Database Client");
            resourceSectionlabel.addStyleName(STYLE_H3);
            resourceSectionlabel.addStyleName(STYLE_COLORED);
            clientDBLayout.addComponent(resourceSectionlabel);
            clientDBLayout.setComponentAlignment(resourceSectionlabel, Alignment.MIDDLE_LEFT);

            //panel resource builders, buttons to add resources
            VerticalLayout resourceButtons = new VerticalLayout();
            resourceButtons.setSpacing(true);

            Button addSystemBtn = new Button("Add Resource", FontAwesome.PLUS);
            addSystemBtn.addStyleName(STYLE_SMALL);
            addSystemBtn.addClickListener(e -> {
                showResourceTypeSelector(resourceButtons);
            });


            resourceButtons.addComponent(addSystemBtn);

            layout.addComponent(clientDBLayout);
            layout.addComponent(resourceButtons);


            // section title
            //layout.addComponent(new Label(""));
            HorizontalLayout titleBar = new HorizontalLayout();
            titleBar.setSpacing(true);
            Label sectionLabel = new Label("Database Content");
            sectionLabel.addStyleName(STYLE_H3);
            sectionLabel.addStyleName(STYLE_COLORED);
            titleBar.addComponent(sectionLabel);
            titleBar.setComponentAlignment(sectionLabel, Alignment.MIDDLE_LEFT);
            layout.addComponent(titleBar);

            // add resource functionality

            systemTable = new SystemSearchList(db, event -> {
                if (event.getButton() == MouseButton.LEFT)
                {
                    try
                    {
                        // select and open module configuration
                        String sysUID = (String)event.getItem().getItemProperty(SystemSearchList.PROP_SYSTEM_UID).getValue();
                        if (sysUID != null)
                            showSystemData(db, sysUID);
                    }
                    catch (Exception e)
                    {
                        DisplayUtils.showErrorPopup("Unexpected error when selecting system", e);
                    }
                }
            });

            // right click on table to add resources to table


            // also add context menu
            systemTable.getTable().addActionHandler(new Handler() {
                @Override
                public Action[] getActions(Object target, Object sender)
                {
                    List<Action> actions = new ArrayList<>(10);
                    actions.add(DELETE_SYSTEM_ACTION);
                    actions.add(DELETE_OBS_ACTION);
                    return actions.toArray(new Action[0]);
                }

                @Override
                public void handleAction(Action action, Object sender, Object target)
                {
                    String uid = (String)((TreeTable)sender).getValue();
                    
                    if (action == DELETE_SYSTEM_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to remove all data and metadata associated with system:<br/><b>" + uid + "?</b>");
                        popup.addCloseListener(event -> {
                            if (popup.isConfirmed())
                            {
                                try
                                {
                                    var eventBus = module.getParentHub().getEventBus();
                                    var txnHandler = new SystemDatabaseTransactionHandler(eventBus, db);
                                    txnHandler.getSystemHandler(uid).delete(true);
                                    systemTable.updateTable(db, new SystemFilter.Builder().build());
                                }
                                catch (Exception ex)
                                {
                                    getOshLogger().error("Error deleting system", ex);
                                }
                            }
                        });
                        systemTable.getUI().addWindow(popup);
                    }
                    else if (action == DELETE_OBS_ACTION)
                    {
                        final ConfirmDialog popup = new ConfirmDialog("Are you sure you want to remove all observations from system:<br/><b>" + uid + "?</b>");
                        popup.addCloseListener(event -> {
                            if (popup.isConfirmed())
                            {
                                try
                                {
                                    db.getObservationStore().removeEntries(new ObsFilter.Builder()
                                        .withDataStreams()
                                            .withSystems()
                                                .withUniqueIDs(uid)
                                                .done()
                                            .done()
                                        .build());
                                    systemTable.updateTable(db, new SystemFilter.Builder().build());
                                }
                                catch (Exception ex)
                                {
                                    getOshLogger().error("Error deleting observations", ex);
                                }
                            }
                        });
                        systemTable.getUI().addWindow(popup);
                    }
                }
            });

            layout.addComponent(systemTable);
            
            Page.getCurrent().getStyles()
                .add(".datastore-table { min-height: 400px }");
            dataStreamTabs = new TabSheet();
            dataStreamTabs.addStyleName("datastore-table");
            dataStreamTabs.addSelectedTabChangeListener(e -> {
                // load data when tab is selected
                ((DatabaseStreamPanel)dataStreamTabs.getSelectedTab()).refreshContent();
            });


            layout.addComponent(dataStreamTabs);
            
            addComponent(layout);
        }
    }

    protected void buildResourceList(VerticalLayout layout, List resouceList){
        final TreeTable table = new TreeTable();
        table.setSizeFull();
        table.setSelectable(true);
        table.setNullSelectionAllowed(false);
        table.setImmediate(true);
        table.setColumnReorderingAllowed(false);
        table.addContainerProperty(PROP_NAME, String.class, PROP_NAME);
        table.addContainerProperty(PROP_RESOURCE_OBJECT, IResource.class, null);

//        addResourceToTable(table);

//        table.addItemClickListener(new Tree.ItemClickListener(){
//            @Override
//            public void itemClick(Tree.ItemClick event) {
//                try{
//
//                    var item = event.getItem();
//                    System.out.println("item chosen: "+ event.getItem());
//
//                }catch(Exception e){
//                    DisplayUtils.showErrorPopup("Unexpected error when selecting resource", e);
//                }
//            }
//        });

    }

    IObsSystemDatabase writeDatabase;
    IObsSystemDatabase readDatabase;

    protected synchronized void showSystemData(final IObsSystemDatabase db, String sysUID)
    {
        // remove previous tabs
        dataStreamTabs.removeAllComponents();
        
        // show in tabs
        db.getDataStreamStore().selectEntries(new DataStreamFilter.Builder()
                .withSystems().withUniqueIDs(sysUID).done()
                .withLimit(10)
                .build())
            .forEach(dsEntry -> {
                var dsID = dsEntry.getKey().getInternalID();
                var dsInfo = dsEntry.getValue();
                var dsPanel = new DatabaseStreamPanel(db, dsInfo, dsID);
                dataStreamTabs.addTab(dsPanel, dsPanel.getCaption(), FontAwesome.DATABASE);
            });
    }



    protected void showResourceTypeSelector(VerticalLayout clientLayout){
        final ObjectTypeSelectionPopup.ObjectTypeSelectionWithClearCallback callback = new ObjectTypeSelectionPopup.ObjectTypeSelectionWithClearCallback() {
            @Override
            public void onSelected(Class<?> objectType)
            {
                try
                {
                    var obj = objectType.newInstance();
//                    if (obj instanceof BaseBuilder)
//                        obj = ((BaseBuilder<?>)obj).build();
                    var tree = buildResourceTree((IResource) obj);

                    Button saveRscButton = new Button("Save Resource");
                    saveRscButton.addClickListener(new Button.ClickListener() {
                        @Override
                        public void buttonClick(Button.ClickEvent event) {
                            // save to database and call the builder :D
                        }
                    });
                    clientLayout.addComponents(tree, saveRscButton);
                }
                catch (Exception e)
                {
                    DisplayUtils.showErrorPopup("Error during opening resource selection", e);
                }
            }

            @Override
            public void onClearSelection()
            {

            }
        };

        var popup = new ObjectTypeSelectionPopup("Add new Resource", resourceTypes, callback);

        popup.setModal(true);
        UI.getCurrent().addWindow(popup);
    }

    protected void showResourceTree(String resourceType){

    }


    void resizeTextField(TextArea txtBox, String txt)
    {
        int numLines = 1;
        for (int i = 0; i < txt.length(); i++) {
            if (txt.charAt(i) == '\n') {
                numLines++;
            }
        }

        txtBox.setRows(numLines);
    }

    protected TreeTable buildResourceTree(IResource resource){

        TreeTable tree = new TreeTable();
        tree.setPageLength(0);
        tree.addStyleName(STYLE_SMALL);
        tree.addContainerProperty(ResourceTree.PROP_NAME, String.class, null);
        tree.addContainerProperty(ResourceTree.PROP_VALUE, String.class, null);
        tree.addContainerProperty(ResourceTree.PROP_CALLBACK, Consumer.class, null);
        tree.setVisibleColumns(ResourceTree.PROP_NAME, ResourceTree.PROP_VALUE);
        tree.setColumnHeaderMode(Table.ColumnHeaderMode.HIDDEN);
        tree.setColumnWidth(ResourceTree.PROP_NAME, 230);
        tree.setColumnWidth(ResourceTree.PROP_VALUE, 400);
        tree.setColumnAlignment(ResourceTree.PROP_NAME, Table.Align.LEFT);

        tree.setTableFieldFactory(new TableFieldFactory() {
            @Override
            @SuppressWarnings("unchecked")
            public Field<?> createField(Container container, Object itemId, Object propertyId, Component uiContext) {

                var item = tree.getItem(itemId);
                Field<?> field = null;

                if (ResourceTree.PROP_VALUE.equals(propertyId))
                {
                    var value = item.getItemProperty(ResourceTree.PROP_VALUE).getValue();
                    var callback = (Consumer<Boolean>)item.getItemProperty(ResourceTree.PROP_CALLBACK).getValue();

                    if (callback != null)
                    {
                        var checkbox = new CheckBox();

                        checkbox.addValueChangeListener(e -> {
                            callback.accept(checkbox.getValue());
                        });

                        field = checkbox;
                    }
                    else if (value != null)
                    {
                        var textArea = new TextArea();
                        textArea.setStyleName(STYLE_SMALL);
                        textArea.setImmediate(true);
                        textArea.setRows(1);
                        textArea.setWidth(100, Unit.PERCENTAGE);
                        textArea.setTextChangeEventMode(AbstractTextField.TextChangeEventMode.EAGER);

                        textArea.setValue((String) value);
                        resizeTextField(textArea, textArea.getValue());

                        textArea.addTextChangeListener(e -> {
                            var txt = e.getText();
                            resizeTextField(textArea, txt);
                        });

                        textArea.addValueChangeListener(e -> {
                            resizeTextField(textArea, textArea.getValue());
                        });

                        field = textArea;
                    }
                }

                return field;
            }
        });

        tree.setEditable(true);



//        // Create resource and keep track of its class

        if(resource.getClass().isAssignableFrom(DataStreamInfo.class)){
            var rscBuilder = new DataStreamInfo();
//                    .withName("datastream1")
//                    .withSystem(new FeatureId(BigId.NONE, "urn:osh:system:kalyn"))
//                    .withRecordEncoding(new SWEHelper().newTextEncoding(",", "\n"))
//                    .withRecordDescription(new SMLHelper().createRecord()
//                                    .name("Output1")
//                                    .addField("sampleTime", new SWEHelper().createTime()
//                                            .asSamplingTimeIsoUTC()
//                                            .label("Sample Time")
//                                            .description("Time of data collection"))
//                                    .build())
//                    .build();

            if(rscBuilder != null){

            // Render Tree onto TreeTable
                var root = ResourceTree.create(resource.getClass());
                var rootItemId = root.renderResourceAsTree(tree, null, rscBuilder);
//

                for (Object item: tree.getItemIds())
                    tree.setCollapsed(item, false);
//
            }

        }else if(resource.getClass().isAssignableFrom(PhysicalSystem.class)){
////            rscBuilder = new SMLBuilders.PhysicalSystemBuilder(new SMLFactory()).build();
        }
////        var resource = new DataStreamInfo.Builder().build();

        return tree;
        // Adding TreeTable to layout
//        layout.addComponent(tree);
    }

}
