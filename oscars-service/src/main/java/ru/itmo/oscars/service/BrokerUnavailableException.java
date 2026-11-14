package ru.itmo.oscars.service;

public class BrokerUnavailableException extends RuntimeException {
    public BrokerUnavailableException() {
        super("Очередь задач временно недоступна; попробуйте позже");
    }
}
