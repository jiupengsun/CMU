/**
 * Created by samy on 9/14/16.
 * Ranked Retrieve model
 */
public class RetrievalModelRankedBoolean extends RetrievalModel{
  @Override
  public String defaultQrySopName() {
    return new String ("#or");
  }
}
