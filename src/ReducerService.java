import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

public class ReducerService extends UnicastRemoteObject implements ReducerServiceInterface {
    private final String storage_rmi_address = "rmi://localhost:2022/storageservice";
    private LinkedList<ProcessCombinationModel> combinationStatistics = new LinkedList<>();

    public ReducerService() throws RemoteException {
    }

    @Override
    public boolean process_combinations(List<Set<String>> combinations, int fileCount) throws RemoteException {
        boolean status = false;
        Set r;
        StringBuffer line = new StringBuffer();
        Iterator combIterator = combinations.iterator();
        System.out.println("Número combinações " + combinations.size());
        int i = 0;
        while (combIterator.hasNext()){
            r = (Set) combIterator.next();

            line.setLength(0);
            Iterator lineIterator = r.iterator();
            while(lineIterator.hasNext()){
                if(line.length()>0) line.append(",");
                line.append(lineIterator.next().toString());
            }

            ProcessCombinationModel combinationInfo= new ProcessCombinationModel();
            combinationInfo.combination = line.toString();
          status =  calculateStatistics(combinationInfo,fileCount);
            if(i++%100000==0) System.out.println("Comb " + i);
        }

        return status;
    }


    /**
     * Auxiliary method that calculates statistics for each resource
     * @param combinationInfo single combination of resources
     * @param fileCount number of runs
     */
    private boolean calculateStatistics(ProcessCombinationModel combinationInfo, int fileCount){
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
            System.out.print("Combination + probability");
            for(String s: resources) System.out.print(System.identityHashCode(s) + "  ");
            System.out.print(combinationInfo.percentage + "\n");

            this.combinationStatistics.add(combinationInfo);
            System.out.println("Comb valida. Percentagem: " + combinationInfo.percentage);
        }
        return true;
    }
}
