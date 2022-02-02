/***************************** BEGIN LICENSE BLOCK ***************************

The contents of this file are subject to the Mozilla Public License, v. 2.0.
If a copy of the MPL was not distributed with this file, You can obtain one
at http://mozilla.org/MPL/2.0/.

Software distributed under the License is distributed on an "AS IS" basis,
WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
for the specific language governing rights and limitations under the License.
 
Copyright (C) 2019 Sensia Software LLC. All Rights Reserved.
 
******************************* END LICENSE BLOCK ***************************/

package org.sensorhub.api.datastore.system;

import org.sensorhub.api.datastore.EmptyFilterIntersection;
import org.sensorhub.api.datastore.feature.FeatureFilterBase;
import org.sensorhub.api.datastore.feature.FoiFilter;
import org.sensorhub.api.datastore.obs.DataStreamFilter;
import org.sensorhub.api.resource.ResourceFilter;
import org.vast.ogc.om.IProcedure;


/**
 * <p>
 * Immutable filter object for system resources<br/>
 * There is an implicit AND between all filter parameters
 * </p>
 *
 * @author Alex Robin
 * @date Apr 2, 2018
 */
public class SystemFilter extends FeatureFilterBase<IProcedure>
{
    protected SystemFilter parentFilter;
    protected DataStreamFilter dataStreamFilter;

    
    /*
     * this class can only be instantiated using builder
     */
    protected SystemFilter() {}
    
    
    public SystemFilter getParentFilter()
    {
        return parentFilter;
    }
    
    
    public DataStreamFilter getDataStreamFilter()
    {
        return dataStreamFilter;
    }
    
    
    /**
     * Computes the intersection (logical AND) between this filter and another filter of the same kind
     * @param filter The other filter to AND with
     * @return The new composite filter
     * @throws EmptyFilterIntersection if the intersection doesn't exist
     */
    @Override
    public SystemFilter intersect(ResourceFilter<IProcedure> filter) throws EmptyFilterIntersection
    {
        if (filter == null)
            return this;
        
        return intersect((SystemFilter)filter, new Builder()).build();
    }
    
    
    protected <B extends SystemFilterBuilder<B, SystemFilter>> B intersect(SystemFilter otherFilter, B builder) throws EmptyFilterIntersection
    {
        super.intersect(otherFilter, builder);
        
        var parentFilter = this.parentFilter != null ? this.parentFilter.intersect(otherFilter.parentFilter) : otherFilter.parentFilter;
        if (parentFilter != null)
            builder.withParents(parentFilter);
        
        var dataStreamFilter = this.dataStreamFilter != null ? this.dataStreamFilter.intersect(otherFilter.dataStreamFilter) : otherFilter.dataStreamFilter;
        if (dataStreamFilter != null)
            builder.withDataStreams(dataStreamFilter);
        
        return builder;
    }
    
    
    /**
     * Deep clone this filter
     */
    public SystemFilter clone()
    {
        return Builder.from(this).build();
    }
    
    
    /*
     * Builder
     */
    public static class Builder extends SystemFilterBuilder<Builder, SystemFilter>
    {
        public Builder()
        {
            super(new SystemFilter());
        }
        
        /**
         * Builds a new filter using the provided filter as a base
         * @param base Filter used as base
         * @return The new builder
         */
        public static Builder from(SystemFilter base)
        {
            return new Builder().copyFrom(base);
        }
    }
    
    
    /*
     * Nested builder for use within another builder
     */
    public static abstract class NestedBuilder<B> extends SystemFilterBuilder<NestedBuilder<B>, SystemFilter>
    {
        B parent;
        
        public NestedBuilder(B parent)
        {
            super(new SystemFilter());
            this.parent = parent;
        }
                
        public abstract B done();
    }
    
    
    @SuppressWarnings("unchecked")
    public static abstract class SystemFilterBuilder<
            B extends SystemFilterBuilder<B, F>,
            F extends SystemFilter>
        extends FeatureFilterBaseBuilder<B, IProcedure, F>
    {        
        
        protected SystemFilterBuilder(F instance)
        {
            super(instance);
        }
                
        
        @Override
        public B copyFrom(F base)
        {
            super.copyFrom(base);
            instance.parentFilter = base.parentFilter;
            instance.dataStreamFilter = base.dataStreamFilter;
            return (B)this;
        }
        
        
        /**
         * Select only subsystems belonging to the matching parent systems
         * @param filter Parent system filter
         * @return This builder for chaining
         */
        public B withParents(SystemFilter filter)
        {
            instance.parentFilter = filter;
            return (B)this;
        }

        
        /**
         * Keep only subsystems belonging to the matching parent systems.<br/>
         * Call done() on the nested builder to go back to main builder.
         * @return The {@link SystemFilter} builder for chaining
         */
        public SystemFilter.NestedBuilder<B> withParents()
        {
            return new SystemFilter.NestedBuilder<B>((B)this) {
                @Override
                public B done()
                {
                    SystemFilterBuilder.this.withParents(build());
                    return (B)SystemFilterBuilder.this;
                }                
            };
        }
        
        
        /**
         * Select only systems belonging to the parent systems with
         * specific internal IDs
         * @param ids List of IDs of parent systems
         * @return This builder for chaining
         */
        public B withParents(long... ids)
        {
            return withParents()
                .withInternalIDs(ids)
                .done();
        }
        
        
        /**
         * Select only systems belonging to the parent systems with
         * specific unique IDs
         * @param uids List of UIDs of parent systems
         * @return This builder for chaining
         */
        public B withParents(String... uids)
        {
            return withParents()
                .withUniqueIDs(uids)
                .done();
        }
        
        
        /**
         * Select only systems that have no parent
         * @return This builder for chaining
         */
        public B withNoParent()
        {
            return withParents()
                .withInternalIDs(0)
                .done();
        }
        
        
        /**
         * Select only systems with data streams matching the filter
         * @param filter Data stream filter
         * @return This builder for chaining
         */
        public B withDataStreams(DataStreamFilter filter)
        {
            instance.dataStreamFilter = filter;
            return (B)this;
        }

        
        /**
         * Keep only systems from data streams matching the filter.<br/>
         * Call done() on the nested builder to go back to main builder.
         * @return The {@link DataStreamFilter} builder for chaining
         */
        public DataStreamFilter.NestedBuilder<B> withDataStreams()
        {
            return new DataStreamFilter.NestedBuilder<B>((B)this) {
                @Override
                public B done()
                {
                    SystemFilterBuilder.this.withDataStreams(build());
                    return (B)SystemFilterBuilder.this;
                }                
            };
        }
        

        /**
         * Select only systems that produced observations of features of interest
         * matching the filter
         * @param filter Features of interest filter
         * @return This builder for chaining
         */
        public B withFois(FoiFilter filter)
        {
            return withDataStreams(new DataStreamFilter.Builder()
                .withFois(filter)
                .build());
        }

        
        /**
         * Select only systems that produced observations of features of interest
         * matching the filter.<br/>
         * Call done() on the nested builder to go back to main builder.
         * @return The {@link FoiFilter} builder for chaining
         */
        public FoiFilter.NestedBuilder<B> withFois()
        {
            return new FoiFilter.NestedBuilder<B>((B)this) {
                @Override
                public B done()
                {
                    SystemFilterBuilder.this.withFois(build());
                    return (B)SystemFilterBuilder.this;
                }
            };
        }
    }
}