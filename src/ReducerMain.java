import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class    ReducerMain {
    public static void main(String[] args){
        Registry r = null;
        ReducerServiceInterface reducerService;
        Integer port = Integer.parseInt(args[0]);
        try{
            r = LocateRegistry.createRegistry(port);
        }catch(RemoteException a){
            a.printStackTrace();
        }

        try{
            reducerService = new ReducerService();
            r.rebind("reducerservice", reducerService);

            System.out.println("Reducer ready");
        }catch(Exception e) {
            System.out.println("Reducer main " + e.getMessage());
        }
    }
}
