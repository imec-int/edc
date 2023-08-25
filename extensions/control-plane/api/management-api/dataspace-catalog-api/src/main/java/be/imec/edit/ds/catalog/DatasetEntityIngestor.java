package be.imec.edit.ds.catalog;

import be.imec.edit.ds.catalog.common.DataSpaceCatalogIngestorBase;
import com.linkedin.common.FabricType;
import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.dataset.DatasetProperties;
import com.linkedin.dataset.EditableDatasetProperties;
import com.linkedin.schema.SchemaField;
import com.linkedin.schema.SchemaFieldArray;
import com.linkedin.schema.SchemaMetadata;
import datahub.client.MetadataWriteResponse;
import datahub.client.rest.RestEmitter;
import datahub.shaded.org.apache.kafka.common.errors.ApiException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/***
 * The class to ingest metadata about data asset (dataset) to data space catalog.
 *
* */

public class DatasetEntityIngestor extends DataSpaceCatalogIngestorBase {
  Logger log = LoggerFactory.getLogger(this.getClass().getName());


  final String entityType = "dataset";
/***
 * editableDatasetProperties aspect of dataset entity - see details on datahub documentation for dataset entity aspects
 * */
  private DatasetProperties _datasetProperties(Asset asset) {
    var createdAt=new com.linkedin.common.TimeStamp();
    createdAt.setTime(asset.getCreatedAt());
    return new DatasetProperties()
        .setDescription(asset.getDescription())
        .setCreated(createdAt);

  }

  /***
 * editableDatasetProperties aspect of dataset entity
 * */
  private EditableDatasetProperties _editableDatasetProperties(Asset asset) {
    return new EditableDatasetProperties();
  }
  /***
   * schemaMetadata aspect of dataset entity
   * */
  public SchemaMetadata _schemaMetadata(Asset asset) { //todo: This should not be handcrafted, rather should come (if any) from an avro
    SchemaFieldArray fields = new SchemaFieldArray();
    fields.add(new SchemaField());
    return new SchemaMetadata().setFields(fields);
  }

  /***
   * Returns datahub style urn for an asset - includes `test` as platform, and includes EDC asset name and id within the urn.
   * The FabricType is the environment type such as Dev, Prod, etc.
   * */
  public Urn _urn(Asset asset) throws URISyntaxException {
    return new DatasetUrn(_platformUrn(entityType), asset.getName()+asset.getId(), FabricType.DEV);
  }

  /***
   * This method emits whole dataset, with all aspects (defined within) to dataspace catalog. To only ingest/emit a single aspect, see specs.
   * In this method, we first create a dataset with a single aspect - datasetProperties Aspect. Then, we create other aspects such as
   * schemaMetadata and editableProperties aspects, and ingest them in parallel.
   * Usually it can be done sequentially, but this is to show that, if an entity already exists, then aspects can be pushed in parallel as well.
   * Since the calls are asynchronous, the datahub api at the receiving end will respond asynchronously.
   * */
  public Urn emitMetadataChangeProposal(Asset asset)
      throws URISyntaxException, IOException, ExecutionException, InterruptedException {
    Urn datasetUrn = _urn(asset);
    log.info("Pushing dataset to data space catalog");
    Future<MetadataWriteResponse> responseFuture = emitter.emit(_metadataChangeProposalWrapper(_datasetProperties(asset), entityType, datasetUrn));
    if(responseFuture.isDone() && responseFuture.get().isSuccess()){
      Future<MetadataWriteResponse> editablePropsFut = emitter.emit(_metadataChangeProposalWrapper(_editableDatasetProperties(asset), entityType, datasetUrn));
      Future<MetadataWriteResponse> schemaPropsFut = emitter.emit(_metadataChangeProposalWrapper(_schemaMetadata(asset), entityType, datasetUrn));
      int numThreads = 2; // Number of threads in the thread pool
      ExecutorService executor = Executors.newFixedThreadPool(numThreads);
      List<Future<MetadataWriteResponse>> futures = List.of(editablePropsFut, schemaPropsFut);
      List<Future<Future<MetadataWriteResponse>>> responses = new ArrayList<>();
      for (Future<MetadataWriteResponse> future: futures){
        Callable<Future<MetadataWriteResponse>> callable = () -> future;
        Future<Future<MetadataWriteResponse>> resp = executor.submit(callable);
        responses.add(resp);
      }
      for (Future<Future<MetadataWriteResponse>> response: responses){
        try{
          Future<MetadataWriteResponse> fresp = response.get();
          if(response.isDone() && fresp.isDone() && fresp.get().isSuccess()){
            log.info("Success ingesting "+ fresp.get().getResponseContent());
          }
          else {
            log.error(fresp.get().getResponseContent(), ApiException.class);
          }
        }
        catch (Exception e) {
          e.printStackTrace();
          log.error(e.getMessage());
          return null;
        }
      }
    }
    return datasetUrn;
  }


}
