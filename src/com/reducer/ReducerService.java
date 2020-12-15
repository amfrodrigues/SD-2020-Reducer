package com.reducer;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ReducerService extends UnicastRemoteObject implements ReducerServiceInterface {

    public ReducerService() throws RemoteException {
    }
}
