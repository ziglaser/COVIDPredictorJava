package com.covidpredictor.covidpredictor;

public class Pool{
    int size;
    Model[] pool;

    public Pool(int size){
        pool = new Model[size];
        for (int i = 0; i < size; i++){
            pool[i] = new Model();
        }
    }

    private void nextGeneration(){

    }

    private void sort(){

    }
}



