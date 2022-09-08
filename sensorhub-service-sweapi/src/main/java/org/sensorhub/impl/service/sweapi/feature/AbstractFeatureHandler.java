/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2020 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.impl.service.sweapi.feature;

import java.time.Instant;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.xml.namespace.QName;
import org.sensorhub.api.common.BigId;
import org.sensorhub.api.common.IdEncoder;
import org.sensorhub.api.common.IdEncoders;
import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.FeatureKey;
import org.sensorhub.api.datastore.feature.IFeatureStoreBase;
import org.sensorhub.api.datastore.feature.FeatureFilterBase.FeatureFilterBaseBuilder;
import org.sensorhub.impl.service.sweapi.InvalidRequestException;
import org.sensorhub.impl.service.sweapi.ResourceParseException;
import org.sensorhub.impl.service.sweapi.RestApiServlet.ResourcePermissions;
import org.sensorhub.impl.service.sweapi.resource.RequestContext;
import org.sensorhub.impl.service.sweapi.resource.ResourceHandler;
import org.sensorhub.impl.service.sweapi.resource.RequestContext.ResourceRef;
import org.vast.ogc.gml.IFeature;
import com.google.common.base.Strings;


public abstract class AbstractFeatureHandler<
    V extends IFeature,
    F extends FeatureFilterBase<? super V>,
    B extends FeatureFilterBaseBuilder<B,? super V,F>,
    S extends IFeatureStoreBase<V,?,F>> extends ResourceHandler<FeatureKey, V, F, B, S>
{
    static final int MIN_UID_CHARS = 12;
    
    
    protected AbstractFeatureHandler(S dataStore, IdEncoder idEncoder, IdEncoders allIdEncoders, ResourcePermissions permissions)
    {
        super(dataStore, idEncoder, allIdEncoders, permissions);
    }


    @Override
    protected void buildFilter(final ResourceRef parent, final Map<String, String[]> queryParams, final B builder) throws InvalidRequestException
    {
        super.buildFilter(parent, queryParams, builder);
        
        // uid param
        var featureUIDs = parseMultiValuesArg("uid", queryParams);
        if (featureUIDs != null && !featureUIDs.isEmpty())
            builder.withUniqueIDs(featureUIDs);
                
        // validTime param
        var validTime = parseTimeStampArg("validTime", queryParams);
        if (validTime != null)
            builder.withValidTime(validTime);
        //else
        //    builder.withCurrentVersion();
        
        // use opensearch bbox param to filter spatially
        var bbox = parseBboxArg("bbox", queryParams);
        if (bbox != null)
            builder.withLocationWithin(bbox);
        
        // geom param
        var geom = parseGeomArg("geom", queryParams);
        if (geom != null)
            builder.withLocationIntersecting(geom);
        
        // featureType
        var fType = getSingleParam("featureType", queryParams);
        if (!Strings.isNullOrEmpty(fType))
            builder.withValuePredicate(f -> f.getType() != null && f.getType().equals(fType));
        
        buildPredicate(queryParams, builder);
    }
    
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected void buildPredicate(final Map<String, String[]> queryParams, final B builder) throws InvalidRequestException
    {
        // property values
        Predicate<V> predicate = null;
        for (var e: queryParams.entrySet())
        {
            if (e.getKey().startsWith("p:"))
                predicate = addPredicate(e, predicate);
        }
        if (predicate != null)
            builder.withValuePredicate((Predicate)predicate);
    }
    
    
    Predicate<V> addPredicate(final Map.Entry<String, String[]> param, Predicate<V> prevPredicate)
    {
        if (param.getValue().length == 1)
        {
            var propName = param.getKey().substring(param.getKey().indexOf(':')+1);
            var valueStr = param.getValue()[0];
            var pattern =  Pattern.compile(valueStr.replace("*", ".*").replace("?", "."));
            Predicate<V> p;
            
            if ("name".equals(propName))
            {
                p = f -> f.getName() != null && pattern.matcher(f.getName()).matches();
            }
            else if ("description".equals(propName))
            {
                p = f -> f.getDescription() != null && pattern.matcher(f.getDescription()).matches();
            }
            else
            {
                var propQname = new QName(propName);
                p = f -> {
                    var val = f.getProperties().get(propQname);
                    if (val instanceof String)
                        return pattern.matcher((String)val).matches();
                    else if (val instanceof Number)
                        return ((Number)val).equals(Double.parseDouble(valueStr));
                    return false;
                };
            }
            
            return prevPredicate != null ? prevPredicate.and(p) : p;
        }
        
        return null;
    }


    @Override
    protected void validate(V resource) throws ResourceParseException
    {
        // check UID
        var uid = resource.getUniqueIdentifier();
        if (Strings.isNullOrEmpty(uid))
            throw new ResourceParseException("Missing feature UID");
        if (uid.length() < MIN_UID_CHARS)
            throw new ResourceParseException("Feature UID should be at least " + MIN_UID_CHARS + " characters");
        
        // check name
        var name = resource.getName();
        if (Strings.isNullOrEmpty(name))
            throw new ResourceParseException("Missing feature name");
    }
    
    
    @Override
    protected Stream<Entry<FeatureKey, V>> postProcessResultList(Stream<Entry<FeatureKey, V>> results, F filter)
    {
        if (filter.getValidTime() == null)
            return FeatureUtils.keepOnlyClosestToNow(results);
        else
            return results;
    }


    @Override
    protected FeatureKey getKey(BigId internalID)
    {
        //return dataStore.getCurrentVersionKey(internalID);
        return FeatureUtils.getClosestKeyToNow(dataStore, internalID);
    }
    
    
    protected FeatureKey getKey(BigId internalID, Instant validTime)
    {
        if (validTime == null)
            return getKey(internalID);
        
        return dataStore.selectKeys(dataStore.filterBuilder()
                .withInternalIDs(internalID)
                .validAtTime(validTime)
                .build())
            .findFirst()
            .orElse(null);
    }
    
    
    @Override
    protected FeatureKey addEntry(final RequestContext ctx, final V f) throws DataStoreException
    {        
        return dataStore.add(f);
    }
    
    
    @Override
    protected boolean updateEntry(final RequestContext ctx, final FeatureKey key, final V f) throws DataStoreException
    {        
        return dataStore.put(key, f) != null;
    }
    
    
    @Override
    protected boolean deleteEntry(final RequestContext ctx, final FeatureKey key) throws DataStoreException
    {
        // remove all versions of feature with given ID
        return dataStore.removeEntries(dataStore.filterBuilder()
            .withInternalIDs(key.getInternalID())
            .build()) > 0;
    }
}
