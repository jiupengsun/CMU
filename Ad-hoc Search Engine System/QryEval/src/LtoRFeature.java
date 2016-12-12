import com.sun.javafx.image.BytePixelSetter;
import org.apache.lucene.index.Term;
import org.omg.CORBA.DoubleHolder;
import org.omg.CORBA.OBJ_ADAPTER;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.server.ExportException;
import java.rmi.server.ObjID;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Created by samy on 10/28/16.
 */
public class LtoRFeature {

  private static Map<Integer, Double> pageRanks = null;

  public static double feature(int featureID, Map<String, Object> params) throws Exception{
    return (double)LtoRFeature.class.getMethod("feature" + featureID, Map.class).invoke(null, params);
  }

  /**
   *
   * Feature 1, return spam score
   * @param params
   * @return
   */
  public static double feature1(Map<String, Object> params) throws Exception{
    Integer docid = (Integer)params.get(Constant.PARAMS_DOC_ID);
    if (docid == null)
      throw new Exception();
    int spamScore = Integer.parseInt(Idx.getAttribute("score", docid));
    return spamScore;
  }

  /**
   *
   * Feature 2, url depth for document
   * @param params
   * @return
   */
  public static double feature2(Map<String, Object> params) throws Exception{
    Integer docid = (Integer)params.get(Constant.PARAMS_DOC_ID);
    if (docid == null)
      throw new Exception();
    String url = Idx.getAttribute("rawUrl", docid);
    int urlDepth = 0;
    for (int i=0, l=url.length(); i<l; ++i)
      if (url.charAt(i) == '/')
        ++urlDepth;
    return urlDepth ;
  }

  /**
   *
   * Feature 3, FromWikipedia score for d
   * @param params
   * @return
   */
  public static double feature3(Map<String, Object> params) throws Exception{
    Integer docid = (Integer)params.get(Constant.PARAMS_DOC_ID);
    if (docid == null)
      throw new Exception();

    String url = Idx.getAttribute("rawUrl", docid);
    if (url.contains("wikipedia.org"))
      return 1;
    else
      return 0;
  }

  /**
   *
   * Feature4, calculate PageRank score
   * @return
   */
  public static double feature4(Map<String, Object> params) throws Exception {
    Integer docid = (Integer)params.get(Constant.PARAMS_DOC_ID);
    String rankFile = (String) params.get(Constant.PARAMS_RANKFILE_PATH);
    if (docid == null || rankFile == null || rankFile.equals(""))
      throw new Exception();
    if (pageRanks == null) {
      pageRanks = new HashMap<>();
      BufferedReader br = null;
      // load pageRank file
      br = new BufferedReader(new FileReader(new File(rankFile)));
      String line;
      while ((line = br.readLine()) != null) {
        String[] rank = line.split("\t");
        Integer internal_id = null;
        try {
          internal_id = Idx.getInternalDocid(rank[0]);
        } catch (Exception e) {
          continue;
        }
        pageRanks.put(internal_id, Double.parseDouble(rank[1]));
      }
      br.close();
    }
    Double score = pageRanks.get(docid);
    return score == null ? 0 : score;

  }

