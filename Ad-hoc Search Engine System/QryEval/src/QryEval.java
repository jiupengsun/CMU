import java.io.*;
import java.util.*;

/**
 *  This software illustrates the architecture for the portion of a
 *  search engine that evaluates queries.  It is a guide for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 */
public class QryEval {

  //  --------------- Constants and variables ---------------------

  private static final String USAGE =
      "Usage:  java QryEval paramFile\n\n";

  private static final String[] TEXT_FIELDS =
      { "body", "title", "url", "inlink" };

  // maximum output number
  public static final int MAX_OUTPUT_NUMBER = 100;

  // store reference result
  private static Map<String, List<TrecEval>> referenceDocList = new HashMap<>();


  //  --------------- Methods ---------------------------------------

  /**
   *  @param args The only argument is the parameter file name.
   *  @throws Exception Error accessing the Lucene index.
   */
  public static void main(String[] args) throws Exception {

    //  This is a timer that you may find useful.  It is used here to
    //  time how long the entire program takes, but you can move it
    //  around to time specific parts of your code.

    Timer timer = new Timer();
    timer.start ();

    //  Check that a parameter file is included, and that the required
    //  parameters are present.  Just store the parameters.  They get
    //  processed later during initialization of different system
    //  components.

    if (args.length < 1) {
      throw new IllegalArgumentException (USAGE);
    }

    Map<String, String> parameters = readParameterFile (args[0]);

    //  Open the index and initialize the retrieval model.

    Idx.open (parameters.get ("indexPath"));
    RetrievalModel model = initializeRetrievalModel(parameters);

    if (model instanceof RetrievalModelLetor) {
      // letor
      // call a different process

      QryLtoR.run(parameters);
      return;
    }

    // otherwise goes the traditional process
    BufferedWriter bw = null;
    // open file
    try {
      bw = new BufferedWriter(new FileWriter(
                                                new File(parameters.get("trecEvalOutputPath"))));
      String diversity = parameters.get("diversity");
      if (diversity!=null && diversity.toLowerCase().equals("true")) {
        // diversity
        QryDiversity qryDiversity = new QryDiversity(model, bw, parameters);
        qryDiversity.processQuery();
      } else {
        // normal process
        processQueryFile(parameters.get("queryFilePath"), model, bw, parameters);
      }

    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (bw != null)
        bw.close();
    }

    //  Perform experiments.


    //  Clean up.

    timer.stop ();
    System.out.println ("Time:  " + timer);
  }

  /**
   *  Allocate the retrieval model and initialize it using parameters
   *  from the parameter file.
   *  @return The initialized retrieval model
   *  @throws IOException Error accessing the Lucene index.
   */
  private static RetrievalModel initializeRetrievalModel (Map<String, String> parameters)
      throws IOException {

    RetrievalModel model = null;
    String modelString = parameters.get ("retrievalAlgorithm");
    if (modelString==null || modelString.equals(""))
      return null;
    modelString = modelString.toLowerCase();

    switch (modelString) {
      case "unrankedboolean":
        model = new RetrievalModelUnrankedBoolean();
        break;
      case "rankedboolean":
        model = new RetrievalModelRankedBoolean();
        break;
      case "bm25":
        float k1 = Float.parseFloat(parameters.get("BM25:k_1"));
        float k3 = Float.parseFloat(parameters.get("BM25:k_3"));
        float b = Float.parseFloat(parameters.get("BM25:b"));
        model = new RetrievalModelBM25(k1, k3, b);
        break;
      case "indri":
        float mu = Float.parseFloat(parameters.get("Indri:mu"));
        float lambda = Float.parseFloat(parameters.get("Indri:lambda"));
        model = new RetrievalModelIndri(mu, lambda);
        break;
      case "letor":
        model = new RetrievalModelLetor();
        break;

      default:
        throw new IllegalArgumentException
                  ("Unknown retrieval model " + parameters.get("retrievalAlgorithm"));

    }

    return model;
  }

