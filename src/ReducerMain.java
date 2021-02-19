import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class    ReducerMain {
    public static void main(String[] args){
       Thread thread_main = new Thread(() -> {
           Registry r = null;
           ReducerServiceInterface reducerService;
           Integer port = Integer.parseInt(args[0]);
           try{
               r = LocateRegistry.createRegistry(port);
           }catch(RemoteException a){
               a.printStackTrace();
           }

           try{
               reducerService = new ReducerService(port);
               r.rebind("reducerservice", reducerService);

               System.out.println("Reducer ready port:"+port);
           }catch(Exception e) {
               System.out.println("Reducer main " + e.getMessage());
           }
       });
       thread_main.start();

        Thread thread_heartbeat = new Thread(() -> {
            while (true){
                MasterServiceInterface masterService = null;
                try{
                    Thread.sleep(2000);
                    System.out.println("Reducer["+args[0]+"] is heartbeat alive");
                    masterService = (MasterServiceInterface) Naming.lookup("rmi://localhost:2023/masterservice");
                    masterService.heartbeat_check("rmi://localhost:"+args[0]+"/reducerservice","reducer");
                    Thread.sleep(13*1000);
                }catch(Exception e){e.printStackTrace();}
            }
        });

        thread_heartbeat.start();
    }
}
