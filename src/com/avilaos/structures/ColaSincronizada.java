/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.avilaos.structures;

/**
 *
 * @author KUBO
 */
public class ColaSincronizada<T> {
    private Nodo<T> primero;
    private Nodo<T> ultimo;
    private int tamaño;

    public ColaSincronizada() {
        this.primero = null;
        this.ultimo = null;
        this.tamaño = 0;
    }

    public synchronized void encolar(T elemento) {
        Nodo<T> nuevoNodo = new Nodo<>(elemento);
        if (estaVacia()) {
            primero = nuevoNodo;
            ultimo = nuevoNodo;
        } else {
            ultimo.setSiguiente(nuevoNodo);
            ultimo = nuevoNodo;
        }
        tamaño++;
    }

    public synchronized T desencolar() {
        if (estaVacia()) {
            return null;
        }
        T dato = primero.getDato();
        primero = primero.getSiguiente();
        if (primero == null) {
            ultimo = null;
        }
        tamaño--;
        return dato;
    }

    public synchronized T verPrimero() {
        if (estaVacia()) {
            return null;
        }
        return primero.getDato();
    }

    public synchronized boolean eliminar(T elemento) {
        if (estaVacia()) return false;

        if (primero.getDato().equals(elemento)) {
            desencolar();
            return true;
        }

        Nodo<T> actual = primero;
        while (actual.getSiguiente() != null) {
            if (actual.getSiguiente().getDato().equals(elemento)) {
                if (actual.getSiguiente() == ultimo) {
                    ultimo = actual;
                }
                actual.setSiguiente(actual.getSiguiente().getSiguiente());
                tamaño--;
                return true;
            }
            actual = actual.getSiguiente();
        }
        return false;
    }

    public synchronized int getTamaño() {
        return tamaño;
    }

    public synchronized boolean estaVacia() {
        return tamaño == 0;
    }
}
