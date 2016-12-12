import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by samy on 9/14/16.
 */
public class QryIopNear extends QryIop{

  private int distance = 0;

  /**
   * set display name and distance of near operator
   * @param name The query operator's display name
   */
  @Override
  public void setDisplayName(String name) {
    String[] params = name.split("/");
    try {
      distance = Integer.parseInt(params[1].trim());
    } catch (NumberFormatException e) {
      e.printStackTrace();
      distance = 0;
    }
    super.setDisplayName(name);
  }

  @Override
  protected void evaluate() throws IOException {
    // generate new invertedList
    this.invertedList = new InvList(this.getField());

    if (args.size() == 0)
      return;

    // find all match
    while (this.docIteratorHasMatchAll(null)) {
      QryIop q_0 = (QryIop) this.args.get(0);
      int docid = q_0.docIteratorGetMatch();

      List<Integer> posList = new ArrayList<>();

      while(q_0.locIteratorHasMatch()) {
        boolean isMatch = true, isEnd = false;
        int pos_0 = q_0.locIteratorGetMatch();

        for (int i=1, l=this.args.size(); i<l; ++i) {
          QryIop q_i = (QryIop) this.args.get(i);
          q_i.locIteratorAdvancePast(pos_0);

          if (q_i.locIteratorHasMatch()) {
            int pos_i = q_i.locIteratorGetMatch();
            if (pos_i - pos_0 <= distance)
              pos_0 = pos_i;
            else {
              isMatch = false;
              break;
            }
          } else {
            isEnd = true;
            break;
          }
        }

        if (isEnd)
          break;

        if (isMatch)
          // found one
          posList.add(pos_0);

        q_0.locIteratorAdvancePast(pos_0);
      }

      if (posList.size() > 0) {
        // find positions
        this.invertedList.appendPosting(docid, posList);
      }

      // move to process the next document
      q_0.docIteratorAdvancePast(docid);
    }
  }

  /**
  @Override
  protected void evaluate() throws IOException {

    // generate new invertedList
    this.invertedList = new InvList(this.getField());

    if (args.size() == 0)
      return;

    // find all match
    while (this.docIteratorHasMatchAll(null)) {
      QryIop q_0 = (QryIop) this.args.get(0);
      int docid = q_0.docIteratorGetMatch();
      // find same doc id
      int pos = 0;
      List<Integer> posList = new ArrayList<>();
      while (pos >= 0) {
        boolean isMatch = true;
        // the first args - QryIopTerm
        // get position vector
        InvList.DocPosting doc_0 = q_0.docIteratorGetMatchPosting();
        // exceeds max position
        // end loop
        if (pos >= doc_0.tf)
          break;
        // get the pos'th position in the vector
        int pos_0 = doc_0.positions.get(pos);
        for(int i=1, l=this.args.size(); i<l; ++i) {
          QryIop q_i = (QryIop) this.args.get(i);
          InvList.DocPosting doc_i = q_i.docIteratorGetMatchPosting();
          if (pos >= doc_i.tf) {
            // end loop
            pos = -1;
            isMatch = false;
            break;
          }
          int pos_i = doc_i.positions.get(pos);
          // (0, distance)
          if (pos_i - pos_0 > 0 && pos_i - pos_0 < distance)
            pos_0 = pos_i;
          else {
            // failed to match
            // to the next loop
            isMatch = false;
            break;
          }
        }

        // match a position
        if (isMatch) {
          // find a match
          // insert into new position
          // add last term pos
          posList.add(pos_0);
        }

        if (pos >= 0)
          ++pos;

      }
      if (posList.size() > 0) {
        // find a matched list
        // insert into new invertedList
        Collections.sort(posList);
        this.invertedList.appendPosting(docid, posList);
      }
      q_0.docIteratorAdvancePast(docid);
    }
  }
  */
}
