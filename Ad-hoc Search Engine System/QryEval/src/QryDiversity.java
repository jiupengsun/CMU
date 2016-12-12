import java.io.*;
import java.sql.ResultSet;
import java.util.*;

/**
 * Created by samy on 11/17/16.
 */
public class QryDiversity {
  private RetrievalModel model;
  private BufferedWriter bw = null;
  private Map<String, String> parameters = null;
  private File queryFile = null;
  private File initialRankingFile = null;
  private String algorithm;
  private int maxInputRankingLength = 100;
  private int maxResultRankingLength = 100;
  private float lambda = 0f;
  private Map<String, List<Doc>> standardRanking = null;

  public QryDiversity(RetrievalModel rm, BufferedWriter bw, Map<String, String> param) {
    this.model = rm;
    this.bw = bw;
    this.parameters = param;
  }

  private class SortComparator implements Comparator<Doc> {

    @Override
    public int compare(Doc o1, Doc o2) {
      return o1.score_o > o2.score_o ? -1 : (o1.score_o < o2.score_o ? 1 : 0);
    }
  }

  public void processQuery() throws Exception{
    checkParameters();
    List<Query> queries = prepareQuery();
    if(this.initialRankingFile != null) {
      readInitialRankingFile();
    }
    if(this.algorithm.equals("pm2")) {
      PM2(queries);

    } else if(this.algorithm.equals("xquad")) {
      xQuAD(queries);
    }
  }

  private void readInitialRankingFile() throws Exception{
    BufferedReader br = new BufferedReader(new FileReader(this.initialRankingFile));
    standardRanking = new HashMap<>();
    String line;
    Map<String, Map<String, Double>> intendMap = new HashMap<>();
    while( (line=br.readLine()) != null) {
      String[] split = line.split(" ");
      String qid = split[0];
      String eid = split[2];
      double score = Double.parseDouble(split[4]);

      if (qid.contains(".")) {
        // intend doc
        Map<String, Double> map = intendMap.get(qid);
        if (map == null)
          map = new HashMap<>();
        if(map.size() >= this.maxInputRankingLength)
          continue;
        map.put(eid, score);
        intendMap.put(qid, map);
      } else {
        // original doc
        List<Doc> list = standardRanking.get(qid);
        if(list == null)
          list = new ArrayList<>();
        if(list.size() >= this.maxInputRankingLength)
          continue;
        list.add(new Doc(eid, score));
        standardRanking.put(qid, list);
      }
    }

    // append intend score to doc list
    Iterator<String> it = standardRanking.keySet().iterator();
    while(it.hasNext()) {
      String qid = it.next();
      List<Doc> list = standardRanking.get(qid);
      boolean isNormalized = true;
      for(Doc d : list) {
        int intend = 1;
        if (d.score_o >= 1)
          isNormalized = false;
        while(true) {
          String inid = qid+"."+(intend++);
          Map<String, Double> map = intendMap.get(inid);
          if (map == null)
            break;
          Double score = map.get(d.externalId);
          if (score == null)
            score = 0d;
          if (score >= 1)
            isNormalized = false;
          d.score_list.add(score);
        }
      }
      // need normalization
      if (!isNormalized)
        normalization(list, list.get(0).score_list.size());

    }

    br.close();
  }

  /**
   * xQuAD algorithm
   * @param queries
   */
  private void xQuAD(List<Query> queries) throws Exception{
    for(Query q : queries) {
      ArrayList<Doc> resultList = new ArrayList<>(this.maxResultRankingLength);

      int intend = q.queryDivList.size();
      if (intend == 0)
        // intend should be larger than 0
        continue;

      List<Doc> list = retrieveDiversityList(q);
      // have got a Doc list
      // use xQuAD in the follow
      double max_score = Integer.MIN_VALUE;
      int index = -1;
      double r_intend = 1d/intend;
      // transform list from ArrayList to LinkedList for future editing
      list = new LinkedList<>(list);
      int count = 0;
      while(!list.isEmpty() && count++<this.maxResultRankingLength) {
        for(int i=0, l=list.size(); i<l; ++i) {
          Doc di = list.get(i);
          double score = 0d;
          for(int j=0; j<intend; ++j) {
            double tmp = r_intend * di.score_list.get(j);
            for(Doc rd: resultList) {
              tmp *= (1 - rd.score_list.get(j));
            }
            score += tmp;
          }
          score = (1-lambda) * di.score_o + lambda * score;
          if(score > max_score) {
            max_score = score;
            index = i;
          }
        }
        // after an iteration, choose top 1, remove it
        // from list and add to result list
        Doc doc = list.remove(index);
        doc.score_o = max_score;
        resultList.add(doc);
        // initialize max_score
        max_score = Integer.MIN_VALUE;
      }
      Collections.sort(resultList, new SortComparator());
      // output result list
      QryEval.writeResults(q.qid, resultList, this.bw);
    }
  }

