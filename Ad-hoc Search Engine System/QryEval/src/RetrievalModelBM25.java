/**
 * Created by samy on 9/30/16.
 */
public class RetrievalModelBM25 extends RetrievalModel{

  // k1 parameter
  private float k1;
  // k3 parameter
  private float k3;
  // b parameter
  private float b;

  public RetrievalModelBM25(float k1, float k3, float b) {
    this.k1 = k1;
    this.k3 = k3;
    this.b = b;
  }

  public float getK1() {
    return k1;
  }

  public float getK3() {
    return k3;
  }

  public float getB() {
    return b;
  }

  @Override
  public String defaultQrySopName() {
    return "#sum";
  }
}
