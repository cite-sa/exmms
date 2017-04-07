package gr.cite.femme.metadata.xpath.elasticsearch;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gr.cite.commons.metadata.analyzer.core.JSONPath;
import gr.cite.femme.exceptions.MetadataIndexException;
import gr.cite.femme.metadata.xpath.core.MetadataSchema;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.entity.ContentType;
import org.apache.http.nio.entity.NStringEntity;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.UnknownHostException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ElasticMetadataIndexDatastoreClient {

	private static final Logger logger = LoggerFactory.getLogger(ElasticMetadataIndexDatastoreClient.class);
	private static final ObjectMapper mapper = new ObjectMapper();
	
	private static final String ELASTICSEARCH_HOST_NAME = "localhost";
	private static final int ELASTICSEARCH_PORT = 9200;
	private static final String ELASTICSEARCH_INDEX_ALIAS = "metadataindex";
	private static final String ELASTICSEARCH_TYPE_PREFIX = "metadataschema_";
	private static final String ELASTICSEARCH_TYPE = "xpath";

	private RestClient client;
	private String indexAlias;

	public ElasticMetadataIndexDatastoreClient() throws UnknownHostException, MetadataIndexException {
		this(ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_HOST_NAME,
				ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_PORT,
				ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_INDEX_ALIAS);
	}

	public ElasticMetadataIndexDatastoreClient(String hostName, int port, String indexAlias) throws UnknownHostException {
		this.indexAlias = indexAlias;
		this.client = RestClient.builder(new HttpHost(hostName, port, "http")).build();
		/*try {
			Response indexExistenceResponse = client.performRequest("HEAD", "/" + this.indexAlias);
			if (indexExistenceResponse.getStatusLine().getStatusCode() == 404) {
				*//*this.client.performRequest("PUT", "/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_INDEX_ALIAS);*//*
				String indexName = createIndex();
				createIndexAliasAssociation(indexName);
			}
			if (getIndexByAlias(this.indexAlias) == null) {
				String indexName = createIndex();
				createIndexAliasAssociation(indexName);
			}
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}*/
	}

	public void close() throws IOException {
		client.close();
	}

	public RestClient get() {
		return client;
	}

	public String getIndexAlias() {
		return indexAlias;
	}

	public String createIndex() throws IOException {
		String indexName = this.indexAlias + "_" + Instant.now().toEpochMilli();
		this.client.performRequest("PUT", "/" + indexName);
		return indexName;
	}

	void createIndex(String indexName) throws MetadataIndexException {
		try {
			this.client.performRequest("PUT", "/" + indexName);
		} catch (IOException e) {
			throw new MetadataIndexException("Index " + indexName + " creation failed", e);
		}
	}

	boolean indexExists(String indexName) throws MetadataIndexException {
		Response indexExistenceResponse;
		try {
			indexExistenceResponse = this.client.performRequest("HEAD", "/" + indexName);
		} catch (IOException e) {
			throw new MetadataIndexException("Index " + indexName + " existence check failed", e);
		}
		return indexExistenceResponse.getStatusLine().getStatusCode() != 404;
	}

	void createIndexAliasAssociation(String indexName) throws MetadataIndexException {
		String indexAliasAssociationRequest = "{" +
				"\"actions\":[" +
					"{" +
						"\"add\":{" +
							"\"index\":\"" + indexName + "\", \"alias\":\"" + this.indexAlias + "\"" +
						"}" +
					"}" +
				"]" +
			"}" ;
		HttpEntity entity = new NStringEntity(indexAliasAssociationRequest, ContentType.APPLICATION_JSON);
		try {
			this.client.performRequest("POST", "/_aliases", Collections.emptyMap(), entity);
		} catch (IOException e) {
			throw new MetadataIndexException("Index " + indexName + " association with alias " + this.indexAlias + " failed", e);
		}
	}

	String swapIndex(String newIndexName) throws MetadataIndexException {
		String oldIndexName = getIndexByAlias(this.indexAlias);
		String indexSwapRequest = "{" +
				"\"actions\":[" +
					"{" +
						"\"add\":{" +
							"\"index\":\"" + newIndexName + "\"," +
							" \"alias\":\"" + this.indexAlias + "\"" +
						"}" +
					"}," +
					"{" +
						"\"remove_index\":{" +
							"\"index\":\"" + oldIndexName + "\"" +
						"}" +
					"}" +
				"]" +
			"}" ;
		HttpEntity entity = new NStringEntity(indexSwapRequest, ContentType.APPLICATION_JSON);
		try {
			this.client.performRequest("POST", "/_aliases", Collections.emptyMap(), entity);
		} catch (IOException e) {
			throw new MetadataIndexException("Metadata index swaping failed: " + oldIndexName + " -> " + newIndexName, e);
		}
		return oldIndexName;
	}

	void swapWithAliasOldIndices(Set<String> newIndices) throws MetadataIndexException {
		List<String> oldIndices = getIndicesByAlias(this.indexAlias);
		String indexSwapRequest = "{\"actions\":[" +
				"{\"add\": {" +
					"\"indices\": [" +
						newIndices.stream().map(newIndex -> "\"" + newIndex + "\"").collect(Collectors.joining(",")) +
					"], \"alias\": \"" + this.indexAlias + "\"}" +
				"}" +
				(oldIndices.size() > 0 ? "," : "") +
				oldIndices.stream().map(oldIndex -> "{" +
					"\"remove_index\":{" +
						"\"index\":\"" + oldIndex + "\"" +
					"}" +
				"}").collect(Collectors.joining(",")) +
			"]" +
		"}";

		logger.info("Swap request: " + indexSwapRequest);

		HttpEntity entity = new NStringEntity(indexSwapRequest, ContentType.APPLICATION_JSON);
		try {
			this.client.performRequest("POST", "/_aliases", Collections.emptyMap(), entity);
		} catch (IOException e) {
			throw new MetadataIndexException("Indices swap failed: old indices: [" + String.join(",", oldIndices) + "], " +
					"new indices: [" + String.join(",", newIndices) + "]", e);
		}
	}

	String getIndexByAlias(String indexAlias) throws MetadataIndexException {
		Response indexNameResponse;
		try {
			indexNameResponse = this.client.performRequest("GET", "/_alias/" + indexAlias);
		} catch (IOException e) {
			throw new MetadataIndexException("Index association retrieval for index alias " + indexAlias + " failed", e);
		}

		Map<String, Object> indexNameResponseMap;
		try {
			indexNameResponseMap = mapper.readValue(indexNameResponse.getEntity().getContent(), new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			throw new MetadataIndexException("Serialization of index association retrieval response for index alias " + indexAlias + " failed", e);
		}

		String aliasIndexName;
		if (indexNameResponseMap == null || indexNameResponseMap.isEmpty()) {
			throw new MetadataIndexException("No index associated with index alias " + indexAlias);
		} else if (indexNameResponseMap.size() > 1) {
			throw new MetadataIndexException("Multiple indices associated with index alias " + indexAlias);
		} else {
			aliasIndexName = new ArrayList<>(indexNameResponseMap.keySet()).stream().sorted().findFirst().get();
		}
		return aliasIndexName;
	}

	List<String> getIndicesByAlias(String indexAlias) throws MetadataIndexException {
		Response indexNameResponse;
		try {
			indexNameResponse = this.client.performRequest("GET", "/*/_alias/" + indexAlias);
		} catch (IOException e) {
			throw new MetadataIndexException("Indices association retrieval for index alias " + indexAlias + " failed", e);
		}

		Map<String, Object> indicesResponseMap;
		try {
			indicesResponseMap = mapper.readValue(indexNameResponse.getEntity().getContent(), new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			throw new MetadataIndexException("Deserialization of index association retrieval response for index alias " + indexAlias + " failed", e);
		}

		return new ArrayList<>(indicesResponseMap.keySet());

		/*List<String> indices = new ArrayList<>();
		if (indicesResponseMap == null || indicesResponseMap.isEmpty()) {
			throw new MetadataIndexException("No index associated with index alias " + indexAlias);
		} else if (indicesResponseMap.size() > 1) {
			throw new MetadataIndexException("Multiple indexes associated with index alias " + indexAlias);
		} else {
			aliasIndexName = return new ArrayList<>(indicesResponseMap.keySet());
		}
		return aliasIndexName;*/
	}

	public String findIndex(String indexName) throws MetadataIndexException {
		Response response;
		try {
			response = this.client.performRequest("GET", "/" + indexName + "*/_settings");
		} catch (IOException e) {
			throw new MetadataIndexException("Index search for " + indexName + " failed", e);
		}
		Map<String, Object> indices;
		try {
			indices = mapper.readValue(response.getEntity().getContent(), new TypeReference<Map<String, Object>>(){});
		} catch (IOException e) {
			throw new MetadataIndexException("Serialization of index association retrieval response for index alias " + indexAlias + " failed", e);
		}
		return indices.keySet().stream().findFirst().orElse(null);
	}

	void createMapping(MetadataSchema metadataSchema, String indexName) throws MetadataIndexException {
		String dynamicTemplate = metadataSchema.getSchema().stream().filter(JSONPath::isArray)
				.map(nestedPath -> "{" +
					"\"arrays_as_nested\":{" +
						"\"path_match\":\"value." + nestedPath.getPath() + "\"," +
						"\"mapping\":{" +
							"\"type\":\"nested\"" +
						"}" +
					"}" +
				"}").collect(Collectors.joining(",", "{\"dynamic_templates\":[", "]}"));
		/*MappingTree mappingTree = new MappingTree();
		String nestedValueMapping = mappingTree.buildMapping(schema.getSchema().stream().filter(JSONPath::isArray).map(JSONPath::getPath).collect(Collectors.toList()));*/

		HttpEntity entity = new NStringEntity(dynamicTemplate, ContentType.APPLICATION_JSON);
		try {
			this.client.performRequest(
					"PUT",
					//"/" + indexName + "/_mapping/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE_PREFIX + schema.getId(),
					"/" + indexName + "/_mapping/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE,
					Collections.emptyMap(),
					entity);
		} catch (IOException e) {
			throw new MetadataIndexException("ElasticSearch dynamic template creation failed");
		}
	}

	boolean mappingExists(String indexName) throws MetadataIndexException {
		Response mapping;
		try {
			mapping = this.client.performRequest(
				"HEAD",
				//"/" + indexName + "/_mapping/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE_PREFIX + metadataSchemaId);
				"/" + indexName + "/_mapping/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE);
		} catch (IOException e) {
			throw new MetadataIndexException("Mapping existence check failed", e);
		}
		return mapping.getStatusLine().getStatusCode() != 404;
	}

	public void insert(String document, String indexName) throws MetadataIndexException {
		try {
			this.client.performRequest(
					"POST",
					//"/" + indexName + "/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE_PREFIX + metadataSchemaId,
					"/" + indexName + "/" + ElasticMetadataIndexDatastoreClient.ELASTICSEARCH_TYPE,
					Collections.emptyMap(),
					new NStringEntity(document, ContentType.APPLICATION_JSON));
		} catch (IOException e) {
			throw new MetadataIndexException("Indexing failed", e);
		}
	}

	public static void main(String[] args) throws MetadataIndexException, UnknownHostException {
		ElasticMetadataIndexDatastoreClient client = new ElasticMetadataIndexDatastoreClient();
		client.findIndex("metadataindex_58d26c229a3192303c6");

	}
}
