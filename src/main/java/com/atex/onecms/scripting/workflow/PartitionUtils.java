package com.atex.onecms.scripting.workflow;

import com.atex.onecms.content.ContentWrite;
import com.atex.onecms.content.ContentWriteBuilder;
import com.atex.onecms.content.metadata.MetadataInfo;
import com.polopoly.metadata.Dimension;
import com.polopoly.metadata.Entity;
import com.polopoly.metadata.Metadata;

import java.util.ArrayList;
import java.util.List;

public final class PartitionUtils {

    private static final String PARTITION_DIMENSION_ID = "dimension.partition";

    private PartitionUtils() { }

    public static <T> ContentWrite<T> changePartition(final ContentWrite<T> cw, final String partitionId) {
        ContentWriteBuilder<T> cwb = new ContentWriteBuilder<>();
        cwb.origin(cw.getId());
        cwb.type(cw.getContentDataType());
        cwb.aspects(cw.getAspects());
        cwb.mainAspectData(cw.getContentData());

        MetadataInfo metadataInfo = cw.getAspect(MetadataInfo.ASPECT_NAME, MetadataInfo.class);
        if (metadataInfo != null) {
            //clear out partition dimension
            Metadata metadata = metadataInfo.getMetadata();
            if (metadata != null) {
                List<Dimension> dimensions = new ArrayList();
                for (Dimension dimension : metadata.getDimensions()) {
                    if (!dimension.getId().equals(PARTITION_DIMENSION_ID)) {
                        dimensions.add(dimension);
                    }
                }
                Dimension paramDimension = createDimensionWithEntity(PARTITION_DIMENSION_ID, partitionId);
                dimensions.add(paramDimension);
                metadata.setDimensions(dimensions);
            }

            cwb.aspect(MetadataInfo.ASPECT_NAME, metadataInfo);
        }

        return cwb.buildUpdate();
    }

    private static Dimension createDimensionWithEntity(final String dimension, final String entity) {
        return new Dimension(dimension, dimension, false, new Entity(entity, entity));
    }
}
