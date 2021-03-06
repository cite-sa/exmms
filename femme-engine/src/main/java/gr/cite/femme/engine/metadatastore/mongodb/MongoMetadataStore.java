package gr.cite.femme.engine.metadatastore.mongodb;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import com.mongodb.client.MongoCursor;
import gr.cite.femme.core.model.Status;
import gr.cite.femme.core.datastores.MetadataStore;
import gr.cite.femme.core.exceptions.MetadataIndexException;
import gr.cite.femme.core.exceptions.MetadataStoreException;
import gr.cite.femme.engine.metadata.xpath.MetadataXPath;
import gr.cite.femme.engine.metadata.xpath.ReIndexingProcess;
import gr.cite.femme.core.model.Metadatum;
import gr.cite.commons.utils.xml.XMLConverter;
import gr.cite.commons.utils.xml.XPathEvaluator;
import gr.cite.commons.utils.xml.exceptions.XMLConversionException;
import gr.cite.commons.utils.xml.exceptions.XPathEvaluationException;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.xpath.XPathFactoryConfigurationException;

public class MongoMetadataStore implements MetadataStore {
	private static final Logger logger = LoggerFactory.getLogger(MongoMetadataStore.class);

	private MongoMetadataStoreClient mongoMetadataStoreClient;
	private MetadataJsonCollection metadataMongoCollection;
	private MetadataGridFS metadataGridFS;
	private MetadataXPath metadataXPath;

	private final StampedLock lock = new StampedLock();
	
	public MongoMetadataStore() {
		this.mongoMetadataStoreClient = new MongoMetadataStoreClient();
		this.metadataGridFS = new MetadataGridFS(this.mongoMetadataStoreClient.getMetadataGridFSBucket(), this.mongoMetadataStoreClient.getMetadataGridFSFilesCollection());
		this.metadataXPath = new MetadataXPath();
	}

	public MongoMetadataStore(String host, int port, String name) {
		this.mongoMetadataStoreClient = new MongoMetadataStoreClient(host, port, name);
		this.metadataGridFS = new MetadataGridFS(this.mongoMetadataStoreClient.getMetadataGridFSBucket(), this.mongoMetadataStoreClient.getMetadataGridFSFilesCollection());
	}

	public MongoMetadataStore(String host, int port, String name, String bucketName) {
		this.mongoMetadataStoreClient = new MongoMetadataStoreClient(host, port, name, bucketName);
		this.metadataGridFS = new MetadataGridFS(this.mongoMetadataStoreClient.getMetadataGridFSBucket(), this.mongoMetadataStoreClient.getMetadataGridFSFilesCollection());
	}
	
	public MongoMetadataStore(String host, int port, String name, String bucketName, MetadataXPath metadataXPath) {
		this.mongoMetadataStoreClient = new MongoMetadataStoreClient(host, port, name, bucketName);
//		this.metadataMongoCollection = new MetadataJsonCollection(mongoMetadataStoreClient.getMetadataJson());
		this.metadataGridFS = new MetadataGridFS(this.mongoMetadataStoreClient.getMetadataGridFSBucket(), this.mongoMetadataStoreClient.getMetadataGridFSFilesCollection());
		this.metadataXPath = metadataXPath;
	}

	@Override
	public void close() {
		this.mongoMetadataStoreClient.close();
	}

	@Override
	public void insert(Metadatum metadatum) throws MetadataStoreException, MetadataIndexException {
		long stamp = lock.readLock();
		try {
			getMetadataStore(metadatum).insert(metadatum);
			index(metadatum);
		} finally {
			lock.unlockRead(stamp);
		}
	}

	@Override
	public Metadatum update(Metadatum metadatum) throws MetadataStoreException, MetadataIndexException {
		Metadatum updatedMetadatum = this.metadataGridFS.update(metadatum);

		if (updatedMetadatum != null) {
			index(metadatum);
			deIndexMetadatum(metadatum.getId());
		}
		return updatedMetadatum;
	}

	/*@Override
	public Metadatum update(String id, Map<String, Object> fieldsAndValues) throws MetadataStoreException, MetadataIndexException {
		return this.metadataGridFS.update(id, fieldsAndValues);
	}*/

	@Override
	public void index(Metadatum metadatum) throws MetadataIndexException {
		if (isFemmeInIndexMode() && isXPathable(metadatum)) {
			this.metadataXPath.index(metadatum);
		}
	}

	@Override
	public void deIndexMetadatum(String id) throws MetadataIndexException {
		if (isFemmeInIndexMode()) {
			this.metadataXPath.deIndex(id);
		}
	}

	@Override
	public void deIndexElement(String elementId) throws MetadataIndexException {
		if (isFemmeInIndexMode()) {
			this.metadataXPath.deIndexAll(elementId);
		}
	}

	@Override
	public void reIndexAll() throws MetadataIndexException, MetadataStoreException {
		if (isFemmeInIndexMode()) {
			MongoCursor<Metadatum> snapshotCursor;
			ReIndexingProcess reIndexer;

			long stamp = lock.writeLock();
			try {
				Instant snapshotTimestamp = Instant.now();
				snapshotCursor = metadataGridFS.findAllBeforeTimestamp(snapshotTimestamp);
				reIndexer = this.metadataXPath.beginReIndexing();
			} finally {
				lock.unlockWrite(stamp);
			}

			List<Future<String>> futures = new ArrayList<>();
			ExecutorService executor = Executors.newFixedThreadPool(2);
			while (snapshotCursor.hasNext()) {
				try {
					final Metadatum metadatum = snapshotCursor.next();
					futures.add(executor.submit(() -> {
						try {
							reIndexer.index(metadatum);
						} catch (MetadataIndexException e) {
							throw new RuntimeException(e);
						}
						return metadatum.getId();
					}));
				} catch (RuntimeException e) {
					throw new MetadataIndexException(e);
				}
			}

			for (Future<String> future : futures) {
				try {
					String metadatumId = future.get();
					logger.info("Metadatum " + metadatumId + " successfully indexed");
				} catch (InterruptedException | ExecutionException e) {
					logger.error(e.getMessage(), e);
				}
			}
			executor.shutdown();

			stamp = lock.writeLock();
			try {
				this.metadataXPath.endReIndexing();
			} finally {
				lock.unlockWrite(stamp);
			}
		}
	}

