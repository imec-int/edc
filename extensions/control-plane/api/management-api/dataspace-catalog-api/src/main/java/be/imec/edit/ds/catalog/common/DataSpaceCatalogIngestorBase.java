package be.imec.edit.ds.catalog.common;

import com.linkedin.common.urn.DataPlatformUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import datahub.event.MetadataChangeProposalWrapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract public class DataSpaceCatalogIngestorBase {
  Logger log = LoggerFactory.getLogger(this.getClass().getName());
  public MetadataChangeProposalWrapper _metadataChangeProposalWrapper(RecordTemplate aspect, String entityType, Urn urn) {
    return MetadataChangeProposalWrapper.builder()
        .entityType(entityType)
        .entityUrn(urn)
        .upsert()
        .aspect(aspect)
        .build();
  }
  public DataPlatformUrn _platformUrn(String entityType) throws URISyntaxException {
    return DataPlatformUrn.createFromUrn(DataPlatformUrn.createFromTuple(entityType, "test"));
  }
  public abstract Urn emitMetadataChangeProposal(Asset asset) throws URISyntaxException, IOException, ExecutionException,
                                                            InterruptedException;

}
