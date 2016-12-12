import javax.management.Query;
import java.io.*;
import java.nio.Buffer;
import java.util.*;

/**
 * Created by samy on 10/28/16.
 */
public class QryLtoR {

  public static void run(Map<String, String> parameters) {
    // read training queries and relevance judgement from input files
    String trainingQueryFile = parameters.get(Constant.LETOR_TRAINING_QUERY_FILE);
    String trainingQrelsFile = parameters.get(Constant.LETOR_TRAINING_QREL_FILE);
    String queryFile = parameters.get(Constant.QUERY_FILE_PATH);

    List<QueryTerms> trainingQueryList = parseQueryFile(trainingQueryFile);
    Map<String, List<TrecEval>> trainingQrelsMap = parseQrelFile(trainingQrelsFile);

    // generate and write the feature vectors
    generateFeatureVectors(parameters, trainingQueryList, trainingQrelsMap, true);
    // train SVM
    callSVM(parameters, true);
    //
    List<QueryTerms> queryList = parseQueryFile(queryFile);
    // retrieval using BM25
    Map<String, List<TrecEval>> resultsMap = queryWithBM25(queryList, parameters);
    //
    generateFeatureVectors(parameters, queryList, resultsMap, false);
    // calculate the score with SVM
    callSVM(parameters, false);
    // generate final trec file
    readSVMScore(parameters, resultsMap, queryList);
  }

  private static void readSVMScore(Map<String, String> parameters, Map<String, List<TrecEval>> resultsMap, List<QueryTerms> queryList) {
    String svmScoreOutputFile = parameters.get(Constant.LETOR_TESTING_DOC_SCORES);
    String trecOutputFile = parameters.get(Constant.TRECEVAL_OUTPUT_PATH);
    File f = new File(svmScoreOutputFile);
    if (f.exists()) {
      BufferedReader br = null;
      BufferedWriter bw = null;

      try {
        br = new BufferedReader(new FileReader(f));
        bw = new BufferedWriter(new FileWriter(new File(trecOutputFile)));

        String line;
        for (QueryTerms query : queryList) {
          List<TrecEval> docList = resultsMap.get(query.getQid());
          for (TrecEval te : docList) {
            line = br.readLine();
            if (line != null)
              te.setScore(Double.parseDouble(line));
          }
          Collections.sort(docList);
          // output to trec file
          QryEval.writeResults(query.getQid(), docList, bw);
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
          try {
            if (br != null)
              br.close();
            if (bw != null)
              bw.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
      }

    }
  }

  private static Map<String,List<TrecEval>> parseQrelFile(String filePath) {
    if (filePath == null)
      return null;
    Map<String, List<TrecEval>> docMap = new HashMap<>();
    File file = new File(filePath);
    if (file.exists()) {
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(file));
        String line = null;
        while ( (line = br.readLine()) != null) {
          String[] d = line.trim().split(" ");
          // qid digit external_id relevance
          TrecEval te = new TrecEval();
          te.setQueryId(d[0]);
          te.setExternalDocId(d[2]);
          te.setScore(Double.parseDouble(d[3]));
          if(te.getInternalDocId() > 0) {
            // jump non exist doc
            List<TrecEval> docList = docMap.get(d[0]);
            if (docList == null)
              docList = new ArrayList<>();
            docList.add(te);
            docMap.put(d[0], docList);
          }
        }
        return docMap;
      } catch (IOException e) {
        e.printStackTrace();
        if (br != null)
          try {
            br.close();
          } catch (IOException e1) {
            e1.printStackTrace();
          }
      }
    }
    return null;
  }

