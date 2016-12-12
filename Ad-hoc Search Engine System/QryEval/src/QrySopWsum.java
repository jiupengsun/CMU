import java.io.IOException;

/**
 * Created by samy on 10/1/16.
 */
public class QrySopWsum extends QrySopWeight{
  @Override
  public double getScore(RetrievalModel r) throws IOException {
    if (this.docIteratorHasMatchCache()) {
      int docid = this.docIteratorGetMatch();
      double score = 0.0;
      for (int i=0, l=this.args.size(); i<l; ++i) {
        QrySop q_i = (QrySop) this.args.get(i);
        double w = this.weights.get(i);
        if (q_i.docIteratorHasMatchCache() && q_i.docIteratorGetMatch() == docid) {
          score += q_i.getScore(r) * (w / sumWeight);
        } else {
          score += q_i.getDefaultScore(r, docid) * (w / sumWeight);
        }
      }
      return score;
    }
    return 1.0;
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    double score = 0.0;
    for (int i=0, l=this.args.size(); i<l; ++i) {
      QrySop q_i = (QrySop) this.args.get(i);
      double w = this.weights.get(i);
      score += q_i.getDefaultScore(r, docid) *(w / sumWeight);
    }
    return score;
  }

  @Override
  public boolean docIteratorHasMatch(RetrievalModel r) {
    return this.docIteratorHasMatchMin(r);
  }
}