	@Override
	public Metadatum get(Metadatum metadatum) throws MetadataStoreException {
		return get(metadatum, false);
	}

	@Override
	public Metadatum get(Metadatum metadatum, boolean lazy) throws MetadataStoreException {
		return getMetadataStore(metadatum).get(metadatum.getId(), lazy);
	}

	@Override
	public List<Metadatum> find(String elementId) throws MetadataStoreException {
		return find(elementId, false);
	}

	@Override
	public List<Metadatum> find(String elementId, boolean lazy) throws MetadataStoreException {
		return find(elementId, lazy, false);
	}

	@Override
	public List<Metadatum> find(String elementId, boolean lazy, boolean loadInactive) throws MetadataStoreException {
		List<Metadatum> metadata = new ArrayList<>();

		//List<Metadatum> jsonMetadata = metadataMongoCollection.get(elementId, lazy);
		List<Metadatum> otherMetadata = this.metadataGridFS.find(elementId, lazy, loadInactive);

		//metadata.addAll(jsonMetadata);
		metadata.addAll(otherMetadata);

		return metadata;
	}

	@Override
	public List<Metadatum> xPath(String xPath, boolean lazyPayload) throws MetadataStoreException {
		if (isFemmeInIndexMode()) {
			return xPath(new ArrayList<>(), xPath, lazyPayload);
		} else {
			throw new MetadataStoreException("FeMME not in index mode");
		}
	}
	@Override
	public List<Metadatum> xPath(List<String> elementIds, String xPath, boolean lazyPayload) throws MetadataStoreException {
		if (isFemmeInIndexMode()) {
			try {
				return this.metadataXPath.xPath(elementIds, xPath, lazyPayload);
			} catch (MetadataIndexException e) {
				throw new MetadataStoreException(e.getMessage(), e);
			}
		} else {
			throw new MetadataStoreException("FeMME not in index mode");
		}
	}


	@Override
	public List<Metadatum> xPathInMemory(String xPath) throws MetadataStoreException {
		return xPathInMemory(new ArrayList<>(), xPath);
	}

	@Override
	public List<Metadatum> xPathInMemory(List<String> elementIds, String xPath) throws MetadataStoreException {
		List<Metadatum> xPathedMetadata = new ArrayList<>();
		List<Metadatum> metadata = this.metadataGridFS.find(elementIds, false);
		
		for (Metadatum metadatum: metadata) {
			if (metadatum.getValue() != null) {
				try {
					String xPathResult = new XPathEvaluator(XMLConverter.stringToNode(metadatum.getValue())).evaluate(xPath).stream().collect(Collectors.joining());
					
					if (xPathResult.length() > 0) {
						metadatum.setValue(xPathResult);
						xPathedMetadata.add(metadatum);
					}
				} catch (XPathEvaluationException | XMLConversionException | XPathFactoryConfigurationException e) {
					logger.error("In memory XPath: " + e.getMessage());
				}
			}
		}
		
		return xPathedMetadata;
	}
	
	@Override
	public void delete(Metadatum metadatum) throws MetadataStoreException, MetadataIndexException {
		deIndexMetadatum(metadatum.getId());
		getMetadataStore(metadatum).delete(metadatum.getId());
	}
	
	@Override
	public void deleteAll(String elementId) throws MetadataStoreException, MetadataIndexException {
		deIndexElement(elementId);
		//metadataMongoCollection.deleteAll(elementId);
		this.metadataGridFS.deleteAll(elementId);
		
	}

	@Override
	public void softDelete(String metadatumId) throws MetadataStoreException, MetadataIndexException {
		this.metadataGridFS.updateStatus(metadatumId, Status.INACTIVE);
	}

	@Override
	public void softDeleteAll(String elementId) throws MetadataStoreException, MetadataIndexException {
		List<Metadatum> metadata = this.metadataGridFS.find(elementId, true);
		for (Metadatum metadatum: metadata) {
			this.metadataGridFS.updateStatus(metadatum.getId(), Status.INACTIVE);
		}
	}

	@Override
	public String generateMetadatumId() {
		return new ObjectId().toString();
	}

	private MongoMetadataCollection getMetadataStore(Metadatum metadatum) throws MetadataStoreException {
		if (metadatum.getContentType() != null) {
			/*if (metadatum.getContentType().toLowerCase().contains("json")) {
				return metadataMongoCollection;
			} else {
				return metadataGridFS;
			}*/
			return metadataGridFS;
		} else {
			throw new MetadataStoreException("No metadata content type provided");
		}
		
	}
	
	private boolean isIndexable(Metadatum metadatum) {
		return metadatum.getContentType() != null && (metadatum.getContentType().toLowerCase().contains("xml") || metadatum.getContentType().toLowerCase().contains("json"));
	}

	private boolean isXPathable(Metadatum metadatum) {
		return metadatum.getContentType() != null && (metadatum.getContentType().toLowerCase().contains("xml") || metadatum.getContentType().toLowerCase().contains("json"));
	}

	private boolean isFemmeInIndexMode() {
		return this.metadataXPath != null;
	}

}
