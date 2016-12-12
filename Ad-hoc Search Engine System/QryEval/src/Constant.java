/**
 * Created by samy on 10/28/16.
 */
public class Constant {

  public final static String INDEX_PATH = "indexPath";
  public final static String RETRIEVAL_ALGORITHM = "retrievalAlgorithm";
  public final static String QUERY_FILE_PATH= "queryFilePath";
  public final static String TRECEVAL_OUTPUT_PATH = "trecEvalOutputPath";
  public final static String BM25_K1 = "BM25:k_1";
  public final static String BM25_B = "BM25:b";
  public final static String BM25_K3 = "BM25:k_3";
  public final static String INDRI_MU = "Indri:mu";
  public final static String INDRI_LAMBDA = "Indri:lambda";
  public final static String FB = "fb";
  public final static String FB_DOCS = "fbDocs";
  public final static String FB_TERMS = "fbTerms";
  public final static String FB_MU = "fbMu";
  public final static String FB_ORI_WEIGHT = "fbOrigWeight";
  public final static String FB_EXPANSION_QUERY_FILE = "fbExpansionQueryFile";
  public final static String FB_INITIAL_RANKING_FILE = "fbInitialRankingFile";
  public final static String LETOR_TRAINING_QUERY_FILE = "letor:trainingQueryFile";
  public final static String LETOR_TRAINING_QREL_FILE = "letor:trainingQrelsFile";
  public final static String LETOR_TRAINING_FEATURE_VECTOR = "letor:trainingFeatureVectorsFile";
  public final static String LETOR_PAGERANK = "letor:pageRankFile";
  public final static String LETOR_FEATURE_DISABLE = "letor:featureDisable";
  public final static String LETOR_SVM_LEARN = "letor:svmRankLearnPath";
  public final static String LETOR_SVM_CLASSIFY = "letor:svmRankClassifyPath";
  public final static String LETOR_SVM_RANK_PARAMC = "letor:svmRankParamC";
  public final static String LETOR_SVM_RANK_MODEL = "letor:svmRankModelFile";
  public final static String LETOR_TESTING_FEATURE_VECTOR = "letor:testingFeatureVectorsFile";
  public final static String LETOR_TESTING_DOC_SCORES = "letor:testingDocumentScores";

  ///////////////

  public final static String BODY_FIELD = "body";
  public final static String TITLE_FIELD = "title";
  public final static String URL_FIELD = "url";
  public final static String INLINK_FIELD = "inlink";
  public final static String KEYWORKD_FIELD = "inlink";

  public final static int FEATURE_NUMBER = 18;

  // feature parameters
  public final static String PARAMS_DOC_ID = "docid";
  public final static String PARAMS_TM_BODY = "tm_body";
  public final static String PARAMS_TM_TITLE = "tm_title";
  public final static String PARAMS_TM_URL = "tm_url";
  public final static String PARAMS_TM_INLINK = "tm_inlink";
  public final static String PARAMS_TM_KEYWORD = "tm_inlink";
  public final static String PARAMS_BM25_K1= "bm25_k1";
  public final static String PARAMS_BM25_K3 = "bm25_k3";
  public final static String PARAMS_BM25_B = "bm25_b";
  public final static String PARAMS_INDRI_MU = "indri_mu";
  public final static String PARAMS_INDRI_LAMBDA = "indri_lambda";
  public final static String PARAMS_QUERY_TERMS = "query_terms";
  public final static String PARAMS_RANKFILE_PATH = "rankfile";
}
