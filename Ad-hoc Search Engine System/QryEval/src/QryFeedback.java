import java.util.*;

/**
 * Created by samy on 10/21/16.
 */
public class QryFeedback {

  private final String FIELD = "body";

  public String queryExpansion(RetrievalModel model, List<TrecEval> rankDocs, Map<String, String> parameters) {
    StringBuilder expandQuery = new StringBuilder("");
    if (model instanceof RetrievalModelIndri) {
      float mu = Float.parseFloat(parameters.get("fbMu"));
      int termCount = Integer.parseInt(parameters.get("fbTerms"));
      List<TermTuple> termTuples = getTermWeightsIndri(rankDocs, mu, termCount);
      expandQuery.append("#WAND(");
      for (TermTuple tt : termTuples) {
        expandQuery.append(String.format("%.4f", tt.score));
        expandQuery.append(" " + tt.term + " ");
      }
      expandQuery.append(")");
    }

    return expandQuery.toString();
  }

  private List<TermTuple> getTermWeightsIndri(List<TrecEval> rankDocs, float mu, int termCount) {
    Map<String, Map<Integer, Integer>> termMap = new HashMap<>();
    List<TermTuple> termTuples = null;
    try {
      for (TrecEval trec : rankDocs) {
        int docid = trec.getInternalDocId();
        TermVector tv = new TermVector(docid, FIELD);
        for (int i=1, l=tv.stemsLength(); i<l; ++i) {
          String stem = tv.stemString(i);
          // ignore terms has periods
          if (stem.contains(".") || stem.contains(","))
            continue;
          Map<Integer, Integer> docList = termMap.get(stem);
          if (docList == null)
            docList = new HashMap<>();
            // has
          docList.put(docid, tv.stemFreq(i));
          termMap.put(stem, docList);
        }
      }

      termTuples = new ArrayList<>(termMap.size());
      // get the whole term set
      long len_c = Idx.getSumOfFieldLengths(FIELD);
      Iterator<String> term_iterator = termMap.keySet().iterator();
      while (term_iterator.hasNext()) {
        String term = term_iterator.next();
        double term_score = 0.0;
        double ctf = Idx.getTotalTermFreq(FIELD, term);
        double p_MLE = ctf / len_c;
        double m_p_MLE = mu * p_MLE;
        double l_p_MLE = Math.log(1 / p_MLE);
        // get docs that contain this term
        Map<Integer, Integer> map= termMap.get(term);
        for (TrecEval trec : rankDocs) {
          int docid = trec.getInternalDocId();
          int len_d = trec.getBody_length();
          double doc_score = trec.getScore();
          Integer tf = map.get(docid);
          // doc doesn't has this term, then term frequency would be 0
          if (tf == null)
            tf = 0;
          term_score += ((((double)tf) + m_p_MLE) / (len_d + mu)) * doc_score * l_p_MLE;
        }
        TermTuple tt = new TermTuple(term, term_score);
        termTuples.add(tt);
      }

      Collections.sort(termTuples);
      termTuples = termTuples.subList(0, termCount);

    } catch (Exception e) {
      e.printStackTrace();
    }

    return termTuples;
  }

  private List<TermTuple> getTermWeightsIndri1(List<TrecEval> rankDocs, float mu, int termCount) {
    Set<String> termSet = new HashSet<>();
    List<TermTuple> termTuples = new ArrayList<>();
    // traverse all the docs and get a term collection
    try {
      for (TrecEval trec : rankDocs) {
        int docid = trec.getInternalDocId();
        TermVector tv = new TermVector(docid, FIELD);
        for (int i=1, l=tv.stemsLength(); i<l; ++i) {
          String stem = tv.stemString(i);
          // ignore terms has periods
          if (stem.contains(".") || stem.contains(","))
            continue;
          termSet.add(stem);
        }
      }

      long len_c = Idx.getSumOfFieldLengths(FIELD);
      Iterator<String> term_iterator = termSet.iterator();
      while(term_iterator.hasNext()) {
        String term = term_iterator.next();
        double term_score = 0.0;
        double ctf = Idx.getTotalTermFreq(FIELD, term);
        double p_MLE = ctf / len_c;
        for (TrecEval trec : rankDocs) {
          int docid = trec.getInternalDocId();
          int len_d = Idx.getFieldLength(FIELD, docid);
          double doc_score = trec.getScore();
          TermVector tv = new TermVector(docid, FIELD);
          int index = tv.indexOfStem(term);
          double tf = index < 0 ? 0 : tv.stemFreq(index);
          term_score += ((tf + mu * p_MLE) / (len_d + mu)) * doc_score * Math.log(1 / p_MLE);
        }
        TermTuple tt = new TermTuple(term, term_score);
        termTuples.add(tt);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    Collections.sort(termTuples);
    termTuples = termTuples.subList(0, termCount);

    return termTuples;

/*    return sortAndTruncate(termTuples, termCount);*/
  }

  private void addTerm(Map<String, Double> termDics, String term, double score) {
    if (termDics.containsKey(term)) {
      termDics.put(term, termDics.get(term) + score);
    } else {
      termDics.put(term, score);
    }
  }

  /**
   * sort and select the top n terms, n equals termCount
   * @param termTuples
   * @param termCount
   * @return
   */
  private List<TermTuple> sortAndTruncate(List<TermTuple> termTuples, int termCount) {
    TermTuple[] terms = new TermTuple[termTuples.size()];
    termTuples.toArray(terms);
    // selection sort
    for(int i=0; i<termCount; ++i) {
      int index = i;
      for (int j=i+1; j<terms.length; ++j) {
        if (terms[index].compareTo(terms[j]) > 0) {
          index = j;
        }
      }
      // swap i and index
      TermTuple tmp = terms[i];
      terms[i] = terms[index];
      terms[index] = tmp;
    }

    List<TermTuple> list = new ArrayList<>();
    for (int i=0; i<termCount; ++i)
      list.add(terms[i]);

    return list;
  }

  private class TermTuple implements Comparable<TermTuple>{
    String term;
    double score;

    TermTuple(String t, double s) {
      term = t;
      score = s;
    }

    @Override
    public int compareTo(TermTuple o) {
      return score < o.score ? 1 : (score > o.score ? -1 : 0);
    }
  }
}
