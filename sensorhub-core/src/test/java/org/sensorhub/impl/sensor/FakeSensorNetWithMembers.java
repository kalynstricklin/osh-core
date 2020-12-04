/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.sensor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import net.opengis.gml.v32.AbstractFeature;
import net.opengis.gml.v32.Point;
import net.opengis.gml.v32.impl.GMLFactory;
import net.opengis.sensorml.v20.AbstractProcess;
import net.opengis.sensorml.v20.PhysicalComponent;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.data.IDataProducer;
import org.sensorhub.api.data.IMultiSourceDataProducer;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.event.EventUtils;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.event.IEventSourceInfo;
import org.sensorhub.api.procedure.IProcedureDriver;
import org.sensorhub.api.procedure.IProcedureGroupDriver;
import org.sensorhub.impl.event.EventSourceInfo;
import org.vast.ogc.gml.GenericFeatureImpl;
import org.vast.ogc.gml.IGeoFeature;
import org.vast.sensorML.SMLFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;


public class FakeSensorNetWithMembers extends FakeSensor implements IMultiSourceDataProducer
{
    static String SENSORNET_UID = "urn:sensors:mysensornet:001";
    static String SENSOR_UID_PREFIX = SENSORNET_UID + ":sensors:";
    GMLFactory gmlFac = new GMLFactory(true);
    Map<String, IDataProducer> members = new LinkedHashMap<>();
    Map<String, AbstractFeature> allFois = new LinkedHashMap<>();
    
    
    public class FakeDataProducer implements IDataProducer
    {
        PhysicalComponent sensor;
        Map<String, IGeoFeature> fois;
        Map<String, IStreamingDataInterface> outputs = new HashMap<>();
        EventSourceInfo eventSrcInfo;
        
        public FakeDataProducer(int memberIdx)
        {
            Point p = gmlFac.newPoint();
            p.setPos(new double[] {memberIdx, memberIdx, 0.0});
            
            // create sensor description
            String memberUID = String.format(SENSOR_UID_PREFIX + "%03d", memberIdx);
            SMLFactory fac = new SMLFactory();
            sensor = fac.newPhysicalComponent();
            sensor.setUniqueIdentifier(memberUID);
            sensor.setName("Networked sensor " + memberUID.substring(memberUID.lastIndexOf(':')+1));
            sensor.addPositionAsPoint(p);
            
            // create FOI
            QName fType = new QName("http://myNsUri", "MyFeature");
            String foiUID = getFoiUID(memberIdx);
            var foi = new GenericFeatureImpl(fType);
            foi.setId("F" + memberIdx);
            foi.setUniqueIdentifier(foiUID);
            foi.setName("Station " + memberIdx);
            foi.setDescription("This is measurement station #" + memberIdx);
            foi.setGeometry(p);
            allFois.put(foiUID, foi);
            fois = ImmutableMap.of(foiUID, foi);
            
            String groupID = getParentGroup().getUniqueIdentifier();
            String sourceID = EventUtils.getProcedureSourceID(memberUID);
            eventSrcInfo = new EventSourceInfo(groupID, sourceID);
        }   
        
        public void addOutputs(IStreamingDataInterface... outputs)
        {
            for (var output: outputs)
                this.outputs.put(output.getName(), output);            
        }

        @Override
        public IProcedureGroupDriver<? extends IProcedureDriver> getParentGroup()
        {
            return FakeSensorNetWithMembers.this;
        }

        @Override
        public String getParentGroupUID()
        {
            return FakeSensorNetWithMembers.this.getUniqueIdentifier();
        }
    
        @Override
        public String getUniqueIdentifier()
        {
            return sensor.getUniqueIdentifier();
        }
    
        @Override
        public AbstractProcess getCurrentDescription()
        {
            return sensor;
        }

        @Override
        public long getLastDescriptionUpdate()
        {
            return 0;
        }

        @Override
        public Map<String, ? extends IStreamingDataInterface> getOutputs()
        {
            return Collections.unmodifiableMap(outputs);
        }

