import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samy on 10/1/16.
 */
public class QryIopWindow extends QryIop{
  private int distance = 0;
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
    this.invertedList = new InvList(this.getField());

    if (args.size() == 0)
      return;

    while (this.docIteratorHasMatchAll(null)) {
      QryIop q_0 = (QryIop) this.args.get(0);
      int docid = q_0.docIteratorGetMatch();

      List<Integer> posList = new ArrayList<>();
      while(q_0.locIteratorHasMatch()) {
        boolean isEnd = false;
        int pos_0 = q_0.locIteratorGetMatch();
        int min_pos = pos_0;
        int max_pos = pos_0;

        for (int i=1, l=this.args.size(); i<l; ++i) {
          QryIop q_i = (QryIop) this.args.get(i);
          if (q_i.locIteratorHasMatch()) {
            int pos_i = q_i.locIteratorGetMatch();
            min_pos = Math.min(pos_i, min_pos);
            max_pos = Math.max(pos_i, max_pos);
          } else {
            isEnd = true;
            break;
          }
        }

        if (isEnd)
          break;

        if (max_pos - min_pos < distance) {
          // match!
          posList.add(max_pos);
          // all of lists move to next pos
          for (Qry q : this.args) {
            ((QryIop)q).locIteratorAdvance();
          }
        } else {
          // don't match
          // all of lists move past min
          for (Qry q : this.args) {
            ((QryIop)q).locIteratorAdvancePast(min_pos);
          }
        }
      }

      if (posList.size() > 0) {
        this.invertedList.appendPosting(docid, posList);
      }

      // move to next document
      q_0.docIteratorAdvancePast(docid);
    }

  }
}
