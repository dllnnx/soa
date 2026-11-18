package ru.itmo.oscars.service;

public class CommandPublicationException extends RuntimeException {
    public CommandPublicationException(Throwable cause) {
        super("Очередь команд временно недоступна; приём операции не подтверждён", cause);
    }
}
