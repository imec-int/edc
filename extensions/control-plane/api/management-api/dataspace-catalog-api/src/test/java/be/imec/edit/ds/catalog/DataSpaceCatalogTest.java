package be.imec.edit.ds.catalog;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.eclipse.edc.jsonld.util.JacksonJsonLd;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToAssetTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonObjectToDataAddressTransformer;
import org.eclipse.edc.core.transform.transformer.to.JsonValueToGenericTypeTransformer;
import org.eclipse.edc.spi.result.Result;
import org.eclipse.edc.spi.types.domain.asset.Asset;
import org.eclipse.edc.transform.spi.TypeTransformerRegistry;
import org.eclipse.edc.core.transform.TypeTransformerRegistryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.*;


public class DataSpaceCatalogTest extends DatasetEntityIngestor {
  private final ObjectMapper objectMapper = JacksonJsonLd.createObjectMapper();
  Logger log = LoggerFactory.getLogger(this.getClass().getName());
  //private final TypeTransformerRegistry transformerRegistry = mock(TypeTransformerRegistry.class);
  private final TypeTransformerRegistry transformer = new TypeTransformerRegistryImpl();


  @BeforeEach
  void setUp() {
    transformer.register(new JsonObjectToAssetTransformer());
    transformer.register(new JsonValueToGenericTypeTransformer(objectMapper));
    transformer.register(new JsonObjectToDataAddressTransformer());
  }

  @Test
  void ingestDataSetEntity() throws URISyntaxException, IOException {
    ClassLoader classloader = Thread.currentThread().getContextClassLoader();
    URL resource = classloader.getResource("asset.json");

    try (JsonReader jsonReader = Json.createReader(new StringReader(Files.readString(Paths.get(resource.toURI()))))) {

      JsonObject readObject = jsonReader.readObject();

      //Transform the json object to Asset object
      Result<Asset> asset = transformer.transform(readObject, Asset.class);
      emitMetadataChangeProposal(asset.getContent());
    } catch (ExecutionException | InterruptedException e) {
      e.printStackTrace();
    }
  }
}
