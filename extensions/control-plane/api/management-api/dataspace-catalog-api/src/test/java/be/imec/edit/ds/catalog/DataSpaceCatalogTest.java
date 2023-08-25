package be.imec.edit.ds.catalog;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;


public class DataSpaceCatalogTest extends DatasetEntityIngestor {
  Logger log = LoggerFactory.getLogger(this.getClass().getName());
  private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);

  @Test
  void ingestDataSetEntity() throws URISyntaxException, IOException {
    URL resource = null; // getClass().getClassLoader().getResource("asset.json");
    try (JsonReader jsonReader = Json.createReader(new StringReader(Files.readString(Paths.get(resource.toURI()))))) {

      JsonObject readObject = jsonReader.readObject();
      Result<Asset> asset = transformerRegistry.transform(readObject, Asset.class);
      emitMetadataChangeProposal(asset.getContent());
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
