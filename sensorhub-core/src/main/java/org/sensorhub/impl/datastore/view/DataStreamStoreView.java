/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.view;

import java.time.ZoneOffset;
import java.util.Set;
import java.util.stream.Stream;
import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.obs.DataStreamFilter;
import org.sensorhub.api.obs.IDataStreamInfo;
import org.sensorhub.api.obs.IDataStreamStore;
import org.sensorhub.api.obs.IDataStreamStore.DataStreamInfoField;
import org.sensorhub.api.resource.ResourceFilter;
import org.sensorhub.impl.datastore.registry.ReadOnlyDataStore;
import org.vast.util.Asserts;


public class DataStreamStoreView extends ReadOnlyDataStore<Long, IDataStreamInfo, DataStreamInfoField, DataStreamFilter> implements IDataStreamStore
{    
    IDataStreamStore delegate;
    DataStreamFilter viewFilter;
    
    
    public DataStreamStoreView(IDataStreamStore delegate, DataStreamFilter viewFilter)
    {
        this.delegate = delegate;
        this.viewFilter = viewFilter;
    }
    

    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Stream<Entry<Long, IDataStreamInfo>> selectEntries(DataStreamFilter filter, Set<DataStreamInfoField> fields)
    {
        try
        {
            return delegate.selectEntries(viewFilter.and((ResourceFilter)filter), fields);
        }
        catch (EmptyFilterIntersection e)
        {
            return Stream.empty();
        }
    }
    
    
    @Override
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public long countMatchingEntries(DataStreamFilter filter)
    {
        try
        {
            return delegate.countMatchingEntries(viewFilter.and((ResourceFilter)filter));
        }
        catch (EmptyFilterIntersection e)
        {
            return 0L;
        } 
    }


    @Override
    public IDataStreamInfo get(Object key)
    {
        Asserts.checkArgument(key instanceof Long);
        
        if (viewFilter.getInternalIDs() != null && !viewFilter.getInternalIDs().contains(key))
            return null;
        
        var proc = delegate.get(key);
        return viewFilter.test(proc) ? proc : null;
    }
    
    
    @Override
    public String getDatastoreName()
    {
        return delegate.getDatastoreName();
    }


    @Override
    public ZoneOffset getTimeZone()
    {
        return delegate.getTimeZone();
    }


    @Override
    public Long add(IDataStreamInfo dsInfo)
    {
        throw new UnsupportedOperationException(READ_ONLY_ERROR_MSG);
    }
}