package gr.cite.femme.engine.datastore.mongodb.cache;

import java.util.List;

import gr.cite.femme.core.model.Metadatum;
import gr.cite.femme.core.model.Element;

public interface XPathCacheManager {
	
	public void createIndex();
	
	public void removeIndex();
	
	/*public void createIndexOnXPath(Metadatum metadatum, String xpath, List<String> xPathResultm T element);*/
	
	public <T extends Element> void checkAndCreateIndexOnXPath(Metadatum metadatum, String xpath, List<String> xPathResult, T element);
	
	public void removeIndexOnXPath();
}