  /**
   * Print a message indicating the amount of memory used. The caller can
   * indicate whether garbage collection should be performed, which slows the
   * program but reduces memory usage.
   *
   * @param gc
   *          If true, run the garbage collector before reporting.
   */
  public static void printMemoryUsage(boolean gc) {

    Runtime runtime = Runtime.getRuntime();

    if (gc)
      runtime.gc();

    System.out.println("Memory used:  "
                           + ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L)) + " MB");
  }

  /**
   * Process one query.
   * @param qString A string that contains a query.
   * @param model The retrieval model determines how matching and scoring is done.
   * @return Search results
   * @throws IOException Error accessing the index
   */
  static ScoreList processQuery(String qString, RetrievalModel model, int max_output_number)
      throws IOException {

    String defaultOp = model.defaultQrySopName ();
    qString = defaultOp + "(" + qString + ")";
    Qry q = QryParser.getQuery (qString);

    // Show the query that is evaluated


    if (q != null) {

      ScoreList r = new ScoreList(max_output_number);

      if (q.args.size () > 0) {		// Ignore empty queries

        q.initialize (model);

        while (q.docIteratorHasMatch (model)) {
          int docid = q.docIteratorGetMatch ();
          double score = ((QrySop) q).getScore (model);
          r.add (docid, score);
          q.docIteratorAdvancePast (docid);
        }
        r.truncate(max_output_number);
      }

      return r;
    } else
      return null;
  }

  /**
   *  Process the query file.
   *  @param queryFilePath
   *  @param model
   *  @throws IOException Error accessing the Lucene index.
   */
  static void processQueryFile(String queryFilePath,
                               RetrievalModel model, BufferedWriter bw, Map<String, String> parameters)
      throws IOException {

    BufferedReader input = null;
    BufferedWriter bw_query = null;

    try {
      String qLine = null;

      input = new BufferedReader(new FileReader(queryFilePath));
      String expansionFile = parameters.get("fbExpansionQueryFile");
      if (expansionFile != null && !expansionFile.equals(""))
        bw_query = new BufferedWriter(new FileWriter(new File(expansionFile)));

      //  Each pass of the loop processes one query.

      while ((qLine = input.readLine()) != null) {
        int d = qLine.indexOf(':');

        if (d < 0) {
          throw new IllegalArgumentException
                    ("Syntax error:  Missing ':' in query line.");
        }

        printMemoryUsage(false);

        // read a query from the file
        String qid = qLine.substring(0, d);
        String query = qLine.substring(d + 1);

        System.out.println("Query " + qLine);

        ScoreList r = null;

        String fb = parameters.get("fb");
        if (fb==null || !fb.toLowerCase().equals("true")) {
          // use the query to retrieve documents directly
          r = processQuery(query, model, MAX_OUTPUT_NUMBER);
        } else {
          String fbInitialRankingFile = parameters.get("fbInitialRankingFile");
          int fbDocs = Integer.parseInt(parameters.get("fbDocs"));
          List<TrecEval> rankDocList = new ArrayList<>();
          if (fbInitialRankingFile==null || fbInitialRankingFile.equals("")) {
            r = processQuery(query, model, MAX_OUTPUT_NUMBER);
            for (int i=0, s=r.size(); i<s && i<fbDocs; ++i) {
              TrecEval te = new TrecEval();
              te.setInternalDocId(r.getDocid(i));
              te.setQueryId(qid);
              te.setScore(r.getDocidScore(i));
              rankDocList.add(te);
            }
          } else {
            rankDocList = referenceDocList.get(qid);
            if (rankDocList == null) {
              // not load reference docs yet
              BufferedReader br = null;
              try {
                br = new BufferedReader(new FileReader(new File(fbInitialRankingFile)));
                if (br != null) {
                  String line;
                  String last_qid = "";
                  List<TrecEval> trecList = null;
                  int i = 0;
                  while( (line = br.readLine()) != null) {
                    String[] trec_line = line.split(" ");
                    if ( !last_qid.equals(trec_line[0]) ) {
                      // start a new query
                        // not the first and i doesn't equal to fbDocs
                      trecList = new ArrayList<>();
                      last_qid = trec_line[0];
                      TrecEval te = new TrecEval();
                      te.setExternalDocId(trec_line[2]);
                      te.setQueryId(trec_line[0]);
                      te.setScore(Double.parseDouble(trec_line[4]));
                      trecList.add(te);
                      i = 1;
                    } else if (i < fbDocs) {
                      TrecEval te = new TrecEval();
                      te.setExternalDocId(trec_line[2]);
                      te.setQueryId(trec_line[0]);
                      te.setScore(Double.parseDouble(trec_line[4]));
                      trecList.add(te);
                      ++i;
                      if (i == fbDocs) {
                        // store
                        referenceDocList.put(last_qid, trecList);
                        trecList = new ArrayList<>();
                      }
                    } else {
                      continue;
                    }
                  }
                  /*
                  there's a bug here, if fbdocs is larger than 100, then the last
                  block would not be stored in the map. However, because this program
                  will not output more than 100 results, so not fix this bug here.
                   */
                }
              } catch (IOException e) {
                System.out.println("Cannot open fbinitial rank file");
                System.exit(1);
              }
            }
            rankDocList = referenceDocList.get(qid);
          }

          //
          if (rankDocList.size() > 0) {
            QryFeedback qr = new QryFeedback();
            String expandQuery = qr.queryExpansion(model, rankDocList, parameters);
            // write new query to file
            bw_query.write(qid + ":" + expandQuery + "\n");
            StringBuilder newQuery = new StringBuilder();
            float weight = Float.parseFloat(parameters.get("fbOrigWeight"));
            newQuery.append("#WAND(");
            newQuery.append(weight + " #AND(" + query + ") ");
            newQuery.append((1-weight) + " " + expandQuery);
            newQuery.append(")");
            r = processQuery(newQuery.toString(), model, MAX_OUTPUT_NUMBER);
          }
        }


        if (r != null) {
          printResults(qid, r);
          writeResults(qid, r, bw);
          System.out.println();
        } else {
          bw.write(qid + " Q0 dummy 1 0 run-1\n");
        }
      }
    } catch (IOException ex) {
      ex.printStackTrace();
    } finally {
      input.close();
      if (bw_query != null) {
        bw_query.flush();
        bw_query.close();
      }
    }
  }

  static void writeResults(String qid, ScoreList r, BufferedWriter bw) throws IOException {
    for (int i=0, s=r.size(); i<s; ++i) {
      bw.write(qid + " Q0 " + Idx.getExternalDocid(r.getDocid(i)) +
                   " " + (i+1) + " " + String.format("%.12f", r.getDocidScore(i)) + " run-1\n");
    }

  }

  static void writeResults(String qid, List<TrecEval> trecList, BufferedWriter bw) throws IOException {
    int i = 1;
    for (TrecEval te : trecList) {
      bw.write(qid + " Q0 " + te.getExternalDocId() +
                   " " + (i++) + " " + String.format("%.12f", te.getScore()) + " run-1\n");
    }

  }

  static void writeResults(String qid, ArrayList<Doc> resultList, BufferedWriter bw) throws IOException {
    int i = 1;
    for (Doc doc: resultList) {
      bw.write(qid + " Q0 " + doc.externalId +
                   " " + (i++) + " " + String.format("%.12f", doc.score_o) + " run-1\n");
    }

  }

  /**
   * Print the query results.
   *
   * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
   * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
   *
   * QueryID Q0 DocID Rank Score RunID
   *
   * @param queryName
   *          Original query.
   * @param result
   *          A list of document ids and scores
   * @throws IOException Error accessing the Lucene index.
   */
  static void printResults(String queryName, ScoreList result) throws IOException {

    System.out.println(queryName + ":  ");
    if (result.size() < 1) {
      System.out.println(queryName + " Q0 dummy 1 0 run-1\n");
    } else {
      for (int i = 0; i < result.size(); i++) {
        System.out.println("\t" + queryName+ " Q0 " + Idx.getExternalDocid(result.getDocid(i)) +
                               " " + (i+1) + " " + String.format("%.12f", result.getDocidScore(i)) + " run-1");
      }
    }
  }

  /**
   *  Read the specified parameter file, and confirm that the required
   *  parameters are present.  The parameters are returned in a
   *  HashMap.  The caller (or its minions) are responsible for processing
   *  them.
   *  @return The parameters, in <key, value> format.
   */
  private static Map<String, String> readParameterFile (String parameterFileName)
      throws IOException {

    Map<String, String> parameters = new HashMap<String, String>();

    File parameterFile = new File (parameterFileName);

    if (! parameterFile.canRead ()) {
      throw new IllegalArgumentException
                ("Can't read " + parameterFileName);
    }

    Scanner scan = new Scanner(parameterFile);
    String line = null;
    do {
      line = scan.nextLine().trim();
      // jump to empty line
      if (line.equals("") || line.startsWith("#"))
        continue;
      String[] pair = line.split ("=");
      if (pair.length == 2)
        parameters.put(pair[0].trim(), pair[1].trim());
    } while (scan.hasNext());

    scan.close();

    if (! (parameters.containsKey ("indexPath") &&
               parameters.containsKey ("queryFilePath") &&
               parameters.containsKey ("trecEvalOutputPath") )) {
      throw new IllegalArgumentException
                ("Required parameters were missing from the parameter file.");
    }

    return parameters;
  }

}
