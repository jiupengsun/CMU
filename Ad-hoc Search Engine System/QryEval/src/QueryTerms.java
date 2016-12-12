/**
 * Created by samy on 10/28/16.
 */
public class QueryTerms {
  private String qid;
  private String originQuery;
  private String[] terms;

  public String getQid() {
    return qid;
  }

  public void setQid(String qid) {
    this.qid = qid;
  }

  public String getOriginQuery() {
    return originQuery;
  }

  public void setOriginQuery(String originQuery) {
    this.originQuery = originQuery;
  }

  public String[] getTerms() {
    return terms;
  }

  public void setTerms(String[] terms) {
    this.terms = terms;
  }
}
