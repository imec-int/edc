package be.imec.edit.ds.catalog;

import be.imec.edit.ds.catalog.common.DataSpaceCatalogIngestorBase;
import com.linkedin.common.urn.Urn;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DomainEntityIngestor extends DataSpaceCatalogIngestorBase {
  Logger log = LoggerFactory.getLogger(this.getClass().getName());
  @Override
  public Urn emitMetadataChangeProposal(Asset asset)
      throws URISyntaxException, IOException, ExecutionException, InterruptedException {
    return null;
  }
}
