import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ReducerService extends UnicastRemoteObject implements ReducerServiceInterface {
    private final String storage_rmi_address = "rmi://localhost:2022/storageservice";
    private String id;

    public ReducerService(Integer id) throws RemoteException {
        this.id = id.toString();
    }



    @Override
    public void process_combinations(ArrayList<ArrayList<String>> combinations, int fileCount) throws RemoteException {
        System.out.println("REDUCER "+id+": Starting to process combinations" );
        boolean status = false;
        ArrayList<String> r;
        StringBuffer line = new StringBuffer();
        Iterator combIterator = combinations.iterator();
        System.out.println("REDUCER "+id+": Número combinações " + combinations.size());
        int i = 0;
        while (combIterator.hasNext()){
            r = (ArrayList<String>) combIterator.next();
            line.setLength(0);
            Iterator lineIterator = r.iterator();
            while(lineIterator.hasNext()){
                if(line.length()>0) line.append(",");
                line.append(lineIterator.next().toString());
            }
            //System.out.println("Reducer["+id+"] debug: ReadLine="+line.toString());

            CombinationProcessingData combinationProcessingData = new CombinationProcessingData();
            combinationProcessingData.combination = line.toString();
            //System.out.println("Reducer: Test combination_string = "+ combinationProcessingData.combination);
          //  if(combinationProcessingData instanceof Serializable) System.out.println("Reducer: Test combination_string serializable");

          status =  calculateStatistics(combinationProcessingData,fileCount);
            if(i++%100000==0) System.out.println("REDUCER "+id+": Comb " + i);
         }

    }


    /**
     * Auxiliary method that calculates statistics for each resource
     * @param combinationInfo single combination of resources
     * @param fileCount number of runs
     */
   private boolean calculateStatistics(CombinationProcessingData combinationInfo, int fileCount){
       // System.out.println("Reducer["+this.id+"] : Starting Calculate Statistic");
        boolean resourceFound=false;
        StorageServiceInterface storage_rmi = null;
        LinkedHashMap<String, ArrayList<ResourceInfo>> timeHarMap = null;
        try{
            storage_rmi = (StorageServiceInterface) Naming.lookup(storage_rmi_address);
            timeHarMap = storage_rmi.getTimeHarMap();
        }catch(Exception e){e.printStackTrace(); return false;}

        String[] resources = combinationInfo.combination.split(","); // resources of each combination
        for (int i =0; i < fileCount; i++) { //controlo por run
            for(String combinationResource : resources){
                resourceFound=false;
                for(ResourceInfo comb : timeHarMap.get(combinationResource))
                    if(comb.harRun == i) {
                        resourceFound = true;
                        combinationInfo.resourceLength += comb.resourceLength;
                        break;
                    }
                if(! resourceFound){ break;}
            }
            if(resourceFound){
                combinationInfo.numberOfRuns++;
            }
        }

        combinationInfo.percentage = (float) combinationInfo.numberOfRuns/fileCount;

        if(combinationInfo.percentage > 0.5) {
            System.out.print("REDUCER "+id+": Combination + probability");
            for(String s: resources) System.out.print(System.identityHashCode(s) + "  ");
            System.out.print(combinationInfo.percentage + "\n");

            try{
                storage_rmi.process_reducer_data(combinationInfo);
            }catch(Exception e){e.printStackTrace();}
            System.out.println("REDUCER "+id+": Comb valida. Percentagem: " + combinationInfo.percentage);
        }
        return true;
    }
}
