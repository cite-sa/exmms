package gr.cite.femme.datastore.api;

import java.util.List;

import gr.cite.femme.exceptions.MetadataIndexException;
import gr.cite.femme.exceptions.MetadataStoreException;
import gr.cite.femme.model.Element;
import gr.cite.femme.model.Metadatum;

public interface MetadataStore {

	public void close();

	public void insert(Metadatum metadatum) throws MetadataStoreException, MetadataIndexException;

	public void update(Metadatum metadatum) throws MetadataStoreException, MetadataIndexException;

	public void index(Metadatum metadatum) throws MetadataIndexException;

	public void reIndexAll() throws MetadataIndexException, MetadataStoreException;
	
	public Metadatum get(Metadatum metadatum) throws MetadataStoreException;
	
	public List<Metadatum> find(String elementId) throws MetadataStoreException;
	
	public List<Metadatum> find(String elementId, boolean lazy) throws MetadataStoreException;

	public List<Metadatum> xPath(String xPath) throws MetadataStoreException;

	public <T extends Element> T xPath(T element, String xPath) throws MetadataStoreException;
	
	public void delete(Metadatum metadatum) throws MetadataStoreException;
	
	public void deleteAll(String elementId) throws MetadataStoreException;

	public String generateMetadatumId();

}
