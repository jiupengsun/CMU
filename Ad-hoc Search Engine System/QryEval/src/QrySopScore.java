/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */

import javax.jws.soap.SOAPBinding;
import java.io.*;
import java.lang.IllegalArgumentException;

/**
 *  The SCORE operator for all retrieval models.
 */
public class QrySopScore extends QrySop {

  /**
   *  Document-independent values that should be determined just once.
   *  Some retrieval models have these, some don't.
   */
  
  /**
   *  Indicates whether the query has a match.
   *  @param r The retrieval model that determines what is a match
   *  @return True if the query matches, otherwise false.
   */
  public boolean docIteratorHasMatch (RetrievalModel r) {
    return this.docIteratorHasMatchFirst (r);
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
    } else if (r instanceof RetrievalModelBM25) {
      return this.getScoreBM25((RetrievalModelBM25) r);
    } else if (r instanceof RetrievalModelIndri){
      return this.getScoreIndri((RetrievalModelIndri) r);
    } else {
      throw new IllegalArgumentException
        (r.getClass().getName() + " doesn't support the SCORE operator.");
    }
  }

  @Override
  public double getDefaultScore(RetrievalModel r, int docid) throws IOException {
    float mu = ((RetrievalModelIndri)r).getMu();
    float lambda = ((RetrievalModelIndri)r).getLambda();
    QryIop q = (QryIop) this.args.get(0);
    double ctf = q.getCtf();
    long c_length = Idx.getSumOfFieldLengths(q.getField());
    double mle = ctf / c_length;
    long d_length = Idx.getFieldLength(q.getField(), docid);
    return (
               (1-lambda) * ((mu * mle) / (d_length + mu))
                   + lambda * mle
    );
  }

  /**
   *  getScore for the Unranked retrieval model.
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

  private double getScoreRankedBoolean (RetrievalModel r) throws IOException {
    if (! this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      QryIop q_0 = (QryIop) this.args.get(0);
      return q_0.getTf();
    }
  }

  private double getScoreBM25 (RetrievalModelBM25 r) throws IOException {
    if (!this.docIteratorHasMatchCache()) {
      return 0.0;
    } else {
      QryIop q = (QryIop) this.args.get(0);
      long N = Idx.getNumDocs();
      int df = q.getDf();
      int tf = q.getTf();
      // qtf always set to 1 here
      int qtf = 1;
      float k1 = r.getK1();
      float k3 = r.getK3();
      float b = r.getB();
      String field = q.getField();
      long sumOfFieldLength = Idx.getSumOfFieldLengths(field);
      int docCount = Idx.getDocCount(field);
      int fieldLength = Idx.getFieldLength(field, this.docIteratorGetMatch());
      double averDocLen = ((double)sumOfFieldLength) / docCount;
      double IDF = Math.log((N - df + 0.5) / (df + 0.5));
      double TF_weight = tf / (tf + k1 * ((1 - b) + b * (fieldLength / averDocLen)));
      double User_weight = ((1 + k3) * qtf) / (k3 + qtf);
      return IDF * TF_weight * User_weight;
    }
  }

  private double getScoreIndri (RetrievalModelIndri r) throws IOException {
    if (this.docIteratorHasMatchCache()) {
      float mu = r.getMu();
      float lambda = r.getLambda();
      QryIop q = (QryIop) this.args.get(0);
      double tf = q.getTf();
      double ctf = q.getCtf();
      long c_length = Idx.getSumOfFieldLengths(q.getField());
      double mle = ctf / c_length;
      long d_length = Idx.getFieldLength(q.getField(), this.docIteratorGetMatch());
      return (
                 (1-lambda) * ((tf + mu * mle) / (d_length + mu))
                 + lambda * mle
                 );

    }
    return 1.0;
  }

  /**
   *  Initialize the query operator (and its arguments), including any
   *  internal iterators.  If the query operator is of type QryIop, it
   *  is fully evaluated, and the results are stored in an internal
   *  inverted list that may be accessed via the internal iterator.
   *  @param r A retrieval model that guides initialization
   *  @throws IOException Error accessing the Lucene index.
   */
  public void initialize (RetrievalModel r) throws IOException {

    Qry q = this.args.get (0);
    q.initialize (r);
  }

}
