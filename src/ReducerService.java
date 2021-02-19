import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ReducerService extends UnicastRemoteObject implements ReducerServiceInterface {
    private final String storage_rmi_address = "rmi://localhost:2022/storageservice";
    private final String id;

    public ReducerService(Integer id) throws RemoteException {
        this.id = id.toString();
    }


    /*
        Method that processes the combinations issued by the mappers
     */
    @Override
    public boolean process_combinations(ArrayList<ArrayList<String>> combinations) throws RemoteException {
      //  System.out.println("REDUCER "+id+": Starting to process combinations" );
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

            CombinationProcessingData combinationProcessingData = new CombinationProcessingData();
            combinationProcessingData.combination = line.toString();
           StorageServiceInterface storage_rmi = null;

            try{
                storage_rmi = (StorageServiceInterface) Naming.lookup(storage_rmi_address);
            }catch(Exception e){e.printStackTrace();}
            calculateStatistics(combinationProcessingData,storage_rmi.getFileCount());
            if(i++%100000==0) System.out.println("REDUCER "+id+": Comb " + i);
         }
            System.out.println("Reducer["+this.id+"] finished the task");
        return true;
    }


    /**
     * Auxiliary method that calculates statistics for each resource
     * @param combinationInfo single combination of resources
     * @param fileCount number of runs
     */
   private void calculateStatistics(CombinationProcessingData combinationInfo, int fileCount){
       // System.out.println("Reducer["+this.id+"] : Starting Calculate Statistic");
        boolean resourceFound=false;
        StorageServiceInterface storage_rmi = null;
        LinkedHashMap<String, ArrayList<ResourceInfo>> timeHarMap = null;
        try{
            storage_rmi = (StorageServiceInterface) Naming.lookup(storage_rmi_address);
            timeHarMap = storage_rmi.getTimeHarMap();
        }catch(Exception e){e.printStackTrace();}

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
                storage_rmi.process_reducer_data(combinationInfo); // saves the statistics in the storage so master can get them for the client
            }catch(Exception e){e.printStackTrace();}
            System.out.println("REDUCER "+id+": Comb valida. Percentagem: " + combinationInfo.percentage);
        }
    }
}