  /**
   * Feature5, calculate BM25 score for <q, d_body>
   * @param params
   * @return
   */
  public static double feature5(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_BODY);
    return BM25Score(params, tm, Constant.BODY_FIELD);
  }

  /**
   * Feature6, calculate Indri score for <q, d_body>
   * @param params
   * @return
   */
  public static double feature6(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_BODY);
    return IndriScore(params, tm, Constant.BODY_FIELD);
  }

  /**
   * Feature7, term overlap score for <q, d_body>
   * @param params
   * @return
   */
  public static double feature7(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_BODY);
    return overlapScore(params, tm);
  }

  /**
   * Feature8, BM25 score for <q, d_title>
   * @param params
   * @return
   */
  public static double feature8(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_TITLE);
    return BM25Score(params, tm, Constant.TITLE_FIELD);
  }

  /**
   * Feature9, Indri score for <q, d_title>
   * @param params
   * @return
   */
  public static double feature9(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_TITLE);
    return IndriScore(params, tm, Constant.TITLE_FIELD);
  }

  /**
   * Feature 10, overlap score for <q, d_title>
   * @param params
   * @return
   */
  public static double feature10(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_TITLE);
    return overlapScore(params, tm);
  }

  /**
   * Feature 11, BM25 score for <q, d_url>
   * @param params
   * @return
   */
  public static double feature11(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_URL);
    return BM25Score(params, tm, Constant.URL_FIELD);
  }

  /**
   * Feature 12, Indri score for <q, d_url>
   * @param params
   * @return
   */
  public static double feature12(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_URL);
    return IndriScore(params, tm, Constant.URL_FIELD);
  }

  /**
   * Feature 13, overlap score for <q, d_url>
   * @param params
   * @return
   */
  public static double feature13(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_URL);
    return overlapScore(params, tm);
  }

  /**
   * Feature 14, BM25 score for <q, d_inlink>
   * @param params
   * @return
   */
  public static double feature14(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_INLINK);
    return BM25Score(params, tm, Constant.INLINK_FIELD);
  }

  /**
   * Feature 15, Indri score for <q, d_inlink>
   * @param params
   * @return
   */
  public static double feature15(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_INLINK);
    return IndriScore(params, tm, Constant.INLINK_FIELD);
  }

  /**
   * Feature 16, overlap score for <q, d_inlink>
   * @param params
   * @return
   */
  public static double feature16(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_INLINK);
    return overlapScore(params, tm);
  }

  /**
   * Feature 17, calculate Vector Space Similarity, using body field
   * @param params
   * @return
   */
  public static double feature17(Map<String,Object> params ) throws Exception {
    String[] queryTerms = (String[]) params.get(Constant.PARAMS_QUERY_TERMS);
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_BODY);
    return calculateVectorSpace(queryTerms, tm);
  }

  /**
   * Feature 18, Indri score for keyword field
   * @param params
   * @return
   * @throws Exception
   */
  public static double feature18(Map<String, Object> params) throws Exception {
    TermVector tm = (TermVector) params.get(Constant.PARAMS_TM_KEYWORD);
    return IndriScore(params, tm, Constant.KEYWORKD_FIELD);
  }

  private static double calculateVectorSpace(String[] queryTerms, TermVector tm) throws Exception {
    if (queryTerms == null || tm == null)
      throw new Exception();
    if (tm.stemsLength() == 0)
      // empty vector
      return Double.MIN_VALUE;
    int qlength = 0;
    for(String stem : queryTerms) {
      int index = tm.indexOfStem(stem);
      if (index < 0)
        continue;
      qlength += tm.stemFreq(index);
    }
    return (double)qlength / (Math.sqrt(queryTerms.length) * Math.sqrt(tm.getNormalizeLength()));
  }

  private static double overlapScore(Map<String, Object> params, TermVector tm) throws Exception{
    String[] queryTerms = (String[]) params.get(Constant.PARAMS_QUERY_TERMS);
    if (queryTerms == null)
      throw new Exception();
    int count = 0;
    for (String q : queryTerms) {
      if (tm.indexOfStem(q) > 0)
        ++count;
    }
    return tm.stemsLength() > 0 ? (double)count / queryTerms.length
               : Double.MIN_VALUE;
  }

  private static double BM25Score (Map<String, Object> params, TermVector tm, String field) throws Exception {
    String[] queryTerms = (String[]) params.get(Constant.PARAMS_QUERY_TERMS);
    Float k1 = (Float) params.get(Constant.PARAMS_BM25_K1);
    Float b = (Float) params.get(Constant.PARAMS_BM25_B);
    Float k3 = (Float) params.get(Constant.PARAMS_BM25_K3);
    if (queryTerms == null || k1 == null
        || b == null || k3 == null)
      throw new Exception();
    double score = 0d;
    try {
      // qtf always set to be 1 here
      // int qtf = 1;
      //double User_weight = ((1 + k3) * qtf) / (k3 + qtf);
      double User_weight = 1d;
      long sumOfFieldLength = Idx.getSumOfFieldLengths(field);
      int docCount = Idx.getDocCount(field);
      long fieldLength = tm.positionsLength();
      double averDocLen = ((double)sumOfFieldLength ) / docCount;
      double tmp_value = k1 * (1 - b + b * (fieldLength / averDocLen));
      double N = Idx.getNumDocs();
      for (String q : queryTerms) {
        int index = tm.indexOfStem(q);
        if (index < 0)
          // if this doc doesn't contain this term
          continue;
        int df = tm.stemDf(index);
        int tf = tm.stemFreq(index);
        double IDF = Math.log((N - df + 0.5) / (df + 0.5));
        double TF_weight = tf / (tf + tmp_value);
        score += IDF * TF_weight * User_weight;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    return tm.stemsLength() > 0 ? score : Double.MIN_VALUE;
  }

  private static double IndriScore(Map<String, Object> params, TermVector tm, String field) throws Exception {
    String[] queryTerms = (String[]) params.get(Constant.PARAMS_QUERY_TERMS);
    Float lambda = (Float) params.get(Constant.PARAMS_INDRI_LAMBDA);
    Float mu = (Float) params.get(Constant.PARAMS_INDRI_MU);
    if (queryTerms == null || lambda == null || mu == null)
      throw new Exception();

    double score = 1d;
    long sumOfFieldLength = Idx.getSumOfFieldLengths(field);
    long d_length = tm.positionsLength();
    double pow = 1 / ((double)queryTerms.length);
    boolean flag = false;
    for (String q : queryTerms) {
      int index = tm.indexOfStem(q);
      int tf = 0;
      if (index > 0) {
        tf = tm.stemFreq(index);
        flag = true;
      }
      double ctf = Idx.getTotalTermFreq(field, q);
      double mle = ctf / sumOfFieldLength;
      double q_score =
          (1-lambda) * ((tf + mu * mle) / (d_length + mu))
              + lambda * mle;
      score *= Math.pow(q_score, pow);
    }
    // if this field doesn't contain any term, return 0
    return tm.stemsLength() > 0 ? (flag ? score : 0) : Double.MIN_VALUE;
  }

}
