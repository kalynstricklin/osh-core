/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.h2;

import java.nio.ByteBuffer;
import java.time.Instant;
import org.h2.mvstore.DataUtils;
import org.h2.mvstore.WriteBuffer;
import org.h2.mvstore.type.DataType;


/**
 * <p>
 * H2 DataType implementation to index observations by series ID, then 
 * phenomenon time.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 12, 2019
 */
class MVObsKeyDataType implements DataType
{
    private static final int MEM_SIZE = 8+12; // long ID + instant
    
    
    @Override
    public int compare(Object objA, Object objB)
    {
        MVObsKey a = (MVObsKey)objA;
        MVObsKey b = (MVObsKey)objB;
        
        // first compare series IDs
        int comp = Long.compare(a.seriesID, b.seriesID);
        if (comp != 0)
            return comp;
        
        // if series IDs are equal, compare time stamps
        return a.getPhenomenonTime().compareTo(b.getPhenomenonTime());
    }
    

    @Override
    public int getMemory(Object obj)
    {
        return MEM_SIZE;
    }
    

    @Override
    public void write(WriteBuffer wbuf, Object obj)
    {
        MVObsKey key = (MVObsKey)obj;
        wbuf.putVarLong(key.seriesID);
        H2Utils.writeInstant(wbuf, key.getPhenomenonTime());
    }
    

    @Override
    public void write(WriteBuffer wbuf, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            write(wbuf, obj[i]);
    }
    

    @Override
    public Object read(ByteBuffer buff)
    {
        long seriesID = DataUtils.readVarLong(buff);
        Instant phenomenonTime = H2Utils.readInstant(buff);        
        return new MVObsKey(seriesID, phenomenonTime);
    }
    

    @Override
    public void read(ByteBuffer buff, Object[] obj, int len, boolean key)
    {
        for (int i=0; i<len; i++)
            obj[i] = read(buff);        
    }

}
