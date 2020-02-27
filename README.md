# RESTful API for money transactions between accounts.

## Backend test task for Revolut
**Author**: Igor Bobko <limit-speed@yandex.ru>

## Limitations
* Not enough logging. Need to be added.
* The system doesn't use any database, all object are created in loading.
* It supports only the list of holders, the single holder, list of accounts, total system balance and money transaction.

## Introduction
This system based on Martin Fowler's pattern Accounting Transaction: https://martinfowler.com/eaaDev/AccountingTransaction.html<br/>
In turn, this means that the methods corresponding to **double-entry bookkeeping** are used.<br>
Also It used Two-Legged approach, what means that only one account exchange is produced at once by single transaction. 

## Technologies
* Java 9
* [Joda-Money](https://www.joda.org/joda-money/) for class Money is used.
* [Google Guice](https://github.com/google/guice) for DI
* [Jetty](https://www.eclipse.org/jetty/) for servlet implementation
* [JUnit 5](https://junit.org/junit5/) for testing
* [Maven](https://maven.apache.org/) for building
* [Lombok](https://projectlombok.org) for simplification:)
* [google/gson](https://github.com/google/gson) for json 
* [Apache HttpClient](https://hc.apache.org/index.html)  for testing

## How to run
`mvn assembly:assembly`<br/>
`java -jar "./target/Revolut-Test-Task-1.0-SNAPSHOT-jar-with-dependencies.jar"`

## Access to Swagger API
http://localhost:8080/api/v1/swagger.json

## Currently available endpoints
### GET @ `http://localhost:8080/api/v1/holders`
Returns a JSON array of Holders with accounts

### GET @ `http://localhost:8080/api/v1/holders/{id}`
Returns a single JSON Holder by its id

### GET @ `http://localhost:8080/api/v1/transactions/total-system-balance/{currency}`
Returns a single JSON with Overall sum of system by specified currency.

### PUT @ `http://localhost:8080/api/v1/transactions`
Returns result of transaction.

Parameter
```
TransactionRequest: {
    type: "object",
    properties: {
    sum: {
        type: "number"
    },
    payerAccountId: {
        type: "integer",
        format: "int64"
    },
    payeeAccountId: {
        type: "integer",
        format: "int64"
    }
    }
}
```

## Http Status
* 200 OK: The request has succeeded
* 400 Bad Request: The request could not be understood by the server
* 404 Not Found: The requested resource cannot be found
* 500 Internal Server Error: The server encountered an unexpected condition



