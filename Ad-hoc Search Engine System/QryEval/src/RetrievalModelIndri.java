/**
 * Created by samy on 10/1/16.
 */
public class RetrievalModelIndri extends RetrievalModel{

  private float mu;
  private float lambda;

  public RetrievalModelIndri(float m, float lam) {
    mu = m;
    lambda = lam;
  }

  public float getLambda() {
    return lambda;
  }

  public float getMu() {

    return mu;
  }

  @Override
  public String defaultQrySopName() {
    return "#and";
  }
}
