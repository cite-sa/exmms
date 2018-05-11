package gr.cite.femme.fulltext.engine;

import gr.cite.femme.fulltext.core.FulltextDocument;
import gr.cite.femme.fulltext.engine.semantic.search.taxonomy.ElasticsearchClient;
import gr.cite.femme.fulltext.engine.semantic.search.taxonomy.TaxonomyRepository;
import org.junit.Before;
import org.junit.Test;

import java.net.UnknownHostException;
import java.util.UUID;

public class FullTextEngineTest {
	private FulltextIndexEngine engine;

	@Before
	public void init() throws UnknownHostException {
		ElasticsearchClient elasticsearchClient = new ElasticsearchClient("localhost", 9200, "semantic_search", "taxonomies");
		TaxonomyRepository taxonomyRepository = new TaxonomyRepository(elasticsearchClient);
		this.engine = new FulltextIndexEngine("localhost", 9200, "fulltext_search", taxonomyRepository);
	}

	@Test
	public void testInsert() throws FemmeFulltextException {
		FulltextDocument doc = new FulltextDocument();
		doc.setElementId(UUID.randomUUID().toString());
		doc.setMetadatumId(UUID.randomUUID().toString());

		doc.setFulltextField("testField", "Evolutionary algorithm");
		this.engine.insert(doc);
	}
}
