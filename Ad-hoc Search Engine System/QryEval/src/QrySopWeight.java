import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samy on 10/1/16.
 */
public abstract class QrySopWeight extends QrySop{

  protected List<Float> weights;
  protected double sumWeight;

  public QrySopWeight() {
    weights = new ArrayList<Float>();
    sumWeight = 0.0;
  }

  public void addWeight(float w) {
    sumWeight += w;
    weights.add(w);
  }
}
