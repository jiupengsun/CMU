import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by samy on 11/22/16.
 */
public class ParseTrec {

  public static void main(String[] args) {
    if(args.length == 0) {
      System.out.println("No input parameter");
      System.exit(1);
    }
    String filePath = args[0];
    try {
      File f = new File(filePath);
      if(!f.exists()) {
        throw new Exception("File not found");
      }
      BufferedReader br = new BufferedReader(new FileReader(f));
      String line;
      List<List<String>> list = new ArrayList<>();
      while( (line=br.readLine())!=null) {
        String[] score = line.split(",");
        if(list.size() == 0) {
          int size = score.length;
          for(int i=0; i<size; ++i) {
            List<String> l = new ArrayList<>();
            l.add(score[i]);
            list.add(l);
          }
        } else {
          for(int i=0; i<score.length; ++i) {
            list.get(i).add(score[i]);
          }
        }
      }
      List<String> ndcg = list.get(13);
      System.out.println("NDCG: " + ndcg.get(ndcg.size()-1));
      List<String> ia10 = list.get(18);
      System.out.println("IA10: " + ia10.get(ia10.size()-1));
      List<String> ia20 = list.get(19);
      System.out.println("IA20: " + ia20.get(ia20.size()-1));
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
