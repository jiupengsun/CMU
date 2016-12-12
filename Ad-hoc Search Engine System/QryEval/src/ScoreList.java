/**
 *  Copyright (c) 2016, Carnegie Mellon University.  All Rights Reserved.
 */
import java.io.*;
import java.util.*;

/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 */
public class ScoreList {

  //  A utility class to create a <internalDocid, externalDocid, score>
  //  object.
  private int max_list_length = Integer.MAX_VALUE;

  public ScoreList(int length) {
    this.max_list_length = length;
  }

  public ScoreList() {}

  private class ScoreListEntry implements Comparable{
    private int docid;
    private String externalId;
    private double score;

    private ScoreListEntry(int internalDocid, double score) {
      this.docid = internalDocid;
      this.score = score;

      try {
        this.externalId = Idx.getExternalDocid (this.docid);
      }
      catch (IOException ex){
        ex.printStackTrace();
      }
    }

    @Override
    public int compareTo(Object o) {
      ScoreListEntry sle = (ScoreListEntry) o;
      if (this.score > sle.score)
        return -1;
      else
      if (this.score < sle.score)
        return 1;
      else {
        if (this.externalId.compareTo(sle.externalId) < 0)
          return -1;
        else if (this.externalId.compareTo(sle.externalId) > 0)
          return 1;
        else
          return 0;
      }
    }
  }

  private boolean unOrdered = true;

  /**
   *  A list of document ids and scores. 
   */
  private List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

  /**
   *
   * @param docid
   * @param score
   */
  public void add(int docid, double score) {
    if (size() < this.max_list_length) {
      // add without checking
      scores.add(new ScoreListEntry(docid, score));
      return;
    } else{
      if (unOrdered) {
        // sort
        sort();
        this.unOrdered = false;
      }
      ScoreListEntry sle = new ScoreListEntry(docid, score);
      // get the min score in the list
      if (sle.compareTo(this.scores.get(this.max_list_length - 1)) > 0)
        // if score is smaller than minScore
        // discard it directly
        return;
      // insert using binary search
      int i=0, j=this.max_list_length-1, mid;
      while (i <= j) {
        mid = (i + j) >> 1;
        ScoreListEntry midsle = this.scores.get(mid);
        if (sle.compareTo(midsle) < 0)
          j = mid - 1;
        else
          i = mid + 1;
      }
      scores.add(i, sle);
    }
  }

  /**
   *  Get the internal docid of the n'th entry.
   *  @param n The index of the requested document.
   *  @return The internal document id.
   */
  public int getDocid(int n) {
    return this.scores.get(n).docid;
  }

  /**
   *  Get the score of the n'th entry.
   *  @param n The index of the requested document score.
   *  @return The document's score.
   */
  public double getDocidScore(int n) {
    return this.scores.get(n).score;
  }

  /**
   *  Set the score of the n'th entry.
   *  @param n The index of the score to change.
   *  @param score The new score.
   */
  public void setDocidScore(int n, double score) {
    this.scores.get(n).score = score;
  }

  /**
   *  Get the size of the score list.
   *  @return The size of the posting list.
   */
  public int size() {
    return this.scores.size();
  }

  /**
   *  Sort the list by score and external document id.
   */
  public void sort () {
    Collections.sort(this.scores);
  }

  /**
   * Reduce the score list to the first num results to save on RAM.
   *
   * @param num Number of results to keep.
   */
  public void truncate(int num) {
    List<ScoreListEntry> truncated = new ArrayList<ScoreListEntry>(this.scores.subList(0,
        Math.min(num, scores.size())));
    this.scores.clear();
    this.scores = truncated;
  }
}
