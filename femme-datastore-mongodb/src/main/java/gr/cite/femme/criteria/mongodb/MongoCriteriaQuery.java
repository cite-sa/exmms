package gr.cite.femme.criteria.mongodb;

import java.util.List;

import org.bson.Document;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;

import gr.cite.femme.core.Element;
import gr.cite.femme.criteria.CriteriaQuery;
import gr.cite.femme.criteria.Where;
import gr.cite.femme.datastore.mongodb.MongoDatastoreClient;

public class MongoCriteriaQuery<T extends Element> implements CriteriaQuery<T> {
	private MongoCollection collection;
	
	public MongoCriteriaQuery(MongoDatastoreClient mongoClient) {
		collection = mongoClient.getElementsCollection();
	}

	@Override
	public Where<T> whereBuilder() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <S> Where<S> expressionFactory() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T find(String id) {
		collection.find(new Document().append("_id", id)).first();
		return null;
	}

	@Override
	public T find(T t) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<T> find() {
		// TODO Auto-generated method stub
		return null;
	}

}
