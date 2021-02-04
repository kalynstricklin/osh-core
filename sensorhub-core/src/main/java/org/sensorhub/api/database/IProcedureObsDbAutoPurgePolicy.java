/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.database;

import org.slf4j.Logger;


/**
 * <p>
 * Interface for policies used to automatically purge historical data.<br/>
 * The criteria used for purging is up to the implementation (e.g. time of
 * oldest records, total number of records, size of the DB file, etc...)
 * </p>
 *
 * @author Alex Robin
 * @date Sep 23, 2019
 */
public interface IProcedureObsDbAutoPurgePolicy
{    
        
    /**
     * Implementation of this method executes whatever actions are necessary
     * for this aging policy
     * @param db
     * @param log
     */
    public void trimStorage(IProcedureObsDatabase db, Logger log); 
}
