package gr.cite.femme.metadata.xpath.elasticsearch.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ElasticResponseShards {

	@JsonProperty("total")
	private Integer total;

	@JsonProperty("successful")
	private Integer successful;

	@JsonProperty("failed")
	private Integer failed;

	@JsonProperty("failures")
	private List<ElasticResponseShardsFailure> failures;

	public Integer getTotal() {
		return total;
	}

	public void setTotal(Integer total) {
		this.total = total;
	}

	public Integer getSuccessful() {
		return successful;
	}

	public void setSuccessful(Integer successful) {
		this.successful = successful;
	}

	public Integer getFailed() {
		return failed;
	}

	public void setFailed(Integer failed) {
		this.failed = failed;
	}

	public List<ElasticResponseShardsFailure> getFailures() {
		return failures;
	}

	public void setFailures(List<ElasticResponseShardsFailure> failures) {
		this.failures = failures;
	}
}