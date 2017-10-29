package com.example.harsh.mobilep2p;
import java.util.*;


/**
 * Created by dell on 10/29/2017.
 */

public class HeadInfo {
   private List<Filemetadata> files= new ArrayList<Filemetadata>();
    private Map<String, ArrayList<Filemetadata> > nodescontent=new HashMap<>();
    private Map<Filemetadata,ArrayList<String> >file_presentin;

    public void add_file_info(Filemetadata received_file,String node){
        Iterator it=files.iterator();
        Filemetadata f;
        while(it.hasNext()) {
            f = (Filemetadata) it.next();
            if (f.getFilename().equals(received_file.getFilename()) && f.getFilesize()==received_file.getFilesize()) {
                add_file_presentin(received_file,node);
            }
        }

        files.add(received_file);
        add_file_presentin(received_file,node);
        add_nodescontent(received_file,node);

        return;
    }

    public void add_file_presentin(Filemetadata received_file,String node){
        if(file_presentin.containsKey(received_file)){
            file_presentin.get(received_file).add(node);
        }
        else {
            ArrayList<String>list =new ArrayList<String>();
            list.add(node);
            file_presentin.put(received_file,list);
        }
        return;
    }

    public void add_nodescontent(Filemetadata received_file,String node){
        if(nodescontent.containsKey(node)){
            nodescontent.get(node).add(received_file);
        }
        else{
            ArrayList<Filemetadata>list =new ArrayList<Filemetadata>();
            list.add(received_file);
            nodescontent.put(node,list);
        }
        return;
    }

    Filemetadata get_file(String file_name){
        for(Filemetadata f: files){
            if(f.getFilename().equals(file_name))
                return f;
        }
        throw new IllegalArgumentException("no such file present");
      /*  Filemetadata nothing_obj=new Filemetadata();
        nothing_obj.setFilename("");
        nothing_obj.setFilesize(0);
        return nothing_obj;*/
    }

    ArrayList<String> get_nodes_containingfile(String file){
        Filemetadata f1 =new Filemetadata();
        for(Filemetadata f: files){
            if(f.getFilename().equals(file)){
                f1=f;
                break;
            }
        }
        ArrayList<String> nothing=new ArrayList<String>();
        if(file_presentin.containsKey(f1)){
            return file_presentin.get(f1);
        }
        else {
            throw new IllegalArgumentException("no such file present at any node");
            //return nothing;
        }
    }
}
