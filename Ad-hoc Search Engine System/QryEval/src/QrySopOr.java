/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

/**
 *  The OR operator for all retrieval models.
 */
public class QrySopOr extends QrySop {

  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchMin (r);
  }

  /**
   *  Get a score for the document that docIteratorHasMatch matched.
   *  @param r The retrieval model that determines how scores are calculated.
   *  @return The document score.
   *  @throws IOException Error accessing the Lucene index
   */
  public double getScore (RetrievalModel r) throws IOException {

    if (r instanceof RetrievalModelUnrankedBoolean) {
      return this.getScoreUnrankedBoolean (r);
    } else if (r instanceof RetrievalModelRankedBoolean) {
      return this.getScoreRankedBoolean (r);
    }
    else {
      throw new IllegalArgumentException
                (r.getClass().getName() + " doesn't support the OR operator.");
    }
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    return 0;
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
      double max_score = Integer.MIN_VALUE;
      int min_docId = Integer.MAX_VALUE;
      for (int i=0, l=this.args.size(); i<l; i++) {
        QrySop q_i = (QrySop)this.args.get(i);
        if (q_i.docIteratorHasMatchCache()) {
          int docid = q_i.docIteratorGetMatch();
          if (docid < min_docId) {
            // update min_docId
            min_docId = docid;
            max_score = q_i.getScore(r);
          } else if (docid == min_docId) {
            double score = q_i.getScore(r);
            if (score > max_score)
              max_score = score;
          }
        }
      }
      return max_score;
    }
  }

}
