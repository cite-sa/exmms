package gr.cite.femme.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import gr.cite.commons.pipeline.ProcessingPipeline;
import gr.cite.commons.pipeline.exceptions.ProcessingPipelineException;
import gr.cite.femme.core.query.execution.MetadataQueryExecutorBuilder;
import gr.cite.femme.engine.datastore.mongodb.utils.FieldNames;
import gr.cite.femme.core.datastores.Datastore;
import gr.cite.femme.core.datastores.MetadataStore;
import gr.cite.femme.core.exceptions.DatastoreException;
import gr.cite.femme.core.exceptions.FemmeException;
import gr.cite.femme.core.exceptions.MetadataIndexException;
import gr.cite.femme.core.exceptions.MetadataStoreException;
import gr.cite.femme.core.model.Collection;
import gr.cite.femme.core.model.DataElement;
import gr.cite.femme.core.model.Element;
import gr.cite.femme.core.model.Metadatum;
import gr.cite.femme.core.model.Status;
import gr.cite.femme.core.model.SystemicMetadata;
import gr.cite.femme.core.query.construction.Criterion;
import gr.cite.femme.core.query.construction.Query;
import gr.cite.femme.core.dto.QueryOptionsMessenger;
import gr.cite.femme.engine.pipeline.config.DatastoreType;
import gr.cite.femme.engine.pipeline.config.PipelineTypesConfiguration;
import gr.cite.femme.engine.query.construction.mongodb.CriterionBuilderMongo;
import gr.cite.femme.engine.query.execution.mongodb.QueryExecutorFactory;
import gr.cite.femme.engine.query.construction.mongodb.QueryMongo;
import gr.cite.femme.fulltext.client.FulltextException;
import gr.cite.femme.fulltext.client.FulltextIndexClientAPI;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Femme {
	private static final Logger logger = LoggerFactory.getLogger(Femme.class);
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final String PIPELINE_CONFIG_FILE = "pipeline-config.json";

	private Datastore datastore;
	private MetadataStore metadataStore;
	private FulltextIndexClientAPI fulltextClient;

	private PipelineTypesConfiguration pipelineConfiguration;
	
	@Inject
	public Femme(Datastore datastore, MetadataStore metadataStore) throws IOException {
		this.datastore = datastore;
		this.metadataStore = metadataStore;

		String config = Resources.toString(Resources.getResource(Femme.PIPELINE_CONFIG_FILE), Charsets.UTF_8);
		this.pipelineConfiguration = new PipelineTypesConfiguration(config);
	}
	
	@Inject
	public void setFulltextClient(FulltextIndexClientAPI fulltextClient) {
		this.fulltextClient = fulltextClient;
	}
	
	public String insertOrUpdateElement(Element element) throws DatastoreException, MetadataStoreException, FemmeException {
		Element updatedElement = null;
		Element existing = exists(element);
		
		if (existing != null) {
			//element.setId(id);
			merge(existing, element);
			updatedElement = updateElement(element);
		} else {
			insertElement(element);
			
			if (this.fulltextClient != null) {
				insertInFulltextIndex(element);
			}
		}

		return updatedElement != null ? null : element.getId();
	}
	
	private Element exists(Element element) throws FemmeException {
		Element existingElement = null;
		try {
			if (element instanceof DataElement) {
				existingElement = this.datastore.getDataElementByNameEndpointAndCollections(element.getName(), element.getEndpoint(), ((DataElement) element).getCollections());
			} else if (element instanceof Collection) {
				existingElement = this.datastore.getCollectionByNameAndEndpoint(element.getName(), element.getEndpoint());
			}
		} catch (Exception e) {
			throw new FemmeException("Error on " + element.getClass().getSimpleName() + " existence check");
		}
		
		return existingElement;
	}
	
	private <T extends Element> void merge(T existingElement, T newElement) {
		newElement.setId(existingElement.getId());
		if (newElement.getSystemicMetadata() == null) newElement.setSystemicMetadata(new SystemicMetadata());
		newElement.getSystemicMetadata().setCreated(existingElement.getSystemicMetadata().getCreated());
	}

	private void insertElement(Element element) throws DatastoreException, MetadataStoreException, FemmeException {
		if (element.getId() == null) {
			element.setId(this.datastore.generateId());
		}

		element.getMetadata().forEach(metadatum -> metadatum.setElementId(element.getId()));

		if (element.getSystemicMetadata() == null) {
			element.setSystemicMetadata(new SystemicMetadata());
		}
		element.getSystemicMetadata().setStatus(Status.ACTIVE);

		this.datastore.insert(element);
		
		if (element instanceof Collection) {
			insertCollectionDataElements((Collection) element);
			// TODO check how subDataElements are stored in MongoDB
		} else if (element instanceof DataElement) {
			insertDataElementDataElements((DataElement) element);
		}
		
		insertElementMetadata(element);
	}

	private void insertCollectionDataElements(Collection collection) throws DatastoreException, MetadataStoreException, FemmeException {
		for (DataElement dataElement: collection.getDataElements()) {
			dataElement.addCollection(collection);
			if (dataElement.getId() == null) {
				dataElement.setId(this.datastore.generateId());
			}
			insertElement(dataElement);
		}
	}

	private void insertDataElementDataElements(DataElement dataElement) throws DatastoreException, MetadataStoreException, FemmeException {
		for (DataElement subDataElement: dataElement.getDataElements()) {
			insertElement(subDataElement);
		}
	}

	private void insertElementMetadata(Element element) throws MetadataStoreException {
		for (Metadatum metadatum : element.getMetadata()) {
			try {
				this.metadataStore.insert(metadatum);
			} catch (MetadataIndexException e) {
				logger.warn(element.getClass().getSimpleName() + " " + element.getId() + " metadatum indexing failed", e);
			}
		}
	}
	
	public List<String> insertMetadata(List<Metadatum> metadata) throws MetadataStoreException {
		List<String> metadataIds = new ArrayList<>();
		
		for (Metadatum metadatum: metadata) {
			insertMetadatum(metadatum);
			metadataIds.add(metadatum.getId());
		}
		
		return metadataIds;
	}
	
	public String insertMetadatum(Metadatum metadatum) throws MetadataStoreException {
		try {
			this.metadataStore.insert(metadatum);
		} catch (MetadataIndexException e) {
			logger.warn("Metadatum [" + metadatum.getId() + "] indexing failed", e);
		}
		
		return metadatum.getId();
	}

	public <T extends Element> T updateElement(T element) throws DatastoreException, MetadataStoreException {
		T updatedElement = null;
		if (element.getId() != null) {
			updatedElement = this.datastore.update(element);

			List<Metadatum> existingMetadataToBeDeleted = this.metadataStore.find(element.getId());
			
			updateElementMetadata(element, existingMetadataToBeDeleted);
			deleteMetadata(existingMetadataToBeDeleted);
		}

		return updatedElement;
	}

	private void updateElementMetadata(Element element, List<Metadatum> existingMetadata) {
		for (Metadatum metadatum : element.getMetadata()) {
			metadatum.setElementId(element.getId());
			try {
				this.metadataStore.update(metadatum);
				existingMetadata.removeIf(existing -> existing.getId().equals(metadatum.getId()));
				
			} catch (MetadataStoreException e) {
				logger.error(element.getClass().getSimpleName() + " " + element.getId() + " metadatum insertion failed", e);
			} catch (MetadataIndexException e) {
				logger.warn(element.getClass().getSimpleName() + " " + element.getId() + " metadatum indexing failed", e);

			}
		}
	}

	public Metadatum updateMetadatum(Metadatum metadatum) throws FemmeException {
		try {
			return this.metadataStore.update(metadatum);
		} catch (MetadataStoreException | MetadataIndexException e) {
			throw new FemmeException("Metadatum " + metadatum.getId() + " update failed", e);
		}
	}

	public void deleteElement(String elementId, Class<? extends Element> elementSubtype) throws FemmeException {
		try {
			this.datastore.delete(elementId, elementSubtype);
			this.metadataStore.deleteAll(elementId);
			
			if (elementSubtype.equals(Collection.class)) {
				List<DataElement> collectionDataElements = this.datastore.getDataElementsByCollection(elementId);
				for (DataElement dataElement: collectionDataElements) {
					deleteElement(dataElement.getId(), DataElement.class);
				}
			}
			
			deleteElementFromAuxiliaryServices(elementId);
			
		} catch (MetadataIndexException | MetadataStoreException e) {
			throw new FemmeException("Delete metadata of " + elementSubtype.getSimpleName() + " [" + elementId + "] failed", e);
		} catch (DatastoreException e) {
			throw new FemmeException("Delete " + elementSubtype.getSimpleName() + " [" + elementId + "] failed", e);
		}
	}
	
	private void deleteElementFromAuxiliaryServices(String elementId) {
		if (this.fulltextClient != null) {
			this.fulltextClient.deleteByElementId(elementId);
		}
		// TODO Delete from geo service
	}
	
	private void deleteMetadata(List<Metadatum> existingMetadata) throws MetadataStoreException {
		for (Metadatum obsoleteMetadatum : existingMetadata) {
			try {
				this.metadataStore.delete(obsoleteMetadatum);
			} catch (MetadataIndexException e) {
				logger.warn("Metadatum de-indexing failed", e);
			}
		}
	}
	
	private void deleteMetadatum(Metadatum metadatum) throws MetadataIndexException, MetadataStoreException {
		if (metadatum == null) return;
		this.metadataStore.delete(metadatum);
	}

	public void softDeleteElement(String id, Class<? extends Element> elementSubType) throws FemmeException {
		try {
			this.metadataStore.softDeleteAll(id);
			this.datastore.softDelete(id, elementSubType);
		} catch (DatastoreException | MetadataIndexException | MetadataStoreException e) {
			throw new FemmeException("Soft delete " + elementSubType.getSimpleName() + " " + id + " failed", e);
		}
	}

	public void softDeleteMetadatum(String id) throws FemmeException {
		try {
			this.metadataStore.softDelete(id);
		} catch (MetadataIndexException | MetadataStoreException e) {
			throw new FemmeException("Soft delete metadatum " + id + " failed", e);
		}
	}

	public String addToCollection(DataElement dataElement, String collectionId) throws DatastoreException, MetadataStoreException, IllegalArgumentException, FemmeException {
		Collection collection = this.datastore.get(collectionId, Collection.class);
		if (collection == null) {
			throw new DatastoreException("Collection doesn't exist [" + collectionId + "]");
		}

		dataElement.setCollections(Collections.singletonList(collection));
		return insertOrUpdateElement(dataElement);
	}

	public String addToCollection(DataElement dataElement, Query<? extends Criterion> query) throws DatastoreException, MetadataStoreException, FemmeException {
		List<Collection> collections;
		collections = this.datastore.find(query, Collection.class).list();

		if (collections != null && collections.size() > 0) {
			dataElement.setCollections(collections);
			return insertOrUpdateElement(dataElement);
		}

		return null;
	}

	public boolean exists(Class<? extends Element> elementSubtype, String elementId) throws DatastoreException, MetadataStoreException {
		QueryMongo countQuery = QueryMongo.query().addCriterion(CriterionBuilderMongo.root().eq(FieldNames.ID, this.datastore.generateId(elementId)).end());
		return query(elementSubtype).count(countQuery).execute() > 0;
	}

	public <T extends Element> T get(Class<T> elementSubType, String id) throws DatastoreException, MetadataStoreException {
		return get(elementSubType, id, null, null, false);
	}

	public <T extends Element> T get(Class<T> elementSubType, String id, String xPath) throws DatastoreException, MetadataStoreException {
		return get(elementSubType, id, xPath, null, false);
	}

	public <T extends Element> T get(Class<T> elementSubType, String id, String xPath, QueryOptionsMessenger options) throws DatastoreException, MetadataStoreException {
		return get(elementSubType, id, xPath, options, false);
	}

	public <T extends Element> T get(Class<T> elementSubType, String id, String xPath, QueryOptionsMessenger options, boolean loadInactiveMetadata) throws DatastoreException, MetadataStoreException {
		if (options != null) {
			options.setLoadInactiveMetadata(Boolean.toString(loadInactiveMetadata));
		} else {
			options = QueryOptionsMessenger.builder().loadInactiveMetadata(loadInactiveMetadata).build();
		}

		try {
			return query(elementSubType).find(QueryMongo.query().addCriterion(CriterionBuilderMongo.root().eq(FieldNames.ID, new ObjectId(id)).end()))
							 .options(options).xPath(xPath).execute().first();
		} catch (IllegalArgumentException e) {
			throw new DatastoreException("Invalid " + elementSubType.getSimpleName() + " id: [" + id + "]");
		}
	}
	
	public List<DataElement> getDataElementsByCollection(String collectionId) throws DatastoreException {
		return this.datastore.getDataElementsByCollection(collectionId);
		
	}
	
	public List<Metadatum> getElementMetadata(String elementId) throws MetadataStoreException {
		return this.metadataStore.find(elementId);
	}

	/*public <T extends Element> T get(String id, String xPath, Class<T> elementSubType) throws DatastoreException, MetadataStoreException {
		try {
			return query(elementSubType).find(QueryMongo.query().addCriterion(CriterionBuilderMongo.root().eq(FieldNames.ID, this.datastore.generateId(id)).end())).xPath(xPath).execute().first();
		} catch (IllegalArgumentException e) {
			throw new DatastoreException("Invalid " + elementSubType.getSimpleName() + " id: [" + id + "]");
		}
	}*/

	/*public <T extends Element> MetadataQueryExecutor<T> find(Query<? extends Criterion> getQueryExecutor, Class<T> elementSubType) {
		return new QueryOptionsBuilderMongo<T>().getQueryExecutor(this.datastore, this.metadataStore, elementSubType).find(getQueryExecutor);
	}*/
	/*public <T extends Element> MetadataQueryExecutor<T> find(Query<? extends Criterion> getQueryExecutor, Class<T> elementSubType) {
		return new QueryOptionsBuilderMongo<T>().getQueryExecutor(this.datastore, this.metadataStore, elementSubType).find(getQueryExecutor);
	}*/

	public <T extends Element> MetadataQueryExecutorBuilder<T> query(Class<T> elementSubType) {
		return QueryExecutorFactory.getQueryExecutor(this.datastore, this.metadataStore, elementSubType);
	}

	/*public <T extends Element> MetadataQueryExecutorBuilder<T> find(Query<? extends Criterion> getQueryExecutor, Class<T> elementSubType) {
		return new QueryExecutorFactory<T>().getQueryExecutor(this.datastore, this.metadataStore, elementSubType).find(getQueryExecutor);
	}*/

	/*public <T extends Element> long count(Query<? extends Criterion> query, Class<T> elementSubtype) {
		return this.datastore.count(query, elementSubtype);
	}*/

	public void reIndex() throws MetadataStoreException, MetadataIndexException {
		this.metadataStore.reIndexAll();
	}

	private void insertInFulltextIndex(Element element) {
		if (! this.pipelineConfiguration.isEmpty()) {
			ProcessingPipeline pipeline = new ProcessingPipeline(this.pipelineConfiguration.getConfigurationForDatastoreTypeAndElementType(DatastoreType.FULLTEXT, element.getType()));

			for (Metadatum metadatum : element.getMetadata()) {
				try {
					if (this.fulltextClient != null) {
						this.fulltextClient.insert(metadatum.getElementId(), metadatum.getId(), pipeline.process(metadatum.getValue(), metadatum.getContentType().toLowerCase().split("/")[1]));
					}
				} catch (ProcessingPipelineException | FulltextException e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
	}

	private void deleteFromFulltextIndexByElementId(String elementId) throws FemmeException {
		if (this.fulltextClient != null) {
			this.fulltextClient.deleteByElementId(elementId);
		}
	}

	private void deleteFromFulltextIndexByMetadatumId(String metadatumId) throws FemmeException {
		// TODO implement
	}

	public String generateId() {
		return this.datastore.generateId();
	}

	public Object generateId(String id) {
		return this.datastore.generateId(id);
	}

}
