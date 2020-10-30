/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.feature;

import org.sensorhub.api.datastore.DataStoreException;
import org.sensorhub.api.datastore.TemporalFilter;
import org.sensorhub.api.datastore.ValueField;
import org.sensorhub.api.feature.FeatureFilterBase.FeatureFilterBaseBuilder;
import org.sensorhub.api.feature.IFeatureStoreBase.FeatureField;
import org.sensorhub.api.resource.IResourceStore;
import org.vast.ogc.gml.IFeature;
import org.vast.util.Bbox;


/**
 * <p>
 * Base interface for data stores containing objects derived from IFeature.
 * </p>
 * 
 * @param <K> Key type
 * @param <V> Value type
 * @param <VF> Value field enum type
 * @param <F> Filter type
 *
 * @author Alex Robin
 * @date Mar 19, 2018
 */
public interface IFeatureStoreBase<V extends IFeature, VF extends FeatureField, F extends FeatureFilterBase<? super V>> extends IResourceStore<FeatureKey, V, VF, F>
{
    
    public static class FeatureField extends ValueField
    {
        public static final FeatureField UID = new FeatureField("UID");
        public static final FeatureField NAME = new FeatureField("name");
        public static final FeatureField DESCRIPTION = new FeatureField("description");
        public static final FeatureField GEOMETRY = new FeatureField("geometry");
        public static final FeatureField VALID_TIME = new FeatureField("validTime");
        public static final FeatureField PROPERTIES = new FeatureField("properties");
        
        public FeatureField(String name)
        {
            super(name);
        }
    }
    
    
    @Override
    public FeatureFilterBaseBuilder<?,?,F> filterBuilder();
    
    
    /**
     * Add a new version of an existing feature
     * @param feature The feature object to be stored
     * @return The key associated with the new feature
     * @throws DataStoreException if the parent doesn't exist in this store
     */
    public FeatureKey addVersion(V feature);
    
    
    /**
     * Add a new feature to the store, generating a new key for it, and
     * also add it as a child of the specified parent
     * @param parentID Internal ID of parent feature
     * @param value New feature object
     * @return The newly allocated key (internal ID)
     * @throws DataStoreException if the parent doesn't exist in this store
     */
    FeatureKey add(long parentID, V value);
    
    
    /**
     * Helper method to retrieve the full key corresponding to the current version
     * of the feature with the given unique ID
     * @param uid The feature unique ID
     * @return The feature key or null if no feature with the given ID was found
     */
    public default FeatureKey getCurrentVersionKey(String uid)
    {
        return selectKeys(filterBuilder()
                .withUniqueIDs(uid)
                .withCurrentVersion()
                .build())
            .findFirst()
            .orElse(null);
    }
    
    
    /**
     * Helper method to retrieve the full key corresponding to the current version
     * of the feature with the given internal ID
     * @param internalID The feature internal ID
     * @return The feature key or null if no feature with the given ID was found
     */
    public default FeatureKey getCurrentVersionKey(long internalID)
    {
        return selectKeys(filterBuilder()
                .withInternalIDs(internalID)
                .withCurrentVersion()
                .build())
            .findFirst()
            .orElse(null);
    }
    
    
    /**
     * Helper method to retrieve the entry corresponding to the current version
     * of the feature with the given unique ID
     * @param uid The feature unique ID
     * @return The feature entry or null if no feature with the given ID was found
     */
    public default Entry<FeatureKey, V> getCurrentVersionEntry(String uid)
    {
        return selectEntries(filterBuilder()
                .withUniqueIDs(uid)
                .withCurrentVersion()
                .build())
            .findFirst()
            .orElse(null);            
    }
    
    
    /**
     * Helper method to retrieve the entry corresponding to the current version
     * of the feature with the given internal ID
     * @param internalID The feature internal ID
     * @return The feature entry or null if no feature with the given ID was found
     */
    public default Entry<FeatureKey, V> getCurrentVersionEntry(long internalID)
    {
        return selectEntries(filterBuilder()
                .withInternalIDs(internalID)
                .withCurrentVersion()
                .build())
            .findFirst()
            .orElse(null);            
    }
    
    
    /**
     * Helper method to retrieve the current version of the feature with the given
     * unique ID
     * @param uid The feature unique ID
     * @return The feature representation or null if no feature with the
     * given ID was found
     */
    public default V getCurrentVersion(String uid)
    {
        Entry<FeatureKey, V> e = getCurrentVersionEntry(uid);
        return e != null ? e.getValue() : null;
    }
    
    
    /**
     * Helper method to retrieve the current version of the feature with the given
     * internal ID
     * @param internalID The feature internal ID
     * @return The feature representation or null if no feature with the
     * given ID was found
     */
    public default V getCurrentVersion(long internalID)
    {
        Entry<FeatureKey, V> e = getCurrentVersionEntry(internalID);
        return e != null ? e.getValue() : null;
    }


    /**
     * Checks if store contains a feature with the given unique ID
     * @param uid The feature unique ID
     * @return True if a procedure with the given ID exists, false otherwise
     */
    public default boolean contains(String uid)
    {
        return getCurrentVersionKey(uid) != null;
    }
    
    
    /**
     * Helper method to remove all versions of the feature with the given UID
     * @param uid The feature unique ID
     * @return The feature key of the current version or null if nothing was removed
     */
    public default FeatureKey remove(String uid)
    {
        var fk = getCurrentVersionKey(uid);
        if (fk == null)
            return null;
        
        long count = removeEntries(filterBuilder()
            .withUniqueIDs(uid)
            .build());
        
        return count > 0 ? fk : null;
    }
       
    
    /**
     * @return Total number of distinct features contained in this data store.<br/>
     * Note that this is different from {@link #getNumRecords()} because the
     * later will count one entry for each version of the same feature while this
     * method will count the feature only once.
     */
    public default long getNumFeatures()
    {
        return countMatchingEntries(filterBuilder()
            .withValidTime(new TemporalFilter.Builder()
                .withLatestTime()
                .build())
            .build());
    }


    /**
     * @return Overall bounding rectangle of all features contained in this
     * data store
     */
    public Bbox getFeaturesBbox();
    
    
    /**
     * Gets a set of more precise bounding rectangles for regions where features
     * contained in this data store are located
     * @param filter Spatial filter to limit the search
     * @return Stream of bounding boxes
     *
    public Stream<Bbox> getFeatureClusters(SpatialFilter filter);*/
    
}
