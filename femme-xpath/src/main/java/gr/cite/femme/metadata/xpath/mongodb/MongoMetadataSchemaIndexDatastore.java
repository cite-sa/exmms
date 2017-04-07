package gr.cite.femme.metadata.xpath.mongodb;

import com.mongodb.Block;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import gr.cite.femme.metadata.xpath.core.MetadataSchema;
import gr.cite.femme.metadata.xpath.datastores.api.MetadataSchemaIndexDatastore;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoMetadataSchemaIndexDatastore implements MetadataSchemaIndexDatastore {

	private static final Logger logger = LoggerFactory.getLogger(MongoMetadataSchemaIndexDatastore.class);

	private MongoMetadataIndexDatastoreClient mongoClient;

	private MongoCollection<MetadataSchema> schemasCollection;

	public MongoMetadataSchemaIndexDatastore() {
		this.mongoClient = new MongoMetadataIndexDatastoreClient();
		this.schemasCollection = mongoClient.getSchemasCollection();
	}

	public MongoMetadataSchemaIndexDatastore(String host, int port, String name) {
		this.mongoClient = new MongoMetadataIndexDatastoreClient(host, port, name);
		this.schemasCollection = mongoClient.getSchemasCollection();
	}

	public void close() {
		mongoClient.close();
	}

	@Override
	public void index(MetadataSchema schema) {
		MetadataSchema existingSchema = schemasCollection.find(Filters.eq("checksum", schema.getChecksum())).limit(1).first();
		if (existingSchema == null) {
			schemasCollection.insertOne(schema);
		} else {
			schema.setId(existingSchema.getId());
		}
	}

	/*public List<String> findSuperSchemas(MetadataSchema metadataSchema) {
		metadataSchema.getSchema().removeIf(jsonPath -> !jsonPath.isArray());
		findArrayMetadataIndexPathsAndGroupById().forEach(metadataSchemaWithArrays -> {

		});

		return null;
	}*/

	@Override
	public List<MetadataSchema> findMetadataIndexPath(String regex) {
		List<MetadataSchema> metadataSchemas = new ArrayList<>();
		Bson filterByRegex = Aggregates.match(Filters.regex("schema.path", regex));
		this.schemasCollection.aggregate(Arrays.asList(
				filterByRegex,
				Aggregates.unwind("$schema"),
				filterByRegex,
				//Aggregates.project(new Document().append("_id", 0).append("schema", 1)),
				Aggregates.group(null, Accumulators.addToSet("schema", "$schema"))
		)).into(metadataSchemas);

		return metadataSchemas;
	}

	@Override
	public List<MetadataSchema> findArrayMetadataIndexPaths() {
		List<MetadataSchema> metadataSchemas = new ArrayList<>();

		this.schemasCollection.aggregate(Arrays.asList(
				Aggregates.match(new Document().append("schema.array", true)),
				Aggregates.unwind("$schema"),
				Aggregates.match(new Document().append("schema.array", true)),
				Aggregates.group(null, Accumulators.addToSet("schema", "$schema"))
		)).into(metadataSchemas);

		return metadataSchemas;
	}

	//@Override
	public List<MetadataSchema> findArrayMetadataIndexPathsAndGroupById() {
		List<MetadataSchema> metadataSchemas = new ArrayList<>();

		this.schemasCollection.aggregate(Arrays.asList(
				Aggregates.match(new Document().append("schema.array", true)),
				Aggregates.unwind("$schema"),
				Aggregates.match(new Document().append("schema.array", true)),
				Aggregates.group("$_id", Accumulators.addToSet("schema", "$schema"))
		)).into(metadataSchemas);

		return metadataSchemas;
	}

	@Override
	public List<MetadataSchema> findArrayMetadataIndexPaths(String id) {
		List<MetadataSchema> metadataSchemas = new ArrayList<>();

		this.schemasCollection.aggregate(Arrays.asList(
				Aggregates.match(Filters.and(Filters.eq("_id", new ObjectId(id)), new Document().append("schema.array", true))),
				Aggregates.unwind("$schema"),
				Aggregates.match(new Document().append("schema.array", true)),
				Aggregates.group("$_id", Accumulators.addToSet("schema", "$schema"))
		)).into(metadataSchemas);

		return metadataSchemas;
	}

}


