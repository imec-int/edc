package be.imec.edit.ds.catalog.common;

import com.linkedin.common.urn.DataPlatformUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.RecordTemplate;
import datahub.client.rest.RestEmitter;
import datahub.event.MetadataChangeProposalWrapper;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


abstract public class DataSpaceCatalogIngestorBase {
  Logger log = LoggerFactory.getLogger(this.getClass().getName());
  /***
  * To create an emitter that is pushing to datahub instance that is remote (though an IP or url), see RestEmitter class. It has examples for creating emitter with external urls. An example is shown below commented out
   *
  * */
  protected RestEmitter emitter = RestEmitter.createWithDefaults();
  //protected RestEmitter emitter = RestEmitter.create(b -> b.server("http://localhost:8080")); // todo: replace the `localhost:8080` with ip address or address of the dathub gms.
  /***
   * Method to build change proposal for any entity. aspect represents an aspect, e.g. dataSetProperties or Ownership Aspect of dataset, entityType is e.g. dathahub entity `dataset` etc.
   * And the urn is `datahub` style urn.
   * */
  public MetadataChangeProposalWrapper _metadataChangeProposalWrapper(RecordTemplate aspect, String entityType, Urn urn) {
    return MetadataChangeProposalWrapper.builder()
        .entityType(entityType)
        .entityUrn(urn)
        .upsert()
        .aspect(aspect)
        .build();
  }
  /**
   * At the moment, edc connectors are experimental. The data platforms (bigquery, snowflake, hudi etc.) are unknown and not provided, so we use `imec-edc`. But we can
   * Use `conf` files to configure this.
   * */
  public DataPlatformUrn _platformUrn(String entityType) throws URISyntaxException {
    return DataPlatformUrn.createFromUrn(DataPlatformUrn.createFromTuple(entityType, "IMEC_EDC_PLATFORM"));
  }

  /***
   * A method used by subclasses to implement entity specific changeproposals and emit to datahub (data space catalog)
  * */
  public abstract Urn emitMetadataChangeProposal(Asset asset) throws URISyntaxException, IOException, ExecutionException,
                                                            InterruptedException;

}