        @Override
        public Map<String, ? extends IGeoFeature> getCurrentFeaturesOfInterest()
        {
            return fois;
        }

        @Override
        public void registerListener(IEventListener listener)
        {
        }

        @Override
        public void unregisterListener(IEventListener listener)
        {                
        }

        @Override
        public String getName()
        {
            return sensor.getName();
        }

        @Override
        public String getDescription()
        {
            return sensor.getDescription();
        }

        @Override
        public boolean isEnabled()
        {
            return FakeSensorNetWithMembers.this.isStarted();
        }

        @Override
        public IEventSourceInfo getEventSourceInfo()
        {
            return eventSrcInfo;
        }
    };


    @Override
    public void init() throws SensorHubException
    {
        super.init();
        this.uniqueID = SENSORNET_UID;
        this.xmlID = "SENSORNET1";
    }
    
    
    public void addMembers(int numMembers, Consumer<FakeDataProducer> memberConfigurator)
    {
        for (int memberIdx = 1; memberIdx <= numMembers; memberIdx++)
        {
            FakeDataProducer producer = new FakeDataProducer(memberIdx);
            memberConfigurator.accept(producer);
            members.put(producer.getUniqueIdentifier(), producer);
        }
    }


    @Override
    public Map<String, ? extends IDataProducer> getMembers()
    {
        return Collections.unmodifiableMap(members);
    }


    @Override
    public Map<String, AbstractFeature> getCurrentFeaturesOfInterest()
    {
        return Collections.unmodifiableMap(allFois);
    }
    
    
    @Override
    public String getFoiUID(int foiNum)
    {
        return String.format(SENSOR_UID_PREFIX + "%03d:foi", foiNum);
    }


    @Override
    public Collection<String> getProceduresWithFoi(String foiUID)
    {
        if (!allFois.containsKey(foiUID))
            return Collections.emptySet();
        
        String memberUID = foiUID.substring(0, foiUID.indexOf(":foi:"));
        return ImmutableList.of(memberUID);
    }
    

    @Override
    protected void updateSensorDescription()
    {
        synchronized (sensorDescLock)
        {
            super.updateSensorDescription();
            
            int index = 0;
            for (IDataProducer sensor: members.values())
            {
                index++;
                ((PhysicalSystem)sensorDescription).getComponentList().add("sensor"+index, sensor.getUniqueIdentifier(), null);
            }
        }
    }


    @Override
    public void start() throws SensorHubException
    {        
    }
    
    
    @Override
    public void stop() throws SensorHubException
    {
        for (IDataProducer sensor: members.values())
            for (IStreamingDataInterface o: sensor.getOutputs().values())
                ((IFakeSensorOutput)o).stop();
    }


    @Override
    public boolean isConnected()
    {
        return true;
    }
    
    
    @Override
    public void cleanup() throws SensorHubException
    {
    }


    @Override
    public CompletableFuture<Void> startSendingData()
    {
        var outputFutures = new ArrayList<CompletableFuture<?>>();
        
        for (IDataProducer sensor: members.values())
        {
            for (IStreamingDataInterface o: sensor.getOutputs().values())
            {
                var f = ((IFakeSensorOutput)o).start();
                outputFutures.add(f);
            }
        }
        
        return CompletableFuture.allOf(outputFutures.toArray(new CompletableFuture[0]));
    }
    
    
    public CompletableFuture<Void> startSendingData(String... memberUIDs)
    {
        var outputFutures = new ArrayList<CompletableFuture<?>>();
        
        for (var uid: memberUIDs)
        {
            var sensor = members.get(uid);
            for (IStreamingDataInterface o: sensor.getOutputs().values())
            {
                var f = ((IFakeSensorOutput)o).start();
                outputFutures.add(f);
            }
        }
        
        return CompletableFuture.allOf(outputFutures.toArray(new CompletableFuture[0]));
    }
    
    
    public boolean hasMoreData()
    {
        for (IDataProducer sensor: members.values())
            for (IStreamingDataInterface o: sensor.getOutputs().values())
                if (o.isEnabled())
                    return true;
        
        return false;
    }
    
}
