package gr.cite.femme.engine.metadatastore.mongodb;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.mongodb.client.MongoCursor;
import gr.cite.commons.utils.hash.HashGenerationException;
import gr.cite.femme.core.model.Metadatum;
import gr.cite.femme.core.model.Status;
import gr.cite.femme.core.exceptions.MetadataStoreException;

public interface MongoMetadataCollection {
	
	public void insert(Metadatum metadatum) throws MetadataStoreException;

	public Metadatum update(Metadatum metadatum) throws MetadataStoreException;

	//public Metadatum update(String id, Map<String, Object> fieldsAndValues) throws MetadataStoreException;

	public void updateStatus(String id, Status status) throws MetadataStoreException;

	//public Metadatum get(Metadatum metadatum) throws MetadataStoreException;

	public Metadatum get(String id) throws MetadataStoreException;

	public Metadatum get(String id, boolean lazy) throws MetadataStoreException;

	public List<Metadatum> find(String elementId) throws MetadataStoreException;

	public List<Metadatum> find(String elementId, boolean lazy) throws MetadataStoreException;

	public List<Metadatum> find(String elementId, boolean lazy, boolean loadInactive) throws MetadataStoreException;
	
	public List<Metadatum> find(List<String> elementIds) throws MetadataStoreException;
	
	public List<Metadatum> find(List<String> elementIds, boolean lazy) throws MetadataStoreException;
	
	public List<Metadatum> find(List<String> elementIds, boolean lazy, boolean loadInactive) throws MetadataStoreException;

	public MongoCursor<Metadatum> findAllBeforeTimestamp(Instant timestamp) throws MetadataStoreException;

	public MongoCursor<Metadatum> findAll(boolean lazy) throws MetadataStoreException;

	public List<String> xPath(Metadatum metadatum, String xPath) throws MetadataStoreException;

	public void delete(String metadatumId);
	
	public void deleteAll(String elementId) throws MetadataStoreException;

	public void deleteAll(Map<String, Object> fieldsAndValues) throws MetadataStoreException;
}
