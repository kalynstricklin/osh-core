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

import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.datastore.IFeatureDatabase;
import org.sensorhub.api.persistence.StorageException;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.FileUtils;
import net.opengis.gml.v32.AbstractFeature;


/**
 * <p>
 * Implementation of the {@link IFeatureDatabase} interface backed by
 * a single H2 MVStore and a {@link MVFeatureStoreImpl}.
 * </p>
 *
 * @author Alex Robin
 * @date Oct 11, 2019
 */
public class MVFeatureDatabase extends AbstractModule<MVFeatureDatabaseConfig> implements IFeatureDatabase<AbstractFeature>
{
    final static String FEATURE_STORE_NAME = "feature_store";
    
    MVStore mvStore;
    MVFeatureStoreImpl featureStore;
    
    
    @Override
    public void start() throws SensorHubException
    {
        try
        {
            // check file path is valid
            if (!FileUtils.isSafeFilePath(config.storagePath))
                throw new StorageException("Storage path contains illegal characters: " + config.storagePath);
            
            MVStore.Builder builder = new MVStore.Builder()
                                      .fileName(config.storagePath);
            
            if (config.memoryCacheSize > 0)
                builder = builder.cacheSize(config.memoryCacheSize/1024);
                                      
            if (config.autoCommitBufferSize > 0)
                builder = builder.autoCommitBufferSize(config.autoCommitBufferSize);
            
            if (config.useCompression)
                builder = builder.compress();
            
            mvStore = builder.open();
            mvStore.setVersionsToKeep(0);
            
            // open procedure store
            if (H2Utils.getDataStoreInfo(mvStore, FEATURE_STORE_NAME) == null)
            {
                featureStore = MVFeatureStoreImpl.create(mvStore, MVDataStoreInfo.builder()
                    .withName(FEATURE_STORE_NAME)
                    .build());
            }
            else
                featureStore = MVFeatureStoreImpl.open(mvStore, FEATURE_STORE_NAME);
        }
        catch (Exception e)
        {
            throw new StorageException("Error while starting MVStore", e);
        }
    }


    @Override
    public void stop() throws SensorHubException
    {
        if (mvStore != null) 
        {
            mvStore.close();
            mvStore = null;
        }
    }
    

    @Override
    public MVFeatureStoreImpl getFeatureStore()
    {
        return featureStore;
    }


    @Override
    public void commit()
    {
        if (mvStore != null)
            mvStore.commit();        
    }

    
    public MVStore getMVStore()
    {
        return mvStore;
    }

}