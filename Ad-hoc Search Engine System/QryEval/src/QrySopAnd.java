import java.io.IOException;

/**
 * Created by samy on 9/14/16.
 */
public class QrySopAnd extends QrySop{
  @Override
  public double getScore(RetrievalModel r) throws IOException {
    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } else if (r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean (r);
    } else if (r instanceof RetrievalModelIndri) {
      return this.getScoreIndri((RetrievalModelIndri) r);
    }
    else {
      throw new IllegalArgumentException
                (r.getClass().getName() + " doesn't support the AND operator.");
    }
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    double score = 1.0;
    double l = this.args.size();
    double pow = 1/ l;
    for (int i=0; i<l; ++i) {
      QrySop q_i = (QrySop) this.args.get(i);

      score *= Math.pow(q_i.getDefaultScore(r, docid), pow);
    }
    return score;
  }

  @Override
  public boolean docIteratorHasMatch(RetrievalModel r) {
    if (r instanceof RetrievalModelIndri)
      return this.docIteratorHasMatchMin(r);
    return this.docIteratorHasMatchAll(r);
  }

  /**
   *  getScore for the UnrankedBoolean retrieval model.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  private double getScoreUnrankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      return 1.0;
    }
  }

  /**
   * getScore for the RankedBoolean retrieval model
   * @param r
   * @return
   * @throws IOException
   */
  private double getScoreRankedBoolean(RetrievalModel r) throws IOException {
    if (!this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      double min_score = Integer.MAX_VALUE;
      int min_docId = Integer.MAX_VALUE;
      for (int i=0, l=this.args.size(); i<l; i++) {
        QrySop q_i = (QrySop)this.args.get(i);
        if (q_i.docIteratorHasMatchCache()) {
          int docid = q_i.docIteratorGetMatch();
          if (docid < min_docId) {
            // update min_docId
            min_docId = docid;
            min_score = q_i.getScore(r);
          } else if (docid == min_docId) {
            double score = q_i.getScore(r);
            if (score < min_score)
              min_score = score;
          }
        }
      }
      return min_score;
    }
  }

  /**
   * Get the score of Indri model
   * @param r
   * @return
   * @throws IOException
   */
  private double getScoreIndri(RetrievalModelIndri r) throws IOException {
    if (this.docIteratorHasMatchCache()) {
      double score = 1.0;
      int l = this.args.size();
      double pow = 1 / ((double) l);
      int docid = this.docIteratorGetMatch();
      for (int i = 0; i < l; ++i) {
        QrySop q_i = (QrySop) this.args.get(i);
        if (q_i.docIteratorHasMatchCache() && q_i.docIteratorGetMatch() == docid)
          score *= Math.pow(q_i.getScore(r), pow);
        else
          score *= Math.pow(q_i.getDefaultScore(r, docid), pow);
      }
      return score;
    }
    return 1.0;
  }
}
