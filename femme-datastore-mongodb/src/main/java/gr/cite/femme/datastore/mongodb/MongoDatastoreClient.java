package gr.cite.femme.datastore.mongodb;

import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;

import gr.cite.femme.core.Collection;
import gr.cite.femme.core.DataElement;
import gr.cite.femme.core.Metadatum;
import gr.cite.femme.datastore.mongodb.codecs.ElementCodecProvider;
import gr.cite.femme.datastore.mongodb.codecs.MetadatumCodecProvider;
import gr.cite.femme.datastore.mongodb.codecs.SystemicMetadataCodecProvider;

public class MongoDatastoreClient {
	private static final String DATABASE_HOST = "localhost:27017";
	private static final String DATABASE_NAME = "femme-db";
	private static final String COLLECTIONS_COLLECTION_NAME = "collections";
	private static final String DATA_ELEMENTS_COLLECTION_NAME = "dataElements";
	private static final String METADATA_COLLECTION_NAME = "metadata";
	private static final String METADATA_BUCKET_NAME = "metadata";

	private MongoClient client;
	private MongoDatabase database;
	private MongoCollection<Collection> collections;
	private MongoCollection<DataElement> dataElements;
	private MongoCollection<Metadatum> metadata;
	private GridFSBucket metadataGridFS;
	
	public MongoDatastoreClient() {
		client = new MongoClient(DATABASE_HOST);
		database = client.getDatabase(DATABASE_NAME);

		CodecRegistry codecRegistry = CodecRegistries
				.fromRegistries(
						CodecRegistries.fromProviders(
								new ElementCodecProvider(),
								new MetadatumCodecProvider(),
								new SystemicMetadataCodecProvider()),
				MongoClient.getDefaultCodecRegistry());

		collections = database.getCollection(COLLECTIONS_COLLECTION_NAME, Collection.class).withCodecRegistry(codecRegistry);
		dataElements = database.getCollection(DATA_ELEMENTS_COLLECTION_NAME, DataElement.class).withCodecRegistry(codecRegistry);
		metadata = database.getCollection(METADATA_COLLECTION_NAME, Metadatum.class).withCodecRegistry(codecRegistry);
		metadataGridFS = GridFSBuckets.create(database, METADATA_BUCKET_NAME);
	}
	
	public MongoDatastoreClient(MongoDatabase db) {
		database = db;

		CodecRegistry codecRegistry = CodecRegistries
				.fromRegistries(
						CodecRegistries.fromProviders(
								new ElementCodecProvider(),
								new MetadatumCodecProvider(),
								new SystemicMetadataCodecProvider()),
				MongoClient.getDefaultCodecRegistry());

		collections = database.getCollection(COLLECTIONS_COLLECTION_NAME, Collection.class).withCodecRegistry(codecRegistry);
		dataElements = database.getCollection(DATA_ELEMENTS_COLLECTION_NAME, DataElement.class).withCodecRegistry(codecRegistry);
		metadata = database.getCollection(METADATA_COLLECTION_NAME, Metadatum.class).withCodecRegistry(codecRegistry);
		metadataGridFS = GridFSBuckets.create(database, METADATA_BUCKET_NAME);
	}

	public MongoCollection<Collection> getCollections() {
		return collections;
	}
	
	public MongoCollection<DataElement> getDataElements() {
		return dataElements;
	}
	
	public MongoCollection<Metadatum> getMetadata() {
		return metadata;
	}
	
	public GridFSBucket getMetadataGridFS() {
		return metadataGridFS;
	}

	public void close() {
		client.close();
	}
}