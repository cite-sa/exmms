package gr.cite.femme.criteria.walker;

import gr.cite.femme.criteria.serializer.CriteriaQuerySerializer;
import gr.cite.femme.query.criteria.CriteriaQuery;
import gr.cite.femme.query.criteria.UnsupportedQueryOperationException;

public class CriteriaWalker<T> {
	CriteriaQuerySerializer<T> querySerializer;

	CriteriaQuery<T> datastoreQuery;

	public CriteriaWalker(CriteriaQuerySerializer<T> querySerializer, CriteriaQuery<T> datastoreQuery) {
		super();
		this.querySerializer = querySerializer;
		this.datastoreQuery = datastoreQuery;
	}

	public CriteriaQuery<T> walk() throws UnsupportedQueryOperationException {
		return WhereWalker.walk(querySerializer.getWhere(), datastoreQuery.whereBuilder(), datastoreQuery);
	}
}