  /**
   *
   * @param queries
   * @throws Exception
   */
  private void PM2(List<Query> queries) throws Exception {
    for(Query q : queries) {
      ArrayList<Doc> resultList = new ArrayList<>(this.maxResultRankingLength);

      int intend = q.queryDivList.size();
      if(intend == 0)
        // intend should be larger than 0
        continue;

      List<Doc> list = retrieveDiversityList(q);

      // have got a normalized doc list
      // use PM2 algorithm
      double V = (double)this.maxResultRankingLength / intend;
      // S vector are initialized to zero
      double[] S = new double[intend];
      double[] Qt = new double[intend];
      int count = 0;
      double max_score = Integer.MIN_VALUE;
      int index = -1;
      while(!list.isEmpty() && count++ < this.maxResultRankingLength) {
        // calculate qt vector
        // qt_i = v_i/(2s_i+1)
        int intend_index = 0;
        for(int i=0; i<intend; ++i) {
          Qt[i] = V / (2 * S[i] + 1);
          if(Qt[i] > Qt[intend_index])
            intend_index = i;
        }
        for(int i=0, s=list.size(); i<s; ++i) {
          Doc doc = list.get(i);
          double score = 0;
          for(int j=0; j<intend; ++j) {
            if(j == intend_index)
              score += lambda * Qt[j] * doc.score_list.get(j);
            else
              score += (1-lambda) * Qt[j] * doc.score_list.get(j);
          }
          if(score > max_score) {
            max_score = score;
            index = i;
          }
        }
        Doc doc = list.remove(index);
        doc.score_o = max_score;
        resultList.add(doc);
        max_score = Integer.MIN_VALUE;
        // update S vector
        // sum intend value
        double sumIntend = 0;
        for(double d : doc.score_list)
          sumIntend += d;
        if(sumIntend != 0) {
          for (int i = 0; i < intend; ++i) {
            S[i] += doc.score_list.get(i) / sumIntend;
          }
        }
      }
      Collections.sort(resultList, new SortComparator());
      QryEval.writeResults(q.qid, resultList, this.bw);
    }
  }

  /**
   *
   * @param q
   * @return
   */
  private List<Doc> retrieveDiversityList(Query q) throws Exception {
    List<Doc> list = null;
    if(this.initialRankingFile == null) {
      list = new ArrayList<>();
      // use query to search
      ScoreList r = QryEval.processQuery(q.query, model, this.maxInputRankingLength);
      // add original query
      for(int i=0; i<r.size(); ++i) {
        Doc doc = new Doc(r.getDocid(i), r.getDocidScore(i));
        list.add(doc);
      }
      // search and add diversity query
      for(String q_div : q.queryDivList) {
        ScoreList r_div = QryEval.processQuery(q_div, model, this.maxInputRankingLength);
        // transform to Map for future searching
        Map<Integer, Double> map = new HashMap<>();
        for(int i=0; i<r_div.size(); ++i) {
          map.put(r_div.getDocid(i), r_div.getDocidScore(i));
        }
        // add to list
        for(Doc doc : list) {
          Double s = map.get(doc.docid);
          doc.score_list.add(s==null ? 0d : s);
        }
      }
      // normalization
      if (this.model instanceof RetrievalModelBM25) {
        normalization(list, q.queryDivList.size());
      }
    } else {
      // use initial ranking file
      list = standardRanking.get(q.qid);
      if (list == null || list.size() == 0)
        throw new Exception("No initial ranking query");
    }
    return list;
  }

