/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.datastore.h2;

import java.util.concurrent.Callable;
import org.h2.mvstore.MVStore;
import org.sensorhub.api.common.SensorHubException;
import org.sensorhub.api.database.IProcedureObsDatabaseModule;
import org.sensorhub.api.database.IProcedureObsDatabase;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.command.ICommandStore;
import org.sensorhub.api.datastore.feature.IFoiStore;
import org.sensorhub.api.datastore.obs.IObsStore;
import org.sensorhub.api.datastore.procedure.IProcedureStore;
import org.sensorhub.impl.module.AbstractModule;
import org.sensorhub.utils.FileUtils;


/**
 * <p>
 * Implementation of the {@link IProcedureObsDatabase} interface backed by
 * a single H2 MVStore that contains all maps necessary to store observations,
 * features of interest and procedure history.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 23, 2019
 */
public class MVObsDatabase extends AbstractModule<MVObsDatabaseConfig> implements IProcedureObsDatabase, IProcedureObsDatabaseModule<MVObsDatabaseConfig>
{
    public final static int CURRENT_VERSION = 1;
    final static String KRYO_CLASS_MAP_NAME = "kryo_class_map";
    final static String PROCEDURE_STORE_NAME = "proc_store";
    final static String FOI_STORE_NAME = "foi_store";
    final static String OBS_STORE_NAME = "obs_store";
    final static String CMD_STORE_NAME = "cmd_store";
    
    MVStore mvStore;
    MVProcedureStoreImpl procStore;
    MVObsStoreImpl obsStore;
    MVFoiStoreImpl foiStore;
    MVCommandStoreImpl cmdStore;
    
    
    @Override
    protected void beforeInit() throws SensorHubException
    {
        super.beforeInit();
        
        // check file path is valid
        if (!FileUtils.isSafeFilePath(config.storagePath))
            throw new DataStoreException("Storage path contains illegal characters: " + config.storagePath);
    }
    
    
    @Override
    protected void doStart() throws SensorHubException
    {
        try
        {
            MVStore.Builder builder = new MVStore.Builder().fileName(config.storagePath);
            
            if (config.readOnly)
                builder.readOnly();
            
            if (config.memoryCacheSize > 0)
                builder.cacheSize(config.memoryCacheSize/1024);
            
            if (config.autoCommitBufferSize > 0)
                builder.autoCommitBufferSize(config.autoCommitBufferSize);
            
            if (config.useCompression)
                builder.compress();
            
            mvStore = builder.open();
            mvStore.setAutoCommitDelay(config.autoCommitPeriod*1000);
            mvStore.setVersionsToKeep(0);
            
            // open procedure store
            procStore = MVProcedureStoreImpl.open(mvStore, config.idProviderType, MVDataStoreInfo.builder()
                .withName(PROCEDURE_STORE_NAME)
                .build());
            
            // open foi store
            foiStore = MVFoiStoreImpl.open(mvStore, config.idProviderType, MVDataStoreInfo.builder()
                .withName(FOI_STORE_NAME)
                .build());
            
            // open observation store
            obsStore = MVObsStoreImpl.open(mvStore, config.idProviderType, MVDataStoreInfo.builder()
                .withName(OBS_STORE_NAME)
                .build());
            
            // open command store
            cmdStore = MVCommandStoreImpl.open(mvStore, config.idProviderType, MVDataStoreInfo.builder()
                .withName(CMD_STORE_NAME)
                .build());
            
            procStore.linkTo(obsStore.getDataStreams());
            foiStore.linkTo(procStore);
            foiStore.linkTo(obsStore);
            obsStore.linkTo(foiStore);
            obsStore.getDataStreams().linkTo(procStore);
            cmdStore.getCommandStreams().linkTo(procStore);
        }
        catch (Exception e)
        {
            throw new DataStoreException("Error while starting MVStore", e);
        }
    }
    
    
    @Override
    protected void afterStart()
    {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().register(this);
    }
    
    
    @Override
    protected void beforeStop()
    {
        if (hasParentHub() && config.databaseNum != null)
            getParentHub().getDatabaseRegistry().unregister(this);
    }


    @Override
    protected void doStop() throws SensorHubException
    {
        if (mvStore != null) 
        {
            // must call commit first to make sure kryo persistent class resolver
            // is updated before we serialize it again in close
            mvStore.commit();
            mvStore.close();
            mvStore = null;
        }
    }


    @Override
    public Integer getDatabaseNum()
    {
        return config.databaseNum;
    }


    @Override
    public IProcedureStore getProcedureStore()
    {
        checkStarted();
        return procStore;
    }


    @Override
    public IObsStore getObservationStore()
    {
        checkStarted();
        return obsStore;
    }


    @Override
    public IFoiStore getFoiStore()
    {
        checkStarted();
        return foiStore;
    }


    @Override
    public ICommandStore getCommandStore()
    {
        checkStarted();
        return cmdStore;
    }


    @Override
    public void commit()
    {
        checkStarted();
        mvStore.commit();
    }
    
    
    @Override
    public <T> T executeTransaction(Callable<T> transaction) throws Exception
    {
        checkStarted();
        synchronized (mvStore)
        {
            long currentVersion = mvStore.getCurrentVersion();
            
            try
            {
                return transaction.call();
            }
            catch (Exception e)
            {
                mvStore.rollbackTo(currentVersion);
                throw e;
            }
        }
    }


    @Override
    public boolean isOpen()
    {
        return mvStore != null &&
            !mvStore.isClosed() &&
            isStarted();
    }


    @Override
    public boolean isReadOnly()
    {
        checkStarted();
        return mvStore.isReadOnly();
    }

    
    public MVStore getMVStore()
    {
        return mvStore;
    }
}
