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

import java.time.Instant;

/**
 * <p>
 * Key to index observation series.<br/>
 * Each series corresponds to a combination of procedure ID, result time
 * and FOI ID.
 * </p>
 *
 * @author Alex Robin
 * @date Sep 12, 2019
 */
class MVObsSeriesKey
{
    long dataStreamID;
    long foiID;
    Instant resultTime;
    
    
    MVObsSeriesKey(long dataStreamID, long foiID, Instant resultTime)
    {
        this.dataStreamID = dataStreamID;
        this.foiID = foiID;
        this.resultTime = resultTime;
    }
}