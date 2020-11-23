/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.procedure;

import java.time.Instant;
import java.util.Map.Entry;
import org.sensorhub.api.data.DataEvent;
import org.sensorhub.api.data.FoiEvent;
import org.sensorhub.api.data.IStreamingDataInterface;
import org.sensorhub.api.datastore.obs.DataStreamKey;
import org.sensorhub.api.datastore.obs.IDataStreamStore;
import org.sensorhub.api.event.Event;
import org.sensorhub.api.event.IEventListener;
import org.sensorhub.api.obs.DataStreamInfo;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.obs.ObsData;
import org.sensorhub.utils.DataComponentChecks;
import org.sensorhub.utils.SWEDataUtils;
import org.vast.swe.SWEHelper;
import org.vast.swe.ScalarIndexer;
import org.vast.util.Asserts;
import org.vast.util.TimeExtent;
import net.opengis.swe.v20.DataBlock;


public class DataStreamEventPersistenceHandler implements IEventListener
{
    ProcedureEventPersistenceHandler procedureHandler;
    ScalarIndexer timeStampIndexer;
    long dataStreamID;
    
    // TODO optimize for case where procedure has a single FOI
    // but account for the fact that it can change in time (notify foi change from parent)
    // FeatureId singleFoiID; 
        

    public DataStreamEventPersistenceHandler(ProcedureEventPersistenceHandler procedure)
    {
        this.procedureHandler = Asserts.checkNotNull(procedure, ProcedureEventPersistenceHandler.class);        
    }
    
    
    protected boolean register(IStreamingDataInterface dataStream)
    {
        Asserts.checkNotNull(dataStream, IStreamingDataInterface.class);
        
        var procId = procedureHandler.procID;
        var procUID = procedureHandler.procUID;
        boolean isNew = true;
        
        // try to retrieve existing data stream
        IDataStreamStore dataStreamStore = procedureHandler.getDatabase().getDataStreamStore();
        Entry<DataStreamKey, IDataStreamInfo> dsEntry = dataStreamStore.getLatestVersionEntry(procUID, dataStream.getName());
        DataStreamKey dsKey;
        
        if (dsEntry == null)
        {
            // create new data stream
            DataStreamInfo dsInfo = new DataStreamInfo.Builder()
                .withProcedure(procId)
                .withRecordDescription(dataStream.getRecordDescription())
                .withRecordEncoding(dataStream.getRecommendedEncoding())
                .build();
            dsKey = dataStreamStore.add(dsInfo);
        }
        else
        {
            // if an output with the same name already existed
            dsKey = dsEntry.getKey();
            IDataStreamInfo dsInfo = dsEntry.getValue();
            
            dsInfo = new DataStreamInfo.Builder()
                .withProcedure(procId)
                .withRecordDescription(dataStream.getRecordDescription())
                .withRecordEncoding(dataStream.getRecommendedEncoding())
                .withValidTime(TimeExtent.endNow(Instant.now()))
                .build();
            
            // 2 cases
            // if structure has changed, create a new datastream
            if (!DataComponentChecks.checkStructCompatible(dsInfo.getRecordStructure(), dataStream.getRecordDescription()))
                dsKey = dataStreamStore.add(dsInfo);
            
            // if something else has changed, update existing datastream
            else if (!DataComponentChecks.checkStructEquals(dsInfo.getRecordStructure(), dataStream.getRecordDescription()))
                dataStreamStore.put(dsKey, dsInfo); 
            
            // else don't update and return existing key
            else
                isNew = false;
        }
        
        dataStreamID = dsKey.getInternalID();
        timeStampIndexer = SWEHelper.getTimeStampIndexer(dataStream.getRecordDescription());
        if (dataStream.isEnabled())
            dataStream.registerListener(this);
        
        return isNew;
    }
    
    
    @Override
    public void handleEvent(Event e)
    {
        if (e instanceof DataEvent)
        {
            DataEvent dataEvent = (DataEvent)e;
            String foiUID = dataEvent.getFoiUID();
            
            // lookup FOI full ID
            var foiId = ObsData.NO_FOI;
            if (foiUID != null)
            {
                var fid = procedureHandler.fois.get(foiUID);
                if (fid != null)
                    foiId = fid;
                else
                    throw new IllegalStateException("Unknown FOI: " + foiUID);
            }
            
            // store all records
            for (DataBlock record: dataEvent.getRecords())
            {
                // get time stamp
                double time;
                if (timeStampIndexer != null)
                    time = timeStampIndexer.getDoubleValue(record);
                else
                    time = e.getTimeStamp() / 1000.;
            
                // store record with proper key
                ObsData obs = new ObsData.Builder()
                    .withDataStream(dataStreamID)
                    .withFoi(foiId)
                    .withPhenomenonTime(SWEDataUtils.toInstant(time))
                    .withResult(record)
                    .build();
                
                procedureHandler.getDatabase().getObservationStore().add(obs);
            }
        }
        
        else if (e instanceof FoiEvent)
        {
            if (((FoiEvent) e).getFoi() != null)
                procedureHandler.register(((FoiEvent) e).getFoi());
        }
    }
}
