import java.io.IOException;

/**
 * Created by samy on 9/30/16.
 */
public class QrySopSum extends QrySop{
  @Override
  public double getScore(RetrievalModel r) throws IOException {
    if (this.docIteratorHasMatchCache()) {
      double score = 0.0;
      if (r instanceof RetrievalModelBM25) {
        for (int i = 0, l = this.args.size(); i < l; ++i) {
          QrySop q_i = (QrySop) this.args.get(i);
          if (q_i.docIteratorHasMatchCache() &&
                  q_i.docIteratorGetMatch() == this.docIteratorGetMatch())
            score += q_i.getScore(r);
        }
      }
      return score;
    }
    return 0.0;
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    return 0;
  }

  @Override
  public boolean docIteratorHasMatch(RetrievalModel r) {
    return this.docIteratorHasMatchMin(r);
  }
}
