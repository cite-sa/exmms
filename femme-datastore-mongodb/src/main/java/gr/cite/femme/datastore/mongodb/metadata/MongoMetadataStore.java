package gr.cite.femme.datastore.mongodb.metadata;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.gridfs.GridFSBucket;

import gr.cite.femme.client.api.MetadataIndexClient;
import gr.cite.femme.datastore.api.MetadataStore;
import gr.cite.femme.datastore.mongodb.MongoDatastoreClient;
import gr.cite.femme.datastore.mongodb.cache.XPathCacheManager;
import gr.cite.femme.datastore.mongodb.codecs.MetadatumJson;
import gr.cite.femme.exceptions.MetadataStoreException;
import gr.cite.femme.index.client.FemmeIndexClient;
import gr.cite.femme.model.Element;
import gr.cite.femme.model.Metadatum;
import gr.cite.femme.model.MetadatumXPathCache;

public class MongoMetadataStore implements MetadataStore {
	
	private static final Logger logger = LoggerFactory.getLogger(MongoMetadataStore.class);
	
	private static final String METADATA_INDEX_HOST = "http://localhost:8083/femme-index-application/metadata-index";
	
	private MetadataJsonCollection metadataMongoCollection;
	
	private MetadataGridFS metadataGridFS;
	
	private MetadataIndexClient metadataIndexClient;
	
	/*private XPathCacheManager indexManager;*/
	
	public MongoMetadataStore(MongoDatastoreClient mongoClient) {
		this(mongoClient, METADATA_INDEX_HOST);
	}
	
	public MongoMetadataStore(MongoDatastoreClient mongoClient, String metadataIndexHost) {
		this.metadataMongoCollection = new MetadataJsonCollection(mongoClient.getMetadataJson());
		this.metadataGridFS = new MetadataGridFS(mongoClient.getMetadataGridFS());
		/*this.indexManager = indexManager;*/
		metadataIndexClient = new FemmeIndexClient(metadataIndexHost);
	}

	@Override
	public void insert(Metadatum metadatum) throws MetadataStoreException {
		getMetadataStore(metadatum).insert(metadatum);
		if (metadatum.getId() != null && isIndexable(metadatum)) {
			metadataIndexClient.index(metadatum);
		}
	}

	@Override
	public Metadatum get(Metadatum metadatum) throws MetadataStoreException {
		return getMetadataStore(metadatum).get(metadatum);
	}

	@Override
	public List<Metadatum> find(String elementId) throws MetadataStoreException {
		return find(elementId, true);
	}

	@Override
	public List<Metadatum> find(String elementId, boolean lazy) throws MetadataStoreException {
		List<Metadatum> metadata = new ArrayList<>();
		
		List<Metadatum> jsonMetadata = metadataMongoCollection.find(elementId, lazy);
		List<Metadatum> otherMetadata = metadataGridFS.find(elementId, lazy);
		
		metadata.addAll(jsonMetadata);
		metadata.addAll(otherMetadata);
		
		return metadata;
	}
	
	@Override
	public <T extends Element> T xPath(T element, String xPath) throws MetadataStoreException {
		boolean xPathSatisfied = false;
		for (Metadatum metadatum: element.getMetadata()) {
			/*if (getIndexedXPath(metadatum, xPath) != null) {
				xPathSatisfied = true;
			} else {*/
				List<String> xPathResult = getMetadataStore(metadatum).xPath(metadatum, xPath);
			if (xPathResult.size() > 0) {
				xPathSatisfied = true;
				/*indexManager.checkAndCreateIndexOnXPath(metadatum, xPath, xPathResult, element);*/
			}
			/*}*/
		}
		
		return xPathSatisfied ? element : null;
	}
	
	/*private MetadatumXPathCache getIndexedXPath(Metadatum metadatum, String xPath) {
		if (metadatum.getXPathCache() != null) {
			for (MetadatumXPathCache index: metadatum.getXPathCache()) {
				if (index != null && index.getXPath().equals(xPath)) {
					return index;
				}
			}
		}
		
		return null;
	}*/
	/*@Override
	public <T extends Element> List<T> find(List<T> elements, String xPath) throws MetadataStoreException {
		try {
			return elements.stream().filter(new Predicate<T>() {
				@Override
				public boolean test(T t) {
					for (Metadatum metadatum: t.getMetadata()) {
						try {
							if (getMetadataStore(metadatum).xPath(metadatum, xPath) != null) {
								return true;
							}
						} catch (MetadataStoreException e) {
							logger.error(e.getMessage(), e);
							throw new RuntimeException(e.getMessage(), e);
						}
					}
					return false;
				}
			}).collect(Collectors.toList());
		} catch (RuntimeException e) {
			throw new MetadataStoreException(e.getMessage(), e);
		}
	}*/
	
	/*@Override
	public Metadatum xPath(Metadatum metadatum, String xPath) throws MetadataStoreException {
		return getMetadataStore(metadatum).xPath(metadatum, xPath);
	}*/

	/*@Override
	public List<Metadatum> find(Metadatum metadatum) {
		// TODO Auto-generated method stub
		return null;
	}*/

	/*@Override
	public List<Metadatum> find(List<Metadatum> metadataList) throws MetadataStoreException {
		// TODO Auto-generated method stub
		return null;
	}*/

	@Override
	public void delete(Metadatum metadatum) throws MetadataStoreException {
		getMetadataStore(metadatum).delete(metadatum);
	}
	
	@Override
	public void deleteAll(String elementId) throws MetadataStoreException {
		metadataMongoCollection.deleteAll(elementId);
		metadataGridFS.deleteAll(elementId);
		
	}

	private MongoMetadataCollection getMetadataStore(Metadatum metadatum) throws MetadataStoreException {
		if (metadatum.getContentType() != null) {
			if (metadatum.getContentType().toLowerCase().equals("json") || metadatum.getContentType().toLowerCase().contains("json")) {
				return metadataMongoCollection;
			} else {
				return metadataGridFS;
			}
		} else {
			throw new MetadataStoreException("No metadata content type provided");
		}
		
	}
	
	private boolean isIndexable(Metadatum metadatum) {
		if (metadatum.getContentType() != null && metadatum.getContentType().contains("xml")) {
			return true;
		}
		return false;
	}
}
