/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.

Copyright (C) 2012-2015 Sensia Software LLC. All Rights Reserved.

******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.ui.resources;

import org.sensorhub.api.command.CommandStreamInfo;
import org.sensorhub.api.data.DataStreamInfo;
import org.sensorhub.ui.GenericConfigForm;
import org.sensorhub.ui.data.BaseProperty;
import org.vast.sensorML.SMLBuilders;

import java.util.LinkedHashMap;
import java.util.Map;


@SuppressWarnings({"serial"})
public class ResourceViewConfigForm extends GenericConfigForm
{

    public static final String PROP_RESOURCES = "includeResources";

    @Override
    public Map<String, Class<?>> getPossibleTypes(String propId, BaseProperty<?> prop)
    {

        if (propId.equals(PROP_RESOURCES))
        {
            Map<String, Class<?>> classList = new LinkedHashMap<>();
            classList.put("Physical System", SMLBuilders.PhysicalSystemBuilder.class);
            classList.put("DataStream", DataStreamInfo.Builder.class);
            classList.put("Command Stream", CommandStreamInfo.Builder.class);
//            classList.put("Obs", ObsData.ObsDataBuilder.class);
//            classList.put("Command", CommandInfo.class);
//            classList.put("FOI", CommandInfo.class);
            return classList;
        }
        return super.getPossibleTypes(propId, prop);
    }


}
