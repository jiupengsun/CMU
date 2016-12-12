import java.io.IOException;

/**
 * Created by samy on 10/20/16.
 */
public class TrecEval implements Comparable<TrecEval>{
  private String queryId;
  private int internalDocId;
  private String externalDocId;
  private double score;
  private int body_length;

  public TrecEval() {
    this.internalDocId = -1;
  }

  public int getBody_length() {
    return body_length;
  }

  public String getQueryId() {
    return queryId;
  }

  public int getInternalDocId() {
    return internalDocId;
  }

  public void setInternalDocId(int internalDocId) {
    this.internalDocId = internalDocId;
    try {
      this.body_length = Idx.getFieldLength(Constant.BODY_FIELD, internalDocId);
    } catch (IOException e) {
      e.printStackTrace();
      this.internalDocId = -1;
    }
  }

  public void setQueryId(String queryId) {
    this.queryId = queryId;
  }

  public String getExternalDocId() {
    return externalDocId;
  }

  public void setExternalDocId(String externalDocId) {
    this.externalDocId = externalDocId;
    try {
      if (this.internalDocId == -1)
        this.setInternalDocId(Idx.getInternalDocid(this.externalDocId));
    } catch (Exception e) {
      this.internalDocId = -1;
      //e.printStackTrace();
    }
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  @Override
  public int compareTo(TrecEval o) {
    double score = o.getScore();
    return this.score > score ? -1 : (this.score < score ? 1 : 0);
  }
}
