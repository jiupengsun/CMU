import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samy on 10/1/16.
 */
public class QrySopWand extends QrySopWeight{


  public QrySopWand() {
    super();
    weights = new ArrayList<>();
    sumWeight = 0.0;
  }

  @Override
  public double getScore(RetrievalModel r) throws IOException {
    if (this.docIteratorHasMatchCache()) {
      int docid = this.docIteratorGetMatch();
      double score = 1.0;
      for (int i=0, l=this.args.size(); i<l; ++i) {
        QrySop q_i = (QrySop) this.args.get(i);
        double w = this.weights.get(i);
        if (q_i.docIteratorHasMatchCache() && q_i.docIteratorGetMatch() == docid) {
          score *= Math.pow(q_i.getScore(r), w / sumWeight);
        } else {
          score *= Math.pow(q_i.getDefaultScore(r, docid), w / sumWeight);
        }
      }
      return score;
    }
    return 1.0;
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    double score = 1.0;
    for (int i=0, l=this.args.size(); i<l; ++i) {
      QrySop q_i = (QrySop) this.args.get(i);
      double w = this.weights.get(i);
      score *= Math.pow(q_i.getDefaultScore(r, docid), w / sumWeight);
    }
    return score;
  }

  @Override
  public boolean docIteratorHasMatch(RetrievalModel r) {
    return this.docIteratorHasMatchMin(r);
  }
}