  private void normalization(List<Doc> list, int intend) {
    // sum and count
    if(list == null || list.size() == 0)
      return;
    // calculate the maximum denominator
    double[] sum_score = new double[intend + 1];
    for(Doc d : list) {
      sum_score[0] += d.score_o;
      for(int i=0; i<intend; ++i)
        sum_score[i+1] += d.score_list.get(i);
    }
    double max_score = Integer.MIN_VALUE;
    for(double d : sum_score)
      max_score = Math.max(max_score, d);
    // normalize
    for(Doc d : list) {
      d.score_o /= max_score;
      List<Double> o_list = d.score_list;
      d.score_list = new ArrayList<>();
      for(double s : o_list)
        d.score_list.add(s/max_score);
    }
  }

  private List<Query> prepareQuery() throws Exception{
    BufferedReader br = new BufferedReader(new FileReader(queryFile));
    String intentQueryFile = parameters.get("diversity:intentsFile");
    BufferedReader br_div = new BufferedReader(new FileReader(new File(intentQueryFile)));
    List<Query> queryList = new ArrayList<>();
    String qLine;
    String divLine = null;
    divLine = br_div.readLine();
    while( divLine!= null && (qLine = br.readLine()) != null) {
      int d = qLine.indexOf(':');

      if (d < 0) {
        continue;
      }

      // read a query from the file
      String qid = qLine.substring(0, d);
      String query = qLine.substring(d + 1);
      Query Q = new Query(qid, query);
      do {
        if(!divLine.startsWith(qid))
          // not match to current qid
          break;
        String qdiversity = divLine.substring(divLine.indexOf(":") + 1);
        Q.queryDivList.add(qdiversity);
      } while( (divLine = br_div.readLine()) != null);
      queryList.add(Q);
    }

    br_div.close();
    br.close();

    return queryList;
  }

  private void checkParameters() throws Exception {
    if(bw == null || parameters == null)
      throw new Exception("Wrong initialization");
    String path = parameters.get("queryFilePath");
    if (path==null || path.equals(""))
      throw new Exception("Wrong initialization");
    queryFile = new File(path);
    if (!queryFile.exists())
      throw new Exception("Cannot load query file");
    String algorithm = parameters.get("diversity:algorithm");
    if (algorithm==null || algorithm.equals(""))
      throw new Exception("Algorithm not supported");
    this.algorithm = algorithm.trim().toLowerCase();
    String initialRF = parameters.get("diversity:initialRankingFile");
    if (initialRF!=null && !initialRF.equals("")) {
      this.initialRankingFile = new File(initialRF);
    }
    if (initialRF==null && this.model==null) {
      throw new Exception("Retrieve Model not defined");
    }
    String mirl = parameters.get("diversity:maxInputRankingsLength");
    String mrrl = parameters.get("diversity:maxResultRankingLength");
    this.maxInputRankingLength = mirl!=null && !mirl.equals("") ? Integer.parseInt(mirl) : this.maxResultRankingLength;
    this.maxResultRankingLength = mrrl !=null && !mrrl.equals("") ? Integer.parseInt(mrrl) : this.maxResultRankingLength;
    String lambdaString = parameters.get("diversity:lambda");
    this.lambda = lambdaString!=null && !lambdaString.equals("") ? Float.parseFloat(lambdaString) : lambda;
  }

  private class Query {
    String qid;
    String query;
    List<String> queryDivList;
    Query(String id, String q) {
      qid = id;
      query = q;
      queryDivList = new ArrayList<>();
    }
  }
}

class Doc {
  int docid;
  String externalId;
  double score_o;
  List<Double> score_list;
  Doc(String eid, double s) {
    externalId = eid;
    score_o = s;
    score_list = new ArrayList<>();
  }
  Doc(int id, double score) {
    docid = id;
    try {
      externalId = Idx.getExternalDocid(id);
    } catch (IOException e) {
      e.printStackTrace();
    }
    score_o = score;
    score_list = new ArrayList<>();
  }
}
