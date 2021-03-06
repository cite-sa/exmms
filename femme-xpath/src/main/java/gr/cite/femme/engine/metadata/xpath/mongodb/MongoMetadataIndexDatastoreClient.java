package gr.cite.femme.engine.metadata.xpath.mongodb;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import gr.cite.femme.engine.metadata.xpath.mongodb.codecs.IndexableMetadatumCodecProvider;
import gr.cite.femme.engine.metadata.xpath.mongodb.codecs.MetadataSchemaCodecProvider;
import gr.cite.femme.engine.metadata.xpath.core.MetadataSchema;
import gr.cite.femme.engine.metadata.xpath.core.IndexableMetadatum;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class MongoMetadataIndexDatastoreClient {

	private static final Logger logger = LoggerFactory.getLogger(MongoMetadataIndexDatastoreClient.class);

	private static final String DATABASE_HOST = "localhost";
	private static final int DATABASE_PORT = 27017;
	private static final String DATABASE_NAME = "metadata-schema-db-devel";
	private static final String TRANSFORMED_METADATA_COLLECTION_NAME = "metadata";
	private static final String METADATA_SCHEMAS_COLLECTION_NAME = "metadataSchemas";

	private MongoClient client;
	private MongoDatabase database;
	/*private MongoCollection<MaterializedPathsNode> materializedPaths;*/
	private MongoCollection<IndexableMetadatum> metadataCollection;
	private MongoCollection<MetadataSchema> schemasCollection;

	public MongoMetadataIndexDatastoreClient() {
		this(MongoMetadataIndexDatastoreClient.DATABASE_HOST, MongoMetadataIndexDatastoreClient.DATABASE_PORT, MongoMetadataIndexDatastoreClient.DATABASE_NAME);
	}

	public MongoMetadataIndexDatastoreClient(String host, int port, String name) {
		this(host, port,  name, false);
	}

	public MongoMetadataIndexDatastoreClient(String host, int port, String name, boolean metadataIndexStorage) {
		client = new MongoClient(host, port);
		database = client.getDatabase(name);

		List<CodecProvider> codecProviders = new ArrayList<>();
		codecProviders.add(new MetadataSchemaCodecProvider());
		if (metadataIndexStorage) {
			codecProviders.add(new IndexableMetadatumCodecProvider());
		}
		CodecRegistry codecRegistry = CodecRegistries.fromRegistries(MongoClient.getDefaultCodecRegistry(), CodecRegistries.fromProviders(codecProviders));

		/*materializedPaths = database.getCollection(MATERIALIZED_PATHS_COLLECTION_NAME, MaterializedPathsNode.class).withCodecRegistry(codecRegistry);*/
		if (metadataIndexStorage) {
			metadataCollection = database.getCollection(MongoMetadataIndexDatastoreClient.TRANSFORMED_METADATA_COLLECTION_NAME, IndexableMetadatum.class).withCodecRegistry(codecRegistry);
		}
		schemasCollection = database.getCollection(MongoMetadataIndexDatastoreClient.METADATA_SCHEMAS_COLLECTION_NAME, MetadataSchema.class).withCodecRegistry(codecRegistry);

		createIndexes();

	}

	/*protected MongoCollection<MaterializedPathsNode> getMaterializedPaths() {
		return materializedPaths;
	}*/

	public MongoCollection<IndexableMetadatum> getMetadataCollection() {
		return metadataCollection;
	}

	public MongoCollection<MetadataSchema> getSchemasCollection() {
		return schemasCollection;
	}

	public void close() {
		logger.info("Closing connection to " + client.getAddress());
		client.close();
	}

	private void createIndexes() {
		IndexOptions uniqueIndex = new IndexOptions().unique(true);
		//IndexOptions notUniqueIndex = new IndexOptions().unique(false);

		if (metadataCollection != null) {
			metadataCollection.createIndex(Indexes.ascending("metadatumId"), uniqueIndex);
		}
		schemasCollection.createIndex(Indexes.ascending("checksum"), uniqueIndex);
	}
}