  private static List<QueryTerms> parseQueryFile(String filePath) {
    if (filePath == null)
      return null;
    File file = new File(filePath);
    if (file.exists()) {
      List<QueryTerms> queryList = new ArrayList<>();
      BufferedReader br = null;
      try {
        br = new BufferedReader(new FileReader(file));
        String line = null;
        while ( (line = br.readLine()) != null) {
          String[] q = line.trim().split(":");
          QueryTerms qt = new QueryTerms();
          qt.setOriginQuery(q[1]);
          qt.setQid(q[0]);
          qt.setTerms(QryParser.tokenizeString(q[1]));
          queryList.add(qt);
        }
        return queryList;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }

  /**
   * execute bm25 model
   * @param parameters
   * @return
   */
  private static Map<String, List<TrecEval>> queryWithBM25(List<QueryTerms> queryList, Map<String, String> parameters) {
    float bm25_k3 = Float.parseFloat(parameters.get(Constant.BM25_K3));
    float bm25_b = Float.parseFloat(parameters.get(Constant.BM25_B));
    float bm25_k1 = Float.parseFloat(parameters.get(Constant.BM25_K1));
    RetrievalModel bm25Model = new RetrievalModelBM25(bm25_k1, bm25_k3, bm25_b);
    Map<String, List<TrecEval>> docMap = new HashMap<>();
    try {
      for (QueryTerms query : queryList) {
        ScoreList r = QryEval.processQuery(query.getOriginQuery(), bm25Model, QryEval.MAX_OUTPUT_NUMBER);
        List<TrecEval> docList = new ArrayList<>();
        for (int i=0, l=r.size(); i<l; ++i) {
          TrecEval te = new TrecEval();
          int docid = r.getDocid(i);
          te.setInternalDocId(docid);
          te.setExternalDocId(Idx.getExternalDocid(docid));
          te.setQueryId(query.getQid());
          te.setScore(r.getDocidScore(i));
          docList.add(te);
        }
        docMap.put(query.getQid(), docList);

      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return docMap;
  }

  private static void generateFeatureVectors(Map<String, String> parameters, List<QueryTerms> qt,
                                             Map<String, List<TrecEval>> docMap, boolean isTraining) {
    String svmTrainingFeatureVectorFile = parameters.get(Constant.LETOR_TRAINING_FEATURE_VECTOR);
    String svmTestingFeatureVectorFile = parameters.get(Constant.LETOR_TESTING_FEATURE_VECTOR);
    String svmFeatureDiasble = parameters.get(Constant.LETOR_FEATURE_DISABLE);
    Set<Integer> featureDis = new HashSet<>();
    if (svmFeatureDiasble != null && !svmFeatureDiasble.equals("")) {
      String[] fs = svmFeatureDiasble.split(",");
      for (String ss : fs) {
        featureDis.add(Integer.parseInt(ss));
      }
    }

    BufferedWriter bw = null;
    // write to training or testing feature vector
    String featureVectorFile = isTraining ? svmTrainingFeatureVectorFile : svmTestingFeatureVectorFile;

    try {
      bw = new BufferedWriter(new FileWriter(new File(featureVectorFile)));
      // iteratively process query
      for (QueryTerms query : qt) {
        String[] qterms = query.getTerms();

        // get doclist related to specific query
        List<TrecEval> docList = docMap.get(query.getQid());

        // two-dimension score array
        // the last two row stores the maximum and minimum value of each feature
        // the first column stores the relative score
        int row_length = docList.size() + 2;
        int column_length = Constant.FEATURE_NUMBER + 1;
        double[][] featureVectors = new double[row_length][column_length];
        // initialize, row[length-2] is the maximum value array
        // row[length-1] is the minimum value array
        for (int i=docList.size(), j=1; j<=Constant.FEATURE_NUMBER; ++j)
          featureVectors[i][j] = Double.MIN_VALUE;
        for (int i=docList.size() + 1, j=1; j<Constant.FEATURE_NUMBER; ++j)
          featureVectors[i][j] = Double.MAX_VALUE;
        int current_row = 0;

        for(TrecEval te : docList) {

          /**
           * execute each feature
           */
          int docid = te.getInternalDocId();
          TermVector tm_body = new TermVector(docid, Constant.BODY_FIELD);
          TermVector tm_title = new TermVector(docid, Constant.TITLE_FIELD);
          TermVector tm_url = new TermVector(docid, Constant.URL_FIELD);
          TermVector tm_inlink = new TermVector(docid, Constant.INLINK_FIELD);
          float bm25_k1 = Float.parseFloat(parameters.get(Constant.BM25_K1));
          float bm25_k3 = Float.parseFloat(parameters.get(Constant.BM25_K3));
          float bm25_b = Float.parseFloat(parameters.get(Constant.BM25_B));
          float indri_mu = Float.parseFloat(parameters.get(Constant.INDRI_MU));
          float indri_lambda = Float.parseFloat(parameters.get(Constant.INDRI_LAMBDA));
          String rankFile = parameters.get(Constant.LETOR_PAGERANK);
          // setting parameters
          Map<String, Object> params = new HashMap<>();
          params.put(Constant.PARAMS_DOC_ID, docid);
          params.put(Constant.PARAMS_TM_BODY, tm_body);
          params.put(Constant.PARAMS_TM_TITLE, tm_title);
          params.put(Constant.PARAMS_TM_URL, tm_url);
          params.put(Constant.PARAMS_TM_INLINK, tm_inlink);
          params.put(Constant.PARAMS_BM25_K1, bm25_k1);
          params.put(Constant.PARAMS_BM25_K3, bm25_k3);
          params.put(Constant.PARAMS_BM25_B, bm25_b);
          params.put(Constant.PARAMS_INDRI_MU, indri_mu);
          params.put(Constant.PARAMS_INDRI_LAMBDA, indri_lambda);
          params.put(Constant.PARAMS_QUERY_TERMS, qterms);
          params.put(Constant.PARAMS_RANKFILE_PATH, rankFile);

          featureVectors[current_row][0] = te.getScore();

          for (int j=1; j<column_length; ++j) {
            if (featureDis.contains(j))
              // skip disabled feature
              continue;
            try {
              double score = LtoRFeature.feature(j, params);
              featureVectors[current_row][j] = score;
              if (score == Double.MIN_VALUE)
                continue;
              featureVectors[row_length - 2][j] = Math.max(featureVectors[row_length - 2][j],
                  featureVectors[current_row][j]);
              featureVectors[row_length - 1][j] = Math.min(featureVectors[row_length - 1][j],
                  featureVectors[current_row][j]);
            } catch (Exception e) {
              e.printStackTrace();
            }
          }

          ++current_row;
        }

        // normalize
        for(int j=1; j<column_length; ++j) {
          if (featureDis.contains(j))
            // skip disabled feature
            continue;
          // max_value - min_value
          double denominator = featureVectors[row_length - 2][j] -
                                   featureVectors[row_length - 1][j];
          for (int i=0; i<row_length - 2; ++i) {
            // if all the value in a feature are the same
            // then set this vector to zero
            if (denominator == 0 || featureVectors[i][j] == Double.MIN_VALUE)
              //if (denominator == 0)
              featureVectors[i][j] = 0;
            else
              // (feature_value - min_value) / (max_value - min_value)
              featureVectors[i][j] = (featureVectors[i][j] - featureVectors[row_length - 1][j])
                                         / denominator;
          }
        }

        // output vector
        for (int i=0; i<row_length - 2; ++i) {
          StringBuilder line = new StringBuilder("");
          line.append((int)featureVectors[i][0] + " ");
          line.append("qid:" + query.getQid() + " ");

          for (int j = 1; j < column_length; ++j) {
            if (featureDis.contains(j))
              // don't output
              continue;
            line.append(j + ":" + String.format("%.12f", featureVectors[i][j]) + " ");
          }
          line.append(" # " + docList.get(i).getExternalDocId());
          line.append("\n");
          bw.write(line.toString());
        }
      }

    } catch (IOException e) {

    } finally {
      if (bw != null) {
        try {
          bw.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
  }

  /**
   * execute SVM through cmd
   * @param parameters
   */
  private static void callSVM(Map<String, String> parameters, boolean isTraining) {
    String svmTrainingFeatureVectorFile = parameters.get(Constant.LETOR_TRAINING_FEATURE_VECTOR);
    String svmTestingFeatureVectorFile = parameters.get(Constant.LETOR_TESTING_FEATURE_VECTOR);
    String svmScoreOutputFile = parameters.get(Constant.LETOR_TESTING_DOC_SCORES);
    String svmLearnExe = parameters.get(Constant.LETOR_SVM_LEARN);
    String svmClassifyExe = parameters.get(Constant.LETOR_SVM_CLASSIFY);
    String param_c = parameters.get(Constant.LETOR_SVM_RANK_PARAMC);
    String model_output = parameters.get(Constant.LETOR_SVM_RANK_MODEL);
    Process cmdProc;
    try {
      if (isTraining) {
        cmdProc = Runtime.getRuntime().exec(
            new String[]{svmLearnExe, "-c", param_c, svmTrainingFeatureVectorFile, model_output}
        );
      } else {
        cmdProc = Runtime.getRuntime().exec(
            new String[]{svmClassifyExe, svmTestingFeatureVectorFile, model_output, svmScoreOutputFile}
        );

      }
      BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
      String line;
      while ( (line = stdoutReader.readLine()) != null) {
        System.out.println(line);
      }
      // consume stderr and print it for debugging purposes
      BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()));
      while ( (line = stderrReader.readLine()) != null) {
        System.out.println(line);
      }

      // get the return value from the executable
      // 0 means success, otherwise indicates a problem
      int retValue = cmdProc.waitFor();
      if (retValue != 0) {
        throw new Exception("SVM Rank crashed");
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
