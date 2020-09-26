/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.obs;

import java.time.Instant;
import org.sensorhub.api.procedure.ProcedureId;
import com.google.common.collect.Range;
import net.opengis.swe.v20.DataComponent;
import net.opengis.swe.v20.DataEncoding;


/**
 * <p>
 * Interface for IDataStreamStore value objects
 * </p>
 *
 * @author Alex Robin
 * @date Mar 23, 2020
 */
public interface IDataStreamInfo
{

    /**
     * @return The identifier of the procedure that generated this data stream
     */
    ProcedureId getProcedureID();


    /**
     * @return The name of the procedure output that is/was the source of
     * this data stream
     */
    String getOutputName();


    /**
     * @return The version of the output record schema used in this data stream
     */
    int getRecordVersion();


    /**
     * @return The data stream record structure
     */
    DataComponent getRecordDescription();


    /**
     * @return The recommended encoding for the data stream
     */
    DataEncoding getRecordEncoding();


    /**
     * @return The range of phenomenon times of all observations that are part
     * of this data stream
     */
    Range<Instant> getPhenomenonTimeRange();


    /**
     * @return The range of result times of all observations that are part
     * of this data stream
     */
    Range<Instant> getResultTimeRange();

}