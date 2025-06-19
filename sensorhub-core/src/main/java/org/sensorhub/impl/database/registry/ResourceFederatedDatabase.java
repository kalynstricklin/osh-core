/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.database.registry;

import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import net.opengis.sensorml.v20.PhysicalSystem;
import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.api.database.IDatabaseRegistry;
import org.sensorhub.api.database.IObsSystemDatabase;
import org.sensorhub.impl.datastore.view.ResourceDatabaseView;
import org.vast.util.Asserts;

import java.util.Collection;
import java.util.Set;


/**
 * <p>
 * Extension of federated obs database allowing to filter out certain
 * databases or add new ones.
 * </p>
 *
 * @author Alex Robin
 * @since Dec 11, 2020
 */
public class ResourceFederatedDatabase extends FederatedDatabase
{
    final Set<Integer> unfilteredDatabases = Sets.newHashSet();
    final PhysicalSystem physicalSystem;
    final DataStreamInfo dataStreamInfo;
    final CommandStreamInfo commandStreamInfo;


    public ResourceFederatedDatabase(IDatabaseRegistry registry, DataStreamInfo dataStreamInfo, int... unfilteredDatabases)
    {
        super(registry);
        this.dataStreamInfo = Asserts.checkNotNull(dataStreamInfo, DataStreamInfo.class);
        this.physicalSystem = null;
        this.commandStreamInfo = null;

        for (int dbNum: unfilteredDatabases)
            this.unfilteredDatabases.add(dbNum);
    }


    public ResourceFederatedDatabase(IDatabaseRegistry registry, PhysicalSystem physicalSystem, int... unfilteredDatabases)
    {
        super(registry);
        this.dataStreamInfo = null;
        this.physicalSystem = Asserts.checkNotNull(physicalSystem, PhysicalSystem.class);
        this.commandStreamInfo = null;

        for (int dbNum: unfilteredDatabases)
            this.unfilteredDatabases.add(dbNum);
    }


    public ResourceFederatedDatabase(IDatabaseRegistry registry, CommandStreamInfo commandStreamInfo, DataStreamInfo dataStreamInfo, int... unfilteredDatabases)
    {
        super(registry);
        this.commandStreamInfo = Asserts.checkNotNull(commandStreamInfo, CommandStreamInfo.class);
        this.dataStreamInfo = Asserts.checkNotNull(dataStreamInfo, DataStreamInfo.class);
        this.physicalSystem = null;

        for (int dbNum: unfilteredDatabases)
            this.unfilteredDatabases.add(dbNum);
    }


    public ResourceFederatedDatabase(IDatabaseRegistry registry, CommandStreamInfo commandStreamInfo, DataStreamInfo dataStreamInfo, PhysicalSystem physicalSystem, int... unfilteredDatabases)
    {
        super(registry);
        this.commandStreamInfo = Asserts.checkNotNull(commandStreamInfo, CommandStreamInfo.class);
        this.dataStreamInfo = Asserts.checkNotNull(dataStreamInfo, DataStreamInfo.class);
        this.physicalSystem = Asserts.checkNotNull(physicalSystem, PhysicalSystem.class);
        
        for (int dbNum: unfilteredDatabases)
            this.unfilteredDatabases.add(dbNum);
    }
    
    
//    protected IObsSystemDatabase getResourceSystemDatabase(BigId id)
//    {
//        var db = super.getObsSystemDatabase(id);
//        if (db != null)
//            return new ObsSystemDatabaseView(db, obsFilter, cmdFilter);
//        else
//            return null;
//    }
//
//
//    protected IProcedureDatabase getProcedureDatabase(BigId id)
//    {
//        var db = super.getProcedureDatabase(id);
//        if (db != null)
//            return new ProcedureDatabaseView(db, procFilter);
//        else
//            return null;
//    }
//
//
    @Override
    protected Collection<IObsSystemDatabase> getAllObsDatabases()
    {
        var allDbs = super.getAllObsDatabases();
        return Collections2.transform(allDbs, db -> {
            var dbNum = db.getDatabaseNum();
            if (!unfilteredDatabases.contains(dbNum))
                return new ResourceDatabaseView(db, dataStreamInfo, commandStreamInfo);
            return db;
        });
    }

//
//    @Override
//    protected Collection<IProcedureDatabase> getAllProcDatabases()
//    {
//        var allDbs = super.getAllProcDatabases();
//        return Collections2.transform(allDbs, db -> {
//            var dbNum = db.getDatabaseNum();
//            if (!unfilteredDatabases.contains(dbNum))
//                return new ProcedureDatabaseView(db, procFilter);
//            return db;
//        });
//    }

}
